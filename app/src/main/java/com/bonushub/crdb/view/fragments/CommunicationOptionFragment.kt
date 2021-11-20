package com.bonushub.crdb.view.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentCommunicationOptionBinding
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.model.local.TerminalParameterTable
import com.bonushub.crdb.utils.ToastUtils
import com.bonushub.crdb.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.utils.dialog.OnClickDialogOkCancel
import com.bonushub.crdb.utils.logger
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.adapter.BankFunctionsAdminVasAdapter
import com.bonushub.crdb.view.adapter.BankFunctionsCommParamAdapter
import com.bonushub.crdb.view.adapter.BankFunctionsTableEditAdapter
import com.bonushub.crdb.viewmodel.BankFunctionsViewModel
import com.bonushub.crdb.viewmodel.BatchFileViewModel
import com.bonushub.pax.utils.BankFunctionsAdminVasItem
import com.bonushub.pax.utils.CommunicationParamItem
import com.bonushub.pax.utils.PreferenceKeyConstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class CommunicationOptionFragment : Fragment(), ICommunicationOptionFragmentItemClick {

    private val cptListItem: MutableList<CommunicationParamItem> by lazy { mutableListOf<CommunicationParamItem>() }
    private var iCommunicationOptionFragmentItemClick:ICommunicationOptionFragmentItemClick? = null

    lateinit var bankFunctionsViewModel: BankFunctionsViewModel

    private lateinit var type :CommunicationParamItem
    lateinit var terminalParameterTable: TerminalParameterTable

    lateinit var binding: FragmentCommunicationOptionBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentCommunicationOptionBinding.inflate(inflater, container, false)
        return binding?.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        iCommunicationOptionFragmentItemClick = this
        bankFunctionsViewModel = ViewModelProvider(this).get(BankFunctionsViewModel::class.java)

        cptListItem.clear()
        cptListItem.addAll(CommunicationParamItem.values())
        setupRecyclerview()


    }

    private fun setupRecyclerview(){
        lifecycleScope.launch(Dispatchers.Main) {
            binding?.let {
                it.recyclerView.layoutManager = GridLayoutManager(activity, 1)
                it.recyclerView.adapter = BankFunctionsCommParamAdapter(iCommunicationOptionFragmentItemClick, cptListItem)
            }

        }
    }



    override fun CommunicationOptionItemClick(communicationParamItem: CommunicationParamItem) {

        when(communicationParamItem){

            CommunicationParamItem.TXN_PARAM -> {
                type = CommunicationParamItem.TXN_PARAM

                val bundle = Bundle()
                (activity as NavigationActivity).transactFragment(CommunicationOptionSubMenuFragment().apply {
                    arguments = bundle.apply {
                        putSerializable(
                            "type",
                            CommunicationParamItem.TXN_PARAM
                        )
                    }
                }, true)


            }

            CommunicationParamItem.APP_UPDATE_PARAM -> {
                type = CommunicationParamItem.APP_UPDATE_PARAM

                val bundle = Bundle()
                (activity as NavigationActivity).transactFragment(CommunicationOptionSubMenuFragment().apply {
                    arguments = bundle.apply {
                        putSerializable(
                            "type",
                            CommunicationParamItem.APP_UPDATE_PARAM
                        )
                    }
                }, true)
            }
        }

            //setUiForEdit()
    }

}

interface ICommunicationOptionFragmentItemClick{

    fun CommunicationOptionItemClick(communicationParamItem: CommunicationParamItem)
}

