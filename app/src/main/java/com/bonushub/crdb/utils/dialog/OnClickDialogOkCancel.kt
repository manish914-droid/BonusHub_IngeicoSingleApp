package com.bonushub.crdb.utils.dialog

import android.app.Dialog

interface OnClickDialogOkCancel {

    fun onClickOk(dialog: Dialog, password:String)
    fun onClickCancel()
}