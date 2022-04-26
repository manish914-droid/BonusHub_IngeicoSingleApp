package com.bonushub.crdb.india.view.fragments

import  android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bonushub.crdb.india.HDFCApplication

import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentNewInputAmountBinding

import com.bonushub.crdb.india.db.AppDatabase
import com.bonushub.crdb.india.model.local.AppPreference
import com.bonushub.crdb.india.model.local.BrandEMISubCategoryTable
import com.bonushub.crdb.india.model.local.HDFCTpt
import com.bonushub.crdb.india.model.local.TerminalParameterTable
import com.bonushub.crdb.india.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.india.model.remote.BrandEMIProductDataModal
import com.bonushub.crdb.india.model.remote.BrandEmiBillSerialMobileValidationModel
import com.bonushub.crdb.india.repository.ServerRepository
import com.bonushub.crdb.india.serverApi.RemoteService
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.convertValue2BCD
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.isTipEnable
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.maxAmountLimitDialog

import com.bonushub.crdb.india.view.activity.IFragmentRequest
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.base.IDialog
import com.bonushub.crdb.india.viewmodel.NewInputAmountViewModel
import com.bonushub.crdb.india.utils.EDashboardItem

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.bonushub.crdb.india.utils.BhTransactionType
import com.bonushub.crdb.india.utils.UiAction

import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_new_input_amount.*
import kotlinx.coroutines.*
@AndroidEntryPoint
class NewInputAmountFragment : Fragment() {

    /** need to use Hilt for instance initializing here..*/
    private val remoteService: RemoteService = RemoteService()
    private val dbObj: AppDatabase = AppDatabase.getInstance(HDFCApplication.appContext)
    private val serverRepository: ServerRepository = ServerRepository(dbObj, remoteService)
    private lateinit var eDashBoardItem: EDashboardItem
    private var testEmiTxnType: String? = null
    var tpt: TerminalParameterTable? = null
    public var hdfctpt: HDFCTpt? = null
    private var iDialog: IDialog? = null
    var status: Boolean? = null
    var brandEntryValidationModel: BrandEmiBillSerialMobileValidationModel? = null

    private var brandEmiSubCatData: BrandEMISubCategoryTable? = null
    private var brandEmiCatData: BrandEMISubCategoryTable? = null
    private var brandEmiProductData: BrandEMIProductDataModal? = null
    private var brandDataMaster: BrandEMIMasterDataModal? = null

    private var isBillNumRequiredForBankEmi = false
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
    private var isMobilNumUiNeed = false
    lateinit var newInputAmountViewModel: NewInputAmountViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IFragmentRequest) {
            iFrReq = context
        }
        if (context is IDialog) iDialog = context
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
        eDashBoardItem = (arguments?.getSerializable("type")) as EDashboardItem
        newInputAmountViewModel = ViewModelProvider(this).get(NewInputAmountViewModel::class.java)
        brandEmiSubCatData =
            arguments?.getSerializable("brandEmiSubCat") as? BrandEMISubCategoryTable
        brandEmiCatData =
            arguments?.getSerializable("brandEmiCatData") as? BrandEMISubCategoryTable
        brandEmiProductData =
            arguments?.getSerializable("brandEmiProductData") as? BrandEMIProductDataModal
        brandDataMaster = arguments?.getSerializable("brandDataMaster") as? BrandEMIMasterDataModal
        testEmiTxnType = (arguments?.getSerializable("TestEmiOption") ?: "") as? String

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }
        binding?.subHeaderView?.subHeaderText?.text=eDashBoardItem.title
        binding?.subHeaderView?.headerImage?.setImageResource(eDashBoardItem.res)
        initAnimation()
        setTptValue()
        when (eDashBoardItem) {
            EDashboardItem.SALE_WITH_CASH -> {
                //  binding?.enterCashAmountTv?.visibility = View.VISIBLE
                binding?.cashAmtCrdView?.visibility = View.VISIBLE
                cashAmount?.hint = HDFCApplication.appContext.getString(R.string.cash_amount)
                //   binding?.enterCashAmountTv?.text = VerifoneApp.appContext.getString(R.string.cash_amount)

            }
            EDashboardItem.SALE -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    // for testing
                   /* val result = DBModule.appDatabase.appDao.getIngenicoInitialization()
                    logger("result", result?.get(0).toString(), "e")
                    logger("result2", result?.get(0)?.tidList.toString(),"e")
                    logger("result3", result?.get(0)?.tidStatusList.toString(),"e")
                    logger("result4", result?.get(0)?.initdataList.toString(),"e")*/

                    if(isTipEnable())
                    {
                        logger("isTipEnable()","true")
                        withContext(Dispatchers.Main){
                            binding?.cashAmtCrdView?.visibility = View.VISIBLE
                            cashAmount?.hint = getString(R.string.enter_tip_amount)
                        }
                    }else{
                        logger("isTipEnable()","false")
                        withContext(Dispatchers.Main){
                            binding?.cashAmtCrdView?.visibility = View.GONE
                        }
                    }
                   // logger("isTipEnable",""+isTipEnable(DBModule.appDatabase.appDao))
                }
               /* if (checkHDFCTPTFieldsBitOnOff(TransactionType.TIP_SALE)) {
                    //   binding?.enterCashAmountTv?.visibility = View.VISIBLE
                    binding?.cashAmtCrdView?.visibility = View.VISIBLE
                    cashAmount?.hint =
                        HDFCApplication.appContext.getString(R.string.enter_tip_amount)
                    //    binding?.enterCashAmountTv?.text = VerifoneApp.appContext.getString(R.string.enter_tip_amount)

                } else {
                    cashAmount?.visibility = View.GONE
                    binding?.cashAmtCrdView?.visibility = View.GONE
                    //  binding?.enterCashAmountTv?.visibility = View.GONE

                }*/
                isMobileNumberEntryOnsale { isMobileNeeded, isMobilenumberMandatory ->
                    if (isMobileNeeded) {
                        binding?.mobNoCrdView?.visibility = View.VISIBLE
                    } else {
                        binding?.mobNoCrdView?.visibility = View.GONE
                    }
                }

                observeNewInpuAmountViewModelForHdfcTpt(BhTransactionType.TIP_SALE)

            }
            EDashboardItem.BRAND_EMI->{
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

            }
            EDashboardItem.BANK_EMI , EDashboardItem.TEST_EMI->{
                isMobileNumberEntryAndBillEntryRequiredOnBankEmi { isMobileNeeded, isBillNoNeeded ->
                    if (isMobileNeeded) {
                        binding?.mobNoCrdView?.visibility = View.VISIBLE
                    } else {
                        binding?.mobNoCrdView?.visibility = View.GONE
                    }
                    isBillNumRequiredForBankEmi = isBillNoNeeded
                }

            }

            else -> {
                cashAmount?.visibility = View.GONE
                binding?.cashAmtCrdView?.visibility = View.GONE
                //   binding?.enterCashAmountTv?.visibility = View.GONE
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
        setOnTextChangeListeners()

    }

    private fun setTptValue():TerminalParameterTable? {
        lifecycleScope.launch(Dispatchers.Main) {
            newInputAmountViewModel.fetchtptData()?.observe(viewLifecycleOwner,{
                tpt = it
                Log.d("tptllll===>:- ", Gson().toJson(it))
            })

        }
        Log.d("tpt===>:- ", Gson().toJson(tpt))
        return tpt
    }
    private fun observeNewInpuAmountViewModelForHdfcTpt(bhTransactionType: BhTransactionType)  {
      lifecycleScope.launch(Dispatchers.Main) {
            newInputAmountViewModel.fetchHdfcTptData()?.observe(viewLifecycleOwner,{
                hdfctpt = it
            checkHDFCTPTFieldsBitOnOff(bhTransactionType,it)
                Log.d("Hdfctpt===>:- ", Gson().toJson(it))
                Log.d("Hdfctpt===>:- ", Gson().toJson(status))
            })

        }
        Log.d("Hdfctpt===>:- ", Gson().toJson(status))

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

    private fun setOnTextChangeListeners()
    {
        binding?.saleAmount?.addTextChangedListener {
            changeEditTextBackground(binding?.saleAmount,binding?.saleAmtCrdView)
        }

        binding?.cashAmount?.addTextChangedListener {
            changeEditTextBackground(binding?.cashAmount,binding?.cashAmtCrdView)
        }

        binding?.mobNumbr?.addTextChangedListener {
            changeEditTextBackground(binding?.mobNumbr,binding?.mobNoCrdView)
        }
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

    private fun onOKClicked(amt: String) {

        Log.e("SALE", "OK CLICKED  ${binding?.saleAmount?.text.toString()}")
        Log.e("CASh", "OK CLICKED  ${cashAmount?.text}")
        Log.e("AMT", "OK CLICKED  $amt")
        val maxTxnLimit = 1000000.0///"%.2f".format(getTransactionLimitForHDFCIssuer()).toDouble()
        Log.e("TXN LIMIT", "Txn type = $eDashBoardItem  Txn maxLimit = $maxTxnLimit")

        try {
            (binding?.saleAmount?.text.toString()).toDouble()
        } catch (ex: Exception) {
            ex.printStackTrace()
            showToast( "Please enter amount")
            return
        }

        val cashAmtStr = (cashAmount?.text.toString())
        var cashAmt = 0.toDouble()
        if (cashAmtStr != "") {
            cashAmt = (cashAmount?.text.toString()).toDouble()
        } else if (eDashBoardItem == EDashboardItem.SALE_WITH_CASH) {
           showToast(getString(R.string.please_enter_cash_amount))
            return
        }
        val saleAmountStr = binding?.saleAmount?.text.toString()
        var saleAmount = 0.toDouble()
        if (saleAmountStr != "") {
            saleAmount = (binding?.saleAmount?.text.toString()).toDouble()
        }
        if (saleAmount < 1) {
           showToast(getString(R.string.sale_amount_should_greater_then_1))
            return
        }
        when (eDashBoardItem) {
          EDashboardItem.SALE -> {
                val saleAmt = saleAmount.toString().trim().toDouble()
                val saleTipAmt = cashAmt.toString().trim().toDouble()
                val trnsAmt = saleAmt + saleTipAmt
                if (trnsAmt > maxTxnLimit) {
                    maxAmountLimitDialog(iDialog, maxTxnLimit)
                    return
                }
                if (saleTipAmt > 0) {
                    when {
                        !TextUtils.isEmpty(binding?.mobNumbr?.text.toString()) -> if (binding?.mobNumbr?.text.toString().length in 10..13) {
                            val extraPairData =
                                Triple(binding?.mobNumbr?.text.toString(), "", third = true)
                          validateTIP(trnsAmt, saleAmt, extraPairData)
                           // temproryCheck(trnsAmt,saleAmount)
                        } else
                            context?.getString(R.string.enter_valid_mobile_number)
                                ?.let { showToast( it) }

                        TextUtils.isEmpty(binding?.mobNumbr?.text.toString()) -> {
                            val extraPairData = Triple("", "", third = true)
                      validateTIP(trnsAmt, saleAmt, extraPairData)
                            //temproryCheck(trnsAmt,saleAmount)
                        }
                    }


                } else {
                    isMobileNumberEntryOnsale { isMobileNeeded, _ ->
                        if (isMobileNeeded) {
                            when {
                                !TextUtils.isEmpty(binding?.mobNumbr?.text.toString()) -> if (binding?.mobNumbr?.text.toString().length in 10..13) {
                                    val extraPairData = Triple(
                                        binding?.mobNumbr?.text.toString(),
                                        "",
                                        third = true
                                    )
                                    iFrReq?.onFragmentRequest(
                                        EDashboardItem.SALE,
                                        Pair(
                                            trnsAmt.toString().trim(),
                                            cashAmt.toString().trim()
                                        ),
                                        extraPairData
                                    )
                                } else
                                    context?.getString(R.string.enter_valid_mobile_number)
                                        ?.let { showToast( it) }

                                TextUtils.isEmpty(binding?.mobNumbr?.text.toString()) -> {
                                    iFrReq?.onFragmentRequest(
                                        EDashboardItem.SALE,
                                        Pair(
                                            trnsAmt.toString().trim(),
                                            cashAmt.toString().trim()
                                        )
                                    )

                                }
                            }
                        } else {
                            iFrReq?.onFragmentRequest(
                                EDashboardItem.SALE,
                                Pair(trnsAmt.toString().trim(), cashAmt.toString().trim())
                            )
                        }
                    }
                }
            }
            EDashboardItem.BANK_EMI, EDashboardItem.TEST_EMI -> {

                var uiAction = UiAction.BANK_EMI
                if (eDashBoardItem == EDashboardItem.TEST_EMI) {
                    uiAction = UiAction.TEST_EMI
                }

                if((saleAmount.toString().trim()).toDouble() > maxTxnLimit){
                    maxAmountLimitDialog(iDialog,maxTxnLimit)
                    return
                }
                isMobileNumberEntryAndBillEntryRequiredOnBankEmi { isMobileNeeded, isBillNumNeeded ->
                    when {
                        isMobileNeeded -> {
                            when {
                                !TextUtils.isEmpty(binding?.mobNumbr?.text.toString()) -> if (binding?.mobNumbr?.text.toString().length in 10..13) {
                                    navigateToBankEmiNextProcess(saleAmount.toString().trim(),binding?.mobNumbr?.text.toString().trim())
                                } else
                                    context?.getString(R.string.enter_valid_mobile_number)
                                        ?.let { showToast(it) }

                                TextUtils.isEmpty(binding?.mobNumbr?.text.toString()) -> {
                                    navigateToBankEmiNextProcess(saleAmount.toString().trim(),binding?.mobNumbr?.text.toString().trim())

                                }
                            }
                        }
                        isBillNumNeeded -> {
                            navigateToBankEmiNextProcess(saleAmount.toString().trim(),binding?.mobNumbr?.text.toString().trim())

                        }
                        else -> {
                            (activity as NavigationActivity).startTransactionActivityForEmi(eDashBoardItem,amt=saleAmount.toString(), testEmiTxnType = testEmiTxnType?:"")
                        }
                    }
                }
            }
            EDashboardItem.BRAND_EMI -> {
                when {
                    // mobile entry  optional handling
                    brandEntryValidationModel?.isMobileNumReq == true -> {
                        when {
                            !TextUtils.isEmpty(binding?.mobNumbr?.text.toString()) -> if (binding?.mobNumbr?.text.toString().length == 10) {
                                navigateToBrandEmiNextProcess(saleAmountStr,binding?.mobNumbr?.text.toString())
                            } else
                                context?.getString(R.string.enter_valid_mobile_number)
                                    ?.let {showToast(it) }

                            TextUtils.isEmpty(binding?.mobNumbr?.text.toString()) -> {
                                navigateToBrandEmiNextProcess(saleAmountStr,binding?.mobNumbr?.text.toString())
                            }
                        }
                    }
                    // mobile entry mandatory handling
                    brandEntryValidationModel?.isMobileNumMandatory == true -> {
                        if (!TextUtils.isEmpty(binding?.mobNumbr?.text.toString()) && (binding?.mobNumbr?.text.toString().length in 10..13)) {
                            navigateToBrandEmiNextProcess(saleAmountStr,binding?.mobNumbr?.text.toString())

                        } else {
                            context?.getString(R.string.enter_valid_mobile_number)
                                ?.let { showToast(it) }
                        }
                    }
                    else -> {
                        // no mobile number require
                        navigateToBrandEmiNextProcess(saleAmountStr,binding?.mobNumbr?.text.toString())
                    }
                }
            }
            EDashboardItem.CASH_ADVANCE -> {
                iFrReq?.onFragmentRequest(
                    EDashboardItem.CASH_ADVANCE,
                    Pair(
                        saleAmount,
                        cashAmt.toString().trim()
                    )
                )
            }
            EDashboardItem.SALE_WITH_CASH -> {
                if((saleAmount.toString().trim()).toDouble() > maxTxnLimit){
                    maxAmountLimitDialog(iDialog,maxTxnLimit)
                    return
                }
                iFrReq?.onFragmentRequest(
                    EDashboardItem.SALE_WITH_CASH,
                    Pair(
                        saleAmount.toString().trim(),
                        cashAmt.toString().trim()
                    )
                )
            }
            EDashboardItem.REFUND -> {
                if((saleAmount.toString().trim()).toDouble() > maxTxnLimit){
                    maxAmountLimitDialog(iDialog,maxTxnLimit)
                    return
                }
                iFrReq?.onFragmentRequest(
                    EDashboardItem.REFUND,
                    Pair(saleAmount.toString().trim(), "0")
                )
            }
            EDashboardItem.PREAUTH -> {
                if((saleAmount.toString().trim()).toDouble() > maxTxnLimit){
                    maxAmountLimitDialog(iDialog,maxTxnLimit)
                    return
                }
                iFrReq?.onFragmentRequest(
                    EDashboardItem.PREAUTH,
                    Pair(saleAmount.toString().trim(), "0")
                )
            }
            EDashboardItem.PREAUTH_COMPLETE->{
    if((saleAmount.toString().trim()).toDouble() > maxTxnLimit){
        maxAmountLimitDialog(iDialog,maxTxnLimit)
        return
    }
    iFrReq?.onFragmentRequest(
        EDashboardItem.PREAUTH_COMPLETE,
        Pair(saleAmount.toString().trim(), "0")
    )

}
            EDashboardItem.EMI_ENQUIRY -> {
                if (tpt?.bankEnquiryMobNumberEntry == true) {
                    showMobileBillDialog(activity, BhTransactionType.EMI_ENQUIRY.type) {
                        //  sendStartSale(inputAmountEditText?.text.toString(), extraPairData)
                        iFrReq?.onFragmentRequest(
                            EDashboardItem.EMI_ENQUIRY,
                            Pair(saleAmount.toString().trim(),"0"), it
                        )
                    }
                } else {
                    iFrReq?.onFragmentRequest(
                        EDashboardItem.EMI_ENQUIRY,
                        Pair(saleAmount.toString().trim(), "0")
                    )
                }
            }
            EDashboardItem.BRAND_EMI_CATALOGUE -> {
                when {
                    TextUtils.isEmpty(
                        saleAmount.toString().trim()
                    ) -> showToast( "Enter Sale Amount")
                    //  TextUtils.isEmpty(binding?.mobNumbr?.text?.toString()?.trim()) -> VFService.showToast("Enter Mobile Number")
                    else -> iFrReq?.onFragmentRequest(
                        EDashboardItem.BRAND_EMI_CATALOGUE,
                        Pair(saleAmount.toString().trim(), brandEmiSubCatData?.brandID)
                    )
                }
            }
            EDashboardItem.BANK_EMI_CATALOGUE -> {

                when {
                    TextUtils.isEmpty(
                        saleAmount.toString().trim()
                    ) -> showToast( "Enter Sale Amount")
                    //  TextUtils.isEmpty(binding?.mobNumbr?.text?.toString()?.trim()) -> VFService.showToast("Enter Mobile Number")
                    else -> iFrReq?.onFragmentRequest(
                        EDashboardItem.BANK_EMI_CATALOGUE,
                        Pair(saleAmount.toString().trim(), cashAmt.toString().trim())
                    )
                }
            }

            else -> {
            }
        }


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
            (brandEmiProductData?.isRequired == "1" && brandEmiProductData?.validationTypeName?.isNotBlank() == true) || (brandEmiProductData?.isRequired == "0" && brandEmiProductData?.validationTypeName?.isNotBlank() == true)

        cb(brandEntryValidationModel)

    }

    // fun for checking mobile number and Bill number on Emi sale
    private fun isMobileNumberEntryAndBillEntryRequiredOnBankEmi(cb: (Boolean, Boolean) -> Unit) {
                if (tpt?.reservedValues?.substring(1, 2) == "1" && tpt?.reservedValues?.substring(2, 3) == "1")
                    cb(true, true)
                else if (tpt?.reservedValues?.substring(1, 2) == "1")
                    cb(true, false)
                else if (tpt?.reservedValues?.substring(2, 3) == "1")
                    cb(false, true)
                else
                    cb(false,false)
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

    private fun navigateToBrandEmiNextProcess(amt:String, mobileNum:String) {
        if (brandEntryValidationModel?.isBillNumReq == true || brandEntryValidationModel?.isBillNumMandatory == true || brandEntryValidationModel?.isIemeiOrSerialNumReq == true) {
       val bun=     Bundle().apply {
                putSerializable("eDashBoardItem", eDashBoardItem)
                putSerializable("brandValidation", brandEntryValidationModel)
                putSerializable("brandEmiSubCat", brandEmiSubCatData)
                putSerializable("brandEmiCatData", brandEmiCatData)
                putSerializable("brandEmiProductData", brandEmiProductData)
                putSerializable("brandDataMaster", brandDataMaster)
                putSerializable("amt", amt)
                putSerializable("mobileNum", binding?.mobNumbr?.text.toString())

                //   putSerializable("transType", transactionType)

            }

            (activity as NavigationActivity).transactFragment(BillNumSerialNumEntryFragment().apply {
                arguments =bun
            }, true)
        } else {
                  brandEmiSubCatData?.let {
                        brandEmiProductData?.let { it1 ->
                            brandDataMaster?.let { it2 ->
                                (activity as NavigationActivity).startTransactionActivityForEmi(eDashBoardItem,amt=saleAmount.toString(),mobileNum = mobileNum,brandDataMaster = it2,
                                    brandEmiSubCatData = it,brandEmiCat=brandEmiCatData,brandEmiProductData = it1, testEmiTxnType = testEmiTxnType?:""
                                )
                            }
                        }
                    }


        }

    }

    private fun navigateToBankEmiNextProcess(amt:String, mobileNum:String){
        if (isBillNumRequiredForBankEmi) {
            val bun  = Bundle().apply {
                putSerializable("eDashBoardItem", eDashBoardItem)
                putSerializable("mobileNum", binding?.mobNumbr?.text.toString())
                putString("amt", amt)

                putString("testEmiType", testEmiTxnType ?: "") // test emi
                putBoolean("isBillRequire", isBillNumRequiredForBankEmi)
                putBoolean("isSerialNumRequired", false)

               /* putSerializable("brandValidation", brandEntryValidationModel)
                putSerializable("brandEmiSubCat", brandEmiSubCatData)
                putSerializable("brandEmiProductData", brandEmiProductData)
                putSerializable("brandDataMaster", brandDataMaster)*/
            }
            (activity as NavigationActivity).transactFragment(BillNumSerialNumEntryFragment().apply {
                arguments =bun
            })


        } else {
            (activity as NavigationActivity).startTransactionActivityForEmi(eDashBoardItem,amt=saleAmount.toString(),mobileNum = mobileNum, testEmiTxnType = testEmiTxnType?:""
            )

        }

    }

    private fun isMobileNumberEntryOnsale(cb: (Boolean, Boolean) -> Unit) {
        lifecycleScope.launch(Dispatchers.Main) {
            newInputAmountViewModel.fetchtptData()?.observe(viewLifecycleOwner, {
                tpt = it
                Log.d("tptllll===>:- ", Gson().toJson(it))
                when (eDashBoardItem) {
                    EDashboardItem.SALE -> {

                        Log.d("reservedValues===>:- ", Gson().toJson(tpt?.reservedValues))
                        if (tpt?.reservedValues?.substring(0, 1) == "1")
                            cb(true, false)
                        else
                            cb(false, false)
                    }
                    else -> {
                        cb(false, false)
                    }
                }
            })
        }

    }

    //region=========================Below method to check HDFC TPT Fields Check:-
    private fun checkHDFCTPTFieldsBitOnOff(bhTransactionType: BhTransactionType, hdfcTpt:HDFCTpt): Boolean {
        Log.d("HDFC TPT:- ", hdfcTpt.toString())
        var data: String? = null
        when (bhTransactionType) {
            BhTransactionType.VOID -> {
                data = convertValue2BCD(hdfcTpt.localTerminalOption)
                return data[1] == '1' // checking second position of data for on/off case
            }
            BhTransactionType.REFUND -> {
                data = convertValue2BCD(hdfcTpt.localTerminalOption)
                return data[2] == '1' // checking third position of data for on/off case
            }
            BhTransactionType.TIP_ADJUSTMENT -> {
                data = convertValue2BCD(hdfcTpt.localTerminalOption)
                return data[3] == '1' // checking fourth position of data for on/off case
            }
            BhTransactionType.TIP_SALE -> {
                data = convertValue2BCD(hdfcTpt.option1)
               if(data[2] == '1'){ // checking third position of data for on/off case
                   binding?.cashAmtCrdView?.visibility = View.VISIBLE
                   cashAmount?.hint =
                       HDFCApplication.appContext.getString(R.string.enter_tip_amount)
               }else{
                   cashAmount?.visibility = View.GONE
                   binding?.cashAmtCrdView?.visibility = View.GONE
                   //  binding?.enterCashAmountTv?.visibility = View.GONE
               }
            }
            else -> {
            }
        }

        return false
    }

    //endregion
//region=========================Below method to check HDFC Transaction Limit Check:-
    private fun getTransactionLimitForHDFCIssuer(): Double {
        return try {
            runBlocking(Dispatchers.IO) {
                val maxAmt =
                    dbObj.appDao.getIssuerTableDataByIssuerID(AppPreference.WALLET_ISSUER_ID)?.transactionAmountLimit?.toDouble()
                        ?.div(100)
                ("%.2f".format((maxAmt).toString().toDouble())).toDouble()
            }

        } catch (ex: Exception) {
            0.00
        }

    }
    //endregion

    private fun validateTIP(
        totalTransAmount: Double,
        saleAmt: Double,
        extraPair: Triple<String, String, Boolean>
    ) {
        lifecycleScope.launch(Dispatchers.Main) {
            newInputAmountViewModel.fetchtptData()?.observe(viewLifecycleOwner) {
                tpt = it
                if (tpt != null) {
                    val tipAmount = try {
                        cashAmount?.text.toString().toFloat()
                    } catch (ex: Exception) {
                        0f
                    }
                    val maxTipPercent =
                        if (tpt?.maxTipPercent?.isEmpty() == true) 0f else (tpt?.maxTipPercent?.toFloat())?.div(
                            100
                        )
                    val maxTipLimit =
                        if (tpt?.maxTipLimit?.isEmpty() == true) 0f else (tpt?.maxTipLimit?.toFloat())?.div(
                            100
                        )
                    if (maxTipLimit != 0f) { // flat tip check is applied
                        if (tipAmount <= maxTipLimit!!) {
                            // iDialog?.showProgress()
                            lifecycleScope.launch(Dispatchers.IO) {

                                iFrReq?.onFragmentRequest(
                                    EDashboardItem.SALE,
                                    Pair(
                                        totalTransAmount.toString().trim(),
                                        cashAmount?.text.toString().trim()
                                    ), extraPair
                                )
                            }
                        } else {
                            val msg =
                                "Maximum tip allowed on this terminal is \u20B9 ${
                                    "%.2f".format(
                                        maxTipLimit
                                    )
                                }."
                            lifecycleScope.launch(Dispatchers.Main) {
                                iDialog?.getInfoDialog("Tip Sale Error", msg) {}
                            }
                        }
                    } else { // percent tip check is applied
                        val maxAmountTip = (maxTipPercent?.div(100))?.times(saleAmt)
                        val formatMaxTipAmount = "%.2f".format(maxAmountTip)
                        if (maxAmountTip != null) {
                            if (tipAmount <= maxAmountTip.toFloat()) {
                                //   iDialog?.showProgress()
                                lifecycleScope.launch(Dispatchers.IO) {

                                    iFrReq?.onFragmentRequest(
                                        EDashboardItem.SALE,
                                        Pair(
                                            totalTransAmount.toString().trim(),
                                            cashAmount?.text.toString().trim()
                                        ), extraPair
                                    )
                                }
                            } else {
                                //    val tipAmt = saleAmt * per / 100
                                val msg = "Tip limit for this transaction is \n \u20B9 ${
                                    "%.2f".format(
                                        formatMaxTipAmount.toDouble()
                                    )
                                }"
                                /* "Maximum ${"%.2f".format(
                                                 maxTipPercent.toDouble()
                                             )}% tip allowed on this terminal.\nTip limit for this transaction is \u20B9 ${"%.2f".format(
                                                 formatMaxTipAmount.toDouble()
                                             )}"*/
                                lifecycleScope.launch(Dispatchers.Main) {
                                    iDialog?.getInfoDialog("Tip Sale Error", msg) {}
                                }
                            }
                        }
                    }
                } else {
                    showToast("TPT not fount")
                }
            }

        }

    }

    private fun temproryCheck(totalTransAmount: Double,saleAmt:Double){
        lifecycleScope.launch(Dispatchers.IO) {
           val tipamt= cashAmount?.text.toString().trim().toFloat()
            if (isTipEnable() && tipamt>=saleAmt) {
                val msg =
                    "Maximum tip allowed on this terminal is \u20B9 ${
                        "%.2f".format(
                            saleAmt
                        )
                    }."
               withContext(Dispatchers.Main) {
                    iDialog?.getInfoDialog("Tip Sale Error", msg) {}
                }
            } else {
                val extraPairData = Triple(
                    binding?.mobNumbr?.text.toString(),
                    "",
                    third = true
                )
                iFrReq?.onFragmentRequest(
                    EDashboardItem.SALE,
                    Pair(
                        totalTransAmount.toString().trim(),
                        cashAmount?.text.toString().trim()
                    ), extraPairData
                )

            }
        }

    }

    private fun showToast(str:String){
        lifecycleScope.launch(Dispatchers.Main) {
            ToastUtils.showToast(activity, str)
        }
    }

}