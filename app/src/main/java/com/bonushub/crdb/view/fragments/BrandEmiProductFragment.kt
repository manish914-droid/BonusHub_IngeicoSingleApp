package com.bonushub.crdb.view.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.bonushub.crdb.HDFCApplication
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.BrandEmiListAndSearchUiBinding
import com.bonushub.crdb.db.AppDatabase
import com.bonushub.crdb.model.local.BrandEMIDataTable
import com.bonushub.crdb.model.local.BrandEMISubCategoryTable
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.model.remote.BrandEMIProductDataModal
import com.bonushub.crdb.repository.GenericResponse
import com.bonushub.crdb.repository.ServerRepository
import com.bonushub.crdb.serverApi.RemoteService
import com.bonushub.crdb.utils.ToastUtils
import com.bonushub.crdb.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.utils.logger
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.activity.TransactionActivity
import com.bonushub.crdb.view.adapter.BrandEMIMasterCategoryAdapter
import com.bonushub.crdb.view.adapter.BrandEmiProductAdapter
import com.bonushub.crdb.view.base.IDialog
import com.bonushub.crdb.viewmodel.BrandEmiMasterCategoryViewModel
import com.bonushub.crdb.viewmodel.BrandEmiProductViewModel
import com.bonushub.crdb.viewmodel.viewModelFactory.BrandEmiViewModelFactory
import com.bonushub.pax.utils.EDashboardItem
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class BrandEmiProductFragment : Fragment() {
    /** need to use Hilt for instance initializing here..*/
    private val remoteService: RemoteService = RemoteService()
    private val dbObj: AppDatabase = AppDatabase.getInstance(HDFCApplication.appContext)
    private val serverRepository: ServerRepository = ServerRepository(dbObj, remoteService)
    private lateinit var eDashBoardItem: EDashboardItem
    private val action by lazy { arguments?.getSerializable("type") ?: "" }
    private lateinit var brandEmiProductViewModel: BrandEmiProductViewModel
    private var brandEmiProductBinding: BrandEmiListAndSearchUiBinding? = null

    private var brandDataMaster: BrandEMIMasterDataModal? = null
    private val brandEMIProductAdapter by lazy {
        BrandEmiProductAdapter(::onItemClick)
    }


    private var brandEmiCatData: BrandEMISubCategoryTable? = null

    private var brandEmiSubCatData: BrandEMISubCategoryTable? = null

    // for backpress manage
    var isFirstTime = false
    var firstTimeData : ArrayList<BrandEMIProductDataModal?>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        brandEmiProductBinding=   BrandEmiListAndSearchUiBinding.inflate(layoutInflater, container, false)
        return brandEmiProductBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        eDashBoardItem = (arguments?.getSerializable("type")) as EDashboardItem
        brandEmiSubCatData = arguments?.getSerializable("brandEmiSubCat") as? BrandEMISubCategoryTable
       brandEmiCatData = arguments?.getSerializable("brandEmiCat") as? BrandEMISubCategoryTable
        brandDataMaster = arguments?.getSerializable("brandDataMaster") as? BrandEMIMasterDataModal

        logger("callInit","0 + ${brandEmiSubCatData?.brandID} + ${brandEmiSubCatData?.categoryID}","e")
        isFirstTime = true
        (activity as IDialog).showProgress()
        brandEmiProductViewModel= ViewModelProvider(this, BrandEmiViewModelFactory(serverRepository,brandEmiSubCatData?.brandID?:"",brandEmiSubCatData?.categoryID?:"")).get(
            BrandEmiProductViewModel::class.java
        )
        if (eDashBoardItem  == EDashboardItem.BRAND_EMI_CATALOGUE) {
            brandEmiProductBinding?.subHeaderView?.subHeaderText?.text = getString(R.string.brandEmiCatalogue)
            brandEmiProductBinding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_emicatalogue)
          //  eDashBoardItem= EDashboardItem.BRAND_EMI_CATALOGUE

        }else{
            brandEmiProductBinding?.subHeaderView?.subHeaderText?.text = "Brand Emi"//uiAction.title
        brandEmiProductBinding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_brandemi)
          //  eDashBoardItem= EDashboardItem.BRAND_EMI
    }
        //  brandMasterBinding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_brand_emi_sub_header_logo)

        brandEmiProductBinding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }




        brandEmiProductViewModel.brandEMIProductLivedata.observe(
            viewLifecycleOwner,
            {
                (activity as IDialog).hideProgress()
                when (val genericResp = it) {
                    is GenericResponse.Success -> {
                        println(Gson().toJson(genericResp.data))
                        println("dataListSize"+Gson().toJson(genericResp.data?.size))
                        setUpRecyclerView()
                        brandEMIProductAdapter.submitList(genericResp.data)
                        if(isFirstTime) {
                            firstTimeData = ArrayList()
                            firstTimeData!!.addAll(genericResp.data!!)
                            //genericResp.data?.let { it1 -> firstTimeData!!.addAll(it1) }
                            isFirstTime = false
                        }
                    }
                    is GenericResponse.Error -> {
                        ToastUtils.showToast(activity, genericResp.errorMessage)
                        println(genericResp.errorMessage.toString())
                    }
                    is GenericResponse.Loading -> {

                    }
                }
            })

        // region search product from server

        brandEmiProductBinding?.brandSearchET?.addTextChangedListener(object : TextWatcher {

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                if (TextUtils.isEmpty(p0.toString())) {
                    brandEmiProductBinding?.emptyTxt?.visibility = View.GONE
                    DialogUtilsNew1.hideKeyboardIfOpen(requireActivity())
//                    (activity as IDialog).showProgress()
//                    lifecycleScope.launch(Dispatchers.IO){
//                        logger("callBackPress","0 + ${brandEmiSubCatData?.brandID} + ${brandEmiSubCatData?.categoryID}","e")
//                        brandEmiProductViewModel.getBrandData("0",brandEmiSubCatData?.brandID?:"",brandEmiSubCatData?.categoryID?:"")
//                        brandEMIProductAdapter.submitList(firstTimeData)
//                        isFirstTime = false
//                    }
                    if(firstTimeData != null){
                        brandEMIProductAdapter.submitList(firstTimeData)
                        isFirstTime = false
                    }
                }
            }
        })
        brandEmiProductBinding?.searchButton?.setOnClickListener {

            DialogUtilsNew1.hideKeyboardIfOpen(requireActivity())
            brandEmiProductBinding?.emptyTxt?.visibility = View.GONE
            var searchText = brandEmiProductBinding?.brandSearchET?.text.toString().trim()

            (activity as IDialog).showProgress()

            lifecycleScope.launch(Dispatchers.IO){
                logger("callsearch","0 + ${brandEmiSubCatData?.brandID} + ${brandEmiSubCatData?.categoryID} ${searchText}","e")
                brandEmiProductViewModel.getBrandData("0",brandEmiSubCatData?.brandID?:"",brandEmiSubCatData?.categoryID?:"",searchText, true)
            }
            /*
            totalRecord = "0"
            brandEmiSearchedProductDataList.clear()
            //Initially on searching of product we were not showing the products category with requestType 3.But now with request type 11,product category name is coming
            field57RequestData = "${EMIRequestType.BRAND_EMI_Product_WithCategory.requestType}^$totalRecord^${brandEMIDataModal?.brandID}^^$searchedProductName"
            Log.d("57Data:-", field57RequestData.toString())
            fetchBrandEMIProductDataFromHost(isSearchedDataCall = true)
            */
        }
        // end region
    }
    //region===========================SetUp RecyclerView :-
    private fun setUpRecyclerView() {
        brandEmiProductBinding?.brandEmiMasterRV?.apply {
            layoutManager = LinearLayoutManager(context)
            hasFixedSize()
            itemAnimator = DefaultItemAnimator()
            adapter = brandEMIProductAdapter
        }
    }

    //endregion
    private fun onItemClick(productData: BrandEMIProductDataModal) {
        println("On Product Clicked " + Gson().toJson(productData))
    ///    ToastUtils.showToast(activity, Gson().toJson(productData))
        (activity as NavigationActivity).transactFragment(NewInputAmountFragment().apply {
            arguments = Bundle().apply {
                putSerializable("brandEmiProductData", productData)
                putSerializable("brandEmiSubCat", brandEmiSubCatData)
                putSerializable("brandEmiCatData", brandEmiCatData)
                putSerializable("brandDataMaster", brandDataMaster)
                putSerializable("type", eDashBoardItem )
            }
        })

        //val intent = Intent (activity, TransactionActivity::class.java)
        //activity?.startActivity(intent)

    }

}