package com.bonushub.crdb.view.fragments.digi_pos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentPendingDetailsBinding
import com.bonushub.crdb.model.local.DigiPosDataTable
import com.bonushub.crdb.utils.*
import com.bonushub.crdb.utils.printerUtils.PrintUtil
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.base.BaseActivityNew
import com.bonushub.pax.utils.KeyExchanger.Companion.getDigiPosStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*


class DigiPosTXNListDetailFragment : Fragment() {

    var binding:FragmentPendingDetailsBinding? = null
    private var detailPageData: DigiPosTxnModal? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_digi_pos_t_x_n_list_detail, container, false)
        binding = FragmentPendingDetailsBinding.inflate(inflater,container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        detailPageData = arguments?.getParcelable("data")

        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.txn_detail_page)
        binding?.subHeaderView?.backImageButton?.setOnClickListener { parentFragmentManager.popBackStackImmediate() }

        if (detailPageData?.txnStatus?.toLowerCase(Locale.ROOT).equals("success", true)) {
            binding?.transactionIV?.setImageResource(R.drawable.circle_with_tick_mark_green)
            val message = "Transaction ${detailPageData?.txnStatus}"
            binding?.transactionMessageTV?.text = message
            binding?.printButton?.text = getString(R.string.print)
        } else {
            binding?.transactionIV?.setImageResource(R.drawable.ic_exclaimation_mark_circle_error)
            binding?.printButton?.text = getString(R.string.getStatus)
            val message = "Transaction ${detailPageData?.txnStatus}"
            binding?.transactionMessageTV?.text = message
        }

        val amountData = "\u20B9${detailPageData?.amount}"
        binding?.transactionAmountTV?.text = amountData
        binding?.transactionDateTime?.text = detailPageData?.transactionTime
        binding?.paymentModeTV?.text = detailPageData?.paymentMode
       /* when {
            detailPageData?.paymentMode?.toLowerCase(Locale.ROOT).equals("sms pay", true) -> {
                binding?.paymentModeTV?.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.sms_icon,
                    0,
                    0,
                    0
                )
            }
            detailPageData?.paymentMode?.toLowerCase(Locale.ROOT).equals("upi", true) -> {
                binding?.paymentModeTV?.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.upi_icon,
                    0,
                    0,
                    0
                )
            }
            else -> {
                binding?.paymentModeTV?.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_qrcode,
                    0,
                    0,
                    0
                )
            }
        }*/
        binding?.mobileNumberTV?.text = detailPageData?.customerMobileNumber
        binding?.ptxnTV?.text = detailPageData?.partnerTXNID
        binding?.mtxnTV?.text = detailPageData?.mTXNID
        binding?.txnStatusTV?.text = detailPageData?.status

        //OnClick event of Bottom Button:-
        binding?.printButton?.setOnClickListener {
            if (detailPageData?.txnStatus.equals("success", true)) {
                val tabledata = DigiPosDataTable()
                runBlocking {
                    tabledata.status = detailPageData?.status ?: ""
                    tabledata.statusMsg = detailPageData?.statusMessage ?: ""
                    tabledata.statusCode = detailPageData?.statusCode ?: ""
                    tabledata.mTxnId = detailPageData?.mTXNID ?: ""
                    tabledata.partnerTxnId = detailPageData?.partnerTXNID ?: ""
                    tabledata.transactionTimeStamp = detailPageData?.transactionTime ?: ""
                    val dateTime = detailPageData?.transactionTime?.split(" ")
                    tabledata.txnDate = dateTime?.get(0) ?: ""
                    tabledata.txnTime = dateTime?.get(1) ?: ""
                    tabledata.amount = detailPageData?.amount ?: ""
                    tabledata.paymentMode = detailPageData?.paymentMode ?: ""
                    tabledata.customerMobileNumber = detailPageData?.customerMobileNumber ?: ""
                    tabledata.description = detailPageData?.description ?: ""
                    tabledata.pgwTxnId = detailPageData?.pgwTXNID ?: ""
                    tabledata.txnStatus=detailPageData?.txnStatus ?:""
                    //tabledata.txnStatus = detailPageData.txnStatus
                }

                PrintUtil(context).printSMSUPIChagreSlip(
                    tabledata,
                    EPrintCopyType.DUPLICATE,
                    context
                ) { alertCB, _ ->
                    if (!alertCB) {
                        startActivity(Intent(activity, NavigationActivity::class.java).apply {
                            flags =
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    }
                }
            } else {
                getTransactionStatus()
            }
        }
    }

    //region=========================Get Transaction Status:-
    private fun getTransactionStatus() {
        (activity as BaseActivityNew)?.showProgress()
        lifecycleScope.launch(Dispatchers.IO) {
            val req57 =
                "${EnumDigiPosProcess.GET_STATUS.code}^${detailPageData?.partnerTXNID}^${detailPageData?.mTXNID}^"
            Log.d("Field57:- ", req57)
            getDigiPosStatus(
                req57, EnumDigiPosProcessingCode.DIGIPOSPROCODE.code,
                false
            ) { isSuccess, responseMsg, responsef57, fullResponse ->
                try {
                    if (isSuccess) {
                        val statusRespDataList = responsef57.split("^")
                        val txnStatus = statusRespDataList[5]
                        (activity as BaseActivityNew)?.hideProgress()
                        lifecycleScope.launch(Dispatchers.Main) {
                            if (txnStatus.toLowerCase(Locale.ROOT).equals("success", true)) {
                                binding?.transactionIV?.setImageResource(R.drawable.circle_with_tick_mark_green)
                                val message = "Transaction ${detailPageData?.status}"
                                binding?.transactionMessageTV?.text = message
                                detailPageData?.txnStatus= txnStatus
                                binding?.printButton?.text = getString(R.string.print)
                            }
                            if (txnStatus.toLowerCase(Locale.ROOT).equals("InProgress", true)) {
                              //  ToastUtils.showToast(requireContext(),getString(R.string.txn_status_still_pending))
                                CoroutineScope(Dispatchers.Main).launch {
                                    (activity as? NavigationActivity)?.getInfoDialog("Error", getString(R.string.no_data_found) ?: "") {}
                                }
                            }
                            if(txnStatus.isBlank() || statusRespDataList[1].toLowerCase(Locale.ROOT).equals("Failed", true)){
                                (activity as BaseActivityNew)?.alertBoxWithAction(
                                    getString(R.string.error), statusRespDataList[1],
                                    false, getString(R.string.positive_button_ok),
                                    {}, {}, R.drawable.ic_info)

                            }
                            else{
                                (activity as BaseActivityNew)?.alertBoxWithAction(
                                    getString(R.string.error), txnStatus,
                                    false, getString(R.string.positive_button_ok),
                                    {}, {}, R.drawable.ic_info)

                            }
                        }

                    } else {
                        lifecycleScope.launch(Dispatchers.Main) {
                            (activity as BaseActivityNew)?.hideProgress()
                            (activity as BaseActivityNew)?.alertBoxWithAction(
                                getString(R.string.error), responseMsg,
                                false, getString(R.string.positive_button_ok),
                                {}, {}, R.drawable.ic_info)
                        }
                    }
                } catch (ex: Exception) {
                    (activity as BaseActivityNew)?.hideProgress()
                    ex.printStackTrace()
                    logger(LOG_TAG.DIGIPOS.tag, "Somethig wrong... in response data field 57")
                }
            }
        }
    }
    //endregion
}