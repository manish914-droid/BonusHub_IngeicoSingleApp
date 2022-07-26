
package com.bonushub.crdb.india.transactionprocess

import android.os.DeadObjectException
import android.os.RemoteException
import android.util.Log
import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.model.local.AppPreference
import com.bonushub.crdb.india.model.local.AppPreference.GENERIC_REVERSAL_KEY
import com.bonushub.crdb.india.model.local.AppPreference.ONLINE_EMV_DECLINED
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.ingenico.EMVInfoUtil
import com.bonushub.crdb.india.utils.ingenico.TLV
import com.bonushub.crdb.india.view.baseemv.EmvHandler
import com.bonushub.crdb.india.vxutils.Utility.byte2HexStr
import com.bonushub.crdb.india.vxutils.stringToHex
import com.google.gson.Gson
import com.usdk.apiservice.aidl.emv.*
import java.util.*

class CompleteSecondGenAc constructor(var printExtraDataSB: (Triple<String, String, String>?, String?) -> Unit) {

    companion object {
        val TAG = CompleteSecondGenAc::class.java.simpleName
    }

    var cardProcessedDataModal: CardProcessedDataModal? = null
    lateinit var data: IsoDataReader
    var isoData: IsoDataWriter? = null

    var printData: Triple<String, String, String>? = null

    val iemv: UEMV? = DeviceHelper.getEMV()

    lateinit var testEmvHandler:EmvHandler
    var field56data: String? = null

    constructor(cardProcessedDataModal: CardProcessedDataModal?,
                data: IsoDataReader, isoData: IsoDataWriter? = null,
                testEmvHandler:EmvHandler,
                printExtraDataSB: (Triple<String, String, String>?,String?) -> Unit):this(printExtraDataSB) {
        this.cardProcessedDataModal = cardProcessedDataModal
        this.data = data
        this.isoData = isoData
        //this.printExtraDataSB = printExtraDataSB
        this.testEmvHandler = testEmvHandler

    }

    /*init {
        performSecondGenAc(cardProcessedDataModal,data)
    }*/

    //Below method is used to complete second gen ac in case of EMV Card Type:-
    fun performSecondGenAc(cardProcessedDataModal: CardProcessedDataModal?,data: IsoDataReader) {

        val aidstr = cardProcessedDataModal?.getAID() ?: ""

        val finalaidstr = if(aidstr.isNotBlank()) { aidstr.subSequence(0,10).toString() } else { ""}

        try {
            //   var  cardStatus =  VFService.vfsmartReader?.checkCardStatus()
            //   VFService.showToast("Card status is"+VFService.vfsmartReader?.checkCardStatus())
        }
        catch (ex: DeadObjectException){
            ex.printStackTrace()
        }
        catch (ex: RemoteException){
            ex.printStackTrace()
        }
        catch (ex: Exception){
            ex.printStackTrace()
        }

        var printData: Triple<String, String, String>? = null
        var de55: String?= null
        var tc = false
        val authCode = data.isoMap[38]?.parseRaw2ByteArr() ?: byteArrayOf()
        if (authCode.isNotEmpty()) {
            val ac = authCode.byteArr2Str().replace(" ", "").str2ByteArr()
        }
        val responseCode = (data.isoMap[39]?.parseRaw2String().toString())
        val field55 = data.isoMap[55]?.rawData ?: ""
        // ===================== Dummy field 55 start ===========================
        //910A16F462F8DCDBD7400012720F860D84240000088417ADCFE4D04B81
        //910A55A52CC220D48AEC0014722C9F180430303030860E84DA00CB090767BED29D791A7B70861384DA00C80E0000000000009039CED44D2F36E5
        //910ADE930EAD11D6F1720014
        // ===================== Dummy field 55 end ===========================
        println("Filed55 value is --> $field55")
        val field60Data = data.isoMap[60]?.parseRaw2String().toString()
        val f60DataList = field60Data.split('|')
        //   Auto settle flag | Bank id| Issuer id | MID | TID | Batch No | Stan | Invoice | Card Type
        //0|1|51|000000041501002|41501369|000150|260|000260|RUPAY|
        try {
            var hostBankID = f60DataList[1]
            var hostIssuerID = f60DataList[2]
            var hostMID = f60DataList[3]
            val hostTID = f60DataList[4]
            val hostBatchNumber = f60DataList[5]
            val hostRoc = f60DataList[6]
            val hostInvoice = f60DataList[7]
            var hostCardType = f60DataList[8]

            //val dateTime: Long = Calendar.getInstance().timeInMillis
            //val formatedDate = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault()).format(dateTime)
            //In reversal we have to send Old date and old time means trnsaction date and trnsaction time

            val OldformatedDateTime = AppPreference.getString("OldCurrentDate")+AppPreference.getString("OldCurrentTime")

            field56data = "${hostTID}${hostBatchNumber}${hostRoc}${OldformatedDateTime}${""}${hostInvoice}"



        } catch (ex: Exception) {
            ex.printStackTrace()
            // batchFileData
        }



        val f55Hash = HashMap<Int, String>()
        tlvParser(field55, f55Hash)
        val ta8A = 0x8A
        val ta91 = 0x91
        val resCode = data.isoMap[39]?.rawData ?: "05"
        //responseCode changed to response code by Manish
        //In X990 its responsecode instead of resCode
        val tagData8a = f55Hash[ta8A] ?: resCode
        try {
            if (tagData8a.isNotEmpty()) {
                val ba = tagData8a.hexStr2ByteArr()
                logger(TAG, "On setting ${Integer.toHexString(ta8A)} tag status = $ba.", "e")
            }
        } catch (ex: Exception) {
            logger(TAG, ex.message ?: "", "e")
        }

        val tagDatatag91 = f55Hash[ta91] ?: ""
        val mba = ArrayList<Byte>()
        val mbaCheckSize = ArrayList<Byte>()
        try {
            if (tagDatatag91.isNotEmpty()) {
                val ba = tagDatatag91.hexStr2ByteArr()
                mba.addAll(ba.asList())
                mbaCheckSize.addAll(ba.asList())
                mba.addAll(tagData8a.str2ByteArr().asList())

                logger(TAG, "On setting ${Integer.toHexString(ta91)} tag status = $", "e")
            }
        } catch (ex: Exception) {
            logger(TAG, ex.message ?: "")
        }

        var f71 = f55Hash[0x71] ?: ""
        var f72 = f55Hash[0x72] ?: ""

        val script71 :ByteArray =  try {
            if (f71.isNotEmpty()) {
                var lenStr = Integer.toHexString(f71.length / 2)
                lenStr = addPad(lenStr, "0", 2)

                f71= "${Integer.toHexString(0x71)}$lenStr$f71"
                logger("Field71:- ", "71 = ${f71}")

                f71.hexStr2ByteArr()

            }
            else byteArrayOf()
        } catch (ex: Exception) {
            logger("Exception:- ", ex.message ?: "")
            byteArrayOf()

        }

        val script72 :ByteArray = if (f72.isNotEmpty()) {
            var lenStr = Integer.toHexString(f72.length / 2)
            lenStr = addPad(lenStr, "0", 2)
            f72 = "${Integer.toHexString(0x72)}$lenStr$f72"
            logger("Field72:- ", "72 = ${f72}")
            f72.hexStr2ByteArr()
        } else byteArrayOf()



        try {

            // println("Field55 value inside ---> " + Integer.toHexString(ta91) + "0A" + byte2HexStr(mba.toByteArray()) + f71 + f72)
            println("Field55 value inside ---> " +  Integer.toHexString(ta91) + "0A" + byte2HexStr(mba.toByteArray()) + byte2HexStr(script71) + byte2HexStr(script72))

            //   val field55 =  Integer.toHexString(ta91) + "0A" + byte2HexStr(mba.toByteArray()) + f71 + f72
            val field55ServerResponse =  Integer.toHexString(ta91) + "0A" + byte2HexStr(mba.toByteArray()) + byte2HexStr(script71) + byte2HexStr(script72)

            val onlineResult = StringBuffer()
            onlineResult.append(EMVTag.DEF_TAG_ONLINE_STATUS).append("01").append("00")

            var onlineApproved = false
            var hostRespCode = "3030"
            if(responseCode=="00"){
                hostRespCode="3030"
                onlineApproved=true
                // hexString2String(hostRespCode)
            }else if(responseCode!="00" &&  ((iemv!!.getTLV(Integer.toHexString(0x84))?:"").take(10))==CardAid.VISA.aid){
                cardProcessedDataModal?.txnResponseMsg = data.isoMap[58]?.parseRaw2String()
                hostRespCode="3035"
                onlineApproved=false
            }else{
                hostRespCode= stringToHex(responseCode)
                cardProcessedDataModal?.txnResponseMsg = data.isoMap[58]?.parseRaw2String()
                onlineApproved=false
            }

            onlineResult.append(EMVTag.EMV_TAG_TM_ARC).append("02").append(hostRespCode)// --> 8A
            //  onlineResult.append(EMVTag.EMV_TAG_TM_ISSAUTHDT).append("02").append(hostRespCode)// --> 91
            //  onlineResult.append(EMVTag.EMV_TAG_TM_ISSSCR1).append("02").append(hostRespCode)// --> 71
            //  onlineResult.append(EMVTag.EMV_TAG_TM_ISSSCR2).append("02").append(hostRespCode)// --> 72


            onlineResult.append(EMVTag.DEF_TAG_AUTHORIZE_FLAG).append("01").append(if (onlineApproved) "01" else "00")




            // Here we are passing taf 55 forsecond gen as it is with adding extra Tag 8A in that.


            val newfield55=   field55+EMVTag.EMV_TAG_TM_ARC+"02"+hostRespCode
            if(aidstr==CardAid.AMEX.aid){
                val hostTlvData = field55ServerResponse+EMVTag.EMV_TAG_TM_ARC+"02"+hostRespCode
                println("F55 For second gen ---->   $hostTlvData")
                onlineResult.append(
                    TLV.fromData(EMVTag.DEF_TAG_HOST_TLVDATA, BytesUtil.hexString2Bytes(hostTlvData)).toString()
                )

            }
            else {
                println("F55 For second gen ---->   $newfield55")
                onlineResult.append(
                    TLV.fromData(EMVTag.DEF_TAG_HOST_TLVDATA, BytesUtil.hexString2Bytes(newfield55))
                        .toString()
                )
            }




         //   testEmvHandler.getCompleteSecondGenAc(this,cardProcessedDataModal)
            iemv?.respondEvent(onlineResult.toString())
            // println("Field55 value inside ---> " + Integer.toHexString(ta91) + "0A" + byte2HexStr(mba.toByteArray()) + f71 + f72)

        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
            //println("Exception is" + ex.printStackTrace())
        }

    }

    fun performSecondGenOnFail(cardProcessedDataModal: CardProcessedDataModal?){
        val onlineResult = StringBuffer()
        val hostRespCode = "3035"
        onlineResult.append(EMVTag.EMV_TAG_TM_ARC).append("02").append(hostRespCode)

//        testEmvHandler.getCompleteSecondGenAc(this,cardProcessedDataModal)
        iemv?.respondEvent(onlineResult.toString())

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
                    //Reversal save To Preference code here.............
                    isoData?.mti = "0400"

                    println("1Field56 data in reversal in second ac $field56data")
                    isoData?.apply {
                        additionalData["F56reversal"] = field56data ?: ""
                    }

                    val reversalPacket = Gson().toJson(isoData)
                    //      AppPreference.saveStringReversal(GENERIC_REVERSAL_KEY, reversalPacket)
                    AppPreference.saveString(GENERIC_REVERSAL_KEY, reversalPacket)
                    AppPreference.saveBoolean(ONLINE_EMV_DECLINED, true)

                    printData = Triple("", "", "")
                    printExtraDataSB(printData,"")
                }
                EMVError.ERROR_EMV_RESULT_APDU_STATUS_ERROR -> {
                    //Reversal save To Preference code here.............
                    isoData?.mti = "0400"

                    println("2Field56 data in reversal in second ac $field56data")
                    isoData?.apply {
                        additionalData["F56reversal"] = field56data ?: ""
                    }

                    val reversalPacket = Gson().toJson(isoData)
                    //    AppPreference.saveStringReversal(GENERIC_REVERSAL_KEY, reversalPacket)
                    AppPreference.saveString(GENERIC_REVERSAL_KEY, reversalPacket)
                    AppPreference.saveBoolean(ONLINE_EMV_DECLINED, true)

                    printData = Triple("", "", "")
                    printExtraDataSB(printData,"")
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


                    Log.e("2REVERSAL obj ->",""+isoData.toString())
                    val reversalPacket = Gson().toJson(isoData)
                    Log.e("3REVERSAL obj ->",""+reversalPacket)
                    //     AppPreference.saveStringReversal(GENERIC_REVERSAL_KEY, reversalPacket)
                    AppPreference.saveBoolean(ONLINE_EMV_DECLINED, false)
                    Log.e("4REVERSAL obj ->",""+AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))
                    println(""+AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))

                    cardProcessedDataModal?.setTC(tcData)
                    printData = Triple(tvrData, aidData,tsiData)
                    printExtraDataSB(printData,"")
                }
                ACType.EMV_ACTION_AAC.toByte() -> {
                    //Reversal save To Preference code here.............
                    isoData?.mti = "0400"

                    println("4Field56 data in reversal in second ac $field56data")
                    isoData?.apply {
                        additionalData["F56reversal"] = field56data ?: ""
                    }

                    val reversalPacket = Gson().toJson(isoData)
                    //    AppPreference.saveStringReversal(GENERIC_REVERSAL_KEY, reversalPacket)
                    AppPreference.saveString(GENERIC_REVERSAL_KEY, reversalPacket)
                    AppPreference.saveBoolean(ONLINE_EMV_DECLINED, true)

                    printData = Triple("", "", "")
                    printExtraDataSB(printData,"")
                }
                else -> {
                    printData = Triple("", "", "")
                    printExtraDataSB(printData,"")
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

