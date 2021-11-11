package com.bonushub.crdb.view.fragments

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.lifecycle.lifecycleScope
import com.bonushub.crdb.HDFCApplication

import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentNewInputAmountBinding
import com.bonushub.crdb.db.AppDatabase
import com.bonushub.crdb.model.local.BrandEMISubCategoryTable
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.model.remote.BrandEMIProductDataModal
import com.bonushub.crdb.model.remote.BrandEmiBillSerialMobileValidationModel
import com.bonushub.crdb.repository.ServerRepository
import com.bonushub.crdb.serverApi.RemoteService
import com.bonushub.crdb.utils.KeyboardModel
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.pax.utils.EDashboardItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NewInputAmountFragment : Fragment() {

    /** need to use Hilt for instance initializing here..*/
    private val remoteService: RemoteService = RemoteService()
    private val dbObj: AppDatabase = AppDatabase.getInstance(HDFCApplication.appContext)
    private val serverRepository: ServerRepository = ServerRepository(dbObj, remoteService)


    var brandEntryValidationModel: BrandEmiBillSerialMobileValidationModel? = null

    private var brandEmiSubCatData: BrandEMISubCategoryTable? = null
    private var brandEmiProductData: BrandEMIProductDataModal? = null
    private var brandDataMaster: BrandEMIMasterDataModal? = null

    private var binding: FragmentNewInputAmountBinding? = null
    private val keyModelSaleAmount: KeyboardModel by lazy {
        KeyboardModel()
    }
    private val keyModelCashAmount: KeyboardModel by lazy {
        KeyboardModel()
    }
    private val keyModelMobNumber: KeyboardModel by lazy {
        KeyboardModel()
    }
    var inputInSaleAmount = false
    var inputInCashAmount = false
    var inputInMobilenumber = false
    private var iFrReq: IFragmentRequest? = null
    private var animShow: Animation? = null
    private var animHide: Animation? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IFragmentRequest) {
            iFrReq = context
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentNewInputAmountBinding.inflate(inflater, container, false)
        return binding?.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        brandEmiSubCatData = arguments?.getSerializable("brandEmiSubCat") as? BrandEMISubCategoryTable
        brandEmiProductData = arguments?.getSerializable("brandEmiProductData") as? BrandEMIProductDataModal
        brandDataMaster = arguments?.getSerializable("brandDataMaster") as? BrandEMIMasterDataModal

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }
        initAnimation()

        isMobileNumBillEntryAndSerialNumRequiredOnBrandEmi {
            if (it != null) {
                //  brandEmiValidationModel = it
                if (it.isMobileNumReq || it.isMobileNumMandatory) {
                    binding?.mobNoCrdView?.visibility = View.VISIBLE
                } else {
                    binding?.mobNoCrdView?.visibility = View.GONE
                }
            }
        }

        // keyModelMobNumber.isInutSimpleDigit = true
        binding?.mainKeyBoard?.root?.visibility = View.VISIBLE
        binding?.mainKeyBoard?.root?.startAnimation(animShow)
        keyModelSaleAmount.view = binding?.saleAmount
        keyModelSaleAmount.callback = ::onOKClicked

        inputInSaleAmount = true
        inputInCashAmount = false
        inputInMobilenumber = false
        setOnClickListeners()
    }

    private fun setOnClickListeners() {

        binding?.saleAmount?.setOnClickListener {
            keyModelSaleAmount.view = it
            keyModelSaleAmount.callback = ::onOKClicked
            inputInSaleAmount = true
            inputInCashAmount = false
            inputInMobilenumber = false
        }

        binding?.cashAmount?.setOnClickListener {
            keyModelCashAmount.view = it
            keyModelCashAmount.callback = ::onOKClicked
           // if(transactionType == EDashboardItem.FLEXI_PAY)
               // keyModelCashAmount.isInutSimpleDigit = true
            inputInSaleAmount = false
            inputInCashAmount = true
            inputInMobilenumber = false
        }

        binding?.mobNumbr?.setOnClickListener {
            keyModelMobNumber.view = it
            keyModelMobNumber.callback = ::onOKClicked
            keyModelMobNumber.isInutSimpleDigit = true
            inputInSaleAmount = false
            inputInCashAmount = false
            inputInMobilenumber = true

        }
        onSetKeyBoardButtonClick()
    }
    private fun onSetKeyBoardButtonClick() {
        binding?.mainKeyBoard?.key0?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("0")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("0")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("0")
                }
            }
        }
        binding?.mainKeyBoard?.key00?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("00")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("00")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("00")
                }
            }
        }
        binding?.mainKeyBoard?.key000?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("000")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("000")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("000")
                }
            }
        }
        binding?.mainKeyBoard?.key1?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("1")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("1")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("1")
                }
            }
        }
        binding?.mainKeyBoard?.key2?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    Log.e("SALE", "KEY 2")
                    keyModelSaleAmount.onKeyClicked("2")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("2")
                }
                else -> {
                    Log.e("CASH", "KEY 2")
                    keyModelCashAmount.onKeyClicked("2")
                }
            }
        }
        binding?.mainKeyBoard?.key3?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("3")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("3")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("3")
                }
            }
        }
        binding?.mainKeyBoard?.key4?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("4")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("4")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("4")
                }
            }
        }
        binding?.mainKeyBoard?.key5?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("5")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("5")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("5")
                }
            }
        }
        binding?.mainKeyBoard?.key6?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("6")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("6")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("6")
                }
            }
        }
        binding?.mainKeyBoard?.key7?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("7")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("7")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("7")
                }
            }
        }
        binding?.mainKeyBoard?.key8?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("8")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("8")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("8")
                }
            }
        }
        binding?.mainKeyBoard?.key9?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("9")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("9")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("9")
                }
            }
        }
        binding?.mainKeyBoard?.keyClr?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("c")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("c")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("c")
                }
            }
        }
        binding?.mainKeyBoard?.keyDelete?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("d")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("d")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("d")
                }
            }
        }
        binding?.mainKeyBoard?.keyOK?.setOnClickListener {
            when {
                inputInSaleAmount -> {
                    keyModelSaleAmount.onKeyClicked("o")
                }
                inputInMobilenumber -> {
                    keyModelMobNumber.onKeyClicked("o")
                }
                else -> {
                    keyModelCashAmount.onKeyClicked("o")
                }
            }
        }

    }
    private fun onOKClicked(saleAmountStr: String) {
        val saleAmountStr = binding?.saleAmount?.text.toString()
        var saleAmount = 0.toDouble()
        if (saleAmountStr != "") {
            saleAmount = (binding?.saleAmount?.text.toString()).toDouble()
        }
       /* (activity as NavigationActivity).transactFragment(BrandEmiProductFragment().apply {
            arguments = Bundle().apply {
              //  putSerializable("type", action)
                // putString("proc_code", ProcessingCode.PRE_AUTH.code)
               /// putString("mobileNumber", extraPair?.first)
                putString("enquiryAmt", saleAmount.toString().trim())
                // putSerializable("imagesData", emiCatalogueImageList as HashMap<*, *>)
                //  putSerializable("brandEMIDataModal", brandEMIDataModal)

            }
        })*/
lifecycleScope.launch(Dispatchers.IO) {
    serverRepository.getEMITenureData()
}
        /*iFrReq?.onFragmentRequest(
            UiAction.EMI_ENQUIRY,
            Pair(saleAmount.toString().trim(), "0")
        )*/
    }
    private fun initAnimation() {
        animShow = AnimationUtils.loadAnimation(activity, R.anim.view_show)
        animHide = AnimationUtils.loadAnimation(activity, R.anim.view_hide)
    }


    // fun for checking mobile number, Bill number and serial number on Brand Emi sale
    private fun isMobileNumBillEntryAndSerialNumRequiredOnBrandEmi(cb: (BrandEmiBillSerialMobileValidationModel?) -> Unit) {

                brandEntryValidationModel = BrandEmiBillSerialMobileValidationModel()
                when (brandDataMaster?.mobileNumberBillNumberFlag?.get(0)) {
                    '0' -> {
                        brandEntryValidationModel?.isMobileNumReq = false
                    }// not required
                    '1' -> {
                        brandEntryValidationModel?.isMobileNumReq = true
                    }
                    '2' -> {
                        brandEntryValidationModel?.isMobileNumMandatory = true
                    }
                }
                when (brandDataMaster?.mobileNumberBillNumberFlag?.get(2)) {
                    '0' -> {
                        brandEntryValidationModel?.isBillNumReq = false
                    }// not required
                    '1' -> {
                        brandEntryValidationModel?.isBillNumReq = true
                    }
                    '2' -> {
                        brandEntryValidationModel?.isBillNumMandatory = true
                    }
                }
                brandEntryValidationModel?.isSerialNumReq = isShowSerialDialog()
                brandEntryValidationModel?.isImeiNumReq = isShowIMEIDialog()
                brandEntryValidationModel?.isIemeiOrSerialNumReq =
                    ( brandEmiProductData?.isRequired == "1" && brandEmiProductData?.validationTypeName?.isNotBlank() == true)|| (brandEmiProductData?.isRequired == "0" && brandEmiProductData?.validationTypeName?.isNotBlank() == true)

                cb(brandEntryValidationModel)

    }

    //region=====================Condition to check Whether we need to show Serial input or not:-
    private fun isShowSerialDialog(): Boolean {
        return brandEmiProductData?.validationTypeName == "SerialNo" ||
                brandEmiProductData?.validationTypeName == "SerialNo"
    }

    //endregion
    //region=====================Condition to check Whether we need to show IMEI input or not:-
    private fun isShowIMEIDialog(): Boolean {
        return brandEmiProductData?.validationTypeName == "IMEI" ||
                brandEmiProductData?.validationTypeName == "imei"

    }
    //endregion


}