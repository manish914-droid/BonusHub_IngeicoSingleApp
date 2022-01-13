package com.bonushub.crdb.view.fragments.digi_pos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentQrBinding
import com.bonushub.crdb.databinding.FragmentUpiSmsDynamicPayQrInputDetailBinding
import com.bonushub.crdb.utils.logger
import com.bonushub.pax.utils.DigiPosItem


class QrFragment : Fragment() {

    var binding:FragmentQrBinding? = null
    lateinit var digiPosItemType:DigiPosItem

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


        binding?.btnNo?.setOnClickListener {
            logger("btnNo","click","e")
        }

        binding?.btnYes?.setOnClickListener {
            logger("btnYes","click","e")
        }

    }
}