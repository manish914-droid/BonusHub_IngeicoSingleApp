package com.bonushub.crdb.view.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.databinding.FragmentBankFunctionsTerminalBinding
import com.bonushub.crdb.view.adapter.BankFunctionsTerminalParamAdapter
import com.bonushub.pax.utils.BankFunctionsAdminVasItem
import com.bonushub.pax.utils.BankFunctionsTerminalItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BankFunctionsTerminalFragment : Fragment(), IBankFunctionsTerminalItemClick {

    private val terminalListItem: MutableList<BankFunctionsTerminalItem> by lazy { mutableListOf<BankFunctionsTerminalItem>() }
    private var iBankFunctionsTerminalItemClick:IBankFunctionsTerminalItemClick? = null
    var binding:FragmentBankFunctionsTerminalBinding? = null

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
        terminalListItem.addAll(BankFunctionsTerminalItem.values())
        setupRecyclerview()
    }

    private fun setupRecyclerview(){
        lifecycleScope.launch(Dispatchers.Main) {
            binding?.let {
                it.recyclerView.layoutManager = GridLayoutManager(activity, 1)
                it.recyclerView.adapter = BankFunctionsTerminalParamAdapter(iBankFunctionsTerminalItemClick,terminalListItem)
            }

        }
    }

    override fun bankFunctionsTerminalItemClick(bankFunctionsTerminalItem: BankFunctionsTerminalItem) {
        // implement when need
        Log.d("teminal item",""+bankFunctionsTerminalItem._name)
    }
}

interface IBankFunctionsTerminalItemClick{

    fun bankFunctionsTerminalItemClick(bankFunctionsTerminalItem: BankFunctionsTerminalItem)
}