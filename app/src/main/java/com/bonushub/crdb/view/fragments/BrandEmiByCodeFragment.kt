package com.bonushub.crdb.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentBankFunctionsAdminVasBinding
import com.bonushub.crdb.databinding.FragmentBrandEmiByCodeBinding
import com.bonushub.crdb.utils.dialog.DialogUtilsNew1


class BrandEmiByCodeFragment : Fragment() {

    var binding:FragmentBrandEmiByCodeBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentBrandEmiByCodeBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.subHeaderView?.subHeaderText?.text = "BRAND EMI BYCODE"
        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_brand_emi_code)

        binding?.txtViewSubmit?.setOnClickListener {

            // temp
            DialogUtilsNew1.showBrandEmiByCodeDetailsDialog(requireContext(),"Citi",
                "FX-A7s/1545KIT-EElH","APS-C Low","6 months","2565.00","2565.00","2565.00"){

            }
        }

    }
}