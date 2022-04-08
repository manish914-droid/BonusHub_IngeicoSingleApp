package com.bonushub.crdb.india.transactionprocess

import android.text.TextUtils
import android.util.Log
import com.bonushub.crdb.india.model.local.AppPreference
import com.bonushub.crdb.india.serverApi.HitServer
import com.bonushub.crdb.india.utils.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SyncReversalToHost(private var transactionISOData: IsoDataWriter?, var syncTransactionCallback: (Boolean, String) -> Unit) {

    private var successResponseCode: String? = null
    private var transMsg: String? = null

    init {

        GlobalScope.launch(Dispatchers.IO) {
            syncReversal()
        }
    }

    private suspend fun syncReversal() {

        if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
            System.out.println("HitReversal called1")
            //In case of reversal add current date time , add field 56 data (Contains data for reversal) and change the MTI as Reversal Mti
            transactionISOData?.mti = Mti.REVERSAL.mti
            transactionISOData?.additionalData?.get("F56reversal")
                ?.let { transactionISOData?.addFieldByHex(56, it) }
            transactionISOData?.let { addIsoDateTime(it) }

            var field55      =  transactionISOData?.isoMap?.get(55)?.rawData ?: ""
            var DE55reversal =  transactionISOData?.additionalData?.get("DE55reversal") ?: ""

            System.out.println("HitReversal called2"+field55)

            if(null !=field55 && field55.isNotBlank() && null !=DE55reversal && DE55reversal.isNotBlank()) {
                field55 = field55 + DE55reversal
                println("Issuer script data in reversal"+field55)
                transactionISOData?.addField(55, field55)
            }
            else if(null !=field55 && field55.isNotBlank()){
                println("Issuer script data without reversal"+field55)
                System.out.println("HitReversal called3")
                transactionISOData?.addField(55, field55)
            }

            transactionISOData?.additionalData?.get("F39reversal")
                    ?.let {
                        //    VFService.showToast("39 data in reversal normal "+it)
                        println("39 data in reversal "+it)
                        transactionISOData?.addFieldByHex(39, it)
                    }
        }
        val transactionISOByteArray = transactionISOData?.generateIsoByteRequest()
        if (transactionISOData != null) {
            transactionISOData?.isoMap?.let { logger("Transaction REQUEST PACKET --->>", it, "e") }
        }
        val reversalPacket = Gson().toJson(transactionISOData)
        AppPreference.saveString(AppPreference.GENERIC_REVERSAL_KEY, reversalPacket)
        transactionISOByteArray?.byteArr2HexStr()?.let { logger("PACKET-->", it) }
        // throw CreateReversal()

        if (transactionISOByteArray != null) {
            System.out.println("HitReversal called")
            HitServer.hitServer(transactionISOByteArray, { result, success ->
                if (success) {
                    //Reversal save To Preference code here.............
                    //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-

                  //  ROCProviderV2.incrementFromResponse(ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(), AppPreference.getBankCode())

                    Utility().incrementRoc()

                    Log.d("Success Data:- ", result)
                    val responseIsoData: IsoDataReader = readIso(result, false)
                    logger("Transaction RESPONSE ", "---", "e")
                    logger("Transaction RESPONSE --->>", responseIsoData.isoMap, "e")
                    Log.e(
                        "Success 39-->  ",
                        responseIsoData.isoMap[39]?.parseRaw2String().toString() + "---->" +
                                responseIsoData.isoMap[58]?.parseRaw2String().toString()
                    )
                    successResponseCode = (responseIsoData.isoMap[39]?.parseRaw2String().toString())
                    transMsg = responseIsoData.isoMap[58]?.parseRaw2String().toString()
                    if (successResponseCode == "00") {
                        syncTransactionCallback(true, transMsg.toString())
                    } else {
                        syncTransactionCallback(false, transMsg.toString())
                    }
                } else {
                    //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
                //    ROCProviderV2.incrementFromResponse(ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(), AppPreference.getBankCode())
                    Utility().incrementRoc()
                    syncTransactionCallback(false, "Uploading reversal fail....")
                }
            }, {
                //backToCalled(it, false, true)
            })
        }
    }

}