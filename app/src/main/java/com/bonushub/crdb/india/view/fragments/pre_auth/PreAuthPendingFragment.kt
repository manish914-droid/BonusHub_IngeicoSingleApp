package com.bonushub.crdb.india.view.fragments.pre_auth

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentPreAuthPendingBinding
import com.bonushub.crdb.india.databinding.ItemPendingPreauthBinding
import com.bonushub.crdb.india.databinding.ItemPreAuthPendingBinding
import com.bonushub.crdb.india.transactionprocess.StubBatchData
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.printerUtils.PrintUtil
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.base.BaseActivityNew
import com.bonushub.crdb.india.view.base.IDialog
import com.bonushub.crdb.india.view.fragments.AuthCompletionData
import com.bonushub.crdb.india.viewmodel.PreAuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class PreAuthPendingFragment : Fragment() {

    private var iDialog: IDialog? = null
    var binding:FragmentPreAuthPendingBinding? = null

    private val authData: AuthCompletionData by lazy { AuthCompletionData() }

    lateinit var preAuthViewModel : PreAuthViewModel
    lateinit var pendingPreAuthDataResponse: PendingPreAuthDataResponse

    var updatedPosition:Int = 0
    lateinit var mAdapter : PreAuthPendingAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPreAuthPendingBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        iDialog = (activity as NavigationActivity)

        binding?.subHeaderView?.subHeaderText?.text = "PENDING PREAUTH"
        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_preauth_submenu)
        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }


        preAuthViewModel = ViewModelProvider(this).get(PreAuthViewModel::class.java) //


        lifecycleScope.launch(Dispatchers.IO){

            preAuthViewModel.getPendingPreAuthData()

        }
        preAuthViewModel.pendingPreAuthData?.observe(viewLifecycleOwner){ pendingPreAuthData ->

            when(pendingPreAuthData.apiStatus){

                ApiStatus.Success ->{
                    logger("ApiStatus ->","Success")
                    iDialog?.hideProgress()
                    pendingPreAuthDataResponse = pendingPreAuthData
                    setUpRecyclerView()
                }

                ApiStatus.Processing ->{
                    logger("ApiStatus ->","Processing")
                    iDialog?.showProgress(pendingPreAuthData.msg?:"Getting Pending Pre-Auth From Server")
                }

                ApiStatus.Failed -> {
                    logger("ApiStatus ->", "Failed")
                    iDialog?.hideProgress()


                    iDialog?.alertBoxWithActionNew(
                        getString(R.string.error_hint),
                        pendingPreAuthData.msg ?: "",
                        R.drawable.ic_info_new,
                        getString(R.string.positive_button_ok),
                        "", false, false,
                        { alertPositiveCallback ->
                            if (alertPositiveCallback) {

                                gotoDashboard()
                                // 1105
                                /*if (!TextUtils.isEmpty(autoSettlementCheck))
                                       syncOfflineSaleAndAskAutoSettlement(
                                           autoSettlementCheck.substring(
                                               0,
                                               1
                                           ), context as BaseActivityNew
                                       )*/
                            }
                        },
                        {})
                }


                ApiStatus.Nothing ->{
                    logger("ApiStatus ->","Nothing")
                }
            }

        }


        binding?.btnPrint?.setOnClickListener {

            if(pendingPreAuthDataResponse.pendingList.size > 0 ){
                iDialog?.showProgress(getString(R.string.printing))
                PrintUtil(activity).printPendingPreAuth(activity, pendingPreAuthDataResponse.pendingList){
                    printCb,_ ->

                    (activity as BaseActivityNew).hideProgress()

                    if(!printCb){
                        iDialog?.alertBoxWithActionNew("Alert","Printing error!",R.drawable.ic_init_next,"OK","",false,false,{},{})
                    }
                }
            }else{
                iDialog?.alertBoxWithActionNew("","Pending Pre-Auth not available.",R.drawable.ic_init_next,"OK","",false,false,{},{})
            }
        }

        preAuthViewModel.completePreAuthData.observe(viewLifecycleOwner){
            when(it.apiStatus){

                ApiStatus.Success ->{
                    logger("ApiStatus","Success","e")
                    iDialog?.hideProgress()
                    dialogBuilder.hide()
                    // stub batch data

                    StubBatchData("", it.cardProcessedDataModal.getTransType(), it.cardProcessedDataModal, null, it.isoResponse?:""){
                            stubbedData ->

                        Utility().saveTempBatchFileDataTable(stubbedData)

                        val transactionDate = dateFormaterNew(it.cardProcessedDataModal.getTimeStamp()?.toLong() ?: 0L)
                        val transactionTime = timeFormaterNew(it.cardProcessedDataModal.getTime()?:"")

                        var amt = ""
                        try {
                            amt = (((stubbedData.transactionalAmmount)?.toDouble())?.div(100)).toString()
                            amt = "%.2f".format(amt.toDouble())
                        }catch (ex:Exception){
                            amt = "0.00"
                        }

                        iDialog?.txnApprovedDialog(EDashboardItem.PENDING_PREAUTH.res,EDashboardItem.PENDING_PREAUTH.title,amt,
                            "${transactionDate}, ${transactionTime}") {

                            lifecycleScope.launch(Dispatchers.IO) {
                                withContext(Dispatchers.Main){
                                    printChargeSlip((activity as BaseActivityNew), EPrintCopyType.MERCHANT,stubbedData) { it ->


                                        Handler(Looper.getMainLooper()).postDelayed({

                                            (activity as NavigationActivity).alertBoxWithActionNew("",
                                                getString(R.string.print_customer_copy),
                                                R.drawable.ic_print_customer_copy,
                                                getString(R.string.positive_button_yes),
                                                getString(R.string.no),
                                                true,
                                                false,
                                                {
                                                    lifecycleScope.launch(Dispatchers.IO)
                                                    {
                                                        withContext(Dispatchers.Main){
                                                            printChargeSlip((activity as BaseActivityNew), EPrintCopyType.CUSTOMER,stubbedData) { it ->
                                                            }
                                                        }
                                                    }
                                                },
                                                {})

                                        }, 1000)


                                    }
                                }

                            }

                        }

                    }

                    mAdapter.refreshListRemoveAt(updatedPosition)

                }

                ApiStatus.Processing ->{
                    logger("ApiStatus","Processing","e")
                    iDialog?.showProgress(it.msg?:"Getting Pending Pre-Auth From Server")

                }

                ApiStatus.Failed ->{
                    logger("ApiStatus","Failed","e")
                    iDialog?.hideProgress()
                    dialogBuilder.hide()

                    if(it.isReversal){
                        iDialog?.alertBoxWithActionNew(
                            getString(R.string.reversal),
                            getString(R.string.reversal_upload_fail),
                            R.drawable.ic_info_new,
                            getString(R.string.positive_button_ok),
                            "",false,false,
                            {},
                            {})
                    }else {
                        if (it.msg.equals("Declined")) {
                            iDialog?.alertBoxWithActionNew(
                                "Declined",
                                "Transaction Declined",
                                R.drawable.ic_info_new,
                                getString(R.string.positive_button_ok),
                                "", false, false,
                                { alertPositiveCallback ->
                                    gotoDashboard()
                                },
                                {})
                        } else {
                            iDialog?.alertBoxWithActionNew(
                                getString(R.string.error_hint),
                                it.msg ?: "",
                                R.drawable.ic_info_new,
                                getString(R.string.positive_button_ok),
                                "", false, false,
                                { alertPositiveCallback ->
                                    // gotoDashboard()
                                },
                                {})
                        }
                    }

                }
                else -> {
                    logger("ApiStatus","nothing","e")

                }
            }
        }


    }

    private fun setUpRecyclerView() {

        mAdapter = PreAuthPendingAdapter(pendPreauthData = pendingPreAuthDataResponse.pendingList){ data, position ->

            onTouchViewShowDialog(data, position)
        }

        binding?.rvPreAuthPending?.apply{
            layoutManager = GridLayoutManager(activity, 1)
            adapter = mAdapter

        }
    }

    lateinit var dialogBuilder : Dialog
    private fun onTouchViewShowDialog(pendingPreauthData: PendingPreauthData, position: Int) {

        dialogBuilder = Dialog(requireActivity())
        //  builder.setTitle(title)
        //  builder.setMessage(msg)
        val bindingg = ItemPendingPreauthBinding.inflate(LayoutInflater.from(context))

        dialogBuilder.setContentView(bindingg.root)

        dialogBuilder.setCancelable(true)
        val window = dialogBuilder.window
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
       // bindingg.preautnPendingBtnsView.visibility = View.VISIBLE
       // bindingg.enterAmountView.visibility = View.VISIBLE
        val batchNo =  invoiceWithPadding(pendingPreauthData.batch.toString())
        bindingg.batchNoTv.text = batchNo
        val roc = invoiceWithPadding(pendingPreauthData.roc.toString())
        bindingg.rocTv.text = roc
        val pan = pendingPreauthData.pan
        bindingg.panNoTv.text = pan
        val amt = "%.2f".format(pendingPreauthData.amount)
        bindingg.amtTv.text = amt
        val date = pendingPreauthData.date
        bindingg.dateTv.text = date
        val time = pendingPreauthData.time
        bindingg.timeTv.text = time
        var isClicablebtn = false

        var amount = ""

        AmountTextWatcher(bindingg.amountEt)



        bindingg.printBtnn.setOnClickListener {
            val arList = arrayListOf<PendingPreauthData>()
            arList.add(pendingPreauthData)

            printReceipt(arList, dialogBuilder)

        }

        bindingg.completeBtnn.setOnClickListener {

            if (bindingg.amountEt.text.toString().isNotBlank()) {
                if (bindingg.amountEt.text.toString().toFloat() >= 1) {

                    val tpt = Utility().getTptData()
                    authData.authTid = tpt?.terminalId
                    authData.authAmt = bindingg.amountEt.text.toString()
                    authData.authBatchNo = invoiceWithPadding(pendingPreauthData.batch.toString())
                    authData.authRoc = invoiceWithPadding(pendingPreauthData.roc.toString())
                    updatedPosition = position
                    getConfirmCompletePreAuth(authData)

                } else {
                    ToastUtils.showToast(activity,"Amount should be greater than 1 rs")
                }
            } else {
                ToastUtils.showToast(activity,"**** Enter Amount ****")

            }
        }

        dialogBuilder.show()
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

    }

    private fun getConfirmCompletePreAuth(authData: AuthCompletionData) {

        lifecycleScope.launch(Dispatchers.IO) {
            preAuthViewModel.getCompletePreAuthData(authData)
        }

    }

    private fun gotoDashboard() {

        startActivity(Intent(activity, NavigationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })

    }

    private fun printReceipt(arList: ArrayList<PendingPreauthData>, dialogBuilder: Dialog) {
        iDialog?.showProgress(getString(R.string.printing))
        PrintUtil(activity).printPendingPreAuth(activity, arList){
                printCb,_ ->

            (activity as BaseActivityNew).hideProgress()
            dialogBuilder.dismiss()

            if(!printCb){
                iDialog?.alertBoxWithActionNew("Alert","Printing error!",R.drawable.ic_init_next,"OK","",false,false,{},{})
            }
        }
    }


}

class PreAuthPendingAdapter(val pendPreauthData: ArrayList<PendingPreauthData>,
                            var ontouchView: (PendingPreauthData, Int) -> Unit) : RecyclerView.Adapter<PreAuthPendingAdapter.PreAuthPendingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreAuthPendingViewHolder {

        val itemBinding = ItemPreAuthPendingBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)
        return PreAuthPendingViewHolder(
            itemBinding
        )
    }

    override fun getItemCount(): Int = pendPreauthData.size


    override fun onBindViewHolder(holder: PreAuthPendingViewHolder, position: Int) {

        holder.viewBinding.txtViewBatch.text = invoiceWithPadding(pendPreauthData[position].batch.toString())
        holder.viewBinding.txtViewRoc.text = invoiceWithPadding(pendPreauthData[position].roc.toString())
        holder.viewBinding.txtViewPan.text = pendPreauthData[position].pan
        holder.viewBinding.txtViewAmt.text = "%.2f".format(pendPreauthData[position].amount)
        holder.viewBinding.txtViewDate.text = pendPreauthData[position].date
        holder.viewBinding.txtViewTime.text = pendPreauthData[position].time

        holder.viewBinding.linLayItem.setOnClickListener { ontouchView(pendPreauthData[position], position) }

    }

    fun refreshListRemoveAt(position: Int) {
        pendPreauthData.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, pendPreauthData.size)
    }

    inner class PreAuthPendingViewHolder(val viewBinding: ItemPreAuthPendingBinding) : RecyclerView.ViewHolder(viewBinding.root)
}
