package com.bonushub.crdb.view.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.databinding.FragmentBankFunctionsTerminalBinding
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.model.local.TerminalCommunicationTable
import com.bonushub.crdb.model.local.TerminalParameterTable
import com.bonushub.crdb.view.adapter.BankFunctionsTerminalParamAdapter
import com.bonushub.crdb.viewmodel.BankFunctionsViewModel
import com.bonushub.pax.utils.BankFunctionsAdminVasItem
import com.bonushub.pax.utils.BankFunctionsTerminalItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BankFunctionsTerminalFragment : Fragment(), IBankFunctionsTerminalItemClick {

    private val terminalListItem: MutableList<BankFunctionsTerminalItem> by lazy { mutableListOf<BankFunctionsTerminalItem>() }
    private var iBankFunctionsTerminalItemClick:IBankFunctionsTerminalItemClick? = null
    var binding:FragmentBankFunctionsTerminalBinding? = null

    lateinit var bankFunctionsViewModel: BankFunctionsViewModel

    lateinit var dataList: ArrayList<TableEditHelper>

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
        bankFunctionsViewModel = ViewModelProvider(this).get(BankFunctionsViewModel::class.java)

//        terminalListItem.addAll(BankFunctionsTerminalItem.values())
//        dataList = ArrayList()
//
//        setupRecyclerview()

        lifecycleScope.launchWhenCreated {
            bankFunctionsViewModel.getTerminalParamField()
        }

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

class TableEditHelper(
    var titleName: String,
    var titleValue: String,
    var index:Int=0,
    var isUpdated: Boolean = false
) :
    Comparable<TableEditHelper> {
    override fun compareTo(other: TableEditHelper): Int =
        if (titleName > other.titleName) 1 else if (titleName < other.titleName) -1 else 0
}