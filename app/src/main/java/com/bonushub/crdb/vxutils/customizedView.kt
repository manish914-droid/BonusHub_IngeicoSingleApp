package com.bonushub.crdb.vxutils

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import androidx.appcompat.widget.AppCompatTextView
import com.bonushub.crdb.R

class AmountEditText : androidx.appcompat.widget.AppCompatEditText {


    constructor(context: Context) : super(context) {
        implementTextListener()
    }

    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) {
       // init(attributeSet)
        implementTextListener()
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
  //      init(attributeSet)
        implementTextListener()
    }

   /* private fun init(attributeSet: AttributeSet) {
        val a = context.obtainStyledAttributes(attributeSet, R.styleable.BH_Font, 0, 0)
        val fontType = a.getString(R.styleable.BH_Font_fname)
        if (fontType != null) {
            val font = if (fontType == "1") {
                Typeface.createFromAsset(context.assets, "fonts/Muli-SemiBold.ttf")
            } else {
                Typeface.createFromAsset(context.assets, "fonts/Muli-Regular.ttf")
            }
            super.setTypeface(font)
        }
        a.recycle()
    }*/

    private fun implementTextListener() {
        /* val tx = "%.2f".format(0f)
         setText(tx)
         requestFocus()
         setSelection(tx.length)*/
        addTextChangedListener(watcher1)
    }


    private fun doFormatting() {
        if (text?.isNotEmpty() == true) {

            if (text?.toString() == "0.0") {
                setText("")
            } else {
                val fl = text.toString().replace(".", "").toLong()
                val tx = "%.2f".format(fl.toDouble() / 100)
                setText(tx)
                setSelection(tx.length)
            }
            removeTextChangedListener(watcher2)
            addTextChangedListener(watcher1)
        } else {
            val tx = "%.2f".format(0f)
            setText(tx)
            setSelection(tx.length)
            removeTextChangedListener(watcher2)
            addTextChangedListener(watcher1)
        }
    }


    private val watcher1 = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {

        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            removeTextChangedListener(this)
            addTextChangedListener(watcher2)
            doFormatting()

        }

    }

    private val watcher2 = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

    }

}

class BHTextView : AppCompatTextView, View.OnTouchListener {
    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
        if (hasOnClickListeners()) {
            when (p1?.action) {
                MotionEvent.ACTION_DOWN -> isSelected = true
                MotionEvent.ACTION_CANCEL -> isSelected = false
            }
        }
        return false
    }


    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) {
        init(attributeSet)
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        init(attributeSet)
    }

    constructor(context: Context) : super(context)

    private fun init(attributeSet: AttributeSet) {
        val a = context.obtainStyledAttributes(attributeSet, R.styleable.BH_Font, 0, 0)
        val fontType = a.getString(R.styleable.BH_Font_fname)
        if (fontType != null) {
            val font = if (fontType == "1") {
                Typeface.createFromAsset(context.assets, "fonts/Muli-SemiBold.ttf")
            } else {
                Typeface.createFromAsset(context.assets, "fonts/Muli-Regular.ttf")
            }
            super.setTypeface(font)
        }
        a.recycle()

        setOnTouchListener(this)

    }


}

