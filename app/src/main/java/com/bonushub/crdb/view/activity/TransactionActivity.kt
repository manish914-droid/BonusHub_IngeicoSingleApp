package com.bonushub.crdb.view.activity

import android.R.attr
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.fragment.app.viewModels
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
import com.bonushub.crdb.transactionprocess.CreateTransactionPacket
import com.bonushub.crdb.utils.*
import com.bonushub.crdb.utils.printerUtils.PrintUtil
import com.bonushub.crdb.view.base.BaseActivityNew
import com.bonushub.crdb.view.fragments.AuthCompletionData
import com.bonushub.crdb.viewmodel.SearchViewModel
import com.bonushub.crdb.viewmodel.SettlementViewModel
import com.bonushub.crdb.viewmodel.TransactionViewModel
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
import android.app.Activity

import android.R.attr.data
import com.bonushub.crdb.model.remote.BankEMITenureDataModal
import com.ingenico.hdfcpayment.type.CardCaptureType


private var bankEMIRequestCode = "4"

@AndroidEntryPoint
class TransactionActivity : BaseActivityNew(){
    private var emvBinding: ActivityEmvBinding? = null
    private val transactionAmountValue by lazy { intent.getStringExtra("amt") ?: "0" }
    private val transactionViewModel : TransactionViewModel by viewModels()
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
            globalCardProcessedModel.setTransType(transactionType)
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
                        Toast.makeText(this@TransactionActivity,"EMV mode detected",Toast.LENGTH_LONG).show()
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
    val intent = Intent (this@TransactionActivity, TenureSchemeActivity::class.java).apply {
        val field57=  "$bankEMIRequestCode^0^${brandDataMaster.brandID}^${brandEmiProductData.productID}^${imeiOrSerialNum}" +
                "^${/*cardBinValue.substring(0, 8)*/""}^$saleAmt"

        putExtra("cardProcessedData",cardProcessdatamodel)
        putExtra("brandID",brandDataMaster.brandID)
        putExtra("productID",brandEmiProductData.productID)
        putExtra("imeiOrSerialNum",imeiOrSerialNum)
        putExtra("transactionType", cardProcessdatamodel.getTransType())
    }
    startActivityForResult(intent,1)
}

                        }

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                val result =data?.getParcelableExtra<BankEMITenureDataModal>("EMITenureDataModal")
                emvBinding?.cardDetectImg?.visibility = View.GONE
                emvBinding?.tvInsertCard?.visibility = View.GONE
                emvBinding?.subHeaderView?.backImageButton?.visibility = View.GONE
                try {
                    DeviceHelper.doEMITxn(
                        EMISaleRequest(
                            amount = (saleAmt.toFloat() *100).toLong(),
                            tipAmount = 0L ?: 0,
                            emiTxnName=transactionTypeEDashboardItem.title,
                            tid = result?.txnTID,
                            cardCaptureType = CardCaptureType.EMV_NO_CAPTURING,
                            track1 = null,
                            track2 = null,
                            transactionUuid = UUID.randomUUID().toString(),
                        ),
                        listener = object : OnPaymentListener.Stub() {
                            override fun onCompleted(result: PaymentResult?) {
                                val txnResponse = result?.value as? TransactionResponse
                                val receiptDetail = txnResponse?.receiptDetail
                                receiptDetail?.txnName=transactionTypeEDashboardItem.title
                                Log.d(TAG, "Response Code: ${txnResponse?.responseCode}")
                                when (txnResponse?.responseCode) {
                                    ResponseCode.SUCCESS.value -> {
                                        val jsonResp=Gson().toJson(receiptDetail)
                                        println(jsonResp)
                                        AppPreference.saveLastReceiptDetails(jsonResp) // save last sale receipt11
                                        if (receiptDetail != null) {
                                            val batchData=BatchTable(receiptDetail)
                                            creatCardProcessingModelData(receiptDetail)
                                           /* val transactionISO =
                                                CreateTransactionPacket(globalCardProcessedModel).createTransactionPacket()

                                            val jsonResp=Gson().toJson(transactionISO)
                                            println(jsonResp)
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                transactionViewModel.serverCall(transactionISO)
                                            }
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                //    appDao.insertBatchData(batchData)
                                                batchData.invoice= receiptDetail.invoice.toString()
                                                batchData.transactionType = com.bonushub.pax.utils.BhTransactionType.SALE.type
                                                appDatabase.appDao.insertBatchData(batchData)
                                            }*/
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                //    appDao.insertBatchData(batchData)
                                                batchData.invoice= receiptDetail.invoice.toString()
                                                batchData.emiBrandData=brandDataMaster
                                                batchData.emiCategoryData=brandEmiSubCatData
                                                batchData.emiProductData=brandEmiProductData
                                                batchData.imeiOrSerialNum=imeiOrSerialNum

                                                batchData.transactionType = BhTransactionType.BRAND_EMI.type
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

                catch (ex:Exception){
                    ex.printStackTrace()
                }


            }
            if (resultCode == RESULT_CANCELED) {
                // Write  code if there's no result
            }
        }
    }



    //Below function is used to deal with EMV Card Fallback when we insert EMV Card from other side then chip side:-
   fun handleEMVFallbackFromError(title: String, msg: String, showCancelButton: Boolean, emvFromError: (Boolean) -> Unit) {
        lifecycleScope.launch(Dispatchers.Main) {
            alertBoxWithAction(title,
                msg, showCancelButton, getString(R.string.positive_button_ok), { alertCallback ->
                    if (alertCallback) {
                        emvFromError(true)
                    }
                }, {})
        }
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
                                            creatCardProcessingModelData(receiptDetail)
                                            val transactionISO =
                                                CreateTransactionPacket(globalCardProcessedModel).createTransactionPacket()

                                            val jsonResp=Gson().toJson(transactionISO)
                                            println(jsonResp)
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                transactionViewModel.serverCall(transactionISO)
                                            }
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
                                                batchData.transactionType = BhTransactionType.PRE_AUTH_COMPLETE.type
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

    fun creatCardProcessingModelData(receiptDetail: ReceiptDetail){
        globalCardProcessedModel.setProcessingCode("920001")
        receiptDetail.txnAmount?.let { globalCardProcessedModel.setTransactionAmount(it.toLong()) }
        receiptDetail.txnOtherAmount?.let { globalCardProcessedModel.setOtherAmount(it.toLong()) }
        globalCardProcessedModel.setMobileBillExtraData(Pair(mobileNumber, billNumber))
        receiptDetail.stan?.let { globalCardProcessedModel.setAuthRoc(it) }
        globalCardProcessedModel.setCardMode("0553- emv with pin")
        globalCardProcessedModel.setRrn(receiptDetail?.rrn)
        receiptDetail?.authCode?.let { globalCardProcessedModel.setAuthCode(it) }
        globalCardProcessedModel.setTid(receiptDetail?.tid)
        globalCardProcessedModel.setMid(receiptDetail?.mid)
        globalCardProcessedModel.setBatch(receiptDetail?.batchNumber)
        globalCardProcessedModel.setInvoice(receiptDetail?.invoice)
        val date = receiptDetail.dateTime
        val parts = date?.split(" ")
        globalCardProcessedModel.setDate(parts!![0])
        globalCardProcessedModel.setTime(parts[1])
        globalCardProcessedModel.setTimeStamp(receiptDetail.dateTime!!)
        globalCardProcessedModel.setPosEntryMode("0553")
    }


}