package com.bonushub.crdb.view.fragments.digi_pos

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentTxnListBinding
import com.bonushub.crdb.databinding.ItemPendingTxnBinding
import com.bonushub.crdb.utils.*
import com.bonushub.crdb.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.base.BaseActivityNew
import com.bonushub.pax.utils.KeyExchanger.Companion.getDigiPosStatus
import com.bonushub.crdb.utils.logger
import com.bonushub.crdb.utils.EDashboardItem
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class TxnListFragment : Fragment(), ITxnListItemClick {

    private var sheetBehavior: BottomSheetBehavior<ConstraintLayout>? = null

    var binding:FragmentTxnListBinding? = null
    lateinit var transactionType: EDashboardItem

    lateinit var iTxnListItemClick:ITxnListItemClick

    private var selectedFilterTransactionType: String = ""
    private var selectedFilterTxnID: String = ""

    private var txnDataList = mutableListOf<DigiPosTxnModal>()
    private lateinit var digiPosTxnListAdapter: DigiPosTxnListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentTxnListBinding.inflate(inflater,container, false)
        return binding?.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sheetBehavior = binding?.bottomSheet?.let { BottomSheetBehavior.from(it.bottomLayout) }

        iTxnListItemClick = this
        transactionType = arguments?.getSerializable("type") as EDashboardItem

        binding?.subHeaderView?.subHeaderText?.text = transactionType.title
        binding?.subHeaderView?.headerImage?.setImageResource(transactionType.res)

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            try {
                parentFragmentManager.popBackStackImmediate()
            }catch (ex:Exception)
            {
                ex.printStackTrace()
            }
        }

        // region
        digiPosTxnListAdapter = DigiPosTxnListAdapter(
            txnDataList,
            ::onItemClickCB
        )
        // end region
        setupRecyclerview()
      //  getDigiPosTransactionListFromHost() // kushal

        binding?.txtViewFilters?.setOnClickListener {
            logger("filter","openBottomSheet","e")
            toggleBottomSheet()
        }

        // region bottom sheet
        binding?.bottomSheet?.closeIconBottom?.setOnClickListener {
            closeBottomSheet()
        }

        //region===================Filter Transaction Type's RadioButton OnClick events:-
        binding?.bottomSheet?.upiCollectBottomRB?.setOnClickListener {

            selectedFilterTransactionType =
                binding?.bottomSheet?.upiCollectBottomRB?.text?.toString() ?: ""
            //filterTransactionType = EnumDigiPosProcess.UPIDigiPOS.code
            binding?.bottomSheet?.upiCollectBottomRB?.setTextColor(Color.parseColor("#001F79"))
            binding?.bottomSheet?.upiCollectBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#001F79"))

            binding?.bottomSheet?.dynamicQRBottomRB?.isChecked = false
            binding?.bottomSheet?.dynamicQRBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.dynamicQRBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))

            binding?.bottomSheet?.smsPayBottomRB?.isChecked = false
            binding?.bottomSheet?.smsPayBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.smsPayBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))

            binding?.bottomSheet?.staticQRBottomRB?.isChecked = false
            binding?.bottomSheet?.staticQRBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.staticQRBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))

            Log.d("SelectedRB:- ", selectedFilterTransactionType)
        }

        binding?.bottomSheet?.dynamicQRBottomRB?.setOnClickListener {
            selectedFilterTransactionType =
                binding?.bottomSheet?.dynamicQRBottomRB?.text?.toString() ?: ""
            //filterTransactionType = EnumDigiPosProcess.DYNAMIC_QR.code
            binding?.bottomSheet?.dynamicQRBottomRB?.setTextColor(Color.parseColor("#001F79"))
            binding?.bottomSheet?.dynamicQRBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#001F79"))

            binding?.bottomSheet?.upiCollectBottomRB?.isChecked = false
            binding?.bottomSheet?.upiCollectBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.upiCollectBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))

            binding?.bottomSheet?.smsPayBottomRB?.isChecked = false
            binding?.bottomSheet?.smsPayBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.smsPayBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))

            binding?.bottomSheet?.staticQRBottomRB?.isChecked = false
            binding?.bottomSheet?.staticQRBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.staticQRBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))
            Log.d("SelectedRB:- ", selectedFilterTransactionType)
        }

        binding?.bottomSheet?.smsPayBottomRB?.setOnClickListener {
            selectedFilterTransactionType =
                binding?.bottomSheet?.smsPayBottomRB?.text?.toString() ?: ""
            //filterTransactionType = EnumDigiPosProcess.SMS_PAYDigiPOS.code
            binding?.bottomSheet?.smsPayBottomRB?.setTextColor(Color.parseColor("#001F79"))
            binding?.bottomSheet?.smsPayBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#001F79"))

            binding?.bottomSheet?.dynamicQRBottomRB?.isChecked = false
            binding?.bottomSheet?.dynamicQRBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.dynamicQRBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))

            binding?.bottomSheet?.upiCollectBottomRB?.isChecked = false
            binding?.bottomSheet?.upiCollectBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.upiCollectBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))

            binding?.bottomSheet?.staticQRBottomRB?.isChecked = false
            binding?.bottomSheet?.staticQRBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.staticQRBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))
            Log.d("SelectedRB:- ", selectedFilterTransactionType)
        }

        binding?.bottomSheet?.staticQRBottomRB?.setOnClickListener {
            selectedFilterTransactionType =
                binding?.bottomSheet?.staticQRBottomRB?.text?.toString() ?: ""
           // filterTransactionType = EnumDigiPosProcess.STATIC_QR.code
            binding?.bottomSheet?.staticQRBottomRB?.setTextColor(Color.parseColor("#001F79"))
            binding?.bottomSheet?.staticQRBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#001F79"))

            binding?.bottomSheet?.dynamicQRBottomRB?.isChecked = false
            binding?.bottomSheet?.dynamicQRBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.dynamicQRBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))

            binding?.bottomSheet?.smsPayBottomRB?.isChecked = false
            binding?.bottomSheet?.smsPayBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.smsPayBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))

            binding?.bottomSheet?.upiCollectBottomRB?.isChecked = false
            binding?.bottomSheet?.upiCollectBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.upiCollectBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))
            Log.d("SelectedRB:- ", selectedFilterTransactionType)
        }
        //endregion

        //region===================PTXN ID and MTXN ID RadioButtons OnClick Listener event:-
        binding?.bottomSheet?.ptxnIDBottomRB?.setOnClickListener {
            selectedFilterTxnID = binding?.bottomSheet?.ptxnIDBottomRB?.text?.toString() ?: ""
            binding?.bottomSheet?.ptxnIDBottomRB?.setTextColor(Color.parseColor("#001F79"))
            binding?.bottomSheet?.ptxnIDBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#001F79"))

            binding?.bottomSheet?.mtxnIDBottomRB?.isChecked = false
            binding?.bottomSheet?.mtxnIDBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.mtxnIDBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))
        }

        binding?.bottomSheet?.mtxnIDBottomRB?.setOnClickListener {
            selectedFilterTxnID = binding?.bottomSheet?.mtxnIDBottomRB?.text?.toString() ?: ""
            binding?.bottomSheet?.mtxnIDBottomRB?.setTextColor(Color.parseColor("#001F79"))
            binding?.bottomSheet?.mtxnIDBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#001F79"))

            binding?.bottomSheet?.ptxnIDBottomRB?.isChecked = false
            binding?.bottomSheet?.ptxnIDBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.ptxnIDBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))
        }
        //endregion

        // end region
    }

    //Method to be called when Bottom Sheet Toggle:-
    private fun toggleBottomSheet() {
        if (sheetBehavior?.state != BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            DialogUtilsNew1.hideKeyboardIfOpen(requireActivity())
            sheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }



    //region==================OnItemClickCB:-
    private fun onItemClickCB(position: Int, clickItem: String) {
        if (position > -1) {
            if (clickItem == GET_TXN_STATUS) {
                (activity as BaseActivityNew).showProgress()
                lifecycleScope.launch(Dispatchers.IO) {
                    val req57 =
                        "${EnumDigiPosProcess.GET_STATUS.code}^${txnDataList[position].partnerTXNID}^${txnDataList[position].mTXNID}^"
                    Log.d("Field57:- ", req57)
                    getDigiPosStatus(
                        req57,
                        EnumDigiPosProcessingCode.DIGIPOSPROCODE.code,
                        false
                    ) { isSuccess, responseMsg, responsef57, fullResponse ->
                        try {
                            if (isSuccess) {
                                val statusRespDataList = responsef57.split("^")
                                val modal = txnDataList[position]
                                modal.transactionType = statusRespDataList[0]
                                modal.status = statusRespDataList[1]
                                modal.statusMessage = statusRespDataList[2]
                                modal.statusCode = statusRespDataList[3]
                                modal.mTXNID = statusRespDataList[4]
                                modal.txnStatus = statusRespDataList[5]
                                modal.partnerTXNID = statusRespDataList[6]
                                modal.transactionTime = statusRespDataList[7]
                                modal.amount = statusRespDataList[8]
                                modal.paymentMode = statusRespDataList[9]
                                modal.customerMobileNumber = statusRespDataList[10]
                                modal.description = statusRespDataList[11]
                                modal.pgwTXNID = statusRespDataList[12]

                                lifecycleScope.launch(Dispatchers.IO) {
                                    when (modal.txnStatus) {

                                        EDigiPosPaymentStatus.Pending.desciption -> {
                                            withContext(Dispatchers.Main) {
                                                (activity as BaseActivityNew).hideProgress()
                                                ToastUtils.showToast(requireContext(), getString(R.string.txn_status_still_pending))
                                            }
                                        }
                                        EDigiPosPaymentStatus.Approved.desciption -> {
                                            withContext(Dispatchers.Main) {
                                                txnDataList[position] = modal
                                                digiPosTxnListAdapter.notifyItemChanged(position)
                                                (activity as BaseActivityNew).hideProgress()
                                                binding?.transactionListRV?.smoothScrollToPosition(0)
                                            }
                                        }
                                        ""->{
                                            if(statusRespDataList[1].toLowerCase(Locale.ROOT).equals("Failed", true)){
                                                (activity as BaseActivityNew).hideProgress()
                                                ToastUtils.showToast(requireContext(),statusRespDataList[1])
                                            }
                                        }
                                        else -> {
                                            withContext(Dispatchers.Main) {
                                                (activity as BaseActivityNew).hideProgress()
                                                ToastUtils.showToast(requireContext(),modal.txnStatus)
                                            }
                                        }
                                    }
                                }
                            } else {
                                lifecycleScope.launch(Dispatchers.Main) {
                                    (activity as BaseActivityNew).hideProgress()
                                    (activity as BaseActivityNew).alertBoxWithAction(
                                        getString(R.string.error), responseMsg,
                                        false, getString(R.string.positive_button_ok),
                                        {}, {})
                                }
                            }
                        } catch (ex: java.lang.Exception) {
                            (activity as BaseActivityNew).hideProgress()
                            ex.printStackTrace()
                            logger(
                                LOG_TAG.DIGIPOS.tag,
                                "Somethig wrong... in response data field 57"
                            )
                        }
                    }
                }
            } else {
                    //DigiPosTXNListDetailPage()
                (activity as NavigationActivity).transactFragment(PendingDetailsFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable("data", txnDataList[position])
                        // putString(INPUT_SUB_HEADING, "")
                        putSerializable("type", EDashboardItem.PENDING_TXN)
                    }
                })
            }
        }
    }
//endregion

    //Method to be called on Bottom Sheet Close:-
    private fun closeBottomSheet() {
        DialogUtilsNew1.hideKeyboardIfOpen(requireActivity())
        if (sheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {

            sheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun setupRecyclerview(){
        binding?.transactionListRV?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = DefaultItemAnimator()
            adapter = digiPosTxnListAdapter
        }
    }

    override fun iTxnListItemClick() {
        logger("item","click","e")
    }

}

interface ITxnListItemClick{

    fun iTxnListItemClick()
}

class DigiPosTxnListAdapter( private var dataList: MutableList<DigiPosTxnModal>?,
                             private val onCategoryItemClick: (Int, String) -> Unit) : RecyclerView.Adapter<DigiPosTxnListAdapter.TxnListViewHolder>() {

    private val adapterTXNList: MutableList<DigiPosTxnModal> = mutableListOf()

    init {
        logger("LIST SIZE", "${dataList?.size}", "e")
        if (dataList?.isNotEmpty() == true)
            adapterTXNList.addAll(dataList!!)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TxnListViewHolder {

        val itemBinding = ItemPendingTxnBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)
        return TxnListViewHolder(itemBinding)
    }

    override fun getItemCount(): Int = adapterTXNList.size
    //override fun getItemCount(): Int = 10

    override fun onBindViewHolder(holder: TxnListViewHolder, p1: Int) {

        val modal = adapterTXNList[p1]
        if (!TextUtils.isEmpty(modal.partnerTXNID)) {
            holder.viewBinding.txtViewTxnType.text = modal.paymentMode
            when {
                modal.paymentMode.toLowerCase(Locale.ROOT).equals("sms pay", true) -> {
                    holder.viewBinding.txtViewTxnType.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_sms_pay_transparent,
                        0,
                        0,
                        0
                    )
                }
                modal.paymentMode.toLowerCase(Locale.ROOT).equals("upi", true) -> {
                    holder.viewBinding.txtViewTxnType.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_upi_transparent,
                        0,
                        0,
                        0
                    )
                }
                else -> {
                    holder.viewBinding.txtViewTxnType.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_qr_code,
                        0,
                        0,
                        0
                    )
                }
            }
            val amountData = "\u20B9${modal.amount}"
            holder.viewBinding.txtViewAmount.text = amountData
            if(modal.transactionTime.isNotBlank())
                holder.viewBinding.txtViewDateTime.text = getDateInDisplayFormatDigipos(modal.transactionTime)
            holder.viewBinding.txtViewPhoneNumber.text = modal.customerMobileNumber

            when {
                modal.txnStatus.toLowerCase(Locale.ROOT).equals("success", true) -> {
                    holder.viewBinding.imgViewTxnStatus.setImageResource(R.drawable.circle_with_tick_mark_green)
                    holder.viewBinding.btnGetStatus.visibility = View.GONE
                }
                else -> {
                    holder.viewBinding.imgViewTxnStatus.setImageResource(R.drawable.ic_exclaimation_mark_circle_error)
                    holder.viewBinding.btnGetStatus.visibility = View.VISIBLE
                }
            }

            //Showing Visibility of All Views:-
            holder.viewBinding.imgViewTxnStatus.visibility = View.VISIBLE
            holder.viewBinding.parentSubHeader.visibility = View.VISIBLE
            if(modal.customerMobileNumber.isNullOrEmpty())
                holder.viewBinding.txtViewPhoneNumber.visibility = View.INVISIBLE
            //holder.viewBinding.sepraterLineView.visibility = View.VISIBLE
        }

        //val model = digiPosItem[position]
//        holder.viewBinding.txtViewTxnType.text = "SMS Pay"
//        holder.viewBinding.txtViewDateTime.text = "25 April, 05:39 PM"
//        holder.viewBinding.txtViewAmount.text = "300.00"
//        holder.viewBinding.txtViewPhoneNumber.text = "******3211"
//
//        holder.viewBinding.imgViewTxnStatus.setImageResource(R.drawable.ic_init_payment_success)
//
//        if(position == 2)
//        {
//            holder.viewBinding.imgViewTxnStatus.setImageResource(R.drawable.ic_init_payment_null)
//            //holder.viewBinding.txtViewTxnType.setD
//        }

//        holder.viewBinding.btnGetStatus.setOnClickListener {
//
//            iTxnListItemClick?.iTxnListItemClick()
//
//        }

    }

    //region==========================Below Method is used to refresh Adapter New Data and Also
    fun refreshAdapterList(refreshList: MutableList<DigiPosTxnModal>) {
        val diffUtilCallBack = DigiPosTXNListDiffUtil(this.adapterTXNList, refreshList)
        val diffResult = DiffUtil.calculateDiff(diffUtilCallBack)
        this.adapterTXNList.clear()
        this.adapterTXNList.addAll(refreshList)
        diffResult.dispatchUpdatesTo(this)
    }
    //endregion

    inner class TxnListViewHolder(val viewBinding: ItemPendingTxnBinding) : RecyclerView.ViewHolder(viewBinding.root){
        init {
            viewBinding.btnGetStatus.setOnClickListener {
                onCategoryItemClick(
                    adapterPosition,
                    GET_TXN_STATUS
                )
            }
            viewBinding.parentSubHeader.setOnClickListener {
                onCategoryItemClick(
                    adapterPosition,
                    SHOW_TXN_DETAIL_PAGE
                )
            }
        }
    }
}

//region=============================DigiPos Txn List Data Modal==========================
@Parcelize
data class DigiPosTxnModal(
    var transactionType: String,
    var status: String,
    var statusMessage: String,
    var statusCode: String,
    var mTXNID: String,
    var txnStatus: String,
    var partnerTXNID: String,
    var transactionTime: String,
    var amount: String,
    var paymentMode: String,
    var customerMobileNumber: String,
    var description: String,
    var pgwTXNID: String
) : Parcelable
//endregion