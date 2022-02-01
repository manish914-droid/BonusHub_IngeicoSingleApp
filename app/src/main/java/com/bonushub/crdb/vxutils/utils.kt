package com.bonushub.crdb.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bonushub.crdb.BuildConfig
import com.bonushub.crdb.R
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.model.local.*
import com.bonushub.crdb.model.remote.BrandEMIProductDataModal
import com.bonushub.crdb.utils.Field48ResponseTimestamp.getTptData
import com.bonushub.crdb.utils.Field48ResponseTimestamp.getTptDataByLinkTidType
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.fragments.DashboardFragment
import com.bonushub.crdb.vxutils.BHTextView
import com.bonushub.crdb.vxutils.Utility.*
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.ingenico.hdfcpayment.listener.OnOperationListener
import com.ingenico.hdfcpayment.model.ReceiptDetail
import com.ingenico.hdfcpayment.request.CommRequest
import com.ingenico.hdfcpayment.request.TerminalInitializationRequest
import com.ingenico.hdfcpayment.response.OperationResult
import com.ingenico.hdfcpayment.response.TerminalInitializationResponse
import com.ingenico.hdfcpayment.type.RequestStatus
import com.usdk.apiservice.aidl.BaseError
import com.usdk.apiservice.aidl.pinpad.*
import kotlinx.coroutines.*
import java.io.*
import java.lang.Runnable
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private var listofTids= ArrayList<String>()
private var listofTxnTids= ArrayList<String>()
private var listofReversalTids= ArrayList<String>()

//Below method is used to encrypt Pannumber data:-
fun getEncryptedPanorTrackData(panNumber: String,isTrackData: Boolean): String {
    val encryptedByteArray: ByteArray?
    var dataDescription: String? = null
    if(isTrackData){
       dataDescription = panNumber
    }
    else{
       dataDescription = "02|$panNumber"
    }
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
        val result = calculateDes(dataDescription)

        return result
    } else return "TRACK57_LENGTH<8"

}

fun getEncryptedDataForSyncing(panNum:String,receiptData: ReceiptDetail):String{
    //  02,36|PAN Number |card holder name~Application label~CardIssuerCountryCode~mode of txn~pin entry type

  /*  mode of txn- 3 for CTLS, emv-2, swipe 1
    Pin entry type - 1 for online pin and 2 for offline pin and 0 signature
*/
    var pinentry=0
    pinentry = if(receiptData.isVerifyPin == true){
        1
    }else{
        2
    }

    var  dataDescription = "02,36|$panNum|${receiptData.cardHolderName}~${receiptData.appName}~~2~${pinentry}"

   logger("F57_PLAIN",dataDescription,"e")
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

        return calculateDes(dataDescription)
    } else return "TRACK57_LENGTH<8"


}


private fun calculateDes(dataDescription: String): String {

   return try {

       // logger(TAG, "=> TDES")
        val key: ByteArray = BytesUtil.hexString2Bytes(AppPreference.getString("dpk"))
        val strtohex = dataDescription.str2ByteArr().byteArr2HexStr()
       var desMode = DESMode(DESMode.DM_ENC, DESMode.DM_OM_TECB)
       var  encResult = DeviceHelper.getPinpad(KAPId(0, 0), 0, DeviceName.IPP)?.calculateDes(DemoConfig.KEYID_DES, desMode, null, BytesUtil.hexString2Bytes(strtohex))
       if (encResult == null) {
          println("Calculate encrypt fail"+encResult)

       }
       println("TECB encrypt result = " + byte2HexStr(encResult))

       desMode = DESMode(DESMode.DM_DEC, DESMode.DM_OM_TECB)
       val decResult: ByteArray? = DeviceHelper.getPinpad(KAPId(0, 0), 0, DeviceName.IPP)?.calculateDes(DemoConfig.KEYID_DES, desMode, null, encResult)
       if (decResult == null) {
           println("")
           println("Calculate decrypt fail"+encResult)
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


//Below method is used to show Pop-Up in case of Sale and Bank EMI to enter either Mobile Number or Bill Number on Condition Base:-
fun showMobileBillDialog(
    context: Context?,
    transactionType: Int,
    brandEMIDataModal: BrandEMIProductDataModal? = null,
    dialogCB: (Triple<String, String, Boolean>) -> Unit
) {
    runBlocking(Dispatchers.Main) {
        val dialog = context?.let { Dialog(it) }
        val inflate = LayoutInflater.from(context).inflate(R.layout.mobile_bill_dialog_view, null)
        dialog?.setContentView(inflate)
        dialog?.setCancelable(false)
        dialog?.window?.attributes?.windowAnimations = R.style.DialogAnimation
        val window = dialog?.window
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        val mobileNumberET: TextInputEditText? = dialog?.findViewById(R.id.mobileNumberET)
        val billNumberET: TextInputEditText? = dialog?.findViewById(R.id.billNumberET)
        val billNumberTil: TextInputLayout? = dialog?.findViewById(R.id.bill_number_til)
        val cancelButton: Button? = dialog?.findViewById(R.id.cancel_btn)
        val okButton: Button? = dialog?.findViewById(R.id.ok_btn)
        val tpt = getTptData()

        when (transactionType) {
            BhTransactionType.BRAND_EMI.type -> {
                //Hide Mobile Number Field:-
                if (brandEMIDataModal?.validationTypeName?.substring(0, 1) == "0") {
                    mobileNumberET?.visibility = View.GONE
                } else
                    mobileNumberET?.visibility = View.VISIBLE
                //Hide Invoice Number Field:-
                if (brandEMIDataModal?.validationTypeName?.substring(2, 3) == "0") {
                    billNumberTil?.visibility = View.GONE
                } else
                    billNumberTil?.visibility = View.VISIBLE
            }
            else -> {
                if ((tpt?.reservedValues?.substring(
                        2,
                        3
                    ) == "1" && transactionType == BhTransactionType.EMI_SALE.type)
                )
                    billNumberTil?.visibility = View.VISIBLE
                else
                    billNumberTil?.visibility = View.GONE
            }
        }


        //Cancel Button OnClick:-
        cancelButton?.setOnClickListener {
            dialog.dismiss()
            dialogCB(Triple("", "", third = false))
        }

        //Ok Button OnClick:-
        okButton?.setOnClickListener {
            when (transactionType) {
                //region=====================Brand EMI Validation:-
                BhTransactionType.BRAND_EMI.type -> {
                    if (brandEMIDataModal?.validationTypeName
                            ?.substring(0, 1) == "1" && brandEMIDataModal.validationTypeName
                            ?.substring(2, 3) == "1"
                    ) {
                        dialog.dismiss()
                        dialogCB(
                            Triple(
                                mobileNumberET?.text.toString(),
                                billNumberET?.text.toString(),
                                third = true
                            )
                        )
                    } else if (brandEMIDataModal?.validationTypeName
                            ?.substring(0, 1) == "1" && brandEMIDataModal.validationTypeName
                            ?.substring(2, 3)?.toInt() ?: 0 > "1".toInt()
                    ) {
                        if (!TextUtils.isEmpty(billNumberET?.text.toString())) {
                            dialog.dismiss()
                            dialogCB(
                                Triple(
                                    mobileNumberET?.text.toString(),
                                    billNumberET?.text.toString(),
                                    third = true
                                )
                            )
                        } else {
                             ToastUtils.showToast(context,context.getString(R.string.enter_valid_bill_number))
                        }
                    } else if (brandEMIDataModal?.validationTypeName
                            ?.substring(2, 3) == "1" && brandEMIDataModal.validationTypeName
                            ?.substring(0, 1)?.toInt() ?: 0 > "1".toInt()
                    ) {
                        if (!TextUtils.isEmpty(mobileNumberET?.text.toString()) && mobileNumberET?.text.toString().length in 10..13) {
                            dialog.dismiss()
                            dialogCB(
                                Triple(
                                    mobileNumberET?.text.toString(),
                                    billNumberET?.text.toString(),
                                    third = true
                                )
                            )
                        } else {
                             ToastUtils.showToast(context,context.getString(R.string.enter_valid_mobile_number))
                        }
                    } else {
                        when {
                            TextUtils.isEmpty(mobileNumberET?.text.toString()) || mobileNumberET?.text.toString().length !in 10..13 ->
                                 ToastUtils.showToast(context,context.getString(R.string.enter_valid_mobile_number))
                            TextUtils.isEmpty(billNumberET?.text.toString()) && (brandEMIDataModal?.validationTypeName
                                ?.substring(
                                    0,
                                    1
                                ) == "1" && brandEMIDataModal.validationTypeName
                                ?.substring(2, 3)
                                ?.toInt() ?: 0 > "1".toInt()) ->  ToastUtils.showToast(context,
                                context.getString(
                                    R.string.enter_valid_bill_number
                                )
                            )
                            else -> {
                                dialog.dismiss()
                                dialogCB(
                                    Triple(
                                        mobileNumberET?.text.toString(),
                                        billNumberET?.text.toString(),
                                        third = true
                                    )
                                )
                            }
                        }
                    }
                }
                //endregion

                //region Other Transaction Validation:-
                else -> {
                    if (transactionType == BhTransactionType.SALE.type && tpt?.reservedValues?.substring(
                            0,
                            1
                        ) == "1"
                    ) {
                        when {
                            //  when mobile number entered
                            !TextUtils.isEmpty(mobileNumberET?.text.toString()) -> if (mobileNumberET?.text.toString().length in 10..13) {
                                dialog.dismiss()
                                dialogCB(Triple(mobileNumberET?.text.toString(), "", third = true))
                            } else
                                 ToastUtils.showToast(context,context.getString(R.string.enter_valid_mobile_number))
                            TextUtils.isEmpty(mobileNumberET?.text.toString()) -> {
                                dialog.dismiss()
                                dialogCB(Triple("", "", third = true))
                            }
                        }
                    } else if (transactionType == BhTransactionType.EMI_SALE.type && tpt?.reservedValues?.substring(
                            1,
                            2
                        ) == "1" && tpt.reservedValues.substring(2, 3) == "1"
                    ) {
                        when {
                            !TextUtils.isEmpty(mobileNumberET?.text.toString()) && !TextUtils.isEmpty(
                                billNumberET?.text.toString()
                            ) -> if (mobileNumberET?.text.toString().length in 10..13) {
                                dialog.dismiss()
                                dialogCB(
                                    Triple(
                                        mobileNumberET?.text.toString(),
                                        billNumberET?.text.toString(),
                                        third = true
                                    )
                                )
                            } else
                                 ToastUtils.showToast(context,context.getString(R.string.enter_valid_mobile_number))

                            !TextUtils.isEmpty(mobileNumberET?.text.toString()) -> if (mobileNumberET?.text.toString().length in 10..13) {
                                dialog.dismiss()
                                dialogCB(Triple(mobileNumberET?.text.toString(), "", third = true))
                            } else
                                 ToastUtils.showToast(context,context.getString(R.string.enter_valid_mobile_number))

                            !TextUtils.isEmpty(billNumberET?.text.toString()) -> {
                                dialog.dismiss()
                                dialogCB(Triple("", billNumberET?.text.toString(), third = true))
                            }

                            TextUtils.isEmpty(mobileNumberET?.text.toString()) && TextUtils.isEmpty(
                                mobileNumberET?.text.toString()
                            ) -> {
                                dialog.dismiss()
                                dialogCB(Triple("", "", third = true))
                            }
                        }
                    } else if (transactionType == BhTransactionType.EMI_SALE.type && tpt?.reservedValues?.substring(
                            1,
                            2
                        ) == "1"
                    ) {
                        when {
                            !TextUtils.isEmpty(mobileNumberET?.text.toString()) -> if (mobileNumberET?.text.toString().length in 10..13) {
                                dialog.dismiss()
                                dialogCB(Triple(mobileNumberET?.text.toString(), "", third = true))
                            } else
                                 ToastUtils.showToast(context,context.getString(R.string.enter_valid_mobile_number))
                            TextUtils.isEmpty(mobileNumberET?.text.toString()) -> {
                                dialog.dismiss()
                                dialogCB(Triple("", "", third = true))
                            }
                        }
                    } else if (transactionType == BhTransactionType.EMI_SALE.type && tpt?.reservedValues?.substring(
                            2,
                            3
                        ) == "1"
                    ) {
                        when {
                            !TextUtils.isEmpty(billNumberET?.text.toString()) -> {
                                dialog.dismiss()
                                dialogCB(Triple("", billNumberET?.text.toString(), third = true))
                            }
                            TextUtils.isEmpty(billNumberET?.text.toString()) -> {
                                dialog.dismiss()
                                dialogCB(Triple("", "", third = true))
                            }
                        }
                    } else if (transactionType == BhTransactionType.EMI_ENQUIRY.type) {
                        when {
                            !TextUtils.isEmpty(mobileNumberET?.text.toString()) -> if (mobileNumberET?.text.toString().length in 10..13) {
                                dialog.dismiss()
                                dialogCB(Triple(mobileNumberET?.text.toString(), "", third = true))
                            } else
                                 ToastUtils.showToast(context,context.getString(R.string.enter_valid_mobile_number))
                        }

                    } else {
                        dialog.dismiss()
                        dialogCB(Triple("", "", third = true))
                    }
                }
                //endregion
            }

        }
        dialog?.show()

    }
}

//To check Reversal
suspend fun checkReversal(batchData: MutableList<BatchTableReversal>): ArrayList<String> {
    listofReversalTids.clear()

    val distinctByNameCusts = batchData.distinctBy{it -> it.receiptData?.tid}
    distinctByNameCusts.forEach{ it ->
        listofReversalTids.add(it.receiptData?.tid ?: "")
    }
    return listofReversalTids
}

//To check Initiaization Status
suspend fun checkSettlementTid(batchData: MutableList<BatchTable>): ArrayList<String> {
    listofTxnTids.clear()

    val distinctByNameCusts = batchData.distinctBy{it -> it.receiptData?.tid}
    distinctByNameCusts.forEach{ it ->
        listofTxnTids.add(it.receiptData?.tid ?: "")
    }
    return listofTxnTids
}



//To check Initiaization Status
suspend fun checkBaseTid(appDao: AppDao): ArrayList<String> {
    listofTids.clear()
    val tpt = appDao.getAllTerminalParameterTableData()

    tpt.forEachIndexed { index, terminalParameterTable ->
        if(terminalParameterTable?.tidType == "1"){
            listofTids.add(0,terminalParameterTable.terminalId)
        }
        else if(terminalParameterTable?.tidType != "-1"){
            listofTids.add(terminalParameterTable?.terminalId ?: "")
        }
    }

    //Have to change again
  /*  tpt[0]?.tidType?.forEachIndexed { index, tidType ->
        //Have to change again
        if(tidType == "1"){
            tpt[0]?.terminalId?.get(index)
            listofTids.add(0,tpt[0]?.terminalId?.get(index) ?: "")
        }
        else{
            listofTids.add(tpt[0]?.terminalId?.get(index) ?: "")
        }
    }*/
    return listofTids
}

suspend fun getBaseTID(appDao: AppDao):String{
    val tpt = appDao.getAllTerminalParameterTableData()
    var tid=""

    tpt.forEachIndexed { index, terminalParameterTable ->
        if(terminalParameterTable?.tidType == "1"){
            tid = terminalParameterTable.terminalId
        }

    }

    //Have to change again
  /*  tpt[0]?.tidType?.forEachIndexed { index, tidType ->
        //Have to change again
        if (tidType == "1") {
            tid = tpt[0]?.terminalId?.get(index).toString()
        }
    }*/
    return tid

}

//To check Initiaization Status
suspend fun updateBaseTid(appDao: AppDao, updatedTid:String): ArrayList<String> {
    listofTids.clear()
    var tpt = appDao?.getAllTerminalParameterTableData()
    tpt.forEachIndexed { index, terminalParameterTable ->
        if(terminalParameterTable?.tidType.equals("1")){
            listofTids.add(updatedTid)
        }
        else if(terminalParameterTable?.tidType != "-1"){
            listofTids.add(terminalParameterTable?.terminalId ?: "")
        }
    }

    //Have to change again
 /*   tpt[0]?.tidType?.forEachIndexed { index, tidType ->
        //Have to change again
        if(tidType.equals("1")){
            tpt[0]?.terminalId?.get(index)
            listofTids.add(updatedTid)
        }
        else{
            listofTids.add(tpt[0]?.terminalId?.get(index) ?: "")
        }
    }*/
    return listofTids
}

//To check Initiaization Status
suspend fun checkTidUpdate(appDao: AppDao): Boolean {
    var isdiffTid: Boolean = false
    val result = appDao.getIngenicoInitialization()
    var tpt = appDao?.getAllTerminalParameterTableData()
    val rseultsize = result?.size
    if (rseultsize != null) {
        if(rseultsize > 0){
            result.forEach { IngenicoInitialization ->
                val tidList = IngenicoInitialization?.tidList
                //Have to change again
                //here size -1 becz dummy tpt record come from assets
                if(tidList?.size == tpt.size-1){

                    isdiffTid = true
                    return isdiffTid
                }
                //here size -1 becz dummy tpt record come from assets
                else if(tidList?.size.isGreaterThan(tpt.size-1)){

                    var strstatus = ArrayList<String>()
                    tpt.forEach {
                        if(it?.tidType !="-1")
                            strstatus.add("Success")
                    }

                    var arrayListTid = ArrayList<String>()
                    tpt.forEach {
                        if(it?.tidType !="-1")
                            arrayListTid.add(it?.terminalId ?: "")
                    }

                    var ingenicoInitialization = IngenicoInitialization()
                    ingenicoInitialization.id = 0
                    ingenicoInitialization.responseCode    = IngenicoInitialization?.responseCode
                    ingenicoInitialization.apiresponseCode = IngenicoInitialization?.apiresponseCode
                    ingenicoInitialization.tidList         = arrayListTid
                    ingenicoInitialization.tidStatusList   = strstatus
                    ingenicoInitialization.initdataList    = IngenicoInitialization?.initdataList

                    val returnvalue =updateIngenicoInitialization(appDao, ingenicoInitialization)
                    println("Diff value is"+returnvalue)
                    val result = appDao.getIngenicoInitialization()
                    println("Tid value is"+result?.forEach { it->
                        val tidList = it?.tidList
                        println("TID list are "+tidList?.forEach {
                            println("TIDS are "+it)
                        })
                    })

                    isdiffTid = true
                    return isdiffTid
                }
                else{
                    isdiffTid = false
                    return isdiffTid
                }

            }
        }
    }

    return false
}



fun Int?.isGreaterThan(other: Int?) = this != null && other != null && this > other

fun Int?.isLessThan(other: Int?) = this != null && other != null && this < other



//To check Initiaization Status
 suspend fun checkInitializtionStatus(appDao: AppDao): Boolean {
    var initializationstatus: Boolean = false
    val result = appDao.getIngenicoInitialization()
    val rseultsize = result?.size
    if (rseultsize != null) {
        if(rseultsize > 0){
            result.forEach { IngenicoInitialization ->
                val responsecode = IngenicoInitialization?.responseCode
                val apirespnsecode = IngenicoInitialization?.apiresponseCode
                if(responsecode.equals("00") && apirespnsecode.equals("SUCCESS")){
                    initializationstatus = true
                    return initializationstatus
                } else{
                    initializationstatus = false
                    return initializationstatus
                }
            }
        }

    }
    return initializationstatus
}

//Do communication Txn
suspend fun doCommsTransaction(appDao: AppDao){

    val wifiCommunicationTable = runBlocking(Dispatchers.IO){appDao.getAllWifiCTTableData()}

    val request = CommRequest(
        gprsIp   = wifiCommunicationTable?.get(0)?.gprPrimaryHostIP,
        gprsPort = wifiCommunicationTable?.get(0)?.gprPrimaryHostPort,
        wifiIp   = wifiCommunicationTable?.get(0)?.wifiHostIP,
        wifiPort = wifiCommunicationTable?.get(0)?.wifiHostPort,
        apn      = wifiCommunicationTable?.get(0)?.gprAPN,
        username = wifiCommunicationTable?.get(0)?.gprAPNUserName,
        password = wifiCommunicationTable?.get(0)?.gprAPNPassword
    )

   try {
       DeviceHelper.setCommunicationSettings(
           request = request,
           listener = object : OnOperationListener.Stub() {

               override fun onCompleted(result: OperationResult?) {
                   println("Result: ${result?.value?.status}")

                   println("Result responseCode: ${(result?.value)?.responseCode}")

                   println("Completed transaction...")

                   val status = result?.value?.status
                   when (status) {
                       RequestStatus.SUCCESS -> {
                           println("Success")
                           AppPreference.saveString(PreferenceKeyConstant.Wifi_Communication.keyName, "1")

                           println("Wifi comm ->"+AppPreference.getString(PreferenceKeyConstant.Wifi_Communication.keyName))
                       }
                       RequestStatus.FAILED -> {
                           AppPreference.saveString(PreferenceKeyConstant.Wifi_Communication.keyName, "0")
                           println("Failed")
                       }
                       else -> {
                           AppPreference.saveString(PreferenceKeyConstant.Wifi_Communication.keyName, "0")
                           println("Error")
                       }
                   }
               }

           }
       )
      }
   catch (ex: Exception){
       ex.printStackTrace()
   }

}

//Do initiaization
 suspend fun doInitializtion(appDao: AppDao,listofTids: ArrayList<String>) {
   val checkdiffTid = checkTidUpdate(appDao)
    if(!checkdiffTid){
        deletInitializtionData(appDao)
    }
    var checkinitststus = checkInitializtionStatus(appDao)
    if(!checkinitststus){
        try {
            DeviceHelper.doTerminalInitialization(
                request = TerminalInitializationRequest(
                    listofTids.size,
                    listofTids,
                ),
                listener = object : OnOperationListener.Stub() {
                    override fun onCompleted(p0: OperationResult?) {
                        Log.d(DashboardFragment.TAG, "OnTerminalInitializationListener.onCompleted")
                        val response = p0?.value as? TerminalInitializationResponse
                        val initResult =
                            """
                                   Response_Code = ${response?.responseCode}
                                   API_Response_Status = ${response?.status}
                                   Response_Code = ${response?.responseCode}
                                   TIDStatusList = [${response?.tidStatusList?.joinToString()}]
                                   TIDs = [${response?.tidList?.joinToString()}]
                                   INITDATAList = [${response?.initDataList?.firstOrNull().toString()}]
                                """.trimIndent()

                        when (response?.status) {
                            RequestStatus.SUCCESS -> {
                                println(initResult)
                                val model = IngenicoInitialization()
                                var initDataListList = InitDataListList()
                                var tidarrayList = ArrayList<String>()
                                var tidstatusList = ArrayList<String>()
                                CoroutineScope(Dispatchers.IO).launch{
                                    model.id = 0
                                    model.responseCode = response?.responseCode
                                    model.apiresponseCode = response?.status.name
                                    response?.tidList?.forEach {
                                        tidarrayList.add(it)


                                    }
                                    response?.tidStatusList?.forEach {
                                        tidstatusList.add(it)
                                    }
                                    model.tidList = tidarrayList
                                    model.tidStatusList = tidstatusList
                                    var list = response?.initDataList

                                    list.forEach { it->
                                        initDataListList.adminPassword = it.adminPassword
                                        initDataListList.helpDeskNumber = it.helpDeskNumber
                                        initDataListList.merAddHeader1 = it.merAddHeader1
                                        initDataListList.merAddHeader2 = it.merAddHeader2
                                        initDataListList.merchantName  = it.merchantName
                                        initDataListList.isRefundPasswordEnable = it.isRefundPasswordEnable
                                        initDataListList.isReportPasswordEnable = it.isReportPasswordEnable
                                        initDataListList.isVoidPasswordEnable = it.isVoidPasswordEnable
                                        initDataListList.isTipEnable = it.isTipEnable

                                        model.initdataList = listOf(initDataListList)
                                    }
                                    saveInitializtionData(appDao,model)
                                }

                            }
                            RequestStatus.ABORTED,
                            RequestStatus.FAILED -> {
                                println(initResult)
                                val model = IngenicoInitialization()
                                var initDataListList = InitDataListList()
                                var tidarrayList = ArrayList<String>()
                                var tidstatusList = ArrayList<String>()
                                CoroutineScope(Dispatchers.IO).launch{
                                    model.id = 0
                                    model.responseCode = response?.responseCode
                                    model.apiresponseCode = response?.status.name
                                    response?.tidList?.forEach {
                                        tidarrayList.add(it)
                                    }
                                    response?.tidStatusList?.forEach {
                                        tidstatusList.add(it)
                                    }
                                    model.tidList = tidarrayList
                                    model.tidStatusList = tidstatusList
                                    var list = response?.initDataList

                                    list.forEach { it->
                                        initDataListList.adminPassword = it.adminPassword
                                        initDataListList.helpDeskNumber = it.helpDeskNumber
                                        initDataListList.merAddHeader1 = it.merAddHeader1
                                        initDataListList.merAddHeader2 = it.merAddHeader2
                                        initDataListList.merchantName  = it.merchantName
                                        initDataListList.isRefundPasswordEnable = it.isRefundPasswordEnable
                                        initDataListList.isReportPasswordEnable = it.isReportPasswordEnable
                                        initDataListList.isVoidPasswordEnable = it.isVoidPasswordEnable
                                        initDataListList.isTipEnable = it.isTipEnable

                                        model.initdataList = listOf(initDataListList)
                                    }
                                    saveInitializtionData(appDao,model)
                                }
                            }
                            else -> {
                                println(initResult)
                                val model = IngenicoInitialization()
                                var initDataListList = InitDataListList()
                                var tidarrayList = ArrayList<String>()
                                var tidstatusList = ArrayList<String>()
                                CoroutineScope(Dispatchers.IO).launch{
                                    model.id = 0
                                    model.responseCode = response?.responseCode
                                    model.apiresponseCode = response?.status?.name
                                    response?.tidList?.forEach {
                                        tidarrayList.add(it)
                                    }
                                    response?.tidStatusList?.forEach {
                                        tidstatusList.add(it)
                                    }
                                    model.tidList = tidarrayList
                                    model.tidStatusList = tidstatusList
                                    var list = response?.initDataList

                                    list?.forEach { it->
                                        initDataListList.adminPassword = it.adminPassword
                                        initDataListList.helpDeskNumber = it.helpDeskNumber
                                        initDataListList.merAddHeader1 = it.merAddHeader1
                                        initDataListList.merAddHeader2 = it.merAddHeader2
                                        initDataListList.merchantName  = it.merchantName
                                        initDataListList.isRefundPasswordEnable = it.isRefundPasswordEnable
                                        initDataListList.isReportPasswordEnable = it.isReportPasswordEnable
                                        initDataListList.isVoidPasswordEnable = it.isVoidPasswordEnable
                                        initDataListList.isTipEnable = it.isTipEnable

                                        model.initdataList = listOf(initDataListList)
                                    }
                                    saveInitializtionData(appDao,model)
                                }
                            }
                        }
                    }


                }
            )
        }
        catch (ex: Exception){
            ex.printStackTrace()
            println("Exception is "+ex.printStackTrace())
        }
    }

}

//To save Initiaization Data
 suspend fun saveInitializtionData(appDao: AppDao,model: IngenicoInitialization) {
    appDao.insertIngenicoIntializationData(model)
}

//To delete Initiaization Data in case of new Tid
suspend fun deletInitializtionData(appDao: AppDao) {
    appDao.deleteIngenicoInitiaization()
}

//To delete Initiaization Data in case of new Tid
suspend fun updateIngenicoInitialization(appDao: AppDao, initialization: IngenicoInitialization){
    return appDao.updateAll(initialization)
}

fun deviceModel(): String{
    return try {
        var strdeviceModel = DeviceHelper.getDeviceModel() ?: ""
        strdeviceModel = strdeviceModel.replace("\\s".toRegex(),"")
        println("Device Model after trim is"+strdeviceModel.subSequence(0,6).toString())
        return strdeviceModel.subSequence(0,6).toString()
       }
    catch (ex: Exception){
        ex.printStackTrace()
        return ""
    }

}

//region================================AppVersionName + AppRevisionID returning method:-
fun getAppVersionNameAndRevisionID(): String {
    return "${BuildConfig.VERSION_NAME}.${BuildConfig.REVISION_ID}"
}
//endregion

fun failureImpl(
    context: Context,
    servicemsg: String,
    msg: String,
    exception: Exception = Exception()
) {
    val builder = AlertDialog.Builder(context)
    object : Thread() {
        override fun run() {
            Looper.prepare()
            builder.setTitle("Alert...!!")
            builder.setCancelable(false)
            builder.setMessage("Something went wrong.\n" + "Your data is safe")
                .setCancelable(false)
                /* .setPositiveButton("Start") { _, _ ->
                     forceStart(context)
                 }*/
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    context.startActivity(Intent(context, NavigationActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    //  (context as Activity).finishAffinity()
                }
            val alert: AlertDialog = builder.create()
            try {
                if (!alert.isShowing) {
                    alert.show()
                    Looper.loop()
                }
            } catch (ex: WindowManager.BadTokenException) {
                ex.printStackTrace()
                Handler(Looper.getMainLooper()).postDelayed(Runnable {
                    GlobalScope.launch {
                       // VFService.connectToVFService(VerifoneApp.appContext)
                    }
                }, 200)
            } catch (ex: Exception) {
                ex.printStackTrace()
                Handler(Looper.getMainLooper()).postDelayed(Runnable {
                    GlobalScope.launch {
                       // VFService.connectToVFService(VerifoneApp.appContext)
                    }
                }, 200)
            }

        }
    }.start()
}

//region===============================Below method is used to show Invoice with Padding:-
fun invoiceWithPadding(invoiceNo: String) =
    addPad(input = invoiceNo, padChar = "0", totalLen = 6, toLeft = true)
//endregion



//region=====================================GET Transaction Type Name:-
fun getTransactionTypeName(type: Int): String? {
    var name: String? = null
    name = when (type) {
        BhTransactionType.SALE.type -> BhTransactionType.SALE.txnTitle
        BhTransactionType.SALE_WITH_CASH.type -> BhTransactionType.SALE_WITH_CASH.txnTitle
        BhTransactionType.CASH_AT_POS.type -> BhTransactionType.CASH_AT_POS.txnTitle
        BhTransactionType.PRE_AUTH.type -> BhTransactionType.PRE_AUTH.txnTitle
        BhTransactionType.PRE_AUTH_COMPLETE.type -> BhTransactionType.PRE_AUTH_COMPLETE.txnTitle
        BhTransactionType.PENDING_PREAUTH.type -> BhTransactionType.PENDING_PREAUTH.txnTitle
        BhTransactionType.VOID.type -> BhTransactionType.VOID.txnTitle
        BhTransactionType.REFUND.type -> BhTransactionType.REFUND.txnTitle
        BhTransactionType.VOID_REFUND.type -> BhTransactionType.VOID_REFUND.txnTitle
        BhTransactionType.EMI.type -> BhTransactionType.EMI.txnTitle
        BhTransactionType.EMI_SALE.type -> BhTransactionType.EMI_SALE.txnTitle
        BhTransactionType.TIP_SALE.type -> BhTransactionType.TIP_SALE.txnTitle
        BhTransactionType.BRAND_EMI.type -> BhTransactionType.BRAND_EMI.txnTitle
        BhTransactionType.TEST_EMI.type -> BhTransactionType.TEST_EMI.txnTitle
        else -> "NONE"
    }
    return name
}
//endregion

fun txnSuccessToast(context: Context, msg: String = "Transaction Approved") {
    try {
        GlobalScope.launch(Dispatchers.Main) {
           // VFService.vfBeeper?.startBeep(200)
            val layout = (context as Activity).layoutInflater.inflate(
                R.layout.new_success_toast,
                context.findViewById<LinearLayout>(R.id.custom_toast_layout)
            )
            layout.findViewById<BHTextView>(R.id.txtvw)?.text = msg
            val myToast = Toast(context)
            myToast.duration = Toast.LENGTH_LONG
            myToast.setGravity(Gravity.CENTER_VERTICAL, 0, 0)
            myToast.view = layout//setting the view of custom toast layout
            myToast.show()
        }


    } catch (ex: java.lang.Exception) {
      //  VFService.showToast(context.getString(R.string.transaction_approved_successfully))
      //  VFService.connectToVFService(context)
    }
}

//Below method is used to write App Revision ID for Update Confirmation:-
fun writeAppRevisionIDInFile(context: Context) {
    try {
        val saveFile = File(context.externalCacheDir, "version.txt")
        val writer = BufferedWriter(FileWriter(saveFile))
        writer.write(BuildConfig.REVISION_ID.toString())
        writer.flush()
        writer.close()
        Log.d("FilePath:- ", Uri.fromFile(saveFile).toString())
    } catch (e: IOException) {
      //  VFService.showToast("An error occurred.")
        e.printStackTrace()
    }
}

//Below Method is used to get App Revision ID Saved in Terminal File:-
fun getRevisionIDFromFile(context: Context, cb: (Boolean) -> Unit) {
    try {
        readAppRevisionIDFromFile(context) { fileStoredAppRevisionID ->
            if (!TextUtils.isEmpty(fileStoredAppRevisionID)) {
                if (fileStoredAppRevisionID < BuildConfig.REVISION_ID.toString()) {
                    cb(true)
                } else
                    cb(false)
            } else
                cb(false)
        }
    } catch (ex: Exception) {
        ex.printStackTrace()
        cb(false)
    }
}

//Below method is used to read App Revision ID from saved File in Terminal:-
fun readAppRevisionIDFromFile(context: Context, cb: (String) -> Unit) {
    var revisionID: String? = null
    try {
        val file = File(context.externalCacheDir, "version.txt")
        val text: StringBuilder? = null
        val br = BufferedReader(FileReader(file))
        var line: String?
        while (br.readLine().also { line = it } != null) {
            text?.append(line)
            text?.append('\n')
            revisionID = line.toString()
        }
        Log.d("DataList:- ", revisionID.toString())
        br.close().toString()
        cb(revisionID ?: "")
    } catch (ex: IOException) {
        ex.printStackTrace()
        cb(revisionID ?: "")
    }
}

// region ============================Get System Time in 24Hour Format:-
fun getSystemTimeIn24Hour(): String {
    val calendar = Calendar.getInstance()
    val dateFormatter = SimpleDateFormat("yyyMMddHH:mm:ss", Locale.getDefault())
   // logger("date",""+dateFormatter.format(calendar.time).replaceTimeColon())
    return dateFormatter.format(calendar.time).replaceTimeColon()
}
//endregion

//region=================Format Date by replacing colon:-
fun String.replaceTimeColon() = this.replace(":", "")
//endregion

//region=================Get Terminal Date according to Passed Index Value:-
fun String.terminalDate() = this.substring(0, 8)
//endregion

//region=================Get Terminal Date according to Passed Index Value:-
fun String.terminalTime() = this.substring(8, this.length)
//endregion


@Deprecated("Old way of getting tid for test emi")
fun getTidForTestTxnOldLogic(testEmiItem:String):String{
    val tpt = runBlocking(Dispatchers.IO) { getTptData() }
    val linkedTid:ArrayList<String> =tpt?.LinkTidType as ArrayList<String>
    val tidList= tpt.terminalId as ArrayList<String>
    val hm= hashMapOf<String,String>()
    for (tidIndices in linkedTid.indices){
        when(linkedTid[tidIndices]){
            "0"->{
                // for Amex
            }
            "1"->{
                // DC type
                hm[TestEmiItem.BASE_TID.id] = tidList[tidIndices]
            }
            "2"->{
                // off us Tid
                hm[TestEmiItem.OFFUS_TID.id] = tidList[tidIndices]
            }
            "3"->{
                // 3 months onus
                hm[TestEmiItem._3_M_TID.id] = tidList[tidIndices]
            }
            "6"->{
                // 6 months onus
                hm[TestEmiItem._6_M_TID.id] = tidList[tidIndices]
            }
            "9"->{
                // 9 months onus
                hm[TestEmiItem._9_M_TID.id] = tidList[tidIndices]
            }
            "12"->{
                // 12 months onus
                hm[TestEmiItem._12_M_TID.id] = tidList[tidIndices]
            }

        }
    }
   return hm[testEmiItem]?:"00000000"

}

fun getTidForTestTxn(testEmiItem: String):String{
    val requiredTpt = runBlocking(Dispatchers.IO) { getTptDataByLinkTidType(testEmiItem) }
   return requiredTpt?.terminalId ?: "000000"
}