package com.bonushub.crdb.utils

import android.util.Log
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.view.base.BaseActivity
import java.text.SimpleDateFormat
import java.util.*

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