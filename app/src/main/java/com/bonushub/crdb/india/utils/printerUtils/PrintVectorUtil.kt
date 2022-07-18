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
import com.bonushub.crdb.india.model.local.AppPreference
import com.bonushub.crdb.india.model.local.HDFCTpt
import com.bonushub.crdb.india.model.local.TempBatchFileDataTable
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getTptData
import com.bonushub.crdb.india.vxutils.BhTransactionType
import com.bonushub.crdb.india.vxutils.failureImpl
import com.bonushub.crdb.india.vxutils.getTransactionTypeName
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

    private val textBlockList: HashMap<Int,String> = HashMap()
    private val textFormatGlobal = Bundle()


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

                    /*
                    int[] weights = {3, 3, 3, 3};
        int[] aligns = {Alignment.NORMAL, Alignment.OPPOSITE, Alignment.NORMAL, Alignment.OPPOSITE};
        vectorPrinter.addTextColumns(null,
                new String[]{"凭证号:", "002134", "授权码", "118525"}, weights, aligns);
                    AlignMode.LEFT*/
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

                textBlockList.put(AlignMode.LEFT , "INVOICE:${hostInvoice}")
                mixStyleTextPrint(textBlockList)
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
                    sigleLineText("TRANSACTION FAILED", AlignMode.CENTER)
                } else {
                    if(isNoEmiOnlyCashBackAppl) {
                        sigleLineText("SALE", AlignMode.CENTER, TextSize.NORMAL)
                    }
                    else{
                        getTransactionTypeName(batchTable.transactionType)?.let { sigleLineText(it, AlignMode.CENTER, TextSize.NORMAL) }
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
                       // printEMISale(batchTable) // TODO

                    }
                    BhTransactionType.REVERSAL.type -> {
                        val amt = (((batchTable.baseAmmount)?.toLong())?.div(100)).toString()
                        textBlockList.put(AlignMode.LEFT, "TOTAL AMOUNT:")
                        textBlockList.put(AlignMode.RIGHT, "INR:${"%.2f".format(amt.toDouble())}")
                        mixStyleTextPrint(textBlockList)
                        textBlockList.clear()

                    }
                    BhTransactionType.PRE_AUTH_COMPLETE.type ->{
                       // preAuthCompleteTransaction(batchTable) // TODO
                    }
                    BhTransactionType.VOID_PREAUTH.type ->{
                        // TODO preAuthCompleteTransaction(batchTable)
                    }
                    else -> {
                        // TODO voidTransaction(batchTable)

                    }

                }
                printSeperator()
                //region=====================BRAND TAndC===============
                if (batchTable.transactionType == BhTransactionType.EMI_SALE.type ||batchTable.transactionType == BhTransactionType.TEST_EMI.type||batchTable.transactionType == BhTransactionType.BRAND_EMI.type) {
                    // TODO if(!isNoEmiOnlyCashBackApplied!!)
                    // TODO  printBrandTnC(batchTable)

                }
                //region=====================BRAND PRODUACT DATA===============
                if (batchTable.transactionType == BhTransactionType.BRAND_EMI.type) {
                    // TODO  printProduactData(batchTable)
                    printSeperator()
                    // TODO  baseAmounthandling(batchTable)
                }

                if(isReversal){
                    sigleLineText("Please contact your card issuer for reversal of debit if any\n", AlignMode.CENTER)
                    sigleLineText(footerText[1], AlignMode.CENTER)
                }else {

                    //printer?.setAscScale(ASCScale.SC1x2) // TODO
                    //printer?.setAscSize(ASCSize.DOT16x8) // TODO

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
                        }


                        batchTable.cardHolderName?.let { sigleLineText(it, AlignMode.CENTER, TextSize.NORMAL) }
                    }

                    // TODO printer?.setAscScale(ASCScale.SC1x1)
                    // TODO printer?.setAscSize(ASCSize.DOT24x8)

                    try {
                        val issuerParameterTable =
                            Field48ResponseTimestamp.getIssuerData(AppPreference.WALLET_ISSUER_ID)

                        var dec = issuerParameterTable?.walletIssuerDisclaimer

                        logger("dec", dec ?: "")
                        /*textBlockList.put(AlignMode.CENTER, dec ?: "")

                        mixStyleTextPrint(textBlockList)
                        textBlockList.clear()*/
                        sigleLineText(dec?:"",AlignMode.CENTER)

                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }

                    sigleLineText(copyType.pName, AlignMode.CENTER)
                    sigleLineText("", AlignMode.CENTER,TextSize.TINY)
                    sigleLineText(footerText[0], AlignMode.CENTER)
                    sigleLineText(footerText[1], AlignMode.CENTER)
                }

//                val bhlogo: ByteArray? = context?.let { printLogo(it, "BH.bmp") }
//                printer?.addBmpImage(0, FactorMode.BMP1X1, bhlogo)

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
                                            // TODO val chunks: List<String> = chunkTnC(emiTnc, limit)
                                            // TODO for (st in chunks) {
                                                logger("TNC", "st", "e")
                                                /*textBlockList.add(
                                                    sigleLineformat(
                                                        st, AlignMode.LEFT
                                                    )
                                                )*/ // TODO
//                                            printer?.setHzScale(HZScale.SC1x1)
//                                            printer?.setHzSize(HZSize.DOT24x16)
//                                                printer?.setAscScale(ASCScale.SC1x1) // TODO
//                                                printer?.setAscSize(ASCSize.DOT24x8) // TODO
//                                                printer?.addText( AlignMode.LEFT, st) // TODO
                                            // TODO }
                                            //printer?.setAscSize(ASCSize.DOT24x12) // TODO
                                        }
                                    }
                                } else {
                                    /*textBlockList.add(
                                        sigleLineformat(
                                            "# ${issuerTAndCData.footerTAndC}", AlignMode.LEFT
                                        )
                                    )*/ // TODO
//                                printer?.setHzScale(HZScale.SC1x1)
//                                printer?.setHzSize(HZSize.DOT24x16)
//                                    printer?.setAscScale(ASCScale.SC1x1)// TODO
//                                    printer?.setAscSize(ASCSize.DOT24x8) // TODO
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
            if (null != hdfcTpt && hdfcTpt?.receiptL2?.isNotBlank() ?: false && hdfcTpt?.receiptL2?.isNotEmpty() ?: true) {
                hdfcTpt?.receiptL2?.let { sigleLineText(it ?: "".trim(), AlignMode.CENTER, TextSize.NORMAL, false) }
            } else {
                tpt?.receiptHeaderTwo?.let { sigleLineText(it ?: "".trim(), AlignMode.CENTER, TextSize.NORMAL, false) }
            }
            if (null != hdfcTpt && hdfcTpt?.receiptL3?.isNotBlank() ?: false && hdfcTpt?.receiptL3?.isNotEmpty() ?: true) {
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
        textFormat.putBoolean(VectorPrinterData.BOLD, isBold)

        vectorPrinter!!.addText(textFormat, text)
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

           // val  weights = intArrayOf(1,1)
            val  weights = intArrayOf(leftW,rightW)
            val aligns = intArrayOf(AlignMode.LEFT, AlignMode.RIGHT)
            vectorPrinter!!.addTextColumns(
                textFormat,
                arrayOf(data.get(AlignMode.LEFT)?:"", data.get(AlignMode.RIGHT)?:""),
                weights,
                aligns
            )

        }else{
            val weights = intArrayOf((data.get(AlignMode.LEFT)?:"".trim()).length, (data.get(AlignMode.CENTER)?:"".trim()).length, (data.get(AlignMode.RIGHT)?:"".trim()).length)
            val aligns = intArrayOf(AlignMode.LEFT, AlignMode.CENTER, AlignMode.RIGHT)
            vectorPrinter!!.addTextColumns(
                textFormat,
                arrayOf(data.get(AlignMode.LEFT)?:"",data.get(AlignMode.CENTER)?:"", data.get(AlignMode.RIGHT)?:""),
                weights,
                aligns
            )
        }

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
//            textBlockList.put(AlignMode.LEFT, "SALE AMOUNT:")
//            textBlockList.put(AlignMode.RIGHT, "$currencySymbol :${"%.2f".format(amt.toDouble())}")
//            mixStyleTextPrint(textBlockList,TextSize.NORMAL, false)
//            textBlockList.clear()
            sigleLineText("SALE AMOUNT",AlignMode.CENTER,TextSize.NORMAL, false)
            sigleLineText("$currencySymbol :${"%.2f".format(amt.toDouble())}",AlignMode.CENTER,TextSize.NORMAL, false)
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
                mixStyleTextPrint(textBlockList,TextSize.NORMAL, false)
                textBlockList.clear()
                totalAmount = "%.2f".format((amt.toDouble())).toString()
            }else{
                textBlockList.put(AlignMode.LEFT, "CASH WITHDRAWN AMT:")
                textBlockList.put(AlignMode.RIGHT, "$currencySymbol :${"%.2f".format(amt.toDouble())}")
                mixStyleTextPrint(textBlockList,TextSize.NORMAL, false)
                textBlockList.clear()
                totalAmount = "%.2f".format((amt.toDouble()))
            }

        }
        // textBlockList.add(sigleLineformat( "00",AlignMode.RIGHT))


//        textBlockList.put(AlignMode.LEFT, "TOTAL AMOUNT:")
//        textBlockList.put(AlignMode.RIGHT, "$currencySymbol :${totalAmount}")
//        mixStyleTextPrint(textBlockList,TextSize.NORMAL, false)
//        textBlockList.clear()
    }
}