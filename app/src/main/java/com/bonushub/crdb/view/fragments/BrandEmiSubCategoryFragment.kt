package com.bonushub.crdb.view.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.bonushub.crdb.databinding.BrandEmiListAndSearchUiBinding
import com.bonushub.crdb.model.local.BrandEMISubCategoryTable
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.adapter.BrandEMISubCategoryAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BrandEmiSubCategoryFragment : Fragment() {
    private var brandMasterBinding: BrandEmiListAndSearchUiBinding? = null
    private var brandDataMaster: BrandEMIMasterDataModal? = null
    private var brandSubCatList: ArrayList<BrandEMISubCategoryTable>? = null
    private var filteredSubCat: ArrayList<BrandEMISubCategoryTable> = arrayListOf()
    private val brandEMISubCategoryAdapter by lazy {
        BrandEMISubCategoryAdapter(::onCategoryItemClick)
    }


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
        Log.e("OPEN FRAG", "TOUCHED")
        brandDataMaster = arguments?.getSerializable("brandDataMaster") as? BrandEMIMasterDataModal
        brandSubCatList =
            arguments?.getSerializable("brandSubCatList") as? ArrayList<BrandEMISubCategoryTable>

        filteredSubCat =
            brandSubCatList?.filter {
                it.brandID == brandDataMaster?.brandID && it.parentCategoryID == "0"
            } as ArrayList<BrandEMISubCategoryTable>
        setUpRecyclerView()
        brandEMISubCategoryAdapter.submitList(filteredSubCat)


    }

    //region===========================SetUp RecyclerView :-
    private fun setUpRecyclerView() {
        brandMasterBinding?.brandEmiMasterRV?.apply {
            layoutManager = LinearLayoutManager(context)
            hasFixedSize()
            itemAnimator = DefaultItemAnimator()
            adapter = brandEMISubCategoryAdapter
        }
    }

    private fun onCategoryItemClick(brandEMISubCategoryTable: BrandEMISubCategoryTable) {
        Log.d("CategoryName:- ", brandEMISubCategoryTable.toString())
        filteredSubCat =
            brandSubCatList?.filter { brandEMISubCategoryTable.categoryID == it.parentCategoryID }
                    as ArrayList<BrandEMISubCategoryTable>
        Log.e(
            "FILTEREDLIST",
            filteredSubCat.toString() + "  Filter List Size --->  ${filteredSubCat.size}"
        )
        if (filteredSubCat.isNotEmpty()) {
            brandEMISubCategoryAdapter.submitList(filteredSubCat)
        } else {
            (activity as NavigationActivity).transactFragment(BrandEmiProductFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("brandDataMaster", brandDataMaster)
                }
            })

        }
    }

}