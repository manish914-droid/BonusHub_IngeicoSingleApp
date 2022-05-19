package com.bonushub.crdb.india.utils

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import com.google.android.material.textfield.TextInputEditText

class AmountTextWatcher(val textInputEditText: TextInputEditText) : TextWatcher, View.OnClickListener {

    var amount = ""
    init {
        textInputEditText.addTextChangedListener(this)
        textInputEditText.setOnClickListener(this)
    }
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        // not need
    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        // not need
    }

    override fun afterTextChanged(p0: Editable?) {
        if(!amount.equals(p0.toString())) {
            // bindingg.amountEt.setText(doFormatting(p0.toString()))
            amount = doFormatting(p0.toString())
            textInputEditText.setText(amount)
            textInputEditText.setSelection(amount.length)
        }
    }

    override fun onClick(p0: View?) {
        textInputEditText.setSelection(textInputEditText.text.toString().length)
    }
}