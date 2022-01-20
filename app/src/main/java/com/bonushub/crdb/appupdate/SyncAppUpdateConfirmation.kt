package com.bonushub.crdb.appupdate

import android.util.Log
import com.bonushub.crdb.serverApi.HitServer
import com.bonushub.crdb.utils.IsoDataReader
import com.bonushub.crdb.utils.Utility
import com.bonushub.crdb.utils.logger
import com.bonushub.crdb.utils.readIso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SyncAppUpdateConfirmation(private var confirmationISOData: ByteArray, cb: (Boolean) -> Unit) {
    init {
        GlobalScope.launch(Dispatchers.IO) {
            cb(sendAppUpdateConfirmation(confirmationISOData))
        }
    }

    private suspend fun sendAppUpdateConfirmation(offlineSaleIso: ByteArray): Boolean {
        var isBool = false
        HitServer.hitServer(offlineSaleIso, { result, success ->
            if (success) {
                //Below we are incrementing previous ROC (Because ROC will always be incremented whenever Server Hit is performed:-
             //   Utility().incrementRoc()
              //  ROCProviderV2.incrementFromResponse(ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(), AppPreference.getBankCode())

                val responseIsoData: IsoDataReader = readIso(result, false)
                logger("Transaction RESPONSE ", "---", "e")
                logger("Transaction RESPONSE --->>", responseIsoData.isoMap, "e")
                Log.e(
                    "Success 39-->  ",
                    responseIsoData.isoMap[39]?.parseRaw2String().toString() + "---->" +
                            responseIsoData.isoMap[58]?.parseRaw2String().toString()
                )
                val successResponseCode = (responseIsoData.isoMap[39]?.parseRaw2String().toString())
                isBool = successResponseCode == "00"
            } else {
              //  Utility().incrementRoc()
              //  ROCProviderV2.incrementFromResponse(ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(), AppPreference.getBankCode())
                isBool = false
            }
        }, {},isAppUpdate = true)
        return isBool
    }
}