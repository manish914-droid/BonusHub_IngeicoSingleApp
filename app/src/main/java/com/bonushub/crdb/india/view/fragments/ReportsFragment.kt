package com.bonushub.crdb.india.view.fragments

import android.os.Bundle
import android.os.SystemClock
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
import com.bonushub.crdb.india.model.local.TempBatchFileDataTable
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.india.utils.printerUtils.PrintUtil
import com.bonushub.crdb.india.utils.printerUtils.PrintVectorUtil
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.adapter.ReportsAdapter
import com.bonushub.crdb.india.view.base.IDialog
import com.bonushub.crdb.india.viewmodel.BatchFileViewModel
import com.bonushub.crdb.india.viewmodel.BatchReversalViewModel
import com.bonushub.crdb.india.viewmodel.SettlementViewModel
import com.bonushub.crdb.india.vxutils.BhTransactionType
import com.bonushub.crdb.india.vxutils.invoiceWithPadding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReportsFragment : Fragment(), IReportsFragmentItemClick {

    private val reportsItemList: MutableList<ReportsItem> by lazy { mutableListOf<ReportsItem>() }
    private var iReportsFragmentItemClick: IReportsFragmentItemClick? = null
    private val dataList: MutableList<TempBatchFileDataTable> by lazy { mutableListOf<TempBatchFileDataTable>() }
    var binding: FragmentReportsBinding? = null
    private var onlyPreAuthFlag: Boolean? = true
    private var iDiag: IDialog? = null

    private val batchFileViewModel:BatchFileViewModel by viewModels()
    private val settlementViewModel : SettlementViewModel by viewModels()
    private val batchReversalViewModel : BatchReversalViewModel by viewModels()

    private lateinit var reportsAdapter:ReportsAdapter

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
        (activity as NavigationActivity).manageTopToolBar(false)

        /*binding?.subHeaderView?.subHeaderText?.text = getString(R.string.reports_header)
        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_reports)*/
        refreshSubToolbarLogos(this,null,R.drawable.ic_reports, getString(R.string.reports_header))

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
        reportsAdapter = ReportsAdapter(reportsItemList, iReportsFragmentItemClick)
        lifecycleScope.launch(Dispatchers.Main) {
            binding?.let {
                it.recyclerView.layoutManager = GridLayoutManager(activity, 1)
                it.recyclerView.adapter = reportsAdapter
            }

        }
    }

    private var lastTimeClicked: Long = 0
    private var defaultInterval: Int = 1500
    override fun ReportsOptionItemClick(reportsItem: ReportsItem, itemPosition:Int) {

        if (SystemClock.elapsedRealtime() - lastTimeClicked < defaultInterval) {
            return
        }
        lastTimeClicked = SystemClock.elapsedRealtime()

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
                            /*PrintUtil(activity).startPrinting(batchData,
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

                            PrintVectorUtil(requireContext()).startPrinting(batchData,
                                EPrintCopyType.DUPLICATE,
                                activity
                            ){
                                    printCB, printingFail ->
                                if (printCB) {
                                    iDiag?.hideProgress()
                                    logger("PRINTING", "LAST_RECEIPT")
                                } else {
                                    iDiag?.hideProgress()
                                }
                            }
                            reportsAdapter.notifyItemChanged(itemPosition)
                        }

                        else -> {
                            lifecycleScope.launch(Dispatchers.Main) {
                                iDiag?.hideProgress()
                                ToastUtils.showToast(requireContext(),"Report not found")
                                reportsAdapter.notifyItemChanged(itemPosition)
                            }
                        }
                    }

                } else {

                   // DialogUtilsNew1.showMsgOkDialog(activity,getString(R.string.empty_batch),getString(R.string.last_receipt_not_available), false)
                   // iDiag?.getInfoDialog(getString(R.string.empty_batch),getString(R.string.last_receipt_not_available)){}
                    iDiag?.alertBoxWithActionNew(
                        getString(R.string.empty_batch),
                        getString(R.string.last_receipt_not_available),
                        R.drawable.ic_info_orange,
                        getString(R.string.positive_button_ok),
                        "",false,false,
                        {
                        reportsAdapter.notifyItemChanged(itemPosition)
                        },
                        {})
                }

                logger("repost", ReportsItem.LAST_RECEIPT._name)

            }
// End of Last Receipt case

            ReportsItem.LAST_CANCEL_RECEIPT -> {
                logger("repost", ReportsItem.LAST_CANCEL_RECEIPT._name)

                val isoW = AppPreference.getReversalNew()
                if (isoW != null) {
                    //iDiag?.getMsgDialog(
                    iDiag?.alertBoxWithActionNew(
                        getString(R.string.confirmation),
                        getString(R.string.last_cancel_report_confirmation),
                        R.drawable.ic_print_customer_copy,
                        "Yes",
                        "No",true,false,
                        {

                            lifecycleScope.launch(Dispatchers.Main) {
                                iDiag?.showProgress(getString(R.string.printing_last_cancel_receipt))
                            }
                            lifecycleScope.launch {
                                try {
                                    PrintVectorUtil(context).printReversal(context, AppPreference.getString(AppPreference.FIELD_60_DATA_REVERSAL_KEY)) {
                                        //  VFService.showToast(it)
                                        iDiag?.hideProgress()
                                    }
                                } catch (ex: java.lang.Exception) {
                                    ex.printStackTrace()
                                    iDiag?.hideProgress()
                                }
                                reportsAdapter.notifyItemChanged(itemPosition)
                            }
                        },
                        {
                            //Cancel Handling
                            reportsAdapter.notifyItemChanged(itemPosition)
                        })
                } else {
                    lifecycleScope.launch(Dispatchers.Main) {
                       // iDiag?.alertBoxWithAction(
                        iDiag?.alertBoxWithActionNew(
                            getString(R.string.no_receipt),
                            getString(R.string.no_cancel_receipt_found),
                            R.drawable.ic_info_orange,
                            getString(R.string.positive_button_ok),
                            "",false,
                            false,
                            {
                                reportsAdapter.notifyItemChanged(itemPosition)
                            },
                            {})
                    }


                }

            }
// End of last cancel receipt

            ReportsItem.ANY_RECEIPT -> {
                logger("repost", ReportsItem.ANY_RECEIPT._name)

                DialogUtilsNew1.getInputDialog(requireContext(), "Enter Invoice Number", "", true,false,"Invoice Number", { invoice ->

                        iDiag?.showProgress(getString(R.string.printing_receipt))
                        lifecycleScope.launch {
                            try {

                                batchFileViewModel?.getTempBatchTableDataListByInvoice(
                                    invoiceWithPadding(invoice)
                                )?.observe(viewLifecycleOwner) { bat ->

                                    if (bat != null && bat.size > 0) {
                                        PrintVectorUtil(activity).startPrinting(
                                            bat.get(0)!!,
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
                                        lifecycleScope.launch(Dispatchers.Main){
                                            reportsAdapter.notifyItemChanged(itemPosition)
                                        }
                                    } else {
                                        // launch(Dispatchers.Main) {
                                        iDiag?.hideProgress()
                                        //DialogUtilsNew1.showMsgOkDialog(activity,getString(R.string.invalid_invoice),getString(R.string.invoice_is_invalid), false)
                                        // iDiag?.getInfoDialog(getString(R.string.invalid_invoice),getString(R.string.invoice_is_invalid)){}
                                        iDiag?.alertBoxWithActionNew(
                                            getString(R.string.invalid_invoice),
                                            getString(R.string.invoice_is_invalid),
                                            R.drawable.ic_info_orange,
                                            getString(R.string.positive_button_ok),
                                            "", false, false,
                                            {
                                                reportsAdapter.notifyItemChanged(itemPosition)
                                            },
                                            {})
                                        //   }
                                    }

                                }


                            } catch (ex: Exception) {
                                ex.printStackTrace()
                                //launch(Dispatchers.Main) {
                                    iDiag?.hideProgress()
                                    //DialogUtilsNew1.showMsgOkDialog(activity,getString(R.string.error),"something Wrong", false)

                                iDiag?.alertBoxWithActionNew(
                                    getString(R.string.error),
                                    "something Wrong",
                                    R.drawable.ic_info_orange,
                                    getString(R.string.positive_button_ok),
                                    "",false,false,
                                    {
                                        reportsAdapter.notifyItemChanged(itemPosition)
                                    },
                                    {})

                              //  }
                            }
                        }
                    },{
                    reportsAdapter.notifyItemChanged(itemPosition)
                })
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

                    settlementViewModel?.getTempBatchFileData()?.observe(viewLifecycleOwner) { batchData ->
                        //onlyPreAuthCheck(batchData as MutableList<BatchTable>) // do later
                        if (batchData.isNotEmpty()  /*&& onlyPreAuthFlag == false*/) {
                            //iDiag?.getMsgDialog(
                            iDiag?.alertBoxWithActionNew(
                                getString(R.string.confirmation),
                                getString(R.string.want_print_detail),
                                R.drawable.ic_print_customer_copy,
                                getString(R.string.yes),
                                getString(R.string.no),true,false,
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
                                                dataList.addAll(batchData as MutableList<TempBatchFileDataTable>)
                                                // kushal enble later
                                                PrintVectorUtil(activity).printDetailReportupdate(
                                                    dataList,
                                                    activity
                                                ) { detailPrintStatus ->
                                                    iDiag?.hideProgress()

                                                }
                                                lifecycleScope.launch(Dispatchers.Main){
                                                    reportsAdapter.notifyItemChanged(itemPosition)
                                                }

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
                                                reportsAdapter.notifyItemChanged(itemPosition)
                                            }
                                        }

                                    }

                                },
                                {
                                    //handle cancel here
                                    reportsAdapter.notifyItemChanged(itemPosition)
                                })
                        } else {
                            lifecycleScope.launch(Dispatchers.Main) {
                                //iDiag?.alertBoxWithAction(
                                iDiag?.alertBoxWithActionNew(
                                    getString(R.string.empty_batch),
                                    getString(R.string.detail_report_not_found),
                                    R.drawable.ic_info_orange,
                                    getString(R.string.positive_button_ok),
                                    "",false,false,
                                    {
                                        reportsAdapter.notifyItemChanged(itemPosition)
                                    },
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

                        settlementViewModel?.getTempBatchFileData()?.observe(viewLifecycleOwner) { batList ->
                            //onlyPreAuthCheck(batList as MutableList<BatchTable>) // do later
                            if (batList.isNotEmpty() /*&& onlyPreAuthFlag == false*/) {
                                iDiag?.alertBoxWithActionNew(
                                    getString(R.string.confirmation),
                                    "Do you want to print summary Report",
                                    R.drawable.ic_print_customer_copy,
                                    "Yes",
                                    "No",true,false,
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
                                                dataList.addAll(batList as MutableList<TempBatchFileDataTable>)
                                                try {

                                                    PrintVectorUtil(activity).printSettlementReportupdate(
                                                        activity,
                                                        dataList,
                                                        false
                                                    ) {
                                                    }

                                                    lifecycleScope.launch(Dispatchers.Main){
                                                        reportsAdapter.notifyItemChanged(itemPosition)
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
                                                    /*iDiag?.getInfoDialog(
                                                        "Error",
                                                        " Summery is not available."
                                                    ) {}*/

                                                    iDiag?.alertBoxWithActionNew(
                                                        "Error",
                                                        "Summery is not available.",
                                                        R.drawable.ic_info_orange,
                                                        getString(R.string.positive_button_ok),
                                                        "",false,false,
                                                        {
                                                            reportsAdapter.notifyItemChanged(itemPosition)
                                                        },
                                                        {})
                                                }
                                            }

                                        }
                                    },
                                    {
                                        //Cancel handle here
                                        reportsAdapter.notifyItemChanged(itemPosition)
                                    })
                            } else {
                                GlobalScope.launch(Dispatchers.Main) {
                                    iDiag?.alertBoxWithActionNew(
                                        getString(R.string.empty_batch),
                                        getString(R.string.summary_report_not_available),
                                        R.drawable.ic_info_orange,
                                        getString(R.string.positive_button_ok),
                                        "",false,false,
                                        {
                                            reportsAdapter.notifyItemChanged(itemPosition)
                                        },
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

                if (batList != null && batList.size > 0) {
                    iDiag?.alertBoxWithActionNew(
                        getString(R.string.confirmation),
                        getString(R.string.last_summary_confirmation),
                        R.drawable.ic_print_customer_copy,
                        "Yes",
                        "No",true,false,
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
                                        PrintVectorUtil(activity).printSettlementReportupdate(activity, batList as MutableList<TempBatchFileDataTable>, false,true) {

                                        }
                                    } catch (ex: java.lang.Exception) {
                                        ex.message ?: getString(R.string.error_in_printing)
                                    } finally {
                                        launch(Dispatchers.Main) {
                                            iDiag?.hideProgress()
                                            //   iDiag?.showToast(msg)
                                        }
                                    }
                                    lifecycleScope.launch(Dispatchers.Main){
                                        reportsAdapter.notifyItemChanged(itemPosition)
                                    }
                                } else {
                                    launch(Dispatchers.Main) {
                                        iDiag?.hideProgress()

                                        iDiag?.alertBoxWithActionNew(
                                            "Error",
                                            "Last summary is not available.",
                                            R.drawable.ic_info_orange,
                                            getString(R.string.positive_button_ok),
                                            "",false,false,
                                            {
                                                reportsAdapter.notifyItemChanged(itemPosition)
                                            },
                                            {})
                                    }
                                }

                            }
                        },
                        {
                            //Cancel Handling
                            reportsAdapter.notifyItemChanged(itemPosition)
                        })
                } else {
                    lifecycleScope.launch(Dispatchers.Main) {
                        iDiag?.alertBoxWithActionNew(
                            getString(R.string.no_receipt),
                            getString(R.string.last_summary_not_available),
                            R.drawable.ic_info_orange,
                            getString(R.string.positive_button_ok),"",false,false,
                            {
                                reportsAdapter.notifyItemChanged(itemPosition)
                            },
                            {})
                    }
                }

            }
// End of last Summery Report receipt

            /*ReportsItem.PRINT_REVERSAL_REPORT -> {
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
            }*/
        }

    }
    /*private fun onlyPreAuthCheck(dataList: MutableList<BatchTable>) {
        for (i in 0 until dataList.size) {
            if(dataList[i].transactionType != BhTransactionType.PRE_AUTH.type){
                onlyPreAuthFlag=false
                break
            }

        }
    }*/
}

interface IReportsFragmentItemClick {

    fun ReportsOptionItemClick(reportsItem: ReportsItem, itemPosition:Int)
}