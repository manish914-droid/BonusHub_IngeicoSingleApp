package com.bonushub.crdb.india.transactionprocess

import android.app.Activity
import android.app.AlertDialog
import android.os.*
import android.util.Log
import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.type.EmvOption
import com.bonushub.crdb.india.utils.DeviceHelper
import com.bonushub.crdb.india.view.activity.TransactionActivity
import com.bonushub.crdb.india.view.baseemv.VFEmvHandler
import com.usdk.apiservice.aidl.pinpad.DeviceName
import com.usdk.apiservice.aidl.pinpad.KAPId
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DoEmv(var testVFEmvHandler: VFEmvHandler,var  activity: Activity, var cardProcessedDataModal: CardProcessedDataModal,
            var transactionCallback: (CardProcessedDataModal,VFEmvHandler) -> Unit) {


    //    private var iemv: IEMV? = VFService.vfIEMV
    var transactionalAmount = cardProcessedDataModal.getTransactionAmount() ?: 0
    var otherAmount = cardProcessedDataModal.getOtherAmount() ?: 0

    init {
        startEMVProcess(transactionalAmount)
    }

    // region ========================== First GEN AC   (Setting parameters for first gen then start EMV process)
    private fun startEMVProcess(transactionalAmount: Long) {
        val emvOption = EmvOption.create().apply {
            flagPSE(0x00.toByte())
        }
        testVFEmvHandler = emvHandler()
        DeviceHelper.getEMV()?.startEMV(emvOption?.toBundle(), testVFEmvHandler)
    }
    //endregion

    //region========================================Below Method is a Handler for EMV CardType:-
    private fun emvHandler(): VFEmvHandler {
        println("DoEmv VfEmvHandler is calling")
        println("IEmv value is" + DeviceHelper.getEMV().toString())
        return  VFEmvHandler(
            DeviceHelper.getPinpad(KAPId(0, 0), 0, DeviceName.IPP),
            DeviceHelper.getEMV(),activity as TransactionActivity,cardProcessedDataModal){ cardProcessedData ->
            transactionCallback(cardProcessedData,testVFEmvHandler)
        }
    }
    //endregion



}