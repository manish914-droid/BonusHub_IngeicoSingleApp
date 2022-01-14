package com.bonushub.crdb.view.fragments.digi_pos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentPendingTxnBinding
import com.bonushub.pax.utils.EDashboardItem


class SearchTxnFragment : Fragment() {

    lateinit var transactionType:EDashboardItem
    var binding:FragmentPendingTxnBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPendingTxnBinding.inflate(inflater,container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionType = arguments?.getSerializable("type") as EDashboardItem

        binding?.linLayPendingTnx?.visibility = View.GONE
        binding?.linLaySearch?.visibility = View.VISIBLE

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

        binding?.txtViewPendingTxn?.setBackgroundColor(resources.getColor(R.color.txt_color_transparent))
        binding?.txtViewSearch?.setBackgroundColor(resources.getColor(R.color.txt_color))

        binding?.txtViewPendingTxn?.setOnClickListener {
            try {
                parentFragmentManager.popBackStackImmediate()
            }catch (ex:Exception)
            {
                ex.printStackTrace()
            }
        }

        binding?.btnSearch?.setOnClickListener {
            binding?.searchEt?.setText("search btn click")
        }

    }
}