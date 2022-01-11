package com.bonushub.crdb.view.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentCommunicationOptionSubMenuBinding
import com.bonushub.crdb.di.DBModule
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.utils.ToastUtils
import com.bonushub.crdb.utils.checkBaseTid
import com.bonushub.crdb.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.utils.dialog.OnClickDialogOkCancel
import com.bonushub.crdb.utils.logger
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.adapter.BankFunctionsTableEditAdapter
import com.bonushub.crdb.view.base.IDialog
import com.bonushub.crdb.viewmodel.BankFunctionsViewModel
import com.bonushub.crdb.viewmodel.BatchFileViewModel
import com.bonushub.crdb.viewmodel.InitViewModel
import com.bonushub.pax.utils.CommunicationParamItem
import com.bonushub.pax.utils.PreferenceKeyConstant
import com.mindorks.example.coroutines.utils.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class CommunicationOptionSubMenuFragment : Fragment(), IBankFunctionsTableEditItemClick {

    private val bankFunctionsViewModel : BankFunctionsViewModel by viewModels()
    private var dialogSuperAdminPassword:Dialog? = null

    private val batchFileViewModel: BatchFileViewModel by viewModels()
    private var iBankFunctionsTableEditItemClick:IBankFunctionsTableEditItemClick? = null
    private lateinit var type :CommunicationParamItem
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
                    val tids = checkBaseTid(DBModule.appDatabase?.appDao)

                    if (dataList[position]?.titleValue.equals(tids[0])){
                        verifySuperAdminPasswordAndUpdate()
                    }else {
                        updateTPTOptionsValue(position, dataList[position]?.titleName?:"")
                    }
                }

            }

            CommunicationParamItem.APP_UPDATE_PARAM -> {
                logger("sub","menu2")
                runBlocking {
                    val tids = checkBaseTid(DBModule.appDatabase?.appDao)

                    if (dataList[position]?.titleValue.equals(tids[0])){
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
            dataList[position]?.titleName?:"") {
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
        }
    }

    suspend fun updateTable()
    {
        logger("type",type.value.toString())
        bankFunctionsViewModel.updateTerminalCommunicationTable(dataList, type.value.toString(), requireContext()).observe(viewLifecycleOwner,{ isUpdateTid ->

            if(isUpdateTid){
                // call init process
                iDialog?.showProgress(getString(R.string.please_wait_host))

                runBlocking {
                    val tids = checkBaseTid(DBModule.appDatabase?.appDao)
                    if(!tids.get(0).isEmpty()!!) {

                        initViewModel.insertInfo1(tids[0] ?:"")
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
                    iDialog?.hideProgress()
                    (activity as NavigationActivity).transactFragment(DashboardFragment())
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