package com.bonushub.crdb.view.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentSettlementBinding
import com.bonushub.crdb.databinding.ItemSettlementBinding
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.disputetransaction.CreateSettlementPacket
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.model.local.BatchTable
import com.bonushub.crdb.model.local.BatchTableReversal
import com.bonushub.crdb.utils.*
import com.bonushub.crdb.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.utils.printerUtils.PrintUtil
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.base.IDialog
import com.bonushub.crdb.viewmodel.BatchReversalViewModel
import com.bonushub.crdb.viewmodel.SettlementViewModel
import com.mindorks.example.coroutines.utils.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class SettlementFragment : Fragment() {

    private val fragmensettlementBinding : FragmentSettlementBinding by lazy {
        FragmentSettlementBinding.inflate(layoutInflater)
    }
    @Inject
    lateinit var appDao: AppDao
    private val settlementViewModel : SettlementViewModel by viewModels()
    private val dataList: MutableList<BatchTable> by lazy { mutableListOf<BatchTable>() }
    private val dataListReversal: MutableList<BatchTableReversal> by lazy { mutableListOf<BatchTableReversal>() }
    private val settlementAdapter by lazy { SettlementAdapter(dataList) }
    private var settlementByteArray: ByteArray? = null
    private var navController: NavController? = null
    private var iDialog: IDialog? = null

    private val batchReversalViewModel : BatchReversalViewModel by viewModels()

    private var ioSope = CoroutineScope(Dispatchers.IO)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IDialog) iDialog = context
    }

    override fun onDetach() {
        super.onDetach()
        iDialog = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return fragmensettlementBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
       // (activity as NavigationActivity).showBottomNavigationBar(isShow = false)
        fragmensettlementBinding.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_settlement)
        fragmensettlementBinding.subHeaderView?.subHeaderText?.text = getString(R.string.settlement)
        fragmensettlementBinding.subHeaderView?.backImageButton?.setOnClickListener { /*parentFragmentManager?.popBackStack()*/
            try {
                (activity as NavigationActivity).decideDashBoardOnBackPress()
            }catch (ex:Exception){
                ex.printStackTrace()

            }
        }


        //region===============Get Batch Data from HDFCViewModal:-
        settlementViewModel.getBatchData()?.observe(requireActivity()) { batchData ->
                Log.d("TPT Data:- ", batchData.toString())
                dataList.clear()
                dataList.addAll(batchData as MutableList<BatchTable>)
                setUpRecyclerView()

            }

        lifecycleScope.launch {
            batchReversalViewModel?.getBatchTableReversalData()?.observe(viewLifecycleOwner) { batchReversalList ->
              dataListReversal.clear()
              dataListReversal.addAll(batchReversalList as MutableList<BatchTableReversal>)
            }
        }



        //endregion
        //region================================RecyclerView On Scroll Extended Floating Button Hide/Show:-
        fragmensettlementBinding.nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->

            //Scroll Down Condition:-
            if (scrollY > oldScrollY + 20 && fragmensettlementBinding.settlementFloatingButton?.isExtended == true)
                fragmensettlementBinding?.settlementFloatingButton?.shrink()

            //Scroll Up Condition:-
            if (scrollY < oldScrollY - 20 && fragmensettlementBinding?.settlementFloatingButton?.isExtended != true)
                fragmensettlementBinding?.settlementFloatingButton?.extend()

            //At the Top Condition:-
            if (scrollY == 0)
                fragmensettlementBinding?.settlementFloatingButton?.extend()
        })
        //endregion

        //region========================OnClick Event of SettleBatch Button:-
        fragmensettlementBinding?.settlementFloatingButton?.setOnClickListener {

            DialogUtilsNew1.alertBoxWithAction(requireContext(), getString(R.string.do_you_want_to_settle_batch),"",getString(R.string.confirm),"Cancel",R.drawable.ic_info,
                {


                    // **** for zero settlement *****
                    if (dataList.size == 0) {

                        /*settlementViewModel.settlementResponse()
                        settlementViewModel.ingenciosettlement.observe(requireActivity()) { result ->

                            logger("result in Zero ",result.toString())

                            when (result.status) {
                                Status.SUCCESS -> {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        //  AppPreference.saveBatchInPreference(dataList as MutableList<BatchTable>)
                                        val data =
                                            CreateSettlementPacket(appDao).createSettlementISOPacket()
                                        settlementByteArray = data.generateIsoByteRequest()
                                        try {
                                            (activity as NavigationActivity).settleBatch1(
                                                settlementByteArray
                                            ) {
                                                logger("zero settlement",it.toString(),"e")
                                            }
                                        } catch (ex: Exception) {
                                            (activity as NavigationActivity).hideProgress()
                                            ex.printStackTrace()
                                        }
                                    }
                                    //  Toast.makeText(activity,"Sucess called  ${result.message}", Toast.LENGTH_LONG).show()
                                }
                                Status.ERROR -> {

                                    // Toast.makeText(activity,"Error called  ${result.error}", Toast.LENGTH_LONG).show()
                                }
                                Status.LOADING -> {
                                    // Toast.makeText(activity,"Loading called  ${result.message}", Toast.LENGTH_LONG).show()


                                }
                            }

                        }*/

                        //----------------------
                        lifecycleScope.launch(
                            Dispatchers.IO
                        ) {
                            val data =
                                CreateSettlementPacket(appDao).createSettlementISOPacket()
                            settlementByteArray =
                                data.generateIsoByteRequest()
                            try {
                                (activity as NavigationActivity).settleBatch1(
                                    settlementByteArray
                                ) {
                                    logger("zero settlement",it.toString(),"e")
                                }
                            } catch (ex: Exception) {
                                (activity as NavigationActivity).hideProgress()
                                ex.printStackTrace()
                            }
                        }

                    }else{

                    PrintUtil(activity).printDetailReportupdate(
                        dataList,
                        activity
                    ) { detailPrintStatus ->
                    }


                        ioSope.launch {
                           var reversalTid  = checkReversal(dataListReversal)
                           var listofTxnTid =  checkSettlementTid(dataList)

                            val result: ArrayList<String> = ArrayList()
                            result.addAll(listofTxnTid)

                            for (e in reversalTid) {
                                if (!result.contains(e)) result.add(e)
                            }
                           System.out.println("Total transaction tid is"+result.forEach {
                               println("Tid are "+it)
                           })
                           settlementViewModel.settlementResponse(result)
                        }

                        settlementViewModel.ingenciosettlement.observe(requireActivity()) { result ->

                        when (result.status) {
                            Status.SUCCESS -> {
                                CoroutineScope(Dispatchers.IO).launch {
                                    //  AppPreference.saveBatchInPreference(dataList as MutableList<BatchTable>)
                                    val data = CreateSettlementPacket(appDao).createSettlementISOPacket()
                                    settlementByteArray = data.generateIsoByteRequest()
                                    try {
                                        (activity as NavigationActivity).settleBatch1(
                                            settlementByteArray
                                        ) {
                                        }
                                    } catch (ex: Exception) {
                                        (activity as NavigationActivity).hideProgress()
                                        ex.printStackTrace()
                                    }
                                }
                                //  Toast.makeText(activity,"Sucess called  ${result.message}", Toast.LENGTH_LONG).show()
                            }
                            Status.ERROR -> {

                                // Toast.makeText(activity,"Error called  ${result.error}", Toast.LENGTH_LONG).show()
                            }
                            Status.LOADING -> {
                                // Toast.makeText(activity,"Loading called  ${result.message}", Toast.LENGTH_LONG).show()


                            }
                        }

                    }
                }


            },{})
        }
        //endregion

    }
    //region====================================SetUp RecyclerView:-
    private fun setUpRecyclerView() {
        if (dataList.size > 0) {
             fragmensettlementBinding?.settlementFloatingButton?.visibility = View.VISIBLE  // visible for zero settlement
            fragmensettlementBinding?.settlementRv?.visibility = View.VISIBLE
            fragmensettlementBinding?.lvHeadingView?.visibility = View.VISIBLE

            fragmensettlementBinding?.settlementRv?.apply {
                layoutManager = LinearLayoutManager(context)
                itemAnimator = DefaultItemAnimator()
                adapter = settlementAdapter
            }
        } else {
             fragmensettlementBinding?.settlementFloatingButton?.visibility = View.GONE  // visible for zero settlement
            fragmensettlementBinding?.settlementRv?.visibility = View.GONE
            fragmensettlementBinding?.lvHeadingView?.visibility = View.GONE
            fragmensettlementBinding?.emptyViewPlaceholder?.visibility = View.VISIBLE
        }


    }
    //endregion
}

internal class SettlementAdapter(private val list: List<BatchTable>) :
    RecyclerView.Adapter<SettlementAdapter.SettlementHolder>() {

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): SettlementHolder {
        val binding : ItemSettlementBinding by lazy {
            ItemSettlementBinding.inflate(LayoutInflater.from(p0.context),p0,false)
        }

        return SettlementHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: SettlementHolder, p1: Int) {

        holder.binding.tvInvoiceNumber.text = invoiceWithPadding(list[p1].receiptData?.invoice ?: "")
        val amount = "%.2f".format(list[p1].receiptData?.txnAmount?.toDouble()?.div(100))
        holder.binding.tvBaseAmount.text = amount
        holder.binding.tvTransactionType.text = getTransactionTypeName(list[p1].transactionType)
        holder.binding.tvTransactionDate.text = list[p1].receiptData?.dateTime




    }

    inner class SettlementHolder(val binding: ItemSettlementBinding) :
        RecyclerView.ViewHolder(binding.root)
}