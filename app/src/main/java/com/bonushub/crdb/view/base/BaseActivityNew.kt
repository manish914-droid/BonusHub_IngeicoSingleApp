package com.bonushub.crdb.view.base

import android.app.Activity
import android.app.Dialog
import android.app.NativeActivity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils

import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bonushub.crdb.HDFCApplication
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.DialogMsgWithIconBinding
import com.bonushub.crdb.databinding.ItemOkBtnDialogBinding
import com.bonushub.crdb.databinding.NewPrintCustomerCopyBinding
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.pax.utils.VxEvent

abstract class BaseActivityNew : AppCompatActivity(), IDialog {

    private lateinit var progressDialog: Dialog
    lateinit var progressTitleMsg: TextView
    lateinit var progressPercent:ProgressBar
    lateinit var progressPercentTv:TextView
    lateinit var horizontalPLL:LinearLayout
    lateinit var verticalProgressBar: ProgressBar


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
            setContentView(R.layout.new_tem_progress_dialog)
            setCancelable(false)
        }
        progressTitleMsg = progressDialog.findViewById(R.id.msg_et)
        progressPercent=progressDialog.findViewById(R.id.pBar)
        progressPercentTv=progressDialog.findViewById(R.id.downloadPercentTv)
        horizontalPLL=progressDialog.findViewById(R.id.horizontalProgressLL)
        verticalProgressBar=progressDialog.findViewById(R.id.verticalProgressbr)
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
        val dialog = Dialog(this)
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
        }.show()

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

            with(findViewById<Button>(R.id.msg_dialog_ok)) {
                text = positiveTxt
                setOnClickListener {
                    dismiss()
                    positiveAction()
                }
            }

            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            findViewById<Button>(R.id.msg_dialog_cancel).apply {
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
        cancelButtonCallback: (Boolean) -> Unit
    ) {
        val dialogBuilder = Dialog(this)
        //builder.setTitle(title)
        //  builder.setMessage(msg)
        val bindingg = NewPrintCustomerCopyBinding.inflate(LayoutInflater.from(this))

        dialogBuilder.setContentView(bindingg.root)
        if (title == getString(R.string.print_customer_copy)|| title == getString(R.string.sms_upi_pay)) {
            if(title==getString(R.string.sms_upi_pay)){
                bindingg.imgPrinter.setImageResource(R.drawable.ic_link_icon)
            } else if(title==getString(R.string.print_customer_copy)){
                bindingg.imgPrinter.setImageResource(R.drawable.ic_printer)
            }
            bindingg.imgPrinter.visibility = View.VISIBLE
        } else {
            bindingg.imgPrinter.visibility = View.GONE
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
            bindingg.imgPrinter.visibility = View.GONE
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
    }

    override fun alertBoxMsgWithIconOnly(
        icon: Int, msg: String
    ) {
        val dialogBuilder = Dialog(this)
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
        dialogBuilder.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
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

    open fun transactFragment(fragment: Fragment, isBackStackAdded: Boolean = false): Boolean {
        val trans = supportFragmentManager.beginTransaction().apply {
            replace(R.id.nav_host_fragment, fragment, fragment::class.java.simpleName)
            addToBackStack(fragment::class.java.simpleName)
        }
        if (isBackStackAdded) trans.addToBackStack(null)
        return trans.commitAllowingStateLoss() >= 0
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
    fun onEvents(event: VxEvent)

    fun showToast(msg: String)
    fun showProgress(progressMsg: String = "Please Wait....")
    fun hideProgress()
    fun getInfoDialog(title: String, msg: String,icon: Int = R.drawable.ic_info , acceptCb: () -> Unit)
    fun getInfoDialogdoubletap(title: String, msg: String, acceptCb: (Boolean, Dialog) -> Unit)
    fun alertBoxWithAction(
        title: String, msg: String, showCancelButton: Boolean, positiveButtonText: String,
        alertCallback: (Boolean) -> Unit, cancelButtonCallback: (Boolean) -> Unit
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
}





