package com.bonushub.crdb.india.view.fragments

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentCommunicationOptionSubMenuBinding
import com.bonushub.crdb.india.di.DBModule
import com.bonushub.crdb.india.model.local.AppPreference
import com.bonushub.crdb.india.model.local.TerminalParameterTable
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.india.utils.dialog.OnClickDialogOkCancel
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.adapter.BankFunctionsTableEditAdapter
import com.bonushub.crdb.india.view.base.IDialog
import com.bonushub.crdb.india.viewmodel.BankFunctionsViewModel
import com.bonushub.crdb.india.viewmodel.BatchFileViewModel
import com.bonushub.crdb.india.viewmodel.InitViewModel
import com.bonushub.pax.utils.KeyExchanger
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class CommunicationOptionSubMenuFragment : Fragment(), IBankFunctionsTableEditItemClick {

    private val bankFunctionsViewModel : BankFunctionsViewModel by viewModels()
    private var dialogSuperAdminPassword:Dialog? = null

    private val batchFileViewModel: BatchFileViewModel by viewModels()
    private var iBankFunctionsTableEditItemClick:IBankFunctionsTableEditItemClick? = null
    private lateinit var type : CommunicationParamItem
    lateinit var dataList: ArrayList<TableEditHelper?>
    lateinit var mAdapter : BankFunctionsTableEditAdapter

    private val initViewModel : InitViewModel by viewModels()
    private var iDialog: IDialog? = null

    var binding:FragmentCommunicationOptionSubMenuBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentCommunicationOptionSubMenuBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        type = arguments?.getSerializable("type") as CommunicationParamItem

        iBankFunctionsTableEditItemClick = this

        iDialog = (activity as NavigationActivity)

        setUiForEdit()

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }

    }

    var dataPosition:Int = 0
    override fun bankFunctionsTableEditItemClick(position: Int) {
        // get click of sub menu
        when(type){

            CommunicationParamItem.TXN_PARAM -> {

                logger("sub","menu")
                this.dataPosition = position
                // implement when need
                // Log.d("teminal item",""+bankFunctionsTerminalItem._name)
                // updateTPTOptionsValue(position, dataList[position]?.titleName?:"")  // temp

                /*Below condition is executed only in case when user try to change terminal id ,
                So we need to check below condition and then perform fresh init with new terminal id:-
                1.Batch should be empty or settled
                2.Server Hit Status should be false
                3.Reversal should be cleared or synced to host successfully.
                */

                runBlocking {
                    //val tids = checkBaseTid(DBModule.appDatabase?.appDao) // old

                    var tpt: TerminalParameterTable? = null
                    if(AppPreference.getLogin()) {
                        tpt = DBModule.appDatabase.appDao?.getTerminalParameterTableDataByTidType("1")
                    }else{
                        tpt = DBModule.appDatabase.appDao?.getTerminalParameterTableDataByTidType("-1")
                    }

                    //if (dataList[position]?.titleValue.equals(tids[0])){// old
                    if (dataList[position]?.titleValue.equals(tpt?.terminalId)){
                        verifySuperAdminPasswordAndUpdate()
                    }else {
                        updateTPTOptionsValue(position, dataList[position]?.titleName?:"")
                    }
                }

            }

            CommunicationParamItem.APP_UPDATE_PARAM -> {
                logger("sub","menu2")
                runBlocking {
                    //val tids = checkBaseTid(DBModule.appDatabase?.appDao) // old

                    var tpt: TerminalParameterTable? = null
                    if(AppPreference.getLogin()) {
                        tpt = DBModule.appDatabase.appDao?.getTerminalParameterTableDataByTidType("1")
                    }else{
                        tpt = DBModule.appDatabase.appDao?.getTerminalParameterTableDataByTidType("-1")
                    }

                   // if (dataList[position]?.titleValue.equals(tids[0])){ // old
                    if (dataList[position]?.titleValue.equals(tpt?.terminalId)){
                        verifySuperAdminPasswordAndUpdate()
                    }else {
                        updateTPTOptionsValue(position, dataList[position]?.titleName?:"")
                    }
                }

            }
        }
    }

    private fun setUiForEdit()
    {
        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_bankfunction_new)

        when(type){

            CommunicationParamItem.TXN_PARAM -> {

                binding?.subHeaderView?.subHeaderText?.text = getString(R.string.txn_param_header)

                lifecycleScope.launch(Dispatchers.Main) {
                    bankFunctionsViewModel.getTerminalCommunicationTableByRecordType(type.value.toString())?.observe(viewLifecycleOwner,{

                        //logger("menuList",""+it)
                        setTableEditRecyclerview(it)
                    })

                }


            }

            CommunicationParamItem.APP_UPDATE_PARAM -> {

                binding?.subHeaderView?.subHeaderText?.text = getString(R.string.app_update_param_header)

                lifecycleScope.launch(Dispatchers.Main) {
                    bankFunctionsViewModel.getTerminalCommunicationTableByRecordType(type.value.toString())?.observe(viewLifecycleOwner,{

                        //logger("menuList",""+it)
                        setTableEditRecyclerview(it)
                    })

                }

            }
        }
    }

    private fun setTableEditRecyclerview(dataList: ArrayList<TableEditHelper?>){
        this.dataList = dataList
        mAdapter = BankFunctionsTableEditAdapter(dataList, iBankFunctionsTableEditItemClick)

        if(dataList?.size?:0 == 0){
            ToastUtils.showToast(requireContext(),R.string.recoard_not_found)
        }else{

            lifecycleScope.launch(Dispatchers.Main) {
                binding?.let {
                    it.recyclerView.layoutManager = GridLayoutManager(activity, 1)
                    it.recyclerView.adapter = mAdapter
                }

            }
        }

    }

    fun verifySuperAdminPasswordAndUpdate()
    {
        DialogUtilsNew1.showDialog(activity,getString(R.string.super_admin_password),getString(R.string.hint_enter_super_admin_password), onClickDialogOkCancel, false)
    }

    var onClickDialogOkCancel: OnClickDialogOkCancel = object : OnClickDialogOkCancel {

        override fun onClickOk(dialog: Dialog, password:String) {

            dialogSuperAdminPassword = dialog
            bankFunctionsViewModel.isSuperAdminPassword(password)?.observe(viewLifecycleOwner,{ success ->

                if(success)
                {
                    dialogSuperAdminPassword?.dismiss()


                    lifecycleScope.launch(Dispatchers.Main){

                        batchFileViewModel.getBatchTableData().observe(viewLifecycleOwner,{ batchData ->

                            when {
                                AppPreference.getBoolean(PreferenceKeyConstant.SERVER_HIT_STATUS.keyName.toString()) ->
                                    ToastUtils.showToast(requireContext(),getString(R.string.please_clear_fbatch_before_init))

                                !TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY)) ->
                                    ToastUtils.showToast(requireContext(), getString(R.string.reversal_found_please_clear_or_settle_first_before_init))

                                batchData.size > 0 -> ToastUtils.showToast(requireContext(),getString(R.string.please_settle_batch_first_before_init))
                                else -> {
                                    updateTPTOptionsValue(dataPosition, dataList[dataPosition]?.titleName?:"")
                                }
                            }
                        })

                    }
                }else{
                    ToastUtils.showToast(requireContext(),R.string.invalid_password)

                }
            })


        }

        override fun onClickCancel() {

        }

    }

    //Below code is to perform update of values in TPT Options:-
    private fun updateTPTOptionsValue(position: Int, titleName: String) {

        var isTID = titleName.equals("Terminal ID", ignoreCase = true)

        DialogUtilsNew1.getInputDialog(
            context as Context,
            getString(R.string.update),
            dataList[position]?.titleValue?:"",
            false,
            isTID,
            dataList[position]?.titleName?:"", {
            if (titleName.equals("Terminal ID", ignoreCase = true)) {
                when {
                    it == dataList[position]?.titleValue -> {
                        ToastUtils.showToast(requireContext(),"TID Unchanged")
                    }
                    it.length < 8 -> {
                        ToastUtils.showToast(requireContext(), "Please enter a valid 8 digit TID")
                    }
                    else -> {
                        dataList[position]?.titleValue = it
                        dataList[position]?.isUpdated = true
                        mAdapter.notifyItemChanged(position)
                        lifecycleScope.launch {
                            updateTable()
                        }

                    }
                }

            } else {
                dataList[position]?.titleValue = it
                dataList[position]?.isUpdated = true
                mAdapter.notifyItemChanged(position)
                lifecycleScope.launch {
                    updateTable()
                }
            }
        },{})
    }

    suspend fun updateTable()
    {
        logger("type",type.value.toString())
        bankFunctionsViewModel.updateTerminalCommunicationTable(dataList, type.value.toString(), requireContext()).observe(viewLifecycleOwner,{ isUpdateTid ->

            if(isUpdateTid){
                // call init process
                iDialog?.showProgress(getString(R.string.please_wait_host))

                runBlocking {
                    // val tids = checkBaseTid(DBModule.appDatabase?.appDao) // old
                    var tpt:TerminalParameterTable? = null
                    if(AppPreference.getLogin()) {
                        tpt = DBModule.appDatabase.appDao?.getTerminalParameterTableDataByTidType("1")
                    }else{
                        tpt = DBModule.appDatabase.appDao?.getTerminalParameterTableDataByTidType("-1")
                    }

                   // if(!tids.get(0).isEmpty()!!) {
                    if(!tpt?.terminalId.isNullOrEmpty()) {

                        //initViewModel.insertInfo1(tids[0] ?:"") // old
                        initViewModel.insertInfo1(tpt?.terminalId?:"")
                        observeMainViewModel()
                    }
                }

            }
        })
    }

    private fun observeMainViewModel(){

        initViewModel.initData.observe(viewLifecycleOwner, Observer { result ->

            when (result.status) {
                Status.SUCCESS -> {
                    var isStaticQrAvailable=false
                    lifecycleScope.launch(Dispatchers.IO)
                    {
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

                        withContext(Dispatchers.Main)
                        {
                            iDialog?.hideProgress()
                            (activity as NavigationActivity).transactFragment(DashboardFragment())
                        }
                    }

                }
                Status.ERROR -> {
                    iDialog?.hideProgress()
                    ToastUtils.showToast(activity,"Error called  ${result.error}")
                }
                Status.LOADING -> {
                    iDialog?.showProgress("Sending/Receiving From Host")

                }
            }

        })


    }
}

interface IBankFunctionsTableEditItemClick{

    fun bankFunctionsTableEditItemClick(position: Int)
}