package com.bonushub.crdb.view.fragments.digi_pos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bonushub.crdb.databinding.FragmentQrBinding
import com.bonushub.crdb.utils.logger
import com.bonushub.pax.utils.EDashboardItem


class QrFragment : Fragment() {

    var binding:FragmentQrBinding? = null
    lateinit var transactionType:EDashboardItem

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentQrBinding.inflate(inflater,container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionType = arguments?.getSerializable("type") as EDashboardItem

        binding?.subHeaderView?.subHeaderText?.text = transactionType.title
        binding?.subHeaderView?.headerImage?.setImageResource(transactionType.res)

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            try {
                parentFragmentManager.popBackStackImmediate()
            }catch (ex:Exception)
            {
                ex.printStackTrace()
            }
        }


        binding?.btnNo?.setOnClickListener {
            logger("btnNo","click","e")
        }

        binding?.btnYes?.setOnClickListener {
            logger("btnYes","click","e")
        }

    }
}