package com.bonushub.crdb.india.repository

import android.text.TextUtils
import android.util.Log
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

    /*companion object{

        @Synchronized
        fun getInstance():PreAuthRepository{
            return PreAuthRepository()
        }
    }*/


    val data = MutableLiveData<PendingPreAuthDataResponse>()
    var dataLocal = PendingPreAuthDataResponse()


    private val cardProcessedData: CardProcessedDataModal by lazy { CardProcessedDataModal() }
    val pendingPreauthList = ArrayList<PendingPreauthData>()
    var counter = 0

    suspend fun getPendingPreAuthTxn():MutableLiveData<PendingPreAuthDataResponse>{

       /* withContext(Dispatchers.Main){
            dataLocal.apiStatus = ApiStatus.Processing
            data.postValue(dataLocal)
        }*/


        doPendingPreAuth(counter)


        return data
    }

    private suspend fun doPendingPreAuth(counter: Int) {
        val transactionalAmount = 0L //authCompletionData.authAmt?.replace(".", "")?.toLong() ?: 0L
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
        //  runOnUiThread { showProgress(getString(R.string.sale_data_sync)) }
        withContext(Dispatchers.IO) {
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
//            withContext(Dispatchers.Main) { // done
//                (context as BaseActivityNew).showProgress("Getting Pending Pre-Auth From Server")
//            }

    withContext(Dispatchers.Main){
        dataLocal = PendingPreAuthDataResponse()
        dataLocal.msg = "Getting Pending Pre-Auth From Server"
        logger("ApiStatus","Processing","e")
        dataLocal.apiStatus = ApiStatus.Processing
        //data.postValue(dataLocal)
        data.value = dataLocal
    }


            syncPreAuthTransactionToHost(
                transactionISOByteArray,
                cardProcessedDataModal,
                false
            ) { syncStatus, responseCode, transactionMsg ->
//                GlobalScope.launch(Dispatchers.Main) { // done
//                    (context as BaseActivityNew).hideProgress()
//                }

                if (syncStatus && responseCode == "00") {
                    //  AppPreference.clearReversal()
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
                            //Again Request for pending pre auth transaction with next counter
                            runBlocking(Dispatchers.IO) {  doPendingPreAuth(counter) }

                        } else {

                            logger("Pending Preauth", "Finish")
                            //--

                            try {
                                runBlocking (Dispatchers.Main){
                                    pendingPreauthList.sortBy { it.bankId }
                                    logger("ApiStatus","Success","e")
                                    dataLocal = PendingPreAuthDataResponse()
                                    dataLocal.apiStatus = ApiStatus.Success
                                    dataLocal.cardProcessedDataModal = cardProcessedDataModal
                                    dataLocal.pendingList = pendingPreauthList
                                    //data.postValue(dataLocal)
                                    data.value = dataLocal
                                }

                            }catch (ex:Exception){
                                ex.printStackTrace()
                            }

                            // kushal 1105 done
                            /*(context as BaseActivityNew).transactFragment(
                                PreAuthPendingFragment()
                                    .apply {
                                        arguments = Bundle().apply {
                                            putSerializable(
                                                "PreAuthData",
                                                pendingPreauthList as java.util.ArrayList<PendingPreauthData>
                                            )
                                            putSerializable(
                                                "CardProcessData",
                                                cardProcessedDataModal
                                            )
                                        }
                                    })*/

                            //--
                            /*PrintUtil(context).printPendingPreauth(
                                cardProcessedDataModal,
                                context,
                                pendingPreauthList
                            ) { printCB ->
                                if (!printCB) {
                                    //Here we are Syncing Offline Sale if we have any in Batch Table and also Check Sale Response has Auto Settlement enabled or not:-
                                    //If Auto Settlement Enabled Show Pop Up and User has choice whether he/she wants to settle or not:-
                                    if (!TextUtils.isEmpty(autoSettlementCheck))
                                        syncOfflineSaleAndAskAutoSettlement(
                                            autoSettlementCheck.substring(
                                                0,
                                                1
                                            ), context as BaseActivity
                                        )
                                }

                            }*/
                        }
                    } else {
                    // kushal 1105
                    /*GlobalScope.launch(Dispatchers.Main) {
                            (context as BaseActivityNew).getInfoDialog(
                                "Info",
                                "No more Pending Pre-auth available"
                            ) {}
                        }*/
                        logger("ApiStatus","Success","e")
                        runBlocking(Dispatchers.Main) {
                            dataLocal.apiStatus = ApiStatus.Success
                            // data.postValue(dataLocal)
                            data.value = dataLocal
                        }

                    }

                } else if (syncStatus && responseCode != "00") {
                    //  AppPreference.clearReversal()
                    val resIso = readIso(transactionMsg, false)
                    logger("RESP DATA..>", transactionMsg)
                    logger("PendingPre RES -->", resIso.isoMap, "e")
                    val autoSettlementCheck =
                        resIso.isoMap[60]?.parseRaw2String().toString()
                    //---

                    logger("ApiStatus","Failed","e")
                    runBlocking(Dispatchers.Main){
                        dataLocal = PendingPreAuthDataResponse()
                        dataLocal.msg = resIso.isoMap[58]?.parseRaw2String().toString()
                        dataLocal.apiStatus = ApiStatus.Failed
                        //data.postValue(dataLocal)
                        data.value = dataLocal
                    }

                    // kushal 1105 done
                   /* GlobalScope.launch(Dispatchers.Main) {
                        context.getString(R.string.error_hint).let {
                            (context as BaseActivityNew).alertBoxWithActionNew(
                                it,
                                resIso.isoMap[58]?.parseRaw2String().toString(),
                                R.drawable.ic_info,
                                context.getString(R.string.positive_button_ok),
                                "",false,false,
                                { alertPositiveCallback ->
                                    if (alertPositiveCallback) {
                                        if (!TextUtils.isEmpty(autoSettlementCheck))
                                            syncOfflineSaleAndAskAutoSettlement(
                                                autoSettlementCheck.substring(
                                                    0,
                                                    1
                                                ), context as BaseActivityNew
                                            )
                                    }
                                },
                                {})
                        }
                    }*/
                } else {
                    //   AppPreference.clearReversal()
                    logger("ApiStatus","Failed","e")
                    runBlocking(Dispatchers.Main){
                        dataLocal = PendingPreAuthDataResponse()
                        dataLocal.msg = "Connection Failed"
                        dataLocal.apiStatus = ApiStatus.Failed
                        // data.postValue(dataLocal)
                        data.value = dataLocal
                    }

                    // kushal 1105 done
                /*GlobalScope.launch(Dispatchers.Main) {
                        (context as BaseActivityNew).hideProgress()
                        (context as BaseActivityNew).alertBoxWithActionNew(
                            (context as BaseActivityNew).getString(R.string.connection_failed),
                            (context as BaseActivityNew).getString(R.string.pending_preauthdetails),
                            R.drawable.ic_info,
                            (context as BaseActivityNew).getString(R.string.positive_button_ok),
                            "",false,false,
                            { alertPositiveCallback ->
                                if (alertPositiveCallback)
                                    declinedTransaction()
                            },
                            {})

                        //    VFService.showToast(transactionMsg)
                    }*/
                }
            }
        }
        //Sending Reversal Data Packet to Host:-(In Case of reversal)
        else {
            if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
                /*withContext(Dispatchers.Main) {
                    (context as BaseActivityNew).showProgress((context as NavigationActivity).getString(
                        R.string.reversal_data_sync))
                }*/
                logger("ApiStatus","Processing","e")
                withContext(Dispatchers.Main){
                    dataLocal = PendingPreAuthDataResponse()
                    dataLocal.msg = "Uploading reversal"
                    dataLocal.apiStatus = ApiStatus.Processing
                    // data.postValue(dataLocal)
                    data.value = dataLocal
                }


                SyncReversalToHost(AppPreference.getReversalNew()) { isSyncToHost, transMsg ->
                    //(context as BaseActivityNew).hideProgress()
                    if (isSyncToHost) {
                        AppPreference.clearReversal()
                        GlobalScope.launch(Dispatchers.IO) {
                            checkReversalPerformPendingPreAuthTransaction(
                                transactionISOByteArray,
                                cardProcessedDataModal
                            )
                        }
                    } else {
                        GlobalScope.launch(Dispatchers.Main) {
                            //  VFService.showToast(transMsg)
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
            HitServer.hitServer(transactionISOByteArray, { result, success ->
                //Save Server Hit Status in Preference:-
                AppPreference.saveBoolean(PrefConstant.SERVER_HIT_STATUS.keyName.toString(), true)
                try {
                    if (success) {
                        //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                        // kushal 1105
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
                            syncAuthTransactionCallback(
                                true,
                                successResponseCode.toString(),
                                result
                            )

                        } else {
                               AppPreference.clearReversal()
                            syncAuthTransactionCallback(
                                true,
                                successResponseCode.toString(),
                                result
                            )
                            //  VFService.showToast("Transaction Fail Error Code = ${responseIsoData.isoMap[39]?.parseRaw2String().toString()}")
                        }
                    } else {
                        //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                        // kushal 1105
                        /*ROCProviderV2.incrementFromResponse(
                            ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                            AppPreference.getBankCode()
                        )*/
                        /* if (!isReversal) {
                         AppPreference.clearReversal()
                     }*/
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
    val completePreAuthData = MutableLiveData<CompletePreAuthData>()
    var completePreAuthDataLocal = CompletePreAuthData()
    private val cardProcessedDataForComplete: CardProcessedDataModal by lazy { CardProcessedDataModal() }

    private var successResponseCode: String? = null
    suspend fun confirmCompletePreAuth(
        authCompletionData: AuthCompletionData
    ):MutableLiveData<CompletePreAuthData> {
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
        val transactionISO = CreateAuthPacket().createPreAuthCompleteAndVoidPreauthISOPacket(
            authCompletionData,
            cardProcessedData
        )
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
        //    logger("Transaction REQUEST PACKET --->>", transactionISO.isoMap, "e")
        //  runOnUiThread { showProgress(getString(R.string.sale_data_sync)) }
        /*activity?.let {
            SyncAuthTransToHost(it as BaseActivity).checkReversalPerformAuthTransaction(
                transactionISO, cardProcessedData
            ) { isSuccess, msg ->

                if (isSuccess) {
                    mAdapter.refreshListRemoveAt(position)
                }
                // VFService.showToast("$msg----------->  $isSuccess")
                logger("PREAUTHCOMP", "Is success --->  $isSuccess  Msg --->  $msg")
                //   parentFragmentManager.popBackStackImmediate()
            }
        }*/



        checkReversalPerformAuthTransaction(
                transactionISO, cardProcessedData
            ) { isSuccess, msg ->

                if (isSuccess) {
                   // mAdapter.refreshListRemoveAt(position) // kushal 1605
                       runBlocking(Dispatchers.Main){
                           completePreAuthDataLocal.apiStatus = ApiStatus.Success
                           completePreAuthData.value = completePreAuthDataLocal
                       }

                }
                // VFService.showToast("$msg----------->  $isSuccess")
                logger("PREAUTHCOMP", "Is success --->  $isSuccess  Msg --->  $msg")
                //   parentFragmentManager.popBackStackImmediate()
            }


        return completePreAuthData

    }

    suspend fun checkReversalPerformAuthTransaction(
        transactionISOByteArray: IsoDataWriter,
        cardProcessedDataModal: CardProcessedDataModal, cb: (Boolean, String) -> Unit
    ) {
        // //Sending Transaction Data Packet to Host:-(In case of no reversal)
        logger("coroutine 4", Thread.currentThread().name, "e")
        if (TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
           /* withContext(Dispatchers.Main) {
                activityContext?.getString(R.string.sale_data_sync)?.let {
                    activityContext?.showProgress(it)
                }
            }*/
                withContext(Dispatchers.Main){
                    completePreAuthDataLocal.apiStatus = ApiStatus.Processing
                    completePreAuthDataLocal.msg = "Sending/Receiving From Host"
                    completePreAuthData.value = completePreAuthDataLocal
                }


            logger("coroutine 5", Thread.currentThread().name, "e")
            syncAuthTransactionToHost(transactionISOByteArray, cardProcessedDataModal) { syncStatus, responseCode, transactionMsg ->
                //withactivityContext(Dispatchers.Main){
                //activityContext?.hideProgress()

                //}
                if (syncStatus && responseCode == "00") {
                    /*GlobalScope.launch(Dispatchers.Main) {
                        activityContext?.let { txnSuccessToast(it) }
                    }*/

                        runBlocking(Dispatchers.Main){
                            completePreAuthDataLocal.apiStatus = ApiStatus.Success
                            completePreAuthDataLocal.msg = ""
                            completePreAuthData.value = completePreAuthDataLocal
                        }


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
                    StubBatchData("", cardProcessedDataModal.getTransType(), cardProcessedDataModal, null, autoSettlementCheck) { stubbedData ->

                        // kushal 1605
                        /*activityContext?.let {
                            printAndSaveAuthTransToBatchDataInDB(stubbedData, autoSettlementCheck, it) { isSuccess, msg ->
                                cb(true, msg)

                            }
                        }*/

                    }
                } else if (syncStatus && responseCode != "00") {
                    AppPreference.clearReversal()
                    val responseIsoData: IsoDataReader = readIso(transactionMsg, false)
                    val autoSettlementCheck =
                        responseIsoData.isoMap[60]?.parseRaw2String().toString()
                    //--------------
                    runBlocking(Dispatchers.Main){
                        completePreAuthDataLocal.apiStatus = ApiStatus.Failed
                        completePreAuthDataLocal.msg = responseIsoData.isoMap[58]?.parseRaw2String().toString()
                        completePreAuthData.value = completePreAuthDataLocal
                    }


                    /*GlobalScope.launch(Dispatchers.Main) {
                        activityContext?.getString(R.string.error_hint)?.let {
                            activityContext?.alertBoxWithAction(null,
                                null,
                                it,
                                responseIsoData.isoMap[58]?.parseRaw2String().toString(),
                                false,
                                activityContext!!.getString(R.string.positive_button_ok),
                                { alertPositiveCallback ->
                                    if (alertPositiveCallback) {
                                        if (!TextUtils.isEmpty(autoSettlementCheck))
                                            syncOfflineSaleAndAskAutoSettlement(
                                                autoSettlementCheck.substring(
                                                    0,
                                                    1
                                                ), activityContext!!
                                            ) { isucc, msg ->
                                                cb(
                                                    false,
                                                    responseIsoData.isoMap[58]?.parseRaw2String()
                                                        .toString()
                                                )
                                            }
                                        else {
                                            // activityContext?.startActivity(Intent((activityContext as BaseActivity), MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK })
                                            cb(
                                                false,
                                                responseIsoData.isoMap[58]?.parseRaw2String()
                                                    .toString()
                                            )

                                        }
                                    }
                                },
                                {})
                        }
                    }*/
                } else {
                    //    VFService.showToast(transactionMsg)
                        runBlocking(Dispatchers.Main){
                            completePreAuthDataLocal.apiStatus = ApiStatus.Failed
                            completePreAuthDataLocal.msg = "Declined"
                            completePreAuthData.value = completePreAuthDataLocal
                        }


                    /*activityContext?.hideProgress()
                    checkForPrintReversalReceipt(activityContext,"") {
                        (activityContext as BaseActivity).hideProgress()
                        GlobalScope.launch(Dispatchers.Main) {
                            (activityContext as BaseActivity).alertBoxWithAction(
                                null,
                                null,
                                activityContext!!.getString(R.string.declined),
                                activityContext!!.getString(R.string.transaction_delined_msg),
                                false,
                                activityContext!!.getString(R.string.positive_button_ok),
                                { alertPositiveCallback ->
                                    if (alertPositiveCallback)
                                        cb(
                                            false,
                                            activityContext!!.getString(R.string.transaction_delined_msg)
                                        )
                                    //  declinedTransaction()
                                },
                                {})
                        }
                    }*/

                }
            }
        }
        //Sending Reversal Data Packet to Host:-(In Case of reversal)
        else {
            /*if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
                withContext(Dispatchers.Main) {
                    activityContext?.showProgress("Reversal Data Sync...")
                }
                SyncReversalToHost(
                    AppPreference.getReversal()
                ) { syncStatus, transactionMsg ->
                    activityContext?.hideProgress()
                    if (syncStatus) {
                        AppPreference.clearReversal()
                        GlobalScope.launch(Dispatchers.IO) {
                            checkReversalPerformAuthTransaction(
                                transactionISOByteArray,
                                cardProcessedDataModal
                            ) { bool, str ->
                                cb(bool, str)
                            }
                        }
                    } else {
                        GlobalScope.launch(Dispatchers.Main) {
                            VFService.showToast(transactionMsg)
                            cb(false, transactionMsg)
                            GlobalScope.launch(Dispatchers.Main) {
                                (activityContext as BaseActivity).alertBoxWithAction(
                                    null,
                                    null,
                                    activityContext!!.getString(R.string.reversal),
                                    activityContext!!.getString(R.string.reversal_upload_fail),
                                    false,
                                    activityContext!!.getString(R.string.positive_button_ok),
                                    { alertPositiveCallback ->

                                        if (alertPositiveCallback)
                                            cb(
                                                false,
                                                activityContext!!.getString(R.string.transaction_delined_msg)
                                            )
                                        // declinedTransaction()
                                    },
                                    {})
                            }


                        }
                    }
                }
            }*/
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

    }

