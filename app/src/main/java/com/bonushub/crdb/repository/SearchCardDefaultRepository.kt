package com.bonushub.crdb.repository

import android.content.Context
import android.os.Bundle
import android.os.RemoteException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bonushub.crdb.R
import com.bonushub.crdb.di.USDKScope
import com.bonushub.crdb.entity.CardOption
import com.bonushub.crdb.entity.EMVOption
import com.bonushub.crdb.model.CardProcessedDataModal
import com.bonushub.crdb.utils.BytesUtil
import com.bonushub.crdb.utils.Field48ResponseTimestamp
import com.bonushub.crdb.utils.getEncryptedPanorTrackData
import com.bonushub.crdb.utils.ingenico.DemoConfig
import com.bonushub.crdb.utils.ingenico.DialogUtil
import com.bonushub.crdb.utils.ingenico.EMVInfoUtil
import com.bonushub.crdb.utils.ingenico.TLV
import com.bonushub.crdb.utils.logger
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.activity.TransactionActivity
import com.bonushub.crdb.view.activity.TransactionActivity.*
import com.bonushub.pax.utils.EFallbackCode
import com.bonushub.pax.utils.TransactionType
import com.usdk.apiservice.aidl.BaseError
import com.usdk.apiservice.aidl.algorithm.AlgError
import com.usdk.apiservice.aidl.algorithm.AlgMode
import com.usdk.apiservice.aidl.algorithm.UAlgorithm
import com.usdk.apiservice.aidl.data.BytesValue
import com.usdk.apiservice.aidl.emv.*
import com.usdk.apiservice.aidl.pinpad.MagTrackEncMode
import com.usdk.apiservice.aidl.pinpad.UPinpad
import dagger.hilt.android.qualifiers.ActivityContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import javax.inject.Inject


class SearchCardDefaultRepository @Inject constructor(@USDKScope private var algorithm: UAlgorithm?,
                                                      @USDKScope private var pinpad: UPinpad?,
                                                      @USDKScope private var emv: UEMV?,
                                                      @ActivityContext private val context: Context): SearchCardRepository {

    val TAG = SearchCardDefaultRepository::class.java.simpleName

    private var lastCardRecord: CardRecord? = null

    private val _insertCardStatus = MutableLiveData<CardProcessedDataModal>()

    override fun stopEmv() {
        emv?.stopSearch()
        emv?.stopEMV()

    }

    override fun observeCardType(cardProcessedDataModal: CardProcessedDataModal, cardOption: CardOption): LiveData<CardProcessedDataModal> {
      var cardType = detectCard(cardProcessedDataModal,cardOption)

        return _insertCardStatus
    }

    override fun observemvEventHandler(emvOption: EMVOption, cardProcessedDataModal: CardProcessedDataModal) : LiveData<CardProcessedDataModal>{
      var carddata = startemv(emvOption,cardProcessedDataModal)

        return _insertCardStatus
    }

    // Detecting the card type ie(emv,cls,mag...)
    private fun detectCard(cardProcessedDataModal: CardProcessedDataModal,cardOption: CardOption){

        try {
            emv?.searchCard(cardOption.toBundle(), DemoConfig.TIMEOUT, object : SearchCardListener.Stub() {

                override fun onCardPass(cardType: Int) {
                    logger(TAG, "=> onCardPass | cardType = $cardType")
                     cardProcessedDataModal.setReadCardType(DetectCardType.CONTACT_LESS_CARD_TYPE)
                     _insertCardStatus.postValue(cardProcessedDataModal)
                     emv?.stopSearch()

                }

                override fun onCardInsert() {
                    logger(TAG, "=> onCardInsert")
                      cardProcessedDataModal.setReadCardType(DetectCardType.EMV_CARD_TYPE)
                   //  _insertCardStatus.postValue(cardProcessedDataModal)
                    startemv(EMVOption.create(), cardProcessedDataModal)
                   // emv?.stopSearch()

                }

                override fun onCardSwiped(track: Bundle) {
                    logger(TAG, "=> onCardSwiped")
                    logger(TAG, "==> Pan: " + track.getString(EMVData.PAN))
                    logger(TAG, "==> Track 1: " + track.getString(EMVData.TRACK1))
                    logger(TAG, "==> Track 2: " + track.getString(EMVData.TRACK2))
                    logger(TAG, "==> Track 3: " + track.getString(EMVData.TRACK3))
                    logger(TAG, "==> Service code: " + track.getString(EMVData.SERVICE_CODE))
                    logger(TAG, "==> Card exprited date: " + track.getString(EMVData.EXPIRED_DATE))

                    val trackStates = track.getIntArray(EMVData.TRACK_STATES)
                    for (i in trackStates!!.indices) {
                        logger(
                            TAG,
                            "=> onCardSwiped" + String.format(
                                "==> Track %s state: %d",
                                i + 1,
                                trackStates!![i]
                            )
                        )
                    }

                    var serviceCode = track.getString(EMVData.SERVICE_CODE)

                    if (serviceCode != null) {
                        cardProcessedDataModal.setServiceCodeData(serviceCode)
                    }
                    val sc = cardProcessedDataModal.getServiceCodeData()
                    // val sc: String? = ""
                    var scFirstByte: Char? = null
                    var scLastbyte: Char? = null
                    if (null != sc) {
                        scFirstByte = sc.first()
                        scLastbyte = sc.last()

                    }
                    if (cardProcessedDataModal.getFallbackType() != EFallbackCode.EMV_fallback.fallBackCode){
                             //Checking Fallback
                        if (scFirstByte == '2' || scFirstByte == '6') {
                            doEndProcess(EFallbackCode.Swipe_fallback.fallBackCode, cardProcessedDataModal)
                        }
                     }
                    else {

                        cardProcessedDataModal.setReadCardType(DetectCardType.MAG_CARD_TYPE)
                        var encryptTrack2data =
                            getEncryptedPanorTrackData(track.getString(EMVData.TRACK2) ?: "", true)
                        println("Track data is" + track.getString(EMVData.TRACK2))
                        cardProcessedDataModal.setTrack2Data(encryptTrack2data ?: "")
                        _insertCardStatus.postValue(cardProcessedDataModal)
                        emv?.stopSearch()
                    }
                }

                override fun onTimeout() {
                    logger(TAG, "=> onTimeout")
                    cardProcessedDataModal.setReadCardType(DetectCardType.CARD_ERROR_TYPE)
                    _insertCardStatus.postValue(cardProcessedDataModal)

                    emv?.stopSearch()
                    emv?.stopEMV()
                }

                override fun onError(code: Int, message: String) {
                    logger(TAG, "=> onError"+String.format("=> onError | %s[0x%02X]", message, code))
                    emv?.stopSearch()

                }
            })
        } catch (e: Exception) {
          //  handleException(e)
        }

    }


    private fun startemv(emvOption: EMVOption,cardProcessedDataModal: CardProcessedDataModal) {

        emv?.startEMV(emvOption.toBundle(),object : EMVEventHandler.Stub(){
            @Throws(RemoteException::class)
            override fun onInitEMV() {
                doInitEMV()
            }

            @Throws(RemoteException::class)
            override fun onWaitCard(flag: Int) {
                doWaitCard(flag)
            }

            @Throws(RemoteException::class)
            override fun onCardChecked(cardType: Int) {
                // Only happen when use startProcess()
                // doCardChecked(cardType)
            }

            @Throws(RemoteException::class)
            override fun onAppSelect(reSelect: Boolean, list: List<CandidateAID>) {
                doAppSelect(reSelect, list)
            }

            @Throws(RemoteException::class)
            override fun onFinalSelect(finalData: FinalData) {
                //doFinalSelect(finalData);
                emv?.respondEvent(null)
            }

            @Throws(RemoteException::class)
            override fun onReadRecord(cardRecord: CardRecord) {
                lastCardRecord = cardRecord
                doReadRecord(cardRecord,cardProcessedDataModal)
            }

            @Throws(RemoteException::class)
            override fun onCardHolderVerify(cvmMethod: CVMMethod) {

            }

            @Throws(RemoteException::class)
            override fun onOnlineProcess(transData: TransData) {

            }

            @Throws(RemoteException::class)
            override fun onEndProcess(result: Int, transData: TransData) {
                doEndProcess(result,cardProcessedDataModal)
            }

            @Throws(RemoteException::class)
            override fun onVerifyOfflinePin(
                flag: Int,
                random: ByteArray,
                caPublicKey: CAPublicKey,
                offlinePinVerifyResult: OfflinePinVerifyResult
            ) {

            }

            @Throws(RemoteException::class)
            override fun onObtainData(ins: Int, data: ByteArray) {

            }

            @Throws(RemoteException::class)
            override fun onSendOut(ins: Int, data: ByteArray) {

            }

         })


    }

    /**
     * Inform the application that the EMV transaction is completed and the kernel exits.
     */
    fun doEndProcess(result: Int,cardProcessedDataModal: CardProcessedDataModal) {
        println("Fallbackcode is "+result)
        when (result) {
            //Swipe fallabck case when cip and swipe card used
            EFallbackCode.Swipe_fallback.fallBackCode -> {
                cardProcessedDataModal.setFallbackType(EFallbackCode.Swipe_fallback.fallBackCode)
                //_insertCardStatus.postValue(cardProcessedDataModal)
                (context as? TransactionActivity)?.handleEMVFallbackFromError(context.getString(R.string.fallback),
                    context.getString(R.string.please_use_another_option), false) {
                    cardProcessedDataModal.setFallbackType(EFallbackCode.Swipe_fallback.fallBackCode)
                    detectCard(cardProcessedDataModal, CardOption.create().apply {
                        supportICCard(true)
                        supportMagCard(false)
                        supportRFCard(false)
                    })
                }
            }

            CardErrorCode.EMV_FALLBACK_ERROR_CODE.errorCode -> {
                //EMV Fallback case when we insert card from other side then chip side:-
                cardProcessedDataModal.setReadCardType(DetectCardType.EMV_Fallback_TYPE)
                cardProcessedDataModal.setFallbackType(EFallbackCode.EMV_fallback.fallBackCode)


                (context as? TransactionActivity)?.handleEMVFallbackFromError(context.getString(R.string.fallback), context.getString(R.string.please_use_another_option), false) {
                    cardProcessedDataModal.setFallbackType(EFallbackCode.EMV_fallback.fallBackCode)
                    detectCard(cardProcessedDataModal, CardOption.create().apply {
                        supportICCard(false)
                        supportMagCard(true)
                        supportRFCard(false)
                    })
                }
            // _insertCardStatus.postValue(cardProcessedDataModal)
            }

            else -> {

            }

        }


        /*    if (result != EMVError.SUCCESS) {
                println("=> onEndProcess | " + EMVInfoUtil.getErrorMessage(result))
            } else {
                println("=> onEndProcess | EMV_RESULT_NORMAL | " + EMVInfoUtil.getTransDataDesc(transData))
            }
            println("\n")*/
    }


    @Throws(RemoteException::class)
    fun doInitEMV() {
        //    outputText("=> onInitEMV ")
        manageAID()

        //  init transaction parameters，please refer to transaction parameters
        //  chapter about onInitEMV event in《UEMV develop guide》
        //  For example, if VISA is supported in the current transaction,
        //  the label: DEF_TAG_PSE_FLAG(M) must be set, as follows:
        emv?.setTLV(KernelID.VISA, EMVTag.DEF_TAG_PSE_FLAG, "03")
        // For example, if AMEX is supported in the current transaction，
        // labels DEF_TAG_PSE_FLAG(M) and DEF_TAG_PPSE_6A82_TURNTO_AIDLIST(M) must be set, as follows：
        // emv.setTLV(KernelID.AMEX, EMVTag.DEF_TAG_PSE_FLAG, "03");
        // emv.setTLV(KernelID.AMEX, EMVTag.DEF_TAG_PPSE_6A82_TURNTO_AIDLIST, "01");
    }

    @Throws(RemoteException::class)
    protected fun manageAID() {
        //   outputBlueText("****** manage AID ******")
        val aids = arrayOf(
            "A000000333010106",
            "A000000333010103",
            "A000000333010102",
            "A000000333010101",
            "A0000000651010",
            "A0000000043060",
            "A0000000041010",
            "A000000003101001",
            "A000000003101002",
            "A000000003101004",
            "A0000000031010"
        )
        for (aid in aids) {
            val ret: Int = emv!!.manageAID(ActionFlag.ADD, aid, true)
            //  outputResult(ret, "=> add AID : $aid")
        }
    }

    @Throws(RemoteException::class)
    fun doWaitCard(flag: Int) {
       /*   when (flag) {
              WaitCardFlag.ISS_SCRIPT_UPDATE,
              WaitCardFlag.SHOW_CARD_AGAIN -> searchRFCard(Runnable { respondCard() })
              WaitCardFlag.EXECUTE_CDCVM -> {
                  emv?.halt()
                  uiHandler.postDelayed(Runnable { searchRFCard(Runnable { respondCard() }) }, 1200)
              }
              else -> outputRedText("!!!! unknow flag !!!!")
          }*/
    }

    fun doAppSelect(reSelect: Boolean, candList: List<CandidateAID>) {
        //  outputText("=> onAppSelect: cand AID size = " + candList.size)
        if (candList.size > 1) {
            selectApp(candList, object : DialogUtil.OnSelectListener {
                override fun onCancel() {
                    try {
                        emv?.stopEMV()
                    } catch (e: RemoteException) {
                        e.printStackTrace()
                    }
                }

                override fun onSelected(item: Int) {
                     respondAID(candList[item].aid)
                }
            })
        } else {
               respondAID(candList[0].aid)
        }
    }


    protected fun respondAID(aid: ByteArray?) {
        try {
            // outputBlueText("Select aid: " + BytesUtil.bytes2HexString(aid))
            val tmAid = TLV.fromData(EMVTag.EMV_TAG_TM_AID, aid)
            emv!!.respondEvent(tmAid.toString())
        } catch (e: Exception) {
            //  handleException(e)
        }
    }

    @Throws(RemoteException::class)
    fun doReadRecord(record: CardRecord?, cardProcessedDataModal: CardProcessedDataModal) {
        cardProcessedDataModal.setPanNumberData(EMVInfoUtil.getRecordDataDesc(record))
        System.out.println("Card pannumber data"+EMVInfoUtil.getRecordDataDesc(record))
        var encrptedPan = getEncryptedPanorTrackData(EMVInfoUtil.getRecordDataDesc(record),false)
        cardProcessedDataModal.setEncryptedPan(encrptedPan)
        println("Pannumber is"+EMVInfoUtil.getRecordDataDesc(record))
        TDES(EMVInfoUtil.getRecordDataDesc(record))
        _insertCardStatus.postValue(cardProcessedDataModal)
        // outputText("=> onReadRecord | " + EMVInfoUtil.getRecordDataDesc(record))
        // outputResult(emv.respondEvent(null), "...onReadRecord: respondEvent")
    }

    protected fun selectApp(candList: List<CandidateAID>, listener: DialogUtil.OnSelectListener?) {
        val aidInfoList: MutableList<String> = ArrayList()
        for (candAid in candList) {
            aidInfoList.add(String(candAid.apn))
        }
        (context as TransactionActivity).runOnUiThread(Runnable {
            DialogUtil.showSelectDialog(context, "Please select app", aidInfoList, 0, listener)
        })
    }

    private fun TDES(recordDataDesc: String) {
        try {
            logger(TAG, "=> TDES")
            val key: ByteArray = BytesUtil.hexString2Bytes("31313131313131313131313131313131")
            val dataIn: ByteArray = BytesUtil.hexString2Bytes(recordDataDesc)
            var mode = AlgMode.EM_alg_TDESENCRYPT or AlgMode.EM_alg_TDESTECBMODE
            val encResult = BytesValue()
            var ret = algorithm?.TDES(mode, key, dataIn, encResult)
            if (ret != AlgError.SUCCESS) {
                ret?.let { getErrorDetail(it) }?.let { logger(TAG, it) }
                return
            }
            logger(TAG,"Encrypt result : " + encResult.toHexString())
            val decResult = BytesValue()
            mode = AlgMode.EM_alg_TDESDECRYPT or AlgMode.EM_alg_TDESTECBMODE
            ret = algorithm?.TDES(mode, key, encResult.data, decResult)
            if (ret != AlgError.SUCCESS) {
                ret?.let { getErrorDetail(it) }?.let { logger(TAG, it) }
                return
            }
            logger(TAG,"Decrypt result : " + decResult.toHexString())
        } catch (e: Exception) {
           // handleException(e)
        }
    }

    fun getErrorDetail(error: Int): String {
        val message = getErrorMessage(error)
        return if (error < 0) {
            "$message[$error]"
        } else message + String.format("[0x%02X]", error)
    }

    fun getErrorMessage(error: Int): String {
        val message: String
        message = when (error) {
            BaseError.SERVICE_CRASH -> "SERVICE_CRASH"
            BaseError.REQUEST_EXCEPTION -> "REQUEST_EXCEPTION"
            BaseError.ERROR_CANNOT_EXECUTABLE -> "ERROR_CANNOT_EXECUTABLE"
            BaseError.ERROR_INTERRUPTED -> "ERROR_INTERRUPTED"
            BaseError.ERROR_HANDLE_INVALID -> "ERROR_HANDLE_INVALID"
            else -> "Unknown error"
        }
        return message
    }

    fun encryptMagTrack(keyId: Int) {
       // outputBlueText(">>> encryptMagTrack")
        try {
            val mode = MagTrackEncMode.MTEM_ECBMODE
            val trackData = "12345678"
            val result: ByteArray? =
                pinpad?.encryptMagTrack(mode, keyId, BytesUtil.hexString2Bytes(trackData))
            if (result == null) {
               // outputPinpadError("encryptMagTrack fail")
                return
            }
         //   outputText("ECB encrypt result = " + BytesUtil.bytes2HexString(result))
        } catch (e: Exception) {
           // handleException(e)
        }
    }
}