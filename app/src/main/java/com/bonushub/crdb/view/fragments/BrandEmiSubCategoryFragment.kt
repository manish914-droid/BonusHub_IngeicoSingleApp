package com.bonushub.crdb.view.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.BrandEmiListAndSearchUiBinding
import com.bonushub.crdb.model.local.BrandEMISubCategoryTable
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.adapter.BrandEMISubCategoryAdapter
import com.bonushub.pax.utils.EDashboardItem
import dagger.hilt.android.AndroidEntryPoint

var catagory:BrandEMISubCategoryTable?=null
// var FRAGMENT_COUNTER=0
@AndroidEntryPoint
class BrandEmiSubCategoryFragment : Fragment() {
    private var brandSubCatBinding: BrandEmiListAndSearchUiBinding? = null
    private var brandDataMaster: BrandEMIMasterDataModal? = null
    private var brandSubCatList: ArrayList<BrandEMISubCategoryTable>? = null
    private var filteredSubCat: ArrayList<BrandEMISubCategoryTable> = arrayListOf()
    private lateinit var eDashBoardItem: EDashboardItem

    private var openedFragmentFromBrandData=false
    private val brandEMISubCategoryAdapter by lazy {
        BrandEMISubCategoryAdapter(::onCategoryItemClick)
    }

  override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        brandSubCatBinding =
            BrandEmiListAndSearchUiBinding.inflate(layoutInflater, container, false)
        return brandSubCatBinding?.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        eDashBoardItem = (arguments?.getSerializable("type")) as EDashboardItem
        Log.e("OPEN FRAG", "TOUCHED")
        brandDataMaster = arguments?.getSerializable("brandDataMaster") as? BrandEMIMasterDataModal
        brandSubCatList = arguments?.getSerializable("brandSubCatList") as? ArrayList<BrandEMISubCategoryTable>
        filteredSubCat= arguments?.getSerializable("filteredSubCat") as ArrayList<BrandEMISubCategoryTable>
        openedFragmentFromBrandData= arguments?.getBoolean("fromBranddata",false) == true
        if (eDashBoardItem  == EDashboardItem.BRAND_EMI_CATALOGUE) {
            brandSubCatBinding?.subHeaderView?.subHeaderText?.text = getString(R.string.brandEmiCatalogue)
            brandSubCatBinding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_emicatalogue)

        }else {
            brandSubCatBinding?.subHeaderView?.subHeaderText?.text = "Brand Emi"//uiAction.title
            brandSubCatBinding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_brandemi)
        }
        brandSubCatBinding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }

        if(openedFragmentFromBrandData && filteredSubCat.isEmpty()){
            brandSubCatBinding?.emptyTxt?.visibility=View.VISIBLE
            brandSubCatBinding?.dataSearchUi?.visibility=View.GONE
        }else {
            brandSubCatBinding?.emptyTxt?.visibility=View.GONE
            brandSubCatBinding?.dataSearchUi?.visibility=View.VISIBLE
            setUpRecyclerView()
            brandEMISubCategoryAdapter.submitList(filteredSubCat)
        }

    }

    //region===========================SetUp RecyclerView :-
    private fun setUpRecyclerView() {
        brandSubCatBinding?.brandEmiMasterRV?.apply {
            layoutManager = LinearLayoutManager(context)
            hasFixedSize()
            itemAnimator = DefaultItemAnimator()
            adapter = brandEMISubCategoryAdapter
        }
    }

    private fun onCategoryItemClick(brandEMISubCategoryTable: BrandEMISubCategoryTable) {
     if(openedFragmentFromBrandData){
         catagory=brandEMISubCategoryTable
         Log.d("CategoryName:- ", brandEMISubCategoryTable.toString())
     }
       // Log.d("CategoryName:- ", brandEMISubCategoryTable.toString())
        filteredSubCat = brandSubCatList?.filter { brandEMISubCategoryTable.categoryID == it.parentCategoryID }
                    as ArrayList<BrandEMISubCategoryTable>
        Log.e("FILTEREDLIST", filteredSubCat.toString() + "  Filter List Size --->  ${filteredSubCat.size}")
        if (filteredSubCat.isNotEmpty()) {
        //    FRAGMENT_COUNTER += 1

            (activity as NavigationActivity).transactSubCatFragment(false,brandDataMaster,brandSubCatList,filteredSubCat,eDashBoardItem)

          //  brandEMISubCategoryAdapter.submitList(filteredSubCat)
        } else {
             //   FRAGMENT_COUNTER= "0".toInt()
            (activity as NavigationActivity).transactFragment(BrandEmiProductFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("brandEmiSubCat", brandEMISubCategoryTable)
                    putSerializable("brandEmiCat", catagory)
                    putSerializable("brandDataMaster", brandDataMaster)
                    putSerializable("type", eDashBoardItem)
                }
            })

        }
    }

}