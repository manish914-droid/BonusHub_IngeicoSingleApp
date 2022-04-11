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
                           // uemv?.stopSearch()

                            println("=> onCardSwiped")
                            println("==> Pan: " + track.getString(EMVData.PAN))
                            println("==> Track 1: " + track.getString(EMVData.TRACK1))
                            println("==> Track 2: " + track.getString(EMVData.TRACK2))
                            println("==> Track 3: " + track.getString(EMVData.TRACK3))
                            println("==> Service code: " + track.getString(EMVData.SERVICE_CODE))
                            println("==> Card exprited date: " + track.getString(EMVData.EXPIRED_DATE))
                            cardProcessedDataModal.setReadCardType(DetectCardType.MAG_CARD_TYPE)

                            /*val pan = track.getString(EMVData.PAN)
                            val track1 = track.getString(EMVData.TRACK1)
                            val track2 = track.getString(EMVData.TRACK2)
                            val track3 = track.getString(EMVData.TRACK3)
                            val serviceCode = track.getString(EMVData.SERVICE_CODE)
                            pan?.let { cardProcessedDataModal.setPanNumberData(it) }
                            track1?.let { cardProcessedDataModal.setTrack1Data(it) }
                            track2?.let { cardProcessedDataModal.setTrack2Data(it) }
                            track3?.let { cardProcessedDataModal.setTrack3Data(it) }
                            serviceCode?.let { cardProcessedDataModal.setServiceCodeData(it) }

                            transactionCallback(cardProcessedDataModal)*/

                            try{
                           //     if (fallbackType != EFallbackCode.Swipe_fallback.fallBackCode) {
                                    Log.e("detectCard", "onCardSwiped ...")

                                    val pan = track.getString(EMVData.PAN)
                                    val track1 = track.getString(EMVData.TRACK1)
                                    val track2 = track.getString(EMVData.TRACK2)
                                    val track3 = track.getString(EMVData.TRACK3)
                                    val serviceCode = track.getString(EMVData.SERVICE_CODE)


                                    val currDate = getCurrentDateforMag()
                                    val validDate =
                                        track.getString(EMVData.EXPIRED_DATE)

                                    if (currDate.compareTo(validDate!!) <= 0) {
                                        println("Correct Date")

                                        Log.d("detectCard", "onCardSwiped ...1")
                                        val bytes: ByteArray = hexStr2Byte(track2)
                                        Log.d(
                                            "detectCard",
                                            "Track2:" + track2 + " (" + byte2HexStr(bytes) + ")"
                                        )

                                        Utility.getCardHolderName(cardProcessedDataModal, track1, '^', '^')

                                        // please check
                                        var bIsKeyExist: Boolean? =  false // false is temp /*VFService.getPinPadData()
                                        if (!bIsKeyExist!!) {
                                            Log.e("detectCard", "no key exist type: 12, @: 1")
                                        }
                                        // please check
                                        val enctypted: ByteArray? = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0) // this byte array is temp
                                        // please check
                                            /*VFService.getDupkt(
                                                1,
                                                1,
                                                1,
                                                bytes,
                                                byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
                                            )*/
                                        if (null == enctypted) {
                                            Log.e("detectCard", "NO DUKPT Encrypted got")
                                        } else {
                                            Log.d(
                                                "detectCard",
                                                "DUKPT:" + byte2HexStr(enctypted)
                                            )
                                        }
                                        // please check
                                       // bIsKeyExist = VFService.getPinPadData()
                                        if (!bIsKeyExist!!) {
                                            Log.e("detectCard", "no key exist type: 12, @: 1")
                                        }

                                        Log.d("detectCard", "onCardSwiped ...3")

                                        //Stubbing Card Processed Data:-
                                        cardProcessedDataModal.setReadCardType(DetectCardType.MAG_CARD_TYPE)
                                        if (track2 != null) {
                                            // please check
                                            /*cardProcessedDataModal.setTrack2Data(
                                            getEncryptedTrackData(
                                                    track2,
                                                    cardProcessedDataModal
                                                ).toString()
                                            )*/
                                        }
                                        if (track1 != null) {
                                            cardProcessedDataModal.setTrack1Data(track1)
                                        }
                                        if (track3 != null) {
                                            cardProcessedDataModal.setTrack3Data(track3)
                                        }
                                        if (pan != null) {
                                            cardProcessedDataModal.setPanNumberData(pan)
                                            cardProcessedDataModal.getPanNumberData()?.let {
                                                Log.e(
                                                    "SWIPE_PAN",
                                                    it
                                                )
                                            }
                                            //  cardProcessedDataModal.setPanNumberData("6789878786")
                                            if (!cardProcessedDataModal.getPanNumberData()
                                                    ?.let { cardLuhnCheck(it) }!!) {
                                                onError(
                                                    DetectError.IncorrectPAN.errorCode,
                                                    "Invalid Card Number"
                                                )
                                            } else {
                                                if (serviceCode != null) {
                                                    cardProcessedDataModal.setServiceCodeData(serviceCode)
                                                }
                                                val sc = cardProcessedDataModal.getServiceCodeData()
                                                // val sc: String? = ""
                                                var scFirstByte: Char? = null
                                                var scLastbyte: Char? = null
                                                if (null != sc) {
                                                    scFirstByte = sc.first()
                                                    scLastbyte = sc.last()

                                                }
                                                //Checking the card has a PIN or WITHOUTPIN
                                                // Here the changes are , Now we have to ask pin for all swipe txns ...
                                                val isPin = true //scLastbyte == '0' || scLastbyte == '3' || scLastbyte == '5' || scLastbyte == '6' || scLastbyte == '7'
                                                //Here we are bypassing the pin condition for test case ANSI_MAG_001.
                                                //  isPin = false
                                                if (isPin) {
                                                    cardProcessedDataModal.setIsOnline(1)
                                                    cardProcessedDataModal.setPinEntryFlag("1")
                                                } else {
                                                    //0 for no pin
                                                    cardProcessedDataModal.setIsOnline(0)
                                                    cardProcessedDataModal.setPinEntryFlag("0")
                                                }
                                                if (cardProcessedDataModal.getFallbackType() != EFallbackCode.EMV_fallback.fallBackCode) {
                                                    //Checking Fallback
                                                    if (scFirstByte == '2' || scFirstByte == '6') {
                                                        onError(EFallbackCode.Swipe_fallback.fallBackCode, "FallBack")
                                                    } else {
                                                        //region================Condition Check and ProcessSwipeCardWithPinOrWithoutPin:-
                                                        // please check
                                                            /*when (cardProcessedDataModal.getTransType()) {
                                                            TransactionType.SALE.type -> processSwipeCardWithPINorWithoutPIN(isPin, cardProcessedDataModal)
                                                            TransactionType.EMI_SALE.type,
                                                            TransactionType.BRAND_EMI.type,
                                                            ->
                                                            {
                                                                //region==========Implementing Scheme and Offer:-
                                                                cardProcessedDataModal.setEmiTransactionAmount(transactionalAmt)
                                                                if (!TextUtils.isEmpty(cardProcessedDataModal.getPanNumberData())) {
                                                                    GlobalScope.launch(Dispatchers.Main) { (activity as VFTransactionActivity).showProgress();iemv?.stopCheckCard() }
                                                                    GenericEMISchemeAndOffer(activity, cardProcessedDataModal, cardProcessedDataModal.getPanNumberData() ?: "", transactionalAmt,brandEmiData) { bankEMISchemeAndTAndCData, hostResponseCodeAndMessage ->
                                                                        GlobalScope.launch(Dispatchers.Main) {
                                                                            if (hostResponseCodeAndMessage.first) {
                                                                                (activity as VFTransactionActivity).startActivityForResult(
                                                                                    Intent(activity, EMISchemeAndOfferActivity::class.java).apply {
                                                                                        putParcelableArrayListExtra("emiSchemeDataList", bankEMISchemeAndTAndCData.first as ArrayList<out Parcelable>)
                                                                                        putParcelableArrayListExtra("emiTAndCDataList", bankEMISchemeAndTAndCData.second as ArrayList<out Parcelable>)
                                                                                        putExtra("cardProcessedData", cardProcessedDataModal)
                                                                                        putExtra("transactionType",cardProcessedDataModal.getTransType()) // for Transaction Type
                                                                                    },
                                                                                    EIntentRequest.BankEMISchemeOffer.code
                                                                                )
                                                                                (activity as VFTransactionActivity).hideProgress()
                                                                            } else {
                                                                                (activity as VFTransactionActivity).hideProgress()
                                                                                (activity as VFTransactionActivity).alertBoxWithAction(null, null, activity.getString(R.string.error), hostResponseCodeAndMessage.second, false, activity.getString(R.string.positive_button_ok), { (activity as VFTransactionActivity).declinedTransaction() },
                                                                                    {})
                                                                            }

                                                                        }
                                                                    }
                                                                }
                                                                //endregion
                                                            }
                                                            TransactionType.FLEXI_PAY.type -> {
                                                                //region=========This Field is use only in case of flexipay Field58 Transaction Amount:-
                                                                cardProcessedDataModal.setFlexiPayTransactionAmount(transactionalAmt)
                                                                //endregion
                                                                iemv?.startEMV(
                                                                    ConstIPBOC.startEMV.processType.full_process,
                                                                    Bundle(),
                                                                    GenericReadCardData(activity, iemv) { cardBinValue ->
                                                                        iemv?.stopCheckCard()
                                                                        iemv?.abortEMV()
                                                                        val panEncypted = getEncryptedPan(cardBinValue)
                                                                        Log.e(
                                                                            "FLEXI PAY",
                                                                            "CARD BIN ------->> $cardBinValue   ========= ENCHRypted  --->  $panEncypted"
                                                                        )
                                                                        //  cardProcessedDataModal.setTrack2Data(Utility.byte2HexStr(encryptedTrack2ByteArray))
                                                                        if (!TextUtils.isEmpty(panEncypted)) {
                                                                            GlobalScope.launch(Dispatchers.Main) { (activity as VFTransactionActivity).showProgress();iemv?.stopCheckCard() }
                                                                            FlexiPayReqSentServerAndParseData(
                                                                                panEncypted,
                                                                                activity,
                                                                                cardProcessedDataModal,
                                                                                transactionalAmt.toString()
                                                                            ) { flexiPayData, isSucess, hostMsg ->
                                                                                GlobalScope.launch(Dispatchers.Main) {
                                                                                    if (isSucess) {
                                                                                        (activity as VFTransactionActivity).startActivityForResult(
                                                                                            Intent(activity, EMISchemeAndOfferActivity::class.java).apply {
                                                                                                putParcelableArrayListExtra("flexiPayData", flexiPayData as ArrayList<out Parcelable>)
                                                                                                putExtra("cardProcessedData", cardProcessedDataModal)
                                                                                                putExtra("transactionType",cardProcessedDataModal.getTransType()) // for Transaction Type
                                                                                            },
                                                                                            EIntentRequest.FlexiPaySchemeOffer.code
                                                                                        )
                                                                                        (activity as VFTransactionActivity).hideProgress()
                                                                                    } else {
                                                                                        (activity as VFTransactionActivity).hideProgress()
                                                                                        (activity as VFTransactionActivity).alertBoxWithAction(null, null, activity.getString(R.string.error), hostMsg, false, activity.getString(R.string.positive_button_ok), {
                                                                                            (activity as VFTransactionActivity).declinedTransaction()
                                                                                        },
                                                                                            {})
                                                                                    }

                                                                                }
                                                                            }
                                                                        }
                                                                    })
                                                            }
                                                            TransactionType.TEST_EMI.type ->{
                                                                cardProcessedDataModal.setEmiTransactionAmount(transactionalAmt)
                                                                Log.d("emiScheme:- ", Gson().toJson(emiSchemeOfferDataList))
                                                                (activity as VFTransactionActivity).startActivityForResult(
                                                                    Intent(activity, EMISchemeAndOfferActivity::class.java).apply {
                                                                        putParcelableArrayListExtra("emiSchemeDataList",calculateEmi() as? ArrayList<out Parcelable>)
                                                                        putExtra("cardProcessedData", cardProcessedDataModal)
                                                                        putExtra("transactionType",cardProcessedDataModal.getTransType()) // for Transaction Type
                                                                    },
                                                                    EIntentRequest.BankEMISchemeOffer.code
                                                                )
                                                            }
                                                            else -> processSwipeCardWithPINorWithoutPIN(
                                                                isPin, cardProcessedDataModal
                                                            )
                                                        }*/
                                                        //endregion
                                                    }
                                                } else {
                                                    //region================Condition Check and ProcessSwipeCardWithPinOrWithoutPin:-
                                                    // please check
                                                /*when (cardProcessedDataModal.getTransType()) {
                                                        TransactionType.SALE.type -> processSwipeCardWithPINorWithoutPIN(isPin, cardProcessedDataModal)
                                                        TransactionType.EMI_SALE.type,
                                                        TransactionType.BRAND_EMI.type
                                                        -> {
                                                            cardProcessedDataModal.setEmiTransactionAmount(transactionalAmt)
                                                            //region==========Implementing Scheme and Offer:-
                                                            if (!TextUtils.isEmpty(cardProcessedDataModal.getPanNumberData())) {
                                                                GlobalScope.launch(Dispatchers.Main) { (activity as VFTransactionActivity).showProgress();iemv?.stopCheckCard() }
                                                                GenericEMISchemeAndOffer(activity, cardProcessedDataModal, cardProcessedDataModal.getPanNumberData() ?: "", transactionalAmt,brandEmiData) { bankEMISchemeAndTAndCData, hostResponseCodeAndMessage ->
                                                                    GlobalScope.launch(Dispatchers.Main) {
                                                                        if (hostResponseCodeAndMessage.first) {
                                                                            (activity as VFTransactionActivity).startActivityForResult(
                                                                                Intent(activity, EMISchemeAndOfferActivity::class.java).apply {
                                                                                    putParcelableArrayListExtra("emiSchemeDataList", bankEMISchemeAndTAndCData.first as ArrayList<out Parcelable>)
                                                                                    putParcelableArrayListExtra("emiTAndCDataList", bankEMISchemeAndTAndCData.second as ArrayList<out Parcelable>)
                                                                                    putExtra("cardProcessedData", cardProcessedDataModal)
                                                                                    putExtra("transactionType",cardProcessedDataModal.getTransType())
                                                                                },
                                                                                EIntentRequest.BankEMISchemeOffer.code
                                                                            )
                                                                            (activity as VFTransactionActivity).hideProgress()
                                                                        } else {
                                                                            (activity as VFTransactionActivity).hideProgress()
                                                                            (activity as VFTransactionActivity).alertBoxWithAction(
                                                                                null,
                                                                                null,
                                                                                activity.getString(R.string.error),
                                                                                hostResponseCodeAndMessage.second,
                                                                                false,
                                                                                activity.getString(R.string.positive_button_ok),
                                                                                {
                                                                                    (activity as VFTransactionActivity).declinedTransaction()
                                                                                },
                                                                                {})
                                                                        }

                                                                    }
                                                                }
                                                            }
                                                            //endregion
                                                        }
                                                        TransactionType.TEST_EMI.type ->{

                                                            cardProcessedDataModal.setEmiTransactionAmount(transactionalAmt)
                                                            Log.d("emiScheme:- ", Gson().toJson(emiSchemeOfferDataList))
                                                            (activity as VFTransactionActivity).startActivityForResult(
                                                                Intent(activity, EMISchemeAndOfferActivity::class.java).apply {
                                                                    putParcelableArrayListExtra("emiSchemeDataList", calculateEmi() as? ArrayList<out Parcelable> )
                                                                    putExtra("cardProcessedData", cardProcessedDataModal)
                                                                    putExtra("transactionType",cardProcessedDataModal.getTransType()) // for Transaction Type
                                                                },
                                                                EIntentRequest.BankEMISchemeOffer.code
                                                            )
                                                        }
                                                        TransactionType.FLEXI_PAY.type -> {
                                                            //region=========This Field is use only in case of flexipay Field58 Transaction Amount:-
                                                            cardProcessedDataModal.setFlexiPayTransactionAmount(transactionalAmt)
                                                            if (!TextUtils.isEmpty(cardProcessedDataModal.getPanNumberData())) {
                                                                GlobalScope.launch(Dispatchers.Main) { (activity as VFTransactionActivity).showProgress();iemv?.stopCheckCard() }
                                                                FlexiPayReqSentServerAndParseData(getEncryptedPan(cardProcessedDataModal.getPanNumberData()!!)
                                                                    ,
                                                                    activity,
                                                                    cardProcessedDataModal,
                                                                    transactionalAmt.toString()
                                                                ) { flexiPayData, isSucess, hostMsg ->
                                                                    GlobalScope.launch(Dispatchers.Main) {
                                                                        if (isSucess) {
                                                                            (activity as VFTransactionActivity).startActivityForResult(
                                                                                Intent(activity, EMISchemeAndOfferActivity::class.java).apply {
                                                                                    putParcelableArrayListExtra("flexiPayData", flexiPayData as ArrayList<out Parcelable>)
                                                                                    putExtra("cardProcessedData", cardProcessedDataModal)
                                                                                    putExtra("transactionType",cardProcessedDataModal.getTransType()) // for Transaction Type
                                                                                },
                                                                                EIntentRequest.FlexiPaySchemeOffer.code
                                                                            )
                                                                            (activity as VFTransactionActivity).hideProgress()
                                                                        } else {
                                                                            (activity as VFTransactionActivity).hideProgress()
                                                                            (activity as VFTransactionActivity).alertBoxWithAction(null, null, activity.getString(R.string.error), hostMsg, false, activity.getString(R.string.positive_button_ok), {
                                                                                (activity as VFTransactionActivity).declinedTransaction()
                                                                            },
                                                                                {})
                                                                        }

                                                                    }
                                                                }
                                                            }
                                                            //endregion

                                                        }
                                                        else -> processSwipeCardWithPINorWithoutPIN(
                                                            isPin, cardProcessedDataModal
                                                        )
                                                    }*/
                                                    //endregion
                                                }
                                            }
                                            //  }
                                        } else {
                                            // please check
                                            /*onError(
                                                    fallbackType,
                                                    "Try other option + ${EFallbackCode.Swipe_fallback.fallBackCode}"
                                                )*/
                                        }
                                    } else {
                                    // please check
                                    /*(activity as TransactionActivity).handleEMVFallbackFromError(
                                            activity.getString(R.string.card_read_error),
                                            activity.getString(R.string.reinitiate_trans),
                                            false
                                        ) { alertCBBool ->
                                            if (alertCBBool)
                                                try {
                                                    (activity as VFTransactionActivity).declinedTransaction()
                                                } catch (ex: Exception) {
                                                    ex.printStackTrace()
                                                }
                                        }*/
                                        // println("Incorrect Date")
                                    }


                             //   }

                            }catch (ex:Exception){

                            }


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