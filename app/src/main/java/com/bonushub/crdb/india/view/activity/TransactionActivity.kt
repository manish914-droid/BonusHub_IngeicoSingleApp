package com.bonushub.crdb.india.view.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.ActivityEmvBinding
import com.bonushub.crdb.india.db.AppDao
import com.bonushub.crdb.india.entity.CardOption
import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.model.local.AppPreference
import com.bonushub.crdb.india.model.local.BatchTable
import com.bonushub.crdb.india.model.local.TempBatchFileDataTable
import com.bonushub.crdb.india.model.remote.*
import com.bonushub.crdb.india.repository.GenericResponse
import com.bonushub.crdb.india.repository.ServerRepository
import com.bonushub.crdb.india.serverApi.bankEMIRequestCode
import com.bonushub.crdb.india.transactionprocess.*
import com.bonushub.crdb.india.type.DemoConfigs
import com.bonushub.crdb.india.type.EmvOption
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getMaskedPan
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getTptData
import com.bonushub.crdb.india.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.india.utils.ingenico.TLV
import com.bonushub.crdb.india.utils.printerUtils.PrintUtil
import com.bonushub.crdb.india.utils.printerUtils.checkForPrintReversalReceipt
import com.bonushub.crdb.india.view.base.BaseActivityNew
import com.bonushub.crdb.india.view.baseemv.EmvHandler
import com.bonushub.crdb.india.view.baseemv.GenericCardReadHandler
import com.bonushub.crdb.india.view.baseemv.SearchCard
import com.bonushub.crdb.india.viewmodel.TenureSchemeViewModel
import com.bonushub.crdb.india.vxutils.BhTransactionType
import com.bonushub.crdb.india.vxutils.Utility.byte2HexStr
import com.bonushub.crdb.india.vxutils.Utility.getCardHolderName
import com.bonushub.crdb.india.vxutils.dateFormaterNew
import com.bonushub.crdb.india.vxutils.getEncryptedPanorTrackData
import com.bonushub.crdb.india.vxutils.timeFormaterNew
import com.google.gson.Gson
import com.usdk.apiservice.aidl.emv.EMVError.*
import com.usdk.apiservice.aidl.emv.EMVTag
import com.usdk.apiservice.aidl.emv.SearchCardListener
import com.usdk.apiservice.aidl.pinpad.DeviceName
import com.usdk.apiservice.aidl.pinpad.KAPId
import com.usdk.apiservice.aidl.pinpad.OnPinEntryListener
import com.usdk.apiservice.aidl.pinpad.PinpadData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class TransactionActivity : BaseActivityNew() {

    companion object {
        val TAG = TransactionActivity::class.java.simpleName
    }

    private var isToExit = false

    @Inject
    lateinit var appDao: AppDao

    @Inject
    lateinit var serverRepository: ServerRepository

    private lateinit var tenureSchemeViewModel: TenureSchemeViewModel


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

    private var cardDetectionCb: (CardProcessedDataModal) -> Unit = ::onCardDetectionAction


    private fun onCardDetectionAction(cardProcessedDataModal: CardProcessedDataModal) {
        println("Detected card type ------> ${cardProcessedDataModal.getReadCardType()}")
        processAccordingToCardType(cardProcessedDataModal)
    }


    private  var emvProcessHandler: EmvHandler?=null

    private val brandDataMaster by lazy { intent.getSerializableExtra("brandDataMaster") as BrandEMIMasterDataModal }
    private val brandEmiProductData by lazy { intent.getSerializableExtra("brandEmiProductData") as BrandEMIProductDataModal }
    private val imeiOrSerialNum by lazy { intent.getStringExtra("imeiOrSerialNum") ?: "" }

    private var emiSelectedData: BankEMITenureDataModal? = null // BankEMIDataModal
    private var emiTAndCData: BankEMIIssuerTAndCDataModal? = null

    private val brandEMIData by lazy {
        intent.getSerializableExtra("brandEMIData") as BrandEMIDataModal?
    }

    private var txnAmountAfterApproved = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        emvBinding = ActivityEmvBinding.inflate(layoutInflater)
        //setContentView(R.layout.activity_emv)
        setContentView(emvBinding?.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        emvBinding?.subHeaderView?.subHeaderText?.text = transactionTypeEDashboardItem.title
        emvBinding?.subHeaderView?.headerImage?.setImageResource(transactionTypeEDashboardItem.res) //= transactionTypeEDashboardItem.title
        emvBinding?.txnAmtLl?.visibility = View.VISIBLE

        globalCardProcessedModel.setTransType(transactionType)
        globalCardProcessedModel.setTransactionAmount((saleAmt.toFloat() * 100).toLong())
        globalCardProcessedModel.setOtherAmount((cashBackAmt.toFloat() * 100).toLong())
        globalCardProcessedModel.setProcessingCode(transactionProcessingCode)

        tenureSchemeViewModel = ViewModelProvider(this).get(TenureSchemeViewModel::class.java)

        when (transactionType) {

            BhTransactionType.SALE_WITH_CASH.type -> {
                val amt = saleAmt.toFloat() + cashBackAmt.toFloat()
                val frtAmt = "%.2f".format(amt)
                emvBinding?.baseAmtTv?.text = getString(R.string.rupees_symbol) + frtAmt
                emvBinding?.tvInsertCard?.text = "Please Insert/Swipe/TAP Card"
            }

            BhTransactionType.BRAND_EMI.type, BhTransactionType.EMI_SALE.type -> {
                val amt = saleAmt.toFloat() + cashBackAmt.toFloat()
                val frtAmt = "%.2f".format(amt)
                txnAmountAfterApproved = frtAmt
                emvBinding?.baseAmtTv?.text = getString(R.string.rupees_symbol) + frtAmt
                emvBinding?.tvInsertCard?.text = "Please Insert/Swipe Card"
                globalCardProcessedModel.setEmiTransactionAmount((saleAmt.toDouble() * 100).toLong())
            }

            else -> {
                val frtAmt = "%.2f".format(saleAmt.toFloat())
                txnAmountAfterApproved = frtAmt
                emvBinding?.baseAmtTv?.text = getString(R.string.rupees_symbol) + frtAmt
                emvBinding?.tvInsertCard?.text = "Please Insert/Swipe/TAP Card"

            }
        }

        val cardOption = CardOption.create().apply {
            supportICCard(true)
            supportMagCard(true)
            supportRFCard(true)
        }

        detectCard(globalCardProcessedModel, cardOption)

        emvBinding?.subHeaderView?.backImageButton?.setOnClickListener {
            declinedTransaction()
        }

        tenureSchemeViewModel.emiTenureLiveData.observe(
            this@TransactionActivity
        ) {
            when (val genericResp = it) {
                is GenericResponse.Success -> {
                    println(Gson().toJson(genericResp.data))
                    val resp = genericResp.data as TenuresWithIssuerTncs
                    logger("instaEmi", "yes", "e")
                    hideProgress()
                    DialogUtilsNew1.instaEmiDialog(this@TransactionActivity,
                        { dialog, activity ->
                            // emi
                            globalCardProcessedModel.setTransType(BhTransactionType.EMI_SALE.type)
                            globalCardProcessedModel.setEmiType(1)  //1 for insta emi
                            emvBinding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_header_emi_sale)
                            emvBinding?.subHeaderView?.subHeaderText?.text =
                                BhTransactionType.EMI_SALE.txnTitle
                            dialog.dismiss()
                            val intent = Intent(activity, TenureSchemeActivity::class.java)
                            intent.putParcelableArrayListExtra(
                                "emiSchemeOfferDataList",
                                resp.bankEMISchemesDataList as ArrayList<out Parcelable>
                            )
                            intent.putExtra("emiIssuerTAndCDataList", resp.bankEMIIssuerTAndCList)
                            intent.putExtra("cardProcessedData", globalCardProcessedModel)
                            intent.putExtra(
                                "transactionType",
                                BhTransactionType.SALE.type
                            ) // for insta emi
                            activity.startActivityForResult(
                                intent,
                                EIntentRequest.BankEMISchemeOffer.code
                            )
                        },
                        { dialog ->
                            // sale
                            dialog.dismiss()
                            globalCardProcessedModel.setTransType(BhTransactionType.SALE.type) //0306

                            if(globalCardProcessedModel.getReadCardType()==DetectCardType.MAG_CARD_TYPE){
                            val isPin= globalCardProcessedModel.getIsOnline()==1
                             processSwipeCardWithPINorWithoutPIN(isPin,globalCardProcessedModel)
                            }
                            if(globalCardProcessedModel.getReadCardType()==DetectCardType.EMV_CARD_TYPE) {
                                startEmvAfterCardGenricCardRead()
                            }

                        },
                        { dialog ->
                            // cancel
                            dialog.dismiss()
                            declinedTransaction()
                        })

                }
                is GenericResponse.Error -> {
                    hideProgress()
                    globalCardProcessedModel.setTransType(BhTransactionType.SALE.type) //0306
                    val emvOption = EmvOption.create().apply { flagPSE(0x00.toByte()) }
                    emvProcessHandler = emvHandler()
                    logger("2testVFEmvHandler", "" + emvProcessHandler, "e")
                    DeviceHelper.getEMV()?.startEMV(emvOption.toBundle(), emvProcessHandler)
                    println(genericResp.errorMessage.toString())
                }
                is GenericResponse.Loading -> {
// currently not in use ....
                }
            }
        }

    }


    private fun processAccordingToCardType(cardProcessedDataModal: CardProcessedDataModal) {
        when (cardProcessedDataModal.getReadCardType()) {
            DetectCardType.MAG_CARD_TYPE -> {
                logger("DetectCard", "MAG detected", "e")

                if (cardProcessedDataModal.getFallbackType() != EFallbackCode.Swipe_fallback.fallBackCode) {
                    val currDate = getCurrentDateforMag()
                    if (/*currDate.compareTo(cardProcessedDataModal.getExPiryDate()!!) <= 0*/true) {
                        println("Correct Date")
                        Log.d(TAG, "onCardSwiped ...1")
                        //  val bytes: ByteArray = ROCProviderV2.hexStr2Byte(track2)
                        // Log.d(TAG, "Track2:" + track2 + " (" + ROCProviderV2.byte2HexStr(bytes) + ")")

                        getCardHolderName(cardProcessedDataModal, cardProcessedDataModal.getTrack1Data(), '^', '^')
                        //Stubbing Card Processed Data:-
                        cardProcessedDataModal.setReadCardType(DetectCardType.MAG_CARD_TYPE)

                        cardProcessedDataModal.getTrack2Data()

                        //region----------- sir please check
                        val track2 = cardProcessedDataModal.getTrack2Data()

                        /*var track21 = "35,36|${
                            track2?.replace("D", "=")?.replace("F", "")
                        }" + "|" + cardProcessedDataModal?.getCardHolderName() + "~~~" +
                                cardProcessedDataModal?.getTypeOfTxnFlag() + "~" + cardProcessedDataModal?.getPinEntryFlag()*/

                        val field57 = "35|" + track2?.replace("D", "=")?.replace("F", "")
                        println("Field 57 data is" + field57)
                        val encrptedPan = getEncryptedPanorTrackData(field57, true)
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
                                        onEndProcessCalled(
                                            EFallbackCode.Swipe_fallback.fallBackCode,
                                            cardProcessedDataModal
                                        )
                                    } else {
                                        //region================Condition Check and ProcessSwipeCardWithPinOrWithoutPin:-
                                        when (cardProcessedDataModal.getTransType()) {
                                            BhTransactionType.SALE.type -> processSwipeCardWithPINorWithoutPIN(
                                                isPin,
                                                cardProcessedDataModal
                                            )
                                            BhTransactionType.EMI_SALE.type -> {
                                                val intent = Intent(
                                                    this@TransactionActivity,
                                                    TenureSchemeActivity::class.java
                                                ).apply {
                                                    putExtra(
                                                        "cardProcessedData",
                                                        globalCardProcessedModel
                                                    )
                                                    putExtra(
                                                        "transactionType",
                                                        globalCardProcessedModel.getTransType()
                                                    )
                                                    putExtra("mobileNumber", mobileNumber)
                                                }
                                                startActivityForResult(
                                                    intent,
                                                    BhTransactionType.EMI_SALE.type
                                                )
                                            }
                                            BhTransactionType.BRAND_EMI.type -> {
                                                val intent = Intent(
                                                    this@TransactionActivity,
                                                    TenureSchemeActivity::class.java
                                                ).apply {
                                                    putExtra(
                                                        "cardProcessedData",
                                                        globalCardProcessedModel
                                                    )
                                                    putExtra(
                                                        "transactionType",
                                                        globalCardProcessedModel.getTransType()
                                                    )
                                                    putExtra("mobileNumber", mobileNumber)
                                                    putExtra("brandID", brandEMIData?.brandID ?: "")
                                                    putExtra(
                                                        "productID",
                                                        brandEMIData?.productID ?: ""
                                                    )
                                                    putExtra(
                                                        "imeiOrSerialNum",
                                                        brandEMIData?.imeiORserailNum ?: ""
                                                    )
                                                }
                                                startActivityForResult(
                                                    intent,
                                                    BhTransactionType.EMI_SALE.type
                                                )
                                            }
                                            else -> processSwipeCardWithPINorWithoutPIN(
                                                isPin, cardProcessedDataModal
                                            )
                                            //endregion
                                        }

                                    }
                                }

                                else {
                                    hideEmvCardImage()

                                    when (cardProcessedDataModal.getTransType()) {
                                        BhTransactionType.SALE.type -> {
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                if (checkInstaEmi()) {
                                                    val field57Data = "$bankEMIRequestCode^0^1^0^^${/*cardProcessedDataModal?.getPanNumberData()?.substring(0, 8)*/""}^${globalCardProcessedModel.getTransactionAmount()}"
                                                    runBlocking {
                                                        withContext(Dispatchers.Main) {
                                                            showProgress()
                                                        }
                                                        withContext(Dispatchers.IO) {
                                                            tenureSchemeViewModel.getEMITenureData(
                                                                globalCardProcessedModel.getPanNumberData()
                                                                    ?: "",
                                                                field57Data
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    processSwipeCardWithPINorWithoutPIN(
                                                        isPin,
                                                        cardProcessedDataModal
                                                    )
                                                }
                                            }

                                        }
                                        BhTransactionType.EMI_SALE.type -> {
                                            val intent = Intent(
                                                this@TransactionActivity,
                                                TenureSchemeActivity::class.java
                                            ).apply {
                                                putExtra(
                                                    "cardProcessedData",
                                                    globalCardProcessedModel
                                                )
                                                putExtra(
                                                    "transactionType",
                                                    globalCardProcessedModel.getTransType()
                                                )
                                                putExtra("mobileNumber", mobileNumber)
                                            }
                                            startActivityForResult(
                                                intent,
                                                BhTransactionType.EMI_SALE.type
                                            )
                                        }
                                        BhTransactionType.BRAND_EMI.type -> {
                                            val intent = Intent(this@TransactionActivity, TenureSchemeActivity::class.java).apply {
                                                putExtra("cardProcessedData", globalCardProcessedModel)
                                                putExtra("transactionType", globalCardProcessedModel.getTransType())
                                                putExtra("mobileNumber", mobileNumber)
                                                putExtra("brandID", brandEMIData?.brandID ?: "")
                                                putExtra("productID", brandEMIData?.productID ?: "")
                                                putExtra("imeiOrSerialNum", brandEMIData?.imeiORserailNum ?: "")
                                            }
                                            startActivityForResult(
                                                intent,
                                                BhTransactionType.EMI_SALE.type
                                            )
                                        }
                                        else -> processSwipeCardWithPINorWithoutPIN(isPin, cardProcessedDataModal)
                                        //endregion
                                    }
                                }
                            }
                        }
                        else {
                            onEndProcessCalled(
                                cardProcessedDataModal.getFallbackType(),
                                cardProcessedDataModal
                            )
                        }
                        //endregion
                    } else {
                        handleEMVFallbackFromError(
                            "card read error",
                            "reinitiate txn",
                            false
                        ) { alertCBBool ->
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

            DetectCardType.EMV_CARD_TYPE -> {
                CoroutineScope(Dispatchers.IO).launch {
                    logger("DetectCard", "EMV detected", "e")
                    when (globalCardProcessedModel.getTransType()) {
                        BhTransactionType.SALE.type -> {
                            if (checkInstaEmi()) {
                                //   val cardReadHandler=
                                val emvOption = EmvOption.create().apply { flagPSE(0x00.toByte()) }
                                DeviceHelper.getEMV()?.startEMV(emvOption.toBundle(),
                                    GenericCardReadHandler(
                                        DeviceHelper.getEMV(),
                                        this@TransactionActivity,
                                        globalCardProcessedModel
                                    ) {
                                        globalCardProcessedModel.setPanNumberData(it)

                                        val field57 = "$bankEMIRequestCode^0^1^0^^${/*cardProcessedDataModal?.getPanNumberData()?.substring(0, 8)*/""}^${globalCardProcessedModel.getTransactionAmount()}"
                                        runBlocking {
                                            withContext(Dispatchers.Main){
                                                    showProgress()
                                            }
                                            withContext(Dispatchers.IO) {
                                                tenureSchemeViewModel.getEMITenureData(globalCardProcessedModel.getPanNumberData() ?: "", field57)
                                            }
                                        }
                                    }.apply {
                                        onEndProcessCallback = ::onEndProcessCalled
                                    }
                                )
                            } else {
                               startFullEmvProcess()
                            }

                        }
                        BhTransactionType.SALE_WITH_CASH.type->{
                            startFullEmvProcess()
                        }
                        BhTransactionType.CASH_AT_POS.type->{
                            startFullEmvProcess()
                        }
                        BhTransactionType.EMI_SALE.type,BhTransactionType.BRAND_EMI.type -> {
                            val emvOption = EmvOption.create().apply { flagPSE(0x00.toByte()) }
                            DeviceHelper.getEMV()?.startEMV(emvOption.toBundle(),
                                GenericCardReadHandler(
                                    DeviceHelper.getEMV(),
                                    this@TransactionActivity,
                                    globalCardProcessedModel
                                ) {
                                    globalCardProcessedModel.setPanNumberData(it)

                                    val intent = Intent(this@TransactionActivity, TenureSchemeActivity::class.java).apply {
                                        putExtra("cardProcessedData", globalCardProcessedModel)
                                        putExtra("transactionType", globalCardProcessedModel.getTransType())
                                        putExtra("mobileNumber", mobileNumber)
                                        putExtra("brandID", brandEMIData?.brandID ?: "")
                                        putExtra("productID", brandEMIData?.productID ?: "")
                                        putExtra("imeiOrSerialNum", brandEMIData?.imeiORserailNum ?: "")
                                    }
                                    startActivityForResult(intent, globalCardProcessedModel.getTransType())
                                }.apply {
                                    onEndProcessCallback = ::onEndProcessCalled
                                }
                            )
                        }
                       BhTransactionType.BRAND_EMI.type -> {

                           startActivityForResult(
                               intent,
                               BhTransactionType.EMI_SALE.type
                           )

                       }
                        else -> {
                            startFullEmvProcess()
                        }
                    }
                }
            }

            DetectCardType.CONTACT_LESS_CARD_TYPE -> {
                CoroutineScope(Dispatchers.Main).launch {
                    //Toast.makeText(applicationContext,"Contactless detected", Toast.LENGTH_LONG).show
                    logger("DetectCard", "Contactless detected", "e")
                    val emvOption = EmvOption.create().apply {
                        flagPSE(0x01.toByte())
                    }
                    DeviceHelper.getEMV()?.stopEMV()
                    DeviceHelper.getEMV()?.stopSearch()
                    DeviceHelper.getEMV()?.halt()
                    emvProcessHandler = emvHandler()
                    DeviceHelper.getEMV()?.startEMV(emvOption.toBundle(), emvProcessHandler)
                    DeviceHelper.getEMV()?.switchDebug(2) //emvLogLevel -> 2
                }
            }

            DetectCardType.TIMEOUT->{
                runOnUiThread {
                    alertBoxWithActionNew(
                        DetectCardType.TIMEOUT.cardTypeName,
                        "",
                        R.drawable.ic_info_new,
                        "OK",
                        "",
                        false,
                        false,
                        { declinedTransaction() },
                        {})
                }
            }

            DetectCardType.ERROR->{
                runOnUiThread {
                    alertBoxWithActionNew(
                        DetectCardType.ERROR.cardTypeName,
                        "",
                        R.drawable.ic_info_new,
                        "OK",
                        "",
                        false,
                        false,
                        { declinedTransaction() },
                        {})
                }
            }

            else -> {

            }
        }
    }

    private fun startFullEmvProcess() {
        val emvOption = EmvOption.create().apply { flagPSE(0x00.toByte()) }
        emvProcessHandler = emvHandler()
        logger("2testVFEmvHandler", "" + emvProcessHandler, "e")
        DeviceHelper.getEMV()?.startEMV(emvOption.toBundle(), emvProcessHandler)
        DeviceHelper.getEMV()?.switchDebug(2) //emvLogLevel -> 2
    }


    private fun startEmvAfterCardGenricCardRead() {
        try {
            val cardOption = CardOption.create().apply {
                supportICCard(true)
                supportMagCard(true)
                supportRFCard(true)
            }
            DeviceHelper.getEMV()?.searchCard(
                cardOption.toBundle(),
                DemoConfigs.TIMEOUT,
                object : SearchCardListener.Stub() {
                    override fun onCardPass(cardType: Int) {
                        /* println("=> onCardPass | cardType = $cardType")
                         cardProcessedDataModal.setReadCardType(DetectCardType.CONTACT_LESS_CARD_TYPE)
                         transactionCallback(cardProcessedDataModal)*/
                    }

                    override fun onCardInsert() {
                        /* println("=> onCardInsert")
                         cardProcessedDataModal.setReadCardType(DetectCardType.EMV_CARD_TYPE)
                         transactionCallback(cardProcessedDataModal)*/
                        val emvOption = EmvOption.create().apply { flagPSE(0x00.toByte()) }
                        emvProcessHandler = emvHandler()
                        logger("2testVFEmvHandler", "" + emvProcessHandler, "e")
                        DeviceHelper.getEMV()?.startEMV(emvOption.toBundle(), emvProcessHandler)
                    }

                    override fun onCardSwiped(track: Bundle) {
                        // uemv?.stopSearch()
/*

                        println("=> onCardSwiped")
                        println("==> Pan: " + track.getString(EMVData.PAN))
                        println("==> Track 1: " + track.getString(EMVData.TRACK1))
                        println("==> Track 2: " + track.getString(EMVData.TRACK2))
                        println("==> Track 3: " + track.getString(EMVData.TRACK3))
                        println("==> Service code: " + track.getString(EMVData.SERVICE_CODE))
                        println("==> Card exprited date: " + track.getString(EMVData.EXPIRED_DATE))
                        cardProcessedDataModal.setReadCardType(DetectCardType.MAG_CARD_TYPE)

                        val pan = track.getString(EMVData.PAN)
                        val track1 = track.getString(EMVData.TRACK1)
                        val track2 = track.getString(EMVData.TRACK2)
                        val track3 = track.getString(EMVData.TRACK3)
                        val serviceCode = track.getString(EMVData.SERVICE_CODE)
                        pan?.let { cardProcessedDataModal.setPanNumberData(it) }
                        track1?.let { cardProcessedDataModal.setTrack1Data(it) }
                        track2?.let { cardProcessedDataModal.setTrack2Data(it) }
                        track3?.let { cardProcessedDataModal.setTrack3Data(it) }
                        serviceCode?.let { cardProcessedDataModal.setServiceCodeData(it) }


                        //Expiry Date
                        serviceCode?.let { cardProcessedDataModal.setExpiredDate(track.getString(
                            EMVData.EXPIRED_DATE)) }

                        transactionCallback(cardProcessedDataModal)

*/

                    }

                    override fun onTimeout() {
                        println("=> onTimeout")
                    }

                    override fun onError(code: Int, message: String) {
                        println("Code: $code")
                        println("message: $message")
                        println(String.format("=> onError | %s[0x%02X]", message, code))
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }







    }

    private fun hideEmvCardImage() {

        CoroutineScope(Dispatchers.Main).launch {
            emvBinding?.cardDetectImg?.visibility = View.GONE
            emvBinding?.tvInsertCard?.visibility = View.GONE

            val msg: String = getString(R.string.processing)
            runOnUiThread { showProgress(msg) }
        }
    }

    // Process Swipe card with or without PIN .
    private fun processSwipeCardWithPINorWithoutPIN(
        ispin: Boolean,
        cardProcessedDataModal: CardProcessedDataModal
    ) {
        runOnUiThread {
            hideProgress()
        }
        if (ispin) {
            val panBlock: String? = cardProcessedDataModal.getPanNumberData()
            val param = Bundle()
            param.putByteArray(PinpadData.PIN_LIMIT, byteArrayOf(0, 4, 5, 6, 7, 8, 9, 10, 11, 12))


            val listener: OnPinEntryListener = object : OnPinEntryListener.Stub() {
                override fun onInput(arg0: Int, arg1: Int) {}
                override fun onConfirm(data: ByteArray, arg1: Boolean) {
                    System.out.println("PinBlock is" + byte2HexStr(data))
                    Log.d(
                        "PinBlock",
                        "PinPad hex encrypted data ---> " + hexString2String(
                            BytesUtil.bytes2HexString(data)
                        )
                    )

                    respondCVMResult(1.toByte())

                    when (cardProcessedDataModal.getReadCardType()) {
                        DetectCardType.EMV_CARD_TYPE -> {
                            if (cardProcessedDataModal.getIsOnline() == 1) {
                                cardProcessedDataModal.setGeneratePinBlock(
                                    BytesUtil.bytes2HexString(
                                        data
                                    )
                                )
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
                                cardProcessedDataModal.setGeneratePinBlock(
                                    BytesUtil.bytes2HexString(
                                        data
                                    )
                                )
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

                    // if(!checkInstaEmi(cardProcessedDataModal)) {
                    emvProcessNext(cardProcessedDataModal)
                    //}
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
            DeviceHelper.getPinpad(KAPId(0, 0), 0, DeviceName.IPP)
                ?.startPinEntry(DemoConfig.KEYID_PIN, param, listener)

        } else {
            if (cardProcessedDataModal.getFallbackType() == com.bonushub.crdb.india.utils.EFallbackCode.EMV_fallback.fallBackCode)
                cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_FALL_MAGPIN.posEntry.toString())
            else
                cardProcessedDataModal.setPosEntryMode(PosEntryModeType.POS_ENTRY_SWIPED_NO4DBC_PIN.posEntry.toString())
            cardProcessedDataModal.setApplicationPanSequenceValue("00")
            emvProcessNext(cardProcessedDataModal)
        }
    }

    //region========================================Below Method is a Handler for EMV CardType:-
    private fun emvHandler(): EmvHandler {
        println("DoEmv VfEmvHandler is calling")
        println("IEmv value is" + DeviceHelper.getEMV().toString())

        return EmvHandler(
            DeviceHelper.getPinpad(KAPId(0, 0), 0, DeviceName.IPP),
            DeviceHelper.getEMV(),
            this@TransactionActivity,
            globalCardProcessedModel,
            cardDetectionCb
        ).also { ei ->
            ei.onEndProcessCallback = ::onEndProcessCalled
            ei.vfEmvHandlerCallback = ::onEmvprocessnext
        }

    }

    private fun onEmvprocessnext(cardProcessedDataModal: CardProcessedDataModal) {
        println("processflow called")

        hideEmvCardImage()

        when (transactionType) {
            BhTransactionType.BRAND_EMI.type -> {
                val intent = Intent(this@TransactionActivity, TenureSchemeActivity::class.java).apply {
                    /*          val field57 =
                                  "$bankEMIRequestCode^0^${brandDataMaster.brandID}^${brandEmiProductData.productID}^${imeiOrSerialNum}" +
                                          "^${cardBinValue.substring(0, 8)""}^${globalCardProcessedModel.getTransactionAmount()}"
              */
                    putExtra("cardProcessedData", globalCardProcessedModel)
                    putExtra("brandID", brandDataMaster.brandID)
                    putExtra("productID", brandEmiProductData.productID)
                    putExtra("imeiOrSerialNum", imeiOrSerialNum)
                    putExtra("transactionType", globalCardProcessedModel.getTransType())
                    putExtra("mobileNumber", mobileNumber)
                }
                startActivityForResult(intent, BhTransactionType.BRAND_EMI.type)
            }
            BhTransactionType.EMI_SALE.type -> {
                val intent = Intent(this@TransactionActivity, TenureSchemeActivity::class.java).apply {
                    putExtra("cardProcessedData", globalCardProcessedModel)
                    putExtra("transactionType", globalCardProcessedModel.getTransType())
                    putExtra("mobileNumber", mobileNumber)
                }
                startActivityForResult(intent, BhTransactionType.EMI_SALE.type)
            }
            BhTransactionType.PRE_AUTH.type -> {
                emvProcessNext(cardProcessedDataModal)

            }
            BhTransactionType.SALE.type -> {
                // check insta emi
                ///  if(!checkInstaEmi(cardProcessedDataModal)) {
                //Condition executes when  No EMI Available instantly(Directly Normal Sale)
                emvProcessNext(cardProcessedDataModal)
                ///   }

            }
            BhTransactionType.CASH_AT_POS.type->{
                emvProcessNext(cardProcessedDataModal)

            }
            BhTransactionType.SALE_WITH_CASH.type->{
                emvProcessNext(cardProcessedDataModal)

            } BhTransactionType.REFUND.type->{
                emvProcessNext(cardProcessedDataModal)

            }
        }
    }

    private suspend fun checkInstaEmi(): Boolean {
//return false
        var hasInstaEmi = false
        val tpt = getTptData()
        var limitAmt = 0f
        if (tpt?.surChargeValue?.isNotEmpty()!!) {
            limitAmt = try {
                // tpt.surChargeValue.toFloat() / 100
                tpt.surChargeValue.toFloat()
            } catch (ex: Exception) {
                0f
            }
        }
        if (tpt.surcharge.isNotEmpty()) {
            hasInstaEmi = try {
                tpt.surcharge == "1"
            } catch (ex: Exception) {
                false
            }
        }
        if (tpt.bankEmi != "1") {
            hasInstaEmi = false
        }
        // Condition executes, If insta EMI is Available on card
        val transactionalAmt = (saleAmt.toDouble() * 100).toLong()

        return if ((limitAmt <= transactionalAmt) && hasInstaEmi && (globalCardProcessedModel.getTransType() == BhTransactionType.SALE.type)) {
            //region=========This Field is use only in case of BankEMI Field58 Transaction Amount:-
            globalCardProcessedModel.setEmiTransactionAmount(transactionalAmt)
            true
        } else {
            false
        }
    }

    lateinit var cardBinLoad: Deferred<Boolean>


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            BhTransactionType.EMI_SALE.type, BhTransactionType.BRAND_EMI.type, EIntentRequest.BankEMISchemeOffer.code -> {
                val cardProcessedData =
                    data?.getSerializableExtra("cardProcessedDataModal") as CardProcessedDataModal
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
                    if (cardProcessedData.getTransType() == BhTransactionType.TEST_EMI.type) {
                        /* val baseAmountValue = getString(R.string.rupees_symbol) + "1.00"
                         binding?.baseAmtTv?.text = baseAmountValue*/
                    } else {
                        val baseAmountValue = getString(R.string.rupees_symbol) + "%.2f".format(
                            (((emiSelectedData?.transactionAmount)?.toDouble())?.div(100)).toString()
                                .toDouble()
                        )
                        emvBinding?.baseAmtTv?.text = baseAmountValue
                        txnAmountAfterApproved = "%.2f".format(
                            (((emiSelectedData?.transactionAmount)?.toDouble())?.div(100)).toString()
                                .toDouble()
                        )
                    }
                }


                //region===============Check Transaction Type and Perform Action Accordingly:-
                if (cardProcessedData.getReadCardType() == DetectCardType.MAG_CARD_TYPE && cardProcessedData.getTransType() != BhTransactionType.TEST_EMI.type) {
                    val isPin = cardProcessedData.getIsOnline() == 1
                    cardProcessedData.setProcessingCode(transactionProcessingCode)
                    processSwipeCardWithPINorWithoutPIN(isPin, cardProcessedData)
                } else {
                    if (cardProcessedData.getTransType() == BhTransactionType.TEST_EMI.type) {
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
                        emvProcessHandler = emvHandler()
                        logger("3testVFEmvHandler", "" + emvProcessHandler, "e")


                        // sir please check emv call
                        DoEmv(
                            emvProcessHandler,
                            this,
                            cardProcessedData
                        ) { cardProcessedDataModal, vfEmvHandler ->
                            Log.d("CardEMIData:- ", cardProcessedDataModal.toString())
                            emvProcessHandler = vfEmvHandler
                            cardProcessedDataModal.setProcessingCode(transactionProcessingCode)
                            //    cardProcessedDataModal.setTransactionAmount(emiSelectedTransactionAmount ?: 0L)
                            //     cardProcessedDataModal.setOtherAmount(otherTransAmount)
                            cardProcessedDataModal.setMobileBillExtraData(
                                Pair(
                                    mobileNumber,
                                    billNumber
                                )
                            )
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

            else -> {

            }
        }
    }

    private fun onEndProcessCalled(result: Int, cardProcessedDataModal: CardProcessedDataModal) {
        println("Fallback called")
        when (result) {
            //Swipe fallback case when chip and swipe card used
            EFallbackCode.Swipe_fallback.fallBackCode -> {
                globalCardProcessedModel.setFallbackType(EFallbackCode.Swipe_fallback.fallBackCode)
                //_insertCardStatus.postValue(cardProcessedDataModal)
                handleEMVFallbackFromError(
                    getString(R.string.fallback),
                    getString(R.string.please_use_another_option), false
                ) {
                    globalCardProcessedModel.setFallbackType(EFallbackCode.Swipe_fallback.fallBackCode)

                    detectCard(globalCardProcessedModel, CardOption.create().apply {
                        supportICCard(true)
                        supportMagCard(false)
                        supportRFCard(false)
                    })

                }
            }

            EFallbackCode.EMV_fallback.fallBackCode -> {
                //EMV Fallback case when we insert card from other side then chip side:-
                globalCardProcessedModel.setReadCardType(DetectCardType.EMV_Fallback_TYPE)
                globalCardProcessedModel.setFallbackType(EFallbackCode.EMV_fallback.fallBackCode)
                handleEMVFallbackFromError(
                    getString(R.string.fallback),
                    getString(R.string.please_use_another_option), false
                ) {
                 //   globalCardProcessedModel.setFallbackType(EFallbackCode.EMV_fallback.fallBackCode)
                    detectCard(globalCardProcessedModel, CardOption.create().apply {
                        supportICCard(false)
                        supportMagCard(true)
                        supportRFCard(false)
                    })

                }

            }

            else -> {
              val errorMsg=  when (result) {
                    ERROR_POWERUP_FAIL -> "ERROR_POWERUP_FAIL"
                    ERROR_ACTIVATE_FAIL -> "ERROR_ACTIVATE_FAIL"
                    ERROR_WAITCARD_TIMEOUT -> "ERROR_WAITCARD_TIMEOUT"
                    ERROR_NOT_START_PROCESS -> "ERROR_NOT_START_PROCESS"
                    ERROR_PARAMERR -> "ERROR_PARAMERR"
                    ERROR_MULTIERR -> "ERROR_MULTIERR"
                    ERROR_CARD_NOT_SUPPORT -> "ERROR_CARD_NOT_SUPPORT"
                    ERROR_EMV_RESULT_BUSY -> "ERROR_EMV_RESULT_BUSY"
                    ERROR_EMV_RESULT_NOAPP -> "ERROR_EMV_RESULT_NOAPP"
                    ERROR_EMV_RESULT_NOPUBKEY -> "ERROR_EMV_RESULT_NOPUBKEY"
                    ERROR_EMV_RESULT_EXPIRY -> "ERROR_EMV_RESULT_EXPIRY"
                    ERROR_EMV_RESULT_FLASHCARD -> "ERROR_EMV_RESULT_FLASHCARD"
                    ERROR_EMV_RESULT_STOP -> "ERROR_EMV_RESULT_STOP"
                    ERROR_EMV_RESULT_REPOWERICC -> "ERROR_EMV_RESULT_REPOWERICC"
                    ERROR_EMV_RESULT_REFUSESERVICE -> "ERROR_EMV_RESULT_REFUSESERVICE"
                    ERROR_EMV_RESULT_CARDLOCK -> "ERROR_EMV_RESULT_CARDLOCK"
                    ERROR_EMV_RESULT_APPLOCK -> "ERROR_EMV_RESULT_APPLOCK"
                    ERROR_EMV_RESULT_EXCEED_CTLMT -> "ERROR_EMV_RESULT_EXCEED_CTLMT"
                    ERROR_EMV_RESULT_APDU_ERROR -> "ERROR_EMV_RESULT_APDU_ERROR"
                    ERROR_EMV_RESULT_APDU_STATUS_ERROR -> "ERROR_EMV_RESULT_APDU_STATUS_ERROR"
                  ERROR_EMV_RESULT_KERNEL_ABSENT->"ERROR_EMV_RESULT_KERNEL_ABSENT"
                    ERROR_EMV_RESULT_ALL_FLASH_CARD -> "ERROR_EMV_RESULT_ALL_FLASH_CARD"
                    EMV_RESULT_AMOUNT_EMPTY -> "EMV_RESULT_AMOUNT_EMPTY"
                    else -> "unknow error"
                }
                lifecycleScope.launch(Dispatchers.Main) {
                   alertBoxWithActionNew(
                       getString(R.string.transaction_delined_msg),
                       "onEndProcessCalled $result --> $errorMsg",
                       R.drawable.ic_txn_declined,
                       getString(R.string.positive_button_ok),
                       "", false, false,
                       { alertPositiveCallback ->
                           if (alertPositiveCallback) {
                               goToDashBoard()
                           }
                       },
                       {})
                   Log.e("onEndProcessCalled", "Error in onEndProcessCalled Result --> $result")
               }
            }
        }

    }
    //endregion

    private fun detectCard(cardProcessedDataModal: CardProcessedDataModal, cardOption: CardOption) {
        defaultScope.launch {
            SearchCard(cardProcessedDataModal,cardOption, cardDetectionCb)
        }

    }

    // Creating transaction packet and
    private fun emvProcessNext(cardProcessedData: CardProcessedDataModal) {
        val transactionISO = CreateTransactionPacketNew(
            appDao,
            emiSelectedData,
            emiTAndCData,
            brandEMIData,
            cardProcessedData,
            BatchTable()
        ).createTransactionPacketNew()
        cardProcessedData.indicatorF58 = transactionISO.additionalData["indicatorF58"] ?: ""

        // logger("Transaction REQUEST PACKET --->>", transactionISO.isoMap, "e")
        //  runOnUiThread { showProgress(getString(R.string.sale_data_sync)) }
        lifecycleScope.launch(Dispatchers.IO) {
            checkReversal(transactionISO, cardProcessedData)
        }
    }

    private suspend fun checkReversal(
        transactionISOByteArray: IsoDataWriter,
        cardProcessedDataModal: CardProcessedDataModal
    ) {
        //runOnUiThread {
        // cardView_l.visibility = View.GONE
        //}
        // If case Sale data sync to server
        Log.e("1REVERSAL obj ->", "" + AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))
        println(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))
        val reversalObj = AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY)
        println(reversalObj)
        println("AppPreference.getReversal()" + AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))
        if (TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
            println("sale_data_sync")
            if (isShowProgress()) {
                withContext(Dispatchers.Main) { setProgressTitle(getString(R.string.authenticating_transaction_msg)) }
            } else {
                val msg: String = getString(R.string.authenticating_transaction_msg)
                withContext(Dispatchers.Main) { showProgress(msg) }
            }

            logger("1testVFEmvHandler", ("" + emvProcessHandler), "e")
            SyncTransactionToHost(
                transactionISOByteArray,
                cardProcessedDataModal,
                emvProcessHandler
            ) { syncStatus, responseCode, transactionMsg, printExtraData, de55, doubletap ->
                Log.e("hideProgress", "2")
                hideProgress()

                if (syncStatus) {
                    val responseIsoData: IsoDataReader = readIso(transactionMsg.toString(), false)
                    val autoSettlementCheck =
                        responseIsoData.isoMap[60]?.parseRaw2String().toString()
                    if (syncStatus && responseCode == "00" && !AppPreference.getBoolean(
                            AppPreference.ONLINE_EMV_DECLINED))
                        {
                        //Below we are saving batch data and print the receipt of transaction:-

                        /*GlobalScope.launch(Dispatchers.Main) {
                            Toast.makeText(this@TransactionActivity,"TXn approved",Toast.LENGTH_SHORT).show()
                        }*/

                        lifecycleScope.launch(Dispatchers.Main) {

                            val transactionDate = dateFormaterNew(
                                cardProcessedDataModal.getTimeStamp()?.toLong() ?: 0L
                            )
                            val transactionTime =
                                timeFormaterNew(cardProcessedDataModal.getTime() ?: "")
                            txnApprovedDialog(
                                transactionTypeEDashboardItem.res,
                                transactionTypeEDashboardItem.title,
                                txnAmountAfterApproved,
                                "${transactionDate}, $transactionTime"
                            ) { status, dialog ->

                                StubBatchData(
                                    de55,
                                    cardProcessedDataModal.getTransType(),
                                    cardProcessedDataModal,
                                    printExtraData,
                                    autoSettlementCheck
                                ) { stubbedData ->
                                    if (cardProcessedDataModal.getTransType() == BhTransactionType.EMI_SALE.type ||
                                        cardProcessedDataModal.getTransType() == BhTransactionType.BRAND_EMI.type ||
                                        cardProcessedDataModal.getTransType() == BhTransactionType.BRAND_EMI_BY_ACCESS_CODE.type ||
                                        cardProcessedDataModal.getTransType() == BhTransactionType.FLEXI_PAY.type ||
                                        cardProcessedDataModal.getTransType() == BhTransactionType.TEST_EMI.type

                                    ) {

                                        stubEMI(
                                            stubbedData,
                                            emiSelectedData,
                                            emiTAndCData,
                                            brandEMIData/*, brandEMIAccessData*/
                                        ) { data ->
                                            Log.d("StubbedEMIData:- ", data.toString())

                                            printAndSaveBatchDataInDB(stubbedData) {

                                                if (it) {
                                                    AppPreference.saveLastReceiptDetails(stubbedData)
                                                    Log.e("EMI ", "COMMENT ******")
                                                    lifecycleScope.launch(Dispatchers.Main) {
                                                        dialog.dismiss()
                                                    }

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
                                    } else {
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
                        }


                    }
                    else  {
                        lifecycleScope.launch(Dispatchers.Main) {
                            var msg =""
                            msg = if(AppPreference.getBoolean(AppPreference.ONLINE_EMV_DECLINED)){
                                getString(R.string.emv_declined)
                            }else{
                                responseIsoData.isoMap[58]?.parseRaw2String().toString()
                            }
                            alertBoxWithActionNew(
                                getString(R.string.transaction_delined_msg),
                                msg,
                                R.drawable.ic_txn_declined,
                                getString(R.string.positive_button_ok),
                                "", false, false,
                                { alertPositiveCallback ->
                                    if (alertPositiveCallback) {
                                        /*if (!TextUtils.isEmpty(autoSettlementCheck)) {
                                            syncOfflineSaleAndAskAutoSettlement(
                                                autoSettlementCheck.substring(0, 1)
                                            )
                                        } else {*/
                                        checkForPrintReversalReceipt(
                                            this@TransactionActivity,
                                            autoSettlementCheck
                                        ) {
                                            goToDashBoard()
                                        }
                                    }
                                },
                                {})
                        }
                    }


                }
                else {

                    runOnUiThread { hideProgress() }
                    //below condition is for print reversal receipt if reversal is generated
                    // and also check is need to printed or not(backend enable disable)
                    checkForPrintReversalReceipt(this, "") {
                        logger("ReversalReceipt", it, "e")
                    }

                    if (ConnectionError.NetworkError.errorCode.toString() == responseCode) {
                        runOnUiThread {
                            alertBoxWithActionNew(
                                getString(R.string.network),
                                getString(R.string.network_error),
                                R.drawable.ic_info_orange,
                                getString(R.string.positive_button_ok),
                                "", false, false,
                                { alertPositiveCallback ->
                                    if (alertPositiveCallback)
                                        declinedTransaction()
                                },
                                {})
                        }
                    } else if (ConnectionError.ConnectionTimeout.errorCode.toString() == responseCode) {
                        runOnUiThread {
                            alertBoxWithActionNew(
                                getString(R.string.error_hint),
                                getString(R.string.connection_error),
                                R.drawable.ic_info_orange,
                                getString(R.string.positive_button_ok),
                                "", false,
                                false,
                                { alertPositiveCallback ->
                                    if (alertPositiveCallback)
                                        declinedTransaction()
                                },
                                {})
                        }
                    } else {
                        runOnUiThread {
                            alertBoxWithActionNew(
                                transactionMsg ?: getString(R.string.transaction_failed_msg),
                                "",
                                R.drawable.ic_info_new,
                                "OK",
                                "",
                                false,
                                false,
                                { declinedTransaction() },
                                {})
                        }
                    }


                }

            }
        } else {
            println("410")
            if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
                println("412")
                if (isShowProgress()) {
                    withContext(Dispatchers.Main) { setProgressTitle(getString(R.string.reversal_data_sync)) }
                } else {
                    val msg: String = getString(R.string.reversal_data_sync)
                    withContext(Dispatchers.Main) { showProgress(msg) }
                }
                //runOnUiThread { showProgress(getString(R.string.reversal_data_sync)) }
                SyncReversalToHost(AppPreference.getReversalNew()) { isSyncToHost, transMsg ->
                    Log.e("hideProgress", "1")
                    //  hideProgress()
                    if (isSyncToHost) {
                        AppPreference.clearReversal()

                        lifecycleScope.launch(Dispatchers.IO) {
                            checkReversal(transactionISOByteArray, cardProcessedDataModal)
                        }
                        println("clearReversal -> check again")
                    } else {
                        lifecycleScope.launch(Dispatchers.Main) {
                            //  VFService.showToast(transMsg)
                            alertBoxWithActionNew(
                                getString(R.string.reversal_upload_fail),
                                getString(R.string.transaction_delined_msg),
                                R.drawable.ic_txn_declined,
                                getString(R.string.positive_button_ok), "",
                                false, false,
                                { alertPositiveCallback ->
                                    if (alertPositiveCallback)
                                        declinedTransaction()
                                },
                                {})


                        }
                    }
                }
            } else {
                println("442")
            }
        }
        //Else case is to Sync Reversal data Packet to Host:-

    }

    //Below method is used to handle Transaction Declined case:-
    fun declinedTransaction() {
        try {
            hideProgress()
            DeviceHelper.getEMV()?.stopSearch()
            //    finish()
            startActivity(Intent(this, NavigationActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        } catch (ex: java.lang.Exception) {
            //    finish()
            hideProgress()
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
        //Here we are saving printerReceiptData in BatchFileData Table:-
        if (stubbedData.transactionType != BhTransactionType.PRE_AUTH.type) {
            Utility().saveTempBatchFileDataTable(stubbedData)
        }

        lifecycleScope.launch(Dispatchers.Main) {
            showProgress(getString(R.string.printing))
            var printsts = false
            if (stubbedData != null) {
                PrintUtil(this@TransactionActivity as BaseActivityNew).startPrinting(
                    stubbedData,
                    EPrintCopyType.MERCHANT,
                    this@TransactionActivity as BaseActivityNew
                ) { printCB, printingFail ->
                    Log.e("hideProgress", "3")
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
        /* withContext(Dispatchers.Main) {
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
         }*/

        withContext(Dispatchers.Main) {
            val printerUtil: PrintUtil? = null
            (this@TransactionActivity as BaseActivityNew).alertBoxWithActionNew("",
                getString(R.string.print_customer_copy),
                R.drawable.ic_print_customer_copy,
                getString(R.string.positive_button_yes),
                getString(R.string.no),
                true,
                false,
                { status ->
                    showProgress(getString(R.string.printing))
                    PrintUtil(this@TransactionActivity as BaseActivityNew).startPrinting(
                        batchTable,
                        EPrintCopyType.CUSTOMER,
                        this@TransactionActivity as BaseActivityNew
                    ) { printCB, printingFail ->
                        Log.e("hideProgress", "4")
                        (this@TransactionActivity as BaseActivityNew).hideProgress()
                        if (printCB) {
                            lifecycleScope.launch(Dispatchers.IO) {
                                cb(printCB)
                            }
                            Log.e("hideProgress", "5")
                            (this@TransactionActivity as BaseActivityNew).hideProgress()

                        }

                    }
                },
                {
                    lifecycleScope.launch(Dispatchers.IO) {
                        cb(true)
                    }
                    Log.e("hideProgress", "6")
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

    fun handleEMVFallbackFromError(
        title: String,
        msg: String,
        showCancelButton: Boolean,
        emvFromError: (Boolean) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.Main) {
            alertBoxWithActionNew(title,
                msg,
                R.drawable.ic_txn_declined,
                getString(R.string.positive_button_ok),
                "Cancel",
                showCancelButton,
                false,
                { alertCallback ->
                    if (alertCallback) {
                        emvFromError(true)
                    }
                },
                {})
        }
    }

    enum class CardErrorCode(var errorCode: Int) {
        EMV_FALLBACK_ERROR_CODE(40961),
        CTLS_ERROR_CODE(6)
    }

    enum class EFallbackCode(var fallBackCode: Int) {
        Swipe_fallback(111),
        EMV_fallback(40961),
        NO_fallback(0),
        EMV_fallbackNew(12),
        CTLS_fallback(333)
    }

    fun respondCVMResult(result: Byte) {
        try {
            val chvStatus = TLV.fromData(EMVTag.DEF_TAG_CHV_STATUS, byteArrayOf(result))
            val ret = DeviceHelper.getEMV()!!.respondEvent(chvStatus.toString())
            println("...onCardHolderVerify: respondEvent$ret")

        } catch (e: java.lang.Exception) {
            //handleException(e);
        }
    }
}