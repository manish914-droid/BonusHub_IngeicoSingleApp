package com.bonushub.crdb.india.view.fragments

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
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentSettlementBinding
import com.bonushub.crdb.india.databinding.ItemSettlementBinding
import com.bonushub.crdb.india.db.AppDao
import com.bonushub.crdb.india.disputetransaction.CreateSettlementPacket
import com.bonushub.crdb.india.model.local.AppPreference
import com.bonushub.crdb.india.model.local.BatchTable
import com.bonushub.crdb.india.model.local.BatchTableReversal
import com.bonushub.crdb.india.model.local.TempBatchFileDataTable
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.india.utils.printerUtils.PrintUtil
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.base.IDialog
import com.bonushub.crdb.india.viewmodel.BatchReversalViewModel
import com.bonushub.crdb.india.viewmodel.SettlementViewModel
import com.bonushub.crdb.india.utils.PrefConstant
import com.bonushub.crdb.india.view.base.BaseActivityNew
import com.bonushub.crdb.india.viewmodel.TransactionViewModel
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
    //private val dataList: MutableList<BatchTable> by lazy { mutableListOf<BatchTable>() }/ old
    private val tempDataList: MutableList<TempBatchFileDataTable> by lazy { mutableListOf<TempBatchFileDataTable>() }
    private val dataListReversal: MutableList<BatchTableReversal> by lazy { mutableListOf<BatchTableReversal>() }
   // private val settlementAdapter by lazy { SettlementAdapter(dataList) } // old
    private val settlementAdapter by lazy { SettlementAdapter(tempDataList) }
    private var settlementByteArray: ByteArray? = null
    private var navController: NavController? = null
    private var iDialog: IDialog? = null
    private var onlyPreAuthFlag: Boolean? = true
    private val batchReversalViewModel : BatchReversalViewModel by viewModels()

    private var ioSope = CoroutineScope(Dispatchers.IO)

    private val transactionViewModel: TransactionViewModel by viewModels()

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


        //region===============Get Batch Data from HDFCViewModal:- kushal
       /* settlementViewModel.getBatchData()?.observe(requireActivity()) { batchData ->
            Log.d("TPT Data:- ", batchData.toString())
            dataList.clear()
            dataList.addAll(batchData as MutableList<BatchTable>)
            onlyPreAuthCheck(dataList)
            setUpRecyclerView()

        }*/

        //region===============Get Batch Data from HDFCViewModal:-
        settlementViewModel.getTempBatchFileData()?.observe(requireActivity()) { tempBatchData ->
            Log.d("TPT Data:- ", tempBatchData.toString())
            tempDataList.clear()
            tempDataList.addAll(tempBatchData as MutableList<TempBatchFileDataTable>)
           // onlyPreAuthCheck(tempDataList) // do later
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

            DialogUtilsNew1.alertBoxWithAction(requireContext(), getString(R.string.do_you_want_to_settle_batch),"",getString(R.string.confirm),"Cancel",R.drawable.ic_info, {
                    // **** for zero settlement *****
                    if (tempDataList.size == 0) {

                        //----------------------
                        lifecycleScope.launch(Dispatchers.IO) {
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

                        lifecycleScope.launch(Dispatchers.IO){
                            withContext(Dispatchers.Main){

                                (activity as BaseActivityNew).showProgress("Transaction syncing...")
                            }
                            Utility().syncPendingTransaction(transactionViewModel){
                                if(it){
                                       // withContext(Dispatchers.Main){
                                            (activity as BaseActivityNew).hideProgress()
                                       // }

                                    // kushal
                                    /*PrintUtil(activity).printDetailReportupdate(tempDataList, activity) {
                                            detailPrintStatus ->

                                    }*/


                                    lifecycleScope.launch(Dispatchers.Main) {
                                        val reversalTid  = checkReversal(dataListReversal)
                                        val listofTxnTid =  checkSettlementTid(tempDataList)

                                        val result: ArrayList<String> = ArrayList()
                                        result.addAll(listofTxnTid)

                                        for (e in reversalTid) {
                                            if (!result.contains(e)) result.add(e)
                                        }
                                        System.out.println("Total transaction tid is"+result.forEach {
                                            println("Tid are "+it)
                                        })

                                        if(AppPreference.getBoolean(PrefConstant.BLOCK_MENU_OPTIONS.keyName.toString())) {
                                            CoroutineScope(Dispatchers.IO).launch{
                                                try {
                                                    val processingCode: String = ProcessingCode.FORCE_SETTLEMENT.code
                                                    val data = CreateSettlementPacket(appDao).createSettlementISOPacket()
                                                    val   settlementByteArray = data.generateIsoByteRequest()
                                                    (activity as NavigationActivity).settleBatch1(settlementByteArray) {}
                                                } catch (ex: Exception) {
                                                    println("Exception is "+ex.printStackTrace())
                                                    (activity as NavigationActivity).hideProgress()
                                                    ex.printStackTrace()
                                                }
                                            }
                                        }
                                        else {
                                            settlementViewModel.settlementResponse(result)
                                            settlementViewModel.ingenciosettlement.observe(requireActivity()) { result ->

                                                when (result.status) {
                                                    Status.SUCCESS -> {
                                                        CoroutineScope(Dispatchers.IO).launch {
                                                            AppPreference.saveBoolean(PrefConstant.BLOCK_MENU_OPTIONS.keyName.toString(), false)
                                                            AppPreference.saveBoolean(PrefConstant.BLOCK_MENU_OPTIONS_INGENICO.keyName.toString(), false)

                                                            // region upload digipos pending txn
                                                            logger("UPLOAD DIGI"," ----------------------->  START","e")
                                                            uploadPendingDigiPosTxn(requireActivity()){
                                                                logger("UPLOAD DIGI"," ----------------------->   BEFOR PRINT","e")
                                                                CoroutineScope(Dispatchers.IO).launch{
                                                                    val processingCode: String = ProcessingCode.SETTLEMENT.code
                                                                    val data = CreateSettlementPacket(appDao).createSettlementISOPacket()
                                                                    settlementByteArray = data.generateIsoByteRequest()
                                                                    try {
                                                                        (activity as NavigationActivity).settleBatch1(settlementByteArray) {}
                                                                    } catch (ex: Exception) {
                                                                        (activity as NavigationActivity).hideProgress()
                                                                        ex.printStackTrace()
                                                                    }
                                                                }

                                                            }
                                                            // end region

                                                        }
                                                        //  Toast.makeText(activity,"Sucess called  ${result.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                    Status.ERROR -> {
                                                        println("Error in ingenico settlement")
                                                        AppPreference.saveBoolean(PrefConstant.BLOCK_MENU_OPTIONS.keyName.toString(), false)
                                                        AppPreference.saveBoolean(PrefConstant.BLOCK_MENU_OPTIONS_INGENICO.keyName.toString(), true)
                                                        // Toast.makeText(activity,"Error called  ${result.error}", Toast.LENGTH_LONG).show()
                                                    }
                                                    Status.LOADING -> {
                                                        // Toast.makeText(activity,"Loading called  ${result.message}", Toast.LENGTH_LONG).show()


                                                    }
                                                }
                                            }

                                        }
                                    }
                                }else{
                                    logger("sync","failed terminate settlement")
                                    (activity as? NavigationActivity)?.hideProgress()
                                    lifecycleScope.launch(Dispatchers.Main){
                                        (activity as? BaseActivityNew)?.getInfoDialog("","Syncing failed settlement not allow.",R.drawable.ic_info){
                                            try {
                                                (activity as? NavigationActivity)?.decideDashBoardOnBackPress()
                                            }catch (ex:Exception){
                                                ex.printStackTrace()
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    }


                },{})
        }
        //endregion


    }
    private fun onlyPreAuthCheck(dataList: MutableList<BatchTable>) {
        for (i in 0 until dataList.size) {
            if(dataList[i].transactionType != BhTransactionType.PRE_AUTH.type){
                onlyPreAuthFlag=false
                break
            }

        }
    }
    //region====================================SetUp RecyclerView:-
    private fun setUpRecyclerView() {

        //if (dataList.size > 0  ) { // kushal
        if (tempDataList.size > 0  ) {
            fragmensettlementBinding?.settlementFloatingButton?.visibility = View.VISIBLE  // visible for zero settlement
            fragmensettlementBinding?.settlementRv?.visibility = View.VISIBLE
            fragmensettlementBinding?.lvHeadingView?.visibility = View.VISIBLE
           // kushal check later
            /*if(onlyPreAuthFlag==true){
                fragmensettlementBinding?.settlementFloatingButton?.visibility = View.GONE
            }*/
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

internal class SettlementAdapter(private val list: List<TempBatchFileDataTable>) :
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

        holder.binding.tvInvoiceNumber.text = invoiceWithPadding(list[p1].invoiceNumber ?: "")
        val amount = "%.2f".format(list[p1]?.transactionalAmmount?.toDouble()?.div(100))
        holder.binding.tvBaseAmount.text = amount
        holder.binding.tvTransactionType.text = getTransactionTypeName(list[p1].transactionType)
        if(getTransactionTypeName(list[p1].transactionType) == "TEST EMI TXN"){
            holder.binding.tvTransactionType.text="SALE "
        }
        holder.binding.tvTransactionDate.text = list[p1].date
    }

    inner class SettlementHolder(val binding: ItemSettlementBinding) :
        RecyclerView.ViewHolder(binding.root)
}