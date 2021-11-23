package com.bonushub.crdb.utils

import android.app.Dialog
import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import com.bonushub.crdb.R
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.model.CardProcessedDataModal
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.model.local.IngenicoInitialization
import com.bonushub.crdb.model.local.InitDataListList
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.model.remote.BrandEMIProductDataModal
import com.bonushub.crdb.utils.Field48ResponseTimestamp.getTptData
import com.bonushub.crdb.view.fragments.DashboardFragment
import com.bonushub.crdb.vxutils.Utility.*
import com.bonushub.pax.utils.TransactionType
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.ingenico.hdfcpayment.listener.OnOperationListener
import com.ingenico.hdfcpayment.request.TerminalInitializationRequest
import com.ingenico.hdfcpayment.response.OperationResult
import com.ingenico.hdfcpayment.response.TerminalInitializationResponse
import com.ingenico.hdfcpayment.type.RequestStatus
import com.usdk.apiservice.aidl.BaseError
import com.usdk.apiservice.aidl.algorithm.AlgError
import com.usdk.apiservice.aidl.algorithm.AlgMode
import com.usdk.apiservice.aidl.algorithm.UAlgorithm
import com.usdk.apiservice.aidl.data.BytesValue
import com.usdk.apiservice.aidl.pinpad.*
import com.usdk.apiservice.aidl.pinpad.MagTrackEncMode
import com.usdk.apiservice.aidl.pinpad.UPinpad
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.charset.StandardCharsets

private var listofTids= ArrayList<String>()

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
            TransactionType.BRAND_EMI.type -> {
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
                    ) == "1" && transactionType == TransactionType.EMI_SALE.type)
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
                TransactionType.BRAND_EMI.type -> {
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
                    if (transactionType == TransactionType.SALE.type && tpt?.reservedValues?.substring(
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
                    } else if (transactionType == TransactionType.EMI_SALE.type && tpt?.reservedValues?.substring(
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
                    } else if (transactionType == TransactionType.EMI_SALE.type && tpt?.reservedValues?.substring(
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
                    } else if (transactionType == TransactionType.EMI_SALE.type && tpt?.reservedValues?.substring(
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
                    } else if (transactionType == TransactionType.EMI_ENQUIRY.type) {
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

//To check Initiaization Status
suspend fun checkBaseTid(appDao: AppDao): ArrayList<String> {
    listofTids.clear()
    var tpt = appDao?.getAllTerminalParameterTableData()
    tpt[0]?.tidType?.forEachIndexed { index, tidType ->
        if(tidType.equals("1")){
            tpt[0]?.terminalId?.get(index)
            listofTids.add(0,tpt[0]?.terminalId?.get(index) ?: "")
        }
        else{
            listofTids.add(tpt[0]?.terminalId?.get(index) ?: "")
        }
    }
    return listofTids
}


//To check Initiaization Status
 suspend fun checkInitializtionStatus(appDao: AppDao): Boolean {
    var initializationstatus: Boolean = false
    var result = appDao.getIngenicoInitialization()
    var rseultsize = result?.size
    if (rseultsize != null) {
        if(rseultsize > 0){
            result?.forEach { IngenicoInitialization ->
                val responsecode = IngenicoInitialization?.responseCode
                val apirespnsecode = IngenicoInitialization?.apiresponseCode
                if(responsecode.equals("00") && apirespnsecode.equals("SUCCESS")){
                    initializationstatus = true
                    return initializationstatus
                }
                else{
                    initializationstatus = false
                    return initializationstatus
                }
            }
        }

    }
    return initializationstatus
}

//Do initiaization
 suspend fun doInitializtion(appDao: AppDao,listofTids: ArrayList<String>) {
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
        }
    }

}

//To save Initiaization Data
 suspend fun saveInitializtionData(appDao: AppDao,model: IngenicoInitialization) {
    appDao.insertIngenicoIntializationData(model)
}








