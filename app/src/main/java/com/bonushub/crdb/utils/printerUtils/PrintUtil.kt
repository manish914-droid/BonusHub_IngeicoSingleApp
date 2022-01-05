package com.bonushub.crdb.utils.printerUtils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.DeadObjectException
import android.os.RemoteException
import android.text.TextUtils
import android.util.Log
import com.bonushub.crdb.BuildConfig
import com.bonushub.crdb.di.DBModule
import com.bonushub.crdb.model.local.BatchTable
import com.bonushub.crdb.model.local.BatchTableReversal
import com.bonushub.crdb.model.local.BrandEMISubCategoryTable
import com.bonushub.crdb.model.local.TerminalParameterTable
import com.bonushub.crdb.model.remote.BankEMIIssuerTAndCDataModal
import com.bonushub.crdb.model.remote.BankEMITenureDataModal
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.model.remote.BrandEMIProductDataModal
import com.bonushub.crdb.utils.*
import com.bonushub.crdb.utils.Field48ResponseTimestamp.getBrandTAndCData
import com.bonushub.crdb.utils.Field48ResponseTimestamp.getBrandTAndCDataByBrandId
import com.bonushub.crdb.utils.Field48ResponseTimestamp.getIssuerTAndCDataByIssuerId
import com.bonushub.crdb.utils.Field48ResponseTimestamp.getTptData
import com.bonushub.crdb.utils.Field48ResponseTimestamp.panMasking
import com.bonushub.crdb.utils.Field48ResponseTimestamp.transactionType2Name

import com.bonushub.pax.utils.EPrintCopyType
import com.bonushub.pax.utils.BhTransactionType
import com.bonushub.pax.utils.EDashboardItem
import com.bonushub.pax.utils.SplitterTypes
import com.google.gson.Gson
import com.ingenico.hdfcpayment.model.ReceiptDetail
import com.ingenico.hdfcpayment.type.TransactionType
import com.usdk.apiservice.aidl.printer.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

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
             isNoEmiOnlyCashBackApplied  =  bankEMITenureDataModal?.tenure=="1"
            setLogoAndHeader()
            val terminalData = getTptData()
            try {
                receiptDetail.merAddHeader1?.let { sigleLineText(it, AlignMode.CENTER) }
                receiptDetail.merAddHeader2?.let { sigleLineText(it, AlignMode.CENTER) }
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
                            "M.BILL NO:${receiptDetail.invoice}",
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
                        receiptDetail.txnName?.let { sigleLineText(it, AlignMode.CENTER) }
                    }
                }



                textBlockList.add(
                    sigleLineformat(
                        "CARD TYPE:${receiptDetail.appName}",
                        AlignMode.LEFT
                    )
                )
                textBlockList.add(sigleLineformat("EXP:XX/XX", AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)

                textBlockList.clear()


                textBlockList.add(
                    sigleLineformat(
                        "CARD NO:${receiptDetail.maskedPan}",
                        AlignMode.LEFT
                    )
                )
                textBlockList.add(sigleLineformat("Chip", AlignMode.RIGHT))
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

                textBlockList.add(sigleLineformat("TVR:${receiptDetail.tvr}", AlignMode.LEFT))
                textBlockList.add(sigleLineformat("TSI:${receiptDetail.tsi}", AlignMode.RIGHT))
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

                when (txnName) {
                    "SALE", "SALECASH" -> {
                        saleTransaction(receiptDetail)
                    }
                   "EMI SALE" -> {
                        printEMISale(batchTable)

                   }

                    "REVERSAL" -> {

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
                if(txnName.equals("EMI SALE")){
                    printBrandTnC(batchTable)
                    printSeperator()
                }
                //region=====================BRAND PRODUACT DATA===============
                if (batchTable.transactionType == BhTransactionType.BRAND_EMI.type) {

                    printProduactData(batchTable)
                }

                if (receiptDetail.entryMode.equals("CLESS_EMV")) {

                    textBlockList.add(sigleLineformat("PIN NOT REQUIRED FOR CONTACTLESS TRANSACTION UPTO ${receiptDetail?.cvmRequiredLimit}", AlignMode.CENTER))
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()

                } else {
                    if (receiptDetail.isSignRequired == true)
                        sigleLineText("PIN VERIFIDE OK", AlignMode.CENTER)
                    if (receiptDetail.isSignRequired != true)
                        sigleLineText("SIGNATURE NOT REQUIRED", AlignMode.CENTER)
                    receiptDetail.cardHolderName?.let { sigleLineText(it, AlignMode.CENTER) }
                }
                textBlockList.add(sigleLineformat("I am satisfied with goods received and agree to pay issuer agreenent.", AlignMode.CENTER))
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
                                     /*       textBlockList.add(
                                                sigleLineformat(
                                                    st, AlignMode.LEFT
                                                )
                                            )*/
                                            printer?.setHzScale(HZScale.SC1x1)
                                            printer?.setHzSize(HZSize.DOT24x16)
                                            printer?.addText( AlignMode.LEFT, st)
                                        }
                                    }
                                }
                            } else {
                           /*     textBlockList.add(
                                    sigleLineformat(
                                        "# ${issuerTAndCData.footerTAndC}", AlignMode.LEFT
                                    )
                                )*/
                                printer?.setHzScale(HZScale.SC1x1)
                                printer?.setHzSize(HZSize.DOT24x16)
                                printer?.addText( AlignMode.LEFT, "# ${issuerTAndCData.footerTAndC}")
                            }
                        }
                    }
                }

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

    private fun saleTransaction(receiptDetail: ReceiptDetail) {
        textBlockList.add(sigleLineformat("SALE AMOUNT:", AlignMode.LEFT))
        val amt = (((receiptDetail.txnAmount)?.toLong())?.div(100)).toString()

        val tipAmount = (((receiptDetail.txnOtherAmount)?.toLong())?.div(100)).toString()
        textBlockList.add(sigleLineformat("INR:${"%.2f".format(amt.toDouble())}", AlignMode.RIGHT))
        printer?.addMixStyleText(textBlockList)
        textBlockList.clear()
        if (receiptDetail.txnName.equals("SALE")) {

            if (tipAmount == "0") {
                textBlockList.add(sigleLineformat("TIP AMOUNT:", AlignMode.LEFT))
                textBlockList.add(sigleLineformat(".............", AlignMode.RIGHT))
            } else {
                textBlockList.add(sigleLineformat("TIP AMOUNT:", AlignMode.LEFT))
                textBlockList.add(
                    sigleLineformat(
                        "%.2f".format(tipAmount.toDouble()),
                        AlignMode.RIGHT
                    )
                )
            }
        } else {
            textBlockList.add(sigleLineformat("CASH AMOUNT: ", AlignMode.LEFT))
            textBlockList.add(
                sigleLineformat(
                    "INR:${"%.2f".format(tipAmount.toDouble())}",
                    AlignMode.RIGHT
                )
            )
        }
        // textBlockList.add(sigleLineformat( "00",AlignMode.RIGHT))
        printer?.addMixStyleText(textBlockList)
        textBlockList.clear()
        val totalAmount = "%.2f".format((amt.toDouble() + tipAmount.toDouble()))
        textBlockList.add(sigleLineformat("TOTAL AMOUNT:", AlignMode.LEFT))
        textBlockList.add(sigleLineformat("INR:${totalAmount}", AlignMode.RIGHT))
        printer?.addMixStyleText(textBlockList)
        textBlockList.clear()
    }

    private fun voidTransaction(receiptDetail: ReceiptDetail) {
        textBlockList.add(sigleLineformat("BASE AMOUNT:", AlignMode.LEFT))
        val amt = (((receiptDetail.txnAmount)?.toLong())?.div(100)).toString()
        textBlockList.add(sigleLineformat("INR:${"%.2f".format(amt.toDouble())}", AlignMode.RIGHT))
        printer?.addMixStyleText(textBlockList)
        textBlockList.clear()

        textBlockList.add(sigleLineformat("TOTAL AMOUNT:", AlignMode.LEFT))
        textBlockList.add(sigleLineformat("INR:${"%.2f".format(amt.toDouble())}", AlignMode.RIGHT))
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

        textBlockList.add(sigleLineformat("TXN AMOUNT", AlignMode.LEFT))
        val txnAmount =
            (((receiptDetail.txnAmount)?.toLong())?.div(100)).toString()
        logger("txnAmount",""+txnAmount)
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
            if (bankEMITenureDataModal?.instantDiscount.isNotBlank() && bankEMITenureDataModal?.instantDiscount.toInt() > 0) {
                val instantDis =
                    "%.2f".format(
                        (((bankEMITenureDataModal.instantDiscount).toDouble()).div(
                            100
                        )).toString().toDouble()
                    )

                textBlockList.add(sigleLineformat("INSTA DISCOUNT", AlignMode.LEFT))
                val authAmount =
                    (((bankEMITenureDataModal?.transactionAmount)?.toLong())?.div(100)).toString()
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
        val authAmount =
            (((bankEMITenureDataModal?.transactionAmount)?.toLong())?.div(100)).toString()
        textBlockList.add(
            sigleLineformat(
                "$currencySymbol:${"%.2f".format(authAmount.toDouble())}",
                AlignMode.RIGHT
            )
        )
        printer?.addMixStyleText(textBlockList)
        textBlockList.clear()
        textBlockList.add(sigleLineformat("CARD ISSUER", AlignMode.LEFT))
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
        val tenureDuration = "${bankEMITenureDataModal?.tenure} Months"
        val tenureHeadingDuration = "${bankEMITenureDataModal?.tenure} Months Scheme"
        var roi = bankEMITenureDataModal?.tenureInterestRate?.toInt()?.let { divideAmountBy100(it).toString() }
        var loanamt = bankEMITenureDataModal?.loanAmount?.toInt()?.let { divideAmountBy100(it).toString() }
        roi = "%.2f".format(roi?.toDouble()) + " %"
        loanamt = "%.2f".format(loanamt?.toDouble())
        textBlockList.add(sigleLineformat("ROI", AlignMode.LEFT))
        textBlockList.add(
            sigleLineformat(
                "${roi}",
                AlignMode.RIGHT
            )
        )
        printer?.addMixStyleText(textBlockList)
        textBlockList.clear()

        textBlockList.add(sigleLineformat("TENURE", AlignMode.LEFT))
        textBlockList.add(sigleLineformat("${tenureDuration}", AlignMode.RIGHT))
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
                    nextLineAppendStr = "Payback Amt"
                }
                "52", "55" ,"54"-> {
                    nextLineAppendStr = "Cashback Amt"
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


            textBlockList.add(sigleLineformat(cashBackAmountHeadingText, AlignMode.LEFT))
            textBlockList.add(sigleLineformat(" ", AlignMode.RIGHT))
            printer?.addMixStyleText(textBlockList)
            textBlockList.clear()


            textBlockList.add(sigleLineformat(nextLineAppendStr, AlignMode.LEFT))
            textBlockList.add(sigleLineformat(":$currencySymbol $cashBackAmount", AlignMode.RIGHT))
            printer?.addMixStyleText(textBlockList)
            textBlockList.clear()
        } else {
            if (bankEMITenureDataModal?.cashBackAmount != "0") {
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
                textBlockList.add(sigleLineformat("$currencySymbol ${bankEMITenureDataModal?.discountCalculatedValue}", AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

            } else {
                textBlockList.add(sigleLineformat(discountPercentHeadingText, AlignMode.LEFT))
                textBlockList.add(sigleLineformat("$currencySymbol ${bankEMITenureDataModal?.discountCalculatedValue}", AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()
            }
        }
        if (!TextUtils.isEmpty(bankEMITenureDataModal?.discountAmount) && bankEMITenureDataModal?.discountAmount != "0") {
            val discAmount =
                "%.2f".format(bankEMITenureDataModal?.discountAmount?.toFloat()?.div(100))

            if (islongTextHeading) {

              /*  textBlockList.add(sigleLineformat(discountPercentHeadingText, AlignMode.LEFT))
                textBlockList.add(sigleLineformat("", AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()*/

                textBlockList.add(sigleLineformat(nextLineAppendStr, AlignMode.LEFT))
                textBlockList.add(sigleLineformat("$currencySymbol ${discAmount}", AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()


            } else {

                textBlockList.add(sigleLineformat(nextLineAppendStr, AlignMode.LEFT))
                textBlockList.add(sigleLineformat("$currencySymbol ${discAmount}", AlignMode.RIGHT))
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
        var emiAmount = bankEMITenureDataModal?.emiAmount?.toInt()?.let { divideAmountBy100(it).toString() }
        textBlockList.add(
            sigleLineformat(
                "$currencySymbol:${emiAmount}",
                AlignMode.RIGHT
            )
        )
        printer?.addMixStyleText(textBlockList)
        textBlockList.clear()

        textBlockList.add(sigleLineformat("TOTAL INTEREST", AlignMode.LEFT))
        var totalInterestPay = bankEMITenureDataModal?.totalInterestPay?.toInt()?.let { divideAmountBy100(it).toString() }
        textBlockList.add(
            sigleLineformat(
                "$currencySymbol:${totalInterestPay}",
                AlignMode.RIGHT
            )
        )
        printer?.addMixStyleText(textBlockList)
        textBlockList.clear()

        textBlockList.add(sigleLineformat("TOTAL AMT(With Int)", AlignMode.LEFT))
        var totalEmiPay = bankEMITenureDataModal?.totalEmiPay?.toInt()?.let { divideAmountBy100(it).toString() }
        textBlockList.add(
            sigleLineformat(
                "$currencySymbol:${totalEmiPay}",
                AlignMode.RIGHT
            )
        )
        printer?.addMixStyleText(textBlockList)
        textBlockList.clear()
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
        if (issuerTAndCData != null) {
            logger("getting issuer h tnc=",issuerTAndCData.headerTAndC.toString(),"e")
            issuerHeaderTAndC =
                if (batchTable.transactionType == BhTransactionType.TEST_EMI.type) {
                    testTnc.split(SplitterTypes.POUND.splitter)
                } else {
                    issuerTAndCData.headerTAndC?.split(SplitterTypes.POUND.splitter)

                }
            logger("getting header tnc=",issuerTAndCData.headerTAndC.toString(),"e")
            logger("getting footer tnc=",issuerTAndCData.footerTAndC.toString(),"e")
        }
        if (issuerHeaderTAndC?.isNotEmpty() == true) {
            for (i in issuerHeaderTAndC.indices) {
                if (!TextUtils.isEmpty(issuerHeaderTAndC?.get(i))) {
                    val limit = 46
                    if (!(issuerHeaderTAndC[i].isBlank())) {
                        val emiTnc = "#" + issuerHeaderTAndC[i]
                        val chunks: List<String> = chunkTnC(emiTnc, limit)
                        for (st in chunks) {
                            logger("issuerHeaderTAndC", st, "e")
                            printer?.setHzScale(HZScale.SC1x1)
                            printer?.setHzSize(HZSize.DOT24x16)
                            printer?.addText( AlignMode.LEFT, st)
                         /*   textBlockList.add(
                                sigleLineformat(
                                    st,
                                    AlignMode.CENTER
                                )
                            )
                            printer?.addMixStyleText(textBlockList)
                            textBlockList.clear()*/
                   /*         textBlockList.add(sigleLineformat(st, AlignMode.LEFT))
                            printer?.addMixStyleText(textBlockList)
                            textBlockList.clear()*/
                        }
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
           val chunk: List<String> = chunkTnC(brandTnc)
            for (st in chunk) {
               logger("Brand Tnc", st, "e")
               /*    sigleLineText(
                st.replace(bankEMIFooterTAndCSeparator, "")
                    .replace(Companion.disclaimerIssuerClose, ""), AlignMode.CENTER
            )*/
               printer?.setHzScale(HZScale.SC1x1)
               printer?.setHzSize(HZSize.DOT24x16)
               printer?.addText(
                   AlignMode.LEFT, st.replace(bankEMIFooterTAndCSeparator, "")
                       .replace(Companion.disclaimerIssuerClose, "")
               )
/*            textBlockList.add(
                sigleLineformat(
                    st.replace(bankEMIFooterTAndCSeparator, "")
                        .replace(Companion.disclaimerIssuerClose, ""), AlignMode.LEFT
                )
            )
            printer?.addMixStyleText(textBlockList)
            textBlockList.clear()*/

           }
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
                        printer?.setHzScale(HZScale.SC1x1)
                        printer?.setHzSize(HZSize.DOT24x16)
                        printer?.addText( AlignMode.LEFT,  st.replace(bankEMIFooterTAndCSeparator, "")
                            .replace(Companion.disclaimerIssuerClose, ""))
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
                        val tenureWiseTAndC: List<String>? =
                            bankEMITenureDataModal?.tenureWiseDBDTAndC?.let { chunkTnC(it) }
                        if (tenureWiseTAndC != null) {
                            for (st in tenureWiseTAndC) {
                                logger("tenureWiseDBDTAndC", st, "e")
                                textBlockList.add(sigleLineformat(st, AlignMode.LEFT))
                                printer?.addMixStyleText(textBlockList)
                                textBlockList.clear()

                            }
                        }
                    }
                }

            }

        }
        }
    }

    private fun printProduactData(batchTable: BatchTable){

        val brandEMIMasterDataModal: BrandEMIMasterDataModal? = batchTable.emiBrandData
        val brandEMISubCategoryTable: BrandEMISubCategoryTable? = batchTable.emiCategoryData
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
                    "${brandEMISubCategoryTable?.categoryName}",
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
            val mobnum="9808080810"
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
                val tenureTAndC: List<String>? = bankEMITenureDataModal?.tenureTAndC?.let {
                    chunkTnC(
                        it
                    )
                }
                if (tenureTAndC != null) {
                    for (st in tenureTAndC) {
                        logger("TNC", st, "e")
          /*              textBlockList.add(sigleLineformat(st, AlignMode.CENTER))

                        printer?.addMixStyleText(textBlockList)
                        textBlockList.clear()
*/
                        printer?.setHzScale(HZScale.SC1x1)
                        printer?.setHzSize(HZSize.DOT24x16)
                        printer?.addText( AlignMode.LEFT,  st)
                    }
                }
                printSeperator()
            }
            //endregion


        }

    }

    fun printDetailReportupdate(
        batch: MutableList<BatchTable>,
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
                    //  ------------------------------------------
                    val td = System.currentTimeMillis()
                    val formatdate = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                    val formattime = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)

                    val date = formatdate.format(td)
                    val time = formattime.format(td)


                    textBlockList.add(sigleLineformat("DATE:${date}", AlignMode.LEFT))
                    textBlockList.add(sigleLineformat("TIME:${date}", AlignMode.RIGHT))
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

                        textBlockList.add(
                            sigleLineformat(
                                "${b.receiptData?.txnName}",
                                AlignMode.LEFT
                            )
                        )
                        textBlockList.add(sigleLineformat(transAmount, AlignMode.RIGHT))
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
                                    sigleLineformat(
                                        "=" + m.count + " ${
                                            getCurrencySymbol(
                                                tpt
                                            )
                                        }", AlignMode.CENTER
                                    )
                                )
                                textBlockList.add(
                                    sigleLineformat(
                                        "%.2f".format(
                                            (((m.total).toDouble()).div(
                                                100
                                            )).toString().toDouble()
                                        ), AlignMode.RIGHT
                                    )
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
                    textBlockList.add(sigleLineformat("ROC", AlignMode.RIGHT))
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

                        textBlockList.add(
                            sigleLineformat(
                                "${b.receiptData?.txnName}",
                                AlignMode.LEFT
                            )
                        )
                        textBlockList.add(sigleLineformat(transAmount, AlignMode.RIGHT))
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
                            b.receiptData?.maskedPan?.let {
                                sigleLineformat(
                                    it,
                                    AlignMode.RIGHT
                                )
                            }?.let {
                                textBlockList.add(
                                    it
                                )
                            }
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


                            sigleLineText("***TOTAL REVERSAL***", AlignMode.CENTER)
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
                                        "=" + m.count + " ${
                                            getCurrencySymbol(
                                                tpt
                                            )
                                        }", AlignMode.CENTER
                                    )
                                )
                                textBlockList.add(
                                    sigleLineformat(
                                        "%.2f".format(
                                            (((m.total).toDouble()).div(
                                                100
                                            )).toString().toDouble()
                                        ), AlignMode.RIGHT
                                    )
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
                setLogoAndHeader()
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
                val tpt = getTptData()

                setLogoAndHeader()

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

                    if (it.transactionType == BhTransactionType.EMI_SALE.type || it.transactionType == BhTransactionType.BRAND_EMI.type || it.transactionType == BhTransactionType.BRAND_EMI_BY_ACCESS_CODE.type) {
                        it.receiptData?.cardType = it.receiptData?.appName
                        it.transactionType = BhTransactionType.EMI_SALE.type
                    }
                    if (it.transactionType == BhTransactionType.VOID_EMI.type) {
                        it.receiptData?.cardType = it.receiptData?.appName
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
                        _issuerName = it.receiptData?.cardType
                        if (map.containsKey(it.receiptData?.tid + it.receiptData?.mid + it.receiptData?.batchNumber + it.receiptData?.cardType)) {
                            _issuerName = it.receiptData?.cardType

                            val ma =
                                map[it.receiptData?.tid + it.receiptData?.mid + it.receiptData?.batchNumber + it.receiptData?.cardType] as MutableMap<Int, SummeryModel>
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
                            map[it.receiptData?.tid + it.receiptData?.mid + it.receiptData?.batchNumber + it.receiptData?.cardType] =
                                hm
                            it.receiptData?.tid?.let { it1 -> list.add(it1) }
                        }
                    } else {
                        tempTid = it.receiptData?.tid
                        _issuerName = it.receiptData?.cardType
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
                        map[it.receiptData?.tid + it.receiptData?.mid + it.receiptData?.batchNumber + it.receiptData?.cardType] =
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


                        if (ietration > 0) {
                            printSeperator()

                            textBlockList.add(sigleLineformat("MID:${hostMid}", AlignMode.LEFT))
                            textBlockList.add(sigleLineformat("TID:${hostTid}", AlignMode.RIGHT))
                            printer?.addMixStyleText(textBlockList)
                            textBlockList.clear()

                            textBlockList.add(
                                sigleLineformat(
                                    "BATCH NO:${hostBatchNumber}",
                                    AlignMode.LEFT
                                )
                            )
                            printer?.addMixStyleText(textBlockList)
                            textBlockList.clear()
                            ietration--
                        }
                        if (cardIssuer.isNullOrEmpty()) {
                            cardIssuer = _issuerName.toString()
                            _issuerNameString = "CARD ISSUER"

                        }

                        printSeperator()

                        textBlockList.add(sigleLineformat(_issuerNameString, AlignMode.LEFT))

                        printer?.addMixStyleText(textBlockList)
                        textBlockList.clear()

                        // if(ind==0){
                        textBlockList.add(sigleLineformat("TXN TYPE", AlignMode.LEFT))
                        textBlockList.add(sigleLineformat("TOTAL", AlignMode.CENTER))
                        textBlockList.add(sigleLineformat("COUNT", AlignMode.RIGHT))
                        printer?.addMixStyleText(textBlockList)
                        textBlockList.clear()


                    }
                    for ((k, m) in _map) {

                        val amt =
                            "%.2f".format((((m.total)?.toDouble())?.div(100)).toString().toDouble())
                        if (k == BhTransactionType.PRE_AUTH_COMPLETE.type || k == BhTransactionType.VOID_PREAUTH.type) {
                            // need Not to show
                        } else {

                            m.type?.let {
                                sigleLineformat(
                                    it.toUpperCase(Locale.ROOT),
                                    AlignMode.LEFT
                                )
                            }?.let {
                                textBlockList.add(it)
                            }
                            //textBlockList.add(sigleLineformat(amt, AlignMode.CENTER)) // old
                            textBlockList.add(sigleLineformat("${amt} ${getCurrencySymbol(tpt)}", AlignMode.CENTER))
                            /*textBlockList.add(
                                sigleLineformat(
                                    "${m.count} ${getCurrencySymbol(tpt)}",
                                    AlignMode.RIGHT
                                )
                            )*/
                            textBlockList.add(
                                sigleLineformat(
                                    "${m.count}",
                                    AlignMode.RIGHT
                                )
                            )
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
                            /* alignLeftRightText(
                                 textInLineFormatBundle,
                                 "${transactionType2Name(k).toUpperCase(Locale.ROOT)}${"     =" + m.count}",
                                 "Rs.     ${"%.2f".format(((m.total).toDouble() / 100))}"

                             )*/


                            textBlockList.add(
                                sigleLineformat(
                                    transactionType2Name(k).toUpperCase(
                                        Locale.ROOT
                                    ), AlignMode.LEFT
                                )
                            )
                            textBlockList.add(
                                sigleLineformat("${"%.2f".format(
                                    (((m.total).toDouble()).div(
                                        100
                                    )).toString().toDouble()
                                )} ${getCurrencySymbol(tpt)}"
                                    , AlignMode.CENTER
                                )
                            )
                            textBlockList.add(
                                sigleLineformat(
                                    "  = " + m.count , AlignMode.RIGHT
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
                if (!isLastSummary) {

                }
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

    private fun printSeperator() {
        sigleLineText("-----------------------------------------", AlignMode.CENTER)
    }

    private fun setLogoAndHeader() {
        val image: ByteArray? = context?.let { printLogo(it, "hdfc_print_logo.bmp") }
        printer?.addBmpImage(0, FactorMode.BMP1X1, image)
    }

    //Below method is used to chunk the TNC's text(words's are not splitted in between) which was printed on EMI sale :-
    fun chunkTnC(s: String, limit: Int = 32): List<String> {

        var str = s
        val parts: MutableList<String> = ArrayList()
        while (str.length > 34) {
            var splitAt = 34 - 1
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
    }
}
