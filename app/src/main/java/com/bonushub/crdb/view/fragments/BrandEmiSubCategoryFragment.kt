package com.bonushub.crdb.view.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.BrandEmiListAndSearchUiBinding
import com.bonushub.crdb.model.local.BrandEMISubCategoryTable
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.model.remote.BrandEMIMasterSubCategoryDataModal
import com.bonushub.crdb.view.adapter.BrandEMISubCategoryAdapter
import com.google.gson.Gson
import dagger.hilt.EntryPoint
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BrandEmiSubCategoryFragment : Fragment() {
    private var brandMasterBinding: BrandEmiListAndSearchUiBinding? = null
    private var brandDataMaster: BrandEMIMasterDataModal? = null
    private var  brandSubCatList :ArrayList<BrandEMISubCategoryTable>?=null
    private var filteredSubCat:ArrayList<BrandEMISubCategoryTable> = arrayListOf()
    private val brandEMISubCategoryAdapter by lazy {
       BrandEMISubCategoryAdapter(::onCategoryItemClick)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        brandMasterBinding =
            BrandEmiListAndSearchUiBinding.inflate(layoutInflater, container, false)
        return brandMasterBinding?.root  }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.e("OPEN FRAG","TOUCHED")
        brandDataMaster = arguments?.getSerializable("brandDataMaster") as? BrandEMIMasterDataModal
        brandSubCatList= arguments?.getSerializable("brandSubCatList") as? ArrayList<BrandEMISubCategoryTable>

        filteredSubCat =
            brandSubCatList?.filter {
                it.brandID == brandDataMaster?.brandID && it.parentCategoryID == "0"
            } as ArrayList<BrandEMISubCategoryTable>
        setUpRecyclerView()
        brandEMISubCategoryAdapter.submitList(filteredSubCat)
       /* brandEMIMasterSubCategoryAdapter.refreshAdapterList(
            brandEmiMasterSubCategoryDataList
        )*/

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
        /* try {
             //  Log.d("CategoryName:- ", brandEmiMasterSubCategoryDataList[position].categoryName)
             Log.d("Category & Subcategory data- ", Gson().toJson(brandEMIAllDataList))
             val childFilteredList = brandEMIAllDataList.filter {
                 brandEmiMasterSubCategoryDataList[position].categoryID == it.parentCategoryID
             }
                     as MutableList<BrandEMIMasterSubCategoryDataModal>?
             Log.d("Data:- ", Gson().toJson(brandEmiMasterSubCategoryDataList))
             if (position > -1 && childFilteredList?.isNotEmpty() == true) {
                 navigateToBrandEMIDataByCategoryIDPage(position, true)
             } else
                 navigateToProductPage(isSubCategoryItem = false, position)
         } catch (ex: IndexOutOfBoundsException) {
             ex.printStackTrace()
         }*/
    }

}