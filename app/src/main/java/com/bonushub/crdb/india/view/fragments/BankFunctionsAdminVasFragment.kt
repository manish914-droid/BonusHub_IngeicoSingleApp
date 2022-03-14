package com.bonushub.crdb.india.view.fragments

import android.app.Dialog
import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentBankFunctionsAdminVasBinding
import com.bonushub.crdb.india.db.AppDao
import com.bonushub.crdb.india.di.DBModule
import com.bonushub.crdb.india.model.local.AppPreference
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.checkInternetConnection
import com.bonushub.crdb.india.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.india.utils.dialog.OnClickDialogOkCancel
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.adapter.BankFunctionsAdminVasAdapter
import com.bonushub.crdb.india.view.base.IDialog
import com.bonushub.crdb.india.view.fragments.tets_emi.TestEmiFragment
import com.bonushub.crdb.india.viewmodel.BankFunctionsViewModel
import com.bonushub.crdb.india.viewmodel.BatchFileViewModel
import com.bonushub.crdb.india.viewmodel.InitViewModel
import com.bonushub.pax.utils.KeyExchanger
import com.google.gson.Gson
import com.mindorks.example.coroutines.utils.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*
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

        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.admin_vas_header)

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
                parentFragmentManager.popBackStackImmediate()
            }catch (ex:Exception)
            {
                ex.printStackTrace()
            }
        }

        observeMainViewModel()
    }

    private fun setupRecyclerview(){
        lifecycleScope.launch(Dispatchers.Main) {
            binding?.let {
                it.recyclerView.layoutManager = GridLayoutManager(activity, 1)
                it.recyclerView.adapter = BankFunctionsAdminVasAdapter(iBankFunctionsAdminVasItemClick, adminVasListItem)
            }

        }
    }


    override fun bankFunctionsAdminVasItemClick(bankFunctionsAdminVasItem: BankFunctionsAdminVasItem) {
        when(bankFunctionsAdminVasItem){
            BankFunctionsAdminVasItem.INIT ->{

                if (checkInternetConnection()) {

                    if(!AppPreference.getBoolean(AppPreference.LOGIN_KEY)){
                        //showEnterTIDPopUp
                        DialogUtilsNew1.getInputDialog(requireContext(),"ENTER TID","",true,true,"TID") {

                            if(it.length < 8){
                            ToastUtils.showToast(requireContext(), "Please enter a valid 8 digit TID")
                            }else {
                                initViewModel.insertInfo1(it)
                                //observeMainViewModel()
                            }
                        }

                    }else{
                        // check batch open or not

                        lifecycleScope.launch(Dispatchers.Main) {

                            batchFileViewModel.getBatchTableData()
                                .observe(viewLifecycleOwner, { batchData ->

                                    when {
                                        AppPreference.getBoolean(PreferenceKeyConstant.SERVER_HIT_STATUS.keyName.toString()) ->
                                            ToastUtils.showToast(
                                                requireContext(),
                                                getString(R.string.please_clear_fbatch_before_init)
                                            )

                                        !TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY)) ->
                                            ToastUtils.showToast(
                                                requireContext(),
                                                getString(R.string.reversal_found_please_clear_or_settle_first_before_init)
                                            )

                                        batchData.size > 0 -> ToastUtils.showToast(
                                            requireContext(),
                                            getString(R.string.please_settle_batch_first_before_init)
                                        )
                                        else -> {
                                            startFullInitProcess()
                                        }
                                    }
                                })

                        }
                    }

                } else {
                    ToastUtils.showToast(requireContext(),getString(R.string.no_internet_available_please_check_your_internet))
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
                                        ToastUtils.showToast(
                                            requireContext(),
                                            R.string.invalid_password
                                        )
                                    }
                                }


                            }

                            override fun onClickCancel() {

                            }

                        }, false)

                    }else{
                        ToastUtils.showToast(requireContext(),getString(R.string.no_internet_available_please_check_your_internet))
                    }
                }else{
                    ToastUtils.showToast(requireContext(),"** Initialize Terminal **")
                }

            }

            BankFunctionsAdminVasItem.TERMINAL_PARAM ->{
                // TERMINAL PARAM
                (activity as NavigationActivity).transactFragment(BankFunctionsTerminalFragment(), true)
            }

            BankFunctionsAdminVasItem.COMM_PARAM ->{
                // COMM PARAM
                (activity as NavigationActivity).transactFragment(CommunicationOptionFragment(), true)
            }

//            BankFunctionsAdminVasItem.ENV_PARAM ->{
//                // ENV PARAM
//            }

            BankFunctionsAdminVasItem.INIT_PAYMENT_APP ->{
                // INIT PAYMENT APP
                if(AppPreference.getLogin()){
                (activity as NavigationActivity).transactFragment(BankFunctionsInitPaymentAppFragment(), true)
                }else{
                    ToastUtils.showToast(requireContext(),"** Initialize Terminal **")
                }
            }


            BankFunctionsAdminVasItem.CLEAR_SYNCING_DATA->{
                lifecycleScope.launch(Dispatchers.IO) {
                    appDao.deletePendingSyncTransactionTable()
                }
            }

        }
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

                DialogUtilsNew1.getInputDialog(requireContext(),"ENTER TID","",true,true,"TID") {

                    if(it.length < 8){
                        ToastUtils.showToast(requireContext(), "Please enter a valid 8 digit TID")
                    }else {
                        initViewModel.insertInfo1(it)
                    }
                }
                //(activity as NavigationActivity).transactFragment(InitFragment())
            }
        }
    }

    private fun observeMainViewModel(){

        initViewModel.initData.observe(viewLifecycleOwner, { result ->

            if(!isFromStop) {
                when (result.status) {
                    Status.SUCCESS -> {

                        var isStaticQrAvailable=false

                        CoroutineScope(Dispatchers.IO).launch {
                            Utility().readInitServer(result?.data?.data as ArrayList<ByteArray>) { result, message ->
                                iDialog?.hideProgress()

                                lifecycleScope.launch(Dispatchers.IO){

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
                                                tpt1?.digiPosResponseType = responsF57List[0].toString()
                                                tpt1?.digiPosStatus = responsF57List[1].toString()
                                                tpt1?.digiPosStatusMessage =
                                                    responsF57List[2].toString()
                                                tpt1?.digiPosStatusCode = responsF57List[3].toString()
                                                tpt1?.digiPosTerminalStatus  = responsF57List[4].toString()
                                                tpt1?.digiPosBQRStatus = responsF57List[5].toString()
                                                tpt1?.digiPosUPIStatus =  responsF57List[6].toString()
                                                tpt1?.digiPosSMSpayStatus = responsF57List[7].toString()
                                                tpt1?.digiPosStaticQrDownloadRequired =
                                                    responsF57List[8].toString()
                                                tpt1?.digiPosCardCallBackRequired =
                                                    responsF57List[9].toString()

                                                if ((tpt1?.digiPosTerminalStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode) && (tpt1?.digiPosUPIStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode
                                                            || tpt1.digiPosBQRStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode
                                                            || tpt1.digiPosSMSpayStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode) ){
                                                    tpt1.isDigiposActive = "1"
                                                }
                                                else{
                                                    tpt1?.isDigiposActive = "0"
                                                }

                                                if (tpt1 != null) {
                                                    Field48ResponseTimestamp.performOperation(tpt1) {
                                                        logger(
                                                            LOG_TAG.DIGIPOS.tag,
                                                            "Terminal parameter Table updated successfully $tpt1 "
                                                        )
                                                        //val ttp = TerminalParameterTable.selectFromSchemeTable()
                                                        val ttp = Field48ResponseTimestamp.getTptData()
                                                        val tptObj = Gson().toJson(ttp)
                                                        logger(
                                                            LOG_TAG.DIGIPOS.tag,
                                                            "After success      $tptObj "
                                                        )
                                                    }
                                                    if (tpt1.digiPosBQRStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode) {
                                                        var imgbm: Bitmap? = null
                                                        runBlocking(Dispatchers.IO) {
                                                            val tpt= Field48ResponseTimestamp.getTptData()
                                                            imgbm = loadStaticQrFromInternalStorage() // it return null when file not exist
                                                            if(imgbm==null || tpt?.digiPosStaticQrDownloadRequired =="1") {
                                                                isStaticQrAvailable=true
                                                            }
                                                        }

                                                    }

                                                }

                                                //  }

                                                /* else {
                                                        logger("DIGI_POS", "DIGI_POS_UNAVAILABLE")
                                                    }*/
                                            }else{
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

                                    if(isStaticQrAvailable){
                                        // getting static qr from server if required
                                        //withContext(Dispatchers.IO){
                                        getStaticQrFromServerAndSaveToFile(requireActivity()){
                                            // FAIL AND SUCCESS HANDELED IN FUNCTION getStaticQrFromServerAndSaveToFile itself
                                        }
                                        //}

                                    }
                                    var checkinitstatus = checkInitializationStatus(appDao)
                                    if(checkinitstatus) {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            (activity as? NavigationActivity)?.getString(R.string.successfull_init)?.let {
                                                (activity as? NavigationActivity)?.alertBoxMsgWithIconOnly(
                                                    R.drawable.ic_tick,
                                                    it
                                                )
                                            }
                                        }
                                    }
                                    else{
                                        (activity as? NavigationActivity)?.transactFragment(DashboardFragment())
                                    }
                                }

                            }

                        }
                    }
                    Status.ERROR -> {
                        iDialog?.hideProgress()
                        CoroutineScope(Dispatchers.Main).launch {
                            (activity as? NavigationActivity)?.getInfoDialog(
                                "Error",
                                result.error ?: ""
                            ) {}
                        }
                        // ToastUtils.showToast(activity,"Error called  ${result.error}")
                    }
                    Status.LOADING -> {
                        iDialog?.showProgress(getString(R.string.sending_receiving_host))

                    }
                }
            }else{
                isFromStop = false
            }
        })


    }

    var isFromStop = false

    override fun onStop() {
        super.onStop()
        logger("kush","rem")
        isFromStop = true
    }
}

interface IBankFunctionsAdminVasItemClick{

    fun bankFunctionsAdminVasItemClick(bankFunctionsAdminVasItem: BankFunctionsAdminVasItem)
}