package com.bonushub.crdb.india.view.fragments

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentVoidMainBinding
import com.bonushub.crdb.india.db.AppDao
import com.bonushub.crdb.india.di.DBModule
import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.model.local.AppPreference
import com.bonushub.crdb.india.model.local.BatchTable
import com.bonushub.crdb.india.model.local.PendingSyncTransactionTable
import com.bonushub.crdb.india.repository.GenericResponse
import com.bonushub.crdb.india.transactionprocess.CreateTransactionPacket
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.dialog.DialogUtilsNew1

import com.bonushub.crdb.india.utils.printerUtils.PrintUtil
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.activity.NavigationActivity.Companion.TAG
import com.bonushub.crdb.india.view.base.BaseActivityNew
import com.bonushub.crdb.india.viewmodel.BatchFileViewModel
import com.bonushub.crdb.india.viewmodel.PendingSyncTransactionViewModel
import com.bonushub.crdb.india.viewmodel.TransactionViewModel
import com.bonushub.crdb.india.utils.BhTransactionType
import com.bonushub.crdb.india.utils.CardEntryMode
import com.bonushub.crdb.india.utils.EPrintCopyType
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getTptData
import com.bonushub.crdb.india.utils.ProcessingCode
import com.google.gson.Gson
import com.ingenico.hdfcpayment.listener.OnPaymentListener
import com.ingenico.hdfcpayment.model.ReceiptDetail
import com.ingenico.hdfcpayment.request.VoidRequest
import com.ingenico.hdfcpayment.response.PaymentResult
import com.ingenico.hdfcpayment.response.TransactionResponse
import com.ingenico.hdfcpayment.type.ResponseCode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
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
            //doVoidTransaction()
            searchTransaction()

            // (activity as NavigationActivity).transactFragment(VoidDetailFragment())
        }
    }

    private fun doVoidTransaction(oldBatchData:BatchTable){
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
            batchFileViewModel.getBatchTableDataListByInvoice(invoiceWithPadding(invoice)).observe(viewLifecycleOwner) { allbat ->

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

            }
        }

    }

    private fun voidTransConfirmationDialog(batchTable: BatchTable) {
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

    private fun voidTransInvoicesDialog(voidData: ArrayList<BatchTable>) {
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
            voidTransConfirmationDialog(it)
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
}