package com.bonushub.crdb.india.utils.printerUtils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.DeadObjectException
import android.os.Message
import android.os.RemoteException
import android.text.TextUtils
import android.util.Log
import com.bonushub.crdb.india.BuildConfig
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.model.local.*
import com.bonushub.crdb.india.model.local.AppPreference.AMEX_BANK_CODE
import com.bonushub.crdb.india.model.remote.BankEMIIssuerTAndCDataModal
import com.bonushub.crdb.india.model.remote.BankEMITenureDataModal
import com.bonushub.crdb.india.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.india.model.remote.BrandEMIProductDataModal
import com.bonushub.crdb.india.transactionprocess.StubBatchData
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getBrandTAndCData
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getBrandTAndCDataByBrandId
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getIssuerTAndCDataByIssuerId
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getTptData
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.panMasking
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.transactionType2Name
import com.bonushub.crdb.india.view.base.BaseActivityNew

import com.bonushub.crdb.india.utils.EPrintCopyType
import com.bonushub.crdb.india.utils.BhTransactionType
import com.bonushub.crdb.india.utils.EDashboardItem
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getHDFCTptData
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getInitdataList
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.selectDigiPosDataAccordingToTxnStatus
import com.bonushub.crdb.india.utils.SplitterTypes
import com.bonushub.crdb.india.view.fragments.pre_auth.PendingPreauthData
import com.google.gson.Gson
import com.ingenico.hdfcpayment.model.ReceiptDetail
import com.usdk.apiservice.aidl.printer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.io.InputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

const val HDFC_BANK_CODE = "01"
const val HDFC_BANK_CODE_SINGLE_DIGIT = "1"
const val AMEX_BANK_CODE_SINGLE_DIGIT = "2"
const val AMEX_BANK_CODE = "02"
const val DEFAULT_BANK_CODE = HDFC_BANK_CODE
const val HDFC_LOGO = "hdfc_print_logo.bmp"
const val AMEX_LOGO = "amex_print.bmp"

class PrintUtil(context: Context?) {
    private var printer: UPrinter? = null
    private var isTipAllowed = false
    private var context: Context? = null
    private var footerText = arrayOf("*Thank You Visit Again*", "POWERED BY")
    private val textBlockList: ArrayList<Bundle> = ArrayList()
    private var _issuerName: String? = null
    private var copyType: String? = null
    private var _issuerNameString = "ISSUER"
    private var isNoEmiOnlyCashBackApplied: Boolean? = false
    private val bankEMIFooterTAndCSeparator = "~!emi~~brd~~!brd~~iss~"
    var nextLineAppendStr = ""
    init {
        this.context = context
        try {
            printer = DeviceHelper.getPrinter()
            if (printer?.status == 0) {
                logger("PrintInit->", "Called Printing", "e")
                logger("PrintUtil->", "Printer Status --->  ${printer?.status}", "e")
                val terminalData = getTptData()
                isTipAllowed = terminalData?.tipProcessing == "1"
            } else {
                //   throw Exception()
                logger("PrintUtil", "Error in printer status --->  ${printer?.status}", "e")
                ToastUtils.showToast(context,getErrorMessage(printer?.status?:0))

            }
        } catch (ex: DeadObjectException) {
            ex.printStackTrace()
            logger("PrintUtil", "DEAD OBJECT EXCEPTION", "e")
            //  VFService.showToast(".... TRY AGAIN ....")

            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take charge slip from the Report menu."
            )

        } catch (e: RemoteException) {
            e.printStackTrace()
            logger("PrintUtil", "REMOTE EXCEPTION", "e")
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take charge slip from the Report menu."
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            logger("PrintUtil", "EXCEPTION", "e")
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take charge slip from the Report menu."
            )
        } finally {

        }

    }

    @SuppressLint("SimpleDateFormat")
    fun startPrinting(
        batchTable: BatchTable, copyType: EPrintCopyType,
        context: Context?, isReversal: Boolean = false,
        printerCallback: (Boolean, Int) -> Unit
    ) {
        try {
            val receiptDetail: ReceiptDetail = batchTable.receiptData ?: ReceiptDetail()
            val bankEMITenureDataModal: BankEMITenureDataModal? = batchTable.emiTenureDataModel
            val bankEMIIssuerTAndCDataModal: BankEMIIssuerTAndCDataModal? = batchTable.emiIssuerDataModel
            val isNoEmiOnlyCashBackApplied : Boolean =  bankEMITenureDataModal?.tenure=="1"
            //setLogoAndHeader()
            val terminalData = getTptData()
            try {
//                receiptDetail.merAddHeader1?.let { sigleLineText(it, AlignMode.CENTER) }
//                receiptDetail.merAddHeader2?.let { sigleLineText(it, AlignMode.CENTER) }
                headerPrinting()

                val date = receiptDetail.dateTime
                val parts = date?.split(" ")
                println("Date: " + parts!![0])
                println("Time: " + (parts[1]))

                textBlockList.add(sigleLineformat("DATE:${parts[0]}", AlignMode.LEFT))
                textBlockList.add(sigleLineformat("TIME:${(parts[1])}", AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

                textBlockList.add(sigleLineformat("MID:${terminalData?.merchantId}", AlignMode.LEFT))
                textBlockList.add(sigleLineformat("TID:${receiptDetail.tid}", AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

                textBlockList.add(
                    sigleLineformat(
                        "BATCH NO:${receiptDetail.batchNumber}",
                        AlignMode.LEFT
                    )
                )
                textBlockList.add(sigleLineformat("ROC:${receiptDetail.stan}", AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

                textBlockList.add(
                    sigleLineformat(
                        "INVOICE:${receiptDetail.invoice}",
                        AlignMode.LEFT
                    )
                )
                if (batchTable.transactionType == BhTransactionType.BRAND_EMI.type)
                    textBlockList.add(
                        sigleLineformat(
                            "M.BILL NO:${batchTable.billNumber}",
                            AlignMode.RIGHT
                        )
                    )
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()
                val isNoEmiOnlyCashBackAppl : Boolean =  bankEMITenureDataModal?.tenure=="1"

                if (isReversal) {
                    sigleLineText("TRANSACTION FAILED", AlignMode.CENTER)
                } else {
                    if(isNoEmiOnlyCashBackAppl) {
                        sigleLineText("SALE", AlignMode.CENTER)
                    }
                    else{
                        getTransactionTypeName(batchTable.transactionType)?.let { sigleLineText(it, AlignMode.CENTER) }
                    }
                }


               if(!receiptDetail.appName.isNullOrEmpty()){
                textBlockList.add(
                    sigleLineformat(
                        "CARD TYPE:${receiptDetail.appName}",
                        AlignMode.LEFT
                    )
                )}
                else{
                   textBlockList.add(
                       sigleLineformat(
                           "CARD TYPE:${receiptDetail.cardType?.trim()}",
                           AlignMode.LEFT
                       ))
               }
                textBlockList.add(sigleLineformat("EXP:XX/XX", AlignMode.RIGHT))

                printer?.addMixStyleText(textBlockList)

                textBlockList.clear()


                textBlockList.add(
                    sigleLineformat(
                        "CARD NO:${receiptDetail.maskedPan}",
                        AlignMode.LEFT
                    )
                )
                receiptDetail.entryMode?.let { sigleLineformat(it, AlignMode.RIGHT) }?.let {
                    textBlockList.add(
                        it
                    )
                }

                printer?.addMixStyleText(textBlockList)

                textBlockList.clear()


                textBlockList.add(
                    sigleLineformat(
                        "AUTH CODE:${receiptDetail.authCode}",
                        AlignMode.LEFT
                    )
                )
                textBlockList.add(sigleLineformat("RRN:${receiptDetail.rrn}", AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)

                textBlockList.clear()
                if (!(receiptDetail.tvr.isNullOrEmpty()) )
                textBlockList.add(sigleLineformat("TVR:${receiptDetail.tvr}", AlignMode.LEFT))
                if (!(receiptDetail.tsi.isNullOrEmpty()) )
                textBlockList.add(sigleLineformat("TSI:${receiptDetail.tsi}", AlignMode.RIGHT))
                if(!(receiptDetail.tvr.isNullOrEmpty()) || !(receiptDetail.tsi.isNullOrEmpty()))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()
                if (!(receiptDetail.aid.isNullOrEmpty()))
                textBlockList.add(sigleLineformat("AID:${receiptDetail.aid}", AlignMode.LEFT))
                if (!(receiptDetail.tc.isNullOrEmpty()))
                textBlockList.add(sigleLineformat("TC:${receiptDetail.tc}", AlignMode.RIGHT))
                if(!(receiptDetail.tc.isNullOrEmpty()) || !(receiptDetail.aid.isNullOrEmpty()))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

                if (isReversal) {
                    textBlockList.add(
                        sigleLineformat(
                            "AID:${receiptDetail.aid}",
                            AlignMode.LEFT
                        )
                    )
                    printer?.addMixStyleText(textBlockList)

                    textBlockList.clear()
                }

                printSeperator()
                var txnName = receiptDetail.txnName

                if (isReversal) {
                    txnName = "REVERSAL"
                }

                when (batchTable.transactionType) {
                    BhTransactionType.SALE.type, BhTransactionType.CASH_AT_POS.type, BhTransactionType.SALE_WITH_CASH.type -> {
                        saleTransaction(batchTable)
                    }
//                    BhTransactionType.EMI_SALE.type , BhTransactionType.TEST_EMI.type, BhTransactionType.BRAND_EMI.type -> {
//                        printEMISale(batchTable)
//
//                   }

                    BhTransactionType.REVERSAL.type -> {
                        val amt = (((receiptDetail.txnAmount)?.toLong())?.div(100)).toString()
                        textBlockList.add(sigleLineformat("TOTAL AMOUNT:", AlignMode.LEFT))
                        textBlockList.add(
                            sigleLineformat(
                                "INR:${"%.2f".format(amt.toDouble())}",
                                AlignMode.RIGHT
                            )
                        )
                        printer?.addMixStyleText(textBlockList)
                        textBlockList.clear()

                    }
                    else -> {
                       // voidTransaction(receiptDetail) /// it is not use now

                    }
                }
                printSeperator()
                //region=====================BRAND TAndC===============
                /*if (batchTable.transactionType == BhTransactionType.EMI_SALE.type ||batchTable.transactionType == BhTransactionType.TEST_EMI.type||batchTable.transactionType == BhTransactionType.BRAND_EMI.type) {
                    if(!isNoEmiOnlyCashBackApplied)
                    printBrandTnC(batchTable)

                }*/
                //region=====================BRAND PRODUACT DATA===============
                /*if (batchTable.transactionType == BhTransactionType.BRAND_EMI.type) {
                    printProduactData(batchTable)
                    printSeperator()
                    baseAmounthandling(batchTable)
                }*/

                printer?.setAscScale(ASCScale.SC1x2)
                printer?.setAscSize(ASCSize.DOT16x8)

                if (receiptDetail.entryMode.equals("CLESS_EMV")) {
                    if (receiptDetail.isVerifyPin == true) {
                        sigleLineText("PIN VERIFIED OK", AlignMode.CENTER)
                    }
                    if (receiptDetail.isVerifyPin == true){
                        sigleLineText("SIGNATURE NOT REQUIRED", AlignMode.CENTER)}
                    else{
                        if (batchTable.transactionType == BhTransactionType.EMI_SALE.type ||batchTable.transactionType == BhTransactionType.TEST_EMI.type||batchTable.transactionType == BhTransactionType.BRAND_EMI.type || batchTable.transactionType == BhTransactionType.SALE.type || batchTable.transactionType == BhTransactionType.CASH_AT_POS.type || batchTable.transactionType == BhTransactionType.SALE_WITH_CASH.type || batchTable.transactionType == BhTransactionType.PRE_AUTH.type ) {
                           val data= "PIN NOT REQUIRED FOR CONTACTLESS TRANSACTION UPTO ${receiptDetail?.cvmRequiredLimit}"
                            val limit = 48
                            val chunks: List<String> = chunkTnC(data, limit)
                            for (st in chunks) {
                                logger("TNC", st, "e")
                                       textBlockList.add(
                                           sigleLineformat(
                                               st, AlignMode.LEFT
                                           )
                                       )
//                                            printer?.setHzScale(HZScale.SC1x1)
//                                            printer?.setHzSize(HZSize.DOT24x16)
                                printer?.setAscScale(ASCScale.SC1x1)
                                printer?.setAscSize(ASCSize.DOT24x8)
                                textBlockList.add(
                                    sigleLineformat(
                                        st,
                                        AlignMode.LEFT
                                    )
                                )
                                printer?.addMixStyleText(textBlockList)
                                textBlockList.clear()
                            }

                        }
                        else{
                            if (receiptDetail.isVerifyPin == true) {
                                sigleLineText("PIN VERIFIED OK", AlignMode.CENTER)
                            }
                            if (receiptDetail.isVerifyPin == true){
                                sigleLineText("SIGNATURE NOT REQUIRED", AlignMode.CENTER)}
                        }
                    }
                } else {
                    if (receiptDetail.isVerifyPin == true){
                        sigleLineText("PIN VERIFIED OK", AlignMode.CENTER)}

                    if (receiptDetail.isVerifyPin == true){
                        sigleLineText("SIGNATURE NOT REQUIRED", AlignMode.CENTER)}
                    else{
                        sigleLineText("SIGN ...................", AlignMode.CENTER)
                    }


                    receiptDetail.cardHolderName?.let { sigleLineText(it, AlignMode.CENTER) }
                }

                printer?.setAscScale(ASCScale.SC1x1)
                printer?.setAscSize(ASCSize.DOT24x8)

                try{
                    val issuerParameterTable = Field48ResponseTimestamp.getIssuerData(AppPreference.WALLET_ISSUER_ID)

                    var dec = issuerParameterTable?.walletIssuerDisclaimer

                   // textBlockList.add(sigleLineformat("I am satisfied with goods received and agree to pay issuer agreenent.", AlignMode.CENTER))
                    logger("dec",dec?:"")
                    textBlockList.add(sigleLineformat(dec?:"", AlignMode.CENTER))

                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()

                }catch (ex:Exception){
                    ex.printStackTrace()
                }

                sigleLineText(copyType.pName, AlignMode.CENTER)
                sigleLineText(footerText[0], AlignMode.CENTER)
                sigleLineText(footerText[1], AlignMode.CENTER)

                val bhlogo: ByteArray? = context?.let { printLogo(it, "BH.bmp") }
                printer?.addBmpImage(0, FactorMode.BMP1X1, bhlogo)
                sigleLineText(
                    "App Version :${BuildConfig.VERSION_NAME}",
                    AlignMode.CENTER
                )

                if(!isNoEmiOnlyCashBackAppl) {
                    val issuerId = bankEMIIssuerTAndCDataModal?.issuerID
                    val issuerTAndCData = issuerId?.let { getIssuerTAndCDataByIssuerId(it) }
                    //region=======================Issuer Footer Terms and Condition=================
                    if (!TextUtils.isEmpty(issuerTAndCData?.footerTAndC)) {
                        printSeperator()

                        val issuerFooterTAndC =
                            issuerTAndCData?.footerTAndC?.split(SplitterTypes.POUND.splitter)
                        logger("getting footer tnc1=",issuerTAndCData?.footerTAndC.toString(),"e")
                        logger("issuerFooterTAndC-->=",issuerFooterTAndC.toString(),"e")
                        if (issuerFooterTAndC != null) {
                            if (issuerFooterTAndC.isNotEmpty()) {
                                for (i in issuerFooterTAndC.indices) {
                                    if (!TextUtils.isEmpty(issuerFooterTAndC[i])) {
                                        val limit = 48
                                        val emiTnc = "#" + issuerFooterTAndC[i]
                                        val chunks: List<String> = chunkTnC(emiTnc, limit)
                                        for (st in chunks) {
                                            logger("TNC", st, "e")
                                            textBlockList.add(
                                                sigleLineformat(
                                                    st, AlignMode.LEFT
                                                )
                                            )
//                                            printer?.setHzScale(HZScale.SC1x1)
//                                            printer?.setHzSize(HZSize.DOT24x16)
                                            printer?.setAscScale(ASCScale.SC1x1)
                                            printer?.setAscSize(ASCSize.DOT24x8)
                                            printer?.addText( AlignMode.LEFT, st)
                                        }
                                        printer?.setAscSize(ASCSize.DOT24x12)
                                    }
                                }
                            } else {
                                textBlockList.add(
                                    sigleLineformat(
                                        "# ${issuerTAndCData.footerTAndC}", AlignMode.LEFT
                                    )
                                )
//                                printer?.setHzScale(HZScale.SC1x1)
//                                printer?.setHzSize(HZSize.DOT24x16)
                                printer?.setAscScale(ASCScale.SC1x1)
                                printer?.setAscSize(ASCSize.DOT24x8)
                                printer?.addText( AlignMode.LEFT, "# ${issuerTAndCData.footerTAndC}")
                            }
                        }
                    }
                }


            } catch (e: ParseException) {
                e.printStackTrace()
            }
            printer?.setPrnGray(3)
            printer?.feedLine(3)
            printer?.startPrint(object : OnPrintListener.Stub() {
                @Throws(RemoteException::class)
                override fun onFinish() {
                    printerCallback(true, 0)
                }

                @Throws(RemoteException::class)
                override fun onError(error: Int) {
                    printerCallback(true, 0)
                }
            })


        } catch (ex: DeadObjectException) {
            ex.printStackTrace()
        }


    }

    @SuppressLint("SimpleDateFormat")
    fun startPrinting(
        batchTable: TempBatchFileDataTable, copyType: EPrintCopyType,
        context: Context?, isReversal: Boolean = false,
        printerCallback: (Boolean, Int) -> Unit
    ) {
        try {
            val hostBankId = if (batchTable.hostBankID.isNotBlank()) {
                batchTable.hostBankID
            } else {
                batchTable.bankCode
            }

            val hostMID = if (batchTable.hostMID.isNotBlank()) {
                batchTable.hostMID
            } else {
                batchTable.mid
            }

            val hostTID = if (batchTable.hostTID.isNotBlank()) {
                batchTable.hostTID
            } else {
                batchTable.tid
            }

            val hostBatchNumber = if (batchTable.hostBatchNumber.isNotBlank()) {
                batchTable.hostBatchNumber
            } else {
                batchTable.batchNumber
            }

            val hostRoc = if (batchTable.hostRoc.isNotBlank()) {
                batchTable.hostRoc
            } else {
                batchTable.roc
            }
            val hostInvoice = if (batchTable.hostInvoice.isNotBlank()) {
                batchTable.hostInvoice
            } else {
                batchTable.invoiceNumber
            }
            val hostCardType = if (batchTable.cardType.isNotBlank()) {
                batchTable.cardType
            } else {
                batchTable.hostCardType
            }

            val isNoEmiOnlyCashBackApplied : Boolean =  batchTable?.tenure=="1"


           // val receiptDetail: ReceiptDetail = batchTable.receiptData ?: ReceiptDetail()
//            val bankEMITenureDataModal: BankEMITenureDataModal? = batchTable.emiTenureDataModel
//            val bankEMIIssuerTAndCDataModal: BankEMIIssuerTAndCDataModal? = batchTable.emiIssuerDataModel
            //setLogoAndHeader()
            //val terminalData = getTptData()
            try {
                headerPrinting(batchTable.hostBankID)


                val time = batchTable.time
                val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
                val timeFormat2 = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                var formattedTime = ""
                try {
                    val t1 = timeFormat.parse(time)
                    formattedTime = timeFormat2.format(t1)
                    Log.e("Time", formattedTime)

                    textBlockList.add(sigleLineformat("DATE:${batchTable.transactionDate}", AlignMode.LEFT))
                    textBlockList.add(sigleLineformat("TIME:${formattedTime}", AlignMode.RIGHT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()

                } catch (e: ParseException) {
                    e.printStackTrace()
                }


                textBlockList.add(sigleLineformat("MID:${hostMID}", AlignMode.LEFT))
                textBlockList.add(sigleLineformat("TID:${hostTID}", AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

                textBlockList.add(
                    sigleLineformat(
                        "BATCH NO:${hostBatchNumber}",
                        AlignMode.LEFT
                    )
                )
                textBlockList.add(sigleLineformat("ROC:${hostRoc}", AlignMode.RIGHT))//
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

                textBlockList.add(
                    sigleLineformat(
                        "INVOICE:${hostInvoice}",
                        AlignMode.LEFT
                    )
                )
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()
               /* if (batchTable.transactionType == BhTransactionType.BRAND_EMI.type)
                    textBlockList.add(
                        sigleLineformat(
                            "M.BILL NO:${batchTable.billNumber}",
                            AlignMode.RIGHT
                        )
                    )
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()*/

                if(batchTable.transactionType == BhTransactionType.PRE_AUTH_COMPLETE.type || batchTable.transactionType == BhTransactionType.VOID_PREAUTH.type){
                    printSeperator()
                    sigleLineText("ENTERED DETAILS", AlignMode.CENTER)

                    if(batchTable.transactionType == BhTransactionType.PRE_AUTH_COMPLETE.type){
                    textBlockList.add(sigleLineformat("TID:${batchTable.authTID}", AlignMode.LEFT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()
                    }

                    textBlockList.add(sigleLineformat("BATCH NO:${paddingInvoiceRoc(batchTable.authBatchNO) }", AlignMode.LEFT))
                    textBlockList.add(sigleLineformat("ROC:${paddingInvoiceRoc(batchTable.authROC)}", AlignMode.RIGHT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()

                    printSeperator()
                }
                val isNoEmiOnlyCashBackAppl : Boolean =  batchTable?.tenure=="1"

                if (isReversal) {
                    sigleLineText("TRANSACTION FAILED", AlignMode.CENTER)
                } else {
                    if(isNoEmiOnlyCashBackAppl) {
                        sigleLineText("SALE", AlignMode.CENTER)
                    }
                    else{
                        getTransactionTypeName(batchTable.transactionType)?.let { sigleLineText(it, AlignMode.CENTER) }
                    }
                }
                //getTransactionTypeName(batchTable.transactionType)?.let { sigleLineText(it, AlignMode.CENTER) }

              /* if(!batchTable.appName.isNullOrEmpty()){
                textBlockList.add(
                    sigleLineformat(
                        "CARD TYPE:${batchTable.appName}",
                        AlignMode.LEFT
                    )
                )}
                else{
                   textBlockList.add(
                       sigleLineformat(
                           "CARD TYPE:${hostCardType}",
                           AlignMode.LEFT
                       ))
               }*/

                if(!isReversal) {
                    if(batchTable?.transactionType != BhTransactionType.VOID_PREAUTH.type) {
                        textBlockList.add(
                            sigleLineformat(
                                "CARD TYPE:${hostCardType}",
                                AlignMode.LEFT
                            )
                        )
                        textBlockList.add(sigleLineformat("EXP:XX/XX", AlignMode.RIGHT))

                        printer?.addMixStyleText(textBlockList)

                        textBlockList.clear()
                    }
                }



                if(batchTable?.transactionType != BhTransactionType.VOID_PREAUTH.type) {
                    textBlockList.add(
                        sigleLineformat(
                            "CARD NO:${batchTable.cardNumber}",
                            AlignMode.LEFT
                        )
                    )

                    batchTable.operationType?.let { sigleLineformat(it, AlignMode.RIGHT) }?.let {
                        textBlockList.add(
                            it
                        )
                    }//
                    printer?.addMixStyleText(textBlockList)

                    textBlockList.clear()

                }




                if(!isReversal) {
                    if(batchTable.merchantMobileNumber.isNotEmpty()){
                        textBlockList.add(
                            sigleLineformat(
                                "MOBILE NO:${batchTable.merchantMobileNumber}",
                                AlignMode.LEFT
                            )
                        )

                        textBlockList.clear()
                    }


                    if(batchTable?.transactionType != BhTransactionType.VOID_PREAUTH.type) {
                        textBlockList.add(
                            sigleLineformat(
                                "AUTH CODE:${batchTable.authCode}",
                                AlignMode.LEFT
                            )
                        )

                        textBlockList.add(sigleLineformat("RRN:${batchTable.referenceNumber}", AlignMode.RIGHT))
                        printer?.addMixStyleText(textBlockList)

                        textBlockList.clear()

                    }

                }

                if(batchTable.transactionType == BhTransactionType.VOID_PREAUTH.type)
                {
                    textBlockList.add(sigleLineformat("CARD NO:${batchTable.cardNumber}", AlignMode.LEFT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()

                    textBlockList.add(sigleLineformat("RRN:${batchTable.referenceNumber}", AlignMode.LEFT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()

                }

                if (!(batchTable.tvr.isNullOrEmpty()) )
                textBlockList.add(sigleLineformat("TVR:${batchTable.tvr}", AlignMode.LEFT))
                if (!(batchTable.tsi.isNullOrEmpty()) )
                textBlockList.add(sigleLineformat("TSI:${batchTable.tsi}", AlignMode.RIGHT))
                if(!(batchTable.tvr.isNullOrEmpty()) || !(batchTable.tsi.isNullOrEmpty()))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()
                if (!(batchTable.aid.isNullOrEmpty()))
                textBlockList.add(sigleLineformat("AID:${batchTable.aid}", AlignMode.LEFT))
                if (!(batchTable.tc.isNullOrEmpty()))
                textBlockList.add(sigleLineformat("TC:${batchTable.tc}", AlignMode.RIGHT))
                if(!(batchTable.tc.isNullOrEmpty()) || !(batchTable.aid.isNullOrEmpty()))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

                /*if (!isReversal) {
                    textBlockList.add(
                        sigleLineformat(
                            "AID:${batchTable.aid}",
                            AlignMode.LEFT
                        )
                    )
                    printer?.addMixStyleText(textBlockList)

                    textBlockList.clear()
                }*/

                printSeperator()
                var txnName = batchTable.transationName

                if (isReversal) {
                    txnName = "REVERSAL"
                    batchTable.transactionType = BhTransactionType.REVERSAL.type
                }

                when (batchTable.transactionType) {
                    BhTransactionType.SALE.type, BhTransactionType.CASH_AT_POS.type, BhTransactionType.SALE_WITH_CASH.type -> {
                        saleTransaction(batchTable)
                    }
                    BhTransactionType.EMI_SALE.type , BhTransactionType.TEST_EMI.type, BhTransactionType.BRAND_EMI.type -> {
                        printEMISale(batchTable)

                    }
                    BhTransactionType.REVERSAL.type -> {
                        val amt = (((batchTable.baseAmmount)?.toLong())?.div(100)).toString()
                        textBlockList.add(sigleLineformat("TOTAL AMOUNT:", AlignMode.LEFT))
                        textBlockList.add(
                            sigleLineformat(
                                "INR:${"%.2f".format(amt.toDouble())}",
                                AlignMode.RIGHT
                            )
                        )
                        printer?.addMixStyleText(textBlockList)
                        textBlockList.clear()

                    }
                    BhTransactionType.PRE_AUTH_COMPLETE.type ->{
                        preAuthCompleteTransaction(batchTable)
                    }
                    BhTransactionType.VOID_PREAUTH.type ->{
                        preAuthCompleteTransaction(batchTable)
                    }
                    else -> {
                        voidTransaction(batchTable)

                    }

                                   }
                printSeperator()
                //region=====================BRAND TAndC===============
                if (batchTable.transactionType == BhTransactionType.EMI_SALE.type ||batchTable.transactionType == BhTransactionType.TEST_EMI.type||batchTable.transactionType == BhTransactionType.BRAND_EMI.type) {
                    if(!isNoEmiOnlyCashBackApplied!!)
                    printBrandTnC(batchTable)

                }
                //region=====================BRAND PRODUACT DATA===============
                if (batchTable.transactionType == BhTransactionType.BRAND_EMI.type) {
                    printProduactData(batchTable)
                    printSeperator()
                    baseAmounthandling(batchTable)
                }

                if(isReversal){
                    sigleLineText("Please contact your card issuer for reversal of debit if any", AlignMode.CENTER)
                    sigleLineText(footerText[1], AlignMode.CENTER)
                }else {

                    printer?.setAscScale(ASCScale.SC1x2)
                    printer?.setAscSize(ASCSize.DOT16x8)

                    if (batchTable.operationType.equals("CLESS_EMV")) {
                        if (batchTable.isPinverified == true) {
                            sigleLineText("PIN VERIFIED OK", AlignMode.CENTER)
                        }
                        if (batchTable.isPinverified == true) {
                            sigleLineText("SIGNATURE NOT REQUIRED", AlignMode.CENTER)
                        } else {
                            if (batchTable.transactionType == BhTransactionType.EMI_SALE.type || batchTable.transactionType == BhTransactionType.TEST_EMI.type || batchTable.transactionType == BhTransactionType.BRAND_EMI.type || batchTable.transactionType == BhTransactionType.SALE.type || batchTable.transactionType == BhTransactionType.CASH_AT_POS.type || batchTable.transactionType == BhTransactionType.SALE_WITH_CASH.type || batchTable.transactionType == BhTransactionType.PRE_AUTH.type) {
                                // val data= "PIN NOT REQUIRED FOR CONTACTLESS TRANSACTION UPTO ${batchTable?.cvmRequiredLimit}" //
                                /* val limit = 48
                            val chunks: List<String> = chunkTnC(data, limit)
                            for (st in chunks) {
                                logger("TNC", st, "e")
                                *//*       textBlockList.add(
                                           sigleLineformat(
                                               st, AlignMode.LEFT
                                           )
                                       )*//*
//                                            printer?.setHzScale(HZScale.SC1x1)
//                                            printer?.setHzSize(HZSize.DOT24x16)
                                printer?.setAscScale(ASCScale.SC1x1)
                                printer?.setAscSize(ASCSize.DOT24x8)
                                textBlockList.add(
                                    sigleLineformat(
                                        st,
                                        AlignMode.LEFT
                                    )
                                )
                                printer?.addMixStyleText(textBlockList)
                                textBlockList.clear()
                            }*/ //

                            } else {
                                if (batchTable.isPinverified) {
                                    sigleLineText("PIN VERIFIED OK", AlignMode.CENTER)
                                }
                                if (batchTable.isPinverified) {
                                    sigleLineText("SIGNATURE NOT REQUIRED", AlignMode.CENTER)
                                }
                            }
                        }
                    } else {
                        if (batchTable.isPinverified) {
                            sigleLineText("PIN VERIFIED OK", AlignMode.CENTER)
                        }

                        if (batchTable.isPinverified) {
                            sigleLineText("SIGNATURE NOT REQUIRED", AlignMode.CENTER)
                        } else {
                            sigleLineText("SIGN ...................", AlignMode.CENTER)
                        }


                        batchTable.cardHolderName?.let { sigleLineText(it, AlignMode.CENTER) }
                    }

                    printer?.setAscScale(ASCScale.SC1x1)
                    printer?.setAscSize(ASCSize.DOT24x8)

                    try {
                        val issuerParameterTable =
                            Field48ResponseTimestamp.getIssuerData(AppPreference.WALLET_ISSUER_ID)

                        var dec = issuerParameterTable?.walletIssuerDisclaimer

                        logger("dec", dec ?: "")
                        textBlockList.add(sigleLineformat(dec ?: "", AlignMode.CENTER))

                        printer?.addMixStyleText(textBlockList)
                        textBlockList.clear()

                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }

                    sigleLineText(copyType.pName, AlignMode.CENTER)
                    sigleLineText(footerText[0], AlignMode.CENTER)
                    sigleLineText(footerText[1], AlignMode.CENTER)
                }

                val bhlogo: ByteArray? = context?.let { printLogo(it, "BH.bmp") }
                printer?.addBmpImage(0, FactorMode.BMP1X1, bhlogo)
                sigleLineText(
                    "App Version :${BuildConfig.VERSION_NAME}",
                    AlignMode.CENTER
                )

                if (batchTable.transactionType == BhTransactionType.EMI_SALE.type ||batchTable.transactionType == BhTransactionType.TEST_EMI.type||batchTable.transactionType == BhTransactionType.BRAND_EMI.type) {
                    if(!isNoEmiOnlyCashBackAppl) {
                        val issuerId = batchTable?.issuerId
                        val issuerTAndCData = issuerId?.let { getIssuerTAndCDataByIssuerId(it) }
                        //region=======================Issuer Footer Terms and Condition=================
                        if (!TextUtils.isEmpty(issuerTAndCData?.footerTAndC)) {
                            printSeperator()

                            val issuerFooterTAndC =
                                issuerTAndCData?.footerTAndC?.split(SplitterTypes.POUND.splitter)
                            logger("getting footer tnc1=",issuerTAndCData?.footerTAndC.toString(),"e")
                            logger("issuerFooterTAndC-->=",issuerFooterTAndC.toString(),"e")
                            if (issuerFooterTAndC != null) {
                                if (issuerFooterTAndC.isNotEmpty()) {
                                    for (i in issuerFooterTAndC.indices) {
                                        if (!TextUtils.isEmpty(issuerFooterTAndC[i])) {
                                            val limit = 48
                                            val emiTnc = "#" + issuerFooterTAndC[i]
                                            val chunks: List<String> = chunkTnC(emiTnc, limit)
                                            for (st in chunks) {
                                                logger("TNC", st, "e")
                                                textBlockList.add(
                                                    sigleLineformat(
                                                        st, AlignMode.LEFT
                                                    )
                                                )
//                                            printer?.setHzScale(HZScale.SC1x1)
//                                            printer?.setHzSize(HZSize.DOT24x16)
                                                printer?.setAscScale(ASCScale.SC1x1)
                                                printer?.setAscSize(ASCSize.DOT24x8)
                                                printer?.addText( AlignMode.LEFT, st)
                                            }
                                            printer?.setAscSize(ASCSize.DOT24x12)
                                        }
                                    }
                                } else {
                                    textBlockList.add(
                                        sigleLineformat(
                                            "# ${issuerTAndCData.footerTAndC}", AlignMode.LEFT
                                        )
                                    )
//                                printer?.setHzScale(HZScale.SC1x1)
//                                printer?.setHzSize(HZSize.DOT24x16)
                                    printer?.setAscScale(ASCScale.SC1x1)
                                    printer?.setAscSize(ASCSize.DOT24x8)
                                    printer?.addText( AlignMode.LEFT, "# ${issuerTAndCData.footerTAndC}")
                                }
                            }
                        }
                    }

                }
                 //


            } catch (e: ParseException) {
                e.printStackTrace()
            }
            printer?.setPrnGray(3)
            printer?.feedLine(5)
            printer?.startPrint(object : OnPrintListener.Stub() {
                @Throws(RemoteException::class)
                override fun onFinish() {
                    printerCallback(true, 0)
                }

                @Throws(RemoteException::class)
                override fun onError(error: Int) {
                    printerCallback(true, 0)
                }
            })


        } catch (ex: DeadObjectException) {
            ex.printStackTrace()
        }


    }


    @SuppressLint("SimpleDateFormat")
    fun printReversal(
        context: Context?, field60Data: String, callback: (String) -> Unit
    ) {
        try {

            val isoW = AppPreference.getReversalNew()

            if (isoW != null) {

                var hostBankID: String? = null
                var hostIssuerID: String? = null
                var hostMID: String? = null
                var hostTID: String? = null
                var hostBatchNumber: String? = null
                var hostRoc: String? = null
                var hostInvoice: String? = null
                var hostCardType: String? = null

                    val f60DataList = field60Data.split('|')
                    //   Auto settle flag | Bank id| Issuer id | MID | TID | Batch No | Stan | Invoice | Card Type
                    // 0|1|51|000000041501002|41501369|000150|260|000260|RUPAY|
                    try {

                        hostBankID = f60DataList[1]
                        hostIssuerID = f60DataList[2]
                        hostMID = f60DataList[3]
                        hostTID = f60DataList[4]
                        hostBatchNumber = f60DataList[5]
                        hostRoc = f60DataList[6]
                        hostInvoice = f60DataList[7]
                        hostCardType = f60DataList[8]

                        println(
                            "Server MID and TID and batchumber and roc and cardType is" +
                                    "MID -> " + hostMID + "TID -> " + hostTID + "Batchnumber -> " + hostBatchNumber + "ROC ->" + hostRoc + "CardType -> " + hostCardType
                        )

                        //  batchFileData
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        //  batchFileData
                    }

                    //Changes By manish Kumar
                    //If in Respnse field 60 data comes Auto settle flag | Bank id| Issuer id | MID | TID | Batch No | Stan | Invoice | Card Type
                    // then show response data otherwise show data available in database
                    //From mid to hostMID (coming from field 60)
                    //From tid to hostTID (coming from field 60)
                    //From batchNumber to hostBatchNumber (coming from field 60)
                    //From roc to hostRoc (coming from field 60)
                    //From invoiceNumber to hostInvoice (coming from field 60)
                    //From cardType to hostCardType (coming from field 60)

                    val roc = isoW.isoMap[11]?.rawData ?: ""
                    val tid = isoW.isoMap[41]?.parseRaw2String() ?: ""
                    val mid = isoW.isoMap[42]?.parseRaw2String() ?: ""
                    val batchdata = isoW.isoMap[60]?.parseRaw2String() ?: ""
                    val batch = batchdata.split("|")[0]
                    val bankID = batchdata.split("|")[1]
                    val cardType = isoW.additionalData["cardType"] ?: ""

                    if (hostBankID?.isNotBlank() == true) {
                        hostBankID
                    } else {
                        hostBankID =  bankID
                    }

                    if (hostMID?.isNotBlank() == true) {
                        hostMID
                    } else {
                        hostMID =  mid
                    }
                    if (hostTID?.isNotBlank() == true) {
                        hostTID
                    } else {
                        hostTID = tid
                    }
                    if (hostBatchNumber?.isNotBlank() == true) {
                        hostBatchNumber
                    } else {
                        hostBatchNumber =  batch
                    }
                    if (hostRoc?.isNotBlank() == true) {
                        hostRoc
                    } else {
                        hostRoc = roc
                    }
                    if (hostCardType?.isNotBlank() == true) {
                        hostCardType
                    } else {
                        hostCardType = cardType
                    }


                    headerPrinting(hostBankID)

                    val cal = Calendar.getInstance()
                    cal.timeInMillis = isoW.timeStamp
                    val yr = cal.get(Calendar.YEAR).toString()
                    val of12 = isoW.isoMap[12]?.rawData ?: ""
                    val of13 = isoW.isoMap[13]?.rawData ?: ""

                    val d = of13 + yr


                    var amountStr = isoW.isoMap[4]?.rawData ?: "0"
                    val amt = amountStr.toFloat() / 100
                    amountStr = "%.2f".format(amt)

                    val date = "${d.substring(2, 4)}/${d.substring(0, 2)}/${d.substring(4, d.length)}"
                    val time =
                        "${of12.substring(0, 2)}:${of12.substring(2, 4)}:${
                            of12.substring(
                                4,
                                of12.length
                            )
                        }"

                    textBlockList.add(sigleLineformat("DATE:${date}", AlignMode.LEFT))
                    textBlockList.add(sigleLineformat("TIME:${time}", AlignMode.RIGHT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()

                    textBlockList.add(sigleLineformat("MID:${hostMID}", AlignMode.LEFT))
                    textBlockList.add(sigleLineformat("TID:${hostTID}", AlignMode.RIGHT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()

                    // alignLeftRightText(textInLineFormatBundle, "DATE:${date}", "TIME:${time}")
                    // alignLeftRightText(textInLineFormatBundle, "MID:${hostMID}", "TID:${hostTID}")

                    textBlockList.add(sigleLineformat("BATCH NO:${hostBatchNumber}",AlignMode.LEFT))
                    textBlockList.add(sigleLineformat("ROC:${hostRoc}", AlignMode.RIGHT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()

//                    alignLeftRightText(
//                        textInLineFormatBundle,
//                        "BATCH NO:${hostBatchNumber}",
//                        "ROC:${invoiceWithPadding(hostRoc)}"
//                    )

                    // centerText(textFormatBundle, "TRANSACTION FAILED")
                    sigleLineText("TRANSACTION FAILED", AlignMode.CENTER)

                    val card = isoW.additionalData["pan"] ?: ""
                    if (card.isNotEmpty())
                    {
                        textBlockList.add(sigleLineformat("CARD NO:${card}", AlignMode.LEFT))
                        textBlockList.add(sigleLineformat("${hostCardType}", AlignMode.RIGHT))
                        printer?.addMixStyleText(textBlockList)
                        textBlockList.clear()
                    }
                    /*alignLeftRightText(
                        textInLineFormatBundle,
                        "CARD NO:$card",
                        hostCardType
                    )//chip,swipe,cls*/


                    val tvr = isoW.additionalData["tvr"] ?: ""
                    val tsi = isoW.additionalData["tsi"] ?: ""
                    var aid = isoW.additionalData["aid"] ?: ""

                    //printer?.addText(textFormatBundle, "--------------------------------")
                    printSeperator()

                    if (tsi.isNotEmpty() && tvr.isNotEmpty()) {
                       // alignLeftRightText(textInLineFormatBundle, "TVR:${tvr}", "TSI:${tsi}")
                        textBlockList.add(sigleLineformat("TVR:${tvr}", AlignMode.LEFT))
                        textBlockList.add(sigleLineformat("TSI:${tsi}", AlignMode.RIGHT))
                        printer?.addMixStyleText(textBlockList)
                        textBlockList.clear()
                    }


                    if (aid.isNotEmpty()) {
                        aid = "AID:$aid"
                        //alignLeftRightText(textInLineFormatBundle, aid, "")
                        textBlockList.add(sigleLineformat(aid, AlignMode.LEFT))
                        printer?.addMixStyleText(textBlockList)
                        textBlockList.clear()
                    }


//                    printSeperator(textFormatBundle)
//                    centerText(textFormatBundle, "TOTAL AMOUNT : ${getCurrencySymbol(TerminalParameterTable.selectFromSchemeTable())} $amountStr")
//                    printSeperator(textFormatBundle)

                    printSeperator()
                    textBlockList.add(sigleLineformat("TOTAL AMOUNT:", AlignMode.LEFT))
                    textBlockList.add(sigleLineformat("INR:${amountStr}", AlignMode.RIGHT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()
                    printSeperator()

//                    centerText(
//                        textFormatBundle,
//                        "Please contact your card issuer for reversal of debit if any."
//                    )
                    sigleLineText("Please contact your card issuer for reversal of debit if any", AlignMode.CENTER)
                    sigleLineText("POWERED BY", AlignMode.CENTER)
                    //centerText(textFormatBundle, "POWERED BY")
//                    printLogo("BH.bmp")
//
//                    centerText(textFormatBundle, "APP VER : ${BuildConfig.VERSION_NAME}")

                    val bhlogo: ByteArray? = context?.let { printLogo(it, "BH.bmp") }
                    printer?.addBmpImage(0, FactorMode.BMP1X1, bhlogo)
                    sigleLineText(
                        "App Version :${BuildConfig.VERSION_NAME}",
                        AlignMode.CENTER
                    )



                    printer?.feedLine(4)


            }

            } catch (e: ParseException) {
                e.printStackTrace()
            }

            printer?.setPrnGray(3)
            printer?.feedLine(5)
            printer?.startPrint(object : OnPrintListener.Stub() {
                @Throws(RemoteException::class)
                override fun onFinish() {
                    callback("true")
                }

                @Throws(RemoteException::class)
                override fun onError(error: Int) {
                    callback("false")
                }
            })

    }

    //printing for logo or you can print any image from assets
    private fun printLogo(ctx: Context, fileName: String): ByteArray? {
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

    // using this you can set your text format in single line ---(Left    Center    Right)
    private fun sigleLineformat(text: String, alignMode: Int): Bundle {
        val format = Bundle()
        format.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
        format.putInt(PrinterData.ASC_SIZE, ASCSize.DOT24x8)
        format.putString(PrinterData.TEXT, text)
        format.putInt(PrinterData.ALIGN_MODE, alignMode)
        return format
    }

    // using this you can set your single text
    private fun sigleLineText(text: String, alignMode: Int) {
        printer?.setHzScale(HZScale.SC1x1)
        printer?.setHzSize(HZSize.DOT24x24)
        printer?.addText(alignMode, text)
    }

    private fun saleTransaction(batchTable: TempBatchFileDataTable) {
        var currencySymbol: String? = "Rs"
        val terminalData = getTptData()
        currencySymbol = terminalData?.currencySymbol
        val receiptDetail = batchTable //: ReceiptDetail = batchTable.receiptData ?: ReceiptDetail()
        val amt = (((receiptDetail.baseAmmount)?.toDouble())?.div(100)).toString()

        var tipAmount:String? = null
        try{
            tipAmount = (((receiptDetail.tipAmmount)?.toLong())?.div(100)).toString()

        }catch (ex:Exception){
            ex.printStackTrace()
            tipAmount = "0"
        }
       // var tipAmount = (((receiptDetail.otherAmount)?.toLong())?.div(100)).toString()
        var totalAmount: String? = null
        if (batchTable.transactionType == BhTransactionType.SALE.type) {
            textBlockList.add(sigleLineformat("SALE AMOUNT:", AlignMode.LEFT))
            textBlockList.add(sigleLineformat("$currencySymbol :${"%.2f".format(amt.toDouble())}", AlignMode.RIGHT))
            printer?.addMixStyleText(textBlockList)
            textBlockList.clear()
            if (tipAmount != "0") {
                tipAmount = (((receiptDetail.tipAmmount)?.toDouble())?.div(100)).toString()
                textBlockList.add(sigleLineformat("TIP AMOUNT:", AlignMode.LEFT))
                textBlockList.add(
                    sigleLineformat(
                        "%.2f".format(tipAmount.toDouble()),
                        AlignMode.RIGHT
                    )
                )
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()
            }
            totalAmount = "%.2f".format((amt.toDouble() ))
        } else {
            if(batchTable.transactionType == BhTransactionType.SALE_WITH_CASH.type){ // kushal
//                val amt1=(((receiptDetail.totalAmmount)?.toLong())?.div(100))
//                val otherAmt1=(((receiptDetail.otherAmount)?.toLong())?.div(100))
//                val saleAmount= otherAmt1?.let { amt1?.minus(it) }
//                textBlockList.add(sigleLineformat("SALE AMOUNT:", AlignMode.LEFT))
//                textBlockList.add(sigleLineformat("$currencySymbol :${"%.2f".format(saleAmount?.toDouble())}", AlignMode.RIGHT))
//                printer?.addMixStyleText(textBlockList)
//                textBlockList.clear()

                textBlockList.add(sigleLineformat("CASH WITHDRAWN AMT: ", AlignMode.LEFT))
                textBlockList.add(
                    sigleLineformat(
                        "$currencySymbol:${"%.2f".format(tipAmount?.toDouble())}",
                        AlignMode.RIGHT
                    )
                )
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()
                totalAmount = "%.2f".format((amt.toDouble())).toString()
            }else{
                textBlockList.add(sigleLineformat("CASH WITHDRAWN AMT:", AlignMode.LEFT))
                textBlockList.add(sigleLineformat("$currencySymbol :${"%.2f".format(amt.toDouble())}", AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()
                totalAmount = "%.2f".format((amt.toDouble()))
            }

        }
        // textBlockList.add(sigleLineformat( "00",AlignMode.RIGHT))


        textBlockList.add(sigleLineformat("TOTAL AMOUNT:", AlignMode.LEFT))
        textBlockList.add(sigleLineformat("$currencySymbol :${totalAmount}", AlignMode.RIGHT))
        printer?.addMixStyleText(textBlockList)
        textBlockList.clear()
    }

    private fun saleTransaction(batchTable: BatchTable) {
        var currencySymbol: String? = "Rs"
        val terminalData = getTptData()
        currencySymbol = terminalData?.currencySymbol
        val receiptDetail : ReceiptDetail = batchTable.receiptData ?: ReceiptDetail()
        val amt = (((receiptDetail.txnAmount)?.toDouble())?.div(100)).toString()
        var tipAmount = (((receiptDetail.txnOtherAmount)?.toLong())?.div(100)).toString()
        var totalAmount: String? = null
        if (batchTable.transactionType == BhTransactionType.SALE.type) {
            textBlockList.add(sigleLineformat("SALE AMOUNT:", AlignMode.LEFT))
            textBlockList.add(sigleLineformat("$currencySymbol :${"%.2f".format(amt.toDouble())}", AlignMode.RIGHT))
            printer?.addMixStyleText(textBlockList)
            textBlockList.clear()
            if (tipAmount != "0") {
                tipAmount = (((receiptDetail.txnOtherAmount)?.toDouble())?.div(100)).toString()
                textBlockList.add(sigleLineformat("TIP AMOUNT:", AlignMode.LEFT))
                textBlockList.add(
                    sigleLineformat(
                        "%.2f".format(tipAmount.toDouble()),
                        AlignMode.RIGHT
                    )
                )
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()
            }
            totalAmount = "%.2f".format((amt.toDouble() ))
        } else {
            if(batchTable.transactionType == BhTransactionType.SALE_WITH_CASH.type){
                val amt1=(((receiptDetail.txnAmount)?.toLong())?.div(100))
                val otherAmt1=(((receiptDetail.txnOtherAmount)?.toLong())?.div(100))
                val saleAmount= otherAmt1?.let { amt1?.minus(it) }
                textBlockList.add(sigleLineformat("SALE AMOUNT:", AlignMode.LEFT))
                textBlockList.add(sigleLineformat("$currencySymbol :${"%.2f".format(saleAmount?.toDouble())}", AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

                textBlockList.add(sigleLineformat("CASH WITHDRAWN AMT: ", AlignMode.LEFT))
                textBlockList.add(
                    sigleLineformat(
                        "$currencySymbol:${"%.2f".format(tipAmount.toDouble())}",
                        AlignMode.RIGHT
                    )
                )
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()
                totalAmount = "%.2f".format((amt.toDouble())).toString()
            }else{
                textBlockList.add(sigleLineformat("CASH WITHDRAWN AMT:", AlignMode.LEFT))
                textBlockList.add(sigleLineformat("$currencySymbol :${"%.2f".format(amt.toDouble())}", AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()
                totalAmount = "%.2f".format((amt.toDouble()))
            }

        }
        // textBlockList.add(sigleLineformat( "00",AlignMode.RIGHT))


        textBlockList.add(sigleLineformat("TOTAL AMOUNT:", AlignMode.LEFT))
        textBlockList.add(sigleLineformat("$currencySymbol :${totalAmount}", AlignMode.RIGHT))
        printer?.addMixStyleText(textBlockList)
        textBlockList.clear()
    }

    private fun voidTransaction(receiptDetail: TempBatchFileDataTable) {
        var currencySymbol: String? = "Rs"
        val terminalData = getTptData()
        currencySymbol = terminalData?.currencySymbol
        textBlockList.add(sigleLineformat("BASE AMOUNT:", AlignMode.LEFT))
        val amt = (((receiptDetail.baseAmmount)?.toDouble())?.div(100)).toString()
        textBlockList.add(sigleLineformat("$currencySymbol :${"%.2f".format(amt.toDouble())}", AlignMode.RIGHT))
        printer?.addMixStyleText(textBlockList)
        textBlockList.clear()

        textBlockList.add(sigleLineformat("TOTAL AMOUNT:", AlignMode.LEFT))
        textBlockList.add(sigleLineformat("$currencySymbol :${"%.2f".format(amt.toDouble())}", AlignMode.RIGHT))
        printer?.addMixStyleText(textBlockList)
        textBlockList.clear()
    }

    private fun preAuthCompleteTransaction(receiptDetail: TempBatchFileDataTable) {
        var currencySymbol: String? = "Rs"
        val terminalData = getTptData()
        currencySymbol = terminalData?.currencySymbol
        textBlockList.add(sigleLineformat("BASE AMOUNT:", AlignMode.LEFT))
        val amt = (((receiptDetail.transactionalAmmount)?.toDouble())?.div(100)).toString()
        textBlockList.add(sigleLineformat("$currencySymbol :${"%.2f".format(amt.toDouble())}", AlignMode.RIGHT))
        printer?.addMixStyleText(textBlockList)
        textBlockList.clear()

        textBlockList.add(sigleLineformat("TOTAL AMOUNT:", AlignMode.LEFT))
        textBlockList.add(sigleLineformat("$currencySymbol :${"%.2f".format(amt.toDouble())}", AlignMode.RIGHT))
        printer?.addMixStyleText(textBlockList)
        textBlockList.clear()
    }

    private fun printEMISale(batchTable: TempBatchFileDataTable) {
        var currencySymbol: String? = "Rs"
        val terminalData = getTptData()
        currencySymbol = terminalData?.currencySymbol
//        val receiptDetail: ReceiptDetail = batchTable.receiptData ?: ReceiptDetail()
//        val bankEMITenureDataModal: BankEMITenureDataModal? = batchTable.emiTenureDataModel
//        val bankEMIIssuerTAndCDataModal: BankEMIIssuerTAndCDataModal? = batchTable.emiIssuerDataModel
        val issuerId = batchTable?.issuerId
        val isNoEmiOnlyCashBackApplied : Boolean =  batchTable?.tenure=="1"
       if(!isNoEmiOnlyCashBackApplied) {
           textBlockList.add(sigleLineformat("TXN AMOUNT", AlignMode.LEFT))

           //   val txnAmount = (((bankEMITenureDataModal?.transactionAmount)?.toLong())?.div(100)).toString()
           var txnAmount = (((batchTable.emiTransactionAmount).toDouble()).div(100)).toString()

           logger("txnAmount", "" + txnAmount)
           textBlockList.add(
               sigleLineformat(
                   "$currencySymbol:${"%.2f".format(txnAmount.toDoubleOrNull())}",
                   AlignMode.RIGHT
               )
           )
           printer?.addMixStyleText(textBlockList)
           textBlockList.clear()

           logger("INSTA DISCOUNT", "  ${batchTable?.instantDiscount}")
           if (batchTable?.instantDiscount?.toIntOrNull() != null) {
               if (batchTable.instantDiscount.isNotBlank() && batchTable.instantDiscount.toInt() > 0) {
                   val instantDis =
                       "%.2f".format(
                           (((batchTable.instantDiscount).toDouble()).div(
                               100
                           )).toString().toDouble()
                       )

                   textBlockList.add(sigleLineformat("INSTA DISCOUNT", AlignMode.LEFT))
                   val authAmount =
                       (((batchTable.transactionAmt)?.toLong())?.div(100)).toString()
                   textBlockList.add(
                       sigleLineformat(
                           "$currencySymbol:${instantDis}",
                           AlignMode.RIGHT
                       )
                   )
                   printer?.addMixStyleText(textBlockList)
                   textBlockList.clear()

               }
           }
           textBlockList.add(sigleLineformat("AUTH AMOUNT", AlignMode.LEFT))
           if (batchTable.transactionType == BhTransactionType.TEST_EMI.type) {

               textBlockList.add(
                   sigleLineformat(
                       "$currencySymbol:${1.00}",
                       AlignMode.RIGHT
                   )
               )
           } else {
               val authAmount =
                   (((batchTable?.transactionAmt)?.toDouble())?.div(100)).toString()
               textBlockList.add(
                   sigleLineformat(
                       "$currencySymbol:${"%.2f".format(authAmount.toDouble())}",
                       AlignMode.RIGHT
                   )
               )
           }


           textBlockList.add(sigleLineformat("CARD ISSUER", AlignMode.LEFT))
           if (batchTable.transactionType == BhTransactionType.TEST_EMI.type) {


               textBlockList.add(
                   sigleLineformat(
                       " TEST ISSUER",
                       AlignMode.RIGHT
                   )
               )
           } else {
               if (batchTable != null) {
                   textBlockList.add(
                       sigleLineformat(
                           batchTable.issuerName,
                           AlignMode.RIGHT
                       )
                   )
               }
           }
           printer?.addMixStyleText(textBlockList)
           textBlockList.clear()
           val tenureDuration = "${batchTable?.tenure} Months"
           val tenureHeadingDuration = "${batchTable?.tenure} Months Scheme"
           var roi = batchTable?.roi?.toInt()
               ?.let { divideAmountBy100(it).toString() }
           var loanamt =
               batchTable?.loanAmt?.toInt()?.let { divideAmountBy100(it).toString() }
           roi = "%.2f".format(roi?.toDouble()) + " %"
           loanamt = "%.2f".format(loanamt?.toDouble())
           textBlockList.add(sigleLineformat("ROI(pa)", AlignMode.LEFT))
           textBlockList.add(
               sigleLineformat(
                   "$roi",
                   AlignMode.RIGHT
               )
           )
           printer?.addMixStyleText(textBlockList)
           textBlockList.clear()

           textBlockList.add(sigleLineformat("TENURE", AlignMode.LEFT))
           textBlockList.add(sigleLineformat(tenureDuration, AlignMode.RIGHT))
           printer?.addMixStyleText(textBlockList)
           textBlockList.clear()
           //region===============Processing Fee Changes And Showing On ChargeSlip:-
           if (!TextUtils.isEmpty(batchTable?.processingFee)) {
               if ((batchTable?.processingFee) != "0") {
                   val procFee = "%.2f".format(
                       (((batchTable?.processingFee)?.toDouble())?.div(100)).toString()
                           .toDouble()
                   )
                   textBlockList.add(sigleLineformat("PROC-FEE", AlignMode.LEFT))
                   textBlockList.add(sigleLineformat("$currencySymbol $procFee", AlignMode.RIGHT))
                   printer?.addMixStyleText(textBlockList)
                   textBlockList.clear()
               }
           }

           if (!TextUtils.isEmpty(batchTable?.processingFeeRate)) {
               val procFeeAmount =
                   batchTable?.processingFeeRate?.toFloat()?.div(100)
               val pfeeData: Int? = procFeeAmount?.toInt()
               if ((pfeeData.toString()) != "0") {
                   val procFeeAmount =
                       "%.2f".format(
                           batchTable?.processingFeeRate?.toFloat()?.div(100)
                       ) + " %"

                   textBlockList.add(sigleLineformat("PROC-FEE", AlignMode.LEFT))
                   textBlockList.add(
                       sigleLineformat(
                           "$currencySymbol $procFeeAmount",
                           AlignMode.RIGHT
                       )
                   )
                   printer?.addMixStyleText(textBlockList)
                   textBlockList.clear()

               }
           }
           if (batchTable != null) {
               if (!TextUtils.isEmpty(batchTable.totalProcessingFee)) {
                   if (!(batchTable.totalProcessingFee).equals("0")) {
                       val totalProcFeeAmount =
                           "%.2f".format(batchTable.totalProcessingFee.toFloat() / 100)

                       textBlockList.add(sigleLineformat("PROC-FEE AMOUNT", AlignMode.LEFT))
                       textBlockList.add(
                           sigleLineformat(
                               "$currencySymbol $totalProcFeeAmount",
                               AlignMode.RIGHT
                           )
                       )
                       printer?.addMixStyleText(textBlockList)
                       textBlockList.clear()
                   }
               }
           }

           var cashBackPercentHeadingText = ""
           var cashBackAmountHeadingText = ""
           var islongTextHeading = true
           if (batchTable != null) {
               when (batchTable.issuerId) {
                   "51" -> {
                       cashBackPercentHeadingText = "Mfg/Mer Payback"
                       cashBackAmountHeadingText = "Mfg/Mer-"
                       //  cashBackAmountHeadingText = "Mfg/Mer Payback Amt"
                   }
                   "64" -> {
                       cashBackPercentHeadingText = "Mfg/Mer Payback"
                       cashBackAmountHeadingText = "Mfg/Mer-"
                       //  cashBackAmountHeadingText = "Mfg/Mer Payback Amt"
                   }
                   "52" -> {
                       cashBackPercentHeadingText = "Mfg/Mer Cashback"
                       cashBackAmountHeadingText = "Mfg/Mer-"
                       //   cashBackAmountHeadingText = "Mfg/Mer Cashback Amt"
                   }
                   "55" -> {
                       cashBackPercentHeadingText = "Mer/Mfr Cashback"
                       cashBackAmountHeadingText = "Mer/Mfr-"
                       //  cashBackAmountHeadingText = "Mer/Mfr Cashback Amt"
                   }
                   else -> {
                       islongTextHeading = false
                       cashBackPercentHeadingText = "CASH BACK"
                       cashBackAmountHeadingText = "TOTAL CASH BACK"
                   }
               }


               when (batchTable.issuerId) {
                   "51", "64" -> {
                       nextLineAppendStr = "Payback Amount"
                   }
                   "52", "55", "54" -> {
                       nextLineAppendStr = "Cashback Amount"
                   }

               }
           }

           //region=============CashBack CalculatedValue====================
           if (!TextUtils.isEmpty(batchTable?.cashBackCalculatedValue)) {
               if (islongTextHeading) {
                   textBlockList.add(sigleLineformat(cashBackPercentHeadingText, AlignMode.LEFT))
                   textBlockList.add(
                       sigleLineformat(
                           "$currencySymbol ${batchTable?.cashBackCalculatedValue}",
                           AlignMode.RIGHT
                       )
                   )
                   printer?.addMixStyleText(textBlockList)
                   textBlockList.clear()
               } else {
                   textBlockList.add(sigleLineformat(cashBackPercentHeadingText, AlignMode.LEFT))
                   textBlockList.add(
                       sigleLineformat(
                           "$currencySymbol ${batchTable?.cashBackCalculatedValue}",
                           AlignMode.RIGHT
                       )
                   )
                   printer?.addMixStyleText(textBlockList)
                   textBlockList.clear()
               }
           }

           if (!TextUtils.isEmpty(batchTable?.cashback) && batchTable?.cashback != "0") {
               val cashBackAmount = "%.2f".format(
                   batchTable?.cashback?.toFloat()
                       ?.div(100)
               )

               if (islongTextHeading) {
                   textBlockList.add(sigleLineformat(cashBackAmountHeadingText, AlignMode.LEFT))
                   textBlockList.add(sigleLineformat(" ", AlignMode.RIGHT))
                   printer?.addMixStyleText(textBlockList)
                   textBlockList.clear()

                   textBlockList.add(sigleLineformat(nextLineAppendStr, AlignMode.LEFT))
                   textBlockList.add(
                       sigleLineformat(
                           "$currencySymbol $cashBackAmount",
                           AlignMode.RIGHT
                       )
                   )
                   printer?.addMixStyleText(textBlockList)
                   textBlockList.clear()
               } else {
                   println("test-->${batchTable?.cashback}")
                   if (batchTable?.cashback != "0" && !(batchTable?.cashback.isNullOrEmpty())) {
                       val cashBackAmount = "%.2f".format(
                           batchTable?.cashback?.toFloat()
                               ?.div(100)
                       )


                       textBlockList.add(sigleLineformat(cashBackAmountHeadingText, AlignMode.LEFT))
                       textBlockList.add(
                           sigleLineformat(
                               "$currencySymbol $cashBackAmount",
                               AlignMode.RIGHT
                           )
                       )
                       printer?.addMixStyleText(textBlockList)
                       textBlockList.clear()
                   }
               }
           }
           println("bankid ${batchTable?.issuerId}")

           var discountPercentHeadingText = ""
           var discountAmountHeadingText = ""
           islongTextHeading = true
           when (batchTable?.issuerId) {
               "51" -> {
                   discountPercentHeadingText = "Mfg/Mer Payback"
                   discountAmountHeadingText = "Mfg/Mer-"
                   //  discountAmountHeadingText = "Mfg/Mer Payback Amt"
               }
               "64" -> {
                   discountPercentHeadingText = "Mfg/Mer Payback"
                   discountAmountHeadingText = "Mfg/Mer-"
                   // discountAmountHeadingText = "Mfg/Mer Payback Amt"
               }
               "52" -> {
                   discountPercentHeadingText = "Mfg/Mer Cashback"
                   discountAmountHeadingText = "Mfg/Mer-"
                   //  discountAmountHeadingText = "Mfg/Mer Cashback Amt"
               }

               "55" -> {
                   discountPercentHeadingText = "Mer/Mfr Cashback"
                   discountAmountHeadingText = "Mer/Mfr"
                   //  discountAmountHeadingText = "Mer/Mfr Cashback Amt"
               }

               else -> {
                   islongTextHeading = false
                   discountPercentHeadingText = "DISCOUNT"
                   discountAmountHeadingText = "TOTAL DISCOUNT"
               }
           }
           if (!TextUtils.isEmpty(batchTable?.discountCalculatedValue)) {
               if (islongTextHeading) {

                   textBlockList.add(sigleLineformat(cashBackPercentHeadingText, AlignMode.LEFT))
                   textBlockList.add(
                       sigleLineformat(
                           "$currencySymbol ${batchTable?.discountCalculatedValue}",
                           AlignMode.RIGHT
                       )
                   )
                   printer?.addMixStyleText(textBlockList)
                   textBlockList.clear()

               } else {
                   textBlockList.add(sigleLineformat(discountPercentHeadingText, AlignMode.LEFT))
                   textBlockList.add(
                       sigleLineformat(
                           "$currencySymbol ${batchTable?.discountCalculatedValue}",
                           AlignMode.RIGHT
                       )
                   )
                   printer?.addMixStyleText(textBlockList)
                   textBlockList.clear()
               }
           }
           if (!(batchTable?.cashDiscountAmt.isNullOrEmpty()) && batchTable?.cashDiscountAmt != "0") {
               val discAmount =
                   "%.2f".format(batchTable?.cashDiscountAmt?.toFloat()?.div(100))

               if (islongTextHeading) {

                     textBlockList.add(sigleLineformat(discountAmountHeadingText, AlignMode.LEFT))
                   textBlockList.add(sigleLineformat("", AlignMode.CENTER))
                textBlockList.add(sigleLineformat("", AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

                   textBlockList.add(sigleLineformat(nextLineAppendStr, AlignMode.LEFT))
                   textBlockList.add(
                       sigleLineformat(
                           "$currencySymbol ${discAmount}",
                           AlignMode.RIGHT
                       )
                   )
                   printer?.addMixStyleText(textBlockList)
                   textBlockList.clear()


               } else {

                   textBlockList.add(sigleLineformat(discountAmountHeadingText, AlignMode.LEFT))
                   textBlockList.add(
                       sigleLineformat(
                           "$currencySymbol ${discAmount}",
                           AlignMode.RIGHT
                       )
                   )
                   printer?.addMixStyleText(textBlockList)
                   textBlockList.clear()

               }
           }






           textBlockList.add(sigleLineformat("LOAN AMOUNT", AlignMode.LEFT))
           textBlockList.add(
               sigleLineformat(
                   "$currencySymbol:${loanamt}",
                   AlignMode.RIGHT
               )
           )
           printer?.addMixStyleText(textBlockList)
           textBlockList.clear()

           textBlockList.add(sigleLineformat("MONTHLY EMI", AlignMode.LEFT))
           if (!(batchTable?.monthlyEmi.isNullOrEmpty()) && batchTable?.monthlyEmi != "0") {
               var emiAmount =
                   "%.2f".format(batchTable?.monthlyEmi?.toFloat()?.div(100))
               textBlockList.add(
                   sigleLineformat(
                       "$currencySymbol:${emiAmount}",
                       AlignMode.RIGHT
                   )
               )
               printer?.addMixStyleText(textBlockList)
               textBlockList.clear()
           }
           textBlockList.add(sigleLineformat("TOTAL INTEREST", AlignMode.LEFT))
           if (!(batchTable?.totalInterest.isNullOrEmpty()) && batchTable?.totalInterest != "0") {
               var totalInterestPay =
                   "%.2f".format(batchTable?.totalInterest?.toFloat()?.div(100))
               textBlockList.add(
                   sigleLineformat(
                       "$currencySymbol:${totalInterestPay}",
                       AlignMode.RIGHT
                   )
               )
               printer?.addMixStyleText(textBlockList)
               textBlockList.clear()
           }
           var totalAmountHeadingText = ""
           // below is the old technique used in old font
           /* totalAmountHeadingText = when (printerReceiptData.issuerId) {
         "52" -> "TOTAL AMOUNT(incl Int)"
         "55" -> "TOTAL EFFECTIVE PAYOUT"
         else -> "TOTAL Amt(With Int)"
     } */
           //  With new font
           totalAmountHeadingText = when (issuerId) {
               "52" -> "TOTAL AMOUNT-"
               "55" -> "TOTAL EFFECTIVE-"
               else -> "TOTAL Amt"
           }
           if (!TextUtils.isEmpty(batchTable?.totalInterest)) {

               if (batchTable.transactionType == BhTransactionType.TEST_EMI.type) {
                   val loanAmt =
                       "%.2f".format(
                           (((batchTable?.loanAmt)?.toDouble())?.div(100)).toString()
                               .toDouble()
                       )
                   val totalInterest =
                       "%.2f".format(
                           (((batchTable?.totalInterest)?.toDouble())?.div(100)).toString()
                               .toDouble()
                       )
                   val totalAmt =
                       "%.2f".format(loanAmt.toDouble().plus(totalInterest.toDouble()))

                   /*alignLeftRightText(
                textInLineFormatBundle,
                totalAmountHeadingText,
                totalAmt.toString(),
                ":$currencySymbol"
            )*/
                   when (issuerId) {
                       "52" -> {

                           textBlockList.add(
                               sigleLineformat(
                                   "$totalAmountHeadingText (incl Int)",
                                   AlignMode.LEFT
                               )
                           )
                           textBlockList.add(
                               sigleLineformat(
                                   "$currencySymbol:${totalAmt.toString()}",
                                   AlignMode.RIGHT
                               )
                           )
                           printer?.addMixStyleText(textBlockList)
                           textBlockList.clear()


                       }
                       "55" -> {

                           textBlockList.add(
                               sigleLineformat(
                                   "$totalAmountHeadingText (PAYOUT)",
                                   AlignMode.LEFT
                               )
                           )

                           textBlockList.add(
                               sigleLineformat(
                                   "$currencySymbol:${totalAmt.toString()}",
                                   AlignMode.RIGHT
                               )
                           )
                           printer?.addMixStyleText(textBlockList)
                           textBlockList.clear()
                       }
                       else -> {


                           textBlockList.add(
                               sigleLineformat(
                                   "$totalAmountHeadingText (With Int)",
                                   AlignMode.LEFT
                               )
                           )

                           textBlockList.add(
                               sigleLineformat(
                                   "$currencySymbol:${totalAmt.toString()}",
                                   AlignMode.RIGHT
                               )
                           )
                           printer?.addMixStyleText(textBlockList)
                           textBlockList.clear()
                       }

                   }

               } else {
                   val f_totalAmt =
                       "%.2f".format(batchTable?.netPay?.toFloat()?.div(100))
                   /*alignLeftRightText(
                textInLineFormatBundle,
                totalAmountHeadingText,
                f_totalAmt.toString(),
                ":$currencySymbol"
            )*/

                   when (issuerId) {
                       "52" -> {

                           textBlockList.add(
                               sigleLineformat(
                                   "$totalAmountHeadingText (incl Int)",
                                   AlignMode.LEFT
                               )
                           )
                           textBlockList.add(
                               sigleLineformat(
                                   "$currencySymbol:${f_totalAmt}",
                                   AlignMode.RIGHT
                               )
                           )
                           printer?.addMixStyleText(textBlockList)
                           textBlockList.clear()

                       }
                       "55" -> {
                           textBlockList.add(
                               sigleLineformat(
                                   "$totalAmountHeadingText (PAYOUT)",
                                   AlignMode.LEFT
                               )
                           )

                           textBlockList.add(
                               sigleLineformat(
                                   "$currencySymbol:${f_totalAmt}",
                                   AlignMode.RIGHT
                               )
                           )
                           printer?.addMixStyleText(textBlockList)
                           textBlockList.clear()

                       }
                       else -> {
                           textBlockList.add(
                               sigleLineformat(
                                   "$totalAmountHeadingText (With Int)",
                                   AlignMode.LEFT
                               )
                           )
                           textBlockList.add(
                               sigleLineformat(
                                   "$currencySymbol:${f_totalAmt}",
                                   AlignMode.RIGHT
                               )
                           )
                           printer?.addMixStyleText(textBlockList)
                           textBlockList.clear()

                       }

                   }
               }
           }

       }else{
           textBlockList.add(sigleLineformat("Scheme", AlignMode.LEFT))
           if (batchTable != null) {
               textBlockList.add(
                   sigleLineformat(
                       batchTable?.tenureLabel,
                       AlignMode.RIGHT
                   )
               )
               printer?.addMixStyleText(textBlockList)
               textBlockList.clear()
           }

           textBlockList.add(sigleLineformat("Card Issuer", AlignMode.LEFT))
           if (batchTable != null) {
               textBlockList.add(
                   sigleLineformat(
                       batchTable.issuerName,
                       AlignMode.RIGHT
                   )
               )
           }
           printer?.addMixStyleText(textBlockList)
           textBlockList.clear()
       }
    }


    private fun printBrandTnC(batchTable: TempBatchFileDataTable) {

        /*val brandEMIMasterDataModal: BrandEMIMasterDataModal? = batchTable.emiBrandData
        val bankEMITenureDataModal: BankEMITenureDataModal? = batchTable.emiTenureDataModel
        val bankEMIIssuerTAndCDataModal: BankEMIIssuerTAndCDataModal? = batchTable.emiIssuerDataModel*/
        val issuerId = batchTable?.issuerId
        var brandId = batchTable?.brandId

        val issuerTAndCData = issuerId?.let { getIssuerTAndCDataByIssuerId(it) }
        val jsonRespp = Gson().toJson(issuerTAndCData)

        println(jsonRespp)

        logger("getting issuer tnc=",jsonRespp.toString(),"e")

        sigleLineText("CUSTOMER CONSENT FOR EMI", AlignMode.CENTER)
        //region=======================Issuer Header Terms and Condition=================
        var issuerHeaderTAndC: List<String>? = null
        val testTnc =
            "#.I have been offered the choice of normal as well as EMI for this purchase and I have chosen EMI.#.I have fully understood and accept the terms of EMI scheme and applicable charges mentioned in this charge-slip.#.EMI conversion subject to Banks discretion and by take minimum * working days.#.GST extra on the interest amount.#.For the first EMI, the interest will be calculated from the loan booking date till the payment due date.#.Convenience fee of Rs --.-- + GST will be applicable on EMI transactions."

            logger("getting issuer h tnc=",issuerTAndCData?.headerTAndC.toString(),"e")
        logger("check kush",""+ (batchTable.transactionType == BhTransactionType.TEST_EMI.type))
        issuerHeaderTAndC =
                if (batchTable.transactionType == BhTransactionType.TEST_EMI.type) {
                    testTnc.split(SplitterTypes.POUND.splitter)
                } else {
                    issuerTAndCData?.headerTAndC?.split(SplitterTypes.POUND.splitter)

                }
            logger("getting header tnc=",issuerTAndCData?.headerTAndC.toString(),"e")
            logger("getting footer tnc=",issuerTAndCData?.footerTAndC.toString(),"e")

        if (issuerHeaderTAndC?.isNotEmpty() == true) {
            for (i in issuerHeaderTAndC.indices) {
                if (!TextUtils.isEmpty(issuerHeaderTAndC?.get(i))) {
                    val limit = 48
                    if (!(issuerHeaderTAndC[i].isBlank())) {
                        val emiTnc = "#" + issuerHeaderTAndC[i]
                        val chunks: List<String> = chunkTnC(emiTnc, limit)
                        printer?.setAscScale(ASCScale.SC1x1)
                        printer?.setAscSize(ASCSize.DOT24x8)
                        for (st in chunks) {
                            logger("issuerHeaderTAndC", st, "e")

                            textBlockList.add(
                                sigleLineformat(
                                    st,
                                    AlignMode.LEFT
                                )
                            )
                            printer?.addMixStyleText(textBlockList)
                            textBlockList.clear()
                        }
                        printer?.setAscSize(ASCSize.DOT24x12)
                    }
                }
            }

        }
        //endregion


        if (batchTable.transactionType == BhTransactionType.BRAND_EMI.type) {
            //region ======================Brand terms and Condition=========================

        //val brandId = brandEMIMasterDataModal?.brandID
        val data = getBrandTAndCData()
        val jsonResp = Gson().toJson(data)


        logger("size=",data?.size.toString(),"e")
        logger("getting=",data.toString(),"e")
        println(jsonResp)
       if (brandId != null) {
           val brandTnc = getBrandTAndCDataByBrandId(brandId)
           logger("Brand Tnc", brandTnc, "e")
           val chunk: List<String> = chunkTnC(brandTnc,48)
           printer?.setAscScale(ASCScale.SC1x1)
           printer?.setAscSize(ASCSize.DOT24x8)
            for (st in chunk) {
               logger("Brand Tnc", st, "e")
               /*    sigleLineText(
                st.replace(bankEMIFooterTAndCSeparator, "")
                    .replace(Companion.disclaimerIssuerClose, ""), AlignMode.CENTER
            )*/
//               printer?.setHzScale(HZScale.SC1x1)
//               printer?.setHzSize(HZSize.DOT24x16)

               // printer?.setPrintFormat(PrintFormat.FORMAT_MOREDATAPROC, PrintFormat.VALUE_MOREDATAPROC_PRNTOEND)
//               printer?.addText(
//                   AlignMode.LEFT, st.replace(bankEMIFooterTAndCSeparator, "")
//                       .replace(Companion.disclaimerIssuerClose, "")
//               )
            textBlockList.add(
                sigleLineformat(
                    st.replace(bankEMIFooterTAndCSeparator, "")
                        .replace(Companion.disclaimerIssuerClose, ""), AlignMode.LEFT
                )
            )
            printer?.addMixStyleText(textBlockList)
            textBlockList.clear()

           }

           // reset printer font
           printer?.setAscSize(ASCSize.DOT24x12)
       }
        //endregion
        //region=====================SCHEME TAndC===============
        //val emiCustomerConsent = bankEMIIssuerTAndCDataModal?.schemeTAndC?.split(SplitterTypes.POUND.splitter)
        val emiCustomerConsent = batchTable?.bankEmiTAndC?.split(SplitterTypes.POUND.splitter)
        logger("getting emiCustomerConsent tnc=",emiCustomerConsent.toString(),"e")
        if (emiCustomerConsent?.isNotEmpty() == true) {
            for (i in emiCustomerConsent?.indices) {
                val limit = 48
                if (!(emiCustomerConsent?.get(i).isNullOrBlank())) {
                    val emiTnc = "#" + (emiCustomerConsent?.get(i) ?: "")
                    val chunks: List<String> = chunkTnC(emiTnc, limit)
                    printer?.setAscScale(ASCScale.SC1x1)
                    printer?.setAscSize(ASCSize.DOT24x8)
                    for (st in chunks) {
                        logger("emiCustomerConsent", st, "e")
                 /*       textBlockList.add(
                            sigleLineformat(
                                st.replace(bankEMIFooterTAndCSeparator, "")
                                    .replace(Companion.disclaimerIssuerClose, ""), AlignMode.CENTER
                            )
                        )
                        printer?.addMixStyleText(textBlockList)
                        textBlockList.clear()*/
//                        printer?.setHzScale(HZScale.SC1x1)
//                        printer?.setHzSize(HZSize.DOT24x16)
                        //-------------old
                       // printer?.setPrintFormat(PrintFormat.FORMAT_MOREDATAPROC, PrintFormat.VALUE_MOREDATAPROC_PRNTOEND)
//                        printer?.addText( AlignMode.LEFT,  st.replace(bankEMIFooterTAndCSeparator, "")
//                            .replace(Companion.disclaimerIssuerClose, ""))

                        textBlockList.add(
                            sigleLineformat(st.replace(bankEMIFooterTAndCSeparator, "")
                                .replace(Companion.disclaimerIssuerClose, ""), AlignMode.LEFT
                            )
                        )
                        printer?.addMixStyleText(textBlockList)
                        textBlockList.clear()
                    }
                }

            }
        } else {
            printSeperator()
            textBlockList.add(sigleLineformat("Scheme:", AlignMode.LEFT))
            if (batchTable != null) {
                textBlockList.add(
                    sigleLineformat(
                        batchTable.tenureLabel,
                        AlignMode.RIGHT
                    )
                )
            }
            printer?.addMixStyleText(textBlockList)
            textBlockList.clear()

            textBlockList.add(sigleLineformat("Card Issuer:", AlignMode.LEFT))
            if (batchTable != null) {
                textBlockList.add(
                    sigleLineformat(
                        batchTable.issuerName,
                        AlignMode.RIGHT
                    )
                )
            }
            printer?.addMixStyleText(textBlockList)
            textBlockList.clear()

        }
//endregion

        //region=====================Printing Merchant Brand Purchase Details:-
        /*if (batchTable.transactionType.equals(EDashboardItem.BRAND_EMI.title)) {
        //region====================Printing DBD Wise TAndC Brand EMI==================
            if (!isNoEmiOnlyCashBackApplied!!) {
                if (copyType?.equals(EPrintCopyType.MERCHANT) == true && (batchTable?.mobileNumberBillNumberFlag?.get(
                        3
                    ) == '1')
                ) {
                    if (!TextUtils.isEmpty(batchTable?.tenureWiseDBDTAndC)) {
                        val tenureWiseTAndC: String? = batchTable?.tenureWiseDBDTAndC
                        if (tenureWiseTAndC != null) {
                            logger("Brand Tnc", tenureWiseTAndC, "e")
                            val chunk: List<String> = chunkTnC(tenureWiseTAndC,48)
                            if (tenureWiseTAndC != null) {
                                printer?.setAscScale(ASCScale.SC1x1)
                                printer?.setAscSize(ASCSize.DOT24x8)
                                for (st in chunk) {
                                    logger("tenureWiseDBDTAndC", st, "e")
                                    textBlockList.add(sigleLineformat(st, AlignMode.LEFT))
                                    printer?.addMixStyleText(textBlockList)
                                    textBlockList.clear()

                                }
                            }
                        }

                        //val tenureWiseTAndC: List<String>? = bankEMITenureDataModal?.tenureWiseDBDTAndC?.let { chunkTnC(it) }

                    }

                    // reset printer font
                    printer?.setAscSize(ASCSize.DOT24x12)
                }

            }

        }*/


        }
        printSeperator()
        if (batchTable.transactionType != BhTransactionType.BRAND_EMI.type )
        baseAmounthandling(batchTable)

    }

    private fun printProduactData(batchTable: TempBatchFileDataTable){

//        val brandEMIMasterDataModal: BrandEMIMasterDataModal? = batchTable.emiBrandData
//        val brandEMISubCategoryTable: BrandEMISubCategoryTable? = batchTable.emiSubCategoryData
//        val brandEMICategoryData: BrandEMISubCategoryTable? = batchTable.emiCategoryData
//        val brandEMIProductDataModal: BrandEMIProductDataModal? = batchTable.emiProductData
//        val bankEMITenureDataModal: BankEMITenureDataModal? = batchTable.emiTenureDataModel
//        val bankEMIIssuerTAndCDataModal: BankEMIIssuerTAndCDataModal? = batchTable.emiIssuerDataModel

        val issuerId = batchTable?.issuerId
        sigleLineText("-----**Product Details**-----", AlignMode.CENTER)
        if (batchTable.brandEMIDataModal != null) {
            textBlockList.add(sigleLineformat("Mer/Mfr Name:", AlignMode.LEFT))
            textBlockList.add(
                sigleLineformat(
                    "${batchTable.brandEMIDataModal?.brandName}",
                    AlignMode.RIGHT
                )
            )
            printer?.addMixStyleText(textBlockList)
            textBlockList.clear()

            textBlockList.add(sigleLineformat("Prod Cat:", AlignMode.LEFT))
            textBlockList.add(
                sigleLineformat(
                    "${batchTable.brandEMIDataModal?.categoryName}",
                    AlignMode.RIGHT
                )
            )
            printer?.addMixStyleText(textBlockList)
            textBlockList.clear()


            if (batchTable.brandEMIDataModal?.producatDesc == "subCat") {
                if (!batchTable.brandEMIDataModal?.productCategoryName.isNullOrEmpty()) {

                    textBlockList.add(sigleLineformat("Prod desc:", AlignMode.LEFT))
                    textBlockList.add(
                        sigleLineformat(
                            "${batchTable.brandEMIDataModal?.productCategoryName}",
                            AlignMode.RIGHT
                        )
                    )
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()
                }
            }

            /*if (brandEMIMasterDataModal.producatDesc == "subCat") {
                if (!brandEMIMasterDataModal.childSubCategoryName.isNullOrEmpty()) {
                    printer?.addText(
                        textInLineFormatBundle,
                        formatTextLMR("Prod desc", ":", brandEmiData.childSubCategoryName, 10)
                    )
                }
            }*/


            textBlockList.add(sigleLineformat("Prod Name:", AlignMode.LEFT))
            textBlockList.add(
                sigleLineformat(
                    "${batchTable.brandEMIDataModal?.productName}",
                    AlignMode.RIGHT
                )
            )
            printer?.addMixStyleText(textBlockList)
            textBlockList.clear()

            if (!TextUtils.isEmpty(batchTable.brandEMIDataModal?.imeiORserailNum)) {
                textBlockList.add(sigleLineformat("Prod ${"IEMI"}:", AlignMode.LEFT))
                textBlockList.add(
                    sigleLineformat(
                        "${batchTable.brandEMIDataModal?.imeiORserailNum}",
                        AlignMode.RIGHT
                    )
                )
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

            }
            val mobnum=batchTable.merchantMobileNumber?:""
            if (!TextUtils.isEmpty(mobnum)) {
                when (batchTable.brandEMIDataModal.mobileNumberBillNumberFlag.substring(1, 2)) {
                    "1" -> {
                        // MASK PRINT
                        val maskedMob = panMasking(
                            mobnum,
                            "000****000"
                        )
                        textBlockList.add(sigleLineformat("Mobile No:", AlignMode.LEFT))
                        textBlockList.add(
                            sigleLineformat(
                                maskedMob,
                                AlignMode.RIGHT
                            )
                        )
                        printer?.addMixStyleText(textBlockList)
                        textBlockList.clear()


                    }
                    //PLAIN PRINT
                    "2" -> {

                        textBlockList.add(sigleLineformat("Mobile No:", AlignMode.LEFT))
                        textBlockList.add(
                            sigleLineformat(
                                mobnum,
                                AlignMode.RIGHT
                            )
                        )
                        printer?.addMixStyleText(textBlockList)
                        textBlockList.clear()
                    }
                    else -> {
                        // NO PRINT
                    }
                }
            }

            //region====================Printing Tenure TAndC==================
            if (!TextUtils.isEmpty(batchTable?.tenureTAndC)) {
                printSeperator()
                val tenureTAndC: String? = batchTable?.tenureTAndC
                val chunk: List<String>? = tenureTAndC?.let { chunkTnC(it,48) }
                if (tenureTAndC != null) {
                    if (chunk != null) {
                        for (st in chunk) {
                            logger("TNC", st, "e")
                            printer?.setAscScale(ASCScale.SC1x1)
                            printer?.setAscSize(ASCSize.DOT24x8)

                            textBlockList.add(
                                sigleLineformat(st, AlignMode.LEFT
                                )
                            )
                            printer?.addMixStyleText(textBlockList)
                            textBlockList.clear()
                        }
                    }
                }
                printer?.setAscSize(ASCSize.DOT24x12)

                printer?.setAscScale(ASCScale.SC1x1)
                printer?.setAscSize(ASCSize.DOT24x8)
            }
            //endregion





        }

    }
    private fun baseAmounthandling(batchTable: TempBatchFileDataTable){

//        val bankEMITenureDataModal: BankEMITenureDataModal? = batchTable.emiTenureDataModel
//        val bankEMIIssuerTAndCDataModal: BankEMIIssuerTAndCDataModal? = batchTable.emiIssuerDataModel
        var currencySymbol: String? = "Rs"
        val terminalData = getTptData()
        currencySymbol = terminalData?.currencySymbol
        if (!TextUtils.isEmpty(batchTable?.transactionalAmmount)) {
            var baseAmount =  "%.2f".format((((batchTable?.transactionalAmmount)?.toDouble())?.div(100)).toString().toDouble())
            if (batchTable.transactionType == BhTransactionType.TEST_EMI.type){
                 baseAmount = "1.00"

            }

            textBlockList.add(sigleLineformat("BASE AMOUNT:", AlignMode.LEFT))

                textBlockList.add(
                    sigleLineformat(
                        "$currencySymbol:${"%.2f".format(baseAmount.toDoubleOrNull())}",
                        AlignMode.RIGHT
                    )
                )
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

        }
    }

    fun printPendingPreAuth(context: Context?,listPendingPreauthData:ArrayList<PendingPreauthData>,printerCallback: (Boolean, Int) -> Unit){

        try{

            try {

                var tpt = Utility().getTptData()

                var item = listPendingPreauthData.get(0)
                //headerPrinting(batchTable.hostBankID)
                headerPrinting(item.bankId)



                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val timeFormat2 = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                try {

                    var date = dateFormat.format(Date())
                    var time = timeFormat2.format(Date())
                    logger("date",date)
                    logger("time",time)

                    textBlockList.add(sigleLineformat("DATE:${date}", AlignMode.LEFT))
                    textBlockList.add(sigleLineformat("TIME:${time}", AlignMode.RIGHT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()

                } catch (e: ParseException) {
                    e.printStackTrace()
                }


                textBlockList.add(sigleLineformat("MID:${tpt?.merchantId}", AlignMode.LEFT))
                textBlockList.add(sigleLineformat("TID:${tpt?.terminalId}", AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

               // getTransactionTypeName(batchTable.transactionType)?.let { sigleLineText(it, AlignMode.CENTER) }
                sigleLineText("PRE-AUTH TXN", AlignMode.CENTER)
                printSeperator()


                for(item in listPendingPreauthData){
                    textBlockList.add(
                        sigleLineformat(
                            "BATCH NO:${invoiceWithPadding(item.batch.toString())}",
                            AlignMode.LEFT
                        )
                    )
                    textBlockList.add(sigleLineformat("ROC:${invoiceWithPadding(item.roc.toString())}", AlignMode.RIGHT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()


                    textBlockList.add(
                        sigleLineformat(
                            "PAN:${item.pan}",
                            AlignMode.LEFT
                        )
                    )
                    Log.e("item.amount",item.amount.toString())
                    //textBlockList.add(sigleLineformat("AMT:${item.amount}", AlignMode.RIGHT))
                    textBlockList.add(sigleLineformat("AMT:${"%.2f".format(item.amount)}", AlignMode.RIGHT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()


                    textBlockList.add(
                        sigleLineformat(
                            "DATE:${item.date}",
                            AlignMode.LEFT
                        )
                    )
                    textBlockList.add(sigleLineformat("TIME:${item.time}", AlignMode.RIGHT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()

                    printSeperator()
                }

                sigleLineText(footerText[1], AlignMode.CENTER)
                val bhlogo: ByteArray? = context?.let { printLogo(it, "BH.bmp") }
                printer?.addBmpImage(0, FactorMode.BMP1X1, bhlogo)
                sigleLineText(
                    "App Version :${BuildConfig.VERSION_NAME}",
                    AlignMode.CENTER
                )

            }catch (ex:Exception){
                ex.printStackTrace()
            }

            printer?.setPrnGray(3)
            printer?.feedLine(5)
            printer?.startPrint(object : OnPrintListener.Stub() {
                @Throws(RemoteException::class)
                override fun onFinish() {
                    printerCallback(true, 0)  //
                }

                @Throws(RemoteException::class)
                override fun onError(error: Int) {
                    printerCallback(true, 0) //
                }
            })

        }catch (ex:Exception){
            ex.printStackTrace()
        }
    }


    fun printDetailReportupdate(
        batch: MutableList<TempBatchFileDataTable>,
        context: Context?,
        printCB: (Boolean) -> Unit
    ) {
        try {
            var isFirstTimeForAmxLogo = true
            val pp = printer?.status
            Log.e("Printer Status", pp.toString())
            if (pp == 0) {

                val appVersion = BuildConfig.VERSION_NAME
                val tpt = getTptData()
                batch.sortBy { it.tid }

                if (batch.isEmpty()) {
                    // alignLeftRightText(textInLineFormatBundle, "MID : ${tpt?.merchantId}", "TID : ${tpt?.terminalId}")
                }
                else {
                    //-----------------------------------------------
                    // setLogoAndHeader() // handel in headerPrintiong methid
                  /*  receiptDetail.merAddHeader1?.let { sigleLineText(it, AlignMode.CENTER) }
                    receiptDetail.merAddHeader2?.let { sigleLineText(it, AlignMode.CENTER) }*/
//                    val ingtpt=getInitdataList()
//                    val header1= ingtpt?.merAddHeader1
//                    val header2=ingtpt?.merAddHeader2
//                    val merchantName=ingtpt?.merchantName.toString().trim()
//
//                    sigleLineText(merchantName, AlignMode.CENTER)
//                    sigleLineText(hexString2String(header1?:"").trim(), AlignMode.CENTER)
//                    sigleLineText(hexString2String(header2?:"").trim(), AlignMode.CENTER)

                    // headerPrinting()
                    val isHdfcPresent = batch.find{ it.hostBankID.equals(HDFC_BANK_CODE_SINGLE_DIGIT) || it.hostBankID.equals(HDFC_BANK_CODE)}
                    val isAmexPresent = batch.find{ it.hostBankID.equals(AMEX_BANK_CODE) || it.hostBankID.equals(AMEX_BANK_CODE_SINGLE_DIGIT)}
                    if(isHdfcPresent?.hostBankID.equals(HDFC_BANK_CODE) || isHdfcPresent?.hostBankID.equals(HDFC_BANK_CODE_SINGLE_DIGIT)){
                        headerPrinting(HDFC_BANK_CODE)}
                    else if(isAmexPresent?.hostBankID.equals(AMEX_BANK_CODE) || isAmexPresent?.hostBankID.equals(AMEX_BANK_CODE_SINGLE_DIGIT)){
                        headerPrinting(AMEX_BANK_CODE)
                        isFirstTimeForAmxLogo = false
                    }else{
                        headerPrinting(DEFAULT_BANK_CODE)
                    }




                    //  ------------------------------------------
                    val td = System.currentTimeMillis()
                    val formatdate = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                    val formattime = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)

                    val date = formatdate.format(td)
                    val time = formattime.format(td)


                    textBlockList.add(sigleLineformat("DATE:${date}", AlignMode.LEFT))
                    textBlockList.add(sigleLineformat("TIME:${time}", AlignMode.RIGHT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()
                    sigleLineText("DETAIL REPORT", AlignMode.CENTER)

                    val terminalData = getTptData()

                    textBlockList.add(
                        sigleLineformat(
                            "MID:${terminalData?.merchantId}",
                            AlignMode.LEFT
                        )
                    )
                    textBlockList.add(
                        sigleLineformat(
                            "TID:${batch[0].tid}",
                            AlignMode.RIGHT
                        )
                    )
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()

                    textBlockList.add(
                        sigleLineformat(
                            "BATCH NO:${batch[0].hostBatchNumber}",
                            AlignMode.LEFT
                        )
                    )
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()

                    printSeperator()
                }

                if (batch.isEmpty()) {
                    // alignLeftRightText(textInLineFormatBundle, "Total Transaction", "0")
                } else {
                    textBlockList.add(sigleLineformat("TRANS-TYPE", AlignMode.LEFT))
                    textBlockList.add(sigleLineformat("AMOUNT", AlignMode.RIGHT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()

                    textBlockList.add(sigleLineformat("ISSUER", AlignMode.LEFT))
                    textBlockList.add(sigleLineformat("PAN/CID", AlignMode.RIGHT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()

                    textBlockList.add(sigleLineformat("DATE-TIME", AlignMode.LEFT))
                    textBlockList.add(sigleLineformat("INVOICE", AlignMode.RIGHT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()
                    printSeperator()

                    val totalMap = mutableMapOf<Int, SummeryTotalType>()
                    val deformatter = SimpleDateFormat("yyMMdd HHmmss", Locale.ENGLISH)

                    var frequency = 0
                    var count = 0
                    var lastfrequecny = 0
                    var hasfrequency = false
                    var updatedindex = 0
                    var iteration = 0
                    val frequencylist = mutableListOf<String>()
                    val tidlist = mutableListOf<String>()

                    for (item in batch) {
                        item.tid.let {
                            if (it != null) {
                                tidlist.add(it)
                            }
                        }
                    }
                    for (item in tidlist.distinct()) {
                        println(
                            "Frequency of item" + item + ": " + Collections.frequency(
                                tidlist,
                                item
                            )
                        )
                        frequencylist.add("" + Collections.frequency(tidlist, item))
                    }

                    iteration = tidlist.distinct().size - 1
                    var pre_authLastItem:Boolean=false

                    for (b in batch) {
                        //  || b.transactionType == TransactionType.VOID_PREAUTH.type
                        if (updatedindex <= frequencylist.size - 1)
                            frequency = frequencylist[updatedindex].toInt() + lastfrequecny
                        count++
                        if (b.transactionType == BhTransactionType.PRE_AUTH.type) {

                            if(count<frequency || count==1)
                                continue  // Do not add pre auth transactions
                            else {
                                pre_authLastItem = true // just to hanlde if pre-auth is last item
                            }

                        }
                        if(!pre_authLastItem) {
                            if (b.transactionType == BhTransactionType.EMI_SALE.type || b.transactionType == BhTransactionType.BRAND_EMI.type || b.transactionType == BhTransactionType.BRAND_EMI_BY_ACCESS_CODE.type) {
                                b.transactionType = BhTransactionType.EMI_SALE.type
                            }

                            if (b.transactionType == BhTransactionType.TEST_EMI.type) {
                                b.transactionType = BhTransactionType.SALE.type
                            }





                            if (totalMap.containsKey(b.transactionType)) {
                                val x = totalMap[b.transactionType]
                                if (x != null) {
                                    x.count += 1
                                    x.total += b.transactionalAmmount?.toLong()!!
                                }
                            } else {
                                if(b.transactionalAmmount != null && !b.transactionalAmmount.equals("null")){
                                    totalMap[b.transactionType] =
                                        b.transactionalAmmount?.toLong()
                                            ?.let { SummeryTotalType(1, it) }!!
                                }else{
                                    logger("vd","vd")
                                }


                            }
                            var transAmount = "0.0"
                            if(b.transactionalAmmount != null && !b.transactionalAmmount.equals("null")){
                                transAmount = "%.2f".format(
                                    b.transactionalAmmount?.toDouble()
                                        ?.div(100)
                                )
                            }

                            if (b.transationName.equals("TEST EMI TXN")) {
                                textBlockList.add(
                                    sigleLineformat(
                                        "${"SALE"}",
                                        AlignMode.LEFT
                                    )
                                )
                            } else {
                                textBlockList.add(
                                    sigleLineformat(
                                        "${b.transationName}",
                                        AlignMode.LEFT
                                    )
                                )
                            }

                        textBlockList.add(sigleLineformat(transAmount, AlignMode.RIGHT))
                        printer?.addMixStyleText(textBlockList)
                        textBlockList.clear()
                        if (b.transactionType == BhTransactionType.VOID_PREAUTH.type) {
                            textBlockList.add(
                                sigleLineformat(
                                    "${b.appName}",
                                    AlignMode.LEFT
                                )
                            )
                            textBlockList.add(
                                sigleLineformat(
                                    "${b.panMask}",
                                    AlignMode.RIGHT
                                )
                            )
                            printer?.addMixStyleText(textBlockList)
                            textBlockList.clear()
                        } else {
                            textBlockList.add(
                                sigleLineformat(
                                    "${b.cardType}",
                                    AlignMode.LEFT
                                )
                            )
                            textBlockList.add(
                                sigleLineformat(
                                    "${b.cardNumber}",
                                    AlignMode.RIGHT
                                )
                            )
                            printer?.addMixStyleText(textBlockList)
                            textBlockList.clear()
                        }
                        if (b.transactionType == BhTransactionType.OFFLINE_SALE.type || b.transactionType == BhTransactionType.VOID_OFFLINE_SALE.type) {
                            try {

                                    //val dat = "${b.dateTime}"
                                    val dat = "${b.printDate} - ${b.time}"
                                    textBlockList.add(sigleLineformat(dat, AlignMode.LEFT))
                                    b.hostInvoice?.let { invoiceWithPadding(it) }?.let {
                                        sigleLineformat(
                                            it, AlignMode.RIGHT
                                        )
                                    }?.let { textBlockList.add(it) }
                                    printer?.addMixStyleText(textBlockList)
                                    textBlockList.clear()


                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                }

                            } else {
//                                val date = b.dateTime
//                                val parts = date?.split(" ")
//                                println("Date: " + parts!![0])
//                                println("Time: " + (parts[1]))

                                val timee = b.time
                                val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
                                val timeFormat2 = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                var formattedTime = ""

                                try {

                                    //val dat = "${parts!![0]} - ${parts[1]}"

                                    val t1 = timeFormat.parse(timee)
                                    formattedTime = timeFormat2.format(t1)
                                    Log.e("Time", formattedTime)
                                    val dat = "${b.transactionDate} - $formattedTime"

                                    textBlockList.add(sigleLineformat(dat, AlignMode.LEFT))

                                    b.hostInvoice?.let { invoiceWithPadding(it) }?.let {
                                        sigleLineformat(
                                            it, AlignMode.RIGHT
                                        )
                                    }?.let { textBlockList.add(it) }
                                    printer?.addMixStyleText(textBlockList)
                                    textBlockList.clear()
                                    //alignLeftRightText(textInLineFormatBundle," "," ")
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                }
                            }

                            printSeperator()
                        }
                        if (frequency == count) {
                            lastfrequecny = frequency
                            hasfrequency = true
                            updatedindex++
                        } else {
                            hasfrequency = false
                        }
                        if (hasfrequency) {


                            sigleLineText("***TOTAL TRANSACTIONS***", AlignMode.CENTER)
                            val sortedMap = totalMap.toSortedMap(compareByDescending { it })

                            for ((k, m) in sortedMap) {
                                textBlockList.add(
                                    sigleLineformat(
                                        transactionType2Name(k).toUpperCase(
                                            Locale.ROOT
                                        ), AlignMode.LEFT
                                    )
                                )
                                textBlockList.add(
                                    /*sigleLineformat(
                                        "=" + m.count + " ${
                                            getCurrencySymbol(
                                                tpt
                                            )
                                        }", AlignMode.CENTER
                                    )*/
                                            sigleLineformat("= ${m.count}" , AlignMode.CENTER)
                                )
                                /*textBlockList.add(
                                    sigleLineformat(
                                        "%.2f".format(
                                            (((m.total).toDouble()).div(
                                                100
                                            )).toString().toDouble()
                                        ), AlignMode.RIGHT
                                    )
                                )*/
                                textBlockList.add(sigleLineformat("${getCurrencySymbol(tpt)}:${"%.2f".format((((m.total).toDouble()).div(100)).toString().toDouble())}", AlignMode.RIGHT)
                                )
                                printer?.addMixStyleText(textBlockList)
                                textBlockList.clear()
                            }
                            val terminalData = getTptData()
                            if (iteration > 0) {
                                printSeperator()

                                // handling printing logo
                                val bankId = batch[frequency].hostBankID
                                var logo = ""
                                if (bankId == AMEX_BANK_CODE_SINGLE_DIGIT || bankId == AMEX_BANK_CODE) {
                                    AMEX_LOGO
                                } else if (bankId == HDFC_BANK_CODE_SINGLE_DIGIT || bankId == HDFC_BANK_CODE){
                                    logo = HDFC_LOGO
                                }else{
                                    logo = ""
                                }

                                if (isFirstTimeForAmxLogo && logo != null && !logo.equals("")) {
                                    isFirstTimeForAmxLogo = false
                                    printLogo(context!!,logo)
                                    printSeperator()
                                }
                                // end region

                                textBlockList.add(
                                    sigleLineformat(
                                        "MID:${terminalData?.merchantId}",
                                        AlignMode.LEFT
                                    )
                                )
                                textBlockList.add(
                                    sigleLineformat(
                                        "TID:${batch[frequency].tid}",
                                        AlignMode.RIGHT
                                    )
                                )
                                printer?.addMixStyleText(textBlockList)
                                textBlockList.clear()
                                textBlockList.add(
                                    sigleLineformat(
                                        "BATCH NO:${batch[frequency].batchNumber}",
                                        AlignMode.LEFT
                                    )
                                )
                                printer?.addMixStyleText(textBlockList)
                                textBlockList.clear()
                                printSeperator()
                                iteration--
                            }

                            totalMap.clear()
                        }
                    }

                }

                //endregion
                // region === Below code is execute when digi txns are available on POS
                val digiPosDataList =
                    selectDigiPosDataAccordingToTxnStatus(EDigiPosPaymentStatus.Approved.desciption) as ArrayList<DigiPosDataTable>
                if (digiPosDataList.isNotEmpty()) {
                    printSeperator()
                    // centerText(textFormatBundle, "---------X-----------X----------")

                    sigleLineText("Digi Pos Detail Report", AlignMode.CENTER)

                    tpt?.terminalId?.let { sigleLineText( "TID : $it",AlignMode.CENTER) }
                    printSeperator()
                    // Txn description
                    textBlockList.add(sigleLineformat("MODE", AlignMode.LEFT))
                    textBlockList.add(sigleLineformat("AMOUNT(INR)", AlignMode.RIGHT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()
                    textBlockList.add(sigleLineformat("PartnetTxnId", AlignMode.LEFT))
                    textBlockList.add(sigleLineformat("DATE-TIME", AlignMode.RIGHT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()
                    textBlockList.add(sigleLineformat("mTxnId", AlignMode.LEFT))
                    textBlockList.add(sigleLineformat("pgwTxnId", AlignMode.RIGHT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()
                    printSeperator()
                    //Txn Detail
                    for (digiPosData in digiPosDataList) {

                        textBlockList.add(sigleLineformat( digiPosData.paymentMode, AlignMode.LEFT))
                        textBlockList.add(sigleLineformat( digiPosData.amount, AlignMode.RIGHT))
                        printer?.addMixStyleText(textBlockList)
                        textBlockList.clear()
                        textBlockList.add(sigleLineformat(digiPosData.partnerTxnId, AlignMode.LEFT))
                        textBlockList.add(sigleLineformat( digiPosData.txnDate + "  " + digiPosData.txnTime, AlignMode.RIGHT))
                        printer?.addMixStyleText(textBlockList)
                        textBlockList.clear()

                        textBlockList.add(sigleLineformat(  digiPosData.mTxnId, AlignMode.LEFT))
                        textBlockList.add(sigleLineformat(  digiPosData.pgwTxnId, AlignMode.RIGHT))
                        printer?.addMixStyleText(textBlockList)
                        textBlockList.clear()

                        sigleLineText("----------------------------------------", AlignMode.CENTER)
                    }
                    //   DigiPosDataTable.deletAllRecordAccToTxnStatus(EDigiPosPaymentStatus.Approved.desciption)
                }
                //endregion


                if (batch.isNotEmpty()) {
                    printSeperator()
                    sigleLineText("Bonushub", AlignMode.CENTER)
                    sigleLineText("App Version:${BuildConfig.VERSION_NAME}", AlignMode.CENTER)
                    printer?.feedLine(4)
                }
                printer?.startPrint(object : OnPrintListener.Stub() {
                    @Throws(RemoteException::class)
                    override fun onFinish() {
                        Log.e("DEATIL REPORT", "SUCESS__")
                        printCB(true)
                    }

                    @Throws(RemoteException::class)
                    override fun onError(error: Int) {
                        Log.e("DEATIL REPORT", "FAIL__")
                        printCB(false)
                    }
                })
            }
        } catch (ex: DeadObjectException) {
            ex.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        } catch (e: RemoteException) {
            e.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        } finally {
//   VFService.connectToVFService(VerifoneApp.appContext)
        }
    }

    fun printReversalReportupdate(
        batch: MutableList<BatchTableReversal>,
        context: Context?,
        printCB: (Boolean) -> Unit
    ) {
        try {
            val pp = printer?.status
            Log.e("Printer Status", pp.toString())
            if (pp == 0) {

                val appVersion = BuildConfig.VERSION_NAME
                val tpt = getTptData()
                batch.sortBy { it.receiptData?.tid }

                if (batch.isEmpty()) {
                    // alignLeftRightText(textInLineFormatBundle, "MID : ${tpt?.merchantId}", "TID : ${tpt?.terminalId}")
                } else {
                    //-----------------------------------------------
                    setLogoAndHeader()
                    /*  receiptDetail.merAddHeader1?.let { sigleLineText(it, AlignMode.CENTER) }
                      receiptDetail.merAddHeader2?.let { sigleLineText(it, AlignMode.CENTER) }*/
//                    val ingtpt=getInitdataList()
//                    val header1= ingtpt?.merAddHeader1
//                    val header2=ingtpt?.merAddHeader2
//                    val merchantName=ingtpt?.merchantName.toString().trim()
//
//                    sigleLineText(merchantName, AlignMode.CENTER)
//                    sigleLineText(hexString2String(header1?:"").trim(), AlignMode.CENTER)
//                    sigleLineText(hexString2String(header2?:"").trim(), AlignMode.CENTER)

                    headerPrinting()

                    //  ------------------------------------------
                    val td = System.currentTimeMillis()
                    val formatdate = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                    val formattime = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)

                    val date = formatdate.format(td)
                    val time = formattime.format(td)


                    textBlockList.add(sigleLineformat("DATE:${date}", AlignMode.LEFT))
                    textBlockList.add(sigleLineformat("TIME:${time}", AlignMode.RIGHT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()
                    sigleLineText("REVERSAL REPORT", AlignMode.CENTER)

                    val terminalData = getTptData()

                    textBlockList.add(
                        sigleLineformat(
                            "MID:${terminalData?.merchantId}",
                            AlignMode.LEFT
                        )
                    )
                    textBlockList.add(
                        sigleLineformat(
                            "TID:${batch[0].receiptData?.tid}",
                            AlignMode.RIGHT
                        )
                    )
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()

                    textBlockList.add(
                        sigleLineformat(
                            "BATCH NO:${batch[0].receiptData?.batchNumber}",
                            AlignMode.LEFT
                        )
                    )
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()

                    printSeperator()
                }

                if (batch.isEmpty()) {
                    // alignLeftRightText(textInLineFormatBundle, "Total Transaction", "0")
                } else {
                    textBlockList.add(sigleLineformat("TRANS-TYPE", AlignMode.LEFT))
                    textBlockList.add(sigleLineformat("AMOUNT", AlignMode.RIGHT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()

                    textBlockList.add(sigleLineformat("ISSUER", AlignMode.LEFT))
                    textBlockList.add(sigleLineformat("PAN/CID", AlignMode.RIGHT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()

                    textBlockList.add(sigleLineformat("DATE-TIME", AlignMode.LEFT))
                    textBlockList.add(sigleLineformat("INVOICE", AlignMode.RIGHT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()
                    printSeperator()

                    val totalMap = mutableMapOf<Int, SummeryTotalType>()
                    val deformatter = SimpleDateFormat("yyMMdd HHmmss", Locale.ENGLISH)

                    var frequency = 0
                    var count = 0
                    var lastfrequecny = 0
                    var hasfrequency = false
                    var updatedindex = 0
                    var iteration = 0
                    val frequencylist = mutableListOf<String>()
                    val tidlist = mutableListOf<String>()

                    for (item in batch) {
                        item.receiptData?.tid.let {
                            if (it != null) {
                                tidlist.add(it)
                            }
                        }
                    }
                    for (item in tidlist.distinct()) {
                        println(
                            "Frequency of item" + item + ": " + Collections.frequency(
                                tidlist,
                                item
                            )
                        )
                        frequencylist.add("" + Collections.frequency(tidlist, item))
                    }

                    iteration = tidlist.distinct().size - 1

                    for (b in batch) {
                        //  || b.transactionType == TransactionType.VOID_PREAUTH.type
                        if (b.transactionType == BhTransactionType.PRE_AUTH.type) continue  // Do not add pre auth transactions

                        if (b.transactionType == BhTransactionType.EMI_SALE.type || b.transactionType == BhTransactionType.BRAND_EMI.type || b.transactionType == BhTransactionType.BRAND_EMI_BY_ACCESS_CODE.type) {
                            b.transactionType = BhTransactionType.EMI_SALE.type
                        }

                        if (b.transactionType == BhTransactionType.TEST_EMI.type) {
                            b.transactionType = BhTransactionType.SALE.type
                        }

                        count++
                        if (updatedindex <= frequencylist.size - 1)
                            frequency = frequencylist[updatedindex].toInt() + lastfrequecny


                        if (totalMap.containsKey(b.transactionType)) {
                            val x = totalMap[b.transactionType]
                            if (x != null) {
                                x.count += 1
                                x.total += b.receiptData?.txnAmount?.toLong()!!
                            }
                        } else {

                            totalMap[b.transactionType] =
                                b.receiptData?.txnAmount?.toLong()
                                    ?.let { SummeryTotalType(1, it) }!!
                        }
                        val transAmount = "%.2f".format(
                            b.receiptData?.txnAmount?.toDouble()
                                ?.div(100)
                        )
                        if(b.receiptData?.txnName.equals("TEST EMI TXN")){
                            textBlockList.add(
                                sigleLineformat(
                                    "${"SALE"}",
                                    AlignMode.LEFT
                                )
                            )
                        }else{
                            textBlockList.add(
                                sigleLineformat(
                                    "${b.receiptData?.txnName}",
                                    AlignMode.LEFT
                                )
                            )
                        }

                            textBlockList.add(sigleLineformat("$transAmount", AlignMode.RIGHT))
                            printer?.addMixStyleText(textBlockList)
                            textBlockList.clear()
                            if (b.transactionType == BhTransactionType.VOID_PREAUTH.type) {
                                textBlockList.add(
                                    sigleLineformat(
                                        "${b.receiptData?.appName}",
                                        AlignMode.LEFT
                                    )
                                )
                                textBlockList.add(
                                    sigleLineformat(
                                        "${b.receiptData?.maskedPan}",
                                        AlignMode.RIGHT
                                    )
                                )
                                printer?.addMixStyleText(textBlockList)
                                textBlockList.clear()
                            } else {
                                textBlockList.add(
                                    sigleLineformat(
                                        "${b.receiptData?.appName}",
                                        AlignMode.LEFT
                                    )
                                )
                                textBlockList.add(
                                    sigleLineformat(
                                        "${b.receiptData?.maskedPan}",
                                        AlignMode.RIGHT
                                    )
                                )
                                printer?.addMixStyleText(textBlockList)
                                textBlockList.clear()
                            }
                            if (b.transactionType == BhTransactionType.OFFLINE_SALE.type || b.transactionType == BhTransactionType.VOID_OFFLINE_SALE.type) {
                                try {

                                val dat = "${b.receiptData?.dateTime}"
                                textBlockList.add(sigleLineformat(dat, AlignMode.LEFT))
                                b.receiptData?.invoice?.let { invoiceWithPadding(it) }?.let {
                                    sigleLineformat(
                                        it, AlignMode.RIGHT
                                    )
                                }?.let { textBlockList.add(it) }
                                printer?.addMixStyleText(textBlockList)
                                textBlockList.clear()


                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }

                        } else {
                            val date = b.receiptData?.dateTime
                            val parts = date?.split(" ")
                            println("Date: " + parts!![0])
                            println("Time: " + (parts[1]))
                            try {

                                val dat = "${parts!![0]} - ${parts[1]}"
                                textBlockList.add(sigleLineformat(dat, AlignMode.LEFT))
                                b.receiptData?.invoice?.let { invoiceWithPadding(it) }?.let {
                                    sigleLineformat(
                                        it, AlignMode.RIGHT
                                    )
                                }?.let { textBlockList.add(it) }
                                printer?.addMixStyleText(textBlockList)
                                textBlockList.clear()
                                //alignLeftRightText(textInLineFormatBundle," "," ")
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }

                        printSeperator()
                        if (frequency == count) {
                            lastfrequecny = frequency
                            hasfrequency = true
                            updatedindex++
                        } else {
                            hasfrequency = false
                        }
                        if (hasfrequency) {


                            sigleLineText("***TOTAL TRANSACTIONS***", AlignMode.CENTER)
                            val sortedMap = totalMap.toSortedMap(compareByDescending { it })

                            for ((k, m) in sortedMap) {
                                textBlockList.add(
                                    sigleLineformat(
                                        transactionType2Name(k).toUpperCase(
                                            Locale.ROOT
                                        ), AlignMode.LEFT
                                    )
                                )
                                textBlockList.add(
                                    /*sigleLineformat(
                                        "=" + m.count + " ${
                                            getCurrencySymbol(
                                                tpt
                                            )
                                        }", AlignMode.CENTER
                                    )*/
                                    sigleLineformat("= ${m.count}" , AlignMode.CENTER)
                                )
                                /*textBlockList.add(
                                    sigleLineformat(
                                        "%.2f".format(
                                            (((m.total).toDouble()).div(
                                                100
                                            )).toString().toDouble()
                                        ), AlignMode.RIGHT
                                    )
                                )*/
                                textBlockList.add(sigleLineformat("${getCurrencySymbol(tpt)}:${"%.2f".format((((m.total).toDouble()).div(100)).toString().toDouble())}", AlignMode.RIGHT)
                                )
                                printer?.addMixStyleText(textBlockList)
                                textBlockList.clear()
                            }
                            val terminalData = getTptData()
                            if (iteration > 0) {
                                printSeperator()
                                textBlockList.add(
                                    sigleLineformat(
                                        "MID:${terminalData?.merchantId}",
                                        AlignMode.LEFT
                                    )
                                )
                                textBlockList.add(
                                    sigleLineformat(
                                        "TID:${batch[frequency].receiptData?.tid}",
                                        AlignMode.RIGHT
                                    )
                                )
                                printer?.addMixStyleText(textBlockList)
                                textBlockList.clear()
                                textBlockList.add(
                                    sigleLineformat(
                                        "BATCH NO:${batch[frequency].receiptData?.batchNumber}",
                                        AlignMode.LEFT
                                    )
                                )
                                printer?.addMixStyleText(textBlockList)
                                textBlockList.clear()
                                printSeperator()
                                iteration--
                            }

                            totalMap.clear()
                        }
                    }

                }

                //endregion
                // region === Below code is execute when digi txns are available on POS
                val digiPosDataList =
                    selectDigiPosDataAccordingToTxnStatus(EDigiPosPaymentStatus.Approved.desciption) as ArrayList<DigiPosDataTable>
                if (digiPosDataList.isNotEmpty()) {
                    printSeperator()
                    // centerText(textFormatBundle, "---------X-----------X----------")

                    sigleLineText("Digi Pos Detail Report", AlignMode.CENTER)

                    tpt?.terminalId?.let { sigleLineText( "TID : $it",AlignMode.CENTER) }
                    printSeperator()
                    // Txn description
                    textBlockList.add(sigleLineformat("MODE", AlignMode.LEFT))
                    textBlockList.add(sigleLineformat("AMOUNT(INR)", AlignMode.RIGHT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()
                    textBlockList.add(sigleLineformat("PartnetTxnId", AlignMode.LEFT))
                    textBlockList.add(sigleLineformat("DATE-TIME", AlignMode.RIGHT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()
                    textBlockList.add(sigleLineformat("mTxnId", AlignMode.LEFT))
                    textBlockList.add(sigleLineformat("pgwTxnId", AlignMode.RIGHT))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()
                    printSeperator()
                    //Txn Detail
                    for (digiPosData in digiPosDataList) {

                        textBlockList.add(sigleLineformat( digiPosData.paymentMode, AlignMode.LEFT))
                        textBlockList.add(sigleLineformat( digiPosData.amount, AlignMode.RIGHT))
                        printer?.addMixStyleText(textBlockList)
                        textBlockList.clear()
                        textBlockList.add(sigleLineformat(digiPosData.partnerTxnId, AlignMode.LEFT))
                        textBlockList.add(sigleLineformat( digiPosData.txnDate + "  " + digiPosData.txnTime, AlignMode.RIGHT))
                        printer?.addMixStyleText(textBlockList)
                        textBlockList.clear()

                        textBlockList.add(sigleLineformat(  digiPosData.mTxnId, AlignMode.LEFT))
                        textBlockList.add(sigleLineformat(  digiPosData.pgwTxnId, AlignMode.RIGHT))
                        printer?.addMixStyleText(textBlockList)
                        textBlockList.clear()

                        sigleLineText("----------------------------------------", AlignMode.CENTER)
                    }
                    //   DigiPosDataTable.deletAllRecordAccToTxnStatus(EDigiPosPaymentStatus.Approved.desciption)
                }
                //endregion


                if (batch.isNotEmpty()) {
                    printSeperator()
                    sigleLineText("Bonushub", AlignMode.CENTER)
                    sigleLineText("App Version:${BuildConfig.VERSION_NAME}", AlignMode.CENTER)
                    printer?.feedLine(4)
                }
                printer?.startPrint(object : OnPrintListener.Stub() {
                    @Throws(RemoteException::class)
                    override fun onFinish() {
                        Log.e("DEATIL REPORT", "SUCESS__")
                        printCB(true)
                    }

                    @Throws(RemoteException::class)
                    override fun onError(error: Int) {
                        Log.e("DEATIL REPORT", "FAIL__")
                        printCB(false)
                    }
                })
            }
        } catch (ex: DeadObjectException) {
            ex.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        } catch (e: RemoteException) {
            e.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take chargeslip from the Report menu."
            )
        } finally {
//   VFService.connectToVFService(VerifoneApp.appContext)
        }
    }

    // old
    /*fun printSettlementReportupdate(
        context: Context?,
        batch: MutableList<BatchTable>,
        isSettlementSuccess: Boolean = false,
        isLastSummary: Boolean = false,
        callBack: (Boolean) -> Unit
    ) {
//  val format = Bundle()
//   val fmtAddTextInLine = Bundle()

//below if condition is for zero settlement
        if (batch.size <= 0) {
            try {
                val tpt = getTptData()
               // setLogoAndHeader()


                headerPrinting()

                val td = System.currentTimeMillis()
                val formatdate = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                val formattime = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
                val date = formatdate.format(td)
                val time = formattime.format(td)

                textBlockList.add(sigleLineformat("DATE:${date}", AlignMode.LEFT))
                textBlockList.add(sigleLineformat("TIME:${time}", AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

                if (isLastSummary) {

                    sigleLineText("LAST SUMMARY REPORT", AlignMode.CENTER)
                } else {

                    sigleLineText("SUMMARY REPORT", AlignMode.CENTER)
                }


                textBlockList.add(sigleLineformat("TID:${tpt?.terminalId}", AlignMode.LEFT))
                textBlockList.add(sigleLineformat("MID:${tpt?.merchantId}", AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

                textBlockList.add(sigleLineformat("BATCH NO:${tpt?.batchNumber}", AlignMode.LEFT))

                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()


                printSeperator()
                textBlockList.add(sigleLineformat("TOTAL TXN = 0", AlignMode.LEFT))
                textBlockList.add(sigleLineformat("${getCurrencySymbol(tpt)}", AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

                sigleLineText("ZERO SETTLEMENT SUCCESSFUL", AlignMode.CENTER)
                if (!isLastSummary)
                digiposReport()
                sigleLineText("BonusHub", AlignMode.CENTER)
                sigleLineText("App Version", AlignMode.CENTER)

                printer?.feedLine(4)


                printer?.startPrint(object : OnPrintListener.Stub() {
                    @Throws(RemoteException::class)
                    override fun onFinish() {
                        callBack(true)
                        Log.e("Settle_RECEIPT", "SUCESS__")
                    }

                    @Throws(RemoteException::class)
                    override fun onError(error: Int) {
                        callBack(false)
                        Log.e("Settle_RECEIPT", "FAIL__")
                    }
                })
            } catch (ex: DeadObjectException) {
                ex.printStackTrace()
                failureImpl(
                    context as Activity,
                    "Printer Service stopped.",
                    "Please take chargeslip from the Report menu."
                )
            } catch (e: RemoteException) {
                e.printStackTrace()
                failureImpl(
                    context as Activity,
                    "Printer Service stopped.",
                    "Please take chargeslip from the Report menu."
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
                failureImpl(
                    context as Activity,
                    "Printer Service stopped.",
                    "Please take chargeslip from the Report menu."
                )
            }
        }
////below if condition is for settlement(Other than zero settlement)
        else {
            try {
                val map = mutableMapOf<String, MutableMap<Int, SummeryModel>>()
                val map1 = mutableMapOf<String, MutableMap<Int, SummeryModel>>()
                //to hold the tid for which tid mid printed
                val listTidPrinted = mutableListOf<String>()
                val tpt = getTptData()

               // setLogoAndHeader()

                headerPrinting()

                val td = System.currentTimeMillis()
                val formatdate = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                val formattime = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)

                val date = formatdate.format(td)
                val time = formattime.format(td)

                textBlockList.add(sigleLineformat("DATE:${date}", AlignMode.LEFT))
                textBlockList.add(sigleLineformat("TIME:${time}", AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

                //  alignLeftRightText(fmtAddTextInLine,"DATE : ${batch.date}","TIME : ${batch.time}")
                *//*   alignLeftRightText(textInLineFormatBundle, "MID : ${batch[0].mid}", "TID : ${batch[0].tid}")
                   alignLeftRightText(textInLineFormatBundle, "BATCH NO  : ${batch[0].batchNumber}", "")*//*

                if (isLastSummary) {

                    sigleLineText("LAST SUMMARY REPORT", AlignMode.CENTER)
                } else {

                    sigleLineText("SUMMARY REPORT", AlignMode.CENTER)
                }

                batch.sortBy { it.receiptData?.tid }

                var tempTid = batch[0].receiptData?.tid

                val list = mutableListOf<String>()
                val frequencylist = mutableListOf<String>()

                for (it in batch) {  // Do not count preauth transaction
// || it.transactionType == TransactionType.VOID_PREAUTH.type
                    if (it.transactionType == BhTransactionType.PRE_AUTH.type) continue

                    if (it.transactionType == BhTransactionType.EMI_SALE.type ||
                        it.transactionType == BhTransactionType.BRAND_EMI.type ||
                        it.transactionType == BhTransactionType.BRAND_EMI_BY_ACCESS_CODE.type) {
                        it.receiptData?.appName = it.emiIssuerDataModel?.issuerName
                        it.transactionType = BhTransactionType.EMI_SALE.type
                    }
                    if (it.transactionType == BhTransactionType.VOID_EMI.type) {
                        it.receiptData?.appName = it.emiIssuerDataModel?.issuerName
                    }

                    if (it.transactionType == BhTransactionType.TEST_EMI.type) {
                        it.receiptData?.appName = "Test Issuer"
                        it.receiptData?.cardType = "Test Issuer"
                        it.transactionType = BhTransactionType.SALE.type

                    }

                    val transAmt = try {
                        it.receiptData?.txnAmount?.toLong()
                    } catch (ex: Exception) {
                        0L
                    }


                    if (tempTid == it.receiptData?.tid) {
                        _issuerName = it.receiptData?.appName
                        if (map.containsKey(it.receiptData?.tid + it.receiptData?.mid + it.receiptData?.batchNumber + it.receiptData?.appName)) {
                            _issuerName = it.receiptData?.appName

                            val ma =
                                map[it.receiptData?.tid + it.receiptData?.mid + it.receiptData?.batchNumber + it.receiptData?.appName] as MutableMap<Int, SummeryModel>
                            if (ma.containsKey(it.transactionType)) {
                                val m = ma[it.transactionType] as SummeryModel
                                m.count += 1
                                if (transAmt != null) {
                                    m.total = m.total?.plus(transAmt)
                                }
                            } else {
                                val txnName = it.receiptData?.txnName
                                val rtid = it.receiptData?.tid
                                val sm = SummeryModel(
                                    txnName, 1, transAmt, rtid
                                )
                                ma[it.transactionType] = sm
                            }
                        } else {
                            val hm = HashMap<Int, SummeryModel>().apply {
                                this[it.transactionType] = transAmt?.let { it1 ->
                                    it.receiptData?.txnName?.let { it2 ->
                                        it.receiptData?.tid?.let { it3 ->
                                            SummeryModel(
                                                it2,
                                                1,
                                                it1,
                                                it3
                                            )
                                        }
                                    }
                                }!!
                            }
                            map[it.receiptData?.tid + it.receiptData?.mid + it.receiptData?.batchNumber + it.receiptData?.appName] =
                                hm
                            it.receiptData?.tid?.let { it1 -> list.add(it1) }
                        }
                    } else {
                        tempTid = it.receiptData?.tid
                        _issuerName = it.receiptData?.appName
                        val hm = HashMap<Int, SummeryModel>().apply {
                            this[it.transactionType] = transAmt?.let { it1 ->
                                it.receiptData?.txnName?.let { it2 ->
                                    it.receiptData!!.tid?.let { it3 ->
                                        SummeryModel(
                                            it2,
                                            1,
                                            it1,
                                            it3
                                        )
                                    }
                                }
                            }!!
                        }
                        map[it.receiptData?.tid + it.receiptData?.mid + it.receiptData?.batchNumber + it.receiptData?.appName] =
                            hm
                        it.receiptData?.tid?.let { it1 -> list.add(it1) }
                    }

                }

                for (item in list.distinct()) {
                    println("Frequency of item" + item + ": " + Collections.frequency(list, item))
                    frequencylist.add("" + Collections.frequency(list, item))
                }


                val totalMap = mutableMapOf<Int, SummeryTotalType>()


                var ietration = list.distinct().size
                var curentIndex = 0
                var frequency = 0
                var count = 0
                var lastfrequecny = 0
                var hasfrequency = false
                var updatedindex = 0

                for ((key, _map) in map.onEachIndexed { index, entry -> curentIndex = index }) {


                    count++
                    if (updatedindex <= frequencylist.size - 1)
                        frequency = frequencylist.get(updatedindex).toInt() + lastfrequecny

                    if (key.isNotBlank()) {

                        var hostTid = if (key.isNotBlank() && key.length >= 8) {
                            key.subSequence(0, 8).toString()
                        } else {
                            ""
                        }
                        var hostMid = if (key.isNotBlank() && key.length >= 23) {
                            key.subSequence(8, 23).toString()
                        } else {
                            ""
                        }
                        var hostBatchNumber = if (key.isNotBlank() && key.length >= 29) {
                            key.subSequence(23, 29).toString()
                        } else {
                            ""
                        }
                        var cardIssuer = if (key.isNotBlank() && key.length >= 30) {
                            key.subSequence(29, key.length).toString()
                        } else {
                            ""
                        }

                        val tpt= getTptData()
                       val mid=tpt?.merchantId
                      //  if (ietration > 0) {
                        // if hostid is not avialable in this or list is blanck then print this line
                        if((listTidPrinted.size==0) || !(listTidPrinted.contains(hostTid)))
                        {
                            listTidPrinted.add(hostTid)// add the tid for which this code is printed
                            printSeperator()
                            textBlockList.add(sigleLineformat("MID:${mid}", AlignMode.LEFT))
                            textBlockList.add(sigleLineformat("TID:${hostTid}", AlignMode.RIGHT))
                            printer?.addMixStyleText(textBlockList)
                            textBlockList.clear()
                            textBlockList.add(sigleLineformat("BATCH NO:${hostBatchNumber}", AlignMode.LEFT))
                            printer?.addMixStyleText(textBlockList)
                            textBlockList.clear()
                            // take a mutable list here


                            //   ietration--
                        }
                       // }
                        if (cardIssuer.isEmpty()) {
                            cardIssuer = _issuerName.toString()
                            _issuerNameString = "CARD ISSUER"
                        }
                        printSeperator()
                        textBlockList.add(sigleLineformat(_issuerNameString, AlignMode.LEFT))
                        textBlockList.add(sigleLineformat( "  ${cardIssuer.toUpperCase(Locale.ROOT)}", AlignMode.CENTER))
                        textBlockList.add(sigleLineformat(" ", AlignMode.RIGHT))
                        printer?.addMixStyleText(textBlockList)
                        textBlockList.clear()
                        // if(ind==0){
                        textBlockList.add(sigleLineformat("TXN TYPE", AlignMode.LEFT))
                        textBlockList.add(sigleLineformat("COUNT", AlignMode.CENTER))
                        textBlockList.add(sigleLineformat("TOTAL", AlignMode.RIGHT))
                        printer?.addMixStyleText(textBlockList)
                        textBlockList.clear()

                    }
                    for ((k, m) in _map) {
                        val amt =
                            "%.2f".format((((m.total)?.toDouble())?.div(100)).toString().toDouble())
                        if (*//*k == BhTransactionType.PRE_AUTH_COMPLETE.type ||*//* k == BhTransactionType.VOID_PREAUTH.type) {
                            // need Not to show
                        } else {
                            m.type?.let {
                                if(it == "TEST EMI TXN"){
                                    sigleLineformat(
                                        "SALE",
                                        AlignMode.LEFT
                                    )
                                }else{
                                    sigleLineformat(
                                        transactionType2Name(k).toUpperCase(Locale.ROOT),
                                        AlignMode.LEFT
                                    )
                                }

                            }?.let {
                                textBlockList.add(it)
                            }
                            textBlockList.add(
                                sigleLineformat(
                                    "${m.count}",
                                    AlignMode.CENTER
                                )
                            )
                            textBlockList.add(sigleLineformat("${getCurrencySymbol(tpt)}:$amt", AlignMode.RIGHT))
                            printer?.addMixStyleText(textBlockList)
                            textBlockList.clear()
                        }

                        if (totalMap.containsKey(k)) {
                            val x = totalMap[k]
                            if (x != null) {
                                x.count += m.count
                                x.total += m.total!!
                            }
                        } else {
                            totalMap[k] = m.total?.let { SummeryTotalType(m.count, it) }!!
                        }
                    }

                    if (frequency == count) {
                        lastfrequecny = frequency
                        hasfrequency = true
                        updatedindex++
                    } else {
                        hasfrequency = false
                    }
                    if (hasfrequency) {
                        printSeperator()
                        sigleLineText("***TOTAL TRANSACTION***", AlignMode.CENTER)

                        val sortedMap = totalMap.toSortedMap(compareByDescending { it })
                        for ((k, m) in sortedMap) {

                            textBlockList.add(
                                sigleLineformat(
                                    transactionType2Name(k).toUpperCase(
                                        Locale.ROOT
                                    ), AlignMode.LEFT
                                )
                            )
                            textBlockList.add(
                                sigleLineformat(
                                    "  = " + m.count , AlignMode.CENTER
                                )
                            )
                            textBlockList.add(
                                sigleLineformat("${getCurrencySymbol(tpt)}:${"%.2f".format(
                                    (((m.total).toDouble()).div(
                                        100
                                    )).toString().toDouble()
                                )}"
                                    , AlignMode.RIGHT
                                )
                            )

                            printer?.addMixStyleText(textBlockList)
                            textBlockList.clear()

                        }



                        totalMap.clear()
                    }

                    //  sb.appendln()
                }

                //    sb.appendln(getChar(LENGTH, '='))

                printSeperator()
                if (isSettlementSuccess) {
                    sigleLineText("SETTLEMENT SUCCESSFUL", AlignMode.CENTER)

                }
                // Below code is used for Digi POS Settlement report
                // Below code is used for Digi POS Settlement report
                if (!isLastSummary)
                digiposReport()
                sigleLineText("Bonushub", AlignMode.CENTER)
                sigleLineText("App Version:${BuildConfig.VERSION_NAME}", AlignMode.CENTER)

                ///  centerText(textFormatBundle, "---------X-----------X----------")
                printer?.feedLine(4)

                // start print here
                printer?.startPrint(object : OnPrintListener.Stub() {
                    @Throws(RemoteException::class)
                    override fun onFinish() {
                        callBack(true)
                        Log.e("Settle_RECEIPT", "SUCESS__")
                    }

                    @Throws(RemoteException::class)
                    override fun onError(error: Int) {
                        callBack(false)
                        Log.e("Settle_RECEIPT", "FAIL__")
                    }
                })
            } catch (ex: DeadObjectException) {
                ex.printStackTrace()
                failureImpl(
                    context as Activity,
                    "Printer Service stopped.",
                    "Please take chargeslip from the Report menu."
                )
            } catch (e: RemoteException) {
                e.printStackTrace()
                failureImpl(
                    context as Activity,
                    "Printer Service stopped.",
                    "Please take chargeslip from the Report menu."
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
                failureImpl(
                    context as Activity,
                    "Printer Service stopped.",
                    "Please take chargeslip from the Report menu."
                )
            }
        }
    }*/

    fun printSettlementReportupdate(
        context: Context?,
        batch: MutableList<TempBatchFileDataTable>,
        isSettlementSuccess: Boolean = false,
        isLastSummary: Boolean = false,
        callBack: (Boolean) -> Unit
    ) {
        var isFirstTimeForAmxLogo = true

//  val format = Bundle()
//   val fmtAddTextInLine = Bundle()

//below if condition is for zero settlement
        if (batch.size <= 0) {
            try {
                val tpt = getTptData()
               // setLogoAndHeader()


                headerPrinting(DEFAULT_BANK_CODE)

                val td = System.currentTimeMillis()
                val formatdate = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                val formattime = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
                val date = formatdate.format(td)
                val time = formattime.format(td)

                textBlockList.add(sigleLineformat("DATE:${date}", AlignMode.LEFT))
                textBlockList.add(sigleLineformat("TIME:${time}", AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

                if (isLastSummary) {

                    sigleLineText("LAST SUMMARY REPORT", AlignMode.CENTER)
                } else {

                    sigleLineText("SUMMARY REPORT", AlignMode.CENTER)
                }


                textBlockList.add(sigleLineformat("TID:${tpt?.terminalId}", AlignMode.LEFT))
                textBlockList.add(sigleLineformat("MID:${tpt?.merchantId}", AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

                textBlockList.add(sigleLineformat("BATCH NO:${tpt?.batchNumber}", AlignMode.LEFT))

                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()


                printSeperator()
                textBlockList.add(sigleLineformat("TOTAL TXN = 0", AlignMode.LEFT))
                textBlockList.add(sigleLineformat("${getCurrencySymbol(tpt)}", AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

                sigleLineText("ZERO SETTLEMENT SUCCESSFUL", AlignMode.CENTER)
                if (!isLastSummary)
                digiposReport()
                sigleLineText("BonusHub", AlignMode.CENTER)
                sigleLineText("App Version", AlignMode.CENTER)

                printer?.feedLine(4)


                printer?.startPrint(object : OnPrintListener.Stub() {
                    @Throws(RemoteException::class)
                    override fun onFinish() {
                        callBack(true)
                        Log.e("Settle_RECEIPT", "SUCESS__")
                    }

                    @Throws(RemoteException::class)
                    override fun onError(error: Int) {
                        callBack(false)
                        Log.e("Settle_RECEIPT", "FAIL__")
                    }
                })
            } catch (ex: DeadObjectException) {
                ex.printStackTrace()
                failureImpl(
                    context as Activity,
                    "Printer Service stopped.",
                    "Please take chargeslip from the Report menu."
                )
            } catch (e: RemoteException) {
                e.printStackTrace()
                failureImpl(
                    context as Activity,
                    "Printer Service stopped.",
                    "Please take chargeslip from the Report menu."
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
                failureImpl(
                    context as Activity,
                    "Printer Service stopped.",
                    "Please take chargeslip from the Report menu."
                )
            }
        }
////below if condition is for settlement(Other than zero settlement)
        else {
            try {
                val mapTidToBankId = mutableMapOf<String, String>()

                val map = mutableMapOf<String, MutableMap<Int, SummeryModel>>()
                val map1 = mutableMapOf<String, MutableMap<Int, SummeryModel>>()
                //to hold the tid for which tid mid printed
                val listTidPrinted = mutableListOf<String>()
                val tpt = getTptData()

               // setLogoAndHeader()

                //headerPrinting()
                val isHdfcPresent = batch.find{ it.hostBankID.equals(HDFC_BANK_CODE) || it.hostBankID.equals(HDFC_BANK_CODE_SINGLE_DIGIT)}
                val isAmexPresent = batch.find{ it.hostBankID.equals(AMEX_BANK_CODE) || it.hostBankID.equals(AMEX_BANK_CODE_SINGLE_DIGIT)}
                if(isHdfcPresent?.hostBankID.equals(HDFC_BANK_CODE) || isHdfcPresent?.hostBankID.equals(HDFC_BANK_CODE_SINGLE_DIGIT)){
                    headerPrinting(HDFC_BANK_CODE)}
                else if(isAmexPresent?.hostBankID.equals(AMEX_BANK_CODE) || isAmexPresent?.hostBankID.equals(AMEX_BANK_CODE_SINGLE_DIGIT)){
                    headerPrinting(AMEX_BANK_CODE)
                    isFirstTimeForAmxLogo = false
                }else{
                    headerPrinting(DEFAULT_BANK_CODE)
                }


                val td = System.currentTimeMillis()
                val formatdate = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                val formattime = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)

                val date = formatdate.format(td)
                val time = formattime.format(td)

                textBlockList.add(sigleLineformat("DATE:${date}", AlignMode.LEFT))
                textBlockList.add(sigleLineformat("TIME:${time}", AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

                //  alignLeftRightText(fmtAddTextInLine,"DATE : ${batch.date}","TIME : ${batch.time}")
                /*   alignLeftRightText(textInLineFormatBundle, "MID : ${batch[0].mid}", "TID : ${batch[0].tid}")
                   alignLeftRightText(textInLineFormatBundle, "BATCH NO  : ${batch[0].batchNumber}", "")*/

                if (isLastSummary) {

                    sigleLineText("LAST SUMMARY REPORT", AlignMode.CENTER)
                } else {

                    sigleLineText("SUMMARY REPORT", AlignMode.CENTER)
                }

                batch.sortBy { it?.tid }

                var tempTid = batch[0]?.tid

                val list = mutableListOf<String>()
                val frequencylist = mutableListOf<String>()

                for (it in batch) {  // Do not count preauth transaction
// || it.transactionType == TransactionType.VOID_PREAUTH.type

                    mapTidToBankId[it.hostTID] = it.hostBankID

                    if (it.transactionType == BhTransactionType.PRE_AUTH.type) continue

                    if (it.transactionType == BhTransactionType.EMI_SALE.type ||
                        it.transactionType == BhTransactionType.BRAND_EMI.type ||
                        it.transactionType == BhTransactionType.BRAND_EMI_BY_ACCESS_CODE.type) {
                        //it?.appName = it.emiIssuerDataModel?.issuerName//
                        it.transactionType = BhTransactionType.EMI_SALE.type
                    }
                    if (it.transactionType == BhTransactionType.VOID_EMI.type) {
                       // it?.appName = it.emiIssuerDataModel?.issuerName//
                    }

                    if (it.transactionType == BhTransactionType.TEST_EMI.type) {
                        it?.appName = "Test Issuer"
                        it?.cardType = "Test Issuer"
                        it.transactionType = BhTransactionType.SALE.type

                    }

                    val transAmt = try {
                        it?.baseAmmount?.toLong()
                    } catch (ex: Exception) {
                        0L
                    }


                    if (tempTid == it?.tid) {
                        _issuerName = it?.appName
                        if (map.containsKey(it?.tid + it.mid + it.batchNumber + it.appName)) {
                            _issuerName = it.appName

                            val ma =
                                map[it.tid + it.mid + it.batchNumber + it.appName] as MutableMap<Int, SummeryModel>
                            if (ma.containsKey(it.transactionType)) {
                                val m = ma[it.transactionType] as SummeryModel
                                m.count += 1
                                if (transAmt != null) {
                                    m.total = m.total?.plus(transAmt)
                                }
                            } else {
                                val txnName = it.transationName
                                val rtid = it.tid
                                val sm = SummeryModel(
                                    txnName, 1, transAmt, rtid
                                )
                                ma[it.transactionType] = sm
                            }
                        } else {
                            val hm = HashMap<Int, SummeryModel>().apply {
                                this[it.transactionType] = transAmt?.let { it1 ->
                                    it.transationName?.let { it2 ->
                                        it.tid?.let { it3 ->
                                            SummeryModel(
                                                it2,
                                                1,
                                                it1,
                                                it3
                                            )
                                        }
                                    }
                                }!!
                            }
                            map[it.tid + it.mid + it.batchNumber + it.appName] =
                                hm
                            it.tid?.let { it1 -> list.add(it1) }
                        }
                    } else {
                        tempTid = it.tid
                        _issuerName = it.appName
                        val hm = HashMap<Int, SummeryModel>().apply {
                            this[it.transactionType] = transAmt?.let { it1 ->
                                it.transationName?.let { it2 ->
                                    it.tid?.let { it3 ->
                                        SummeryModel(
                                            it2,
                                            1,
                                            it1,
                                            it3
                                        )
                                    }
                                }
                            }!!
                        }
                        map[it.tid + it.mid + it.batchNumber + it.appName] =
                            hm
                        it.tid?.let { it1 -> list.add(it1) }
                    }

                }

                for (item in list.distinct()) {
                    println("Frequency of item" + item + ": " + Collections.frequency(list, item))
                    frequencylist.add("" + Collections.frequency(list, item))
                }


                val totalMap = mutableMapOf<Int, SummeryTotalType>()


                var ietration = list.distinct().size
                var curentIndex = 0
                var frequency = 0
                var count = 0
                var lastfrequecny = 0
                var hasfrequency = false
                var updatedindex = 0

                for ((key, _map) in map.onEachIndexed { index, entry -> curentIndex = index }) {


                    count++
                    if (updatedindex <= frequencylist.size - 1)
                        frequency = frequencylist.get(updatedindex).toInt() + lastfrequecny

                    if (key.isNotBlank()) {

                        var hostTid = if (key.isNotBlank() && key.length >= 8) {
                            key.subSequence(0, 8).toString()
                        } else {
                            ""
                        }
                        var hostMid = if (key.isNotBlank() && key.length >= 23) {
                            key.subSequence(8, 23).toString()
                        } else {
                            ""
                        }
                        var hostBatchNumber = if (key.isNotBlank() && key.length >= 29) {
                            key.subSequence(23, 29).toString()
                        } else {
                            ""
                        }
                        var cardIssuer = if (key.isNotBlank() && key.length >= 30) {
                            key.subSequence(29, key.length).toString()
                        } else {
                            ""
                        }

                        val tpt= getTptData()
                       val mid=tpt?.merchantId
                      //  if (ietration > 0) {
                        // if hostid is not avialable in this or list is blanck then print this line
                        if((listTidPrinted.size==0) || !(listTidPrinted.contains(hostTid)))
                        {
                            // handle logo printing
                            var bankId = mapTidToBankId[hostTid]
                            var logo = ""
                            if (bankId.equals(AMEX_BANK_CODE_SINGLE_DIGIT) || bankId.equals(AMEX_BANK_CODE)) {
                                logo = AMEX_LOGO
                            } else if(bankId.equals(HDFC_BANK_CODE_SINGLE_DIGIT)  || bankId.equals(HDFC_BANK_CODE)){
                                logo = HDFC_LOGO
                            }else{
                                logo = ""
                            }

                            if (isFirstTimeForAmxLogo && logo != null && !logo.equals("")) {
                                isFirstTimeForAmxLogo = false
                                printSeperator()
                                printLogo(context!!,logo)
                            }
                            // end region

                            listTidPrinted.add(hostTid)// add the tid for which this code is printed
                            printSeperator()
                            textBlockList.add(sigleLineformat("MID:${mid}", AlignMode.LEFT))
                            textBlockList.add(sigleLineformat("TID:${hostTid}", AlignMode.RIGHT))
                            printer?.addMixStyleText(textBlockList)
                            textBlockList.clear()
                            textBlockList.add(sigleLineformat("BATCH NO:${hostBatchNumber}", AlignMode.LEFT))
                            printer?.addMixStyleText(textBlockList)
                            textBlockList.clear()
                            // take a mutable list here


                            //   ietration--
                        }
                       // }
                        if (cardIssuer.isEmpty()) {
                            cardIssuer = _issuerName.toString()
                            _issuerNameString = "CARD ISSUER"
                        }
                        printSeperator()
                        textBlockList.add(sigleLineformat(_issuerNameString, AlignMode.LEFT))
                        textBlockList.add(sigleLineformat( "  ${cardIssuer.toUpperCase(Locale.ROOT)}", AlignMode.CENTER))
                        textBlockList.add(sigleLineformat(" ", AlignMode.RIGHT))
                        printer?.addMixStyleText(textBlockList)
                        textBlockList.clear()
                        // if(ind==0){
                        textBlockList.add(sigleLineformat("TXN TYPE", AlignMode.LEFT))
                        textBlockList.add(sigleLineformat("COUNT", AlignMode.CENTER))
                        textBlockList.add(sigleLineformat("TOTAL", AlignMode.RIGHT))
                        printer?.addMixStyleText(textBlockList)
                        textBlockList.clear()

                    }
                    for ((k, m) in _map) {
                        val amt =
                            "%.2f".format((((m.total)?.toDouble())?.div(100)).toString().toDouble())
                        if (/*k == BhTransactionType.PRE_AUTH_COMPLETE.type ||*/ k == BhTransactionType.VOID_PREAUTH.type) {
                            // need Not to show
                        } else {
                            m.type?.let {
                                if(it == "TEST EMI TXN"){
                                    sigleLineformat(
                                        "SALE",
                                        AlignMode.LEFT
                                    )
                                }else{
                                    sigleLineformat(
                                        transactionType2Name(k).toUpperCase(Locale.ROOT),
                                        AlignMode.LEFT
                                    )
                                }

                            }?.let {
                                textBlockList.add(it)
                            }
                            textBlockList.add(
                                sigleLineformat(
                                    "${m.count}",
                                    AlignMode.CENTER
                                )
                            )
                            textBlockList.add(sigleLineformat("${getCurrencySymbol(tpt)}:$amt", AlignMode.RIGHT))
                            printer?.addMixStyleText(textBlockList)
                            textBlockList.clear()
                        }

                        if (totalMap.containsKey(k)) {
                            val x = totalMap[k]
                            if (x != null) {
                                x.count += m.count
                                x.total += m.total!!
                            }
                        } else {
                            totalMap[k] = m.total?.let { SummeryTotalType(m.count, it) }!!
                        }
                    }

                    if (frequency == count) {
                        lastfrequecny = frequency
                        hasfrequency = true
                        updatedindex++
                    } else {
                        hasfrequency = false
                    }
                    if (hasfrequency) {
                        printSeperator()
                        sigleLineText("***TOTAL TRANSACTION***", AlignMode.CENTER)

                        val sortedMap = totalMap.toSortedMap(compareByDescending { it })
                        for ((k, m) in sortedMap) {

                            textBlockList.add(
                                sigleLineformat(
                                    transactionType2Name(k).toUpperCase(
                                        Locale.ROOT
                                    ), AlignMode.LEFT
                                )
                            )
                            textBlockList.add(
                                sigleLineformat(
                                    "  = " + m.count , AlignMode.CENTER
                                )
                            )
                            textBlockList.add(
                                sigleLineformat("${getCurrencySymbol(tpt)}:${"%.2f".format(
                                    (((m.total).toDouble()).div(
                                        100
                                    )).toString().toDouble()
                                )}"
                                    , AlignMode.RIGHT
                                )
                            )

                            printer?.addMixStyleText(textBlockList)
                            textBlockList.clear()

                        }



                        totalMap.clear()
                    }

                    //  sb.appendln()
                }

                //    sb.appendln(getChar(LENGTH, '='))

                printSeperator()
                if (isSettlementSuccess) {
                    sigleLineText("SETTLEMENT SUCCESSFUL", AlignMode.CENTER)

                }
                // Below code is used for Digi POS Settlement report
                // Below code is used for Digi POS Settlement report
                if (!isLastSummary)
                digiposReport()
                sigleLineText("Bonushub", AlignMode.CENTER)
                sigleLineText("App Version:${BuildConfig.VERSION_NAME}", AlignMode.CENTER)

                ///  centerText(textFormatBundle, "---------X-----------X----------")
                printer?.feedLine(5)

                // start print here
                printer?.startPrint(object : OnPrintListener.Stub() {
                    @Throws(RemoteException::class)
                    override fun onFinish() {
                        callBack(true)
                        Log.e("Settle_RECEIPT", "SUCESS__")
                    }

                    @Throws(RemoteException::class)
                    override fun onError(error: Int) {
                        callBack(false)
                        Log.e("Settle_RECEIPT", "FAIL__")
                    }
                })
            } catch (ex: DeadObjectException) {
                ex.printStackTrace()
                failureImpl(
                    context as Activity,
                    "Printer Service stopped.",
                    "Please take chargeslip from the Report menu."
                )
            } catch (e: RemoteException) {
                e.printStackTrace()
                failureImpl(
                    context as Activity,
                    "Printer Service stopped.",
                    "Please take chargeslip from the Report menu."
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
                failureImpl(
                    context as Activity,
                    "Printer Service stopped.",
                    "Please take chargeslip from the Report menu."
                )
            }
        }
    }

    fun digiposReport(){

            val digiPosDataList =
                selectDigiPosDataAccordingToTxnStatus(EDigiPosPaymentStatus.Approved.desciption)
            val requiredTxnhm = hashMapOf<String, ArrayList<DigiPosDataTable>>()
            if (digiPosDataList.isNotEmpty()) {
                for (i in digiPosDataList) {
                    val digiData = arrayListOf<DigiPosDataTable>()
                    for (j in digiPosDataList) {
                        if (i != null) {
                            if (j != null) {
                                if (i.paymentMode == j.paymentMode) {
                                    digiData.add(j)
                                    requiredTxnhm[i.paymentMode] = digiData
                                }
                            }
                        }
                    }
                }

                ///  centerText(textFormatBundle, "---------X-----------X----------")
                sigleLineText("Digi Pos Summary Report", AlignMode.CENTER)
               val tpt= getTptData()
                tpt?.terminalId?.let { sigleLineText( "TID : $it",AlignMode.CENTER) }
                printSeperator()
                // Txn description
                textBlockList.add(sigleLineformat("TXN TYPE", AlignMode.LEFT))
                textBlockList.add(sigleLineformat("TOTAL", AlignMode.CENTER))
                textBlockList.add(sigleLineformat("COUNT", AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()
                printSeperator()
                var totalAmount = 0.0f
                var totalCount = 0
                for ((k, v) in requiredTxnhm) {
                    val txnType = k
                    val txnCount = v.size
                    var txnTotalAmount = 0.0f
                    for (value in v) {
                        txnTotalAmount += (value.amount.toFloat())
                        totalAmount += (value.amount.toFloat())
                        totalCount++
                    }

                    textBlockList.add(sigleLineformat(txnType, AlignMode.LEFT))
                    textBlockList.add(sigleLineformat( "%.2f".format(txnTotalAmount), AlignMode.CENTER))
                    textBlockList.add(sigleLineformat( txnCount.toString() + getCurrencySymbol(tpt), AlignMode.RIGHT))

                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()
                }
                printSeperator()
                textBlockList.add(sigleLineformat("Total TXNs", AlignMode.LEFT))
                textBlockList.add(sigleLineformat( totalCount.toString() + getCurrencySymbol(tpt), AlignMode.CENTER))
                textBlockList.add(sigleLineformat( "%.2f".format(totalAmount), AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()
                printSeperator()
            }

    }
    fun printSMSUPIChagreSlip(
        digiPosData: DigiPosDataTable,
        copyType: EPrintCopyType,
        context: Context?,
        printerCallback: (Boolean, Int) -> Unit
    ) {
        //  printer=null
        try {
            var currencySymbol: String? = "Rs"
            val terminalData = getTptData()
            currencySymbol = terminalData?.currencySymbol

            setLogoAndHeaderForDigiPos()

          /*  receiptDetail.merAddHeader1?.let { sigleLineText(it, AlignMode.CENTER) }
            receiptDetail.merAddHeader2?.let { sigleLineText(it, AlignMode.CENTER) }*/

//            val ingtpt=getInitdataList()
//            val header1= ingtpt?.merAddHeader1
//            val header2=ingtpt?.merAddHeader2
//            val merchantName=ingtpt?.merchantName.toString().trim()
//
//            sigleLineText(merchantName, AlignMode.CENTER)
//            sigleLineText(hexString2String(header1?:"").trim(), AlignMode.CENTER)
//            sigleLineText(hexString2String(header2?:"").trim(), AlignMode.CENTER)

            headerPrinting()

            printSeperator()
            textBlockList.add(sigleLineformat("DATE:${digiPosData.txnDate}", AlignMode.LEFT))
            textBlockList.add(sigleLineformat("TIME:${digiPosData.txnTime}", AlignMode.RIGHT))
            printer?.addMixStyleText(textBlockList)
            textBlockList.clear()

            textBlockList.add(sigleLineformat("TID:${terminalData?.terminalId}", AlignMode.LEFT))
            printer?.addMixStyleText(textBlockList)
            textBlockList.clear()




            textBlockList.add(sigleLineformat("Partner Txn Id:${digiPosData.partnerTxnId}", AlignMode.LEFT))
            printer?.addMixStyleText(textBlockList)
            textBlockList.clear()

            textBlockList.add(sigleLineformat("mTxnId:${digiPosData.mTxnId}", AlignMode.LEFT))
            printer?.addMixStyleText(textBlockList)
            textBlockList.clear()

            textBlockList.add(sigleLineformat("PgwTxnId:${digiPosData.pgwTxnId}", AlignMode.LEFT))
            printer?.addMixStyleText(textBlockList)
            textBlockList.clear()

            printSeperator()

            val str = "Txn Status:${digiPosData.txnStatus}"
            sigleLineText(
                str,
                AlignMode.CENTER
            )
            sigleLineText(
                "Txn Amount :  $currencySymbol ${digiPosData.amount}",
                AlignMode.CENTER
            )

            printSeperator()

            textBlockList.add(sigleLineformat("Mob:${digiPosData.customerMobileNumber}", AlignMode.LEFT))
            textBlockList.add(sigleLineformat( "Mode:${digiPosData.paymentMode}", AlignMode.RIGHT))
            printer?.addMixStyleText(textBlockList)
            textBlockList.clear()

            sigleLineText(copyType.pName, AlignMode.CENTER)
            sigleLineText(footerText[0], AlignMode.CENTER)
            sigleLineText(footerText[1], AlignMode.CENTER)
            val bhlogo: ByteArray? = context?.let { printLogo(it, "BH.bmp") }
            printer?.addBmpImage(0, FactorMode.BMP1X1, bhlogo)
            sigleLineText(
                "App Version :${BuildConfig.VERSION_NAME}",
                AlignMode.CENTER
            )


            //
            //   printer?.addText(format, "---------X-----------X----------")
            printer?.feedLine(4)

            // start print here
            printer?.startPrint(
                ISmSUpiPrintListener(
                    this,
                    context,
                    copyType,
                    digiPosData,
                    printerCallback
                )
            )
        } catch (ex: DeadObjectException) {
            ex.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take charge slip from the Report menu."
            )
        } catch (e: RemoteException) {
            e.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take charge slip from the Report menu."
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            failureImpl(
                context as Activity,
                "Printer Service stopped.",
                "Please take charge slip from the Report menu."
            )
        } finally {
            //   VFService.connectToVFService(VerifoneApp.appContext)
        }
    }

    private fun printSeperator() {

        printer?.setAscSize(ASCSize.DOT24x12)
        printer?.setPrintFormat(PrintFormat.FORMAT_MOREDATAPROC, PrintFormat.VALUE_MOREDATAPROC_PRNONELINE)
        sigleLineText("----------------------------------------", AlignMode.CENTER)
    }

    internal open class ISmSUpiPrintListener(
        var printerUtil: PrintUtil,
        var context: Context?,
        var copyType: EPrintCopyType,
        var digiPosData: DigiPosDataTable,
        var isSuccess: (Boolean, Int) -> Unit
    ) : OnPrintListener.Stub() {
        @Throws(RemoteException::class)
        override fun onError(error: Int) {
            if (error == 240)
            //VFService.showToast("Printing roll not available..")
                isSuccess(true, 0)
            else
            //VFService.showToast("Printer Error------> $error")
                isSuccess(false, 0)
        }

        @Throws(RemoteException::class)
        override fun onFinish() {
            val msg = Message()
            msg.data.putString("msg", "print finished")
            // VFService.showToast("Printing Successfully")
            when (copyType) {
                EPrintCopyType.MERCHANT -> {
                    GlobalScope.launch(Dispatchers.Main) {

                        (context as BaseActivityNew).showMerchantAlertBoxSMSUpiPay(
                            printerUtil,
                            digiPosData
                        ) { dialogCB ->
                            isSuccess(dialogCB, 1)
                        }

                    }

                }
                EPrintCopyType.CUSTOMER -> {
                    //VFService.showToast("Customer Transaction Slip Printed Successfully")
                    isSuccess(false, 1)
                }
                EPrintCopyType.DUPLICATE -> {
                    isSuccess(true, 1)
                }
            }
        }
    }

    private fun setLogoAndHeader(logo: String = HDFC_LOGO) {
        val image: ByteArray? = context?.let { printLogo(it, logo) }
        printer?.addBmpImage(0, FactorMode.BMP1X1, image)
    }

    private fun setLogoAndHeaderForDigiPos() {
        val image: ByteArray? = context?.let { printLogo(it, Companion.DIGI_SMART_HUB_LOGO) }
        printer?.addBmpImage(0, FactorMode.BMP1X1, image)
    }

    //Below method is used to chunk the TNC's text(words's are not splitted in between) which was printed on EMI sale :-
    private fun chunkTnC(s: String, limit: Int = 32): List<String> {

        var str = s
        val parts: MutableList<String> = ArrayList()
        while (str.length > limit) {
            var splitAt = limit - 1
            while (splitAt > 0 && !Character.isWhitespace(str[splitAt])) {
                splitAt--
            }
            if (splitAt == 0) return parts // can't be split
            parts.add(str.substring(0, splitAt))
            str = str.substring(splitAt + 1)
        }
        parts.add(str)
        return parts
    }

    fun getCurrencySymbol(tpt: TerminalParameterTable?): String {
        return if (!TextUtils.isEmpty(tpt?.currencySymbol)) {
            tpt?.currencySymbol ?: "Rs"
        } else {
            "Rs"
        }
    }

    internal data class SummeryModel(
        val type: String?,
        var count: Int = 0,
        var total: Long? = 0,
        var hostTid: String?
    )

    companion object {
        private const val disclaimerIssuerClose = "~!iss~"
        const val DIGI_SMART_HUB_LOGO = "smart_hub.bmp"
    }

    private fun headerPrinting(logo: String? = HDFC_LOGO) {

        val tpt = getTptData()
        var hdfcTpt: HDFCTpt?
        runBlocking(Dispatchers.IO) {
            hdfcTpt = getHDFCTptData()
        }

        val logo = if (logo == AMEX_BANK_CODE_SINGLE_DIGIT || logo == AMEX_BANK_CODE) {
            AMEX_LOGO
        } else {
            HDFC_LOGO
        }

        setLogoAndHeader(logo)

        if (logo == AMEX_BANK_CODE_SINGLE_DIGIT || logo == AMEX_BANK_CODE) {
            tpt?.receiptHeaderOne?.let {
                sigleLineText(it ?: "".trim(), AlignMode.CENTER)
            }
            tpt?.receiptHeaderTwo?.let { sigleLineText(it ?: "".trim(), AlignMode.CENTER) }
            tpt?.receiptHeaderThree?.let { sigleLineText(it ?: "".trim(), AlignMode.CENTER) }
        } else {
            if (null != hdfcTpt && hdfcTpt?.defaultMerchantName?.isNotBlank() ?: false && hdfcTpt?.defaultMerchantName?.isNotEmpty() ?: false) {
                hdfcTpt?.defaultMerchantName?.trim()
                    ?.let { sigleLineText(it ?: "".trim(), AlignMode.CENTER) }
            } else {
                tpt?.receiptHeaderOne?.let { sigleLineText(it ?: "".trim(), AlignMode.CENTER) }
            }
            if (null != hdfcTpt && hdfcTpt?.receiptL2?.isNotBlank() ?: false && hdfcTpt?.receiptL2?.isNotEmpty() ?: false) {
                hdfcTpt?.receiptL2?.let { sigleLineText(it ?: "".trim(), AlignMode.CENTER) }
            } else {
                tpt?.receiptHeaderTwo?.let { sigleLineText(it ?: "".trim(), AlignMode.CENTER) }
            }
            if (null != hdfcTpt && hdfcTpt?.receiptL3?.isNotBlank() ?: false && hdfcTpt?.receiptL3?.isNotEmpty() ?: false) {
                hdfcTpt?.receiptL3?.let { sigleLineText(it ?: "".trim(), AlignMode.CENTER) }
            } else {
                tpt?.receiptHeaderThree?.let { sigleLineText(it ?: "".trim(), AlignMode.CENTER) }
            }
        }


    }

    fun getErrorMessage(error: Int): String? {
        val message: String
        when (error) {
            PrinterError.ERROR_NOT_INIT -> message = "ERROR NOT INIT"
            PrinterError.ERROR_PARAM -> message = "ERROR PARAM"
            PrinterError.ERROR_BMBLACK -> message = "ERROR BMBLACK"
            PrinterError.ERROR_BUFOVERFLOW -> message = "ERROR BUFOVERFLOW"
            PrinterError.ERROR_BUSY -> message = "ERROR BUSY"
            PrinterError.ERROR_COMMERR -> message = "ERROR COMMERR"
            PrinterError.ERROR_CUTPOSITIONERR -> message = "ERROR CUTPOSITIONERR"
            PrinterError.ERROR_HARDERR -> message = "ERROR HARDERR"
            PrinterError.ERROR_LIFTHEAD -> message = "ERROR LIFTHEAD"
            PrinterError.ERROR_LOWTEMP -> message = "ERROR LOWTEMP"
            PrinterError.ERROR_LOWVOL -> message = "ERROR LOWVOL"
            PrinterError.ERROR_MOTORERR -> message = "ERROR MOTORERR"
            PrinterError.ERROR_NOBM -> message = "ERROR NOBM"
            PrinterError.ERROR_OVERHEAT -> message = "ERROR OVERHEAT"
            PrinterError.ERROR_PAPERENDED -> message = "ERROR PAPERENDED"
            PrinterError.ERROR_PAPERENDING -> message = "ERROR PAPERENDING"
            PrinterError.ERROR_PAPERJAM -> message = "ERROR PAPERJAM"
            PrinterError.ERROR_PENOFOUND -> message = "ERROR PENOFOUND"
            PrinterError.ERROR_WORKON -> message = "ERROR WORKON"
            else -> message = "ERROR UNKNOWN"
        }
        return message
    }
}

fun checkForPrintReversalReceipt(
    context: Context?,
    field60Data: String,
    callback: (String) -> Unit
) {
    if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
//        val tpt = TerminalParameterTable.selectFromSchemeTable()
//        tpt?.cancledTransactionReceiptPrint?.let { logger("CancelPrinting", it, "e") }
        //if (tpt?.cancledTransactionReceiptPrint == "01") {
            PrintUtil(context).printReversal(context, field60Data) {
                callback(it)
            }
//        } else {
//            callback("")
//        }
    } else {
        callback("")
    }
}
