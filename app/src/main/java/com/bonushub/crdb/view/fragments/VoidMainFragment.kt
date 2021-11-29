package com.bonushub.crdb.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentBankFunctionsAdminVasBinding
import com.bonushub.crdb.databinding.FragmentVoidMainBinding
import com.bonushub.crdb.utils.logger
import com.bonushub.crdb.view.activity.NavigationActivity


class VoidMainFragment : Fragment() {


    var binding:FragmentVoidMainBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
         binding = FragmentVoidMainBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // set header
        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.void_sale)
        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_void)


        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }

        binding?.txtViewSearchTransaction?.setOnClickListener {
            logger("txtViewSearchTransaction","click")

            (activity as NavigationActivity).transactFragment(VoidDetailFragment())
        }
    }
}