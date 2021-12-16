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
import com.bonushub.crdb.model.CardProcessedDataModal
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
import com.bonushub.pax.utils.BhTransactionType
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import android.app.Activity

import android.content.Intent




@AndroidEntryPoint
class TenureSchemeActivity : AppCompatActivity() {
    /** need to use Hilt for instance initializing here..*/
    private val remoteService: RemoteService = RemoteService()
    private val dbObj: AppDatabase = AppDatabase.getInstance(HDFCApplication.appContext)
    private val serverRepository: ServerRepository = ServerRepository(dbObj, remoteService)

    private lateinit var tenureSchemeViewModel: TenureSchemeViewModel
    var binding: FragmentTenureSchemeBinding? = null
    private var selectedSchemeUpdatedPosition = -1

    private var cardProcessedDataModal: CardProcessedDataModal? = null
    private var transactionType = -1
    private var bankEMIRequestCode = "4"
    private var transactionAmount = "20000"
    private val brandID by lazy {
        intent.getStringExtra("brandID")
    }
    private val productID by lazy {
        intent.getStringExtra("productID")
    }
    private val imeiOrSerialNum by lazy {
        intent.getStringExtra("imeiOrSerialNum")
    }

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

        cardProcessedDataModal = intent?.getSerializableExtra("cardProcessedData") as? CardProcessedDataModal?
        transactionType        = intent?.getIntExtra("transactionType",-1) ?: -1

        var field57=""
        field57 = if(transactionType==BhTransactionType.BRAND_EMI.type) {
            "$bankEMIRequestCode^0^${brandID}^${productID}^${imeiOrSerialNum}" +
                    "^${/*cardBinValue.substring(0, 8)*/""}^$transactionAmount"
        }else{
            "$bankEMIRequestCode^0^1^0^^${cardProcessedDataModal?.getPanNumberData()?.substring(0, 8)}^$transactionAmount"
        }


        tenureSchemeViewModel = ViewModelProvider(
            this, TenureSchemeActivityVMFactory(
                serverRepository,
                cardProcessedDataModal?.getPanNumberData() ?: "",
                field57
            )
        ).get(TenureSchemeViewModel::class.java)


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
                val returnIntent = Intent()
                returnIntent.putExtra("EMITenureDataModal", (emiSchemeOfferDataList?.get(selectedSchemeUpdatedPosition)))
                setResult(RESULT_OK, returnIntent)
                finish()

                /*
                for cancel case

                val returnIntent = Intent()
                setResult(RESULT_CANCELED, returnIntent)
                finish()*/
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