package com.bonushub.crdb.india.view.fragments.digi_pos

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentUpiSmsDynamicPayQrInputDetailBinding
import com.bonushub.crdb.india.model.local.DigiPosDataTable
import com.bonushub.crdb.india.serverApi.HitServer
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.deleteDigiposData
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.insertOrUpdateDigiposData
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.selectAllDigiPosData
import com.bonushub.crdb.india.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.india.utils.printerUtils.PrintVectorUtil
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.base.BaseActivityNew
import com.bonushub.pax.utils.KeyExchanger.Companion.getDigiPosStatus
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*


class UpiSmsDynamicPayQrInputDetailFragment : Fragment() {

    var binding:FragmentUpiSmsDynamicPayQrInputDetailBinding? = null
    //lateinit var digiPosItemType:DigiPosItem
    private lateinit var transactionType: EDashboardItem
    private var amount_ = ""
    private var vpa_ = ""
    private var mobile_ = ""
    var uniqueID: String = ""


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        binding = FragmentUpiSmsDynamicPayQrInputDetailBinding.inflate(inflater,container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionType = arguments?.getSerializable("type") as EDashboardItem

        (activity as NavigationActivity).manageTopToolBar(false)
        refreshSubToolbarLogos(
            this,
            transactionType
        )

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            try {
                DialogUtilsNew1.hideKeyboardIfOpen(requireActivity())
                parentFragmentManager.popBackStackImmediate()
            }catch (ex:Exception)
            {
                ex.printStackTrace()
            }
        }

        // region set ui
        when(transactionType)
        {
            EDashboardItem.UPI ->{
                binding?.enterAmountCrdView?.visibility = View.VISIBLE
                binding?.vpaCrdView?.visibility = View.VISIBLE
                binding?.mobileNumberCrdView?.visibility = View.VISIBLE
                binding?.enterDescriptionCrdView?.visibility = View.VISIBLE
            }

            EDashboardItem.BHARAT_QR, EDashboardItem.SMS_PAY ->{
                binding?.enterAmountCrdView?.visibility = View.VISIBLE
                binding?.vpaCrdView?.visibility = View.GONE
                binding?.mobileNumberCrdView?.visibility = View.VISIBLE
                binding?.enterDescriptionCrdView?.visibility = View.VISIBLE
            }

            else ->{
                binding?.enterAmountCrdView?.visibility = View.GONE
                binding?.vpaCrdView?.visibility = View.GONE
                binding?.mobileNumberCrdView?.visibility = View.GONE
                binding?.enterDescriptionCrdView?.visibility = View.GONE
            }
        }
        // end region

        when (transactionType) {
            EDashboardItem.SMS_PAY, EDashboardItem.BHARAT_QR -> {
                binding?.mobileNumberCrdView?.hint = getString(R.string.enter_mobile_number)
            }
            EDashboardItem.UPI -> {
                binding?.mobileNumberCrdView?.hint = getString(R.string.enter_mobile_number_optional)

            }
            else -> {

            }
        }

        AmountTextWatcher(binding?.amountEt!!)
        binding?.btnProceed?.setOnClickListener {

            DialogUtilsNew1.hideKeyboardIfOpen(requireActivity())

            when(transactionType)
            {
                EDashboardItem.UPI ->{
                    logger("transactionType",""+transactionType.title)
                    if(isVerify()){


           /*             (activity as NavigationActivity).transactFragment(UpiCollectFragment().apply {
                            arguments = Bundle().apply {
                                putSerializable("type", EDashboardItem.UPI)
                                putSerializable("amount", amount_)
                                putSerializable("vpa", vpa_)
                                putSerializable("mobile", mobile_)
                            }
                        }, false)*/

                 ///   DialogUtilsNew1.showUpiCollectDialog(requireContext(),amount_,vpa_,mobile_,{},{})
                        validateAndSyncRequestToServer(binding?.amountEt?.text.toString().trim())
                    }

                }

                EDashboardItem.BHARAT_QR->{
                    logger("transactionType",""+transactionType.title)
//                    (activity as NavigationActivity).transactFragment(QrFragment().apply {
//                        arguments = Bundle().apply {
//                            putSerializable("type", EDashboardItem.DYNAMIC_QR)
//                        }
//                    })

                    validateAndSyncRequestToServer(binding?.amountEt?.text.toString().trim())
                }

                EDashboardItem.SMS_PAY ->{
                   logger("transactionType",""+transactionType.title)
//                    (activity as NavigationActivity).transactFragment(UpiCollectFragment().apply {
//                        arguments = Bundle().apply {
//                            putString("amount", binding?.amountEt?.text.toString().trim())
//                            putString("vpa", binding?.vpaEt?.text.toString().trim())
//                            putString("des", binding?.enterDescriptionEt?.text.toString().trim())
//                        }
//                    })
                    validateAndSyncRequestToServer(binding?.amountEt?.text.toString().trim())
                }

                else ->{
                    logger("transactionType",""+transactionType.title)
                }
            }

        }
    }


    private fun isVerify():Boolean{
        amount_ =  binding?.amountEt?.text.toString().trim()
        vpa_ =  binding?.vpaEt?.text.toString().trim()
        mobile_ =  binding?.mobileNumberEt?.text.toString().trim()

        if(amount_.isEmpty())
        {
           // ( activity as IDialog).showToast("Please Enter Amount")// ToastUtils.showToast(requireContext(),"Please Enter Amount")
            binding?.amountEt?.setError(getString(R.string.error_plz_enter_amount))
            return false
        }
        else if(vpa_.isEmpty())
        {
            binding?.vpaEt?.setError(getString(R.string.error_enter_vap))
            return false
        }
        else if(mobile_.isNotEmpty() && mobile_.length <10)
        {
            binding?.mobileNumberEt?.setError(getString(R.string.error_enter_mobile))
            return false
        }

        return true
    }

    private fun validateAndSyncRequestToServer(amt: String) {
        if (amt == "") {
      //      ToastUtils.showToast(activity,"Please Enter Amount")
            //( activity as IDialog).showToast("Please Enter Amount")
            binding?.amountEt?.setError(getString(R.string.error_plz_enter_amount))
            return
        }
        val formattedAmt = "%.2f".format(amt.toFloat())
        val amtValue = formattedAmt.toFloat()
        when {
            amtValue <= 0 -> {
                binding?.amountEt?.setError(getString(R.string.error_plz_enter_amount))
            }
            transactionType == EDashboardItem.UPI && !TextUtils.isEmpty(binding?.mobileNumberEt?.text.toString()) && binding?.mobileNumberEt?.text.toString().length != 10 ->
                binding?.mobileNumberEt?.setError(getString(R.string.enter_valid_mobile_number))

            (transactionType == EDashboardItem.SMS_PAY || transactionType == EDashboardItem.BHARAT_QR) && binding?.mobileNumberEt?.text.toString().length != 10 -> {
                binding?.mobileNumberEt?.setError(getString(R.string.enter_valid_mobile_number))
            }

            transactionType == EDashboardItem.BHARAT_QR && !TextUtils.isEmpty(binding?.mobileNumberEt?.text.toString()) && binding?.mobileNumberEt?.text.toString().length != 10 ->
                binding?.mobileNumberEt?.setError(getString(R.string.enter_valid_mobile_number))

            !TextUtils.isEmpty(binding?.vpaEt?.text.toString()) && transactionType == EDashboardItem.UPI &&(!(binding?.vpaEt?.text.toString().contains('@')))-> {
                binding?.vpaEt?.setError(getString(R.string.error_enter_vap))
            }
            else -> {
                //   "5^1.08^^8287305603^064566935811302^:"
                val dNow = Date()
                val ft = SimpleDateFormat("yyMMddhhmmssMs", Locale.getDefault())
                uniqueID = ft.format(dNow)
                println("TXN UNIQUE ID -->  $uniqueID")
                val rndn1 = (10..99).random().toString()
                val rndn2 = (10..99).random().toString()
                println("Rndom1  Rndom2 -->  $rndn1 , $rndn2")
                uniqueID= rndn1+uniqueID+rndn2
                println("uniqueID -->  $uniqueID")
                var f56 = ""
                f56 = when (transactionType) {
                    EDashboardItem.UPI -> EnumDigiPosProcess.UPIDigiPOS.code + "^" + formattedAmt + "^" + binding?.enterDescriptionEt?.text?.toString() + "^" + binding?.mobileNumberEt?.text?.toString() + "^" + binding?.vpaEt?.text?.toString() + "^" + uniqueID
                    EDashboardItem.BHARAT_QR -> EnumDigiPosProcess.DYNAMIC_QR.code + "^" + formattedAmt + "^" + binding?.enterDescriptionEt?.text?.toString() + "^" + binding?.mobileNumberEt?.text?.toString() + "^" + uniqueID
                    else -> EnumDigiPosProcess.SMS_PAYDigiPOS.code + "^" + formattedAmt + "^" + binding?.enterDescriptionEt?.text?.toString() + "^" + binding?.mobileNumberEt?.text?.toString() + "^" + uniqueID
                }
                sendReceiveDataFromHost(f56)
            }
        }
    }

    private fun sendReceiveDataFromHost(field57: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                (activity as BaseActivityNew).showProgress()
            }
            val isSavedTrans = false//transactionType != EDashboardItem.DYNAMIC_QR

            getDigiPosStatus(
                field57,
                EnumDigiPosProcessingCode.DIGIPOSPROCODE.code,
                isSavedTrans
            ) { isSuccess, responseMsg, responsef57, fullResponse ->
                (activity as BaseActivityNew).hideProgress()
                try {

                    if (isSuccess) {

                        saveDigiPosPendingTxn(field57)

                        when (transactionType) {
                            EDashboardItem.SMS_PAY, EDashboardItem.UPI -> {
                                lifecycleScope.launch(Dispatchers.Main) {
                                    val respDataList = responsef57.split("^")
                                    // condition checks for status is success or fail
                                    if (respDataList[4] == EnumDigiPosTerminalStatusCode.TerminalStatusCodeS101.code) {
                                        var msg = ""
                                        if (transactionType == EDashboardItem.UPI) {
                                            msg = getString(R.string.upi_payment_link_sent)
                                        }
                                        if (transactionType == EDashboardItem.SMS_PAY) {
                                            msg = getString(R.string.sms_payment_link_sent)
                                        }

                                        (activity as BaseActivityNew).alertBoxWithActionNew(
                                           getString(R.string.sms_upi_pay),
                                            msg,R.drawable.ic_link_circle,getString(R.string.positive_button_yes),getString(R.string.no),true,
                                            false,
                                            { status ->
                                                if (status) {
                                                    lifecycleScope.launch(Dispatchers.IO) {
                                                        withContext(Dispatchers.Main) {
                                                            (activity as BaseActivityNew).showProgress()
                                                        }
                                                        val req57 =
                                                            EnumDigiPosProcess.GET_STATUS.code + "^" + respDataList[1] + "^"

                                                        getDigiPosStatus(
                                                            req57,
                                                            EnumDigiPosProcessingCode.DIGIPOSPROCODE.code,
                                                            false
                                                        ) { isSuccess, responseMsg, responsef57, fullResponse ->
                                                            try {
                                                                (activity as BaseActivityNew).hideProgress()
                                                                if (isSuccess) {
                                                                    val statusRespDataList =
                                                                        responsef57.split("^")
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

                                                                           insertOrUpdateDigiposData(
                                                                                tabledata
                                                                            )
                                                                            Log.e(
                                                                                "F56->>",
                                                                                responsef57
                                                                            )

                                                                            lifecycleScope.launch(
                                                                                Dispatchers.Main
                                                                            ) {
                                                                                ToastUtils.showToast(activity,
                                                                                    getString(R.string.txn_status_still_pending)
                                                                                )
                                                                                parentFragmentManager.popBackStackImmediate()
                                                                            }
                                                                        }

                                                                        EDigiPosPaymentStatus.Approved.desciption -> {
                                                                            tabledata.txnStatus =
                                                                                statusRespDataList[5]
                                                                        insertOrUpdateDigiposData(
                                                                                tabledata
                                                                            )
                                                                            Log.e(
                                                                                "F56->>",
                                                                                responsef57
                                                                            )

                                                                            lifecycleScope.launch(Dispatchers.Main){
                                                                                (activity as BaseActivityNew).alertBoxMsgWithIconOnly(R.drawable.ic_tick_green,"Transaction Approved")
                                                                            }

                                                                            //txnSuccessToast(activity as Context)
                                                                            // kushal
                                                                            logger("call","printSMSUPIChagreSlip","e")
                                                                            PrintVectorUtil(context).printSMSUPIChagreSlip(
                                                                                tabledata,
                                                                                EPrintCopyType.MERCHANT,
                                                                                context
                                                                            ) { alertCB, printingFail ->
                                                                                //context.hideProgress()
                                                                                if (!alertCB) {
                                                                                    parentFragmentManager.popBackStack()

                                                                                }
                                                                            }
                                                                        }
                                                                        else -> {
                                                                            Field48ResponseTimestamp.deleteDigiposData(
                                                                                tabledata
                                                                            )

                                                                        }
                                                                    }

                                                                } else {
                                                                    lifecycleScope.launch(
                                                                        Dispatchers.Main
                                                                    ) {
                                                                        (activity as BaseActivityNew).alertBoxWithActionNew(
                                                                            getString(R.string.transaction_failed_msg),
                                                                            responseMsg,R.drawable.ic_info_orange,getString(R.string.positive_button_ok),"",
                                                                            false,true,
                                                                            { alertPositiveCallback ->
                                                                                if (alertPositiveCallback) {
                                                                                    Field48ResponseTimestamp.deleteDigiposData(
                                                                                        field57.split(
                                                                                            "^"
                                                                                        ).last()
                                                                                    )
                                                                                    parentFragmentManager.popBackStack()
                                                                                }
                                                                            },
                                                                            {})
                                                                    }
                                                                }

                                                            } catch (ex: java.lang.Exception) {
                                                                ex.printStackTrace()
                                                                logger(
                                                                    LOG_TAG.DIGIPOS.tag,
                                                                    "Somethig wrong... in response data field 57"
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            {
                                                val dp = selectAllDigiPosData()
                                                val dpObj = Gson().toJson(dp)
                                                logger(LOG_TAG.DIGIPOS.tag, "--->      $dpObj ")
                                                parentFragmentManager.popBackStack()
                                            })
                                    } else {
                                        // received other than S101(show Fail info dialog here)
                                        withContext(Dispatchers.Main) {
                                            (activity as BaseActivityNew).alertBoxWithActionNew(

                                             getString(R.string.transaction_failed_msg),
                                                getString(R.string.transaction_failed_msg),R.drawable.ic_info_orange,getString(R.string.positive_button_ok),"",
                                                false,true,
                                                { alertPositiveCallback ->
                                                    if (alertPositiveCallback) {
                                                        Field48ResponseTimestamp.deleteDigiposData(
                                                            field57.split("^").last()
                                                        )
                                                        parentFragmentManager.popBackStack()
                                                    }
                                                },
                                                {})
                                        }

                                    }
                                }
                            }

                            EDashboardItem.BHARAT_QR -> {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    val respDataList = responsef57.split("^")
//reqest type, parterid,status,statusmsg,statuscode,QrBlob
                                    if (respDataList[2] == EDigiPosPaymentStatus.Approved.desciption) {
                                        val tabledata = DigiPosDataTable()
                                        tabledata.requestType = respDataList[0].toInt()
                                        tabledata.partnerTxnId = respDataList[1]
                                        tabledata.status = respDataList[2]
                                        tabledata.statusMsg = respDataList[3]
                                        tabledata.statusCode = respDataList[4]
                                        val responseIsoData: IsoDataReader =
                                            readIso(fullResponse, false)

                                        Log.e(
                                            "BitmapHexString-->  ",
                                            responseIsoData.isoMap[59]?.rawData.toString() + "---->"
                                        )


                                        val blobHexString =
                                            responseIsoData.isoMap[59]?.rawData.toString()
                                        /*   val sstrr =
                                               "424d6e100000000000003e00000028000000d200000094000000010001000000000000000000c40e0000c40e00000200000002000000000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffc000ffffffffffffffffffffffffffffffffffffffffffffffffffffc000ffffffffffffffffffffffffffffffffffffffffffffffffffffc000ffffffffffffffffffffffffffffffffffffffffffffffffffffc000ffffffffffffffffffffffffffffffffffffffffffffffffffffc000ffffffffffffffffffffffffffffffffffffffffffffffffffffc000ffffffffffffffffffffffffffffffffffffffffffffffffffffc000ffffffffffffffffffffffffffffffffffffffffffffffffffffc000ffffffffffffffffffffffffffffffffffffffffffffffffffffc000ffffffffffffffffffffffffffffffffffffffffffffffffffffc000ffffffffffffffffffffffffffffffffffffffffffffffffffffc000ffffffffffffffffffffffffffffffffffffffffffffffffffffc000ffffffffffffffffffffffffffffffffffffffffffffffffffffc000fffffffffff00033f3330fcfcf0f30cf3c33c0ff03ffffffffffc000fffffffffff00033f3330fcfcf0f30cf3c33c0ff03ffffffffffc000fffffffffff3ff33c30ffcfff3330ff303ccff3cf3ffffffffffc000fffffffffff3ff33c30ffcfff3330ff303ccff3cf3ffffffffffc000fffffffffff303333f0f00f3fccf303cf3000cfcf3ffffffffffc000fffffffffff303333f0f00f3fccf303cf3000cfcf3ffffffffffc000fffffffffff30333333c0f003f0cf3cf3cfff000f3ffffffffffc000fffffffffff30333333c0f003f0cf3cf3cfff000f3ffffffffffc000fffffffffff30333f30f30ffc00333cf3cf0c00333ffffffffffc000fffffffffff30333f30f30ffc00333cf3cf0c00333ffffffffffc000fffffffffff3ff3cf0cc0f3fc3f3cff303ccc3f0cfffffffffffc000fffffffffff3ff3cf0cc0f3fc3f3cff303ccc3f0cfffffffffffc000fffffffffff000330c030003f333300cff00333033ffffffffffc000fffffffffff000330c030003f333300cff00333033ffffffffffc000fffffffffffffff0003cff3033f0c3cf3cf3c3f0cfffffffffffc000fffffffffffffff0003cff3033f0c3cf3cf3c3f0cfffffffffffc000fffffffffff00f00fc0000cfc003f3cc3f30c000f3ffffffffffc000fffffffffff00f00fc0000cfc003f3cc3f30c000f3ffffffffffc000fffffffffff033fccf303f30c33fcff000ccfcc3f3ffffffffffc000fffffffffff033fccf303f30c33fcff000ccfcc3f3ffffffffffc000ffffffffffff003f030033fcc3c3300ccf00030c33ffffffffffc000ffffffffffff003f030033fcc3c3300ccf00030c33ffffffffffc000fffffffffff3cfc33ff0fc333cfccfcf0cf3cf0fcfffffffffffc000fffffffffff3cfc33ff0fc333cfccfcf0cf3cf0fcfffffffffffc000ffffffffffff0330cfcf03ccc30cf30f3ff0cf0303ffffffffffc000ffffffffffff0330cfcf03ccc30cf30f3ff0cf0303ffffffffffc000fffffffffff000ff0ffc0cf3c33ccff3000ffcfcf3ffffffffffc000fffffffffff000ff0ffc0cf3c33ccff3000ffcfcf3ffffffffffc000ffffffffffff0003ffff3ff3cf003c0cff30033cf3ffffffffffc000ffffffffffff0003ffff3ff3cf003c0cff30033cf3ffffffffffc000fffffffffff3cff03c3ccc303ccfc3cf3cc3c33fcfffffffffffc000fffffffffff3cff03c3ccc303ccfc3cf3cc3c33fcfffffffffffc000fffffffffff00000f00f30cfc300f30f3f30c30333ffffffffffc000fffffffffff00000f00f30cfc300f30f3f30c30333ffffffffffc000fffffffffff300cc0ccf0f03f03fcff300cffffcc3ffffffffffc000fffffffffff300cc0ccf0f03f03fcff300cffffcc3ffffffffffc000ffffffffffff0f03333ffcf3cf03303cff30000cf3ffffffffffc000ffffffffffff0f03333ffcf3cf03303cff30000cf3ffffffffffc000fffffffffff33cffc00f3c003cfcffff0cfff03ccfffffffffffc000fffffffffff33cffc00f3c003cfcffff0cfff03ccfffffffffffc000fffffffffff0cf0f3c30ffffc30ff00cfc30cf0333ffffffffffc000fffffffffff0cf0f3c30ffffc30ff00cfc30cf0333ffffffffffc000fffffffffffc33c3fc0ffc33cf0ccff300cffcfcf3ffffffffffc000fffffffffffc33c3fc0ffc33cf0ccff300cffcfcf3ffffffffffc000fffffffffff3c30ffcf0f303cc33303cff30030cf3ffffffffffc000fffffffffff3c30ffcf0f303cc33303cff30030cf3ffffffffffc000fffffffffff33fccfffff303cfcfff33ccfccfccfffffffffffc000fffffffffffc33fccfffff303cfcfff33ccfccfccfffffffffffc000ffffffffffff3f00ccc000cff30ff3ccfcf3030333ffffffffffc000ffffffffffff3f00ccc000cff30ff3ccfcf3030333ffffffffffc000ffffffffffff0cff3fcc0fc30f3fccf003ccfcccf3ffffffffffc000ffffffffffff0cff3fcc0fc30f3fccf003ccfcccf3ffffffffffc000ffffffffffffc33330fcc0fc0f03303ccf00030cf3ffffffffffc000ffffffffffffc33330fcc0fc0f03303ccf00030cf3ffffffffffc000fffffffffff3f3c3c3ccff33fccccfcf0cf3cfcccfffffffffffc000fffffffffff3f3c3c3ccff33fccccfcf0cf3cfcccfffffffffffc000ffffffffffff3003c3ff00ccf000f30ffcf3000333ffffffffffc000ffffffffffff3003c3ff00ccf000f30ffcf3000333ffffffffffc000ffffffffffffc3f3f0030ff303f3cff303cfc3f0ffffffffffffc000ffffffffffffc3f3f0030ff303f3cff303cfc3f0ffffffffffffc000fffffffffff0c3330ccf0fcfc333000ccf00033033ffffffffffc000fffffffffff0c3330ccf0fcfc333000ccf00033033ffffffffffc000fffffffffff033f33c3cccf003f3c3cf0cfff3f0cfffffffffffc000fffffffffff033f33c3cccf003f3c3cf0cfff3f0cfffffffffffc000ffffffffffff0000ff0f03cff000330f3c33300303ffffffffffc000ffffffffffff0000ff0f03cff000330f3c33300303ffffffffffc000ffffffffffffcfc3cc0f0000cf030cf300cfcf30ffffffffffffc000ffffffffffffcfc3cc0f0000cf030cf300cfcf30ffffffffffffc000fffffffffffc30000fcf33fff33f003ccf300cfcf3ffffffffffc000fffffffffffc30000fcf33fff33f003ccf300cfcf3ffffffffffc000fffffffffff03ff0fc0c00000f00f3cf00c3cf0ccfffffffffffc000fffffffffff03ff0fc0c00000f00f3cf00c3cf0ccfffffffffffc000fffffffffffc303fcf333ffffcfcf0cffc33303f33ffffffffffc000fffffffffffc303fcf333ffffcfcf0cffc33303f33ffffffffffc000fffffffffff3fffcff003ffccf300cf303cfcf00f3ffffffffffc000fffffffffff3fffcff003ffccf300cf303cfcf00f3ffffffffffc000fffffffffff3fc3ffc0f30c3f33f000cff303fccf3ffffffffffc000fffffffffff3fc3ffc0f30c3f33f000cff303fccf3ffffffffffc000ffffffffffffc0f3c3ff3f03cc00c3cf0cffcc0ccfffffffffffce07ffffffffffffc0f3c3ff3f03cc00c3cf0cffcc0ccfffffffffffc000ffffffffffffff00fcc000f0fcf0c30cfc3333ff33ffffffffffc000ffffffffffffff00fcc000f0fcf0c30cfc3333ff33ffffffffffc000fffffffffff3f3c3f3333cf0cf03fff303cccf00f3ffffffffffc000fffffffffff3f3c3f3333cf0cf03fff303cccf00f3ffffffffffc000fffffffffff3c33fc3c3000f30ff000cff3000fcf3ffffffffffc000fffffffffff3c33fc3c3000f30ff000cff3000fcf3ffffffffffc000fffffffffffcccfff3f0fc300f30c3ff0cf3cf30cfffffffffffc000fffffffffffcccfff3f0fc300f30c3ff0cf3cf30cfffffffffffc000fffffffffff30003fc0f03cfccfff0cc3cf3f33f03ffffffffffc000fffffffffff30003fc0f03cfccfff0cc3cf3f33f03ffffffffffc000fffffffffff00fff033f3cf3cf3fccf0000fcf00ffffffffffffc000fffffffffff00fff033f3cf3cf3fccf0000fcf00ffffffffffffc000fffffffffffcc03f3c0f03f3f0fc000cff303cc3f3ffffffffffc000fffffffffffcc03f3c0f03f3f0fc000cff303cc3f3ffffffffffc000fffffffffff03ff0003cccf03c33ffcf30c3cf0fcfffffffffffc000fffffffffff03ff0003cccf03c33ffcf30c3cf0fcfffffffffffc000fffffffffff0033cccf03cffcfff0cc3c30f30f33ffffffffffc000fffffffffff00033cccf03cffcfff0cc3c30f30f33ffffffffffc000fffffffffff03ccc030ffcf30f3c0ff303cfff30f3ffffffffffc000fffffffffff03ccc030ffcf30f3c0ff303cfff30f3ffffffffffc000fffffffffff33c30fc0f0fccf3ff300ccf3c30fcf3ffffffffffc000fffffffffff33c30fc0f0fccf3ff300ccf3c30fcf3ffffffffffc000fffffffffff0fcc0300f3c003cc0f3ff00c3cf3ccfffffffffffc000fffffffffff0fcc0300f3c003cc0f3ff00c3cf3ccfffffffffffc000fffffffffff00303fff0cffff003f0cffff3f0cccfffffffffffc000fffffffffff00303fff0cffff003f0cffff3f0cccfffffffffffc000fffffffffffffffc33cffc3fc3f00ff303cfcfffffffffffffffc000fffffffffffffffc33cffc3fc3f00ff303cfcfffffffffffffffc000fffffffffff0003333333333333333333333330003ffffffffffc000fffffffffff0003333333333333333333333330003ffffffffffc000fffffffffff3ff33f3fcc0f3c3f30c0cff303f3ff3ffffffffffc000fffffffffff3ff33f3fcc0f3c3f30c0cff303f3ff3ffffffffffc000fffffffffff3033ff3ffff03c000c3c33cc00f3033ffffffffffc000fffffffffff3033ff3ffff03c000c3c33cc00f3033ffffffffffc000fffffffffff3033330c300f0f0fc00cf0c33333033ffffffffffc000fffffffffff3033330c300f0f0fc00cf0c33333033ffffffffffc000fffffffffff3033c0f0f33f30f30fcf33ccf033033ffffffffffc000fffffffffff3033c0f0f33f30f30fcf33ccf033033ffffffffffc000fffffffffff3ff30f0fc3c33cfff003ccf33c33ff3ffffffffffc000fffffffffff3ff30f0fc3c33cfff003ccf33c33ff3ffffffffffc000fffffffffff0003c3cccff300cc0ffc300cfc30003ffffffffffc000fffffffffff0003c3cccff300cc0ffc300cfc30003ffffffffffc000ffffffffffffffffffffffffffffffffffffffffffffffffffffc000ffffffffffffffffffffffffffffffffffffffffffffffffffffc000ffffffffffffffffffffffffffffffffffffffffffffffffffffc000ffffffffffffffffffffffffffffffffffffffffffffffffffffc000ffffffffffffffffffffffffffffffffffffffffffffffffffffc000ffffffffffffffffffffffffffffffffffffffffffffffffffffc000ffffffffffffffffffffffffffffffffffffffffffffffffffffc000ffffffffffffffffffffffffffffffffffffffffffffffffffffc000ffffffffffffffffffffffffffffffffffffffffffffffffffffc000ffffffffffffffffffffffffffffffffffffffffffffffffffffc000ffffffffffffffffffffffffffffffffffffffffffffffffffffc000ffffffffffffffffffffffffffffffffffffffffffffffffffffc000ffffffffffffffffffffffffffffffffffffffffffffffffffffc000"
                                         */
                                        //val blobByteArray = blobHexString.decodeHexStringToByteArray()
                                        val blobByteArray = BytesUtil.hexString2Bytes(blobHexString)

                                        //    val blobByteArray = blobHexString.decodeHexStringToByteArray()
                                        var transactionTypeTemp = transactionType
                                        (activity as BaseActivityNew).transactFragment(QrFragment().apply {
                                            arguments = Bundle().apply {
                                                putByteArray("QrByteArray", blobByteArray)
                                                putSerializable("type", transactionTypeTemp)
                                                putParcelable("tabledata", tabledata) // kushal
                                            }
                                        })
                                    } else if (respDataList[2] == EDigiPosPaymentStatus.Fail.desciption) {
                                        deleteDigiposData(uniqueID)
                                    } else {
                                        lifecycleScope.launch(Dispatchers.Main) {
                                            ToastUtils.showToast(activity, respDataList[3])
                                        }
                                    }
                                }
                            }
                            else -> {
                                logger("DigiPOS", "Unknown trans", "e")
                            }
                        }
                    } else {

                        lifecycleScope.launch(Dispatchers.Main) {
                           // ToastUtils.showToast(activity, responseMsg)
                            (activity as BaseActivityNew).alertBoxWithActionNew("",responseMsg,R.drawable.ic_info_orange,"","",false,true,{},{})
                        }
                    }

                } catch (ex: Exception) {
                    ex.printStackTrace()
                    logger(LOG_TAG.DIGIPOS.tag, "Something wrong... in response data field 57")
                }
            }
        }
    }

    private fun saveDigiPosPendingTxn(field57:String){

            //val datatosave = isoWriterData.isoMap[57]?.parseRaw2String().toString()
            logger(HitServer.TAG, "SAVED TO DIGIPOS -->$field57", "e")
            val datalist = field57.split("^")

            val digiposData = DigiPosDataTable()
            digiposData.requestType = datalist[0].toInt()
            digiposData.amount = datalist[1]
            digiposData.description = datalist[2]
            digiposData.customerMobileNumber = datalist[3]
            digiposData.displayFormatedDate = getCurrentDateInDisplayFormatDigipos()

            when {
                datalist[0].toInt() == EnumDigiPosProcess.UPIDigiPOS.code.toInt() -> {
                    digiposData.vpa = datalist[4]
                    digiposData.partnerTxnId = datalist[5]
                    digiposData.paymentMode = "UPI Pay"
                }
                datalist[0].toInt() == EnumDigiPosProcess.DYNAMIC_QR.code.toInt() -> {
                    digiposData.partnerTxnId = datalist[4]
                    digiposData.paymentMode = "Bhqr Pay"
                }
                else -> {
                    digiposData.partnerTxnId = datalist[4]
                    digiposData.paymentMode = "SMS Pay"
                }
            }
            //

            insertOrUpdateDigiposData(digiposData)
    }

}