package com.bonushub.crdb.india.view.fragments

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentBankFunctionsTerminalBinding
import com.bonushub.crdb.india.db.AppDao
import com.bonushub.crdb.india.di.DBModule
import com.bonushub.crdb.india.model.local.AppPreference
import com.bonushub.crdb.india.model.local.TerminalParameterTable
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.india.utils.dialog.OnClickDialogOkCancel
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.adapter.BankFunctionsTerminalParamAdapter
import com.bonushub.crdb.india.view.base.IDialog
import com.bonushub.crdb.india.viewmodel.BankFunctionsViewModel
import com.bonushub.crdb.india.viewmodel.BatchFileViewModel
import com.bonushub.crdb.india.viewmodel.InitViewModel
import com.bonushub.pax.utils.KeyExchanger
import com.google.gson.Gson
import com.bonushub.crdb.india.utils.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class BankFunctionsTerminalFragment : Fragment(), IBankFunctionsTerminalItemClick {

    @Inject
    lateinit var appDao: AppDao

    private var iBankFunctionsTerminalItemClick:IBankFunctionsTerminalItemClick? = null
    var binding:FragmentBankFunctionsTerminalBinding? = null

    private val bankFunctionsViewModel : BankFunctionsViewModel by viewModels()
    private var dialogSuperAdminPassword:Dialog? = null

    private val batchFileViewModel: BatchFileViewModel by viewModels()
    private val initViewModel : InitViewModel by viewModels()
    private var iDialog: IDialog? = null

    lateinit var mAdapter : BankFunctionsTerminalParamAdapter
    lateinit var dataList: ArrayList<TableEditHelper?>
    private var lastTid: String? = null
    private var position: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentBankFunctionsTerminalBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_bankfunction_new)
        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.terminal_param_header)

        iBankFunctionsTerminalItemClick = this

        iDialog = (activity as NavigationActivity)


        lifecycleScope.launch(Dispatchers.Main) {
            bankFunctionsViewModel.getTerminalParamField()?.observe(viewLifecycleOwner,{

                //logger("menuList",""+it)
                setupRecyclerview(it)
            })

        }


        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }
        observeMainViewModel()

    }

    private fun setupRecyclerview(dataList: ArrayList<TableEditHelper?>){
        this.dataList = dataList
        mAdapter = BankFunctionsTerminalParamAdapter(dataList,iBankFunctionsTerminalItemClick)

        lifecycleScope.launch(Dispatchers.Main) {
            binding?.let {
                it.recyclerView.layoutManager = GridLayoutManager(activity, 1)
                it.recyclerView.adapter = mAdapter
            }

        }
    }

    var dataPosition:Int = 0
    override fun bankFunctionsTerminalItemClick(position: Int) {
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

        runBlocking{
           // val tids = checkBaseTid(DBModule.appDatabase.appDao) // old

            var tpt: TerminalParameterTable? = null
            if(AppPreference.getLogin()) {
                tpt = DBModule.appDatabase.appDao?.getTerminalParameterTableDataByTidType("1")
            }else{
                tpt = DBModule.appDatabase.appDao?.getTerminalParameterTableDataByTidType("-1")
            }

            //if(dataList[position]?.titleValue.equals(tids[0])) { // old
            if(dataList[position]?.titleValue.equals(tpt?.terminalId)) {

                verifySuperAdminPasswordAndUpdate()
            }else{
                updateTPTOptionsValue(position, dataList[position]?.titleName?:"")

            }
        }
    }

    fun verifySuperAdminPasswordAndUpdate() {
        DialogUtilsNew1.showDialog(activity,getString(R.string.super_admin_password),getString(R.string.hint_enter_super_admin_password), onClickDialogOkCancel, false)
    }

    var onClickDialogOkCancel: OnClickDialogOkCancel = object : OnClickDialogOkCancel {

        override fun onClickOk(dialog: Dialog, password:String) {

            dialogSuperAdminPassword = dialog

            bankFunctionsViewModel.isSuperAdminPassword(password)?.observe(viewLifecycleOwner, {success ->

                if(success) {
                    dialogSuperAdminPassword?.dismiss()
                    //
                    //val batchData = BatchFileDataTable.selectBatchData() // get BatchFileDataTable data
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
        lastTid = dataList[position]?.titleValue?:""
        var isTID = titleName.equals("TID", ignoreCase = true)

        DialogUtilsNew1.getInputDialog(context as Context, getString(R.string.update), dataList[position]?.titleValue?:"", false, isTID,dataList[position]?.titleName?:"",{
            if (titleName.equals("TID", ignoreCase = true)) {
                when {
                    it == dataList[position]?.titleValue -> {
                        ToastUtils.showToast(requireContext(),"TID Unchanged")
                    }
                    it.length < 8 -> {
                        ToastUtils.showToast(requireContext(), "Please enter a valid 8 digit TID")
                    }
                    else -> {
                        lifecycleScope.launch {
                            dataList[position]?.titleValue = it
                            dataList[position]?.isUpdated = true
                            mAdapter.notifyItemChanged(position)
                            updateTable(position,lastTid ?: "",it)
                        }

                    }
                }

            } else {
                dataList[position]?.titleValue = it
                dataList[position]?.isUpdated = true
                mAdapter.notifyItemChanged(position)
                lifecycleScope.launch {
                    updateTable(position, it, it)
                }
            }
        },{})
    }

    suspend fun updateTable(positionValue: Int, lastTidValue: String, updatedTid: String) {
        bankFunctionsViewModel.updateTerminalTable(dataList, requireContext())?.observe(viewLifecycleOwner,{ isUpdateTid ->

            if(isUpdateTid){
                // call init process
                iDialog?.showProgress(getString(R.string.sending_receiving_host))

                runBlocking {
                        val tids = checkBaseTid(DBModule.appDatabase?.appDao)

                            AppPreference.saveString(PreferenceKeyConstant.PC_NUMBER_ONE.keyName, "0")
                            AppPreference.saveString(PreferenceKeyConstant.PC_NUMBER_TWO.keyName, "0")
                            initViewModel.insertInfo1(updatedTid)
                           // observeMainViewModel(position,lastTidValue)
                            position = positionValue
                            lastTid = lastTidValue


                    }

            }
        })
    }


    private fun observeMainViewModel() {

        initViewModel.initData.observe(viewLifecycleOwner, Observer { result ->

            when (result.status) {
                Status.SUCCESS -> {

                    var isStaticQrAvailable=false

                    CoroutineScope(Dispatchers.IO).launch{
                        Utility().readInitServer(result?.data?.data as java.util.ArrayList<ByteArray>) { result, message ->
                            iDialog?.hideProgress()

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

                                            try{
                                                if(responsF57List.size > 1){
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
                                                }
                                            }catch (ex:Exception){

                                            }


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
                                                R.drawable.ic_success_with_star,
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
                    dataList[position]?.titleValue = lastTid ?: ""
                    dataList[position]?.isUpdated = true
                    mAdapter.notifyItemChanged(position)
                    iDialog?.hideProgress()

                    CoroutineScope(Dispatchers.Main).launch {
                        reupdateTable(dataList[position]?.titleValue)
                    }

                    CoroutineScope(Dispatchers.Main).launch {
                        (activity as? NavigationActivity)?.getInfoDialog("Error", result.error ?: "") {}
                    }

                 //   ToastUtils.showToast(activity,"Error called  ${result.error}")
                }
                Status.LOADING -> {
                    iDialog?.showProgress(getString(R.string.sending_receiving_host))

                }
            }

        })


    }

     suspend fun reupdateTable(titleValue: String?) {
         bankFunctionsViewModel.updateTerminalTable(dataList, requireContext())
             ?.observe(viewLifecycleOwner, { isUpdateTid ->

             })
     }


}

interface IBankFunctionsTerminalItemClick{

    fun bankFunctionsTerminalItemClick(position: Int)
}

class TableEditHelper(
    var titleName: String,
    var titleValue: String,
    var index:Int=0,
    var isUpdated: Boolean = false
) :
    Comparable<TableEditHelper> {
    override fun compareTo(other: TableEditHelper): Int =
        if (titleName > other.titleName) 1 else if (titleName < other.titleName) -1 else 0
}

fun getEditorActionListener(callback: (v: TextView) -> Unit): TextView.OnEditorActionListener {
    val oal = TextView.OnEditorActionListener { v, actionId, event ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            callback(v)
            true
        } else if (actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
            if (event != null) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    callback(v)
                    true
                } else false
            } else false
        } else false
    }
    return oal
}