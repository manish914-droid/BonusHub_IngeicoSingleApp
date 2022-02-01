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
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bonushub.crdb.HDFCApplication
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.ActivityEmvBinding
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.db.AppDatabase
import com.bonushub.crdb.di.DBModule
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
import com.bonushub.crdb.utils.Field48ResponseTimestamp.getBatchDataByInvoice
import com.bonushub.crdb.utils.Field48ResponseTimestamp.getTptData
import com.bonushub.crdb.utils.ingenico.RawStripe
import com.bonushub.crdb.utils.printerUtils.PrintUtil
import com.bonushub.crdb.view.base.BaseActivityNew
import com.bonushub.crdb.view.fragments.AuthCompletionData
import com.bonushub.crdb.viewmodel.PendingSyncTransactionViewModel
import com.bonushub.crdb.viewmodel.SearchViewModel
import com.bonushub.crdb.viewmodel.TenureSchemeViewModel
import com.bonushub.crdb.viewmodel.TransactionViewModel
import com.bonushub.crdb.viewmodel.viewModelFactory.TenureSchemeActivityVMFactory
import com.google.gson.Gson
import com.ingenico.hdfcpayment.listener.OnPaymentListener
import com.ingenico.hdfcpayment.model.ReceiptDetail
import com.ingenico.hdfcpayment.model.Track1
import com.ingenico.hdfcpayment.model.Track2
import com.ingenico.hdfcpayment.request.*
import com.ingenico.hdfcpayment.response.PaymentResult
import com.ingenico.hdfcpayment.response.TransactionResponse
import com.ingenico.hdfcpayment.type.CardCaptureType
import com.ingenico.hdfcpayment.type.ResponseCode
import com.ingenico.hdfcpayment.type.TransactionType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class TransactionActivity : BaseActivityNew() {

    @Inject
    lateinit var appDao: AppDao

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
    private var field54Data: Long? = null
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

        if (transactionTypeEDashboardItem == EDashboardItem.BRAND_EMI || transactionTypeEDashboardItem == EDashboardItem.BANK_EMI || transactionTypeEDashboardItem == EDashboardItem.TEST_EMI) {
            emvBinding?.cardDetectImg?.visibility = View.VISIBLE
            emvBinding?.tvInsertCard?.visibility = View.VISIBLE
            emvBinding?.subHeaderView?.backImageButton?.visibility = View.VISIBLE
        } else {
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
        if(transactionType==BhTransactionType.SALE_WITH_CASH.type){
            val amt= saleAmt.toFloat() + cashBackAmt.toFloat()
          val frtAmt=  "%.2f".format(amt)
            emvBinding?.baseAmtTv?.text =frtAmt
        }else{
            val frtAmt=  "%.2f".format(saleAmt.toFloat())
            emvBinding?.baseAmtTv?.text = frtAmt
        }

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
                    globalCardProcessedModel = cardProcessDataModel
                    globalCardProcessedModel.setTransactionAmount((saleAmt.toDouble() * 100).toLong())
                    when (globalCardProcessedModel.getReadCardType()) {
                        DetectCardType.EMV_CARD_TYPE, DetectCardType.MAG_CARD_TYPE -> {

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
                                                    "^${/*cardBinValue.substring(0, 8)*/""}^${globalCardProcessedModel.getTransactionAmount()}"
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

                                BhTransactionType.EMI_SALE.type, BhTransactionType.TEST_EMI.type -> {
                                    val intent = Intent(
                                        this@TransactionActivity,
                                        TenureSchemeActivity::class.java
                                    ).apply {

                                        putExtra("testEmiOption", testEmiOption)
                                        putExtra("cardProcessedData", globalCardProcessedModel)
                                        putExtra(
                                            "transactionType",
                                            globalCardProcessedModel.getTransType()
                                        )
                                    }
                                    startActivityForResult(
                                        intent,
                                        globalCardProcessedModel.getTransType()
                                    )

                                }

                            }

                        }
                        DetectCardType.CONTACT_LESS_CARD_TYPE -> {
                            //  Toast.makeText(this@TransactionActivity,"Contactless mode detected",Toast.LENGTH_LONG).show()
                        }

                        else -> {

                        }
                    }

                })
        }
    }

    private fun continueTenureProcess() {
        val f57 = "$bankEMIRequestCode^0^1^0^^${
            globalCardProcessedModel.getPanNumberData()?.substring(0, 8)
        }^${globalCardProcessedModel.getTransactionAmount()}"
        tenureSchemeViewModel = ViewModelProvider(
            this@TransactionActivity, TenureSchemeActivityVMFactory(
                serverRepository,
                globalCardProcessedModel.getPanNumberData() ?: "",
                f57
            )
        ).get(TenureSchemeViewModel::class.java)
        tenureSchemeViewModel.emiTenureLiveData.observe(
            this
        ) {
            hideProgress()
            when (val genericResp = it) {
                is GenericResponse.Success -> {
                    println(Gson().toJson(genericResp.data))
                    val resp = genericResp.data as TenuresWithIssuerTncs
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
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // conversion of sale to Bank emi
        var reqCode = requestCode
        if (requestCode == BhTransactionType.SALE.type) {
            reqCode = BhTransactionType.EMI_SALE.type
            globalCardProcessedModel.setTransType(BhTransactionType.EMI_SALE.type)
        }
        if (reqCode == BhTransactionType.BRAND_EMI.type || reqCode == BhTransactionType.EMI_SALE.type || reqCode == BhTransactionType.TEST_EMI.type) {
            emvBinding?.subHeaderView?.subHeaderText?.text =
                getTransactionTypeName(globalCardProcessedModel.getTransType())
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
                    var amt = 0L
                    if (reqCode == BhTransactionType.TEST_EMI.type) {
                        globalCardProcessedModel.testEmiOption = testEmiOption
                        emiTenureData?.txnTID = getTidForTestTxn(testEmiOption)
                        amt = 100L
                    } else {
                        amt = (saleAmt.toFloat() * 100).toLong()
                    }
                    var track1: Track1? = null
                    var track2: Track2? = null
                    val cardCaptureType: CardCaptureType
                    if (globalCardProcessedModel.getReadCardType() == DetectCardType.MAG_CARD_TYPE) {
                        val tracksData = RawStripe(
                            globalCardProcessedModel.getTrack1Data(),
                            globalCardProcessedModel.getTrack2Data()
                        )
                        track1 = tracksData.track1
                        track2 = tracksData.track2
                        cardCaptureType =
                            if (globalCardProcessedModel.getFallbackType() == com.bonushub.crdb.utils.EFallbackCode.EMV_fallback.fallBackCode) {
                                //  swipe from emv fall back
                                CardCaptureType.FALLBACK_EMV_NO_CAPTURING
                            } else {
                                //  only swipe case
                                CardCaptureType.EMV_NO_CAPTURING
                            }
                    } else {
                        //  insert case
                        cardCaptureType = CardCaptureType.EMV_NO_CAPTURING
                    }

                    // region save data for PFR
                    val batchData = BatchTable(null)
                    batchData.emiIssuerDataModel = emiIssuerData
                    batchData.invoice = ""
                    if (requestCode == BhTransactionType.BRAND_EMI.type) {
                        batchData.emiBrandData = brandDataMaster
                        batchData.emiSubCategoryData = brandEmiSubCatData
                        batchData.emiCategoryData = brandEmiCatData
                        batchData.emiProductData = brandEmiProductData
                    }
                    batchData.imeiOrSerialNum = imeiOrSerialNum
                    batchData.mobileNumber = mobileNumber
                    batchData.billNumber = billNumber
                    batchData.emiTenureDataModel = emiTenureData
                    batchData.transactionType = globalCardProcessedModel.getTransType()

                    val tranUuid = UUID.randomUUID().toString()
                    var restartHandlingModel: RestartHandlingModel? = null
                    when (reqCode) {

                        BhTransactionType.BRAND_EMI.type -> {
                            restartHandlingModel =
                                RestartHandlingModel(tranUuid, EDashboardItem.BANK_EMI, batchData)
                        }


                        BhTransactionType.EMI_SALE.type -> {
                            restartHandlingModel =
                                RestartHandlingModel(tranUuid, EDashboardItem.BRAND_EMI, batchData)
                        }


                        BhTransactionType.TEST_EMI.type -> {
                            restartHandlingModel =
                                RestartHandlingModel(tranUuid, EDashboardItem.TEST_EMI, batchData)
                        }
                    }
                    // val restartHandlingModel = RestartHandlingModel(tranUuid, requestCode, batchData)
                    restartHandlingList.add(restartHandlingModel!!)
                    val jsonResp = Gson().toJson(restartHandlingModel)
                    println(jsonResp)
                    AppPreference.saveRestartDataPreference(jsonResp)

                    // end region
                    DeviceHelper.doEMITxn(
                        EMISaleRequest(
                            amount = amt,
                            tipAmount = 0L,
                            emiTxnName = getTransactionTypeName(globalCardProcessedModel.getTransType()),
                            tid = emiTenureData?.txnTID,
                            cardCaptureType = cardCaptureType,
                            track1 = track1,
                            track2 = track2,
                            transactionUuid = tranUuid,
                        ),
                        listener = object : OnPaymentListener.Stub() {
                            override fun onCompleted(result: PaymentResult?) {
                                val txnResponse = result?.value as? TransactionResponse
                                val receiptDetail = txnResponse?.receiptDetail
                                receiptDetail?.txnName =
                                    getTransactionTypeName(globalCardProcessedModel.getTransType())
                                Log.d(TAG, "Response Code: ${txnResponse?.responseCode}")
                                var isUnblockingNeeded = false
                                when (txnResponse?.responseCode) {
                                    ResponseCode.SUCCESS.value -> {
                                        val jsonResp = Gson().toJson(receiptDetail)
                                        println(jsonResp)
                                        // AppPreference.saveLastReceiptDetails(jsonResp) // save last sale receipt11
                                        if (receiptDetail != null) {
                                            val batchData = BatchTable(receiptDetail)
                                            val tpt = runBlocking(Dispatchers.IO) {
                                                getTptData()
                                            }
                                            println("Selected tid id " + tpt?.terminalId)
                                            println("Batch number in emi " + tpt?.batchNumber)
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                batchData.emiIssuerDataModel = emiIssuerData
                                                batchData.invoice = receiptDetail.invoice.toString()
                                                if (requestCode == BhTransactionType.BRAND_EMI.type) {
                                                    batchData.emiBrandData = brandDataMaster
                                                    batchData.emiSubCategoryData =
                                                        brandEmiSubCatData
                                                    batchData.emiCategoryData = brandEmiCatData
                                                    batchData.emiProductData = brandEmiProductData
                                                }
                                                batchData.imeiOrSerialNum = imeiOrSerialNum
                                                batchData.mobileNumber = mobileNumber
                                                batchData.billNumber = billNumber
                                                batchData.emiTenureDataModel = emiTenureData
                                                batchData.transactionType =
                                                    globalCardProcessedModel.getTransType()
                                                //To assign bonushub batchnumber,bonushub invoice,bonuhub stan
                                                batchData.bonushubbatchnumber =
                                                    tpt?.batchNumber ?: ""
                                                batchData.bonushubInvoice = tpt?.invoiceNumber ?: ""
                                                batchData.bonushubStan = tpt?.stan ?: ""

                                                createCardProcessingModelData(receiptDetail)

                                                // because we did not save pan num in plain in batchTable
                                                cardProcessedDataModal.getPanNumberData()?.let {
                                                    globalCardProcessedModel.setPanNumberData(
                                                        it
                                                    )
                                                }
                                                // cardProcessedDataModal.getPanNumberData() --> Containing plain pan
                                                // globalCardProcessedModel.getPanNumberData() --> Containing masked pan
var f57Data=""

                                                when(reqCode){

    BhTransactionType.TEST_EMI.type->{
        batchData.field57EncryptedData= globalCardProcessedModel.getPanNumberData()
            ?.let {
                batchData.receiptData?.let { it1 ->
                    getEncryptedDataForSyncing(
                        it,
                        it1
                    )
                }
            }.toString()

        batchData.field58EmiData=createField58ForTestEmi(globalCardProcessedModel)

    }
    BhTransactionType.EMI_SALE.type->{
        batchData.field57EncryptedData= cardProcessedDataModal.getPanNumberData()
            ?.let {
                batchData.receiptData?.let { it1 ->
                    getEncryptedDataForSyncing(
                        it,
                        it1
                    )
                }
            }.toString()
        batchData.field58EmiData=createField58ForBankEmi(globalCardProcessedModel,batchData)

    }


    BhTransactionType.BRAND_EMI.type->{

        batchData.field57EncryptedData=  cardProcessedDataModal.getPanNumberData()
            ?.let {
                batchData.receiptData?.let { it1 ->
                    getEncryptedDataForSyncing(
                        it,
                        it1
                    )
                }
            }.toString()
        batchData.field58EmiData=createField58ForBrandEmi(globalCardProcessedModel,batchData)
    }


}
             /*                                   if (reqCode == BhTransactionType.TEST_EMI.type) {
                                                    val data1 =
                                                        globalCardProcessedModel.getPanNumberData()
                                                            ?.let {
                                                                batchData.receiptData?.let { it1 ->
                                                                    getEncryptedDataForSyncing(
                                                                        it,
                                                                        it1
                                                                    )
                                                                }
                                                            }

                                                    if (data1 != null) {
                                                        batchData.field57EncryptedData = data1
                                                    }
                                                } else {
                                                    val data1 =
                                                        cardProcessedDataModal.getPanNumberData()
                                                            ?.let {
                                                                batchData.receiptData?.let { it1 ->
                                                                    getEncryptedDataForSyncing(
                                                                        it,
                                                                        it1
                                                                    )
                                                                }
                                                            }

                                                    if (data1 != null) {
                                                        batchData.field57EncryptedData = data1
                                                    }
                                                }*/




                                                appDatabase.appDao.insertBatchData(batchData)

                                                AppPreference.saveLastReceiptDetails(batchData)
                                                AppPreference.clearRestartDataPreference()

                                                //To increment base Stan
                                                Utility().incrementUpdateRoc()
                                                //To increment base invoice
                                                Utility().incrementUpdateInvoice()

                                                printingSaleData(batchData) {
                                                    // region sync transaction
                                                    withContext(Dispatchers.Main) {
                                                        showProgress(getString(R.string.transaction_syncing_msg))
                                                    }
                                                    val transactionISO = CreateTransactionPacket(
                                                        appDao,
                                                        globalCardProcessedModel,
                                                        batchData
                                                    ).createTransactionPacket()
                                                    //   sync pending transaction
                                                    Utility().syncPendingTransaction(
                                                        transactionViewModel
                                                    ) {}
                                                    when (val genericResp =
                                                        transactionViewModel.serverCall(
                                                            transactionISO
                                                        )) {
                                                        is GenericResponse.Success -> {

                                                            withContext(Dispatchers.Main) {
                                                                logger(
                                                                    "success:- ",
                                                                    "in success $genericResp",
                                                                    "e"
                                                                )
                                                                hideProgress()
                                                                goToDashBoard()
                                                            }

                                                        }
                                                        is GenericResponse.Error -> {
                                                            logger(
                                                                "error:- ",
                                                                "in error ${genericResp.errorMessage}",
                                                                "e"
                                                            )
                                                            logger(
                                                                "error:- ",
                                                                "save transaction sync later",
                                                                "e"
                                                            )
                                                            val pendingSyncTransactionTable =
                                                                PendingSyncTransactionTable(
                                                                    invoice = receiptDetail.invoice.toString(),
                                                                    batchTable = batchData,
                                                                    responseCode = genericResp.toString(),
                                                                    cardProcessedDataModal = globalCardProcessedModel
                                                                )
                                                            pendingSyncTransactionViewModel.insertPendingSyncTransactionData(
                                                                pendingSyncTransactionTable
                                                            )
                                                            withContext(Dispatchers.Main) {
                                                                hideProgress()
                                                                errorOnSyncing(
                                                                    genericResp.errorMessage
                                                                        ?: "Sync Error...."
                                                                )
                                                            }
                                                        }
                                                        is GenericResponse.Loading -> {
                                                            logger(
                                                                "Loading:- ",
                                                                "in Loading $genericResp",
                                                                "e"
                                                            )
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
                                    ResponseCode.FAILED.value -> {
                                        AppPreference.clearRestartDataPreference()
                                        errorFromIngenico(
                                            txnResponse.responseCode,
                                            txnResponse.status.toString()
                                        )
                                    }
                                    ResponseCode.ABORTED.value -> {
                                        isUnblockingNeeded = true
                                        AppPreference.clearRestartDataPreference()
                                        errorFromIngenico(
                                            txnResponse.responseCode,
                                            txnResponse.status.toString()
                                        )
                                    }
                                    ResponseCode.REVERSAL.value -> {
                                        AppPreference.clearRestartDataPreference()

                                        isUnblockingNeeded = true
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
                                        errorFromIngenico(
                                            txnResponse.responseCode,
                                            txnResponse.status.toString()
                                        )
                                    }
                                    else -> {
                                        isUnblockingNeeded = true
                                        errorFromIngenico(
                                            txnResponse?.responseCode,
                                            txnResponse?.status.toString()
                                        )
                                    }
                                }

                                if (emiTenureData != null && isUnblockingNeeded) {
                                    if (emiIssuerData != null) {
                                        txnResponse?.responseCode?.let {
                                            txnResponse.status.toString().let { it1 ->
                                                unBlockingImeiSerialNum(
                                                    emiTenureData, emiIssuerData,
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

    private fun unBlockingImeiSerialNum(
        bankEmiTenureData: BankEMITenureDataModal,
        schemeData: BankEMIIssuerTAndCDataModal,
        txnRespCode: String,
        txnResponseMsg: String
    ) {
        if (imeiOrSerialNum.isNotBlank()) {
            var isBlockUnblockSuccess = Pair(false, "")

            //   "Request Type^Skip Record Count^Brand Id^ProductID^Product serial^Bin Value^Transaction Amt^Issuer Id^Mobile No^EMI Scheme^Tenure^txn response code^txn response msg"

            val field57 =
                "13^0^${brandDataMaster.brandID}^${brandEmiProductData.productID}^${imeiOrSerialNum}^${/*globalCardProcessedModel.getEncryptedPan()*/""}^${bankEmiTenureData.totalEmiPay}^${schemeData.issuerID}^${mobileNumber}^${schemeData.emiSchemeID}^${bankEmiTenureData.tenure}^${txnRespCode}^${txnResponseMsg}"
            lifecycleScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    showProgress("Unblocking Serial/IMEI")

                }
                isBlockUnblockSuccess = serverRepository.blockUnblockSerialNum(field57)
                hideProgress()
                if (isBlockUnblockSuccess.first) {
                    errorFromIngenico(
                        txnRespCode,
                        txnResponseMsg
                    )

                } else {
                    withContext(Dispatchers.Main) {
                        showToast(isBlockUnblockSuccess.second)
                    }
                }

            }

        } else {
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
            EDashboardItem.BRAND_EMI, EDashboardItem.BANK_EMI, EDashboardItem.TEST_EMI -> {
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
                    withContext(Dispatchers.IO) { getTptData() }
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
                if (tpt.bankEmi != "1") {
                    hasInstaEmi = false
                }
                // Condition executes, If insta EMI is Available on card
                if ((saleAmt.toFloat() * 100).toLong() >= limitAmt && hasInstaEmi) {
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
                field54Data = amt
                try {
                    val tranUuid = UUID.randomUUID().toString().also {
                        ecrID = it

                    }
                    val restartHandlingModel =
                        RestartHandlingModel(tranUuid, EDashboardItem.CASH_ADVANCE)
                    restartHandlingList.add(restartHandlingModel)
                    val jsonResp = Gson().toJson(restartHandlingModel)
                    println(jsonResp)
                    AppPreference.saveRestartDataPreference(jsonResp)

                    DeviceHelper.doCashAdvanceTxn(
                        CashOnlyRequest(
                            cashAmount = amt,
                            tid = tid,
                            transactionUuid = tranUuid
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

                                            //To get tpt data acccording to tid
                                            val tpt = runBlocking(Dispatchers.IO) {
                                                getTptData()
                                            }

                                            val batchData = BatchTable(receiptDetail)

                                            // region print and save data
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                //    appDao.insertBatchData(batchData)
                                                batchData.invoice = receiptDetail.invoice.toString()
                                                batchData.transactionType =
                                                    BhTransactionType.CASH_AT_POS.type
                                                batchData.bonushubbatchnumber =
                                                    tpt?.batchNumber ?: ""
                                                batchData.bonushubInvoice = tpt?.invoiceNumber ?: ""
                                                batchData.bonushubStan = tpt?.stan ?: ""

                                                createCardProcessingModelData(receiptDetail)
                                                val data =
                                                    globalCardProcessedModel.getPanNumberData()
                                                        ?.let {
                                                            batchData.receiptData?.let { it1 ->
                                                                getEncryptedDataForSyncing(
                                                                    it,
                                                                    it1
                                                                )
                                                            }
                                                        }
                                                if (data != null) {
                                                    batchData.field57EncryptedData = data
                                                }

                                                appDatabase.appDao.insertBatchData(batchData)
                                                AppPreference.saveLastReceiptDetails(batchData)
                                                AppPreference.clearRestartDataPreference()

                                                //To increment base Stan
                                                Utility().incrementUpdateRoc()
                                                //To increment base invoice
                                                Utility().incrementUpdateInvoice()

                                                printingSaleData(batchData) {
                                                    // region sync transaction
                                                    withContext(Dispatchers.Main) {
                                                        showProgress(getString(R.string.transaction_syncing_msg))
                                                    }

                                                    val transactionISO = CreateTransactionPacket(
                                                        appDao,
                                                        globalCardProcessedModel,
                                                        batchData
                                                    ).createTransactionPacket()

                                                    // sync pending transaction
                                                    Utility().syncPendingTransaction(
                                                        transactionViewModel
                                                    ) {}

                                                    when (val genericResp =
                                                        transactionViewModel.serverCall(
                                                            transactionISO
                                                        )) {
                                                        is GenericResponse.Success -> {
                                                            withContext(Dispatchers.Main) {
                                                                logger(
                                                                    "success:- ",
                                                                    "in success $genericResp",
                                                                    "e"
                                                                )
                                                                hideProgress()
                                                                goToDashBoard()
                                                            }
                                                        }
                                                        is GenericResponse.Error -> {
                                                            logger(
                                                                "error:- ",
                                                                "in error $genericResp",
                                                                "e"
                                                            )
                                                            logger(
                                                                "error:- ",
                                                                "save transaction sync later",
                                                                "e"
                                                            )

                                                            val pendingSyncTransactionTable =
                                                                PendingSyncTransactionTable(
                                                                    invoice = receiptDetail.invoice.toString(),
                                                                    batchTable = batchData,
                                                                    responseCode = genericResp.toString(),
                                                                    cardProcessedDataModal = globalCardProcessedModel
                                                                )

                                                            pendingSyncTransactionViewModel.insertPendingSyncTransactionData(
                                                                pendingSyncTransactionTable
                                                            )
                                                            withContext(Dispatchers.Main) {
                                                                hideProgress()
                                                                errorOnSyncing(
                                                                    genericResp.errorMessage
                                                                        ?: "Sync Error...."
                                                                )
                                                            }
                                                        }
                                                        is GenericResponse.Loading -> {
                                                            logger(
                                                                "Loading:- ",
                                                                "in Loading $genericResp",
                                                                "e"
                                                            )
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

                                    ResponseCode.FAILED.value -> {
                                        AppPreference.clearRestartDataPreference()
                                        errorFromIngenico(
                                            txnResponse.responseCode,
                                            txnResponse.status.toString()
                                        )
                                    }

                                    ResponseCode.ABORTED.value -> {
                                        AppPreference.clearRestartDataPreference()
                                        errorFromIngenico(
                                            txnResponse.responseCode,
                                            txnResponse.status.toString()
                                        )
                                    }
                                    ResponseCode.REVERSAL.value -> {
                                        AppPreference.clearRestartDataPreference()
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
                                        errorFromIngenico(
                                            txnResponse.responseCode,
                                            txnResponse.status.toString()
                                        )
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
                    field54Data = cashBackAmount
                    var ecrID: String

                    val tranUuid = UUID.randomUUID().toString().also {
                        ecrID = it
                    }
                    val restartHandlingModel =
                        RestartHandlingModel(tranUuid, EDashboardItem.SALE_WITH_CASH)
                    restartHandlingList.add(restartHandlingModel)
                    val jsonResp = Gson().toJson(restartHandlingModel)
                    println(jsonResp)
                    AppPreference.saveRestartDataPreference(jsonResp)

                    DeviceHelper.doSaleWithCashTxn(
                        SaleCashBackRequest(
                            amount = amt,
                            cashAmount = cashBackAmount,
                            tid = tid,
                            transactionUuid = tranUuid
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


                                            //To get tpt data acccording to tid
                                            val tpt = runBlocking(Dispatchers.IO) {
                                                getTptData()
                                            }

                                            // region print and save data
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                batchData.invoice = receiptDetail.invoice.toString()
                                                batchData.transactionType =
                                                    BhTransactionType.SALE_WITH_CASH.type
                                                batchData.bonushubbatchnumber =
                                                    tpt?.batchNumber ?: ""
                                                batchData.bonushubInvoice = tpt?.invoiceNumber ?: ""
                                                batchData.bonushubStan = tpt?.stan ?: ""
                                                val txnAmt= batchData.receiptData?.txnOtherAmount?.toLong()
                                                    ?.let {
                                                        batchData.receiptData?.txnAmount?.toLong()
                                                            ?.plus(it)
                                                    }
                                                batchData.receiptData?.txnAmount=txnAmt.toString()
                                                createCardProcessingModelData(receiptDetail)
                                                val data =
                                                    globalCardProcessedModel.getPanNumberData()
                                                        ?.let {
                                                            batchData.receiptData?.let { it1 ->
                                                                getEncryptedDataForSyncing(
                                                                    it,
                                                                    it1
                                                                )
                                                            }
                                                        }
                                                if (data != null) {
                                                    batchData.field57EncryptedData = data
                                                }

                                                appDatabase.appDao.insertBatchData(batchData)
                                                AppPreference.saveLastReceiptDetails(batchData)
                                              AppPreference.clearRestartDataPreference()

                                                //To increment base Stan
                                                Utility().incrementUpdateRoc()
                                                //To increment base invoice
                                                Utility().incrementUpdateInvoice()

                                                printingSaleData(batchData) {
                                                    withContext(Dispatchers.Main) {
                                                        showProgress(getString(R.string.transaction_syncing_msg))
                                                    }

                                                    val transactionISO = CreateTransactionPacket(
                                                        appDao,
                                                        globalCardProcessedModel,
                                                        batchData
                                                    ).createTransactionPacket()
                                                    // sync pending transaction
                                                    Utility().syncPendingTransaction(
                                                        transactionViewModel
                                                    ) {

                                                    }
                                                    when (val genericResp =
                                                        transactionViewModel.serverCall(
                                                            transactionISO
                                                        )) {
                                                        is GenericResponse.Success -> {
                                                            withContext(Dispatchers.Main) {
                                                                logger(
                                                                    "success:- ",
                                                                    "in success $genericResp",
                                                                    "e"
                                                                )
                                                                hideProgress()
                                                                goToDashBoard()
                                                            }
                                                        }
                                                        is GenericResponse.Error -> {
                                                            logger(
                                                                "error:- ",
                                                                "in error $genericResp",
                                                                "e"
                                                            )
                                                            logger(
                                                                "error:- ",
                                                                "save transaction sync later",
                                                                "e"
                                                            )

                                                            val pendingSyncTransactionTable =
                                                                PendingSyncTransactionTable(
                                                                    invoice = receiptDetail.invoice.toString(),
                                                                    batchTable = batchData,
                                                                    responseCode = genericResp.toString(),
                                                                    cardProcessedDataModal = globalCardProcessedModel
                                                                )

                                                            pendingSyncTransactionViewModel.insertPendingSyncTransactionData(
                                                                pendingSyncTransactionTable
                                                            )
                                                            withContext(Dispatchers.Main) {
                                                                hideProgress()
                                                                errorOnSyncing(
                                                                    genericResp.errorMessage
                                                                        ?: "Sync Error...."
                                                                )
                                                            }
                                                        }
                                                        is GenericResponse.Loading -> {
                                                            logger(
                                                                "Loading:- ",
                                                                "in Loading $genericResp",
                                                                "e"
                                                            )
                                                            withContext(Dispatchers.Main) {
                                                                hideProgress()
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }


                                    }
                                    ResponseCode.FAILED.value -> {
                                        AppPreference.clearRestartDataPreference()
                                        errorFromIngenico(
                                            txnResponse.responseCode,
                                            txnResponse.status.toString()
                                        )
                                    }
                                    ResponseCode.ABORTED.value -> {
                                        AppPreference.clearRestartDataPreference()
                                        errorFromIngenico(
                                            txnResponse.responseCode,
                                            txnResponse.status.toString()
                                        )
                                    }
                                    ResponseCode.REVERSAL.value -> {
                                        AppPreference.clearRestartDataPreference()
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
                                        errorFromIngenico(
                                            txnResponse.responseCode,
                                            txnResponse.status.toString()
                                        )
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

                    val tranUuid = UUID.randomUUID().toString().also {
                        ecrID = it

                    }
                    val restartHandlingModel = RestartHandlingModel(tranUuid, EDashboardItem.REFUND)
                    restartHandlingList.add(restartHandlingModel)
                    val jsonResp = Gson().toJson(restartHandlingModel)
                    println(jsonResp)
                    AppPreference.saveRestartDataPreference(jsonResp)

                    DeviceHelper.doRefundTxn(
                        RefundRequest(
                            amount = amt,
                            tid = tid,
                            transactionUuid = tranUuid
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
                                            //To get tpt data acccording to tid
                                            val tpt = runBlocking(Dispatchers.IO) {
                                                getTptData()
                                            }

                                            // region print and save data
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                //    appDao.insertBatchData(batchData)
                                                batchData.invoice = receiptDetail.invoice.toString()
                                                batchData.transactionType =
                                                    BhTransactionType.REFUND.type
                                                batchData.bonushubbatchnumber =
                                                    tpt?.batchNumber ?: ""
                                                batchData.bonushubInvoice = tpt?.invoiceNumber ?: ""
                                                batchData.bonushubStan = tpt?.stan ?: ""

                                                createCardProcessingModelData(receiptDetail)
                                                val data =
                                                    globalCardProcessedModel.getPanNumberData()
                                                        ?.let {
                                                            batchData.receiptData?.let { it1 ->
                                                                getEncryptedDataForSyncing(
                                                                    it,
                                                                    it1
                                                                )
                                                            }
                                                        }
                                                if (data != null) {
                                                    batchData.field57EncryptedData = data
                                                }

                                                appDatabase.appDao.insertBatchData(batchData)
                                                AppPreference.saveLastReceiptDetails(batchData)
                                                AppPreference.clearRestartDataPreference()

                                                //To increment base Stan
                                                Utility().incrementUpdateRoc()
                                                //To increment base invoice
                                                Utility().incrementUpdateInvoice()

                                                printingSaleData(batchData) {
                                                    withContext(Dispatchers.Main) {
                                                        showProgress(getString(R.string.transaction_syncing_msg))
                                                    }

                                                    val transactionISO = CreateTransactionPacket(
                                                        appDao,
                                                        globalCardProcessedModel,
                                                        batchData
                                                    ).createTransactionPacket()
                                                    // sync pending transaction
                                                    Utility().syncPendingTransaction(
                                                        transactionViewModel
                                                    ) {}

                                                    when (val genericResp =
                                                        transactionViewModel.serverCall(
                                                            transactionISO
                                                        )) {
                                                        is GenericResponse.Success -> {
                                                            withContext(Dispatchers.Main) {
                                                                logger(
                                                                    "success:- ",
                                                                    "in success $genericResp",
                                                                    "e"
                                                                )
                                                                hideProgress()
                                                                goToDashBoard()
                                                            }
                                                        }
                                                        is GenericResponse.Error -> {
                                                            logger(
                                                                "error:- ",
                                                                "in error $genericResp",
                                                                "e"
                                                            )
                                                            logger(
                                                                "error:- ",
                                                                "save transaction sync later",
                                                                "e"
                                                            )

                                                            val pendingSyncTransactionTable =
                                                                PendingSyncTransactionTable(
                                                                    invoice = receiptDetail.invoice.toString(),
                                                                    batchTable = batchData,
                                                                    responseCode = genericResp.toString(),
                                                                    cardProcessedDataModal = globalCardProcessedModel
                                                                )

                                                            pendingSyncTransactionViewModel.insertPendingSyncTransactionData(
                                                                pendingSyncTransactionTable
                                                            )
                                                            withContext(Dispatchers.Main) {
                                                                hideProgress()
                                                                errorOnSyncing(
                                                                    genericResp.errorMessage
                                                                        ?: "Sync Error...."
                                                                )
                                                            }
                                                        }
                                                        is GenericResponse.Loading -> {
                                                            logger(
                                                                "Loading:- ",
                                                                "in Loading $genericResp",
                                                                "e"
                                                            )
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
                                    ResponseCode.FAILED.value -> {
                                        AppPreference.clearRestartDataPreference()
                                        errorFromIngenico(
                                            txnResponse.responseCode,
                                            txnResponse.status.toString()
                                        )
                                    }
                                    ResponseCode.ABORTED.value -> {
                                        AppPreference.clearRestartDataPreference()
                                        errorFromIngenico(
                                            txnResponse.responseCode,
                                            txnResponse.status.toString()
                                        )

                                    }
                                    ResponseCode.REVERSAL.value -> {
                                        AppPreference.clearRestartDataPreference()
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
                                        errorFromIngenico(
                                            txnResponse.responseCode,
                                            txnResponse.status.toString()
                                        )
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

                    val tranUuid = UUID.randomUUID().toString().also {
                        ecrID = it
                    }
                    val restartHandlingModel =
                        RestartHandlingModel(tranUuid, EDashboardItem.PREAUTH)
                    restartHandlingList.add(restartHandlingModel)
                    val jsonResp = Gson().toJson(restartHandlingModel)
                    println(jsonResp)
                    AppPreference.saveRestartDataPreference(jsonResp)

                    DeviceHelper.doPreAuthTxn(
                        PreAuthRequest(
                            amount = amt,
                            tid = tid,
                            transactionUuid = tranUuid
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
                                            //To get tpt data acccording to tid
                                            val tpt = runBlocking(Dispatchers.IO) {
                                                getTptData()
                                            }
                                            val batchData = BatchTable(receiptDetail)

                                            // region print and save data
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                //    appDao.insertBatchData(batchData)
                                                batchData.invoice = receiptDetail.invoice.toString()
                                                batchData.transactionType =
                                                    BhTransactionType.PRE_AUTH.type
                                                //To get bonushub batchumber,bonushub invoice,bonushub stan
                                                batchData.bonushubbatchnumber =
                                                    tpt?.batchNumber ?: ""
                                                batchData.bonushubInvoice = tpt?.invoiceNumber ?: ""
                                                batchData.bonushubStan = tpt?.stan ?: ""

                                                createCardProcessingModelData(receiptDetail)
                                                val data =
                                                    globalCardProcessedModel.getPanNumberData()
                                                        ?.let {
                                                            batchData.receiptData?.let { it1 ->
                                                                getEncryptedDataForSyncing(
                                                                    it,
                                                                    it1
                                                                )
                                                            }
                                                        }
                                                if (data != null) {
                                                    batchData.field57EncryptedData = data
                                                }

                                                appDatabase.appDao.insertBatchData(batchData)
                                                AppPreference.saveLastReceiptDetails(batchData)
                                                AppPreference.clearRestartDataPreference()

                                                //To increment base Stan
                                                Utility().incrementUpdateRoc()
                                                //To increment base invoice
                                                Utility().incrementUpdateInvoice()

                                                printingSaleData(batchData) {
                                                    withContext(Dispatchers.Main) {
                                                        showProgress(getString(R.string.transaction_syncing_msg))
                                                    }

                                                    val transactionISO = CreateTransactionPacket(
                                                        appDao,
                                                        globalCardProcessedModel,
                                                        batchData
                                                    ).createTransactionPacket()
                                                    // sync pending transaction
                                                    Utility().syncPendingTransaction(
                                                        transactionViewModel
                                                    ) {}

                                                    when (val genericResp =
                                                        transactionViewModel.serverCall(
                                                            transactionISO
                                                        )) {
                                                        is GenericResponse.Success -> {
                                                            withContext(Dispatchers.Main) {
                                                                logger(
                                                                    "success:- ",
                                                                    "in success $genericResp",
                                                                    "e"
                                                                )
                                                                hideProgress()
                                                                goToDashBoard()
                                                            }
                                                        }
                                                        is GenericResponse.Error -> {
                                                            logger(
                                                                "error:- ",
                                                                "in error $genericResp",
                                                                "e"
                                                            )
                                                            logger(
                                                                "error:- ",
                                                                "save transaction sync later",
                                                                "e"
                                                            )

                                                            val pendingSyncTransactionTable =
                                                                PendingSyncTransactionTable(
                                                                    invoice = receiptDetail.invoice.toString(),
                                                                    batchTable = batchData,
                                                                    responseCode = genericResp.toString(),
                                                                    cardProcessedDataModal = globalCardProcessedModel
                                                                )

                                                            pendingSyncTransactionViewModel.insertPendingSyncTransactionData(
                                                                pendingSyncTransactionTable
                                                            )
                                                            withContext(Dispatchers.Main) {
                                                                hideProgress()
                                                                errorOnSyncing(
                                                                    genericResp.errorMessage
                                                                        ?: "Sync Error...."
                                                                )
                                                            }
                                                        }
                                                        is GenericResponse.Loading -> {
                                                            logger(
                                                                "Loading:- ",
                                                                "in Loading $genericResp",
                                                                "e"
                                                            )
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
                                    ResponseCode.FAILED.value -> {
                                        AppPreference.clearRestartDataPreference()
                                        errorFromIngenico(
                                            txnResponse.responseCode,
                                            txnResponse.status.toString()
                                        )
                                    }
                                    ResponseCode.ABORTED.value -> {
                                        AppPreference.clearRestartDataPreference()
                                        errorFromIngenico(
                                            txnResponse.responseCode,
                                            txnResponse.status.toString()
                                        )

                                    }
                                    ResponseCode.REVERSAL.value -> {
                                        AppPreference.clearRestartDataPreference()
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
                                        errorFromIngenico(
                                            txnResponse.responseCode,
                                            txnResponse.status.toString()
                                        )
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
                    val tranUuid = UUID.randomUUID().toString().also {
                        ecrID = it
                    }
                    val restartHandlingModel =
                        RestartHandlingModel(tranUuid, EDashboardItem.PREAUTH_COMPLETE)
                    restartHandlingList.add(restartHandlingModel)
                    val jsonResp = Gson().toJson(restartHandlingModel)
                    println(jsonResp)
                    AppPreference.saveRestartDataPreference(jsonResp)

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
                            transactionUuid = tranUuid
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

                                        if (receiptDetail != null) {
                                            val tpt = runBlocking(Dispatchers.IO) {
                                                getTptData()
                                            }

                                            val oldBatchTable = runBlocking(Dispatchers.IO) {
                                             val table=   getBatchDataByInvoice(receiptDetail.invoice.toString())
                                                if(table?.transactionType==BhTransactionType.PRE_AUTH.type){
                                                    table
                                                }else{
                                                    null
                                                }
                                            }
                                            val newBatchData = BatchTable(receiptDetail)
                                            // region print and save data
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                if(oldBatchTable != null){
                                                    // replace reciept data if present in our batch data
                                                        val oldDateTime=oldBatchTable.receiptData?.dateTime?:""
val oldstan=oldBatchTable.bonushubStan
                                                    oldBatchTable.receiptData = receiptDetail
                                                    oldBatchTable.oldDateTimeInVoid=oldDateTime
                                                    oldBatchTable.oldStanForVoid=oldstan

                                                    oldBatchTable.transactionType = BhTransactionType.PRE_AUTH_COMPLETE.type
                                                    oldBatchTable.bonushubStan = tpt?.stan.toString()
                                                    oldBatchTable.bonushubbatchnumber= tpt?.batchNumber.toString()
                                                    appDatabase.appDao.insertBatchData(oldBatchTable)
                                                    AppPreference.saveLastReceiptDetails(oldBatchTable)
                                                    Utility().incrementUpdateRoc()
                                                }
                                                else{
                                                    //    appDao.insertBatchData(batchData)
                                                    newBatchData.invoice = receiptDetail.invoice.toString()
                                                    newBatchData.transactionType = BhTransactionType.PRE_AUTH_COMPLETE.type
                                                    //To get bonushb batchnumber,bonuhubinvoice,bonuhub stan
                                                    newBatchData.bonushubbatchnumber =
                                                        tpt?.batchNumber .toString()
                                                    newBatchData.bonushubInvoice = tpt?.invoiceNumber.toString()
                                                    newBatchData.bonushubStan = tpt?.stan .toString()
                                                    newBatchData.oldStanForVoid=tpt?.stan.toString()
                                                    newBatchData.oldDateTimeInVoid = receiptDetail.dateTime.toString()
                                                    appDatabase.appDao.insertBatchData(newBatchData)
                                                    AppPreference.saveLastReceiptDetails(newBatchData)

                                                    Utility().incrementUpdateRoc()
                                                    Utility().incrementUpdateInvoice()
                                                }

                                                val requiredBatchData :BatchTable= oldBatchTable ?: newBatchData
                                                requiredBatchData.receiptData?.let {
                                                    createCardProcessingModelData(
                                                        it
                                                    )
                                                }
                                                val data =
                                                    globalCardProcessedModel.getPanNumberData()
                                                        ?.let {
                                                            requiredBatchData.receiptData?.let { it1 ->
                                                                getEncryptedDataForSyncing(
                                                                    it,
                                                                    it1
                                                                )
                                                            }
                                                        }
                                                if (data != null) {
                                                    requiredBatchData.field57EncryptedData = data
                                                }
                                                AppPreference.clearRestartDataPreference()
                                                printingSaleData(requiredBatchData) {
                                                    withContext(Dispatchers.Main) {
                                                        showProgress(getString(R.string.transaction_syncing_msg))
                                                    }

                                                    val transactionISO = CreateTransactionPacket(
                                                        appDao,
                                                        globalCardProcessedModel,
                                                        requiredBatchData
                                                    ).createTransactionPacket()
                                                    // sync pending transaction
                                                    Utility().syncPendingTransaction(
                                                        transactionViewModel
                                                    ) {}

                                                    when (val genericResp =
                                                        transactionViewModel.serverCall(
                                                            transactionISO
                                                        )) {
                                                        is GenericResponse.Success -> {
                                                            withContext(Dispatchers.Main) {
                                                                logger(
                                                                    "success:- ",
                                                                    "in success $genericResp",
                                                                    "e"
                                                                )
                                                                hideProgress()
                                                                goToDashBoard()
                                                            }
                                                        }
                                                        is GenericResponse.Error -> {
                                                            logger(
                                                                "error:- ",
                                                                "in error $genericResp",
                                                                "e"
                                                            )
                                                            logger(
                                                                "error:- ",
                                                                "save transaction sync later",
                                                                "e"
                                                            )

                                                            val pendingSyncTransactionTable =
                                                                PendingSyncTransactionTable(
                                                                    invoice = receiptDetail.invoice.toString(),
                                                                    batchTable = requiredBatchData,
                                                                    responseCode = genericResp.toString(),
                                                                    cardProcessedDataModal = globalCardProcessedModel
                                                                )

                                                            pendingSyncTransactionViewModel.insertPendingSyncTransactionData(
                                                                pendingSyncTransactionTable
                                                            )
                                                            withContext(Dispatchers.Main) {
                                                                hideProgress()
                                                                errorOnSyncing(
                                                                    genericResp.errorMessage
                                                                        ?: "Sync Error...."
                                                                )
                                                            }
                                                        }
                                                        is GenericResponse.Loading -> {
                                                            logger(
                                                                "Loading:- ",
                                                                "in Loading $genericResp",
                                                                "e"
                                                            )
                                                            withContext(Dispatchers.Main) {
                                                                hideProgress()
                                                            }
                                                        }
                                                    }

                                                }
                                            }


                                        }


                                    }

                                    ResponseCode.ABORTED.value, ResponseCode.FAILED.value -> {
                                        AppPreference.clearRestartDataPreference()
                                        errorFromIngenico(
                                            txnResponse.responseCode,
                                            txnResponse.status.toString()
                                        )
                                    }
                                    "03" -> {
                                        AppPreference.clearRestartDataPreference()

                                        errorFromIngenico(
                                            txnResponse.responseCode,
                                            txnResponse.status.toString()
                                        )

                                    }
                                    ResponseCode.REVERSAL.value -> {
                                        AppPreference.clearRestartDataPreference()
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
                                        errorFromIngenico(
                                            txnResponse?.responseCode,
                                            txnResponse?.status.toString()
                                        )
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

    private suspend fun initiateNormalSale() {
        val amt = (saleAmt.toFloat() * 100).toLong()
        val cashBackAmount = (saleWithTipAmt.toFloat() * 100).toLong()
        field54Data = cashBackAmount
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

                                        //To get tpt data acccording to tid
                                        val tpt = runBlocking(Dispatchers.IO) {
                                            getTptData()
                                        }
                                        val batchData = BatchTable(receiptDetail)
                                        println(jsonResp)
                                        batchData.invoice = receiptDetail.invoice.toString()
                                        batchData.transactionType = BhTransactionType.SALE.type
                                        //To assign bonushub batchnumber,bonushub invoice,bonuhub stan
                                        batchData.bonushubbatchnumber = tpt?.batchNumber ?: ""
                                        batchData.bonushubInvoice = tpt?.invoiceNumber ?: ""
                                        batchData.bonushubStan = tpt?.stan ?: ""

                                        createCardProcessingModelData(receiptDetail)
                                        val data =
                                            globalCardProcessedModel.getPanNumberData()?.let {
                                                batchData.receiptData?.let { it1 ->
                                                    getEncryptedDataForSyncing(
                                                        it,
                                                        it1
                                                    )
                                                }
                                            }
                                        if (data != null) {
                                            batchData.field57EncryptedData = data
                                        }

                                        appDatabase.appDao.insertBatchData(batchData)
                                        AppPreference.saveLastReceiptDetails(batchData)
                                       AppPreference.clearRestartDataPreference()

                                        //To increment base Stan
                                        Utility().incrementUpdateRoc()
                                        //To increment base invoice
                                        Utility().incrementUpdateInvoice()
                                        printingSaleData(batchData) {
                                            withContext(Dispatchers.Main) {
                                                showProgress(getString(R.string.transaction_syncing_msg))
                                            }

                                            val transactionISO = CreateTransactionPacket(
                                                appDao,
                                                globalCardProcessedModel,
                                                batchData
                                            ).createTransactionPacket()
                                            // sync pending transaction
                                            Utility().syncPendingTransaction(transactionViewModel) {}

                                            when (val genericResp =
                                                transactionViewModel.serverCall(transactionISO)) {
                                                is GenericResponse.Success -> {
                                                    withContext(Dispatchers.Main) {
                                                        logger(
                                                            "success:- ",
                                                            "in success $genericResp",
                                                            "e"
                                                        )
                                                        hideProgress()
                                                        goToDashBoard()
                                                    }

                                                }
                                                is GenericResponse.Error -> {
                                                    logger("error:- ", "in error $genericResp", "e")
                                                    val pendingSyncTransactionTable =
                                                        PendingSyncTransactionTable(
                                                            invoice = receiptDetail.invoice.toString(),
                                                            batchTable = batchData,
                                                            responseCode = genericResp.toString(),
                                                            cardProcessedDataModal = globalCardProcessedModel
                                                        )
                                                    pendingSyncTransactionViewModel.insertPendingSyncTransactionData(
                                                        pendingSyncTransactionTable
                                                    )
                                                    withContext(Dispatchers.Main) {
                                                        hideProgress()
                                                        errorOnSyncing(
                                                            genericResp.errorMessage
                                                                ?: "Sync Error...."
                                                        )
                                                    }

                                                }
                                                is GenericResponse.Loading -> {
                                                    withContext(Dispatchers.Main) {
                                                        hideProgress()
                                                    }
                                                    logger(
                                                        "Loading:- ",
                                                        "in Loading $genericResp",
                                                        "e"
                                                    )
                                                }
                                            }

                                        }


                                    }
                                }
                            }

                            ResponseCode.ABORTED.value,  ResponseCode.FAILED.value -> {
                                AppPreference.clearRestartDataPreference()
                                errorFromIngenico(
                                    txnResponse.responseCode,
                                    txnResponse.status.toString()
                                )
                            }
                            ResponseCode.REVERSAL.value -> {
                                AppPreference.clearRestartDataPreference()
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

    private suspend fun initiateSaleFromInstaEMiOption() {
        try {
            val amt = (saleAmt.toFloat() * 100).toLong()
            var track1: Track1? = null
            var track2: Track2? = null
            val cardCaptureType: CardCaptureType
            if (globalCardProcessedModel.getReadCardType() == DetectCardType.MAG_CARD_TYPE) {
                val tracksData =
                    RawStripe(
                        globalCardProcessedModel.getTrack1Data(),
                        globalCardProcessedModel.getTrack2Data()
                    )
                track1 = tracksData.track1
                track2 = tracksData.track2
                cardCaptureType =
                    if (globalCardProcessedModel.getFallbackType() == com.bonushub.crdb.utils.EFallbackCode.EMV_fallback.fallBackCode) {
                        //  swipe from emv fall back
                        CardCaptureType.FALLBACK_EMV_NO_CAPTURING
                    } else {
                        //  only swipe case
                        CardCaptureType.EMV_NO_CAPTURING
                    }
            } else {
                //  insert case
                cardCaptureType = CardCaptureType.EMV_NO_CAPTURING
            }
            val cashBackAmount = (saleWithTipAmt.toFloat() * 100).toLong()
            DeviceHelper.doEMITxn(
                EMISaleRequest(
                    amount = amt,
                    tipAmount = cashBackAmount,
                    emiTxnName = getTransactionTypeName(globalCardProcessedModel.getTransType()),
                    tid = tid,
                    cardCaptureType = cardCaptureType,
                    track1 = track1,
                    track2 = track2,
                    transactionUuid = UUID.randomUUID().toString(),
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
                                            getTptData()
                                        }

                                        val batchData = BatchTable(receiptDetail)
                                        println(jsonResp)
                                        batchData.invoice = receiptDetail.invoice.toString()
                                        batchData.transactionType = BhTransactionType.SALE.type
                                        //To assign bonushub batchnumber,bonushub invoice,bonuhub stan
                                        batchData.bonushubbatchnumber = tpt?.batchNumber ?: ""
                                        batchData.bonushubInvoice = tpt?.invoiceNumber ?: ""
                                        batchData.bonushubStan = tpt?.stan ?: ""
                                        createCardProcessingModelData(receiptDetail)
                                        val data =
                                            globalCardProcessedModel.getPanNumberData()?.let {
                                                batchData.receiptData?.let { it1 ->
                                                    getEncryptedDataForSyncing(
                                                        it,
                                                        it1
                                                    )
                                                }
                                            }
                                        if (data != null) {
                                            batchData.field57EncryptedData = data
                                        }
                                        appDatabase.appDao.insertBatchData(batchData)
                                        AppPreference.saveLastReceiptDetails(batchData)

                                        //To increment base Stan
                                        Utility().incrementUpdateRoc()
                                        //To increment base invoice
                                        Utility().incrementUpdateInvoice()

                                        printingSaleData(batchData) {
                                            withContext(Dispatchers.Main) {
                                                showProgress(getString(R.string.transaction_syncing_msg))
                                            }

                                            val transactionISO = CreateTransactionPacket(
                                                appDao,
                                                globalCardProcessedModel,
                                                batchData
                                            ).createTransactionPacket()
                                            // sync pending transaction
                                            Utility().syncPendingTransaction(transactionViewModel) {}

                                            when (val genericResp =
                                                transactionViewModel.serverCall(transactionISO)) {
                                                is GenericResponse.Success -> {
                                                    withContext(Dispatchers.Main) {
                                                        logger(
                                                            "success:- ",
                                                            "in success $genericResp",
                                                            "e"
                                                        )
                                                        hideProgress()
                                                        goToDashBoard()
                                                    }

                                                }
                                                is GenericResponse.Error -> {
                                                    logger("error:- ", "in error $genericResp", "e")
                                                    val pendingSyncTransactionTable =
                                                        PendingSyncTransactionTable(
                                                            invoice = receiptDetail.invoice.toString(),
                                                            batchTable = batchData,
                                                            responseCode = genericResp.toString(),
                                                            cardProcessedDataModal = globalCardProcessedModel
                                                        )
                                                    pendingSyncTransactionViewModel.insertPendingSyncTransactionData(
                                                        pendingSyncTransactionTable
                                                    )
                                                    withContext(Dispatchers.Main) {
                                                        hideProgress()
                                                        errorOnSyncing(
                                                            genericResp.errorMessage
                                                                ?: "Sync Error...."
                                                        )
                                                    }

                                                }
                                                is GenericResponse.Loading -> {
                                                    withContext(Dispatchers.Main) {
                                                        hideProgress()
                                                    }
                                                    logger(
                                                        "Loading:- ",
                                                        "in Loading $genericResp",
                                                        "e"
                                                    )
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
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

    }

    private suspend fun initiateInstaEmi(
        bankEMIIssuerTandCData: BankEMIIssuerTAndCDataModal,
        bankEMISchemesDataList: MutableList<BankEMITenureDataModal>
    ) {
        val intent = Intent(
            this@TransactionActivity,
            TenureSchemeActivity::class.java
        ).apply {
            globalCardProcessedModel.setTransactionAmount((saleAmt.toDouble() * 100).toLong())

            putExtra("cardProcessedData", globalCardProcessedModel)
            putExtra("transactionType", globalCardProcessedModel.getTransType())

            putParcelableArrayListExtra(
                "emiSchemeOfferDataList",
                bankEMISchemesDataList as java.util.ArrayList<out Parcelable>
            )
            putExtra("emiIssuerTAndCDataList", bankEMIIssuerTandCData)
        }
        startActivityForResult(intent, globalCardProcessedModel.getTransType())

    }

    private fun showEMISaleDialog(
        bankEMIIssuerTAndC: BankEMIIssuerTAndCDataModal,
        bankEMISchemesDataList: MutableList<BankEMITenureDataModal>
    ) {
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
                initiateSaleFromInstaEMiOption()
            }

        }

        dialog.findViewById<Button>(R.id.cardemiButton).setOnClickListener {
            dialog.dismiss()
            //  cardProcessedDataModal.setTransType(BhTransactionType.EMI_SALE.type)
            //   cardProcessedDataModal.setEmiType(1)  //1 for insta emi
            /* binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.emi_catalog_icon)
             binding?.subHeaderView?.subHeaderText?.text = BhTransactionType.EMI_SALE.txnTitle*/
            lifecycleScope.launch(Dispatchers.IO) {
                initiateInstaEmi(bankEMIIssuerTAndC, bankEMISchemesDataList)
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


    suspend fun printingSaleData(batchTable: BatchTable, cb: suspend (Boolean) -> Unit) {
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

    private suspend fun showMerchantAlertBox(
        batchTable: BatchTable,
        cb: suspend (Boolean) -> Unit
    ) {
        withContext(Dispatchers.Main) {
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

    suspend fun errorOnSyncing(msg: String) {
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
        logger("", "" + receiptDetail)
        when (globalCardProcessedModel.getTransType()) {
            BhTransactionType.SALE.type, BhTransactionType.TEST_EMI.type,
            BhTransactionType.BRAND_EMI.type, BhTransactionType.EMI_SALE.type -> {
                globalCardProcessedModel.setProcessingCode(ProcessingCode.SALE.code)
            }
            BhTransactionType.CASH_AT_POS.type -> {
                globalCardProcessedModel.setProcessingCode(ProcessingCode.CASH_AT_POS.code)
            }
            BhTransactionType.SALE_WITH_CASH.type -> {
                globalCardProcessedModel.setProcessingCode(ProcessingCode.SALE_WITH_CASH.code)
            }
            BhTransactionType.VOID.type -> {
                globalCardProcessedModel.setProcessingCode(ProcessingCode.VOID.code)
            }
            BhTransactionType.REFUND.type -> {
                globalCardProcessedModel.setProcessingCode(ProcessingCode.REFUND.code)
            }
            BhTransactionType.VOID_PREAUTH.type -> {
                globalCardProcessedModel.setProcessingCode(ProcessingCode.VOID_PREAUTH.code)
            }
            BhTransactionType.PRE_AUTH_COMPLETE.type -> {
                globalCardProcessedModel.setProcessingCode(ProcessingCode.PRE_SALE_COMPLETE.code)
            }
            BhTransactionType.PRE_AUTH.type -> {
                globalCardProcessedModel.setProcessingCode(ProcessingCode.PRE_AUTH.code)
            }
        }

        receiptDetail.txnAmount?.let { globalCardProcessedModel.setTransactionAmount(it.toLong()) }
        receiptDetail.txnOtherAmount?.let { globalCardProcessedModel.setOtherAmount(it.toLong()) }
        globalCardProcessedModel.setMobileBillExtraData(Pair(mobileNumber, billNumber))
        receiptDetail.stan?.let { globalCardProcessedModel.setAuthRoc(it) }
        //globalCardProcessedModel.setCardMode("0553- emv with pin")
        logger(
            "mode22 ->",
            CardMode(receiptDetail.entryMode ?: "", receiptDetail.isVerifyPin ?: false),
            "e"
        )
        globalCardProcessedModel.setCardMode(
            CardMode(
                receiptDetail.entryMode ?: "",
                receiptDetail.isVerifyPin ?: false
            )
        )
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

    fun createField58ForBankEmi(
        cardProcessedData: CardProcessedDataModal,
        batchdata: BatchTable
    ): String {
        val cardIndFirst = "0"
        val firstTwoDigitFoCard = cardProcessedData.getPanNumberData()?.substring(0, 2)
        val cardDataTable = DBModule.appDatabase.appDao.getCardDataByPanNumber(
            cardProcessedData.getPanNumberData().toString()
        )
        //  val cardDataTable = CardDataTable.selectFromCardDataTable(cardProcessedData.getTrack2Data()!!)
        val cdtIndex = cardDataTable?.cardTableIndex ?: ""
        val accSellection = "00"
        val tenureData = batchdata.emiTenureDataModel
        val imeiOrSerialNo = batchdata.imeiOrSerialNum
        val emiIssuerDataModel = batchdata.emiIssuerDataModel
        return "$cardIndFirst|$firstTwoDigitFoCard|$cdtIndex|$accSellection," +
                "${cardProcessedData.getPanNumberData()?.substring(0, 8)}," +
                "${emiIssuerDataModel?.issuerID}," +
                "${emiIssuerDataModel?.emiSchemeID},1,0,${cardProcessedData.getTransactionAmount()}," +
                "${tenureData?.discountAmount},${tenureData?.loanAmount},${tenureData?.tenure}," +
                "${tenureData?.tenureInterestRate},${tenureData?.emiAmount},${tenureData?.cashBackAmount}," +
                "${tenureData?.netPay},${cardProcessedData.getMobileBillExtraData()?.second ?: ""}," +
                ",,${cardProcessedData.getMobileBillExtraData()?.first ?: ""},,0,${tenureData?.processingFee},${tenureData?.processingRate}," +
                "${tenureData?.totalProcessingFee},,${tenureData?.instantDiscount}"


    }

    fun createField58ForBrandEmi(
        cardProcessedData: CardProcessedDataModal,
        batchdata: BatchTable
    ): String {
        val cardIndFirst = "0"
        val firstTwoDigitFoCard = cardProcessedData.getPanNumberData()?.substring(0, 2)
        val cardDataTable = DBModule.appDatabase.appDao.getCardDataByPanNumber(
            cardProcessedData.getPanNumberData().toString()
        )
        //  val cardDataTable = CardDataTable.selectFromCardDataTable(cardProcessedData.getTrack2Data()!!)
        val cdtIndex = cardDataTable?.cardTableIndex ?: ""
        val accSellection = "00"

        val brandData = batchdata.emiBrandData
        val productData = batchdata.emiProductData
        val categoryData = batchdata.emiSubCategoryData
        val tenureData = batchdata.emiTenureDataModel
        val imeiOrSerialNo = batchdata.imeiOrSerialNum
        val emiIssuerDataModel = batchdata.emiIssuerDataModel

        return "$cardIndFirst|$firstTwoDigitFoCard|$cdtIndex|$accSellection," +
                "${cardProcessedData.getPanNumberData()?.substring(0, 8)}," +
                "${emiIssuerDataModel?.issuerID},${emiIssuerDataModel?.emiSchemeID},${brandData?.brandID}," +
                "${productData?.productID},${cardProcessedData.getTransactionAmount()}," +
                "${tenureData?.discountAmount},${tenureData?.loanAmount},${tenureData?.tenure}," +
                "${tenureData?.tenureInterestRate},${tenureData?.emiAmount},${tenureData?.cashBackAmount}," +
                "${tenureData?.netPay},${cardProcessedData.getMobileBillExtraData()?.second ?: ""}," +
                "${imeiOrSerialNo ?: ""},,${cardProcessedData.getMobileBillExtraData()?.first ?: ""},,0,${tenureData?.processingFee},${tenureData?.processingRate}," +
                "${tenureData?.totalProcessingFee},,${tenureData?.instantDiscount}"


    }

    fun createField58ForTestEmi(cardProcessedData: CardProcessedDataModal): String {
        val cardIndFirst = "0"
        val firstTwoDigitFoCard = cardProcessedData.getPanNumberData()?.substring(0, 2)
        val cardDataTable = DBModule.appDatabase.appDao.getCardDataByPanNumber(
            cardProcessedData.getPanNumberData().toString()
        )
        //  val cardDataTable = CardDataTable.selectFromCardDataTable(cardProcessedData.getTrack2Data()!!)
        val cdtIndex = cardDataTable?.cardTableIndex ?: ""
        val accSellection = "00"
        return "$cardIndFirst|$firstTwoDigitFoCard|$cdtIndex|$accSellection|${cardProcessedData.testEmiOption}"


    }

    fun CardMode(entryMode: String, isPinVerify: Boolean): String {
        logger("entryMode", "" + entryMode, "e")
        when (entryMode) {

            CardEntryMode.EMV_WITH_PIN._name -> {
                if (isPinVerify) {
                    logger("entryMode", "EMV_WITH_PIN", "e")
                    return CardEntryMode.EMV_WITH_PIN._value
                } else {
                    logger("entryMode", "EMV_NO_PIN", "e")
                    return CardEntryMode.EMV_NO_PIN._value
                }

            }

            CardEntryMode.EMV_FALLBACK_SWIPE_WITH_PIN._name -> {
                if (isPinVerify) {
                    logger("entryMode", "EMV_FALLBACK_SWIPE_WITH_PIN", "e")
                    return CardEntryMode.EMV_FALLBACK_SWIPE_WITH_PIN._value
                } else {
                    logger("entryMode", "EMV_FALLBACK_SWIPE_NO_PIN", "e")
                    return CardEntryMode.EMV_FALLBACK_SWIPE_NO_PIN._value
                }

            }

            CardEntryMode.SWIPE_WITH_PIN._name -> {
                if (isPinVerify) {
                    logger("entryMode", "SWIPE_WITH_PIN", "e")
                    return CardEntryMode.SWIPE_WITH_PIN._value
                } else {
                    logger("entryMode", "SWIPE_NO_PIN", "e")
                    return CardEntryMode.SWIPE_NO_PIN._value
                }

            }

            CardEntryMode.CTLS_SWIPE_NO_PIN._name -> {
                if (isPinVerify) {
                    logger("entryMode", "CTLS_SWIPE_WITH_PIN", "e")
                    return CardEntryMode.CTLS_SWIPE_WITH_PIN._value
                } else {
                    logger("entryMode", "CTLS_SWIPE_NO_PIN", "e")
                    return CardEntryMode.CTLS_SWIPE_NO_PIN._value
                }

            }

            CardEntryMode.CTLS_EMV_NO_PIN._name -> {
                if (isPinVerify) {
                    logger("entryMode", "CTLS_EMV_WITH_PIN", "e")
                    return CardEntryMode.CTLS_EMV_WITH_PIN._value
                } else {
                    logger("entryMode", "CTLS_EMV_NO_PIN", "e")
                    return CardEntryMode.CTLS_EMV_NO_PIN._value
                }

            }
            else -> {
                return ""
            }
        }

    }


}