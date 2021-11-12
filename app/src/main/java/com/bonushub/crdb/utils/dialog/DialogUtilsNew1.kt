package com.bonushub.crdb.utils.dialog

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.Window
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

    }

}