package com.bonushub.crdb.india.view.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentOnBoardingEnterOtpBinding
import com.bonushub.crdb.india.view.activity.NavigationActivity

class OnBoardingEnterOtpFragment : Fragment() {

    lateinit var binding:FragmentOnBoardingEnterOtpBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_on_boarding_enter_otp, container, false)
        binding=FragmentOnBoardingEnterOtpBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as NavigationActivity).manageTopToolBar(false)

        binding.subHeaderView.backImageButton.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }

        binding.subHeaderView.backImageButton.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }

        binding.submitBtn.setOnClickListener {
            val dialog = Dialog(requireActivity())
            // dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(false)
            dialog.setContentView(R.layout.pay_later_onboarding_dialog_box)



            dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
            val window = dialog.window
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                 //ViewGroup.LayoutParams.WRAP_CONTENT,
                 //WindowManager.LayoutParams.WRAP_CONTENT
                WindowManager.LayoutParams.MATCH_PARENT
            )
            dialog.show()
            //window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            (activity as NavigationActivity).manageTopToolBar(false)

            dialog.findViewById<CardView>(R.id.ok_btn_onboarding).setOnClickListener {
                dialog.dismiss()
                //parentFragmentManager.popBackStackImmediate()
            }

        }


        /*binding.submitBtn.setOnClickListener {
            *//*val builder = AlertDialog.Builder(this,R.style.CustomAlertDialog)
                .create()*//*

            val dialog = Dialog(requireActivity())
            val view = layoutInflater.inflate(R.layout.pay_later_onboarding_dialog_box,null)
            *//*val  button = view.findViewById<Button>(R.id.dialogDismiss_button)
            builder.setView(view)*//*
            *//*button.setOnClickListener {
                builder.dismiss()
            }*//*
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()

            dialog.findViewById<CardView>(R.id.ok_btn_onboarding).setOnClickListener {
                dialog.dismiss()
            }
        }*/
        setmobileotp()
        this.setemailotp()
    }

    fun setmobileotp()
    {
        binding.mobD1.addTextChangedListener(object:TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (binding.mobD1.text.toString().isNotEmpty())
                {
                    binding.mobD2.requestFocus()
                }
            }

        })
        binding.mobD2.addTextChangedListener(object:TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (binding.mobD2.text.toString().isNotEmpty())
                {
                    println(binding.mobD2.toString())
                    binding.mobD3.requestFocus()
                }
                else{
                    binding.mobD1.requestFocus()
                }
            }

        })
        binding.mobD3.addTextChangedListener(object:TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (binding.mobD3.text.toString().isNotEmpty())
                {
                    binding.mobD4.requestFocus()
                }
                else{
                    binding.mobD2.requestFocus()
                }
            }

        })
        binding.mobD4.addTextChangedListener(object:TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (binding.mobD4.text.toString().isNotEmpty())
                {
                    binding.mobD5.requestFocus()
                }
                else{
                    binding.mobD3.requestFocus()
                }
            }

        })
        binding.mobD5.addTextChangedListener(object:TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (binding.mobD5.text.toString().isNotEmpty())
                {
                    binding.mobD6.requestFocus()
                }
                else{
                    binding.mobD4.requestFocus()
                }
            }

        })
        binding.mobD6.addTextChangedListener(object:TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (binding.mobD6.text.toString().isEmpty())
                {
                    binding.mobD5.requestFocus()
                }
                else{
                    //binding.mobD4.requestFocus()
                    (activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).
                    hideSoftInputFromWindow(view?.windowToken, 0)
                }
            }

        })
    }

    fun setemailotp()
    {
        binding.emlD1.addTextChangedListener(object:TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (binding.emlD1.text.toString().isNotEmpty())
                {
                    binding.emlD2.requestFocus()
                }
            }

        })
        binding.emlD2.addTextChangedListener(object:TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (binding.emlD2.text.toString().isNotEmpty())
                {
                   // println(binding.mobD2.toString())
                    binding.emlD3.requestFocus()
                }
                else{
                    binding.emlD1.requestFocus()
                }
            }

        })
        binding.emlD3.addTextChangedListener(object:TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (binding.emlD3.text.toString().isNotEmpty())
                {
                    binding.emlD4.requestFocus()
                }
                else{
                    binding.emlD2.requestFocus()
                }
            }

        })
        binding.emlD4.addTextChangedListener(object:TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (binding.emlD4.text.toString().isNotEmpty())
                {
                    binding.emlD5.requestFocus()
                }
                else{
                    binding.emlD3.requestFocus()
                }
            }

        })
        binding.emlD5.addTextChangedListener(object:TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (binding.emlD5.text.toString().isNotEmpty())
                {
                    binding.emlD6.requestFocus()
                }
                else{
                    binding.emlD4.requestFocus()
                }
            }

        })
        binding.emlD6.addTextChangedListener(object:TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (binding.emlD6.text.toString().isEmpty())
                {
                    binding.emlD5.requestFocus()



                    /*val imm = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)*/

                }
                 else{
                     (activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).
                     hideSoftInputFromWindow(view?.windowToken, 0)

                 }
            }

        })
    }

}