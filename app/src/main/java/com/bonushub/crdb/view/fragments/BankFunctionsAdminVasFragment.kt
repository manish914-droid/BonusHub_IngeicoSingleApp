package com.bonushub.crdb.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentBankFunctionsAdminVasBinding
import com.bonushub.crdb.model.local.TerminalParameterTable
import com.bonushub.crdb.utils.logger
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.adapter.BankFunctionsAdminVasAdapter
import com.bonushub.crdb.view.base.IDialog
import com.bonushub.crdb.viewmodel.BankFunctionsViewModel
import com.bonushub.crdb.viewmodel.InitViewModel
import com.bonushub.pax.utils.BankFunctionsAdminVasItem
import com.mindorks.example.coroutines.utils.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BankFunctionsAdminVasFragment : Fragment() , IBankFunctionsAdminVasItemClick{

    private val adminVasListItem: MutableList<BankFunctionsAdminVasItem> by lazy { mutableListOf<BankFunctionsAdminVasItem>() }
    private var iBankFunctionsAdminVasItemClick:IBankFunctionsAdminVasItemClick? = null
    var binding:FragmentBankFunctionsAdminVasBinding? = null

   // private lateinit var initViewModel : InitViewModel
   private val initViewModel : InitViewModel by viewModels()

    lateinit var bankFunctionsViewModel: BankFunctionsViewModel
    lateinit var terminalParameterTable: TerminalParameterTable

    // for init
    //private var iDialog: IDialog? = null


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

        //if(context is IDialog) iDialog = context

        iBankFunctionsAdminVasItemClick = this
        bankFunctionsViewModel = ViewModelProvider(this).get(BankFunctionsViewModel::class.java)
//        initViewModel = ViewModelProvider(this).get(InitViewModel::class.java)


        adminVasListItem.clear()
        adminVasListItem.addAll(BankFunctionsAdminVasItem.values())
        setupRecyclerview()

        lifecycleScope.launch(Dispatchers.Main) {
            bankFunctionsViewModel.getTerminalParameterTable()?.observe(viewLifecycleOwner,{

                terminalParameterTable = it

            })
        }
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
                // INIT
               // iDialog?.showProgress(getString(R.string.please_wait_host))

                if(this::terminalParameterTable.isInitialized && !terminalParameterTable.terminalId.isEmpty()){

                    logger("get tid","by table")
                    // get tid from table and init
                    initViewModel.insertInfo1(terminalParameterTable.terminalId)
                    observeMainViewModel()


                }else{
                    // get tid by user
                    logger("get tid","by user")
                    //navHostFragment?.navController?.popBackStack()
                    (activity as NavigationActivity).transactFragment(InitFragment())

                }

            }

            BankFunctionsAdminVasItem.TEST_EMI ->{
                // TEST EMI depends on bank emi

            }

            BankFunctionsAdminVasItem.TERMINAL_PARAM ->{
                // TERMINAL PARAM
                (activity as NavigationActivity).transactFragment(BankFunctionsTerminalFragment(), true)
            }

            BankFunctionsAdminVasItem.COMM_PARAM ->{
                // COMM PARAM
                (activity as NavigationActivity).transactFragment(CommunicationOptionFragment(), true)
            }

            BankFunctionsAdminVasItem.ENV_PARAM ->{
                // ENV PARAM
            }

            BankFunctionsAdminVasItem.INIT_PAYMENT_APP ->{
                // INIT PAYMENT APP
                (activity as NavigationActivity).transactFragment(BankFunctionsInitPaymentAppFragment(), true)
            }
        }
    }

    private fun observeMainViewModel(){

        initViewModel.initData.observe(viewLifecycleOwner, Observer { result ->

            when (result.status) {
                Status.SUCCESS -> {
                   // iDialog?.hideProgress()
                    (activity as NavigationActivity).transactFragment(DashboardFragment())
                }
                Status.ERROR -> {
                    //iDialog?.hideProgress()
                    Toast.makeText(activity,"Error called  ${result.error}", Toast.LENGTH_LONG).show()
                }
                Status.LOADING -> {
                    //iDialog?.showProgress("Sending/Receiving From Host")

                }
            }

        })


    }
}

interface IBankFunctionsAdminVasItemClick{

    fun bankFunctionsAdminVasItemClick(bankFunctionsAdminVasItem:BankFunctionsAdminVasItem)
}