package com.bonushub.crdb.view.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.bonushub.crdb.NavigationActivity
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.ActivityNavigationBinding
import com.bonushub.crdb.databinding.FragmentInitBinding
import com.bonushub.crdb.utils.Result
import com.bonushub.crdb.viewmodel.InitViewModel
import com.bonushub.crdb.viewmodel.MainViewModel
import com.bonushub.pax.utils.Utility
import androidx.lifecycle.Observer
import com.bonushub.crdb.IDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InitFragment : Fragment() {
    private val initViewModel : InitViewModel by viewModels()
    private var progressBar : ProgressBar? = null
    private var iDialog: IDialog? = null
    lateinit var progressView: ProgressBar
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
        progressBar = ProgressBar(activity as NavigationActivity)
 /*       fragmentInitBinding?.ifProceedBtn?.isEnabled = false
        fragmentInitBinding?.ifProceedBtn?.isClickable = false
        fragmentInitBinding?.ifEt?.addTextChangedListener(textWatcher)
        fragmentInitBinding?.ifEt?.transformationMethod = null
        fragmentInitBinding?.ifEt?.addTextChangedListener(Utility.OnTextChange {
            fragmentInitBinding?.ifProceedBtn?.isEnabled = it.length == 8
            if (fragmentInitBinding?.ifProceedBtn?.isEnabled)
                fragmentInitBinding?.ifProceedBtn?.context?.resources?.let { it1 ->
                    fragmentInitBinding?.ifProceedBtn?.setBackgroundColor(
                        it1.getColor(R.color.init_button)
                    )
                };

        })*/

        fragmentInitBinding?.ifProceedBtn.setOnClickListener {
          //  iDialog?.showProgress(getString(R.string.please_wait_host))
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

                fragmentInitBinding?.ifProceedBtn?.context?.resources?.let { it1 ->
                    fragmentInitBinding?.ifProceedBtn?.setBackgroundColor(
                        it1.getColor(R.color.colorGrey))
                };
            }
            else if(start>=8){
                fragmentInitBinding?.ifProceedBtn?.context?.resources?.let { it1 ->
                    fragmentInitBinding?.ifProceedBtn?.setBackgroundColor(
                        it1.getColor(R.color.init_button))
                };
            }
        }
    }
    fun observeMainViewModel(){
        initViewModel.mutableLiveData.observe(viewLifecycleOwner, Observer { result ->

            when (result.status) {
                Result.Status.SUCCESS -> {
                    iDialog?.hideProgress()
                    Toast.makeText(activity,"Success called $", Toast.LENGTH_LONG).show()
                }

                Result.Status.ERROR -> {
                    iDialog?.hideProgress()
                    Toast.makeText(activity,"Error called  ${result.message}", Toast.LENGTH_LONG).show()
                }

                Result.Status.LOADING -> {
                    iDialog?.hideProgress()
                    Toast.makeText(activity,"Loading called $", Toast.LENGTH_LONG).show()
                }
            }

        })

        initViewModel.isLoading().observe(viewLifecycleOwner, Observer {
            if(it) {
                iDialog?.showProgress()
            }else
                iDialog?.hideProgress()
        })
    }
}