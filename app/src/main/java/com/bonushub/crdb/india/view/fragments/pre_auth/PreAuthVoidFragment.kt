package com.bonushub.crdb.india.view.fragments.pre_auth

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentPreAuthCompleteDetailBinding
import com.bonushub.crdb.india.databinding.ItemVoidPreauthDialogBinding
import com.bonushub.crdb.india.transactionprocess.StubBatchData
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.printerUtils.checkForPrintReversalReceipt
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.base.BaseActivityNew
import com.bonushub.crdb.india.view.base.IDialog
import com.bonushub.crdb.india.view.fragments.AuthCompletionData
import com.bonushub.crdb.india.viewmodel.PreAuthViewModel
import com.bonushub.crdb.india.vxutils.dateFormaterNew
import com.bonushub.crdb.india.vxutils.invoiceWithPadding
import com.bonushub.crdb.india.vxutils.timeFormaterNew
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class PreAuthVoidFragment : Fragment() {


    var binding :FragmentPreAuthCompleteDetailBinding? = null
    lateinit var eDashBoardItem:EDashboardItem
    private val authData: AuthCompletionData by lazy { AuthCompletionData() }
    lateinit var preAuthViewModel : PreAuthViewModel
    private var iDialog: IDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        //binding = FragmentPreAuthVoidBinding.inflate(layoutInflater, container, false)
        binding = FragmentPreAuthCompleteDetailBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        iDialog = (activity as NavigationActivity)

        eDashBoardItem = (arguments?.getSerializable("type")) as EDashboardItem

        binding?.subHeaderView?.subHeaderText?.text = eDashBoardItem.title
        binding?.subHeaderView?.headerImage?.setImageResource(eDashBoardItem.res)

        binding?.amtCrdView?.visibility = View.GONE
        binding?.authCompleteBtn?.text = "Void PreAuth"

        preAuthViewModel = ViewModelProvider(this).get(PreAuthViewModel::class.java)

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }

        binding?.authCompleteBtn?.setOnClickListener {

            authData.authTid = binding?.tidEt?.text.toString()
            authData.authBatchNo = binding?.batchNo?.text.toString()
            authData.authRoc = binding?.rocNo?.text.toString()


            when {
                authData.authTid.isNullOrBlank() -> {
                    ToastUtils.showToast(activity,"Enter TID")
                    return@setOnClickListener
                }
                authData.authBatchNo.isNullOrBlank() -> {
                    ToastUtils.showToast(activity,"Enter batch Number")
                    return@setOnClickListener
                }
                authData.authRoc.isNullOrBlank() -> {
                    ToastUtils.showToast(activity,"Enter ROC")
                    return@setOnClickListener
                }
                else -> {
                    // voidAuthDataCreation(authData)
                    confirmationDialog(authData)
                }
            }

        }

        preAuthViewModel.voidPreAuthData.observe(viewLifecycleOwner) {
            when (it.apiStatus) {

                ApiStatus.Success -> {
                    logger("ApiStatus", "Success", "e")
                    iDialog?.hideProgress()
                    dialogBuilder.dismiss()
                    // stub batch data

                    StubBatchData(
                        "",
                        it.cardProcessedDataModal.getTransType(),
                        it.cardProcessedDataModal,
                        null,
                        it.isoResponse ?: ""
                    ) { stubbedData ->

                        Utility().saveTempBatchFileDataTable(stubbedData)

                        val transactionDate = dateFormaterNew(
                            it.cardProcessedDataModal.getTimeStamp()?.toLong() ?: 0L
                        )
                        val transactionTime =
                            timeFormaterNew(it.cardProcessedDataModal.getTime() ?: "")

                        var amt = ""
                        try {
                            amt =
                                (((stubbedData.transactionalAmmount)?.toDouble())?.div(100)).toString()
                            amt = "%.2f".format(amt.toDouble())
                        } catch (ex: Exception) {
                            amt = "0.00"
                        }

                        iDialog?.txnApprovedDialog(
                            EDashboardItem.PENDING_PREAUTH.res,
                            EDashboardItem.PENDING_PREAUTH.title,
                            amt,
                            "${transactionDate}, ${transactionTime}"
                        ) {
                                status , dialog ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                withContext(Dispatchers.Main) {
                                    printChargeSlip(
                                        (activity as BaseActivityNew),
                                        EPrintCopyType.MERCHANT,
                                        stubbedData
                                    ) { it ->


                                        Handler(Looper.getMainLooper()).postDelayed({

                                            (activity as NavigationActivity).alertBoxWithActionNew("",
                                                getString(R.string.print_customer_copy),
                                                R.drawable.ic_print_customer_copy,
                                                getString(R.string.positive_button_yes),
                                                getString(R.string.no),
                                                true,
                                                false,
                                                {
                                                    lifecycleScope.launch(Dispatchers.IO)
                                                    {
                                                        withContext(Dispatchers.Main) {
                                                            printChargeSlip(
                                                                (activity as BaseActivityNew),
                                                                EPrintCopyType.CUSTOMER,
                                                                stubbedData
                                                            ) { it ->
                                                                dialog.dismiss()
                                                                gotoDashboard()
                                                            }
                                                        }
                                                    }
                                                },
                                                {
                                                    dialog.dismiss()
                                                    gotoDashboard()
                                                })

                                        }, 1000)


                                    }
                                }

                            }

                        }

                    }

                    //  mAdapter.refreshListRemoveAt(updatedPosition)

                }

                ApiStatus.Processing -> {
                    logger("ApiStatus", "Processing", "e")
                    iDialog?.showProgress(it.msg ?: "Getting Pending Pre-Auth From Server")

                }

                ApiStatus.Failed -> {
                    logger("ApiStatus", "Failed", "e")
                    iDialog?.hideProgress()
                    dialogBuilder.dismiss()

                    if (it.isReversal) {
                        iDialog?.alertBoxWithActionNew(
                            getString(R.string.reversal),
                            getString(R.string.reversal_upload_fail),
                            R.drawable.ic_info_new,
                            getString(R.string.positive_button_ok),
                            "", false, false,
                            {},
                            {})
                    } else {
                        if (it.msg.equals("Declined")) {

                            iDialog?.showProgress(getString(R.string.printing))
                            checkForPrintReversalReceipt(context,"") {
                                iDialog?.hideProgress()
                                lifecycleScope.launch(Dispatchers.Main){
                                    iDialog?.alertBoxWithActionNew(
                                        "Declined",
                                        "Transaction Declined",
                                        R.drawable.ic_info_new,
                                        getString(R.string.positive_button_ok),
                                        "", false, false,
                                        { alertPositiveCallback ->
                                            //gotoDashboard()
                                        },
                                        {})
                                }

                            }

                        } else {
                            iDialog?.alertBoxWithActionNew(
                                getString(R.string.error_hint),
                                it.msg ?: "",
                                R.drawable.ic_info_new,
                                getString(R.string.positive_button_ok),
                                "", false, false,
                                { alertPositiveCallback ->
                                    // gotoDashboard()
                                },
                                {})
                        }
                    }

                }
                else -> {
                    logger("ApiStatus", "nothing", "e")

                }
            }
        }
    }

    lateinit var dialogBuilder : Dialog
    private fun confirmationDialog(authData: AuthCompletionData) {

        dialogBuilder = Dialog(requireActivity())
        val bindingg = ItemVoidPreauthDialogBinding.inflate(LayoutInflater.from(context))

        dialogBuilder.setContentView(bindingg.root)

        dialogBuilder.setCancelable(true)
        val window = dialogBuilder.window
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )


        val batchNo =  invoiceWithPadding(authData.authBatchNo.toString())
        bindingg.batchNoTv.text = batchNo
        val roc = invoiceWithPadding(authData.authRoc.toString())
        bindingg.rocTv.text = roc
        val tid = authData.authTid
        bindingg.tidTv.text = tid

        bindingg.cancelBtn.setOnClickListener {
            dialogBuilder.dismiss()
        }

        bindingg.confirmBtn.setOnClickListener {
            Log.e("preAuth","comp")

            voidPreAuth(authData)
        }

        dialogBuilder.show()
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

    }

    private fun voidPreAuth(authData: AuthCompletionData) {

        lifecycleScope.launch(Dispatchers.IO) {
            preAuthViewModel.getVoidPreAuthData(authData)
        }
    }

    private fun gotoDashboard() {

        startActivity(Intent(activity, NavigationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })

    }
}