package com.bonushub.crdb.india.view.baseemv


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import android.widget.Toast
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.entity.CardOption
import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.transactionprocess.CompleteSecondGenAc
import com.bonushub.crdb.india.transactionprocess.SecondGenAcOnNetworkError
import com.bonushub.crdb.india.type.DemoConfigs
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.ingenico.DialogUtil
import com.bonushub.crdb.india.utils.ingenico.EMVInfoUtil
import com.bonushub.crdb.india.utils.ingenico.TLV
import com.bonushub.crdb.india.utils.ingenico.TLVList
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.activity.TransactionActivity
import com.bonushub.crdb.india.view.base.BaseActivityNew
import com.bonushub.crdb.india.vxutils.Utility
import com.bonushub.crdb.india.vxutils.Utility.byte2HexStr
import com.bonushub.crdb.india.vxutils.getEncryptedPanorTrackData
import com.usdk.apiservice.aidl.emv.*
import com.usdk.apiservice.aidl.pinpad.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*


open class EmvHandler constructor(): EMVEventHandler.Stub() {

    companion object {
        val TAG = EmvHandler::class.java.simpleName
    }

    private var defaultScope = CoroutineScope(Dispatchers.Default)
    //for offline Pin
    var pinTryRemainingTimes = 0
    var pinTryCounter = -1
    var maxPin =0;

    //result: Int, transData: TransData?
    lateinit var onEndProcessCallback: (Int,CardProcessedDataModal) -> Unit

    private var lastCardRecord: CardRecord? = null

    private var pinPad: UPinpad? = null
    private var emv: UEMV? = null
    private lateinit var activity: TransactionActivity
    private lateinit var cardProcessedDataModal: CardProcessedDataModal
    private lateinit var testCompleteSecondGenAc:CompleteSecondGenAc
    private lateinit var secondGenAconNetworkError:  SecondGenAcOnNetworkError

    lateinit var vfEmvHandlerCallback: (CardProcessedDataModal) -> Unit
    lateinit var vfFallbackCallback: (CardProcessedDataModal) -> Unit

    constructor(
        pinPad: UPinpad?,
        emv: UEMV?, activity: TransactionActivity, cardProcessedDataModal: CardProcessedDataModal,
        vfEmvHandlerCallback: (CardProcessedDataModal) -> Unit):this()
    {
        this.pinPad = pinPad
        this.emv = emv
        this.activity = activity
        this.cardProcessedDataModal = cardProcessedDataModal
        this.vfEmvHandlerCallback = vfEmvHandlerCallback
        this.vfFallbackCallback   = vfEmvHandlerCallback
    }

    //1
    @Throws(RemoteException::class)
    override fun onInitEMV() {
        Log.e("VFEmvHandler","onInitEMV")
        doInitEMV()

    }

    @Throws(RemoteException::class)
    override fun onWaitCard(flag: Int) {
        Log.e("VFEmvHandler","onWaitCard  --->   $flag")
        // WaitCardFlag.EXECUTE_CDCVM

        when (flag) {
            WaitCardFlag.NORMAL -> {
                defaultScope.launch {
                    println("Searching card...")
                    try {
                        val cardOption = CardOption.create().apply {
                            supportICCard(true)
                            supportMagCard(true)
                            supportRFCard(true)
                        }
                        DeviceHelper.getEMV()?.searchCard(
                            cardOption.toBundle(),
                            DemoConfigs.TIMEOUT,
                            object : SearchCardListener.Stub() {
                                override fun onCardPass(cardType: Int) {
                                    println("=> onCardPass | cardType = $cardType")
                                    cardProcessedDataModal.setReadCardType(DetectCardType.CONTACT_LESS_CARD_TYPE)
                                    // transactionCallback(cardProcessedDataModal)
                                    emv?.respondCard()
                                }

                                override fun onCardInsert() {
                                    println("=> onCardInsert")
                                    cardProcessedDataModal.setReadCardType(DetectCardType.EMV_CARD_TYPE)
                                    //  transactionCallback(cardProcessedDataModal)

                                }

                                override fun onCardSwiped(track: Bundle) {

                                }

                                override fun onTimeout() {
                                    println("=> onTimeout")
                                    cardProcessedDataModal.setReadCardType(DetectCardType.TIMEOUT)
                                    // onEndProcess()
                                }

                                override fun onError(code: Int, message: String) {
                                    println("Code: $code")
                                    println("message: $message")
                                    println(String.format("=> onError | %s[0x%02X]", message, code))
                                }
                            })
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            WaitCardFlag.EXECUTE_CDCVM -> {
                defaultScope.launch(Dispatchers.Main) {
                    (activity as? BaseActivityNew)?.alertBoxWithActionNew(
                        activity?.getString(R.string.see_Phone),
                        "Execute CDCVM",
                        R.drawable.ic_txn_declined,
                        activity?.getString(R.string.positive_button_ok),
                        "", false, true,
                        { alertPositiveCallback ->
                            if (alertPositiveCallback) {
                                try {
                                    val cardOption = CardOption.create().apply {
                                        supportICCard(true)
                                        supportMagCard(true)
                                        supportRFCard(true)
                                    }
                                    DeviceHelper.getEMV()?.searchCard(
                                        cardOption.toBundle(),
                                        DemoConfigs.TIMEOUT,
                                        object : SearchCardListener.Stub() {
                                            override fun onCardPass(cardType: Int) {
                                                println("=> onCardPass | cardType = $cardType")
                                                cardProcessedDataModal.setReadCardType(DetectCardType.CONTACT_LESS_CARD_TYPE)
                                                // transactionCallback(cardProcessedDataModal)
                                                emv?.respondCard()
                                            }

                                            override fun onCardInsert() {
                                                println("=> onCardInsert")
                                                cardProcessedDataModal.setReadCardType(DetectCardType.EMV_CARD_TYPE)
                                                //  transactionCallback(cardProcessedDataModal)

                                            }

                                            override fun onCardSwiped(track: Bundle) {

                                            }

                                            override fun onTimeout() {
                                                println("=> onTimeout")
                                                cardProcessedDataModal.setReadCardType(DetectCardType.TIMEOUT)
                                                // onEndProcess()
                                            }

                                            override fun onError(code: Int, message: String) {
                                                println("Code: $code")
                                                println("message: $message")
                                                println(String.format("=> onError | %s[0x%02X]", message, code))
                                            }
                                        })
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        {})

                }


            }
        }


    }

    @Throws(RemoteException::class)
    override fun onCardChecked(cardType: Int) {
        // Only happen when use startProcess()
        Log.e("VFEmvHandler","onCardChecked")
    }

    //2
    @Throws(RemoteException::class)
    override fun onAppSelect(reSelect: Boolean, list: List<CandidateAID>) {
        Log.e("VFEmvHandler","onAppSelect")
        doAppSelect(reSelect, list)
    }

    //3
    @Throws(RemoteException::class)
    override fun onFinalSelect(finalData: FinalData?) {
        Log.e("VFEmvHandler","onFinalSelect")
        if (finalData != null) {
          doFinalSelect(finalData,cardProcessedDataModal,emv)

        }
    }

    //5
    @Throws(RemoteException::class)
    override fun onReadRecord(cardRecord: CardRecord) {
        Log.e("VFEmvHandler","onReadRecord")
        lastCardRecord = cardRecord
        doReadRecord(cardRecord)
    }
// 6
    @Throws(RemoteException::class)
    override fun onCardHolderVerify(cvmMethod: CVMMethod?) {
        Log.e("VFEmvHandler","onCardHolderVerify")
        if (cvmMethod != null) {
            doCardHolderVerify(cvmMethod)
        }
    }

    @Throws(RemoteException::class)
    override fun onOnlineProcess(transData: TransData?) {
        Log.e("VFEmvHandler","onOnlineProcess")
        if (transData != null) {
            doOnlineProcess(transData)
        }

    }

    @Throws(RemoteException::class)
    override fun onEndProcess(result: Int, transData: TransData?) {
        Log.e("VFEmvHandler","onEndProcess")
        doEndProcess(result, transData)
    }

    @Throws(RemoteException::class)
    override fun onVerifyOfflinePin(flag: Int, random: ByteArray?, caPublicKey: CAPublicKey?, offlinePinVerifyResult: OfflinePinVerifyResult?) {
        Log.e("VFEmvHandler","onVerifyOfflinePin")
        if (offlinePinVerifyResult != null) {
            doVerifyOfflinePin(flag, random, caPublicKey, offlinePinVerifyResult)
        }
    }

    @Throws(RemoteException::class)
    override fun onObtainData(ins: Int, data: ByteArray?) {
        Log.e("VFEmvHandler","onObtainData")
        //	outputText("=> onObtainData: instruction is 0x" + Integer.toHexString(ins) + ", data is " + BytesUtil.bytes2HexString(data));
    }

    //4
    @Throws(RemoteException::class)
    override fun onSendOut(ins: Int, data: ByteArray?) {
        Log.e("VFEmvHandler","onSendOut")
        doSendOut(ins, data!!)

    }


    // 1 calling
    @Throws(RemoteException::class)
    fun doInitEMV() {
        println("=> onInitEMV ")
        settingAids(emv)
        settingCAPkeys(emv)
    }

    // 2 calling
    open fun doAppSelect(reSelect: Boolean, candList: List<CandidateAID>) {
        println("=> onAppSelect: cand AID size = " + candList.size)
        if (candList.size > 1) {
            selectApp(candList, object : DialogUtil.OnSelectListener {
                override fun onCancel() {
                    try {
                        emv!!.stopEMV()
                        val intent = Intent(activity, NavigationActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        activity.startActivity(intent)

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

    private fun selectApp(candList: List<CandidateAID>, listener: DialogUtil.OnSelectListener?) {
        activity.runOnUiThread {
            DialogUtil.showSelectDialog(
                activity,
                "Please select app",
                candList,
                0,
                listener
            )
        }
    }



    @Throws(RemoteException::class)
    open fun doOnlineProcess(transData: TransData) {
        System.out.println("=> onOnlineProcess | TLVData for online:" + BytesUtil.bytes2HexString(transData.tlvData))

        println("EMV Balance is" + emv!!.balance)
        println("TLV data is" + emv!!.getTLV("9F02"))
        // Commented by Manish Becz Online PIN Pos code is wrong
        // cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_NO_PIN.posEntry.toString())
        when (cardProcessedDataModal.getReadCardType()) {
            DetectCardType.EMV_CARD_TYPE -> {
                when (cardProcessedDataModal.getIsOnline()) {
                    0 -> cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_NO_PIN.posEntry.toString())
                }
            }
            DetectCardType.CONTACT_LESS_CARD_TYPE -> {
                when (cardProcessedDataModal.getIsOnline()) {
                    0 -> cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_EMV_POS_ENTRY_CODE.posEntry.toString())
                }
            }
            DetectCardType.CONTACT_LESS_CARD_WITH_MAG_TYPE -> {
                when (cardProcessedDataModal.getIsOnline()) {
                    0 -> cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_MSD_POS_ENTRY_CODE.posEntry.toString())
                }
            }

            DetectCardType.MAG_CARD_TYPE -> cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_MSD_POS_ENTRY_CODE.posEntry.toString())

            else -> {
            }
        }

        //   cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_NO_PIN.posEntry.toString())

        val applicationsquence = emv!!.getTLV(Integer.toHexString(0x5F34))

        cardProcessedDataModal.setApplicationPanSequenceValue(applicationsquence)

        // val onlineResult: String = doOnlineProcess()
        // val ret = emv!!.respondEvent(onlineResult)
        // println("...onOnlineProcess: respondEvent" + ret)
        //  tagOfF55.toString()

        val field55: String = getFields55()
        println("Field 55 is $field55")
        cardProcessedDataModal.setField55(field55)
        vfEmvHandlerCallback(cardProcessedDataModal)
    }


    @Throws(RemoteException::class)
    open fun getFields55(): String {
        val tagList = arrayOf(
            0x5F2A,
            0x5F34,
            0x82,
            0x84,
            0x95,
            0x9A,
            0x9B,
            0x9C,
            0x9F02,
            0x9F03,
            0x9F06,
            0x9F1A,
            /*      0x9F6E,*/
            0x9F26,
            0x9F27,
            0x9F33,
            0x9F34,
            0x9F35,
            0x9F36,
            0x9F37,
            0x9F10,

            /*
              0x9F08// App version
             0x8E,
               0x9F2E,
               0x9F2D,
               0x9F2F*/

        )
        val sb = StringBuilder()
        for (tag in tagList) {
            //   println(  "9F34 --->  "+    emv!!.getTLV(Integer.toHexString(f).toUpperCase(Locale.ROOT)))
            if(emv!!.getTLV(Integer.toHexString(tag).toUpperCase(Locale.ROOT)).isEmpty())
                continue

            val v1 = emv!!.getTLV(Integer.toHexString(tag).toUpperCase(Locale.ROOT))
            val v = BytesUtil.hexString2Bytes(v1)
            if (v != null) {
                sb.append(Integer.toHexString(tag))
                var l = Integer.toHexString(v.size)
                if (l.length < 2) {
                    l = "0$l"
                }

                if (tag == 0x9F10 && CardAid.AMEX.aid == cardProcessedDataModal.getAID()){//f == 0x9F10 /*&& CardAid.AMEX.aid == cardProcessedDataModal.getAID()*/) {
                    val c = l + BytesUtil.bytes2HexString(v)
                    var le = Integer.toHexString(c.length / 2)
                    if (le.length < 2) {
                        le = "0$le"
                    }

                    sb.append(le)
                    sb.append(c)

                } else {
                    sb.append(l)
                    sb.append(BytesUtil.bytes2HexString(v))
                }
            } else if (tag == 0x9F03) {
                sb.append(Integer.toHexString(tag))
                sb.append("06")
                sb.append("000000000000")
            } else if (tag == 0x5f34 /*&& CardAid.Rupay.aid.equals(cardProcessedDataModal.getAID())*/) {
                sb.append(Integer.toHexString(tag))
                sb.append("01")
                sb.append("00")
            }
        }
        return sb.toString().toUpperCase(Locale.ROOT)
    }

    open fun doOnlineProcess(): String {
        println("****** doOnlineProcess ******")
        println("... ...")
        println("... ...")
        val onlineSuccess = true
        return if (onlineSuccess) {
            val onlineResult = StringBuffer()
            onlineResult.append(EMVTag.DEF_TAG_ONLINE_STATUS).append("01").append("00")
            val hostRespCode = "3030"
            onlineResult.append(EMVTag.EMV_TAG_TM_ARC).append("02").append(hostRespCode)
            val onlineApproved = true
            onlineResult.append(EMVTag.DEF_TAG_AUTHORIZE_FLAG).append("01")
                .append(if (onlineApproved) "01" else "00")
            val hostTlvData =
                "9F3501229C01009F3303E0F1C89F02060000000000019F03060000000000009F101307010103A0A802010A010000000052856E2C9B9F2701809F260820F63D6E515BD2CC9505008004E8009F1A0201565F2A0201569F360201C982027C009F34034203009F37045D5F084B9A031710249F1E0835303530343230308408A0000003330101019F090200309F410400000001"
            onlineResult.append(
                TLV.fromData(
                    EMVTag.DEF_TAG_HOST_TLVDATA,
                    BytesUtil.hexString2Bytes(hostTlvData)
                ).toString()
            )
            activity.runOnUiThread {
                Toast.makeText(activity, hostTlvData, Toast.LENGTH_LONG).show()
            }
            onlineResult.toString()
        } else {
            println("!!! online failed !!!")
            "DF9181090101"
        }
    }

    open fun doEndProcess(result: Int, transData: TransData?) {
        if (result != EMVError.SUCCESS) {
            println("=> onEndProcess | " + EMVInfoUtil.getErrorMessage(result))
            if(cardProcessedDataModal.getSuccessResponseCode() == "00"){
                if(this::testCompleteSecondGenAc.isInitialized){
                    testCompleteSecondGenAc.getEndProcessData(result,transData)
                }
                else if(this::secondGenAconNetworkError.isInitialized){
                    secondGenAconNetworkError.getEndProcessData(result,transData)
                }
                else{
                    logger("testCompleteSecondGenAc","uninitialized","e")
                }
            }
            else{
                onEndProcessCallback(result,cardProcessedDataModal)
            }

        }
        else {
            println("=> onEndProcess | EMV_RESULT_NORMAL | " + EMVInfoUtil.getTransDataDesc(transData))
            println(transData?.cvm?.let { EMVInfoUtil.getCVMDesc(it) })
            utilityFunctionForCardDataSetting(cardProcessedDataModal, emv!!)
            if (transData != null) {
                when(transData.acType.toInt()){
                    // Transaction declined offline
                    ACType.EMV_ACTION_AAC->{
                        println("ACType.EMV_ACTION_AAC")
                        emv?.stopProcess()
                        (activity as BaseActivityNew).hideProgress()
                        if(cardProcessedDataModal.txnResponseMsg.isNullOrBlank())
                            activity.txnDeclinedDialog()
                        else
                            activity.txnDeclinedDialog(msg = cardProcessedDataModal.txnResponseMsg!!)
                    }
                    // Transaction go online form authorization
                    ACType.EMV_ACTION_ARQC->{
                        println("ACType.EMV_ACTION_ARQC")
                        doFlowTypeAction(transData.flowType,result, transData)
                    }
                    // Transaction approved by the terminal itself
                    ACType.EMV_ACTION_TC->{
                        println("ACType.EMV_ACTION_TC")
                        if(this::testCompleteSecondGenAc.isInitialized){
                            doFlowTypeAction(transData.flowType,result, transData)
                        }else{
                            emv?.stopProcess()
                            activity.txnDeclinedDialog()
                            println("testCompleteSecondGenAc is null , EMV declined --> Terminal declined the txn")
                        }


                    }
                }
            }
        }
    }

    open fun getACTypeDesc(acType: Byte): String? {
        val desc: String
        desc = when (acType) {
            ACType.EMV_ACTION_AAC.toByte() -> "AAC"
            ACType.EMV_ACTION_ARQC.toByte() -> "ARQC"
            ACType.EMV_ACTION_TC.toByte() -> "TC"
            else -> "Unkown type"
        }
        return desc + String.format("[0x%02X]", acType)
    }

    open fun getCVMDesc(cvm: Byte): String? {
        val desc: String
        desc = when (cvm) {
            CVMFlag.EMV_CVMFLAG_NOCVM.toByte() -> "No CVM verification required"
            CVMFlag.EMV_CVMFLAG_OFFLINEPIN.toByte() -> "Offline PIN"
            CVMFlag.EMV_CVMFLAG_ONLINEPIN.toByte() -> "Online PIN"
            CVMFlag.EMV_CVMFLAG_SIGNATURE.toByte() -> "Signature"
            CVMFlag.EMV_CVMFLAG_OLPIN_SIGN.toByte() -> "Online PIN plus signature"
            CVMFlag.EMV_CVMFLAG_CDV.toByte() -> "Consumer Device Verification(qVSDC/qPBOC)"
            CVMFlag.EMV_CVMFLAG_CCV.toByte() -> "Confirmation Code Verified(PayPass)"
            CVMFlag.EMV_CVMFLAG_CERTIFICATE.toByte() -> "Certificate verification"
            CVMFlag.EMV_CVMFLAG_ECASHPIN.toByte() -> "Electronic cash recharge PIN"
            else -> "Unknown type"
        }
        return desc + String.format("[0x%02X]", cvm)
    }


    private fun doFlowTypeAction(cbFlowType: Byte, result: Int, transData: TransData) {
        val desc: String

        var flowType: Byte? = null

        try {
            val tlvcardTypeLabel =    lastCardRecord?.flowType
            flowType = if (tlvcardTypeLabel!=null) {
                lastCardRecord?.flowType
                //Log.d(TAG,"Card Type ->${hexString2String(tlvcardTypeLabel)}")
            }
            else {
                cbFlowType
            }
        } catch (ex: Exception) {
            Log.e(TAG, ex.message ?: "")
        }
        Log.e("getFlow",flowType.toString())
        Log.e("CbgetFlow",cbFlowType.toString())


        when (flowType) {
            FlowType.EMV_FLOWTYPE_EMV.toByte() -> {
                if(this::testCompleteSecondGenAc.isInitialized){
                    testCompleteSecondGenAc.getEndProcessData(result,transData)
                }else{
                    println("doFlowTypeAction  ... testCompleteSecondGenAc is null , EMV declined --> Terminal declined ")
                }

            }
            FlowType.EMV_FLOWTYPE_ECASH.toByte() -> println("ECASH")
            FlowType.EMV_FLOWTYPE_QPBOC.toByte() ->println( "QPBOC")
            FlowType.EMV_FLOWTYPE_PBOC_CTLESS.toByte() -> println("PBOC_CTLESS")
            FlowType.EMV_FLOWTYPE_M_STRIPE.toByte() -> println("M_STRIPE")
            FlowType.EMV_FLOWTYPE_MSD.toByte() ->println( "MSD")
            FlowType.EMV_FLOWTYPE_MSD_LEGACY.toByte() -> println("MSD_LEGACY")
            FlowType.EMV_FLOWTYPE_WAVE2.toByte() -> println("WAVE2")
            FlowType.EMV_FLOWTYPE_A_XP2_MS.toByte(),FlowType.EMV_FLOWTYPE_A_XPM_MS.toByte() -> {

                cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_MSD_POS_ENTRY_CODE.posEntry.toString())
                cardProcessedDataModal.setReadCardType(DetectCardType.CONTACT_LESS_CARD_WITH_MAG_TYPE)

                val applicationsquence = emv!!.getTLV(Integer.toHexString(0x5F34))
                cardProcessedDataModal.setApplicationPanSequenceValue(applicationsquence)

                val tagDF46 = emv!!.getTLV(Integer.toHexString(0xDF46))
                val tagDf46Str = hexString2String(tagDF46)
                val track2data = tagDf46Str.substring(1,tagDf46Str.length-1)
                val field57 =   "35|"+ track2data.replace("D", "=").replace("F", "")
                println("Field 57 data is"+field57)
                val encrptedPan = getEncryptedPanorTrackData(field57,true)
                cardProcessedDataModal.setEncryptedPan(encrptedPan)

                vfEmvHandlerCallback(cardProcessedDataModal)

            }
            FlowType.EMV_FLOWTYPE_A_XP2_EMV.toByte(),FlowType.EMV_FLOWTYPE_QVSDC.toByte() ,FlowType.EMV_FLOWTYPE_M_CHIP.toByte() -> {

                when (transData.cvm) {
                    CVMFlag.EMV_CVMFLAG_NOCVM.toByte() -> {

                        //cardProcessedDataModal.setIsOnline(0)

                        cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_EMV_POS_ENTRY_CODE.posEntry.toString())
                        cardProcessedDataModal.setReadCardType(DetectCardType.CONTACT_LESS_CARD_TYPE)

                        val applicationsquence = emv!!.getTLV(Integer.toHexString(0x5F34))
                        cardProcessedDataModal.setApplicationPanSequenceValue(applicationsquence)


                        val track2data = emv!!.getTLV(Integer.toHexString(0x57))
                        val field57 =   "35|"+ track2data.replace("D", "=").replace("F", "")
                        println("Field 57 data is"+field57)
                        val encrptedPan = getEncryptedPanorTrackData(field57,true)
                        cardProcessedDataModal.setEncryptedPan(encrptedPan)

                        val field55: String = getFields55()
                        println("Field 55 is $field55")

                        cardProcessedDataModal.setField55(field55)
                        vfEmvHandlerCallback(cardProcessedDataModal)

                    }
                    CVMFlag.EMV_CVMFLAG_ONLINEPIN.toByte() -> {

                        val param = Bundle()
                        //optional Pin Block format by default its 0
                        param.putByte(PinpadData.PIN_BLOCK_FORMAT,0)
                        param.putByteArray(PinpadData.PIN_LIMIT, byteArrayOf(0, 4, 5, 6, 7, 8, 9, 10, 11, 12))

                        val listener: OnPinEntryListener = object : OnPinEntryListener.Stub() {
                            override fun onInput(arg0: Int, arg1: Int) {}
                            override fun onConfirm(data: ByteArray, arg1: Boolean) {
                                System.out.println("PinBlock is"+byte2HexStr(data))
                                Log.d("PinBlock", "PinPad hex encrypted data ---> " + hexString2String(BytesUtil.bytes2HexString(data)))
                                respondCVMResult(1.toByte())

                                when (cardProcessedDataModal.getReadCardType()) {
                                    DetectCardType.EMV_CARD_TYPE -> {
                                        if (cardProcessedDataModal.getIsOnline() == 1) {
                                            cardProcessedDataModal.setGeneratePinBlock(hexString2String(BytesUtil.bytes2HexString(data)))
                                            //insert with pin
                                            cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_PIN.posEntry.toString())
                                        } else {
                                            cardProcessedDataModal.setGeneratePinBlock("")
                                            //off line pin
                                            cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_OFFLINE_PIN.posEntry.toString())
                                        }
                                    }
                                    DetectCardType.CONTACT_LESS_CARD_TYPE -> {
                                        if (cardProcessedDataModal.getIsOnline() == 1) {
                                            cardProcessedDataModal.setGeneratePinBlock(hexString2String(BytesUtil.bytes2HexString(data)))
                                            cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_EMV_POS_WITH_PIN.posEntry.toString())

                                            val applicationsquence = emv!!.getTLV(Integer.toHexString(0x5F34))
                                            cardProcessedDataModal.setApplicationPanSequenceValue(applicationsquence)

                                            val track2data = emv!!.getTLV(Integer.toHexString(0x57))
                                            val field57 =   "35|"+ track2data.replace("D", "=").replace("F", "")
                                            println("Field 57 data is"+field57)
                                            val encrptedPan = getEncryptedPanorTrackData(field57,true)
                                            cardProcessedDataModal.setEncryptedPan(encrptedPan)

                                            val field55: String = getFields55()
                                            println("Field 55 is $field55")

                                            cardProcessedDataModal.setField55(field55)
                                            vfEmvHandlerCallback(cardProcessedDataModal)

                                        } else {
                                            cardProcessedDataModal.setGeneratePinBlock("")
                                            //  cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_PIN.posEntry.toString())
                                        }
                                    }
                                    DetectCardType.MAG_CARD_TYPE -> {
                                        //   vfIEMV?.importPin(1, data) // in Magnetic pin will not import
                                        cardProcessedDataModal.setGeneratePinBlock(hexString2String(BytesUtil.bytes2HexString(data)))

                                        if (cardProcessedDataModal.getFallbackType() == EFallbackCode.EMV_fallback.fallBackCode)
                                            cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_FALL_MAGPIN.posEntry.toString())
                                        else
                                            cardProcessedDataModal.setPosEntryMode(PosEntryModeType.POS_ENTRY_SWIPED_NO4DBC_PIN.posEntry.toString())
                                        cardProcessedDataModal.setApplicationPanSequenceValue("00")
                                    }

                                    else -> {
                                    }
                                }


                            }

                            override fun onCancel() {
                                //  respondCVMResult(0.toByte())
                                Log.d("Data", "PinPad onCancel")
                                try {
                                    //  GlobalScope.launch(Dispatchers.Main) {
                                    activity.declinedTransaction()
                                    //    }
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                }
                            }

                            override fun onError(error: Int) {
                                respondCVMResult(2.toByte())
                                Log.d("Data", "PinPad onError, code:$error")
                                try {
                                    GlobalScope.launch(Dispatchers.Main) {
                                        (activity as TransactionActivity).declinedTransaction()
                                    }
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                }
                            }
                        }

                        println("=> onCardHolderVerify | onlinpin")
                        cardProcessedDataModal.setIsOnline(1)

                        // byte2HexStr(lastCardRecord!!.pan)
                        //    println("Pan block is "+byte2HexStr(lastCardRecord!!.pan))
                        //   addPad("374245001751006", "0", 16, true)

                        println(("CardPanNumber data is " + cardProcessedDataModal.getPanNumberData()) ?: "")

                        // Getting Pan number for Cls online Pin here

                        val trackList=emv!!.getTLV(Integer.toHexString(0x57)).split('D', ignoreCase = true)
                        val panNum=trackList[0]
                        cardProcessedDataModal.setPanNumberData(panNum ?: "")

                        //  Utility.hexStr2Byte(addPad("374245001751006", "0", 16, true))
                        param.putByteArray(PinpadData.PAN_BLOCK, Utility.hexStr2Byte(addPad(cardProcessedDataModal.getPanNumberData() ?: "", "0", 16, true)))

                        pinPad!!.startPinEntry(DemoConfig.KEYID_PIN, param, listener)


                    }
                    CVMFlag.EMV_CVMFLAG_SIGNATURE.toByte() -> {

                        //cardProcessedDataModal.setIsOnline(0)

                        cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_EMV_POS_ENTRY_CODE.posEntry.toString())
                        cardProcessedDataModal.setReadCardType(DetectCardType.CONTACT_LESS_CARD_TYPE)

                        val applicationsquence = emv!!.getTLV(Integer.toHexString(0x5F34))
                        cardProcessedDataModal.setApplicationPanSequenceValue(applicationsquence)

                        val track2data = emv!!.getTLV(Integer.toHexString(0x57))
                        val field57 =   "35|"+ track2data.replace("D", "=").replace("F", "")
                        println("Field 57 data is"+field57)
                        val encrptedPan = getEncryptedPanorTrackData(field57,true)
                        cardProcessedDataModal.setEncryptedPan(encrptedPan)

                        val field55: String = getFields55()
                        println("Field 55 is $field55")

                        cardProcessedDataModal.setField55(field55)
                        vfEmvHandlerCallback(cardProcessedDataModal)
                    }

                    else -> "Unknown type"
                }

                //    cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_EMV_POS_ENTRY_CODE.posEntry.toString())
                //   cardProcessedDataModal.setReadCardType(DetectCardType.CONTACT_LESS_CARD_TYPE)



            }

            else -> {
                println("Unkown Flow Type")

            }
        }
    }

    open fun doVerifyOfflinePin(flag: Int, random: ByteArray?, capKey: CAPublicKey?, result: OfflinePinVerifyResult) {
        println("=> onVerifyOfflinePin")
        try {
            // * 内置插卡- 0；内置挥卡 – 6；外置设备接USB - 7；外置设备接COM口 -8
            // * inside insert card - 0；inside swing card – 6；External device is connected to the USB port - 7；External device is connected to the COM port -8
            val icToken = 0
            //Specify the type of "PIN check APDU message" that will be sent to the IC card.Currently only support VCF_DEFAULT.
            val cmdFmt = OfflinePinVerify.VCF_DEFAULT
            val offlinePinVerify = OfflinePinVerify(flag.toByte(), icToken, cmdFmt, random)
            val pinVerifyResult = PinVerifyResult()
            val ret: Boolean = pinPad!!.verifyOfflinePin(offlinePinVerify, getPinPublicKey(capKey), pinVerifyResult)
            if (!ret) {
                println("verifyOfflinePin fail: " + pinPad!!.getLastError())
                //stopEMV()
                return
            }
            val apduRet = pinVerifyResult.apduRet
            val sw1 = pinVerifyResult.sW1
            val sw2 = pinVerifyResult.sW2
            result.setSW(sw1.toInt(), sw2.toInt())
            result.result = apduRet.toInt()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    open fun getPinPublicKey(from: CAPublicKey?): PinPublicKey? {
        if (from == null) {
            return null
        }
        val to = PinPublicKey()
        to.mRid = from.rid
        to.mExp = from.exp
        to.mExpiredDate = from.expDate
        to.mHash = from.hash
        to.mHasHash = from.hashFlag
        to.mIndex = from.index
        to.mMod = from.mod
        return to
    }

    protected open fun respondAID(aid: ByteArray?) {
        try {
            println("Select aid: " + BytesUtil.bytes2HexString(aid))
            val tmAid = TLV.fromData(EMVTag.EMV_TAG_TM_AID, aid)
            println(""+ emv!!.respondEvent(tmAid.toString())+ "...onAppSelect: respondEvent")
        } catch (e: Exception) {
            //handleException(e);
        }
    }

    // CVM rule
    @Throws(RemoteException::class)
    open fun doCardHolderVerify(cvm: CVMMethod) {
        println("=> onCardHolderVerify | " + EMVInfoUtil.getCVMDataDesc(cvm))
        val param = Bundle()
        //optional Pin Block format by default its 0
        param.putByte(PinpadData.PIN_BLOCK_FORMAT,0)
        param.putByteArray(PinpadData.PIN_LIMIT, byteArrayOf( 4, 5, 6, 7, 8, 9, 10, 11, 12))

        val listener: OnPinEntryListener = object : OnPinEntryListener.Stub() {
            override fun onInput(arg0: Int, arg1: Int) {}
            override fun onConfirm(data: ByteArray, arg1: Boolean) {
                println("PinBlock is"+byte2HexStr(data))
                Log.d("PinBlock", "PinPad hex encrypted data ---> " + hexString2String(BytesUtil.bytes2HexString(data)))
                respondCVMResult(1.toByte())

                when (cardProcessedDataModal.getReadCardType()) {
                    DetectCardType.EMV_CARD_TYPE -> {
                        if (cardProcessedDataModal.getIsOnline() == 1) {
                            cardProcessedDataModal.setGeneratePinBlock(hexString2String(BytesUtil.bytes2HexString(data)))
                            //insert with pin
                            cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_PIN.posEntry.toString())
                        } else {
                            cardProcessedDataModal.setGeneratePinBlock("")
                            //off line pin
                            cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_OFFLINE_PIN.posEntry.toString())
                        }
                    }
                    DetectCardType.CONTACT_LESS_CARD_TYPE -> {
                        if (cardProcessedDataModal.getIsOnline() == 1) {
                            cardProcessedDataModal.setGeneratePinBlock(hexString2String(BytesUtil.bytes2HexString(data)))
                            cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_EMV_POS_WITH_PIN.posEntry.toString())
                        } else {
                            cardProcessedDataModal.setGeneratePinBlock("")
                            //  cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_PIN.posEntry.toString())
                        }
                    }
                    DetectCardType.MAG_CARD_TYPE -> {
                        //   vfIEMV?.importPin(1, data) // in Magnetic pin will not import
                        cardProcessedDataModal.setGeneratePinBlock(hexString2String(BytesUtil.bytes2HexString(data)))

                        if (cardProcessedDataModal.getFallbackType() == EFallbackCode.EMV_fallback.fallBackCode)
                            cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_FALL_MAGPIN.posEntry.toString())
                        else
                            cardProcessedDataModal.setPosEntryMode(PosEntryModeType.POS_ENTRY_SWIPED_NO4DBC_PIN.posEntry.toString())
                        cardProcessedDataModal.setApplicationPanSequenceValue("00")
                    }

                    else -> {
                    }
                }


            }

            override fun onCancel() {
                //  respondCVMResult(0.toByte())
                Log.d("Data", "PinPad onCancel")
                try {
                    //  GlobalScope.launch(Dispatchers.Main) {
                    activity.declinedTransaction()
                    //    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }

            override fun onError(error: Int) {
                respondCVMResult(2.toByte())
                Log.d("Data", "PinPad onError, code:$error")
                try {
                    GlobalScope.launch(Dispatchers.Main) {
                        (activity as TransactionActivity).declinedTransaction()
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
        when (cvm.cvm) {


            CVMFlag.EMV_CVMFLAG_OFFLINEPIN.toByte() ->  {
                cardProcessedDataModal.setIsOnline(2)
                println("ORIGINAL PIN LIMIT ---> ${cvm.pinTimes.toInt()}")
                // hexString2String(emv!!.getTLV(Integer.toHexString(0x9F17).toUpperCase(Locale.ROOT))  )
                if(pinTryCounter == -1)
                    pinTryCounter = cvm.pinTimes.toInt()//emv!!.getTLV(Integer.toHexString(0x9F17).toUpperCase(Locale.ROOT)).toInt() ?: 0
                // val pinTryLimit = BytesUtil.hexString2Bytes(pinTryCounter)

                if(pinTryCounter>3){
                    pinTryCounter=3
                }
                println("Pin Try limit is "+pinTryCounter)
                pinTryRemainingTimes = pinTryCounter
                println("Pin Try Remaining is "+pinTryRemainingTimes)

                if (pinTryRemainingTimes >= 3) {
                    pinTryRemainingTimes = 3
                    maxPin = 3
                } else {
                    pinTryRemainingTimes = pinTryCounter
                }
                if (maxPin == 3) {
                    println("Going in first")
                    when (pinTryRemainingTimes) {
                        3 -> {
                            pinPad!!.startOfflinePinEntry(param, listener).also {
                                pinTryCounter--
                                pinTryRemainingTimes--
                                println("Pin Try Remaining is1 " + pinTryRemainingTimes)
                            }
                        }
                        2 -> {
                            GlobalScope.launch(Dispatchers.Main) {
                                (activity as? BaseActivityNew)?.alertBoxWithActionNew(
                                    "Invalid PIN",
                                    "Wrong PIN.Please try again",
                                    R.drawable.ic_txn_declined,
                                    (activity as? BaseActivityNew)!!.getString(R.string.positive_button_ok),
                                    "", false, true, { alertPositiveCallback ->
                                        if (alertPositiveCallback) {
                                            pinPad!!.startOfflinePinEntry(param, listener).also {
                                                pinTryCounter--
                                                pinTryRemainingTimes--
                                                println("Pin Try Remaining is1 " + pinTryRemainingTimes)
                                            }

                                        }
                                    },
                                    {})
                            }

                        }
                        1 -> {
                            GlobalScope.launch(Dispatchers.Main) {
                                (activity as? BaseActivityNew)?.alertBoxWithActionNew(
                                    "Invalid PIN",
                                    "Wrong PIN.This is your last attempt",
                                    R.drawable.ic_txn_declined,
                                    (activity as? BaseActivityNew)!!.getString(R.string.positive_button_ok),
                                    "", false, true, { alertPositiveCallback ->
                                        if (alertPositiveCallback) {
                                            pinPad!!.startOfflinePinEntry(param, listener).also {
                                                pinTryCounter--
                                                pinTryRemainingTimes--
                                                println("Pin Try Remaining is1 " + pinTryRemainingTimes)
                                            }

                                        }
                                    },
                                    {})
                            }

                        }
                        else -> {
                            activity.txnDeclinedDialog("Wrong Pin Declined")
                            println("Going in first --> ELSE")
                        }
                    }
                }
                else if(pinTryRemainingTimes == 2){
                    println("Going in second")
                    when (pinTryRemainingTimes) {
                        2 -> {
                            pinPad!!.startOfflinePinEntry(param, listener).also {
                                pinTryCounter--
                                pinTryRemainingTimes--
                                println("Pin Try Remaining is1 $pinTryRemainingTimes")
                            }
                        }
                        1 -> {
                            GlobalScope.launch(Dispatchers.Main) {
                                (activity as? BaseActivityNew)?.alertBoxWithActionNew(
                                    "Invalid PIN",
                                    "This is your last attempt",
                                    R.drawable.ic_txn_declined,
                                    (activity as? BaseActivityNew)!!.getString(R.string.positive_button_ok),
                                    "", false, true, { alertPositiveCallback ->
                                        if (alertPositiveCallback) {
                                            pinPad!!.startOfflinePinEntry(param, listener).also {
                                                pinTryCounter--
                                                pinTryRemainingTimes--
                                                println("Pin Try Remaining is1 " + pinTryRemainingTimes)
                                            }

                                        }
                                    },
                                    {})
                            }

                        }
                        else -> {
                            activity.txnDeclinedDialog("Wrong Pin Declined")
                            println("Going in second------> Else")
                        }
                    }

                }
                else {
                    println("Going in else")
                    GlobalScope.launch(Dispatchers.Main) {
                        (activity as? BaseActivityNew)?.alertBoxWithActionNew(
                            "Invalid PIN",
                            "This is your last PIN attempt",
                            R.drawable.ic_txn_declined,
                            (activity as? BaseActivityNew)!!.getString(R.string.positive_button_ok),
                            "", false, true, { alertPositiveCallback ->
                                if (alertPositiveCallback) {
                                    pinPad!!.startOfflinePinEntry(param, listener).also {
                                        pinTryCounter--
                                        pinTryRemainingTimes--
                                        println("Pin Try Remaining is1 " + pinTryRemainingTimes)
                                    }

                                }
                            },
                            {})
                    }
                }

            }


            CVMFlag.EMV_CVMFLAG_ONLINEPIN.toByte() -> {
                println("=> onCardHolderVerify | onlinpin")
                cardProcessedDataModal.setIsOnline(1)


                byte2HexStr(lastCardRecord!!.pan)
                println("Pan block is "+byte2HexStr(lastCardRecord!!.pan))
                //  addPad("374245001751006", "0", 16, true)

                println("CardPanNumber data is "+cardProcessedDataModal.getPanNumberData() ?: "")

                Utility.hexStr2Byte(addPad("374245001751006", "0", 16, true))
                param.putByteArray(PinpadData.PAN_BLOCK,
                    Utility.hexStr2Byte(
                        addPad(
                            cardProcessedDataModal.getPanNumberData() ?: "",
                            "0",
                            16,
                            true
                        )
                    )
                )
                pinPad!!.startPinEntry(DemoConfig.KEYID_PIN, param, listener)
            }
            else -> {
                println("=> onCardHolderVerify | default")
                respondCVMResult(1.toByte())
            }
        }
    }



    protected open fun respondCVMResult(result: Byte) {
        try {
            val chvStatus = TLV.fromData(EMVTag.DEF_TAG_CHV_STATUS, byteArrayOf(result))
            val ret = emv!!.respondEvent(chvStatus.toString())
            println("...onCardHolderVerify: respondEvent$ret")
        } catch (e: Exception) {
            //handleException(e);
        }
    }

    // 5 calling
    @Throws(RemoteException::class)
    open fun doReadRecord(record: CardRecord?) {
        println("=> onReadRecord | " + BytesUtil.bytes2HexString(record?.pan))
        var track22: String? = null
        val track2 = BytesUtil.bytes2HexString(record?.pan)

        var a = track2.indexOf('F')
        if (a > 0) {
            track22 = track2.substring(0, a)
        } else {
            a = track2.indexOf('=')

            track22 = if (a > 0) {
                track2.substring(0, a)
            }else{
                track2
            }
        }

        cardProcessedDataModal.setPanNumberData(track22 ?: "")
        System.out.println("Card pannumber data "+cardProcessedDataModal.getPanNumberData())


        if(((emv?.getTLV(Integer.toHexString(0x84))?:"").take(10))!=CardAid.AMEX.aid){
            val encrptedPan = getEncryptedPanorTrackData(EMVInfoUtil.getRecordDataDesc(record),false)
            cardProcessedDataModal.setEncryptedPan(encrptedPan)
        }


        cardProcessedDataModal.setFlowType(record?.flowType.toString() ?: "")
        println("Flow Type ---> " + record?.flowType.toString())


        val tvDynamicLimit = emv!!.getTLV(Integer.toHexString(0x9F70))  // card Type TAG
        if (null != tvDynamicLimit && !(tvDynamicLimit.isEmpty())) {
            println("Dynamic Limit  Type ---> " + tvDynamicLimit)
            //cardProcessedDataModal.setcardLabel(hexString2String(tvDynamicLimit))
        }

        record?.pan?.byteArr2HexStr()
        // settingCAPkeys(emv)
        utilityFunctionForCardDataSetting(cardProcessedDataModal, emv!!)

        println("...onReadRecord: respondEvent" + emv!!.respondEvent(null))
    }


    // 4 calling
    open fun doSendOut(ins: Int, data: ByteArray) {
        when (ins) {
            KernelINS.DISPLAY ->            // DisplayMsg: MsgID（1 byte） + Currency（1 byte）+ DataLen（1 byte） + Data（30 bytes）
                if (data[0] == MessageID.ICC_ACCOUNT.toByte() ) {
                    val len: Byte = data[2]
                    val account = BytesUtil.subBytes(data, 1 + 1 + 1, len.toInt())
                    val accTLVList = TLVList.fromBinary(account)
                    val track2 = BytesUtil.bytes2HexString(accTLVList.getTLV("57").bytesValue)
                    println("Field 57 data is1"+track2)
                    var field57 =   "35|"+track2.replace("D", "=")?.replace("F", "")
                    println("Field 57 data is"+field57)
                    val encrptedPan = getEncryptedPanorTrackData(field57,true)
                    cardProcessedDataModal.setEncryptedPan(encrptedPan)
                    println("=> onSendOut | track2 = $field57")


                }
            KernelINS.DBLOG -> {
                var i = data.size - 1
                while (i >= 0) {
                    if (data[i].toInt() == 0x00) {
                        data[i] = 0x20
                    }
                    i--
                }
                Log.d("DBLOG", String(data))
            }
            KernelINS.CLOSE_RF -> {
                println("=> onSendOut: Notify the application to halt contactless module")
                emv!!.halt()
            }
            else -> println(
                "=> onSendOut: instruction is 0x" + Integer.toHexString(ins) + ", data is " + BytesUtil.bytes2HexString(data)
            )
        }
    }

    fun SecondGenAcOnNetworkError(secondGenAconNetworkError: SecondGenAcOnNetworkError, cardProcessedDataModal: CardProcessedDataModal?){
        this.secondGenAconNetworkError = secondGenAconNetworkError
        logger("vfemvHan",""+this,"e")
        if (cardProcessedDataModal != null) {
            this.cardProcessedDataModal  = cardProcessedDataModal
        }
    }
    fun getCompleteSecondGenAc(testCompleteSecondGenAc: CompleteSecondGenAc, cardProcessedDataModal: CardProcessedDataModal?){
        this.testCompleteSecondGenAc = testCompleteSecondGenAc
        logger("vfemvHan",""+this,"e")
        if (cardProcessedDataModal != null) {
            this.cardProcessedDataModal  = cardProcessedDataModal
        }
    }

}






