package com.bonushub.crdb.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.bonushub.crdb.HDFCApplication
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.view.base.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

const val QR_FILE_NAME = "staticQr"
enum class EnumDigiPosProcess(val code: String) {
    InitializeDigiPOS("1"),
    UPIDigiPOS("2"),
    SMS_PAYDigiPOS("5"),
    GET_STATUS("6"),
    TXN_LIST("7"),
    DYNAMIC_QR("3"),
    STATIC_QR("4"),
    TRANSACTION_CALL_BACK("8"),


}

enum class EDigiPosPaymentStatus(val code: Int, val desciption: String) {
    Pending(0, "InProgress"),
    Approved(1, "Success"),
    Failed(2, "SaleFailed"),
    UNKnown(3, "Something went wrong"),
    Fail(4,"Failed"),

}

enum class EnumDigiPosProcessingCode(val code: String) {
    DIGIPOSPROCODE("982003")
}

enum class EDigiPosTerminalStatusResponseCodes(val statusCode: String) {
    SuccessString("Success"),
    FailString("Failed"),
    ActiveString("Active"),
    DeactiveString("Deactive"),
}

enum class LOG_TAG(val tag: String) {
    DIGIPOS("DIGI_POS_TAG")

}

enum class EnumDigiPosTerminalStatusCode(val code: String, val description: String) {
    TerminalStatusCodeE106("E106", "Decryption Failed"),
    TerminalStatusCodeP101("P101", "Invalid Request"),

    // StatusCodeP101("P101","Terminal ID is null or Invalid"),
    TerminalStatusCodeS102("S102", "Failed"),
    TerminalStatusCodeS101("S101", "Success")
}

// retrieve static qr on internal storage
suspend fun loadStaticQrFromInternalStorage(): Bitmap? {
    return withContext(Dispatchers.IO) {
        var bitmap: Bitmap? = null
        val file = HDFCApplication.appContext.filesDir.listFiles()
        file?.filter { it.name == "$QR_FILE_NAME.jpg" }?.map {
            val bytes = it.readBytes()
            bitmap = BitmapUtils.convertCompressedByteArrayToBitmap(bytes)
        }
        bitmap
    }
}

// region
suspend fun uploadPendingDigiPosTxn(activity: BaseActivity, appDao: AppDao, cb: (Boolean) -> Unit){
    try {

//    val digiPosDataList = appDao.getDigiPosDataTableByTxnStatus(EDigiPosPaymentStatus.Pending.desciption) as ArrayList<DigiPosDataTable>
//    if(digiPosDataList.size==0){
//        Log.e("UPLOAD DIGI"," ----------------------->  NO PENDING DIGI POS TXN FOUND ...END")
//        cb(true)
//        return
//    }
//    for(digiPosTabledata in digiPosDataList) {
//        Log.e("TXN ID to upload -->"," ------ID--->  ${digiPosTabledata.partnerTxnId}   --------> Amount----> ${digiPosTabledata.amount} ")
//        val req57 = EnumDigiPosProcess.GET_STATUS.code + "^" + digiPosTabledata.partnerTxnId + "^^"
    // do later kushal
    /*getDigiPosStatus(req57, EnumDigiPosProcessingCode.DIGIPOSPROCODE.code, false)
        { isSuccess, responseMsg, responsef57, fullResponse ->
            try {
                if (isSuccess) {
                    val statusRespDataList = responsef57.split("^")
                    if(statusRespDataList[5]== EDigiPosPaymentStatus.Pending.desciption || statusRespDataList[5]== EDigiPosPaymentStatus.Approved.desciption){
                        val tabledata =
                            DigiPosDataTable()
                        tabledata.requestType =
                            statusRespDataList[0].toInt()
                        //  tabledata.partnerTxnId = statusRespDataList[1]
                        tabledata.status =
                            statusRespDataList[1]
                        tabledata.statusMsg =
                            statusRespDataList[2]
                        tabledata.statusCode =
                            statusRespDataList[3]
                        tabledata.mTxnId =
                            statusRespDataList[4]
                        tabledata.partnerTxnId =
                            statusRespDataList[6]
                        tabledata.transactionTimeStamp =
                            statusRespDataList[7]
                        tabledata.displayFormatedDate =
                            getDateInDisplayFormatDigipos(
                                statusRespDataList[7]
                            )
                        val dateTime =
                            statusRespDataList[7].split(
                                " "
                            )
                        tabledata.txnDate = dateTime[0]
                        tabledata.txnTime = dateTime[1]
                        tabledata.amount =
                            statusRespDataList[8]
                        tabledata.paymentMode =
                            statusRespDataList[9]
                        tabledata.customerMobileNumber =
                            statusRespDataList[10]
                        tabledata.description =
                            statusRespDataList[11]
                        tabledata.pgwTxnId =
                            statusRespDataList[12]

                        when (statusRespDataList[5]) {
                            EDigiPosPaymentStatus.Pending.desciption -> {
                                tabledata.txnStatus =
                                    statusRespDataList[5]
                                ToastUtils.showToast(activity,statusRespDataList[5])
                                *//*DigiPosDataTable.insertOrUpdateDigiposData(
                                    tabledata
                                )*//*
                                runBlocking(Dispatchers.IO) {
                                    appDao.insertDigiPosData(tabledata)
                                }

                                Log.e("UPLOADED PENDING->>", responsef57)
                            }
                            EDigiPosPaymentStatus.Approved.desciption -> {
                                tabledata.txnStatus =
                                    statusRespDataList[5]
                                //DigiPosDataTable.insertOrUpdateDigiposData(tabledata)
                                runBlocking(Dispatchers.IO) {
                                    appDao.insertDigiPosData(tabledata)
                                }
                                logger("UPLOADED SUCCESS->>", ""+responsef57,"e")
                            }
                            else -> {
                                *//*DigiPosDataTable.deletRecord(
                                    tabledata.partnerTxnId
                                )*//*
                                runBlocking(Dispatchers.IO) {
                                    appDao.deleteDigiPosData(tabledata.partnerTxnId)
                                }
                                logger("UPLOAD FAIL->>", ""+responsef57,"e")
                                ToastUtils.showToast(activity,statusRespDataList[5])
                            }
                        }
                    }else{
                        //DigiPosDataTable.deletRecord(digiPosTabledata.partnerTxnId)
                        runBlocking(Dispatchers.IO) {
                            appDao.deleteDigiPosData(digiPosTabledata.partnerTxnId)
                        }
                        logger( "DIGI_POS_TAG","Fail Txn response of Partner id --->  ${digiPosTabledata.partnerTxnId} ","e")
                    }
                } else {
                    logger( "DIGI_POS_TAG","Fail Txn() response of Partner id --->  ${digiPosTabledata.partnerTxnId}  --->  Other than 00 response","e")
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                logger(
                    "DIGI_POS_TAG",
                    "Somethig wrong... in UPLOAD DIGIPOS response data field 57 ","e"
                )
                cb(true)
            }
        }*/
    //}
    Log.e("UPLOAD DIGI"," ----------------------->  END")
    cb(true)

    }catch (ex:java.lang.Exception)
    {
        ex.printStackTrace()
    }
}
// end region

fun getDateInDisplayFormatDigipos(dateStr: String): String {
    //val dateStr = "2021-06-11 11:00:45"//Date()
    return try {
        val ft = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateStr)
        val ft2 = SimpleDateFormat("dd MMM, h:mm aa", Locale.getDefault())
        ft2.format(ft)
    }catch (ex:Exception){
        ""
    }
}
fun getCurrentDateInDisplayFormatDigipos(): String {
    val dNow = Date()
    val fttt = SimpleDateFormat("dd MMM, h:mm aa", Locale.getDefault())
    return fttt.format(dNow)
}

object BitmapUtils {
    /**
     * Converts bitmap to byte array in PNG format
     * @param bitmap source bitmap
     * @return result byte array
     */
    fun convertBitmapToByteArray(bitmap: Bitmap?): ByteArray {
        var baos: ByteArrayOutputStream? = null
        return try {
            baos = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.PNG, 100, baos)
            baos.toByteArray()
        } finally {
            if (baos != null) {
                try {
                    baos.close()
                } catch (e: IOException) {
                    Log.e(
                        BitmapUtils::class.java.simpleName,
                        "ByteArrayOutputStream was not closed"
                    )
                }
            }
        }
    }

    /**
     * Converts bitmap to the byte array without compression
     * @param bitmap source bitmap
     * @return result byte array
     */
    fun convertBitmapToByteArrayUncompressed(bitmap: Bitmap): ByteArray {
        val byteBuffer = ByteBuffer.allocate(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(byteBuffer)
        byteBuffer.rewind()
        return byteBuffer.array()
    }

    /**
     * Converts compressed byte array to bitmap
     * @param src source array
     * @return result bitmap
     */
    fun convertCompressedByteArrayToBitmap(src: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(src, 0, src.size)
    }

    fun getBitmap(byteArr: ByteArray): Bitmap {
        val bmp = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        val buffer = ByteBuffer.wrap(byteArr)
        bmp.copyPixelsFromBuffer(buffer)
        return bmp
    }


}