package com.bonushub.crdb.india.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bonushub.crdb.india.databinding.FragmentPayLaterBinding
import com.bonushub.crdb.india.view.activity.NavigationActivity


class PayLaterFragment : Fragment() {

    lateinit var binding:FragmentPayLaterBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
       // return inflater.inflate(R.layout.fragment_pay_later, container, false)
        binding=FragmentPayLaterBinding.inflate(layoutInflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as NavigationActivity).manageTopToolBar(false)

        binding.subHeaderView.backImageButton.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }

        //binding.key2.startAnimation(animShow)
        binding.payLatrYes.setOnClickListener {
            //(activity as NavigationActivity).manageTopToolBar(false)

                (activity as NavigationActivity).transactFragment(OnBoardingPayLaterFragment().apply {

                }, isBackStackAdded = true)
        }

        binding.payLatrNo.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }
    }

}