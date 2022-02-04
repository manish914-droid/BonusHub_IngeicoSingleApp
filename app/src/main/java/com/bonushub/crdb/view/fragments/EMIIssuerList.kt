package com.bonushub.crdb.view.fragments

import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.HDFCApplication
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentEmiIssuerListBinding
import com.bonushub.crdb.databinding.ItemEmiIssuerListBinding
import com.bonushub.crdb.db.AppDatabase
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.model.remote.BrandEMIProductDataModal
import com.bonushub.crdb.repository.GenericResponse
import com.bonushub.crdb.repository.ServerRepository
import com.bonushub.crdb.serverApi.EMIRequestType
import com.bonushub.crdb.serverApi.RemoteService
import com.bonushub.crdb.utils.ToastUtils
import com.bonushub.crdb.utils.UiAction
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.adapter.IssuerTenureListAdapter
import com.bonushub.crdb.view.base.IDialog
import com.bonushub.crdb.viewmodel.EmiissuerListViewModel
import com.bonushub.crdb.viewmodel.viewModelFactory.IssuerListEmiViewModelFactory
import com.google.gson.Gson
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.util.*


class EMIIssuerList : Fragment() {

    /** need to use Hilt for instance initializing here..*/
    private val remoteService: RemoteService = RemoteService()
    private val dbObj : AppDatabase = AppDatabase.getInstance(HDFCApplication.appContext)
    private val serverRepository: ServerRepository = ServerRepository(dbObj, remoteService)
    private lateinit var emiissuerListViewModel: EmiissuerListViewModel

    private var binding: FragmentEmiIssuerListBinding? = null
   // private var allIssuers: ArrayList<IssuerParameterTable> = arrayListOf()
    private var allIssuerBankList: MutableList<IssuerBankModal> = mutableListOf()
    private var allIssuerTenureList: MutableList<TenureBankModal> = mutableListOf()
    private val enquiryAmtStr by lazy { arguments?.getString("enquiryAmt") ?: "0" }
    private val mobileNumber by lazy { arguments?.getString("mobileNumber") ?: "" }
    private val brandId by lazy { arguments?.getString("brandId") ?: "" }
    var emiCatalogueImageList: MutableMap<String, Uri>? = null
    private var mobileNumberOnOff: Boolean = false
    private val action by lazy { arguments?.getSerializable("type") ?: "" }
    private var enquiryAmount : Long? =null

    var states = arrayOf(
        intArrayOf(android.R.attr.state_enabled),
        intArrayOf(-android.R.attr.state_enabled),
        )

    var colors = intArrayOf(
        Color.BLACK,
        Color.RED,
        Color.GREEN,
        Color.BLUE
    )
    var mycolorList = ColorStateList(states, colors)
  private val brandEMIData by lazy { arguments?.getSerializable("brandEMIDataModal") as BrandEMIProductDataModal
  ? }

    private val issuerListAdapter by lazy {
        IssuerListAdapter(
            temporaryAllIssuerList,
            emiCatalogueImageList
        )
    }
    private val issuerTenureListAdapter by lazy {
        IssuerTenureListAdapter(
            temporaryAllTenureList,
            ::onTenureSelectedEvent
        )
    }
    private var temporaryAllIssuerList = mutableListOf<IssuerBankModal>()
    private var temporaryAllTenureList = mutableListOf<TenureBankModal>()
    private var refreshedBanksByTenure = mutableListOf<IssuerBankModal>()
    private var firstClick = true
    private var iDialog: IDialog? = null
    // private var brandEmiData: BrandEMIDataTable? = null
    private var moreDataFlag = "0"
    private var totalRecord: String? = "0"
    private var perPageRecord: String? = "0"
    private var compareActionName: String? = null
    private var field57RequestData = ""
    private var selectedTenure: String? = null
    // private var brandEMISelectedData: BrandEMIDataTable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentEmiIssuerListBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mobileNumberOnOff = !TextUtils.isEmpty(mobileNumber)
        enquiryAmount=((enquiryAmtStr.toFloat()) * 100).toLong()

       //emiCatalogueImageList = arguments?.getSerializable("imagesData") as MutableMap<String, Uri>
        setUpRecyclerViews()
        binding?.subHeaderView?.headerHome?.visibility= View.VISIBLE
        binding?.subHeaderView?.headerHome?.setOnClickListener {   (activity as NavigationActivity).transactFragment(DashboardFragment()) }
       // binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_brand_emi_catalogue)
        binding?.subHeaderView?.headerImage?.visibility= View.GONE // now header text is show in center


        binding?.subHeaderView?.backImageButton?.setOnClickListener { parentFragmentManager.popBackStackImmediate() }
        //Request All Scheme Data from Host:-
        Log.d("CompareActioneName:- ", compareActionName ?: "")
        emiissuerListViewModel = ViewModelProvider(this, IssuerListEmiViewModelFactory(serverRepository)).get(
            EmiissuerListViewModel::class.java
        )
        (activity as IDialog).showProgress()
        Log.d("enquiryAmtStr:- ", enquiryAmtStr)
        Log.d("enquiryAmount:- ", enquiryAmount.toString())
        Log.d("action:- ", action.toString())
        if (action == UiAction.BRAND_EMI_CATALOGUE) {
            binding?.subHeaderView?.subHeaderText?.text = getString(R.string.brandEmiCatalogue)
            binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_brandemi)
            //  brandEMISelectedData = runBlocking(Dispatchers.IO) { BrandEMIDataTable.getAllEMIData() }
            field57RequestData =
                "${EMIRequestType.EMI_CATALOGUE_ACCESS_CODE.requestType}^$totalRecord^${brandId}" +
                        "^${brandEMIData?.productID}^^^$enquiryAmount"
        } else {
            binding?.subHeaderView?.subHeaderText?.text = getString(R.string.bankEmiCatalogue)
            //  binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_bank_emi)
            field57RequestData =
                if (AppPreference.getLongData(AppPreference.ENQUIRY_AMOUNT_FOR_EMI_CATALOGUE) != 0L)
                    "${EMIRequestType.EMI_CATALOGUE_ACCESS_CODE.requestType}^$totalRecord^1^^^^${
                        enquiryAmount
                    }"
                else
                    "${EMIRequestType.EMI_CATALOGUE_ACCESS_CODE.requestType}^$totalRecord^1^^^^$enquiryAmount"
        }
        Log.d("enquiryAmount:- ", field57RequestData.toString())
        lifecycleScope.launch(Dispatchers.IO) {
            emiissuerListViewModel.getIssuerListData(field57RequestData)
        }
        emiissuerListViewModel.emiIssuerListLivedata.observe(viewLifecycleOwner) {
            (activity as IDialog).hideProgress()
            when (val genericResp = it) {
                is GenericResponse.Success -> {
                    println(Gson().toJson(genericResp.data))
                    setUpRecyclerViews()
                    Log.d("genericResp.data:- ", genericResp.data.toString())
                    allIssuerBankList = genericResp.data as MutableList<IssuerBankModal>
                    Log.d("allIssuerBankList:- ", allIssuerBankList.toString())
                    //  brandEMIMasterCategoryAdapter.submitList(genericResp.data)
                }
                is GenericResponse.Error -> {
                    lifecycleScope.launch(Dispatchers.Main) {
                        iDialog?.hideProgress()
                        iDialog?.alertBoxWithAction(getString(R.string.info), "No record found",
                            false, getString(R.string.positive_button_ok),
                            {
                                parentFragmentManager.popBackStackImmediate()
                            }, {})
                    }
                }
                is GenericResponse.Loading -> {

                }
            }
        }
        emiissuerListViewModel.emiIssuerTenureListLiveData.observe(viewLifecycleOwner, {
            when (val genericResp = it) {
                is GenericResponse.Success -> {
                    println(Gson().toJson(genericResp.data))

                    Log.d("genericResp.data:- ", genericResp.data.toString())
                    allIssuerTenureList= genericResp.data as MutableList<TenureBankModal>
                    Log.d("allIssuerTenureList:- ",allIssuerTenureList.toString())
                    if (allIssuerBankList.isNotEmpty() && allIssuerTenureList.isNotEmpty()) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            AppPreference.setLongData(
                                AppPreference.ENQUIRY_AMOUNT_FOR_EMI_CATALOGUE,

                                enquiryAmount!!
                            )
                            temporaryAllIssuerList =
                                allIssuerBankList.distinctBy { it.issuerID }.toMutableList()
                            temporaryAllTenureList =
                                allIssuerTenureList.distinctBy { it.bankTenure }.toMutableList()
                            withContext(Dispatchers.Main) {
                                compareActionName = CompareActionType.COMPARE_BY_BANK.compareType
                                issuerListAdapter.refreshBankList(temporaryAllIssuerList)
                                issuerTenureListAdapter.refreshTenureList(temporaryAllTenureList)
                                Log.d("DistinctIssuer:-", temporaryAllIssuerList.toString())
                                Log.d("DistinctTenure:-", temporaryAllIssuerList.toString())

                                //region===============Below Code will Execute EveryTime Merchant Came to This Screen:-
                                compareByBankSelectEventMethod()
                                setUpRecyclerViews()
                                //endregion
                            }
                        }
                    }
                }
                is GenericResponse.Error -> {

                }
                is GenericResponse.Loading -> {

                }
            }
        })
        binding?.headingText?.text = getString(R.string.calculate_and_compare_emi_offers)

        //region=================Select All CheckBox OnClick Event:-
        binding?.selectAllBankCheckButton?.setOnClickListener {
            if (binding?.selectAllBankCheckButton?.isChecked == true) {
                binding?.selectAllBankCheckButton?.text = getString(R.string.unselect_all_banks)
               
                issuerListAdapter.selectAllIssuerBank(
                    true,
                    CompareActionType.COMPARE_BY_TENURE.compareType
                )
              
            } else {
                binding?.selectAllBankCheckButton?.text = getString(R.string.select_all_banks)
               
                issuerListAdapter.selectAllIssuerBank(
                    false,
                    CompareActionType.COMPARE_BY_TENURE.compareType
                )
              
            }
        }
        //endregion

        //region==============OnClick event of Compare By Tenure CardView:-
        binding?.compareByTenure?.setOnClickListener {

            compareByTenureSelectEventMethod()
        }
        //endregion

        // region==============OnClick event of Compare By Bank CardView:-
        binding?.compareByBank?.setOnClickListener {
            compareByBankSelectEventMethod()
        }
        //endregion

        //region===============Proceed EMI Catalogue button onClick Event:-
      binding?.proceedEMICatalogue?.setOnClickListener { proceedToCompareFragmentScreen() }
        //endregion



}

    //region=================SetUp Tenure and Banks Recyclerview:-
    private fun setUpRecyclerViews() {
        //region==========Binding Tenure RecyclerView:-
        binding?.tenureRV?.apply {
            layoutManager = GridLayoutManager(activity, 3)
            itemAnimator = DefaultItemAnimator()
            adapter = issuerTenureListAdapter
        }
        //endregion

        //region==========Binding Issuer Bank RecyclerView SetUp:-
        binding?.issuerRV?.apply {
            layoutManager = GridLayoutManager(activity, 3)
            itemAnimator = DefaultItemAnimator()
            adapter = issuerListAdapter
        }
        //endregion
    }
    //endregion



    //region============================Proceed To Compare Fragment Screen:-
    private fun proceedToCompareFragmentScreen() {
        //region===========Condition to Check When Compare By Bank is Selected and Single Bank is Selected:-
        when (compareActionName) {
            CompareActionType.COMPARE_BY_BANK.compareType -> {
                val selectedIssuerNameData =
                    temporaryAllIssuerList.filter { it.isIssuerSelected == true }
                if (selectedIssuerNameData.isNotEmpty()) {
                    val selectedIssuerNameFullData =
                        allIssuerBankList.filter { it.issuerSchemeID == selectedIssuerNameData[0].issuerSchemeID }
                    if (selectedIssuerNameFullData.isNotEmpty()) {
                        (activity as NavigationActivity).transactFragment(EMICompareFragment().apply {
                            arguments = Bundle().apply {
                                putString("compareActionName", compareActionName)
                                putSerializable("type", action)
                                putParcelableArrayList(
                                    "dataModal",
                                    selectedIssuerNameFullData as java.util.ArrayList<out Parcelable>
                                )
                            }
                        })
                    } else {
                        ToastUtils.showToast(HDFCApplication.appContext,getString(R.string.please_select_one_issuer_bank))
                    }
                } else {
                    ToastUtils.showToast(HDFCApplication.appContext,getString(R.string.please_select_one_issuer_bank))
                }
            }
            CompareActionType.COMPARE_BY_TENURE.compareType -> {
                val selectedIssuerNameData = refreshedBanksByTenure.filter {
                    it.isIssuerSelected == true
                }
                val selectedIssuerNameData2 =
                    temporaryAllIssuerList.filter { it.isIssuerSelected == true }
                val tenureWiseSelectedIssuerFullData = mutableListOf<IssuerBankModal>()
                for (value in selectedIssuerNameData) {
                    tenureWiseSelectedIssuerFullData.addAll(allIssuerBankList.filter {
                        it.issuerSchemeID == value.issuerSchemeID && it.issuerBankTenure == selectedTenure

                    })

                }

                if (tenureWiseSelectedIssuerFullData.isNotEmpty()) {
                    (activity as NavigationActivity).transactFragment(EMICompareFragment().apply {
                        arguments = Bundle().apply {
                            putString("compareActionName", compareActionName)
                            putSerializable("type", action)
                            putParcelableArrayList(
                                "dataModal",
                                tenureWiseSelectedIssuerFullData as java.util.ArrayList<out Parcelable>
                            )
                        }
                    })
                } else if (!selectedIssuerNameData2.isNotEmpty() && selectedTenure?.isNotEmpty() == true) {
                    ToastUtils.showToast(HDFCApplication.appContext,getString(R.string.please_select_one_issuer_bank))
                }
                else {
                    ToastUtils.showToast(HDFCApplication.appContext,getString(R.string.please_select_tenure))
                }
                Log.d("TWSIFD:- ", tenureWiseSelectedIssuerFullData.toString())
            }
        }
        //endregion
    }
//endregion

    //region===================Compare By Bank Select Event:-
    private fun compareByTenureSelectEventMethod() {
        binding?.tenureHeadingText?.visibility = View.VISIBLE
        binding?.tenureHeadingText?.text = getString(R.string.select_tenure)
        binding?.tenureRV?.visibility = View.VISIBLE
        binding?.issuerRV?.visibility = View.GONE
        binding?.selectBankHeadingText?.visibility = View.GONE
        binding?.selectBankHeadingText?.text = getString(R.string.select_banks_to_compare)
        binding?.selectAllBankCheckButton?.visibility = View.GONE
        issuerListAdapter.selectAllIssuerBank(
            false,
            CompareActionType.COMPARE_BY_TENURE.compareType
        )
        issuerListAdapter.unCheckAllIssuerBankRadioButton()
        compareActionName = CompareActionType.COMPARE_BY_TENURE.compareType
      
    }
//endregion

    //region======================Compare By Tenure Select Event Method:-
    private fun compareByBankSelectEventMethod() {
        binding?.tenureHeadingText?.visibility = View.GONE
        binding?.tenureRV?.visibility = View.GONE
        binding?.selectBankHeadingText?.visibility = View.VISIBLE
        binding?.selectBankHeadingText?.text = getString(R.string.select_bank_to_compare_tenure)
        binding?.selectAllBankCheckButton?.visibility = View.GONE
        runBlocking {
            temporaryAllIssuerList = allIssuerBankList.distinctBy { it.issuerID }.toMutableList()
        }
        issuerListAdapter.refreshBankList(temporaryAllIssuerList)
        issuerListAdapter.selectAllIssuerBank(false, CompareActionType.COMPARE_BY_BANK.compareType)
        issuerTenureListAdapter.unCheckAllTenureRadioButton()
        binding?.issuerRV?.visibility = View.VISIBLE
        compareActionName = CompareActionType.COMPARE_BY_BANK.compareType
     
    }
//endregion

    //region===================OnTenureSelectedEvent:-
    private fun onTenureSelectedEvent(position: Int) {
        if (position > -1) {
            selectedTenure = temporaryAllTenureList[position].bankTenure ?: ""
           
            binding?.selectAllBankCheckButton?.isChecked = false
            binding?.selectAllBankCheckButton?.text = getString(R.string.select_all_banks)

            Log.d("GsonResponse:- ", Gson().toJson(allIssuerBankList))
            refreshedBanksByTenure = allIssuerBankList.filter {
                it.issuerBankTenure == temporaryAllTenureList[position].bankTenure
            } as MutableList<IssuerBankModal>
            if (refreshedBanksByTenure.isNotEmpty()) {
                refreshIssuerBankOnTenureSelection(refreshedBanksByTenure)
                issuerListAdapter.selectAllIssuerBank(
                    false,
                    CompareActionType.COMPARE_BY_TENURE.compareType
                )
            } else {
                binding?.selectBankHeadingText?.visibility = View.GONE
                binding?.selectAllBankCheckButton?.visibility = View.GONE
                binding?.issuerRV?.visibility = View.GONE
            }
          
        }
    }
//endregion

    //region===================Refresh Issuer Bank List on Tenure Selection in RecyclerView:-
    private fun refreshIssuerBankOnTenureSelection(refreshBankList: MutableList<IssuerBankModal>) {
        issuerListAdapter.refreshBankList(refreshBankList)
        if (refreshBankList.size == 1) {
            binding?.selectBankHeadingText?.text = getString(R.string.select_bank)
            binding?.selectAllBankCheckButton?.visibility = View.GONE
        } else {
            binding?.selectBankHeadingText?.text = getString(R.string.select_banks_to_compare)
            binding?.selectAllBankCheckButton?.visibility = View.VISIBLE
        }
        binding?.selectBankHeadingText?.visibility = View.VISIBLE
        binding?.issuerRV?.visibility = View.VISIBLE
      
    }
//endregion

    override fun onStop() {
        super.onStop()
        compareActionName = null
        allIssuerBankList.clear()
        allIssuerTenureList.clear()
        temporaryAllIssuerList.clear()
        temporaryAllTenureList.clear()
        refreshedBanksByTenure.clear()
        totalRecord = "0"
        moreDataFlag = "0"
        totalRecord = "0"
        perPageRecord = "0"
        selectedTenure = null
    }


}
//region===============Below adapter is used to show the All Issuer Bank lists available:-
class IssuerListAdapter(
    var issuerList: MutableList<IssuerBankModal>,
    var emiCatalogueImagesMap: MutableMap<String, Uri>?
) :
    RecyclerView.Adapter<IssuerListAdapter.IssuerListViewHolder>() {

    var compareActionName: String? = null
    var index = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IssuerListViewHolder {
        val itemBinding = ItemEmiIssuerListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return IssuerListViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: IssuerListViewHolder, position: Int) {
        val modal = issuerList[position]

        //If Condition to check whether we have got EMI catalogue Data from File which we save after Settlement Success, Image Data from Host:-
        if (emiCatalogueImagesMap?.isNotEmpty() == true && emiCatalogueImagesMap?.containsKey(modal.issuerID) == true) {
            val imageUri: Uri? = emiCatalogueImagesMap?.get(modal.issuerID)
            val bitmap =
                MediaStore.Images.Media.getBitmap(HDFCApplication.appContext.contentResolver, imageUri)
            if (bitmap != null) {
                holder.viewBinding.issuerBankLogo.setImageBitmap(bitmap)
            }
        }
        //Else Condition to Read Images From Drawable and Show:-
        else {
            val resource: Int? = when (modal.issuerBankName?.toLowerCase(Locale.ROOT)?.trim()) {
                "hdfc bank cc" -> R.drawable.hdfc_issuer_icon
                "hdfc bank dc" -> R.drawable.hdfc_dc_issuer_icon
                "sbi card" -> R.drawable.sbi_issuer_icon
                "citi" -> R.drawable.citi_issuer_icon
                "icici" -> R.drawable.icici_issuer_icon
                "yes" -> R.drawable.yes_issuer_icon
                "kotak" -> R.drawable.kotak_issuer_icon
                "rbl" -> R.drawable.rbl_issuer_icon
                "scb" -> R.drawable.scb_issuer_icon
                "axis" -> R.drawable.axis_issuer_icon
                "indusind" -> R.drawable.indusind_issuer_icon
                else -> null
            }
            if (resource != null) {
                holder.viewBinding.issuerBankLogo.setImageResource(resource)
            }
        }

        //region===============Below Code will only execute in case of Multiple Selection:-
        if (compareActionName == CompareActionType.COMPARE_BY_TENURE.compareType) {
            if (modal.isIssuerSelected == true) {
                holder.viewBinding.issuerCheckedIV.visibility = View.VISIBLE
            } else {
                holder.viewBinding.issuerCheckedIV.visibility = View.GONE
            }
        }
        //endregion

        holder.viewBinding.issuerBankLogo.setOnClickListener {
            if (compareActionName == CompareActionType.COMPARE_BY_TENURE.compareType) {
                modal.isIssuerSelected = !modal.isIssuerSelected!!
                holder.viewBinding.issuerCheckedIV.visibility = View.VISIBLE
            } else {
                index = position
            }
            notifyDataSetChanged()
        }

        //region=================Below Code will execute in case of Single Issuer Bank Selection:-
        if (compareActionName == CompareActionType.COMPARE_BY_BANK.compareType) {
            if (index == position) {
                modal.isIssuerSelected = true
                holder.viewBinding.issuerCheckedIV.visibility = View.VISIBLE
            } else {
                modal.isIssuerSelected = false
                holder.viewBinding.issuerCheckedIV.visibility = View.GONE
            }
        }
        //endregion
    }

    override fun getItemCount(): Int = issuerList.size

    inner class IssuerListViewHolder(var viewBinding: ItemEmiIssuerListBinding) :
        RecyclerView.ViewHolder(viewBinding.root)


    //region==================Refresh Bank List Data on UI:-
    fun refreshBankList(refreshBankList: MutableList<IssuerBankModal>) {
        this.issuerList = refreshBankList
        notifyDataSetChanged()
    }
    //endregion

    //region===============Select All Issuer Bank:-
    fun selectAllIssuerBank(isAllStatus: Boolean, compareAction: String) {
        val dataSize = issuerList.size
        this.compareActionName = compareAction
        for (i in 0 until dataSize) {
            when (isAllStatus) {
                true -> issuerList[i].isIssuerSelected = true
                false -> issuerList[i].isIssuerSelected = false
            }
        }
        notifyDataSetChanged()
    }
    //endregion

    //region==================Uncheck All Tenure RadioButtons:-
    fun unCheckAllIssuerBankRadioButton() {
        index = -1
        notifyDataSetChanged()
    }
    //endregion

}
//endregion


//region====================Data Modal Class:-
@Parcelize
data class IssuerBankModal(
    var issuerBankTenure: String,
    var tenureInterestRate: String,
    var effectiveRate: String,
    var discountModel: String,
    var transactionAmount: String = "0",
    var discountAmount: String = "0",
    var discountFixedValue: String,
    var discountPercentage: String,
    var loanAmount: String = "0",
    var emiAmount: String,
    var totalEmiPay: String,
    var processingFee: String,
    var processingRate: String,
    var totalProcessingFee: String,
    var totalInterestPay: String = "0",
    val cashBackAmount: String = "0",
    var netPay: String,
    var tenureTAndC: String,
    var tenureWiseDBDTAndC: String,
    var discountCalculatedValue: String,
    var cashBackCalculatedValue: String,
    var issuerID: String,
    var issuerBankName: String?,
    var issuerSchemeID: String?,
    var isIssuerSelected: Boolean? = false
) : Parcelable

data class TenureBankModal(val bankTenure: String?, var isTenureSelected: Boolean? = false) :
    Serializable
//endregion

//region ENUM:-
enum class CompareActionType(val compareType: String) {
    COMPARE_BY_TENURE("compare_by_tenure"),
    COMPARE_BY_BANK("compare_by_bank")
}
//endregion
