package com.bonushub.crdb.view.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentBankFunctionsBinding
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.utils.DeviceHelper
import com.bonushub.crdb.utils.ToastUtils
import com.bonushub.crdb.utils.Utility
import com.bonushub.crdb.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.utils.dialog.OnClickDialogOkCancel
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.adapter.BankFunctionsAdapter
import com.bonushub.crdb.viewmodel.BankFunctionsViewModel
import com.bonushub.pax.utils.BankFunctionsItem
import com.ingenico.hdfcpayment.listener.OnOperationListener
import com.ingenico.hdfcpayment.response.OperationResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BankFunctionsFragment : Fragment(), IBankFunctionItemClick {

    @Inject
    lateinit var appDao: AppDao
    private val bankFunctionsItem: MutableList<BankFunctionsItem> by lazy { mutableListOf<BankFunctionsItem>() }
    private var iBankFunctionItemClick:IBankFunctionItemClick? = null

    var binding : FragmentBankFunctionsBinding? = null

    private val bankFunctionsViewModel : BankFunctionsViewModel by viewModels()
    private var dialogSuperAdminPassword:Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentBankFunctionsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        DialogUtilsNew1.hideKeyboardIfOpen(requireActivity())
        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.bank_functions_header)

        iBankFunctionItemClick = this
        bankFunctionsItem.clear()
        bankFunctionsItem.addAll(BankFunctionsItem.values())

        setupRecyclerview()

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }



    }


    private fun setupRecyclerview(){
        lifecycleScope.launch(Dispatchers.Main) {
            binding?.let {
                it.recyclerView.layoutManager = GridLayoutManager(activity, 1)
                it.recyclerView.adapter = BankFunctionsAdapter(iBankFunctionItemClick, bankFunctionsItem)
            }

        }
    }


    var onClickDialogOkCancel: OnClickDialogOkCancel = object : OnClickDialogOkCancel {

        override fun onClickOk(dialog: Dialog, password:String) {

            dialogSuperAdminPassword = dialog
            bankFunctionsViewModel.isSuperAdminPassword(password)?.observe(viewLifecycleOwner,{

                if(it)
                {
                    dialogSuperAdminPassword?.dismiss()
                    (activity as NavigationActivity).transactFragment(BankFunctionsAdminVasFragment())
                }else{
                    ToastUtils.showToast(requireContext(),R.string.invalid_password)
                }
            })
        }

        override fun onClickCancel() {

        }

    }



    override fun bankFunctionItemClick(bankFunctionsItem: BankFunctionsItem) {
        when(bankFunctionsItem){

            BankFunctionsItem.ADMIN_VAS ->{

                DialogUtilsNew1.showDialog(activity,getString(R.string.super_admin_password),getString(R.string.hint_enter_super_admin_password),onClickDialogOkCancel, false)
            }

            BankFunctionsItem.ADMIN_PAYMENT ->{

                // interact with another app
                DeviceHelper.showAdminFunction(object: OnOperationListener.Stub(){
                    override fun onCompleted(p0: OperationResult?) {
                        p0?.value?.apply {
                            println("Status = $status")
                            println("Response code = $responseCode")
                        }
                    }
                })
            }
        }
    }


}

interface IBankFunctionItemClick{

    fun bankFunctionItemClick(bankFunctionsItem:BankFunctionsItem)
}