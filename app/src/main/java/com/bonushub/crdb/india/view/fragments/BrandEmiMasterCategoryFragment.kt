package com.bonushub.crdb.india.view.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.BrandEmiListAndSearchUiBinding
import com.bonushub.crdb.india.db.AppDao
import com.bonushub.crdb.india.model.local.BrandEMISubCategoryTable
import com.bonushub.crdb.india.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.india.repository.GenericResponse
import com.bonushub.crdb.india.utils.EDashboardItem
import com.bonushub.crdb.india.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.india.utils.logger
import com.bonushub.crdb.india.utils.refreshSubToolbarLogos
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.adapter.BrandEMIMasterCategoryAdapter
import com.bonushub.crdb.india.view.base.BaseActivityNew
import com.bonushub.crdb.india.view.base.IDialog
import com.bonushub.crdb.india.viewmodel.BrandEmiMasterCategoryViewModel
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class BrandEmiMasterCategoryFragment : Fragment() {


    @Inject
    lateinit var appDao:AppDao

    private val brandEmiMasterCategoryViewModel: BrandEmiMasterCategoryViewModel by viewModels()

    // old
//    private val remoteService: RemoteService = RemoteService()//
//    private val dbObj: AppDatabase = AppDatabase.getInstance(HDFCApplication.appContext)//
   // private val serverRepository: ServerRepository = ServerRepository(dbObj, remoteService)//
    ////private val action by lazy { arguments?.getSerializable("type") ?: "" }
   // private lateinit var brandEmiMasterCategoryViewModel: BrandEmiMasterCategoryViewModel//

    private lateinit var eDashBoardItem: EDashboardItem

    private var brandMasterBinding: BrandEmiListAndSearchUiBinding? = null
    private val brandEMIMasterCategoryAdapter by lazy {
        BrandEMIMasterCategoryAdapter(::onItemClick)
    }

    var brandEmiMasterDataList = mutableListOf<BrandEMIMasterDataModal>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        brandMasterBinding =
            BrandEmiListAndSearchUiBinding.inflate(layoutInflater, container, false)
        return brandMasterBinding?.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        eDashBoardItem = (arguments?.getSerializable("type")) as EDashboardItem
        if (eDashBoardItem == EDashboardItem.BRAND_EMI_CATALOGUE) {
//            brandMasterBinding?.subHeaderView?.subHeaderText?.text = getString(R.string.brandEmiCatalogue)
//            brandMasterBinding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_emicatalogue)

            refreshSubToolbarLogos(this,null,R.drawable.ic_emicatalogue, getString(R.string.brandEmiCatalogue))

            brandMasterBinding?.subHeaderView?.headerHome?.visibility= View.VISIBLE
            brandMasterBinding?.subHeaderView?.headerHome?.setOnClickListener {   (activity as NavigationActivity).transactFragment(
                DashboardFragment()
            ) }
        }else {
//            brandMasterBinding?.subHeaderView?.subHeaderText?.text = "Brand Emi"//uiAction.title
//            brandMasterBinding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_brandemi)
            refreshSubToolbarLogos(this,null,R.drawable.ic_brandemi, "Brand Emi")

        }

        (activity as NavigationActivity).manageTopToolBar(false)

        brandMasterBinding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()

        }

        brandMasterBinding?.emptyTxt?.text = "No Data Found"
        brandMasterBinding?.emptyTxt?.visibility = View.GONE
        brandMasterBinding?.brandSearchET?.setText("")

        (activity as IDialog).showProgress()
        // old
        //brandEmiMasterCategoryViewModel = ViewModelProvider(this, BrandEmiViewModelFactory(serverRepository)).get(BrandEmiMasterCategoryViewModel::class.java)

        brandEmiMasterCategoryViewModel.brandEMIMasterSubCategoryLivedata.observe(
            viewLifecycleOwner
        ) {
            (activity as IDialog).hideProgress()
            when (val genericResp = it) {
                is GenericResponse.Success -> {
                    println(Gson().toJson(genericResp.data))
                    setUpRecyclerView()

                    brandEmiMasterDataList.clear()
                    brandMasterBinding?.brandSearchET?.setText("")
                    brandEmiMasterDataList.addAll(genericResp.data as List<BrandEMIMasterDataModal>)

                    val dataList = genericResp.data.sortedBy { it.brandName }
                    brandEMIMasterCategoryAdapter.submitList(dataList)
                }
                is GenericResponse.Error -> {
                    //    ToastUtils.showToast(activity, genericResp.errorMessage)
                    //(activity as MainActivity).show
                    lifecycleScope.launch(Dispatchers.Main) {
                        (activity as BaseActivityNew).alertBoxWithActionNew(
                            genericResp.errorMessage ?: "Oops something went wrong",
                            "",
                            R.drawable.ic_info_new,
                            getString(R.string.positive_button_ok),"",false,
                            true,
                            {
                                /* finish()
                                 goToDashBoard()*/
                                parentFragmentManager.popBackStack()
                            },
                            {})
                    }
                    println(genericResp.errorMessage.toString())
                }
                is GenericResponse.Loading -> {

                }
            }
        }


        //local search region

        brandMasterBinding?.brandSearchET?.addTextChangedListener(object : TextWatcher {

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                if (TextUtils.isEmpty(p0.toString())) {
                    brandMasterBinding?.emptyTxt?.visibility = View.GONE
                    brandEmiMasterDataList.sortBy { it.brandName }
                    brandEMIMasterCategoryAdapter.submitList(brandEmiMasterDataList)
                    DialogUtilsNew1.hideKeyboardIfOpen(requireActivity())
                }
            }
        })

        brandMasterBinding?.searchButton?.setOnClickListener {
            logger("searchButton","click","e")
            logger("searchButton",""+brandMasterBinding?.brandSearchET?.text.toString(),"e")
            DialogUtilsNew1.hideKeyboardIfOpen(requireActivity())

            if(brandMasterBinding?.brandSearchET?.text.toString().trim().isEmpty())
            {
                return@setOnClickListener
            }

            (activity as IDialog).showProgress()
            getSearchedBrands(brandMasterBinding?.brandSearchET?.text.toString().trim())
        }
        // local search end region
    }

    //region===========================SetUp RecyclerView :-
    private fun setUpRecyclerView() {
        brandMasterBinding?.brandEmiMasterRV?.apply {
            layoutManager = LinearLayoutManager(context)
            hasFixedSize()
            itemAnimator = DefaultItemAnimator()
            adapter = brandEMIMasterCategoryAdapter
        }
    }

    //endregion
    private fun onItemClick(brandDataMaster: BrandEMIMasterDataModal) {
        println("On Brand Clicked " + Gson().toJson(brandDataMaster))
      //  ToastUtils.showToast(activity, Gson().toJson(brandDataMaster))

        lifecycleScope.launch(Dispatchers.IO) {
            val brandSubCatList: ArrayList<BrandEMISubCategoryTable> =
                appDao.getBrandEMISubCategoryData() as ArrayList<BrandEMISubCategoryTable>
            val  filteredSubCat =
                brandSubCatList.filter {
                    it.brandID == brandDataMaster.brandID && it.parentCategoryID == "0"
                } as ArrayList<BrandEMISubCategoryTable>
            if (brandSubCatList.isNotEmpty()) {
                (activity as NavigationActivity).transactFragment(BrandEmiSubCategoryFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("brandDataMaster", brandDataMaster)
                        putSerializable("brandSubCatList", brandSubCatList)
                        putSerializable("filteredSubCat", filteredSubCat)
                        putSerializable("fromBranddata", true)
                        putSerializable("type", eDashBoardItem)

                      //  putBoolean("navigateFromMaster",true)
                        // putParcelableArrayList("brandSubCatList",ArrayList<Parcelable>( brandSubCatList))
                    }
                })

            } else {
                (activity as NavigationActivity).transactFragment(BrandEmiProductFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("brandDataMaster", brandDataMaster)
                        putSerializable("type", eDashBoardItem)
                    }
                })
            }
        }
    }

    //region===================Get Searched Results from Brand List:-
    private fun getSearchedBrands(searchText: String?) {

        val searchedDataList = mutableListOf<BrandEMIMasterDataModal>()
        lifecycleScope.launch(Dispatchers.Default) {
            if (!TextUtils.isEmpty(searchText)) {
                for (i in 0 until brandEmiMasterDataList.size) {
                    val brandData = brandEmiMasterDataList[i]
                    //check whether brand name contains letter which is inserted in search box:-
                    if (brandData.brandName.toLowerCase(Locale.ROOT).trim()
                            .contains(searchText?.toLowerCase(Locale.ROOT)?.trim()!!)
                    )
                        searchedDataList.add(
                            BrandEMIMasterDataModal(
                                brandData.brandID, brandData.brandName,
                                brandData.mobileNumberBillNumberFlag
                            )
                        )
                    Log.d("searchedDataList:- ", searchedDataList.toString())
                }
                lifecycleScope.launch(Dispatchers.Main) {

                    if(searchedDataList.size>0) {
                        searchedDataList.sortBy { it.brandName }
                        brandEMIMasterCategoryAdapter.submitList(searchedDataList)
                        brandMasterBinding?.emptyTxt?.visibility = View.GONE
                        (activity as IDialog).hideProgress()
                    }
                    else{

                        brandEMIMasterCategoryAdapter.submitList(searchedDataList)
                        (activity as IDialog).hideProgress()
                        brandMasterBinding?.emptyTxt?.visibility = View.VISIBLE

                    }
                }
            } else
                withContext(Dispatchers.Main) {
                    (activity as IDialog).hideProgress()

                }
        }
    }
    //endregion

}