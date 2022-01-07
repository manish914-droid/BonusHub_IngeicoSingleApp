package com.bonushub.crdb.view.fragments.digi_pos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentUpiSmsDynamicPayQrInputDetailBinding
import com.bonushub.pax.utils.DigiPosItem
import com.bonushub.pax.utils.EDashboardItem


class UpiSmsDynamicPayQrInputDetailFragment : Fragment() {

    var binding:FragmentUpiSmsDynamicPayQrInputDetailBinding? = null
    lateinit var digiPosItemType:DigiPosItem

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        binding = FragmentUpiSmsDynamicPayQrInputDetailBinding.inflate(inflater,container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        digiPosItemType = arguments?.getSerializable("type") as DigiPosItem

        binding?.subHeaderView?.subHeaderText?.text = digiPosItemType.title
        binding?.subHeaderView?.headerImage?.setImageResource(digiPosItemType.res)

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            try {
                parentFragmentManager.popBackStackImmediate()
            }catch (ex:Exception)
            {
                ex.printStackTrace()
            }
        }

        // region set ui
        when(digiPosItemType)
        {
            DigiPosItem.UPI ->{
                binding?.enterAmountCrdView?.visibility = View.VISIBLE
                binding?.vpaCrdView?.visibility = View.VISIBLE
                binding?.mobileNumberCrdView?.visibility = View.VISIBLE
                binding?.enterDescriptionCrdView?.visibility = View.GONE
            }

            DigiPosItem.DYNAMIC_QR, DigiPosItem.SMS_PAY ->{
                binding?.enterAmountCrdView?.visibility = View.VISIBLE
                binding?.vpaCrdView?.visibility = View.GONE
                binding?.mobileNumberCrdView?.visibility = View.VISIBLE
                binding?.enterDescriptionCrdView?.visibility = View.VISIBLE
            }

            else ->{
                binding?.enterAmountCrdView?.visibility = View.GONE
                binding?.vpaCrdView?.visibility = View.GONE
                binding?.mobileNumberCrdView?.visibility = View.GONE
                binding?.enterDescriptionCrdView?.visibility = View.GONE
            }
        }
        // end region
    }
}