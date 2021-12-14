package com.bonushub.crdb.view.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.ActivityEmvBinding
import com.bonushub.crdb.di.DBModule.appDatabase
import com.bonushub.crdb.entity.CardOption
import com.bonushub.crdb.model.CardProcessedDataModal
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.model.local.BatchTable
import com.bonushub.crdb.model.local.BrandEMISubCategoryTable
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.model.remote.BrandEMIProductDataModal
import com.bonushub.crdb.utils.*
import com.bonushub.crdb.utils.printerUtils.PrintUtil
import com.bonushub.crdb.view.base.BaseActivityNew
import com.bonushub.crdb.view.fragments.AuthCompletionData
import com.bonushub.crdb.viewmodel.SearchViewModel
import com.bonushub.pax.utils.*
import com.google.gson.Gson
import com.ingenico.hdfcpayment.listener.OnPaymentListener
import com.ingenico.hdfcpayment.model.ReceiptDetail
import com.ingenico.hdfcpayment.request.*

import com.ingenico.hdfcpayment.response.PaymentResult
import com.ingenico.hdfcpayment.response.TransactionResponse
import com.ingenico.hdfcpayment.type.ResponseCode
import com.ingenico.hdfcpayment.type.TransactionType
import com.usdk.apiservice.aidl.printer.*


import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_new_input_amount.*
import kotlinx.coroutines.*
import java.util.*

private var bankEMIRequestCode = "4"

@AndroidEntryPoint
class TransactionActivity : BaseActivityNew(){
    private var emvBinding: ActivityEmvBinding? = null
    private val transactionAmountValue by lazy { intent.getStringExtra("amt") ?: "0" }

    //used for other cash amount
    private val transactionOtherAmountValue by lazy { intent.getStringExtra("otherAmount") ?: "0" }

    private val testEmiOperationType by lazy { intent.getStringExtra("TestEmiOption") ?: "0" }

    private val brandEmiSubCatData by lazy { intent.getSerializableExtra("brandEmiSubCatData") as BrandEMISubCategoryTable } //: BrandEMISubCategoryTable? = null
    private val brandEmiProductData by lazy { intent.getSerializableExtra("brandEmiProductData") as BrandEMIProductDataModal }
    private val brandDataMaster by lazy { intent.getSerializableExtra("brandDataMaster") as BrandEMIMasterDataModal }
    private val imeiOrSerialNum by lazy { intent.getStringExtra("imeiOrSerialNum") ?: "" }


    private val saleAmt by lazy { intent.getStringExtra("saleAmt") ?: "0" }
    private val cashBackAmt by lazy { intent.getStringExtra("cashBackAmt") ?: "0" }
    private val authCompletionData by lazy { intent.getSerializableExtra("authCompletionData") as AuthCompletionData }

    private val mobileNumber by lazy { intent.getStringExtra("mobileNumber") ?: "" }

    private val billNumber by lazy { intent.getStringExtra("billNumber") ?: "0" }
    private val saleWithTipAmt by lazy { intent.getStringExtra("saleWithTipAmt") ?: "0" }
    private val title by lazy { intent.getStringExtra("title") }
    private val transactionType by lazy { intent.getIntExtra("type", -1947) }
    private val  transactionTypeEDashboardItem by lazy{ (intent.getSerializableExtra("edashboardItem") ?: EDashboardItem.NONE) as EDashboardItem}
    val TAG = TransactionActivity::class.java.simpleName

    private val searchCardViewModel : SearchViewModel by viewModels()
    private var globalCardProcessedModel = CardProcessedDataModal()


    //  private lateinit var deviceService: UsdkDeviceService
    /* @Inject
     lateinit var appDao: AppDao*/

    private var tid ="000000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        emvBinding = ActivityEmvBinding.inflate(layoutInflater)
        setContentView(emvBinding?.root)
        emvBinding?.subHeaderView?.subHeaderText?.text =transactionTypeEDashboardItem.title
        globalCardProcessedModel.setTransType(transactionType)

        if(transactionTypeEDashboardItem!= EDashboardItem.BRAND_EMI) {
            emvBinding?.cardDetectImg?.visibility = View.GONE
            emvBinding?.tvInsertCard?.visibility = View.GONE
            emvBinding?.subHeaderView?.backImageButton?.visibility = View.GONE
        }
        lifecycleScope.launch(Dispatchers.IO) {
            tid=  getBaseTID(appDatabase.appDao)
            setupFlow()
        }

     /*   searchCardViewModel.fetchCardTypeData(globalCardProcessedModel,CardOption.create().apply {
            supportICCard(true)
            supportMagCard(true)
            supportRFCard(false)
        })
        CoroutineScope(Dispatchers.Main).launch {
            setupObserver()
        }*/

    }

    override fun onEvents(event: VxEvent) {
        TODO("Not yet implemented")
    }

    private suspend fun setupObserver() {
        withContext(Dispatchers.Main){
            searchCardViewModel.allcadType.observe(this@TransactionActivity, Observer { cardProcessdatamodel  ->
                when(cardProcessdatamodel.getReadCardType()){
                    DetectCardType.EMV_CARD_TYPE -> {
                        when(cardProcessdatamodel.getTransType()){
                            BhTransactionType.SALE.type -> {
                                // Checking Insta Emi Available or not
                                var hasInstaEmi = false
                                val tpt = runBlocking(Dispatchers.IO) { Field48ResponseTimestamp.getTptData() }
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
                                if (limitAmt <= "2000".toLong() && hasInstaEmi) {
                                    val intent = Intent (this@TransactionActivity, TenureSchemeActivity::class.java).apply {
                                        putExtra("cardProcessedData", cardProcessdatamodel)
                                        putExtra("transactionType", cardProcessdatamodel.getTransType())
                                    }
                                    startActivity(intent)

                                }
                                else {

                                }
                            }
BhTransactionType.BRAND_EMI.type->{
    setupEMVObserver()
}

                        }

                        Toast.makeText(this@TransactionActivity,"EMV mode detected",Toast.LENGTH_LONG).show()
                    }
                    DetectCardType.CONTACT_LESS_CARD_TYPE -> {
                      //  Toast.makeText(this@TransactionActivity,"Contactless mode detected",Toast.LENGTH_LONG).show()
                    }
                    DetectCardType.MAG_CARD_TYPE -> {
                        Toast.makeText(this@TransactionActivity,"Swipe mode detected",Toast.LENGTH_LONG).show()
                    }
                    else -> {

                    }
                }

            })
        }
    }

    //Below function is used to deal with EMV Card Fallback when we insert EMV Card from other side then chip side:-
   fun handleEMVFallbackFromError(title: String, msg: String, showCancelButton: Boolean, emvFromError: (Boolean) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            alertBoxWithAction(title,
                msg, showCancelButton, getString(R.string.positive_button_ok), { alertCallback ->
                    if (alertCallback) {
                        emvFromError(true)
                    }
                }, {})
        }
    }
var field56=""
    private fun setupEMVObserver() {
        searchCardViewModel.cardTpeData.observe(this, Observer { cardProcessedDataModal ->
            if(cardProcessedDataModal.getPanNumberData() !=null) {
                cardProcessedDataModal.getPanNumberData()
                field56=cardProcessedDataModal.getEncryptedPan().toString()
                var ecrID: String

                Toast.makeText(
                    this,
                    cardProcessedDataModal.getPanNumberData().toString(),
                    Toast.LENGTH_LONG
                ).show()
                startTenureActivity()
                /*  lifecycleScope.launch(Dispatchers.IO) {
                     // serverRepository.getEMITenureData(cardProcessedDataModal.getEncryptedPan().toString())
                      serverRepository.getEMITenureData("B1DFEFE944EE27E9B78136F34C3EB5EE2B891275D5942360")
                  }*/

            }

        })
    }

private fun startTenureActivity(){
    val intent = Intent (this, TenureSchemeActivity::class.java).apply {
        putExtra("field56",field56)
      val field57=  "$bankEMIRequestCode^0^${brandDataMaster.brandID}^${brandEmiProductData.productID}^${imeiOrSerialNum}" +
                "^${/*cardBinValue.substring(0, 8)*/""}^$saleAmt"
        putExtra("field57",field57)
    }
    startActivityForResult(intent,1)
}
    private suspend fun setupFlow(){
        emvBinding?.baseAmtTv?.text=saleAmt
        when(transactionTypeEDashboardItem){
            EDashboardItem.BRAND_EMI->{
                searchCardViewModel.fetchCardTypeData(globalCardProcessedModel, CardOption.create().apply {
                    supportICCard(true)
                    supportMagCard(true)
                    supportRFCard(false)
                })
                setupObserver()

            }
            EDashboardItem.SALE->{
                val amt=(saleAmt.toFloat() * 100).toLong()
                var ecrID: String
                try {
                    DeviceHelper.doSaleTransaction(
                        SaleRequest(
                            amount = amt ?: 0,
                            tipAmount = 0L ?: 0,
                            transactionType = TransactionType.SALE,
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
                                        val jsonResp=Gson().toJson(receiptDetail)
                                        println(jsonResp)

                                        AppPreference.saveLastReceiptDetails(jsonResp) // save last sale receipt11

                                        //   detailResponse.forEach { println(it) }
                                        //  uids.add(ecrID)
                                        // defaultScope.launch { onSaveUId(ecrID, handleLoadingUIdsResult) }
                                        if (receiptDetail != null) {
                                            val batchData=BatchTable(receiptDetail)
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                //    appDao.insertBatchData(batchData)
                                                batchData.invoice= receiptDetail.invoice.toString()
                                                batchData.transactionType = com.bonushub.pax.utils.BhTransactionType.SALE.type
                                                appDatabase.appDao.insertBatchData(batchData)
                                            }
                                            printingSaleData(receiptDetail)
                                        }


                                    }
                                    ResponseCode.FAILED.value,
                                    ResponseCode.ABORTED.value -> {

                                        errorFromIngenico(txnResponse.responseCode,txnResponse.status.toString())
                                    }
                                    else -> {
                                        errorFromIngenico(txnResponse?.responseCode,txnResponse?.status.toString())
                                    }
                                }
                            }
                        }
                    )
                }
                catch (exc: Exception){
                    exc.printStackTrace()
                }
            }
            EDashboardItem.CASH_ADVANCE->{
                val amt=(saleAmt.toFloat() * 100).toLong()
                var ecrID: String
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
                                        val jsonResp=Gson().toJson(receiptDetail)
                                        println(jsonResp)


                                        AppPreference.saveLastReceiptDetails(jsonResp) // save last sale receipt11
                                        //   detailResponse.forEach { println(it) }
                                        //  uids.add(ecrID)
                                        // defaultScope.launch { onSaveUId(ecrID, handleLoadingUIdsResult) }
                                        if (receiptDetail != null) {
                                            val batchData=BatchTable(receiptDetail)
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                //    appDao.insertBatchData(batchData)
                                                batchData.invoice= receiptDetail.invoice.toString()
                                                batchData.transactionType = com.bonushub.pax.utils.BhTransactionType.CASH_AT_POS.type
                                                appDatabase.appDao.insertBatchData(batchData)
                                            }
                                            printingSaleData(receiptDetail)
                                        }


                                    }
                                    ResponseCode.FAILED.value,
                                    ResponseCode.ABORTED.value -> {
                                        errorFromIngenico(txnResponse?.responseCode,txnResponse?.status.toString())
                                    }
                                    else -> {
                                        errorFromIngenico(txnResponse?.responseCode,txnResponse?.status.toString())
                                    }
                                }
                            }
                        }
                    )
                }
                catch (exc: Exception){
                    exc.printStackTrace()
                }
            }
            EDashboardItem.SALE_WITH_CASH->{
                try {
                    val amt=(saleAmt.toFloat() * 100).toLong()
                    val cashBackAmount=(cashBackAmt.toFloat() * 100).toLong()
                    var ecrID: String

                    DeviceHelper.doSaleWithCashTxn(
                        SaleCashBackRequest(
                            amount = amt,
                            cashAmount=cashBackAmount,
                            tid = tid,
                            transactionUuid = "12345"/*UUID.randomUUID().toString().also {
                                ecrID = it

                            }*/
                        ),
                        listener = object : OnPaymentListener.Stub() {
                            override fun onCompleted(result: PaymentResult?) {
                                val txnResponse = result?.value as? TransactionResponse
                                val receiptDetail = txnResponse?.receiptDetail

                                Log.d(TAG, "Response Code: ${txnResponse?.responseCode}")
                                when (txnResponse?.responseCode) {
                                    ResponseCode.SUCCESS.value -> {
                                        val jsonResp=Gson().toJson(receiptDetail)
                                        println(jsonResp)

                                        AppPreference.saveLastReceiptDetails(jsonResp) // save last sale receipt11

                                        //   detailResponse.forEach { println(it) }
                                        //  uids.add(ecrID)
                                        // defaultScope.launch { onSaveUId(ecrID, handleLoadingUIdsResult) }
                                        if (receiptDetail != null) {
                                            val batchData=BatchTable(receiptDetail)
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                //    appDao.insertBatchData(batchData)
                                                batchData.invoice= receiptDetail.invoice.toString()
                                                batchData.transactionType = com.bonushub.pax.utils.BhTransactionType.SALE_WITH_CASH.type
                                                appDatabase.appDao.insertBatchData(batchData)
                                            }
                                            printingSaleData(receiptDetail)
                                        }


                                    }
                                    ResponseCode.FAILED.value,
                                    ResponseCode.ABORTED.value -> {
                                        errorFromIngenico(txnResponse?.responseCode,txnResponse?.status.toString())
                                    }
                                    else -> {
                                        errorFromIngenico(txnResponse?.responseCode,txnResponse?.status.toString())
                                    }
                                }
                            }
                        }
                    )
                }
                catch (exc: Exception){
                    exc.printStackTrace()
                }
            }
            EDashboardItem.REFUND->{
                try {
                    val amt=(saleAmt.toFloat() * 100).toLong()
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
                                        val jsonResp=Gson().toJson(receiptDetail)
                                        println(jsonResp)

                                        AppPreference.saveLastReceiptDetails(jsonResp) // save last sale receipt11

                                        //   detailResponse.forEach { println(it) }
                                        //  uids.add(ecrID)
                                        // defaultScope.launch { onSaveUId(ecrID, handleLoadingUIdsResult) }
                                        if (receiptDetail != null) {
                                            val batchData=BatchTable(receiptDetail)
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                //    appDao.insertBatchData(batchData)
                                                batchData.invoice= receiptDetail.invoice.toString()
                                                batchData.transactionType = com.bonushub.pax.utils.BhTransactionType.REFUND.type
                                                appDatabase.appDao.insertBatchData(batchData)
                                            }
                                            printingSaleData(receiptDetail)
                                        }


                                    }
                                    ResponseCode.FAILED.value,
                                    ResponseCode.ABORTED.value -> {
                                        errorFromIngenico(txnResponse?.responseCode,txnResponse?.status.toString())

                                    }
                                    else -> {
                                        errorFromIngenico(txnResponse?.responseCode,txnResponse?.status.toString())
                                    }
                                }
                            }
                        }
                    )
                }
                catch (exc: Exception){
                    exc.printStackTrace()
                }
            }
            EDashboardItem.PREAUTH->{
                try {
                    val amt=(saleAmt.toFloat() * 100).toLong()

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
                                        val jsonResp=Gson().toJson(receiptDetail)
                                        println(jsonResp)

                                        AppPreference.saveLastReceiptDetails(jsonResp) // save last sale receipt11

                                        //   detailResponse.forEach { println(it) }
                                        //  uids.add(ecrID)
                                        // defaultScope.launch { onSaveUId(ecrID, handleLoadingUIdsResult) }
                                        if (receiptDetail != null) {
                                            val batchData=BatchTable(receiptDetail)
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                //    appDao.insertBatchData(batchData)
                                                batchData.invoice= receiptDetail.invoice.toString()
                                                batchData.transactionType = com.bonushub.pax.utils.BhTransactionType.PRE_AUTH.type
                                                appDatabase.appDao.insertBatchData(batchData)
                                            }
                                            printingSaleData(receiptDetail)
                                        }


                                    }
                                    ResponseCode.FAILED.value,
                                    ResponseCode.ABORTED.value -> {
                                        errorFromIngenico(txnResponse?.responseCode,txnResponse?.status.toString())

                                    }
                                    else -> {
                                        errorFromIngenico(txnResponse?.responseCode,txnResponse?.status.toString())
                                    }
                                }
                            }
                        }
                    )
                }
                catch (exc: Exception){
                    exc.printStackTrace()
                }
            }
            EDashboardItem.PREAUTH_COMPLETE->{
                val amt=(saleAmt.toFloat() * 100).toLong()
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
                                        val jsonResp=Gson().toJson(receiptDetail)
                                        println(jsonResp)

                                        AppPreference.saveLastReceiptDetails(jsonResp) // save last sale receipt11

                                        //   detailResponse.forEach { println(it) }
                                        //  uids.add(ecrID)
                                        // defaultScope.launch { onSaveUId(ecrID, handleLoadingUIdsResult) }
                                        if (receiptDetail != null) {
                                            val batchData=BatchTable(receiptDetail)
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                //    appDao.insertBatchData(batchData)
                                                batchData.invoice= receiptDetail.invoice.toString()
                                                batchData.transactionType = com.bonushub.pax.utils.BhTransactionType.PRE_AUTH_COMPLETE.type
                                                appDatabase.appDao.insertBatchData(batchData)
                                            }
                                            printingSaleData(receiptDetail)
                                        }


                                    }
                                    ResponseCode.FAILED.value,
                                    ResponseCode.ABORTED.value -> {
                                        errorFromIngenico(txnResponse.responseCode,txnResponse.status.toString())
                                    }
                                    "03"->{
                                        errorFromIngenico(txnResponse.responseCode,txnResponse.status.toString())

                                    }
                                    else -> {
                                        errorFromIngenico(txnResponse?.responseCode,txnResponse?.status.toString())
                                    }
                                }
                            }
                        }
                    )
                }
                catch (exc: Exception){
                    exc.printStackTrace()
                }
            }

            else -> {

            }
        }
    }
    fun printingSaleData(receiptDetail: ReceiptDetail) {
        lifecycleScope.launch(Dispatchers.Main) {
            showProgress(getString(R.string.printing))
            var printsts = false
            PrintUtil(this@TransactionActivity as BaseActivityNew).startPrinting(
                receiptDetail,
                EPrintCopyType.MERCHANT,
                this@TransactionActivity as BaseActivityNew
            ) { printCB, printingFail ->

                (this@TransactionActivity as BaseActivityNew).hideProgress()
                if (printCB) {
                    printsts = printCB
                    lifecycleScope.launch(Dispatchers.Main) {
                        showMerchantAlertBox(receiptDetail)
                    }

                } else {
                    ToastUtils.showToast(this@TransactionActivity as BaseActivityNew,getString(R.string.printer_error))
                }
            }
        }
    }
    private fun showMerchantAlertBox(
        receiptDetail: ReceiptDetail
    ) {
        lifecycleScope.launch(Dispatchers.Main) {

            val printerUtil: PrintUtil? = null
            (this@TransactionActivity as BaseActivityNew).alertBoxWithAction(
                getString(R.string.print_customer_copy),
                getString(R.string.print_customer_copy),
                true, getString(R.string.positive_button_yes), { status ->
                    showProgress(getString(R.string.printing))
                    PrintUtil(this@TransactionActivity as BaseActivityNew).startPrinting(
                        receiptDetail,
                        EPrintCopyType.CUSTOMER,
                        this@TransactionActivity as BaseActivityNew
                    ) { printCB, printingFail ->
                        (this@TransactionActivity as BaseActivityNew).hideProgress()
                        if (printCB) {
                            val intent =
                                Intent(this@TransactionActivity, NavigationActivity::class.java)
                            startActivity(intent)
                        }

                    }
                }, {
                    (this@TransactionActivity as BaseActivityNew).hideProgress()
                    val intent = Intent(this@TransactionActivity, NavigationActivity::class.java)
                    startActivity(intent)
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

    fun errorFromIngenico(responseCode:String?,msg:String?){
        lifecycleScope.launch(Dispatchers.Main) {
            getInfoDialog(responseCode?:"", msg?:"Unknown Error"){
                goToDashBoard()
            }
        }


    }

    private fun goToDashBoard(){
        startActivity(Intent(this@TransactionActivity, NavigationActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
}