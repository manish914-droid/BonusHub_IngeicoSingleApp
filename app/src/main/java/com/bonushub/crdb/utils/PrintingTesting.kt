package com.bonushub.crdb.utils

import android.content.Context
import android.os.Bundle
import android.os.DeadObjectException
import android.os.RemoteException
import android.util.Log

import com.bonushub.crdb.view.base.BaseActivityNew
import com.bonushub.pax.utils.VxEvent
import com.ingenico.hdfcpayment.model.ReceiptDetail
import com.usdk.apiservice.aidl.printer.*
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.drawable.Drawable

import android.graphics.Bitmap
import android.graphics.Canvas

import android.graphics.drawable.BitmapDrawable
import java.io.ByteArrayOutputStream


class PrintingTesting: BaseActivityNew(){
    private var printer: UPrinter? = null
    private val sheetNum = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initDeviceInstance()
        startPrinting(ReceiptDetail())
    }
    // Printing Sale Charge slip....
    fun startPrinting(receiptDetail: ReceiptDetail) {
        //  printer=null
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
            fmtAddTextInLine.putString(PrinterData.TEXT, "DATE:${receiptDetail.dateTime?.let {
                dateFormater(
                    it.toLong())
            }}")
            fmtAddTextInLine.putInt(PrinterData.ALIGN_MODE, AlignMode.LEFT)
            textBlockList.add(fmtAddTextInLine)
            try {
                fmtAddTextInLine.putString(PrinterData.TEXT, "TIME:${receiptDetail.dateTime?.let {
                    timeFormater(
                        it.toLong())
                }}")
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
            printer!!.setHzScale(HZScale.SC1x1)
            printer!!.setHzSize(HZSize.DOT24x24)
            printer!!.addText(AlignMode.LEFT, "INVOICE:${receiptDetail.invoice}")
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
            printer!!.addText(AlignMode.CENTER, receiptDetail.cardHolderName)


            printer!!.setHzScale(HZScale.SC1x1)
            printer!!.setHzSize(HZSize.DOT24x24)
            printer!!.addText(AlignMode.LEFT, "I am satisfied with goods recived and agree to pay issuer agreenent.")


            printer!!.setHzScale(HZScale.SC1x1)
            printer!!.setHzSize(HZSize.DOT24x24)
            printer!!.addText(AlignMode.CENTER, "*thank yoy visit again*")

            printer!!.setHzScale(HZScale.SC1x1)
            printer!!.setHzSize(HZSize.DOT24x24)
            printer!!.addText(AlignMode.CENTER, "SANDEEP SARASWAT")
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
    override fun onEvents(event: VxEvent) {
        TODO("Not yet implemented")
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

    fun getStatus() {
        ToastUtils.showToast(this,">>> getStatus")
        try {
            val status = printer!!.status
            if (status != PrinterError.SUCCESS) {
              //  ToastUtils.showToast(this,getErrorDetail(status))
                return
            }
            ToastUtils.showToast(this,"The printer status is normal!")
        } catch (e: Exception) {
           // handleException(e)
        }
    }
    protected fun initDeviceInstance() {
        printer = DeviceHelper.getPrinter()
    }
    fun scaleSize(bundal : Bundle){
        bundal.putInt(PrinterData.ASC_SCALE, ASCScale.SC1x1)
        bundal.putInt(PrinterData.ASC_SIZE, ASCSize.DOT24x8)
    }

    fun dateFormater(date: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)

    fun timeFormater(date: Long): String =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(date)
}