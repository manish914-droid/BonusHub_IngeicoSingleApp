package com.bonushub.crdb.india.view.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.*
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.ActivityEmvBinding
import com.bonushub.crdb.india.db.AppDao
import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.model.local.*
import com.bonushub.crdb.india.model.remote.*
import com.bonushub.crdb.india.transactionprocess.CreateTransactionPacketNew
import com.bonushub.crdb.india.transactionprocess.SyncTransactionToHost
import com.bonushub.crdb.india.type.EmvOption
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.view.base.BaseActivityNew
import com.bonushub.crdb.india.view.baseemv.SearchCard
import com.bonushub.crdb.india.viewmodel.*
import com.bonushub.crdb.india.view.baseemv.VFEmvHandler
import com.ingenico.hdfcpayment.request.*
import com.usdk.apiservice.aidl.pinpad.DeviceName
import com.usdk.apiservice.aidl.pinpad.KAPId
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class TransactionActivity : BaseActivityNew() {

    private var isToExit = false

    @Inject
    lateinit var appDao: AppDao

    private var emvBinding: ActivityEmvBinding? = null

    private var defaultScope = CoroutineScope(Dispatchers.Default)
    private var globalCardProcessedModel = CardProcessedDataModal()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
      //  emvBinding = ActivityEmvBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_emv)
        defaultScope.launch {
            // uemv = deviceService!!.getEMV()
            SearchCard(DeviceHelper.getEMV(), globalCardProcessedModel) { localCardProcessedData ->
                processAccordingToCardType(localCardProcessedData)

            }
        }

    }

    private fun processAccordingToCardType(cardProcessedData: CardProcessedDataModal) {
        when (cardProcessedData.getReadCardType()) {
            DetectCardType.MAG_CARD_TYPE-> {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(applicationContext,"Mag Card  detected", Toast.LENGTH_LONG).show()
                }

            }

            DetectCardType.EMV_CARD_TYPE-> {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(applicationContext,"EMV card contact detected", Toast.LENGTH_LONG).show()
                    val emvOption = EmvOption.create().apply {
                        flagPSE(0x00.toByte())
                    }
                    DeviceHelper.getEMV()?.startEMV(emvOption?.toBundle(), emvHandler())

                }

            }

            DetectCardType.CONTACT_LESS_CARD_TYPE -> {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(applicationContext,"Contactless detected", Toast.LENGTH_LONG).show()
                }
            }

            else -> {

            }
        }
    }

    //region========================================Below Method is a Handler for EMV CardType:-
    private fun emvHandler(): VFEmvHandler {
        println("DoEmv VfEmvHandler is calling")
        println("IEmv value is" + DeviceHelper.getEMV().toString())
        return VFEmvHandler(DeviceHelper.getPinpad(KAPId(0, 0), 0, DeviceName.IPP),DeviceHelper.getEMV(),this@TransactionActivity,globalCardProcessedModel) { cardProcessedData ->
          //  transactionCallback(cardProcessedData)
            emvProcessNext(cardProcessedData)
            Log.d("Track2Data:- ", cardProcessedData.getTrack2Data() ?: "")
            Log.d("PanNumber:- ", cardProcessedData.getPanNumberData() ?: "")
        }
    }
    //endregion

    // Creating transaction packet and
    private fun emvProcessNext(cardProcessedData: CardProcessedDataModal) {
        val transactionISO = CreateTransactionPacketNew(appDao,cardProcessedData,BatchTable()).createTransactionPacketNew()
        cardProcessedData.indicatorF58 = transactionISO.additionalData["indicatorF58"] ?: ""

        // logger("Transaction REQUEST PACKET --->>", transactionISO.isoMap, "e")
        //  runOnUiThread { showProgress(getString(R.string.sale_data_sync)) }
        GlobalScope.launch(Dispatchers.IO) {
            checkReversal(transactionISO, cardProcessedData)
        }
    }

    private fun checkReversal(transactionISOByteArray: IsoDataWriter, cardProcessedDataModal: CardProcessedDataModal) {
        runOnUiThread {
           // cardView_l.visibility = View.GONE
        }
        // If case Sale data sync to server
        if (true) {
            val msg: String = getString(R.string.sale_data_sync)
            runOnUiThread { showProgress(msg) }
            SyncTransactionToHost(transactionISOByteArray, cardProcessedDataModal) { syncStatus, responseCode, transactionMsg, printExtraData, de55, doubletap ->
                hideProgress()
                if (syncStatus) {
                    val responseIsoData: IsoDataReader = readIso(transactionMsg.toString(), false)
                    val autoSettlementCheck = responseIsoData.isoMap[60]?.parseRaw2String().toString()
                    if (syncStatus && responseCode == "00") {
                        //Below we are saving batch data and print the receipt of transaction:-

                              GlobalScope.launch(Dispatchers.Main) {
                                  Toast.makeText(this@TransactionActivity,"TXn approved",Toast.LENGTH_SHORT).show()
                              }


                    } else if (syncStatus && responseCode != "00") {
                        GlobalScope.launch(Dispatchers.Main) {
                            alertBoxWithAction(getString(R.string.transaction_delined_msg), responseIsoData.isoMap[58]?.parseRaw2String().toString(), false, getString(R.string.positive_button_ok), { alertPositiveCallback ->
                                    if (alertPositiveCallback) {

                                    }
                                },
                                {})
                        }
                    }
                    //Condition for having a reversal(EMV CASE)
                    else if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {

                    }
                }

            }
        }
        //Else case is to Sync Reversal data Packet to Host:-

    }



    override fun onBackPressed() {
        exitApp()

          }

    private fun exitApp() {
        if (isToExit) {
            super.finishAffinity()
        } else {
            isToExit = true
            Handler(Looper.getMainLooper()).postDelayed({
                isToExit = false
                Toast.makeText(this, "Double click back button to exit.", Toast.LENGTH_SHORT).show()

            }, 1000)
        }
    }
    //endregion

   }