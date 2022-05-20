package com.bonushub.crdb.india.view.base

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils

import android.view.*
import android.webkit.WebView
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bonushub.crdb.india.HDFCApplication
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.*
import com.bonushub.crdb.india.model.local.DigiPosDataTable
import com.bonushub.crdb.india.utils.printerUtils.PrintUtil
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.utils.EPrintCopyType

abstract class BaseActivityNew : AppCompatActivity(), IDialog {

    private lateinit var progressDialog: Dialog
    lateinit var progressTitleMsg: TextView
    lateinit var progressPercent:ProgressBar
    lateinit var progressPercentTv:TextView
    lateinit var horizontalPLL:LinearLayout
    lateinit var verticalProgressBar: WebView


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setProgressDialog()

    }

    override fun showToast(msg: String) {
        Toast(HDFCApplication.appContext).apply {
            setGravity(Gravity.NO_GRAVITY, 0, 0)
            duration = Toast.LENGTH_LONG
            val vi = (layoutInflater.inflate(R.layout.custom_toast, null) as TextView)
            vi.text = msg
            view = vi

        }.show()
    }

    private fun setProgressDialog() {
        progressDialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
           // setContentView(R.layout.new_tem_progress_dialog) // old
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setContentView(R.layout.new_progress_dialog)
            setCancelable(false)
        }
        progressTitleMsg = progressDialog.findViewById(R.id.msg_et)
        progressPercent=progressDialog.findViewById(R.id.pBar)
        progressPercentTv=progressDialog.findViewById(R.id.downloadPercentTv)
        horizontalPLL=progressDialog.findViewById(R.id.horizontalProgressLL)
        verticalProgressBar=progressDialog.findViewById(R.id.verticalProgressbr)
        verticalProgressBar?.loadUrl("file:///android_asset/loader.html")

    }

    override fun showProgress(progressMsg: String) {
        if (!progressDialog.isShowing && !(this as Activity).isFinishing) {
            progressTitleMsg.text = progressMsg
            progressDialog.show()
        }
    }

    override fun updatePercentProgress(percent: Int) {
        progressPercent.visibility=View.VISIBLE
        progressPercentTv.visibility=View.VISIBLE
        val downloadPercent= "$percent %"
        progressPercent.progress = percent
        progressPercentTv.text=downloadPercent

    }

    override fun showPercentDialog(progressMsg: String) {
        if (!progressDialog.isShowing && !(this as Activity).isFinishing) {
            progressTitleMsg.text = progressMsg
            horizontalPLL.visibility=View.VISIBLE
            verticalProgressBar.visibility=View.GONE
            progressTitleMsg .text=progressMsg
            progressDialog.show()
        }
    }



    override fun hideProgress() {
        if (progressDialog.isShowing && !(this as Activity).isFinishing) {
            progressDialog.dismiss()
            horizontalPLL.visibility=View.GONE
            verticalProgressBar.visibility=View.VISIBLE
        }
    }

    override fun setProgressTitle(title: String) {
        progressTitleMsg.text = title
    }


    override fun getInfoDialog(title: String, msg: String, icon: Int,acceptCb: () -> Unit) {
            /*val dialog = Dialog(this)
            dialog.apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(R.layout.msg_dialog)
                setCancelable(false)
                window?.attributes?.windowAnimations = R.style.DialogAnimation
                val window = dialog.window
                window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
                findViewById<ImageView>(R.id.img_header).setImageResource(icon)
                findViewById<TextView>(R.id.msg_dialog_title).text = title
                findViewById<TextView>(R.id.msg_dialog_msg).text = msg

                with(findViewById<View>(R.id.msg_dialog_ok)) {
                    setOnClickListener {
                        dismiss()
                        acceptCb()
                    }
                }
                findViewById<View>(R.id.msg_dialog_cancel).visibility = View.GONE
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }.show()*/

        alertBoxWithActionNew(
            title,
            msg,
            icon,
            getString(R.string.positive_button_ok),
            "",false,false,
            { acceptCb() },
            {})


    }

    override fun getInfoDialogdoubletap(title: String, msg: String, acceptCb: (Boolean, Dialog) -> Unit) {
        val dialog = Dialog(this)
        dialog.apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.msg_dialog)
            setCancelable(false)

            dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
            val window = dialog.window
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )

            findViewById<TextView>(R.id.msg_dialog_title).text = title
            findViewById<TextView>(R.id.msg_dialog_msg).text = msg
            findViewById<TextView>(R.id.msg_dialog_ok).visibility = View.INVISIBLE

            with(findViewById<View>(R.id.msg_dialog_ok)) {
                View.GONE
                Handler(Looper.getMainLooper()).postDelayed({
                    acceptCb(true,dialog)
                }, 500)

            }
            findViewById<View>(R.id.msg_dialog_cancel).visibility = View.INVISIBLE
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }.show()


    }

    override fun getMsgDialog(
        title: String,
        msg: String,
        positiveTxt: String,
        negativeTxt: String,
        positiveAction: () -> Unit,
        negativeAction: () -> Unit,
        isCancellable: Boolean
    ) {

        val dialog = Dialog(this)
        dialog.apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.msg_dialog)
            setCancelable(isCancellable)
            window?.attributes?.windowAnimations = R.style.DialogAnimation
            val window = dialog.window
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            findViewById<TextView>(R.id.msg_dialog_title).text = title
            findViewById<TextView>(R.id.msg_dialog_msg).text = msg

            with(findViewById<TextView>(R.id.msg_dialog_ok)) {
                text = positiveTxt
                setOnClickListener {
                    dismiss()
                    positiveAction()
                }
            }

            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            findViewById<TextView>(R.id.msg_dialog_cancel).apply {
                text = negativeTxt
                setOnClickListener {
                    dismiss()
                    negativeAction()
                }
            }
        }.show()

    }

    override fun alertBoxWithAction(
        title: String, msg: String, showCancelButton: Boolean,
        positiveButtonText: String, alertCallback: (Boolean) -> Unit,
        cancelButtonCallback: (Boolean) -> Unit, dialogIcon:Int
    ) {

        var icon = if(title==getString(R.string.print_customer_copy)){
            R.drawable.ic_print_customer_copy
        }else{
            R.drawable.ic_info
        }

        if (TextUtils.isEmpty(positiveButtonText)) {
            alertBoxWithActionNew(
                title,
                msg,
                icon,
                positiveButtonText,
                "No",showCancelButton,false,
                { alertCallback (it)},
                { cancelButtonCallback (it)})
        }else{
            alertBoxWithActionNew(
                title,
                msg,
                icon,
                positiveButtonText,
                "No",showCancelButton,true,
                { alertCallback (it)},
                { cancelButtonCallback (it)})
        }
       /* val dialogBuilder = Dialog(this)
        //builder.setTitle(title)
        //  builder.setMessage(msg)
        val bindingg = NewPrintCustomerCopyBinding.inflate(LayoutInflater.from(this))

        dialogBuilder.setContentView(bindingg.root)
        if(dialogIcon == 0){
            if (title == getString(R.string.print_customer_copy)|| title == getString(R.string.sms_upi_pay)|| title == getString(R.string.no_receipt)) {
                if(title==getString(R.string.sms_upi_pay)){
                    bindingg.imgPrinter.setImageResource(R.drawable.ic_link_icon)
                } else if(title==getString(R.string.print_customer_copy)){
                    bindingg.imgPrinter.setImageResource(R.drawable.ic_printer)
                } else if(title==getString(R.string.no_receipt)){
                    bindingg.imgPrinter.setImageResource(R.drawable.ic_info)
                }
                bindingg.imgPrinter.visibility = View.VISIBLE
            } else {
                // bindingg.imgPrinter.visibility = View.GONE
            }
        }else{
            bindingg.imgPrinter.setImageResource(dialogIcon)
        }

        if(positiveButtonText==""){
            bindingg.yesBtn.visibility=View.GONE

        }
        dialogBuilder.setCancelable(false)
        val window = dialogBuilder.window
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        bindingg.yesBtn.text = positiveButtonText
        bindingg.dialogMsg.text = msg

        bindingg.yesBtn.setOnClickListener {
            dialogBuilder.dismiss()
            alertCallback(true)
        }
        //Below condition check is to show Cancel Button in Alert Dialog on condition base:-
        if (showCancelButton) {
            bindingg.noBtn.setOnClickListener {
                dialogBuilder.cancel()
                cancelButtonCallback(true)
            }
        } else {
            //bindingg.imgPrinter.visibility = View.GONE
            bindingg.noBtn.visibility = View.GONE
        }
        //     val alert: androidx.appcompat.app.AlertDialog = dialogBuilder.create()
        //Below Handler will execute to auto cancel Alert Dialog Pop-Up when positiveButtonText isEmpty:-
        if (TextUtils.isEmpty(positiveButtonText)) {
            Handler(Looper.getMainLooper()).postDelayed({
                dialogBuilder.dismiss()
                dialogBuilder.cancel()
                startActivity(Intent(this, NavigationActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            }, 2000)
        }

        try {
            if (!dialogBuilder.isShowing) {
                dialogBuilder.show()
            }
        } catch (ex: WindowManager.BadTokenException) {
            ex.printStackTrace()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        dialogBuilder.show()
        dialogBuilder.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
*/
    }


    override fun alertBoxWithActionNew(header:String, msg:String, icon: Int, positiveButtonText: String, negativeButtonText:String, isShowNegativeBtn:Boolean,
                                       isAutoCancel:Boolean, yesButtonCallback: (Boolean) -> Unit, noButtonCallback: (Boolean) -> Unit) {
        val dialogBuilder = Dialog(this)
        //builder.setTitle(title)
        //  builder.setMessage(msg)
        val bindingg = DialogAlertMsgNewBinding.inflate(LayoutInflater.from(this))

       // dialogBuilder.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogBuilder.setContentView(bindingg.root)
        dialogBuilder.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogBuilder.setCancelable(false)
        val window = dialogBuilder.window
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )


        if(header.isNotEmpty()) {
            bindingg?.txtViewHeading.text = header
            bindingg?.txtViewHeading.visibility = View.VISIBLE
        }

        if(icon != 0) {
            bindingg?.imgViewDialog.setImageResource(icon)
            bindingg?.imgViewDialog.visibility = View.VISIBLE
        }


        if(msg.isNotEmpty()) {
            bindingg?.txtViewMsg.text = msg
            bindingg?.txtViewMsg.visibility = View.VISIBLE
        }

        if(isShowNegativeBtn) {
            bindingg?.txtViewNo.text = negativeButtonText
            bindingg?.txtViewNo.visibility = View.VISIBLE
        }

        if(positiveButtonText.isNotEmpty()) {
            bindingg?.txtViewYes.text = positiveButtonText
            bindingg?.txtViewYes.visibility = View.VISIBLE
        }
        //bindingg.txtViewYes.text = positiveButtonText

        bindingg?.txtViewNo?.setOnClickListener {
            dialogBuilder.dismiss()
            noButtonCallback(true)
        }

        bindingg?.txtViewYes?.setOnClickListener {
            dialogBuilder.dismiss()
            yesButtonCallback(true)
        }

        if(isAutoCancel)
        {
            Handler(Looper.getMainLooper()).postDelayed({
                dialogBuilder.dismiss()
                dialogBuilder.cancel()
                /*startActivity(Intent(this, NavigationActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })*/
            }, 2000)
        }

       /* bindingg.yesBtn.text = positiveButtonText
        bindingg.dialogMsg.text = msg

        bindingg.yesBtn.setOnClickListener {
            dialogBuilder.dismiss()
            alertCallback(true)
        }
        //Below condition check is to show Cancel Button in Alert Dialog on condition base:-
        if (showCancelButton) {
            bindingg.noBtn.setOnClickListener {
                dialogBuilder.cancel()
                cancelButtonCallback(true)
            }
        } else {
            //bindingg.imgPrinter.visibility = View.GONE
            bindingg.noBtn.visibility = View.GONE
        }
        //     val alert: androidx.appcompat.app.AlertDialog = dialogBuilder.create()
        //Below Handler will execute to auto cancel Alert Dialog Pop-Up when positiveButtonText isEmpty:-
        if (TextUtils.isEmpty(positiveButtonText)) {
            Handler(Looper.getMainLooper()).postDelayed({
                dialogBuilder.dismiss()
                dialogBuilder.cancel()
                startActivity(Intent(this, NavigationActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            }, 2000)
        }*/

        try {
            if (!dialogBuilder.isShowing) {
                dialogBuilder.show()
            }
        } catch (ex: WindowManager.BadTokenException) {
            ex.printStackTrace()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        dialogBuilder.show()
        dialogBuilder.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }


    override fun txnApprovedDialog(
        headerImage:Int,headerText:String, amount: String, dateTime: String, alertCallback: (Boolean) -> Unit
    ) {
        val dialogBuilder = Dialog(this,android.R.style.ThemeOverlay_Material_ActionBar)
        //builder.setTitle(title)
        //  builder.setMessage(msg)
        val bindingg = DialogTxnApprovedBinding.inflate(LayoutInflater.from(this))

        dialogBuilder.setContentView(bindingg.root)

        if(headerImage != 0) {
            bindingg.subHeaderView?.headerImage.setImageResource(headerImage)
        }

        bindingg.subHeaderView?.subHeaderText.text = headerText
        bindingg.txtViewAmount.text = amount
        bindingg.txtViewDateTime.text = dateTime

        dialogBuilder.setCancelable(false)
        val window = dialogBuilder.window
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )


        Handler(Looper.getMainLooper()).postDelayed({
            dialogBuilder.dismiss()
            dialogBuilder.cancel()

            alertCallback(true)
                /*startActivity(Intent(this, NavigationActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })*/

                                                    }, 2000)


        try {
            if (!dialogBuilder.isShowing) {
                dialogBuilder.show()
            }
        } catch (ex: WindowManager.BadTokenException) {
            ex.printStackTrace()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        dialogBuilder.show()
        dialogBuilder.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    override fun alertBoxMsgWithIconOnly(
        icon: Int, msg: String
    ) {

        alertBoxWithActionNew(
            "",
            msg,
            icon,
            "",
            "",false,true,
            {},
            {})

       /* val dialogBuilder = Dialog(this)
        //builder.setTitle(title)
        //  builder.setMessage(msg)
        val bindingg = DialogMsgWithIconBinding.inflate(LayoutInflater.from(this))

        dialogBuilder.setContentView(bindingg.root)

        bindingg.imgHeader.setImageResource(icon)
        bindingg.txtViewMsg.text = msg

        dialogBuilder.setCancelable(false)
        val window = dialogBuilder.window
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        //Below Handler will execute to auto cancel Alert Dialog Pop-Up

            Handler(Looper.getMainLooper()).postDelayed({
                dialogBuilder.dismiss()
                dialogBuilder.cancel()
               if(msg.equals(getString(R.string.successfull_init))){
                startActivity(Intent(this, NavigationActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                }
            }, 2000)

        try {
            if (!dialogBuilder.isShowing) {
                dialogBuilder.show()
            }
        } catch (ex: WindowManager.BadTokenException) {
            ex.printStackTrace()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        dialogBuilder.show()
        dialogBuilder.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))*/
    }


    override fun alertBoxWithOnlyOk(
        title: String, msg: String, showCancelButton: Boolean,
        positiveButtonText: String, alertCallback: (Boolean) -> Unit,
        cancelButtonCallback: (Boolean) -> Unit
    ) {

        val dialogBuilder = Dialog(this)
        //  builder.setTitle(title)
        //  builder.setMessage(msg)

        val bindingg = ItemOkBtnDialogBinding.inflate(LayoutInflater.from(this))

        dialogBuilder.setContentView(bindingg.root)

        dialogBuilder.setCancelable(false)
        val window = dialogBuilder.window
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )


        bindingg.msgTv.text = msg
        /* .setPositiveButton(positiveButtonText) { dialog, _ ->
             dialog.dismiss()
             alertCallback(true)
         }*/

        bindingg.okBtn.setOnClickListener {
            dialogBuilder.dismiss()
            alertCallback(true)
        }

        //     val alert: androidx.appcompat.app.AlertDialog = dialogBuilder.create()
        //Below Handler will execute to auto cancel Alert Dialog Pop-Up when positiveButtonText isEmpty:-
        if (TextUtils.isEmpty(positiveButtonText)) {
            Handler(Looper.getMainLooper()).postDelayed({
                dialogBuilder.dismiss()
                dialogBuilder.cancel()
                startActivity(Intent(this, NavigationActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            }, 2000)
        }

        try {
            if (!dialogBuilder.isShowing) {
                dialogBuilder.show()
            }
        } catch (ex: WindowManager.BadTokenException) {
            ex.printStackTrace()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        dialogBuilder.show()
        dialogBuilder.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

    }



    open fun transactFragment(fragment: Fragment, isBackStackAdded: Boolean = true): Boolean {
        val trans = supportFragmentManager.beginTransaction().apply {
            replace(R.id.nav_host_fragment, fragment, fragment::class.java.simpleName)
            addToBackStack(fragment::class.java.simpleName)
        }
        if (isBackStackAdded) trans.addToBackStack(null)
        return trans.commitAllowingStateLoss() >= 0
    }

    fun showMerchantAlertBoxSMSUpiPay(
        printerUtil: PrintUtil,
        digiposData: DigiPosDataTable,
        dialogCB: (Boolean) -> Unit
    ) {
        alertBoxWithAction(
            getString(R.string.print_customer_copy),
            getString(R.string.print_customer_copy),
            true, getString(R.string.positive_button_yes), { status ->
                if (status) {
                    printerUtil.printSMSUPIChagreSlip(
                        digiposData,
                        EPrintCopyType.CUSTOMER,
                        this
                    ) { customerCopyPrintSuccess, printingFail ->
                        if (!customerCopyPrintSuccess) {
                            //  VFService.showToast(getString(R.string.customer_copy_print_success))
                            dialogCB(false)
                        }
                    }

                }
            }, {
                dialogCB(false)
            })
    }


    override fun onDestroy() {
        hideProgress()
        super.onDestroy()
    }


}

interface IDialog {
    /* fun getMsgDialog(
         title: String, msg: String, positiveTxt: String, negativeTxt: String
         , positiveAction: () -> Unit, negativeAction: () -> Unit, isCancellable: Boolean = false
     )

     fun getInfoDialog(title: String, msg: String, acceptCb: () -> Unit)

     fun setProgressTitle(title: String)*/
    fun getMsgDialog(
        title: String,
        msg: String,
        positiveTxt: String,
        negativeTxt: String,
        positiveAction: () -> Unit,
        negativeAction: () -> Unit,
        isCancellable: Boolean = false
    )

    fun setProgressTitle(title: String)
   // fun onEvents(event: VxEvent)

    fun showToast(msg: String)
    fun showProgress(progressMsg: String = "Please Wait....")
    fun hideProgress()
    fun getInfoDialog(title: String, msg: String,icon: Int = R.drawable.ic_info , acceptCb: () -> Unit)
    fun getInfoDialogdoubletap(title: String, msg: String, acceptCb: (Boolean, Dialog) -> Unit)
    fun alertBoxWithAction(
        title: String, msg: String, showCancelButton: Boolean, positiveButtonText: String,
        alertCallback: (Boolean) -> Unit, cancelButtonCallback: (Boolean) -> Unit, dialogIcon:Int = 0
    )

    fun alertBoxWithOnlyOk(
        title: String, msg: String, showCancelButton: Boolean = false, positiveButtonText: String,
        alertCallback: (Boolean) -> Unit, cancelButtonCallback: (Boolean) -> Unit
    )
    fun updatePercentProgress(percent:Int)
    fun showPercentDialog(progressMsg: String = "Please Wait....")

    fun alertBoxMsgWithIconOnly(
        icon: Int, msg: String
    )

    fun txnApprovedDialog(
        headerImage:Int = 0 ,headerText:String = "",amount: String = "0.00", dateTime: String = "", alertCallback: (Boolean) -> Unit
    )

    fun alertBoxWithActionNew(header:String, msg:String, icon: Int, positiveButtonText: String, negativeButtonText:String, isShowNegativeBtn:Boolean,
                              isAutoCancel:Boolean, yesButtonCallback: (Boolean) -> Unit, noButtonCallback: (Boolean) -> Unit)
}





