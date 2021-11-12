package com.bonushub.crdb.view.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentBankFunctionsBinding
import com.bonushub.crdb.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.utils.dialog.OnClickDialogOkCancel
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.adapter.BankFunctionsAdapter
import com.bonushub.crdb.viewmodel.BankFunctionsViewModel
import com.bonushub.pax.utils.BankFunctionsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BankFunctionsFragment : Fragment(), IBankFunctionItemClick {

    private val bankFunctionsItem: MutableList<BankFunctionsItem> by lazy { mutableListOf<BankFunctionsItem>() }
    private var iBankFunctionItemClick:IBankFunctionItemClick? = null
    lateinit var bankFunctionsViewModel: BankFunctionsViewModel

    var binding : FragmentBankFunctionsBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentBankFunctionsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        iBankFunctionItemClick = this
        bankFunctionsItem.addAll(BankFunctionsItem.values())
        bankFunctionsViewModel = ViewModelProvider(this).get(BankFunctionsViewModel::class.java)

        setupRecyclerview()
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

            bankFunctionsViewModel.isSuperAdminPassword(password)?.observe(requireActivity(),{

                if(it)
                {
                    dialog.dismiss()
                    (activity as NavigationActivity).transactFragment(BankFunctionsAdminVasFragment())
                }else{
                    Toast.makeText(requireContext(),"Password not match", Toast.LENGTH_LONG).show()
                }
            })

            //(activity as NavigationActivity).transactFragment(BankFunctionsAdminVasFragment())
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
            }
        }
    }


}

interface IBankFunctionItemClick{

    fun bankFunctionItemClick(bankFunctionsItem:BankFunctionsItem)
}