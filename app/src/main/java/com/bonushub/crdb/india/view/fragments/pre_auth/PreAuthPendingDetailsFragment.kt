package com.bonushub.crdb.india.view.fragments.pre_auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentPreAuthPendingDetailsBinding
import com.bonushub.crdb.india.utils.logger
import com.bonushub.crdb.india.utils.refreshSubToolbarLogos


class PreAuthPendingDetailsFragment : Fragment() {


    var binding :FragmentPreAuthPendingDetailsBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPreAuthPendingDetailsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

       /* binding?.subHeaderView?.subHeaderText?.text = "PRE-AUTH"
        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_preauth)*/
        refreshSubToolbarLogos(this,null,R.drawable.ic_preauth, "PRE-AUTH")

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }

        // temp
        binding?.txtViewBatch?.text = "000075"
        binding?.txtViewRoc?.text = "000154"
        binding?.txtViewPan?.text = "5413********0078"
        binding?.txtViewAmt?.text = "10.00"
        binding?.txtViewDate?.text = "22/01/2021"
        binding?.txtViewTime?.text = "16:23:33"

        //binding?.amountEt

        binding?.txtViewComplete?.setOnClickListener {
            logger("temp","complete")
        }


        binding?.txtViewPrint?.setOnClickListener {
            logger("temp","print")
        }
    }
}