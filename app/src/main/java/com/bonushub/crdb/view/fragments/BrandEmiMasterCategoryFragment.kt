package com.bonushub.crdb.view.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.bonushub.crdb.HDFCApplication
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.BrandEmiListAndSearchUiBinding
import com.bonushub.crdb.db.AppDatabase
import com.bonushub.crdb.model.local.BrandEMISubCategoryTable
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.repository.GenericResponse
import com.bonushub.crdb.repository.ServerRepository
import com.bonushub.crdb.serverApi.RemoteService
import com.bonushub.crdb.utils.ToastUtils
import com.bonushub.crdb.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.utils.logger
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.adapter.BrandEMIMasterCategoryAdapter
import com.bonushub.crdb.view.base.IDialog
import com.bonushub.crdb.viewmodel.BrandEmiMasterCategoryViewModel
import com.bonushub.crdb.viewmodel.viewModelFactory.BrandEmiViewModelFactory
import com.bonushub.pax.utils.EDashboardItem
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

@AndroidEntryPoint
class BrandEmiMasterCategoryFragment : Fragment() {

    /** need to use Hilt for instance initializing here..*/
    private val remoteService: RemoteService = RemoteService()
    private val dbObj: AppDatabase = AppDatabase.getInstance(HDFCApplication.appContext)
    private val serverRepository: ServerRepository = ServerRepository(dbObj, remoteService)
    //private val action by lazy { arguments?.getSerializable("type") ?: "" }
    private lateinit var eDashBoardItem: EDashboardItem
    private lateinit var brandEmiMasterCategoryViewModel: BrandEmiMasterCategoryViewModel
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
            brandMasterBinding?.subHeaderView?.subHeaderText?.text = getString(R.string.brandEmiCatalogue)
            brandMasterBinding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_emicatalogue)

        }else {
            brandMasterBinding?.subHeaderView?.subHeaderText?.text = "Brand Emi"//uiAction.title
            brandMasterBinding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_brandemi)

        }
        brandMasterBinding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()

        }

        brandMasterBinding?.emptyTxt?.text = "No Data Found"
        brandMasterBinding?.emptyTxt?.visibility = View.GONE
        brandMasterBinding?.brandSearchET?.setText("")

        (activity as IDialog).showProgress()
        brandEmiMasterCategoryViewModel = ViewModelProvider(this, BrandEmiViewModelFactory(serverRepository)).get(BrandEmiMasterCategoryViewModel::class.java)

        brandEmiMasterCategoryViewModel.brandEMIMasterSubCategoryLivedata.observe(
            viewLifecycleOwner,
            {
                (activity as IDialog).hideProgress()
                when (val genericResp = it) {
                    is GenericResponse.Success -> {
                        println(Gson().toJson(genericResp.data))
                        setUpRecyclerView()

                        brandEmiMasterDataList.clear()
                        brandMasterBinding?.brandSearchET?.setText("")
                        brandEmiMasterDataList.addAll(genericResp.data as List<BrandEMIMasterDataModal>)

                        brandEMIMasterCategoryAdapter.submitList(genericResp.data)
                    }
                    is GenericResponse.Error -> {
                        ToastUtils.showToast(activity, genericResp.errorMessage)
                        println(genericResp.errorMessage.toString())
                    }
                    is GenericResponse.Loading -> {

                    }
                }
            })


        //local search region

        brandMasterBinding?.brandSearchET?.addTextChangedListener(object : TextWatcher {

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                if (TextUtils.isEmpty(p0.toString())) {
                    brandMasterBinding?.emptyTxt?.visibility = View.GONE
                    brandEMIMasterCategoryAdapter.submitList(brandEmiMasterDataList)
                    DialogUtilsNew1.hideKeyboardIfOpen(requireActivity())
                }
            }
        })

        brandMasterBinding?.searchButton?.setOnClickListener {
            logger("searchButton","click","e")
            logger("searchButton",""+brandMasterBinding?.brandSearchET?.text.toString(),"e")
            DialogUtilsNew1.hideKeyboardIfOpen(requireActivity())
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
                dbObj.appDao.getBrandEMISubCategoryData() as ArrayList<BrandEMISubCategoryTable>
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