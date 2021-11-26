package com.bonushub.crdb.utils.printerUtils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.DeadObjectException
import android.os.RemoteException
import android.util.Log
import com.bonushub.crdb.utils.DeviceHelper
import com.bonushub.crdb.utils.Field48ResponseTimestamp.getTptData
import com.bonushub.crdb.utils.failureImpl
import com.bonushub.crdb.utils.logger
import com.bonushub.pax.utils.EPrintCopyType
import com.ingenico.hdfcpayment.model.ReceiptDetail
import com.usdk.apiservice.aidl.printer.*
import java.io.IOException
import java.io.InputStream
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class PrintUtil(context: Context?) {
    private var printer: UPrinter? = null
    private var isTipAllowed = false
    private var context: Context? = null
    private var footerText = arrayOf("*Thank You Visit Again*", "POWERED BY")

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
    fun startPrinting(receiptDetail: ReceiptDetail, copyType: EPrintCopyType,
                      context: Context?,
                      printerCallback: (Boolean, Int) -> Unit) {
        try {
            val image: ByteArray? = context?.let { printLogo(it, "hdfc_print_logo.bmp") }
            printer?.addBmpImage(0, FactorMode.BMP1X1, image)
            val textBlockList: ArrayList<Bundle> = ArrayList()
            try {
                receiptDetail.merAddHeader1?.let { sigleLineText(it,AlignMode.CENTER) }
                receiptDetail.merAddHeader2?.let { sigleLineText(it,AlignMode.CENTER) }
                val date = receiptDetail.dateTime
                val parts = date?.split(" ")
                println("Date: " + parts!![0])
                println("Time: " + (parts?.get(1)) )

                textBlockList.add( sigleLineformat( "DATE:${parts?.get(0)}",AlignMode.LEFT))
              textBlockList.add(sigleLineformat( "TIME:${(parts?.get(1))}",AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

                textBlockList.add( sigleLineformat( "MID:${receiptDetail.mid}",AlignMode.LEFT))
                textBlockList.add(sigleLineformat( "TID:${receiptDetail.tid}",AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

                textBlockList.add( sigleLineformat( "BATCH NO:${receiptDetail.batchNumber}",AlignMode.LEFT))
                textBlockList.add(sigleLineformat( "ROC:${receiptDetail.stan}",AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

                textBlockList.add( sigleLineformat( "INVOICE:${receiptDetail.invoice}",AlignMode.LEFT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

                receiptDetail.txnName?.let { sigleLineText(it,AlignMode.CENTER) }

                textBlockList.add( sigleLineformat( "CARD TYPE:${receiptDetail.appName}",AlignMode.LEFT))
                textBlockList.add(sigleLineformat( "EXP:XX/XX",AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)

                textBlockList.clear()


                textBlockList.add( sigleLineformat(  "CARD NO:${"00000gl3790"}",AlignMode.LEFT))
                textBlockList.add(sigleLineformat( "Chip",AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)

                textBlockList.clear()


                textBlockList.add( sigleLineformat(  "AUTH CODE:${receiptDetail.authCode}",AlignMode.LEFT))
                textBlockList.add(sigleLineformat( "RRN:${receiptDetail.rrn}",AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)

                textBlockList.clear()

                textBlockList.add( sigleLineformat(  "TVR:${receiptDetail.tvr}",AlignMode.LEFT))
                textBlockList.add(sigleLineformat( "TSI:${receiptDetail.tsi}",AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)

                textBlockList.clear()

                sigleLineText("-----------------------------------------",AlignMode.CENTER)

                textBlockList.add( sigleLineformat(  "SALE AMOUNT:${receiptDetail.authCode}",AlignMode.LEFT))
                textBlockList.add(sigleLineformat( "INR:${receiptDetail.txnAmount}",AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)

                textBlockList.clear()


                textBlockList.add( sigleLineformat(  "TIP AMOUNT    :    .............",AlignMode.LEFT))
               // textBlockList.add(sigleLineformat( "00",AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()

                textBlockList.add( sigleLineformat(  "TOTAL AMOUNT:",AlignMode.LEFT))
                textBlockList.add(sigleLineformat( "INR:${receiptDetail.txnAmount}",AlignMode.RIGHT))
                printer?.addMixStyleText(textBlockList)
                textBlockList.clear()
                sigleLineText("-----------------------------------------",AlignMode.CENTER)
                if(receiptDetail.isSignRequired == true)
                    sigleLineText("PIN VERIFIDE OK",AlignMode.CENTER)
                if(receiptDetail.isSignRequired != true)
                    sigleLineText("SIGNATURE NOT REQUIRED",AlignMode.CENTER)
                receiptDetail.cardHolderName?.let { sigleLineText(it,AlignMode.CENTER) }
                sigleLineText("I am satisfied with goods recived and agree to pay issuer agreenent.",AlignMode.CENTER)
                sigleLineText(copyType.pName,AlignMode.CENTER)
                sigleLineText(footerText[0],AlignMode.CENTER)
                sigleLineText(footerText[1],AlignMode.CENTER)
                val bhlogo: ByteArray? = context?.let { printLogo(it, "BH.bmp") }
                printer?.addBmpImage(0, FactorMode.BMP1X1, bhlogo)

            } catch (e: ParseException) {
                e.printStackTrace()
            }
            printer?.setPrnGray(3)
            printer?.feedLine(5)
            printer?.startPrint(object : OnPrintListener.Stub() {
                @Throws(RemoteException::class)
                override fun onFinish() {
                    printerCallback(true,0)
                }
                @Throws(RemoteException::class)
                override fun onError(error: Int) {
                    printerCallback(true,0)
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
    private fun sigleLineformat(text : String, alignMode: Int):Bundle{
        val format = Bundle()
        format.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
        format.putInt(PrinterData.ASC_SIZE, ASCSize.DOT24x8)
        format.putString(PrinterData.TEXT, text)
        format.putInt(PrinterData.ALIGN_MODE, alignMode)
        return format
    }

    // using this you can set your single text 
    private fun sigleLineText(text: String,alignMode: Int) {
        printer?.setHzScale(HZScale.SC1x1)
        printer?.setHzSize(HZSize.DOT24x24)
        printer?.addText(alignMode, text)
    }


}
