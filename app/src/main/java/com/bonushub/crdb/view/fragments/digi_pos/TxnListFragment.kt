package com.bonushub.crdb.view.fragments.digi_pos

import android.content.Context
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
import com.bonushub.crdb.HDFCApplication
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentTxnListBinding
import com.bonushub.crdb.databinding.ItemPendingTxnBinding
import com.bonushub.crdb.di.DBModule
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.model.local.TerminalParameterTable
import com.bonushub.crdb.serverApi.HitServer
import com.bonushub.crdb.utils.*
import com.bonushub.crdb.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.base.BaseActivityNew
import com.bonushub.pax.utils.KeyExchanger.Companion.getDigiPosStatus
import com.bonushub.crdb.utils.logger
import com.bonushub.crdb.utils.EDashboardItem
import com.bonushub.crdb.utils.Field48ResponseTimestamp.parseDataListWithSplitter
import com.bonushub.crdb.view.base.IDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.*


class TxnListFragment : Fragment() {

    private var sheetBehavior: BottomSheetBehavior<ConstraintLayout>? = null

    var binding:FragmentTxnListBinding? = null
    lateinit var transactionType: EDashboardItem

   // lateinit var iTxnListItemClick:ITxnListItemClick

    private var selectedFilterTransactionType: String = ""
    private var selectedFilterTxnID: String = ""

    private var txnDataList = mutableListOf<DigiPosTxnModal>()
    private lateinit var digiPosTxnListAdapter: DigiPosTxnListAdapter

    private var iDialog:BaseActivityNew? = null

    private var perPageRecord = "0"
    private var hasMoreData = false
    private var pageNumber = "1"
    private var partnerTransactionID = ""
    private var mTransactionID = ""
    private var bottomSheetAmountData = ""
    private var filterTransactionType = ""
    private var totalRecord = "0"
    private var requestTypeID = EnumDigiPosProcess.TXN_LIST.code
    private var field57RequestData = "$requestTypeID^$totalRecord^$filterTransactionType^$bottomSheetAmountData^$partnerTransactionID^$mTransactionID^$pageNumber^"
    private var tempDataList = mutableListOf<String>()

    private var selectedFilterTxnIDValue: String = ""
    private var selectedFilterAmountValue: String = ""

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

        iDialog = (activity as BaseActivityNew)
        sheetBehavior = binding?.bottomSheet?.let { BottomSheetBehavior.from(it.bottomLayout) }

      //  iTxnListItemClick = this
        transactionType = arguments?.getSerializable("type") as EDashboardItem

        binding?.subHeaderView?.subHeaderText?.text = transactionType.title
        binding?.subHeaderView?.headerImage?.setImageResource(transactionType.res)

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            try {
                DialogUtilsNew1.hideKeyboardIfOpen(requireActivity())
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
        getDigiPosTransactionListFromHost() // kushal

        binding?.txtViewFilters?.setOnClickListener {
            logger("filter","openBottomSheet","e")
            toggleBottomSheet()
        }

        binding?.bottomSheet?.applyReset?.setOnClickListener {

            DialogUtilsNew1.hideKeyboardIfOpen(requireActivity())

            binding?.bottomSheet?.amountBottomET?.setText("0.0")
            binding?.bottomSheet?.transactionIDET?.text?.clear()
            binding?.bottomSheet?.txnIDRG?.clearCheck()
            binding?.bottomSheet?.upiCollectBottomRB?.isChecked = false
            binding?.bottomSheet?.dynamicQRBottomRB?.isChecked=false
            binding?.bottomSheet?.smsPayBottomRB?.isChecked=false
            binding?.bottomSheet?.staticQRBottomRB?.isChecked=false
            cleardata()

        }

        //region======================Filter Apply Button onclick event:-
        binding?.bottomSheet?.applyFilter?.setOnClickListener {
            DialogUtilsNew1.hideKeyboardIfOpen(requireActivity())
            val amtStr = binding?.bottomSheet?.amountBottomET?.text?.toString() ?: "0.0"
            bottomSheetAmountData = if (amtStr == "0.0") "" else amtStr
            if (binding?.bottomSheet?.ptxnIDBottomRB?.isChecked == true)
                partnerTransactionID = binding?.bottomSheet?.transactionIDET?.text.toString()
            if (binding?.bottomSheet?.mtxnIDBottomRB?.isChecked == true)
                mTransactionID = binding?.bottomSheet?.transactionIDET?.text.toString()

            field57RequestData = "$requestTypeID^0^$filterTransactionType^$bottomSheetAmountData^$partnerTransactionID^$mTransactionID^1^"
            closeBottomSheet()
            tempDataList.clear()
            txnDataList.clear()
            getDigiPosTransactionListFromHost()
        }
        //endregion



        // region bottom sheet
        binding?.bottomSheet?.closeIconBottom?.setOnClickListener {
            closeBottomSheet()
        }

        //region===================Filter Transaction Type's RadioButton OnClick events:-
        binding?.bottomSheet?.upiCollectBottomRB?.setOnClickListener {

            selectedFilterTransactionType =
                binding?.bottomSheet?.upiCollectBottomRB?.text?.toString() ?: ""
           filterTransactionType = EnumDigiPosProcess.UPIDigiPOS.code
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
            filterTransactionType = EnumDigiPosProcess.DYNAMIC_QR.code
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
           filterTransactionType = EnumDigiPosProcess.SMS_PAYDigiPOS.code
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
          filterTransactionType = EnumDigiPosProcess.STATIC_QR.code
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

        //region======================OnScrollListener to Load More Data in RecyclerView:-
        binding?.transactionListRV?.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!binding?.transactionListRV?.canScrollVertically(1)!! && dy > 0 && hasMoreData) {
                    Log.d("MoreData:- ", "Loading.....")
                    pageNumber = pageNumber.toInt().plus(1).toString()
                    field57RequestData =
                        "$requestTypeID^$totalRecord^$filterTransactionType^$bottomSheetAmountData^$partnerTransactionID^$mTransactionID^" +
                                "$pageNumber^"
                    getDigiPosTransactionListFromHost()
                }
            }

        })
        //endregion
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

    private  fun cleardata(){
        selectedFilterTransactionType = ""
        selectedFilterTxnID = ""
        selectedFilterTxnIDValue = ""
        selectedFilterAmountValue = ""
        hasMoreData = false
        perPageRecord = "0"
        totalRecord = "0"
        pageNumber = "1"
        partnerTransactionID = ""
        mTransactionID = ""
        bottomSheetAmountData = ""
        filterTransactionType = ""
        tempDataList.clear()
        txnDataList.clear()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        //if (context is IDialog) iDialog = context // kushal
    }

    override fun onStop() {
        super.onStop()
        cleardata()
    }

    override fun onDetach() {
        super.onDetach()
       // iDialog = null // kushal
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
                                                    withContext(Dispatchers.Main){
                                                        (activity as BaseActivityNew).hideProgress()
                                                        ToastUtils.showToast(requireContext(),statusRespDataList[1])
                                                    }

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
                (activity as NavigationActivity).transactFragment(DigiPosTXNListDetailFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable("data", txnDataList[position])
                        // putString(INPUT_SUB_HEADING, "")
                       // putSerializable("type", EDashboardItem.PENDING_TXN)
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

    /*override fun iTxnListItemClick() {
        logger("item","click","e")
    }*/

    private var processingCode = EnumDigiPosProcessingCode.DIGIPOSPROCODE.code

    //region==========================Get DigiPos TXN List Data from Host:-
    private fun getDigiPosTransactionListFromHost() {
        Log.d("Field57:- ", field57RequestData)
        iDialog?.showProgress()
        val idw = runBlocking(Dispatchers.IO) {
            IsoDataWriter().apply {
                //val terminalData = TerminalParameterTable.selectFromSchemeTable() // old

                var terminalData: TerminalParameterTable? = null
                if(AppPreference.getLogin()) {
                    terminalData = DBModule.appDatabase.appDao?.getTerminalParameterTableDataByTidType("1")
                }else{
                    terminalData = DBModule.appDatabase.appDao?.getTerminalParameterTableDataByTidType("-1")
                }

                if (terminalData != null) {
                    mti = Mti.EIGHT_HUNDRED_MTI.mti

                    //Processing Code Field 3
                    addField(3, processingCode)

                    //STAN(ROC) Field 11
                    //addField(11, ROCProviderV2.getRoc(AppPreference.getBankCode()).toString()) // kushal
                    addField(11, "000236")

                    //NII Field 24
                    addField(24, Nii.BRAND_EMI_MASTER.nii)

                    //TID Field 41
                    addFieldByHex(41, terminalData.terminalId)

                    //Connection Time Stamps Field 48
                    addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

                    //adding Field 57
                    addFieldByHex(57, field57RequestData)

                    //adding Field 61
                    val version = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)
                    val pcNumber =
                        addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY), "0", 9)
                    val pcNumber2 =
                        addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY_2), "0", 9)
                    val f61 = ConnectionType.GPRS.code + addPad(
                        AppPreference.getString("deviceModel"),
                        " ",
                        6,
                        false
                    ) + addPad(
                        HDFCApplication.appContext.getString(R.string.app_name),
                        " ",
                        10,
                        false
                    ) + version + pcNumber + pcNumber2
                    //adding Field 61
                    addFieldByHex(61, f61)

                    //adding Field 63
                    val deviceSerial =
                        addPad(AppPreference.getString("serialNumber"), " ", 15, false)
                    val bankCode = AppPreference.getBankCode()
                    val f63 = "$deviceSerial$bankCode"
                    addFieldByHex(63, f63)
                }
            }
        }

        logger("DIGIPOS REQ1>>", idw.isoMap, "e")

        // val idwByteArray = idw.generateIsoByteRequest()

        lifecycleScope.launch(Dispatchers.IO) {
            HitServer.hitDigiPosServer(idw, false) { result, success ->
                if (success) {
                    var responseMsg = ""
                    val responseIsoData: IsoDataReader = readIso(result, false)
                    val txnMsg=   responseIsoData.isoMap[39]?.parseRaw2String().toString() +
                            responseIsoData.isoMap[58]?.parseRaw2String().toString()
                    // kushal
                    /*ROCProviderV2.incrementFromResponse(
                        ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                        AppPreference.getBankCode()
                    )*/

                    logger("Transaction RESPONSE ", "---", "e")
                    logger("Transaction RESPONSE --->>", responseIsoData.isoMap, "e")
                    Log.e(
                        "Success 39-->  ",
                        responseIsoData.isoMap[39]?.parseRaw2String().toString() + "---->" +
                                responseIsoData.isoMap[58]?.parseRaw2String().toString()
                    )
                    val successResponseCode =
                        responseIsoData.isoMap[39]?.parseRaw2String().toString()
                    if (responseIsoData.isoMap[58] != null) {
                        responseMsg = responseIsoData.isoMap[58]?.parseRaw2String().toString()
                    }

                    val responseField57 = responseIsoData.isoMap[57]?.parseRaw2String().toString()

                    when (successResponseCode) {
                        "00" -> {
                            if (responseIsoData.isoMap[57] != null) {
                                parseTXNListDataAndShowInRecyclerView(responseField57)
                            }
                        }

                        "-1" -> {
                            lifecycleScope.launch(Dispatchers.Main) {
                                iDialog?.hideProgress()
                                // binding?.emptyViewPlaceholder?.visibility= View.VISIBLE
                                ToastUtils.showToast(requireContext(),responseMsg)
                                digiPosTxnListAdapter.refreshAdapterList(txnDataList)
                            }
                        }

                        else -> {
                            lifecycleScope.launch(Dispatchers.Main) {
                                iDialog?.hideProgress()
                                digiPosTxnListAdapter.refreshAdapterList(txnDataList)
                                hasMoreData = false
                                iDialog?.alertBoxWithAction(
                                    getString(R.string.error), txnMsg,
                                    false, getString(R.string.positive_button_ok),
                                    { parentFragmentManager.popBackStackImmediate() }, {}, R.drawable.ic_info)
                            }
                        }
                    }
                } else {
                    // kushal
                    /*ROCProviderV2.incrementFromResponse(
                        ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                        AppPreference.getBankCode()
                    )*/

                    iDialog?.hideProgress()
                    lifecycleScope.launch(Dispatchers.Main) {
                        iDialog?.hideProgress()
                        hasMoreData = false
                        iDialog?.alertBoxWithAction(
                            getString(R.string.error), result,
                            false, getString(R.string.positive_button_ok),
                            { parentFragmentManager.popBackStackImmediate() }, {},R.drawable.ic_info)
                    }
                }
            }
        }
    }
    //endregion

    //region========================Parse DigiPos TXN List Data and Show in RecyclerView:-
    private fun parseTXNListDataAndShowInRecyclerView(field57Data: String) {
        if (!TextUtils.isEmpty(field57Data)) {
            val dataList =
                parseDataListWithSplitter(SplitterTypes.VERTICAL_LINE.splitter, field57Data)
            if (dataList.isNotEmpty()) {
                requestTypeID = dataList[0]
                //hasMoreData = dataList[1] ----> This Data from Host will always be "0" so we need to manage Pagination in App-End Side
                perPageRecord = dataList[2]
                totalRecord = (totalRecord.toInt().plus(perPageRecord.toInt()).toString())

                tempDataList.clear()
                tempDataList = dataList.subList(3, dataList.size)
                for (i in tempDataList.indices) {
                    //Below we are splitting Data from tempDataList to extract brandID , categoryID , parentCategoryID , categoryName:-
                    if (!TextUtils.isEmpty(tempDataList[i])) {
                        val splitData = parseDataListWithSplitter(
                            SplitterTypes.CARET.splitter,
                            tempDataList[i]
                        )
                        txnDataList.add(
                            DigiPosTxnModal(
                                requestTypeID,
                                splitData[0], splitData[1],
                                splitData[2], splitData[3],
                                splitData[4], splitData[5],
                                splitData[6], splitData[7],
                                splitData[8], splitData[9],
                                splitData[10], splitData[11]
                            )
                        )
                    }
                }
                //Inflate Update Data in Adapter List:-
                lifecycleScope.launch(Dispatchers.Main) {
                    hasMoreData = tempDataList.isNotEmpty() && tempDataList.size >= 10
                    if (txnDataList.isNotEmpty()) {
                        digiPosTxnListAdapter.refreshAdapterList(txnDataList)
                    }
                    iDialog?.hideProgress()
                }
            }
        } else {
            lifecycleScope.launch(Dispatchers.Main) {
                iDialog?.hideProgress()
                hasMoreData = false
                ToastUtils.showToast(requireContext(),"No Data Found")
            }
        }
    }
    //endregion

}

/*interface ITxnListItemClick{

    fun iTxnListItemClick()
}*/

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