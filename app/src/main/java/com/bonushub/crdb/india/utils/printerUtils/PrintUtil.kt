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

const val HDFC_BANK_CODE = "01"
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
    private var isNoEmiOnlyCashBackApplied: Boolean? = null
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
                    BhTransactionType.EMI_SALE.type , BhTransactionType.TEST_EMI.type, BhTransactionType.BRAND_EMI.type -> {
                        printEMISale(batchTable)

                   }

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
                        voidTransaction(receiptDetail)

                    }
                }
                printSeperator()
                //region=====================BRAND TAndC===============
                if (batchTable.transactionType == BhTransactionType.EMI_SALE.type ||batchTable.transactionType == BhTransactionType.TEST_EMI.type||batchTable.transactionType == BhTransactionType.BRAND_EMI.type) {
                    if(!isNoEmiOnlyCashBackApplied)
                    printBrandTnC(batchTable)

                }
                //region=====================BRAND PRODUACT DATA===============
                if (batchTable.transactionType == BhTransactionType.BRAND_EMI.type) {
                    printProduactData(batchTable)
                    printSeperator()
                    baseAmounthandling(batchTable)
                }

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

           // val receiptDetail: ReceiptDetail = batchTable.receiptData ?: ReceiptDetail()
//            val bankEMITenureDataModal: BankEMITenureDataModal? = batchTable.emiTenureDataModel
//            val bankEMIIssuerTAndCDataModal: BankEMIIssuerTAndCDataModal? = batchTable.emiIssuerDataModel
//            val isNoEmiOnlyCashBackApplied : Boolean =  bankEMITenureDataModal?.tenure=="1"
            //setLogoAndHeader()
            val terminalData = getTptData()
            try {
                headerPrinting()


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
                textBlockList.clear()
                val isNoEmiOnlyCashBackAppl : Boolean =  bankEMITenureDataModal?.tenure=="1"*/

                /*if (isReversal) {
                    sigleLineText("TRANSACTION FAILED", AlignMode.CENTER)
                } else {
                    if(isNoEmiOnlyCashBackAppl) {
                        sigleLineText("SALE", AlignMode.CENTER)
                    }
                    else{
                        getTransactionTypeName(batchTable.transactionType)?.let { sigleLineText(it, AlignMode.CENTER) }
                    }
                }*/

                getTransactionTypeName(batchTable.transactionType)?.let { sigleLineText(it, AlignMode.CENTER) }

               /*if(!batchTable.appName.isNullOrEmpty()){
                textBlockList.add(
                    sigleLineformat(
                        "CARD TYPE:${batchTable.appName}",
                        AlignMode.LEFT
                    )
                )}
                else{*/
                   textBlockList.add(
                       sigleLineformat(
                           "CARD TYPE:${hostCardType}",
                           AlignMode.LEFT
                       ))
              // }
                textBlockList.add(sigleLineformat("EXP:XX/XX", AlignMode.RIGHT))

                printer?.addMixStyleText(textBlockList)

                textBlockList.clear()


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


                textBlockList.add(
                    sigleLineformat(
                        "AUTH CODE:${batchTable.authCode}",
                        AlignMode.LEFT
                    )
                )
               // textBlockList.add(sigleLineformat("RRN:${batchTable.rrn}", AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)

                textBlockList.clear()
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

                if (isReversal) {
                    textBlockList.add(
                        sigleLineformat(
                            "AID:${batchTable.aid}",
                            AlignMode.LEFT
                        )
                    )
                    printer?.addMixStyleText(textBlockList)

                    textBlockList.clear()
                }

                printSeperator()
                var txnName = batchTable.transationName

                if (isReversal) {
                    txnName = "REVERSAL"
                }

                when (batchTable.transactionType) {
                    BhTransactionType.SALE.type, BhTransactionType.CASH_AT_POS.type, BhTransactionType.SALE_WITH_CASH.type -> {
                        saleTransaction(batchTable)
                    }
                   /* BhTransactionType.EMI_SALE.type , BhTransactionType.TEST_EMI.type, BhTransactionType.BRAND_EMI.type -> {
                        printEMISale(batchTable)

                   }

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
                        voidTransaction(receiptDetail)

                    }*/
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

                if (batchTable.operationType.equals("CLESS_EMV")) {
                    if (batchTable.isPinverified == true) {
                        sigleLineText("PIN VERIFIED OK", AlignMode.CENTER)
                    }
                    if (batchTable.isPinverified == true){
                        sigleLineText("SIGNATURE NOT REQUIRED", AlignMode.CENTER)}
                    else{
                        if (batchTable.transactionType == BhTransactionType.EMI_SALE.type ||batchTable.transactionType == BhTransactionType.TEST_EMI.type||batchTable.transactionType == BhTransactionType.BRAND_EMI.type || batchTable.transactionType == BhTransactionType.SALE.type || batchTable.transactionType == BhTransactionType.CASH_AT_POS.type || batchTable.transactionType == BhTransactionType.SALE_WITH_CASH.type || batchTable.transactionType == BhTransactionType.PRE_AUTH.type ) {
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

                        }
                        else{
                            if (batchTable.isPinverified) {
                                sigleLineText("PIN VERIFIED OK", AlignMode.CENTER)
                            }
                            if (batchTable.isPinverified){
                                sigleLineText("SIGNATURE NOT REQUIRED", AlignMode.CENTER)}
                        }
                    }
                } else {
                    if (batchTable.isPinverified){
                        sigleLineText("PIN VERIFIED OK", AlignMode.CENTER)}

                    if (batchTable.isPinverified){
                        sigleLineText("SIGNATURE NOT REQUIRED", AlignMode.CENTER)}
                    else{
                        sigleLineText("SIGN ...................", AlignMode.CENTER)
                    }


                    batchTable.cardHolderName?.let { sigleLineText(it, AlignMode.CENTER) }
                }

                printer?.setAscScale(ASCScale.SC1x1)
                printer?.setAscSize(ASCSize.DOT24x8)

                try{
                    val issuerParameterTable = Field48ResponseTimestamp.getIssuerData(AppPreference.WALLET_ISSUER_ID)

                    var dec = issuerParameterTable?.walletIssuerDisclaimer

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

               /* if(!isNoEmiOnlyCashBackAppl) {
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
                                     *//*       textBlockList.add(
                                                sigleLineformat(
                                                    st, AlignMode.LEFT
                                                )
                                            )*//*
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
                           *//*     textBlockList.add(
                                    sigleLineformat(
                                        "# ${issuerTAndCData.footerTAndC}", AlignMode.LEFT
                                    )
                                )*//*
//                                printer?.setHzScale(HZScale.SC1x1)
//                                printer?.setHzSize(HZSize.DOT24x16)
                                printer?.setAscScale(ASCScale.SC1x1)
                                printer?.setAscSize(ASCSize.DOT24x8)
                                printer?.addText( AlignMode.LEFT, "# ${issuerTAndCData.footerTAndC}")
                            }
                        }
                    }
                }*/ //


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

    private fun voidTransaction(receiptDetail: ReceiptDetail) {
        var currencySymbol: String? = "Rs"
        val terminalData = getTptData()
        currencySymbol = terminalData?.currencySymbol
        textBlockList.add(sigleLineformat("BASE AMOUNT:", AlignMode.LEFT))
        val amt = (((receiptDetail.txnAmount)?.toDouble())?.div(100)).toString()
        textBlockList.add(sigleLineformat("$currencySymbol :${"%.2f".format(amt.toDouble())}", AlignMode.RIGHT))
        printer?.addMixStyleText(textBlockList)
        textBlockList.clear()

        textBlockList.add(sigleLineformat("TOTAL AMOUNT:", AlignMode.LEFT))
        textBlockList.add(sigleLineformat("$currencySymbol :${"%.2f".format(amt.toDouble())}", AlignMode.RIGHT))
        printer?.addMixStyleText(textBlockList)
        textBlockList.clear()
    }

    private fun printEMISale(batchTable: BatchTable) {
        var currencySymbol: String? = "Rs"
        val terminalData = getTptData()
        currencySymbol = terminalData?.currencySymbol
        val receiptDetail: ReceiptDetail = batchTable.receiptData ?: ReceiptDetail()
        val bankEMITenureDataModal: BankEMITenureDataModal? = batchTable.emiTenureDataModel
        val bankEMIIssuerTAndCDataModal: BankEMIIssuerTAndCDataModal? = batchTable.emiIssuerDataModel
        val issuerId = bankEMIIssuerTAndCDataModal?.issuerID
        val isNoEmiOnlyCashBackApplied : Boolean =  bankEMITenureDataModal?.tenure=="1"
       if(!isNoEmiOnlyCashBackApplied) {
           textBlockList.add(sigleLineformat("TXN AMOUNT", AlignMode.LEFT))

           //   val txnAmount = (((bankEMITenureDataModal?.transactionAmount)?.toLong())?.div(100)).toString()
           val txnAmount = (((batchTable.emiEnteredAmt).toDouble()).div(100)).toString()

           logger("txnAmount", "" + txnAmount)
           textBlockList.add(
               sigleLineformat(
                   "$currencySymbol:${"%.2f".format(txnAmount.toDoubleOrNull())}",
                   AlignMode.RIGHT
               )
           )
           printer?.addMixStyleText(textBlockList)
           textBlockList.clear()

           logger("INSTA DISCOUNT", "  ${bankEMITenureDataModal?.instantDiscount}")
           if (bankEMITenureDataModal?.instantDiscount?.toIntOrNull() != null) {
               if (bankEMITenureDataModal.instantDiscount.isNotBlank() && bankEMITenureDataModal.instantDiscount.toInt() > 0) {
                   val instantDis =
                       "%.2f".format(
                           (((bankEMITenureDataModal.instantDiscount).toDouble()).div(
                               100
                           )).toString().toDouble()
                       )

                   textBlockList.add(sigleLineformat("INSTA DISCOUNT", AlignMode.LEFT))
                   val authAmount =
                       (((bankEMITenureDataModal.transactionAmount)?.toLong())?.div(100)).toString()
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
                   (((bankEMITenureDataModal?.transactionAmount)?.toDouble())?.div(100)).toString()
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
               if (bankEMIIssuerTAndCDataModal != null) {
                   textBlockList.add(
                       sigleLineformat(
                           bankEMIIssuerTAndCDataModal.issuerName,
                           AlignMode.RIGHT
                       )
                   )
               }
           }
           printer?.addMixStyleText(textBlockList)
           textBlockList.clear()
           val tenureDuration = "${bankEMITenureDataModal?.tenure} Months"
           val tenureHeadingDuration = "${bankEMITenureDataModal?.tenure} Months Scheme"
           var roi = bankEMITenureDataModal?.tenureInterestRate?.toInt()
               ?.let { divideAmountBy100(it).toString() }
           var loanamt =
               bankEMITenureDataModal?.loanAmount?.toInt()?.let { divideAmountBy100(it).toString() }
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
           if (!TextUtils.isEmpty(bankEMITenureDataModal?.processingFee)) {
               if ((bankEMITenureDataModal?.processingFee) != "0") {
                   val procFee = "%.2f".format(
                       (((bankEMITenureDataModal?.processingFee)?.toDouble())?.div(100)).toString()
                           .toDouble()
                   )
                   textBlockList.add(sigleLineformat("PROC-FEE", AlignMode.LEFT))
                   textBlockList.add(sigleLineformat("$currencySymbol $procFee", AlignMode.RIGHT))
                   printer?.addMixStyleText(textBlockList)
                   textBlockList.clear()
               }
           }

           if (!TextUtils.isEmpty(bankEMITenureDataModal?.processingRate)) {
               val procFeeAmount =
                   bankEMITenureDataModal?.processingRate?.toFloat()?.div(100)
               val pfeeData: Int? = procFeeAmount?.toInt()
               if ((pfeeData.toString()) != "0") {
                   val procFeeAmount =
                       "%.2f".format(
                           bankEMITenureDataModal?.processingRate?.toFloat()?.div(100)
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
           if (bankEMITenureDataModal != null) {
               if (!TextUtils.isEmpty(bankEMITenureDataModal.totalProcessingFee)) {
                   if (!(bankEMITenureDataModal.totalProcessingFee).equals("0")) {
                       val totalProcFeeAmount =
                           "%.2f".format(bankEMITenureDataModal.totalProcessingFee.toFloat() / 100)

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
           if (bankEMIIssuerTAndCDataModal != null) {
               when (bankEMIIssuerTAndCDataModal.issuerID) {
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


               when (bankEMIIssuerTAndCDataModal.issuerID) {
                   "51", "64" -> {
                       nextLineAppendStr = "Payback Amount"
                   }
                   "52", "55", "54" -> {
                       nextLineAppendStr = "Cashback Amount"
                   }

               }
           }

           //region=============CashBack CalculatedValue====================
           if (!TextUtils.isEmpty(bankEMITenureDataModal?.cashBackCalculatedValue)) {
               if (islongTextHeading) {
                   textBlockList.add(sigleLineformat(cashBackPercentHeadingText, AlignMode.LEFT))
                   textBlockList.add(
                       sigleLineformat(
                           "$currencySymbol ${bankEMITenureDataModal?.cashBackCalculatedValue}",
                           AlignMode.RIGHT
                       )
                   )
                   printer?.addMixStyleText(textBlockList)
                   textBlockList.clear()
               } else {
                   textBlockList.add(sigleLineformat(cashBackPercentHeadingText, AlignMode.LEFT))
                   textBlockList.add(
                       sigleLineformat(
                           "$currencySymbol ${bankEMITenureDataModal?.cashBackCalculatedValue}",
                           AlignMode.RIGHT
                       )
                   )
                   printer?.addMixStyleText(textBlockList)
                   textBlockList.clear()
               }
           }

           if (!TextUtils.isEmpty(bankEMITenureDataModal?.cashBackAmount) && bankEMITenureDataModal?.cashBackAmount != "0") {
               val cashBackAmount = "%.2f".format(
                   bankEMITenureDataModal?.cashBackAmount?.toFloat()
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
                   println("test-->${bankEMITenureDataModal?.cashBackAmount}")
                   if (bankEMITenureDataModal?.cashBackAmount != "0" && !(bankEMITenureDataModal?.cashBackAmount.isNullOrEmpty())) {
                       val cashBackAmount = "%.2f".format(
                           bankEMITenureDataModal?.cashBackAmount?.toFloat()
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
           println("bankid ${bankEMIIssuerTAndCDataModal?.issuerID}")

           var discountPercentHeadingText = ""
           var discountAmountHeadingText = ""
           islongTextHeading = true
           when (bankEMIIssuerTAndCDataModal?.issuerID) {
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
           if (!TextUtils.isEmpty(bankEMITenureDataModal?.discountCalculatedValue)) {
               if (islongTextHeading) {

                   textBlockList.add(sigleLineformat(cashBackPercentHeadingText, AlignMode.LEFT))
                   textBlockList.add(
                       sigleLineformat(
                           "$currencySymbol ${bankEMITenureDataModal?.discountCalculatedValue}",
                           AlignMode.RIGHT
                       )
                   )
                   printer?.addMixStyleText(textBlockList)
                   textBlockList.clear()

               } else {
                   textBlockList.add(sigleLineformat(discountPercentHeadingText, AlignMode.LEFT))
                   textBlockList.add(
                       sigleLineformat(
                           "$currencySymbol ${bankEMITenureDataModal?.discountCalculatedValue}",
                           AlignMode.RIGHT
                       )
                   )
                   printer?.addMixStyleText(textBlockList)
                   textBlockList.clear()
               }
           }
           if (!(bankEMITenureDataModal?.discountAmount.isNullOrEmpty()) && bankEMITenureDataModal?.discountAmount != "0") {
               val discAmount =
                   "%.2f".format(bankEMITenureDataModal?.discountAmount?.toFloat()?.div(100))

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
           if (!(bankEMITenureDataModal?.emiAmount.isNullOrEmpty()) && bankEMITenureDataModal?.emiAmount != "0") {
               var emiAmount =
                   "%.2f".format(bankEMITenureDataModal?.emiAmount?.toFloat()?.div(100))
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
           if (!(bankEMITenureDataModal?.totalInterestPay.isNullOrEmpty()) && bankEMITenureDataModal?.totalInterestPay != "0") {
               var totalInterestPay =
                   "%.2f".format(bankEMITenureDataModal?.totalInterestPay?.toFloat()?.div(100))
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
           if (!TextUtils.isEmpty(bankEMITenureDataModal?.totalInterestPay)) {

               if (batchTable.transactionType == BhTransactionType.TEST_EMI.type) {
                   val loanAmt =
                       "%.2f".format(
                           (((bankEMITenureDataModal?.loanAmount)?.toDouble())?.div(100)).toString()
                               .toDouble()
                       )
                   val totalInterest =
                       "%.2f".format(
                           (((bankEMITenureDataModal?.totalInterestPay)?.toDouble())?.div(100)).toString()
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
                       "%.2f".format(bankEMITenureDataModal?.netPay?.toFloat()?.div(100))
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
           if (bankEMITenureDataModal != null) {
               textBlockList.add(
                   sigleLineformat(
                       bankEMITenureDataModal?.tenureLabel,
                       AlignMode.RIGHT
                   )
               )
               printer?.addMixStyleText(textBlockList)
               textBlockList.clear()
           }

           textBlockList.add(sigleLineformat("Card Issuer", AlignMode.LEFT))
           if (bankEMIIssuerTAndCDataModal != null) {
               textBlockList.add(
                   sigleLineformat(
                       bankEMIIssuerTAndCDataModal.issuerName,
                       AlignMode.RIGHT
                   )
               )
           }
           printer?.addMixStyleText(textBlockList)
           textBlockList.clear()
       }
    }


    private fun printBrandTnC(batchTable: BatchTable) {

        val brandEMIMasterDataModal: BrandEMIMasterDataModal? = batchTable.emiBrandData
        val bankEMITenureDataModal: BankEMITenureDataModal? = batchTable.emiTenureDataModel
        val bankEMIIssuerTAndCDataModal: BankEMIIssuerTAndCDataModal? = batchTable.emiIssuerDataModel
        val issuerId = bankEMIIssuerTAndCDataModal?.issuerID
        var brandId = brandEMIMasterDataModal?.brandID

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
        val emiCustomerConsent =
            bankEMIIssuerTAndCDataModal?.schemeTAndC?.split(SplitterTypes.POUND.splitter)
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
            if (bankEMITenureDataModal != null) {
                textBlockList.add(
                    sigleLineformat(
                        bankEMITenureDataModal.tenureLabel,
                        AlignMode.RIGHT
                    )
                )
            }
            printer?.addMixStyleText(textBlockList)
            textBlockList.clear()

            textBlockList.add(sigleLineformat("Card Issuer:", AlignMode.LEFT))
            if (bankEMIIssuerTAndCDataModal != null) {
                textBlockList.add(
                    sigleLineformat(
                        bankEMIIssuerTAndCDataModal.issuerName,
                        AlignMode.RIGHT
                    )
                )
            }
            printer?.addMixStyleText(textBlockList)
            textBlockList.clear()

        }
//endregion

        //region=====================Printing Merchant Brand Purchase Details:-
        if (batchTable.transactionType.equals(EDashboardItem.BRAND_EMI.title)) {
        //region====================Printing DBD Wise TAndC Brand EMI==================
            if (!isNoEmiOnlyCashBackApplied!!) {
                if (copyType?.equals(EPrintCopyType.MERCHANT) == true && (brandEMIMasterDataModal?.mobileNumberBillNumberFlag?.get(
                        3
                    ) == '1')
                ) {
                    if (!TextUtils.isEmpty(bankEMITenureDataModal?.tenureWiseDBDTAndC)) {
                        val tenureWiseTAndC: String? = bankEMITenureDataModal?.tenureWiseDBDTAndC
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

        }


        }
        printSeperator()
        if (batchTable.transactionType != BhTransactionType.BRAND_EMI.type )
        baseAmounthandling(batchTable)

    }

    private fun printProduactData(batchTable: BatchTable){

        val brandEMIMasterDataModal: BrandEMIMasterDataModal? = batchTable.emiBrandData
        val brandEMISubCategoryTable: BrandEMISubCategoryTable? = batchTable.emiSubCategoryData
        val brandEMICategoryData: BrandEMISubCategoryTable? = batchTable.emiCategoryData
        val brandEMIProductDataModal: BrandEMIProductDataModal? = batchTable.emiProductData
        val bankEMITenureDataModal: BankEMITenureDataModal? = batchTable.emiTenureDataModel
        val bankEMIIssuerTAndCDataModal: BankEMIIssuerTAndCDataModal? = batchTable.emiIssuerDataModel

        val issuerId = bankEMIIssuerTAndCDataModal?.issuerID
        sigleLineText("-----**Product Details**-----", AlignMode.CENTER)
        if (brandEMIMasterDataModal != null) {
            textBlockList.add(sigleLineformat("Mer/Mfr Name:", AlignMode.LEFT))
            textBlockList.add(
                sigleLineformat(
                    "${brandEMIMasterDataModal?.brandName}",
                    AlignMode.RIGHT
                )
            )
            printer?.addMixStyleText(textBlockList)
            textBlockList.clear()

            textBlockList.add(sigleLineformat("Prod Cat:", AlignMode.LEFT))
            textBlockList.add(
                sigleLineformat(
                    "${brandEMICategoryData?.categoryName}",
                    AlignMode.RIGHT
                )
            )
            printer?.addMixStyleText(textBlockList)
            textBlockList.clear()


            if (brandEMIProductDataModal?.producatDesc == "subCat") {
                if (!brandEMIProductDataModal?.productCategoryName.isNullOrEmpty()) {

                    textBlockList.add(sigleLineformat("Prod desc:", AlignMode.LEFT))
                    textBlockList.add(
                        sigleLineformat(
                            "${brandEMIProductDataModal?.productCategoryName}",
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
                    "${brandEMIProductDataModal?.productName}",
                    AlignMode.RIGHT
                )
            )
            printer?.addMixStyleText(textBlockList)
            textBlockList.clear()

            if (!TextUtils.isEmpty(batchTable?.imeiOrSerialNum)) {
                textBlockList.add(sigleLineformat("Prod ${"IEMI"}:", AlignMode.LEFT))
                textBlockList.add(
                    sigleLineformat(
                        "${batchTable?.imeiOrSerialNum}",
                        AlignMode.RIGHT
                    )
                )
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

            }
            val mobnum=batchTable.mobileNumber?:""
            if (!TextUtils.isEmpty(mobnum)) {
                when (brandEMIMasterDataModal.mobileNumberBillNumberFlag.substring(1, 2)) {
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
            if (!TextUtils.isEmpty(bankEMITenureDataModal?.tenureTAndC)) {
                printSeperator()
                val tenureTAndC: String? = bankEMITenureDataModal?.tenureTAndC
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
    private fun baseAmounthandling(batchTable: BatchTable){

        val bankEMITenureDataModal: BankEMITenureDataModal? = batchTable.emiTenureDataModel
        val bankEMIIssuerTAndCDataModal: BankEMIIssuerTAndCDataModal? = batchTable.emiIssuerDataModel
        var currencySymbol: String? = "Rs"
        val terminalData = getTptData()
        currencySymbol = terminalData?.currencySymbol
        if (!TextUtils.isEmpty(bankEMITenureDataModal?.transactionAmount)) {
            var baseAmount =  "%.2f".format((((bankEMITenureDataModal?.transactionAmount)?.toDouble())?.div(100)).toString().toDouble())
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
                    val isHdfcPresent = batch.find{ it.hostBankID.equals("01") || it.hostBankID.equals("1")}
                    val isAmexPresent = batch.find{ it.hostBankID.equals(AMEX_BANK_CODE) || it.hostBankID.equals(AMEX_BANK_CODE_SINGLE_DIGIT)}
                    if(isHdfcPresent?.hostBankID.equals("01") || isHdfcPresent?.hostBankID.equals("1")){
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

                                totalMap[b.transactionType] =
                                    b.transactionalAmmount?.toLong()
                                        ?.let { SummeryTotalType(1, it) }!!
                            }
                            val transAmount = "%.2f".format(
                                b.transactionalAmmount?.toDouble()
                                    ?.div(100)
                            )
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
                                    "${b.panMask}",
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

    fun printSettlementReportupdate(
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
                /*   alignLeftRightText(textInLineFormatBundle, "MID : ${batch[0].mid}", "TID : ${batch[0].tid}")
                   alignLeftRightText(textInLineFormatBundle, "BATCH NO  : ${batch[0].batchNumber}", "")*/

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
