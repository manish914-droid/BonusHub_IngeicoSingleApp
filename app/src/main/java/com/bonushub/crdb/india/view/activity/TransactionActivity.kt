package com.bonushub.crdb.india.view.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.*
import androidx.lifecycle.lifecycleScope
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.ActivityEmvBinding
import com.bonushub.crdb.india.db.AppDao
import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.model.local.*
import com.bonushub.crdb.india.model.remote.*
import com.bonushub.crdb.india.transactionprocess.CreateTransactionPacketNew
import com.bonushub.crdb.india.transactionprocess.StubBatchData
import com.bonushub.crdb.india.transactionprocess.SyncReversalToHost
import com.bonushub.crdb.india.transactionprocess.SyncTransactionToHost
import com.bonushub.crdb.india.type.EmvOption
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.printerUtils.PrintUtil
import com.bonushub.crdb.india.view.base.BaseActivityNew
import com.bonushub.crdb.india.view.baseemv.SearchCard
import com.bonushub.crdb.india.viewmodel.*
import com.bonushub.crdb.india.view.baseemv.VFEmvHandler
import com.bonushub.crdb.india.vxutils.TransactionType
import com.ingenico.hdfcpayment.model.ReceiptDetail
import com.ingenico.hdfcpayment.request.*
import com.usdk.apiservice.aidl.pinpad.DeviceName
import com.usdk.apiservice.aidl.pinpad.KAPId
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class TransactionActivity : BaseActivityNew() {

    private var isToExit = false

    @Inject
    lateinit var appDao: AppDao

    private var emvBinding: ActivityEmvBinding? = null

    private val transactionProcessingCode by lazy {
        intent.getStringExtra("proc_code") ?: "92001"
    } //Just for checking purpose

    private var defaultScope = CoroutineScope(Dispatchers.Default)
    private var globalCardProcessedModel = CardProcessedDataModal()

    private val saleAmt by lazy { intent.getStringExtra("saleAmt") ?: "0" }
    private val transactionType by lazy { intent.getIntExtra("type", -1947) }
    private val mobileNumber by lazy { intent.getStringExtra("mobileNumber") ?: "" }
    private val billNumber by lazy { intent.getStringExtra("billNumber") ?: "0" }
    private val saleWithTipAmt by lazy { intent.getStringExtra("saleWithTipAmt") ?: "0" }
    private val transactionTypeEDashboardItem by lazy {
        (intent.getSerializableExtra("edashboardItem") ?: EDashboardItem.NONE) as EDashboardItem
    }
    private val cashBackAmt by lazy { intent.getStringExtra("cashBackAmt") ?: "0" }

    lateinit var testVFEmvHandler:VFEmvHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        emvBinding = ActivityEmvBinding.inflate(layoutInflater)
        //setContentView(R.layout.activity_emv)
        setContentView(emvBinding?.root)
        emvBinding?.subHeaderView?.subHeaderText?.text = transactionTypeEDashboardItem.title
        emvBinding?.txnAmtLl?.visibility = View.VISIBLE

        globalCardProcessedModel.setTransType(transactionType)
        globalCardProcessedModel.setTransactionAmount((saleAmt.toDouble() * 100).toLong())
        globalCardProcessedModel.setProcessingCode(transactionProcessingCode)


        if (transactionType == BhTransactionType.SALE_WITH_CASH.type) {
            val amt = saleAmt.toFloat() + cashBackAmt.toFloat()
            val frtAmt = "%.2f".format(amt)
            emvBinding?.baseAmtTv?.text = frtAmt
        } else {
            val frtAmt = "%.2f".format(saleAmt.toFloat())
            emvBinding?.baseAmtTv?.text = frtAmt
        }

        defaultScope.launch {
            // uemv = deviceService!!.getEMV()
            SearchCard(DeviceHelper.getEMV(), globalCardProcessedModel) { localCardProcessedData ->
                processAccordingToCardType(localCardProcessedData)

            }
        }


    }


    private fun processAccordingToCardType(cardProcessedData: CardProcessedDataModal) {
        when (cardProcessedData.getReadCardType()) {
            DetectCardType.MAG_CARD_TYPE-> {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(applicationContext,"Mag Card  detected", Toast.LENGTH_LONG).show()
                }

                //region============Below When Condition is used to check Transaction Types Based Process Execution:-
                when (cardProcessedData.getTransType()) {
                    TransactionType.SALE.type, TransactionType.PRE_AUTH.type,
                    TransactionType.REFUND.type, TransactionType.CASH_AT_POS.type,
                    TransactionType.SALE_WITH_CASH.type, TransactionType.EMI_SALE.type,
                    TransactionType.BRAND_EMI.type, TransactionType.BRAND_EMI_BY_ACCESS_CODE.type,
                    TransactionType.FLEXI_PAY.type,
                    TransactionType.TEST_EMI.type -> {
                        emvProcessNext(cardProcessedData)
//                        val emvOption = EmvOption.create().apply {
//                            flagPSE(0x00.toByte())
//                        }
//                        testVFEmvHandler = emvHandler()
//                        DeviceHelper.getEMV()?.startEMV(emvOption?.toBundle(), testVFEmvHandler)
                    }
                    else -> {
                    }
                }
                //endregion

            }

            DetectCardType.EMV_CARD_TYPE-> {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(applicationContext,"EMV card contact detected", Toast.LENGTH_LONG).show()
                    val emvOption = EmvOption.create().apply {
                        flagPSE(0x00.toByte())
                    }
                    testVFEmvHandler = emvHandler()
                    DeviceHelper.getEMV()?.startEMV(emvOption?.toBundle(), testVFEmvHandler)

                }

            }

            DetectCardType.CONTACT_LESS_CARD_TYPE -> {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(applicationContext,"Contactless detected", Toast.LENGTH_LONG).show()
                    val emvOption = EmvOption.create().apply {
                        flagPSE(0x01.toByte())
                    }

                    testVFEmvHandler = emvHandler()
                    DeviceHelper.getEMV()?.startEMV(emvOption?.toBundle(), testVFEmvHandler)
                }
            }

            else -> {

            }
        }
    }

    //region========================================Below Method is a Handler for EMV CardType:-
    private fun emvHandler(): VFEmvHandler {
        println("DoEmv VfEmvHandler is calling")
        println("IEmv value is" + DeviceHelper.getEMV().toString())
        return VFEmvHandler(DeviceHelper.getPinpad(KAPId(0, 0), 0, DeviceName.IPP),DeviceHelper.getEMV(),this@TransactionActivity,globalCardProcessedModel) { cardProcessedData ->
          //  transactionCallback(cardProcessedData)
            emvProcessNext(cardProcessedData)
            Log.d("Track2Data:- ", cardProcessedData.getTrack2Data() ?: "")
            Log.d("PanNumber:- ", cardProcessedData.getPanNumberData() ?: "")
        }
    }
    //endregion

    // Creating transaction packet and
    private fun emvProcessNext(cardProcessedData: CardProcessedDataModal) {
        val transactionISO = CreateTransactionPacketNew(appDao,cardProcessedData,BatchTable()).createTransactionPacketNew()
        cardProcessedData.indicatorF58 = transactionISO.additionalData["indicatorF58"] ?: ""

        // logger("Transaction REQUEST PACKET --->>", transactionISO.isoMap, "e")
        //  runOnUiThread { showProgress(getString(R.string.sale_data_sync)) }
        GlobalScope.launch(Dispatchers.IO) {
            checkReversal(transactionISO, cardProcessedData)
        }
    }

    private fun checkReversal(transactionISOByteArray: IsoDataWriter, cardProcessedDataModal: CardProcessedDataModal) {
        runOnUiThread {
           // cardView_l.visibility = View.GONE
        }
        // If case Sale data sync to server
        Log.e("1REVERSAL obj ->",""+AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))
        println(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))
        val reversalObj = AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY)
        println(reversalObj)
        println("AppPreference.getReversal()"+AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))
        if (TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
            println("sale_data_sync")
            val msg: String = getString(R.string.sale_data_sync)
            runOnUiThread { showProgress(msg) }
            SyncTransactionToHost(transactionISOByteArray, cardProcessedDataModal, testVFEmvHandler) { syncStatus, responseCode, transactionMsg, printExtraData, de55, doubletap ->
                hideProgress()

                if (syncStatus) {
                    val responseIsoData: IsoDataReader = readIso(transactionMsg.toString(), false)
                    val autoSettlementCheck = responseIsoData.isoMap[60]?.parseRaw2String().toString()
                    if (syncStatus && responseCode == "00" && !AppPreference.getBoolean(AppPreference.ONLINE_EMV_DECLINED)) {
                        //Below we are saving batch data and print the receipt of transaction:-

                              GlobalScope.launch(Dispatchers.Main) {
                                  Toast.makeText(this@TransactionActivity,"TXn approved",Toast.LENGTH_SHORT).show()
                              }

                        StubBatchData(
                            de55,
                            cardProcessedDataModal.getTransType(),
                            cardProcessedDataModal,
                            printExtraData,
                            autoSettlementCheck
                        ) { stubbedData ->
                            /*if (cardProcessedDataModal.getTransType() == TransactionType.EMI_SALE.type ||
                                cardProcessedDataModal.getTransType() == TransactionType.BRAND_EMI.type ||
                                cardProcessedDataModal.getTransType() == TransactionType.BRAND_EMI_BY_ACCESS_CODE.type ||
                                cardProcessedDataModal.getTransType() == TransactionType.FLEXI_PAY.type ||
                                cardProcessedDataModal.getTransType() == TransactionType.TEST_EMI.type

                            ) {

                                stubEMI(stubbedData, emiSelectedData, emiTAndCData, brandEMIAccessData,flexiPayemiSelectedData) { data ->
                                    Log.d("StubbedEMIData:- ", data.toString())

                                    modal=  saveBrandEMIDataToDB(brandEMIData, data.hostInvoice,data.hostTID)
                                    saveBrandEMIbyCodeDataInDB(
                                        brandEMIAccessData,
                                        data.hostInvoice,data.hostTID
                                    )
                                    val modal2=runBlocking(Dispatchers.IO) {
                                        BrandEMIDataTable.getBrandEMIDataByInvoiceAndTid(data.hostInvoice,data.hostTID)
                                    }
                                    if (modal2 != null) {
                                        modal=modal2
                                    }

                                    printSaveSaleEmiDataInBatch(data) { printCB ->
                                        if (!printCB) {
                                            Log.e("EMI FIRST ", "COMMENT ******")
                                            // Here we are Syncing Txn CallBack to server
                                            if(tpt?.digiPosCardCallBackRequired=="1") {
                                                lifecycleScope.launch(Dispatchers.IO) {
                                                    withContext(Dispatchers.Main) {
                                                        showProgress(
                                                            getString(
                                                                R.string.txn_syn
                                                            )
                                                        )
                                                    }

                                                    val amount = MoneyUtil.fen2yuan(
                                                        stubbedData.totalAmmount.toDouble()
                                                            .toLong()
                                                    )
                                                    val txnCbReqData = TxnCallBackRequestTable()
                                                    txnCbReqData.reqtype =
                                                        EnumDigiPosProcess.TRANSACTION_CALL_BACK.code
                                                    txnCbReqData.tid = stubbedData.hostTID
                                                    txnCbReqData.batchnum =
                                                        stubbedData.hostBatchNumber
                                                    txnCbReqData.roc = stubbedData.hostRoc
                                                    txnCbReqData.amount = amount

                                                    txnCbReqData.ecrSaleReqId=stubbedData.ecrTxnSaleRequestId
                                                    txnCbReqData.txnTime = stubbedData.time
                                                    txnCbReqData.txnDate = stubbedData.transactionDate
                                                    TxnCallBackRequestTable.insertOrUpdateTxnCallBackData(
                                                        txnCbReqData
                                                    )
                                                    syncTxnCallBackToHost {
                                                        Log.e(
                                                            "TXN CB ",
                                                            "SYNCED TO SERVER  --> $it"
                                                        )
                                                        hideProgress()
                                                    }
                                                    Log.e("EMI LAST", "COMMENT ******")

                                                    //Here we are Syncing Offline Sale if we have any in Batch Table and also Check Sale Response has Auto Settlement enabled or not:-
                                                    //If Auto Settlement Enabled Show Pop Up and User has choice whether he/she wants to settle or not:-

                                                    if (!TextUtils.isEmpty(autoSettlementCheck)) {
                                                        withContext(Dispatchers.Main) {
                                                            syncOfflineSaleAndAskAutoSettlement(
                                                                autoSettlementCheck.substring(
                                                                    0,
                                                                    1
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            else{
                                                if (!TextUtils.isEmpty(autoSettlementCheck)) {
                                                    GlobalScope.launch(Dispatchers.Main) {
                                                        syncOfflineSaleAndAskAutoSettlement(
                                                            autoSettlementCheck.substring(0, 1)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            else {*/
                                printAndSaveBatchDataInDB(stubbedData) { printCB ->
                                    if (printCB) {
                                        AppPreference.saveLastReceiptDetails(stubbedData)
                                        Log.e("FIRST ", "COMMENT ******")
                                        goToDashBoard()
                                        // Here we are Syncing Txn CallBack to server

                                        /*if(tpt?.digiPosCardCallBackRequired=="1" || AppPreference.getBoolean(AppPreference.IsECRon)) {
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                withContext(Dispatchers.Main) {
                                                    showProgress(
                                                        getString(
                                                            R.string.txn_syn
                                                        )
                                                    )
                                                }
                                                val amount = MoneyUtil.fen2yuan(
                                                    stubbedData.totalAmmount.toDouble().toLong()
                                                )
                                                val txnCbReqData = TxnCallBackRequestTable()
                                                txnCbReqData.reqtype = EnumDigiPosProcess.TRANSACTION_CALL_BACK.code
                                                txnCbReqData.tid = stubbedData.hostTID
                                                txnCbReqData.batchnum = stubbedData.hostBatchNumber
                                                txnCbReqData.roc = stubbedData.hostRoc
                                                txnCbReqData.amount = amount
                                                txnCbReqData.ecrSaleReqId=stubbedData.ecrTxnSaleRequestId
                                                txnCbReqData.txnTime = stubbedData.time
                                                txnCbReqData.txnDate = stubbedData.transactionDate
                                                //    20220302 145902

                                                TxnCallBackRequestTable.insertOrUpdateTxnCallBackData(txnCbReqData)

                                                syncTxnCallBackToHost {
                                                    Log.e(
                                                        "TXN CB ",
                                                        "SYNCED TO SERVER  --> $it"
                                                    )
                                                    hideProgress()
                                                }
                                                Log.e("LAST ", "COMMENT ******")

                                                //Here we are Syncing Offline Sale if we have any in Batch Table and also Check Sale Response has Auto Settlement enabled or not:-
                                                //If Auto Settlement Enabled Show Pop Up and User has choice whether he/she wants to settle or not:-

                                                if (!TextUtils.isEmpty(autoSettlementCheck)) {
                                                    withContext(Dispatchers.Main) {
                                                        syncOfflineSaleAndAskAutoSettlement(
                                                            autoSettlementCheck.substring(0, 1)
                                                        )
                                                    }
                                                }
                                            }
                                        }else{
                                            if (!TextUtils.isEmpty(autoSettlementCheck)) {
                                                GlobalScope.launch(Dispatchers.Main) {
                                                    syncOfflineSaleAndAskAutoSettlement(
                                                        autoSettlementCheck.substring(0, 1)
                                                    )
                                                }
                                            }
                                        }*/
                                    }
                                }
                          //  }


                        }

                    }
                    else{
                        GlobalScope.launch(Dispatchers.Main) {
                            alertBoxWithAction(
                                getString(R.string.transaction_delined_msg),
                                responseIsoData.isoMap[58]?.parseRaw2String().toString(),
                                false,
                                getString(R.string.positive_button_ok),
                                { alertPositiveCallback ->
                                    if (alertPositiveCallback) {
                                        if (!TextUtils.isEmpty(autoSettlementCheck)) {
                                            // syncOfflineSaleAndAskAutoSettlement(autoSettlementCheck.substring(0, 1))
                                        } else {

                                        }
                                    }
                                },
                                {})
                        }
                        //Condition for having a reversal(EMV CASE)
                    }


                }

            }
        }
        else {
            println("410")
            if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
                println("412")
                runOnUiThread { showProgress(getString(R.string.reversal_data_sync)) }
                SyncReversalToHost(AppPreference.getReversalNew()) { isSyncToHost, transMsg ->
                    hideProgress()
                    if (isSyncToHost) {
                        AppPreference.clearReversal()
                        checkReversal(transactionISOByteArray, cardProcessedDataModal)
                        println("clearReversal -> check again")
                    } else {
                        GlobalScope.launch(Dispatchers.Main) {
                            //  VFService.showToast(transMsg)
                            alertBoxWithAction(
                                getString(R.string.reversal_upload_fail),
                                getString(R.string.transaction_delined_msg),
                                false,
                                getString(R.string.positive_button_ok),
                                { alertPositiveCallback ->
                                    if (alertPositiveCallback)
                                        declinedTransaction()
                                },
                                {})


                        }
                    }
                }
            }else{
                println("442")
            }
        }
        //Else case is to Sync Reversal data Packet to Host:-

    }

    //Below method is used to handle Transaction Declined case:-
    fun declinedTransaction() {
        try {
            DeviceHelper.getEMV()?.stopSearch()
            finish()
            startActivity(Intent(this, NavigationActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        } catch (ex: java.lang.Exception) {
            finish()
            startActivity(Intent(this, NavigationActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }



    override fun onBackPressed() {
        exitApp()

          }

    private fun exitApp() {
        if (isToExit) {
            super.finishAffinity()
        } else {
            isToExit = true
            Handler(Looper.getMainLooper()).postDelayed({
                isToExit = false
                Toast.makeText(this, "Double click back button to exit.", Toast.LENGTH_SHORT).show()

            }, 1000)
        }
    }
    //endregion


    //Below method is used to save sale data in batch file data table and print the receipt of it:-
    private fun printAndSaveBatchDataInDB(
        stubbedData: TempBatchFileDataTable,
        cb: suspend (Boolean) -> Unit
    ) {
        // printerReceiptData will not be saved in Batch if transaction is pre auth
       // if (transactionType != TransactionTypeValues.PRE_AUTH) {
            //Here we are saving printerReceiptData in BatchFileData Table:-
        Utility().saveTempBatchFileDataTable(stubbedData)
       // } //kushal

       /* PrintUtil(this).startPrinting(
            stubbedData,
            EPrintCopyType.MERCHANT,
            this
        ) { dialogCB, printingFail ->
            Log.d("Sale Printer Status:- ", printingFail.toString())
            if (printingFail == 0)
                runOnUiThread {
                    alertBoxWithAction(
                        getString(R.string.printer_error),
                        getString(R.string.printing_roll_empty_msg),
                        false,
                        getString(R.string.positive_button_ok),
                        {
                            cb(dialogCB, false)
                            *//* startActivity(
                                 Intent(
                                     this@VFTransactionActivity,
                                     MainActivity::class.java
                                 ).apply {
                                     flags =
                                         Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                 })*//*
                        },
                        {})
                }
            else
                cb(dialogCB, true)
        }*/

        lifecycleScope.launch(Dispatchers.Main) {
            showProgress(getString(R.string.printing))
            var printsts = false
            if (stubbedData != null) {
                PrintUtil(this@TransactionActivity as BaseActivityNew).startPrinting(
                    stubbedData,
                    EPrintCopyType.MERCHANT,
                    this@TransactionActivity as BaseActivityNew
                ) { printCB, printingFail ->

                    (this@TransactionActivity as BaseActivityNew).hideProgress()
                    if (printCB) {
                        printsts = printCB
                        lifecycleScope.launch(Dispatchers.Main) {
                            showMerchantAlertBox(stubbedData, cb)
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
        batchTable: TempBatchFileDataTable,
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

    private fun goToDashBoard() {
        startActivity(Intent(this@TransactionActivity, NavigationActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

   }