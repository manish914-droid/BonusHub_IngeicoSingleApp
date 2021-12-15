package com.bonushub.crdb.view.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentInitBinding
import com.bonushub.crdb.viewmodel.InitViewModel
import androidx.lifecycle.Observer

import com.bonushub.crdb.utils.Utility
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.base.IDialog
import com.mindorks.example.coroutines.utils.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.ArrayList

@AndroidEntryPoint
class InitFragment : Fragment() {
    private val initViewModel : InitViewModel by viewModels()
    private var progressBar : ProgressBar? = null
    private var iDialog: IDialog? = null
    private val fragmentInitBinding :FragmentInitBinding by lazy {
        FragmentInitBinding.inflate(layoutInflater)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is IDialog) iDialog = context

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ):View? {
        return fragmentInitBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // for screen awake
        (activity as NavigationActivity).window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // it's handling for init button is enable or disable ----> it will be enable when Tid length is equal to 8
        fragmentInitBinding.ifProceedBtn.isEnabled = false
        fragmentInitBinding.ifProceedBtn.isClickable = false
        fragmentInitBinding.ifEt.addTextChangedListener(textWatcher)
        fragmentInitBinding.ifEt.transformationMethod = null
        fragmentInitBinding.ifProceedBtn.setBackgroundResource(R.drawable.edge_button_inactive);
        fragmentInitBinding.ifEt.addTextChangedListener(Utility.OnTextChange {
            fragmentInitBinding.ifProceedBtn.isEnabled = it.length == 8
            if (fragmentInitBinding.ifProceedBtn.isEnabled)
                fragmentInitBinding.ifProceedBtn.setBackgroundResource(R.drawable.edge_button);

        })

        fragmentInitBinding.ifProceedBtn.setOnClickListener {
       iDialog?.showProgress(getString(R.string.please_wait_host))
            initViewModel.insertInfo1(fragmentInitBinding.ifEt.text.toString())
        }
        observeMainViewModel()
    }
// it's for watching length of tid and change the color of proceed button according to that
    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            if (start < 8) {
                fragmentInitBinding?.ifProceedBtn?.setBackgroundResource(R.drawable.edge_button_inactive);
            }
            else if(start>=8){
                fragmentInitBinding?.ifProceedBtn?.setBackgroundResource(R.drawable.edge_button);
            }
        }
    }
    private fun observeMainViewModel(){

        initViewModel.initData.observe(viewLifecycleOwner, Observer { result ->

            when (result.status) {
                Status.SUCCESS -> {
                    CoroutineScope(Dispatchers.IO).launch{
                        Utility().readInitServer(result?.data?.data as ArrayList<ByteArray>) { result, message ->
                            iDialog?.hideProgress()
                            CoroutineScope(Dispatchers.Main).launch {
                                (activity as? NavigationActivity)?.alertBoxMsgWithIconOnly(R.drawable.ic_tick,
                                    requireContext().getString(R.string.successfull_init))
                            }

                        }

                    }


                }
                Status.ERROR -> {
                    iDialog?.hideProgress()
                    CoroutineScope(Dispatchers.Main).launch {
                        (activity as? NavigationActivity)?.getInfoDialog("Error", result.error ?: "") {}
                    }
                }
                Status.LOADING -> {
                    iDialog?.showProgress(getString(R.string.sending_receiving_host))

                }
            }

        })


    }



}