package com.bonushub.crdb.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentVoidDetailBinding
import com.bonushub.crdb.databinding.FragmentVoidMainBinding
import com.bonushub.crdb.utils.logger


class VoidDetailFragment : Fragment() {

    var binding:FragmentVoidDetailBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentVoidDetailBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // set header
        binding?.subHeaderView?.subHeaderText?.text = ""

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }

        binding?.txtViewVoidSale?.setOnClickListener {
            logger("void sale","click")
        }

        // temp data
        binding?.txtViewDate?.text = "01/04/2021"
        binding?.txtViewTime?.text = "16:25:34"
        binding?.txtViewTid?.text = "41501372"
        binding?.txtViewInvoiceNumber?.text = "000139"
        binding?.txtViewTotalAmount?.text = "2565.00"

    }
}