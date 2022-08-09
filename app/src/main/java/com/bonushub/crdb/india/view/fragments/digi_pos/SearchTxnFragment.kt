package com.bonushub.crdb.india.view.fragments.digi_pos

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentPendingTxnBinding
import com.bonushub.crdb.india.model.local.DigiPosDataTable
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.india.utils.printerUtils.PrintUtil
import com.bonushub.crdb.india.view.base.BaseActivityNew
import com.bonushub.pax.utils.KeyExchanger.Companion.getDigiPosStatus
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SearchTxnFragment : Fragment() {

    lateinit var transactionType: EDashboardItem
    var binding:FragmentPendingTxnBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPendingTxnBinding.inflate(inflater,container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionType = arguments?.getSerializable("type") as EDashboardItem

        binding?.linLayPendingTnx?.visibility = View.GONE
        binding?.linLaySearch?.visibility = View.VISIBLE

        binding?.subHeaderView?.subHeaderText?.text = transactionType.title
        binding?.subHeaderView?.headerImage?.setImageResource(transactionType.res)

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            try {
                DialogUtilsNew1.hideKeyboardIfOpen(requireActivity())
                //parentFragmentManager.popBackStackImmediate()
                parentFragmentManager.popBackStack(DigiPosMenuFragment::class.java.simpleName, 0);
            }catch (ex:Exception)
            {
                ex.printStackTrace()
            }
        }

        binding?.txtViewPendingTxn?.setBackgroundColor(resources.getColor(R.color.txt_color_transparent))
        binding?.txtViewSearch?.setBackgroundColor(resources.getColor(R.color.txt_color))

        binding?.txtViewPendingTxn?.setOnClickListener {
            try {
                DialogUtilsNew1.hideKeyboardIfOpen(requireActivity())
                parentFragmentManager.popBackStackImmediate()
            }catch (ex:Exception)
            {
                ex.printStackTrace()
            }
        }

        binding?.btnSearch?.setOnClickListener {
            DialogUtilsNew1.hideKeyboardIfOpen(requireActivity())
            validateAndHitServer()
        }

    }

    private fun validateAndHitServer() {
        val txn_id_String = binding?.txnIdSearchET?.text.toString()
        if (txn_id_String.length > 2) {
            //  (context as childFragmentManager).getTxnStatus()
            checkTxnStatus(txn_id_String)
        } else
            (activity as BaseActivityNew).showToast("NO Txn ID")

    }

    fun checkTxnStatus(txnId: String){
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                (activity as BaseActivityNew).showProgress()
            }
            val req57 = EnumDigiPosProcess.GET_STATUS.code + "^" + txnId + "^"

            getDigiPosStatus(req57, EnumDigiPosProcessingCode.DIGIPOSPROCODE.code, false) { isSuccess, responseMsg, responsef57, fullResponse ->
                try {
                    (activity as BaseActivityNew).hideProgress()
                    if (isSuccess) {
                        val statusRespDataList = responsef57.split("^")

                        try {
                            val tabledata = DigiPosDataTable()
                            tabledata.requestType = statusRespDataList[0].toInt()
                            //  tabledata.partnerTxnId = statusRespDataList[1]
                            tabledata.status = statusRespDataList[1]
                            tabledata.statusMsg = statusRespDataList[2]
                            tabledata.statusCode = statusRespDataList[3]
                            tabledata.mTxnId = statusRespDataList[4]
                            tabledata.partnerTxnId = statusRespDataList[6]
                            tabledata.transactionTimeStamp = statusRespDataList[7]
                            val dateTime = statusRespDataList[7].split(" ")
                            tabledata.txnDate = dateTime[0]
                            tabledata.txnTime = dateTime[1]
                            tabledata.amount = statusRespDataList[8]
                            tabledata.paymentMode = statusRespDataList[9]
                            tabledata.customerMobileNumber = statusRespDataList[10]
                            tabledata.description = statusRespDataList[11]
                            tabledata.pgwTxnId = statusRespDataList[12]
                            tabledata.txnStatus=statusRespDataList[5]
                            val dpObj = Gson().toJson(tabledata)
                            logger("SEARCH STATUS", "--->      $dpObj ")
                            Log.e("F56->>", responsef57)
                            lifecycleScope.launch(Dispatchers.Main){
                                txnStatusDialog(tabledata)
                            }
                        }catch (ex:Exception){
                            ex.printStackTrace()

                            lifecycleScope.launch(Dispatchers.Main){
                                (activity as BaseActivityNew).alertBoxWithAction(
                                    getString(R.string.failed),
                                    statusRespDataList[1],
                                    false,
                                    getString(R.string.positive_button_ok),
                                    { alertPositiveCallback ->
                                        if (alertPositiveCallback) {
                                            //parentFragmentManager.popBackStack()
                                            parentFragmentManager.popBackStack(DigiPosMenuFragment::class.java.simpleName, 0);
                                        }
                                    },
                                    {})
                            }

                        }

                    }
                    else {
                        lifecycleScope.launch(Dispatchers.Main) {
                            (activity as BaseActivityNew).alertBoxWithAction(
                                getString(R.string.failed),
                                responseMsg,
                                false,
                                getString(R.string.positive_button_ok),
                                { alertPositiveCallback ->
                                    if (alertPositiveCallback) {
                                        //parentFragmentManager.popBackStack()
                                        parentFragmentManager.popBackStack(DigiPosMenuFragment::class.java.simpleName, 0);
                                    }
                                },
                                {})
                        }
                    }

                } catch (ex: Exception) {
                    ex.printStackTrace()
                    logger(LOG_TAG.DIGIPOS.tag, "Somethig wrong... in response data field 57")
                }
            }
        }
    }

    private fun txnStatusDialog(digiData: DigiPosDataTable) {

        // region
        Dialog(requireContext()).apply {
            getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_pending_txn_details)
            setCancelable(false)
            val window = window
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            val txtViewAmount = findViewById<TextView>(R.id.txtViewAmount)
            val txtViewMode = findViewById<TextView>(R.id.txtViewMode)
            val txtViewTXNId = findViewById<TextView>(R.id.txtViewTXNId)
            val txtViewMTXNId = findViewById<TextView>(R.id.txtViewMTXNId)
            val txtViewPhoneNumber = findViewById<TextView>(R.id.txtViewPhoneNumber)
            val txtViewStatus = findViewById<TextView>(R.id.txtViewStatus)
            val txtViewOk = findViewById<TextView>(R.id.txtViewOk)
            val txtViewPrint = findViewById<TextView>(R.id.txtViewPrint)

            var status=""
            if(digiData.txnStatus==EDigiPosPaymentStatus.Approved.desciption){
                txtViewPrint.visibility=View.VISIBLE
            }else{
                txtViewPrint.visibility=View.GONE
            }

            status=digiData.status

            txtViewAmount.text = digiData.amount
            txtViewMode.text = digiData.paymentMode
            txtViewTXNId.text = digiData.partnerTxnId
            txtViewMTXNId.text = digiData.mTxnId
            txtViewPhoneNumber.text = digiData.customerMobileNumber
            txtViewStatus.text = status



            txtViewOk.setOnClickListener {
                dismiss()
              //  parentFragmentManager.popBackStack()
                parentFragmentManager.popBackStack(DigiPosMenuFragment::class.java.simpleName, 0);

            }

            txtViewPrint.setOnClickListener {
                dismiss()

                PrintUtil(context).printSMSUPIChagreSlip(
                    digiData,
                    EPrintCopyType.DUPLICATE,
                    context
                ) { alertCB, printingFail ->
                    //context.hideProgress()
                    if (!alertCB) {
                        dismiss()
                        //parentFragmentManager.popBackStack()
                        parentFragmentManager.popBackStack(DigiPosMenuFragment::class.java.simpleName, 0);

                    }
                }
            }
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }.show()
        // end region

    }
}