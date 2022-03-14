package com.bonushub.crdb.india.view.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.bonushub.crdb.india.databinding.BrandEmiListAndSearchUiBinding
import com.bonushub.crdb.india.model.local.BrandEMISubCategoryTable
import com.bonushub.crdb.india.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.adapter.BrandEMISubCategoryAdapter
import dagger.hilt.android.AndroidEntryPoint
@AndroidEntryPoint
class TestSubCatFrag : Fragment(){




        private var brandSubCatBinding: BrandEmiListAndSearchUiBinding? = null
        private var brandDataMaster: BrandEMIMasterDataModal? = null
        private var brandSubCatList: ArrayList<BrandEMISubCategoryTable>? = null
        private var filteredSubCat: ArrayList<BrandEMISubCategoryTable> = arrayListOf()
        private val brandEMISubCategoryAdapter by lazy {
            BrandEMISubCategoryAdapter(::onCategoryItemClick)
        }
        private var fragNavigateFromMaster = false


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
            Log.e("OPEN FRAG", "TOUCHED")
            brandDataMaster = arguments?.getSerializable("brandDataMaster") as? BrandEMIMasterDataModal
            brandSubCatList = arguments?.getSerializable("brandSubCatList") as? ArrayList<BrandEMISubCategoryTable>
            fragNavigateFromMaster=arguments?.getBoolean("navigateFromMaster")?:false
            filteredSubCat= arguments?.getSerializable("filteredSubCat") as ArrayList<BrandEMISubCategoryTable>

            brandSubCatBinding?.subHeaderView?.subHeaderText?.text = "Brand Emi"//uiAction.title
            //  brandSubCatBinding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_brand_emi_sub_header_logo)

            brandSubCatBinding?.subHeaderView?.backImageButton?.setOnClickListener {
                parentFragmentManager.popBackStackImmediate()
            }


            /*    filteredSubCat =
                    brandSubCatList?.filter {
                        it.brandID == brandDataMaster?.brandID && it.parentCategoryID == "0"
                    } as ArrayList<BrandEMISubCategoryTable>*/
            setUpRecyclerView()
            brandEMISubCategoryAdapter.submitList(filteredSubCat)


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
            Log.d("CategoryName:- ", brandEMISubCategoryTable.toString())
            filteredSubCat =
                brandSubCatList?.filter { brandEMISubCategoryTable.categoryID == it.parentCategoryID }
                        as ArrayList<BrandEMISubCategoryTable>
            Log.e(
                "FILTEREDLIST",
                filteredSubCat.toString() + "  Filter List Size --->  ${filteredSubCat.size}"
            )
            if (filteredSubCat.isNotEmpty()) {
                (activity as NavigationActivity).transactFragment(BrandEmiSubCategoryFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("brandDataMaster", brandDataMaster)
                        putSerializable("brandSubCatList", brandSubCatList)
                        putSerializable("filteredSubCat", filteredSubCat)

                        //  putBoolean("navigateFromMaster",true)
                        // putParcelableArrayList("brandSubCatList",ArrayList<Parcelable>( brandSubCatList))
                    }
                })
                //  brandEMISubCategoryAdapter.submitList(filteredSubCat)
            } else {
                (activity as NavigationActivity).transactFragment(BrandEmiProductFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("brandEmiSubCat", brandEMISubCategoryTable)
                    }
                })

            }
        }

    }


