

package com.bonushub.crdb.view.fragments

import android.os.Bundle
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentBillNumSerialNumEntryBinding
import com.bonushub.crdb.model.local.BrandEMISubCategoryTable
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.model.remote.BrandEMIProductDataModal
import com.bonushub.crdb.model.remote.BrandEmiBillSerialMobileValidationModel
import com.bonushub.crdb.utils.ToastUtils
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.pax.utils.EDashboardItem

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class BillNumSerialNumEntryFragment : Fragment() {

    private var binding: FragmentBillNumSerialNumEntryBinding? = null

    val eDashBoardItem: EDashboardItem by lazy {
        arguments?.getSerializable("eDashBoardItem") as EDashboardItem
    }

    val mobileNumber: String by lazy {
        arguments?.getString("mobileNum") as String
    }
    val txnAmount: String by lazy {
        arguments?.getString("amt") as String
    }
    val testEmiType: String by lazy {
        arguments?.getString("testEmiType") as String
    }
   /* val isBillRequire: Boolean by lazy {
        arguments?.getBoolean("isBillRequire") as Boolean
    }*/
    val isSerialIEMIRequire: Boolean by lazy {
        arguments?.getBoolean("isSerialImeiNumRequired") as Boolean
    }

  /*  private val brandEMIDataModal1: BrandEMIDataModal by lazy {
        (arguments?.getSerializable("brandEMIDataModal") ?: BrandEMIDataModal()) as BrandEMIDataModal
    }

    private val brandEMIDataModal: BrandEMIDataModal by lazy {
        (arguments?.getSerializable("brandEMIDataModal") ?: BrandEMIDataModal() )as BrandEMIDataModal
    }*/
  val brandValidation: BrandEmiBillSerialMobileValidationModel by lazy {
      arguments?.getSerializable("brandValidation") as BrandEmiBillSerialMobileValidationModel
  }
  private var brandEmiSubCatData: BrandEMISubCategoryTable? = null
    private var brandEmiProductData: BrandEMIProductDataModal? = null
    private var brandDataMaster: BrandEMIMasterDataModal? = null

    private val transType: EDashboardItem by lazy {
        arguments?.getSerializable("transType") as EDashboardItem
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentBillNumSerialNumEntryBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.subHeaderView?.subHeaderText?.text = eDashBoardItem.title
        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_bank_emi)
        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }
        brandEmiSubCatData = arguments?.getSerializable("brandEmiSubCat") as? BrandEMISubCategoryTable
        brandEmiProductData = arguments?.getSerializable("brandEmiProductData") as? BrandEMIProductDataModal
        brandDataMaster = arguments?.getSerializable("brandDataMaster") as? BrandEMIMasterDataModal

        binding?.serialNumEt?.setMaxLength(brandEmiProductData?.maxLength?.toInt() ?: 20)
        binding?.billNumEt?.setMaxLength( 16)

        if (brandValidation.isBillNumReq || brandValidation.isBillNumMandatory || eDashBoardItem==EDashboardItem.BANK_EMI) {
            binding?.billnoCrdView?.visibility = View.VISIBLE
        } else {
            binding?.billnoCrdView?.visibility = View.GONE
        }
//0therwise optional
        if ((brandEmiProductData?.isRequired == "1" && brandEmiProductData?.validationTypeName?.isNotBlank() == true)|| (brandEmiProductData?.isRequired == "0" && brandEmiProductData?.validationTypeName?.isNotBlank() == true) ){
            if (brandValidation.isSerialNumReq) {
                binding?.serialNumEt?.hint = "Enter serial number"
            }
            else if (brandValidation.isImeiNumReq) {
       //     binding?.serialNumEt?.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(brandEMIDataModal.getMaxLength()?.toInt() ?: 16))

                binding?.serialNumEt?.hint = "Enter IMEI number"
            }
            binding?.serialNoCrdView?.visibility = View.VISIBLE
        } else {
            binding?.serialNoCrdView?.visibility = View.GONE
        }


        when (brandEmiProductData?.inputDataType) {
            "1" -> {
                binding?.serialNumEt?.inputType = InputType.TYPE_CLASS_NUMBER

            }
           else-> {
                binding?.serialNumEt?.inputType = InputType.TYPE_CLASS_TEXT

            }
        }
        binding?.proceedBtn?.setOnClickListener {
            if (eDashBoardItem == EDashboardItem.BANK_EMI || eDashBoardItem == EDashboardItem.TEST_EMI) {
                (activity as NavigationActivity).startTransactionActivityForEmi(eDashBoardItem,amt= txnAmount,mobileNum = mobileNumber,billNum =binding?.billNumEt?.text.toString(), testEmiTxnType = testEmiType?:"")
            } else if (eDashBoardItem == EDashboardItem.BRAND_EMI) {
                navigateToTransaction()

            }
        }


    }
// extension function to set edit text maximum length


    private fun navigateToTransaction() {

        if (brandValidation.isBillNumMandatory || brandValidation.isBillNumReq) {
            if (brandValidation.isBillNumMandatory) {
                if (TextUtils.isEmpty(binding?.billNumEt?.text.toString().trim())) {
                    context?.getString(R.string.enter_valid_bill_number)?.let { it1 ->
                        ToastUtils.showToast(activity,it1)
                    }

                    return
                }
            }
        }
        if (brandEmiProductData?.isRequired == "1" || brandEmiProductData?.isRequired == "0") {
            if (brandEmiProductData?.isRequired == "1") {
                if (TextUtils.isEmpty(binding?.serialNumEt?.text.toString().trim())) {
 context?.getString(R.string.enterValid_serial_iemei_no)?.let { it1 ->
     ToastUtils.showToast(activity,it1)
                    }

                        if(brandEmiProductData?.validationTypeName == "IMEI" ||
                            brandEmiProductData?.validationTypeName == "imei") {

                            ToastUtils.showToast(activity,
                                getString(
                                    R.string.enterValid_iemei_no
                                )
                            )
                        }else if (brandEmiProductData?.validationTypeName == "SerialNo"){
                            ToastUtils.showToast(activity,
                                getString(
                                    R.string.enterValid_serial
                                )
                            )

                        }
                    else{
                            ToastUtils.showToast(activity,
                                getString(
                                    R.string.enterValid_serial_iemei_no
                                )
                            )
                        }
                    return
                }
            }

        }
        lifecycleScope.launch(Dispatchers.IO) {
            brandEmiProductData?.imeiOrSerialNum=binding?.serialNumEt?.text.toString()
            brandEmiProductData?.billNum=binding?.billNumEt?.text.toString().trim()
            brandEmiSubCatData?.let {
                brandEmiProductData?.let { it1 ->
                    brandDataMaster?.let { it2 ->
                        (activity as NavigationActivity).startTransactionActivityForEmi(eDashBoardItem,amt= txnAmount,mobileNum = mobileNumber,billNum =binding?.billNumEt?.text.toString(),imeiOrSerialNum=binding?.serialNumEt?.text.toString() ,brandDataMaster = it2,
                            brandEmiSubCatData = it,brandEmiProductData = it1
                        )
                    }
                }
            }
        }

    }


}

fun EditText.setMaxLength(maxLength: Int){

    filters = arrayOf<InputFilter>(LengthFilter(maxLength))

}
