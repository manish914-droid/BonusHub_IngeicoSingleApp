package com.bonushub.crdb.india.view.baseemv

import android.os.Bundle
import com.bonushub.crdb.india.entity.CardOption
import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.type.DemoConfigs
import com.bonushub.crdb.india.utils.DetectCardType
import com.usdk.apiservice.aidl.emv.EMVData
import com.usdk.apiservice.aidl.emv.SearchCardListener
import com.usdk.apiservice.aidl.emv.UEMV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchCard(private var uemv: UEMV?, var cardProcessedDataModal: CardProcessedDataModal,
                 var transactionCallback: (CardProcessedDataModal) -> Unit) {

    init {
        detectCard()
        
    }

    private fun detectCard() {
        var defaultScope = CoroutineScope(Dispatchers.Default)
        // start search card
        defaultScope.launch {
            println("Searching card...")
            try {

                val cardOption = CardOption.create().apply {
                    supportICCard(true)
                    supportMagCard(true)
                    supportRFCard(true)
                }

                uemv?.searchCard(
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
                            println("=> onCardSwiped")
                            println("==> Pan: " + track.getString(EMVData.PAN))
                            println("==> Track 1: " + track.getString(EMVData.TRACK1))
                            println("==> Track 2: " + track.getString(EMVData.TRACK2))
                            println("==> Track 3: " + track.getString(EMVData.TRACK3))
                            println("==> Service code: " + track.getString(EMVData.SERVICE_CODE))
                            println("==> Card exprited date: " + track.getString(EMVData.EXPIRED_DATE))
                            cardProcessedDataModal.setReadCardType(DetectCardType.MAG_CARD_TYPE)
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