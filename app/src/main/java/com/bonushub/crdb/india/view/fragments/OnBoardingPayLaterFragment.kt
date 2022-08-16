package com.bonushub.crdb.india.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentOnBoardingPayLaterBinding
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.viewmodel.ModelOnboarding
import com.bonushub.crdb.india.view.adapter.PayLaterOnboardingAdapter

class OnBoardingPayLaterFragment : Fragment() {
    lateinit var binding:FragmentOnBoardingPayLaterBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentOnBoardingPayLaterBinding.inflate(layoutInflater,container,false)
        // Inflate the layout for this fragment
       // return inflater.inflate(R.layout.fragment_pay_later_on_boarding, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as NavigationActivity).manageTopToolBar(false)

        binding.subHeaderView.backImageButton.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }

        binding.pocedBtn.setOnClickListener {
            (activity as NavigationActivity).transactFragment(MobileNumberVerification().apply {
                }, isBackStackAdded = true)
        }

        binding.subHeaderView.subHeaderView.setOnClickListener {
            (activity as NavigationActivity).transactFragment(TextSpeechFragment().apply {
            }, isBackStackAdded = true)
        }

        binding.anoLyt.setOnClickListener {
            (activity as NavigationActivity).transactFragment(OnBoardingTncFragment().apply {
            }, isBackStackAdded = true)
        }
        val mlist=ArrayList<ModelOnboarding>()

        mlist.add(ModelOnboarding(R.drawable.ic_pay_later, getString(R.string.pay_later)))
        mlist.add(ModelOnboarding(R.drawable.ic_pay_later, getString(R.string.pay_later)))
        mlist.add(ModelOnboarding(R.drawable.ic_pay_later, getString(R.string.pay_later)))

        val adapter=PayLaterOnboardingAdapter(mlist)
        binding.onboardingRecycleView.adapter=adapter
        binding.onboardingRecycleView.layoutManager=LinearLayoutManager(activity)
    }
}