package com.bonushub.crdb.view.fragments.digi_pos

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentPendingTxnBinding
import com.bonushub.crdb.databinding.ItemPendingTxnBinding
import com.bonushub.crdb.model.local.DigiPosDataTable
import com.bonushub.crdb.utils.*
import com.bonushub.crdb.utils.Field48ResponseTimestamp.deleteDigiposData
import com.bonushub.crdb.utils.Field48ResponseTimestamp.insertOrUpdateDigiposData
import com.bonushub.crdb.utils.Field48ResponseTimestamp.selectDigiPosDataAccordingToTxnStatus
import com.bonushub.crdb.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.base.BaseActivityNew
import com.bonushub.crdb.utils.EDashboardItem
import com.bonushub.pax.utils.KeyExchanger.Companion.getDigiPosStatus
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*


class PendingTxnFragment : Fragment(), IPendingListItemClick {

    lateinit var transactionType: EDashboardItem

    lateinit var iPendingListItemClick:IPendingListItemClick
    var binding:FragmentPendingTxnBinding? = null

    private var digiPosData = arrayListOf<DigiPosDataTable>()

    private var pendingTxnAdapter: PendingTxnListAdapter? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPendingTxnBinding.inflate(inflater,container, false)
        return binding?.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        iPendingListItemClick = this
        transactionType = arguments?.getSerializable("type") as EDashboardItem

        binding?.linLayPendingTnx?.visibility = View.VISIBLE
        binding?.linLaySearch?.visibility = View.GONE

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

        binding?.txtViewSearch?.setOnClickListener {
            (activity as NavigationActivity).transactFragment(SearchTxnFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("type", EDashboardItem.PENDING_TXN)
                }
            })

        }

        // pending txn region
        try {
            digiPosData = selectDigiPosDataAccordingToTxnStatus(EDigiPosPaymentStatus.Pending.desciption)  as ArrayList<DigiPosDataTable>

            pendingTxnAdapter = PendingTxnListAdapter(
                digiPosData,
                ::onItemClickCB
            )

        }catch (ex:Exception){
            ex.printStackTrace()
        }
        // end region

        if (digiPosData.size == 0) {
            binding?.emptyViewText?.visibility = View.VISIBLE
            binding?.recyclerView?.visibility = View.GONE
        } else {
            binding?.recyclerView?.visibility = View.VISIBLE
            binding?.emptyViewText?.visibility = View.GONE
            setupRecyclerview()

        }



    }

    private fun setupRecyclerview(){
        lifecycleScope.launch(Dispatchers.Main) {
            binding?.let {
                it.recyclerView.layoutManager = GridLayoutManager(activity, 1)
                //it.recyclerView.adapter = PendingTxnListAdapter(digiPosData, iPendingListItemClick)
                it.recyclerView.adapter = pendingTxnAdapter
            }

        }
    }

    override fun iPendingListItemClick() {
        logger("pending list","click")

        DialogUtilsNew1.showPendingTxnDetailsDialog(requireContext(),"300.00","SMS Pay","000156","15462","******7415","Pending",{}) {

            (activity as NavigationActivity).transactFragment(PendingDetailsFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("type", EDashboardItem.PENDING_TXN)
                }
            })

        }
    }

    //region==================OnItemClickCB:-
    private fun onItemClickCB(position: Int, clickItem: String) {

        when (clickItem) {
            GET_TXN_STATUS -> {
                (activity as BaseActivityNew).showProgress()
                lifecycleScope.launch(Dispatchers.IO) {
                    val req57 =
                        "${EnumDigiPosProcess.GET_STATUS.code}^${digiPosData[position].partnerTxnId}^^"
                    Log.d("Field57:- ", req57)
                    getDigiPosStatus(
                        req57,
                        EnumDigiPosProcessingCode.DIGIPOSPROCODE.code,
                        false
                    ) { isSuccess, responseMsg, responsef57, fullResponse ->
                        try {
                            (activity as BaseActivityNew).hideProgress()
                            if (isSuccess) {
                                val statusRespDataList =
                                    responsef57.split("^")
                                if (statusRespDataList[1] == EDigiPosTerminalStatusResponseCodes.SuccessString.statusCode) {
                                    val tabledata =
                                        DigiPosDataTable()
                                    tabledata.requestType =
                                        statusRespDataList[0].toInt()
                                    //  tabledata.partnerTxnId = statusRespDataList[1]
                                    tabledata.status =
                                        statusRespDataList[1]
                                    tabledata.statusMsg =
                                        statusRespDataList[2]
                                    tabledata.statusCode =
                                        statusRespDataList[3]
                                    tabledata.mTxnId =
                                        statusRespDataList[4]
                                    tabledata.partnerTxnId =
                                        statusRespDataList[6]
                                    tabledata.transactionTimeStamp = statusRespDataList[7]
                                    tabledata.displayFormatedDate =
                                        getDateInDisplayFormatDigipos(statusRespDataList[7])
                                    val dateTime =
                                        statusRespDataList[7].split(
                                            " "
                                        )
                                    tabledata.txnDate = dateTime[0]
                                    if (dateTime.size == 2)
                                        tabledata.txnTime = dateTime[1]
                                    tabledata.amount =
                                        statusRespDataList[8]
                                    tabledata.paymentMode =
                                        statusRespDataList[9]
                                    tabledata.customerMobileNumber =
                                        statusRespDataList[10]
                                    tabledata.description =
                                        statusRespDataList[11]
                                    tabledata.pgwTxnId =
                                        statusRespDataList[12]


                                    when (statusRespDataList[5]) {
                                        EDigiPosPaymentStatus.Pending.desciption -> {
                                            tabledata.txnStatus = statusRespDataList[5]
                                            ToastUtils.showToast(requireContext(),getString(R.string.txn_status_still_pending))
                                        }

                                        EDigiPosPaymentStatus.Approved.desciption -> {
                                            tabledata.txnStatus = statusRespDataList[5]
                                            insertOrUpdateDigiposData(tabledata)
                                            val dp =
                                                selectDigiPosDataAccordingToTxnStatus(
                                                    EDigiPosPaymentStatus.Pending.desciption
                                                )
                                            val dpObj = Gson().toJson(dp)
                                            logger(LOG_TAG.DIGIPOS.tag, "--->      $dpObj ")
                                            Log.e("F56->>", responsef57)
                                            runBlocking(Dispatchers.Main) {
                                                if (dp?.size == 0) {
                                                    binding?.emptyViewText?.visibility =
                                                        View.VISIBLE
                                                    binding?.recyclerView?.visibility = View.GONE
                                                } else {
                                                    binding?.recyclerView?.visibility = View.VISIBLE
                                                    binding?.emptyViewText?.visibility = View.GONE
                                                    binding?.recyclerView?.apply {
                                                        pendingTxnAdapter?.refreshAdapterList(dp as ArrayList<DigiPosDataTable>)
                                                        (activity as BaseActivityNew).hideProgress()
                                                    }
                                                }
                                                //   binding?.recyclerView?.smoothScrollToPosition(0)
                                            }
                                        }
                                        else -> {
                                            deleteDigiposData(digiPosData[position].partnerTxnId)
                                            //DigiPosDataTable.insertOrUpdateDigiposData(tabledata)
                                            val dp =
                                                selectDigiPosDataAccordingToTxnStatus(
                                                    EDigiPosPaymentStatus.Pending.desciption
                                                )
                                            val dpObj = Gson().toJson(dp)
                                            logger(LOG_TAG.DIGIPOS.tag, "--->      $dpObj ")
                                            Log.e("F56->>", responsef57)
                                            runBlocking(Dispatchers.Main) {
                                                if (dp?.size == 0) {
                                                    binding?.emptyViewText?.visibility =
                                                        View.VISIBLE
                                                    binding?.recyclerView?.visibility = View.GONE
                                                } else {
                                                    binding?.recyclerView?.visibility = View.VISIBLE
                                                    binding?.emptyViewText?.visibility = View.GONE
                                                    binding?.recyclerView?.apply {
                                                        pendingTxnAdapter?.refreshAdapterList(dp as ArrayList<DigiPosDataTable>)
                                                        (activity as BaseActivityNew).hideProgress()
                                                    }
                                                }
                                                //   binding?.recyclerView?.smoothScrollToPosition(0)
                                            }

                                        }
                                    }
                                } else {
                                    deleteDigiposData(digiPosData[position].partnerTxnId)
                                    //DigiPosDataTable.insertOrUpdateDigiposData(tabledata)
                                    val dp =
                                        selectDigiPosDataAccordingToTxnStatus(
                                            EDigiPosPaymentStatus.Pending.desciption
                                        )
                                    val dpObj = Gson().toJson(dp)
                                    ToastUtils.showToast(requireContext(),statusRespDataList[1])
                                    logger(LOG_TAG.DIGIPOS.tag, "--->      $dpObj ")
                                    Log.e("F56->>", responsef57)
                                    runBlocking(Dispatchers.Main) {
                                        if (dp?.size == 0) {
                                            binding?.emptyViewText?.visibility = View.VISIBLE
                                            binding?.recyclerView?.visibility = View.GONE
                                        } else {
                                            binding?.recyclerView?.visibility = View.VISIBLE
                                            binding?.emptyViewText?.visibility = View.GONE
                                            binding?.recyclerView?.apply {
                                                pendingTxnAdapter?.refreshAdapterList(dp as ArrayList<DigiPosDataTable>)
                                                (activity as BaseActivityNew).hideProgress()
                                            }
                                        }
                                        //   binding?.recyclerView?.smoothScrollToPosition(0)
                                    }


                                }

                            } else {
                                lifecycleScope.launch(Dispatchers.Main) {
                                    (activity as BaseActivityNew).alertBoxWithAction(
                                        getString(R.string.transaction_failed_msg),
                                        responseMsg,
                                        false,
                                        getString(R.string.positive_button_ok),
                                        { alertPositiveCallback ->
                                            if (alertPositiveCallback) {
                                                /* DigiPosDataTable.deletRecord(
                                                     field57.split("^").last())*/
                                                parentFragmentManager.popBackStack()
                                            }
                                        },
                                        {})
                                }
                            }

                        } catch (ex: java.lang.Exception) {
                            ex.printStackTrace()
                            logger(
                                LOG_TAG.DIGIPOS.tag,
                                "Somethig wrong... in response data field 57"
                            )
                        }
                    }
                }

            }
            SHOW_TXN_DETAIL_PAGE -> {
                (activity as NavigationActivity).transactFragment(PendingDetailsFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable("data", digiPosData[position])// kushal
                        // putString(INPUT_SUB_HEADING, "")
                        putSerializable("type", EDashboardItem.PENDING_TXN)
                    }
                })

            }
        }


    }
//endregion
}

interface IPendingListItemClick{

    fun iPendingListItemClick()
}


class PendingTxnListAdapter(private var digiData:ArrayList<DigiPosDataTable>,private var onCategoryItemClick: (Int, String) -> Unit?) : RecyclerView.Adapter<PendingTxnListAdapter.PendingTxnListViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingTxnListViewHolder {

        val itemBinding = ItemPendingTxnBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)
        return PendingTxnListViewHolder(itemBinding)
    }

    override fun getItemCount(): Int = digiData.size
   // override fun getItemCount(): Int = 10

    override fun onBindViewHolder(holder: PendingTxnListViewHolder, position: Int) {

        var model = digiData[position]

        val amountData = "\u20B9 $model.amount}"
        holder.viewBinding.txtViewTxnType.text = model.paymentMode
        holder.viewBinding.txtViewDateTime.text = model.displayFormatedDate
        holder.viewBinding.txtViewAmount.text = amountData
        holder.viewBinding.txtViewPhoneNumber.text = model.customerMobileNumber


        //---------------------------

        //Checking for txn status
        when {
            digiData[position].txnStatus.toLowerCase(Locale.ROOT).equals("success", true) -> {
                holder.viewBinding.imgViewTxnStatus.setImageResource(R.drawable.ic_init_payment_success)

            }
            else -> {
                holder.viewBinding.imgViewTxnStatus.setImageResource(R.drawable.ic_init_payment_null)
                holder.viewBinding.btnGetStatus.visibility = View.VISIBLE
            }
        }
        //Checking for txn type
        when {
            digiData[position].paymentMode.toLowerCase(Locale.ROOT).equals("sms pay", true) -> {
                holder.viewBinding.txtViewTxnType.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_sms_pay_transparent,
                    0,
                    0,
                    0
                )
            }
            digiData[position].paymentMode.toLowerCase(Locale.ROOT).equals("UPI Pay", true) -> {
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
        //Showing Visibility of All Views:-
//        holder.binding.transactionIV.visibility = View.VISIBLE
//        holder.binding.parentSubHeader.visibility = View.VISIBLE
//        holder.binding.transactionIV.visibility = View.VISIBLE
//        if (digiData[position].customerMobileNumber.isNullOrEmpty())
//            holder.binding.mobileNumberTV.visibility = View.INVISIBLE
//        holder.binding.sepraterLineView.visibility = View.VISIBLE

    }

    //region==========================Below Method is used to refresh Adapter New Data and Also
    fun refreshAdapterList(refreshList: ArrayList<DigiPosDataTable>) {
        this.digiData.clear()
        this.digiData.addAll(refreshList)
        notifyDataSetChanged()
    }
    //endregion

    inner class PendingTxnListViewHolder(val viewBinding: ItemPendingTxnBinding) : RecyclerView.ViewHolder(viewBinding.root){
        init {
            viewBinding.btnGetStatus.setOnClickListener {
                onCategoryItemClick(
                    position,
                    GET_TXN_STATUS
                )
            }
            viewBinding.parentSubHeader.setOnClickListener {
                onCategoryItemClick(
                    position,
                    SHOW_TXN_DETAIL_PAGE
                )
            }
        }
    }
}