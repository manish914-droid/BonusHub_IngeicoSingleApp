package com.bonushub.crdb.india.view.fragments.pre_auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentPreAuthCompleteBinding
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.base.IDialog
import com.bonushub.crdb.india.view.fragments.AuthCompletionData


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