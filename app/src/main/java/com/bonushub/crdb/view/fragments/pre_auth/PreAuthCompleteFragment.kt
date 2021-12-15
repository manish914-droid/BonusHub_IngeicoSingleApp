package com.bonushub.crdb.view.fragments.pre_auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentPreAuthCompleteBinding
import com.bonushub.crdb.databinding.FragmentPreAuthCompleteDetailBinding
import com.bonushub.crdb.utils.Field48ResponseTimestamp
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.base.IDialog
import com.bonushub.crdb.view.fragments.AuthCompletionData
import com.bonushub.pax.utils.EDashboardItem


class PreAuthCompleteFragment : Fragment() {

    private val authData: AuthCompletionData by lazy { AuthCompletionData() }
    var binding:FragmentPreAuthCompleteBinding? = null
    private var iDiag: IDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPreAuthCompleteBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        iDiag = (activity as NavigationActivity)

        binding?.subHeaderView?.subHeaderText?.text = "PRE-AUTH COMPLETE"
        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_preauth)
        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }

        binding?.authCompleteBtn?.setOnClickListener {




        }
    }

}