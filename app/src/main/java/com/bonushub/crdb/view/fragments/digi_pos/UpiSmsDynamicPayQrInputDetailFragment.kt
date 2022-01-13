package com.bonushub.crdb.view.fragments.digi_pos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bonushub.crdb.databinding.FragmentUpiSmsDynamicPayQrInputDetailBinding
import com.bonushub.crdb.utils.ToastUtils
import com.bonushub.crdb.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.utils.logger
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.pax.utils.DigiPosItem


class UpiSmsDynamicPayQrInputDetailFragment : Fragment() {

    var binding:FragmentUpiSmsDynamicPayQrInputDetailBinding? = null
    lateinit var digiPosItemType:DigiPosItem
    var amount = ""
    var vpa = ""
    var mobile = ""

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

        binding?.amountEt?.text.toString().trim()
        binding?.btnProceed?.setOnClickListener {

            DialogUtilsNew1.hideKeyboardIfOpen(requireActivity())

            when(digiPosItemType)
            {
                DigiPosItem.UPI ->{
                    logger("digiPosItemType",""+digiPosItemType.title)
                    if(isVerify()){


                        (activity as NavigationActivity).transactFragment(UpiCollectFragment().apply {
                            arguments = Bundle().apply {
                                putSerializable("type", DigiPosItem.UPI)
                                putSerializable("amount", amount)
                                putSerializable("vpa", vpa)
                                putSerializable("mobile", mobile)
                            }
                        })
                    }

                }

                DigiPosItem.DYNAMIC_QR->{
                    logger("digiPosItemType",""+digiPosItemType.title)
//                    (activity as NavigationActivity).transactFragment(UpiCollectFragment().apply {
//                        arguments = Bundle().apply {
//                            putString("amount", binding?.amountEt?.text.toString().trim())
//                            putString("vpa", binding?.vpaEt?.text.toString().trim())
//                            putString("des", binding?.enterDescriptionEt?.text.toString().trim())
//                        }
//                    })
                    (activity as NavigationActivity).transactFragment(QrFragment().apply {
                        arguments = Bundle().apply {
                            putSerializable("type", DigiPosItem.DYNAMIC_QR)
                        }
                    })
                }

                DigiPosItem.SMS_PAY ->{
                   logger("digiPosItemType",""+digiPosItemType.title)
//                    (activity as NavigationActivity).transactFragment(UpiCollectFragment().apply {
//                        arguments = Bundle().apply {
//                            putString("amount", binding?.amountEt?.text.toString().trim())
//                            putString("vpa", binding?.vpaEt?.text.toString().trim())
//                            putString("des", binding?.enterDescriptionEt?.text.toString().trim())
//                        }
//                    })
                }

                else ->{
                    logger("digiPosItemType",""+digiPosItemType.title)
                }
            }

        }
    }


    fun isVerify():Boolean{
        amount =  binding?.amountEt?.text.toString().trim()
        vpa =  binding?.vpaEt?.text.toString().trim()
        mobile =  binding?.mobileNumberEt?.text.toString().trim()

        if(amount.isEmpty())
        {
            ToastUtils.showToast(requireContext(),"Please enter amount.")
            return false
        }
        else if(vpa.isEmpty())
        {
            ToastUtils.showToast(requireContext(),"Please enter vpa.")
            return false
        }
        else if(mobile.isEmpty() || mobile.length <10)
        {
            ToastUtils.showToast(requireContext(),"Please enter mobile number.")
            return false
        }

        return true
    }
}