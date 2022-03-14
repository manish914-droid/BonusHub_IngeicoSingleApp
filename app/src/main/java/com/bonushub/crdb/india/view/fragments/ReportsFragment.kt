package com.bonushub.crdb.india.view.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentReportsBinding
import com.bonushub.crdb.india.model.local.AppPreference
import com.bonushub.crdb.india.model.local.BatchTable
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.india.utils.printerUtils.PrintUtil
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.adapter.ReportsAdapter
import com.bonushub.crdb.india.view.base.IDialog
import com.bonushub.crdb.india.viewmodel.BatchFileViewModel
import com.bonushub.crdb.india.viewmodel.BatchReversalViewModel
import com.bonushub.crdb.india.viewmodel.SettlementViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReportsFragment : Fragment(), IReportsFragmentItemClick {

    private val reportsItemList: MutableList<ReportsItem> by lazy { mutableListOf<ReportsItem>() }
    private var iReportsFragmentItemClick: IReportsFragmentItemClick? = null
    private val dataList: MutableList<BatchTable> by lazy { mutableListOf<BatchTable>() }
    var binding: FragmentReportsBinding? = null
    private var onlyPreAuthFlag: Boolean? = true
    private var iDiag: IDialog? = null

    private val batchFileViewModel:BatchFileViewModel by viewModels()
    private val settlementViewModel : SettlementViewModel by viewModels()
    private val batchReversalViewModel : BatchReversalViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding?.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        iDiag = (activity as NavigationActivity)

        //iDiag?.onEvents(VxEvent.ChangeTitle(option.name))
       // iDiag?.onEvents(VxEvent.ChangeTitle("Report"))

        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.reports_header)
        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_reports)

        iReportsFragmentItemClick = this

        reportsItemList.clear()
        reportsItemList.addAll(ReportsItem.values())
        setupRecyclerview()


        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            //parentFragmentManager.popBackStackImmediate()
            try {
                (activity as NavigationActivity).decideDashBoardOnBackPress()
            }catch (ex:Exception){
                ex.printStackTrace()

            }
        }

    }

    private fun setupRecyclerview() {
        lifecycleScope.launch(Dispatchers.Main) {
            binding?.let {
                it.recyclerView.layoutManager = GridLayoutManager(activity, 1)
                it.recyclerView.adapter = ReportsAdapter(reportsItemList, iReportsFragmentItemClick)
            }

        }
    }

    override fun ReportsOptionItemClick(reportsItem: ReportsItem) {

        when (reportsItem) {

            ReportsItem.LAST_RECEIPT -> {

                val batchData = AppPreference.getLastSuccessReceipt()

                //val batchData = BatchTable(lastReceiptData)

                if (batchData != null) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        iDiag?.showProgress(getString(R.string.printing_last_receipt))
                    }


                    when (batchData.transactionType) {
                        BhTransactionType.SALE.type, BhTransactionType.CASH_AT_POS.type,
                        BhTransactionType.SALE_WITH_CASH.type, BhTransactionType.REFUND.type,
                            BhTransactionType.PRE_AUTH.type,
                            BhTransactionType.PRE_AUTH_COMPLETE.type,
                        BhTransactionType.VOID.type,
                        BhTransactionType.EMI_SALE.type,
                        BhTransactionType.BRAND_EMI.type,
                        BhTransactionType.TEST_EMI.type -> {
                            //BB
                            logger("print","util")

                            // please uncomment this code to use with batch data for printing
                            PrintUtil(activity).startPrinting(batchData,
                                    EPrintCopyType.DUPLICATE,
                                    activity
                                ) { printCB, printingFail ->
                                    if (printCB) {
                                        iDiag?.hideProgress()
                                        logger("PRINTING", "LAST_RECEIPT")
                                    } else {
                                        iDiag?.hideProgress()
                                    }
                                }

                           /* PrintUtil(activity).startPrinting(lastReceiptData,
                                    EPrintCopyType.DUPLICATE,
                                    activity
                                ) { printCB, printingFail ->
                                    if (printCB) {
                                        iDiag?.hideProgress()
                                        logger("PRINTING", "LAST_RECEIPT")
                                    } else {
                                        iDiag?.hideProgress()
                                    }
                                }*/
                        }

                /*        EDashboardItem.BANK_EMI.title.uppercase() -> {
                            //BB
                            logger("print","util")
                            *//*PrintUtil(activity).printEMISale(
                                    lastReceiptData,
                                    EPrintCopyType.DUPLICATE,
                                    activity
                                ) { printCB, printingFail ->
                                    if (printCB) {
                                        iDiag?.hideProgress()
                                        Log.e("PRINTING", "LAST_RECEIPT")
                                    } else {
                                        iDiag?.hideProgress()
                                    }
                                }*//*
                        }*/
//                        TransactionType.BRAND_EMI.type, TransactionType.BRAND_EMI_BY_ACCESS_CODE.type -> {
//
//                            runBlocking(Dispatchers.IO){
                                //batchFileViewModel?.getBrandEMIDataTable(lastReceiptData.hostInvoice, lastReceiptData.hostTID?:"")?.observe(viewLifecycleOwner,{ brandEmiData ->

                                // BB
                                /*PrintUtil(activity).printEMISale(
                                    lastReceiptData,
                                    EPrintCopyType.DUPLICATE,
                                    activity,
                                    brandEmiData
                                ) { printCB, printingFail ->
                                    if (printCB) {
                                        iDiag?.hideProgress()
                                        Log.e("PRINTING", "LAST_RECEIPT")
                                    } else {
                                        iDiag?.hideProgress()
                                    }
                                }*/

                                //  })
                            /*}


                        }*/
                       // TransactionType.PRE_AUTH_COMPLETE.type -> {
                            //BB
                            /*PrintUtil(activity).printAuthCompleteChargeSlip(
                                lastReceiptData,
                                EPrintCopyType.DUPLICATE,
                                activity
                            ) {
                                if (it) {
                                    iDiag?.hideProgress()
                                    Log.e("PRINTING", "LAST_RECEIPT")
                                } else {
                                    iDiag?.hideProgress()
                                }
                            }*/
                      //  }
                        //TransactionType.VOID_PREAUTH.type -> {
                            //BB
                            /* PrintUtil(activity).printAuthCompleteChargeSlip(
                                 lastReceiptData,
                                 EPrintCopyType.DUPLICATE,
                                 activity
                             ) {
                                 if (it) {
                                     iDiag?.hideProgress()
                                     Log.e("PRINTING", "LAST_RECEIPT")
                                 } else {
                                     iDiag?.hideProgress()
                                 }
                             }*/
                       // }
                       // TransactionType.OFFLINE_SALE.type -> {
                           // activity?.let {
                                //BB
                                /* OfflineSalePrintReceipt().offlineSalePrint(
                                        lastReceiptData, EPrintCopyType.DUPLICATE,
                                        it
                                    ) { printCB, printingFail ->
                                        if (printCB) {
                                            iDiag?.hideProgress()
                                            Log.e("PRINTING", "LAST_RECEIPT")
                                        } else {
                                            iDiag?.hideProgress()
                                        }
                                    }*/
                           // }
                       // }

                        else -> {
                            lifecycleScope.launch(Dispatchers.Main) {
                                iDiag?.hideProgress()
                                ToastUtils.showToast(requireContext(),"Report not found")
                            }
                        }
                    }

                } else {

                   // DialogUtilsNew1.showMsgOkDialog(activity,getString(R.string.empty_batch),getString(R.string.last_receipt_not_available), false)
                    iDiag?.getInfoDialog(getString(R.string.empty_batch),getString(R.string.last_receipt_not_available)){}
                }

                logger("repost", ReportsItem.LAST_RECEIPT._name)

            }
// End of Last Receipt case

            ReportsItem.LAST_CANCEL_RECEIPT -> {
                logger("repost", ReportsItem.LAST_CANCEL_RECEIPT._name)

                val lastCancelReceiptData = AppPreference.getLastCancelReceipt()

                val batchData = BatchTable(lastCancelReceiptData)

                val isoW = AppPreference.getReversal()

                if (lastCancelReceiptData != null) {
                    iDiag?.getMsgDialog(
                        getString(R.string.confirmation),
                        getString(R.string.last_cancel_report_confirmation),
                        "Yes",
                        "No",
                        {

                            GlobalScope.launch(Dispatchers.Main) {
                                iDiag?.showProgress(getString(R.string.printing_last_cancel_receipt))
                            }
                            GlobalScope.launch {
                                try {
                                    // BB
//                                    PrintUtil(context).printReversal(context, "") {
//                                        //  VFService.showToast(it)
//                                        iDiag?.hideProgress()
//                                    }
                                    PrintUtil(activity).startPrinting(batchData,
                                        EPrintCopyType.DUPLICATE,
                                        activity,
                                        true
                                    ) { printCB, printingFail ->
                                        if (printCB) {
                                            iDiag?.hideProgress()
                                            logger("PRINTING", "LAST_CANCEL_RECEIPT")
                                        } else {
                                            iDiag?.hideProgress()
                                        }
                                    }
                                } catch (ex: java.lang.Exception) {
                                    ex.printStackTrace()
                                    iDiag?.hideProgress()
                                }

                            }
                        },
                        {
                            //Cancel Handling
                        })
                } else {
                    lifecycleScope.launch(Dispatchers.Main) {
                        iDiag?.alertBoxWithAction(
                            getString(R.string.no_receipt),
                            getString(R.string.no_cancel_receipt_found),
                            false,
                            getString(R.string.positive_button_ok),
                            {},
                            {})
                    }


                }

            }
// End of last cancel receipt

            ReportsItem.ANY_RECEIPT -> {
                logger("repost", ReportsItem.ANY_RECEIPT._name)

                DialogUtilsNew1.getInputDialog(requireContext(), "Enter Invoice Number", "", true,false,"Invoice Number") { invoice ->

                        iDiag?.showProgress(getString(R.string.printing_receipt))
                        lifecycleScope.launch {
                            try {

                                batchFileViewModel?.getBatchTableDataByInvoice(invoiceWithPadding(invoice))?.observe(viewLifecycleOwner, { bat ->

                                    if(bat?.receiptData != null)
                                    {
                                        // please uncomment this code to use with batch data for printing
                                        /*PrintUtil(activity).startPrinting(
                                            bat,
                                            EPrintCopyType.DUPLICATE,
                                            activity
                                        ) { printCB, printingFail ->
                                            if (printCB) {
                                                iDiag?.hideProgress()
                                                logger("PRINTING", "LAST_RECEIPT")
                                            } else {
                                                iDiag?.hideProgress()
                                            }
                                        }*/

                                        PrintUtil(activity).startPrinting(
                                            bat,
                                            EPrintCopyType.DUPLICATE,
                                            activity
                                        ) { printCB, printingFail ->
                                            if (printCB) {
                                                iDiag?.hideProgress()
                                                logger("PRINTING", "LAST_RECEIPT")
                                            } else {
                                                iDiag?.hideProgress()
                                            }
                                        }
                                    }else{
                                       // launch(Dispatchers.Main) {
                                            iDiag?.hideProgress()
                                            //DialogUtilsNew1.showMsgOkDialog(activity,getString(R.string.invalid_invoice),getString(R.string.invoice_is_invalid), false)
                                            iDiag?.getInfoDialog(getString(R.string.invalid_invoice),getString(R.string.invoice_is_invalid)){}
                                     //   }
                                    }

                                })



                            } catch (ex: Exception) {
                                ex.printStackTrace()
                                //launch(Dispatchers.Main) {
                                    iDiag?.hideProgress()
                                    DialogUtilsNew1.showMsgOkDialog(activity,getString(R.string.error),"something Wrong", false)

                              //  }
                            }
                        }
                    }
            }
// End of Any Receipt case

            ReportsItem.DETAIL_REPORT -> {
                logger("repost", ReportsItem.DETAIL_REPORT._name)
                //val batchData = BatchFileDataTable.selectBatchData()
                lifecycleScope.launch{

                    /*//region===============Get Batch Data from HDFCViewModal:-
                    settlementViewModel.getBatchData()?.observe(requireActivity()) { batchData ->
                        Log.d("TPT Data:- ", batchData.toString())
                        dataList.clear()
                        dataList.addAll(batchData as MutableList<BatchTable>)
                        setUpRecyclerView()
                    }
                    //endregion*/

                    settlementViewModel?.getBatchData()?.observe(viewLifecycleOwner) { batchData ->
                        onlyPreAuthCheck(batchData as MutableList<BatchTable>)
                        if (batchData.isNotEmpty()  && onlyPreAuthFlag == false) {
                            iDiag?.getMsgDialog(
                                getString(R.string.confirmation),
                                getString(R.string.want_print_detail),
                                getString(R.string.yes),
                                getString(R.string.no),
                                {
                                    lifecycleScope.launch {
                                        // val bat = BatchFileDataTable.selectBatchData() // already fetch data in above
                                        if (batchData.isNotEmpty()) {
                                            try {
                                                lifecycleScope.launch(Dispatchers.Main) {
                                                    iDiag?.showProgress(getString(R.string.printing_detail))
                                                }
                                                Log.d("TPT Data:- ", batchData.toString())
                                                dataList.clear()
                                                dataList.addAll(batchData as MutableList<BatchTable>)
                                                PrintUtil(activity).printDetailReportupdate(
                                                    dataList,
                                                    activity
                                                ) { detailPrintStatus ->

                                                }
                                                //BB
//                                                PrintUtil(activity).printDetailReportupdate(batchData, activity) {
//                                                    iDiag?.hideProgress()
//                                                }

                                            } catch (ex: java.lang.Exception) {
                                                ex.message ?: getString(R.string.error_in_printing)
                                                // "catch toast"
                                            } finally {
                                                lifecycleScope.launch(Dispatchers.Main) {
                                                    iDiag?.hideProgress()
                                                    //  iDiag?.showToast(msg)
                                                }
                                            }

                                        } else {
                                            lifecycleScope.launch(Dispatchers.Main) {
                                                iDiag?.hideProgress()
                                                iDiag?.showToast("  Batch is empty.  ")
                                            }
                                        }

                                    }

                                },
                                {
                                    //handle cancel here
                                })
                        } else {
                            lifecycleScope.launch(Dispatchers.Main) {
                                iDiag?.alertBoxWithAction(
                                    getString(R.string.empty_batch),
                                    getString(R.string.detail_report_not_found),
                                    false,
                                    getString(R.string.positive_button_ok),
                                    {},
                                    {})
                            }

                        }

                    }
                }


            }
// End of Detail report

            ReportsItem.SUMMERY_REPORT -> {
                logger("repost", ReportsItem.SUMMERY_REPORT._name)
               // (activity as NavigationActivity).transactFragment(ReportSummaryFragment(), true)

                lifecycleScope.launch{

                        settlementViewModel?.getBatchData()?.observe(viewLifecycleOwner) { batList ->
                            onlyPreAuthCheck(batList as MutableList<BatchTable>)
                            if (batList.isNotEmpty() && onlyPreAuthFlag == false) {
                                iDiag?.getMsgDialog(
                                    getString(R.string.confirmation),
                                    "Do you want to print summary Report",
                                    "Yes",
                                    "No",
                                    {

                                        GlobalScope.launch {
                                            if (batList.isNotEmpty()) {
                                                GlobalScope.launch(Dispatchers.Main) {
                                                    iDiag?.showProgress(
                                                        getString(R.string.printing_summary_report)
                                                    )
                                                }
                                                Log.d("TPT Data:- ", batList.toString())
                                                dataList.clear()
                                                dataList.addAll(batList as MutableList<BatchTable>)
                                                try {
                                                    PrintUtil(activity).printSettlementReportupdate(
                                                        activity,
                                                        dataList,
                                                        false
                                                    ) {

                                                    }
                                                    // region BB
                                                    /*PrintUtil(context).printSettlementReportupdate( context, batList) {
                                                            iDiag?.hideProgress()
                                                        }*/
                                                    // end region
                                                    //  printSummery(batList)
                                                    //  getString(R.string.summery_report_printed)

                                                } catch (ex: java.lang.Exception) {
                                                    //  ex.message ?: getString(R.string.error_in_printing)
                                                    ex.printStackTrace()
                                                } finally {
                                                    launch(Dispatchers.Main) {
                                                        iDiag?.hideProgress()
                                                        // iDiag?.showToast(msg)
                                                    }
                                                }

                                            } else {
                                                launch(Dispatchers.Main) {
                                                    iDiag?.hideProgress()
                                                    iDiag?.getInfoDialog(
                                                        "Error",
                                                        " Summery is not available."
                                                    ) {}
                                                }
                                            }

                                        }
                                    },
                                    {
                                        //Cancel handle here

                                    })
                            } else {
                                GlobalScope.launch(Dispatchers.Main) {
                                    iDiag?.alertBoxWithAction(
                                        getString(R.string.empty_batch),
                                        getString(R.string.summary_report_not_available),
                                        false,
                                        getString(R.string.positive_button_ok),
                                        {},
                                        {})
                                }

                            }

                        }
                }


            }
// End of Summery Report

            ReportsItem.LAST_SUMMERY_REPORT -> {
                logger("repost", ReportsItem.LAST_SUMMERY_REPORT._name)

                val batList = AppPreference.getLastBatch()

                if (batList != null) {
                    iDiag?.getMsgDialog(
                        getString(R.string.confirmation),
                        getString(R.string.last_summary_confirmation),
                        "Yes",
                        "No",
                        {
                            lifecycleScope.launch(Dispatchers.Main) {
                                iDiag?.showProgress(
                                    getString(
                                        R.string.printing_last_summary_report
                                    )
                                )
                            }
                            lifecycleScope.launch {
                                if (batList != null) {
                                    try {
                                        //BB
                                        logger("print","util")
                                        PrintUtil(activity).printSettlementReportupdate(activity, batList as MutableList<BatchTable>, false,true) {

                                        }
                                    } catch (ex: java.lang.Exception) {
                                        ex.message ?: getString(R.string.error_in_printing)
                                    } finally {
                                        launch(Dispatchers.Main) {
                                            iDiag?.hideProgress()
                                            //   iDiag?.showToast(msg)
                                        }
                                    }
                                } else {
                                    launch(Dispatchers.Main) {
                                        iDiag?.hideProgress()
                                        iDiag?.getInfoDialog(
                                            "Error",
                                            "Last summary is not available."
                                        ) {}
                                    }
                                }

                            }
                        },
                        {
                            //Cancel Handling
                        })
                } else {
                    lifecycleScope.launch(Dispatchers.Main) {
                        iDiag?.alertBoxWithAction(
                            getString(R.string.no_receipt),
                            getString(R.string.last_summary_not_available),
                            false,
                            getString(R.string.positive_button_ok),
                            {},
                            {})
                    }
                }

            }
// End of last Summery Report receipt

            ReportsItem.PRINT_REVERSAL_REPORT -> {
               // ToastUtils.showToast(requireContext(), "Not implemented yet.") // batchReversalViewModel
                lifecycleScope.launch{

                    batchReversalViewModel?.getBatchTableReversalData()?.observe(viewLifecycleOwner) { batchReversalList ->

                        if (batchReversalList.isNotEmpty()) {
                            iDiag?.getMsgDialog(
                                getString(R.string.confirmation),
                                "Do you want to print reversal report",
                                "Yes",
                                "No",
                                {

                                    GlobalScope.launch {
                                        if (batchReversalList.isNotEmpty()) {
                                            GlobalScope.launch(Dispatchers.Main) {
                                                iDiag?.showProgress(
                                                    getString(R.string.printing_reversal_report)
                                                )
                                            }
                                            Log.d("batReverData", batchReversalList.toString())
                                           // dataList.clear()
                                           // dataList.addAll(batchReversalList as MutableList<BatchTable>)
                                            try {
                                                Log.e("size==",""+batchReversalList.size)
                                                // please change here batchReversalList
                                                PrintUtil(activity).printReversalReportupdate(
                                                    batchReversalList,
                                                    activity
                                                ) { detailPrintStatus ->

                                                }


                                            } catch (ex: java.lang.Exception) {
                                                //  ex.message ?: getString(R.string.error_in_printing)
                                                ex.printStackTrace()
                                            } finally {
                                                launch(Dispatchers.Main) {
                                                    iDiag?.hideProgress()
                                                    // iDiag?.showToast(msg)
                                                }
                                            }

                                        } else {
                                            launch(Dispatchers.Main) {
                                                iDiag?.hideProgress()
                                                iDiag?.getInfoDialog(
                                                    "Error",
                                                    " Reversal is not available."
                                                ) {}
                                            }
                                        }

                                    }
                                },
                                {
                                    //Cancel handle here

                                })
                        } else {
                            GlobalScope.launch(Dispatchers.Main) {
                                iDiag?.alertBoxWithAction(
                                    getString(R.string.empty_batch),
                                    getString(R.string.reversal_report_not_available),
                                    false,
                                    getString(R.string.positive_button_ok),
                                    {},
                                    {})
                            }

                        }

                    }
                }
            }
        }

    }
    private fun onlyPreAuthCheck(dataList: MutableList<BatchTable>) {
        for (i in 0 until dataList.size) {
            if(dataList[i].transactionType != BhTransactionType.PRE_AUTH.type){
                onlyPreAuthFlag=false
                break
            }

        }
    }
}

interface IReportsFragmentItemClick {

    fun ReportsOptionItemClick(reportsItem: ReportsItem)
}