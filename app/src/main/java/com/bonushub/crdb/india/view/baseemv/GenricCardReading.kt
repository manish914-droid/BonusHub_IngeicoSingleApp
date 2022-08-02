package com.bonushub.crdb.india.view.baseemv

import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.utils.byteArr2HexStr
import com.bonushub.crdb.india.view.base.BaseActivityNew
import com.usdk.apiservice.aidl.emv.*


class GenericCardReadHandler constructor(val emv:UEMV?, val activity: BaseActivityNew, val cardProcessedDataModal: CardProcessedDataModal, val cardBinCb:(String)->Unit) : EMVEventHandler.Stub(){

    lateinit var onEndProcessCallback: (Int,CardProcessedDataModal) -> Unit
    override fun onInitEMV() {
       settingAids(emv)
       //settingCAPkeys(emv)
    }

    override fun onAppSelect(reSelect: Boolean, candidateList: MutableList<CandidateAID>) {
        doEmvAppSelection(reSelect,candidateList,emv,activity)
    }

    override fun onFinalSelect(finalData: FinalData?) {
        if (finalData != null) {
           doFinalSelect(finalData,cardProcessedDataModal,emv)
        }
    }

    override fun onSendOut(p0: Int, p1: ByteArray) {
      //cardBinCb( getCardBinFromEMV(p0,p1,emv))
    }

    override fun onWaitCard(p0: Int) {
       // TODO("Not yet implemented")
    }

    override fun onCardChecked(p0: Int) {
       // TODO("Not yet implemented")
    }

    override fun onReadRecord(cardRecord: CardRecord?) {
    //  emv?.stopSearch()
      //  emv?.stopProcess()
      //  emv?.stopEMV()
      //  emv?.halt()
        cardBinCb(  cardRecord?.pan?.byteArr2HexStr().toString())


    }

    override fun onCardHolderVerify(p0: CVMMethod?) {
        TODO("Not yet implemented")
    }

    override fun onOnlineProcess(p0: TransData?) {
        TODO("Not yet implemented")
    }

    override fun onEndProcess(p0: Int, p1: TransData?) {
        // when aids not setting
        println("onEndProcess")
        onEndProcessCallback(p0,cardProcessedDataModal)
    }

    override fun onVerifyOfflinePin(
        p0: Int,
        p1: ByteArray?,
        p2: CAPublicKey?,
        p3: OfflinePinVerifyResult?
    ) {
      //  TODO("Not yet implemented")
    }

    override fun onObtainData(p0: Int, p1: ByteArray?) {
        TODO("Not yet implemented")
    }

}