package com.bonushub.crdb.view.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentBankFunctionsTerminalBinding
import com.bonushub.crdb.model.local.TerminalParameterTable
import com.bonushub.crdb.utils.ToastUtils
import com.bonushub.crdb.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.utils.dialog.OnClickDialogOkCancel
import com.bonushub.crdb.view.adapter.BankFunctionsTerminalParamAdapter
import com.bonushub.crdb.viewmodel.BankFunctionsViewModel
import com.bonushub.pax.utils.BankFunctionsTerminalItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BankFunctionsTerminalFragment : Fragment(), IBankFunctionsTerminalItemClick {

    private val terminalListItem: MutableList<BankFunctionsTerminalItem> by lazy { mutableListOf<BankFunctionsTerminalItem>() }
    private var iBankFunctionsTerminalItemClick:IBankFunctionsTerminalItemClick? = null
    var binding:FragmentBankFunctionsTerminalBinding? = null

    lateinit var bankFunctionsViewModel: BankFunctionsViewModel

    lateinit var mAdapter : BankFunctionsTerminalParamAdapter
    lateinit var dataList: ArrayList<TableEditHelper?>
    lateinit var terminalParameterTable: TerminalParameterTable

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

        iBankFunctionsTerminalItemClick = this
        bankFunctionsViewModel = ViewModelProvider(this).get(BankFunctionsViewModel::class.java)

//        terminalListItem.addAll(BankFunctionsTerminalItem.values())
//        dataList = ArrayList()
//
//        setupRecyclerview()

        lifecycleScope.launch(Dispatchers.Main) {
            bankFunctionsViewModel.getTerminalParamField()?.observe(requireActivity(),{

                Log.e("menuList",""+it)
                setupRecyclerview(it)
            })

        }

        lifecycleScope.launch(Dispatchers.Main) {
            bankFunctionsViewModel.getTerminalParameterTable()?.observe(requireActivity(),{

                terminalParameterTable = it
            })
        }

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
        if (this::terminalParameterTable.isInitialized && dataList[position]?.titleValue == terminalParameterTable.terminalId.toString()) {

            verifySuperAdminPasswordAndUpdate()

        } else {
            updateTPTOptionsValue(position, dataList[position]?.titleName?:"")
        }
    }

    fun verifySuperAdminPasswordAndUpdate()
    {
        DialogUtilsNew1.showDialog(activity,getString(R.string.super_admin_password),getString(R.string.hint_enter_super_admin_password), onClickDialogOkCancel, false)
    }

    var onClickDialogOkCancel: OnClickDialogOkCancel = object : OnClickDialogOkCancel {

        override fun onClickOk(dialog: Dialog, password:String) {

            bankFunctionsViewModel.isSuperAdminPassword(password)?.observe(requireActivity(),{ success ->

                if(success)
                {
                    dialog.dismiss()
                    //
//                    val batchData = BatchFileDataTable.selectBatchData()
//                    when {
//                        AppPreference.getBoolean(PrefConstant.SERVER_HIT_STATUS.keyName.toString()) ->
//                            VFService.showToast(getString(R.string.please_clear_fbatch_before_init))
//
//                        !TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY)) ->
//                            VFService.showToast(getString(R.string.reversal_found_please_clear_or_settle_first_before_init))
//
//                        batchData.size > 0 -> VFService.showToast(getString(R.string.please_settle_batch_first_before_init))
//                        else -> {
//                            updateTPTOptionsValue(dataPosition, dataList[dataPosition]?.titleName?:"")
//                        }
//                    }
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
        DialogUtilsNew1.getInputDialog(
            context as Context,
            getString(R.string.update),
            dataList[position]?.titleValue?:"",
            true
        ) {
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
                        GlobalScope.launch {
                            updateTable() // BB
                        }

                    }
                }

            } else {
                dataList[position]?.titleValue = it
                dataList[position]?.isUpdated = true
                mAdapter.notifyItemChanged(position)
                GlobalScope.launch {
                    updateTable() // BB
                }
            }
        }
    }

    suspend fun updateTable()
    {
        bankFunctionsViewModel.updateTerminalTable(dataList)
    }


   // private fun getTable(): Any? = when (type) {
    private fun getTable() {

    /*when (type) {
        BankOptions.TPT.ordinal -> {TerminalParameterTable.selectFromSchemeTable()
        }
        BankOptions.CPT.ordinal -> TerminalCommunicationTable.selectFromSchemeTable()
        BankOptions.TXN_COMM_PARAM_TABLE.ordinal -> TerminalCommunicationTable.selectCommTableByRecordType("1")
        BankOptions.APP_UPDATE_COMM_PARAM_TABLE.ordinal -> TerminalCommunicationTable.selectCommTableByRecordType("2")

        else -> null
    }*/
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