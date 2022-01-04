package com.bonushub.crdb.view.fragments

import android.app.Dialog
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
import com.bonushub.crdb.databinding.FragmentBankFunctionsAdminVasBinding
import com.bonushub.crdb.di.DBModule
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.utils.Field48ResponseTimestamp.checkInternetConnection
import com.bonushub.crdb.utils.ToastUtils
import com.bonushub.crdb.utils.Utility
import com.bonushub.crdb.utils.checkBaseTid
import com.bonushub.crdb.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.utils.dialog.OnClickDialogOkCancel
import com.bonushub.crdb.utils.logger
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.adapter.BankFunctionsAdminVasAdapter
import com.bonushub.crdb.view.base.IDialog
import com.bonushub.crdb.view.fragments.tets_emi.TestEmiFragment
import com.bonushub.crdb.viewmodel.BankFunctionsViewModel
import com.bonushub.crdb.viewmodel.BatchFileViewModel
import com.bonushub.crdb.viewmodel.InitViewModel
import com.bonushub.pax.utils.BankFunctionsAdminVasItem
import com.bonushub.pax.utils.PreferenceKeyConstant
import com.mindorks.example.coroutines.utils.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

@AndroidEntryPoint
class BankFunctionsAdminVasFragment : Fragment() , IBankFunctionsAdminVasItemClick{

    private val adminVasListItem: MutableList<BankFunctionsAdminVasItem> by lazy { mutableListOf<BankFunctionsAdminVasItem>() }
    private var iBankFunctionsAdminVasItemClick:IBankFunctionsAdminVasItemClick? = null
    var binding:FragmentBankFunctionsAdminVasBinding? = null

    private val initViewModel : InitViewModel by viewModels()
    private val bankFunctionsViewModel : BankFunctionsViewModel by viewModels()

    // for init`
    private var iDialog: IDialog? = null

    private val batchFileViewModel: BatchFileViewModel by viewModels()


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
            try {
                parentFragmentManager.popBackStackImmediate()
            }catch (ex:Exception)
            {
                ex.printStackTrace()
            }
        }

        observeMainViewModel()
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

                if (checkInternetConnection()) {

                    if(!AppPreference.getBoolean(AppPreference.LOGIN_KEY)){
                        //showEnterTIDPopUp
                        DialogUtilsNew1.getInputDialog(requireContext(),"ENTER TID","",true,true,"TID") {

                            if(it.length < 8){
                            ToastUtils.showToast(requireContext(), "Please enter a valid 8 digit TID")
                            }else {
                                initViewModel.insertInfo1(it)
                                //observeMainViewModel()
                            }
                        }

                    }else{
                        // check batch open or not

                        lifecycleScope.launch(Dispatchers.Main) {

                            batchFileViewModel.getBatchTableData()
                                .observe(viewLifecycleOwner, { batchData ->

                                    when {
                                        AppPreference.getBoolean(PreferenceKeyConstant.SERVER_HIT_STATUS.keyName.toString()) ->
                                            ToastUtils.showToast(
                                                requireContext(),
                                                getString(R.string.please_clear_fbatch_before_init)
                                            )

                                        !TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY)) ->
                                            ToastUtils.showToast(
                                                requireContext(),
                                                getString(R.string.reversal_found_please_clear_or_settle_first_before_init)
                                            )

                                        batchData.size > 0 -> ToastUtils.showToast(
                                            requireContext(),
                                            getString(R.string.please_settle_batch_first_before_init)
                                        )
                                        else -> {
                                            startFullInitProcess()
                                        }
                                    }
                                })

                        }
                    }

                } else {
                    ToastUtils.showToast(requireContext(),getString(R.string.no_internet_available_please_check_your_internet))
                }



            }

            BankFunctionsAdminVasItem.TEST_EMI ->{
                // TEST EMI depends on bank emi
                if(AppPreference.getLogin()){

                    if(checkInternetConnection())
                    {
                        DialogUtilsNew1.showDialog(activity,getString(R.string.super_admin_password),getString(R.string.hint_enter_super_admin_password),object:OnClickDialogOkCancel{
                            override fun onClickOk(dialog: Dialog, password: String) {

                                logger("password",password)
                                bankFunctionsViewModel.isSuperAdminPassword(password)?.observe(viewLifecycleOwner,{

                                    if(it)
                                    {
                                        dialog.dismiss()
                                        (activity as NavigationActivity).transactFragment(TestEmiFragment(), true)


                                    }else{
                                        ToastUtils.showToast(requireContext(),R.string.invalid_password)
                                    }
                                })

                            }

                            override fun onClickCancel() {

                            }

                        }, false)

                    }else{
                        ToastUtils.showToast(requireContext(),getString(R.string.no_internet_available_please_check_your_internet))
                    }
                }else{
                    ToastUtils.showToast(requireContext(),"** Initialize Terminal **")
                }

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

    private fun startFullInitProcess() {

        // INIT
        // iDialog?.showProgress(getString(R.string.please_wait_host))

        runBlocking {
            val tids = checkBaseTid(DBModule.appDatabase?.appDao)

            if(!tids.get(0).isEmpty()!!) {

                logger("get tid", "by table")
                // get tid from table and init

                initViewModel.insertInfo1(tids[0] ?:"")
                //observeMainViewModel()
            }else{
               // get tid by user
                logger("get tid","by user")

                DialogUtilsNew1.getInputDialog(requireContext(),"ENTER TID","",true,true,"TID") {

                    if(it.length < 8){
                        ToastUtils.showToast(requireContext(), "Please enter a valid 8 digit TID")
                    }else {
                        initViewModel.insertInfo1(it)
                    }
                }
                //(activity as NavigationActivity).transactFragment(InitFragment())
            }
        }
    }

    private fun observeMainViewModel(){

        initViewModel.initData.observe(viewLifecycleOwner, { result ->

            if(!isFromStop) {
                when (result.status) {
                    Status.SUCCESS -> {
                        CoroutineScope(Dispatchers.IO).launch {
                            Utility().readInitServer(result?.data?.data as ArrayList<ByteArray>) { result, message ->
                                iDialog?.hideProgress()
                                CoroutineScope(Dispatchers.Main).launch {
                                    (activity as? NavigationActivity)?.alertBoxMsgWithIconOnly(R.drawable.ic_tick,
                                        requireContext().getString(R.string.successfull_init))
                                }
                            }

                        }
                    }
                    Status.ERROR -> {
                        iDialog?.hideProgress()
                        CoroutineScope(Dispatchers.Main).launch {
                            (activity as? NavigationActivity)?.getInfoDialog(
                                "Error",
                                result.error ?: ""
                            ) {}
                        }
                        // ToastUtils.showToast(activity,"Error called  ${result.error}")
                    }
                    Status.LOADING -> {
                        iDialog?.showProgress(getString(R.string.sending_receiving_host))

                    }
                }
            }else{
                isFromStop = false
            }
        })


    }

    var isFromStop = false

    override fun onStop() {
        super.onStop()
        logger("kush","rem")
        isFromStop = true
    }
}

interface IBankFunctionsAdminVasItemClick{

    fun bankFunctionsAdminVasItemClick(bankFunctionsAdminVasItem:BankFunctionsAdminVasItem)
}