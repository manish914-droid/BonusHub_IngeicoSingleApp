package com.bonushub.crdb.india.view.fragments

import android.app.Dialog
import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentBankFunctionsAdminVasBinding
import com.bonushub.crdb.india.db.AppDao
import com.bonushub.crdb.india.di.DBModule
import com.bonushub.crdb.india.model.local.AppPreference
import com.bonushub.crdb.india.model.local.TempBatchFileDataTable
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.checkInternetConnection
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.showToast
import com.bonushub.crdb.india.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.india.utils.dialog.OnClickDialogOkCancel
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.adapter.BankFunctionsAdminVasAdapter
import com.bonushub.crdb.india.view.base.IDialog
import com.bonushub.crdb.india.view.fragments.tets_emi.TestEmiFragment
import com.bonushub.crdb.india.viewmodel.BankFunctionsViewModel
import com.bonushub.crdb.india.viewmodel.BatchFileViewModel
import com.bonushub.crdb.india.viewmodel.InitViewModel
import com.bonushub.crdb.india.vxutils.checkBaseTid
import com.bonushub.crdb.india.vxutils.checkInitializationStatus
import com.bonushub.pax.utils.KeyExchanger
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class BankFunctionsAdminVasFragment : Fragment() , IBankFunctionsAdminVasItemClick{
    @Inject
    lateinit var appDao: AppDao

    private val adminVasListItem: MutableList<BankFunctionsAdminVasItem> by lazy { mutableListOf<BankFunctionsAdminVasItem>() }
    private var iBankFunctionsAdminVasItemClick:IBankFunctionsAdminVasItemClick? = null
    var binding:FragmentBankFunctionsAdminVasBinding? = null

    private val initViewModel : InitViewModel by viewModels()
    private val bankFunctionsViewModel : BankFunctionsViewModel by viewModels()

    // for init`
    private var iDialog: IDialog? = null

    private val batchFileViewModel: BatchFileViewModel by viewModels()
    lateinit var bankFunctionsAdminVasAdapter:BankFunctionsAdminVasAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentBankFunctionsAdminVasBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        DialogUtilsNew1.hideKeyboardIfOpen(requireActivity())
        (activity as NavigationActivity).manageTopToolBar(false)
//        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_drawer_bank_function)
//        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.bank_functions_header)
        refreshSubToolbarLogos(this,null,R.drawable.ic_drawer_bank_function, getString(R.string.bank_functions_header))

        try {
            iDialog = (activity as NavigationActivity)
         logger("iDialog",""+iDialog.toString())
        }catch (ex:Exception)
        {
            ex.printStackTrace()
        }

        iBankFunctionsAdminVasItemClick = this

        adminVasListItem.clear()
        adminVasListItem.addAll(BankFunctionsAdminVasItem.values())
        setupRecyclerview()

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            try {
               // parentFragmentManager.popBackStackImmediate()
                (activity as NavigationActivity).decideDashBoardOnBackPress()
            }catch (ex:Exception)
            {
                ex.printStackTrace()
            }
        }

        observeMainViewModel()
    }


    private fun setupRecyclerview(){
        lifecycleScope.launch(Dispatchers.Main) {
            bankFunctionsAdminVasAdapter = BankFunctionsAdminVasAdapter(iBankFunctionsAdminVasItemClick, adminVasListItem)
            binding?.let {
                it.recyclerView.layoutManager = GridLayoutManager(activity, 1)
                it.recyclerView.adapter = bankFunctionsAdminVasAdapter
            }

        }
    }


    var itemPosition = 0
    override fun bankFunctionsAdminVasItemClick(bankFunctionsAdminVasItem: BankFunctionsAdminVasItem, itemPosition:Int) {
        this.itemPosition = itemPosition

        when(bankFunctionsAdminVasItem){
            BankFunctionsAdminVasItem.INIT ->{
// check before init
                if (checkInternetConnection()) {

                    iDialog?.showProgress()

                    if(!AppPreference.getBoolean(AppPreference.LOGIN_KEY)){
                        //showEnterTIDPopUp
                        /*DialogUtilsNew1.getInputTID_Dialog(requireContext(),"ENTER TID","",true,true,"TID", {

                            if(it.length < 8){
                            ToastUtils.showToast(requireContext(), "Please enter a valid 8 digit TID")
                            }else {
                                initViewModel.insertInfo1(it)
                                //observeMainViewModel()
                            }
                        },
                            { unselectItem() })*/

                        iDialog?.hideProgress()
                        (activity as NavigationActivity).transactFragment(InitFragment(), isBackStackAdded = false)

                    }else{
                        // check batch open or not

                        unselectItem()
                        lifecycleScope.launch(Dispatchers.Main) {

                            batchFileViewModel.getBatchTableData()
                                .observe(viewLifecycleOwner) { batchData ->

                                    when {
                                        AppPreference.getBoolean(PreferenceKeyConstant.SERVER_HIT_STATUS.keyName.toString()) ->{
                                            iDialog?.hideProgress()
                                           showToast(
                                                getString(R.string.please_clear_fbatch_before_init)
                                            )
                                        }


                                        !TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY)) ->{
                                            iDialog?.hideProgress()
                                            ToastUtils.showToast(
                                                requireContext(),
                                                getString(R.string.reversal_found_please_clear_or_settle_first_before_init)
                                            )
                                        }


                                        batchData.size > 0 -> {
                                            iDialog?.hideProgress()
                                            ToastUtils.showToast(
                                                requireContext(),
                                                getString(R.string.please_settle_batch_first_before_init)
                                            )
                                        }
                                        else -> {
                                            startFullInitProcess()
                                        }
                                    }
                                }

                        }
                    }

                } else {
                    ToastUtils.showToast(requireContext(),getString(R.string.no_internet_available_please_check_your_internet))
                    unselectItem()
                }



            }

            BankFunctionsAdminVasItem.TEST_EMI ->{
                // TEST EMI depends on bank emi
                if(AppPreference.getLogin()){

                    if(checkInternetConnection())
                    {
                        DialogUtilsNew1.showDialog(activity,getString(R.string.super_admin_password),getString(R.string.hint_enter_super_admin_password),object:OnClickDialogOkCancel{
                            override fun onClickOk(dialog: Dialog, password: String) {

                                logger("password",password)

                                bankFunctionsViewModel.isSuperAdminPassword(password)?.observe(viewLifecycleOwner) {

                                    if (it) {
                                        dialog.dismiss()
                                        (activity as NavigationActivity).transactFragment(
                                            TestEmiFragment(),
                                            true
                                        )


                                    } else {
                                        /*ToastUtils.showToast(
                                            requireContext(),
                                            R.string.invalid_password
                                        )*/


                                        val edtTextPassword = dialog?.findViewById<View>(R.id.edtTextPassword) as TextInputEditText
                                        edtTextPassword.setError(getString(R.string.invalid_password))
                                    }
                                }


                            }

                            override fun onClickCancel() {
                                unselectItem()
                            }

                        }, false)

                    }else{
                        ToastUtils.showToast(requireContext(),getString(R.string.no_internet_available_please_check_your_internet))
                        unselectItem()
                    }
                }else{
                    ToastUtils.showToast(requireContext(),"** Initialize Terminal **")
                    unselectItem()
                }

            }

            BankFunctionsAdminVasItem.APPLICATION_UPDATE ->{
                (activity as NavigationActivity).window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                // iDialog?.onEvents(VxEvent.AppUpdate)  // please check

                unselectItem()

            }

            else ->{
                DialogUtilsNew1.showDialog(activity,getString(R.string.super_admin_password),getString(R.string.hint_enter_super_admin_password),object:OnClickDialogOkCancel{
                    override fun onClickOk(dialog: Dialog, password: String) {

                        logger("password",password)

                        bankFunctionsViewModel.isSuperAdminPassword(password)?.observe(viewLifecycleOwner) {

                            if (it) {
                                dialog.dismiss()

                                // check other option
                                when(bankFunctionsAdminVasItem){


                                    BankFunctionsAdminVasItem.TERMINAL_PARAM ->{
                                        // TERMINAL PARAM
                                        (activity as NavigationActivity).transactFragment(BankFunctionsTerminalFragment(), true)
                                    }

                                    BankFunctionsAdminVasItem.COMM_PARAM ->{
                                        // COMM PARAM
                                        (activity as NavigationActivity).transactFragment(CommunicationOptionFragment(), true)
                                    }

                                    BankFunctionsAdminVasItem.ENV_PARAM ->{
                                        changeEnvParam()
                                    }

                                    BankFunctionsAdminVasItem.CLEAR_REVERSAL ->{
                                        logger("CLEAR_REVERSAL","click","e")
                                        lifecycleScope.launch(Dispatchers.Main) {
                                            if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY)))
                                                iDialog?.alertBoxWithActionNew(
                                                    getString(R.string.reversal),
                                                    getString(R.string.reversal_clear),
                                                    R.drawable.ic_info_orange,
                                                    "YES","Cancel",
                                                    true,false,
                                                    { alertPositiveCallback ->
                                                        if (alertPositiveCallback) {
                                                            AppPreference.clearReversal()
                                                            iDialog?.showToast("Reversal clear successfully")
                                                        }
                                                        unselectItem()
                                                        //    declinedTransaction()
                                                    },
                                                    {
                                                        unselectItem()
                                                    })
                                            else
                                                iDialog?.alertBoxWithActionNew(
                                                    getString(R.string.reversal),
                                                    getString(R.string.no_reversal_found),
                                                    R.drawable.ic_info_orange,
                                                    getString(R.string.positive_button_ok),"",
                                                    false,true,
                                                    {
                                                        unselectItem()
                                                    },
                                                    {
                                                        unselectItem()
                                                    })


                                        }
                                    }

                                    BankFunctionsAdminVasItem.CLEAR_BATCH ->{
                                       // val batchList = BatchFileDataTable.selectBatchData()
                                        var batchList:MutableList<TempBatchFileDataTable>
                                        runBlocking {
                                            batchList = appDao.getAllTempBatchFileDataTableDataForSettlement()
                                        }
                                        if (batchList.size > 0) {
                                            iDialog?.alertBoxWithActionNew(
                                                "Delete",
                                                "Do you want to delete batch data?",
                                                R.drawable.ic_info_orange,
                                                "YES","NO",
                                                true,false, {
                                                    unselectItem()
                                                    val batchNumber =
                                                        AppPreference.getIntData(PrefConstant.SETTLEMENT_BATCH_INCREMENT.keyName.toString()) + 1
                                                    AppPreference.setIntData(
                                                        PrefConstant.SETTLEMENT_BATCH_INCREMENT.keyName.toString(),
                                                        batchNumber
                                                    )
//                                                    TerminalParameterTable.updateSaleBatchNumber(
//                                                        batchNumber.toString()
//                                                    )
                                                    // Added by MKK for automatic FBatch value zero in case of Clear Batch
                                                    AppPreference.saveBoolean(
                                                        PrefConstant.SERVER_HIT_STATUS.keyName.toString(),
                                                        false
                                                    )
                                                    // Added By MKK
                                                    //After settlement failure and clearing the batch and clearing batch from  host
                                                    //After that doing init transaction was not happening so I mainatin this boolean here
                                                    AppPreference.saveBoolean(PrefConstant.BLOCK_MENU_OPTIONS.keyName.toString(), false)

                                                    // Added By MKK
                                                    //After settlement failure and clearing the batch and clearing batch from  host
                                                    //After that doing init transaction was not happening so I mainatin this boolean here
                                                    AppPreference.saveBoolean(PrefConstant.BLOCK_MENU_OPTIONS.keyName.toString(), false)

                                                   // ROCProviderV2.saveBatchInPreference(batchList)
                                                    //Delete All BatchFile Data from Table after Settlement:-
                                                    lifecycleScope.launch(Dispatchers.IO) {
                                                        appDao.deleteTempBatchFileDataTable()
//                                                        BrandEMIDataTable.brandEmiDataclear()
//                                                        BrandEMIAccessDataModalTable.brandEmiByCodeclear()
                                                        withContext(Dispatchers.Main) {
                                                            ToastUtils.showToast(requireContext(),"Batch Deleted Successfully")
                                                        }
                                                    }
                                                }, {
                                                    unselectItem()
                                                }
                                            )
                                        } else {
                                            iDialog?.alertBoxWithActionNew(
                                                "Empty",
                                                "Batch is empty",
                                                R.drawable.ic_info_orange,
                                                "OK","",false,
                                                true,
                                                {
                                                    unselectItem()
                                                }, {
                                                    // Added by MKK for automatic FBatch value zero in case of Clear Batch
                                                    AppPreference.saveBoolean(
                                                        PrefConstant.SERVER_HIT_STATUS.keyName.toString(),
                                                        false
                                                    )
                                                    //
                                                    unselectItem()
                                                }
                                            )
                                        }
                                    }

                                    BankFunctionsAdminVasItem.TMK_DOWNLOAD ->{
                                        iDialog?.alertBoxWithActionNew(
                                            getString(R.string.download_tmk),
                                            getString(R.string.do_you_want_to_download_tmk),
                                            R.drawable.ic_info_orange,
                                            getString(R.string.yes),"Cancel",
                                            true,false,
                                            {
                                                (activity as NavigationActivity).window.addFlags(
                                                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                                /*iDialog?.onEvents(VxEvent.DownloadTMKForHDFC) */
                                                unselectItem()
                                            }, // please check
                                            { Log.d("NO:- ", "Clicked")
                                                (activity as NavigationActivity).getWindow().clearFlags(
                                                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                                                unselectItem()
                                            })

                                    }

                                    BankFunctionsAdminVasItem.SYNC_TRANSACTION ->{
                                        lifecycleScope.launch(Dispatchers.Main) {

                                            iDialog?.alertBoxWithActionNew(
                                                getString(R.string.reversal),
                                                getString(R.string.sync_transaction),
                                                R.drawable.ic_info_orange,
                                                getString(R.string.yes),"Cancel",true,
                                                false,
                                                { alertPositiveCallback ->
                                                    if (alertPositiveCallback) {
                                                        //TxnCallBackRequestTable.clear() // please check
                                                        iDialog?.showToast("Sync Transaction clear successfully")
                                                    }

                                                    unselectItem()
                                                    //    declinedTransaction()
                                                },
                                                {
                                                    unselectItem()
                                                })



                                        }
                                    }
                                    else -> {}
                                }
                            } else {
                                /*ToastUtils.showToast(
                                    requireContext(),
                                    R.string.invalid_password
                                )*/

                                val edtTextPassword = dialog?.findViewById<View>(R.id.edtTextPassword) as TextInputEditText
                                edtTextPassword.setError(getString(R.string.invalid_password))
                            }
                        }


                    }

                    override fun onClickCancel() {
                        unselectItem()
                    }

                }, false)

            }
            /*BankFunctionsAdminVasItem.INIT_PAYMENT_APP ->{
                // INIT PAYMENT APP
                if(AppPreference.getLogin()){
                (activity as NavigationActivity).transactFragment(BankFunctionsInitPaymentAppFragment(), true)
                }else{
                    ToastUtils.showToast(requireContext(),"** Initialize Terminal **")
                }
            }*/


            /*BankFunctionsAdminVasItem.CLEAR_SYNCING_DATA->{
                lifecycleScope.launch(Dispatchers.IO) {
                    appDao.deletePendingSyncTransactionTable()
                }
            }*/

        }
    }

    private fun unselectItem()
    {
        bankFunctionsAdminVasAdapter.notifyItemChanged(itemPosition)
    }

    private fun startFullInitProcess() {

        // INIT
        // iDialog?.showProgress(getString(R.string.please_wait_host))

        runBlocking {
            val tids = checkBaseTid(DBModule.appDatabase?.appDao)

            if(!tids.get(0).isEmpty()!!) {

                logger("get tid", "by table")
                // get tid from table and init

                initViewModel.insertInfo1(tids[0] ?:"")
                //observeMainViewModel()
            }else{
               // get tid by user
                logger("get tid","by user")
                iDialog?.hideProgress()

                DialogUtilsNew1.getInputTID_Dialog(requireContext(),"ENTER TID","",true,true,"TID", {

                    if(it.length < 8){
                        ToastUtils.showToast(requireContext(), "Please enter a valid 8 digit TID")
                    }else {
                        initViewModel.insertInfo1(it)
                    }
                },{})
                //(activity as NavigationActivity).transactFragment(InitFragment())
            }
        }
    }

    private fun observeMainViewModel(){

        initViewModel.initData.observe(viewLifecycleOwner) { result ->

            if (!isFromStop) {
                when (result.status) {
                    Status.SUCCESS -> {

                        var isStaticQrAvailable = false

                        CoroutineScope(Dispatchers.IO).launch {
                            Utility().readInitServer(result?.data?.data as ArrayList<ByteArray>) { result, message ->
                                iDialog?.hideProgress()

                                lifecycleScope.launch(Dispatchers.IO) {

                                    KeyExchanger.getDigiPosStatus(
                                        EnumDigiPosProcess.InitializeDigiPOS.code,
                                        EnumDigiPosProcessingCode.DIGIPOSPROCODE.code, false
                                    ) { isSuccess, responseMsg, responsef57, fullResponse ->
                                        try {
                                            if (isSuccess) {
                                                //1^Success^Success^S101^Active^Active^Active^Active^0^1
                                                val responsF57List = responsef57.split("^")
                                                Log.e("F56->>", responsef57)
                                                //  if (responsF57List[4] == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode) {
                                                val tpt1 = Field48ResponseTimestamp.getTptData()

                                                try {
                                                    if (responsF57List.size > 1) {
                                                        tpt1?.digiPosResponseType =
                                                            responsF57List[0].toString()
                                                        tpt1?.digiPosStatus =
                                                            responsF57List[1].toString()
                                                        tpt1?.digiPosStatusMessage =
                                                            responsF57List[2].toString()
                                                        tpt1?.digiPosStatusCode =
                                                            responsF57List[3].toString()
                                                        tpt1?.digiPosTerminalStatus =
                                                            responsF57List[4].toString()
                                                        tpt1?.digiPosBQRStatus =
                                                            responsF57List[5].toString()
                                                        tpt1?.digiPosUPIStatus =
                                                            responsF57List[6].toString()
                                                        tpt1?.digiPosSMSpayStatus =
                                                            responsF57List[7].toString()
                                                        tpt1?.digiPosStaticQrDownloadRequired =
                                                            responsF57List[8].toString()
                                                        tpt1?.digiPosCardCallBackRequired =
                                                            responsF57List[9].toString()
                                                    }

                                                } catch (ex: Exception) {

                                                }


                                                if ((tpt1?.digiPosTerminalStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode) && (tpt1?.digiPosUPIStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode
                                                            || tpt1.digiPosBQRStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode
                                                            || tpt1.digiPosSMSpayStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode)
                                                ) {
                                                    tpt1.isDigiposActive = "1"
                                                } else {
                                                    tpt1?.isDigiposActive = "0"
                                                }

                                                if (tpt1 != null) {
                                                    Field48ResponseTimestamp.performOperation(tpt1) {
                                                        logger(
                                                            LOG_TAG.DIGIPOS.tag,
                                                            "Terminal parameter Table updated successfully $tpt1 "
                                                        )
                                                        //val ttp = TerminalParameterTable.selectFromSchemeTable()
                                                        val ttp =
                                                            Field48ResponseTimestamp.getTptData()
                                                        val tptObj = Gson().toJson(ttp)
                                                        logger(
                                                            LOG_TAG.DIGIPOS.tag,
                                                            "After success      $tptObj "
                                                        )
                                                    }
                                                    if (tpt1.digiPosBQRStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode) {
                                                        var imgbm: Bitmap? = null
                                                        runBlocking(Dispatchers.IO) {
                                                            val tpt =
                                                                Field48ResponseTimestamp.getTptData()
                                                            imgbm =
                                                                loadStaticQrFromInternalStorage() // it return null when file not exist
                                                            if (imgbm == null || tpt?.digiPosStaticQrDownloadRequired == "1") {
                                                                isStaticQrAvailable = true
                                                            }
                                                        }

                                                    }

                                                }

                                                //  }

                                                /* else {
                                                        logger("DIGI_POS", "DIGI_POS_UNAVAILABLE")
                                                    }*/
                                            } else {
                                                //VFService.showToast(responseMsg)
                                            }

                                        } catch (ex: java.lang.Exception) {
                                            ex.printStackTrace()
                                            logger(
                                                LOG_TAG.DIGIPOS.tag,
                                                "Somethig wrong... in response data field 57"
                                            )
                                        }
                                    }

                                    if (isStaticQrAvailable) {
                                        // getting static qr from server if required
                                        //withContext(Dispatchers.IO){
                                        getStaticQrFromServerAndSaveToFile(requireActivity()) {
                                            // FAIL AND SUCCESS HANDELED IN FUNCTION getStaticQrFromServerAndSaveToFile itself
                                        }
                                        //}

                                    }
                                //    var checkinitstatus = checkInitializationStatus(appDao)
                                //    if (checkinitstatus) {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            (activity as? NavigationActivity)?.getString(R.string.successfull_init)
                                                ?.let {
                                                    (activity as? NavigationActivity)?.alertBoxMsgWithIconOnly(
                                                        R.drawable.ic_success_with_star,
                                                        it
                                                    )
                                                }
                                        }
                                //    }
                                      /*else {
                                        (activity as? NavigationActivity)?.transactFragment(
                                            DashboardFragment()
                                        )
                                    }*/
                                }

                            }

                        }
                    }
                    Status.ERROR -> {
                        iDialog?.hideProgress()
                        CoroutineScope(Dispatchers.Main).launch {
                            (activity as? NavigationActivity)?.alertBoxWithActionNew(
                                "Error",
                                result.error ?: ""
                            ,R.drawable.ic_info_orange,getString(R.string.ok),"",false,true,{ unselectItem() },{}
                            )
                        }
                        // ToastUtils.showToast(activity,"Error called  ${result.error}")
                    }
                    Status.LOADING -> {
                        iDialog?.showProgress(getString(R.string.sending_receiving_host))

                    }
                }
            } else {
                isFromStop = false
            }
        }


    }

    var isFromStop = false

    override fun onStop() {
        super.onStop()
        logger("kush","rem")
        isFromStop = true
    }

    private fun changeEnvParam() {
        lifecycleScope.launch(Dispatchers.Main) {
            // not need
            /*val list = arrayListOf<TableEditHelper>()
            val i = IssuerParameterTable.selectFromIssuerParameterTable()
            for (e in i) {
                list.add(TableEditHelper(e.issuerName, e.issuerId))
            }*/

            var isEdit = false
            context?.let {
                Dialog(it).apply {
                    requestWindowFeature(Window.FEATURE_NO_TITLE)
                    setContentView(R.layout.dialog_emv)
                    setCancelable(false)

                    val pcEt = findViewById<EditText>(R.id.emv_pcno_et)
                    val bankEt = findViewById<EditText>(R.id.emv_bankcode_et)

                    findViewById<View>(R.id.env_save_btn).setOnClickListener {
                        AppPreference.saveString(
                            PreferenceKeyConstant.PC_NUMBER_ONE.keyName,
                            pcEt.text.toString()

                        )
                        AppPreference.setBankCode(bankEt.text.toString())
                        dismiss()
                        unselectItem()
                    }
                    findViewById<View>(R.id.env_cancel_btn).setOnClickListener {

                        dismiss()
                        unselectItem()
                    }


                    /*  val issuerEt = findViewById<EditText>(R.id.emv_issuerid_et)
                      val accEt = findViewById<EditText>(R.id.emv_ac_selection_et)*/

                    // pcEt.setText(AppPreference.getString(AppPreference.PC_NUMBER_KEY))
                    pcEt.setText(AppPreference.getString(PreferenceKeyConstant.PC_NUMBER_ONE.keyName))
                    pcEt.setSelection(pcEt.text.length)
                    bankEt.setText(AppPreference.getBankCode())
                    bankEt.setSelection(bankEt.text.length)
                    /*  if (AppPreference.getString(AppPreference.CRDB_ISSUER_ID_KEY).isEmpty()) {
                          val issuerId = addPad(AppPreference.WALLET_ISSUER_ID, "0", 2)
                          issuerEt.setText(issuerId)
                      } else {
                          issuerEt.setText(AppPreference.getString(AppPreference.CRDB_ISSUER_ID_KEY))
                          //  issuerEt.setText(AppPreference.getString(AppPreference.WALLET_ISSUER_ID))
                      }
                      accEt.setText(AppPreference.getString(AppPreference.ACC_SEL_KEY))*/

                    //   val rg = findViewById<RadioGroup>(R.id.emv_radio_grp_btn)

                    /* rg.setOnCheckedChangeListener { _rbg, id ->
                         val rb = _rbg.findViewById<RadioButton>(id)
                         val value = rb.tag as String
                         if (value.isNotEmpty()) {
                             GlobalScope.launch {
                                 val data =
                                     IssuerParameterTable.selectFromIssuerParameterTable(value)
                                 if (data != null) {
                                     val issuerName = data.issuerId
                                    *//* launch(Dispatchers.Main) {
                                            issuerEt.setText(issuerName)
                                        }*//*
                                        AppPreference.saveString(
                                            AppPreference.CRDB_ISSUER_ID_KEY,
                                            issuerName
                                        )

                                    }
                                }
                            }
                        }*/

                    /* list.forEach {
                         val rBtn = RadioButton(context).apply {
                             text = it.titleName
                             tag = it.titleValue
                             setPadding(5, 20, 5, 20)
                         }
                         rg.addView(rBtn)
                         if (it.titleValue == issuerEt.text.toString()) {
                             rBtn.isChecked = true
                         }

                     }*/



                    /* findViewById<TextView>(R.id.emv_edit).setOnClickListener {
                         isEdit = !isEdit
                         val tv = it as TextView
                         if (isEdit) {
                             hh(
                                 arrayOf( sep),
                                 arrayOf(pcEt, bankEt),
                                 View.GONE
                             )
                             tv.text = getString(R.string.save)
                         } else {
                             GlobalScope.launch {
                                 AppPreference.saveString(
                                     AppPreference.PC_NUMBER_KEY,
                                     pcEt.text.toString()
                                 )
                                 AppPreference.setBankCode(bankEt.text.toString())
                                *//* AppPreference.saveString(
                                        AppPreference.ACC_SEL_KEY,
                                        accEt.text.toString()
                                    )
                                    AppPreference.saveString(
                                        AppPreference.CRDB_ISSUER_ID_KEY,
                                        issuerEt.text.toString()
                                    )*//*

                                }
                                hh(arrayOf( sep), arrayOf(pcEt, bankEt), View.GONE)
                                activity?.let { it1 -> ROCProviderV2.refreshToolbarLogos(it1) }
                                tv.text = getString(R.string.edit)
                            }
                        }*/

                }.show()
            }
        }
    }
}

interface IBankFunctionsAdminVasItemClick{

    fun bankFunctionsAdminVasItemClick(bankFunctionsAdminVasItem: BankFunctionsAdminVasItem, itemPosition:Int)
}