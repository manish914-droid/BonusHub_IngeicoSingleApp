package com.bonushub.crdb.india.view.fragments.pre_auth

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentPreAuthPendingBinding
import com.bonushub.crdb.india.databinding.ItemPendingPreauthBinding
import com.bonushub.crdb.india.databinding.ItemPreAuthPendingBinding
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
import java.util.*

@AndroidEntryPoint
class PreAuthPendingFragment : Fragment() {

    private var iDialog: IDialog? = null
    var binding:FragmentPreAuthPendingBinding? = null

    private val authData: AuthCompletionData by lazy { AuthCompletionData() }

    /*val preAuthDataList by lazy {
        arguments?.getSerializable("PreAuthData") as ArrayList<PendingPreauthData>
    }

    val cardProcessData by lazy {
        arguments?.getSerializable("CardProcessData") as CardProcessedDataModal
    }

    //creating our adapter
    val mAdapter by lazy {
        PreAuthPendingAdapter(preAuthDataList) { data, position ->

            //onTouchViewShowDialog(data, position)
        }
    }*/

    private val preAuthViewModel : PreAuthViewModel by viewModels()
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

        binding?.subHeaderView?.subHeaderText?.text = "PRE-AUTH"
        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_preauth)
        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }

        /*binding?.rvPerAuthPending?.apply{
            layoutManager = GridLayoutManager(activity, 1)
            adapter = mAdapter
        }*/

        //preAuthViewModel = ViewModelProvider(this).get(PreAuthViewModel::class.java) //


        lifecycleScope.launch(Dispatchers.Main){

            preAuthViewModel.getPendingPreAuthData()

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
                       // iDialog?.showProgress("Getting Pending Pre-Auth From Server")
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
                                    // kushal 1105
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
        }

      /*  preAuthViewModel.pendingPreAuthData?.observe(viewLifecycleOwner){
                pendingPreAuthData ->

            when(pendingPreAuthData.apiStatus){

                ApiStatus.Success ->{
                    logger("ApiStatus ->","Success")
                    iDialog?.hideProgress()
                    pendingPreAuthDataaaa = pendingPreAuthData
                    setUpRecyclerView()
                }

                ApiStatus.Processing ->{
                    logger("ApiStatus ->","Processing")
                    // iDialog?.showProgress("Getting Pending Pre-Auth From Server")
                    iDialog?.showProgress(pendingPreAuthData.msg?:"Getting Pending Pre-Auth From Server")
                }

                ApiStatus.Failed -> {
                    logger("ApiStatus ->", "Failed")
                    iDialog?.hideProgress()


                    iDialog?.alertBoxWithActionNew(
                        getString(R.string.error_hint),
                        pendingPreAuthData.msg ?: "",
                        R.drawable.ic_info,
                        getString(R.string.positive_button_ok),
                        "", false, false,
                        { alertPositiveCallback ->
                            if (alertPositiveCallback) {
                                // kushal 1105
                                *//* if (!TextUtils.isEmpty(autoSettlementCheck))
                                        syncOfflineSaleAndAskAutoSettlement(
                                            autoSettlementCheck.substring(
                                                0,
                                                1
                                            ), context as BaseActivityNew
                                        )*//*
                            }
                        },
                        {})
                }


                ApiStatus.Nothing ->{
                    logger("ApiStatus ->","Nothing")
                }
            }
        }*/

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
            //(activity as NavigationActivity).transactFragment(PreAuthPendingDetailsFragment(),true)
        }

    }

    private fun setUpRecyclerView() {

        mAdapter = PreAuthPendingAdapter(pendPreauthData = pendingPreAuthDataResponse.pendingList){ data, position ->

            onTouchViewShowDialog(data, position)
        }

        binding?.rvPerAuthPending?.apply{
            layoutManager = GridLayoutManager(activity, 1)
            adapter = mAdapter

        }
    }


    private fun onTouchViewShowDialog(pendingPreauthData: PendingPreauthData, position: Int) {

        val dialogBuilder = Dialog(requireActivity())
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
        bindingg.preautnPendingBtnsView.visibility = View.VISIBLE
        bindingg.enterAmountView.visibility = View.VISIBLE
        val batchNo = "BATCH NO : " + invoiceWithPadding(pendingPreauthData.batch.toString())
        bindingg.batchNoTv.text = batchNo
        val roc = "ROC : " + invoiceWithPadding(pendingPreauthData.roc.toString())
        bindingg.rocTv.text = roc
        val pan = "PAN : " + pendingPreauthData.pan
        bindingg.panNoTv.text = pan
        val amt = "AMT : " + "%.2f".format(pendingPreauthData.amount)
        bindingg.amtTv.text = amt
        val date = "DATE : " + pendingPreauthData.date
        bindingg.dateTv.text = date
        val time = "TIME : " + pendingPreauthData.time
        bindingg.timeTv.text = time
        var isClicablebtn = false

          /*bindingg.amountEt.addTextChangedListener(Utility.OnTextChange {
              //  binding?.ifProceedBtn?.isEnabled = it.length == 8
              if (it.toFloat() >= 1) {
                  //bindingg.completeBtnn.setShapeType(ShapeType.FLAT)
                  isClicablebtn = true
                  bindingg.completeBtnn.setTextColor(Color.WHITE)
              } else {
                  //bindingg.completeBtnn.setShapeType(ShapeType.PRESSED)
                  isClicablebtn = false
                  bindingg.completeBtnn.setTextColor(Color.WHITE)

              }
              //actionDone( view.if_et)
          })*/

        bindingg.amountEt.setOnClickListener {
           // showEditTextSelected(bindingg.amountEt, bindingg.enterAmountView, requireContext()) // kushal 1205
        }


        bindingg.printBtnn.setOnClickListener {
            val arList = arrayListOf<PendingPreauthData>()
            arList.add(pendingPreauthData)

            printReceipt(arList, dialogBuilder) // kushal 1205 done

        }

        bindingg.completeBtnn.setOnClickListener {
            //  VFService.showToast("COMP")
            // kushal 1205
            if (bindingg.amountEt.text.toString().isNotBlank()) {
                if (bindingg.amountEt.text.toString().toFloat() >= 1) {
                   // val tpt = TerminalParameterTable.selectFromSchemeTable()
                    val tpt = Utility().getTptData()
                    authData.authTid = tpt?.terminalId
                    authData.authAmt = bindingg.amountEt.text.toString()
                    //   authData.authAmt = "%.2f".format(pendingPreauthData.amount)
                    authData.authBatchNo = invoiceWithPadding(pendingPreauthData.batch.toString())
                    authData.authRoc = invoiceWithPadding(pendingPreauthData.roc.toString())
                    //lifecycleScope.launch(Dispatchers.IO) {
                        //confirmCompletePreAuth(authData, position) // kushal 1605
                        updatedPosition = position
                        getConfirmCompletePreAuth(authData)

                   // }
                    dialogBuilder.hide()
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

        lifecycleScope.launch(Dispatchers.Main) {
            preAuthViewModel.getCompletePreAuthData(authData).observe(viewLifecycleOwner,{
                when(it.apiStatus){

                    ApiStatus.Success ->{
                        logger("ApiStatus","Success","e")
                        iDialog?.hideProgress()
                        // remove item
                        mAdapter.refreshListRemoveAt(updatedPosition)

                    }

                    ApiStatus.Processing ->{
                        logger("ApiStatus","Processing","e")
                        iDialog?.showProgress(it.msg?:"Getting Pending Pre-Auth From Server")

                    }

                    ApiStatus.Failed ->{
                        logger("ApiStatus","Failed","e")
                        if(it.msg.equals("Declined")){
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
                        }else{
                            iDialog?.alertBoxWithActionNew(
                                getString(R.string.error_hint),
                                it.msg ?: "",
                                R.drawable.ic_info_new,
                                getString(R.string.positive_button_ok),
                                "", false, false,
                                { alertPositiveCallback ->
                                   gotoDashboard()
                                },
                                {})
                        }

                    }
                    else -> {
                        logger("ApiStatus","nothing","e")

                    }
                }
            })
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
    //override fun getItemCount(): Int = 4


    override fun onBindViewHolder(holder: PreAuthPendingViewHolder, position: Int) {

       // val model = listItem[position]

        // temp
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

    inner class PreAuthPendingViewHolder(val viewBinding: ItemPreAuthPendingBinding) : RecyclerView.ViewHolder(viewBinding.root) {

    }
}

/*
public class PendingPreAuthData{
    var apiStatus: ApiStatus = ApiStatus.Nothing
    var successResponseCode: String? = null
    var pendingList = ArrayList<PendingPreauthData>()
    var cardProcessedDataModal = CardProcessedDataModal()

}*/
