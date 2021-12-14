package com.bonushub.crdb.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentBankFunctionsAdminVasBinding
import com.bonushub.crdb.di.DBModule
import com.bonushub.crdb.utils.ToastUtils
import com.bonushub.crdb.utils.Utility
import com.bonushub.crdb.utils.checkBaseTid
import com.bonushub.crdb.utils.logger
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.adapter.BankFunctionsAdminVasAdapter
import com.bonushub.crdb.view.base.IDialog
import com.bonushub.crdb.viewmodel.InitViewModel
import com.bonushub.pax.utils.BankFunctionsAdminVasItem
import com.mindorks.example.coroutines.utils.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.ArrayList

@AndroidEntryPoint
class BankFunctionsAdminVasFragment : Fragment() , IBankFunctionsAdminVasItemClick{

    private val adminVasListItem: MutableList<BankFunctionsAdminVasItem> by lazy { mutableListOf<BankFunctionsAdminVasItem>() }
    private var iBankFunctionsAdminVasItemClick:IBankFunctionsAdminVasItemClick? = null
    var binding:FragmentBankFunctionsAdminVasBinding? = null

    private val initViewModel : InitViewModel by viewModels()
    // for init`
    private var iDialog: IDialog? = null


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
            parentFragmentManager.popBackStackImmediate()
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
                iDialog?.showProgress(getString(R.string.please_wait_host))

                runBlocking {
                    val tids = checkBaseTid(DBModule.appDatabase?.appDao)

                    if(!tids.get(0).isEmpty()!!) {

                        logger("get tid", "by table")
                        // get tid from table and init

                        initViewModel.insertInfo1(tids[0] ?:"")
                        observeMainViewModel()
                    }else{
                       // get tid by user
                        logger("get tid","by user")
                        //navHostFragment?.navController?.popBackStack()
                        (activity as NavigationActivity).transactFragment(InitFragment())
                    }
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

//            BankFunctionsAdminVasItem.ENV_PARAM ->{
//                // ENV PARAM
//            }

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
                    CoroutineScope(Dispatchers.IO).launch{
                        Utility().readInitServer(result?.data?.data as ArrayList<ByteArray>) { result, message ->
                            iDialog?.hideProgress()
                            CoroutineScope(Dispatchers.Main).launch {
                                (activity as? NavigationActivity)?.alertBoxWithAction("", requireContext().getString(R.string.successfull_init),
                                    false, "", {}, {})
                            }
                        }

                    }
                }
                Status.ERROR -> {
                    iDialog?.hideProgress()
                    CoroutineScope(Dispatchers.Main).launch {
                        (activity as? NavigationActivity)?.getInfoDialog("Error", result.error ?: "") {}
                    }
                   // ToastUtils.showToast(activity,"Error called  ${result.error}")
                }
                Status.LOADING -> {
                    iDialog?.showProgress("Sending/Receiving From Host")

                }
            }

        })


    }
}

interface IBankFunctionsAdminVasItemClick{

    fun bankFunctionsAdminVasItemClick(bankFunctionsAdminVasItem:BankFunctionsAdminVasItem)
}