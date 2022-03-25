package com.bonushub.crdb.india.view.baseemv

import android.os.Bundle
import android.os.RemoteException
import androidx.appcompat.app.AppCompatActivity
import com.bonushub.crdb.india.entity.EMVOption
import com.bonushub.crdb.india.utils.DeviceHelper
import com.bonushub.crdb.india.utils.ingenico.BytesUtil
import com.usdk.apiservice.aidl.emv.*

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
     //   DeviceHelper.vfUEMV

        var ret: Int = DeviceHelper.vfUEMV!!.startEMV(EMVOption.create().toBundle(), emvEventHandler)
    }

    protected open var emvEventHandler: EMVEventHandler = object : EMVEventHandler.Stub() {
        @Throws(RemoteException::class)
        override fun onInitEMV() {
            doInitEMV()
        }

        @Throws(RemoteException::class)
        override fun onWaitCard(flag: Int) {
          //  doWaitCard(flag)
        }

        @Throws(RemoteException::class)
        override fun onCardChecked(cardType: Int) {
            // Only happen when use startProcess()
           // doCardChecked(cardType)
        }

        @Throws(RemoteException::class)
        override fun onAppSelect(reSelect: Boolean, list: List<CandidateAID>) {
          //  doAppSelect(reSelect, list)
        }

        @Throws(RemoteException::class)
        override fun onFinalSelect(finalData: FinalData) {
           // doFinalSelect(finalData)
        }

        @Throws(RemoteException::class)
        override fun onReadRecord(cardRecord: CardRecord) {
          //  lastCardRecord = cardRecord
          //  doReadRecord(cardRecord)
        }

        @Throws(RemoteException::class)
        override fun onCardHolderVerify(cvmMethod: CVMMethod) {
          //  doCardHolderVerify(cvmMethod)
        }

        @Throws(RemoteException::class)
        override fun onOnlineProcess(transData: TransData) {
          //  doOnlineProcess(transData)
        }

        @Throws(RemoteException::class)
        override fun onEndProcess(result: Int, transData: TransData) {
           // doEndProcess(result, transData)
        }

        @Throws(RemoteException::class)
        override fun onVerifyOfflinePin(
            flag: Int,
            random: ByteArray,
            caPublicKey: CAPublicKey,
            offlinePinVerifyResult: OfflinePinVerifyResult
        ) {
            //doVerifyOfflinePin(flag, random, caPublicKey, offlinePinVerifyResult)
        }

        @Throws(RemoteException::class)
        override fun onObtainData(ins: Int, data: ByteArray) {

        }

        @Throws(RemoteException::class)
        override fun onSendOut(ins: Int, data: ByteArray) {
           // doSendOut(ins, data)
        }
    }

    open fun doInitEMV() {
       // TODO("Not yet implemented")
    }


}