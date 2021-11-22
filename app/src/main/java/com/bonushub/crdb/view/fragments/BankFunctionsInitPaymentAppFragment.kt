package com.bonushub.crdb.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentBankFunctionsInitPaymentAppBinding
import com.bonushub.crdb.view.adapter.BankFunctionsAdminVasAdapter
import com.bonushub.crdb.view.adapter.BankFunctionsInitPaymentAppAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BankFunctionsInitPaymentAppFragment : Fragment() {


    var binding: FragmentBankFunctionsInitPaymentAppBinding? = null

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

        setupRecyclerview()

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }
    }

    private fun setupRecyclerview(){
        lifecycleScope.launch(Dispatchers.Main) {
            binding?.let {
                it.recyclerView.layoutManager = GridLayoutManager(activity, 1)
                it.recyclerView.adapter = BankFunctionsInitPaymentAppAdapter()
            }

        }
    }


}