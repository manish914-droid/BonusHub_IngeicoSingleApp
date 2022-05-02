package com.bonushub.crdb.india.view.activity

import android.content.Intent
import android.os.*
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.*
import androidx.lifecycle.lifecycleScope
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.ActivityEmvBinding
import com.bonushub.crdb.india.db.AppDao
import com.bonushub.crdb.india.entity.CardOption
import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.model.local.*
import com.bonushub.crdb.india.model.remote.*
import com.bonushub.crdb.india.serverApi.bankEMIRequestCode
import com.bonushub.crdb.india.transactionprocess.*
import com.bonushub.crdb.india.type.EmvOption
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getMaskedPan
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getTptData
import com.bonushub.crdb.india.utils.ingenico.EMVInfoUtil
import com.bonushub.crdb.india.utils.ingenico.TLV
import com.bonushub.crdb.india.utils.printerUtils.PrintUtil
import com.bonushub.crdb.india.utils.printerUtils.checkForPrintReversalReceipt
import com.bonushub.crdb.india.view.base.BaseActivityNew
import com.bonushub.crdb.india.view.baseemv.SearchCard
import com.bonushub.crdb.india.viewmodel.*
import com.bonushub.crdb.india.view.baseemv.VFEmvHandler
import com.bonushub.crdb.india.vxutils.TransactionType
import com.bonushub.crdb.india.vxutils.Utility.byte2HexStr
import com.bonushub.crdb.india.vxutils.Utility.getCardHolderName
import com.google.gson.Gson
import com.ingenico.hdfcpayment.request.*
import com.usdk.apiservice.aidl.emv.CVMFlag
import com.usdk.apiservice.aidl.emv.EMVTag
import com.usdk.apiservice.aidl.pinpad.DeviceName
import com.usdk.apiservice.aidl.pinpad.KAPId
import com.usdk.apiservice.aidl.pinpad.OnPinEntryListener
import com.usdk.apiservice.aidl.pinpad.PinpadData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject
import kotlin.jvm.Throws

@AndroidEntryPoint
class TransactionActivity : BaseActivityNew() {

    companion object {
        val TAG = TransactionActivity::class.java.simpleName
    }

    private var isToExit = false

    @Inject
    lateinit var appDao: AppDao

    private var emvBinding: ActivityEmvBinding? = null

    private val transactionProcessingCode by lazy {
        intent.getStringExtra("proc_code") ?: "920001"
    } //Just for checking purpose

    private var defaultScope = CoroutineScope(Dispatchers.Default)
    private var globalCardProcessedModel = CardProcessedDataModal()

    private val saleAmt by lazy { intent.getStringExtra("saleAmt") ?: "0" }
    private val transactionType by lazy { intent.getIntExtra("type", -1947) }
    private val mobileNumber by lazy { intent.getStringExtra("mobileNumber") ?: "" }
    private val billNumber by lazy { intent.getStringExtra("billNumber") ?: "0" }
    private val saleWithTipAmt by lazy { intent.getStringExtra("saleWithTipAmt") ?: "0" }
    private val transactionTypeEDashboardItem by lazy {
        (intent.getSerializableExtra("edashboardItem") ?: EDashboardItem.NONE) as EDashboardItem
    }
    private val cashBackAmt by lazy { intent.getStringExtra("cashBackAmt") ?: "0" }

    private  var vfEmvHandlerCallback1: (CardProcessedDataModal) -> Unit = ::onDeviceControllerAction

    private fun onDeviceControllerAction(cardProcessedDataModal: CardProcessedDataModal) {
        println("Emvhandler called")
        processAccordingToCardType(cardProcessedDataModal)
    }


    lateinit var testVFEmvHandler:VFEmvHandler

    private val brandDataMaster by lazy { (intent.getSerializableExtra("brandDataMaster") ?: null) as BrandEMIMasterDataModal}
    private val brandEmiProductData by lazy { (intent.getSerializableExtra("brandEmiProductData") ?: null) as BrandEMIProductDataModal}
    private val imeiOrSerialNum by lazy { intent.getStringExtra("imeiOrSerialNum") ?: "" }

    private var emiSelectedData: BankEMITenureDataModal? = null // BankEMIDataModal
    private var emiTAndCData: BankEMIIssuerTAndCDataModal? = null

    private val brandEMIData by lazy {
        intent.getSerializableExtra("brandEMIData") as BrandEMIDataModal?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        emvBinding = ActivityEmvBinding.inflate(layoutInflater)
        //setContentView(R.layout.activity_emv)
        setContentView(emvBinding?.root)
        emvBinding?.subHeaderView?.subHeaderText?.text = transactionTypeEDashboardItem.title
        emvBinding?.subHeaderView?.headerImage?.setImageResource(transactionTypeEDashboardItem.res) //= transactionTypeEDashboardItem.title
        emvBinding?.txnAmtLl?.visibility = View.VISIBLE

        globalCardProcessedModel.setTransType(transactionType)
        globalCardProcessedModel.setTransactionAmount((saleAmt.toDouble() * 100).toLong())
        globalCardProcessedModel.setProcessingCode(transactionProcessingCode)

        // now below converted to when
       /* if (transactionType == BhTransactionType.SALE_WITH_CASH.type) {
            val amt = saleAmt.toFloat() + cashBackAmt.toFloat()
            val frtAmt = "%.2f".format(amt)
            emvBinding?.baseAmtTv?.text = frtAmt
            emvBinding?.tvInsertCard?.text = "Please Insert/Swipe Card"
        } else {
            val frtAmt = "%.2f".format(saleAmt.toFloat())
            emvBinding?.baseAmtTv?.text = frtAmt
            emvBinding?.tvInsertCard?.text = "Please Insert/Swipe Card"

        }*/
        when(transactionType){

            BhTransactionType.SALE_WITH_CASH.type ->{
                val amt = saleAmt.toFloat() + cashBackAmt.toFloat()
                val frtAmt = "%.2f".format(amt)
                emvBinding?.baseAmtTv?.text = getString(R.string.rupees_symbol)+frtAmt
                emvBinding?.tvInsertCard?.text = "Please Insert/Swipe Card"
            }

            BhTransactionType.BRAND_EMI.type, BhTransactionType.EMI_SALE.type ->{
                val amt = saleAmt.toFloat() + cashBackAmt.toFloat()
                val frtAmt = "%.2f".format(amt)
                emvBinding?.baseAmtTv?.text = getString(R.string.rupees_symbol)+frtAmt
                emvBinding?.tvInsertCard?.text = "Please Insert/Swipe Card"
                globalCardProcessedModel.setEmiTransactionAmount((saleAmt.toDouble() * 100).toLong())
            }
            else ->{
                val frtAmt = "%.2f".format(saleAmt.toFloat())
                emvBinding?.baseAmtTv?.text = getString(R.string.rupees_symbol)+frtAmt
                emvBinding?.tvInsertCard?.text = "Please Insert/Swipe Card"

            }
        }


        val cardOption = CardOption.create().apply {
            supportICCard(true)
            supportMagCard(true)
            supportRFCard(true)
        }

        detectCard(globalCardProcessedModel,cardOption)

        emvBinding?.subHeaderView?.backImageButton?.setOnClickListener {
            declinedTransaction()
        }
    }


    private fun processAccordingToCardType(cardProcessedDataModal: CardProcessedDataModal) {
        when (cardProcessedDataModal.getReadCardType()) {
            DetectCardType.MAG_CARD_TYPE-> {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(applicationContext,"Mag Card  detected", Toast.LENGTH_LONG).show()
                }

                if (cardProcessedDataModal.getFallbackType() != EFallbackCode.Swipe_fallback.fallBackCode) {

                    val currDate = getCurrentDateforMag()
                    if (/*currDate.compareTo(cardProcessedDataModal.getExPiryDate()!!) <= 0*/true) {
                        println("Correct Date")
                        Log.d(TAG, "onCardSwiped ...1")
                        //  val bytes: ByteArray = ROCProviderV2.hexStr2Byte(track2)
                        // Log.d(TAG, "Track2:" + track2 + " (" + ROCProviderV2.byte2HexStr(bytes) + ")")

                        getCardHolderName(
                            cardProcessedDataModal,
                            cardProcessedDataModal.getTrack1Data(),
                            '^',
                            '^'
                        )
                        //Stubbing Card Processed Data:-
                        cardProcessedDataModal.setReadCardType(DetectCardType.MAG_CARD_TYPE)

                        cardProcessedDataModal.getTrack2Data()

                        //region----------- sir please check
                        val track2 = cardProcessedDataModal.getTrack2Data()

                        /*var track21 = "35,36|${
                            track2?.replace("D", "=")?.replace("F", "")
                        }" + "|" + cardProcessedDataModal?.getCardHolderName() + "~~~" +
                                cardProcessedDataModal?.getTypeOfTxnFlag() + "~" + cardProcessedDataModal?.getPinEntryFlag()*/

                        var field57 =   "35|"+track2?.replace("D", "=")?.replace("F", "")
                        println("Field 57 data is"+field57)
                        val encrptedPan = getEncryptedPanorTrackData(field57,true)
                        cardProcessedDataModal.setEncryptedPan(encrptedPan)
                        // end region--------

                        cardProcessedDataModal.getTrack1Data()
                        cardProcessedDataModal.getTrack3Data()
                        cardProcessedDataModal.getPanNumberData()

                        if (null != cardProcessedDataModal.getPanNumberData()) {
                            cardProcessedDataModal.getPanNumberData()?.let {
                                logger(
                                    "SWIPE_PAN",
                                    it, "e"
                                )
                            }

                            //  cardProcessedDataModal.setPanNumberData("6789878786")
                            if (!cardProcessedDataModal.getPanNumberData()
                                    ?.let { cardLuhnCheck(it) }!!
                            ) {
                                onEndProcessCalled(
                                    DetectError.IncorrectPAN.errorCode,
                                    cardProcessedDataModal /*"Invalid Card Number"*/
                                )
                            } else {

                                val sc = cardProcessedDataModal.getServiceCodeData()
                                // val sc: String? = ""
                                var scFirstByte: Char? = null
                                var scLastbyte: Char? = null
                                if (null != sc) {
                                    scFirstByte = sc.first()
                                    scLastbyte = sc.last()

                                }

                                //Checking the card has a PIN or WITHOUTPIN
                                // Here the changes are , Now we have to ask pin for all swipe txns ...
                                val isPin = true
                                   /* scLastbyte == '0' || scLastbyte == '3' || scLastbyte == '5' || scLastbyte == '6' || scLastbyte == '7'*/ //true //
                                //Here we are bypassing the pin condition for test case ANSI_MAG_001.
                                //  isPin = false
                                if (isPin) {
                                    cardProcessedDataModal.setIsOnline(1)
                                    cardProcessedDataModal.setPinEntryFlag("1")
                                } else {
                                    //0 for no pin
                                    cardProcessedDataModal.setIsOnline(0)
                                    cardProcessedDataModal.setPinEntryFlag("0")
                                }
                                if (cardProcessedDataModal.getFallbackType() != EFallbackCode.EMV_fallback.fallBackCode) {
                                    //Checking Fallback
                                    if (scFirstByte == '2' || scFirstByte == '6') {
                                        onEndProcessCalled(EFallbackCode.Swipe_fallback.fallBackCode, cardProcessedDataModal)
                                    } else {
                                        //region================Condition Check and ProcessSwipeCardWithPinOrWithoutPin:-
                                        when (cardProcessedDataModal.getTransType()) {
                                            TransactionType.SALE.type -> processSwipeCardWithPINorWithoutPIN(isPin, cardProcessedDataModal)
                                            TransactionType.EMI_SALE.type -> {
                                                val intent = Intent(this@TransactionActivity, TenureSchemeActivity::class.java).apply {
                                                    putExtra("cardProcessedData", globalCardProcessedModel)
                                                    putExtra("transactionType", globalCardProcessedModel.getTransType())
                                                    putExtra("mobileNumber", mobileNumber)
                                                }
                                                startActivityForResult(intent, BhTransactionType.EMI_SALE.type)
                                            }
                                            TransactionType.BRAND_EMI.type -> {
                                                val intent = Intent(this@TransactionActivity, TenureSchemeActivity::class.java).apply {
                                                    putExtra("cardProcessedData", globalCardProcessedModel)
                                                    putExtra("transactionType", globalCardProcessedModel.getTransType())
                                                    putExtra("mobileNumber", mobileNumber)
                                                    putExtra("brandID", brandEMIData?.brandID?:"")
                                                    putExtra("productID", brandEMIData?.productID?:"")
                                                    putExtra("imeiOrSerialNum", brandEMIData?.imeiORserailNum?:"")
                                                }
                                                startActivityForResult(intent, BhTransactionType.EMI_SALE.type)
                                            }
                                            else -> processSwipeCardWithPINorWithoutPIN(
                                                isPin, cardProcessedDataModal
                                            )
                                            //endregion
                                        }

                                    }
                                }
                                else {
                                    when (cardProcessedDataModal.getTransType()) {
                                        TransactionType.SALE.type -> processSwipeCardWithPINorWithoutPIN(isPin, cardProcessedDataModal)
                                        TransactionType.EMI_SALE.type -> {
                                            val intent = Intent(this@TransactionActivity, TenureSchemeActivity::class.java).apply {
                                                putExtra("cardProcessedData", globalCardProcessedModel)
                                                putExtra("transactionType", globalCardProcessedModel.getTransType())
                                                putExtra("mobileNumber", mobileNumber)
                                            }
                                            startActivityForResult(intent, BhTransactionType.EMI_SALE.type)
                                        }
                                        TransactionType.BRAND_EMI.type -> {
                                            val intent = Intent(this@TransactionActivity, TenureSchemeActivity::class.java).apply {
                                                putExtra("cardProcessedData", globalCardProcessedModel)
                                                putExtra("transactionType", globalCardProcessedModel.getTransType())
                                                putExtra("mobileNumber", mobileNumber)
                                                putExtra("brandID", brandEMIData?.brandID?:"")
                                                putExtra("productID", brandEMIData?.productID?:"")
                                                putExtra("imeiOrSerialNum", brandEMIData?.imeiORserailNum?:"")
                                            }
                                            startActivityForResult(intent, BhTransactionType.EMI_SALE.type)
                                        }
                                        else -> processSwipeCardWithPINorWithoutPIN(
                                            isPin, cardProcessedDataModal
                                        )
                                        //endregion
                                    }
                                }
                            }
                        }
                        else{
                            onEndProcessCalled(
                                cardProcessedDataModal.getFallbackType(),
                                cardProcessedDataModal
                            )
                        }
                        //endregion
                    } else {
                        handleEMVFallbackFromError("card read error", "reinitiate txn", false) { alertCBBool ->
                            if (alertCBBool)
                                try {
                                    declinedTransaction()
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                }
                        }

                    }
                }


            }

            DetectCardType.EMV_CARD_TYPE-> {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(applicationContext,"EMV card contact detected", Toast.LENGTH_LONG).show()
                    val emvOption = EmvOption.create().apply {
                        flagPSE(0x00.toByte())
                    }
                    testVFEmvHandler = emvHandler()
                    logger("2testVFEmvHandler",""+testVFEmvHandler,"e")
                    DeviceHelper.getEMV()?.startEMV(emvOption?.toBundle(), testVFEmvHandler)

                }

            }

            DetectCardType.CONTACT_LESS_CARD_TYPE -> {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(applicationContext,"Contactless detected", Toast.LENGTH_LONG).show()
                    val emvOption = EmvOption.create().apply {
                        flagPSE(0x01.toByte())
                    }

                    testVFEmvHandler = emvHandler()
                    DeviceHelper.getEMV()?.startEMV(emvOption?.toBundle(), testVFEmvHandler)
                }
            }

            else -> {

            }
        }
    }

    // Process Swipe card with or without PIN .
    fun processSwipeCardWithPINorWithoutPIN(ispin: Boolean, cardProcessedDataModal: CardProcessedDataModal) {
        if (ispin) {
            val panBlock: String? = cardProcessedDataModal.getPanNumberData()
            val param = Bundle()
            param.putByteArray(PinpadData.PIN_LIMIT, byteArrayOf(0, 4, 5, 6, 7, 8, 9, 10, 11, 12))

            val listener: OnPinEntryListener = object : OnPinEntryListener.Stub() {
                override fun onInput(arg0: Int, arg1: Int) {}
                override fun onConfirm(data: ByteArray, arg1: Boolean) {
                    System.out.println("PinBlock is"+ byte2HexStr(data))
                    Log.d("PinBlock", "PinPad hex encrypted data ---> " + hexString2String(BytesUtil.bytes2HexString(data)))

                    respondCVMResult(1.toByte())

                    when (cardProcessedDataModal.getReadCardType()) {
                        DetectCardType.EMV_CARD_TYPE -> {
                            if (cardProcessedDataModal.getIsOnline() == 1) {
                                cardProcessedDataModal.setGeneratePinBlock(BytesUtil.bytes2HexString(data))
                                //insert with pin
                                cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_PIN.posEntry.toString())
                            } else {
                                cardProcessedDataModal.setGeneratePinBlock("")
                                //off line pin
                                cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_OFFLINE_PIN.posEntry.toString())
                            }
                        }
                        DetectCardType.CONTACT_LESS_CARD_TYPE -> {
                            if (cardProcessedDataModal.getIsOnline() == 1) {
                                cardProcessedDataModal.setGeneratePinBlock(BytesUtil.bytes2HexString(data))
                                cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_EMV_POS_WITH_PIN.posEntry.toString())
                            } else {
                                cardProcessedDataModal.setGeneratePinBlock("")
                                //  cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_PIN.posEntry.toString())
                            }
                        }
                        DetectCardType.MAG_CARD_TYPE -> {
                            //   vfIEMV?.importPin(1, data) // in Magnetic pin will not import
                            // cardProcessedDataModal.setGeneratePinBlock(BytesUtil.bytes2HexString(data))

                            if (cardProcessedDataModal.getFallbackType() == com.bonushub.crdb.india.utils.EFallbackCode.EMV_fallback.fallBackCode)
                                cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_FALL_MAGPIN.posEntry.toString())
                            else
                                cardProcessedDataModal.setPosEntryMode(PosEntryModeType.POS_ENTRY_SWIPED_NO4DBC_PIN.posEntry.toString())
                            cardProcessedDataModal.setApplicationPanSequenceValue("00")
                        }

                        else -> {
                        }
                    }


                    emvProcessNext(cardProcessedDataModal)
                }

                override fun onCancel() {
                    respondCVMResult(0.toByte())
                }

                override fun onError(error: Int) {
                    respondCVMResult(2.toByte())
                }
            }
            println("=> onCardHolderVerify | onlinpin")
            param.putByteArray(PinpadData.PAN_BLOCK, panBlock?.str2ByteArr())
            DeviceHelper.getPinpad(KAPId(0, 0), 0, DeviceName.IPP)?.startPinEntry(DemoConfig.KEYID_PIN, param, listener)

        }else{
            if (cardProcessedDataModal.getFallbackType() == com.bonushub.crdb.india.utils.EFallbackCode.EMV_fallback.fallBackCode)
                cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_FALL_MAGPIN.posEntry.toString())
            else
                cardProcessedDataModal.setPosEntryMode(PosEntryModeType.POS_ENTRY_SWIPED_NO4DBC_PIN.posEntry.toString())
            cardProcessedDataModal.setApplicationPanSequenceValue("00")
            emvProcessNext(cardProcessedDataModal)
        }
    }

    //region========================================Below Method is a Handler for EMV CardType:-
    private fun emvHandler(): VFEmvHandler {
        println("DoEmv VfEmvHandler is calling")
        println("IEmv value is" + DeviceHelper.getEMV().toString())

        return  VFEmvHandler(DeviceHelper.getPinpad(KAPId(0, 0), 0, DeviceName.IPP),
            DeviceHelper.getEMV(),this@TransactionActivity,globalCardProcessedModel,vfEmvHandlerCallback1).also { ei ->
            ei.onEndProcessCallback = ::onEndProcessCalled
            ei.vfEmvHandlerCallback = ::onEmvprocessnext


        }

    }

    private fun onEmvprocessnext(cardProcessedDataModal: CardProcessedDataModal) {
        println("processflow called")
        if(transactionType == BhTransactionType.BRAND_EMI.type)
        {
            val intent = Intent(this@TransactionActivity, TenureSchemeActivity::class.java).apply {
                val field57 =
                    "$bankEMIRequestCode^0^${brandDataMaster.brandID}^${brandEmiProductData.productID}^${imeiOrSerialNum}" +
                            "^${/*cardBinValue.substring(0, 8)*/""}^${globalCardProcessedModel.getTransactionAmount()}"
                putExtra("cardProcessedData", globalCardProcessedModel)
                putExtra("brandID", brandDataMaster.brandID)
                putExtra("productID", brandEmiProductData.productID)
                putExtra("imeiOrSerialNum", imeiOrSerialNum)
                putExtra(
                    "transactionType",
                    globalCardProcessedModel.getTransType()
                )
                putExtra("mobileNumber", mobileNumber)
            }
            startActivityForResult(intent, BhTransactionType.BRAND_EMI.type)
        } else if(transactionType == BhTransactionType.EMI_SALE.type){
            val intent = Intent(this@TransactionActivity, TenureSchemeActivity::class.java).apply {
                putExtra("cardProcessedData", globalCardProcessedModel)
                putExtra("transactionType", globalCardProcessedModel.getTransType())
                putExtra("mobileNumber", mobileNumber)
            }
            startActivityForResult(intent, BhTransactionType.EMI_SALE.type)
        }
        else if(transactionType == BhTransactionType.SALE.type)
        {
            emvProcessNext(cardProcessedDataModal)

        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){

            /*BhTransactionType.BRAND_EMI.type ->{
                val cardProcessedData = data?.getSerializableExtra("cardProcessedDataModal") as CardProcessedDataModal
                val maskedPan = cardProcessedData.getPanNumberData()?.let {
                    getMaskedPan(getTptData(), it)
                }
               // emiSelectedData = data.getParcelableExtra("emiSchemeDataList")
                emiSelectedData = data.getParcelableExtra("EMITenureDataModal")
                emiTAndCData = data.getParcelableExtra("emiIssuerTAndCDataList")
                Log.d("SelectedEMI Data:- ", emiSelectedData.toString())
                Log.d("emiTAndCData Data:- ", emiTAndCData.toString())

                runOnUiThread {
                    emvBinding?.atCardNoTv?.text = maskedPan
                    emvBinding?.cardViewL?.visibility = View.VISIBLE
                    // tv_card_number_heading.visibility = View.VISIBLE
                    Log.e("CHANGED ", "NEW LAUNCH")
                    //    binding?.paymentGif?.loadUrl("file:///android_asset/cardprocess.html")
                    //todo Processing dialog in case of EMI
                    emvBinding?.manualEntryButton?.visibility = View.GONE
                    emvBinding?.tvInsertCard?.visibility = View.GONE
                    if (cardProcessedData.getTransType() == TransactionType.TEST_EMI.type) {
                       *//* val baseAmountValue = getString(R.string.rupees_symbol) + "1.00"
                        binding?.baseAmtTv?.text = baseAmountValue*//*
                    } else {
                        val baseAmountValue = getString(R.string.rupees_symbol) + "%.2f".format((((emiSelectedData?.transactionAmount)?.toDouble())?.div(100)).toString().toDouble())
                        emvBinding?.baseAmtTv?.text = baseAmountValue
                    }
                }


                //region===============Check Transaction Type and Perform Action Accordingly:-
                if (cardProcessedData.getReadCardType() == DetectCardType.MAG_CARD_TYPE && cardProcessedData.getTransType() != TransactionType.TEST_EMI.type) {
                    val isPin = cardProcessedData.getIsOnline() == 1
                    cardProcessedData.setProcessingCode(transactionProcessingCode)
                    processSwipeCardWithPINorWithoutPIN(isPin, cardProcessedData)
                } else {
                    if (cardProcessedData.getTransType() == TransactionType.TEST_EMI.type) {
                        //VFService.showToast("Connect to BH_HOST1...")
                        Log.e("WWW", "-----")
                       *//* cardProcessedData.setTransactionAmount(100)

                        DoEmv(issuerUpdateHandler, this, pinHandler, cardProcessedData, ConstIPBOC.startEMV.intent.VALUE_cardType_smart_card) { cardProcessedDataModal ->
                            cardProcessedDataModal.setProcessingCode(transactionProcessingCode)
                            cardProcessedDataModal.setTransactionAmount(100)
                            cardProcessedDataModal.setOtherAmount(otherTransAmount)
                            cardProcessedDataModal.setMobileBillExtraData(
                                Pair(mobileNumber, billNumber))
                            //    localCardProcessedData.setTransType(transactionType)
                            globalCardProcessedModel = cardProcessedDataModal
                            Log.d("CardProcessedData:- ", Gson().toJson(cardProcessedDataModal))
                            val maskedPan = cardProcessedDataModal.getPanNumberData()?.let {
                                getMaskedPan(TerminalParameterTable.selectFromSchemeTable(), it)
                            }
                            runOnUiThread {
                                binding?.atCardNoTv?.text = maskedPan
                                cardView_l.visibility = View.VISIBLE
                                //    tv_card_number_heading.visibility = View.VISIBLE
                                tv_insert_card.visibility = View.INVISIBLE
                                //  binding?.paymentGif?.visibility = View.INVISIBLE
                            }
                            //Below Different Type of Transaction check Based ISO Packet Generation happening:-
                            processAccordingToCardType(cardProcessedDataModal)
                        }*//*
                    } else {
                        // sir please check emv call
                        DoEmv(testVFEmvHandler, this, cardProcessedData) { cardProcessedDataModal ->
                            Log.d("CardEMIData:- ", cardProcessedDataModal.toString())
                            cardProcessedDataModal.setProcessingCode(transactionProcessingCode)
                        //    cardProcessedDataModal.setTransactionAmount(emiSelectedTransactionAmount ?: 0L)
                       //     cardProcessedDataModal.setOtherAmount(otherTransAmount)
                            cardProcessedDataModal.setMobileBillExtraData(Pair(mobileNumber, billNumber))
                            globalCardProcessedModel = cardProcessedDataModal
                            Log.d("CardProcessedData:- ", Gson().toJson(cardProcessedDataModal))
                            val maskedPan = cardProcessedDataModal.getPanNumberData()?.let {
                               // getMaskedPan(TerminalParameterTable.selectFromSchemeTable(), it)
                            }
                            runOnUiThread {
                              *//*  binding?.atCardNoTv?.text = maskedPan
                                cardView_l.visibility = View.VISIBLE
                                //   tv_card_number_heading.visibility = View.VISIBLE
                                tv_insert_card.visibility = View.INVISIBLE
                                //  binding?.paymentGif?.visibility = View.INVISIBLE*//*
                            }
                            //Below Different Type of Transaction check Based ISO Packet Generation happening:-
                            emvProcessNext(cardProcessedDataModal)

                           // processAccordingToCardType(cardProcessedDataModal)
                        }
                    }
                }


            }*/

            BhTransactionType.EMI_SALE.type, BhTransactionType.BRAND_EMI.type ->{
                val cardProcessedData = data?.getSerializableExtra("cardProcessedDataModal") as CardProcessedDataModal
                val maskedPan = cardProcessedData.getPanNumberData()?.let {
                    getMaskedPan(getTptData(), it)
                }
               // emiSelectedData = data.getParcelableExtra("emiSchemeDataList")
                emiSelectedData = data.getParcelableExtra("EMITenureDataModal")
                emiTAndCData = data.getParcelableExtra("emiIssuerTAndCDataList")
                Log.d("SelectedEMI Data:- ", emiSelectedData.toString())
                Log.d("emiTAndCData Data:- ", emiTAndCData.toString())

                runOnUiThread {
                    emvBinding?.atCardNoTv?.text = maskedPan
                    emvBinding?.cardViewL?.visibility = View.VISIBLE
                    // tv_card_number_heading.visibility = View.VISIBLE
                    Log.e("CHANGED ", "NEW LAUNCH")
                    //    binding?.paymentGif?.loadUrl("file:///android_asset/cardprocess.html")
                    //todo Processing dialog in case of EMI
                    emvBinding?.manualEntryButton?.visibility = View.GONE
                    emvBinding?.tvInsertCard?.visibility = View.GONE
                    if (cardProcessedData.getTransType() == TransactionType.TEST_EMI.type) {
                       /* val baseAmountValue = getString(R.string.rupees_symbol) + "1.00"
                        binding?.baseAmtTv?.text = baseAmountValue*/
                    } else {
                        val baseAmountValue = getString(R.string.rupees_symbol) + "%.2f".format((((emiSelectedData?.transactionAmount)?.toDouble())?.div(100)).toString().toDouble())
                        emvBinding?.baseAmtTv?.text = baseAmountValue
                    }
                }


                //region===============Check Transaction Type and Perform Action Accordingly:-
                if (cardProcessedData.getReadCardType() == DetectCardType.MAG_CARD_TYPE && cardProcessedData.getTransType() != TransactionType.TEST_EMI.type) {
                    val isPin = cardProcessedData.getIsOnline() == 1
                    cardProcessedData.setProcessingCode(transactionProcessingCode)
                    processSwipeCardWithPINorWithoutPIN(isPin, cardProcessedData)
                } else {
                    if (cardProcessedData.getTransType() == TransactionType.TEST_EMI.type) {
                        //VFService.showToast("Connect to BH_HOST1...")
                        Log.e("WWW", "-----")

                       /* cardProcessedData.setTransactionAmount(100)

                        DoEmv(issuerUpdateHandler, this, pinHandler, cardProcessedData, ConstIPBOC.startEMV.intent.VALUE_cardType_smart_card) { cardProcessedDataModal ->
                            cardProcessedDataModal.setProcessingCode(transactionProcessingCode)
                            cardProcessedDataModal.setTransactionAmount(100)
                            cardProcessedDataModal.setOtherAmount(otherTransAmount)
                            cardProcessedDataModal.setMobileBillExtraData(
                                Pair(mobileNumber, billNumber))
                            //    localCardProcessedData.setTransType(transactionType)
                            globalCardProcessedModel = cardProcessedDataModal
                            Log.d("CardProcessedData:- ", Gson().toJson(cardProcessedDataModal))
                            val maskedPan = cardProcessedDataModal.getPanNumberData()?.let {
                                getMaskedPan(TerminalParameterTable.selectFromSchemeTable(), it)
                            }
                            runOnUiThread {
                                binding?.atCardNoTv?.text = maskedPan
                                cardView_l.visibility = View.VISIBLE
                                //    tv_card_number_heading.visibility = View.VISIBLE
                                tv_insert_card.visibility = View.INVISIBLE
                                //  binding?.paymentGif?.visibility = View.INVISIBLE
                            }
                            //Below Different Type of Transaction check Based ISO Packet Generation happening:-
                            processAccordingToCardType(cardProcessedDataModal)
                        }*/
                    } else {

                        val emvOption = EmvOption.create().apply {
                            flagPSE(0x00.toByte())
                        }
                        testVFEmvHandler = emvHandler()
                        logger("3testVFEmvHandler",""+testVFEmvHandler,"e")


                        // sir please check emv call
                        DoEmv(testVFEmvHandler,this, cardProcessedData) { cardProcessedDataModal, vfEmvHandler ->
                            Log.d("CardEMIData:- ", cardProcessedDataModal.toString())
                            testVFEmvHandler = vfEmvHandler
                            cardProcessedDataModal.setProcessingCode(transactionProcessingCode)
                            //    cardProcessedDataModal.setTransactionAmount(emiSelectedTransactionAmount ?: 0L)
                            //     cardProcessedDataModal.setOtherAmount(otherTransAmount)
                            cardProcessedDataModal.setMobileBillExtraData(Pair(mobileNumber, billNumber))
                            globalCardProcessedModel = cardProcessedDataModal
                            Log.d("CardProcessedData:- ", Gson().toJson(cardProcessedDataModal))
                            val maskedPan = cardProcessedDataModal.getPanNumberData()?.let {
                                // getMaskedPan(TerminalParameterTable.selectFromSchemeTable(), it)
                            }
                            runOnUiThread {
                                /*  binding?.atCardNoTv?.text = maskedPan
                                  cardView_l.visibility = View.VISIBLE
                                  //   tv_card_number_heading.visibility = View.VISIBLE
                                  tv_insert_card.visibility = View.INVISIBLE
                                  //  binding?.paymentGif?.visibility = View.INVISIBLE*/
                            }
                            //Below Different Type of Transaction check Based ISO Packet Generation happening:-
                            emvProcessNext(cardProcessedDataModal)

                            // processAccordingToCardType(cardProcessedDataModal)
                        }
                    }
                }


            }

            else ->{

            }
        }
    }

    private fun onEndProcessCalled(result: Int, cardProcessedDataModal: CardProcessedDataModal) {
        println("Fallback called")

              when(result){

                  //Swipe fallabck case when chip and swipe card used
                   EFallbackCode.Swipe_fallback.fallBackCode -> {
                       cardProcessedDataModal.setFallbackType(EFallbackCode.Swipe_fallback.fallBackCode)
                       //_insertCardStatus.postValue(cardProcessedDataModal)
                       handleEMVFallbackFromError(getString(R.string.fallback),
                           getString(R.string.please_use_another_option), false) {
                           cardProcessedDataModal.setFallbackType(EFallbackCode.Swipe_fallback.fallBackCode)

                           detectCard(cardProcessedDataModal, CardOption.create().apply {
                               supportICCard(false)
                               supportMagCard(true)
                               supportRFCard(false)
                           })

                       }
                   }

                  CardErrorCode.EMV_FALLBACK_ERROR_CODE.errorCode -> {
                      //EMV Fallback case when we insert card from other side then chip side:-
                      cardProcessedDataModal.setReadCardType(DetectCardType.EMV_Fallback_TYPE)
                      cardProcessedDataModal.setFallbackType(EFallbackCode.EMV_fallback.fallBackCode)
                      handleEMVFallbackFromError(getString(R.string.fallback),
                          getString(R.string.please_use_another_option), false) {
                          cardProcessedDataModal.setFallbackType(EFallbackCode.EMV_fallback.fallBackCode)

                          detectCard(cardProcessedDataModal, CardOption.create().apply {
                              supportICCard(false)
                              supportMagCard(true)
                              supportRFCard(false)
                          })

                      }

                  }

                  else -> {

                  }


               }

    }
    //endregion

    private fun detectCard(cardProcessedDataModal: CardProcessedDataModal,cardOption: CardOption){
        defaultScope.launch {
            SearchCard(DeviceHelper.getEMV(), cardProcessedDataModal,vfEmvHandlerCallback1).detectCard(cardOption)
        }

    }

    // Creating transaction packet and
    private fun emvProcessNext(cardProcessedData: CardProcessedDataModal) {
        val transactionISO = CreateTransactionPacketNew(appDao,emiSelectedData,emiTAndCData,brandEMIData,cardProcessedData,BatchTable()).createTransactionPacketNew()
        cardProcessedData.indicatorF58 = transactionISO.additionalData["indicatorF58"] ?: ""

        // logger("Transaction REQUEST PACKET --->>", transactionISO.isoMap, "e")
        //  runOnUiThread { showProgress(getString(R.string.sale_data_sync)) }
        GlobalScope.launch(Dispatchers.IO) {
            checkReversal(transactionISO, cardProcessedData)
        }
    }

    private fun checkReversal(transactionISOByteArray: IsoDataWriter, cardProcessedDataModal: CardProcessedDataModal) {
        //runOnUiThread {
           // cardView_l.visibility = View.GONE
        //}
        // If case Sale data sync to server
        Log.e("1REVERSAL obj ->",""+AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))
        println(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))
        val reversalObj = AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY)
        println(reversalObj)
        println("AppPreference.getReversal()"+AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))
        if (TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
            println("sale_data_sync")
            val msg: String = getString(R.string.authenticating_transaction_msg)
            runOnUiThread { showProgress(msg) }
            logger("1testVFEmvHandler",""+testVFEmvHandler,"e")
            SyncTransactionToHost(transactionISOByteArray, cardProcessedDataModal, testVFEmvHandler) { syncStatus, responseCode, transactionMsg, printExtraData, de55, doubletap ->
                hideProgress()

                if (syncStatus) {
                    val responseIsoData: IsoDataReader = readIso(transactionMsg.toString(), false)
                    val autoSettlementCheck = responseIsoData.isoMap[60]?.parseRaw2String().toString()
                    if (syncStatus && responseCode == "00" && !AppPreference.getBoolean(AppPreference.ONLINE_EMV_DECLINED)) {
                        //Below we are saving batch data and print the receipt of transaction:-

                              GlobalScope.launch(Dispatchers.Main) {
                                  Toast.makeText(this@TransactionActivity,"TXn approved",Toast.LENGTH_SHORT).show()
                              }

                        StubBatchData(
                            de55,
                            cardProcessedDataModal.getTransType(),
                            cardProcessedDataModal,
                            printExtraData,
                            autoSettlementCheck
                        ) { stubbedData ->
                            if (cardProcessedDataModal.getTransType() == TransactionType.EMI_SALE.type ||
                                cardProcessedDataModal.getTransType() == TransactionType.BRAND_EMI.type ||
                                cardProcessedDataModal.getTransType() == TransactionType.BRAND_EMI_BY_ACCESS_CODE.type ||
                                cardProcessedDataModal.getTransType() == TransactionType.FLEXI_PAY.type ||
                                cardProcessedDataModal.getTransType() == TransactionType.TEST_EMI.type

                            ) {

                                stubEMI(stubbedData, emiSelectedData, emiTAndCData,brandEMIData/*, brandEMIAccessData*/) { data ->
                                    Log.d("StubbedEMIData:- ", data.toString())

                                    printAndSaveBatchDataInDB(stubbedData){

                                        if(it){
                                            AppPreference.saveLastReceiptDetails(stubbedData)
                                            Log.e("EMI ", "COMMENT ******")
                                            goToDashBoard()
                                        }
                                    }

//                                    modal=  saveBrandEMIDataToDB(brandEMIData, data.hostInvoice,data.hostTID)
//                                    saveBrandEMIbyCodeDataInDB(
//                                        brandEMIAccessData,
//                                        data.hostInvoice,data.hostTID
//                                    )
//                                    val modal2=runBlocking(Dispatchers.IO) {
//                                        BrandEMIDataTable.getBrandEMIDataByInvoiceAndTid(data.hostInvoice,data.hostTID)
//                                    }
//                                    if (modal2 != null) {
//                                        modal=modal2
//                                    }
//
//                                    printSaveSaleEmiDataInBatch(data) { printCB ->
//                                        if (!printCB) {
//                                            Log.e("EMI FIRST ", "COMMENT ******")
//                                            // Here we are Syncing Txn CallBack to server
//                                            if(tpt?.digiPosCardCallBackRequired=="1") {
//                                                lifecycleScope.launch(Dispatchers.IO) {
//                                                    withContext(Dispatchers.Main) {
//                                                        showProgress(
//                                                            getString(
//                                                                R.string.txn_syn
//                                                            )
//                                                        )
//                                                    }
//
//                                                    val amount = MoneyUtil.fen2yuan(
//                                                        stubbedData.totalAmmount.toDouble()
//                                                            .toLong()
//                                                    )
//                                                    val txnCbReqData = TxnCallBackRequestTable()
//                                                    txnCbReqData.reqtype =
//                                                        EnumDigiPosProcess.TRANSACTION_CALL_BACK.code
//                                                    txnCbReqData.tid = stubbedData.hostTID
//                                                    txnCbReqData.batchnum =
//                                                        stubbedData.hostBatchNumber
//                                                    txnCbReqData.roc = stubbedData.hostRoc
//                                                    txnCbReqData.amount = amount
//
//                                                    txnCbReqData.ecrSaleReqId=stubbedData.ecrTxnSaleRequestId
//                                                    txnCbReqData.txnTime = stubbedData.time
//                                                    txnCbReqData.txnDate = stubbedData.transactionDate
//                                                    TxnCallBackRequestTable.insertOrUpdateTxnCallBackData(
//                                                        txnCbReqData
//                                                    )
//                                                    syncTxnCallBackToHost {
//                                                        Log.e(
//                                                            "TXN CB ",
//                                                            "SYNCED TO SERVER  --> $it"
//                                                        )
//                                                        hideProgress()
//                                                    }
//                                                    Log.e("EMI LAST", "COMMENT ******")
//
//                                                    //Here we are Syncing Offline Sale if we have any in Batch Table and also Check Sale Response has Auto Settlement enabled or not:-
//                                                    //If Auto Settlement Enabled Show Pop Up and User has choice whether he/she wants to settle or not:-
//
//                                                    if (!TextUtils.isEmpty(autoSettlementCheck)) {
//                                                        withContext(Dispatchers.Main) {
//                                                            syncOfflineSaleAndAskAutoSettlement(
//                                                                autoSettlementCheck.substring(
//                                                                    0,
//                                                                    1
//                                                                )
//                                                            )
//                                                        }
//                                                    }
//                                                }
//                                            }
//                                            else{
//                                                if (!TextUtils.isEmpty(autoSettlementCheck)) {
//                                                    GlobalScope.launch(Dispatchers.Main) {
//                                                        syncOfflineSaleAndAskAutoSettlement(
//                                                            autoSettlementCheck.substring(0, 1)
//                                                        )
//                                                    }
//                                                }
//                                            }
//                                        }
//                                    }
                                }
                            }
                            else {
                                printAndSaveBatchDataInDB(stubbedData) { printCB ->
                                    if (printCB) {
                                        AppPreference.saveLastReceiptDetails(stubbedData)
                                        Log.e("FIRST ", "COMMENT ******")
                                        goToDashBoard()
                                        // Here we are Syncing Txn CallBack to server

                                        /*if(tpt?.digiPosCardCallBackRequired=="1" || AppPreference.getBoolean(AppPreference.IsECRon)) {
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                withContext(Dispatchers.Main) {
                                                    showProgress(
                                                        getString(
                                                            R.string.txn_syn
                                                        )
                                                    )
                                                }
                                                val amount = MoneyUtil.fen2yuan(
                                                    stubbedData.totalAmmount.toDouble().toLong()
                                                )
                                                val txnCbReqData = TxnCallBackRequestTable()
                                                txnCbReqData.reqtype = EnumDigiPosProcess.TRANSACTION_CALL_BACK.code
                                                txnCbReqData.tid = stubbedData.hostTID
                                                txnCbReqData.batchnum = stubbedData.hostBatchNumber
                                                txnCbReqData.roc = stubbedData.hostRoc
                                                txnCbReqData.amount = amount
                                                txnCbReqData.ecrSaleReqId=stubbedData.ecrTxnSaleRequestId
                                                txnCbReqData.txnTime = stubbedData.time
                                                txnCbReqData.txnDate = stubbedData.transactionDate
                                                //    20220302 145902

                                                TxnCallBackRequestTable.insertOrUpdateTxnCallBackData(txnCbReqData)

                                                syncTxnCallBackToHost {
                                                    Log.e(
                                                        "TXN CB ",
                                                        "SYNCED TO SERVER  --> $it"
                                                    )
                                                    hideProgress()
                                                }
                                                Log.e("LAST ", "COMMENT ******")

                                                //Here we are Syncing Offline Sale if we have any in Batch Table and also Check Sale Response has Auto Settlement enabled or not:-
                                                //If Auto Settlement Enabled Show Pop Up and User has choice whether he/she wants to settle or not:-

                                                if (!TextUtils.isEmpty(autoSettlementCheck)) {
                                                    withContext(Dispatchers.Main) {
                                                        syncOfflineSaleAndAskAutoSettlement(
                                                            autoSettlementCheck.substring(0, 1)
                                                        )
                                                    }
                                                }
                                            }
                                        }else{
                                            if (!TextUtils.isEmpty(autoSettlementCheck)) {
                                                GlobalScope.launch(Dispatchers.Main) {
                                                    syncOfflineSaleAndAskAutoSettlement(
                                                        autoSettlementCheck.substring(0, 1)
                                                    )
                                                }
                                            }
                                        }*/
                                    }
                                }
                            }


                        }

                    }
                    else if (syncStatus && responseCode != "00") {
                        lifecycleScope.launch(Dispatchers.Main) {
                            alertBoxWithAction(
                                getString(R.string.transaction_delined_msg),
                                responseIsoData.isoMap[58]?.parseRaw2String().toString(),
                                false,
                                getString(R.string.positive_button_ok),
                                { alertPositiveCallback ->
                                    if (alertPositiveCallback) {
                                        /*if (!TextUtils.isEmpty(autoSettlementCheck)) {
                                            syncOfflineSaleAndAskAutoSettlement(
                                                autoSettlementCheck.substring(0, 1)
                                            )
                                        } else {*/
                                            startActivity(
                                                Intent(
                                                    this@TransactionActivity,
                                                    NavigationActivity::class.java
                                                ).apply {
                                                    flags =
                                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                })
                                     //   }
                                    }
                                },
                                {})
                        }
                    }
                    else if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {

                        GlobalScope.launch(Dispatchers.Main) {
                            var hostMsg = responseIsoData.isoMap[58]?.parseRaw2String()
                            Log.e("hostMsg",""+hostMsg)
                            if(hostMsg.isNullOrEmpty()){
                                hostMsg = getString(R.string.transaction_delined_msg)
                                Log.e("hostMsgModify",""+hostMsg)
                            }
                            alertBoxWithAction(
                                getString(R.string.transaction_delined_msg),
                                hostMsg,
                                false,
                                getString(R.string.positive_button_ok),
                                { alertPositiveCallback ->
                                    if (alertPositiveCallback) {
                                        if (!TextUtils.isEmpty(autoSettlementCheck)) {
                                            checkForPrintReversalReceipt(this@TransactionActivity,
                                                autoSettlementCheck){
                                                goToDashBoard()
                                            }
                                            // syncOfflineSaleAndAskAutoSettlement(autoSettlementCheck.substring(0, 1))
                                        } else {

                                        }
                                    }
                                },
                                {})
                        }
                        //Condition for having a reversal(EMV CASE)
                    }


                }

            }
        }
        else {
            println("410")
            if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
                println("412")
                runOnUiThread { showProgress(getString(R.string.reversal_data_sync)) }
                SyncReversalToHost(AppPreference.getReversalNew()) { isSyncToHost, transMsg ->
                    hideProgress()
                    if (isSyncToHost) {
                        AppPreference.clearReversal()
                        checkReversal(transactionISOByteArray, cardProcessedDataModal)
                        println("clearReversal -> check again")
                    } else {
                        GlobalScope.launch(Dispatchers.Main) {
                            //  VFService.showToast(transMsg)
                            alertBoxWithAction(
                                getString(R.string.reversal_upload_fail),
                                getString(R.string.transaction_delined_msg),
                                false,
                                getString(R.string.positive_button_ok),
                                { alertPositiveCallback ->
                                    if (alertPositiveCallback)
                                        declinedTransaction()
                                },
                                {})


                        }
                    }
                }
            }else{
                println("442")
            }
        }
        //Else case is to Sync Reversal data Packet to Host:-

    }

    //Below method is used to handle Transaction Declined case:-
    fun declinedTransaction() {
        try {
            DeviceHelper.getEMV()?.stopSearch()
            finish()
            startActivity(Intent(this, NavigationActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        } catch (ex: java.lang.Exception) {
            finish()
            startActivity(Intent(this, NavigationActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
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


    //Below method is used to save sale data in batch file data table and print the receipt of it:-
    private fun printAndSaveBatchDataInDB(
        stubbedData: TempBatchFileDataTable,
        cb: suspend (Boolean) -> Unit
    ) {
        // printerReceiptData will not be saved in Batch if transaction is pre auth
       // if (transactionType != TransactionTypeValues.PRE_AUTH) {
            //Here we are saving printerReceiptData in BatchFileData Table:-
        Utility().saveTempBatchFileDataTable(stubbedData)
       // } //kushal

       /* PrintUtil(this).startPrinting(
            stubbedData,
            EPrintCopyType.MERCHANT,
            this
        ) { dialogCB, printingFail ->
            Log.d("Sale Printer Status:- ", printingFail.toString())
            if (printingFail == 0)
                runOnUiThread {
                    alertBoxWithAction(
                        getString(R.string.printer_error),
                        getString(R.string.printing_roll_empty_msg),
                        false,
                        getString(R.string.positive_button_ok),
                        {
                            cb(dialogCB, false)
                            *//* startActivity(
                                 Intent(
                                     this@VFTransactionActivity,
                                     MainActivity::class.java
                                 ).apply {
                                     flags =
                                         Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                 })*//*
                        },
                        {})
                }
            else
                cb(dialogCB, true)
        }*/

        lifecycleScope.launch(Dispatchers.Main) {
            showProgress(getString(R.string.printing))
            var printsts = false
            if (stubbedData != null) {
                PrintUtil(this@TransactionActivity as BaseActivityNew).startPrinting(
                    stubbedData,
                    EPrintCopyType.MERCHANT,
                    this@TransactionActivity as BaseActivityNew
                ) { printCB, printingFail ->

                    (this@TransactionActivity as BaseActivityNew).hideProgress()
                    if (printCB) {
                        printsts = printCB
                        lifecycleScope.launch(Dispatchers.Main) {
                            showMerchantAlertBox(stubbedData, cb)
                        }

                    } else {
                        ToastUtils.showToast(
                            this@TransactionActivity as BaseActivityNew,
                            getString(R.string.printer_error)
                        )
                        lifecycleScope.launch(Dispatchers.Main) {
                            cb(false)
                        }

                    }
                }
            }
        }
    }

    private suspend fun showMerchantAlertBox(
        batchTable: TempBatchFileDataTable,
        cb: suspend (Boolean) -> Unit
    ) {
        withContext(Dispatchers.Main) {
            val printerUtil: PrintUtil? = null
            (this@TransactionActivity as BaseActivityNew).alertBoxWithAction(
                getString(R.string.print_customer_copy),
                getString(R.string.print_customer_copy),
                true, getString(R.string.positive_button_yes), { status ->
                    showProgress(getString(R.string.printing))
                    PrintUtil(this@TransactionActivity as BaseActivityNew).startPrinting(
                        batchTable,
                        EPrintCopyType.CUSTOMER,
                        this@TransactionActivity as BaseActivityNew
                    ) { printCB, printingFail ->
                        (this@TransactionActivity as BaseActivityNew).hideProgress()
                        if (printCB) {
                            lifecycleScope.launch(Dispatchers.IO) {
                                cb(printCB)
                            }
                            (this@TransactionActivity as BaseActivityNew).hideProgress()

//                            val intent = Intent(this@TransactionActivity, NavigationActivity::class.java)
//                            startActivity(intent)
                        }

                    }
                }, {
                    lifecycleScope.launch(Dispatchers.IO) {
                        cb(true)
                    }
                    (this@TransactionActivity as BaseActivityNew).hideProgress()
//                    val intent = Intent(this@TransactionActivity, NavigationActivity::class.java)
//                    startActivity(intent)
                })
        }
    }

    private fun goToDashBoard() {
        startActivity(Intent(this@TransactionActivity, NavigationActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    fun handleEMVFallbackFromError(title: String, msg: String, showCancelButton: Boolean, emvFromError: (Boolean) -> Unit) {
        lifecycleScope.launch(Dispatchers.Main) {
            alertBoxWithAction(title,
                msg, showCancelButton, getString(R.string.positive_button_ok), { alertCallback ->
                    if (alertCallback) {
                        emvFromError(true)
                    }
                }, {})
        }
    }

    enum class CardErrorCode(var errorCode: Int) {
        EMV_FALLBACK_ERROR_CODE(40961),
        CTLS_ERROR_CODE(6)
    }

    enum class EFallbackCode(var fallBackCode: Int) {
        Swipe_fallback(111),
        EMV_fallback(8),
        NO_fallback(0),
        EMV_fallbackNew(12),
        CTLS_fallback(333)
    }

    protected open fun respondCVMResult(result: Byte) {
        try {
            val chvStatus = TLV.fromData(EMVTag.DEF_TAG_CHV_STATUS, byteArrayOf(result))
            val ret = DeviceHelper.getEMV()!!.respondEvent(chvStatus.toString())
            println("...onCardHolderVerify: respondEvent" + ret)
        } catch (e: java.lang.Exception) {
            //handleException(e);
        }
    }
}