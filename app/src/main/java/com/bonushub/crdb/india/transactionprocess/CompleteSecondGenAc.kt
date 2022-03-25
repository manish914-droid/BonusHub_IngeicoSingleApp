
package com.bonushub.crdb.india.transactionprocess

import android.os.Bundle
import android.os.DeadObjectException
import android.os.RemoteException
import android.text.TextUtils
import android.util.Log
import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.ingenico.TLV
import com.bonushub.crdb.india.vxutils.Utility.byte2HexStr
import com.usdk.apiservice.aidl.emv.EMVTag
import com.usdk.apiservice.aidl.emv.UEMV
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class CompleteSecondGenAc(var cardProcessedDataModal: CardProcessedDataModal?,
                          var data: IsoDataReader, var isoData: IsoDataWriter? = null,
                          var printExtraDataSB: (Triple<String, String, String>?,String?) -> Unit) {

    val iemv: UEMV? = DeviceHelper.getEMV()

    init {
        performSecondGenAc(cardProcessedDataModal,data)
    }

    //Below method is used to complete second gen ac in case of EMV Card Type:-
    private fun performSecondGenAc(cardProcessedDataModal: CardProcessedDataModal?,data: IsoDataReader) {

        var field56data: String? = null
        var aidstr = cardProcessedDataModal?.getAID() ?: ""

        val finalaidstr = if(aidstr.isNotBlank()) { aidstr.subSequence(0,10).toString() } else { aidstr = ""}

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
        val field55 = data.isoMap[55]?.rawData ?: ""//910A16F462F8DCDBD7400012720F860D84240000088417ADCFE4D04B81
        //  910A55A52CC220D48AEC0014722C9F180430303030860E84DA00CB090767BED29D791A7B70861384DA00C80E0000000000009039CED44D2F36E5
        println("Filed55 value is --> $field55")//910ADE930EAD11D6F1720014
        //  VFService.showToast("Field 55 value is"+field55)
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
           // val OldformatedDateTime = AppPreference.getString("OldCurrentDate")+AppPreference.getString("OldCurrentTime")

        //    field56data = "${hostTID}${hostBatchNumber}${hostRoc}${OldformatedDateTime}${""}${hostInvoice}"



        } catch (ex: Exception) {
            ex.printStackTrace()
            // batchFileData
        }



        val f55Hash = HashMap<Int, String>()
        tlvParser(field55, f55Hash)
        val ta8A = 0x8A
        val ta91 = 0x91
        val resCode = data.isoMap[39]?.rawData ?: "05"
        //  VFService.showToast("Response code is"+responseCode)
        val tagData8a = f55Hash[ta8A] ?: responseCode
        try {
            if (tagData8a.isNotEmpty()) {
                val ba = tagData8a.hexStr2ByteArr()
                // rtn = EMVCallback.EMVSetTLVData(ta.toShort(), ba, ba.size)
               // logger(VFTransactionActivity.TAG, "On setting ${Integer.toHexString(ta8A)} tag status = $", "e")
            }
        } catch (ex: Exception) {
          //  logger(VFTransactionActivity.TAG, ex.message ?: "", "e")
        }

        val tagDatatag91 = f55Hash[ta91] ?: ""
        //  mDevCltr.mEmvState.tc = tagDatatag91.hexStr2ByteArr()
        val mba = ArrayList<Byte>()
        val mba1 = ArrayList<Byte>()
        try {
            if (tagDatatag91.isNotEmpty()) {
                val ba = tagDatatag91.hexStr2ByteArr()
                mba.addAll(ba.asList())
                mba1.addAll(ba.asList())
                //
                mba.addAll(tagData8a.str2ByteArr().asList())

                //rtn = EMVCallback.EMVSetTLVData(ta.toShort(), mba.toByteArray(), mba.size)
                logger("Data:- ", "On setting ${Integer.toHexString(ta91)} tag status = $", "e")
            }
        } catch (ex: Exception) {
            logger("Exception:- ", ex.message ?: "")
        }

        var f71 = f55Hash[0x71] ?: ""
        var f72 = f55Hash[0x72] ?: ""

        try {
            val script71 = if (f71.isNotEmpty()) {
                var lenStr = Integer.toHexString(f71.length / 2)
                lenStr = addPad(lenStr, "0", 2)

                f71= "${Integer.toHexString(0x71)}$lenStr$f71"
                f71.hexStr2ByteArr()
                //     rtn = EMVCallback.EMVSetTLVData(ta.toShort(), ba, ba.size)
                logger("Exception:- ", "On setting ${Integer.toHexString(0x71)} tag status = $")
            }
            else byteArrayOf()
        } catch (ex: Exception) {
            logger("Exception:- ", ex.message ?: "")
        }

        val script72 = if (f72.isNotEmpty()) {
            var lenStr = Integer.toHexString(f72.length / 2)
            lenStr = addPad(lenStr, "0", 2)

            f72 = "${Integer.toHexString(0x72)}$lenStr$f72"
            logger("Field72:- ", "72 = $f72")
            f72.hexStr2ByteArr()
        } else byteArrayOf()

        //   val finalRet = EMVCallback.EMVCompleteTrans(resResult, script, script.size, acType)

        try {

                if (field55 != null ) {
                    println("Field55 value inside ---> " + Integer.toHexString(ta91) + "0A" + byte2HexStr(mba.toByteArray()) + f71 + f72)

                    var field55 =  Integer.toHexString(ta91) + "0A" + byte2HexStr(mba.toByteArray()) + f71 + f72

                    val onlineResult = StringBuffer()
                    onlineResult.append(EMVTag.DEF_TAG_ONLINE_STATUS).append("01").append("00")

                    val hostRespCode = "3030"
                    onlineResult.append(EMVTag.EMV_TAG_TM_ARC).append("02").append(hostRespCode)

                    val onlineApproved = true
                    onlineResult.append(EMVTag.DEF_TAG_AUTHORIZE_FLAG).append("01").append(if (onlineApproved) "01" else "00")

                    val hostTlvData = field55
                    onlineResult.append(
                        TLV.fromData(EMVTag.DEF_TAG_HOST_TLVDATA, BytesUtil.hexString2Bytes(hostTlvData)).toString()
                    )


                      iemv?.respondEvent(onlineResult.toString())

                   // println("Field55 value inside ---> " + Integer.toHexString(ta91) + "0A" + byte2HexStr(mba.toByteArray()) + f71 + f72)
                }




        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
            //println("Exception is" + ex.printStackTrace())
        }
        if (tc)
            printExtraDataSB(printData,de55)
        else {
            printData = Triple("", "", "")
            printExtraDataSB(printData,de55)
        }
    }
}

