package com.bonushub.crdb.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.databinding.FragmentBankFunctionsAdminVasBinding
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.adapter.BankFunctionsAdminVasAdapter
import com.bonushub.pax.utils.BankFunctionsAdminVasItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class BankFunctionsAdminVasFragment : Fragment() , IBankFunctionsAdminVasItemClick{

    private val adminVasListItem: MutableList<BankFunctionsAdminVasItem> by lazy { mutableListOf<BankFunctionsAdminVasItem>() }
    private var iBankFunctionsAdminVasItemClick:IBankFunctionsAdminVasItemClick? = null

    var binding:FragmentBankFunctionsAdminVasBinding? = null
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

        iBankFunctionsAdminVasItemClick = this
        adminVasListItem.addAll(BankFunctionsAdminVasItem.values())
        setupRecyclerview()
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
            }

            BankFunctionsAdminVasItem.TEST_EMI ->{
                // TEST EMI
            }

            BankFunctionsAdminVasItem.TERMINAL_PARAM ->{
                // TERMINAL PARAM
                (activity as NavigationActivity).transactFragment(BankFunctionsTerminalFragment(), true)
            }

            BankFunctionsAdminVasItem.COMM_PARAM ->{
                // COMM PARAM
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
}

interface IBankFunctionsAdminVasItemClick{

    fun bankFunctionsAdminVasItemClick(bankFunctionsAdminVasItem:BankFunctionsAdminVasItem)
}