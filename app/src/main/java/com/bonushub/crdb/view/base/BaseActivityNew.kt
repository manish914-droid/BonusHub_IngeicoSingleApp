package com.bonushub.crdb.view.base


import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bonushub.crdb.R



abstract class BaseActivityNew : AppCompatActivity(), IDialog {
    private lateinit var progressDialog: Dialog
    lateinit var progressTitleMsg: TextView
    lateinit var progressPercent:ProgressBar
    lateinit var progressPercentTv:TextView
    lateinit var horizontalPLL:LinearLayout
    lateinit var verticalProgressBar: ProgressBar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Thread.setDefaultUncaughtExceptionHandler(UnCaughtException(this@BaseActivity))
        setProgressDialog()
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


    fun showToast(msg: String)
    fun showProgress(progressMsg: String = "Please Wait....")
    fun hideProgress()
    fun getInfoDialog(title: String, msg: String, acceptCb: () -> Unit)
    fun getInfoDialogdoubletap(title: String, msg: String, acceptCb: (Boolean, Dialog) -> Unit)

    fun updatePercentProgress(percent:Int)
    fun showPercentDialog(progressMsg: String = "Please Wait....")

}




enum class EIntentRequest(val code: Int) {
    TRANSACTION(100),
    EMI_ENQUIRY(101),
    GALLERY(2000),
    PRINTINGRECEIPT(102),
    BankEMISchemeOffer(106),
    FlexiPaySchemeOffer(107),
}