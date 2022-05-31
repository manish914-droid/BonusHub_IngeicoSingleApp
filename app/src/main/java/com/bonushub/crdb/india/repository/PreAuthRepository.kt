package com.bonushub.crdb.india.repository

import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.model.local.AppPreference
import com.bonushub.crdb.india.serverApi.HitServer
import com.bonushub.crdb.india.transactionprocess.CreateAuthPacket
import com.bonushub.crdb.india.transactionprocess.StubBatchData
import com.bonushub.crdb.india.transactionprocess.SyncReversalToHost
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.view.fragments.AuthCompletionData
import com.bonushub.crdb.india.view.fragments.pre_auth.CompletePreAuthData
import com.bonushub.crdb.india.view.fragments.pre_auth.PendingPreAuthDataResponse
import com.bonushub.crdb.india.view.fragments.pre_auth.PendingPreauthData
import com.bonushub.crdb.india.vxutils.Mti
import com.bonushub.crdb.india.vxutils.ProcessingCode
import com.bonushub.crdb.india.vxutils.TransactionType
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

class PreAuthRepository @Inject constructor() {


    var counter = 0

    init {
        counter = 0
    }


    private val dataLocal = MutableLiveData<PendingPreAuthDataResponse>()
    val data: LiveData<PendingPreAuthDataResponse>
        get() = dataLocal


    private val cardProcessedData: CardProcessedDataModal by lazy { CardProcessedDataModal() }
    val pendingPreauthList = ArrayList<PendingPreauthData>()

    suspend fun getPendingPreAuthTxn(){

        doPendingPreAuth(counter)
    }

    private fun doPendingPreAuth(counter: Int) {
        val transactionalAmount = 0L
        cardProcessedData.apply {
            setTransactionAmount(transactionalAmount)
            setTransType(TransactionType.PENDING_PREAUTH.type)
            setProcessingCode(ProcessingCode.PENDING_PREAUTH.code)
        }
        val transactionISO =
            CreateAuthPacket().createPendingPreAuthISOPacket(cardProcessedData, counter)
        //Here we are Saving Date , Time and TimeStamp in CardProcessedDataModal:-
        try {
            val date2: Long = Calendar.getInstance().timeInMillis
            val timeFormater = SimpleDateFormat("HHmmss", Locale.getDefault())
            cardProcessedData.setTime(timeFormater.format(date2))
            val dateFormater = SimpleDateFormat("MMdd", Locale.getDefault())
            cardProcessedData.setDate(dateFormater.format(date2))
            cardProcessedData.setTimeStamp(date2.toString())
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        logger("Transaction REQUEST PACKET --->>", transactionISO.isoMap, "e")

        runBlocking(Dispatchers.IO) {
            checkReversalPerformPendingPreAuthTransaction(
                transactionISO,
                cardProcessedData
            )
        }
    }

    private suspend fun checkReversalPerformPendingPreAuthTransaction(
        transactionISOByteArray: IsoDataWriter,
        cardProcessedDataModal: CardProcessedDataModal
    ) {

        //Sending Transaction Data Packet to Host:-(In case of no reversal)
        if (TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {

        val temp = PendingPreAuthDataResponse()
            temp.msg = "Getting Pending Pre-Auth From Server"
            logger("ApiStatus","Processing","e")
            temp.apiStatus = ApiStatus.Processing
            dataLocal.postValue(temp)



            syncPreAuthTransactionToHost(
                transactionISOByteArray,
                cardProcessedDataModal,
                false
            ) { syncStatus, responseCode, transactionMsg ->


                if (syncStatus && responseCode == "00") {
                      AppPreference.clearReversal()
                    val resIso = readIso(transactionMsg, false)
                    logger("RESP DATA..>", transactionMsg)
                    logger("PendingPre RES -->", resIso.isoMap, "e")
                    val autoSettlementCheck =
                        resIso.isoMap[60]?.parseRaw2String().toString()
                    val f62 = resIso.isoMap[62]?.parseRaw2String() ?: ""
                    val f62Arr = f62.split("|")
                    if (f62Arr.size >= 2) {
                        for (e in 2..(f62Arr.lastIndex)) {
                            if (f62Arr[e].isNotEmpty()) {
                                val ip = PendingPreauthData()
                                ip.pendingPreauthDataParser(f62Arr[e])
                                pendingPreauthList.add(ip)
                            }
                        }
                        if (f62Arr[0] == "1") {
                            counter += f62Arr[1].toInt()
                            doPendingPreAuth(counter)

                        } else {

                            logger("Pending Preauth", "Finish")
                            //--

                            try {
                                    pendingPreauthList.sortBy { it.bankId }
                                    logger("ApiStatus","Success","e")
                                    val temp = PendingPreAuthDataResponse()
                                    temp.apiStatus = ApiStatus.Success
                                    temp.cardProcessedDataModal = cardProcessedDataModal
                                    temp.pendingList = pendingPreauthList
                                    dataLocal.postValue(temp)

                            }catch (ex:Exception){
                                ex.printStackTrace()
                            }
                        }
                    } else {

                        logger("ApiStatus","Success","e")
                            var temp = PendingPreAuthDataResponse()
                            temp.apiStatus = ApiStatus.Success
                            dataLocal.postValue(temp)

                    }

                } else if (syncStatus && responseCode != "00") {
                     AppPreference.clearReversal()
                    val resIso = readIso(transactionMsg, false)
                    logger("RESP DATA..>", transactionMsg)
                    logger("PendingPre RES -->", resIso.isoMap, "e")
                    val autoSettlementCheck =
                        resIso.isoMap[60]?.parseRaw2String().toString()
                    //---

                    logger("ApiStatus","Failed","e")
                        var temp = PendingPreAuthDataResponse()
                    temp.msg = resIso.isoMap[58]?.parseRaw2String().toString()
                    temp.apiStatus = ApiStatus.Failed
                        dataLocal.postValue(temp)
                } else {
                       AppPreference.clearReversal()
                    logger("ApiStatus","Failed","e")
                        var temp = PendingPreAuthDataResponse()
                        temp.msg = "Connection Failed"
                        temp.apiStatus = ApiStatus.Failed
                        dataLocal.postValue(temp)

                }
            }
        }
        //Sending Reversal Data Packet to Host:-(In Case of reversal)
        else {
            if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {

                logger("ApiStatus","Processing","e")
                    var temp = PendingPreAuthDataResponse()
                temp.msg = "Uploading reversal"
                temp.apiStatus = ApiStatus.Processing
                     dataLocal.postValue(temp)


                SyncReversalToHost(AppPreference.getReversalNew()) { isSyncToHost, transMsg ->
                    if (isSyncToHost) {
                        AppPreference.clearReversal()
                        runBlocking(Dispatchers.IO) {
                            checkReversalPerformPendingPreAuthTransaction(
                                transactionISOByteArray,
                                cardProcessedDataModal
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun syncPreAuthTransactionToHost(
        transISODataWriter: IsoDataWriter?,
        cardProcessedDataModal: CardProcessedDataModal?,
        isReversal: Boolean,
        syncAuthTransactionCallback: (Boolean, String, String) -> Unit
    ) {
        var successResponseCode: String? = null

        if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
            transISODataWriter?.mti = Mti.REVERSAL.mti
            transISODataWriter?.additionalData?.get("F56reversal")?.let {
                transISODataWriter.addFieldByHex(
                    56,
                    it
                )
            }
            if (transISODataWriter != null) {
                addIsoDateTime(transISODataWriter)
            }
        } else {
            transISODataWriter?.mti = Mti.PRE_AUTH_MTI.mti
        }

        val transactionISOByteArray = transISODataWriter?.generateIsoByteRequest()
        //  val reversalPacket = Gson().toJson(transISODataWriter)
        // AppPreference.saveString(AppPreference.GENERIC_REVERSAL_KEY, reversalPacket)
        if (transactionISOByteArray != null) {
            Log.e("callback","before hit server")
            HitServer.hitServer(transactionISOByteArray, { result, success ->
                //Save Server Hit Status in Preference:-
                AppPreference.saveBoolean(PrefConstant.SERVER_HIT_STATUS.keyName.toString(), true)
                try {
                    if (success) {
                        //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                        /*ROCProviderV2.incrementFromResponse(
                        ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                        AppPreference.getBankCode()
                    )*/
                        Log.d("Success Data:- ", result)
                        val responseIsoData: IsoDataReader = readIso(result, false)
                        logger("Transaction RESPONSE --->>", responseIsoData.isoMap, "e")
                        Log.e(
                            "Success 39-->  ",
                            responseIsoData.isoMap[39]?.parseRaw2String()
                                .toString() + "---->" + responseIsoData.isoMap[58]?.parseRaw2String()
                                .toString()
                        )
                        successResponseCode =
                            (responseIsoData.isoMap[39]?.parseRaw2String().toString())

                        val authCode = (responseIsoData.isoMap[38]?.parseRaw2String().toString())
                        cardProcessedDataModal?.setAuthCode(authCode.trim())
                        //Here we are getting RRN Number :-
                        val rrnNumber = responseIsoData.isoMap[37]?.rawData ?: ""
                        cardProcessedDataModal?.setRetrievalReferenceNumber(rrnNumber)
                        val encrptedPan = responseIsoData.isoMap[57]?.parseRaw2String().toString()
                        cardProcessedDataModal?.setEncryptedPan(encrptedPan)
                        Log.e("ENCRYPT_PAN", "---->    $encrptedPan")


                        var responseAmount = responseIsoData.isoMap[4]?.rawData ?: "0"
                        responseAmount = responseAmount.toLong().toString()
                        cardProcessedDataModal?.setAmountInResponse(responseAmount)
                        Log.e("TransAmountF4", "---->    $responseAmount")

                        if (successResponseCode == "00") {
                            //   VFService.showToast("Auth-Complete Success")
                                AppPreference.clearReversal()
                            Log.e("callback","hit server end")
                            syncAuthTransactionCallback(
                                true,
                                successResponseCode.toString(),
                                result
                            )

                        } else {
                               AppPreference.clearReversal()
                            Log.e("callback","hit server end")
                            syncAuthTransactionCallback(
                                true,
                                successResponseCode.toString(),
                                result
                            )
                            //  VFService.showToast("Transaction Fail Error Code = ${responseIsoData.isoMap[39]?.parseRaw2String().toString()}")
                        }
                    } else {
                        //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                        /*ROCProviderV2.incrementFromResponse(
                            ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                            AppPreference.getBankCode()
                        )*/
                        /* if (!isReversal) {
                         AppPreference.clearReversal()
                     }*/
                        Log.e("callback","hit server end")
                        syncAuthTransactionCallback(false, successResponseCode.toString(), result)
                        Log.d("Failure Data:- ", result)
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }, {
                //backToCalled(it, false, true)
            })
        }
    }


    // complete pre auth
    //val completePreAuthData = MutableLiveData<CompletePreAuthData>()
    //var completePreAuthDataLocal = CompletePreAuthData()
    private val cardProcessedDataForComplete: CardProcessedDataModal by lazy { CardProcessedDataModal() }

    private var successResponseCode: String? = null

    private val _completePreAuthData = MutableLiveData<PendingPreAuthDataResponse>()
    val completePreAuthData: LiveData<PendingPreAuthDataResponse>
        get() = _completePreAuthData

    suspend fun confirmCompletePreAuth(
        authCompletionData: AuthCompletionData
    ){
        var isSuccessComp = false
        val cardProcessedData: CardProcessedDataModal by lazy { CardProcessedDataModal() }
        val transactionalAmount = authCompletionData.authAmt?.replace(".", "")?.toLong() ?: 0L
        cardProcessedData.apply {
            setTransactionAmount(transactionalAmount)
            setTransType(TransactionType.PRE_AUTH_COMPLETE.type)
            setProcessingCode(ProcessingCode.PRE_SALE_COMPLETE.code)
            setAuthBatch(authCompletionData.authBatchNo.toString())
            setAuthRoc(authCompletionData.authRoc.toString())
            setAuthTid(authCompletionData.authTid.toString())
        }
        AppPreference.saveString(AppPreference.PCKT_DATE, "")
        AppPreference.saveString(AppPreference.PCKT_TIME, "")
        AppPreference.saveString(AppPreference.PCKT_TIMESTAMP, "")

        val transactionISO = CreateAuthPacket().createPreAuthCompleteAndVoidPreauthISOPacket(
            authCompletionData,
            cardProcessedData
        )
        //Here we are Saving Date , Time and TimeStamp in CardProcessedDataModal:-
        try {
            cardProcessedData.setTime(AppPreference.getString(AppPreference.PCKT_TIME))
            cardProcessedData.setDate(AppPreference.getString(AppPreference.PCKT_DATE))
            cardProcessedData.setTimeStamp(AppPreference.getString(AppPreference.PCKT_TIMESTAMP))
        } catch (ex: Exception) {
            ex.printStackTrace()
        }


        checkReversalPerformAuthTransaction(transactionISO, cardProcessedData)

    }

    suspend fun checkReversalPerformAuthTransaction(
        transactionISOByteArray: IsoDataWriter,
        cardProcessedDataModal: CardProcessedDataModal
    ) {
        // //Sending Transaction Data Packet to Host:-(In case of no reversal)
        logger("coroutine 4", Thread.currentThread().name, "e")
        if (TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {

            val temp  = PendingPreAuthDataResponse()
                    temp.apiStatus = ApiStatus.Processing
                    temp.msg = "Sending/Receiving From Host"
                    _completePreAuthData.postValue(temp)


            logger("coroutine 5", Thread.currentThread().name, "e")
            syncAuthTransactionToHost(transactionISOByteArray, cardProcessedDataModal) { syncStatus, responseCode, transactionMsg ->

                if (syncStatus && responseCode == "00") {

                    //Below we are saving batch data and print the receipt of transaction:-
                    val responseIsoData: IsoDataReader = readIso(transactionMsg, false)
                    val autoSettlementCheck = responseIsoData.isoMap[60]?.parseRaw2String().toString()

                    AppPreference.clearReversal()

                    val encyPan = responseIsoData.isoMap[57]?.parseRaw2String().toString()
                    cardProcessedDataModal.setTrack2Data(encyPan)
                    //    cardProcessedDataModal.setPanNumberData(encyPan)
                    responseIsoData.isoMap[4]?.rawData?.toLong()?.let {
                        cardProcessedDataModal.setTransactionAmount(it)
                    }

                    val temp  = PendingPreAuthDataResponse()
                    temp.apiStatus = ApiStatus.Success
                    temp.msg = "Approved"
                    temp.isoResponse = autoSettlementCheck
                    temp.cardProcessedDataModal = cardProcessedDataModal
                    _completePreAuthData.postValue(temp)

                } else if (syncStatus && responseCode != "00") {
                    AppPreference.clearReversal()
                    val responseIsoData: IsoDataReader = readIso(transactionMsg, false)
                    val autoSettlementCheck =
                        responseIsoData.isoMap[60]?.parseRaw2String().toString()
                    //--------------
                    val temp  = PendingPreAuthDataResponse()
                    temp.apiStatus = ApiStatus.Failed
                        temp.msg = responseIsoData.isoMap[58]?.parseRaw2String().toString()
                        _completePreAuthData.postValue(temp)

                } else {
                    val temp  = PendingPreAuthDataResponse()
                    temp.apiStatus = ApiStatus.Failed
                            temp.msg = "Declined"
                            _completePreAuthData.postValue(temp)


                }
            }
        }
        //Sending Reversal Data Packet to Host:-(In Case of reversal)
        else {
            if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {

                val temp  = PendingPreAuthDataResponse()
                temp.isReversal = true
                temp.apiStatus = ApiStatus.Processing
                temp.msg = "Reversal Data Sync..."
                _completePreAuthData.postValue(temp)

                SyncReversalToHost(
                    AppPreference.getReversalNew()
                ) { syncStatus, transactionMsg ->
                    //activityContext?.hideProgress()
                    val temp  = PendingPreAuthDataResponse()
                    temp.isReversal = true
                    temp.apiStatus = ApiStatus.Processing // to sync next txn
                    _completePreAuthData.postValue(temp)

                    if (syncStatus) {
                        AppPreference.clearReversal()
                        runBlocking(Dispatchers.IO) {
                            checkReversalPerformAuthTransaction(
                                transactionISOByteArray,
                                cardProcessedDataModal
                            )
                        }
                    } else {

                        val temp  = PendingPreAuthDataResponse()
                        temp.isReversal = true
                        temp.apiStatus = ApiStatus.Failed
                        temp.msg = "Reversal Upload Fail"
                        _completePreAuthData.postValue(temp)

                    }

                }

            }
        }
    }

    private suspend fun syncAuthTransactionToHost(
        transISODataWriter: IsoDataWriter?, cardProcessedDataModal: CardProcessedDataModal?,
        syncAuthTransactionCallback: (Boolean, String, String) -> Unit
    ) {
        if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
            //In case of reversal add current date time , add field 56 data (Contains data for reversal) and change the MTI as Reversal Mti
            transISODataWriter?.mti = Mti.REVERSAL.mti
            transISODataWriter?.additionalData?.get("F56reversal")?.let {
                transISODataWriter.addFieldByHex(
                    56,
                    it
                )
            }
            if (transISODataWriter != null) {
                addIsoDateTime(transISODataWriter)
            }
        } else {
            //In Case of no reversal
            transISODataWriter?.mti = Mti.PRE_AUTH_COMPLETE_MTI.mti
        }
        val transactionISOByteArray = transISODataWriter?.generateIsoByteRequest()
        val reversalPacket = Gson().toJson(transISODataWriter)
        AppPreference.saveString(AppPreference.GENERIC_REVERSAL_KEY, reversalPacket)
        /*   transISODataWriter?.isoMap?.let { logger("After Save", it, "e") }
           throw CreateReversal()*/

        if (transactionISOByteArray != null) {
            HitServer.hitServersale(transactionISOByteArray, { result, success, readtimeout ->
                try {
                    if (success) {
                        //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                        /*ROCProviderV2.incrementFromResponse(
                            ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                            AppPreference.getBankCode()
                        )*/

                        try {
                            val responseIsoData: IsoDataReader = readIso(result, false)
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            //   syncTransactionCallback(false, "", result, null)
                        }

                        Log.d("Success Data:- ", result)
                        val responseIsoData: IsoDataReader = readIso(result, false)
                        logger("Transaction RESPONSE --->>", responseIsoData.isoMap, "e")
                        Log.e(
                            "Success 39-->  ",
                            responseIsoData.isoMap[39]?.parseRaw2String().toString() + "---->" +
                                    responseIsoData.isoMap[58]?.parseRaw2String().toString()
                        )
                        successResponseCode = (responseIsoData.isoMap[39]?.parseRaw2String().toString())
                        val authCode = (responseIsoData.isoMap[38]?.parseRaw2String().toString())
                        cardProcessedDataModal?.setAuthCode(authCode.trim())
                        //Here we are getting RRN Number :-
                        val rrnNumber = responseIsoData.isoMap[37]?.rawData ?: ""
                        cardProcessedDataModal?.setRetrievalReferenceNumber(rrnNumber)
                        val encrptedPan = responseIsoData.isoMap[57]?.parseRaw2String().toString()
                        cardProcessedDataModal?.setEncryptedPan(encrptedPan)
                        Log.e("ENCRYPT_PAN", "---->    $encrptedPan")

                        var responseAmount = responseIsoData.isoMap[4]?.rawData ?: "0"
                        responseAmount = responseAmount.toLong().toString()
                        cardProcessedDataModal?.setAmountInResponse(responseAmount)
                        Log.e("TransAmountF4", "---->    $responseAmount")

                        syncAuthTransactionCallback(true, successResponseCode.toString(), result)


                    } else {
                        AppPreference.clearReversal()
                        //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                        /*ROCProviderV2.incrementFromResponse(
                            ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                            AppPreference.getBankCode()
                        )*/
                        syncAuthTransactionCallback(
                            false,
                            successResponseCode.toString(),
                            result
                        )
                        Log.d("Failure Data:- ", result)
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    syncAuthTransactionCallback(
                        false,
                        "Something went wrong",
                        result
                    )
                    Log.e("EXCEPTION", "Something went wrong")
                }
            }, {
                //backToCalled(it, false, true)
            })
        }
    }

    // end region

    // void preAuth

    suspend fun voidAuthDataCreation(authCompletionData: AuthCompletionData) {
        val transactionalAmount = 0L //authCompletionData.authAmt?.replace(".", "")?.toLong() ?: 0L
        cardProcessedData.apply {
            setTransactionAmount(transactionalAmount)
            setTransType(TransactionType.VOID_PREAUTH.type)
            setProcessingCode(ProcessingCode.VOID_PREAUTH.code)
            setAuthBatch(authCompletionData.authBatchNo.toString())
            setAuthRoc(authCompletionData.authRoc.toString())
            //  setAuthTid(authCompletionData.authTid.toString())
        }

        AppPreference.saveString(AppPreference.PCKT_DATE, "")
        AppPreference.saveString(AppPreference.PCKT_TIME, "")
        AppPreference.saveString(AppPreference.PCKT_TIMESTAMP, "")

        val transactionISO = CreateAuthPacket().createPreAuthCompleteAndVoidPreauthISOPacket(
            authCompletionData,
            cardProcessedData
        )
        //Here we are Saving Date , Time and TimeStamp in CardProcessedDataModal:-
        try {
            cardProcessedData.setTime(AppPreference.getString(AppPreference.PCKT_TIME))
            cardProcessedData.setDate(AppPreference.getString(AppPreference.PCKT_DATE))
            cardProcessedData.setTimeStamp(AppPreference.getString(AppPreference.PCKT_TIMESTAMP))

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        logger("Transaction REQUEST PACKET --->>", transactionISO.isoMap, "e")
        val voidPreAuthInvoiceNumber = transactionISO.isoMap[62]?.rawData
        checkReversalPerformAuthTransaction(transactionISO, cardProcessedData)

    }
    // end region

    }

