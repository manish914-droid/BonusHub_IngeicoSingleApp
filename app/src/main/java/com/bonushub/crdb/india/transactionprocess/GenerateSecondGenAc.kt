package com.bonushub.crdb.india.transactionprocess
import android.util.Log
import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.model.local.AppPreference
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.ingenico.EMVInfoUtil
import com.bonushub.crdb.india.utils.ingenico.TLV
import com.bonushub.crdb.india.view.baseemv.EmvHandler
import com.google.gson.Gson
import com.usdk.apiservice.aidl.emv.*
import java.util.*


class SecondGenAcOnNetworkError(var networkErrorSecondGenCB: (Boolean) -> Unit) {

    val iemv: UEMV? = DeviceHelper.getEMV()

    var result: String? = null
    var cardProcessedDataModal: CardProcessedDataModal? = null
    lateinit var testEmvHandler: EmvHandler

    constructor(result: String?,
                cardProcessedDataModal: CardProcessedDataModal?,
                testEmvHandler:EmvHandler,
                networkErrorSecondGenCB: (Boolean) -> Unit):this(networkErrorSecondGenCB) {

        this.cardProcessedDataModal = cardProcessedDataModal
        this.result = result
        this.testEmvHandler = testEmvHandler

    }


    fun generateSecondGenAcForNetworkErrorCase(result: String) {
        //Here Second GenAC performed in Every Network Failure cases or Time out case:-
        Log.d("Failure Data:- ", result)
        when (cardProcessedDataModal?.getReadCardType()) {
            DetectCardType.MAG_CARD_TYPE -> {
                networkErrorSecondGenCB(false)
            }
            DetectCardType.EMV_CARD_TYPE -> {
                //Test case 15 Unable to go Online
                //here 2nd genearte AC

                try {

                        var field55 =  "8A" + "02" + "5A33"

                        val onlineResult = StringBuffer()
                        onlineResult.append(EMVTag.DEF_TAG_ONLINE_STATUS).append("01").append("01")

                        val hostRespCode = "Z3"
                        onlineResult.append(EMVTag.EMV_TAG_TM_ARC).append("02").append(hostRespCode)

                        val onlineApproved = false
                        onlineResult.append(EMVTag.DEF_TAG_AUTHORIZE_FLAG).append("01").append(if (onlineApproved) "01" else "00")

                        val hostTlvData = field55
                        onlineResult.append(
                            TLV.fromData(EMVTag.DEF_TAG_HOST_TLVDATA, BytesUtil.hexString2Bytes(hostTlvData)).toString()
                        )

                        testEmvHandler.SecondGenAcOnNetworkError(this,cardProcessedDataModal)
                        iemv?.respondEvent(onlineResult.toString())

                        // println("Field55 value inside ---> " + Integer.toHexString(ta91) + "0A" + byte2HexStr(mba.toByteArray()) + f71 + f72)

                } catch (ex: java.lang.Exception) {
                    ex.printStackTrace()
                    //println("Exception is" + ex.printStackTrace())
                }

            }
            DetectCardType.CONTACT_LESS_CARD_TYPE -> {
                networkErrorSecondGenCB(false)
            }
            DetectCardType.CONTACT_LESS_CARD_WITH_MAG_TYPE -> {
                networkErrorSecondGenCB(false)
            }
            else -> {
                logger(
                    "CARD_ERROR:- ",
                    cardProcessedDataModal?.getReadCardType().toString(),
                    "e"

                )
                networkErrorSecondGenCB(false)
            }
        }
    }

    fun getEndProcessData(result: Int, transData: TransData?) {

        logger("end txn","call","e")
        logger("end txn","result"+result+"transData"+transData.toString(),"e")

        val aidArray = arrayOf("0x9F06")
        val aidData = iemv!!.getTLV(Integer.toHexString(0x9F06).toUpperCase(Locale.ROOT))
        println("Aid Data is ----> $aidData")

        if (result != EMVError.SUCCESS) {

            when (result) {
                EMVError.ERROR_POWERUP_FAIL -> "ERROR_POWERUP_FAIL"
                EMVError.ERROR_ACTIVATE_FAIL -> "ERROR_ACTIVATE_FAIL"
                EMVError.ERROR_WAITCARD_TIMEOUT -> "ERROR_WAITCARD_TIMEOUT"
                EMVError.ERROR_NOT_START_PROCESS -> "ERROR_NOT_START_PROCESS"
                EMVError.ERROR_PARAMERR -> "ERROR_PARAMERR"
                EMVError.ERROR_MULTIERR -> "ERROR_MULTIERR"
                EMVError.ERROR_CARD_NOT_SUPPORT -> "ERROR_CARD_NOT_SUPPORT"
                EMVError.ERROR_EMV_RESULT_BUSY -> "ERROR_EMV_RESULT_BUSY"
                EMVError.ERROR_EMV_RESULT_NOAPP -> "ERROR_EMV_RESULT_NOAPP"
                EMVError.ERROR_EMV_RESULT_NOPUBKEY -> "ERROR_EMV_RESULT_NOPUBKEY"
                EMVError.ERROR_EMV_RESULT_EXPIRY -> "ERROR_EMV_RESULT_EXPIRY"
                EMVError.ERROR_EMV_RESULT_FLASHCARD -> "ERROR_EMV_RESULT_FLASHCARD"
                EMVError.ERROR_EMV_RESULT_STOP -> "ERROR_EMV_RESULT_STOP"
                EMVError.ERROR_EMV_RESULT_REPOWERICC -> "ERROR_EMV_RESULT_REPOWERICC"
                EMVError.ERROR_EMV_RESULT_REFUSESERVICE -> "ERROR_EMV_RESULT_REFUSESERVICE"
                EMVError.ERROR_EMV_RESULT_CARDLOCK -> "ERROR_EMV_RESULT_CARDLOCK"
                EMVError.ERROR_EMV_RESULT_APPLOCK -> "ERROR_EMV_RESULT_APPLOCK"
                EMVError.ERROR_EMV_RESULT_EXCEED_CTLMT -> "ERROR_EMV_RESULT_EXCEED_CTLMT"
                EMVError.ERROR_EMV_RESULT_APDU_ERROR -> {

                }
                EMVError.ERROR_EMV_RESULT_APDU_STATUS_ERROR -> {
                    //Reversal save To Preference code here.............

                }
                EMVError.ERROR_EMV_RESULT_ALL_FLASH_CARD -> "ERROR_EMV_RESULT_ALL_FLASH_CARD"
                EMVError.EMV_RESULT_AMOUNT_EMPTY -> "EMV_RESULT_AMOUNT_EMPTY"
                else -> "unknow error"
            }

            // System.out.println("=> onEndProcess | " + EMVInfoUtil.getErrorMessage(result))
        } else {
            System.out.println("=> onEndProcess | EMV_RESULT_NORMAL | " + EMVInfoUtil.getTransDataDesc(transData))
            val getAcType: String = EMVInfoUtil.getACTypeDesc(transData!!.acType)

            when (transData.acType) {
                ACType.EMV_ACTION_TC.toByte() -> {
                    val tvrArray = arrayOf("0x95")
                    val tvrData = iemv!!.getTLV(Integer.toHexString(0x95).toUpperCase(Locale.ROOT))
                    println("TVR Data is ----> $tvrData")


                    val tsiArray = arrayOf("0x9B")
                    val tsiData = iemv!!.getTLV(Integer.toHexString(0x9B).toUpperCase(Locale.ROOT))
                    println("TSI Data is ----> $tsiData")


                    val tcvalue = arrayOf("0x9F26")
                    val tcData = iemv!!.getTLV(Integer.toHexString(0x9F26).toUpperCase(Locale.ROOT))
                    println("TC Data is ----> $tcData")

                    networkErrorSecondGenCB(true)

                }
                ACType.EMV_ACTION_AAC.toByte() -> {
                    //Reversal save To Preference code here.............
                    networkErrorSecondGenCB(false)

                }
                else -> {
                    networkErrorSecondGenCB(false)
                }
            }


        }

    }

    fun getErrorMessage(error: Int): String? {
        val message: String
        message = when (error) {
            EMVError.ERROR_POWERUP_FAIL -> "ERROR_POWERUP_FAIL"
            EMVError.ERROR_ACTIVATE_FAIL -> "ERROR_ACTIVATE_FAIL"
            EMVError.ERROR_WAITCARD_TIMEOUT -> "ERROR_WAITCARD_TIMEOUT"
            EMVError.ERROR_NOT_START_PROCESS -> "ERROR_NOT_START_PROCESS"
            EMVError.ERROR_PARAMERR -> "ERROR_PARAMERR"
            EMVError.ERROR_MULTIERR -> "ERROR_MULTIERR"
            EMVError.ERROR_CARD_NOT_SUPPORT -> "ERROR_CARD_NOT_SUPPORT"
            EMVError.ERROR_EMV_RESULT_BUSY -> "ERROR_EMV_RESULT_BUSY"
            EMVError.ERROR_EMV_RESULT_NOAPP -> "ERROR_EMV_RESULT_NOAPP"
            EMVError.ERROR_EMV_RESULT_NOPUBKEY -> "ERROR_EMV_RESULT_NOPUBKEY"
            EMVError.ERROR_EMV_RESULT_EXPIRY -> "ERROR_EMV_RESULT_EXPIRY"
            EMVError.ERROR_EMV_RESULT_FLASHCARD -> "ERROR_EMV_RESULT_FLASHCARD"
            EMVError.ERROR_EMV_RESULT_STOP -> "ERROR_EMV_RESULT_STOP"
            EMVError.ERROR_EMV_RESULT_REPOWERICC -> "ERROR_EMV_RESULT_REPOWERICC"
            EMVError.ERROR_EMV_RESULT_REFUSESERVICE -> "ERROR_EMV_RESULT_REFUSESERVICE"
            EMVError.ERROR_EMV_RESULT_CARDLOCK -> "ERROR_EMV_RESULT_CARDLOCK"
            EMVError.ERROR_EMV_RESULT_APPLOCK -> "ERROR_EMV_RESULT_APPLOCK"
            EMVError.ERROR_EMV_RESULT_EXCEED_CTLMT -> "ERROR_EMV_RESULT_EXCEED_CTLMT"
            EMVError.ERROR_EMV_RESULT_APDU_ERROR -> "ERROR_EMV_RESULT_APDU_ERROR"
            EMVError.ERROR_EMV_RESULT_APDU_STATUS_ERROR -> "ERROR_EMV_RESULT_APDU_STATUS_ERROR"
            EMVError.ERROR_EMV_RESULT_ALL_FLASH_CARD -> "ERROR_EMV_RESULT_ALL_FLASH_CARD"
            EMVError.EMV_RESULT_AMOUNT_EMPTY -> "EMV_RESULT_AMOUNT_EMPTY"
            else -> "unknow error"
        }
        return message + String.format("[0x%02X]", error)
    }

}