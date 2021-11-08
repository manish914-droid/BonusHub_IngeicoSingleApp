package com.bonushub.crdb.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.bonushub.crdb.HDFCApplication
import com.bonushub.crdb.databinding.BrandEmiListAndSearchUiBinding
import com.bonushub.crdb.db.AppDatabase
import com.bonushub.crdb.model.local.BrandEMISubCategoryTable
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.repository.GenericResponse
import com.bonushub.crdb.repository.ServerRepository
import com.bonushub.crdb.serverApi.RemoteService
import com.bonushub.crdb.utils.ToastUtils
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.adapter.BrandEMIMasterCategoryAdapter
import com.bonushub.crdb.viewmodel.BrandEmiMasterCategoryViewModel
import com.bonushub.crdb.viewmodel.viewModelFactory.BrandEmiViewModelFactory
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BrandEmiMasterCategoryFragment : Fragment() {

    /** need to use Hilt for instance initializing here..*/
    private val remoteService: RemoteService = RemoteService()
    private val dbObj: AppDatabase = AppDatabase.getInstance(HDFCApplication.appContext)
    private val serverRepository: ServerRepository = ServerRepository(dbObj, remoteService)


    private lateinit var brandEmiMasterCategoryViewModel: BrandEmiMasterCategoryViewModel
    private var brandMasterBinding: BrandEmiListAndSearchUiBinding? = null
    private val brandEMIMasterCategoryAdapter by lazy {
        BrandEMIMasterCategoryAdapter(::onItemClick)
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

        brandEmiMasterCategoryViewModel =
            ViewModelProvider(this, BrandEmiViewModelFactory(serverRepository)).get(
                BrandEmiMasterCategoryViewModel::class.java
            )
        brandEmiMasterCategoryViewModel.brandEMIMasterSubCategoryLivedata.observe(
            viewLifecycleOwner,
            {
                when (val genericResp = it) {
                    is GenericResponse.Success -> {
                        println(Gson().toJson(genericResp.data))
                        setUpRecyclerView()
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
        ToastUtils.showToast(activity, Gson().toJson(brandDataMaster))

        lifecycleScope.launch(Dispatchers.IO) {
            val brandSubCatList: ArrayList<BrandEMISubCategoryTable> =
                dbObj.appDao.getBrandEMISubCategoryData() as ArrayList<BrandEMISubCategoryTable>
            if (brandSubCatList.isNotEmpty()) {
                (activity as NavigationActivity).transactFragment(BrandEmiSubCategoryFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("brandDataMaster", brandDataMaster)
                        putSerializable("brandSubCatList", brandSubCatList)
                        // putParcelableArrayList("brandSubCatList",ArrayList<Parcelable>( brandSubCatList))
                    }
                })

            } else {
                (activity as NavigationActivity).transactFragment(BrandEmiProductFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("brandDataMaster", brandDataMaster)
                    }
                })
            }
        }
    }

}