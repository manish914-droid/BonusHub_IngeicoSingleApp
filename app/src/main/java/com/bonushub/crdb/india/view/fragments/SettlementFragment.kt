package com.bonushub.crdb.india.view.fragments

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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
import com.bonushub.crdb.india.model.local.TempBatchFileDataTable
import com.bonushub.crdb.india.transactionprocess.SyncReversalToHost
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.printerUtils.PrintUtil
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.base.IDialog
import com.bonushub.crdb.india.viewmodel.SettlementViewModel
import com.bonushub.crdb.india.view.base.BaseActivityNew
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
    private val tempDataList: MutableList<TempBatchFileDataTable> by lazy { mutableListOf<TempBatchFileDataTable>() }
    private val settlementAdapter by lazy { SettlementAdapter(tempDataList) }
    private var settlementByteArray: ByteArray? = null
    private var navController: NavController? = null
    private var iDialog: IDialog? = null
    private var onlyPreAuthFlag: Boolean? = true

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
        (activity as NavigationActivity).manageTopToolBar(false)
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
        settlementViewModel.getTempBatchFileData()?.observe(requireActivity()) { tempBatchData ->
            Log.d("TPT Data:- ", tempBatchData.toString())
            tempDataList.clear()
            tempDataList.addAll(tempBatchData as MutableList<TempBatchFileDataTable>)
            setUpRecyclerView()

        }


        //region========================OnClick Event of SettleBatch Button:-
        fragmensettlementBinding?.settlementFloatingButton?.setOnClickListener {

            if(tempDataList.size > 0 ){

                //DialogUtilsNew1.alertBoxWithAction(requireContext(), getString(R.string.do_you_want_to_settle_batch),"",getString(R.string.confirm),"Cancel",R.drawable.ic_info, {
                iDialog?.alertBoxWithActionNew(getString(R.string.do_you_want_to_settle_batch),"",R.drawable.ic_info_orange,getString(R.string.confirm),"Cancel",true,false, {
                    // **** for zero settlement *****
                    if (tempDataList.size == 0) {

                        //----------------------
                        lifecycleScope.launch(Dispatchers.IO) {
                            val data =
                                CreateSettlementPacket(appDao).createSettlementISOPacket()
                            settlementByteArray =
                                data.generateIsoByteRequest()
                            try {
                                (activity as NavigationActivity).settleBatch(
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
//                            (activity as NavigationActivity).window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                            val reversalObj = AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY)
                            println(reversalObj)
                            if (TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
                                    // do txn
                                withContext(Dispatchers.Main){

                                    (activity as BaseActivityNew).showProgress("Transaction syncing...")
                                }

                                printAndDoSettlement()
                            }
                            else {
                                if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {

                                    withContext(Dispatchers.Main) {  (activity as BaseActivityNew).showProgress(getString(R.string.reversal_data_sync))}

                                    SyncReversalToHost(AppPreference.getReversalNew()) { isSyncToHost, transMsg ->
                                        Log.e("hideProgress","1")
                                        //  hideProgress()
                                        if (isSyncToHost) {
                                            AppPreference.clearReversal()

                                            lifecycleScope.launch(Dispatchers.IO) {
                                                printAndDoSettlement()
                                            }
                                            println("clearReversal -> check again")
                                        } else {
                                            lifecycleScope.launch(Dispatchers.Main) {
                                                //  VFService.showToast(transMsg)
                                                (activity as BaseActivityNew).alertBoxWithActionNew(
                                                    getString(R.string.reversal_upload_fail),
                                                    getString(R.string.transaction_delined_msg),
                                                    R.drawable.ic_txn_declined,
                                                    getString(R.string.positive_button_ok),"",
                                                    false,false,
                                                    {},
                                                    {})


                                            }
                                        }
                                    }
                                }else{
                                    println("442")
                                }
                            }





                            /*Utility().syncPendingTransaction(transactionViewModel){
                                if(it){
                                       // withContext(Dispatchers.Main){
                                            (activity as BaseActivityNew).hideProgress()
                                       // }

                                    // kushal
                                    *//*PrintUtil(activity).printDetailReportupdate(tempDataList, activity) {
                                            detailPrintStatus ->

                                    }*//*


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
                            }*/
                        }

                    }


                },{})

            }
        }
        //endregion


    }

    private suspend fun printAndDoSettlement() {

        PrintUtil(activity).printDetailReportupdate(tempDataList, activity){ detailPrintStatus ->

            if (detailPrintStatus) {
                lifecycleScope.launch(
                    Dispatchers.IO
                ) {
                    val data =
                        CreateSettlementPacket(appDao).createSettlementISOPacket()
                    settlementByteArray =
                        data.generateIsoByteRequest()
                    try {
                        (activity as NavigationActivity).settleBatch(
                            settlementByteArray
                        ) {
                            if (!it)
                                enableDisableSettlementButton(
                                    true
                                )
                        }
                    } catch (ex: Exception) {
                        (activity as NavigationActivity).hideProgress()
                        enableDisableSettlementButton(
                            true
                        )
                        ex.printStackTrace()
                    }
                }
            } else {
                (activity as NavigationActivity).hideProgress()
                lifecycleScope.launch(Dispatchers.Main) {
                    (activity as NavigationActivity).alertBoxWithAction(
                        getString(R.string.printer_error),
                        getString(R.string.please_check_printing_roll),
                        true,
                        getString(R.string.yes),
                        {
                            /*val data =
                                CreateSettlementPacket(
                                    ProcessingCode.SETTLEMENT.code,
                                    batchList
                                ).createSettlementISOPacket()
                            settlementByteArray =
                                data.generateIsoByteRequest()
                            try {
                                (activity as MainActivity).hideProgress()
                                GlobalScope.launch(
                                    Dispatchers.IO
                                ) {
                                    (activity as MainActivity).settleBatch(
                                        settlementByteArray
                                    ) {
                                        if (!it)
                                            enableDisableSettlementButton(
                                                true
                                            )
                                    }
                                }
                            } catch (ex: Exception) {
                                (activity as MainActivity).hideProgress()
                                enableDisableSettlementButton(
                                    true
                                )
                                ex.printStackTrace()
                            }*/
                        },
                        {})
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
    //region====================================SetUp RecyclerView:-
    private fun setUpRecyclerView() {

        //if (dataList.size > 0  ) { // kushal
        if (tempDataList.size > 0  ) {
            fragmensettlementBinding?.settlementFloatingButton?.visibility = View.VISIBLE  // visible for zero settlement
            fragmensettlementBinding?.settlementRv?.visibility = View.VISIBLE
           // fragmensettlementBinding?.lvHeadingView?.visibility = View.VISIBLE  // now show always
           // kushal check later
            /*if(onlyPreAuthFlag==true){
                fragmensettlementBinding?.settlementFloatingButton?.visibility = View.GONE
            }*/
            fragmensettlementBinding?.settlementRv?.apply {
                layoutManager = LinearLayoutManager(context)
                itemAnimator = DefaultItemAnimator()
                adapter = settlementAdapter
            }
            fragmensettlementBinding?.conLaySettleBatchBtn?.alpha = 1f
        } else {
            fragmensettlementBinding?.settlementFloatingButton?.visibility = View.VISIBLE  // visible for zero settlement
            fragmensettlementBinding?.settlementRv?.visibility = View.GONE
            //fragmensettlementBinding?.lvHeadingView?.visibility = View.GONE   // now show always
            fragmensettlementBinding?.emptyViewPlaceholder?.visibility = View.VISIBLE
            fragmensettlementBinding?.conLaySettleBatchBtn?.alpha = .5f
        }


    }
    //endregion

    //region==============================method to enable/disable settlement Floating button:-
    private  fun enableDisableSettlementButton(isEnable: Boolean) {

        fragmensettlementBinding.settlementFloatingButton.isEnabled = isEnable

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

        holder.binding.tvInvoiceNumber.text = invoiceWithPadding(list[p1].hostInvoice ?: "")
        try{
            val amount = "%.2f".format(list[p1]?.transactionalAmmount?.toDouble()?.div(100))
            holder.binding.tvBaseAmount.text = amount
        }catch (ex:Exception){

        }
        holder.binding.tvTransactionType.text = getTransactionTypeName(list[p1].transactionType)
        if(getTransactionTypeName(list[p1].transactionType) == "TEST EMI TXN"){
            holder.binding.tvTransactionType.text="SALE "
        }
        holder.binding.tvTransactionDate.text = list[p1].transactionDate
    }

    inner class SettlementHolder(val binding: ItemSettlementBinding) :
        RecyclerView.ViewHolder(binding.root)
}