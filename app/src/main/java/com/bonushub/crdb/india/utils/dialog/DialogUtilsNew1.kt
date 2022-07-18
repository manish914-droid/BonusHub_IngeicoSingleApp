package com.bonushub.crdb.india.utils.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.utils.ToastUtils
import com.bonushub.crdb.india.utils.Utility
import com.bonushub.crdb.india.view.fragments.getEditorActionListener
import com.bonushub.crdb.india.vxutils.BhTransactionType
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.ingenico.hdfcpayment.type.TransactionType
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
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))


            // dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(setCancelable)
            dialog.setContentView(R.layout.dialog_admin)

            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )

            // val textViewHeader = dialog.findViewById<View>(R.id.textViewHeader) as TextView
            val inputTexLayout = dialog.findViewById<View>(R.id.password_crdView) as TextInputLayout
            val edtTextPassword =
                dialog.findViewById<View>(R.id.edtTextPassword) as TextInputEditText
            val txtViewCancel = dialog.findViewById<View>(R.id.txtViewCancel) as TextView
            val txtViewOk = dialog.findViewById<View>(R.id.txtViewOk) as TextView

            //textViewHeader.text = header
            inputTexLayout.hint = hint

            if (header?.equals("ADMIN PASSWORD") == true) {

                edtTextPassword.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(4))

            } else if (header?.equals("SUPER ADMIN PASSWORD") == true) {
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

        fun getInputDialog(
            context: Context,
            title: String,
            _text: String,
            isNumeric: Boolean = false,
            isTID: Boolean = false,
            toastMsg: String,
            callback: (String) -> Unit,
            callbackCancel: () -> Unit
        ) {
            Dialog(context).apply {
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
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

                if (isNumeric || isTID) {
                    if (isTID) {
                        //invoiceET.setKeyListener(DigitsKeyListener.getInstance("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"))
                        invoiceET.inputType = InputType.TYPE_CLASS_TEXT
                    } else {
                        invoiceET.inputType = InputType.TYPE_CLASS_NUMBER
                        invoiceET.keyListener = DigitsKeyListener.getInstance("0123456789.")
                    }

                } else {
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
                    callbackCancel()
                }
                okbtn.setOnClickListener {
                    if (!invoiceET.text.toString().isNullOrEmpty()) {
                        dismiss()
                        callback(invoiceET.text.toString())
                    } else {
                        //ToastUtils.showToast(context, "Please enter $toastMsg.")
                        invoiceET.error = "Please enter $toastMsg."
                    }
                }
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }.show()
        }

        fun getInputTID_Dialog(
            context: Context,
            title: String,
            _text: String,
            isNumeric: Boolean = false,
            isTID: Boolean = false,
            toastMsg: String,
            callback: (String) -> Unit,
            callbackCancel: () -> Unit
        ) {
            Dialog(context).apply {
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(R.layout.dialog_tid)
                setCancelable(false)
                val window = window
                window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
                )

                findViewById<TextView>(R.id.dialogTittle).text = title
                val edtTextTid = findViewById<EditText>(R.id.edtTextTid)
                val edtTextReEnterTid = findViewById<EditText>(R.id.edtTextReEnterTid)
                val okbtn = findViewById<TextView>(R.id.txtViewOk)

                edtTextTid?.addTextChangedListener(Utility.OnTextChange {

                    edtTextReEnterTid.setText("")
                    checkInitProcessEnable(
                        edtTextTid.text.toString(),
                        edtTextReEnterTid.text.toString(),
                        okbtn
                    )

                })

                edtTextTid.setOnFocusChangeListener { view, b ->
                    if (edtTextTid.hasFocus() == true) {
                        Log.e("tid", "focus")
                        //  binding?.ifEt?.setInputType(InputType.TYPE_CLASS_TEXT)

                        edtTextTid.setSelection(edtTextTid.text.toString().length)

                        if (edtTextReEnterTid.text.toString()
                                .isNotEmpty() && edtTextTid.text.toString()
                                .equals(edtTextReEnterTid.text.toString())
                        ) {
                            edtTextReEnterTid.error = null
                        } else {
                            //  binding?.ifEt?.setError(null)
                            if (edtTextReEnterTid.text.toString().isNotEmpty()) {
                                edtTextReEnterTid.error = "TID Mismatch"
                                // (activity as NavigationActivity).showToast("TID Mismatch")
                            } else {
                                edtTextReEnterTid.error = null
                            }
                        }

                    } else {
                        Log.e("tid", "not focus")
                        edtTextTid.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

                        if (edtTextTid.text.toString().length == 8) {
                            // binding?.ifEt?.setError(null) //

                        } else {
                            edtTextTid.error = "Tid should be 8 char."
                        }

                    }

                    checkInitProcessEnable(
                        edtTextTid.text.toString(),
                        edtTextReEnterTid.text.toString(),
                        okbtn
                    )

                }

                edtTextReEnterTid.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }

                    override fun afterTextChanged(p0: Editable?) {

                        val ll = p0.toString().length
                        if (ll > 0) {
                            if (edtTextTid.text.toString().length >= ll && edtTextTid.text.toString()
                                    .substring(0, ll).equals(p0.toString())
                            ) {
                                edtTextReEnterTid.error = null

                            } else {
                                edtTextReEnterTid.error = "TID Mismatch"
                            }
                        } else {
                            edtTextReEnterTid.error = null
                        }

                        checkInitProcessEnable(
                            edtTextTid.text.toString(),
                            edtTextReEnterTid.text.toString(),
                            okbtn
                        )

                    }

                })

                val alphanumericFilter =
                    InputFilter { source, start, end, dest, dstart, dend ->
                        for (i in start until end) {
                            if (!Character.isLetterOrDigit(source[i])) {
                                return@InputFilter ""
                            }
                        }
                        null
                    }
                val lengthFilter = InputFilter.LengthFilter(8)

                val filterError =
                    InputFilter { source, start, end, dest, dstart, dend ->

                        val ll = edtTextReEnterTid.text.toString().length
                        if (ll > 0) {
                            if (edtTextTid.text.toString().length >= ll && edtTextTid.text.toString()
                                    .substring(0, ll).equals(edtTextReEnterTid.text.toString())
                            ) {
                                edtTextReEnterTid.error = null

                            } else {
                                edtTextReEnterTid.error = "TID Mismatch"
                            }
                        } else {
                            edtTextReEnterTid.error = null
                        }

                        null
                    }

                edtTextTid.filters = arrayOf(alphanumericFilter, lengthFilter)
                edtTextReEnterTid.filters = arrayOf(alphanumericFilter, lengthFilter, filterError)

                //-------
                findViewById<TextView>(R.id.txtViewCancel).setOnClickListener {
                    dismiss()
                    callbackCancel()
                }
                okbtn.setOnClickListener {

                    if (checkInitProcessEnable(
                            edtTextTid.text.toString(),
                            edtTextReEnterTid.text.toString(),
                            okbtn
                        )
                    ) {
                        dismiss()
                        callback(edtTextTid.text.toString())
                    }
                }
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }.show()
        }

        private fun checkInitProcessEnable(
            tidText: String,
            tidConfirmText: String,
            okBtn: TextView
        ): Boolean {
            val isEnabled = tidText.length == 8 && tidText.equals(tidConfirmText)
            if (isEnabled) {
                okBtn.alpha = 1f
            } else {
                okBtn.alpha = .5f
            }
            return isEnabled
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
                getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
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
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
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
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
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


        fun alertBoxWithAction(
            context: Context,
            heading: String,
            msg: String,
            positiveButtonText: String,
            negativeButtonText: String,
            imgHeaader: Int,
            callback: () -> Unit,
            cancelButtonCallback: () -> Unit
        ) {
            // BaseActivity. getInfoDialog()

            Dialog(context).apply {
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
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

                if (imgHeaader == 0) {
                    img_header.visibility = View.GONE
                } else {
                    img_header.visibility = View.VISIBLE
                    img_header.setImageResource(imgHeaader)
                }

                if (msg.isEmpty()) {
                    dialog_msg.visibility = View.GONE
                } else {
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
                val imm =
                    activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
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
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
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
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
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

        fun instaEmiDialog(
            activity: Activity?,
            emiCB: (Dialog, Activity) -> Unit,
            saleCB: (Dialog) -> Unit,
            cancelCB: (Dialog) -> Unit
        ) {
            val dialog = Dialog(activity!!)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))


            // dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(false)
            dialog.setContentView(R.layout.dialog_insta_emi)

            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )

            val imgViewCross = dialog.findViewById<View>(R.id.imgViewCross) as ImageView
            val txtViewEmi = dialog.findViewById<View>(R.id.txtViewEmi) as TextView
            val txtViewSale = dialog.findViewById<View>(R.id.txtViewSale) as TextView

            imgViewCross.setOnClickListener {
                cancelCB(dialog)
            }

            txtViewEmi.setOnClickListener {
                emiCB(dialog, activity)
            }

            txtViewSale.setOnClickListener {
                saleCB(dialog)
            }
            dialog.show()
        }

        fun showDetailsConfirmDialog(
            context: Context,
            transactionType: BhTransactionType,
            tid: String?,
            batchNo: String?,
            totalAmount: String?,
            roc: String?,
            amount: String?,
            invoice: String?,
            date: String?,
            time: String?,
            confirmCallback: (Dialog) -> Unit,
            cancelCallback: (Dialog) -> Unit
        ) {
            Dialog(context).apply {
                // getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(R.layout.dialog_details_confirm)
                setCancelable(false)
                val window = window
                window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
                val dialogTittle = findViewById<TextView>(R.id.dialogTittle)
                val tidTittle = findViewById<TextView>(R.id.tidTittle)
                val tid_tv = findViewById<TextView>(R.id.tid_tv)
                val batchNoTittle = findViewById<TextView>(R.id.batchNoTittle)
                val batch_no_tv = findViewById<TextView>(R.id.batch_no_tv)
                val total_amount_tvTittle = findViewById<TextView>(R.id.total_amount_tvTittle)
                val total_amount_tv = findViewById<TextView>(R.id.total_amount_tv)
                val roc_tvTittle = findViewById<TextView>(R.id.roc_tvTittle)
                val roc_tv = findViewById<TextView>(R.id.roc_tv)
                val txtViewAmountTittle = findViewById<TextView>(R.id.txtViewAmountTittle)
                val amt_tv = findViewById<TextView>(R.id.amt_tv)
                val cancel_btn = findViewById<TextView>(R.id.cancel_btn)
                val confirm_btn = findViewById<TextView>(R.id.confirm_btn)
                val viewLine1 = findViewById<View>(R.id.viewLine1)
                val viewLineMiddle = findViewById<View>(R.id.viewLineMiddle)
                val viewLine2 = findViewById<View>(R.id.viewLine2)


                when (transactionType) {

                    BhTransactionType.VOID -> {
                        dialogTittle.text = "VOID SALE"
                        tid_tv.text = tid
                        batchNoTittle.text = "Invoice No:"
                        batch_no_tv.text = invoice
                        total_amount_tv.text = totalAmount
                        roc_tvTittle.text = "Date:"
                        roc_tv.text = date

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

                        txtViewAmountTittle.text = "Time:"
                        amt_tv.text = formattedTime
                    }

                    BhTransactionType.PRE_AUTH_COMPLETE -> {
                        dialogTittle.text = "PRE-AUTH COMPLETE"
                        tid_tv.text = tid
                        batch_no_tv.text = batchNo
                        roc_tv.text = roc
                        amt_tv.text = amount

                        total_amount_tvTittle.visibility = View.GONE
                        total_amount_tv.visibility = View.GONE
                        viewLineMiddle.visibility = View.GONE

                    }

                    else -> {

                    }
                }


                cancel_btn.setOnClickListener {
                    cancelCallback(this)
                }

                confirm_btn.setOnClickListener {
                    confirmCallback(this)
                }
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }.show()
        }
    }

}