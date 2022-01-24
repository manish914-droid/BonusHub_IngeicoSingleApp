package com.bonushub.crdb.view.activity

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bonushub.crdb.HDFCApplication
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.ActivityEmvBinding
import com.bonushub.crdb.db.AppDatabase
import com.bonushub.crdb.di.DBModule.appDatabase
import com.bonushub.crdb.entity.CardOption
import com.bonushub.crdb.model.CardProcessedDataModal
import com.bonushub.crdb.model.local.*
import com.bonushub.crdb.model.remote.*
import com.bonushub.crdb.repository.GenericResponse
import com.bonushub.crdb.repository.ServerRepository
import com.bonushub.crdb.serverApi.RemoteService
import com.bonushub.crdb.serverApi.bankEMIRequestCode
import com.bonushub.crdb.transactionprocess.CreateTransactionPacket
import com.bonushub.crdb.utils.*
import com.bonushub.crdb.utils.Field48ResponseTimestamp.getTptData
import com.bonushub.crdb.utils.Field48ResponseTimestamp.getTptDataByTid
import com.bonushub.crdb.utils.printerUtils.PrintUtil
import com.bonushub.crdb.view.base.BaseActivityNew
import com.bonushub.crdb.view.fragments.AuthCompletionData
import com.bonushub.crdb.viewmodel.PendingSyncTransactionViewModel
import com.bonushub.crdb.viewmodel.SearchViewModel
import com.bonushub.crdb.viewmodel.TenureSchemeViewModel
import com.bonushub.crdb.viewmodel.TransactionViewModel
import com.bonushub.crdb.viewmodel.viewModelFactory.TenureSchemeActivityVMFactory
import com.bonushub.pax.utils.*
import com.google.gson.Gson
import com.ingenico.hdfcpayment.listener.OnPaymentListener
import com.ingenico.hdfcpayment.model.ReceiptDetail
import com.ingenico.hdfcpayment.request.*
import com.ingenico.hdfcpayment.response.PaymentResult
import com.ingenico.hdfcpayment.response.TransactionResponse
import com.ingenico.hdfcpayment.type.CardCaptureType
import com.ingenico.hdfcpayment.type.ResponseCode
import com.ingenico.hdfcpayment.type.TransactionType
import com.usdk.apiservice.aidl.printer.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_new_input_amount.*
import kotlinx.coroutines.*
import java.util.*

@AndroidEntryPoint
class TransactionActivity : BaseActivityNew() {
    /** need to use Hilt for instance initializing here..*/
    private val remoteService: RemoteService = RemoteService()
    private val dbObj: AppDatabase = AppDatabase.getInstance(HDFCApplication.appContext)
    private val serverRepository: ServerRepository = ServerRepository(dbObj, remoteService)


    private val pendingSyncTransactionViewModel: PendingSyncTransactionViewModel by viewModels()
    private var emvBinding: ActivityEmvBinding? = null

    // private val transactionAmountValue by lazy { intent.getStringExtra("amt") ?: "0" }
    private val transactionViewModel: TransactionViewModel by viewModels()

    //used for other cash amount
    //  private val transactionOtherAmountValue by lazy { intent.getStringExtra("otherAmount") ?: "0" }
    private val restartHandlingList: MutableList<RestartHandlingModel> by lazy { mutableListOf<RestartHandlingModel>() }
    private val testEmiOption by lazy { intent.getStringExtra("TestEmiOption") ?: "0" }
    private val brandEmiCatData by lazy { intent.getSerializableExtra("brandEmiCat") as BrandEMISubCategoryTable }
    private val brandEmiSubCatData by lazy { intent.getSerializableExtra("brandEmiSubCatData") as BrandEMISubCategoryTable } //: BrandEMISubCategoryTable? = null
   private val brandEmiProductData by lazy { intent.getSerializableExtra("brandEmiProductData") as BrandEMIProductDataModal }
    private val brandDataMaster by lazy { intent.getSerializableExtra("brandDataMaster") as BrandEMIMasterDataModal }
    private val imeiOrSerialNum by lazy { intent.getStringExtra("imeiOrSerialNum") ?: "" }


    private val saleAmt by lazy { intent.getStringExtra("saleAmt") ?: "0" }
    private var field54Data:Long?= null
    private val cashBackAmt by lazy { intent.getStringExtra("cashBackAmt") ?: "0" }
    private val authCompletionData by lazy { intent.getSerializableExtra("authCompletionData") as AuthCompletionData }

    private val mobileNumber by lazy { intent.getStringExtra("mobileNumber") ?: "" }

    private val billNumber by lazy { intent.getStringExtra("billNumber") ?: "0" }
    private val saleWithTipAmt by lazy { intent.getStringExtra("saleWithTipAmt") ?: "0" }
    private val title by lazy { intent.getStringExtra("title") }
    private val transactionType by lazy { intent.getIntExtra("type", -1947) }
    private val transactionTypeEDashboardItem by lazy {
        (intent.getSerializableExtra("edashboardItem") ?: EDashboardItem.NONE) as EDashboardItem
    }
    val TAG = TransactionActivity::class.java.simpleName

    private val searchCardViewModel: SearchViewModel by viewModels()
    private var globalCardProcessedModel = CardProcessedDataModal()


    //  private lateinit var deviceService: UsdkDeviceService
    /* @Inject
     lateinit var appDao: AppDao*/

    private var tid = "00000000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        emvBinding = ActivityEmvBinding.inflate(layoutInflater)
        setContentView(emvBinding?.root)
        emvBinding?.subHeaderView?.subHeaderText?.text = transactionTypeEDashboardItem.title
        globalCardProcessedModel.setTransType(transactionType)

        if (transactionTypeEDashboardItem == EDashboardItem.BRAND_EMI || transactionTypeEDashboardItem == EDashboardItem.BANK_EMI || transactionTypeEDashboardItem == EDashboardItem.TEST_EMI ) {
            emvBinding?.cardDetectImg?.visibility = View.VISIBLE
            emvBinding?.tvInsertCard?.visibility = View.VISIBLE
            emvBinding?.subHeaderView?.backImageButton?.visibility = View.VISIBLE
        }else{
            emvBinding?.cardDetectImg?.visibility = View.GONE
            emvBinding?.tvInsertCard?.visibility = View.GONE
            emvBinding?.subHeaderView?.backImageButton?.visibility = View.VISIBLE

        }
        emvBinding?.subHeaderView?.backImageButton?.setOnClickListener {
            finish()
            startActivity(Intent(this, NavigationActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
        emvBinding?.baseAmtTv?.text = saleAmt
        lifecycleScope.launch(Dispatchers.IO) {
            tid = getBaseTID(appDatabase.appDao)
            globalCardProcessedModel.setTransType(transactionType)
            setupFlow()
        }

    }



    override fun onBackPressed() {
        // For stopping backPress............
    }

    private lateinit var tenureSchemeViewModel: TenureSchemeViewModel
    private suspend fun setupEmvObserver() {
        withContext(Dispatchers.Main) {
            searchCardViewModel.allcadType.observe(
                this@TransactionActivity,
                Observer { cardProcessDataModel ->
                    globalCardProcessedModel=cardProcessDataModel
                    globalCardProcessedModel.setTransactionAmount((saleAmt.toDouble() * 100).toLong())
                    when (globalCardProcessedModel.getReadCardType()) {
                        DetectCardType.EMV_CARD_TYPE -> {
                            when (globalCardProcessedModel.getTransType()) {
                                BhTransactionType.SALE.type -> {
                                    showProgress()
                                    continueTenureProcess()
                                }

                                BhTransactionType.BRAND_EMI.type -> {
                                    val intent = Intent(
                                        this@TransactionActivity,
                                        TenureSchemeActivity::class.java
                                    ).apply {
                                        val field57 =
                                            "$bankEMIRequestCode^0^${brandDataMaster.brandID}^${brandEmiProductData.productID}^${imeiOrSerialNum}" +
                                                    "^${/*cardBinValue.substring(0, 8)*/""}^${globalCardProcessedModel?.getTransactionAmount()}"
                                        putExtra("cardProcessedData", globalCardProcessedModel)
                                        putExtra("brandID", brandDataMaster.brandID)
                                        putExtra("productID", brandEmiProductData.productID)
                                        putExtra("imeiOrSerialNum", imeiOrSerialNum)
                                        putExtra(
                                            "transactionType",
                                            globalCardProcessedModel.getTransType()
                                        )
                                        putExtra("mobileNumber", mobileNumber)
                                    }
                                    startActivityForResult(intent, BhTransactionType.BRAND_EMI.type)
                                }

                                BhTransactionType.EMI_SALE.type , BhTransactionType.TEST_EMI.type->{
                                    val intent = Intent(
                                        this@TransactionActivity,
                                        TenureSchemeActivity::class.java
                                    ).apply {

                                        putExtra("testEmiOption", testEmiOption)
                                        putExtra("cardProcessedData", globalCardProcessedModel)
                                        putExtra("transactionType", globalCardProcessedModel.getTransType())
                                    }
                                    startActivityForResult(intent, globalCardProcessedModel.getTransType())

                                }

                            }

                        }
                        DetectCardType.CONTACT_LESS_CARD_TYPE -> {
                            //  Toast.makeText(this@TransactionActivity,"Contactless mode detected",Toast.LENGTH_LONG).show()
                        }
                        DetectCardType.MAG_CARD_TYPE -> {
                            Toast.makeText(
                                this@TransactionActivity,
                                "Swipe mode detected",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        else -> {

                        }
                    }

                })
        }
    }

    private fun continueTenureProcess(){
        val f57= "$bankEMIRequestCode^0^1^0^^${
            globalCardProcessedModel.getPanNumberData()?.substring(0, 8)}^${globalCardProcessedModel?.getTransactionAmount()}"
        tenureSchemeViewModel = ViewModelProvider(
            this@TransactionActivity, TenureSchemeActivityVMFactory(
                serverRepository,
                globalCardProcessedModel.getPanNumberData() ?: "",
                f57
            )
        ).get(TenureSchemeViewModel::class.java)
        tenureSchemeViewModel.emiTenureLiveData.observe(
            this,
            {
                hideProgress()
                when (val genericResp = it) {
                    is GenericResponse.Success -> {
                        println(Gson().toJson(genericResp.data))
                        val resp= genericResp.data as TenuresWithIssuerTncs
                        showEMISaleDialog(resp.bankEMIIssuerTAndCList, resp.bankEMISchemesDataList)
                    }
                    is GenericResponse.Error -> {
                       // ToastUtils.showToast(this, genericResp.errorMessage)
                        println(genericResp.errorMessage.toString())
                        lifecycleScope.launch(Dispatchers.IO) {
                            initiateNormalSale()
                        }
                    }
                    is GenericResponse.Loading -> {

                    }
                }
            })




    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // conversion of sale to Bank emi
        var reqCode=requestCode
        if(requestCode== BhTransactionType.SALE.type ){
            reqCode= BhTransactionType.EMI_SALE.type
            globalCardProcessedModel.setTransType(BhTransactionType.EMI_SALE.type)
        }
        if (reqCode == BhTransactionType.BRAND_EMI.type || reqCode == BhTransactionType.EMI_SALE.type || reqCode == BhTransactionType.TEST_EMI.type ) {
            emvBinding?.subHeaderView?.subHeaderText?.text =getTransactionTypeName(globalCardProcessedModel.getTransType())
            if (resultCode == RESULT_OK) {
                val emiTenureData =
                    data?.getParcelableExtra<BankEMITenureDataModal>("EMITenureDataModal")
                val emiIssuerData =
                    data?.getParcelableExtra<BankEMIIssuerTAndCDataModal>("emiIssuerTAndCDataList")
                val cardProcessedDataModal =
                    data?.getSerializableExtra("cardProcessedDataModal") as CardProcessedDataModal
                emvBinding?.cardDetectImg?.visibility = View.GONE
                emvBinding?.tvInsertCard?.visibility = View.GONE
                emvBinding?.subHeaderView?.backImageButton?.visibility = View.VISIBLE
                try {

                    var amt= 0L
                    if(reqCode == BhTransactionType.TEST_EMI.type){
                        globalCardProcessedModel.testEmiOption=testEmiOption
                        emiTenureData?.txnTID= getTidForTestTxn(testEmiOption)
                        amt=100L
                    }else{
                        amt  = (saleAmt.toFloat() * 100).toLong()
                    }
                    DeviceHelper.doEMITxn(
                        EMISaleRequest(
                            amount = amt,
                            tipAmount = 0L,
                            emiTxnName = getTransactionTypeName(globalCardProcessedModel.getTransType()),//transactionTypeEDashboardItem.title,
                            tid = emiTenureData?.txnTID,
                            cardCaptureType = CardCaptureType.EMV_NO_CAPTURING,
                            track1 = null,
                            track2 = null,
                            transactionUuid = UUID.randomUUID().toString(),
                        ),
                        listener = object : OnPaymentListener.Stub() {
                            override fun onCompleted(result: PaymentResult?) {
                                val txnResponse = result?.value as? TransactionResponse
                                val receiptDetail = txnResponse?.receiptDetail


                                receiptDetail?.txnName = getTransactionTypeName(globalCardProcessedModel.getTransType())
                                Log.d(TAG, "Response Code: ${txnResponse?.responseCode}")
                              var isUnblockingNeeded=false
                                    when (txnResponse?.responseCode) {
                                    ResponseCode.SUCCESS.value -> {
                                        val jsonResp = Gson().toJson(receiptDetail)
                                        println(jsonResp)
                                       // AppPreference.saveLastReceiptDetails(jsonResp) // save last sale receipt11
                                        if (receiptDetail != null) {
                                            val batchData = BatchTable(receiptDetail)
                                            val tpt = runBlocking(Dispatchers.IO) {
                                                getTptDataByTid(receiptDetail.tid ?: "")
                                            }
                                            println("Selected tid id "+tpt?.terminalId)
                                            println("Batch number in emi "+tpt?.batchNumber)
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                batchData.emiIssuerDataModel = emiIssuerData
                                                batchData.invoice = receiptDetail.invoice.toString()
                                                if(requestCode == BhTransactionType.BRAND_EMI.type) {
                                                    batchData.emiBrandData = brandDataMaster
                                                    batchData.emiSubCategoryData = brandEmiSubCatData
                                                    batchData.emiCategoryData = brandEmiCatData
                                                    batchData.emiProductData = brandEmiProductData
                                                }
                                                batchData.imeiOrSerialNum = imeiOrSerialNum
                                                batchData.mobileNumber = mobileNumber // kushal add mobile num
                                                batchData.billNumber=billNumber
                                                batchData.emiTenureDataModel = emiTenureData
                                                    batchData.transactionType =
                                                        globalCardProcessedModel.getTransType()
                                                appDatabase.appDao.insertBatchData(batchData)
                                                createCardProcessingModelData(receiptDetail)
                                                // because we did not save pan num in plain
                                                cardProcessedDataModal.getPanNumberData()?.let {
                                                    globalCardProcessedModel.setPanNumberData(
                                                        it
                                                    )
                                                }
                                                AppPreference.saveLastReceiptDetails(batchData)
                                                printingSaleData(batchData){
                                                    // region sync transaction
                                                    withContext(Dispatchers.Main) {
                                                        showProgress(getString(R.string.transaction_syncing_msg))
                                                    }
                                                    val transactionISO = CreateTransactionPacket(globalCardProcessedModel,batchData).createTransactionPacket()
                                                    //   sync pending transaction
                                                   //    Utility().syncPendingTransaction(transactionViewModel)
                                                    when(val genericResp = transactionViewModel.serverCall(transactionISO))
                                                    {
                                                        is GenericResponse.Success -> {
                                                            logger("success:- ", "in success $genericResp","e")
                                                            withContext(Dispatchers.Main) {
                                                                hideProgress()
                                                            }

                                                        }
                                                        is GenericResponse.Error -> {
                                                            logger("error:- ", "in error ${genericResp.errorMessage}", "e")
                                                            logger("error:- ", "save transaction sync later", "e")
                                                            val pendingSyncTransactionTable = PendingSyncTransactionTable(invoice = receiptDetail?.invoice.toString(),
                                                                batchTable = batchData,
                                                                responseCode = genericResp.toString(),
                                                                cardProcessedDataModal = globalCardProcessedModel)
                                                            pendingSyncTransactionViewModel.insertPendingSyncTransactionData(pendingSyncTransactionTable)
                                                            withContext(Dispatchers.Main) {
                                                                hideProgress()
                                                                errorOnSyncing(genericResp.errorMessage?:"Sync Error....")
                                                            }
                                                        }
                                                        is GenericResponse.Loading -> {
                                                            logger("Loading:- ", "in Loading $genericResp","e")
                                                            withContext(Dispatchers.Main) {
                                                                hideProgress()
                                                            }
                                                        }
                                                    }
                                                    // end region

                                                }


                                            }
                                        }
                                    }
                                    ResponseCode.FAILED.value,
                                    ResponseCode.ABORTED.value -> {
                                        isUnblockingNeeded=true
                                    }
                                    ResponseCode.REVERSAL.value -> {
                                        isUnblockingNeeded=true
                                        AppPreference.saveLastCancelReceiptDetails(receiptDetail)
                                        val batchReversalData = BatchTableReversal(receiptDetail)
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            //    appDao.insertBatchData(batchData)
                                            batchReversalData.invoice =
                                                receiptDetail?.invoice.toString()
                                            batchReversalData.transactionType =
                                                BhTransactionType.SALE.type
                                            batchReversalData.responseCode =
                                                ResponseCode.SUCCESS.value
                                            batchReversalData.roc = receiptDetail?.stan.toString()
                                            appDatabase.appDao.insertBatchReversalData(
                                                batchReversalData
                                            )
                                        }
                                    }
                                    else -> {
                                        isUnblockingNeeded=true
                                    }
                                }

                                if (emiTenureData != null && isUnblockingNeeded) {
                                    if (emiIssuerData != null) {
                                        txnResponse?.responseCode?.let {
                                            txnResponse.status.toString().let { it1 ->
                                                unBlockingImeiSerialNum(emiTenureData,emiIssuerData,
                                                    it, it1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
            if (resultCode == RESULT_CANCELED) {
                // Write  code if there's no result
            }
        }
    }

    private fun unBlockingImeiSerialNum(bankEmiTenureData:BankEMITenureDataModal,schemeData:BankEMIIssuerTAndCDataModal,txnRespCode:String,txnResponseMsg:String){
        if(imeiOrSerialNum.isNotBlank()){
            var isBlockUnblockSuccess=Pair(false,"")

            //   "Request Type^Skip Record Count^Brand Id^ProductID^Product serial^Bin Value^Transaction Amt^Issuer Id^Mobile No^EMI Scheme^Tenure^txn response code^txn response msg"

            val field57="13^0^${brandDataMaster.brandID}^${brandEmiProductData.productID}^${imeiOrSerialNum}^${globalCardProcessedModel.getEncryptedPan()}^${bankEmiTenureData.totalEmiPay}^${schemeData.issuerID}^${mobileNumber}^${schemeData.emiSchemeID}^${bankEmiTenureData.tenure}^${txnRespCode}^${txnResponseMsg}"

            showProgress("Blocking Serial/IMEI")
            lifecycleScope.launch(Dispatchers.IO) {
                isBlockUnblockSuccess=  serverRepository.blockUnblockSerialNum(field57)
                hideProgress()
                if(isBlockUnblockSuccess.first){
                    errorFromIngenico(
                        txnRespCode,
                        txnResponseMsg
                    )

                }else {
                    withContext(Dispatchers.Main) {
                        showToast(isBlockUnblockSuccess.second)
                    }
                }

            }

        }else{
            errorFromIngenico(
                txnRespCode,
                txnResponseMsg
            )
        }

    }



    //Below function is used to deal with EMV Card Fallback when we insert EMV Card from other side then chip side:-
    fun handleEMVFallbackFromError(
        title: String,
        msg: String,
        showCancelButton: Boolean,
        emvFromError: (Boolean) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.Main) {
            alertBoxWithAction(title,
                msg, showCancelButton, getString(R.string.positive_button_ok), { alertCallback ->
                    if (alertCallback) {
                        emvFromError(true)
                    }
                }, {})
        }
    }


    private suspend fun setupFlow() {

        when (transactionTypeEDashboardItem) {
            EDashboardItem.BRAND_EMI , EDashboardItem.BANK_EMI , EDashboardItem.TEST_EMI-> {
                searchCardViewModel.fetchCardTypeData(
                    globalCardProcessedModel,
                    CardOption.create().apply {
                        supportICCard(true)
                        supportMagCard(true)
                        supportRFCard(false)
                    })

                setupEmvObserver()

            }
            EDashboardItem.SALE -> {
                // Checking Insta Emi Available or not
                var hasInstaEmi = false
                val tpt =
                    withContext(Dispatchers.IO) { Field48ResponseTimestamp.getTptData() }
                var limitAmt = 0f
                if (tpt?.surChargeValue?.isNotEmpty()!!) {
                    limitAmt = try {
                        tpt.surChargeValue.toFloat() / 100
                    } catch (ex: Exception) {
                        0f
                    }
                }
                if (tpt.surcharge.isNotEmpty()) {
                    hasInstaEmi = try {
                        tpt.surcharge == "1"
                    } catch (ex: Exception) {
                        false
                    }
                }

                // Condition executes, If insta EMI is Available on card
                if ((saleAmt.toFloat() * 100).toLong() >=  limitAmt && hasInstaEmi) {
                    withContext(Dispatchers.Main) {
                        emvBinding?.cardDetectImg?.visibility = View.VISIBLE
                        emvBinding?.tvInsertCard?.visibility = View.VISIBLE
                        emvBinding?.subHeaderView?.backImageButton?.visibility = View.VISIBLE
                    }
                    searchCardViewModel.fetchCardTypeData(
                        globalCardProcessedModel,
                        CardOption.create().apply {
                            supportICCard(true)
                            supportMagCard(true)
                            supportRFCard(false)
                        })

                    setupEmvObserver()

                } else {
                    initiateNormalSale()
                }

            }

            EDashboardItem.CASH_ADVANCE -> {
                val amt = (saleAmt.toFloat() * 100).toLong()
                var ecrID: String
                field54Data= amt
                try {
                    DeviceHelper.doCashAdvanceTxn(
                        CashOnlyRequest(
                            cashAmount = amt,
                            tid = tid,
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
                                        val jsonResp = Gson().toJson(receiptDetail)
                                        println(jsonResp)


                                       // AppPreference.saveLastReceiptDetails(jsonResp) // save last sale receipt11
                                        //   detailResponse.forEach { println(it) }
                                        //  uids.add(ecrID)
                                        // defaultScope.launch { onSaveUId(ecrID, handleLoadingUIdsResult) }
                                        if (receiptDetail != null) {
                                            val batchData = BatchTable(receiptDetail)

                                            // region print and save data
                                            lifecycleScope.launch(Dispatchers.IO){
                                                //    appDao.insertBatchData(batchData)
                                                batchData.invoice = receiptDetail.invoice.toString()
                                                batchData.transactionType =
                                                    BhTransactionType.CASH_AT_POS.type
                                                appDatabase.appDao.insertBatchData(batchData)
                                                AppPreference.saveLastReceiptDetails(batchData)

                                                printingSaleData(batchData){
                                                    // region sync transaction
                                                    withContext(Dispatchers.Main) {
                                                        showProgress(getString(R.string.transaction_syncing_msg))
                                                    }

                                                    createCardProcessingModelData(receiptDetail)
                                                    val transactionISO = CreateTransactionPacket(globalCardProcessedModel).createTransactionPacket()

                                                    // sync pending transaction
                                                  //  Utility().syncPendingTransaction(transactionViewModel)

                                                    when(val genericResp = transactionViewModel.serverCall(transactionISO))
                                                    {
                                                        is GenericResponse.Success -> {
                                                            logger("success:- ", "in success $genericResp","e")
                                                            withContext(Dispatchers.Main) {
                                                                hideProgress()
                                                            }
                                                        }
                                                        is GenericResponse.Error -> {
                                                            logger("error:- ", "in error $genericResp", "e")
                                                            logger("error:- ", "save transaction sync later", "e")

                                                            val pendingSyncTransactionTable = PendingSyncTransactionTable(invoice = receiptDetail?.invoice.toString(),
                                                                batchTable = batchData,
                                                                responseCode = genericResp?.toString(),
                                                                cardProcessedDataModal = globalCardProcessedModel)

                                                            pendingSyncTransactionViewModel.insertPendingSyncTransactionData(pendingSyncTransactionTable)
                                                            withContext(Dispatchers.Main) {
                                                                hideProgress()
                                                                errorOnSyncing(genericResp.errorMessage?:"Sync Error....")
                                                            }
                                                        }
                                                        is GenericResponse.Loading -> {
                                                            logger("Loading:- ", "in Loading $genericResp","e")
                                                            withContext(Dispatchers.Main) {
                                                                hideProgress()
                                                            }
                                                        }
                                                    }
                                                }

                                            }
                                            // endregion

                                        }


                                    }
                                    ResponseCode.FAILED.value,
                                    ResponseCode.ABORTED.value -> {
                                        errorFromIngenico(
                                            txnResponse.responseCode,
                                            txnResponse.status.toString()
                                        )
                                    }
                                    ResponseCode.REVERSAL.value -> {
                                        // kushal
                                        // region
                                        //val jsonResp=Gson().toJson("{\"aid\":\"A0000000041010\",\"appName\":\"Mastercard\",\"authCode\":\"005352\",\"batchNumber\":\"000008\",\"cardHolderName\":\"SANDEEP SARASWAT          \",\"cardType\":\"UP        \",\"cvmRequiredLimit\":0,\"cvmResult\":\"NO_CVM\",\"dateTime\":\"20/12/2021 11:07:26\",\"entryMode\":\"INSERT\",\"invoice\":\"000001\",\"isSignRequired\":false,\"isVerifyPin\":true,\"maskedPan\":\"** ** ** 4892\",\"merAddHeader1\":\"INGBH TEST1 TID\",\"merAddHeader2\":\"NOIDA\",\"mid\":\"               \",\"rrn\":\"000000000035\",\"stan\":\"000035\",\"tc\":\"3BAC31335BDB3383\",\"tid\":\"30160031\",\"tsi\":\"E800\",\"tvr\":\"0400048000\",\"txnAmount\":\"50000\",\"txnName\":\"SALE\",\"txnOtherAmount\":\"0\",\"txnResponseCode\":\"00\"}")
                                        /*val jsonResp=Gson().toJson(ReceiptDetail)
                                        println(jsonResp)

                                        try {
                                            val str = jsonResp
                                            if (!str.isNullOrEmpty()) {
                                                Gson().fromJson<ReceiptDetail>(
                                                    str,
                                                    object : TypeToken<ReceiptDetail>() {}.type
                                                )
                                            } else null
                                        } catch (ex: Exception) {
                                            ex.printStackTrace()
                                        }*/

                                        AppPreference.saveLastCancelReceiptDetails(receiptDetail)

                                        val batchReversalData = BatchTableReversal(receiptDetail)

                                        lifecycleScope.launch(Dispatchers.IO) {
                                            //    appDao.insertBatchData(batchData)
                                            batchReversalData.invoice =
                                                receiptDetail?.invoice.toString()
                                            batchReversalData.transactionType =
                                                BhTransactionType.SALE.type
                                            batchReversalData.responseCode =
                                                ResponseCode.SUCCESS.value
                                            batchReversalData.roc = receiptDetail?.stan.toString()
                                            appDatabase.appDao.insertBatchReversalData(
                                                batchReversalData
                                            )
                                        }
                                    }
                                    else -> {
                                        errorFromIngenico(
                                            txnResponse?.responseCode,
                                            txnResponse?.status.toString()
                                        )
                                    }
                                }
                            }
                        }
                    )
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }
            EDashboardItem.SALE_WITH_CASH -> {
                try {
                    val amt = (saleAmt.toFloat() * 100).toLong()
                    val cashBackAmount = (cashBackAmt.toFloat() * 100).toLong()
                    field54Data=cashBackAmount
                    var ecrID: String

                    DeviceHelper.doSaleWithCashTxn(
                        SaleCashBackRequest(
                            amount = amt,
                            cashAmount = cashBackAmount,
                            tid = tid,
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
                                        val jsonResp = Gson().toJson(receiptDetail)
                                        println(jsonResp)

                                     //   AppPreference.saveLastReceiptDetails(jsonResp) // save last sale receipt11

                                        //   detailResponse.forEach { println(it) }
                                        //  uids.add(ecrID)
                                        // defaultScope.launch { onSaveUId(ecrID, handleLoadingUIdsResult) }
                                        if (receiptDetail != null) {
                                            val batchData = BatchTable(receiptDetail)

                                            // region print and save data
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                batchData.invoice = receiptDetail.invoice.toString()
                                                batchData.transactionType =
                                                    BhTransactionType.SALE_WITH_CASH.type
                                                appDatabase.appDao.insertBatchData(batchData)
                                                AppPreference.saveLastReceiptDetails(batchData)
                                                printingSaleData(batchData){
                                                    withContext(Dispatchers.Main) {
                                                        showProgress(getString(R.string.transaction_syncing_msg))
                                                    }
                                                    createCardProcessingModelData(receiptDetail)
                                                    val transactionISO = CreateTransactionPacket(globalCardProcessedModel).createTransactionPacket()
                                                    // sync pending transaction
                                                //    Utility().syncPendingTransaction(transactionViewModel)
                                                    when(val genericResp = transactionViewModel.serverCall(transactionISO))
                                                    {
                                                        is GenericResponse.Success -> {
                                                            logger("success:- ", "in success $genericResp","e")
                                                            withContext(Dispatchers.Main) {
                                                                hideProgress()
                                                            }
                                                        }
                                                        is GenericResponse.Error -> {
                                                            logger("error:- ", "in error $genericResp", "e")
                                                            logger("error:- ", "save transaction sync later", "e")

                                                            val pendingSyncTransactionTable = PendingSyncTransactionTable(invoice = receiptDetail?.invoice.toString(),
                                                                batchTable = batchData,
                                                                responseCode = genericResp?.toString(),
                                                                cardProcessedDataModal = globalCardProcessedModel)

                                                            pendingSyncTransactionViewModel.insertPendingSyncTransactionData(pendingSyncTransactionTable)
                                                            withContext(Dispatchers.Main) {
                                                                hideProgress()
                                                                errorOnSyncing(genericResp.errorMessage?:"Sync Error....")
                                                            }
                                                        }
                                                        is GenericResponse.Loading -> {
                                                            logger("Loading:- ", "in Loading $genericResp","e")
                                                            withContext(Dispatchers.Main) {
                                                                hideProgress()
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }


                                    }
                                    ResponseCode.FAILED.value,
                                    ResponseCode.ABORTED.value -> {
                                        errorFromIngenico(
                                            txnResponse.responseCode,
                                            txnResponse.status.toString()
                                        )
                                    }
                                    ResponseCode.REVERSAL.value -> {
                                        // kushal
                                        // region

                                        AppPreference.saveLastCancelReceiptDetails(receiptDetail)

                                        val batchReversalData = BatchTableReversal(receiptDetail)

                                        lifecycleScope.launch(Dispatchers.IO) {
                                            //    appDao.insertBatchData(batchData)
                                            batchReversalData.invoice =
                                                receiptDetail?.invoice.toString()
                                            batchReversalData.transactionType =
                                                BhTransactionType.SALE.type
                                            batchReversalData.responseCode =
                                                ResponseCode.SUCCESS.value
                                            batchReversalData.roc = receiptDetail?.stan.toString()
                                            appDatabase.appDao.insertBatchReversalData(
                                                batchReversalData
                                            )
                                        }
                                    }
                                    else -> {
                                        errorFromIngenico(
                                            txnResponse?.responseCode,
                                            txnResponse?.status.toString()
                                        )
                                    }
                                }
                            }
                        }
                    )
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }
            EDashboardItem.REFUND -> {
                try {
                    val amt = (saleAmt.toFloat() * 100).toLong()
                    //  val cashBackAmount=(cashBackAmt.toFloat() * 100).toLong()
                    var ecrID: String

                    DeviceHelper.doRefundTxn(
                        RefundRequest(
                            amount = amt,
                            tid = tid,
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
                                        val jsonResp = Gson().toJson(receiptDetail)
                                        println(jsonResp)

                                      //  AppPreference.saveLastReceiptDetails(jsonResp) // save last sale receipt11

                                        //   detailResponse.forEach { println(it) }
                                        //  uids.add(ecrID)
                                        // defaultScope.launch { onSaveUId(ecrID, handleLoadingUIdsResult) }
                                        if (receiptDetail != null) {
                                            val batchData = BatchTable(receiptDetail)

                                            // region print and save data
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                //    appDao.insertBatchData(batchData)
                                                batchData.invoice = receiptDetail.invoice.toString()
                                                batchData.transactionType =
                                                    BhTransactionType.REFUND.type
                                                appDatabase.appDao.insertBatchData(batchData)
                                                AppPreference.saveLastReceiptDetails(batchData)
                                                printingSaleData(batchData){
                                                    withContext(Dispatchers.Main) {
                                                        showProgress(getString(R.string.transaction_syncing_msg))
                                                    }

                                                    createCardProcessingModelData(receiptDetail)
                                                    val transactionISO = CreateTransactionPacket(globalCardProcessedModel).createTransactionPacket()
                                                    // sync pending transaction
                                                   //   Utility().syncPendingTransaction(transactionViewModel)

                                                    when(val genericResp = transactionViewModel.serverCall(transactionISO))
                                                    {
                                                        is GenericResponse.Success -> {
                                                            logger("success:- ", "in success $genericResp","e")
                                                            withContext(Dispatchers.Main) {
                                                                hideProgress()
                                                            }
                                                        }
                                                        is GenericResponse.Error -> {
                                                            logger("error:- ", "in error $genericResp", "e")
                                                            logger("error:- ", "save transaction sync later", "e")

                                                            val pendingSyncTransactionTable = PendingSyncTransactionTable(invoice = receiptDetail?.invoice.toString(),
                                                                batchTable = batchData,
                                                                responseCode = genericResp?.toString(),
                                                                cardProcessedDataModal = globalCardProcessedModel)

                                                            pendingSyncTransactionViewModel.insertPendingSyncTransactionData(pendingSyncTransactionTable)
                                                            withContext(Dispatchers.Main) {
                                                                hideProgress()
                                                                errorOnSyncing(genericResp.errorMessage?:"Sync Error....")
                                                            }
                                                        }
                                                        is GenericResponse.Loading -> {
                                                            logger("Loading:- ", "in Loading $genericResp","e")
                                                            withContext(Dispatchers.Main) {
                                                                hideProgress()
                                                            }
                                                        }
                                                    }
                                                }


                                            }
                                            // end region



                                        }


                                    }
                                    ResponseCode.FAILED.value,
                                    ResponseCode.ABORTED.value -> {
                                        errorFromIngenico(
                                            txnResponse.responseCode,
                                            txnResponse.status.toString()
                                        )

                                    }
                                    ResponseCode.REVERSAL.value -> {
                                        // kushal
                                        // region

                                        AppPreference.saveLastCancelReceiptDetails(receiptDetail)

                                        val batchReversalData = BatchTableReversal(receiptDetail)

                                        lifecycleScope.launch(Dispatchers.IO) {
                                            //    appDao.insertBatchData(batchData)
                                            batchReversalData.invoice =
                                                receiptDetail?.invoice.toString()
                                            batchReversalData.transactionType =
                                                BhTransactionType.SALE.type
                                            batchReversalData.responseCode =
                                                ResponseCode.SUCCESS.value
                                            batchReversalData.roc = receiptDetail?.stan.toString()
                                            appDatabase.appDao.insertBatchReversalData(
                                                batchReversalData
                                            )
                                        }
                                    }
                                    else -> {
                                        errorFromIngenico(
                                            txnResponse?.responseCode,
                                            txnResponse?.status.toString()
                                        )
                                    }
                                }
                            }
                        }
                    )
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }
            EDashboardItem.PREAUTH -> {
                try {
                    val amt = (saleAmt.toFloat() * 100).toLong()

                    var ecrID: String

                    DeviceHelper.doPreAuthTxn(
                        PreAuthRequest(
                            amount = amt,
                            tid = tid,
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
                                        val jsonResp = Gson().toJson(receiptDetail)
                                        println(jsonResp)

                                       // AppPreference.saveLastReceiptDetails(jsonResp) // save last sale receipt11

                                        //   detailResponse.forEach { println(it) }
                                        //  uids.add(ecrID)
                                        // defaultScope.launch { onSaveUId(ecrID, handleLoadingUIdsResult) }
                                        if (receiptDetail != null) {
                                            val batchData = BatchTable(receiptDetail)

                                            // region print and save data
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                //    appDao.insertBatchData(batchData)
                                                batchData.invoice = receiptDetail.invoice.toString()
                                                batchData.transactionType =
                                                    BhTransactionType.PRE_AUTH.type
                                                appDatabase.appDao.insertBatchData(batchData)
                                                AppPreference.saveLastReceiptDetails(batchData)
                                                printingSaleData(batchData){
                                                    withContext(Dispatchers.Main) {
                                                        showProgress(getString(R.string.transaction_syncing_msg))
                                                    }

                                                    createCardProcessingModelData(receiptDetail)
                                                    val transactionISO = CreateTransactionPacket(globalCardProcessedModel).createTransactionPacket()
                                                    // sync pending transaction
                                                   // Utility().syncPendingTransaction(transactionViewModel)

                                                    when(val genericResp = transactionViewModel.serverCall(transactionISO))
                                                    {
                                                        is GenericResponse.Success -> {
                                                            logger("success:- ", "in success $genericResp","e")
                                                            withContext(Dispatchers.Main) {
                                                                hideProgress()
                                                            }
                                                        }
                                                        is GenericResponse.Error -> {
                                                            logger("error:- ", "in error $genericResp", "e")
                                                            logger("error:- ", "save transaction sync later", "e")

                                                            val pendingSyncTransactionTable = PendingSyncTransactionTable(invoice = receiptDetail?.invoice.toString(),
                                                                batchTable = batchData,
                                                                responseCode = genericResp?.toString(),
                                                                cardProcessedDataModal = globalCardProcessedModel)

                                                            pendingSyncTransactionViewModel.insertPendingSyncTransactionData(pendingSyncTransactionTable)
                                                            withContext(Dispatchers.Main) {
                                                                hideProgress()
                                                                errorOnSyncing(genericResp.errorMessage?:"Sync Error....")
                                                            }
                                                        }
                                                        is GenericResponse.Loading -> {
                                                            logger("Loading:- ", "in Loading $genericResp","e")
                                                            withContext(Dispatchers.Main) {
                                                                hideProgress()
                                                            }
                                                        }
                                                    }
                                                }


                                            }
                                            // end region


                                        }


                                    }
                                    ResponseCode.FAILED.value,
                                    ResponseCode.ABORTED.value -> {
                                        errorFromIngenico(
                                            txnResponse.responseCode,
                                            txnResponse.status.toString()
                                        )

                                    }
                                    ResponseCode.REVERSAL.value -> {
                                        // kushal
                                        // region

                                        AppPreference.saveLastCancelReceiptDetails(receiptDetail)

                                        val batchReversalData = BatchTableReversal(receiptDetail)

                                        lifecycleScope.launch(Dispatchers.IO) {
                                            //    appDao.insertBatchData(batchData)
                                            batchReversalData.invoice =
                                                receiptDetail?.invoice.toString()
                                            batchReversalData.transactionType =
                                                BhTransactionType.SALE.type
                                            batchReversalData.responseCode =
                                                ResponseCode.SUCCESS.value
                                            batchReversalData.roc = receiptDetail?.stan.toString()
                                            appDatabase.appDao.insertBatchReversalData(
                                                batchReversalData
                                            )
                                        }
                                    }
                                    else -> {
                                        errorFromIngenico(
                                            txnResponse?.responseCode,
                                            txnResponse?.status.toString()
                                        )
                                    }
                                }
                            }
                        }
                    )
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }
            EDashboardItem.PREAUTH_COMPLETE -> {
                val amt = (saleAmt.toFloat() * 100).toLong()
                var ecrID: String
                try {
                    DeviceHelper.doPreAuthCompleteTxn(
                        PreAuthCompleteRequest(
                            amount = amt,
                            tid = authCompletionData.authTid,
                            invoice =
                            addPad(
                                authCompletionData.authInvoice ?: "",
                                "0",
                                6
                            ),
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
                                        val jsonResp = Gson().toJson(receiptDetail)
                                        println(jsonResp)

                                       // AppPreference.saveLastReceiptDetails(jsonResp) // save last sale receipt11

                                        //   detailResponse.forEach { println(it) }
                                        //  uids.add(ecrID)
                                        // defaultScope.launch { onSaveUId(ecrID, handleLoadingUIdsResult) }
                                        if (receiptDetail != null) {
                                            val batchData = BatchTable(receiptDetail)
                                            // region print and save data
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                //    appDao.insertBatchData(batchData)
                                                batchData.invoice = receiptDetail.invoice.toString()
                                                batchData.transactionType =
                                                    BhTransactionType.PRE_AUTH_COMPLETE.type
                                                appDatabase.appDao.insertBatchData(batchData)
                                                AppPreference.saveLastReceiptDetails(batchData)
                                                printingSaleData(batchData){
                                                    withContext(Dispatchers.Main) {
                                                        showProgress(getString(R.string.transaction_syncing_msg))
                                                    }
                                                    createCardProcessingModelData(receiptDetail)
                                                    val transactionISO = CreateTransactionPacket(globalCardProcessedModel).createTransactionPacket()
                                                    // sync pending transaction
                                                 //   Utility().syncPendingTransaction(transactionViewModel)

                                                    when(val genericResp = transactionViewModel.serverCall(transactionISO))
                                                    {
                                                        is GenericResponse.Success -> {
                                                            logger("success:- ", "in success $genericResp","e")
                                                            withContext(Dispatchers.Main) {
                                                                hideProgress()
                                                            }
                                                        }
                                                        is GenericResponse.Error -> {
                                                            logger("error:- ", "in error $genericResp", "e")
                                                            logger("error:- ", "save transaction sync later", "e")

                                                            val pendingSyncTransactionTable = PendingSyncTransactionTable(invoice = receiptDetail?.invoice.toString(),
                                                                batchTable = batchData,
                                                                responseCode = genericResp?.toString(),
                                                                cardProcessedDataModal = globalCardProcessedModel)

                                                            pendingSyncTransactionViewModel.insertPendingSyncTransactionData(pendingSyncTransactionTable)
                                                            withContext(Dispatchers.Main) {
                                                                hideProgress()
                                                                errorOnSyncing(genericResp.errorMessage?:"Sync Error....")
                                                            }
                                                        }
                                                        is GenericResponse.Loading -> {
                                                            logger("Loading:- ", "in Loading $genericResp","e")
                                                            withContext(Dispatchers.Main) {
                                                                hideProgress()
                                                            }
                                                        }
                                                    }

                                                }
                                            }



                                        }


                                    }
                                    ResponseCode.FAILED.value,
                                    ResponseCode.ABORTED.value -> {
                                        errorFromIngenico(
                                            txnResponse.responseCode,
                                            txnResponse.status.toString()
                                        )
                                    }
                                    "03" -> {
                                        errorFromIngenico(
                                            txnResponse.responseCode,
                                            txnResponse.status.toString()
                                        )

                                    }
                                    ResponseCode.REVERSAL.value -> {
                                        // kushal
                                        // region
                                        AppPreference.saveLastCancelReceiptDetails(receiptDetail)

                                        val batchReversalData = BatchTableReversal(receiptDetail)

                                        lifecycleScope.launch(Dispatchers.IO) {
                                            //    appDao.insertBatchData(batchData)
                                            batchReversalData.invoice =
                                                receiptDetail?.invoice.toString()
                                            batchReversalData.transactionType =
                                                BhTransactionType.SALE.type
                                            batchReversalData.responseCode =
                                                ResponseCode.SUCCESS.value
                                            batchReversalData.roc = receiptDetail?.stan.toString()
                                            appDatabase.appDao.insertBatchReversalData(
                                                batchReversalData
                                            )
                                        }
                                    }
                                    else -> {
                                        errorFromIngenico(
                                            txnResponse?.responseCode,
                                            txnResponse?.status.toString()
                                        )
                                    }
                                }
                            }
                        }
                    )
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }

            else -> {

            }
        }
    }

    private suspend fun initiateNormalSale(){
        val amt = (saleAmt.toFloat() * 100).toLong()
        val cashBackAmount = (saleWithTipAmt.toFloat() * 100).toLong()
        field54Data=cashBackAmount
        Log.d(TAG, "tip amount: ${cashBackAmount}")
        var ecrID: String
        try {
            val tranUuid = UUID.randomUUID().toString().also {
                ecrID = it

            }
            val restartHandlingModel = RestartHandlingModel(tranUuid, EDashboardItem.SALE)
            restartHandlingList.add(restartHandlingModel)
            val jsonResp = Gson().toJson(restartHandlingModel)
            println(jsonResp)
            AppPreference.saveRestartDataPreference(jsonResp)
            DeviceHelper.doSaleTransaction(
                SaleRequest(
                    amount = amt,
                    tipAmount = cashBackAmount,
                    transactionType = TransactionType.SALE,
                    tid = tid,
                    transactionUuid = tranUuid
                ),
                listener = object : OnPaymentListener.Stub() {
                    override fun onCompleted(result: PaymentResult?) {
                        val txnResponse = result?.value as? TransactionResponse
                        val receiptDetail = txnResponse?.receiptDetail
                        Log.d(TAG, "Response Code: ${txnResponse?.responseCode}")
                        val jsonResp = Gson().toJson(receiptDetail)
                        println(jsonResp)
                        Log.d(TAG, "receiptDetail : $jsonResp")
                        when (txnResponse?.responseCode) {
                            ResponseCode.SUCCESS.value -> {
                              //  AppPreference.saveLastReceiptDetails(jsonResp) // save last sale receipt11

                                //   detailResponse.forEach { println(it) }
                                //  uids.add(ecrID)
                                // defaultScope.launch { onSaveUId(ecrID, handleLoadingUIdsResult) }
                                if (receiptDetail != null) {
                                    lifecycleScope.launch(Dispatchers.IO) {

                                        val tpt = runBlocking(Dispatchers.IO) {
                                            getTptDataByTid(receiptDetail.tid ?: "")
                                        }
                                        val batchData = BatchTable(receiptDetail)
                                        println(jsonResp)
                                        batchData.invoice = receiptDetail.invoice.toString()
                                        batchData.transactionType = BhTransactionType.SALE.type
                                        batchData.bonushubbatchnumber = tpt?.batchNumber ?: ""
                                        batchData.bonushubInvoice     = tpt?.invoiceNumber ?: ""
                                        batchData.bonushubStan        = tpt?.stan ?: ""

                                        appDatabase.appDao.insertBatchData(batchData)
                                        AppPreference.saveLastReceiptDetails(batchData)
                                        //To increment base Stan
                                        Utility().incrementUpdateRoc()
                                        //To increment base invoice
                                        Utility().incrementUpdateInvoice()
                                        printingSaleData(batchData){
                                            withContext(Dispatchers.Main){
                                                showProgress(getString(R.string.transaction_syncing_msg))
                                            }
                                            createCardProcessingModelData(receiptDetail)
                                            val transactionISO = CreateTransactionPacket(globalCardProcessedModel).createTransactionPacket()
                                            // sync pending transaction
                                      //      Utility().syncPendingTransaction(transactionViewModel)

                                            when(val genericResp = transactionViewModel.serverCall(transactionISO))
                                            {
                                                is GenericResponse.Success -> {
                                                    withContext(Dispatchers.Main) {
                                                        logger("success:- ", "in success $genericResp","e")
                                                        hideProgress()
                                                    goToDashBoard()
                                                    }

                                                }
                                                is GenericResponse.Error -> {
                                                    logger("error:- ", "in error $genericResp", "e")
                                                    val pendingSyncTransactionTable = PendingSyncTransactionTable(invoice = receiptDetail?.invoice.toString(),
                                                        batchTable = batchData,
                                                        responseCode = genericResp.toString(),
                                                        cardProcessedDataModal = globalCardProcessedModel)
                                                    pendingSyncTransactionViewModel.insertPendingSyncTransactionData(pendingSyncTransactionTable)
                                                    withContext(Dispatchers.Main) {
                                                        hideProgress()
                                                        errorOnSyncing(genericResp.errorMessage?:"Sync Error....")
                                                    }

                                                }
                                                is GenericResponse.Loading -> {
                                                    withContext(Dispatchers.Main) {
                                                        hideProgress()
                                                    }
                                                    logger("Loading:- ", "in Loading $genericResp","e")
                                                }
                                            }

                                        }


                                    }
                                }
                            }
                            ResponseCode.FAILED.value,
                            ResponseCode.ABORTED.value -> {
                                errorFromIngenico(
                                    txnResponse.responseCode,
                                    txnResponse.status.toString()
                                )
                            }
                            ResponseCode.REVERSAL.value -> {
                                AppPreference.saveLastCancelReceiptDetails(receiptDetail)

                                val batchReversalData = BatchTableReversal(receiptDetail)

                                lifecycleScope.launch(Dispatchers.IO) {
                                    //    appDao.insertBatchData(batchData)
                                    batchReversalData.invoice =
                                        receiptDetail?.invoice.toString()
                                    batchReversalData.transactionType =
                                        BhTransactionType.SALE.type
                                    batchReversalData.responseCode =
                                        ResponseCode.SUCCESS.value
                                    batchReversalData.roc = receiptDetail?.stan.toString()
                                    appDatabase.appDao.insertBatchReversalData(
                                        batchReversalData
                                    )
                                }
                            }

                            // end region
                            else -> {
                                errorFromIngenico(
                                    txnResponse?.responseCode,
                                    txnResponse?.status.toString()
                                )
                            }
                        }
                    }
                }
            )
        } catch (exc: Exception) {
            exc.printStackTrace()
        }


    }

    private suspend fun initiateInstaEmi(bankEMIIssuerTandCData: BankEMIIssuerTAndCDataModal
                                         ,  bankEMISchemesDataList: MutableList<BankEMITenureDataModal>){
        val intent = Intent(
            this@TransactionActivity,
            TenureSchemeActivity::class.java
        ).apply {
            globalCardProcessedModel.setTransactionAmount((saleAmt.toDouble() * 100).toLong())

            putExtra("cardProcessedData", globalCardProcessedModel)
            putExtra("transactionType", globalCardProcessedModel.getTransType())

            putParcelableArrayListExtra("emiSchemeOfferDataList", bankEMISchemesDataList as java.util.ArrayList<out Parcelable>)
            putExtra("emiIssuerTAndCDataList", bankEMIIssuerTandCData)
        }
        startActivityForResult(intent,globalCardProcessedModel.getTransType())

    }

    private fun showEMISaleDialog(  bankEMIIssuerTAndC: BankEMIIssuerTAndCDataModal
                                   ,  bankEMISchemesDataList: MutableList<BankEMITenureDataModal>) {
        val dialog = Dialog(this)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.show_emi_sale_dialog_view)

        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        val window = dialog.window
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )

        dialog.findViewById<Button>(R.id.cardsaleButton).setOnClickListener {
            dialog.dismiss()
            //   cardProcessedDataModal.setTransType(BhTransactionType.SALE.type)
            lifecycleScope.launch(Dispatchers.IO) {
                initiateNormalSale()
            }

        }

        dialog.findViewById<Button>(R.id.cardemiButton).setOnClickListener {
            dialog.dismiss()
            //  cardProcessedDataModal.setTransType(BhTransactionType.EMI_SALE.type)
            //   cardProcessedDataModal.setEmiType(1)  //1 for insta emi
            /* binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.emi_catalog_icon)
             binding?.subHeaderView?.subHeaderText?.text = BhTransactionType.EMI_SALE.txnTitle*/
            lifecycleScope.launch(Dispatchers.IO) {
                initiateInstaEmi(bankEMIIssuerTAndC,bankEMISchemesDataList)
            }


        }

        dialog.findViewById<ImageView>(R.id.closeDialog).setOnClickListener {
            dialog.dismiss()
            finish()
            startActivity(Intent(this, NavigationActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
        try {
            if (!dialog.isShowing && !(this as Activity).isFinishing) {
                dialog.show()
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }

        } catch (ex: WindowManager.BadTokenException) {
            ex.printStackTrace()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    suspend fun printingSaleData(batchTable: BatchTable, cb:suspend (Boolean) ->Unit) {
        val receiptDetail = batchTable.receiptData
        withContext(Dispatchers.Main) {
            showProgress(getString(R.string.printing))
            var printsts = false
            if (receiptDetail != null) {
                PrintUtil(this@TransactionActivity as BaseActivityNew).startPrinting(
                    batchTable,
                    EPrintCopyType.MERCHANT,
                    this@TransactionActivity as BaseActivityNew
                ) { printCB, printingFail ->

                    (this@TransactionActivity as BaseActivityNew).hideProgress()
                    if (printCB) {
                        printsts = printCB
                        lifecycleScope.launch(Dispatchers.Main) {
                            showMerchantAlertBox(batchTable, cb)
                        }

                    } else {
                        ToastUtils.showToast(
                            this@TransactionActivity as BaseActivityNew,
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
            (this@TransactionActivity as BaseActivityNew).alertBoxWithAction(
                getString(R.string.print_customer_copy),
                getString(R.string.print_customer_copy),
                true, getString(R.string.positive_button_yes), { status ->
                    showProgress(getString(R.string.printing))
                    PrintUtil(this@TransactionActivity as BaseActivityNew).startPrinting(
                        batchTable,
                        EPrintCopyType.CUSTOMER,
                        this@TransactionActivity as BaseActivityNew
                    ) { printCB, printingFail ->
                        (this@TransactionActivity as BaseActivityNew).hideProgress()
                        if (printCB) {
                           lifecycleScope.launch(Dispatchers.IO) {

                               cb(printCB)
                           }
                            (this@TransactionActivity as BaseActivityNew).hideProgress()

//                            val intent = Intent(this@TransactionActivity, NavigationActivity::class.java)
//                            startActivity(intent)
                        }

                    }
                }, {
                    lifecycleScope.launch(Dispatchers.IO) {


                        cb(true)
                    }
                    (this@TransactionActivity as BaseActivityNew).hideProgress()
//                    val intent = Intent(this@TransactionActivity, NavigationActivity::class.java)
//                    startActivity(intent)
                })
        }
    }

    enum class DetectCardType(val cardType: Int, val cardTypeName: String = "") {
        EMV_Fallback_TYPE(-1, "Chip Fallabck"),
        CARD_ERROR_TYPE(0),
        MAG_CARD_TYPE(1, "Mag"),
        EMV_CARD_TYPE(2, "Chip"),
        CONTACT_LESS_CARD_TYPE(3, "CTLS"),
        CONTACT_LESS_CARD_WITH_MAG_TYPE(4, "CTLS"),
        MANUAL_ENTRY_TYPE(5, "MAN")
    }

    enum class CardErrorCode(var errorCode: Int) {
        EMV_FALLBACK_ERROR_CODE(40961),
        CTLS_ERROR_CODE(6)
    }

    enum class EFallbackCode(var fallBackCode: Int) {
        Swipe_fallback(111),
        EMV_fallback(8),
        NO_fallback(0),
        EMV_fallbackNew(12),
        CTLS_fallback(333)
    }

    fun errorFromIngenico(responseCode: String?, msg: String?) {
        lifecycleScope.launch(Dispatchers.Main) {
            getInfoDialog(responseCode ?: "", msg ?: "Unknown Error") {
                goToDashBoard()
            }
        }


    }

   suspend fun errorOnSyncing(msg:String){
        withContext(Dispatchers.Main) {
            alertBoxWithAction(
                getString(R.string.no_receipt),
                msg,
                false,
                getString(R.string.positive_button_ok),
                {
                    finish()
                    goToDashBoard()
                },
                {})
        }


    }

    private fun goToDashBoard() {
        startActivity(Intent(this@TransactionActivity, NavigationActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    fun createCardProcessingModelData(receiptDetail: ReceiptDetail) {
        logger("",""+receiptDetail)
        when(globalCardProcessedModel.getTransType()){
            BhTransactionType.SALE.type , BhTransactionType.TEST_EMI.type,
            BhTransactionType.BRAND_EMI.type, BhTransactionType.EMI_SALE.type,
            BhTransactionType.PRE_AUTH.type->{
                globalCardProcessedModel.setProcessingCode(ProcessingCode.SALE.code)
            }
            BhTransactionType.CASH_AT_POS.type->{
                globalCardProcessedModel.setProcessingCode(ProcessingCode.CASH_AT_POS.code)
            }
            BhTransactionType.SALE_WITH_CASH.type->{
                globalCardProcessedModel.setProcessingCode(ProcessingCode.SALE_WITH_CASH.code)
            }
            BhTransactionType.VOID.type->{
                globalCardProcessedModel.setProcessingCode(ProcessingCode.VOID.code)
            }
            BhTransactionType.REFUND.type->{
                globalCardProcessedModel.setProcessingCode(ProcessingCode.REFUND.code)
            }
            BhTransactionType.VOID_PREAUTH.type->{
                globalCardProcessedModel.setProcessingCode(ProcessingCode.VOID_PREAUTH.code)
            }
            BhTransactionType.PRE_AUTH_COMPLETE.type->{
            globalCardProcessedModel.setProcessingCode(ProcessingCode.PRE_SALE_COMPLETE.code)
            }
        }

        receiptDetail.txnAmount?.let { globalCardProcessedModel.setTransactionAmount(it.toLong()) }
        receiptDetail.txnOtherAmount?.let { globalCardProcessedModel.setOtherAmount(it.toLong()) }
        globalCardProcessedModel.setMobileBillExtraData(Pair(mobileNumber, billNumber))
        receiptDetail.stan?.let { globalCardProcessedModel.setAuthRoc(it) }
        //globalCardProcessedModel.setCardMode("0553- emv with pin")
        logger("mode22 ->",CardMode(receiptDetail.entryMode?:"",receiptDetail.isVerifyPin?:false),"e")
        globalCardProcessedModel.setCardMode(CardMode(receiptDetail.entryMode?:"",receiptDetail.isVerifyPin?:false))
        globalCardProcessedModel.setRrn(receiptDetail.rrn)
        receiptDetail.authCode?.let { globalCardProcessedModel.setAuthCode(it) }

        globalCardProcessedModel.setTid(receiptDetail.tid)

        globalCardProcessedModel.setMid(receiptDetail.mid)
        globalCardProcessedModel.setBatch(receiptDetail.batchNumber)
        globalCardProcessedModel.setInvoice(receiptDetail.invoice)
        val date = receiptDetail.dateTime
        val parts = date?.split(" ")
        globalCardProcessedModel.setDate(parts!![0])
        globalCardProcessedModel.setTime(parts[1])
        globalCardProcessedModel.setTimeStamp(receiptDetail.dateTime!!)
        globalCardProcessedModel.setPosEntryMode("0553")
        field54Data?.let { globalCardProcessedModel.setOtherAmount(it) }
        receiptDetail.maskedPan?.let { globalCardProcessedModel.setPanNumberData(it) }
    }

    fun CardMode(entryMode:String, isPinVerify:Boolean):String
    {
        logger("entryMode",""+entryMode,"e")
        when(entryMode){

            CardEntryMode.EMV_WITH_PIN._name -> {
                if(isPinVerify){
                    logger("entryMode","EMV_WITH_PIN","e")
                    return CardEntryMode.EMV_WITH_PIN._value
                }else {
                    logger("entryMode","EMV_NO_PIN","e")
                    return CardEntryMode.EMV_NO_PIN._value
                }

            }

            CardEntryMode.EMV_FALLBACK_SWIPE_WITH_PIN._name -> {
                if(isPinVerify){
                    logger("entryMode","EMV_FALLBACK_SWIPE_WITH_PIN","e")
                    return CardEntryMode.EMV_FALLBACK_SWIPE_WITH_PIN._value
                }else {
                    logger("entryMode","EMV_FALLBACK_SWIPE_NO_PIN","e")
                    return CardEntryMode.EMV_FALLBACK_SWIPE_NO_PIN._value
                }

            }

            CardEntryMode.SWIPE_WITH_PIN._name -> {
                if(isPinVerify){
                    logger("entryMode","SWIPE_WITH_PIN","e")
                    return CardEntryMode.SWIPE_WITH_PIN._value
                }else {
                    logger("entryMode","SWIPE_NO_PIN","e")
                    return CardEntryMode.SWIPE_NO_PIN._value
                }

            }

            CardEntryMode.CTLS_SWIPE_NO_PIN._name -> {
                if(isPinVerify){
                    logger("entryMode","CTLS_SWIPE_WITH_PIN","e")
                    return CardEntryMode.CTLS_SWIPE_WITH_PIN._value
                }else {
                    logger("entryMode","CTLS_SWIPE_NO_PIN","e")
                    return CardEntryMode.CTLS_SWIPE_NO_PIN._value
                }

            }

            CardEntryMode.CTLS_EMV_NO_PIN._name -> {
                if(isPinVerify){
                    logger("entryMode","CTLS_EMV_WITH_PIN","e")
                    return CardEntryMode.CTLS_EMV_WITH_PIN._value
                }else {
                    logger("entryMode","CTLS_EMV_NO_PIN","e")
                    return CardEntryMode.CTLS_EMV_NO_PIN._value
                }

            }
            else -> {
                return ""
            }
        }

    }


}