package com.bonushub.crdb.view.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
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
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentBankFunctionsTerminalBinding
import com.bonushub.crdb.di.DBModule
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.utils.ToastUtils
import com.bonushub.crdb.utils.Utility
import com.bonushub.crdb.utils.checkBaseTid
import com.bonushub.crdb.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.utils.dialog.OnClickDialogOkCancel
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.adapter.BankFunctionsTerminalParamAdapter
import com.bonushub.crdb.view.base.IDialog
import com.bonushub.crdb.viewmodel.BankFunctionsViewModel
import com.bonushub.crdb.viewmodel.BatchFileViewModel
import com.bonushub.crdb.viewmodel.InitViewModel
import com.bonushub.pax.utils.PreferenceKeyConstant
import com.mindorks.example.coroutines.utils.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class BankFunctionsTerminalFragment : Fragment(), IBankFunctionsTerminalItemClick {

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
            val tids = checkBaseTid(DBModule.appDatabase.appDao)

            if(dataList[position]?.titleValue.equals(tids[0])) {

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

        DialogUtilsNew1.getInputDialog(context as Context, getString(R.string.update), dataList[position]?.titleValue?:"", false, isTID,dataList[position]?.titleName?:"") {
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
        }
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
                    CoroutineScope(Dispatchers.IO).launch{
                        Utility().readInitServer(result?.data?.data as java.util.ArrayList<ByteArray>) { result, message ->
                            iDialog?.hideProgress()
                            CoroutineScope(Dispatchers.Main).launch {
                                (activity as? NavigationActivity)?.alertBoxMsgWithIconOnly(R.drawable.ic_tick,
                                    requireContext().getString(R.string.successfull_init))
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