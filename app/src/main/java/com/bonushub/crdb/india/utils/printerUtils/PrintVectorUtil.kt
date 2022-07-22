package com.bonushub.crdb.india.utils.printerUtils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.DeadObjectException
import android.os.RemoteException
import android.text.TextUtils
import android.util.Log
import com.bonushub.crdb.india.BuildConfig
import com.bonushub.crdb.india.HDFCApplication
import com.bonushub.crdb.india.model.local.*
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getTptData
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.panMasking
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.transactionType2Name
import com.bonushub.crdb.india.utils.Utility
import com.bonushub.crdb.india.view.fragments.pre_auth.PendingPreauthData
import com.bonushub.crdb.india.vxutils.*
import com.google.gson.Gson
import com.usdk.apiservice.aidl.printer.ASCScale
import com.usdk.apiservice.aidl.printer.ASCSize
import com.usdk.apiservice.aidl.printer.AlignMode
import com.usdk.apiservice.aidl.printer.FactorMode
import com.usdk.apiservice.aidl.vectorprinter.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


class PrintVectorUtil(context: Context?) {

    private var context:Context? = null
    private var vectorPrinter: UVectorPrinter? = null
    private var footerText = arrayOf("*Thank You Visit Again*", "POWERED BY")
    var nextLineAppendStr = ""
    private var _issuerName: String? = null
    private var _issuerNameString = "ISSUER"

    private val textBlockList: HashMap<Int,String> = HashMap()
    private val textFormatGlobal = Bundle()
    var td:Long?=0L


    init {
        this.context = context

        try{
            PrinterFonts.initialize(HDFCApplication.appContext.assets)

            vectorPrinter = DeviceHelper.getVectorPrinter()

            val initFormat = Bundle()
            val path = HDFCApplication.appContext.externalCacheDir?.path + "/fonts/" + "f25bank.ttf"

            if(path.isNotEmpty()) {
                initFormat.putString(VectorPrinterData.CUSTOM_TYPEFACE_PATH, path)
            }

            //        initFormat.putInt(VectorPrinterData.LINE_SPACING, 10);
            initFormat.putFloat(VectorPrinterData.LETTER_SPACING, 0f)
            initFormat.putBoolean(VectorPrinterData.AUTO_CUT_PAPER, true)

            vectorPrinter?.init(initFormat)

            textFormatGlobal.putInt(VectorPrinterData.ALIGNMENT, AlignMode.LEFT)
            textFormatGlobal.putInt(VectorPrinterData.TEXT_SIZE, TextSize.SMALL)
            textFormatGlobal.putBoolean(VectorPrinterData.BOLD, false)
            textFormatGlobal.putFloat(VectorPrinterData.LETTER_SPACING, 0f)

        }catch (ex: DeadObjectException) {
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
        }


        /*val textFormat = Bundle()
        textFormat.putInt(VectorPrinterData.ALIGNMENT, Alignment.NORMAL)
//        textFormat.putInt(VectorPrinterData.TEXT_SIZE, TextSize.LARGE);
        //        textFormat.putInt(VectorPrinterData.TEXT_SIZE, TextSize.LARGE);
        textFormat.putBoolean(VectorPrinterData.BLACK_BACKGROUND, true)*/
        /*try {
            printUnionPay()
        } catch (e: IOException) {
            e.printStackTrace()
        }*/

        //val startTime = System.currentTimeMillis()
    }

    @Throws(RemoteException::class, IOException::class)
    private fun printUnionPay() {
        val boldFormat = Bundle()
        val textFormat = Bundle()
        textFormat.putInt(VectorPrinterData.ALIGNMENT, Alignment.NORMAL)
        textFormat.putInt(VectorPrinterData.TEXT_SIZE, TextSize.NORMAL)
        vectorPrinter!!.addText(
            textFormat, """
     welcome to bonus hub
     
     """.trimIndent()
        )
        boldFormat.putInt(VectorPrinterData.ALIGNMENT, Alignment.CENTER)
        boldFormat.putInt(VectorPrinterData.TEXT_SIZE, TextSize.LARGE)
        boldFormat.putBoolean(VectorPrinterData.BOLD, true)
        vectorPrinter!!.addText(
            boldFormat, """
     银联POS签购单
     
     """.trimIndent()
        )
        vectorPrinter!!.addText(
            textFormat, """
     商户存根(MERCHANT COPY)
     
     """.trimIndent()
        )
        vectorPrinter!!.addText(textFormat, "----------------------------------------------\n")
        vectorPrinter!!.addText(
            textFormat, """
     商户名(MERCHANT NAME):
     
     """.trimIndent()
        )
        boldFormat.putInt(VectorPrinterData.TEXT_SIZE, TextSize.NORMAL)
        vectorPrinter!!.addText(
            boldFormat, """
     矢量打印测试商户
     
     """.trimIndent()
        )
        vectorPrinter!!.addText(
            textFormat, """
     商户编号(MERCHANT CODE):12345678901234
     
     """.trimIndent()
        )
        vectorPrinter!!.addText(
            textFormat, """
     终端号(TERMINAL NO):12345678
     
     """.trimIndent()
        )
        vectorPrinter!!.addText(
            textFormat, """
     操作员号(OPERATOR NO):01
     
     """.trimIndent()
        )
        vectorPrinter!!.addText(
            textFormat, """
     收单行(ACQUIRER):Testsystem
     
     """.trimIndent()
        )
        vectorPrinter!!.addText(
            textFormat, """
     发卡行(ISSUER):Testsystem
     
     """.trimIndent()
        )
        vectorPrinter!!.addText(
            textFormat, """
     卡号(CARD NO)
     
     """.trimIndent()
        )
        boldFormat.putInt(VectorPrinterData.ALIGNMENT, TextSize.NORMAL)
        vectorPrinter!!.addText(
            boldFormat, """
     622576******6691 /C
     
     """.trimIndent()
        )
        vectorPrinter!!.addText(
            textFormat, """
     有效期(EXP DATE):2024/08
     
     """.trimIndent()
        )
        vectorPrinter!!.addText(
            textFormat, """
     消费类别(TRANS TYPE)
     
     """.trimIndent()
        )
        vectorPrinter!!.addText(
            boldFormat, """
     消费(SALE)
     
     """.trimIndent()
        )
        val weights = intArrayOf(3, 3, 3, 3)
        val aligns =
            intArrayOf(Alignment.NORMAL, Alignment.OPPOSITE, Alignment.NORMAL, Alignment.OPPOSITE)
        vectorPrinter!!.addTextColumns(
            null,
            arrayOf("凭证号:", "002134", "授权码", "118525"),
            weights,
            aligns
        )
        vectorPrinter!!.addText(
            textFormat, """
     批次号(BATCH NO):000003
     
     """.trimIndent()
        )
        vectorPrinter!!.addText(
            textFormat, """
     参考号(REFER NO):123212000003
     
     """.trimIndent()
        )
        vectorPrinter!!.addText(
            textFormat, """
     日期/时间(DATE/TIME):2020/03/02 10:04:07
     
     """.trimIndent()
        )
        vectorPrinter!!.addText(
            textFormat, """
     交易金额(AMOUNT)
     
     """.trimIndent()
        )
        vectorPrinter!!.addText(
            boldFormat, """
     RMB 0.01
     
     """.trimIndent()
        )
        vectorPrinter!!.addText(textFormat, "\n")
        vectorPrinter!!.addText(textFormat, "----------------------------------------------\n")
        vectorPrinter!!.addText(textFormat, "\n")
        vectorPrinter!!.addText(textFormat, "备注(REFERENCE):\n")
        vectorPrinter!!.addText(textFormat, "折扣活动名额剩余：10%\n")
        vectorPrinter!!.addText(textFormat, "卡信息：借记卡\n")
        vectorPrinter!!.addText(textFormat, "\n")
        textFormat.putInt(VectorPrinterData.TEXT_SIZE, TextSize.SMALL)
        vectorPrinter!!.addText(textFormat, "ARQC:F51234512039812\n")
        vectorPrinter!!.addText(textFormat, "APP LABEL:PROC CREDIT\n")
        vectorPrinter!!.addText(textFormat, "APM:\n\n")
        vectorPrinter!!.addText(textFormat, "UMPR NUM:200000000\n")
        vectorPrinter!!.addText(textFormat, "AIP:7c00  CVMR:\n")
        vectorPrinter!!.addText(textFormat, "1AD:012093281092839018293819203809123\n")
        vectorPrinter!!.addText(textFormat, "Term Capa:E0F0CB\n")
        vectorPrinter!!.addText(textFormat, "持卡人签名(CARDHOLDER SIGNATURE):\n")
        vectorPrinter!!.addText(textFormat, "\n")
        vectorPrinter!!.addText(textFormat, "\n")
        vectorPrinter!!.addText(textFormat, "\n")
        vectorPrinter!!.addText(textFormat, "----------------------------------------------\n")
        vectorPrinter!!.addText(textFormat, "本人确认以上交易\n")
        vectorPrinter!!.addText(textFormat, "同意将其计入本卡账户\n")
        vectorPrinter!!.addText(
            textFormat,
            "I ACKNOWLEDGE SATISFACTORY RECEIPT OF RELATIVE GOODS/SERVICE\n"
        )
        vectorPrinter!!.addText(textFormat, "服务热线：1212312312\n")
        vectorPrinter!!.addText(textFormat, "LANDI_APOS A8\n")
        vectorPrinter!!.addQrCode(null, "www.landicorp.com\n", null)
        //        vectorPrinter.addBarCode(Alignment.CENTER, 320, 48, "1234567123");
        vectorPrinter!!.feedPix(50)
    }

    @SuppressLint("SimpleDateFormat")
    fun startPrinting(
        batchTable: TempBatchFileDataTable, copyType: EPrintCopyType,
        context: Context?, isReversal: Boolean = false,
        printerCallback: (Boolean, Int) -> Unit
    ) {
        try {

            //-------------
            /*val textFormat = Bundle()
            textFormat.putInt(VectorPrinterData.ALIGNMENT, AlignMode.LEFT)
            textFormat.putInt(VectorPrinterData.TEXT_SIZE, TextSize.SMALL)

            vectorPrinter!!.addText(textFormat, "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz")
            vectorPrinter!!.addText(textFormat, "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ")
            vectorPrinter!!.addText(textFormat, "12345678901234567890123456789012345678901234567890123456789012345678901234567890")*/
            //------------------

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

                    textBlockList.clear()
                    textBlockList.put(AlignMode.LEFT, "DATE:${batchTable.transactionDate}")
                    textBlockList.put(AlignMode.RIGHT, "TIME:${formattedTime}")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()

                } catch (e: ParseException) {
                    e.printStackTrace()
                }


                textBlockList.put(AlignMode.LEFT, "MID:${hostMID}")
                textBlockList.put(AlignMode.RIGHT, "TID:${hostTID}")
                mixStyleTextPrint(textBlockList)
                textBlockList.clear()

                textBlockList.put(AlignMode.LEFT,"BATCH NO:${hostBatchNumber}")
                textBlockList.put(AlignMode.RIGHT, "ROC:${hostRoc}")//
                mixStyleTextPrint(textBlockList)
                textBlockList.clear()

                textBlockList.put(AlignMode.LEFT,"INVOICE:${hostInvoice}")
                textBlockList.put(AlignMode.RIGHT, " ")//
                mixStyleTextPrint(textBlockList)
                textBlockList.clear()

                //sigleLineText("INVOICE:${hostInvoice}", AlignMode.LEFT)
//                mixStyleTextPrint(textBlockList)
//                textBlockList.clear()
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
                        textBlockList.put(AlignMode.LEFT, "TID:${batchTable.authTID}")
                        mixStyleTextPrint(textBlockList)
                        textBlockList.clear()
                    }

                    textBlockList.put(AlignMode.LEFT, "BATCH NO:${paddingInvoiceRoc(batchTable.authBatchNO) }")
                    textBlockList.put(AlignMode.RIGHT, "ROC:${paddingInvoiceRoc(batchTable.authROC)}")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()

                    printSeperator()
                }
                val isNoEmiOnlyCashBackAppl : Boolean =  batchTable?.tenure=="1"

                if (isReversal) {
                    sigleLineText("TRANSACTION FAILED", AlignMode.CENTER, TextSize.NORMAL,isBold = false)
                } else {
                    if(isNoEmiOnlyCashBackAppl) {
                        sigleLineText("SALE", AlignMode.CENTER, TextSize.NORMAL, false)
                    }
                    else{
                        getTransactionTypeName(batchTable.transactionType)?.let { sigleLineText(it, AlignMode.CENTER, TextSize.NORMAL, false) }
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
                        textBlockList.put(AlignMode.LEFT, "CARD TYPE:${hostCardType}")
                        textBlockList.put(AlignMode.RIGHT, "EXP:XX/XX")

                        mixStyleTextPrint(textBlockList)

                        textBlockList.clear()
                    }
                }



                if(batchTable?.transactionType != BhTransactionType.VOID_PREAUTH.type) {
                    textBlockList.put(AlignMode.LEFT, "CARD NO:${batchTable.cardNumber}")
                    textBlockList.put(AlignMode.RIGHT, batchTable.operationType)
                    mixStyleTextPrint(textBlockList)

                    textBlockList.clear()

                }




                if(!isReversal) {
                    if(batchTable.merchantMobileNumber.isNotEmpty()){
                        textBlockList.put( AlignMode.LEFT, "MOBILE NO:${batchTable.merchantMobileNumber}")
                        textBlockList.put( AlignMode.RIGHT, " ")

                        textBlockList.clear()
                    }


                    if(batchTable?.transactionType != BhTransactionType.VOID_PREAUTH.type) {
                        textBlockList.put( AlignMode.LEFT, "AUTH CODE:${batchTable.authCode}")

                        textBlockList.put( AlignMode.RIGHT, "RRN:${batchTable.referenceNumber}")
                        mixStyleTextPrint(textBlockList)

                        textBlockList.clear()

                    }

                }

                if(batchTable.transactionType == BhTransactionType.VOID_PREAUTH.type)
                {
                    textBlockList.put(AlignMode.LEFT, "CARD NO:${batchTable.cardNumber}")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()

                    textBlockList.put(AlignMode.LEFT, "RRN:${batchTable.referenceNumber}")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()

                }

                if (!(batchTable.tvr.isNullOrEmpty()) )
                    textBlockList.put(AlignMode.LEFT, "TVR:${batchTable.tvr}")
                if (!(batchTable.tsi.isNullOrEmpty()) )
                    textBlockList.put(AlignMode.RIGHT, "TSI:${batchTable.tsi}")
                if(!(batchTable.tvr.isNullOrEmpty()) || !(batchTable.tsi.isNullOrEmpty()))
                    mixStyleTextPrint(textBlockList)
                textBlockList.clear()
                if (!(batchTable.aid.isNullOrEmpty()))
                    textBlockList.put(AlignMode.LEFT, "AID:${batchTable.aid}")
                if (!(batchTable.tc.isNullOrEmpty()))
                    textBlockList.put(AlignMode.RIGHT, "TC:${batchTable.tc}")
                if(!(batchTable.tc.isNullOrEmpty()) || !(batchTable.aid.isNullOrEmpty()))
                    mixStyleTextPrint(textBlockList)
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
                        textBlockList.put(AlignMode.LEFT, "TOTAL AMOUNT:")
                        textBlockList.put(AlignMode.RIGHT, "INR:${"%.2f".format(amt.toDouble())}")
                        mixStyleTextPrint(textBlockList)
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
                if (batchTable.transactionType == BhTransactionType.EMI_SALE.type ||
                    batchTable.transactionType == BhTransactionType.TEST_EMI.type||
                    batchTable.transactionType == BhTransactionType.BRAND_EMI.type) {
                    if(!isNoEmiOnlyCashBackApplied!!)
                      printBrandTnC(batchTable)

                }
                //region=====================BRAND PRODUACT DATA===============
                if (batchTable.transactionType == BhTransactionType.BRAND_EMI.type) {
                    printProduactData(batchTable)
                    printSeperator()
                    baseAmounthandling(batchTable)
                    nextLine()
                }

                if(isReversal){
                    sigleLineText("Please contact your card issuer for reversal of debit if any\n", AlignMode.CENTER)
                    sigleLineText(footerText[1], AlignMode.CENTER)
                }else {

                    if (batchTable.operationType.equals("CLESS_EMV")) {
                        if (batchTable.isPinverified == true) {
                            sigleLineText("PIN VERIFIED OK", AlignMode.CENTER, TextSize.NORMAL)
                        }
                        if (batchTable.isPinverified == true) {
                            sigleLineText("SIGNATURE NOT REQUIRED", AlignMode.CENTER, TextSize.NORMAL)
                        } else {
                            if (batchTable.transactionType == BhTransactionType.EMI_SALE.type ||
                                batchTable.transactionType == BhTransactionType.TEST_EMI.type ||
                                batchTable.transactionType == BhTransactionType.BRAND_EMI.type ||
                                batchTable.transactionType == BhTransactionType.SALE.type ||
                                batchTable.transactionType == BhTransactionType.CASH_AT_POS.type ||
                                batchTable.transactionType == BhTransactionType.SALE_WITH_CASH.type ||
                                batchTable.transactionType == BhTransactionType.PRE_AUTH.type) {
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
                                    sigleLineText("PIN VERIFIED OK", AlignMode.CENTER, TextSize.NORMAL)
                                }
                                if (batchTable.isPinverified) {
                                    sigleLineText("SIGNATURE NOT REQUIRED", AlignMode.CENTER, TextSize.NORMAL)
                                }
                            }
                        }
                    } else {
                        if (batchTable.isPinverified) {
                            sigleLineText("PIN VERIFIED OK", AlignMode.CENTER, TextSize.NORMAL)
                        }

                        if (batchTable.isPinverified) {
                            sigleLineText("SIGNATURE NOT REQUIRED", AlignMode.CENTER, TextSize.NORMAL)
                        } else {
                            sigleLineText("SIGN ...................", AlignMode.CENTER, TextSize.SMALL)
                            sigleLineText("", AlignMode.CENTER,TextSize.TINY)
                        }


                        Log.e("HolderName",""+batchTable.cardHolderName)
                        batchTable.cardHolderName?.let { sigleLineText(it.trim(), AlignMode.CENTER, TextSize.NORMAL) }
                    }

                    try {
                        val issuerParameterTable =
                            Field48ResponseTimestamp.getIssuerData(AppPreference.WALLET_ISSUER_ID)

                        val dec = issuerParameterTable?.walletIssuerDisclaimer

                        logger("dec", dec ?: "")
                        /*textBlockList.put(AlignMode.CENTER, dec ?: "")

                        mixStyleTextPrint(textBlockList)
                        textBlockList.clear()*/
                        sigleLineText(dec?:"",AlignMode.CENTER)

                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }

                    sigleLineText("", AlignMode.CENTER,TextSize.TINY)
                    sigleLineText(copyType.pName, AlignMode.CENTER)
                    sigleLineText("", AlignMode.CENTER,TextSize.TINY)
                    sigleLineText(footerText[0], AlignMode.CENTER)
                    sigleLineText(footerText[1], AlignMode.CENTER)
                }

                printLogo("BH.bmp")

                sigleLineText(
                    "App Version :${BuildConfig.VERSION_NAME}",
                    AlignMode.CENTER
                )

                if (batchTable.transactionType == BhTransactionType.EMI_SALE.type ||
                    batchTable.transactionType == BhTransactionType.TEST_EMI.type||
                    batchTable.transactionType == BhTransactionType.BRAND_EMI.type) {
                    if(!isNoEmiOnlyCashBackAppl) {
                        val issuerId = batchTable?.issuerId
                        val issuerTAndCData = issuerId?.let {
                            Field48ResponseTimestamp.getIssuerTAndCDataByIssuerId(
                                it
                            )
                        }
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
                                            sigleLineText(emiTnc,AlignMode.LEFT)
                                            /*val chunks: List<String> = chunkTnC(emiTnc, limit)
                                            for (st in chunks) {
                                                logger("TNC", "st", "e")
                                                textBlockList.add(
                                                    sigleLineformat(
                                                        st, AlignMode.LEFT
                                                    )
                                                )
//                                                printer?.setAscScale(ASCScale.SC1x1)
//                                                printer?.setAscSize(ASCSize.DOT24x8)
//                                                printer?.addText( AlignMode.LEFT, st)
                                            }
                                            printer?.setAscSize(ASCSize.DOT24x12) */
                                        }
                                    }
                                } else {
                                    /*textBlockList.add(
                                        sigleLineformat(
                                            "# ${issuerTAndCData.footerTAndC}", AlignMode.LEFT
                                        )
                                    )
                                    printer?.setAscScale(ASCScale.SC1x1)
                                    printer?.setAscSize(ASCSize.DOT24x8) */
                                    vectorPrinter?.addText( textFormatGlobal, "# ${issuerTAndCData.footerTAndC}")
                                }
                            }
                        }
                    }

                }
                //


            } catch (e: ParseException) {
                e.printStackTrace()
            }
            /*printer?.setPrnGray(3)
            printer?.feedLine(4)
            printer?.startPrint(object : com.usdk.apiservice.aidl.printer.OnPrintListener.Stub() {
                @Throws(RemoteException::class)
                override fun onFinish() {
                    printerCallback(true, 0)
                }

                @Throws(RemoteException::class)
                override fun onError(error: Int) {
                    printerCallback(true, 0)
                }
            })*/

            vectorPrinter!!.startPrint(object : OnPrintListener.Stub() {
                @Throws(RemoteException::class)
                override fun onFinish() {
                    // outputText("=> onFinish | sheetNo = $curSheetNo")
                   // println("time cost = + " + (System.currentTimeMillis() - startTime))
                    println("0onFinish")
                    printerCallback(true, 0)
                }

                @Throws(RemoteException::class)
                override fun onStart() {
                    //  outputText("=> onStart | sheetNo = $curSheetNo")
                    println("0onStart")
                }

                @Throws(RemoteException::class)
                override fun onError(error: Int, errorMsg: String) {
                    //  outputRedText("=> onError: $errorMsg")
                    println("0onError")
                    printerCallback(true, 0)
                }
            })


        } catch (ex: DeadObjectException) {
            ex.printStackTrace()
        }


    }

    private fun printSeperator() {
        val textFormat = Bundle()
        textFormat.putInt(VectorPrinterData.ALIGNMENT, AlignMode.LEFT)
        textFormat.putInt(VectorPrinterData.TEXT_SIZE, TextSize.SMALL)
        textFormat.putBoolean(VectorPrinterData.BOLD, false)

        vectorPrinter?.addText(textFormat, "----------------------------------\n");
    }

    private fun headerPrinting(logo: String? = HDFC_LOGO) {

        val tpt = Field48ResponseTimestamp.getTptData()
        var hdfcTpt: HDFCTpt?
        runBlocking(Dispatchers.IO) {
            hdfcTpt = Field48ResponseTimestamp.getHDFCTptData()
        }

        val logo = if (logo == AMEX_BANK_CODE_SINGLE_DIGIT || logo == AppPreference.AMEX_BANK_CODE) {
            AMEX_LOGO
        } else {
            HDFC_LOGO
        }

        setLogoAndHeader(logo)

        if (logo == AMEX_BANK_CODE_SINGLE_DIGIT || logo == AppPreference.AMEX_BANK_CODE) {
            tpt?.receiptHeaderOne?.let {
                sigleLineText(it ?: "".trim(), AlignMode.CENTER, TextSize.NORMAL, false)
            }
            tpt?.receiptHeaderTwo?.let { sigleLineText(it ?: "".trim(), AlignMode.CENTER, TextSize.NORMAL, false) }
            tpt?.receiptHeaderThree?.let { sigleLineText(it ?: "".trim(), AlignMode.CENTER, TextSize.NORMAL, false) }
        } else {
            if (null != hdfcTpt && hdfcTpt?.defaultMerchantName?.isNotBlank() ?: false && hdfcTpt?.defaultMerchantName?.isNotEmpty() ?: false) {
                hdfcTpt?.defaultMerchantName?.trim()
                    ?.let { sigleLineText(it ?: "".trim(), AlignMode.CENTER, TextSize.NORMAL, false) }
            } else {
                tpt?.receiptHeaderOne?.let { sigleLineText(it ?: "".trim(), AlignMode.CENTER, TextSize.NORMAL, false) }
            }
            if (null != hdfcTpt && hdfcTpt?.receiptL2?.isNotBlank() ?: false && hdfcTpt?.receiptL2?.isNotEmpty() ?: false) {
                hdfcTpt?.receiptL2?.let { sigleLineText(it ?: "".trim(), AlignMode.CENTER, TextSize.NORMAL, false) }
            } else {
                tpt?.receiptHeaderTwo?.let { sigleLineText(it ?: "".trim(), AlignMode.CENTER, TextSize.NORMAL, false) }
            }
            if (null != hdfcTpt && hdfcTpt?.receiptL3?.isNotBlank() ?: false && hdfcTpt?.receiptL3?.isNotEmpty() ?: false) {
                hdfcTpt?.receiptL3?.let { sigleLineText(it ?: "".trim(), AlignMode.CENTER, TextSize.NORMAL, false) }
            } else {
                tpt?.receiptHeaderThree?.let { sigleLineText(it ?: "".trim(), AlignMode.CENTER, TextSize.NORMAL, false) }
            }
        }

    }

    private fun setLogoAndHeader(logo: String = HDFC_LOGO) {

       // printSeperator()
        try {
            val bitmap = BitmapFactory.decodeStream(context?.assets?.open(logo))
            vectorPrinter!!.addImage(null, bitmap)
        } catch (e: IOException) {
            e.printStackTrace()
        }
       // printSeperator()

    }

    private fun printLogo(logo: String = HDFC_LOGO) {

        try {
           // printSeperator()
            val bitmap = BitmapFactory.decodeStream(context?.assets?.open(logo))
            vectorPrinter!!.addImage(null, bitmap)
           // printSeperator()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // using this you can set your single text
    private fun sigleLineText(text: String, alignMode: Int, txtSize:Int = TextSize.SMALL, isBold: Boolean = false) {
        val textFormat = Bundle()
        textFormat.putInt(VectorPrinterData.ALIGNMENT, alignMode)
        textFormat.putInt(VectorPrinterData.TEXT_SIZE, txtSize)
        textFormat.putFloat(VectorPrinterData.LETTER_SPACING, 0f)
        textFormat.putBoolean(VectorPrinterData.BOLD, isBold)

        vectorPrinter!!.addText(textFormat, text)
        vectorPrinter!!.addText(textFormat, "\n")
    }

    private fun nextLine() {
        val textFormat = Bundle()
        textFormat.putInt(VectorPrinterData.ALIGNMENT, AlignMode.CENTER)
        textFormat.putInt(VectorPrinterData.TEXT_SIZE, TextSize.TINY)
        textFormat.putFloat(VectorPrinterData.LETTER_SPACING, 0f)
        textFormat.putBoolean(VectorPrinterData.BOLD, false)

        vectorPrinter!!.addText(textFormat, "\n")
    }

    private fun mixStyleTextPrint(data:HashMap<Int, String>,textSize:Int = TextSize.SMALL,isBold:Boolean = false){
        /*int[] weights = {3, 3, 3, 3};
        int[] aligns = {Alignment.NORMAL, Alignment.OPPOSITE, Alignment.NORMAL, Alignment.OPPOSITE};
        vectorPrinter.addTextColumns(null,
            new String[]{"凭证号:", "002134", "授权码", "118525"}, weights, aligns);
        AlignMode.LEFT*/

        val textFormat = Bundle()
        textFormat.putInt(VectorPrinterData.TEXT_SIZE, textSize)
        textFormat.putFloat(VectorPrinterData.LETTER_SPACING, 0f)
        textFormat.putBoolean(VectorPrinterData.BOLD, isBold)

        /*val weights : IntArray
        if(data.get(AlignMode.CENTER) == null){
            weights = intArrayOf(5, 5, 5)
        }else{
            weights = intArrayOf(5, 5, 5)
        }*/

        if(data.get(AlignMode.CENTER) == null){
            var leftW = (data.get(AlignMode.LEFT)?:"".trim()).length
            var rightW = (data.get(AlignMode.RIGHT)?:"".trim()).length

            if(leftW+rightW >34){
                sigleLineText(data.get(AlignMode.LEFT)?:"".trim(), AlignMode.LEFT)
                sigleLineText(data.get(AlignMode.RIGHT)?:"".trim(), AlignMode.RIGHT)
            }else{
                // val  weights = intArrayOf(1,1)
                if(leftW == 0){
                    leftW = 1
                }

                if(rightW == 0){
                    rightW = 1
                }
                val  weights = intArrayOf(leftW,rightW)
                val aligns = intArrayOf(AlignMode.LEFT, AlignMode.RIGHT)
                vectorPrinter!!.addTextColumns(
                    textFormat,
                    arrayOf(data.get(AlignMode.LEFT)?:" ", data.get(AlignMode.RIGHT)?:" "),
                    weights,
                    aligns
                )
            }


        }else{
            val weights = intArrayOf((data.get(AlignMode.LEFT)?:"".trim()).length, (data.get(AlignMode.CENTER)?:"".trim()).length, (data.get(AlignMode.RIGHT)?:"".trim()).length)
            //val weights = intArrayOf(14,4,16)
            val aligns = intArrayOf(AlignMode.LEFT, AlignMode.CENTER, AlignMode.RIGHT)
            vectorPrinter!!.addTextColumns(
                textFormat,
                arrayOf(data.get(AlignMode.LEFT)?:"",data.get(AlignMode.CENTER)?:"", data.get(AlignMode.RIGHT)?:""),
                weights,
                aligns
            )
        }

    }

    private fun printToalTxnList(data:HashMap<Int, String>,textSize:Int = TextSize.SMALL,isBold:Boolean = false){

        val textFormat = Bundle()
        textFormat.putInt(VectorPrinterData.TEXT_SIZE, textSize)
        textFormat.putFloat(VectorPrinterData.LETTER_SPACING, 0f)
        textFormat.putBoolean(VectorPrinterData.BOLD, isBold)

        //val weights = intArrayOf((data.get(AlignMode.LEFT)?:"".trim()).length, (data.get(AlignMode.CENTER)?:"".trim()).length, (data.get(AlignMode.RIGHT)?:"".trim()).length)
        val weights = intArrayOf(14,9,16)
        val aligns = intArrayOf(AlignMode.LEFT, AlignMode.CENTER, AlignMode.RIGHT)
        vectorPrinter!!.addTextColumns(
                textFormat,
                arrayOf(data.get(AlignMode.LEFT)?:"".trim(),data.get(AlignMode.CENTER)?:"".trim(), data.get(AlignMode.RIGHT)?:"".trim()),
                weights,
                aligns
            )

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

        nextLine()

        if (batchTable.transactionType == BhTransactionType.SALE.type) {
            textBlockList.put(AlignMode.LEFT, "SALE AMOUNT:")
            textBlockList.put(AlignMode.RIGHT, "$currencySymbol :${"%.2f".format(amt.toDouble())}")
            mixStyleTextPrint(textBlockList,TextSize.SMALL, true)
            textBlockList.clear()
            /*sigleLineText("SALE AMOUNT",AlignMode.CENTER,TextSize.NORMAL, true)
            sigleLineText("$currencySymbol :${"%.2f".format(amt.toDouble())}",AlignMode.CENTER,TextSize.NORMAL, true)*/
//            if (tipAmount != "0") {
//                tipAmount = (((receiptDetail.tipAmmount)?.toDouble())?.div(100)).toString()
//                textBlockList.put(AlignMode.LEFT, "TIP AMOUNT:")
//                textBlockList.put(AlignMode.RIGHT, "%.2f".format(tipAmount.toDouble()))
//                mixStyleTextPrint(textBlockList,TextSize.NORMAL, false)
//                textBlockList.clear()
//            }
//            totalAmount = "%.2f".format((amt.toDouble() ))
        } else {
            if(batchTable.transactionType == BhTransactionType.SALE_WITH_CASH.type){ // kushal
//                val amt1=(((receiptDetail.totalAmmount)?.toLong())?.div(100))
//                val otherAmt1=(((receiptDetail.otherAmount)?.toLong())?.div(100))
//                val saleAmount= otherAmt1?.let { amt1?.minus(it) }
//                textBlockList.add(sigleLineformat("SALE AMOUNT:", AlignMode.LEFT))
//                textBlockList.add(sigleLineformat("$currencySymbol :${"%.2f".format(saleAmount?.toDouble())}", AlignMode.RIGHT))
//                printer?.addMixStyleText(textBlockList)
//                textBlockList.clear()

                textBlockList.put(AlignMode.LEFT, "CASH WITHDRAWN AMT: ")
                textBlockList.put(AlignMode.RIGHT, "$currencySymbol:${"%.2f".format(tipAmount?.toDouble())}",)
                mixStyleTextPrint(textBlockList,TextSize.SMALL, true)
                textBlockList.clear()
                totalAmount = "%.2f".format((amt.toDouble())).toString()
            }else{
                textBlockList.put(AlignMode.LEFT, "CASH WITHDRAWN AMT:")
                textBlockList.put(AlignMode.RIGHT, "$currencySymbol :${"%.2f".format(amt.toDouble())}")
                mixStyleTextPrint(textBlockList,TextSize.SMALL, true)
                textBlockList.clear()
                totalAmount = "%.2f".format((amt.toDouble()))
            }

        }

        nextLine()

        // textBlockList.add(sigleLineformat( "00",AlignMode.RIGHT))


//        textBlockList.put(AlignMode.LEFT, "TOTAL AMOUNT:")
//        textBlockList.put(AlignMode.RIGHT, "$currencySymbol :${totalAmount}")
//        mixStyleTextPrint(textBlockList,TextSize.NORMAL, false)
//        textBlockList.clear()
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
            textBlockList.put(AlignMode.LEFT, "TXN AMOUNT")

            //   val txnAmount = (((bankEMITenureDataModal?.transactionAmount)?.toLong())?.div(100)).toString()
            var txnAmount = (((batchTable.emiTransactionAmount).toDouble()).div(100)).toString()

            logger("txnAmount", "" + txnAmount)
            textBlockList.put(AlignMode.RIGHT, "$currencySymbol:${"%.2f".format(txnAmount.toDoubleOrNull())}")
            mixStyleTextPrint(textBlockList)
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

                    textBlockList.put(AlignMode.LEFT, "INSTA DISCOUNT")
                    val authAmount =
                        (((batchTable.transactionAmt)?.toLong())?.div(100)).toString()
                    textBlockList.put(AlignMode.RIGHT, "$currencySymbol:${instantDis}")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()

                }
            }
            textBlockList.put(AlignMode.LEFT, "AUTH AMOUNT")
            if (batchTable.transactionType == BhTransactionType.TEST_EMI.type) {

                textBlockList.put(AlignMode.RIGHT ,
                        "$currencySymbol:${1.00}")
            } else {
                val authAmount =
                    (((batchTable?.transactionAmt)?.toDouble())?.div(100)).toString()
                textBlockList.put(AlignMode.RIGHT,
                        "$currencySymbol:${"%.2f".format(authAmount.toDouble())}")
            }
            mixStyleTextPrint(textBlockList)
            textBlockList.clear()

            textBlockList.put(AlignMode.LEFT, "CARD ISSUER")
            if (batchTable.transactionType == BhTransactionType.TEST_EMI.type) {


                textBlockList.put(AlignMode.RIGHT, " TEST ISSUER")
            } else {
                if (batchTable != null) {
                    textBlockList.put(AlignMode.RIGHT, batchTable.issuerName)
                }
            }
            mixStyleTextPrint(textBlockList)
            textBlockList.clear()
            val tenureDuration = "${batchTable.tenure} Months"
            val tenureHeadingDuration = "${batchTable.tenure} Months Scheme"
            var roi = batchTable.roi.toInt().let { divideAmountBy100(it).toString() }
            var loanamt = batchTable.loanAmt.toInt().let { divideAmountBy100(it).toString() }
            roi = "%.2f".format(roi.toDouble()) + " %"
            loanamt = "%.2f".format(loanamt.toDouble())
            textBlockList.put(AlignMode.LEFT, "ROI(pa)")
            textBlockList.put(AlignMode.RIGHT, roi)
            mixStyleTextPrint(textBlockList)
            textBlockList.clear()

            textBlockList.put(AlignMode.LEFT, "TENURE")
            textBlockList.put(AlignMode.RIGHT, tenureDuration)
            mixStyleTextPrint(textBlockList)
            textBlockList.clear()
            //region===============Processing Fee Changes And Showing On ChargeSlip:-
            if (!TextUtils.isEmpty(batchTable.processingFee)) {
                if ((batchTable.processingFee) != "0") {
                    val procFee = "%.2f".format(
                        (((batchTable.processingFee)?.toDouble())?.div(100)).toString()
                            .toDouble()
                    )
                    textBlockList.put(AlignMode.LEFT, "PROC-FEE")
                    textBlockList.put(AlignMode.RIGHT, "$currencySymbol $procFee")
                    mixStyleTextPrint(textBlockList)
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

                    textBlockList.put(AlignMode.LEFT, "PROC-FEE")
                    textBlockList.put(AlignMode.RIGHT, "$currencySymbol $procFeeAmount")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()

                }
            }
            if (batchTable != null) {
                if (!TextUtils.isEmpty(batchTable.totalProcessingFee)) {
                    if (!(batchTable.totalProcessingFee).equals("0")) {
                        val totalProcFeeAmount =
                            "%.2f".format(batchTable.totalProcessingFee.toFloat() / 100)

                        textBlockList.put(AlignMode.LEFT, "PROC-FEE AMOUNT")
                        textBlockList.put(AlignMode.RIGHT, "$currencySymbol $totalProcFeeAmount")
                        mixStyleTextPrint(textBlockList)
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
                    textBlockList.put( AlignMode.LEFT, cashBackPercentHeadingText)
                    textBlockList.put(AlignMode.RIGHT, "$currencySymbol ${batchTable?.cashBackCalculatedValue}")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()
                } else {
                    textBlockList.put(AlignMode.LEFT, cashBackPercentHeadingText)
                    textBlockList.put(AlignMode.RIGHT, "$currencySymbol ${batchTable?.cashBackCalculatedValue}")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()
                }
            }

            if (!TextUtils.isEmpty(batchTable?.cashback) && batchTable?.cashback != "0") {
                val cashBackAmount = "%.2f".format(
                    batchTable?.cashback?.toFloat()
                        ?.div(100)
                )

                if (islongTextHeading) {
                    textBlockList.put(AlignMode.LEFT, cashBackAmountHeadingText)
                    textBlockList.put(AlignMode.RIGHT, " ")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()

                    textBlockList.put(AlignMode.LEFT, nextLineAppendStr)
                    textBlockList.put(AlignMode.RIGHT, "$currencySymbol $cashBackAmount")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()
                } else {
                    println("test-->${batchTable?.cashback}")
                    if (batchTable?.cashback != "0" && !(batchTable?.cashback.isNullOrEmpty())) {
                        val cashBackAmount = "%.2f".format(
                            batchTable?.cashback?.toFloat()
                                ?.div(100)
                        )


                        textBlockList.put(AlignMode.LEFT, cashBackAmountHeadingText)
                        textBlockList.put(AlignMode.RIGHT, "$currencySymbol $cashBackAmount")
                        mixStyleTextPrint(textBlockList)
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

                    textBlockList.put( AlignMode.LEFT, cashBackPercentHeadingText)
                    textBlockList.put(AlignMode.RIGHT , "$currencySymbol ${batchTable?.discountCalculatedValue}")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()

                } else {
                    textBlockList.put(AlignMode.LEFT, discountPercentHeadingText)
                    textBlockList.put(AlignMode.RIGHT, "$currencySymbol ${batchTable?.discountCalculatedValue}")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()
                }
            }
            if (!(batchTable?.cashDiscountAmt.isNullOrEmpty()) && batchTable?.cashDiscountAmt != "0") {
                val discAmount =
                    "%.2f".format(batchTable?.cashDiscountAmt?.toFloat()?.div(100))

                if (islongTextHeading) {

                    textBlockList.put( AlignMode.LEFT, discountAmountHeadingText)
                    textBlockList.put(AlignMode.CENTER, "")
                    textBlockList.put(AlignMode.RIGHT, "")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()

                    textBlockList.put(AlignMode.LEFT, nextLineAppendStr)
                    textBlockList.put(AlignMode.RIGHT, "$currencySymbol ${discAmount}")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()


                } else {

                    textBlockList.put(AlignMode.LEFT, discountAmountHeadingText)
                    textBlockList.put(AlignMode.RIGHT, "$currencySymbol ${discAmount}")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()

                }
            }






            textBlockList.put( AlignMode.LEFT, "LOAN AMOUNT")
            textBlockList.put(AlignMode.RIGHT, "$currencySymbol:${loanamt}")
            mixStyleTextPrint(textBlockList)
            textBlockList.clear()

            textBlockList.put(AlignMode.LEFT, "MONTHLY EMI")
            if (!(batchTable?.monthlyEmi.isNullOrEmpty()) && batchTable?.monthlyEmi != "0") {
                var emiAmount =
                    "%.2f".format(batchTable?.monthlyEmi?.toFloat()?.div(100))
                textBlockList.put(AlignMode.RIGHT, "$currencySymbol:${emiAmount}")
                mixStyleTextPrint(textBlockList)
                textBlockList.clear()
            }
            textBlockList.put(AlignMode.LEFT, "TOTAL INTEREST")
            if (!(batchTable?.totalInterest.isNullOrEmpty()) && batchTable?.totalInterest != "0") {
                var totalInterestPay =
                    "%.2f".format(batchTable?.totalInterest?.toFloat()?.div(100))
                textBlockList.put(AlignMode.RIGHT, "$currencySymbol:${totalInterestPay}")
                mixStyleTextPrint(textBlockList)
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

                            textBlockList.put(AlignMode.LEFT, "$totalAmountHeadingText (incl Int)")
                            textBlockList.put(AlignMode.RIGHT, "$currencySymbol:${totalAmt.toString()}")
                            mixStyleTextPrint(textBlockList)
                            textBlockList.clear()


                        }
                        "55" -> {

                            textBlockList.put(AlignMode.LEFT, "$totalAmountHeadingText (PAYOUT)")

                            textBlockList.put(AlignMode.RIGHT,
                                    "$currencySymbol:${totalAmt.toString()}")
                            mixStyleTextPrint(textBlockList)
                            textBlockList.clear()
                        }
                        else -> {


                            textBlockList.put(AlignMode.LEFT,
                                    "$totalAmountHeadingText (With Int)")

                            textBlockList.put(AlignMode.RIGHT,
                                    "$currencySymbol:${totalAmt.toString()}")
                            mixStyleTextPrint(textBlockList)
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

                            textBlockList.put(AlignMode.LEFT,
                                    "$totalAmountHeadingText (incl Int)")
                            textBlockList.put( AlignMode.RIGHT,
                                    "$currencySymbol:${f_totalAmt}")
                            mixStyleTextPrint(textBlockList)
                            textBlockList.clear()

                        }
                        "55" -> {
                            textBlockList.put(AlignMode.LEFT,
                                    "$totalAmountHeadingText (PAYOUT)"
                            )

                            textBlockList.put(AlignMode.RIGHT,
                                    "$currencySymbol:${f_totalAmt}")
                            mixStyleTextPrint(textBlockList)
                            textBlockList.clear()

                        }
                        else -> {
                            textBlockList.put( AlignMode.LEFT,
                                    "$totalAmountHeadingText (With Int)")
                            textBlockList.put(
                                AlignMode.RIGHT,
                                    "$currencySymbol:${f_totalAmt}"
                            )
                            mixStyleTextPrint(textBlockList)
                            textBlockList.clear()

                        }

                    }
                }
            }

        }else{
            textBlockList.put(AlignMode.LEFT, "Scheme")
            if (batchTable != null) {
                textBlockList.put(AlignMode.RIGHT,
                        batchTable?.tenureLabel)
                mixStyleTextPrint(textBlockList)
                textBlockList.clear()
            }

            textBlockList.put(AlignMode.LEFT, "Card Issuer")
            if (batchTable != null) {
                textBlockList.put(AlignMode.RIGHT,
                        batchTable.issuerName)
            }
            mixStyleTextPrint(textBlockList)
            textBlockList.clear()
        }
    }

    private fun printBrandTnC(batchTable: TempBatchFileDataTable) {

        /*val brandEMIMasterDataModal: BrandEMIMasterDataModal? = batchTable.emiBrandData
        val bankEMITenureDataModal: BankEMITenureDataModal? = batchTable.emiTenureDataModel
        val bankEMIIssuerTAndCDataModal: BankEMIIssuerTAndCDataModal? = batchTable.emiIssuerDataModel*/
        val issuerId = batchTable?.issuerId
        var brandId = batchTable?.brandId

        val issuerTAndCData = issuerId?.let {
            Field48ResponseTimestamp.getIssuerTAndCDataByIssuerId(
                it
            )
        }
        val jsonRespp = Gson().toJson(issuerTAndCData)

        println(jsonRespp)

        logger("getting issuer tnc=",jsonRespp.toString(),"e")

        sigleLineText("CUSTOMER CONSENT FOR EMI", AlignMode.CENTER, TextSize.NORMAL)
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

                        sigleLineText(emiTnc,AlignMode.LEFT)
                        /*val chunks: List<String> = chunkTnC(emiTnc, limit)
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
                        printer?.setAscSize(ASCSize.DOT24x12)*/
                    }
                }
            }

        }
        //endregion


        if (batchTable.transactionType == BhTransactionType.BRAND_EMI.type) {
            //region ======================Brand terms and Condition=========================

            //val brandId = brandEMIMasterDataModal?.brandID
            val data = Field48ResponseTimestamp.getBrandTAndCData()
            val jsonResp = Gson().toJson(data)


            logger("size=",data?.size.toString(),"e")
            logger("getting=",data.toString(),"e")
            println(jsonResp)
            if (brandId != null) {
                val brandTnc = Field48ResponseTimestamp.getBrandTAndCDataByBrandId(brandId)
                logger("Brand Tnc", brandTnc, "e")
                sigleLineText(brandTnc,AlignMode.LEFT)
                /*val chunk: List<String> = chunkTnC(brandTnc,48)
                printer?.setAscScale(ASCScale.SC1x1)
                printer?.setAscSize(ASCSize.DOT24x8)
                for (st in chunk) {
                    logger("Brand Tnc", st, "e")
                    textBlockList.add(
                        sigleLineformat(
                            st.replace(bankEMIFooterTAndCSeparator, "")
                                .replace(PrintUtil.disclaimerIssuerClose, ""), AlignMode.LEFT
                        )
                    )
                    printer?.addMixStyleText(textBlockList)
                    textBlockList.clear()

                }

                // reset printer font
                printer?.setAscSize(ASCSize.DOT24x12)*/
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
                        sigleLineText(emiTnc,AlignMode.LEFT)
                        /*val chunks: List<String> = chunkTnC(emiTnc, limit)
                        printer?.setAscScale(ASCScale.SC1x1)
                        printer?.setAscSize(ASCSize.DOT24x8)
                        for (st in chunks) {
                            logger("emiCustomerConsent", st, "e")

                            textBlockList.add(
                                sigleLineformat(st.replace(bankEMIFooterTAndCSeparator, "")
                                    .replace(PrintUtil.disclaimerIssuerClose, ""), AlignMode.LEFT
                                )
                            )
                            printer?.addMixStyleText(textBlockList)
                            textBlockList.clear()
                        }*/
                    }

                }
            } else {
                printSeperator()
                textBlockList.put(AlignMode.LEFT, "Scheme:")
                if (batchTable != null) {
                    textBlockList.put(AlignMode.RIGHT, batchTable.tenureLabel)
                }
                mixStyleTextPrint(textBlockList)
                textBlockList.clear()

                textBlockList.put(AlignMode.LEFT, "Card Issuer:")
                if (batchTable != null) {
                    textBlockList.put(
                        AlignMode.RIGHT, batchTable.issuerName
                    )
                }
                mixStyleTextPrint(textBlockList)
                textBlockList.clear()

            }
//endregion

            //region=====================Printing Merchant Brand Purchase Details:-
            /*if (batchTable.BhTransactionType.equals(EDashboardItem.BRAND_EMI.title)) {
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
        if (batchTable.transactionType != BhTransactionType.BRAND_EMI.type ) {
            baseAmounthandling(batchTable)
            nextLine()
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

            textBlockList.put(AlignMode.LEFT, "BASE AMOUNT:")

            textBlockList.put(
                AlignMode.RIGHT, "$currencySymbol:${"%.2f".format(baseAmount.toDoubleOrNull())}")
            mixStyleTextPrint(textBlockList, TextSize.SMALL, true)
            textBlockList.clear()

        }
    }

    private fun printProduactData(batchTable: TempBatchFileDataTable){

//        val brandEMIMasterDataModal: BrandEMIMasterDataModal? = batchTable.emiBrandData
//        val brandEMISubCategoryTable: BrandEMISubCategoryTable? = batchTable.emiSubCategoryData
//        val brandEMICategoryData: BrandEMISubCategoryTable? = batchTable.emiCategoryData
//        val brandEMIProductDataModal: BrandEMIProductDataModal? = batchTable.emiProductData
//        val bankEMITenureDataModal: BankEMITenureDataModal? = batchTable.emiTenureDataModel
//        val bankEMIIssuerTAndCDataModal: BankEMIIssuerTAndCDataModal? = batchTable.emiIssuerDataModel

        val issuerId = batchTable?.issuerId
        sigleLineText("-----**Product Details**-----", AlignMode.CENTER, TextSize.SMALL, true)
        if (batchTable.brandEMIDataModal != null) {
            textBlockList.put(AlignMode.LEFT, "Mer/Mfr Name:")
            textBlockList.put(AlignMode.RIGHT, "${batchTable.brandEMIDataModal?.brandName}")
            mixStyleTextPrint(textBlockList)
            textBlockList.clear()

            textBlockList.put(AlignMode.LEFT, "Prod Cat:")
            textBlockList.put(AlignMode.RIGHT, "${batchTable.brandEMIDataModal?.categoryName}")
            mixStyleTextPrint(textBlockList)
            textBlockList.clear()


            if (batchTable.brandEMIDataModal?.producatDesc == "subCat") {
                if (!batchTable.brandEMIDataModal?.productCategoryName.isNullOrEmpty()) {

                    textBlockList.put( AlignMode.LEFT, "Prod desc:")
                    textBlockList.put(AlignMode.RIGHT, "${batchTable.brandEMIDataModal?.productCategoryName}")
                    mixStyleTextPrint(textBlockList)
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


            textBlockList.put(AlignMode.LEFT, "Prod Name:")
            textBlockList.put(AlignMode.RIGHT, "${batchTable.brandEMIDataModal?.productName}")
            mixStyleTextPrint(textBlockList)
            textBlockList.clear()

            if (!TextUtils.isEmpty(batchTable.brandEMIDataModal?.imeiORserailNum)) {
                textBlockList.put(AlignMode.LEFT, "Prod ${"IEMI"}:")
                textBlockList.put( AlignMode.RIGHT, "${batchTable.brandEMIDataModal?.imeiORserailNum}")
                mixStyleTextPrint(textBlockList)
                textBlockList.clear()

            }
            val mobnum=batchTable.merchantMobileNumber?:""
            if (!TextUtils.isEmpty(mobnum)) {
                when (batchTable.brandEMIDataModal.mobileNumberBillNumberFlag.substring(1, 2)) {
                    "1" -> {
                        // MASK PRINT
                        val maskedMob = Field48ResponseTimestamp.panMasking(
                            mobnum,
                            "000****000"
                        )
                        textBlockList.put(AlignMode.LEFT, "Mobile No:")
                        textBlockList.put(AlignMode.RIGHT, maskedMob)
                        mixStyleTextPrint(textBlockList)
                        textBlockList.clear()


                    }
                    //PLAIN PRINT
                    "2" -> {

                        textBlockList.put(AlignMode.LEFT, "Mobile No:")
                        textBlockList.put(AlignMode.RIGHT, mobnum)
                        mixStyleTextPrint(textBlockList)
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
                sigleLineText(tenureTAndC?:"", AlignMode.LEFT)
                /*val chunk: List<String>? = tenureTAndC?.let { chunkTnC(it,48) }
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
                printer?.setAscSize(ASCSize.DOT24x8)*/
            }
            //endregion





        }

    }

    private fun voidTransaction(receiptDetail: TempBatchFileDataTable) {
        var currencySymbol: String? = "Rs"
        val terminalData = getTptData()
        currencySymbol = terminalData?.currencySymbol
        textBlockList.put(AlignMode.LEFT, "BASE AMOUNT:")
        val amt = (((receiptDetail.baseAmmount)?.toDouble())?.div(100)).toString()
        textBlockList.put(AlignMode.RIGHT, "$currencySymbol :${"%.2f".format(amt.toDouble())}")
        mixStyleTextPrint(textBlockList)
        textBlockList.clear()

        textBlockList.put( AlignMode.LEFT, "TOTAL AMOUNT:")
        textBlockList.put(AlignMode.RIGHT, "$currencySymbol :${"%.2f".format(amt.toDouble())}")
        mixStyleTextPrint(textBlockList)
        textBlockList.clear()
    }

    private fun preAuthCompleteTransaction(receiptDetail: TempBatchFileDataTable) {
        var currencySymbol: String? = "Rs"
        val terminalData = getTptData()
        currencySymbol = terminalData?.currencySymbol
        textBlockList.put(AlignMode.LEFT, "BASE AMOUNT:")
        val amt = (((receiptDetail.transactionalAmmount)?.toDouble())?.div(100)).toString()
        textBlockList.put(AlignMode.RIGHT, "$currencySymbol :${"%.2f".format(amt.toDouble())}")
        mixStyleTextPrint(textBlockList)
        textBlockList.clear()

        textBlockList.put(AlignMode.LEFT, "TOTAL AMOUNT:")
        textBlockList.put(AlignMode.RIGHT, "$currencySymbol :${"%.2f".format(amt.toDouble())}")
        mixStyleTextPrint(textBlockList)
        textBlockList.clear()
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
                val cardType = isoW.additionalData["cardType"] ?: ""

                try{
                    val bankID = batchdata.split("|").getOrNull(1)

                    if (hostBankID?.isNotBlank() == true) {
                        hostBankID
                    } else {
                        hostBankID =  bankID
                    }
                }catch (ex:Exception){
                    ex.printStackTrace()
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

                textBlockList.put(AlignMode.LEFT, "DATE:${date}")
                textBlockList.put(AlignMode.RIGHT, "TIME:${time}")
                mixStyleTextPrint(textBlockList)
                textBlockList.clear()

                textBlockList.put(AlignMode.LEFT, "MID:${hostMID}")
                textBlockList.put(AlignMode.RIGHT, "TID:${hostTID}")
                mixStyleTextPrint(textBlockList)
                textBlockList.clear()

                // alignLeftRightText(textInLineFormatBundle, "DATE:${date}", "TIME:${time}")
                // alignLeftRightText(textInLineFormatBundle, "MID:${hostMID}", "TID:${hostTID}")

                textBlockList.put(AlignMode.LEFT,"BATCH NO:${hostBatchNumber}")
                textBlockList.put(AlignMode.RIGHT, "ROC:${hostRoc}")
                mixStyleTextPrint(textBlockList)
                textBlockList.clear()

//                    alignLeftRightText(
//                        textInLineFormatBundle,
//                        "BATCH NO:${hostBatchNumber}",
//                        "ROC:${invoiceWithPadding(hostRoc)}"
//                    )

                // centerText(textFormatBundle, "TRANSACTION FAILED")
                sigleLineText("TRANSACTION FAILED", AlignMode.CENTER, TextSize.NORMAL)

                val card = isoW.additionalData["pan"] ?: ""
                if (card.isNotEmpty())
                {
                    textBlockList.put(AlignMode.LEFT, "CARD NO:${card}")
                    textBlockList.put(AlignMode.RIGHT, "${hostCardType}")
                    mixStyleTextPrint(textBlockList)
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
                    textBlockList.put(AlignMode.LEFT, "TVR:${tvr}")
                    textBlockList.put(AlignMode.RIGHT,"TSI:${tsi}")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()
                }


                if (aid.isNotEmpty()) {
                    aid = "AID:$aid"
                    //alignLeftRightText(textInLineFormatBundle, aid, "")
                    textBlockList.put(AlignMode.LEFT, aid)
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()
                }


//                    printSeperator(textFormatBundle)
//                    centerText(textFormatBundle, "TOTAL AMOUNT : ${getCurrencySymbol(TerminalParameterTable.selectFromSchemeTable())} $amountStr")
//                    printSeperator(textFormatBundle)

                printSeperator()
                textBlockList.put(AlignMode.LEFT, "TOTAL AMOUNT:")
                textBlockList.put(AlignMode.RIGHT, "INR:${amountStr}")
                mixStyleTextPrint(textBlockList)
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


                printLogo("BH.bmp")

                sigleLineText(
                    "App Version :${BuildConfig.VERSION_NAME}",
                    AlignMode.CENTER
                )



            }

        } catch (e: ParseException) {
            e.printStackTrace()
        }


        vectorPrinter!!.startPrint(object : OnPrintListener.Stub() {
            @Throws(RemoteException::class)
            override fun onFinish() {
                // outputText("=> onFinish | sheetNo = $curSheetNo")
                // println("time cost = + " + (System.currentTimeMillis() - startTime))
                println("0onFinish")
                callback("true")
            }

            @Throws(RemoteException::class)
            override fun onStart() {
                //  outputText("=> onStart | sheetNo = $curSheetNo")
                println("0onStart")
            }

            @Throws(RemoteException::class)
            override fun onError(error: Int, errorMsg: String) {
                //  outputRedText("=> onError: $errorMsg")
                println("0onError")
                callback("false")
            }
        })

    }

    private fun printPendingPreAuth(context: Context?, listPendingPreauthData:ArrayList<PendingPreauthData>, printerCallback: (Boolean, Int) -> Unit){

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

                    textBlockList.put(AlignMode.LEFT, "DATE:${date}")
                    textBlockList.put(AlignMode.RIGHT, "TIME:${time}")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()

                } catch (e: ParseException) {
                    e.printStackTrace()
                }


                textBlockList.put(AlignMode.LEFT, "MID:${tpt?.merchantId}")
                textBlockList.put(AlignMode.RIGHT , "TID:${item?.TID}")
                mixStyleTextPrint(textBlockList)
                textBlockList.clear()

                // getTransactionTypeName(batchTable.transactionType)?.let { sigleLineText(it, AlignMode.CENTER) }
                sigleLineText("PRE-AUTH TXN", AlignMode.CENTER)
                printSeperator()


                for(item in listPendingPreauthData){
                    textBlockList.put(AlignMode.LEFT ,
                            "BATCH NO:${invoiceWithPadding(item.batch.toString())}")
                    textBlockList.put(AlignMode.RIGHT, "ROC:${invoiceWithPadding(item.roc.toString())}")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()


                    textBlockList.put( AlignMode.LEFT,
                            "PAN:${item.pan}")
                    Log.e("item.amount",item.amount.toString())
                    //textBlockList.put("AMT:${item.amount}", AlignMode.RIGHT))
                    textBlockList.put(AlignMode.RIGHT, "AMT:${"%.2f".format(item.amount)}")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()


                    textBlockList.put( AlignMode.LEFT,
                            "DATE:${item.date}")
                    textBlockList.put(AlignMode.RIGHT, "TIME:${item.time}")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()

                    printSeperator()
                }

                sigleLineText(footerText[1], AlignMode.CENTER)
                /*val bhlogo: ByteArray? = context?.let { printLogo(it, "BH.bmp") }
                printer?.addBmpImage(0, FactorMode.BMP1X1, bhlogo)*/
                printLogo("BH.bmp")
                sigleLineText(
                    "App Version :${BuildConfig.VERSION_NAME}",
                    AlignMode.CENTER
                )

            }catch (ex:Exception){
                ex.printStackTrace()
            }


            vectorPrinter!!.startPrint(object : OnPrintListener.Stub() {
                @Throws(RemoteException::class)
                override fun onFinish() {
                    // outputText("=> onFinish | sheetNo = $curSheetNo")
                    // println("time cost = + " + (System.currentTimeMillis() - startTime))
                    println("0onFinish")
                    printerCallback(true, 0)
                }

                @Throws(RemoteException::class)
                override fun onStart() {
                    //  outputText("=> onStart | sheetNo = $curSheetNo")
                    println("0onStart")
                }

                @Throws(RemoteException::class)
                override fun onError(error: Int, errorMsg: String) {
                    //  outputRedText("=> onError: $errorMsg")
                    println("0onError")
                    printerCallback(true, 0)
                }
            })

        }catch (ex:Exception){
            ex.printStackTrace()
        }


    }

    fun printDetailReportupdate2(
        batch: MutableList<TempBatchFileDataTable>,
        context: Context?,
        printCB: (Boolean) -> Unit
    ) {
        try {
            var isFirstTimeForAmxLogo = true
            val pp = vectorPrinter?.status
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
                    val isAmexPresent = batch.find{ it.hostBankID.equals(AppPreference.AMEX_BANK_CODE) || it.hostBankID.equals(AMEX_BANK_CODE_SINGLE_DIGIT)}
                    if(isHdfcPresent?.hostBankID.equals(HDFC_BANK_CODE) || isHdfcPresent?.hostBankID.equals(HDFC_BANK_CODE_SINGLE_DIGIT)){
                        headerPrinting(HDFC_BANK_CODE)}
                    else if(isAmexPresent?.hostBankID.equals(AppPreference.AMEX_BANK_CODE) || isAmexPresent?.hostBankID.equals(AMEX_BANK_CODE_SINGLE_DIGIT)){
                        headerPrinting(AppPreference.AMEX_BANK_CODE)
                        isFirstTimeForAmxLogo = false
                    }else{
                        //headerPrinting(DEFAULT_BANK_CODE)
                        //headerPrinting(AppPreference.getBankCode())
                        headerPrinting(tpt?.tidBankCode)
                    }




                    //  ------------------------------------------
                    val td = System.currentTimeMillis()
                    val formatdate = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                    val formattime = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)

                    val date = formatdate.format(td)
                    val time = formattime.format(td)


                    textBlockList.put(AlignMode.LEFT, "DATE:${date}")
                    textBlockList.put(AlignMode.RIGHT, "TIME:${time}")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()
                    sigleLineText("DETAIL REPORT", AlignMode.CENTER, TextSize.NORMAL)

                    val terminalData = getTptData()

                    textBlockList.put( AlignMode.LEFT, "MID:${terminalData?.merchantId}")
                    textBlockList.put(
                        AlignMode.RIGHT, "TID:${batch[0].tid}")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()

                    textBlockList.put(AlignMode.LEFT,
                            "BATCH NO:${batch[0].hostBatchNumber}")
                    textBlockList.put(AlignMode.RIGHT,
                            " ")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()

                    printSeperator()
                }

                if (batch.isEmpty()) {
                    // alignLeftRightText(textInLineFormatBundle, "Total Transaction", "0")
                } else {
                    textBlockList.put(AlignMode.LEFT, "TRANS-TYPE")
                    textBlockList.put(AlignMode.RIGHT, "AMOUNT")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()

                    textBlockList.put(AlignMode.LEFT, "ISSUER")
                    textBlockList.put(AlignMode.RIGHT, "PAN/CID")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()

                    textBlockList.put(AlignMode.LEFT, "DATE-TIME")
                    textBlockList.put(AlignMode.RIGHT, "INVOICE")
                    mixStyleTextPrint(textBlockList)
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
                        //  || b.transactionType == BhTransactionType.VOID_PREAUTH.type
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
                                textBlockList.put(AlignMode.LEFT, "${"SALE"}")
                            } else {
                                textBlockList.put( AlignMode.LEFT, "${b.transationName}")
                            }

                            textBlockList.put(AlignMode.RIGHT, transAmount)
                            mixStyleTextPrint(textBlockList)
                            textBlockList.clear()
                            if (b.transactionType == BhTransactionType.VOID_PREAUTH.type) {
                                textBlockList.put(
                                    AlignMode.LEFT,
                                    b.appName
                                )
                                textBlockList.put(AlignMode.RIGHT, b.panMask)
                                mixStyleTextPrint(textBlockList)
                                textBlockList.clear()
                            } else {
                                textBlockList.put(
                                    AlignMode.LEFT,
                                    b.cardType
                                )
                                textBlockList.put(AlignMode.RIGHT, b.cardNumber)
                                mixStyleTextPrint(textBlockList)
                                textBlockList.clear()
                            }
                            if (b.transactionType == BhTransactionType.OFFLINE_SALE.type || b.transactionType == BhTransactionType.VOID_OFFLINE_SALE.type) {
                                try {

                                    //val dat = "${b.dateTime}"
                                    val dat = "${b.printDate} - ${b.time}"
                                    textBlockList.put(AlignMode.LEFT , dat)
                                    b.hostInvoice?.let { invoiceWithPadding(it) }?.let {
                                        textBlockList.put(
                                            AlignMode.RIGHT, it
                                        )
                                    }
                                    mixStyleTextPrint(textBlockList)
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

                                    textBlockList.put(AlignMode.LEFT, dat)

                                    b.hostInvoice?.let { invoiceWithPadding(it) }?.let {
                                        textBlockList.put(
                                            AlignMode.RIGHT, it
                                        )
                                    }
                                    mixStyleTextPrint(textBlockList)
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


                            sigleLineText("***TOTAL TRANSACTIONS***", AlignMode.CENTER, TextSize.NORMAL)
                            val sortedMap = totalMap.toSortedMap(compareByDescending { it })

                            for ((k, m) in sortedMap) {
                                textBlockList.put(
                                    AlignMode.LEFT,
                                        Field48ResponseTimestamp.transactionType2Name(k).toUpperCase(
                                            Locale.ROOT
                                        ))
                                textBlockList.put(
                                    /*sigleLineformat(
                                        "=" + m.count + " ${
                                            getCurrencySymbol(
                                                tpt
                                            )
                                        }", AlignMode.CENTER
                                    )*/
                                    AlignMode.CENTER, "= ${m.count}"
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
                                textBlockList.put(AlignMode.RIGHT, "${getCurrencySymbol(tpt)}:${"%.2f".format((((m.total).toDouble()).div(100)).toString().toDouble())}")
                                printToalTxnList(textBlockList)
                                textBlockList.clear()
                            }
                            val terminalData = getTptData()
                            if (iteration > 0) {
                                printSeperator()

                                // handling printing logo
                                val bankId = batch[frequency].hostBankID
                                var logo = ""
                                if (bankId == AMEX_BANK_CODE_SINGLE_DIGIT || bankId == AppPreference.AMEX_BANK_CODE) {
                                    AMEX_LOGO
                                } else if (bankId == HDFC_BANK_CODE_SINGLE_DIGIT || bankId == HDFC_BANK_CODE){
                                    logo = HDFC_LOGO
                                }else{
                                    logo = ""
                                }

                                if (isFirstTimeForAmxLogo && logo != null && !logo.equals("")) {
                                    isFirstTimeForAmxLogo = false
                                    printLogo(logo)
                                    printSeperator()
                                }
                                // end region

                                textBlockList.put( AlignMode.LEFT,
                                        "MID:${terminalData?.merchantId}")
                                textBlockList.put(AlignMode.RIGHT,
                                        "TID:${batch[frequency].tid}")
                                mixStyleTextPrint(textBlockList)
                                textBlockList.clear()
                                textBlockList.put( AlignMode.LEFT,
                                        "BATCH NO:${batch[frequency].batchNumber}")
                                mixStyleTextPrint(textBlockList)
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
                    Field48ResponseTimestamp.selectDigiPosDataAccordingToTxnStatus(
                        EDigiPosPaymentStatus.Approved.desciption
                    ) as ArrayList<DigiPosDataTable>
                if (digiPosDataList.isNotEmpty()) {
                    printSeperator()
                    // centerText(textFormatBundle, "---------X-----------X----------")

                    sigleLineText("Digi Pos Detail Report", AlignMode.CENTER)

                    tpt?.terminalId?.let { sigleLineText( "TID : $it",AlignMode.CENTER) }
                    printSeperator()
                    // Txn description
                    textBlockList.put(AlignMode.LEFT, "MODE")
                    textBlockList.put(AlignMode.RIGHT, "AMOUNT(INR)")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()
                    textBlockList.put(AlignMode.LEFT, "PartnetTxnId")
                    textBlockList.put(AlignMode.RIGHT,"DATE-TIME")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()
                    textBlockList.put(AlignMode.LEFT, "mTxnId")
                    textBlockList.put(AlignMode.RIGHT, "pgwTxnId")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()
                    printSeperator()
                    //Txn Detail
                    for (digiPosData in digiPosDataList) {

                        textBlockList.put(AlignMode.LEFT, digiPosData.paymentMode)
                        textBlockList.put(AlignMode.RIGHT, digiPosData.amount)
                        mixStyleTextPrint(textBlockList)
                        textBlockList.clear()
                        textBlockList.put(AlignMode.LEFT, digiPosData.partnerTxnId)
                        textBlockList.put(AlignMode.RIGHT, digiPosData.txnDate + "  " + digiPosData.txnTime)
                        mixStyleTextPrint(textBlockList)
                        textBlockList.clear()

                        textBlockList.put(AlignMode.LEFT, digiPosData.mTxnId)
                        textBlockList.put(AlignMode.RIGHT, digiPosData.pgwTxnId)
                        mixStyleTextPrint(textBlockList)
                        textBlockList.clear()

                        printSeperator()
                    }
                    //   DigiPosDataTable.deletAllRecordAccToTxnStatus(EDigiPosPaymentStatus.Approved.desciption)
                }
                //endregion


                if (batch.isNotEmpty()) {
                    printSeperator()
                    sigleLineText("Bonushub", AlignMode.CENTER)
                    sigleLineText("App Version:${BuildConfig.VERSION_NAME}", AlignMode.CENTER)

                }

                vectorPrinter!!.startPrint(object : OnPrintListener.Stub() {
                    @Throws(RemoteException::class)
                    override fun onFinish() {
                        // outputText("=> onFinish | sheetNo = $curSheetNo")
                        // println("time cost = + " + (System.currentTimeMillis() - startTime))
                        println("0onFinish")
                        printCB(true)
                    }

                    @Throws(RemoteException::class)
                    override fun onStart() {
                        //  outputText("=> onStart | sheetNo = $curSheetNo")
                        println("0onStart")
                    }

                    @Throws(RemoteException::class)
                    override fun onError(error: Int, errorMsg: String) {
                        //  outputRedText("=> onError: $errorMsg")
                        println("0onError")
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

    private fun getCurrencySymbol(tpt: TerminalParameterTable?): String {
        return if (!TextUtils.isEmpty(tpt?.currencySymbol)) {
            tpt?.currencySymbol ?: "Rs"
        } else {
            "Rs"
        }
    }

    fun printSettlementReportupdate2(
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


                //headerPrinting(DEFAULT_BANK_CODE)
                //headerPrinting(AppPreference.getBankCode())
                headerPrinting(tpt?.tidBankCode)

                val td = System.currentTimeMillis()
                val formatdate = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                val formattime = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
                val date = formatdate.format(td)
                val time = formattime.format(td)

                textBlockList.put(AlignMode.LEFT, "DATE:${date}")
                textBlockList.put(AlignMode.RIGHT, "TIME:${time}")
                mixStyleTextPrint(textBlockList)
                textBlockList.clear()

                if (isLastSummary) {

                    sigleLineText("LAST SUMMARY REPORT", AlignMode.CENTER, TextSize.NORMAL)
                } else {

                    sigleLineText("SUMMARY REPORT", AlignMode.CENTER, TextSize.NORMAL)
                }


                textBlockList.put(AlignMode.LEFT, "TID:${tpt?.terminalId}")
                textBlockList.put(AlignMode.RIGHT, "MID:${tpt?.merchantId}")
                mixStyleTextPrint(textBlockList)
                textBlockList.clear()

                textBlockList.put(AlignMode.LEFT, "BATCH NO:${tpt?.batchNumber}")

                mixStyleTextPrint(textBlockList)
                textBlockList.clear()


                printSeperator()
                textBlockList.put(AlignMode.LEFT, "TOTAL TXN = 0")
                textBlockList.put(AlignMode.RIGHT,"${getCurrencySymbol(tpt)}")
                mixStyleTextPrint(textBlockList)
                textBlockList.clear()

                sigleLineText("ZERO SETTLEMENT SUCCESSFUL", AlignMode.CENTER, TextSize.NORMAL)
                if (!isLastSummary)
                    digiposReport()
                sigleLineText("BonusHub", AlignMode.CENTER)
                sigleLineText("App Version", AlignMode.CENTER)



                vectorPrinter!!.startPrint(object : OnPrintListener.Stub() {
                    @Throws(RemoteException::class)
                    override fun onFinish() {
                        // outputText("=> onFinish | sheetNo = $curSheetNo")
                        // println("time cost = + " + (System.currentTimeMillis() - startTime))
                        println("0onFinish")
                        callBack(true)
                    }

                    @Throws(RemoteException::class)
                    override fun onStart() {
                        //  outputText("=> onStart | sheetNo = $curSheetNo")
                        println("0onStart")
                    }

                    @Throws(RemoteException::class)
                    override fun onError(error: Int, errorMsg: String) {
                        //  outputRedText("=> onError: $errorMsg")
                        println("0onError")
                        callBack(false)
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

                val map = mutableMapOf<String, MutableMap<Int, PrintUtil.SummeryModel>>()
                val map1 = mutableMapOf<String, MutableMap<Int, PrintUtil.SummeryModel>>()
                //to hold the tid for which tid mid printed
                val listTidPrinted = mutableListOf<String>()
                val tpt = getTptData()
                var firstTimePrintedTid = ""

                // setLogoAndHeader()
                //headerPrinting()
                val isHdfcPresent = batch.find{ it.hostBankID.equals(HDFC_BANK_CODE) || it.hostBankID.equals(HDFC_BANK_CODE_SINGLE_DIGIT)}
                val isAmexPresent = batch.find{ it.hostBankID.equals(AppPreference.AMEX_BANK_CODE) || it.hostBankID.equals(AMEX_BANK_CODE_SINGLE_DIGIT)}
                if(isHdfcPresent?.hostBankID.equals(HDFC_BANK_CODE) || isHdfcPresent?.hostBankID.equals(HDFC_BANK_CODE_SINGLE_DIGIT)){
                    headerPrinting(HDFC_BANK_CODE)
                    firstTimePrintedTid = isHdfcPresent!!.hostTID.toString()
                }
                else if(isAmexPresent?.hostBankID.equals(AppPreference.AMEX_BANK_CODE) || isAmexPresent?.hostBankID.equals(AMEX_BANK_CODE_SINGLE_DIGIT)){
                    headerPrinting(AppPreference.AMEX_BANK_CODE)
                    isFirstTimeForAmxLogo = false
                    firstTimePrintedTid = isAmexPresent!!.hostTID.toString()
                }else{
                    //headerPrinting(DEFAULT_BANK_CODE)
                   // headerPrinting(AppPreference.getBankCode())
                    headerPrinting(tpt?.tidBankCode)
                }


                val td = System.currentTimeMillis()
                val formatdate = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                val formattime = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)

                val date = formatdate.format(td)
                val time = formattime.format(td)

                textBlockList.put(AlignMode.LEFT, "DATE:${date}")
                textBlockList.put(AlignMode.RIGHT, "TIME:${time}")
                mixStyleTextPrint(textBlockList)
                textBlockList.clear()

                //  alignLeftRightText(fmtAddTextInLine,"DATE : ${batch.date}","TIME : ${batch.time}")
                /*   alignLeftRightText(textInLineFormatBundle, "MID : ${batch[0].mid}", "TID : ${batch[0].tid}")
                   alignLeftRightText(textInLineFormatBundle, "BATCH NO  : ${batch[0].batchNumber}", "")*/

                if (isLastSummary) {

                    sigleLineText("LAST SUMMARY REPORT", AlignMode.CENTER, TextSize.NORMAL)
                } else {

                    sigleLineText("SUMMARY REPORT", AlignMode.CENTER, TextSize.NORMAL)
                }

                batch.sortBy { it?.tid }

                var tempTid = batch[0]?.tid

                val list = mutableListOf<String>()
                val frequencylist = mutableListOf<String>()

                for (it in batch) {  // Do not count preauth transaction
// || it.transactionType == BhTransactionType.VOID_PREAUTH.type

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
                                map[it.tid + it.mid + it.batchNumber + it.appName] as MutableMap<Int, PrintUtil.SummeryModel>
                            if (ma.containsKey(it.transactionType)) {
                                val m = ma[it.transactionType] as PrintUtil.SummeryModel
                                m.count += 1
                                if (transAmt != null) {
                                    m.total = m.total?.plus(transAmt)
                                }
                            } else {
                                val txnName = it.transationName
                                val rtid = it.tid
                                val sm = PrintUtil.SummeryModel(
                                    txnName, 1, transAmt, rtid
                                )
                                ma[it.transactionType] = sm
                            }
                        } else {
                            val hm = HashMap<Int, PrintUtil.SummeryModel>().apply {
                                this[it.transactionType] = transAmt?.let { it1 ->
                                    it.transationName?.let { it2 ->
                                        it.tid?.let { it3 ->
                                            PrintUtil.SummeryModel(
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
                        val hm = HashMap<Int, PrintUtil.SummeryModel>().apply {
                            this[it.transactionType] = transAmt?.let { it1 ->
                                it.transationName?.let { it2 ->
                                    it.tid?.let { it3 ->
                                        PrintUtil.SummeryModel(
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

                        val tptTemp= getTptData()
                        val mid=tptTemp?.merchantId
                        //  if (ietration > 0) {
                        // if hostid is not avialable in this or list is blanck then print this line
                        if((listTidPrinted.size==0) || !(listTidPrinted.contains(hostTid)))
                        {
                            // handle logo printing
                            var bankId = mapTidToBankId[hostTid]
                            var logo = ""
                            if (bankId.equals(AMEX_BANK_CODE_SINGLE_DIGIT) || bankId.equals(
                                    AppPreference.AMEX_BANK_CODE
                                )) {
                                logo = AMEX_LOGO
                            } else if(bankId.equals(HDFC_BANK_CODE_SINGLE_DIGIT)  || bankId.equals(HDFC_BANK_CODE)){
                                logo = HDFC_LOGO
                            }else{
                                logo = ""
                            }

                            if (hostTid != firstTimePrintedTid && isFirstTimeForAmxLogo && logo != null && !logo.equals("")) {
                                isFirstTimeForAmxLogo = false
                                printSeperator()
                                printLogo(logo)
                            }
                            // end region

                            listTidPrinted.add(hostTid)// add the tid for which this code is printed
                            printSeperator()
                            textBlockList.put(AlignMode.LEFT, "MID:${mid}")
                            textBlockList.put(AlignMode.RIGHT, "TID:${hostTid}")
                            mixStyleTextPrint(textBlockList)
                            textBlockList.clear()
                            textBlockList.put(AlignMode.LEFT, "BATCH NO:${hostBatchNumber}")
                            mixStyleTextPrint(textBlockList)
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
                        textBlockList.put(AlignMode.LEFT, _issuerNameString)
                        textBlockList.put(AlignMode.CENTER, "${cardIssuer.toUpperCase(Locale.ROOT)}")
                        textBlockList.put(AlignMode.RIGHT, "        ")
                        mixStyleTextPrint(textBlockList)
                        textBlockList.clear()
                        // if(ind==0){
                        textBlockList.put(AlignMode.LEFT, "TXN TYPE")
                        textBlockList.put(AlignMode.CENTER, "COUNT      ")
                        textBlockList.put(AlignMode.RIGHT, "TOTAL")
                        mixStyleTextPrint(textBlockList)
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
                                    textBlockList.put(
                                        AlignMode.LEFT, "SALE"
                                    )
                                }else{
                                    textBlockList.put(  AlignMode.LEFT,
                                        Field48ResponseTimestamp.transactionType2Name(k).toUpperCase(Locale.ROOT))
                                }

                            }
                            textBlockList.put(
                                AlignMode.CENTER,
                                    "${m.count}")
                            textBlockList.put( AlignMode.RIGHT, "${getCurrencySymbol(tpt)}:$amt")
                            printToalTxnList(textBlockList)
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
                        sigleLineText("***TOTAL TRANSACTION***", AlignMode.CENTER, TextSize.NORMAL)

                        val sortedMap = totalMap.toSortedMap(compareByDescending { it })
                        for ((k, m) in sortedMap) {

                            textBlockList.put( AlignMode.LEFT,
                                    Field48ResponseTimestamp.transactionType2Name(k).toUpperCase(
                                        Locale.ROOT
                                    ))
                            textBlockList.put( AlignMode.CENTER,
                                    "  = " + m.count)
                            textBlockList.put( AlignMode.RIGHT, "${getCurrencySymbol(tpt)}:${"%.2f".format(
                                    (((m.total).toDouble()).div(
                                        100
                                    )).toString().toDouble()
                                )}")

                            printToalTxnList(textBlockList)
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


                vectorPrinter!!.startPrint(object : OnPrintListener.Stub() {
                    @Throws(RemoteException::class)
                    override fun onFinish() {
                        // outputText("=> onFinish | sheetNo = $curSheetNo")
                        // println("time cost = + " + (System.currentTimeMillis() - startTime))
                        println("0onFinish")
                        callBack(true)
                    }

                    @Throws(RemoteException::class)
                    override fun onStart() {
                        //  outputText("=> onStart | sheetNo = $curSheetNo")
                        println("0onStart")
                    }

                    @Throws(RemoteException::class)
                    override fun onError(error: Int, errorMsg: String) {
                        //  outputRedText("=> onError: $errorMsg")
                        println("0onError")
                        callBack(false)
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
            Field48ResponseTimestamp.selectDigiPosDataAccordingToTxnStatus(EDigiPosPaymentStatus.Approved.desciption)
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
            textBlockList.put(AlignMode.LEFT, "TXN TYPE")
            textBlockList.put(AlignMode.CENTER, "TOTAL")
            textBlockList.put(AlignMode.RIGHT, "COUNT")
            mixStyleTextPrint(textBlockList)
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

                textBlockList.put( AlignMode.LEFT, txnType)
                textBlockList.put( AlignMode.CENTER, "%.2f".format(txnTotalAmount))
                textBlockList.put( AlignMode.RIGHT, txnCount.toString() + getCurrencySymbol(tpt))

                mixStyleTextPrint(textBlockList)
                textBlockList.clear()
            }
            printSeperator()
            textBlockList.put(AlignMode.LEFT, "Total TXNs")
            textBlockList.put(AlignMode.CENTER, totalCount.toString() + getCurrencySymbol(tpt))
            textBlockList.put(AlignMode.RIGHT, "%.2f".format(totalAmount))
            mixStyleTextPrint(textBlockList)
            textBlockList.clear()
            printSeperator()
        }

    }

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
                // centerText(textFormatBundle, "SETTLEMENT SUCCESSFUL")

                val tpt = getTptData()
                /*   tpt?.receiptHeaderOne?.let { centerText(textInLineFormatBundle, it) }
                   tpt?.receiptHeaderTwo?.let { centerText(textInLineFormatBundle, it) }
                   tpt?.receiptHeaderThree?.let { centerText(textInLineFormatBundle, it) }
   */
                val isHdfcPresent = batch.find{ it.hostBankID.equals("01") || it.hostBankID.equals("1")}
                val isAmexPresent = batch.find{ it.hostBankID.equals(AMEX_BANK_CODE) || it.hostBankID.equals(AMEX_BANK_CODE_SINGLE_DIGIT)}
                if(isHdfcPresent?.hostBankID.equals("01") || isHdfcPresent?.hostBankID.equals("1")){
                    headerPrinting(HDFC_BANK_CODE)}
                else if(isAmexPresent?.hostBankID.equals(AMEX_BANK_CODE) || isAmexPresent?.hostBankID.equals(AMEX_BANK_CODE_SINGLE_DIGIT)){
                    headerPrinting(AMEX_BANK_CODE)
                    isFirstTimeForAmxLogo = false
                }else
                {
                    //headerPrinting(DEFAULT_BANK_CODE)
                    //headerPrinting(AppPreference.getBankCode())
                    headerPrinting(tpt?.tidBankCode)
                }

                if (isLastSummary) {
                    val lastTimeStamp=AppPreference.getString(AppPreference.LAST_BATCH_TimeStamp)
                    td=lastTimeStamp.toLong()
                }else{
                    td = System.currentTimeMillis()
                    AppPreference.saveString(AppPreference.LAST_BATCH_TimeStamp, td.toString())
                }
                val formatdate = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                val formattime = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)

                val date = formatdate.format(td)
                val time = formattime.format(td)

                textBlockList.put(AlignMode.LEFT, "DATE:${date}")
                textBlockList.put(AlignMode.RIGHT, "TIME:${time}")
                mixStyleTextPrint(textBlockList)
                textBlockList.clear()

                if (isLastSummary) {
                    sigleLineText("LAST SUMMARY REPORT", AlignMode.CENTER, TextSize.NORMAL)
                } else {
                    sigleLineText("SUMMARY REPORT", AlignMode.CENTER, TextSize.NORMAL)
                }

                textBlockList.put(AlignMode.LEFT, "TID:${tpt?.terminalId}")
                textBlockList.put(AlignMode.RIGHT, "MID:${tpt?.merchantId}")
                mixStyleTextPrint(textBlockList)
                textBlockList.clear()

                textBlockList.put(AlignMode.LEFT, "BATCH NO:${tpt?.batchNumber}")

                mixStyleTextPrint(textBlockList)
                textBlockList.clear()

                printSeperator()
                textBlockList.put(AlignMode.LEFT, "TOTAL TXN = 0")
                textBlockList.put(AlignMode.RIGHT,"${getCurrencySymbol(tpt)}")
                mixStyleTextPrint(textBlockList)
                textBlockList.clear()


                if (isSettlementSuccess) {
                    sigleLineText("ZERO SETTLEMENT SUCCESSFUL", AlignMode.CENTER, TextSize.NORMAL)
                }else{
                    sigleLineText("ZERO SETTLEMENT", AlignMode.CENTER, TextSize.NORMAL)
                }


                sigleLineText("ZERO SETTLEMENT SUCCESSFUL", AlignMode.CENTER, TextSize.NORMAL)

                // Below code is used for Digi POS Settlement report
                if (!isLastSummary) {
                    digiposReport()
                }

                /*{
                    val digiPosDataList =
                        DigiPosDataTable.selectDigiPosDataAccordingToTxnStatus(EDigiPosPaymentStatus.Approved.desciption)
                    val requiredTxnhm = hashMapOf<String, ArrayList<DigiPosDataTable>>()
                    if (digiPosDataList.isNotEmpty()) {
                        for (i in digiPosDataList) {
                            val digiData = arrayListOf<DigiPosDataTable>()
                            for (j in digiPosDataList) {
                                if (i.paymentMode == j.paymentMode) {
                                    digiData.add(j)
                                    requiredTxnhm[i.paymentMode] = digiData
                                }
                            }
                        }

                        ///  centerText(textFormatBundle, "---------X-----------X----------")
                        centerText(textFormatBundle, "Digi Pos Summary Report", true)
                        tpt?.terminalId?.let { centerText(textFormatBundle, "TID : $it") }
                        printSeperator(textFormatBundle)
                        // Txn description
                        alignLeftRightText(textInLineFormatBundle, "TXN TYPE", "TOTAL", "COUNT")
                        printSeperator(textFormatBundle)
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
                            alignLeftRightText(
                                textInLineFormatBundle,
                                txnType,
                                "%.2f".format(txnTotalAmount),
                                txnCount.toString() + getCurrencySymbol(tpt)
                            )
                        }
                        printSeperator(textFormatBundle)
                        alignLeftRightText(
                            textInLineFormatBundle,
                            "Total TXNs",
                            "%.2f".format(totalAmount),
                            totalCount.toString() + getCurrencySymbol(tpt)
                        )
                        printSeperator(textFormatBundle)
                    }
                }*/
                // todo Digipos txncode here

                sigleLineText("BonusHub", AlignMode.CENTER)
                sigleLineText("App Version", AlignMode.CENTER)


                vectorPrinter!!.startPrint(object : OnPrintListener.Stub() {
                    @Throws(RemoteException::class)
                    override fun onFinish() {
                        // outputText("=> onFinish | sheetNo = $curSheetNo")
                        // println("time cost = + " + (System.currentTimeMillis() - startTime))
                        println("0onFinish")
                        callBack(true)
                    }

                    @Throws(RemoteException::class)
                    override fun onStart() {
                        //  outputText("=> onStart | sheetNo = $curSheetNo")
                        println("0onStart")
                    }

                    @Throws(RemoteException::class)
                    override fun onError(error: Int, errorMsg: String) {
                        //  outputRedText("=> onError: $errorMsg")
                        println("0onError")
                        callBack(false)
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

                val map = mutableMapOf<String, MutableMap<Int, PrintUtil.SummeryModel>>()
                val map1 = mutableMapOf<String, MutableMap<Int, PrintUtil.SummeryModel>>()
                val tpt = getTptData()

                //setLogoAndHeader()
                var isHdfcPresent = batch.find{ it.hostBankID.equals("01") || it.hostBankID.equals("1")}
                var isAmexPresent = batch.find{ it.hostBankID.equals(AMEX_BANK_CODE) || it.hostBankID.equals(AMEX_BANK_CODE_SINGLE_DIGIT)}
                if(isHdfcPresent?.hostBankID.equals("01") || isHdfcPresent?.hostBankID.equals("1")){
                    headerPrinting(HDFC_BANK_CODE)}
                else if(isAmexPresent?.hostBankID.equals(AMEX_BANK_CODE) || isAmexPresent?.hostBankID.equals(AMEX_BANK_CODE_SINGLE_DIGIT)){
                    headerPrinting(AMEX_BANK_CODE)
                    isFirstTimeForAmxLogo = false
                }else
                {
                    //headerPrinting(DEFAULT_BANK_CODE)
                    //headerPrinting(AppPreference.getBankCode())
                    headerPrinting(tpt?.tidBankCode)
                }

                if (isLastSummary) {
                    val lastTimeStamp=AppPreference.getString(AppPreference.LAST_BATCH_TimeStamp)
                    td=lastTimeStamp.toLong()
                }else{
                    td = System.currentTimeMillis()
                    AppPreference.saveString(AppPreference.LAST_BATCH_TimeStamp, td.toString())
                }
                val formatdate = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                val formattime = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)

                val date = formatdate.format(td)
                val time = formattime.format(td)

                textBlockList.put(AlignMode.LEFT, "DATE:${date}")
                textBlockList.put(AlignMode.RIGHT, "TIME:${time}")
                mixStyleTextPrint(textBlockList)
                textBlockList.clear()

                //  alignLeftRightText(fmtAddTextInLine,"DATE : ${batch.date}","TIME : ${batch.time}")
                /*   alignLeftRightText(textInLineFormatBundle, "MID : ${batch[0].mid}", "TID : ${batch[0].tid}")
                   alignLeftRightText(textInLineFormatBundle, "BATCH NO  : ${batch[0].batchNumber}", "")*/

                if (isLastSummary) {

                    sigleLineText("LAST SUMMARY REPORT", AlignMode.CENTER, TextSize.NORMAL)
                } else {

                    sigleLineText("SUMMARY REPORT", AlignMode.CENTER, TextSize.NORMAL)
                }

                batch.sortBy { it.hostTID }
                batch.sortBy { it.hostBankID } // fox amex

                var tempTid = batch[0].hostTID

                val list = mutableListOf<String>()
                val frequencylist = mutableListOf<String>()

                for (it in batch) {  // Do not count preauth transaction

                    mapTidToBankId[it.hostTID] = it.hostBankID

                    // || it.transactionType == TransactionType.VOID_PREAUTH.type
                    if (it.transactionType == BhTransactionType.PRE_AUTH.type) continue

                    if (it.transactionType == BhTransactionType.PRE_AUTH_COMPLETE.type || it.transactionType == BhTransactionType.VOID_PREAUTH.type ) {
                        it.cardType = "AUTH-COMP"
                    }


                    if (it.transactionType == BhTransactionType.EMI_SALE.type || it.transactionType == BhTransactionType.BRAND_EMI.type || it.transactionType == BhTransactionType.BRAND_EMI_BY_ACCESS_CODE.type ) {
                        it.cardType = it.issuerName
                        it.transactionType = BhTransactionType.EMI_SALE.type
                    }
                    if (it.transactionType == BhTransactionType.VOID_EMI.type){
                        it.cardType = it.issuerName
                    }

                    if (it.transactionType == BhTransactionType.TEST_EMI.type) {
                        it.issuerName = "Test Issuer"
                        it.cardType = "Test Issuer"
                        it.transactionType = BhTransactionType.SALE.type

                    }

                    val transAmt = try {
                        it.transactionalAmmount.toLong()
                    } catch (ex: Exception) {
                        0L
                    }


                    if (tempTid == it.hostTID) {
                        _issuerName = it.cardType
                        if (map.containsKey(it.hostTID + it.hostMID + it.hostBatchNumber + it.cardType)) {
                            _issuerName = it.cardType

                            val ma =
                                map[it.hostTID + it.hostMID + it.hostBatchNumber + it.cardType] as MutableMap<Int, PrintUtil.SummeryModel>
                            if (ma.containsKey(it.transactionType)) {
                                val m = ma[it.transactionType] as PrintUtil.SummeryModel
                                m.count += 1
                                m.total = m.total?.plus(transAmt)
                            } else {
                                val sm = PrintUtil.SummeryModel(
                                    transactionType2Name(it.transactionType),
                                    1,
                                    transAmt,
                                    it.hostTID
                                )
                                ma[it.transactionType] = sm
                            }
                        } else {
                            val hm = HashMap<Int, PrintUtil.SummeryModel>().apply {
                                this[it.transactionType] = PrintUtil.SummeryModel(
                                    transactionType2Name(it.transactionType),
                                    1,
                                    transAmt,
                                    it.hostTID
                                )
                            }
                            map[it.hostTID + it.hostMID + it.hostBatchNumber + it.cardType] = hm
                            list.add(it.hostTID)
                        }
                    } else {
                        tempTid = it.hostTID
                        _issuerName = it.cardType
                        val hm = HashMap<Int, PrintUtil.SummeryModel>().apply {
                            this[it.transactionType] = PrintUtil.SummeryModel(
                                transactionType2Name(it.transactionType),
                                1,
                                transAmt,
                                it.hostTID
                            )
                        }
                        map[it.hostTID + it.hostMID + it.hostBatchNumber + it.cardType] = hm
                        list.add(it.hostTID)
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
                var lastTidPrint = ""

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


                        if (ietration > 0 && !lastTidPrint.equals(hostTid)) {

                            // region print logo when hostBankId = 7
                            var bankId = mapTidToBankId[hostTid]
                            val logo = if (bankId == AMEX_BANK_CODE_SINGLE_DIGIT || bankId == AMEX_BANK_CODE) {
                                AMEX_LOGO
                            } else {
                                ""
                                //HDFC_LOGO
                            }

                            if (isFirstTimeForAmxLogo && logo != null && !logo.equals("")) {
                                isFirstTimeForAmxLogo = false
                                printSeperator()
                                printLogo(logo)
                            }
                            // end region

                            lastTidPrint = hostTid
                            printSeperator()

                            textBlockList.put(AlignMode.LEFT, "MID:${hostMid}")
                            textBlockList.put(AlignMode.RIGHT, "TID:${hostTid}")
                            mixStyleTextPrint(textBlockList)
                            textBlockList.clear()
                            textBlockList.put(AlignMode.LEFT, "BATCH NO:${hostBatchNumber}")
                            mixStyleTextPrint(textBlockList)
                            textBlockList.clear()

                            ietration--
                        }
                        if (cardIssuer.isNullOrEmpty()) {
                            cardIssuer = _issuerName.toString()
                            _issuerNameString = "CARD ISSUER"

                        }
                        if ("AUTH-COMP" == cardIssuer) {
                            // need Not to show
                        }
                        else {
                            printSeperator()

                            textBlockList.put(AlignMode.LEFT, _issuerNameString)
                            textBlockList.put(AlignMode.CENTER, "${cardIssuer.toUpperCase(Locale.ROOT)}")
                            textBlockList.put(AlignMode.RIGHT, "        ")
                            mixStyleTextPrint(textBlockList)
                            textBlockList.clear()

                            textBlockList.put(AlignMode.LEFT, "TXN TYPE")
                            textBlockList.put(AlignMode.CENTER, "COUNT      ")
                            textBlockList.put(AlignMode.RIGHT, "TOTAL")
                            mixStyleTextPrint(textBlockList)
                            textBlockList.clear()
                        }
                    }
                    for ((k, m) in _map) {

                        val amt = "%.2f".format((((m.total)?.toDouble())?.div(100)).toString().toDouble())
                        if (k == BhTransactionType.PRE_AUTH_COMPLETE.type || k == BhTransactionType.VOID_PREAUTH.type) {
                            // need Not to show
                        } else {

                            textBlockList.put( AlignMode.LEFT, m.type?.toUpperCase(Locale.ROOT) ?: "")
                            textBlockList.put(
                                AlignMode.CENTER,
                                "${m.count}")
                            textBlockList.put( AlignMode.RIGHT, "${getCurrencySymbol(tpt)+"  "+ amt}")
                            printToalTxnList(textBlockList)
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
                        sigleLineText("***TOTAL TRANSACTION***", AlignMode.CENTER, TextSize.NORMAL)
                        val sortedMap = totalMap.toSortedMap(compareByDescending { it })
                        for ((k, m) in sortedMap) {
                            /* alignLeftRightText(
                                 textInLineFormatBundle,
                                 "${transactionType2Name(k).toUpperCase(Locale.ROOT)}${"     =" + m.count}",
                                 "Rs.     ${"%.2f".format(((m.total).toDouble() / 100))}"

                             )*/

                            textBlockList.put( AlignMode.LEFT,
                                Field48ResponseTimestamp.transactionType2Name(k).toUpperCase(
                                    Locale.ROOT
                                ))
                            textBlockList.put( AlignMode.CENTER,
                                "  = " + m.count)
                            textBlockList.put( AlignMode.RIGHT, "${getCurrencySymbol(tpt)}:${"%.2f".format(
                                (((m.total).toDouble()).div(
                                    100
                                )).toString().toDouble()
                            )}")

                            printToalTxnList(textBlockList)
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
                    digiposReport()
                }
                    /*{
                    val digiPosDataList =
                        DigiPosDataTable.selectDigiPosDataAccordingToTxnStatus(EDigiPosPaymentStatus.Approved.desciption)
                    val requiredTxnhm = hashMapOf<String, ArrayList<DigiPosDataTable>>()
                    if (digiPosDataList.isNotEmpty()) {
                        for (i in digiPosDataList) {
                            val digiData = arrayListOf<DigiPosDataTable>()
                            for (j in digiPosDataList) {
                                if (i.paymentMode == j.paymentMode) {
                                    digiData.add(j)
                                    requiredTxnhm[i.paymentMode] = digiData
                                }
                            }
                        }

                        ///  centerText(textFormatBundle, "---------X-----------X----------")
                        centerText(textFormatBundle, "Digi Pos Summary Report", true)
                        tpt?.terminalId?.let { centerText(textFormatBundle, "TID : $it") }
                        printSeperator(textFormatBundle)
                        // Txn description
                        alignLeftRightText(textInLineFormatBundle, "TXN TYPE", "TOTAL", "COUNT")
                        printSeperator(textFormatBundle)
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
                            alignLeftRightText(
                                textInLineFormatBundle,
                                txnType,
                                getCurrencySymbol(tpt)+"  "+"%.2f".format(txnTotalAmount),
                                txnCount.toString()
                            )
                        }
                        printSeperator(textFormatBundle)
                        alignLeftRightText(
                            textInLineFormatBundle,
                            "Total TXNs",
                            getCurrencySymbol(tpt)+"  "+ "%.2f".format(totalAmount),
                            totalCount.toString()
                        )
                        printSeperator(textFormatBundle)
                    }
                }*/

                sigleLineText("Bonushub", AlignMode.CENTER)
                sigleLineText("App Version:${BuildConfig.VERSION_NAME}", AlignMode.CENTER)


                vectorPrinter!!.startPrint(object : OnPrintListener.Stub() {
                    @Throws(RemoteException::class)
                    override fun onFinish() {
                        // outputText("=> onFinish | sheetNo = $curSheetNo")
                        // println("time cost = + " + (System.currentTimeMillis() - startTime))
                        println("0onFinish")
                        callBack(true)
                    }

                    @Throws(RemoteException::class)
                    override fun onStart() {
                        //  outputText("=> onStart | sheetNo = $curSheetNo")
                        println("0onStart")
                    }

                    @Throws(RemoteException::class)
                    override fun onError(error: Int, errorMsg: String) {
                        //  outputRedText("=> onError: $errorMsg")
                        println("0onError")
                        callBack(false)
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

    fun printDetailReportupdate(
        batch: MutableList<TempBatchFileDataTable>,
        context: Context?,
        printCB: (Boolean) -> Unit
    ) {
        try {
            var isFirstTimeForAmxLogo = true
            val pp = vectorPrinter?.status
            Log.e("Printer Status", pp.toString())
            if (pp == 0) {

                val appVersion = BuildConfig.VERSION_NAME
                val tpt = getTptData()

                batch.sortBy { it.hostTID }
                batch.sortBy { it.hostBankID } // for Amex logo

                if (batch.isEmpty()) {
                    // alignLeftRightText(textInLineFormatBundle, "MID : ${tpt?.merchantId}", "TID : ${tpt?.terminalId}")
                } else {
                    //-----------------------------------------------
                    //setLogoAndHeader()
                    var isHdfcPresent = batch.find{ it.hostBankID.equals("01") || it.hostBankID.equals("1")}
                    var isAmexPresent = batch.find{ it.hostBankID.equals(AMEX_BANK_CODE) || it.hostBankID.equals(AMEX_BANK_CODE_SINGLE_DIGIT)}
                    if(isHdfcPresent?.hostBankID.equals("01") || isHdfcPresent?.hostBankID.equals("1")){
                        headerPrinting(HDFC_BANK_CODE)}
                    else if(isAmexPresent?.hostBankID.equals(AMEX_BANK_CODE) || isAmexPresent?.hostBankID.equals(AMEX_BANK_CODE_SINGLE_DIGIT)){
                        headerPrinting(AMEX_BANK_CODE)
                        isFirstTimeForAmxLogo = false
                    }else{
                        //headerPrinting(DEFAULT_BANK_CODE)
                        //headerPrinting(AppPreference.getBankCode())
                        headerPrinting(tpt?.tidBankCode)
                    }
                    //  ------------------------------------------
                    val td = System.currentTimeMillis()
                    val formatdate = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                    val formattime = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)

                    val date = formatdate.format(td)
                    val time = formattime.format(td)

                    textBlockList.put(AlignMode.LEFT, "DATE:${date}")
                    textBlockList.put(AlignMode.RIGHT, "TIME:${time}")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()
                    sigleLineText("DETAIL REPORT", AlignMode.CENTER, TextSize.NORMAL)


                    textBlockList.put( AlignMode.LEFT, "MID:${tpt?.merchantId}")
                    textBlockList.put(
                        AlignMode.RIGHT, "TID:${batch[0].tid}")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()

                    textBlockList.put(AlignMode.LEFT,
                        "BATCH NO:${batch[0].hostBatchNumber}")
                    textBlockList.put(AlignMode.RIGHT,
                        " ")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()

                    printSeperator()
                }

                if (batch.isEmpty()) {
                    // alignLeftRightText(textInLineFormatBundle, "Total Transaction", "0")
                } else {

                    textBlockList.put(AlignMode.LEFT, "TRANS-TYPE")
                    textBlockList.put(AlignMode.RIGHT, "AMOUNT")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()

                    textBlockList.put(AlignMode.LEFT, "ISSUER")
                    textBlockList.put(AlignMode.RIGHT, "PAN/CID")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()

                    textBlockList.put(AlignMode.LEFT, "DATE-TIME")
                    textBlockList.put(AlignMode.RIGHT, "INVOICE")
                    mixStyleTextPrint(textBlockList)
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
                        tidlist.add(item.hostTID)
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
                                x.total += b.transactionalAmmount.toLong()
                            }
                        } else {
                            totalMap[b.transactionType] =
                                SummeryTotalType(1, b.transactionalAmmount.toLong())
                        }
                        val transAmount = "%.2f".format(b.transactionalAmmount.toDouble() / 100)

                        textBlockList.put(AlignMode.LEFT, transactionType2Name(b.transactionType))
                        textBlockList.put(AlignMode.RIGHT, transAmount)
                        mixStyleTextPrint(textBlockList)
                        textBlockList.clear()

                        if (b.transactionType == BhTransactionType.VOID_PREAUTH.type) {

                            textBlockList.put(
                                AlignMode.LEFT,
                                b.cardType
                            )
                            textBlockList.put(AlignMode.RIGHT, panMasking(b.encryptPan, "0000*0000"))
                            mixStyleTextPrint(textBlockList)
                            textBlockList.clear()
                        } else {

                            textBlockList.put(
                                AlignMode.LEFT,
                                b.cardType
                            )
                            textBlockList.put(AlignMode.RIGHT, panMasking(b.cardNumber, "0000*0000"))
                            mixStyleTextPrint(textBlockList)
                            textBlockList.clear()
                        }
                        if (b.transactionType == BhTransactionType.OFFLINE_SALE.type || b.transactionType == BhTransactionType.VOID_OFFLINE_SALE.type) {
                            try {
                                val dat = "${b.printDate} - ${b.time}"
                                textBlockList.put(AlignMode.LEFT , dat)
                                textBlockList.put(AlignMode.RIGHT , invoiceWithPadding(b.hostInvoice))
                                mixStyleTextPrint(textBlockList)
                                textBlockList.clear()
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }

                        } else {
                            val timee = b.time
                            val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
                            val timeFormat2 = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            var formattedTime = ""
                            try {
                                val t1 = timeFormat.parse(timee)
                                formattedTime = timeFormat2.format(t1)
                                Log.e("Time", formattedTime)
                                val dat = "${b.transactionDate} - $formattedTime"

                                textBlockList.put(AlignMode.LEFT, dat)
                                textBlockList.put(AlignMode.RIGHT, invoiceWithPadding(b.hostInvoice))

                                mixStyleTextPrint(textBlockList)
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
                            // printSeperator(textFormatBundle)
                            sigleLineText("***TOTAL TRANSACTIONS***", AlignMode.CENTER, TextSize.NORMAL)
                            val sortedMap = totalMap.toSortedMap(compareByDescending { it })
                            /* for ((k, v) in sortedMap) {
                             alignLeftRightText(
                                 textInLineFormatBundle,
                                 "${transactionType2Name(k)} = ${v.count}",
                                 "Rs %.2f".format(v.total.toDouble() / 100)
                             )
                         }*/

                            for ((k, m) in sortedMap) {
                                /* alignLeftRightText(
                                 textInLineFormatBundle,
                                 "${transactionType2Name(k).toUpperCase(Locale.ROOT)}${"     =" + m.count}",
                                 "Rs.     ${"%.2f".format(((m.total).toDouble() / 100))}"

                             )*/


                                textBlockList.put(AlignMode.LEFT, transactionType2Name(k).toUpperCase(Locale.ROOT))
                                textBlockList.put(AlignMode.CENTER, "=" + m.count)
                                textBlockList.put(AlignMode.RIGHT, getCurrencySymbol(tpt)+"  "+"%.2f".format((((m.total).toDouble()).div(100)).toString().toDouble()))

                                printToalTxnList(textBlockList)
                                textBlockList.clear()

                            }

                            if (iteration > 0) {
                                printSeperator()

                                // region print logo when hostBankId = 7
                                val bankId = batch[frequency].hostBankID
                                val logo = if (bankId == AMEX_BANK_CODE_SINGLE_DIGIT || bankId == AMEX_BANK_CODE) {
                                    AMEX_LOGO
                                } else {
                                    ""
                                    //HDFC_LOGO
                                }

                                if (isFirstTimeForAmxLogo && logo != null && !logo.equals("")) {
                                    isFirstTimeForAmxLogo = false
                                    printLogo(logo)
                                    printSeperator()
                                }
                                // end region

                                textBlockList.put( AlignMode.LEFT,"MID:${batch[frequency].hostMID}")
                                textBlockList.put(AlignMode.RIGHT,"TID:${batch[frequency].hostTID}")
                                mixStyleTextPrint(textBlockList)
                                textBlockList.clear()
                                textBlockList.put( AlignMode.LEFT,"BATCH NO:${batch[frequency].hostBatchNumber}")
                                textBlockList.put( AlignMode.RIGHT," ")
                                mixStyleTextPrint(textBlockList)
                                textBlockList.clear()
                                printSeperator()

                                iteration--
                            }

                            totalMap.clear()
                        }
                    }

                }
                // region === Below code is execute when digi txns are available on POS
                val digiPosDataList = Field48ResponseTimestamp.selectDigiPosDataAccordingToTxnStatus(
                    EDigiPosPaymentStatus.Approved.desciption
                ) as ArrayList<DigiPosDataTable>

                if (digiPosDataList.isNotEmpty()) {
                    printSeperator()
                    // centerText(textFormatBundle, "---------X-----------X----------")
                    sigleLineText("Digi Pos Detail Report", AlignMode.CENTER)
                    tpt?.terminalId?.let { sigleLineText( "TID : $it",AlignMode.CENTER) }
                    printSeperator()
                    // Txn description
                    textBlockList.put(AlignMode.LEFT, "MODE")
                    textBlockList.put(AlignMode.RIGHT, "AMOUNT(INR)")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()
                    textBlockList.put(AlignMode.LEFT, "PartnetTxnId")
                    textBlockList.put(AlignMode.RIGHT,"DATE-TIME")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()
                    textBlockList.put(AlignMode.LEFT, "mTxnId")
                    textBlockList.put(AlignMode.RIGHT, "pgwTxnId")
                    mixStyleTextPrint(textBlockList)
                    textBlockList.clear()
                    printSeperator()
                    //Txn Detail
                    for (digiPosData in digiPosDataList) {

                        textBlockList.put(AlignMode.LEFT, digiPosData.paymentMode)
                        textBlockList.put(AlignMode.RIGHT, digiPosData.amount)
                        mixStyleTextPrint(textBlockList)
                        textBlockList.clear()
                        textBlockList.put(AlignMode.LEFT, digiPosData.partnerTxnId)
                        textBlockList.put(AlignMode.RIGHT, digiPosData.txnDate + "  " + digiPosData.txnTime)
                        mixStyleTextPrint(textBlockList)
                        textBlockList.clear()

                        textBlockList.put(AlignMode.LEFT, digiPosData.mTxnId)
                        textBlockList.put(AlignMode.RIGHT, digiPosData.pgwTxnId)
                        mixStyleTextPrint(textBlockList)
                        textBlockList.clear()

                        printSeperator()
                    }
                    //   DigiPosDataTable.deletAllRecordAccToTxnStatus(EDigiPosPaymentStatus.Approved.desciption)
                }
                //endregion
                if (batch.isNotEmpty()) {
                    printSeperator()
                    sigleLineText("Bonushub", AlignMode.CENTER)
                    sigleLineText("App Version:${BuildConfig.VERSION_NAME}", AlignMode.CENTER)

                }

                // start print here
                vectorPrinter!!.startPrint(object : OnPrintListener.Stub() {
                    @Throws(RemoteException::class)
                    override fun onFinish() {
                        // outputText("=> onFinish | sheetNo = $curSheetNo")
                        // println("time cost = + " + (System.currentTimeMillis() - startTime))
                        println("0onFinish")
                        printCB(true)
                    }

                    @Throws(RemoteException::class)
                    override fun onStart() {
                        //  outputText("=> onStart | sheetNo = $curSheetNo")
                        println("0onStart")
                    }

                    @Throws(RemoteException::class)
                    override fun onError(error: Int, errorMsg: String) {
                        //  outputRedText("=> onError: $errorMsg")
                        println("0onError")
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
}

fun checkForPrintReversalReceipt(
    context: Context?,
    field60Data: String,
    callback: (String) -> Unit
) {
    if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
        val tpt = getTptData()
        tpt?.canceledTransactionReceiptPrint?.let { logger("CancelPrinting", it, "e") }
        if (tpt?.canceledTransactionReceiptPrint == "01") {
            AppPreference.saveString(AppPreference.FIELD_60_DATA_REVERSAL_KEY, field60Data)
            PrintVectorUtil(context).printReversal(context, field60Data) {
                callback(it)
            }
        } else {
            callback("")
        }
    } else {
        callback("")
    }
}