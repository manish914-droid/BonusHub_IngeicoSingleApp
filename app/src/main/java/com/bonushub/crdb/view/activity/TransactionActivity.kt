package com.bonushub.crdb.view.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.DeadObjectException
import android.os.RemoteException
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
import com.bonushub.crdb.utils.PrintingTesting
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
import com.usdk.apiservice.aidl.printer.*


import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import java.io.InputStream
import java.text.ParseException
import java.text.SimpleDateFormat
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

               val intent = Intent (this, TenureSchemeActivity::class.java)
               startActivity(intent)

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
                searchCardViewModel.fetchCardTypeData()
                setupObserver()
              /*  val intent = Intent (this, TenureSchemeActivity::class.java)
                startActivity(intent)*/
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
                                        if (receiptDetail != null) {
                                            startPrinting(receiptDetail)
                                        }


                                    }
                                    ResponseCode.FAILED.value,
                                    ResponseCode.ABORTED.value -> {
                                      //  detailResponse.forEach { println(it) }
                                       /* if (receiptDetail != null) {
                                            val jsonstr="{\"aid\":\"A0000000041010\",\"appName\":\"Debit MasterCard\",\"authCode\":\"006538\",\"batchNumber\":\"000001\",\"cardHolderName\":\"INSTA DEBIT CARD         /\",\"cardType\":\"UP        \",\"cvmRequiredLimit\":0,\"cvmResult\":\"NO_CVM\",\"dateTime\":\"24/11/2021 14:49:00\",\"entryMode\":\"INSERT\",\"invoice\":\"000012\",\"isSignRequired\":false,\"isVerifyPin\":true,\"merAddHeader1\":\"INGBH TEST2 TID\",\"merAddHeader2\":\"NOIDA\",\"mid\":\"               \",\"rrn\":\"000000000381\",\"stan\":\"000381\",\"tc\":\"1DF19BD576739835\",\"tid\":\"30160035\",\"tsi\":\"E800\",\"tvr\":\"0840048000\",\"txnAmount\":\"5888\",\"txnName\":\"SALE\",\"txnResponseCode\":\"00\"}"
                                           val obj=Gson().fromJson(jsonstr,ReceiptDetail::class.java)
                                           startPrinting(obj)
                                           *//* val intent=Intent(this@TransactionActivity,PrintingTesting::class.java)
                                            startActivity(intent)*//*

                                        }*/
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
    private var printer: UPrinter? = null
    private fun startPrinting(receiptDetail: ReceiptDetail) {
        //  printer=null
        printer = DeviceHelper.getPrinter()
        try {
            //  logger("PS_START", (printer?.status).toString(), "e")
            // Changes By manish Kumar
            //If in Respnse field 60 data comes Auto settle flag | Bank id| Issuer id | MID | TID | Batch No | Stan | Invoice | Card Type
            // then show response data otherwise show data available in database
            //From mid to hostMID (coming from field 60)
            //From tid to hostTID (coming from field 60)
            //From batchNumber to hostBatchNumber (coming from field 60)
            //From roc to hostRoc (coming from field 60)
            //From invoiceNumber to hostInvoice (coming from field 60)
            //From cardType to hostCardType (coming from field 60)
            // bundle format for addText

            val image: ByteArray? = readAssetsFile(this, "hdfc_print_logo.bmp")
            printer!!.addBmpImage(0, FactorMode.BMP1X1, image)
            val format = Bundle()
            // bundle formate for AddTextInLine
            val fmtAddTextInLine = Bundle()
            //   printLogo("hdfc_print_logo.bmp")
            // 打印行混合文本 Print mix text on the same line
            val textBlockList: ArrayList<Bundle> = ArrayList()
            fmtAddTextInLine.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            fmtAddTextInLine.putInt(PrinterData.ASC_SIZE, ASCSize.DOT24x8)
            format.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            format.putInt(PrinterData.ASC_SIZE, ASCSize.DOT24x8)
            val formattertime = receiptDetail.dateTime
            fmtAddTextInLine.putString(PrinterData.TEXT, "DATE:${"24/11/2021"}")
            fmtAddTextInLine.putInt(PrinterData.ALIGN_MODE, AlignMode.LEFT)
            textBlockList.add(fmtAddTextInLine)
            try {
                format.putString(PrinterData.TEXT, "TIME:${"14:49:00"}")
                format.putInt(PrinterData.ALIGN_MODE, AlignMode.RIGHT)
                textBlockList.add(format)
                printer!!.addMixStyleText(textBlockList)
            } catch (e: ParseException) {
                e.printStackTrace()
            }
            textBlockList.clear()
            fmtAddTextInLine.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            fmtAddTextInLine.putInt(PrinterData.ASC_SIZE, ASCSize.DOT24x8)
            format.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            format.putInt(PrinterData.ASC_SIZE, ASCSize.DOT24x8)
            fmtAddTextInLine.putString(PrinterData.TEXT, "MID:${receiptDetail.mid}")
            fmtAddTextInLine.putInt(PrinterData.ALIGN_MODE, AlignMode.LEFT)
            textBlockList.add(fmtAddTextInLine)
            format.putString(PrinterData.TEXT,   "TID:${receiptDetail.tid}")
            format.putInt(PrinterData.ALIGN_MODE, AlignMode.RIGHT)
            textBlockList.add(format)
            printer!!.addMixStyleText(textBlockList)
            textBlockList.clear()
            fmtAddTextInLine.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            fmtAddTextInLine.putInt(PrinterData.ASC_SIZE, ASCSize.DOT24x8)
            format.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            format.putInt(PrinterData.ASC_SIZE, ASCSize.DOT24x8)
            fmtAddTextInLine.putString(PrinterData.TEXT, "BATCH NO:${receiptDetail.batchNumber}")
            fmtAddTextInLine.putInt(PrinterData.ALIGN_MODE, AlignMode.LEFT)
            textBlockList.add(fmtAddTextInLine)
            format.putString(PrinterData.TEXT,   "ROC:${receiptDetail.stan}")
            format.putInt(PrinterData.ALIGN_MODE, AlignMode.RIGHT)
            textBlockList.add(format)
            printer!!.addMixStyleText(textBlockList)
            textBlockList.clear()
            fmtAddTextInLine.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            fmtAddTextInLine.putInt(PrinterData.ASC_SIZE, ASCSize.DOT24x8)
            format.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            format.putInt(PrinterData.ASC_SIZE, ASCSize.DOT24x8)
            format.putString(PrinterData.TEXT, "INVOICE:${receiptDetail.invoice}")
            textBlockList.add(format)
            printer!!.addMixStyleText(textBlockList)
            printer!!.setHzScale(HZScale.SC1x2)
            printer!!.setHzSize(HZSize.DOT24x24)
            printer!!.addText(AlignMode.CENTER, receiptDetail.txnName)

            textBlockList.clear()
            fmtAddTextInLine.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            fmtAddTextInLine.putInt(PrinterData.ASC_SIZE, ASCSize.DOT24x8)
            format.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            format.putInt(PrinterData.ASC_SIZE, ASCSize.DOT24x8)
            fmtAddTextInLine.putString(PrinterData.TEXT, "CARD TYPE:${receiptDetail.appName}")
            fmtAddTextInLine.putInt(PrinterData.ALIGN_MODE, AlignMode.LEFT)
            textBlockList.add(fmtAddTextInLine)
            format.putString(PrinterData.TEXT,   "EXP:XX/XX")
            format.putInt(PrinterData.ALIGN_MODE, AlignMode.RIGHT)
            textBlockList.add(format)
            printer!!.addMixStyleText(textBlockList)

            textBlockList.clear()
            fmtAddTextInLine.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            fmtAddTextInLine.putInt(PrinterData.ASC_SIZE, ASCSize.DOT24x8)
            format.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            format.putInt(PrinterData.ASC_SIZE, ASCSize.DOT24x8)
            fmtAddTextInLine.putString(PrinterData.TEXT, "CARD NO:${"00000gl3790"}")
            fmtAddTextInLine.putInt(PrinterData.ALIGN_MODE, AlignMode.LEFT)
            textBlockList.add(fmtAddTextInLine)
            format.putString(PrinterData.TEXT,   "Chip")
            format.putInt(PrinterData.ALIGN_MODE, AlignMode.RIGHT)
            textBlockList.add(format)
            printer!!.addMixStyleText(textBlockList)

            textBlockList.clear()
            fmtAddTextInLine.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            fmtAddTextInLine.putInt(PrinterData.ASC_SIZE, ASCSize.DOT24x8)
            format.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            format.putInt(PrinterData.ASC_SIZE, ASCSize.DOT24x8)
            fmtAddTextInLine.putString(PrinterData.TEXT, "AUTH CODE:${receiptDetail.authCode}")
            fmtAddTextInLine.putInt(PrinterData.ALIGN_MODE, AlignMode.LEFT)
            textBlockList.add(fmtAddTextInLine)
            format.putString(PrinterData.TEXT,   "RRN:${receiptDetail.rrn}")
            format.putInt(PrinterData.ALIGN_MODE, AlignMode.RIGHT)
            textBlockList.add(format)
            printer!!.addMixStyleText(textBlockList)

            textBlockList.clear()
            fmtAddTextInLine.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            fmtAddTextInLine.putInt(PrinterData.ASC_SIZE, ASCSize.DOT24x8)
            format.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            format.putInt(PrinterData.ASC_SIZE, ASCSize.DOT24x8)
            fmtAddTextInLine.putString(PrinterData.TEXT, "TVR:${receiptDetail.tvr}")
            fmtAddTextInLine.putInt(PrinterData.ALIGN_MODE, AlignMode.LEFT)
            textBlockList.add(fmtAddTextInLine)
            format.putString(PrinterData.TEXT,   "TSI:${receiptDetail.tsi}")
            format.putInt(PrinterData.ALIGN_MODE, AlignMode.RIGHT)
            textBlockList.add(format)
            printer!!.addMixStyleText(textBlockList)

            printer!!.setHzScale(HZScale.SC1x1)
            printer!!.setHzSize(HZSize.DOT24x24)
            printer!!.addText(AlignMode.CENTER, "...................................")

            textBlockList.clear()
            fmtAddTextInLine.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            fmtAddTextInLine.putInt(PrinterData.ASC_SIZE, ASCSize.DOT24x8)
            format.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            format.putInt(PrinterData.ASC_SIZE, ASCSize.DOT24x8)
            fmtAddTextInLine.putString(PrinterData.TEXT, "SALE AMOUNT:")
            fmtAddTextInLine.putInt(PrinterData.ALIGN_MODE, AlignMode.LEFT)
            textBlockList.add(fmtAddTextInLine)
            format.putString(PrinterData.TEXT,   "INR:${receiptDetail.txnAmount}")
            format.putInt(PrinterData.ALIGN_MODE, AlignMode.RIGHT)
            textBlockList.add(format)
            printer!!.addMixStyleText(textBlockList)



            textBlockList.clear()
            fmtAddTextInLine.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            fmtAddTextInLine.putInt(PrinterData.ASC_SIZE, ASCSize.DOT24x8)
            format.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            format.putInt(PrinterData.ASC_SIZE, ASCSize.DOT24x8)
            fmtAddTextInLine.putString(PrinterData.TEXT, "TIP AMOUNT:${""}")
            fmtAddTextInLine.putInt(PrinterData.ALIGN_MODE, AlignMode.LEFT)
            textBlockList.add(fmtAddTextInLine)
            format.putString(PrinterData.TEXT,   "00:${""}")
            format.putInt(PrinterData.ALIGN_MODE, AlignMode.RIGHT)
            textBlockList.add(format)
            printer!!.addMixStyleText(textBlockList)


            textBlockList.clear()
            fmtAddTextInLine.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            fmtAddTextInLine.putInt(PrinterData.ASC_SIZE, ASCSize.DOT24x8)
            format.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
            format.putInt(PrinterData.ASC_SIZE, ASCSize.DOT24x8)
            fmtAddTextInLine.putString(PrinterData.TEXT, "TOTAL AMOUNT:${""}")
            fmtAddTextInLine.putInt(PrinterData.ALIGN_MODE, AlignMode.LEFT)
            textBlockList.add(fmtAddTextInLine)
            format.putString(PrinterData.TEXT,   "INR:${receiptDetail.txnAmount}")
            format.putInt(PrinterData.ALIGN_MODE, AlignMode.RIGHT)
            textBlockList.add(format)
            printer!!.addMixStyleText(textBlockList)


            printer!!.setHzScale(HZScale.SC1x1)
            printer!!.setHzSize(HZSize.DOT24x24)
            printer!!.addText(AlignMode.CENTER, "...................................")

            printer!!.setHzScale(HZScale.SC1x1)
            printer!!.setHzSize(HZSize.DOT24x24)
            printer!!.addText(AlignMode.CENTER, "PIN VERIFIDE OK")

            printer!!.setHzScale(HZScale.SC1x1)
            printer!!.setHzSize(HZSize.DOT24x24)
            printer!!.addText(AlignMode.CENTER, "SIGNATURE NOT REQUIRED")





            printer!!.setHzScale(HZScale.SC1x1)
            printer!!.setHzSize(HZSize.DOT24x24)
            printer!!.addText(AlignMode.LEFT, "I am satisfied with goods recived and agree to pay issuer agreenent.")


            printer!!.setHzScale(HZScale.SC1x1)
            printer!!.setHzSize(HZSize.DOT24x24)
            printer!!.addText(AlignMode.CENTER, "*thank yoy visit again*")

            printer!!.setHzScale(HZScale.SC1x1)
            printer!!.setHzSize(HZSize.DOT24x24)
            printer!!.addText(AlignMode.CENTER, receiptDetail.cardHolderName)
            val bhlogo: ByteArray? = readAssetsFile(this, "BH.bmp")
            printer!!.addBmpImage(0, FactorMode.BMP1X1, bhlogo)
            printer!!.setPrnGray(3)
            printer!!.feedLine(5)
            printer!!.startPrint(object : OnPrintListener.Stub() {
                @Throws(RemoteException::class)
                override fun onFinish() {


                }

                @Throws(RemoteException::class)
                override fun onError(error: Int) {

                }
            })


        } catch (ex: DeadObjectException) {
            ex.printStackTrace()
        }
    }

    private fun readAssetsFile(ctx: Context, fileName: String): ByteArray? {
        var input: InputStream? = null
        return try {
            input = ctx.assets.open(fileName)
            val buffer = ByteArray(input.available())
            input.read(buffer)
            buffer
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } finally {
            if (input != null) {
                try {
                    input.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
    fun dateFormater(date: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)

    fun timeFormater(date: Long): String =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(date)
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