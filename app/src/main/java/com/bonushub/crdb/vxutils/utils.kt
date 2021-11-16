package com.bonushub.crdb.utils

import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.vxutils.Utility.*
import com.usdk.apiservice.aidl.BaseError
import com.usdk.apiservice.aidl.algorithm.AlgError
import com.usdk.apiservice.aidl.algorithm.AlgMode
import com.usdk.apiservice.aidl.algorithm.UAlgorithm
import com.usdk.apiservice.aidl.data.BytesValue
import com.usdk.apiservice.aidl.pinpad.*
import java.nio.charset.StandardCharsets

//Below method is used to encrypt Pannumber data:-
fun getEncryptedPan(panNumber: String, algorithm: UAlgorithm?): String {
    val encryptedByteArray: ByteArray?

    var dataDescription = "02|$panNumber"
    val dataLength = dataDescription.length
    val DIGIT_8 = 8
    if (dataLength >= DIGIT_8) {
        val mod = dataLength % DIGIT_8
        if (mod != 0) {
            val padding = DIGIT_8 - mod
            val totalLength = dataLength + padding
            dataDescription = addPad(dataDescription, " ", totalLength, false)
        }
        logger("Field 56", " -->$dataDescription", "e")
        val byteArray = dataDescription.toByteArray(StandardCharsets.ISO_8859_1)
        val result = TDES(algorithm,dataDescription)

        return result
    } else return "TRACK57_LENGTH<8"

}

//Below method is used to encrypt track2 data:-
fun getEncryptedTrackData(track2Data: String?,pinpad: UPinpad?): String? {
    var encryptedbyteArrrays: ByteArray? = null
    if (null != track2Data) {
          var track21 = "35|" + track2Data.replace("D", "=").replace("F", "")
        println("Track 2 data is$track21")
        val DIGIT_8 = 8

        val mod = track21.length % DIGIT_8
        if (mod != 0) {
            track21 = getEncryptedField57DataForVisa(track21.length, track21)
        }

        val byteArray = track21.toByteArray(StandardCharsets.ISO_8859_1)
        val mode = MagTrackEncMode.MTEM_ECBMODE
        try {
            encryptedbyteArrrays = pinpad?.encryptMagTrack(mode, 0, byteArray)
            if (encryptedbyteArrrays == null) {
                // outputPinpadError("encryptMagTrack fail")
                return ""
            }
        }
        catch (ex: Exception){
            ex.printStackTrace()
        }


        println("Track 2 with encyption is --->" + byte2HexStr(encryptedbyteArrrays))
    }

    return byte2HexStr(encryptedbyteArrrays)
}

fun getEncryptedField57DataForVisa(dataLength: Int, dataDescription: String): String {
    var dataDescription = dataDescription
    val encryptedByteArray: ByteArray?
    val DIGIT_8 = 8
    if (dataLength > DIGIT_8) {
        val mod = dataLength % DIGIT_8
        if (mod != 0) {
            val padding = DIGIT_8 - mod
            val totalLength = dataLength + padding
            dataDescription = addPad(dataDescription, " ", totalLength, false)
        }
        return dataDescription
    } else return "TRACK57_LENGTH<8"
}

private fun TDES(algorithm: UAlgorithm?, dataDescription: String): String {

   return try {

       // logger(TAG, "=> TDES")
        val key: ByteArray = BytesUtil.hexString2Bytes(AppPreference.getString("dpk"))
        val strtohex = dataDescription.str2ByteArr().byteArr2HexStr()
       var desMode = DESMode(DESMode.DM_ENC, DESMode.DM_OM_TECB)
       var  encResult = DeviceHelper.getPinpad(KAPId(0, 0), 0, DeviceName.IPP)?.calculateDes(11, desMode, null, BytesUtil.hexString2Bytes(strtohex))
       if (encResult == null) {


       }
       println("TECB encrypt result = " + byte2HexStr(encResult))

       desMode = DESMode(DESMode.DM_DEC, DESMode.DM_OM_TECB)
       val decResult: ByteArray? = DeviceHelper.getPinpad(KAPId(0, 0), 0, DeviceName.IPP)?.calculateDes(11, desMode, null, encResult)
       if (decResult == null) {
         //  outputPinpadError("calculateDes fail",pinPad)
           // return
       }
       println("TECB decrypt result = " + byte2HexStr(decResult))

        return byte2HexStr(encResult)

    } catch (ex: Exception) {
        // handleException(e)
        return ex.message.toString()
    }
}

fun getErrorDetail(error: Int): String {
    val message = getErrorMessage(error)
    return if (error < 0) {
        "$message[$error]"
    } else message + String.format("[0x%02X]", error)
}

fun getErrorMessage(error: Int): String {
    val message: String
    message = when (error) {
        BaseError.SERVICE_CRASH -> "SERVICE_CRASH"
        BaseError.REQUEST_EXCEPTION -> "REQUEST_EXCEPTION"
        BaseError.ERROR_CANNOT_EXECUTABLE -> "ERROR_CANNOT_EXECUTABLE"
        BaseError.ERROR_INTERRUPTED -> "ERROR_INTERRUPTED"
        BaseError.ERROR_HANDLE_INVALID -> "ERROR_HANDLE_INVALID"
        else -> "Unknown error"
    }
    return message
}

//region=========================Divide Amount by 100 and Return Back:-
fun divideAmountBy100(amount: Int = 0): Double {
    return if (amount != 0) {
        val divideAmount = amount.toDouble()
        divideAmount.div(100)
    } else
        0.0
}
//endregion








