package com.bonushub.crdb.india.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bonushub.crdb.india.databinding.FragmentOnboardingTncBinding
import com.bonushub.crdb.india.view.adapter.OnboardingTncAdapter


class OnBoardingTncFragment : Fragment() {

   lateinit var binding: FragmentOnboardingTncBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_onboarding_tnc, container, false)
        binding=FragmentOnboardingTncBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //val adapter = OnboardingTncAdapter(supportFragmentManager)
        val adapter = OnboardingTncAdapter(supportFragmentManager = parentFragmentManager)
        adapter.addFragment(ViewTncFragment(),"PayLater")
        adapter.addFragment(ViewTncFragment(),"PayLater")
        adapter.addFragment(ViewTncFragment(),"PayLater")

        binding.viwPager.adapter=adapter
        binding.tabItem.setupWithViewPager(binding.viwPager)


    }
}