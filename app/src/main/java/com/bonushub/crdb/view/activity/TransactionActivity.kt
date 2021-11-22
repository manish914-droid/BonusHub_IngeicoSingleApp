package com.bonushub.crdb.view.activity

import android.content.Intent
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.bonushub.crdb.HDFCApplication
import com.bonushub.crdb.R
import com.bonushub.crdb.db.AppDatabase
import com.bonushub.crdb.repository.ServerRepository
import com.bonushub.crdb.serverApi.RemoteService
import com.bonushub.crdb.utils.DeviceHelper
import com.bonushub.crdb.utils.ToastUtils
import com.bonushub.crdb.view.fragments.TenureBankModal
import com.bonushub.crdb.view.fragments.TenureSchemeActivity
import com.bonushub.crdb.viewmodel.SearchViewModel
import com.bonushub.pax.utils.EDashboardItem
import com.ingenico.hdfcpayment.listener.OnOperationListener
import com.ingenico.hdfcpayment.listener.OnPaymentListener
import com.ingenico.hdfcpayment.request.SaleRequest
import com.ingenico.hdfcpayment.request.TerminalInitializationRequest
import com.ingenico.hdfcpayment.response.OperationResult
import com.ingenico.hdfcpayment.response.PaymentResult
import com.ingenico.hdfcpayment.response.TerminalInitializationResponse
import com.ingenico.hdfcpayment.response.TransactionResponse
import com.ingenico.hdfcpayment.type.RequestStatus
import com.ingenico.hdfcpayment.type.ResponseCode
import com.ingenico.hdfcpayment.type.TransactionType


import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class TransactionActivity : AppCompatActivity(){

    private val transactionAmountValue by lazy { intent.getStringExtra("amt") ?: "0" }

    //used for other cash amount
    private val transactionOtherAmountValue by lazy { intent.getStringExtra("otherAmount") ?: "0" }

    private val testEmiOperationType by lazy { intent.getStringExtra("TestEmiOption") ?: "0" }

    //used in case of sale with cash
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
        setContentView(R.layout.activity_emv)
        setupFlow()
        searchCardViewModel.fetchCardTypeData()
        setupObserver()

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
            }
        })


    }

    private fun setupEMVObserver() {
       searchCardViewModel.cardTpeData.observe(this, Observer { cardProcessedDataModal ->
           if(cardProcessedDataModal.getPanNumberData() !=null) {
               cardProcessedDataModal.getPanNumberData()
/*
                try {
                    DeviceHelper.doTerminalInitialization(
                        request = TerminalInitializationRequest(
                            1,
                            "41501370".split(",")
                        ),
                        listener = object : OnOperationListener.Stub() {
                            override fun onCompleted(p0: OperationResult?) {
                                Log.d(TAG, "OnTerminalInitializationListener.onCompleted")
                                val response = p0?.value as? TerminalInitializationResponse
                                val initResult =
                                    """
                                   Response_Code = ${response?.responseCode}
                                   API_Response_Status = ${response?.status}
                                   Response_Code = ${response?.responseCode}
                                   TIDStatusList = [${response?.tidStatusList?.joinToString()}]
                                   TIDs = [${response?.tidList?.joinToString()}]
                                   INITDATAList = [${response?.initDataList?.firstOrNull().toString()}]
                                """.trimIndent()

                                when (response?.status) {
                                    RequestStatus.SUCCESS -> println(initResult)
                                    RequestStatus.ABORTED,
                                    RequestStatus.FAILED -> println(initResult)
                                    else -> println(initResult)
                                }
                            }
                        }
                    )
                }
                 catch (ex: Exception){
                     ex.printStackTrace()
                 }*/

               /* var ecrID: String
                try {
                    DeviceHelper.doSaleTransaction(
                        SaleRequest(
                            amount = 300L ?: 0,
                            tipAmount = 0L ?: 0,
                            transactionType = TransactionType.SALE,
                            tid = "30160031",
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

                DeviceHelper.showAdminFunction(object: OnOperationListener.Stub(){
                    override fun onCompleted(p0: OperationResult?) {
                        p0?.value?.apply {
                            println("Status = $status")
                            println("Response code = $responseCode")
                        }
                    }
                })

                Toast.makeText(
                    this,
                    cardProcessedDataModal.getPanNumberData().toString(),
                    Toast.LENGTH_LONG
                ).show()

                /* lifecycleScope.launch(Dispatchers.IO) {
                    // serverRepository.getEMITenureData(cardProcessedDataModal.getEncryptedPan().toString())
                     serverRepository.getEMITenureData("B1DFEFE944EE27E9B78136F34C3EB5EE2B891275D5942360")
                 }*/
                /* val intent = Intent (this, TenureSchemeActivity::class.java)

                 startActivity(intent)*/

            }

        })
    }

    private  fun setupFlow(){
        when(transactionTypeEDashboardItem){

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