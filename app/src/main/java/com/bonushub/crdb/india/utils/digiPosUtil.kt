package com.bonushub.crdb.india.utils

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.bonushub.crdb.india.HDFCApplication
import com.bonushub.crdb.india.db.AppDao
import com.bonushub.crdb.india.model.local.DigiPosDataTable
import com.bonushub.crdb.india.utils.BitmapUtils.convertCompressedByteArrayToBitmap
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.deleteDigiposData
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.insertOrUpdateDigiposData
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.selectDigiPosDataAccordingToTxnStatus
import com.bonushub.crdb.india.view.base.BaseActivity
import com.bonushub.crdb.india.view.base.BaseActivityNew
import com.bonushub.pax.utils.KeyExchanger.Companion.getDigiPosStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

//region===================Const To Used to Determine Which Item is Clicked in DigiPosTXN List Fragment:-
const val GET_TXN_STATUS = "getTXNStatus"
const val SHOW_TXN_DETAIL_PAGE = "showTXNDetailPage"
//endregion

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

fun uploadPendingDigiPosTxn(activity: Activity,cb: (Boolean) -> Unit){
    val digiPosDataList = selectDigiPosDataAccordingToTxnStatus(EDigiPosPaymentStatus.Pending.desciption) as ArrayList<DigiPosDataTable>
    if(digiPosDataList.size==0){
        Log.e("UPLOAD DIGI"," ----------------------->  NO PENDING DIGI POS TXN FOUND ...END")
        cb(true)
        return
    }
    for(digiPosTabledata in digiPosDataList) {
        Log.e("TXN ID to upload -->"," ------ID--->  ${digiPosTabledata.partnerTxnId}   --------> Amount----> ${digiPosTabledata.amount} ")
        val req57 = EnumDigiPosProcess.GET_STATUS.code + "^" + digiPosTabledata.partnerTxnId + "^^"
        getDigiPosStatus(req57, EnumDigiPosProcessingCode.DIGIPOSPROCODE.code, false)
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
                                runBlocking(Dispatchers.Main){
                                    ToastUtils.showToast(activity, statusRespDataList[5])
                                }
                                insertOrUpdateDigiposData(
                                    tabledata
                                )
                                Log.e("UPLOADED PENDING->>", responsef57)
                            }
                            EDigiPosPaymentStatus.Approved.desciption -> {
                                tabledata.txnStatus =
                                    statusRespDataList[5]
                                insertOrUpdateDigiposData(tabledata)
                                Log.e("UPLOADED SUCCESS->>", responsef57)
                            }
                            else -> {
                                deleteDigiposData(
                                    tabledata.partnerTxnId
                                )
                                runBlocking(Dispatchers.Main) {
                                    Log.e("UPLOAD FAIL->>", responsef57)
                                    ToastUtils.showToast(activity, statusRespDataList[5])
                                }
                            }
                        }
                    }else{
                        deleteDigiposData(digiPosTabledata.partnerTxnId)
                        logger( LOG_TAG.DIGIPOS.tag,"Fail Txn response of Partner id --->  ${digiPosTabledata.partnerTxnId} ","e")
                    }
                } else {
                    logger( LOG_TAG.DIGIPOS.tag,"Fail Txn() response of Partner id --->  ${digiPosTabledata.partnerTxnId}  --->  Other than 00 response","e")
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                logger(
                    LOG_TAG.DIGIPOS.tag,
                    "Somethig wrong... in UPLOAD DIGIPOS response data field 57 ","e"
                )
                cb(true)
            }
        }
    }
    Log.e("UPLOAD DIGI"," ----------------------->  END")
    cb(true)
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

suspend fun getStaticQrFromServerAndSaveToFile(activity: Activity, cb: (Boolean) -> Unit) {
    withContext(Dispatchers.Main) {
        (activity as BaseActivityNew).showProgress()
    }
    getDigiPosStatus(
        EnumDigiPosProcess.STATIC_QR.code,
        EnumDigiPosProcessingCode.DIGIPOSPROCODE.code
    ) { isSuccess, responseMsg, responsef57, fullResponse ->
        (activity as BaseActivityNew).hideProgress()
        try {
            if (isSuccess) {
                val respDataList = responsef57.split("^")
//reqest type, parterid,status,statusmsg,statuscode,qrLink,QrBlob
                val tabledata = DigiPosDataTable()
                tabledata.requestType = respDataList[0].toInt()
                tabledata.partnerTxnId = respDataList[1]
                tabledata.status = respDataList[2]
                tabledata.statusMsg = respDataList[3]
                tabledata.statusCode = respDataList[4]
                val qrLink = respDataList[5]
                val responseIsoData: IsoDataReader = readIso(fullResponse, false)

                Log.e(
                    "BitmapHexString-->  ",
                    responseIsoData.isoMap[59]?.rawData.toString() + "---->"
                )
                val blobHexString = responseIsoData.isoMap[59]?.rawData.toString()
                //   val blobHexString = respDataList[6]
                val byteArray = blobHexString.decodeHexStringToByteArray()
                val bmp = convertCompressedByteArrayToBitmap(byteArray)
                if (saveStaticQrToInternalStorage(bmp)) {
                    logger("StaticQr", "Successfully save qr Bitmap to file", "e")
                    cb(true)
                } else {
                    logger("StaticQr", "Not saved qr Bitmap to file", "e")
                    cb(false)
                }
            } else {
                logger("StaticQr", "Fail from server", "e")
                cb(false)
            }
        } catch (ex: Exception) {
            cb(false)
            ex.printStackTrace()
        }
    }

}

// saving static qr on internal storage
fun saveStaticQrToInternalStorage(bmp: Bitmap): Boolean {
    return try {
        HDFCApplication.appContext.openFileOutput("$QR_FILE_NAME.jpg", Context.MODE_PRIVATE)
            .use { stream ->
                if (!bmp.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                    throw IOException("Could'nt save bitmap")
                }
            }
        true
    } catch (ex: IOException) {
        ex.printStackTrace()
        false
    }
}