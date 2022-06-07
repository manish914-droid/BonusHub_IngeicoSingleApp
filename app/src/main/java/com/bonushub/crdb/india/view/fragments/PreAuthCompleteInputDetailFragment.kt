package com.bonushub.crdb.india.view.fragments

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
import com.bonushub.crdb.india.databinding.ItemCompletePreauthDialogBinding
import com.bonushub.crdb.india.databinding.ItemPendingPreauthBinding
import com.bonushub.crdb.india.di.DBModule
import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.transactionprocess.StubBatchData
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.showToast
import com.bonushub.crdb.india.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.india.utils.printerUtils.checkForPrintReversalReceipt
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.base.BaseActivityNew
import com.bonushub.crdb.india.view.base.IDialog
import com.bonushub.crdb.india.view.fragments.pre_auth.PendingPreauthData
import com.bonushub.crdb.india.viewmodel.PreAuthViewModel
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.util.*

@AndroidEntryPoint
class PreAuthCompleteInputDetailFragment : Fragment() {
   // private val title: String by lazy { arguments?.getString(MainActivity.INPUT_SUB_HEADING) ?: "" }
    private val cardProcessedData: CardProcessedDataModal by lazy { CardProcessedDataModal() }
    private val authData: AuthCompletionData by lazy { AuthCompletionData() }
    private var binding: FragmentPreAuthCompleteDetailBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPreAuthCompleteDetailBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }



    lateinit var eDashBoardItem:EDashboardItem
    lateinit var preAuthViewModel : PreAuthViewModel

    private var iDialog: IDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        iDialog = (activity as NavigationActivity)
        eDashBoardItem = (arguments?.getSerializable("type")) as EDashboardItem

//        binding?.subHeaderView?.subHeaderText?.text = "PRE-AUTH COMPLETE"
//        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_preauth)

        binding?.subHeaderView?.subHeaderText?.text = eDashBoardItem.title
        binding?.subHeaderView?.headerImage?.setImageResource(eDashBoardItem.res)
       
        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }
        //val baseTid = runBlocking(Dispatchers.IO) { getBaseTID(DBModule.appDatabase.appDao) }
        AmountTextWatcher(binding?.amountEt!!)

        preAuthViewModel = ViewModelProvider(this).get(PreAuthViewModel::class.java)

        preAuthViewModel.completePreAuthData.observe(viewLifecycleOwner){
            when(it.apiStatus){

                ApiStatus.Success ->{
                    logger("ApiStatus","Success","e")
                    iDialog?.hideProgress()
                  //  dialogBuilder.dismiss()
                    // stub batch data

                    StubBatchData("", it.cardProcessedDataModal.getTransType(), it.cardProcessedDataModal, null, it.isoResponse?:""){
                            stubbedData ->

                        Utility().saveTempBatchFileDataTable(stubbedData)

                        val transactionDate = dateFormaterNew(it.cardProcessedDataModal.getTimeStamp()?.toLong() ?: 0L)
                        val transactionTime = timeFormaterNew(it.cardProcessedDataModal.getTime()?:"")

                        var amt = ""
                        try {
                            amt = (((stubbedData.transactionalAmmount)?.toDouble())?.div(100)).toString()
                            amt = "%.2f".format(amt.toDouble())
                        }catch (ex:Exception){
                            amt = "0.00"
                        }

                        iDialog?.txnApprovedDialog(EDashboardItem.PENDING_PREAUTH.res,EDashboardItem.PENDING_PREAUTH.title,amt,
                            "${transactionDate}, ${transactionTime}") { status , dialog ->

                            lifecycleScope.launch(Dispatchers.IO) {
                                withContext(Dispatchers.Main){
                                    printChargeSlip((activity as BaseActivityNew), EPrintCopyType.MERCHANT,stubbedData) { it ->


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
                                                        withContext(Dispatchers.Main){
                                                            printChargeSlip((activity as BaseActivityNew), EPrintCopyType.CUSTOMER,stubbedData) { it ->

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

                ApiStatus.Processing ->{
                    logger("ApiStatus","Processing","e")
                    iDialog?.showProgress(it.msg?:"Getting Pending Pre-Auth From Server")

                }

                ApiStatus.Failed ->{
                    logger("ApiStatus","Failed","e")
                    iDialog?.hideProgress()
                  //  dialogBuilder.dismiss()

                    if(it.isReversal){
                        iDialog?.alertBoxWithActionNew(
                            getString(R.string.reversal),
                            getString(R.string.reversal_upload_fail),
                            R.drawable.ic_info_new,
                            getString(R.string.positive_button_ok),
                            "",false,false,
                            {},
                            {})
                    }else {
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
                    logger("ApiStatus","nothing","e")

                }
            }
        }

        binding?.authCompleteBtn?.setOnClickListener {

            authData.authTid = binding?.tidEt?.text.toString()
            authData.authAmt = binding?.amountEt?.text.toString()
            authData.authBatchNo = binding?.batchNo?.text.toString()
            authData.authRoc = binding?.rocNo?.text.toString()

            if (authData.authTid.isNullOrBlank() || authData.authTid!!.length < 8) {
                showToast("Invalid TID")
                return@setOnClickListener
            } else if (authData.authBatchNo.isNullOrBlank()) {
                showToast("Invalid Batch No")
                return@setOnClickListener
            }else if (authData.authRoc.isNullOrBlank()) {
                showToast("Invalid ROC")
                return@setOnClickListener
            }  else if (authData.authAmt.isNullOrBlank() || authData.authAmt!!.toDouble() < 1) {
                showToast("Invalid Amount")
                return@setOnClickListener
            } else {
                Log.e("preAuth","dialog")

                /*(activity as NavigationActivity).onFragmentRequest(
                    EDashboardItem.PREAUTH_COMPLETE,
                    Pair(authData, "")
                )*/
                preAuthConfirmDialog(authData)
            }


        }
    }

//    lateinit var dialogBuilder : Dialog
    private fun preAuthConfirmDialog(authData: AuthCompletionData) {

        DialogUtilsNew1.showDetailsConfirmDialog(requireContext(), transactionType = BhTransactionType.PRE_AUTH_COMPLETE,
            tid = authData.authTid, totalAmount = null, invoice = null, date = null, time = null,
            amount = authData.authAmt, batchNo = invoiceWithPadding(authData.authBatchNo.toString()), roc = invoiceWithPadding(authData.authRoc.toString()),
            confirmCallback = {
                it.dismiss()
                (activity as NavigationActivity).alertBoxWithActionNew("","Do you want to PreAuth Complete this transaction?"
                    ,R.drawable.ic_info_orange,"YES"," NO ",true,false,{

                        lifecycleScope.launch(Dispatchers.IO) {
                            preAuthViewModel.getCompletePreAuthData(authData)
                        }

                    },{
                    })
            },
            cancelCallback = {
                it.dismiss()
            })

        /*dialogBuilder = Dialog(requireActivity())
        val bindingg = ItemCompletePreauthDialogBinding.inflate(LayoutInflater.from(context))

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
        //val amt = "%.2f".format(authData.authAmt)
        bindingg.amtTv.text = authData.authAmt

        bindingg.cancelBtn.setOnClickListener {
            dialogBuilder.dismiss()
        }

        bindingg.confirmBtn.setOnClickListener {
            Log.e("preAuth","comp")

            lifecycleScope.launch(Dispatchers.IO) {
                preAuthViewModel.getCompletePreAuthData(authData)
            }
        }

        dialogBuilder.show()
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))*/

    }

    private fun gotoDashboard() {

        startActivity(Intent(activity, NavigationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })

    }




}

class AuthCompletionData :Serializable{
    var authTid: String? = ""
    var authAmt: String? = ""
    var authInvoice: String? = ""
    var authBatchNo: String? = ""
    var authRoc: String? = ""
}