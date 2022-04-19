package com.bonushub.crdb.india.view.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.BrandEmiListAndSearchUiBinding
import com.bonushub.crdb.india.model.local.BrandEMISubCategoryTable
import com.bonushub.crdb.india.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.india.model.remote.BrandEMIProductDataModal
import com.bonushub.crdb.india.repository.GenericResponse
import com.bonushub.crdb.india.utils.EDashboardItem
import com.bonushub.crdb.india.utils.ToastUtils
import com.bonushub.crdb.india.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.india.utils.logger
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.adapter.BrandEmiProductAdapter
import com.bonushub.crdb.india.view.base.IDialog
import com.bonushub.crdb.india.viewmodel.BrandEmiProductViewModel
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BrandEmiProductFragment : Fragment() {

//    @Inject
//    lateinit var brandEmiProductViewModel: BrandEmiProductViewModel //by viewModels()

    // old
  /*  private val remoteService: RemoteService = RemoteService()
    private val dbObj: AppDatabase = AppDatabase.getInstance(HDFCApplication.appContext)
    private val serverRepository: ServerRepository = ServerRepository(dbObj, remoteService)
    private lateinit var brandEmiProductViewModel: BrandEmiProductViewModel*/

    private lateinit var brandEmiProductViewModel: BrandEmiProductViewModel

    private lateinit var eDashBoardItem: EDashboardItem
    private val action by lazy { arguments?.getSerializable("type") ?: "" }
    private var brandEmiProductBinding: BrandEmiListAndSearchUiBinding? = null

    private var brandDataMaster: BrandEMIMasterDataModal? = null
    private val brandEMIProductAdapter by lazy {
        BrandEmiProductAdapter(::onItemClick)
    }


    private var brandEmiCatData: BrandEMISubCategoryTable? = null

    private var brandEmiSubCatData: BrandEMISubCategoryTable? = null

    // for backpress manage
    var isFirstTime = true
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
        (activity as IDialog).showProgress()

        brandEmiProductViewModel= ViewModelProvider(this).get(BrandEmiProductViewModel::class.java)
        lifecycleScope.launch(Dispatchers.IO){
            brandEmiProductViewModel.getBrandData("0",brandEmiSubCatData?.brandID?:"",brandEmiSubCatData?.categoryID?:"")
        }
        // old
       /* brandEmiProductViewModel= ViewModelProvider(this, BrandEmiViewModelFactory(serverRepository,brandEmiSubCatData?.brandID?:"",brandEmiSubCatData?.categoryID?:"")).get(
            BrandEmiProductViewModel::class.java
        )*/

        if (eDashBoardItem  == EDashboardItem.BRAND_EMI_CATALOGUE) {
            brandEmiProductBinding?.subHeaderView?.subHeaderText?.text = getString(R.string.brandEmiCatalogue)
            brandEmiProductBinding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_emicatalogue)
            brandEmiProductBinding?.subHeaderView?.headerHome?.visibility= View.VISIBLE
            brandEmiProductBinding?.subHeaderView?.headerHome?.setOnClickListener {   (activity as NavigationActivity).transactFragment(
                DashboardFragment()
            ) }
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
                logger("kushal","observer","e")
                if(!isdataChange) {
                    when (val genericResp = it) {
                        is GenericResponse.Success -> {
                            //  if(!isObserve){
                            println(Gson().toJson(genericResp.data))
                            println("dataListSize" + Gson().toJson(genericResp.data?.size))
                            setUpRecyclerView()
                            brandEMIProductAdapter.submitList(genericResp.data)
                            if (isFirstTime) {
                                firstTimeData = ArrayList()
                                firstTimeData!!.addAll(genericResp.data!!)
                                //genericResp.data?.let { it1 -> firstTimeData!!.addAll(it1) }
                                isFirstTime = false
                            }
                            // }

                        }
                        is GenericResponse.Error -> {
                            ToastUtils.showToast(activity, genericResp.errorMessage)
                            println(genericResp.errorMessage.toString())
                        }
                        is GenericResponse.Loading -> {

                        }
                    }
                }else{
                    isdataChange = false
                    setUpRecyclerView()
                    brandEMIProductAdapter.submitList(firstTimeData)
                }
            })

        // region search product from server

        brandEmiProductBinding?.brandSearchET?.addTextChangedListener(object : TextWatcher {

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                if (TextUtils.isEmpty(p0.toString())) {
                    brandEmiProductBinding?.emptyTxt?.visibility = View.GONE
                    isEditTextBlank = true
                    DialogUtilsNew1.hideKeyboardIfOpen(requireActivity())

                    logger("kushal","brandSearchET1","e")
//                    if(comeFromStack)
//                    {
//                        brandEmiProductBinding?.brandSearchET?.setText("")
//                    }

                    if(firstTimeData != null){
                        logger("kushal","brandSearchET2","e")
                        brandEMIProductAdapter.submitList(firstTimeData)
                    }
                }else{
                    isEditTextBlank = false
                }
            }
        })
        brandEmiProductBinding?.searchButton?.setOnClickListener {
            logger("kushal","searchButton","e")
            DialogUtilsNew1.hideKeyboardIfOpen(requireActivity())
            brandEmiProductBinding?.emptyTxt?.visibility = View.GONE
            var searchText = brandEmiProductBinding?.brandSearchET?.text.toString().trim()

            isObserve = false

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


//        if(comeFromStack){
//            brandEmiProductBinding?.brandSearchET?.setText("")
//            comeFromStack = false
//            logger("kushal","comeFromStack","e")
//        }
    }

    override fun onResume() {
        super.onResume()
        logger("kushal","onResume","e")
        //brandEmiProductBinding?.brandSearchET?.setText("")
        //brandEMIProductAdapter.submitList(firstTimeData)
    }

    var comeFromStack = false
    var isObserve = false
    var isEditTextBlank = true
    var isdataChange = false
    override fun onStop() {
        super.onStop()
        logger("kushal","onStop","e")
        comeFromStack = true
        isObserve = true
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
                productData.productCategoryName= brandEmiSubCatData?.categoryName.toString()
                putSerializable("brandEmiProductData", productData)
                putSerializable("brandEmiSubCat", brandEmiSubCatData)
                putSerializable("brandEmiCatData", brandEmiCatData)
                putSerializable("brandDataMaster", brandDataMaster)
                putSerializable("type", eDashBoardItem )
            }
        })

        //val intent = Intent (activity, TransactionActivity::class.java)
        //activity?.startActivity(intent)
        if(!isEditTextBlank) {
            brandEmiProductBinding?.brandSearchET?.setText("")
            isdataChange = true
        }else{
            //isdataChange = false
            brandEmiProductBinding?.brandSearchET?.setText("")
            isdataChange = true
        }
    }

}