package com.bonushub.crdb.view.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentVoidMainBinding
import com.bonushub.crdb.di.DBModule
import com.bonushub.crdb.model.local.BatchTable
import com.bonushub.crdb.utils.*
import com.bonushub.crdb.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.utils.printerUtils.PrintUtil
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.activity.NavigationActivity.Companion.TAG
import com.bonushub.crdb.viewmodel.BatchFileViewModel
import com.bonushub.pax.utils.EPrintCopyType
import com.google.gson.Gson
import com.ingenico.hdfcpayment.listener.OnPaymentListener
import com.ingenico.hdfcpayment.model.ReceiptDetail
import com.ingenico.hdfcpayment.request.VoidRequest
import com.ingenico.hdfcpayment.response.PaymentResult
import com.ingenico.hdfcpayment.response.TransactionResponse
import com.ingenico.hdfcpayment.type.ResponseCode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

@AndroidEntryPoint
class VoidMainFragment : Fragment() {


    var binding:FragmentVoidMainBinding? = null
    private val batchFileViewModel: BatchFileViewModel by viewModels()

    private var tid ="000000"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
         binding = FragmentVoidMainBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // set header
        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.void_sale)
        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_void)


        lifecycleScope.launch(Dispatchers.IO) {
            tid=  getBaseTID(DBModule.appDatabase.appDao)
        }

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }

        binding?.txtViewSearchTransaction?.setOnClickListener {
            logger("txtViewSearchTransaction","click")
            //doVoidTransaction()
            searchTransaction()

            // (activity as NavigationActivity).transactFragment(VoidDetailFragment())
        }
    }

    private fun doVoidTransaction(){
        var ecrID: String
        try {
            DeviceHelper.doVoidTransaction(
                VoidRequest(
                    tid = tid,
                    invoice =  binding?.edtTextSearchTransaction?.text.toString(),
                    transactionUuid = UUID.randomUUID().toString().also {
                        ecrID = it

                    }
                ),
                listener = object : OnPaymentListener.Stub() {
                    override fun onCompleted(result: PaymentResult?) {
                        val txnResponse = result?.value as? TransactionResponse
                        val receiptDetail = txnResponse?.receiptDetail

                        Log.d(TAG, "Response Code: ${txnResponse?.responseCode}")
                        when (txnResponse?.responseCode) {
                            ResponseCode.SUCCESS.value -> {
                                val jsonResp= Gson().toJson(receiptDetail)
                                println(jsonResp)
                                val batchTable =BatchTable(receiptDetail)
                                if (receiptDetail != null) {
                                 printingSaleData(batchTable)
                                }


                            }
                            ResponseCode.FAILED.value,
                            ResponseCode.ABORTED.value -> {
                                //  detailResponse.forEach { println(it) }
                                /* if (receiptDetail != null) {
                                     val jsonstr="{\"aid\":\"A0000000041010\",\"appName\":\"Debit MasterCard\",\"authCode\":\"006538\",\"batchNumber\":\"000001\",\"cardHolderName\":\"INSTA DEBIT CARD         /\",\"cardType\":\"UP        \",\"cvmRequiredLimit\":0,\"cvmResult\":\"NO_CVM\",\"dateTime\":\"24/11/2021 14:49:00\",\"entryMode\":\"INSERT\",\"invoice\":\"000012\",\"isSignRequired\":false,\"isVerifyPin\":true,\"merAddHeader1\":\"INGBH TEST2 TID\",\"merAddHeader2\":\"NOIDA\",\"mid\":\"               \",\"rrn\":\"000000000381\",\"stan\":\"000381\",\"tc\":\"1DF19BD576739835\",\"tid\":\"30160035\",\"tsi\":\"E800\",\"tvr\":\"0840048000\",\"txnAmount\":\"5888\",\"txnName\":\"SALE\",\"txnResponseCode\":\"00\"}"
                                    val obj=Gson().fromJson(jsonstr,ReceiptDetail::class.java)
                                    startPrinting(obj)
                                    *//* val intent=Intent(this@TransactionActivity,PrintingTesting::class.java)
                                            startActivity(intent)*//*

                                        }*/
                            }
                            else -> {
                             /*   val intent = Intent (this, NavigationActivity::class.java)
                                startActivity(intent)*/

                                println("Error")}
                        }
                    }
                }
            )
        }
        catch (exc: Exception){
            exc.printStackTrace()
        }
    }

    fun printingSaleData(batchTable: BatchTable) {
        lifecycleScope.launch(Dispatchers.Main) {
            (activity as NavigationActivity).showProgress(getString(R.string.printing))

            var printsts = false
            PrintUtil(activity as NavigationActivity).startPrinting(
                batchTable,
                EPrintCopyType.MERCHANT,
                (activity as NavigationActivity)
            ) { printCB, printingFail ->

                ((activity as NavigationActivity)).hideProgress()
                if (printCB) {
                    printsts = printCB
                    //GlobalScope.launch(Dispatchers.Main) {
                        showMerchantAlertBox(batchTable)
                   // }

                } else {
                    ToastUtils.showToast(activity,getString(R.string.printer_error))
                }
            }

            //((activity as NavigationActivity)).hideProgress()
        }
    }

    private fun showMerchantAlertBox(
        batchTable: BatchTable
    ) {
        lifecycleScope.launch(Dispatchers.Main) {
            (activity as NavigationActivity).showProgress(getString(R.string.printing))
            val printerUtil: PrintUtil? = null
            (activity as NavigationActivity).alertBoxWithAction(
                getString(R.string.print_customer_copy),
                getString(R.string.print_customer_copy),
                true, getString(R.string.positive_button_yes), { status ->

                    (activity as NavigationActivity).hideProgress()
                    PrintUtil(activity as NavigationActivity).startPrinting(
                        batchTable,
                        EPrintCopyType.CUSTOMER,
                        activity as NavigationActivity
                    ) { printCB, printingFail ->
                        if (printCB) {
                            (activity as NavigationActivity).transactFragment(DashboardFragment())
                        }

                    }
                }, {
                    (activity as NavigationActivity).hideProgress()
                    (activity as NavigationActivity).transactFragment(DashboardFragment())
                })
        }
    }

    private fun searchTransaction()
    {
        val invoice = binding?.edtTextSearchTransaction?.text.toString()
        lifecycleScope.launch {
            batchFileViewModel.getBatchTableDataByInvoice(invoiceWithPadding(invoice)).observe(viewLifecycleOwner, { batchTable ->

                if(batchTable?.receiptData != null) {

                    val date = batchTable.receiptData?.dateTime ?: ""
                    val parts = date.split(" ")
                    println("Date: " + parts[0])
                    println("Time: " + (parts[1]) )
                    val amt ="%.2f".format((((batchTable.receiptData?.txnAmount ?: "")?.toDouble())?.div(100)).toString().toDouble())
                    DialogUtilsNew1.showVoidSaleDetailsDialog(
                        requireContext(),
                        parts[0],
                        parts[1],
                        batchTable.receiptData?.tid ?: "",
                        batchTable.receiptData?.invoice ?: "",
                        amt
                    ) {
                        doVoidTransaction()
                    }
                }else{
                    ToastUtils.showToast(requireContext(),"Data not found.")
                }
            })
        }

    }
}