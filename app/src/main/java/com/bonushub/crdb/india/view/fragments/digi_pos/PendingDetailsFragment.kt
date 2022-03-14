package com.bonushub.crdb.india.view.fragments.digi_pos

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentPendingDetailsBinding
import com.bonushub.crdb.india.model.local.DigiPosDataTable
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.deleteDigiposData
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.insertOrUpdateDigiposData
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.selectAllDigiPosData
import com.bonushub.crdb.india.utils.printerUtils.PrintUtil
import com.bonushub.crdb.india.view.base.BaseActivityNew
import com.bonushub.pax.utils.KeyExchanger.Companion.getDigiPosStatus
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import com.bonushub.crdb.india.utils.EDashboardItem


class PendingDetailsFragment : Fragment() {

    var binding:FragmentPendingDetailsBinding? = null
    lateinit var transactionType: EDashboardItem

    private var detailPageData: DigiPosDataTable? = null
    private var dataToPrintAfterSuccess: DigiPosDataTable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPendingDetailsBinding.inflate(inflater,container, false)
        return binding?.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionType = arguments?.getSerializable("type") as EDashboardItem
        detailPageData = arguments?.getParcelable("data")

        binding?.subHeaderView?.subHeaderText?.text = transactionType.title
        binding?.subHeaderView?.headerImage?.setImageResource(transactionType.res)

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            try {
                parentFragmentManager.popBackStackImmediate()
            }catch (ex:Exception)
            {
                ex.printStackTrace()
            }
        }


        if (detailPageData?.txnStatus?.toLowerCase(Locale.ROOT).equals("success", true)) {
            binding?.printButton?.text = getString(R.string.print)
            binding?.transactionIV?.setImageResource(R.drawable.ic_init_payment_success)
            val message = "Transaction ${detailPageData?.txnStatus}"
            binding?.transactionMessageTV?.text = message
            binding?.printButton?.text = getString(R.string.print)
        } else {
            binding?.printButton?.text = getString(R.string.getStatus)
            binding?.transactionIV?.setImageResource(R.drawable.ic_init_payment_null)
            binding?.printButton?.text = getString(R.string.getStatus)
            val message = "Transaction ${detailPageData?.txnStatus}"
            binding?.transactionMessageTV?.text = message
        }

        val amountData = "\u20B9${detailPageData?.amount}"
        binding?.transactionAmountTV?.text = amountData
        binding?.transactionDateTime?.text = detailPageData?.displayFormatedDate
        binding?.paymentModeTV?.text = detailPageData?.paymentMode
        binding?.mobileNumberTV?.text = detailPageData?.customerMobileNumber
        binding?.ptxnTV?.text = detailPageData?.partnerTxnId
        binding?.mtxnTV?.text = detailPageData?.mTxnId
        binding?.txnStatusTV?.text = detailPageData?.txnStatus

        //OnClick event of Bottom Button:-
        binding?.printButton?.setOnClickListener {
            if (binding?.printButton?.text.toString() == getString(R.string.print)) {
                dataToPrintAfterSuccess?.let { it1 ->
                    PrintUtil(context).printSMSUPIChagreSlip(
                        it1,
                        EPrintCopyType.DUPLICATE,
                        context
                    ) { alertCB, printingFail ->
                        //context.hideProgress()
                        if (!alertCB) {
                            parentFragmentManager.popBackStack()

                        }
                    }
                }

            } else {
                getTransactionStatus()
            }
        }

    }

    //region=========================Get Transaction Status:-
    private fun getTransactionStatus() {
        (activity as BaseActivityNew).showProgress()
        lifecycleScope.launch(Dispatchers.IO) {
            val req57 =
                EnumDigiPosProcess.GET_STATUS.code + "^" + detailPageData?.partnerTxnId + "^" + detailPageData?.partnerTxnId + "^"
            Log.d("Field57:- ", req57)
            getDigiPosStatus(
                req57,
                EnumDigiPosProcessingCode.DIGIPOSPROCODE.code,
                false
            ) { isSuccess, responseMsg, responsef57, fullResponse ->
                try {
                    if (isSuccess) {
                        //  val statusRespDataList = responsef57.split("^")
                        //   val status = statusRespDataList[5]

                        (activity as BaseActivityNew)?.hideProgress()
                        lifecycleScope.launch(Dispatchers.Main) {

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
                            tabledata.transactionTimeStamp = statusRespDataList[7]
                            tabledata.displayFormatedDate =
                                getDateInDisplayFormatDigipos(statusRespDataList[7])
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
                                    tabledata.txnStatus = statusRespDataList[5]
                                    ToastUtils.showToast(requireContext(),getString(R.string.txn_status_still_pending))
                                }

                                EDigiPosPaymentStatus.Approved.desciption -> {
                                    tabledata.txnStatus = statusRespDataList[5]
                                    binding?.transactionIV?.setImageResource(R.drawable.circle_with_tick_mark_green)
                                    val message = "Transaction ${tabledata.txnStatus}"
                                    binding?.transactionMessageTV?.text = message
                                    binding?.txnStatusTV?.text = tabledata.txnStatus
                                    binding?.printButton?.text = getString(R.string.print)
                                    dataToPrintAfterSuccess = tabledata
                                }

                                else -> {
                                    tabledata.txnStatus =
                                        statusRespDataList[5]
                                    ToastUtils.showToast(requireContext(),statusRespDataList[5])
                                    deleteDigiposData(tabledata.partnerTxnId)
                                }
                            }
                            insertOrUpdateDigiposData(tabledata)
                            val dp = selectAllDigiPosData()
                            val dpObj = Gson().toJson(dp)
                            logger(LOG_TAG.DIGIPOS.tag, "--->      $dpObj ")
                            Log.e("F56->>", responsef57)

                        }
                    } else {
                        lifecycleScope.launch(Dispatchers.Main) {
                            (activity as BaseActivityNew)?.hideProgress()
                            (activity as BaseActivityNew)?.alertBoxWithAction(
                                getString(R.string.error), responseMsg,
                                false, getString(R.string.positive_button_ok),
                                {}, {})
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