package com.bonushub.crdb.india.transactionprocess

import android.os.DeadObjectException
import android.os.RemoteException
import android.text.TextUtils
import android.util.Log
import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.model.local.AppPreference
import com.bonushub.crdb.india.model.local.AppPreference.GENERIC_REVERSAL_KEY
import com.bonushub.crdb.india.model.local.AppPreference.clearReversal
import com.bonushub.crdb.india.serverApi.HitServer
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.view.baseemv.EmvHandler
import com.bonushub.crdb.india.vxutils.BhTransactionType
import com.bonushub.crdb.india.vxutils.Mti
import com.google.gson.Gson
import com.usdk.apiservice.aidl.emv.UEMV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class SyncTransactionToHost(var transactionISOByteArray: IsoDataWriter?,
                            var cardProcessedDataModal: CardProcessedDataModal? = null,
                            var testEmvHandler:EmvHandler?,
                            var syncTransactionCallback: (Boolean, String, String?, Triple<String, String, String>?,String?,String?) -> Unit) {

    val iemv: UEMV? = DeviceHelper.getEMV()
    private var successResponseCode: String? = null
    private var secondTap: String? = null

    init {
        GlobalScope.launch(Dispatchers.IO) {
            sendTransactionPacketToHost(transactionISOByteArray)
        }
    }

    //Below method is used to sync Transaction Packet Data to host:-
    private suspend fun sendTransactionPacketToHost(transactionISOData: IsoDataWriter?) {
        when (cardProcessedDataModal?.getTransType()) {
            BhTransactionType.PRE_AUTH.type -> {
                transactionISOData?.mti = Mti.PRE_AUTH_MTI.mti
            }
            else -> {
                transactionISOData?.mti = Mti.DEFAULT_MTI.mti
            }
        }
        //Setting ROC again because if reversal send first and then transaction packet goes to host the ROC is similar in that case because we are creating Trans packet at initial stage
        transactionISOData?.addField(11,Utility().getROC() ?: "")
        val transactionISOByteArray = transactionISOData?.generateIsoByteRequest()
        if (transactionISOData != null) {
            logger("Transaction REQUEST PACKET --->>", transactionISOData.isoMap, "e")
        }
        if (cardProcessedDataModal?.getReadCardType() != DetectCardType.EMV_CARD_TYPE) {
            val reversalPacket = Gson().toJson(transactionISOData)
            AppPreference.saveString(GENERIC_REVERSAL_KEY, reversalPacket)
            transactionISOByteArray?.byteArr2HexStr()?.let { logger("PACKET-->", it) }
        }

        if (transactionISOByteArray != null) {
            HitServer.hitServersale(transactionISOByteArray, { result, success, readtimeout ->
                //Save Server Hit Status in Preference , To Restrict Init and KeyExchange from Terminal:-
                AppPreference.saveBoolean(PrefConstant.SERVER_HIT_STATUS.keyName.toString(), true)
                try {
                    //println("Result is$success")
                    if (success) {
                        //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                    //    ROCProviderV2.incrementFromResponse(ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(), AppPreference.getBankCode())
                        Utility().incrementRoc()
                        Log.d("Success Data:- ", result)
                        //if(!result.isNullOrBlank())
                        if (!TextUtils.isEmpty(result)) {
                            val value = readtimeout.toIntOrNull()
                            if (null != value) {
                                when (value) {
                                    500 -> {
                                        ConnectionError.ReadTimeout.errorCode
                                        when (cardProcessedDataModal?.getReadCardType()) {
                                            DetectCardType.MAG_CARD_TYPE, DetectCardType.CONTACT_LESS_CARD_TYPE,
                                            DetectCardType.CONTACT_LESS_CARD_WITH_MAG_TYPE ,
                                            DetectCardType.EMV_CARD_TYPE -> {
                                                val reversalPacket = Gson().toJson(transactionISOData)
                                                AppPreference.saveString(GENERIC_REVERSAL_KEY, reversalPacket)
                                            }

                                            else -> {
                                            }
                                        }
                                        syncTransactionCallback(false, "", result, null,null,null)
                                    }
                                }
                            }
                            else {

                                try {
                                    val responseIsoData: IsoDataReader = readIso(result, false)
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                       syncTransactionCallback(false, "", result, null, null,null)
                                }

                                //   println("Number format problem")
                                val responseIsoData: IsoDataReader = readIso(result.toString(), false)
                                logger("Transaction RESPONSE ", "---", "e")
                                logger("Transaction RESPONSE --->>", responseIsoData.isoMap, "e")
                                Log.e(
                                        "Success 39-->  ", responseIsoData.isoMap[39]?.parseRaw2String()
                                        .toString() + "---->" + responseIsoData.isoMap[58]?.parseRaw2String()
                                        .toString()
                                )
                                successResponseCode = (responseIsoData.isoMap[39]?.parseRaw2String().toString())
                                val authCode = (responseIsoData.isoMap[38]?.parseRaw2String().toString())
                                cardProcessedDataModal?.setAuthCode(authCode.trim())
                                //Here we are getting RRN Number :-
                                val rrnNumber = responseIsoData.isoMap[37]?.rawData ?: ""
                                cardProcessedDataModal?.setRetrievalReferenceNumber(rrnNumber)

                                val acqRefereal = responseIsoData.isoMap[31]?.parseRaw2String().toString()
                                cardProcessedDataModal?.setAcqReferalNumber(acqRefereal)
                                logger(
                                        "ACQREFERAL",
                                        cardProcessedDataModal?.getAcqReferalNumber().toString(),
                                        "e"
                                )

                               val encrptedPan = responseIsoData.isoMap[57]?.parseRaw2String().toString()
                              //  cardProcessedDataModal?.setEncryptedPan(encrptedPan)

                                val f55 = responseIsoData.isoMap[55]?.rawData


                                if (successResponseCode == "00") {
                                    cardProcessedDataModal?.setSucessResponseCode(successResponseCode)

                                    AppPreference.saveBoolean(AppPreference.ONLINE_EMV_DECLINED, false)

                                    when (cardProcessedDataModal?.getReadCardType()) {

                                        DetectCardType.MAG_CARD_TYPE, DetectCardType.CONTACT_LESS_CARD_TYPE,
                                        DetectCardType.CONTACT_LESS_CARD_WITH_MAG_TYPE,
                                        DetectCardType.MANUAL_ENTRY_TYPE -> {

                                             clearReversal()
                                            syncTransactionCallback(true, successResponseCode.toString(), result, null,null,secondTap)

                                        }
                                        DetectCardType.EMV_CARD_TYPE -> {
                                            if (TextUtils.isEmpty(AppPreference.getString(GENERIC_REVERSAL_KEY))) {

                                                if (cardProcessedDataModal?.getTransType() != BhTransactionType.REFUND.type &&
                                                    cardProcessedDataModal?.getTransType() != BhTransactionType.SALE_WITH_CASH.type/*&&
                                                    && cardProcessedDataModal?.getTransType() != TransactionType.EMI_SALE.type &&
                                                        cardProcessedDataModal?.getTransType() != TransactionType.BRAND_EMI.type
                                                        cardProcessedDataModal?.getTransType() != TransactionType.BRAND_EMI_BY_ACCESS_CODE.type &&
                                                        cardProcessedDataModal?.getTransType() != TransactionType.SALE.type &&
                                                        cardProcessedDataModal?.getTransType() != TransactionType.PRE_AUTH.type &&
                                                        cardProcessedDataModal?.getTransType() != TransactionType.SALE_WITH_CASH.type &&
                                                        cardProcessedDataModal?.getTransType() != TransactionType.CASH_AT_POS.type &&
                                                        cardProcessedDataModal?.getTransType() != TransactionType.TEST_EMI.type*/) {

                                                    logger("CompleteSecondGenAc","yes")
                                                    logger("3testVFEmvHandler",""+testEmvHandler,"e")
                                                    testEmvHandler?.let {
                                                        CompleteSecondGenAc(cardProcessedDataModal, responseIsoData, transactionISOData,
                                                            it
                                                        ) { printExtraData, de55 ->
                                                            syncTransactionCallback(true, successResponseCode.toString(), result, printExtraData, de55, null)
                                                        }.performSecondGenAc(cardProcessedDataModal, responseIsoData)
                                                    }

                                                }
                                                else {
                                                       clearReversal()
                                                       logger("CompleteSecondGenAc","no")
                                                    syncTransactionCallback(true, successResponseCode.toString(), result, null, null, null)

                                                }


                                            } else {
                                                clearReversal()
                                                syncTransactionCallback(true, successResponseCode.toString(), result, null, null, null)
                                            }
                                        }

                                        else -> logger("CARD_ERROR:- ", cardProcessedDataModal?.getReadCardType().toString(), "e")
                                    }
                                    //remove emi case

                                }
                                else {
                                    //here 2nd Gen Ac in case of Failure
                                    //here reversal will also be clear there
                                    when (cardProcessedDataModal?.getReadCardType()) {
                                        DetectCardType.MAG_CARD_TYPE, DetectCardType.CONTACT_LESS_CARD_TYPE,
                                        DetectCardType.CONTACT_LESS_CARD_WITH_MAG_TYPE,
                                        DetectCardType.MANUAL_ENTRY_TYPE -> {
                                            clearReversal()
                                            syncTransactionCallback(true, successResponseCode.toString(), result, null, null, secondTap)
                                        }
                                        DetectCardType.EMV_CARD_TYPE -> {
                                            clearReversal()
                                            if (cardProcessedDataModal?.getTransType() != BhTransactionType.REFUND.type
                                             /*   && cardProcessedDataModal?.getTransType() != TransactionType.EMI_SALE.type &&
                                                    cardProcessedDataModal?.getTransType() != TransactionType.BRAND_EMI.type &&
                                                    cardProcessedDataModal?.getTransType() != TransactionType.BRAND_EMI_BY_ACCESS_CODE.type &&
                                                    cardProcessedDataModal?.getTransType() != TransactionType.TEST_EMI.type &&
                                                    cardProcessedDataModal?.getTransType() != TransactionType.SALE.type*/) {
                                                logger("blank if block","true","e")

                                               /* testEmvHandler?.let {
                                                    CompleteSecondGenAc(cardProcessedDataModal, responseIsoData, transactionISOData,
                                                        it
                                                    ) { printExtraData, de55 ->
                                                        syncTransactionCallback(true, successResponseCode.toString(), result, null, null, null)
                                                    }.performSecondGenOnFail(cardProcessedDataModal)
                                                }*/

                                                testEmvHandler?.let {
                                                    CompleteSecondGenAc(cardProcessedDataModal, responseIsoData, transactionISOData, it) { printExtraData, de55 ->
                                                        syncTransactionCallback(true, successResponseCode.toString(), result, printExtraData, de55, null)
                                                    }.performSecondGenAc(cardProcessedDataModal, responseIsoData)
                                                }


                                             //   syncTransactionCallback(true, successResponseCode.toString(), result, null, null, null)


                                            } else {
                                                syncTransactionCallback(true, successResponseCode.toString(), result, null, null, null)
                                            }

                                        }
                                        else -> logger("CARD_ERROR:- ", cardProcessedDataModal?.getReadCardType().toString(), "e")
                                    }

                                }
                            }
                        } else {
                            syncTransactionCallback(false, "", "", null, null, null)
                        }

                    } else {
                        val value = readtimeout.toIntOrNull()
                        if (null != value) {
                            when (value) {
                                ConnectionError.NetworkError.errorCode -> {
                                    AppPreference.saveBoolean(PrefConstant.SERVER_HIT_STATUS.keyName.toString(), false)
                                   value
                                    //Clear reversal
                                    clearReversal()
                                    //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                                    //ROCProviderV2.incrementFromResponse(ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(), AppPreference.getBankCode())
                                    Utility().incrementRoc()

                                    testEmvHandler?.let {
                                        SecondGenAcOnNetworkError(result.toString(), cardProcessedDataModal, it) { secondGenAcErrorStatus ->
                                            if (secondGenAcErrorStatus) {
                                                syncTransactionCallback(false, successResponseCode.toString(), result, null, null, null)
                                            } else {
                                                syncTransactionCallback(false, ConnectionError.NetworkError.errorCode.toString(), result, null, null, null)
                                            }
                                        }.generateSecondGenAcForNetworkErrorCase(result.toString())

                                    }

                                    testEmvHandler?.let {
                                        CompleteSecondGenAc(cardProcessedDataModal, null, transactionISOData, it) { printExtraData, de55 ->
                                            syncTransactionCallback(true, successResponseCode.toString(), result, printExtraData, de55, null)
                                        }.performSecondGenAc(cardProcessedDataModal, IsoDataReader())
                                    }

                                }

                                else -> {

                                    //Clear reversal
                                    clearReversal()
                                    //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                                    //ROCProviderV2.incrementFromResponse(ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(), AppPreference.getBankCode())
                                    Utility().incrementRoc()
                                    syncTransactionCallback(false, ConnectionError.ConnectionTimeout.errorCode.toString(), result, null, null, null)
                                    Log.d("Failure Data:- ", result)
                                }
                            }
                        } else {
                            //Clear reversal
                            clearReversal()
                            //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                            //ROCProviderV2.incrementFromResponse(ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(), AppPreference.getBankCode())
                            Utility().incrementRoc()
                            syncTransactionCallback(false, ConnectionError.ConnectionTimeout.errorCode.toString(), result, null, null, null)
                            Log.d("Failure Data:- ", result)
                        }
                    }
                } catch (ex: DeadObjectException) {
                    // throw RuntimeException(ex)
                    ex.printStackTrace()
                } catch (ex: RemoteException) {
                    //  throw RuntimeException(ex)
                    ex.printStackTrace()
                } catch (ex: Exception) {
                    //throw RuntimeException(ex)
                    ex.printStackTrace()
                }

            }, {
                //backToCalled(it, false, true)
            })
        }
    }
  }
