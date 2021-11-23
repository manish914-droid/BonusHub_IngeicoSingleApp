package com.bonushub.crdb.view.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.HDFCApplication.Companion.appContext
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentReportsBinding
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.model.local.BrandEMIDataTable
import com.bonushub.crdb.utils.ToastUtils
import com.bonushub.crdb.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.utils.logger
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.adapter.ReportsAdapter
import com.bonushub.crdb.view.base.IDialog
import com.bonushub.pax.utils.EPrintCopyType
import com.bonushub.pax.utils.ReportsItem
import com.bonushub.pax.utils.TransactionType
import com.bonushub.pax.utils.VxEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class ReportsFragment : Fragment(), IReportsFragmentItemClick {

    private val reportsItemList: MutableList<ReportsItem> by lazy { mutableListOf<ReportsItem>() }
    private var iReportsFragmentItemClick:IReportsFragmentItemClick? = null

    var binding:FragmentReportsBinding? = null

    private var iDiag: IDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentReportsBinding.inflate(inflater,container,false)
        return binding?.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

     //   iDiag?.onEvents(VxEvent.ChangeTitle(option.name))

        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.reports_header)
        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_reports)


        iReportsFragmentItemClick = this

        reportsItemList.clear()
        reportsItemList.addAll(ReportsItem.values())
        setupRecyclerview()


        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }

    }

    private fun setupRecyclerview(){
        lifecycleScope.launch(Dispatchers.Main) {
            binding?.let {
                it.recyclerView.layoutManager = GridLayoutManager(activity, 1)
                it.recyclerView.adapter = ReportsAdapter( reportsItemList, iReportsFragmentItemClick)
            }

        }
    }

    override fun ReportsOptionItemClick(reportsItem: ReportsItem) {

        when(reportsItem){

            ReportsItem.LAST_RECEIPT ->{
                logger("repost",ReportsItem.LAST_RECEIPT._name)
                DialogUtilsNew1.showMsgOkDialog(activity,getString(R.string.empty_batch),getString(R.string.last_receipt_not_available), false)

                /*val lastReceiptData = AppPreference.getLastSuccessReceipt()
                if (lastReceiptData != null) {
                    GlobalScope.launch(Dispatchers.Main) {
                        iDiag?.showProgress(getString(R.string.printing_last_receipt))
                    }
                    when (lastReceiptData.transactionType) {
                        TransactionType.SALE.type, TransactionType.TIP_SALE.type, TransactionType.REFUND.type, TransactionType.VOID.type, TransactionType.SALE_WITH_CASH.type, TransactionType.CASH_AT_POS.type ,TransactionType.VOID_EMI.type-> {
                            *//*PrintUtil(activity).startPrinting(
                                lastReceiptData,
                                EPrintCopyType.DUPLICATE,
                                activity
                            ) { printCB, printingFail ->
                                if (printCB) {
                                    iDiag?.hideProgress()
                                    logger("PRINTING", "LAST_RECEIPT","e")
                                } else {
                                    iDiag?.hideProgress()
                                }
                            }*//*
                        }
                        TransactionType.EMI_SALE.type ,TransactionType.TEST_EMI.type -> {
                            *//*PrintUtil(activity).printEMISale(
                                lastReceiptData,
                                EPrintCopyType.DUPLICATE,
                                activity
                            ) { printCB, printingFail ->
                                if (printCB) {
                                    iDiag?.hideProgress()
                                    logger("PRINTING", "LAST_RECEIPT","e")
                                } else {
                                    iDiag?.hideProgress()
                                }
                            }*//*
                        }
                        TransactionType.BRAND_EMI.type , TransactionType.BRAND_EMI_BY_ACCESS_CODE.type  -> {
                            *//*val brandEmiData = runBlocking(Dispatchers.IO) {
                                BrandEMIDataTable.getBrandEMIDataByInvoiceAndTid(
                                    lastReceiptData.hostInvoice,lastReceiptData.hostTID
                                )
                            }
                            PrintUtil(activity).printEMISale(
                                lastReceiptData,
                                EPrintCopyType.DUPLICATE,
                                activity,
                                brandEmiData
                            ) { printCB, printingFail ->
                                if (printCB) {
                                    iDiag?.hideProgress()
                                    logger("PRINTING", "LAST_RECEIPT","e")
                                } else {
                                    iDiag?.hideProgress()
                                }
                            }*//*
                        }
                        TransactionType.PRE_AUTH_COMPLETE.type -> {
                            *//*PrintUtil(activity).printAuthCompleteChargeSlip(
                                lastReceiptData,
                                EPrintCopyType.DUPLICATE,
                                activity
                            ) {
                                if (it) {
                                    iDiag?.hideProgress()
                                    logger("PRINTING", "LAST_RECEIPT","e")
                                } else {
                                    iDiag?.hideProgress()
                                }
                            }*//*
                        }
                        TransactionType.VOID_PREAUTH.type -> {
                           *//* PrintUtil(activity).printAuthCompleteChargeSlip(
                                lastReceiptData,
                                EPrintCopyType.DUPLICATE,
                                activity
                            ) {
                                if (it) {
                                    iDiag?.hideProgress()
                                    logger("PRINTING", "LAST_RECEIPT","e")
                                } else {
                                    iDiag?.hideProgress()
                                }
                            }*//*
                        }
                        TransactionType.OFFLINE_SALE.type -> {
                            *//*activity?.let {
                                OfflineSalePrintReceipt().offlineSalePrint(
                                    lastReceiptData, EPrintCopyType.DUPLICATE,
                                    it
                                ) { printCB, printingFail ->
                                    if (printCB) {
                                        iDiag?.hideProgress()
                                        logger("PRINTING", "LAST_RECEIPT","e")
                                    } else {
                                        iDiag?.hideProgress()
                                    }
                                }
                            }*//*
                        }
                        TransactionType.VOID_REFUND.type -> {
                           *//* VoidRefundSalePrintReceipt().startPrintingVoidRefund(
                                lastReceiptData,
                                TransactionType.VOID_REFUND.type,
                                EPrintCopyType.DUPLICATE,
                                activity
                            ) { _, _ ->
                                iDiag?.hideProgress()
                            }*//*
                        }
                        else -> {
                            GlobalScope.launch(Dispatchers.Main) {
                                iDiag?.hideProgress()
                                ToastUtils.showToast(requireContext(),"Report not found")
                            }
                        }
                    }
                } else {
                    GlobalScope.launch(Dispatchers.Main) {
                        //    iDiag?.hideProgress() //
                        //    iDiag?.showToast(getString(R.string.empty_batch)) //
                        //-
                        GlobalScope.launch(Dispatchers.Main) {
                            iDiag?.alertBoxWithAction("",
                                "",
                                false,
                                appContext.getString(R.string.positive_button_ok),
                                {},
                                {})
                        }
                    }
                }*/
            }
// End of Last Receipt case

            ReportsItem.LAST_CANCEL_RECEIPT ->{
                logger("repost",ReportsItem.LAST_CANCEL_RECEIPT._name)

            }
// End of last cancel receipt

            ReportsItem.ANY_RECEIPT ->{
                logger("repost",ReportsItem.ANY_RECEIPT._name)

            }
// End of Any Receipt case

            ReportsItem.DETAIL_REPORT ->{
                logger("repost",ReportsItem.DETAIL_REPORT._name)

            }
// End of Detail report

            ReportsItem.SUMMERY_REPORT ->{
                logger("repost",ReportsItem.SUMMERY_REPORT._name)
                (activity as NavigationActivity).transactFragment(ReportSummaryFragment(),true)

            }
// End of Summery Report

            ReportsItem.LAST_SUMMERY_REPORT ->{
                logger("repost",ReportsItem.LAST_SUMMERY_REPORT._name)

            }
// End of last Summery Report receipt

        }

    }
}

interface IReportsFragmentItemClick{

    fun ReportsOptionItemClick(reportsItem: ReportsItem)
}