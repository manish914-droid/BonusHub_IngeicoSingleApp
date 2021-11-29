package com.bonushub.crdb.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentCommunicationOptionBinding
import com.bonushub.crdb.model.local.TerminalParameterTable
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.adapter.BankFunctionsCommParamAdapter
import com.bonushub.crdb.viewmodel.BankFunctionsViewModel
import com.bonushub.pax.utils.CommunicationParamItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CommunicationOptionFragment : Fragment(), ICommunicationOptionFragmentItemClick {

    private val cptListItem: MutableList<CommunicationParamItem> by lazy { mutableListOf<CommunicationParamItem>() }
    private var iCommunicationOptionFragmentItemClick:ICommunicationOptionFragmentItemClick? = null

    private val bankFunctionsViewModel: BankFunctionsViewModel by viewModels()

    private lateinit var type :CommunicationParamItem
    lateinit var terminalParameterTable: TerminalParameterTable

    var binding: FragmentCommunicationOptionBinding? = null

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

        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.communication_param_header)

        iCommunicationOptionFragmentItemClick = this

        cptListItem.clear()
        cptListItem.addAll(CommunicationParamItem.values())
        setupRecyclerview()


        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }

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

