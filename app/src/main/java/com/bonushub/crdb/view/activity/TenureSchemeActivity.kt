package com.bonushub.crdb.view.activity

import android.os.Bundle
import android.util.Log
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
import com.bonushub.crdb.utils.BhTransactionType
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.bonushub.crdb.model.remote.BankEMIIssuerTAndCDataModal
import com.bonushub.crdb.view.base.BaseActivityNew
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow


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
    private val testEmiOption by lazy {
        intent.getStringExtra("testEmiOption")
    }
    private val imeiOrSerialNum by lazy {
        intent.getStringExtra("imeiOrSerialNum")
    }

    private val mobileNumber by lazy {
        intent.getStringExtra("mobileNumber")
    }
 private val emiSchemeOfferDataListFromIntent by lazy {
        intent.getParcelableArrayListExtra<BankEMITenureDataModal>("emiSchemeOfferDataList") as MutableList<BankEMITenureDataModal>
    }
private val emiIssuerTAndCDataFromIntent by lazy {
        intent.getParcelableExtra("emiIssuerTAndCDataList") as BankEMIIssuerTAndCDataModal?
    }



    private var emiSchemeOfferDataList: MutableList<BankEMITenureDataModal> = mutableListOf()
     var emiIssuerTAndCData: BankEMIIssuerTAndCDataModal?=null
    private val emiSchemeAndOfferAdapter: EMISchemeAndOfferAdapter by lazy {
        EMISchemeAndOfferAdapter(
            transactionType,
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
        field57 = if(transactionType== BhTransactionType.BRAND_EMI.type) {
            "$bankEMIRequestCode^0^${brandID}^${productID}^${imeiOrSerialNum}" +
                    "^${/*cardBinValue.substring(0, 8)*/""}^${cardProcessedDataModal?.getTransactionAmount()}"
        }else{
            "$bankEMIRequestCode^0^1^0^^${/*cardProcessedDataModal?.getPanNumberData()?.substring(0, 8)*/""}^${cardProcessedDataModal?.getTransactionAmount()}"
        }

        if(transactionType== BhTransactionType.BRAND_EMI.type || transactionType== BhTransactionType.EMI_SALE.type) {
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
                            lifecycleScope.launch(Dispatchers.Main) {
                                alertBoxWithAction(
                                    getString(R.string.no_receipt),
                                    genericResp.errorMessage?:"Oops something went wrong",
                                    false,
                                    getString(R.string.positive_button_ok),
                                    {
                                        finish()
                                        startActivity(Intent(this@TenureSchemeActivity, NavigationActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        })
                                    },
                                    {})
                            }
                          //  ToastUtils.showToast(this, genericResp.errorMessage)
                            println(genericResp.errorMessage.toString())
                        }
                        is GenericResponse.Loading -> {
// currently not in use ....
                        }
                    }
                })

        }else if (transactionType== BhTransactionType.SALE.type ){
            emiSchemeOfferDataList=emiSchemeOfferDataListFromIntent
            emiIssuerTAndCData= emiIssuerTAndCDataFromIntent!!
            setUpRecyclerView()
        }else if(transactionType== BhTransactionType.TEST_EMI.type){
lifecycleScope.launch(Dispatchers.IO) {
    emiSchemeOfferDataList = calculateEmi()
    withContext(Dispatchers.Main) {
        setUpRecyclerView()
    }
}

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

                emiSchemeOfferDataList?.get(selectedSchemeUpdatedPosition)?.let { it1 ->
                    blockingImeiSerialNum(
                        it1,emiIssuerTAndCData
                    )
                }

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

private fun blockingImeiSerialNum(bankEmiTenureData:BankEMITenureDataModal,schemeData:BankEMIIssuerTAndCDataModal?){
    if(imeiOrSerialNum?.isNotBlank() == true){
        var isBlockUnblockSuccess=Pair(false,"")

     //   "Request Type^Skip Record Count^Brand Id^ProductID^Product serial^Bin Value^Transaction Amt^Issuer Id^Mobile No^EMI Scheme^Tenure"

val field57="12^0^${brandID}^${productID}^${imeiOrSerialNum}^${""}^${bankEmiTenureData.totalEmiPay}^${schemeData?.issuerID}^${mobileNumber}^${schemeData?.emiSchemeID}^${bankEmiTenureData.tenure}"
       // 12^0^11^3361^123rr^^256662^51^^141^3
        showProgress("Blocking Serial/IMEI")
        lifecycleScope.launch(Dispatchers.IO) {
           isBlockUnblockSuccess=  serverRepository.blockUnblockSerialNum(field57)
            hideProgress()
            if(isBlockUnblockSuccess.first){
                val returnIntent = Intent()
                returnIntent.putExtra("EMITenureDataModal", (emiSchemeOfferDataList.get(selectedSchemeUpdatedPosition)))
                returnIntent.putExtra("emiIssuerTAndCDataList", (emiIssuerTAndCData))
                returnIntent.putExtra("cardProcessedDataModal", cardProcessedDataModal)
                setResult(RESULT_OK, returnIntent)
                finish()

                /*
                for cancel case

                val returnIntent = Intent()
                setResult(RESULT_CANCELED, returnIntent)
                finish()*/

            }else {
                withContext(Dispatchers.Main) {
                    showToast(isBlockUnblockSuccess.second)
                }
            }

        }

    }else{
        val returnIntent = Intent()
        returnIntent.putExtra("EMITenureDataModal", (emiSchemeOfferDataList[selectedSchemeUpdatedPosition]))
        returnIntent.putExtra("emiIssuerTAndCDataList", (emiIssuerTAndCData))
        returnIntent.putExtra("cardProcessedDataModal", cardProcessedDataModal)
        setResult(RESULT_OK, returnIntent)
        finish()

        /*
        for cancel case

        val returnIntent = Intent()
        setResult(RESULT_CANCELED, returnIntent)
        finish()*/
    }

}

  private fun  calculateEmi(): MutableList<BankEMITenureDataModal> {
         var bankEMIDataModal: BankEMITenureDataModal? = null
        when(testEmiOption) {
            "3" -> {
                val monthlyEmi3 = cardProcessedDataModal?.getTransactionAmount()?.toFloat()
                    ?.let { emiCalculator(it, 11.0f, 3.0f) }

                val totalInterestPay3 = cardProcessedDataModal?.getTransactionAmount()?.toDouble()
                    ?.let { (monthlyEmi3?.times(3.0))?.minus(it) }


                bankEMIDataModal = BankEMITenureDataModal(
                    "3", "1100",
                    "", "", cardProcessedDataModal?.getTransactionAmount().toString(), "", "", "",
                    cardProcessedDataModal?.getTransactionAmount().toString(),
                    monthlyEmi3.toString(), "", "", "", "", totalInterestPay3.toString(), "", "",
                    "", "", "", ""
                )

                emiSchemeOfferDataList.add (bankEMIDataModal)
            }
            "6"->{
                val monthlyEmi6 = cardProcessedDataModal?.getTransactionAmount()?.toFloat()
                    ?.let { emiCalculator(it, 12.0f, 6.0f) }

                val totalInterestPay6 = cardProcessedDataModal?.getTransactionAmount()?.toDouble()
                    ?.let { (monthlyEmi6?.times(6.0))?.minus(it) }

                bankEMIDataModal = BankEMITenureDataModal(
                    "6", "1200",
                    "", "", cardProcessedDataModal?.getTransactionAmount().toString(), "", "", "",
                    cardProcessedDataModal?.getTransactionAmount().toString(),
                    monthlyEmi6.toString(), "", "", "", "", totalInterestPay6.toString(), "", "",
                    "", "", "", ""
                )

                emiSchemeOfferDataList.add(bankEMIDataModal)
            }
            "9"-> {
                val monthlyEmi9 = cardProcessedDataModal?.getTransactionAmount()?.toFloat()
                    ?.let { emiCalculator(it, 13.0f, 9.0f) }

                val totalInterestPay9 = cardProcessedDataModal?.getTransactionAmount()?.toDouble()
                    ?.let { (monthlyEmi9?.times(9.0))?.minus(it) }


                bankEMIDataModal = BankEMITenureDataModal(
                    "9", "1300",
                    "", "", cardProcessedDataModal?.getTransactionAmount().toString(), "", "", "",
                    cardProcessedDataModal?.getTransactionAmount().toString(),
                    monthlyEmi9.toString(), "", "", "", "", totalInterestPay9.toString(), "", "",
                    "", "", "", ""
                )

                emiSchemeOfferDataList.add(bankEMIDataModal )
            }
            "12"-> {
                val monthlyEmi12 = cardProcessedDataModal?.getTransactionAmount()?.toFloat()
                    ?.let { emiCalculator(it, 14.0f, 12.0f) }

                val totalInterestPay12 = cardProcessedDataModal?.getTransactionAmount()?.toDouble()
                    ?.let { (monthlyEmi12?.times(12.0))?.minus(it) }


                bankEMIDataModal = BankEMITenureDataModal(
                    "12", "1400",
                    "", "", cardProcessedDataModal?.getTransactionAmount().toString(), "", "", "",
                    cardProcessedDataModal?.getTransactionAmount().toString(),
                    monthlyEmi12.toString(), "", "", "", "", totalInterestPay12.toString(), "", "",
                    "", "", "", ""
                )

                emiSchemeOfferDataList.add(bankEMIDataModal )
            }
            else->{
                val monthlyEmi3 = cardProcessedDataModal?.getTransactionAmount()?.toFloat()
                    ?.let { emiCalculator(it, 11.0f, 3.0f) }

                val totalInterestPay3 = cardProcessedDataModal?.getTransactionAmount()?.toDouble()
                    ?.let { (monthlyEmi3?.times(3.0))?.minus(it) }


                bankEMIDataModal = BankEMITenureDataModal(
                    "3", "1100",
                    "", "", cardProcessedDataModal?.getTransactionAmount().toString(), "", "", "",
                    cardProcessedDataModal?.getTransactionAmount().toString(),
                    monthlyEmi3.toString(), "", "", "", "", totalInterestPay3.toString(), "", "",
                    "", "", "", ""
                )

                emiSchemeOfferDataList.add (bankEMIDataModal )
                val monthlyEmi6 = cardProcessedDataModal?.getTransactionAmount()?.toFloat()
                    ?.let { emiCalculator(it, 12.0f, 6.0f) }

                val totalInterestPay6 = cardProcessedDataModal?.getTransactionAmount()?.toDouble()
                    ?.let { (monthlyEmi6?.times(6.0))?.minus(it) }

                bankEMIDataModal = BankEMITenureDataModal(
                    "6", "1200",
                    "", "", cardProcessedDataModal?.getTransactionAmount().toString(), "", "", "",
                    cardProcessedDataModal?.getTransactionAmount().toString(),
                    monthlyEmi6.toString(), "", "", "", "", totalInterestPay6.toString(), "", "",
                    "", "", "", ""
                )

                emiSchemeOfferDataList.add(bankEMIDataModal )
                val monthlyEmi9 = cardProcessedDataModal?.getTransactionAmount()?.toFloat()
                    ?.let { emiCalculator(it, 13.0f, 9.0f) }

                val totalInterestPay9 = cardProcessedDataModal?.getTransactionAmount()?.toDouble()
                    ?.let { (monthlyEmi9?.times(9.0))?.minus(it) }


                bankEMIDataModal = BankEMITenureDataModal(
                    "9", "1300",
                    "", "", cardProcessedDataModal?.getTransactionAmount().toString(), "", "", "",
                    cardProcessedDataModal?.getTransactionAmount().toString(),
                    monthlyEmi9.toString(), "", "", "", "", totalInterestPay9.toString(), "", "",
                    "", "", "", ""
                )

                emiSchemeOfferDataList.add(bankEMIDataModal )
                val monthlyEmi12 = cardProcessedDataModal?.getTransactionAmount()?.toFloat()
                    ?.let { emiCalculator(it, 14.0f, 12.0f) }

                val totalInterestPay12 = cardProcessedDataModal?.getTransactionAmount()?.toDouble()
                    ?.let { (monthlyEmi12?.times(12.0))?.minus(it) }


                bankEMIDataModal = BankEMITenureDataModal(
                    "12", "1400",
                    "", "", cardProcessedDataModal?.getTransactionAmount().toString(), "", "", "",
                    cardProcessedDataModal?.getTransactionAmount().toString(),
                    monthlyEmi12.toString(), "", "", "", "", totalInterestPay12.toString(), "", "",
                    "", "", "", ""
                )

                emiSchemeOfferDataList.add(bankEMIDataModal )
                Log.d("emiScheme:- ", Gson().toJson(emiSchemeOfferDataList))

            }
        }
        return  emiSchemeOfferDataList
    }
    private fun emiCalculator(principalAmount: Float, rate: Float, time: Float): Double {
        var r = rate
        val t = time
        r /= (12 * 100) // one month interest

        val emi: Float = (principalAmount * r * (1 + r).toDouble().pow(t.toDouble()).toFloat()
                / ((1 + r).toDouble().pow(t.toDouble()) - 1).toFloat())

        return emi.toDouble()
    }

}