package com.bonushub.crdb.utils.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.bonushub.crdb.R
import com.bonushub.crdb.utils.dialog.OnClickDialogOkCancel

class DialogUtilsNew1 {

    companion object
    {
        fun showDialog(activity: Activity?, header:String?, hint:String?, onClick:OnClickDialogOkCancel, setCancelable:Boolean = true) {
            val dialog = Dialog(activity!!)
            dialog.getWindow()?.setBackgroundDrawable( ColorDrawable(Color.TRANSPARENT));
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(setCancelable)
            dialog.setContentView(com.bonushub.crdb.R.layout.dialog_admin)
            val textViewHeader = dialog.findViewById<View>(R.id.textViewHeader) as TextView
            val edtTextPassword = dialog.findViewById<View>(R.id.edtTextPassword) as EditText
            val txtViewCancel = dialog.findViewById<View>(R.id.txtViewCancel) as TextView
            val txtViewOk = dialog.findViewById<View>(R.id.txtViewOk) as TextView

            textViewHeader.text = header
            edtTextPassword.hint = hint

            txtViewOk.setOnClickListener {

                onClick.onClickOk(dialog = dialog, password = edtTextPassword.text.toString())
                //dialog.dismiss()
            }

            txtViewCancel.setOnClickListener {

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
            callback: (String) -> Unit
        ) {
            Dialog(context).apply {
                getWindow()?.setBackgroundDrawable( ColorDrawable(Color.TRANSPARENT));
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(R.layout.dialog_admin)
                setCancelable(false)
                val window = window
//                window?.setLayout(
//                    ViewGroup.LayoutParams.MATCH_PARENT,
//                    WindowManager.LayoutParams.WRAP_CONTENT
//                )
                val invoiceET = findViewById<EditText>(R.id.edtTextPassword)
                val okbtn = findViewById<TextView>(R.id.txtViewOk)

                invoiceET.hint = title
//                if (_text == TerminalParameterTable.selectFromSchemeTable()?.terminalId.toString()) {
//                    invoiceET.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(8))
//                } else {
//                    invoiceET.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(25))
//                }
                invoiceET.apply {
                    setText(_text)
                    inputType = if (isNumeric) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT
                    //setOnEditorActionListener(getEditorActionListener { okbtn.performClick() })
                    setSelection(text.toString().length)
                }

                findViewById<TextView>(R.id.textViewHeader).text = title
                findViewById<TextView>(R.id.txtViewCancel).setOnClickListener {
                    dismiss()
                }
                okbtn.setOnClickListener {
                    if(!invoiceET.text.toString().isNullOrEmpty()) {
                        dismiss()
                        callback(invoiceET.text.toString())
                    }
                    else{
                        //VFService.showToast("Enter Invoice Number")
                    }
                }
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }.show()
        }

    }

}