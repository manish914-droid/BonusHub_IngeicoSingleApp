package com.bonushub.crdb.india.view.baseemv

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.entity.CardOption
import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.type.DemoConfigs
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.vxutils.Utility
import com.bonushub.crdb.india.vxutils.Utility.*
import com.google.gson.Gson
import com.usdk.apiservice.aidl.emv.EMVData
import com.usdk.apiservice.aidl.emv.SearchCardListener
import com.usdk.apiservice.aidl.emv.UEMV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SearchCard constructor(){

    private var uemv: UEMV? = null
    lateinit var cardProcessedDataModal: CardProcessedDataModal
    lateinit var transactionCallback: (CardProcessedDataModal) -> Unit

    constructor(uemv: UEMV?,cardProcessedDataModal: CardProcessedDataModal,
                 transactionCallback: (CardProcessedDataModal) -> Unit):this(){
                   this.uemv = uemv
                   this.cardProcessedDataModal = cardProcessedDataModal
                   this.transactionCallback = transactionCallback
                }

    fun detectCard(cardOption: CardOption) {
        var defaultScope = CoroutineScope(Dispatchers.Default)
        // start search card
        defaultScope.launch {
            println("Searching card...")
            try {
                DeviceHelper.getEMV()?.searchCard(
                    cardOption.toBundle(),
                    DemoConfigs.TIMEOUT,
                    object : SearchCardListener.Stub() {
                        override fun onCardPass(cardType: Int) {
                            println("=> onCardPass | cardType = $cardType")
                            cardProcessedDataModal.setReadCardType(DetectCardType.CONTACT_LESS_CARD_TYPE)
                            transactionCallback(cardProcessedDataModal)
                        }

                        override fun onCardInsert() {
                            println("=> onCardInsert")
                            cardProcessedDataModal.setReadCardType(DetectCardType.EMV_CARD_TYPE)
                            transactionCallback(cardProcessedDataModal)

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

                            transactionCallback(cardProcessedDataModal)


                        }

                        override fun onTimeout() {
                            println("=> onTimeout")
                        }

                        override fun onError(code: Int, message: String) {
                            println("Code: $code")
                            println("message: $message")
                            println(String.format("=> onError | %s[0x%02X]", message, code))
                        }
                    })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}