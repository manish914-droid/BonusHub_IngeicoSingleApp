package com.bonushub.crdb.view.fragments.digi_pos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentUpiCollectBinding
import com.bonushub.crdb.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.utils.printerUtils.PrintUtil
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.base.BaseActivityNew
import com.bonushub.pax.utils.DigiPosItem
import com.bonushub.pax.utils.EPrintCopyType


class UpiCollectFragment : Fragment() {

    //lateinit var digiPosItemType:DigiPosItem
    var amount = ""
    var vpa = ""
    var mobile = ""
    var des = ""

    var binding:FragmentUpiCollectBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentUpiCollectBinding.inflate(inflater,container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //digiPosItemType = arguments?.getSerializable("type") as DigiPosItem
        amount = arguments?.getSerializable("amount").toString()
        vpa = arguments?.getSerializable("vpa").toString()
        mobile = arguments?.getSerializable("mobile").toString()

        binding?.txtViewAmount?.text = amount
        binding?.txtViewVpa?.text = vpa
        binding?.txtViewMobile?.text = mobile

        // temp for text
        binding?.txtViewAmount?.text = "1,999.00"
        binding?.txtViewVpa?.text = "ABC@YBL"
        binding?.txtViewMobile?.text = "9942424299"


        binding?.subHeaderView?.subHeaderText?.text = "UPI COLLECT"
        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_upi)

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            try {
                parentFragmentManager.popBackStackImmediate()
            }catch (ex:Exception)
            {
                ex.printStackTrace()
            }
        }

        binding?.btnProceed?.setOnClickListener {
            des = binding?.enterDescriptionEt?.text.toString().trim()
            DialogUtilsNew1.hideKeyboardIfOpen(requireActivity())

            // progress dialog
           // (activity as NavigationActivity).showProgress("Sending/Receiving from Host")

            // payment dialog
//            (activity as NavigationActivity).alertBoxWithAction("","Payment link has been sent.\nWould you like to check payment status now.",
//                true,"Yes",{
//
//                }, {
//
//                },R.drawable.ic_link_circle)

            // Printer Dialog
//            (activity as NavigationActivity).alertBoxWithAction(
//                getString(R.string.print_customer_copy),
//                getString(R.string.print_customer_copy),
//                true, getString(R.string.positive_button_yes), {
//
//                }, {
//
//                })


            // Transaction Approved dialog
           // (activity as NavigationActivity).alertBoxMsgWithIconOnly(R.drawable.ic_tick,"Transaction Approved")
            //(activity as NavigationActivity).hideProgress()


        }

    }
}