package com.bonushub.crdb.view.activity

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.bonushub.crdb.HDFCApplication
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentTenureSchemeBinding
import com.bonushub.crdb.db.AppDatabase
import com.bonushub.crdb.model.remote.BankEMITenureDataModal
import com.bonushub.crdb.model.remote.TenuresWithIssuerTncs
import com.bonushub.crdb.repository.GenericResponse
import com.bonushub.crdb.repository.ServerRepository
import com.bonushub.crdb.serverApi.RemoteService
import com.bonushub.crdb.utils.ToastUtils
import com.bonushub.crdb.view.adapter.EMISchemeAndOfferAdapter
import com.bonushub.crdb.viewmodel.BrandEmiMasterCategoryViewModel
import com.bonushub.crdb.viewmodel.TenureSchemeViewModel
import com.bonushub.crdb.viewmodel.viewModelFactory.BrandEmiViewModelFactory
import com.bonushub.crdb.viewmodel.viewModelFactory.TenureSchemeActivityVMFactory
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TenureSchemeActivity : AppCompatActivity() {
    /** need to use Hilt for instance initializing here..*/
    private val remoteService: RemoteService = RemoteService()
    private val dbObj: AppDatabase = AppDatabase.getInstance(HDFCApplication.appContext)
    private val serverRepository: ServerRepository = ServerRepository(dbObj, remoteService)

    private lateinit var tenureSchemeViewModel: TenureSchemeViewModel
    var binding: FragmentTenureSchemeBinding? = null
    private var selectedSchemeUpdatedPosition = -1
    private var emiSchemeOfferDataList: MutableList<BankEMITenureDataModal>? = mutableListOf()
    private val emiSchemeAndOfferAdapter: EMISchemeAndOfferAdapter by lazy {
        EMISchemeAndOfferAdapter(
            1,
            emiSchemeOfferDataList,
            ::onSchemeClickEvent
        )
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentTenureSchemeBinding.inflate(layoutInflater)
        setContentView(binding?.root)
      //  getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        tenureSchemeViewModel=  ViewModelProvider(this, TenureSchemeActivityVMFactory(serverRepository)).get(
            TenureSchemeViewModel::class.java)

        binding?.toolbarTxn?.mainToolbarStart?.setBackgroundResource(R.drawable.ic_back_arrow_white)

       /* binding?.toolbarTxn?.mainToolbarStart?.setOnClickListener {
            navigateControlBackToTransaction(
                isTransactionContinue = false
            )
        }*/
      //  tenureSchemeViewModel = ViewModelProvider(this, BrandEmiViewModelFactory(serverRepository)).get(TenureSchemeViewModel::class.java)
        tenureSchemeViewModel.emiTenureLiveData.observe(
            this,
            {
                when (val genericResp = it) {
                    is GenericResponse.Success -> {
                        println(Gson().toJson(genericResp.data))
                        val resp= genericResp.data as TenuresWithIssuerTncs
                        emiSchemeOfferDataList=resp.bankEMISchemesDataList
                        setUpRecyclerView()

                    }
                    is GenericResponse.Error -> {
                        ToastUtils.showToast(this, genericResp.errorMessage)
                        println(genericResp.errorMessage.toString())
                    }
                    is GenericResponse.Loading -> {

                    }
                }
            })



        //region======================Proceed TXN Floating Button OnClick Event:-
        binding?.emiSchemeFloatingButton?.setOnClickListener {
            if (selectedSchemeUpdatedPosition != -1) {
                ToastUtils.showToast(
                    this,
                    (emiSchemeOfferDataList?.get(selectedSchemeUpdatedPosition)).toString()
                )
                Log.e(
                    "SELECTED TENURE ->  ",
                    (emiSchemeOfferDataList?.get(selectedSchemeUpdatedPosition)).toString()
                )

            }   else
                ToastUtils.showToast(this,getString(R.string.please_select_scheme))
        }
        //endregion
    }


    //region==========================onClickEvent==================================================
    private fun onSchemeClickEvent(position: Int) {
        Log.d("Position:- ", emiSchemeOfferDataList?.get(position).toString())
        selectedSchemeUpdatedPosition = position
    }
    //endregion
    //region=========================SetUp RecyclerView Data:-
    private fun setUpRecyclerView() {

            binding?.emiSchemeOfferRV?.apply {
                layoutManager = LinearLayoutManager(context)
                itemAnimator = DefaultItemAnimator()
                adapter = emiSchemeAndOfferAdapter
            }


    }
    //endregion


}