package com.bonushub.crdb.view.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.bonushub.crdb.databinding.ActivityEmvBinding
import com.bonushub.crdb.model.local.BrandEMISubCategoryTable
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.model.remote.BrandEMIProductDataModal
import com.bonushub.crdb.utils.DeviceHelper
import com.bonushub.crdb.viewmodel.SearchViewModel
import com.bonushub.pax.utils.EDashboardItem
import com.google.gson.Gson
import com.ingenico.hdfcpayment.listener.OnPaymentListener
import com.ingenico.hdfcpayment.model.ReceiptDetail
import com.ingenico.hdfcpayment.request.SaleRequest
import com.ingenico.hdfcpayment.response.PaymentResult
import com.ingenico.hdfcpayment.response.TransactionResponse
import com.ingenico.hdfcpayment.type.ResponseCode
import com.ingenico.hdfcpayment.type.TransactionType


import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class TransactionActivity : AppCompatActivity(){
    private var emvBinding: ActivityEmvBinding? = null
    private val transactionAmountValue by lazy { intent.getStringExtra("amt") ?: "0" }

    //used for other cash amount
    private val transactionOtherAmountValue by lazy { intent.getStringExtra("otherAmount") ?: "0" }

    private val testEmiOperationType by lazy { intent.getStringExtra("TestEmiOption") ?: "0" }

  private val brandEmiSubCatData by lazy { intent.getSerializableExtra("brandEmiSubCatData") as BrandEMISubCategoryTable } //: BrandEMISubCategoryTable? = null
    private val brandEmiProductData by lazy { intent.getSerializableExtra("brandEmiProductData") as BrandEMIProductDataModal }
    private val brandDataMaster by lazy { intent.getSerializableExtra("brandDataMaster") as BrandEMIMasterDataModal }
    private val imeiOrSerialNum by lazy { intent.getStringExtra("imeiOrSerialNum") ?: "" }


    private val saleAmt by lazy { intent.getStringExtra("saleAmt") ?: "0" }
    private val mobileNumber by lazy { intent.getStringExtra("mobileNumber") ?: "" }

    private val billNumber by lazy { intent.getStringExtra("billNumber") ?: "0" }
    private val saleWithTipAmt by lazy { intent.getStringExtra("saleWithTipAmt") ?: "0" }
    private val title by lazy { intent.getStringExtra("title") }
    private val transactionType by lazy { intent.getIntExtra("type", -1947) }
    private val  transactionTypeEDashboardItem by lazy{ (intent.getSerializableExtra("edashboardItem") ?: EDashboardItem.NONE) as EDashboardItem}
    val TAG = TransactionActivity::class.java.simpleName

    private val searchCardViewModel : SearchViewModel by viewModels()

    //  private lateinit var deviceService: UsdkDeviceService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        emvBinding = ActivityEmvBinding.inflate(layoutInflater)
        setContentView(emvBinding?.root)

        setupFlow()
        //searchCardViewModel.fetchCardTypeData()


    }

    private fun setupObserver() {
        searchCardViewModel.allcadType.observe(this, Observer { cardProcessdatamodel  ->
            when(cardProcessdatamodel.getReadCardType()){
                DetectCardType.EMV_CARD_TYPE -> {
                    Toast.makeText(this,"EMV mode detected",Toast.LENGTH_LONG).show()
                    searchCardViewModel.fetchCardPanData()
                    setupEMVObserver()
                }
                DetectCardType.CONTACT_LESS_CARD_TYPE -> {
                    Toast.makeText(this,"Contactless mode detected",Toast.LENGTH_LONG).show()
                }
                DetectCardType.MAG_CARD_TYPE -> {
                    Toast.makeText(this,"Swipe mode detected",Toast.LENGTH_LONG).show()
                    searchCardViewModel.fetchCardPanData()
                    setupEMVObserver()
                }
                else -> {

                }
            }
        })
    }

    private fun setupEMVObserver() {
       searchCardViewModel.cardTpeData.observe(this, Observer { cardProcessedDataModal ->
           if(cardProcessedDataModal.getPanNumberData() !=null) {
               cardProcessedDataModal.getPanNumberData()
                var ecrID: String
             /*   try {
                    DeviceHelper.doSaleTransaction(
                        SaleRequest(
                            amount = 300L ?: 0,
                            tipAmount = 0L ?: 0,
                            transactionType = TransactionType.SALE,
                            tid = "30160035",
                            transactionUuid = UUID.randomUUID().toString().also {
                                ecrID = it

                            }
                        ),
                        listener = object : OnPaymentListener.Stub() {
                            override fun onCompleted(result: PaymentResult?) {
                                val txnResponse = result?.value as? TransactionResponse
                                val detailResponse = txnResponse?.receiptDetail
                                    .toString()
                                    .split(",")
                                Log.d(TAG, "Response Code: ${txnResponse?.responseCode}")
                                when (txnResponse?.responseCode) {
                                    ResponseCode.SUCCESS.value -> {
                                        detailResponse.forEach { println(it) }
                                        //  uids.add(ecrID)
                                        // defaultScope.launch { onSaveUId(ecrID, handleLoadingUIdsResult) }
                                    }
                                    ResponseCode.FAILED.value,
                                    ResponseCode.ABORTED.value -> {
                                        detailResponse.forEach { println(it) }
                                    }
                                    else -> println("Error")
                                }
                            }
                        }
                    )
                }
                catch (exc: Exception){
                    exc.printStackTrace()
                }*/

              /*  DeviceHelper.showAdminFunction(object: OnOperationListener.Stub(){
                    override fun onCompleted(p0: OperationResult?) {
                        p0?.value?.apply {
                            println("Status = $status")
                            println("Response code = $responseCode")
                        }
                    }
                })*/

                Toast.makeText(
                    this,
                    cardProcessedDataModal.getPanNumberData().toString(),
                    Toast.LENGTH_LONG
                ).show()


               /*  lifecycleScope.launch(Dispatchers.IO) {
                    // serverRepository.getEMITenureData(cardProcessedDataModal.getEncryptedPan().toString())
                     serverRepository.getEMITenureData("B1DFEFE944EE27E9B78136F34C3EB5EE2B891275D5942360")
                 }*/

            }

        })
    }

    private  fun setupFlow(){
        emvBinding?.baseAmtTv?.text=saleAmt

        when(transactionTypeEDashboardItem){

            EDashboardItem.BRAND_EMI->{
                //setupObserver()
                val intent = Intent (this, TenureSchemeActivity::class.java)
                startActivity(intent)
            }
            EDashboardItem.SALE->{
                val amt=(saleAmt.toFloat() * 100).toLong()
                var ecrID: String
                try {
                    DeviceHelper.doSaleTransaction(
                        SaleRequest(
                            amount = amt ?: 0,
                            tipAmount = 0L ?: 0,
                            transactionType = TransactionType.SALE,
                            tid = "30160035",
                            transactionUuid = UUID.randomUUID().toString().also {
                                ecrID = it

                            }
                        ),
                        listener = object : OnPaymentListener.Stub() {
                            override fun onCompleted(result: PaymentResult?) {
                                val txnResponse = result?.value as? TransactionResponse
                                val receiptDetail = txnResponse?.receiptDetail
                                   /* .toString()
                                    .split(",")*/
                                Log.d(TAG, "Response Code: ${txnResponse?.responseCode}")
                                when (txnResponse?.responseCode) {
                                    ResponseCode.SUCCESS.value -> {
                                        val jsonResp=Gson().toJson(receiptDetail)
                                        println(jsonResp)
                                     //   detailResponse.forEach { println(it) }
                                        //  uids.add(ecrID)
                                        // defaultScope.launch { onSaveUId(ecrID, handleLoadingUIdsResult) }
                                    }
                                    ResponseCode.FAILED.value,
                                    ResponseCode.ABORTED.value -> {
                                      //  detailResponse.forEach { println(it) }
                                    }
                                    else -> {
                                        val intent = Intent (this@TransactionActivity, NavigationActivity::class.java)
                                          startActivity(intent)

                                        println("Error")}
                                }
                            }
                        }
                    )
                }
                catch (exc: Exception){
                    exc.printStackTrace()
                }
            }
            else -> {

            }
        }
    }

    //Below Enum Class is used to detect different card Types:-
    enum class DetectCardType(val cardType: Int, val cardTypeName: String = "") {
        CARD_ERROR_TYPE(0),
        MAG_CARD_TYPE(1, "Mag"),
        EMV_CARD_TYPE(2, "Chip"),
        CONTACT_LESS_CARD_TYPE(3, "CTLS"),
        CONTACT_LESS_CARD_WITH_MAG_TYPE(4, "CTLS"),
        MANUAL_ENTRY_TYPE(5, "MAN")
    }

}