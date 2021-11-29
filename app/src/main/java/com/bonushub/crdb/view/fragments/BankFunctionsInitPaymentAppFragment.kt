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
import com.bonushub.crdb.databinding.FragmentBankFunctionsInitPaymentAppBinding
import com.bonushub.crdb.di.DBModule
import com.bonushub.crdb.utils.ToastUtils
import com.bonushub.crdb.utils.checkBaseTid
import com.bonushub.crdb.utils.logger
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.adapter.BankFunctionsInitPaymentAppAdapter
import com.bonushub.crdb.view.base.IDialog
import com.bonushub.crdb.viewmodel.BankFunctionsViewModel
import com.bonushub.crdb.viewmodel.InitViewModel
import com.mindorks.example.coroutines.utils.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class BankFunctionsInitPaymentAppFragment : Fragment() {

    private var iDialog: IDialog? = null
    private val initViewModel : InitViewModel by viewModels()

    var binding: FragmentBankFunctionsInitPaymentAppBinding? = null
    private val bankFunctionsViewModel : BankFunctionsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentBankFunctionsInitPaymentAppBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.init_payment_app_header)

        iDialog = (activity as NavigationActivity)

        lifecycleScope.launch {

            bankFunctionsViewModel.getAllTidsWithStatus()?.observe(viewLifecycleOwner,{
                    //tpt ->

                setupRecyclerview(it)

                //logger("tpt","data"+tpt.LinkTidType.toString())
                // link tid type use for find type of child tid , tid type use for find base tid
                /*var tidType = tpt.tidType
                var linkTidType = tpt.LinkTidType
                var tids = tpt.terminalId*/

                // temp
                /*var tidType = listOf<String>("0","1","0","0","0")
                var linkTidType = listOf<String>("0","1","2","3","9")
                var tids = listOf<String>("30160043","30160033","30160044","30160045","30160048")*/

                /*
                if tidtype value is  1 then it is base Tid
                else - child tids
                and
                LinkTidType :
                for Amex             - 0
                DC type              - 1
                offus Tid            - 2
                3 months onus        - 3
                6 months onus        - 6
                9 months onus        - 9
                12 months onus       - 12
                and so on*/



            })


        }



        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }

        binding?.textViewInitAllTids?.setOnClickListener {

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
                    (activity as NavigationActivity).transactFragment(InitFragment())
                }
            }
        }
    }

    private fun setupRecyclerview(tidsWithStatusList:ArrayList<TidsListModel>){
        lifecycleScope.launch(Dispatchers.Main) {
            binding?.let {
                it.recyclerView.layoutManager = GridLayoutManager(activity, 1)
                it.recyclerView.adapter = BankFunctionsInitPaymentAppAdapter(tidsWithStatusList)
            }

        }
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

data class TidsListModel(var tids:String, var des:String,var status:String)