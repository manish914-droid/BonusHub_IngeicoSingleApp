package com.bonushub.crdb.view.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.HDFCApplication.Companion.appContext
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentReportsBinding
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.model.local.BatchFileDataTable
import com.bonushub.crdb.model.local.BrandEMIDataTable
import com.bonushub.crdb.utils.ToastUtils
import com.bonushub.crdb.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.utils.logger
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.adapter.ReportsAdapter
import com.bonushub.crdb.view.base.IDialog
import com.bonushub.crdb.viewmodel.BatchFileViewModel
import com.bonushub.pax.utils.EPrintCopyType
import com.bonushub.pax.utils.ReportsItem
import com.bonushub.pax.utils.TransactionType
import com.bonushub.pax.utils.VxEvent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class ReportsFragment : Fragment(), IReportsFragmentItemClick {

    private val reportsItemList: MutableList<ReportsItem> by lazy { mutableListOf<ReportsItem>() }
    private var iReportsFragmentItemClick: IReportsFragmentItemClick? = null

    var binding: FragmentReportsBinding? = null

    private var iDiag: IDialog? = null

    private var batchFileViewModel:BatchFileViewModel? = null

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

        //   iDiag?.onEvents(VxEvent.ChangeTitle(option.name))

        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.reports_header)
        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_reports)

        batchFileViewModel = ViewModelProvider(this).get(BatchFileViewModel::class.java)

        iReportsFragmentItemClick = this

        reportsItemList.clear()
        reportsItemList.addAll(ReportsItem.values())
        setupRecyclerview()


        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
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


                val lastReceiptData = AppPreference.getLastSuccessReceipt()
                if (lastReceiptData != null) {
                    GlobalScope.launch(Dispatchers.Main) {
                        iDiag?.showProgress(getString(R.string.printing_last_receipt))
                    }
                    when (lastReceiptData.transactionType) {
                        TransactionType.SALE.type, TransactionType.TIP_SALE.type, TransactionType.REFUND.type, TransactionType.VOID.type, TransactionType.SALE_WITH_CASH.type, TransactionType.CASH_AT_POS.type, TransactionType.VOID_EMI.type -> {
                        //BB
                            logger("print","util")
                        /*PrintUtil(activity).startPrinting(lastReceiptData,
                                EPrintCopyType.DUPLICATE,
                                activity
                            ) { printCB, printingFail ->
                                if (printCB) {
                                    iDiag?.hideProgress()
                                    Log.e("PRINTING", "LAST_RECEIPT")
                                } else {
                                    iDiag?.hideProgress()
                                }
                            }*/
                        }
                        TransactionType.EMI_SALE.type, TransactionType.TEST_EMI.type -> {
                            //BB
                            logger("print","util")
                        /*PrintUtil(activity).printEMISale(
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
                            }*/
                        }
                        TransactionType.BRAND_EMI.type, TransactionType.BRAND_EMI_BY_ACCESS_CODE.type -> {

                            runBlocking(Dispatchers.IO){
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
                            }


                        }
                        TransactionType.PRE_AUTH_COMPLETE.type -> {
                            //BB
                            logger("print","util")
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
                        }
                        TransactionType.VOID_PREAUTH.type -> {
                            //BB
                            logger("print","util")
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
                        }
                        TransactionType.OFFLINE_SALE.type -> {
                            activity?.let {
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
                            }
                        }
                        TransactionType.VOID_REFUND.type -> {
                        //BB
                        /* VoidRefundSalePrintReceipt().startPrintingVoidRefund(
                                lastReceiptData,
                                TransactionType.VOID_REFUND.type,
                                EPrintCopyType.DUPLICATE,
                                activity
                            ) { _, _ ->
                                iDiag?.hideProgress()
                            }*/
                        }
                        else -> {
                            lifecycleScope.launch(Dispatchers.Main) {
                                iDiag?.hideProgress()
                                ToastUtils.showToast(requireContext(),"Report not found")
                            }
                        }
                    }
                } else {
                    lifecycleScope.launch(Dispatchers.Main) {
                        //    iDiag?.hideProgress()
                        //    iDiag?.showToast(getString(R.string.empty_batch))
                        //-
                        lifecycleScope.launch(Dispatchers.Main) {
                            iDiag?.alertBoxWithAction(
                                getString(R.string.empty_batch),
                                getString(R.string.last_receipt_not_available),
                                false,
                                getString(R.string.positive_button_ok),
                                {},
                                {})
                        }
                    }
                }

                logger("repost", ReportsItem.LAST_RECEIPT._name)
                // DialogUtilsNew1.showMsgOkDialog(activity,getString(R.string.empty_batch),getString(R.string.last_receipt_not_available), false)

            }
// End of Last Receipt case

            ReportsItem.LAST_CANCEL_RECEIPT -> {
                logger("repost", ReportsItem.LAST_CANCEL_RECEIPT._name)

                val isoW = AppPreference.getReversal()
                if (isoW != null) {
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
                    GlobalScope.launch(Dispatchers.Main) {
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

                DialogUtilsNew1.getInputDialog(requireContext(), "Enter Invoice Number", "", true) { invoice ->
                        //   iDiag?.showProgress()
                        iDiag?.showProgress(getString(R.string.printing_receipt))
                        lifecycleScope.launch {
                            try {
                                //val bat= BatchFileDataTable.selectAnyReceipts(invoice) // converted
                                batchFileViewModel?.getBatchTableDataByInvoice(invoice)?.observe(viewLifecycleOwner, { bat ->

                                    when (bat?.size) {
                                        0 -> {
                                            launch(Dispatchers.Main) {
                                                iDiag?.hideProgress()
                                                lifecycleScope.launch(Dispatchers.Main) {
                                                    iDiag?.alertBoxWithAction(
                                                        getString(R.string.invalid_invoice),
                                                        getString(R.string.invoice_is_invalid),
                                                        false,
                                                        getString(R.string.positive_button_ok),
                                                        {},
                                                        {})
                                                }
                                            }
                                        }
                                        1 -> {
                                            //printAnyReceipt(bat[0]) //BB
                                        }
                                        else -> {
                                            lifecycleScope.launch(Dispatchers.Main) {
                                                iDiag?.hideProgress()
                                               // printAnyreceiptTransInvoicesDialog(bat as java.util.ArrayList<BatchFileDataTable>) //BB
                                            }
                                        }
                                    }
                                })



                            } catch (ex: Exception) {
                                ex.printStackTrace()
                                launch(Dispatchers.Main) {
                                    iDiag?.hideProgress()
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        iDiag?.alertBoxWithAction(
                                            getString(R.string.error),
                                            "something Wrong",
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
// End of Any Receipt case

            ReportsItem.DETAIL_REPORT -> {
                logger("repost", ReportsItem.DETAIL_REPORT._name)
                //val batchData = BatchFileDataTable.selectBatchData()
                lifecycleScope.launch{
                    batchFileViewModel?.getBatchTableData()?.observe(viewLifecycleOwner,{ batchData ->

                        if (batchData.isNotEmpty()) {
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

                    })
                }


            }
// End of Detail report

            ReportsItem.SUMMERY_REPORT -> {
                logger("repost", ReportsItem.SUMMERY_REPORT._name)
               // (activity as NavigationActivity).transactFragment(ReportSummaryFragment(), true)

             //   val batList = BatchFileDataTable.selectBatchData()
                lifecycleScope.launch{
                    batchFileViewModel?.getBatchTableData()?.observe(viewLifecycleOwner,{ batList ->

                        if (batList.isNotEmpty()) {
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
                                            try {
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

                    })
                }


            }
// End of Summery Report

            ReportsItem.LAST_SUMMERY_REPORT -> {
                logger("repost", ReportsItem.LAST_SUMMERY_REPORT._name)

                val str = AppPreference.getString(AppPreference.LAST_BATCH)
                val batList = Gson().fromJson<List<BatchFileDataTable>>(
                    str,
                    object : TypeToken<List<BatchFileDataTable>>() {}.type
                )
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
                                val str1 = AppPreference.getString(AppPreference.LAST_BATCH)
                                val batList1 = Gson().fromJson<List<BatchFileDataTable>>(
                                    str1,
                                    object : TypeToken<List<BatchFileDataTable>>() {}.type
                                )

                                if (batList1 != null) {
                                    try {
                                        //BB
                                        logger("print","util")
                                        /*PrintUtil(context).printSettlementReportupdate(
                                            context,
                                            batList1 as MutableList<BatchFileDataTable>,
                                            isSettlementSuccess = false,
                                            isLastSummary = true
                                        ) {
                                            iDiag?.hideProgress()
                                        }*/
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

        }

    }
}

interface IReportsFragmentItemClick {

    fun ReportsOptionItemClick(reportsItem: ReportsItem)
}