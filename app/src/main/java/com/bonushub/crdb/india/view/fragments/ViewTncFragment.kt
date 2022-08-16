package com.bonushub.crdb.india.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentViewTncBinding

class ViewTncFragment : Fragment() {

    lateinit var binding:FragmentViewTncBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_view_tnc, container, false)
        binding=FragmentViewTncBinding.inflate(inflater,container,false)
        return binding.root
    }

}