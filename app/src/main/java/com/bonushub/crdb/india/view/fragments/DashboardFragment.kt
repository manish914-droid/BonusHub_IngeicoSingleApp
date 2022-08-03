package com.bonushub.crdb.india.view.fragments


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
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.appupdate.SendAppUpdateConfirmationPacket
import com.bonushub.crdb.india.appupdate.SyncAppUpdateConfirmation
import com.bonushub.crdb.india.databinding.FragmentDashboardBinding
import com.bonushub.crdb.india.db.AppDao
import com.bonushub.crdb.india.di.DBModule
import com.bonushub.crdb.india.model.local.AppPreference
import com.bonushub.crdb.india.model.local.BatchTable
import com.bonushub.crdb.india.model.remote.RestartHandlingModel
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.Utility
import com.bonushub.crdb.india.utils.printerUtils.PrintUtil
import com.bonushub.crdb.india.view.activity.IFragmentRequest
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.adapter.DashBoardAdapter
import com.bonushub.crdb.india.view.base.BaseActivityNew
import com.bonushub.crdb.india.viewmodel.DashboardViewModel
import com.bonushub.crdb.india.viewmodel.SettlementViewModel
import com.bonushub.crdb.india.viewmodel.TransactionViewModel
import com.bonushub.crdb.india.vxutils.*
import com.ingenico.hdfcpayment.listener.OnOperationListener
import com.ingenico.hdfcpayment.model.ReceiptDetail
import com.ingenico.hdfcpayment.model.TransactionDetail
import com.ingenico.hdfcpayment.response.OperationResult
import com.ingenico.hdfcpayment.response.TransactionDataResponse
import com.ingenico.hdfcpayment.type.RequestStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.coroutines.*
import javax.inject.Inject

var isAutoSettleCodeInitiated=false
@AndroidEntryPoint
class DashboardFragment : androidx.fragment.app.Fragment() {
    companion object {
        var toRefresh = true
        val TAG = DashboardFragment::class.java.simpleName
    }
    private var counter = 0
    @Inject
    lateinit var appDao: AppDao
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

    private val transactionViewModel: TransactionViewModel by viewModels()

    private lateinit var settlementViewModel : SettlementViewModel


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
        AppPreference.saveBoolean(PrefConstant.BLOCK_MENU_OPTIONS.keyName.toString(), false)

        refreshToolbarLogos(activity as NavigationActivity)
        (activity as NavigationActivity).manageTopToolBar(true)
        isDashboardOpen = true
        Utility().hideSoftKeyboard(requireActivity())
        restartHandaling()
        dashboardViewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)

        settlementViewModel = ViewModelProvider(this).get(SettlementViewModel::class.java)
        observeDashboardViewModel()

        //region======================Change isAutoSettleDone Boolean Value to False if Date is greater then :- written by kushal
        //last saved Auto Settle Date:-
        if (!TextUtils.isEmpty(AppPreference.getString(PreferenceKeyConstant.LAST_SAVED_AUTO_SETTLE_DATE.keyName))) {
            if (AppPreference.getString(PreferenceKeyConstant.LAST_SAVED_AUTO_SETTLE_DATE.keyName).toInt() > 0) {
                if (getSystemTimeIn24Hour().terminalDate().toInt() > AppPreference.getString(PreferenceKeyConstant.LAST_SAVED_AUTO_SETTLE_DATE.keyName).toInt()
                ) {
                    isAutoSettleCodeInitiated=false
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

        /*(activity as NavigationActivity).alertBoxWithActionNew(getString(R.string.transaction_delined_msg),"",
            R.drawable.ic_txn_declined,"OK","",false,false,{},{})*/

       /* (activity as NavigationActivity).alertBoxWithActionNew("",getString(R.string.print_customer_copy),
            R.drawable.ic_print_customer_copy,"Yes","No",true,false,{},{})*/

        //DialogUtilsNew1.instaEmiDialog(activity,{ it.dismiss() },{ it.dismiss() },{ it.dismiss() })
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
            dashboardViewModel.eDashboardItem().observe(viewLifecycleOwner) {
                ioSope.launch(Dispatchers.Main) {
                    val result = async { setupRecyclerview(it) }.await()

                }

            }

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
                    //autoSettleBatch()
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
    var txnSyncing: Boolean = false
    //region=======================Check for User IDLE on Dashboard and do auto settle if conditions match:-
    /*private fun autoSettleBatch() {
        //val tptData = runBlocking(Dispatchers.IO) { TerminalParameterTable.selectFromSchemeTable() }
        val tptData = runBlocking(Dispatchers.IO) { getTptData() }
        //val batchData = runBlocking(Dispatchers.IO) { BatchFileDataTable.selectBatchData() }
        val batchData = runBlocking(Dispatchers.IO) { appDao.getAllBatchData() }
        val dataListReversal =  runBlocking(Dispatchers.IO) { appDao.getAllBatchReversalData() }
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
                val conditionForAutoSettle =((tptData.forceSettleTime == getSystemTimeIn24Hour().terminalTime() || getSystemTimeIn24Hour().terminalTime() > tptData.forceSettleTime)) &&  batchData.size > 0

                if (conditionForAutoSettle && !isAutoSettleCodeInitiated) {
                     isAutoSettleCodeInitiated=true
                    logger("Auto Settle:- ", "Auto Settle Available")
                    lifecycleScope.launch(Dispatchers.Main){

                        (activity as BaseActivityNew).showProgress("Transaction syncing...")
                          CoroutineScope(Dispatchers.IO).launch{
                              Utility().syncPendingTransaction(transactionViewModel){ it
                                  txnSyncing  = it
                              }

                                  (activity as BaseActivityNew).hideProgress()

                                  if(txnSyncing){
                                      PrintUtil(activity).printDetailReportupdate(batchData, activity) {
                                              detailPrintStatus ->

                                      }

                                      GlobalScope.launch(Dispatchers.Main) {
                                          var reversalTid  = checkReversal(dataListReversal)
                                          var listofTxnTid =  checkSettlementTid(batchData)

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

                                      GlobalScope.launch(Dispatchers.Main) {
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
                                                                  val data = CreateSettlementPacket(appDao).createSettlementISOPacket()
                                                                  val settlementByteArray = data.generateIsoByteRequest()
                                                                  try {
                                                                      (activity as NavigationActivity).settleBatch(settlementByteArray, SettlementComingFrom.DASHBOARD.screenType) { (activity as NavigationActivity).hideProgress()}
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
                                                      AppPreference.saveBoolean(PrefConstant.BLOCK_MENU_OPTIONS.keyName.toString(), true)
                                                      AppPreference.saveBoolean(PrefConstant.BLOCK_MENU_OPTIONS_INGENICO.keyName.toString(), true)
                                                      // Toast.makeText(activity,"Error called  ${result.error}", Toast.LENGTH_LONG).show()
                                                  }
                                                  Status.LOADING -> {
                                                      // Toast.makeText(activity,"Loading called  ${result.message}", Toast.LENGTH_LONG).show()


                                                  }
                                              }

                                          }
                                      }


                                  }else{
                                      logger("sync","failed terminate settlement")
                                      (activity as? NavigationActivity)?.hideProgress()
                                      Handler(Looper.getMainLooper()).postDelayed(Runnable {
                                          (activity as? BaseActivityNew)?.getInfoDialog("","Syncing failed settlement not allow.",R.drawable.ic_info){
                                              try {
                                                  (activity as? NavigationActivity)?.decideDashBoardOnBackPress()
                                              }catch (ex:Exception){
                                                  ex.printStackTrace()

                                              }
                                          }
                                      }
                                      ,500)
                                  }
                          }

                    }

//                    val data = runBlocking(Dispatchers.IO) {
//                        /*CreateSettlementPacket(
//                            ProcessingCode.SETTLEMENT.code, batchData
//                        ).createSettlementISOPacket()*/
//                        // this code below converted
//
//                        CreateSettlementPacket(appDao).createSettlementISOPacket()
//                    }
//                    lifecycleScope.launch(Dispatchers.IO) {
//                        val settlementByteArray = data.generateIsoByteRequest()
//                        (activity as NavigationActivity).settleBatch1(
//                            settlementByteArray,
//                            SettlementComingFrom.DASHBOARD.screenType
//                        )
//                    }
                } else
                    logger("Auto Settle:- ", "Auto Settle Mismatch Time  , Entered In Checking $isAutoSettleCodeInitiated")
            } else
                logger("Auto Settle:- ", "Auto Settle Not Available")
        } else {
            Log.d("Dashboard Close:- ", "Yes")
        }
    } */
    //endregion

    var restatDataList:RestartHandlingModel? = null

    private fun restartHandaling() {
        restatDataList = AppPreference.getRestartDataPreference()
        if (restatDataList != null) {
            val uid=restatDataList?.transactionUuid
            println("uid = $uid")
            DeviceHelper.getTransactionByUId(uid!!,object: OnOperationListener.Stub(){

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
logger("PFR","Failed","e")
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
        val aid=transactionDetail.aid?.replace(" ", "")

        val batcNumber= transactionDetail.batchNumber?.let { invoiceWithPadding(it) }
        val roc= transactionDetail.stan?.let { invoiceWithPadding(it) }
        val invoice=transactionDetail.invoice?.let { invoiceWithPadding(it) }
         val penmasking = transactionDetail.pan?.let {
             Field48ResponseTimestamp.panMasking(
                 it,
                 "************0000"
             )
         }

           val receiptDetail= ReceiptDetail(transactionDetail.authCode, aid, batcNumber, transactionDetail.cardHolderName, transactionDetail.cardType, transactionDetail.appName, transactionDetail.dateTime, invoice, transactionDetail.mid, transactionDetail.merAddHeader1, transactionDetail.merAddHeader2,  transactionDetail.rrn, roc, transactionDetail.tc,
               transactionDetail.tid, transactionDetail.tsi, transactionDetail.tvr, transactionDetail.entryMode, transactionDetail.txnName,
               transactionDetail.txnResponseCode, transactionDetail.txnAmount, transactionDetail.txnOtherAmount, penmasking,
               isVerifyPin = true,
               isSignRequired = false,
               cvmResult = cvmResult,
             cvmRequiredLimit=cvmRequiredLimit
           )


        if (receiptDetail != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                var batchData = BatchTable(null)
                // here get batch data
                if(restatDataList?.transactionType == EDashboardItem.BANK_EMI || restatDataList?.transactionType == EDashboardItem.BRAND_EMI || restatDataList?.transactionType == EDashboardItem.TEST_EMI)
                {
                    restatDataList?.batchData?.receiptData = receiptDetail
                    batchData = restatDataList?.batchData!!
                }else{
                    batchData = BatchTable(receiptDetail)
                }

                batchData.invoice = receiptDetail.invoice.toString()
                println("invoice code = ${receiptDetail.invoice.toString()}")
                // batchData.transactionType = TransactionType.SALE.type
                DBModule.appDatabase.appDao.insertBatchData(batchData)

                if(batchData.receiptData?.txnOtherAmount == null)
                {
                    batchData.receiptData?.txnOtherAmount = "0"
                }
//                AppPreference.saveLastReceiptDetails(batchData)

                printingSaleData(batchData){
                    AppPreference.clearRestartDataPreference()
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