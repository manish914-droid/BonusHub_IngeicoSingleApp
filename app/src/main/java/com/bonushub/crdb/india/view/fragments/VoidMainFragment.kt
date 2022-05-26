package com.bonushub.crdb.india.view.fragments

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.india.HDFCApplication
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentVoidMainBinding
import com.bonushub.crdb.india.db.AppDao
import com.bonushub.crdb.india.di.DBModule
import com.bonushub.crdb.india.disputetransaction.CreateVoidPacket
import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.model.local.AppPreference
import com.bonushub.crdb.india.model.local.BatchTable
import com.bonushub.crdb.india.model.local.TempBatchFileDataTable
import com.bonushub.crdb.india.transactionprocess.SyncReversalToHost
import com.bonushub.crdb.india.transactionprocess.SyncVoidTransactionToHost
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getTptData
import com.bonushub.crdb.india.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.india.utils.printerUtils.PrintUtil
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.base.BaseActivityNew
import com.bonushub.crdb.india.viewmodel.BatchFileViewModel
import com.bonushub.crdb.india.viewmodel.PendingSyncTransactionViewModel
import com.bonushub.crdb.india.viewmodel.TransactionViewModel
import com.bonushub.crdb.india.vxutils.TransactionType
import com.google.gson.Gson
import com.ingenico.hdfcpayment.model.ReceiptDetail
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class VoidMainFragment : Fragment() {
    @Inject
    lateinit var appDao: AppDao
    private var globalCardProcessedModel = CardProcessedDataModal()

    var binding:FragmentVoidMainBinding? = null
    private val batchFileViewModel: BatchFileViewModel by viewModels()
    private val pendingSyncTransactionViewModel: PendingSyncTransactionViewModel by viewModels()


    // private val transactionAmountValue by lazy { intent.getStringExtra("amt") ?: "0" }
    private val transactionViewModel: TransactionViewModel by viewModels()
    private var tid ="000000"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
         binding = FragmentVoidMainBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as NavigationActivity).manageTopToolBar(false)

        // set header
        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.void_sale)
        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_void)
        globalCardProcessedModel.setTransType(BhTransactionType.VOID.type)

        lifecycleScope.launch(Dispatchers.IO) {
            tid=  getBaseTID(DBModule.appDatabase.appDao)
        }

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }

        binding?.txtViewSearchTransaction?.setOnClickListener {
            logger("txtViewSearchTransaction","click")
            when {
                TextUtils.isEmpty(binding?.edtTextSearchTransaction?.text.toString().trim()) -> ToastUtils.showToast( requireContext(), getString(R.string.invoice_number_should_not_be_empty))
                else -> {
                    if (binding?.edtTextSearchTransaction?.text.isNullOrBlank()) {
                        ToastUtils.showToast(requireContext(),"Enter Invoice")
                    } else {
                        //voidRefundBT?.isEnabled = true
                       // showConfirmation(binding?.edtTextSearchTransaction?.text.toString())
                        searchTransaction()
                    }
                }
            }

            //doVoidTransaction()
            //searchTransaction()

            // (activity as NavigationActivity).transactFragment(VoidDetailFragment())
        }
    }

    // old
   /* private fun doVoidTransaction(oldBatchData:BatchTable){
        var ecrID: String
        try {
            DeviceHelper.doVoidTransaction(
                VoidRequest(
                    tid = oldBatchData.receiptData?.tid,
                    invoice =  binding?.edtTextSearchTransaction?.text.toString(),
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
                                val jsonResp= Gson().toJson(receiptDetail)
                                println(jsonResp)
                                val tpt = runBlocking(Dispatchers.IO) {
                                   getTptData()
                                }
                                if (receiptDetail != null) {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        val batchData =BatchTable(receiptDetail)
                                        batchData.invoice = receiptDetail.invoice.toString()
                                        batchData.transactionType = BhTransactionType.VOID.type
                                        //To assign bonushub batchnumber,bonushub invoice,bonuhub stan

                                     // todo old data update in VOID txn
                                        batchData.bonushubbatchnumber = oldBatchData.bonushubbatchnumber
                                            ?: ""
                                        batchData.bonushubInvoice     = oldBatchData.bonushubInvoice
                                            ?: ""
                                        batchData.bonushubStan        = tpt?.stan.toString()
                                        batchData.oldStanForVoid=oldBatchData.bonushubStan ?: ""
                                        batchData.oldDateTimeInVoid= oldBatchData.receiptData?.dateTime.toString()
                                    batchData.field57EncryptedData=oldBatchData.field57EncryptedData
batchData.field58EmiData=oldBatchData.field58EmiData
                                        DBModule.appDatabase.appDao.insertBatchData(batchData)
                                        AppPreference.saveLastReceiptDetails(batchData)

                                        //To increment base Stan
                                        Utility().incrementUpdateRoc()

                                        printingSaleData(batchData) {
                                            withContext(Dispatchers.Main) {
                                                (activity as BaseActivityNew).showProgress(
                                                    getString(
                                                        R.string.transaction_syncing_msg
                                                    )
                                                )
                                            }
                                            createCardProcessingModelData(receiptDetail)
                                            val transactionISO =
                                                CreateTransactionPacket(appDao,globalCardProcessedModel,batchData).createTransactionPacket()
                                            // sync pending transaction
                                               Utility().syncPendingTransaction(transactionViewModel){}

                                            when (val genericResp =
                                                transactionViewModel.serverCall(transactionISO)) {
                                                is GenericResponse.Success -> {
                                                    withContext(Dispatchers.Main) {
                                                        logger(
                                                            "success:- ",
                                                            "in success $genericResp",
                                                            "e"
                                                        )
                                                        (activity as BaseActivityNew).hideProgress()
                                                        startActivity(Intent(activity, NavigationActivity::class.java).apply {
                                                            flags =
                                                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                        })
                                                    }
                                                }
                                                is GenericResponse.Error -> {
                                                    logger("error:- ", "in error ${genericResp.errorMessage}", "e")
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
                                                        (activity as BaseActivityNew).hideProgress()
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
                                                        (activity as BaseActivityNew).hideProgress()
                                                    }
                                                }
                                            }

                                        }
                                    }
                                }


                            }
                            ResponseCode.FAILED.value,
                            ResponseCode.ABORTED.value -> {

                                startActivity(Intent(activity, NavigationActivity::class.java).apply {
                                    flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                })
                            }
                            else -> {
                                startActivity(Intent(activity, NavigationActivity::class.java).apply {
                                    flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                })
                            }
                        }
                    }
                }
            )
        }
        catch (exc: Exception){
            exc.printStackTrace()
        }
    }*/

    // kushal this is call aidl method to executed txn
    /*private fun doVoidTransaction(oldBatchData:TempBatchFileDataTable){
        var ecrID: String
        try {
            DeviceHelper.doVoidTransaction(
                VoidRequest(
                    tid = oldBatchData.tid,
                    invoice =  binding?.edtTextSearchTransaction?.text.toString(),
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
                                val jsonResp= Gson().toJson(receiptDetail)
                                println(jsonResp)
                                val tpt = runBlocking(Dispatchers.IO) {
                                   getTptData()
                                }
                                if (receiptDetail != null) {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        val batchData =BatchTable(receiptDetail)
                                        batchData.invoice = receiptDetail.invoice.toString()
                                        batchData.transactionType = BhTransactionType.VOID.type
                                        //To assign bonushub batchnumber,bonushub invoice,bonuhub stan

                                     // todo old data update in VOID txn
                                        batchData.bonushubbatchnumber = oldBatchData.bonushubbatchnumber
                                            ?: ""
                                        batchData.bonushubInvoice     = oldBatchData.bonushubInvoice
                                            ?: ""
                                        batchData.bonushubStan        = tpt?.stan.toString()
                                        batchData.oldStanForVoid=oldBatchData.bonushubStan ?: ""
                                        batchData.oldDateTimeInVoid= oldBatchData.receiptData?.dateTime.toString()
                                    batchData.field57EncryptedData=oldBatchData.field57EncryptedData
batchData.field58EmiData=oldBatchData.field58EmiData
                                        DBModule.appDatabase.appDao.insertBatchData(batchData)
                                        AppPreference.saveLastReceiptDetails(batchData)

                                        //To increment base Stan
                                        Utility().incrementUpdateRoc()

                                        printingSaleData(batchData) {
                                            withContext(Dispatchers.Main) {
                                                (activity as BaseActivityNew).showProgress(
                                                    getString(
                                                        R.string.transaction_syncing_msg
                                                    )
                                                )
                                            }
                                            createCardProcessingModelData(receiptDetail)
                                            val transactionISO =
                                                CreateTransactionPacket(appDao,globalCardProcessedModel,batchData).createTransactionPacket()
                                            // sync pending transaction
                                               Utility().syncPendingTransaction(transactionViewModel){}

                                            when (val genericResp =
                                                transactionViewModel.serverCall(transactionISO)) {
                                                is GenericResponse.Success -> {
                                                    withContext(Dispatchers.Main) {
                                                        logger(
                                                            "success:- ",
                                                            "in success $genericResp",
                                                            "e"
                                                        )
                                                        (activity as BaseActivityNew).hideProgress()
                                                        startActivity(Intent(activity, NavigationActivity::class.java).apply {
                                                            flags =
                                                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                        })
                                                    }
                                                }
                                                is GenericResponse.Error -> {
                                                    logger("error:- ", "in error ${genericResp.errorMessage}", "e")
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
                                                        (activity as BaseActivityNew).hideProgress()
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
                                                        (activity as BaseActivityNew).hideProgress()
                                                    }
                                                }
                                            }

                                        }
                                    }
                                }


                            }
                            ResponseCode.FAILED.value,
                            ResponseCode.ABORTED.value -> {

                                startActivity(Intent(activity, NavigationActivity::class.java).apply {
                                    flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                })
                            }
                            else -> {
                                startActivity(Intent(activity, NavigationActivity::class.java).apply {
                                    flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                })
                            }
                        }
                    }
                }
            )
        }
        catch (exc: Exception){
            exc.printStackTrace()
        }
    }*/

    private fun onContinueClicked(voidData: TempBatchFileDataTable) {
        //Sync Reversal
        if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
            logger("goto ","Sync Reversal","e")
            if((activity as NavigationActivity).isShowProgress()){
                (activity as NavigationActivity).setProgressTitle(getString(R.string.reversal_data_sync))
            }else {
                activity?.runOnUiThread { (activity as NavigationActivity).showProgress(getString(R.string.reversal_data_sync)) }
            }
            SyncReversalToHost(AppPreference.getReversalNew()) { isSyncToHost, transMsg ->
                (activity as NavigationActivity).hideProgress()
                if (isSyncToHost) {
                    AppPreference.clearReversal()
                    onContinueClicked(voidData)
                } else {
                    activity?.runOnUiThread {
                         ToastUtils.showToast(requireContext(),transMsg)

                        startActivity(Intent(requireContext(), NavigationActivity::class.java).apply {
                            flags =
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    }
                }
            }
        } else {
            //Sync Main Transaction(VOID transaction)
            if (TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
                GlobalScope.launch {
                    delay(1000)
                    //**** Creating void packet and send to server ****
                    VoidHelper(
                        activity as NavigationActivity, voidData
                    ) { code, respnosedatareader, msg ->
                        GlobalScope.launch(Dispatchers.Main) {
                            //voidRefundBT?.isEnabled = true
                            when (code) {
                                0 -> {
                                    if (msg.isNotEmpty()) Toast.makeText(
                                        activity as Context,
                                        respnosedatareader?.isoMap?.get(58)?.parseRaw2String()
                                            .toString(),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    /*ROCProviderV2.incrementFromResponse(
                                        ROCProviderV2.getRoc(
                                            AppPreference.getBankCode()
                                        ).toString(), AppPreference.getBankCode()
                                    )*/

                                    /*GlobalScope.launch(Dispatchers.Main) {
                                        val autoSettlementCheck =
                                            respnosedatareader?.isoMap?.get(60)?.parseRaw2String()
                                                .toString()
                                        if (!TextUtils.isEmpty(autoSettlementCheck))
                                            syncOfflineSaleAndAskAutoSettlement(
                                                autoSettlementCheck.substring(
                                                    0,
                                                    1
                                                )
                                            )
                                        else {
                                            startActivity(
                                                Intent(
                                                    (activity as BaseActivity),
                                                    NavigationActivity::class.java
                                                ).apply {
                                                    flags =
                                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                })
                                        }
                                    }*/

                                    startActivity(
                                        Intent(
                                            requireActivity(),
                                            NavigationActivity::class.java
                                        ).apply {
                                            flags =
                                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        })
                                }
                                //  Success case
                                1 -> {
                                    // Txn Approved Dialog kushal
                                    //   println("Index and batchListsize is" + index + " and " + " batch " + (batchList.size - 1))
//                                    activity?.runOnUiThread {
//                                        txnSuccessToast(activity as NavigationActivity)
//                                    }

                                    if (respnosedatareader != null) {
                                        // respnosedatareader.isoMap[62]?.parseRaw2String()?.let {
                                        /*  deleteBatchTableDataInDBWithInvoiceNumber(
                                                                        it
                                                                    )*/
                                        val autoSettlementCheck =
                                            respnosedatareader.isoMap.get(60)?.parseRaw2String()
                                                .toString()

                                        val f60DataList = autoSettlementCheck.split('|')
                                        //   Auto settle flag | Bank id| Issuer id | MID | TID | Batch No | Stan | Invoice | Card Type
                                        //0|1|51|000000041501002|41501369|000150|260|000260|RUPAY|
                                        val date: Long = Calendar.getInstance().timeInMillis
                                        val timeFormater =
                                            SimpleDateFormat("HHmmss", Locale.getDefault())
                                        voidData.time = (timeFormater.format(date))
                                        val dateFormater =
                                            SimpleDateFormat("MMdd", Locale.getDefault())
                                        voidData.date = (dateFormater.format(date))
                                        try {
                                            voidData.hostBankID = f60DataList[1]
                                            voidData.hostIssuerID = f60DataList[2]
                                            voidData.hostMID = f60DataList[3]
                                            voidData.hostTID = f60DataList[4]
                                            voidData.hostBatchNumber = f60DataList[5]
                                            voidData.hostRoc = f60DataList[6]
                                            voidData.hostInvoice = f60DataList[7]
                                            voidData.hostCardType = f60DataList[8]
                                            // batchFileData
                                        } catch (ex: Exception) {
                                            ex.printStackTrace()
                                            //batchFileData
                                        }

                                        if (voidData.transactionType == TransactionType.REFUND.type) {
                                            voidData.transactionType = TransactionType.VOID_REFUND.type
                                            voidData.transationName = getTransactionTypeName(TransactionType.VOID_REFUND.type)?:""
                                        } else if ((voidData.transactionType == TransactionType.EMI_SALE.type || voidData.transactionType == TransactionType.BRAND_EMI_BY_ACCESS_CODE.type || voidData.transactionType == TransactionType.BRAND_EMI.type)&& voidData.tenure != "1") {
                                            voidData.transactionType = TransactionType.VOID_EMI.type
                                            voidData.transationName = getTransactionTypeName(TransactionType.VOID_EMI.type)?:""
                                        } else {
                                            if(voidData.tenure=="1"&& voidData.transactionType==TransactionType.SALE.type){
                                                voidData.transactionType = TransactionType.VOID.type
                                                voidData.txnType2=TransactionType.VOID.type
                                                voidData.transationName = getTransactionTypeName(TransactionType.VOID.type)?:""

                                            }else{
                                                voidData.transactionType = TransactionType.VOID.type
                                                voidData.transationName = getTransactionTypeName(TransactionType.VOID.type)?:""
                                                }
                                        }

                                        voidData?.tenure = "0"

                                        voidData.referenceNumber =
                                            (respnosedatareader.isoMap[37]?.parseRaw2String()
                                                ?: "").replace(" ", "")
                                        // kushal
                                        /*voidData.roc =
                                            ROCProviderV2.getRoc(AppPreference.getBankCode())
                                                .toString()
                                        ROCProviderV2.incrementFromResponse(ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(), AppPreference.getBankCode())
*/
                                        logger("save",""+ voidData.transationName)
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            batchFileViewModel.deleteTempBatchFileDataTableFromInvoice(voidData.hostInvoice, voidData.hostTID)
                                            batchFileViewModel?.insertTempBatchFileDataTable(voidData)
                                        }
                                        //BatchFileDataTable.deleteBatchRecord(voidData.hostInvoice,voidData.hostTID)
                                        //BatchFileDataTable.performOperation(voidData)

                                        // Saving for Last Success Receipt
                                        val lastSuccessReceiptData = Gson().toJson(voidData)
                                        AppPreference.saveLastReceiptDetails(lastSuccessReceiptData)

                                        // kushal
                                        val amt = (((voidData.transactionalAmmount)?.toDouble())?.div(100)).toString()
                                        val transactionDate = dateFormaterNew(voidData.timeStamp ?: 0L)
                                        val transactionTime = timeFormaterNew(voidData.time)
                                        (activity as NavigationActivity).txnApprovedDialog(EDashboardItem.VOID_SALE.res,EDashboardItem.VOID_SALE.title,
                                            amt,"${transactionDate}, ${transactionTime}") {
                                                status , dialog ->

                                            (activity as NavigationActivity).showProgress(getString(R.string.printing))

                                            PrintUtil(activity).startPrinting(
                                                voidData,
                                                EPrintCopyType.MERCHANT,
                                                requireContext()
                                            ) { printCB, printingFail ->

                                                (activity as NavigationActivity).hideProgress()

                                                lifecycleScope.launch(Dispatchers.Main){
                                                    if (printCB) {

                                                        (activity as NavigationActivity).alertBoxWithActionNew(
                                                            "",
                                                            getString(R.string.print_customer_copy),
                                                            R.drawable.ic_print_customer_copy,
                                                            "yes", "no",true,false ,{

                                                                (activity as NavigationActivity).showProgress(getString(R.string.printing))

                                                                PrintUtil(activity).startPrinting(
                                                                    voidData,
                                                                    EPrintCopyType.CUSTOMER,
                                                                    requireContext()
                                                                ) { printCB, printingFail ->
                                                                    (activity as NavigationActivity).hideProgress()
                                                                    // go to dashboard

                                                                    dialog.dismiss()
                                                                    startActivity(Intent(requireActivity(), NavigationActivity::class.java).apply {
                                                                        flags =
                                                                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                                    })


                                                                }
                                                            }, {

                                                                (activity as NavigationActivity).hideProgress()
//                    val intent = Intent(this@TransactionActivity, NavigationActivity::class.java)
//                    startActivity(intent)
                                                                // go to dashboard
                                                                dialog.dismiss()
                                                                startActivity(Intent(requireActivity(), NavigationActivity::class.java).apply {
                                                                    flags =
                                                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                                })

                                                            })

                                                        // not need now
                                                        /*val tpt=TerminalParameterTable.selectFromSchemeTable()
                                                        if(tpt?.digiPosCardCallBackRequired=="1") {
                                                            lifecycleScope.launch(Dispatchers.IO) {
                                                                withContext(Dispatchers.Main) {
                                                                    (activity as MainActivity).showProgress(
                                                                        getString(
                                                                            R.string.txn_syn
                                                                        )
                                                                    )
                                                                }
                                                                val amount = MoneyUtil.fen2yuan(
                                                                    voidData.totalAmmount.toDouble().toLong()
                                                                )
                                                                val txnCbReqData = TxnCallBackRequestTable()
                                                                txnCbReqData.reqtype =
                                                                    EnumDigiPosProcess.TRANSACTION_CALL_BACK.code
                                                                txnCbReqData.tid = voidData.hostTID
                                                                txnCbReqData.batchnum = voidData.hostBatchNumber
                                                                txnCbReqData.roc = voidData.hostRoc
                                                                txnCbReqData.amount = amount

                                                                txnCbReqData.ecrSaleReqId=voidData.ecrTxnSaleRequestId
                                                                txnCbReqData.txnTime = voidData.time
                                                                txnCbReqData.txnDate = voidData.transactionDate
                                                                txnCbReqData.txnType= TransactionType.VOID.type

                                                                TxnCallBackRequestTable.insertOrUpdateTxnCallBackData(
                                                                    txnCbReqData
                                                                )
                                                                syncTxnCallBackToHost {
                                                                    Log.e(
                                                                        "TXN CB ",
                                                                        "SYNCED TO SERVER  --> $it"
                                                                    )
                                                                    (activity as MainActivity).hideProgress()
                                                                }
                                                                Log.e("VOID LAST", "COMMENT ******")

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

                                            }

                                        }


                                    }
                                }
                                2 -> {
                                    // kushal
                                    //checkForPrintReversalReceipt(activity, "") {
                                    //}
                                }
                            }
                        }
                    }.start()

                }
            }
        }
    }

    internal class VoidHelper(
        val context: Activity,
        val batch: TempBatchFileDataTable,
        private val callback: (Int, IsoDataReader?, String) -> Unit
    ) {
        companion object

        val TAG = VoidHelper::class.java.simpleName
        fun start() {
            GlobalScope.launch {
                val transactionISO = CreateVoidPacket(batch).createVoidISOPacket()
                //logger1("Transaction REQUEST PACKET --->>", transactionISO.generateIsoByteRequest(), "e")

                if((context as NavigationActivity).isShowProgress()){
                    (context as NavigationActivity).setProgressTitle((context).getString(R.string.sale_data_sync))
                }else {
                    (context as NavigationActivity).runOnUiThread {
                        (context).showProgress(
                            (context).getString(
                                R.string.sale_data_sync
                            )
                        )

                    }
                }


                GlobalScope.launch(Dispatchers.Main) {
                    sendVoidTransToHost(transactionISO)
                }
            }
        }

        private fun sendVoidTransToHost(transactionISOByteArray: IsoDataWriter) {

            if (TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
                //  (context as MainActivity).showProgress((context).getString(R.string.please_wait_offline_sale_sync))
                SyncVoidTransactionToHost(
                    transactionISOByteArray,
                    cardProcessedDataModal = CardProcessedDataModal()
                ) { syncStatus, responseCode, transactionMsg, printExtraData ->
                    (context as NavigationActivity).hideProgress()
                    if (syncStatus) {
                        if (syncStatus && responseCode == "00") {
                            try {
                                val responseIsoData: IsoDataReader =
                                    readIso(transactionMsg.toString(), false)
                                batch.isVoid = true
                                //   batch.isChecked = false
                                AppPreference.clearReversal()
                                callback(
                                    1,
                                    responseIsoData,
                                    responseIsoData.isoMap[39]?.parseRaw2String().toString()
                                )
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                                callback(1, null, "")
                            }
                        } else if (syncStatus && responseCode != "00") {
                            GlobalScope.launch(Dispatchers.Main) {
                                //      VFService.showToast("$responseCode ------> $transactionMsg")
                                try {
                                    val responseIsoData: IsoDataReader =
                                        readIso(transactionMsg.toString(), false)
                                    callback(
                                        0,
                                        responseIsoData,
                                        responseIsoData.isoMap[39]?.parseRaw2String().toString()
                                    )
                                    AppPreference.clearReversal()
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                    callback(1, null, "")
                                }
                            }
                        } else {
                            (context).runOnUiThread {
                                (context).hideProgress()
                                callback(2, null, "")
                            }
                        }
                    } else {
                        (context).runOnUiThread {
                            (context).hideProgress()
                            ToastUtils.showToast(HDFCApplication.appContext,"No Internet Available , Please check your Internet and try again")
                            callback(2, null, "")
                        }
                        //  val responseIsoData: IsoDataReader = readIso(transactionMsg, false)
                        //   callback(0, IsoDataReader(), "")
                    }
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

    private fun searchTransaction()
    {
        val invoice = binding?.edtTextSearchTransaction?.text.toString()


        lifecycleScope.launch {
            batchFileViewModel.getTempBatchTableDataListByInvoice(invoiceWithPadding(invoice)).observe(viewLifecycleOwner) { allbat ->

                logger("TempBatchFileDataTable", allbat.size.toString(), "e")
                var voidData: TempBatchFileDataTable? = null
                val bat: ArrayList<TempBatchFileDataTable> = arrayListOf()
                if (allbat != null) {
                    for (i in allbat) {
                        if (i?.transactionType == BhTransactionType.SALE.type ||
                            i?.transactionType == BhTransactionType.EMI_SALE.type ||
                            i?.transactionType == BhTransactionType.REFUND.type ||
                            i?.transactionType == BhTransactionType.SALE_WITH_CASH.type ||
                            i?.transactionType == BhTransactionType.CASH_AT_POS.type ||
                            i?.transactionType == BhTransactionType.TIP_SALE.type ||
                            i?.transactionType == BhTransactionType.TEST_EMI.type ||
                            i?.transactionType == BhTransactionType.BRAND_EMI.type ||
                            i?.transactionType == BhTransactionType.BRAND_EMI_BY_ACCESS_CODE.type||
                            i?.transactionType == BhTransactionType.PRE_AUTH.type
                        )
                            bat.add(i)
                    }

                    val tpt = getTptData()
                    when (bat.size) {
                        0 -> {

                            CoroutineScope(Dispatchers.Main).launch {
                                (activity as? NavigationActivity)?.alertBoxWithActionNew("Error", getString(R.string.no_data_found) ?: "",R.drawable.ic_info_orange,"Ok","",false,false,{},{})
                            }
                        }
                        1 -> {
                            voidData = bat.first()
                            if (voidData?.transactionType == BhTransactionType.REFUND.type && tpt?.voidRefund != "1") {
                                ToastUtils.showToast(
                                    requireContext(),
                                    getString(R.string.void_refund_not_allowed)
                                )
                            } else {

                                lifecycleScope.launch(Dispatchers.Main) {
                                    voidTransConfirmationDialog(voidData) // void txn
                                }
                            }
                        }
                        else -> {
                            lifecycleScope.launch(Dispatchers.Main) {
                                voidTransInvoicesDialog(bat as ArrayList<TempBatchFileDataTable>) // kushal
                            }
                        }
                    }

                }

            }

            /*batchFileViewModel.getBatchTableDataListByInvoice(invoiceWithPadding(invoice)).observe(viewLifecycleOwner) { allbat ->

                logger("batchTable", allbat.size.toString(), "e")
                var voidData: BatchTable? = null
                val bat: ArrayList<BatchTable> = arrayListOf()
                if (allbat != null) {
                    for (i in allbat) {
                        if (i?.transactionType == BhTransactionType.SALE.type ||
                            i?.transactionType == BhTransactionType.EMI_SALE.type ||
                            i?.transactionType == BhTransactionType.REFUND.type ||
                            i?.transactionType == BhTransactionType.SALE_WITH_CASH.type ||
                            i?.transactionType == BhTransactionType.CASH_AT_POS.type ||
                            i?.transactionType == BhTransactionType.TIP_SALE.type ||
                            i?.transactionType == BhTransactionType.TEST_EMI.type ||
                            i?.transactionType == BhTransactionType.BRAND_EMI.type ||
                            i?.transactionType == BhTransactionType.BRAND_EMI_BY_ACCESS_CODE.type||
                            i?.transactionType == BhTransactionType.PRE_AUTH.type
                        )
                            bat.add(i)
                    }

                    val tpt = getTptData()
                    when (bat.size) {
                        0 -> {

                            CoroutineScope(Dispatchers.Main).launch {
                                (activity as? NavigationActivity)?.getInfoDialog("Error", getString(R.string.no_data_found) ?: "") {}
                            }
                        }
                        1 -> {
                            voidData = bat.first()
                            if (voidData?.transactionType == BhTransactionType.REFUND.type && tpt?.voidRefund != "1") {
                                ToastUtils.showToast(
                                    requireContext(),
                                    getString(R.string.void_refund_not_allowed)
                                )
                            } else {

                                lifecycleScope.launch(Dispatchers.Main) {
                                    voidTransConfirmationDialog(voidData)
                                }
                            }
                        }
                        else -> {
                            lifecycleScope.launch(Dispatchers.Main) {
                                voidTransInvoicesDialog(bat as ArrayList<BatchTable>)
                            }
                        }
                    }

                }

            }*/
        }

    }

    // old
    /*private fun voidTransConfirmationDialog(batchTable: BatchTable) {
        if(batchTable.receiptData != null) {
                    val date = batchTable.receiptData?.dateTime ?: ""
                    val parts = date.split(" ")
                    println("Date: " + parts[0])
                    println("Time: " + (parts[1]) )
                    val amt ="%.2f".format((((batchTable.receiptData?.txnAmount ?: "").toDouble()).div(100)).toString().toDouble())
                    DialogUtilsNew1.showVoidSaleDetailsDialog(
                        requireContext(),
                        parts[0],
                        parts[1],
                        batchTable.receiptData?.tid ?: "",
                        batchTable.receiptData?.invoice ?: "",
                        amt
                    ) {
                        doVoidTransaction(batchTable)
                    }
                }else{
                    ToastUtils.showToast(requireContext(),"Data not found.")
                }
    }*/

    private fun voidTransConfirmationDialog(batchTable: TempBatchFileDataTable) {
        if(batchTable != null) {

                    val amt ="%.2f".format((((batchTable?.transactionalAmmount ?: "").toDouble()).div(100)).toString().toDouble())
                    /*DialogUtilsNew1.showVoidSaleDetailsDialog(
                        requireContext(),
                        batchTable.transactionDate,
                        batchTable.time,
                        batchTable.tid ?: "",
                        batchTable.invoiceNumber ?: "",
                        amt
                    ) {
                       // doVoidTransaction(batchTable) // kushal
                        onContinueClicked(batchTable)
                    }*/

            DialogUtilsNew1.showDetailsConfirmDialog(requireContext(), transactionType = BhTransactionType.VOID,
                tid = batchTable.tid, totalAmount = amt, invoice = batchTable.invoiceNumber, date = batchTable.transactionDate, time = batchTable.time,
                amount = null, batchNo = null, roc = null,
                confirmCallback = {
                                  it.dismiss()
                    (activity as NavigationActivity).alertBoxWithActionNew("","Do you want to Void Sale this transaction?"
                        ,R.drawable.ic_info_orange,"OK","Cancel",true,false,{

                            activity?.runOnUiThread { (activity as NavigationActivity).showProgress(getString(R.string.processing)) }
                            onContinueClicked(batchTable)

                        },{
                        })
                },
                cancelCallback = {
                    it.dismiss()
                })

                }else{
                    ToastUtils.showToast(requireContext(),"Data not found.")
                }
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
    suspend fun errorOnSyncing(msg:String){
        withContext(Dispatchers.Main) {
            (activity as BaseActivityNew). alertBoxWithAction(
                getString(R.string.no_receipt),
                msg,
                false,
                getString(R.string.positive_button_ok),
                {
                    (activity as NavigationActivity).transactFragment(DashboardFragment())
                },
                {})
        }


    }

    // old
    /*private fun voidTransInvoicesDialog(voidData: ArrayList<BatchTable>) {
        val dialog = Dialog(requireActivity())
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.recyclerview_layout)

        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        val window = dialog.window
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        val rv = dialog.findViewById<RecyclerView>(R.id.recycler_view)
        val adptr = VoidTxnAdapter(voidData) {
            dialog.hide()
            //voidTransConfirmationDialog(it) // kushal
        }
        rv.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = adptr
        }
        dialog.show()
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }*/

    private fun voidTransInvoicesDialog(voidData: ArrayList<TempBatchFileDataTable>) {
        val dialog = Dialog(requireActivity())
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.recyclerview_layout)

        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        val window = dialog.window
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        val rv = dialog.findViewById<RecyclerView>(R.id.recycler_view)
        val adptr = VoidTxnAdapter(voidData) {
            dialog.hide()
            voidTransConfirmationDialog(it) // kushal
        }
        rv.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = adptr
        }
        dialog.show()
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
}

class VoidTxnAdapter(
    var batchData: ArrayList<TempBatchFileDataTable>,
    var cb: (TempBatchFileDataTable) -> Unit
) :
    RecyclerView.Adapter<VoidTxnAdapter.VoidTxnViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoidTxnViewHolder {
        return VoidTxnViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_void_invoices_data, parent, false)
        )
    }

    override fun onBindViewHolder(holder: VoidTxnViewHolder, position: Int) {
        val voidData = batchData[position]
        holder.tidView.text = "TID : " + voidData?.tid
        holder.invoiceView.text = "INVOICE : " + voidData.hostInvoice
        holder.voidView.setOnClickListener {
            //  VFService.showToast("CLICKED  $position")
            cb(voidData)
        }
    }

    override fun getItemCount(): Int = batchData.size


    class VoidTxnViewHolder(var v: View) : RecyclerView.ViewHolder(v) {
        var tidView = v.findViewById<TextView>(R.id.txn_tid_tv)
        var invoiceView = v.findViewById<TextView>(R.id.txn_invoice_tv)
        var voidView = v.findViewById<CardView>(R.id.voidTxnLL)

    }
}

// old
/*
class VoidTxnAdapter(
    var batchData: ArrayList<BatchTable>,
    var cb: (BatchTable) -> Unit
) :
    RecyclerView.Adapter<VoidTxnAdapter.VoidTxnViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoidTxnViewHolder {
        return VoidTxnViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_void_invoices_data, parent, false)
        )
    }

    override fun onBindViewHolder(holder: VoidTxnViewHolder, position: Int) {
        val voidData = batchData[position]
        holder.tidView.text = "TID : " + voidData?.receiptData?.tid
        holder.invoiceView.text = "INVOICE : " + voidData.bonushubInvoice
        holder.voidView.setOnClickListener {
            //  VFService.showToast("CLICKED  $position")
            cb(voidData)
        }
    }

    override fun getItemCount(): Int = batchData.size


    class VoidTxnViewHolder(var v: View) : RecyclerView.ViewHolder(v) {
        var tidView = v.findViewById<TextView>(R.id.txn_tid_tv)
        var invoiceView = v.findViewById<TextView>(R.id.txn_invoice_tv)
        var voidView = v.findViewById<CardView>(R.id.voidTxnLL)

    }
}*/
