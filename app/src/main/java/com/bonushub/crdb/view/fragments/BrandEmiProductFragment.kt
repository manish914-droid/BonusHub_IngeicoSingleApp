package com.bonushub.crdb.view.fragments

import android.content.Intent
import android.os.Bundle
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
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.activity.TransactionActivity
import com.bonushub.crdb.view.adapter.BrandEMIMasterCategoryAdapter
import com.bonushub.crdb.view.adapter.BrandEmiProductAdapter
import com.bonushub.crdb.viewmodel.BrandEmiMasterCategoryViewModel
import com.bonushub.crdb.viewmodel.BrandEmiProductViewModel
import com.bonushub.crdb.viewmodel.viewModelFactory.BrandEmiViewModelFactory
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class BrandEmiProductFragment : Fragment() {
    /** need to use Hilt for instance initializing here..*/
    private val remoteService: RemoteService = RemoteService()
    private val dbObj: AppDatabase = AppDatabase.getInstance(HDFCApplication.appContext)
    private val serverRepository: ServerRepository = ServerRepository(dbObj, remoteService)

    private lateinit var brandEmiProductViewModel: BrandEmiProductViewModel
    private var brandEmiProductBinding: BrandEmiListAndSearchUiBinding? = null
    private var brandDataMaster: BrandEMIMasterDataModal? = null
    private val brandEMIProductAdapter by lazy {
        BrandEmiProductAdapter(::onItemClick)
    }

    private var brandEmiSubCatData: BrandEMISubCategoryTable? = null

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
        brandEmiSubCatData = arguments?.getSerializable("brandEmiSubCat") as? BrandEMISubCategoryTable
        brandDataMaster = arguments?.getSerializable("brandDataMaster") as? BrandEMIMasterDataModal

        brandEmiProductViewModel= ViewModelProvider(this, BrandEmiViewModelFactory(serverRepository,brandEmiSubCatData?.brandID?:"",brandEmiSubCatData?.categoryID?:"")).get(
            BrandEmiProductViewModel::class.java
        )
        brandEmiProductBinding?.subHeaderView?.subHeaderText?.text = "EMI Product"//uiAction.title
        //  brandMasterBinding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_brand_emi_sub_header_logo)

        brandEmiProductBinding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }


        brandEmiProductViewModel.brandEMIProductLivedata.observe(
            viewLifecycleOwner,
            {
                when (val genericResp = it) {
                    is GenericResponse.Success -> {
                        println(Gson().toJson(genericResp.data))
                        setUpRecyclerView()
                        brandEMIProductAdapter.submitList(genericResp.data)
                    }
                    is GenericResponse.Error -> {
                        ToastUtils.showToast(activity, genericResp.errorMessage)
                        println(genericResp.errorMessage.toString())
                    }
                    is GenericResponse.Loading -> {

                    }
                }
            })
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
        ToastUtils.showToast(activity, Gson().toJson(productData))
        (activity as NavigationActivity).transactFragment(NewInputAmountFragment().apply {
            arguments = Bundle().apply {
                putSerializable("brandEmiProductData", productData)
                putSerializable("brandEmiSubCat", brandEmiSubCatData)
                putSerializable("brandDataMaster", brandDataMaster)
            }
        })

        //val intent = Intent (activity, TransactionActivity::class.java)
        //activity?.startActivity(intent)

    }

}