package com.bonushub.crdb.india.view.fragments.pre_auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentPreAuthVoidBinding


class PreAuthVoidFragment : Fragment() {


    var binding :FragmentPreAuthVoidBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPreAuthVoidBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.subHeaderView?.subHeaderText?.text = "VOID PREAUTH"
        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_preauth)
        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }


    }
}