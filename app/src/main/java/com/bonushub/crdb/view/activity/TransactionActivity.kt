package com.bonushub.crdb.view.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.bonushub.crdb.HDFCApplication
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.ActivityEmvBinding
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.db.AppDatabase
import com.bonushub.crdb.di.DBModule
import com.bonushub.crdb.di.DBModule.appDatabase
import com.bonushub.crdb.model.local.BatchTable
import com.bonushub.crdb.model.local.BrandEMISubCategoryTable
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.model.remote.BrandEMIProductDataModal
import com.bonushub.crdb.utils.DeviceHelper
import com.bonushub.crdb.utils.ToastUtils
import com.bonushub.crdb.utils.printerUtils.PrintUtil
import com.bonushub.crdb.view.base.BaseActivityNew
import com.bonushub.crdb.viewmodel.SearchViewModel
import com.bonushub.pax.utils.EDashboardItem
import com.bonushub.pax.utils.EPrintCopyType
import com.bonushub.pax.utils.VxEvent
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

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

    private val mobileNumber by lazy { intent.getStringExtra("mobileNumber") ?: "" }

    private val billNumber by lazy { intent.getStringExtra("billNumber") ?: "0" }
    private val saleWithTipAmt by lazy { intent.getStringExtra("saleWithTipAmt") ?: "0" }
    private val title by lazy { intent.getStringExtra("title") }
    private val transactionType by lazy { intent.getIntExtra("type", -1947) }
    private val  transactionTypeEDashboardItem by lazy{ (intent.getSerializableExtra("edashboardItem") ?: EDashboardItem.NONE) as EDashboardItem}
    val TAG = TransactionActivity::class.java.simpleName

    private val searchCardViewModel : SearchViewModel by viewModels()

    private val tid by lazy {
"30160035"

    }
    //  private lateinit var deviceService: UsdkDeviceService
   /* @Inject
    lateinit var appDao: AppDao*/
    private val appDao: AppDatabase = AppDatabase.getInstance(HDFCApplication.appContext)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        emvBinding = ActivityEmvBinding.inflate(layoutInflater)
        setContentView(emvBinding?.root)

        setupFlow()
        //searchCardViewModel.fetchCardTypeData()


    }

    override fun onEvents(event: VxEvent) {
        TODO("Not yet implemented")
    }

    private fun setupObserver() {
        searchCardViewModel.allcadType.observe(this, Observer { cardProcessdatamodel  ->
            when(cardProcessdatamodel.getReadCardType()){
                DetectCardType.EMV_CARD_TYPE -> {
                    Toast.makeText(this,"EMV mode detected",Toast.LENGTH_LONG).show()
                    searchCardViewModel.fetchCardPanData()
                    setupEMVObserver()
                }
                DetectCardType.CONTACT_LESS_CARD_TYPE -> {
                    Toast.makeText(this,"Contactless mode detected",Toast.LENGTH_LONG).show()
                }
                DetectCardType.MAG_CARD_TYPE -> {
                    Toast.makeText(this,"Swipe mode detected",Toast.LENGTH_LONG).show()
                    searchCardViewModel.fetchCardPanData()
                    setupEMVObserver()
                }
                else -> {

                }
            }
        })
    }

    private fun setupEMVObserver() {
       searchCardViewModel.cardTpeData.observe(this, Observer { cardProcessedDataModal ->
           if(cardProcessedDataModal.getPanNumberData() !=null) {
               cardProcessedDataModal.getPanNumberData()
                var ecrID: String
             /*   try {
                    DeviceHelper.doSaleTransaction(
                        SaleRequest(
                            amount = 300L ?: 0,
                            tipAmount = 0L ?: 0,
                            transactionType = TransactionType.SALE,
                            tid = "30160035",
                            transactionUuid = UUID.randomUUID().toString().also {
                                ecrID = it

                            }
                        ),
                        listener = object : OnPaymentListener.Stub() {
                            override fun onCompleted(result: PaymentResult?) {
                                val txnResponse = result?.value as? TransactionResponse
                                val detailResponse = txnResponse?.receiptDetail
                                    .toString()
                                    .split(",")
                                Log.d(TAG, "Response Code: ${txnResponse?.responseCode}")
                                when (txnResponse?.responseCode) {
                                    ResponseCode.SUCCESS.value -> {
                                        detailResponse.forEach { println(it) }
                                        //  uids.add(ecrID)
                                        // defaultScope.launch { onSaveUId(ecrID, handleLoadingUIdsResult) }
                                    }
                                    ResponseCode.FAILED.value,
                                    ResponseCode.ABORTED.value -> {
                                        detailResponse.forEach { println(it) }
                                    }
                                    else -> println("Error")
                                }
                            }
                        }
                    )
                }
                catch (exc: Exception){
                    exc.printStackTrace()
                }*/

              /*  DeviceHelper.showAdminFunction(object: OnOperationListener.Stub(){
                    override fun onCompleted(p0: OperationResult?) {
                        p0?.value?.apply {
                            println("Status = $status")
                            println("Response code = $responseCode")
                        }
                    }
                })*/

                Toast.makeText(
                    this,
                    cardProcessedDataModal.getPanNumberData().toString(),
                    Toast.LENGTH_LONG
                ).show()

               val intent = Intent (this, TenureSchemeActivity::class.java)
               startActivity(intent)

               /*  lifecycleScope.launch(Dispatchers.IO) {
                    // serverRepository.getEMITenureData(cardProcessedDataModal.getEncryptedPan().toString())
                     serverRepository.getEMITenureData("B1DFEFE944EE27E9B78136F34C3EB5EE2B891275D5942360")
                 }*/

            }

        })
    }

    private  fun setupFlow(){
        emvBinding?.baseAmtTv?.text=saleAmt

        when(transactionTypeEDashboardItem){
            EDashboardItem.BRAND_EMI->{
                searchCardViewModel.fetchCardTypeData()
                setupObserver()
              /*  val intent = Intent (this, TenureSchemeActivity::class.java)
                startActivity(intent)*/
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
                                   /* .toString()
                                    .split(",")*/
                                Log.d(TAG, "Response Code: ${txnResponse?.responseCode}")
                                when (txnResponse?.responseCode) {
                                    ResponseCode.SUCCESS.value -> {
                                        val jsonResp=Gson().toJson(receiptDetail)
                                        println(jsonResp)
                                     //   detailResponse.forEach { println(it) }
                                        //  uids.add(ecrID)
                                        // defaultScope.launch { onSaveUId(ecrID, handleLoadingUIdsResult) }
                                        if (receiptDetail != null) {
                                            val batchData=BatchTable(receiptDetail)
                                            lifecycleScope.launch(Dispatchers.IO) {
                                            //    appDao.insertBatchData(batchData)
                                                batchData.invoice= receiptDetail.invoice.toString()
                                                DBModule.appDatabase.appDao.insertBatchData(batchData)
                                            }
                                            printingSaleData(receiptDetail)
                                        }


                                    }
                                    ResponseCode.FAILED.value,
                                    ResponseCode.ABORTED.value -> {
                                      //  detailResponse.forEach { println(it) }
                                       /* if (receiptDetail != null) {
                                            val jsonstr="{\"aid\":\"A0000000041010\",\"appName\":\"Debit MasterCard\",\"authCode\":\"006538\",\"batchNumber\":\"000001\",\"cardHolderName\":\"INSTA DEBIT CARD         /\",\"cardType\":\"UP        \",\"cvmRequiredLimit\":0,\"cvmResult\":\"NO_CVM\",\"dateTime\":\"24/11/2021 14:49:00\",\"entryMode\":\"INSERT\",\"invoice\":\"000012\",\"isSignRequired\":false,\"isVerifyPin\":true,\"merAddHeader1\":\"INGBH TEST2 TID\",\"merAddHeader2\":\"NOIDA\",\"mid\":\"               \",\"rrn\":\"000000000381\",\"stan\":\"000381\",\"tc\":\"1DF19BD576739835\",\"tid\":\"30160035\",\"tsi\":\"E800\",\"tvr\":\"0840048000\",\"txnAmount\":\"5888\",\"txnName\":\"SALE\",\"txnResponseCode\":\"00\"}"
                                           val obj=Gson().fromJson(jsonstr,ReceiptDetail::class.java)
                                           startPrinting(obj)
                                           *//* val intent=Intent(this@TransactionActivity,PrintingTesting::class.java)
                                            startActivity(intent)*//*

                                        }*/
                                    }
                                    else -> {
                                        val intent = Intent (this@TransactionActivity, NavigationActivity::class.java)
                                          startActivity(intent)

                                        println("Error")}
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
                                        //   detailResponse.forEach { println(it) }
                                        //  uids.add(ecrID)
                                        // defaultScope.launch { onSaveUId(ecrID, handleLoadingUIdsResult) }
                                        if (receiptDetail != null) {
                                            val batchData=BatchTable(receiptDetail)
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                //    appDao.insertBatchData(batchData)
                                                batchData.invoice= receiptDetail.invoice.toString()
                                                DBModule.appDatabase.appDao.insertBatchData(batchData)
                                            }
                                            printingSaleData(receiptDetail)
                                        }


                                    }
                                    ResponseCode.FAILED.value,
                                    ResponseCode.ABORTED.value -> {
                                        //  detailResponse.forEach { println(it) }
                                        /* if (receiptDetail != null) {
                                             val jsonstr="{\"aid\":\"A0000000041010\",\"appName\":\"Debit MasterCard\",\"authCode\":\"006538\",\"batchNumber\":\"000001\",\"cardHolderName\":\"INSTA DEBIT CARD         /\",\"cardType\":\"UP        \",\"cvmRequiredLimit\":0,\"cvmResult\":\"NO_CVM\",\"dateTime\":\"24/11/2021 14:49:00\",\"entryMode\":\"INSERT\",\"invoice\":\"000012\",\"isSignRequired\":false,\"isVerifyPin\":true,\"merAddHeader1\":\"INGBH TEST2 TID\",\"merAddHeader2\":\"NOIDA\",\"mid\":\"               \",\"rrn\":\"000000000381\",\"stan\":\"000381\",\"tc\":\"1DF19BD576739835\",\"tid\":\"30160035\",\"tsi\":\"E800\",\"tvr\":\"0840048000\",\"txnAmount\":\"5888\",\"txnName\":\"SALE\",\"txnResponseCode\":\"00\"}"
                                            val obj=Gson().fromJson(jsonstr,ReceiptDetail::class.java)
                                            startPrinting(obj)
                                             val intent=Intent(this@TransactionActivity,PrintingTesting::class.java)
                                            startActivity(intent)

                                        }*/
                                    }
                                    else -> {
                                        val intent = Intent (this@TransactionActivity, NavigationActivity::class.java)
                                        startActivity(intent)

                                        println("Error")}
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
                                        //   detailResponse.forEach { println(it) }
                                        //  uids.add(ecrID)
                                        // defaultScope.launch { onSaveUId(ecrID, handleLoadingUIdsResult) }
                                        if (receiptDetail != null) {
                                            val batchData=BatchTable(receiptDetail)
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                //    appDao.insertBatchData(batchData)
                                                batchData.invoice= receiptDetail.invoice.toString()
                                                DBModule.appDatabase.appDao.insertBatchData(batchData)
                                            }
                                            printingSaleData(receiptDetail)
                                        }


                                    }
                                    ResponseCode.FAILED.value,
                                    ResponseCode.ABORTED.value -> {
                                        //  detailResponse.forEach { println(it) }
                                        /* if (receiptDetail != null) {
                                             val jsonstr="{\"aid\":\"A0000000041010\",\"appName\":\"Debit MasterCard\",\"authCode\":\"006538\",\"batchNumber\":\"000001\",\"cardHolderName\":\"INSTA DEBIT CARD         /\",\"cardType\":\"UP        \",\"cvmRequiredLimit\":0,\"cvmResult\":\"NO_CVM\",\"dateTime\":\"24/11/2021 14:49:00\",\"entryMode\":\"INSERT\",\"invoice\":\"000012\",\"isSignRequired\":false,\"isVerifyPin\":true,\"merAddHeader1\":\"INGBH TEST2 TID\",\"merAddHeader2\":\"NOIDA\",\"mid\":\"               \",\"rrn\":\"000000000381\",\"stan\":\"000381\",\"tc\":\"1DF19BD576739835\",\"tid\":\"30160035\",\"tsi\":\"E800\",\"tvr\":\"0840048000\",\"txnAmount\":\"5888\",\"txnName\":\"SALE\",\"txnResponseCode\":\"00\"}"
                                            val obj=Gson().fromJson(jsonstr,ReceiptDetail::class.java)
                                            startPrinting(obj)
                                             val intent=Intent(this@TransactionActivity,PrintingTesting::class.java)
                                            startActivity(intent)

                                        }*/
                                    }
                                    else -> {
                                        val intent = Intent (this@TransactionActivity, NavigationActivity::class.java)
                                        startActivity(intent)

                                        println("Error")}
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
                                        //   detailResponse.forEach { println(it) }
                                        //  uids.add(ecrID)
                                        // defaultScope.launch { onSaveUId(ecrID, handleLoadingUIdsResult) }
                                        if (receiptDetail != null) {
                                            val batchData=BatchTable(receiptDetail)
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                //    appDao.insertBatchData(batchData)
                                                batchData.invoice= receiptDetail.invoice.toString()
                                                DBModule.appDatabase.appDao.insertBatchData(batchData)
                                            }
                                            printingSaleData(receiptDetail)
                                        }


                                    }
                                    ResponseCode.FAILED.value,
                                    ResponseCode.ABORTED.value -> {
                                        //  detailResponse.forEach { println(it) }
                                        /* if (receiptDetail != null) {
                                             val jsonstr="{\"aid\":\"A0000000041010\",\"appName\":\"Debit MasterCard\",\"authCode\":\"006538\",\"batchNumber\":\"000001\",\"cardHolderName\":\"INSTA DEBIT CARD         /\",\"cardType\":\"UP        \",\"cvmRequiredLimit\":0,\"cvmResult\":\"NO_CVM\",\"dateTime\":\"24/11/2021 14:49:00\",\"entryMode\":\"INSERT\",\"invoice\":\"000012\",\"isSignRequired\":false,\"isVerifyPin\":true,\"merAddHeader1\":\"INGBH TEST2 TID\",\"merAddHeader2\":\"NOIDA\",\"mid\":\"               \",\"rrn\":\"000000000381\",\"stan\":\"000381\",\"tc\":\"1DF19BD576739835\",\"tid\":\"30160035\",\"tsi\":\"E800\",\"tvr\":\"0840048000\",\"txnAmount\":\"5888\",\"txnName\":\"SALE\",\"txnResponseCode\":\"00\"}"
                                            val obj=Gson().fromJson(jsonstr,ReceiptDetail::class.java)
                                            startPrinting(obj)
                                             val intent=Intent(this@TransactionActivity,PrintingTesting::class.java)
                                            startActivity(intent)

                                        }*/
                                    }
                                    else -> {
                                        val intent = Intent (this@TransactionActivity, NavigationActivity::class.java)
                                        startActivity(intent)

                                        println("Error")}
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
                                        //   detailResponse.forEach { println(it) }
                                        //  uids.add(ecrID)
                                        // defaultScope.launch { onSaveUId(ecrID, handleLoadingUIdsResult) }
                                        if (receiptDetail != null) {
                                            val batchData=BatchTable(receiptDetail)
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                //    appDao.insertBatchData(batchData)
                                                batchData.invoice= receiptDetail.invoice.toString()
                                                DBModule.appDatabase.appDao.insertBatchData(batchData)
                                            }
                                            printingSaleData(receiptDetail)
                                        }


                                    }
                                    ResponseCode.FAILED.value,
                                    ResponseCode.ABORTED.value -> {
                                        //  detailResponse.forEach { println(it) }
                                        /* if (receiptDetail != null) {
                                             val jsonstr="{\"aid\":\"A0000000041010\",\"appName\":\"Debit MasterCard\",\"authCode\":\"006538\",\"batchNumber\":\"000001\",\"cardHolderName\":\"INSTA DEBIT CARD         /\",\"cardType\":\"UP        \",\"cvmRequiredLimit\":0,\"cvmResult\":\"NO_CVM\",\"dateTime\":\"24/11/2021 14:49:00\",\"entryMode\":\"INSERT\",\"invoice\":\"000012\",\"isSignRequired\":false,\"isVerifyPin\":true,\"merAddHeader1\":\"INGBH TEST2 TID\",\"merAddHeader2\":\"NOIDA\",\"mid\":\"               \",\"rrn\":\"000000000381\",\"stan\":\"000381\",\"tc\":\"1DF19BD576739835\",\"tid\":\"30160035\",\"tsi\":\"E800\",\"tvr\":\"0840048000\",\"txnAmount\":\"5888\",\"txnName\":\"SALE\",\"txnResponseCode\":\"00\"}"
                                            val obj=Gson().fromJson(jsonstr,ReceiptDetail::class.java)
                                            startPrinting(obj)
                                             val intent=Intent(this@TransactionActivity,PrintingTesting::class.java)
                                            startActivity(intent)

                                        }*/
                                    }
                                    else -> {
                                        val intent = Intent (this@TransactionActivity, NavigationActivity::class.java)
                                        startActivity(intent)

                                        println("Error")}
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
            (this@TransactionActivity as BaseActivityNew).showProgress(getString(R.string.printing))
            val printerUtil: PrintUtil? = null
            (this@TransactionActivity as BaseActivityNew).alertBoxWithAction(
                getString(R.string.print_customer_copy),
                getString(R.string.print_customer_copy),
                true, getString(R.string.positive_button_yes), { status ->
                    PrintUtil(this@TransactionActivity as BaseActivityNew).startPrinting(
                        receiptDetail,
                        EPrintCopyType.CUSTOMER,
                        this@TransactionActivity as BaseActivityNew
                    ) { printCB, printingFail ->
                        if (printCB) {
                            val intent =
                                Intent(this@TransactionActivity, NavigationActivity::class.java)
                            startActivity(intent)
                        }

                    }
                }, {
                    val intent = Intent(this@TransactionActivity, NavigationActivity::class.java)
                    startActivity(intent)
                })
        }
    }
    enum class DetectCardType(val cardType: Int, val cardTypeName: String = "") {
        CARD_ERROR_TYPE(0),
        MAG_CARD_TYPE(1, "Mag"),
        EMV_CARD_TYPE(2, "Chip"),
        CONTACT_LESS_CARD_TYPE(3, "CTLS"),
        CONTACT_LESS_CARD_WITH_MAG_TYPE(4, "CTLS"),
        MANUAL_ENTRY_TYPE(5, "MAN")
    }

}