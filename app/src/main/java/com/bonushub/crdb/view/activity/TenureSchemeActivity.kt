package com.bonushub.crdb.view.activity

import android.os.Bundle
import android.util.Log
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
import com.bonushub.crdb.viewmodel.TenureSchemeViewModel
import com.bonushub.crdb.viewmodel.viewModelFactory.TenureSchemeActivityVMFactory
import com.bonushub.pax.utils.BhTransactionType
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

import android.content.Intent
import com.bonushub.crdb.model.remote.BankEMIIssuerTAndCDataModal
import com.bonushub.crdb.utils.logger
import com.bonushub.crdb.view.base.BaseActivityNew


@AndroidEntryPoint
class TenureSchemeActivity : BaseActivityNew() {
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
   // private var transactionAmount = "20000"
    private val brandID by lazy {
        intent.getStringExtra("brandID")
    }
    private val productID by lazy {
        intent.getStringExtra("productID")
    }
    private val imeiOrSerialNum by lazy {
        intent.getStringExtra("imeiOrSerialNum")
    }
 private val emiSchemeOfferDataListFromIntent by lazy {
        intent.getParcelableArrayListExtra<BankEMITenureDataModal>("emiSchemeOfferDataList") as MutableList<BankEMITenureDataModal>
    }
private val emiIssuerTAndCDataFromIntent by lazy {
        intent.getParcelableExtra("emiIssuerTAndCDataList") as BankEMIIssuerTAndCDataModal?
    }



    private var emiSchemeOfferDataList: MutableList<BankEMITenureDataModal>? = mutableListOf()
    lateinit var emiIssuerTAndCData: BankEMIIssuerTAndCDataModal
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
                    "^${/*cardBinValue.substring(0, 8)*/""}^${cardProcessedDataModal?.getTransactionAmount()}"
        }else{
            "$bankEMIRequestCode^0^1^0^^${/*cardProcessedDataModal?.getPanNumberData()?.substring(0, 8)*/""}^${cardProcessedDataModal?.getTransactionAmount()}"
        }

        if(transactionType==BhTransactionType.BRAND_EMI.type || transactionType==BhTransactionType.EMI_SALE.type) {
         showProgress()
            tenureSchemeViewModel = ViewModelProvider(
                this, TenureSchemeActivityVMFactory(
                    serverRepository,
                    cardProcessedDataModal?.getPanNumberData() ?: "",
                    field57
                )
            ).get(TenureSchemeViewModel::class.java)
            //  tenureSchemeViewModel = ViewModelProvider(this, BrandEmiViewModelFactory(serverRepository)).get(TenureSchemeViewModel::class.java)
            tenureSchemeViewModel.emiTenureLiveData.observe(
                this,
                {
hideProgress()
                    when (val genericResp = it) {
                        is GenericResponse.Success -> {
                            println(Gson().toJson(genericResp.data))
                            val resp= genericResp.data as TenuresWithIssuerTncs
                            emiSchemeOfferDataList=resp.bankEMISchemesDataList
                            emiIssuerTAndCData=resp.bankEMIIssuerTAndCList
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

        }else if (transactionType==BhTransactionType.SALE.type ){
            emiSchemeOfferDataList=emiSchemeOfferDataListFromIntent
            emiIssuerTAndCData= emiIssuerTAndCDataFromIntent!!
            setUpRecyclerView()
        }


        binding?.toolbarTxn?.mainToolbarStart?.apply {  setBackgroundResource(R.drawable.ic_back_arrow_white)
        setOnClickListener {
            finish()
            startActivity(Intent(this@TenureSchemeActivity, NavigationActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
        }

       /* binding?.toolbarTxn?.mainToolbarStart?.setOnClickListener {
            navigateControlBackToTransaction(
                isTransactionContinue = false
            )
        }*/


        //region======================Proceed TXN Floating Button OnClick Event:-
        binding?.emiSchemeFloatingButton?.setOnClickListener {
            if (selectedSchemeUpdatedPosition != -1) {
               /* ToastUtils.showToast(
                    this,
                    (emiSchemeOfferDataList?.get(selectedSchemeUpdatedPosition)).toString()
                )*/
                Log.e(
                    "SELECTED TENURE ->  ",
                    (emiSchemeOfferDataList?.get(selectedSchemeUpdatedPosition)).toString()
                )
                Log.e(
                    "Tncc ->  ",
                    (emiSchemeOfferDataList?.get(selectedSchemeUpdatedPosition)).toString()
                )
                val returnIntent = Intent()
                returnIntent.putExtra("EMITenureDataModal", (emiSchemeOfferDataList?.get(selectedSchemeUpdatedPosition)))
                returnIntent.putExtra("emiIssuerTAndCDataList", (emiIssuerTAndCData))
                returnIntent.putExtra("cardProcessedDataModal", cardProcessedDataModal)
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

    override fun onBackPressed() {
        // for stopping back press
    }

    //region==========================onClickEvent==================================================
    private fun onSchemeClickEvent(position: Int) {
        Log.d("Position:- ", emiSchemeOfferDataList?.get(position).toString())
        selectedSchemeUpdatedPosition = position

//        var newList: MutableList<BankEMITenureDataModal>? = mutableListOf()
//        newList?.toList()
//        for(i in emiSchemeOfferDataList!!.indices)
//        {
//            var item = emiSchemeOfferDataList!![i]
//            if(i == position)
//            {
//                item.isSelected = !item.isSelected
//                newList?.add(item)
//                //emiSchemeOfferDataList!![i].isSelected = !emiSchemeOfferDataList!![i].isSelected
//            }else{
//                item.isSelected = false
//                newList?.add(item)
//                //emiSchemeOfferDataList!![i].isSelected = false
//            }
//        }
//
//        logger("updateList",emiSchemeOfferDataList.toString())
//        logger("updateList2",newList?.toList().toString())
//
//        emiSchemeAndOfferAdapter.submitList(newList?.toList())

    }
    //endregion
    //region=========================SetUp RecyclerView Data:-
    private fun setUpRecyclerView() {

            binding?.emiSchemeOfferRV?.apply {
                layoutManager = LinearLayoutManager(context)
                itemAnimator = DefaultItemAnimator()
                adapter = emiSchemeAndOfferAdapter
            }

//        var tempList = emiSchemeOfferDataList?.toList()
//        emiSchemeAndOfferAdapter.submitList(tempList)


    }
    //endregion


}