package com.bonushub.crdb.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentBankFunctionsAdminVasBinding
import com.bonushub.crdb.databinding.FragmentBankFunctionsBinding
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.adapter.BankFunctionsAdapter
import com.bonushub.crdb.view.adapter.BankFunctionsAdminVasAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class BankFunctionsAdminVasFragment : Fragment() {



    var binding:FragmentBankFunctionsAdminVasBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
       // return inflater.inflate(R.layout.fragment_bank_functions_admin_vas, container, false)
        binding = FragmentBankFunctionsAdminVasBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerview()
    }

    private fun setupRecyclerview(){
        lifecycleScope.launch(Dispatchers.Main) {
            binding?.let {
                it.recyclerView.layoutManager = GridLayoutManager(activity, 1)
                it.recyclerView.adapter = BankFunctionsAdminVasAdapter(this@BankFunctionsAdminVasFragment)
            }

        }
    }

    fun itemClick(position:Int)
    {
        when(position){
            0 ->{
                // INIT
            }

            1 ->{
                // TEST EMI
            }

            2 ->{
                // TERMINAL PARAM
                (activity as NavigationActivity).transactFragment(BankFunctionsTerminalFragment())
            }

            3 ->{
                // COMM PARAM
            }

            4 ->{
                // ENV PARAM
            }

            5 ->{
                // INIT PAYMENT APP
                (activity as NavigationActivity).transactFragment(BankFunctionsInitPaymentAppFragment())
            }
        }
    }
}