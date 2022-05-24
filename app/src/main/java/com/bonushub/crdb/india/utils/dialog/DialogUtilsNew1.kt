package com.bonushub.crdb.india.utils.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.InputFilter
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.utils.ToastUtils
import com.bonushub.crdb.india.view.fragments.getEditorActionListener
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class DialogUtilsNew1 {

    companion object {
        fun showDialog(
            activity: Activity?,
            header: String?,
            hint: String?,
            onClick: OnClickDialogOkCancel,
            setCancelable: Boolean = true
        ) {
            val dialog = Dialog(activity!!)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));


           // dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(setCancelable)
            dialog.setContentView(R.layout.dialog_admin)

            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )

           // val textViewHeader = dialog.findViewById<View>(R.id.textViewHeader) as TextView
            val inputTexLayout = dialog.findViewById<View>(R.id.password_crdView) as TextInputLayout
            val edtTextPassword = dialog.findViewById<View>(R.id.edtTextPassword) as TextInputEditText
            val txtViewCancel = dialog.findViewById<View>(R.id.txtViewCancel) as TextView
            val txtViewOk = dialog.findViewById<View>(R.id.txtViewOk) as TextView

            //textViewHeader.text = header
            inputTexLayout.hint = hint

            if(header?.equals("ADMIN PASSWORD")?:false){

                edtTextPassword.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(4))

            }else if(header?.equals("SUPER ADMIN PASSWORD")?:false)
            {
                edtTextPassword.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(6))
            }

            //edtTextPassword.gravity = Gravity.CENTER_HORIZONTAL

            txtViewOk.setOnClickListener {

                hideKeyboardIfOpen(activity)
                onClick.onClickOk(dialog = dialog, password = edtTextPassword.text.toString())
                //dialog.dismiss()
            }

            txtViewCancel.setOnClickListener {

                hideKeyboardIfOpen(activity)
                onClick.onClickCancel()
                dialog.dismiss()
            }
            dialog.show()
        }

        fun getInputDialog(context: Context, title: String, _text: String, isNumeric: Boolean = false, isTID: Boolean = false,toastMsg:String, callback: (String) -> Unit) {
            Dialog(context).apply {
                getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(R.layout.dialog_admin)
                setCancelable(false)
                val window = window
                window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
                )

                val inputTexLayout = findViewById<View>(R.id.password_crdView) as TextInputLayout
                val invoiceET = findViewById<EditText>(R.id.edtTextPassword)
                val okbtn = findViewById<TextView>(R.id.txtViewOk)

                inputTexLayout.hint = title
//                if (_text == TerminalParameterTable.selectFromSchemeTable()?.terminalId.toString()) {
//                    invoiceET.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(8))
//                } else {
//                    invoiceET.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(25))
//                }
//              this code written below

                if (isTID) {
                    invoiceET.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(8))
                } else {
                    invoiceET.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(25))
                }

                if (isNumeric||isTID) {
                    if(isTID){
                        //invoiceET.setKeyListener(DigitsKeyListener.getInstance("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"))
                        invoiceET.inputType = InputType.TYPE_CLASS_TEXT
                    }else{
                        invoiceET.inputType = InputType.TYPE_CLASS_NUMBER
                        invoiceET.setKeyListener(DigitsKeyListener.getInstance("0123456789."))
                    }

                }else{
                    invoiceET.inputType = InputType.TYPE_CLASS_TEXT
                }

                invoiceET.apply {
                    setText(_text)
//                    inputType =
//                        if (isNumeric) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT
                    setOnEditorActionListener(getEditorActionListener { okbtn.performClick() })
                    setSelection(text.toString().length)
                    //gravity = Gravity.CENTER_HORIZONTAL
                }

//                findViewById<TextView>(R.id.textViewHeader).text = title
                findViewById<TextView>(R.id.txtViewCancel).setOnClickListener {
                    dismiss()
                }
                okbtn.setOnClickListener {
                    if (!invoiceET.text.toString().isNullOrEmpty()) {
                        dismiss()
                        callback(invoiceET.text.toString())
                    } else {
                        ToastUtils.showToast(context, "Please enter $toastMsg.")
                    }
                }
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }.show()
        }

        fun showMsgOkDialog(
            activity: Activity?,
            header: String?,
            msg: String?,
            setCancelable: Boolean = true
        ) {
            Dialog(activity!!).apply {

                val window = window
                window?.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
                getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(setCancelable)

                setContentView(R.layout.dialog_msg)
                val textViewHeader = findViewById<View>(R.id.textViewHeader) as TextView
                val textViewMsg = findViewById<View>(R.id.textViewMsg) as TextView
                val txtViewOk = findViewById<View>(R.id.txtViewOk) as TextView

                textViewHeader.text = header
                textViewMsg.text = msg

                txtViewOk.setOnClickListener {

                    dismiss()
                }

            }.show()
        }

        fun showVoidSaleDetailsDialog(
            context: Context,
            date: String,
            time: String,
            tid: String,
            invoiceNumber: String,
            totalAmount: String,
            callback: () -> Unit
        ) {
            Dialog(context).apply {
                getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(R.layout.dialog_void_details)
                setCancelable(false)
                val window = window
//                window?.setLayout(
//                    ViewGroup.LayoutParams.MATCH_PARENT,
//                    WindowManager.LayoutParams.WRAP_CONTENT
//                )
                val txtViewDate = findViewById<TextView>(R.id.txtViewDate)
                val txtViewTime = findViewById<TextView>(R.id.txtViewTime)
                val txtViewTid = findViewById<TextView>(R.id.txtViewTid)
                val txtViewInvoiceNumber = findViewById<TextView>(R.id.txtViewInvoiceNumber)
                val txtViewTotalAmount = findViewById<TextView>(R.id.txtViewTotalAmount)
                val txtViewCancel = findViewById<TextView>(R.id.txtViewCancel)
                val okbtn = findViewById<TextView>(R.id.txtViewOk)

                txtViewDate.text = date

                val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
                val timeFormat2 = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                var formattedTime = ""
                try {
                    val t1 = timeFormat.parse(time)
                    formattedTime = timeFormat2.format(t1)
                    Log.e("Time", formattedTime)
                } catch (e: ParseException) {
                    e.printStackTrace()
                }

                txtViewTime.text = formattedTime

                txtViewTid.text = tid
                txtViewInvoiceNumber.text = invoiceNumber
                txtViewTotalAmount.text = totalAmount

                txtViewCancel.setOnClickListener {
                    dismiss()
                }

                okbtn.setOnClickListener {
                    dismiss()
                    callback()
                }
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }.show()
        }


        fun showBrandEmiByCodeDetailsDialog(
            context: Context,
            bank: String,
            productName: String,
            categoryName: String,
            tenure: String,
            transactionAmount: String,
            emiAmount: String,
            netPayAmount: String,
            callback: () -> Unit
        ) {
            Dialog(context).apply {
                getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(R.layout.dialog_brand_emi_by_code_details)
                setCancelable(false)
                val window = window
//                window?.setLayout(
//                    ViewGroup.LayoutParams.MATCH_PARENT,
//                    WindowManager.LayoutParams.WRAP_CONTENT
//                )
                val txtViewBank = findViewById<TextView>(R.id.txtViewBank)
                val txtViewProductName = findViewById<TextView>(R.id.txtViewProductName)
                val txtViewCategoryName = findViewById<TextView>(R.id.txtViewCategoryName)
                val txtViewTenure = findViewById<TextView>(R.id.txtViewTenure)
                val txtViewTransactionAmount = findViewById<TextView>(R.id.txtViewTransactionAmount)
                val txtViewEmiAmount = findViewById<TextView>(R.id.txtViewEmiAmount)
                val txtViewNetPayAmount = findViewById<TextView>(R.id.txtViewNetPayAmount)
                val txtViewCancel = findViewById<TextView>(R.id.txtViewCancel)
                val txtViewConfirm = findViewById<TextView>(R.id.txtViewConfirm)

                txtViewBank.text = bank
                txtViewProductName.text = productName
                txtViewCategoryName.text = categoryName
                txtViewTenure.text = tenure
                txtViewTransactionAmount.text = transactionAmount
                txtViewEmiAmount.text = emiAmount
                txtViewNetPayAmount.text = netPayAmount

                txtViewCancel.setOnClickListener {
                    dismiss()
                }

                txtViewConfirm.setOnClickListener {
                    dismiss()
                    callback()
                }
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }.show()
        }


        fun alertBoxWithAction(context: Context,
                               heading: String, msg: String,
            positiveButtonText: String, negativeButtonText: String, imgHeaader:Int, callback: () -> Unit,
            cancelButtonCallback: () -> Unit
        ) {
            // BaseActivity. getInfoDialog()

            Dialog(context).apply {
                getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(R.layout.dialog_alert_message_with_icon)
                setCancelable(false)
                val window = window
//                window?.setLayout(
//                    ViewGroup.LayoutParams.MATCH_PARENT,
//                    WindowManager.LayoutParams.WRAP_CONTENT
//                )
                val dialog_heading = findViewById<TextView>(R.id.dialog_heading)
                val dialog_msg = findViewById<TextView>(R.id.dialog_msg)
                val noBtn = findViewById<TextView>(R.id.noBtn)
                val yesBtn = findViewById<TextView>(R.id.yesBtn)
                val img_header = findViewById<ImageView>(R.id.img_header)

                dialog_heading.text = heading
                dialog_msg.text = msg
                noBtn.text = negativeButtonText
                yesBtn.text = positiveButtonText

                if(imgHeaader == 0){
                    img_header.visibility = View.GONE
                }else{
                    img_header.visibility = View.VISIBLE
                    img_header.setImageResource(imgHeaader)
                }

                if(msg.isEmpty()){
                    dialog_msg.visibility = View.GONE
                }else{
                    dialog_msg.visibility = View.VISIBLE
                }
                noBtn.setOnClickListener {
                    dismiss()
                    cancelButtonCallback()
                }

                yesBtn.setOnClickListener {
                    dismiss()
                    callback()
                }
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }.show()

        }


        fun hideKeyboardIfOpen(activity: Activity) {
            val view = activity.currentFocus
            if (view != null) {
                val imm = activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }

        fun showPendingTxnDetailsDialog(
            context: Context,
            amount: String,
            mode: String,
            txnId: String,
            mTxnId: String,
            phoneNumber: String,
            status: String,
            callback: () -> Unit,
            callbackPrint: () -> Unit
        ) {
            Dialog(context).apply {
                getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(R.layout.dialog_pending_txn_details)
                setCancelable(false)
                val window = window
                window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
                val txtViewAmount = findViewById<TextView>(R.id.txtViewAmount)
                val txtViewMode = findViewById<TextView>(R.id.txtViewMode)
                val txtViewTXNId = findViewById<TextView>(R.id.txtViewTXNId)
                val txtViewMTXNId = findViewById<TextView>(R.id.txtViewMTXNId)
                val txtViewPhoneNumber = findViewById<TextView>(R.id.txtViewPhoneNumber)
                val txtViewStatus = findViewById<TextView>(R.id.txtViewStatus)
                val txtViewOk = findViewById<TextView>(R.id.txtViewOk)
                val txtViewPrint = findViewById<TextView>(R.id.txtViewPrint)

                txtViewAmount.text = amount
                txtViewMode.text = mode
                txtViewTXNId.text = txnId
                txtViewMTXNId.text = mTxnId
                txtViewPhoneNumber.text = phoneNumber
                txtViewStatus.text = status


                txtViewOk.setOnClickListener {
                    dismiss()
                    callback()
                }

                txtViewPrint.setOnClickListener {
                    dismiss()
                    callbackPrint()
                }
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }.show()
        }


        fun showUpiCollectDialog(
            context: Context,
            amount: String,
            vpa: String,
            mobile: String,
            callback: () -> Unit,
            callbackPrint: () -> Unit
        ) {
            Dialog(context).apply {
                getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(R.layout.fragment_upi_collect)
                setCancelable(false)
                val window = window
                window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
                val txtViewAmount = findViewById<TextView>(R.id.txtViewAmount)
                val txtViewVpa = findViewById<TextView>(R.id.txtViewVpa)
                val txtViewMobile = findViewById<TextView>(R.id.txtViewMobile)
                val enter_description_et = findViewById<EditText>(R.id.enter_description_et)
                val btnProceed = findViewById<TextView>(R.id.btnProceed)


                txtViewAmount.text = amount
                txtViewVpa.text = vpa
                txtViewMobile.text = mobile
                //enter_description_et.text = mTxnId
                //btnProceed.text = phoneNumber


                btnProceed.setOnClickListener {
                    dismiss()
                    callback()
                }

//                txtViewPrint.setOnClickListener {
//                    dismiss()
//                    callbackPrint()
//                }
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }.show()
        }

    }

}