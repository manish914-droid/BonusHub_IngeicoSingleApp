package com.bonushub.crdb.view.fragments


import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider

import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.R
import com.bonushub.crdb.appupdate.SendAppUpdateConfirmationPacket
import com.bonushub.crdb.appupdate.SyncAppUpdateConfirmation

import com.bonushub.crdb.databinding.FragmentDashboardBinding
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.disputetransaction.CreateSettlementPacket
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.model.local.BatchTable
import com.bonushub.crdb.utils.*
import com.bonushub.crdb.utils.Field48ResponseTimestamp.getTptData
import com.bonushub.crdb.utils.printerUtils.PrintUtil


import com.bonushub.crdb.view.activity.IFragmentRequest
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.adapter.DashBoardAdapter
import com.bonushub.crdb.view.base.BaseActivityNew
import com.bonushub.crdb.viewmodel.DashboardViewModel
import com.bonushub.crdb.viewmodel.SettlementViewModel
import com.ingenico.hdfcpayment.listener.OnOperationListener
import com.ingenico.hdfcpayment.model.ReceiptDetail
import com.ingenico.hdfcpayment.model.TransactionDetail
import com.ingenico.hdfcpayment.response.OperationResult
import com.ingenico.hdfcpayment.response.TransactionDataResponse
import com.ingenico.hdfcpayment.type.RequestStatus


import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.coroutines.*
import java.lang.Runnable
import javax.inject.Inject


@AndroidEntryPoint
class DashboardFragment : androidx.fragment.app.Fragment() {
    companion object {
        var toRefresh = true
        val TAG = DashboardFragment::class.java.simpleName
    }
    private var counter = 0
    @Inject
    lateinit var appDao: AppDao
    private val settlementViewModel : SettlementViewModel by viewModels()
    private var ioSope = CoroutineScope(Dispatchers.IO)
    private var defaultSope = CoroutineScope(Dispatchers.Default)
    lateinit var dashboardViewModel : DashboardViewModel
    private var iFragmentRequest: IFragmentRequest? = null
    private val itemList = mutableListOf<EDashboardItem>()
    private val list1 = arrayListOf<EDashboardItem>()
    private val list2 = arrayListOf<EDashboardItem>()
    private var battery: String? = null
    private var batteryINper: Int = 0
    private val dashBoardAdapter by lazy {
        DashBoardAdapter(iFragmentRequest, ::onItemLessMoreClick)
    }
    private  var data: MutableList<BannerConfigModal>?=null
    private var animShow: Animation? = null
    private var animHide: Animation? = null
    private var binding: FragmentDashboardBinding? = null

    private var runnable: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding?.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("Dashboard:- ", "onViewCreated")
        isDashboardOpen = true
        Utility().hideSoftKeyboard(requireActivity())
       // restartHandaling()
        dashboardViewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)
        observeDashboardViewModel()

        //region======================Change isAutoSettleDone Boolean Value to False if Date is greater then :- written by kushal
        //last saved Auto Settle Date:-
        if (!TextUtils.isEmpty(AppPreference.getString(PreferenceKeyConstant.LAST_SAVED_AUTO_SETTLE_DATE.keyName))) {
            if (AppPreference.getString(PreferenceKeyConstant.LAST_SAVED_AUTO_SETTLE_DATE.keyName).toInt() > 0) {
                if (getSystemTimeIn24Hour().terminalDate().toInt() >
                    AppPreference.getString(PreferenceKeyConstant.LAST_SAVED_AUTO_SETTLE_DATE.keyName).toInt()
                ) {
                    AppPreference.saveBoolean(PreferenceKeyConstant.IsAutoSettleDone.keyName, false)
                }
            }
        }
        //endregion

        logger("check",""+AppPreference.getBoolean(PreferenceKeyConstant.IsAutoSettleDone.keyName))
        //region=======================Check For AutoSettle at regular interval if App is on Dashboard:-
        if (isDashboardOpen && !AppPreference.getBoolean(PreferenceKeyConstant.IsAutoSettleDone.keyName))
            checkForAutoSettle()
        //endregion
    }

    override fun onPause() {
        super.onPause()
        //timer?.cancel()
        runnable?.let { handler.removeCallbacks(it) }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IFragmentRequest) {
            iFragmentRequest = context
        }
    }


    private fun observeDashboardViewModel(){
        lifecycleScope.launch(Dispatchers.Main) {
            dashboardViewModel.eDashboardItem().observe(viewLifecycleOwner,{
                ioSope.launch(Dispatchers.Main) {
                    val result = async {  setupRecyclerview(it) }.await()
                    val result1 = async {
                        var listofTids = checkBaseTid(appDao)
                        println("List of tids are"+listofTids)
                        delay(1000)
                       // sett()
                    doInitializtion(appDao,listofTids)
                    }.await()

                }
                sendConfirmationToHost()
            })

        }

    }

    /*Below method only executed when the app is updated to newer version and
 previous store version in file < new app updated version:- */
    private fun sendConfirmationToHost() {
        try {
            context?.let {
                getRevisionIDFromFile(it) { isRevisionIDSame ->
                    if (isRevisionIDSame) {
                        sendConfirmation()
                    }

                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            CoroutineScope(Dispatchers.Main).launch {
               // VFService.showToast(getString(R.string.confirmation_app_update_failed))
            }

        }
    }

    //Sync App Confirmation to Host:-
    private fun sendConfirmation() {
        val appUpdateConfirmationISOData = SendAppUpdateConfirmationPacket(appDao).createAppUpdateConfirmationPacket()
        val isoByteArray = appUpdateConfirmationISOData.generateIsoByteRequest()

        GlobalScope.launch(Dispatchers.Main) {
            activity?.let {
                (it as? NavigationActivity)?.showProgress(getString(R.string.please_wait))
            }
        }
        SyncAppUpdateConfirmation(isoByteArray) { syncStatus ->
            GlobalScope.launch(Dispatchers.Main) {
                activity?.let {
                    (it as? NavigationActivity)?.hideProgress()
                }
                if (syncStatus) {
                    AppPreference.saveBoolean("isUpdate", true)
                    context?.let { it1 ->
                        writeAppRevisionIDInFile(it1)
                    }
                } else {
                    counter += 1
                    if (counter < 2) {
                        sendConfirmation()
                    }
                }
            }
        }


    }

    // Setting up recyclerview of DashBoard Items
    private fun setupRecyclerview(list:ArrayList<EDashboardItem>){
        lifecycleScope.launch(Dispatchers.Main) {
            dashboard_RV.layoutManager = GridLayoutManager(activity, 3)
            dashboard_RV.itemAnimator = DefaultItemAnimator()
            dashboard_RV.adapter = dashBoardAdapter
            if (isExpanded) dashBoardAdapter.onUpdatedItem(list) else dashBoardAdapter.onUpdatedItem(
                list
            )
            dashboard_RV.scheduleLayoutAnimation()
        }
    }


    private fun onItemLessMoreClick(item: EDashboardItem) {
        when (item) {
            EDashboardItem.MORE -> {
                isExpanded = true
                binding?.pagerViewLL?.startAnimation(animHide)
                binding?.pagerViewLL?.visibility = View.GONE
                dashBoardAdapter.onUpdatedItem(list2)
                dashBoardAdapter.notifyDataSetChanged()
                binding?.dashboardRV?.scheduleLayoutAnimation()
            }
            EDashboardItem.LESS -> {
                isExpanded = false
                binding?.pagerViewLL?.visibility = View.VISIBLE
                binding?.pagerViewLL?.startAnimation(animShow)
                dashBoardAdapter.onUpdatedItem(list1)
                dashBoardAdapter.notifyDataSetChanged()
                binding?.dashboardRV?.scheduleLayoutAnimation()
            }
            else -> {
            }
        }
    }


    //region============================Auto Settle Check on Dashboard at Regular Intervals:-
    private fun checkForAutoSettle() {
        runnable = object : Runnable {
            override fun run() {
                try {
                    logger("AutoSettle:- ", "Checking....")
                    autoSettleBatch()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    //also call the same runnable to call it at regular interval
                    handler.postDelayed(this, 20000)
                }
            }
        }
        handler.post(runnable as Runnable)
    }
    //endregion

    //region=======================Check for User IDLE on Dashboard and do auto settle if conditions match:-
    private fun autoSettleBatch() {
        //val tptData = runBlocking(Dispatchers.IO) { TerminalParameterTable.selectFromSchemeTable() }
        val tptData = runBlocking(Dispatchers.IO) { getTptData() }
        //val batchData = runBlocking(Dispatchers.IO) { BatchFileDataTable.selectBatchData() }
        val batchData = runBlocking(Dispatchers.IO) { appDao.getAllBatchData() }
        Log.d("HostForceSettle:- ", tptData?.forceSettle ?: "")
        Log.d("HostForceSettleTime:- ", tptData?.forceSettleTime ?: "")
        Log.d(
            "System Date Time:- ",
            "${getSystemTimeIn24Hour().terminalDate()} ${getSystemTimeIn24Hour().terminalTime()}"
        )

        // region temp for testing
//        tptData?.forceSettleTime = "183030"
//        tptData?.forceSettle = "1"
        // end region

        if (isDashboardOpen && !AppPreference.getBoolean(PreferenceKeyConstant.IsAutoSettleDone.keyName)) {

            Log.d("Dashboard Open:- ", "Yes")
            if (!TextUtils.isEmpty(tptData?.forceSettle)
                && !TextUtils.isEmpty(tptData?.forceSettleTime)
                && tptData?.forceSettle == "1"
            ) {
                if ((tptData.forceSettleTime.toLong() == getSystemTimeIn24Hour().terminalTime().toLong()
                            || getSystemTimeIn24Hour().terminalTime().toLong() > tptData.forceSettleTime.toLong()
                            ) /*&& batchData.size > 0*/
                ) {
                    logger("Auto Settle:- ", "Auto Settle Available")
                    val data = runBlocking(Dispatchers.IO) {
                        /*CreateSettlementPacket(
                            ProcessingCode.SETTLEMENT.code, batchData
                        ).createSettlementISOPacket()*/
                        // this code below converted

                        CreateSettlementPacket(appDao).createSettlementISOPacket()
                    }
                    lifecycleScope.launch(Dispatchers.IO) {
                        val settlementByteArray = data.generateIsoByteRequest()
                        (activity as NavigationActivity).settleBatch1(
                            settlementByteArray,
                            SettlementComingFrom.DASHBOARD.screenType
                        )
                    }
                } else
                    logger("Auto Settle:- ", "Auto Settle Mismatch Time")
            } else
                logger("Auto Settle:- ", "Auto Settle Not Available")
        } else {
            Log.d("Dashboard Close:- ", "Yes")
        }
    }
    //endregion


    private fun restartHandaling() {
        val restatDataList = AppPreference.getRestartDataPreference()
        if (restatDataList != null) {
            val uid=restatDataList.transactionUuid
            println("uid = $uid")
            DeviceHelper.getTransactionByUId(uid,object: OnOperationListener.Stub(){

                override fun onCompleted(p0: OperationResult?) {
                    val txnResponse = p0?.value as? TransactionDataResponse
                    p0?.value?.apply {
                        println("Status = $status")
                        println("Response code = $responseCode")
                        println("Response code = $responseCode")
                    }
                    when(txnResponse?.status){
                        RequestStatus.ABORTED,
                        RequestStatus.FAILED ->{

                        }
                        RequestStatus.SUCCESS ->{
                            val transactionDetail =
                                ((p0.value as? TransactionDataResponse)?.transactionDetail)
                            println("transactionDetail data = $transactionDetail")
                            if (transactionDetail != null) {
                                restartStubData(transactionDetail)
                            }
                        }

                    }


                }
            })
        }
    }

    fun restartStubData(transactionDetail: TransactionDetail){
        val cvmResult: com.ingenico.hdfcpayment.type.CvmAction?=null
        val cvmRequiredLimit:Long?=null
           val receiptDetail= ReceiptDetail(transactionDetail.authCode, transactionDetail.aid, transactionDetail.batchNumber, transactionDetail.cardHolderName, transactionDetail.cardType, transactionDetail.appName, "14/01/2022 19:09:18", transactionDetail.invoice, transactionDetail.mid, transactionDetail.merAddHeader1, transactionDetail.merAddHeader2,  transactionDetail.rrn, transactionDetail. stan, transactionDetail.tc,
               transactionDetail.tid, transactionDetail.tsi, transactionDetail.tvr, transactionDetail.entryMode, transactionDetail.txnName,
               transactionDetail.txnResponseCode, transactionDetail.txnAmount, transactionDetail.txnOtherAmount, transactionDetail.pan,
               isVerifyPin = true,
               isSignRequired = false,
               cvmResult = cvmResult,
             cvmRequiredLimit=cvmRequiredLimit
           )

        if (receiptDetail != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                val batchData = BatchTable(receiptDetail)

                batchData.invoice = receiptDetail.invoice.toString()
                println("invoice code = ${receiptDetail.invoice.toString()}")
                batchData.transactionType =
                    BhTransactionType.SALE.type
               // DBModule.appDatabase.appDao.insertBatchData(batchData)
                AppPreference.saveLastReceiptDetails(batchData)
                printingSaleData(batchData){


                }


            }
        }

    }
    suspend fun printingSaleData(batchTable: BatchTable, cb:suspend (Boolean) ->Unit) {
        val receiptDetail = batchTable.receiptData
        withContext(Dispatchers.Main) {
            (activity as BaseActivityNew). showProgress(getString(R.string.printing))
            var printsts = false
            if (receiptDetail != null) {
                PrintUtil(activity as BaseActivityNew).startPrinting(
                    batchTable,
                    EPrintCopyType.MERCHANT,
                   activity as BaseActivityNew
                ) { printCB, printingFail ->

                    (activity as BaseActivityNew).hideProgress()
                    if (printCB) {
                        printsts = printCB
                        lifecycleScope.launch(Dispatchers.Main) {
                            showMerchantAlertBox(batchTable, cb)
                        }

                    } else {
                        ToastUtils.showToast(
                            activity as BaseActivityNew,
                            getString(R.string.printer_error)
                        )
                        lifecycleScope.launch(Dispatchers.Main) {
                            cb(false)
                        }

                    }
                }
            }
        }
    }

    private fun showMerchantAlertBox(
        batchTable: BatchTable,
        cb: suspend (Boolean) ->Unit
    ) {
        lifecycleScope.launch(Dispatchers.Main) {

            val printerUtil: PrintUtil? = null
            (activity as BaseActivityNew).alertBoxWithAction(
                getString(R.string.print_customer_copy),
                getString(R.string.print_customer_copy),
                true, getString(R.string.positive_button_yes), { status ->
                    (activity as BaseActivityNew). showProgress(getString(R.string.printing))
                    PrintUtil(activity as BaseActivityNew).startPrinting(
                        batchTable,
                        EPrintCopyType.CUSTOMER,
                       activity as BaseActivityNew
                    ) { printCB, printingFail ->
                        (activity as BaseActivityNew).hideProgress()
                        if (printCB) {
                            lifecycleScope.launch(Dispatchers.IO) {

                                cb(printCB)
                            }
                            (activity as BaseActivityNew).hideProgress()

//                            val intent = Intent(this@TransactionActivity, NavigationActivity::class.java)
//                            startActivity(intent)
                        }

                    }
                }, {
                    lifecycleScope.launch(Dispatchers.IO) {


                        cb(true)
                    }
                    (activity as BaseActivityNew).hideProgress()
//                    val intent = Intent(this@TransactionActivity, NavigationActivity::class.java)
//                    startActivity(intent)
                })
        }
    }
}




//region===================BannerConfigModal:-
data class BannerConfigModal(
    var bannerImageBitmap: Bitmap,
    var bannerID: String,
    var bannerDisplayOrderID: String,
    var bannerShowOrHideID: String,
    var clickableOrNot: String,
    var bannerClickActionID: String,
    var bannerClickMessageData: String
)
//endregion