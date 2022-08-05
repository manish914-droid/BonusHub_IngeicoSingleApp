package com.bonushub.crdb.india.view.baseemv

import android.os.Bundle
import com.bonushub.crdb.india.entity.CardOption
import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.type.DemoConfigs
import com.bonushub.crdb.india.utils.DetectCardType
import com.bonushub.crdb.india.utils.DeviceHelper
import com.usdk.apiservice.aidl.beeper.BeeperFrequency
import com.usdk.apiservice.aidl.emv.EMVData
import com.usdk.apiservice.aidl.emv.SearchCardListener
import com.usdk.apiservice.aidl.emv.UEMV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchCard (val cardProcessedDataModal: CardProcessedDataModal,val cardOption: CardOption,val searchCardCb:(CardProcessedDataModal)->Unit){

    init {
        try {
            DeviceHelper.getEMV()?.searchCard(
                cardOption.toBundle(),
                DemoConfigs.TIMEOUT,
                object : SearchCardListener.Stub() {
                    override fun onCardPass(cardType: Int) {
                        println("=> onCardPass | cardType = $cardType")
                        cardProcessedDataModal.setReadCardType(DetectCardType.CONTACT_LESS_CARD_TYPE)
                        DeviceHelper.getBeeper()?.startBeep(200)
                        searchCardCb(cardProcessedDataModal)
                    }

                    override fun onCardInsert() {
                        println("=> onCardInsert")
                        cardProcessedDataModal.setReadCardType(DetectCardType.EMV_CARD_TYPE)
                        DeviceHelper.getBeeper()?.startBeep(200)
                        searchCardCb(cardProcessedDataModal)
                    }

                    override fun onCardSwiped(track: Bundle) {
                        // uemv?.stopSearch()
                        println("=> onCardSwiped")
                        println("==> Pan: " + track.getString(EMVData.PAN))
                        println("==> Track 1: " + track.getString(EMVData.TRACK1))
                        println("==> Track 2: " + track.getString(EMVData.TRACK2))
                        println("==> Track 3: " + track.getString(EMVData.TRACK3))
                        println("==> Service code: " + track.getString(EMVData.SERVICE_CODE))
                        println("==> Card exprited date: " + track.getString(EMVData.EXPIRED_DATE))
                        cardProcessedDataModal.setReadCardType(DetectCardType.MAG_CARD_TYPE)
                        val pan = track.getString(EMVData.PAN)
                        val track1 = track.getString(EMVData.TRACK1)
                        val track2 = track.getString(EMVData.TRACK2)
                        val track3 = track.getString(EMVData.TRACK3)
                        val serviceCode = track.getString(EMVData.SERVICE_CODE)
                        pan?.let { cardProcessedDataModal.setPanNumberData(it) }
                        track1?.let { cardProcessedDataModal.setTrack1Data(it) }
                        track2?.let { cardProcessedDataModal.setTrack2Data(it) }
                        track3?.let { cardProcessedDataModal.setTrack3Data(it) }
                        serviceCode?.let { cardProcessedDataModal.setServiceCodeData(it) }
                        //Expiry Date
                        serviceCode?.let { cardProcessedDataModal.setExpiredDate(track.getString(EMVData.EXPIRED_DATE)) }

                        DeviceHelper.getBeeper()?.startBeep(200)
                        searchCardCb(cardProcessedDataModal)


                    }

                    override fun onTimeout() {
                        println("=> onTimeout")
                     //   DetectCardType.TIMEOUT
                        cardProcessedDataModal.setReadCardType(DetectCardType.TIMEOUT)
                        searchCardCb(cardProcessedDataModal)
                    }

                    override fun onError(code: Int, message: String) {
                        println("Code: $code")
                        println("message: $message")
                        println(String.format("=> onError | %s[0x%02X]", message, code))
                        cardProcessedDataModal.setReadCardType(DetectCardType.ERROR)
                        searchCardCb(cardProcessedDataModal)
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }



    }

}

fun success(callback: (CardProcessedDataModal?,Boolean, String) -> Unit) {
    callback(null,true, "")
}

fun error(callback: (Boolean, String) -> Unit, message: String) {
    callback(false, message)
}