package com.bonushub.crdb.india.transactionprocess
import android.util.Log
import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.utils.DetectCardType
import com.bonushub.crdb.india.utils.DeviceHelper
import com.bonushub.crdb.india.utils.logger
import com.usdk.apiservice.aidl.emv.UEMV


class SecondGenAcOnNetworkError(
    var result: String, private var cardProcessedDataModal: CardProcessedDataModal?,
    var networkErrorSecondGenCB: (Boolean) -> Unit
) {
    val vfIEMV: UEMV? by lazy { DeviceHelper.getEMV() }

    init {
        generateSecondGenAcForNetworkErrorCase(result)
    }

    private fun generateSecondGenAcForNetworkErrorCase(result: String) {
        //Here Second GenAC performed in Every Network Failure cases or Time out case:-
        Log.d("Failure Data:- ", result)
        when (cardProcessedDataModal?.getReadCardType()) {
            DetectCardType.MAG_CARD_TYPE -> {
                networkErrorSecondGenCB(false)
            }
            DetectCardType.EMV_CARD_TYPE -> {
                //Test case 15 Unable to go Online
                //here 2nd genearte AC

            }
            DetectCardType.CONTACT_LESS_CARD_TYPE -> {
                networkErrorSecondGenCB(false)
            }
            DetectCardType.CONTACT_LESS_CARD_WITH_MAG_TYPE -> {
                networkErrorSecondGenCB(false)
            }
            else -> logger(
                "CARD_ERROR:- ",
                cardProcessedDataModal?.getReadCardType().toString(),
                "e"
            )
        }
    }
}