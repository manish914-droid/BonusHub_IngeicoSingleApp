package com.bonushub.crdb.view.fragments

import android.app.Activity
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
import androidx.core.content.ContextCompat
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
class InitFragment : Fragment(),IDialog {
    private val initViewModel : InitViewModel by viewModels()
    private var progressBar : ProgressBar? = null
    private var iDialog: IDialog? = null
    lateinit var progressView: ProgressBar
    private lateinit var progressDialog: Dialog
    lateinit var progressTitleMsg: TextView
    lateinit var progressPercent:ProgressBar
    lateinit var progressPercentTv: TextView
    lateinit var horizontalPLL: LinearLayout
    lateinit var verticalProgressBar: ProgressBar
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
        setProgressDialog()
        fragmentInitBinding?.ifProceedBtn?.isEnabled = false
        fragmentInitBinding?.ifProceedBtn?.isClickable = false
        fragmentInitBinding?.ifEt?.addTextChangedListener(textWatcher)
        fragmentInitBinding?.ifEt?.transformationMethod = null
        fragmentInitBinding?.ifEt?.addTextChangedListener(Utility.OnTextChange {
            fragmentInitBinding?.ifProceedBtn?.isEnabled = it.length == 8
            if (fragmentInitBinding?.ifProceedBtn?.isEnabled)
                fragmentInitBinding?.ifProceedBtn?.setBackgroundResource(R.drawable.edge_button);

        })

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
                Result.Status.SUCCESS -> {
                    hideProgress()
                    (activity as NavigationActivity).transactFragment(DashboardFragment())
                }
                Result.Status.ERROR -> {
                    hideProgress()
                    Toast.makeText(activity,"Error called  ${result.message}", Toast.LENGTH_LONG).show()
                }
                Result.Status.LOADING -> {
                    showProgress("Sending/Receiving From Host")

                }
            }

        })


    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Thread.setDefaultUncaughtExceptionHandler(UnCaughtException(this@BaseActivity))
        setProgressDialog()
    }
    private fun setProgressDialog() {
        progressDialog = activity?.let {
            Dialog(it).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(R.layout.new_tem_progress_dialog)
                setCancelable(false)
            }
        }!!
        progressTitleMsg = progressDialog.findViewById(R.id.msg_et)
        progressPercent=progressDialog.findViewById(R.id.pBar)
        progressPercentTv=progressDialog.findViewById(R.id.downloadPercentTv)
        horizontalPLL=progressDialog.findViewById(R.id.horizontalProgressLL)
        verticalProgressBar=progressDialog.findViewById(R.id.verticalProgressbr)
    }
    override fun showProgress(progressMsg: String) {
        if (!progressDialog.isShowing && !(activity)?.isFinishing!!) {
            progressTitleMsg.text = progressMsg
            progressDialog.show()
        }
    }
    override fun hideProgress() {
        if (progressDialog.isShowing && !(activity)?.isFinishing!!) {
            progressDialog.dismiss()
            horizontalPLL.visibility=View.GONE
            verticalProgressBar.visibility=View.VISIBLE
        }
    }

    override fun getInfoDialog(title: String, msg: String, acceptCb: () -> Unit) {
        TODO("Not yet implemented")
    }

    override fun getInfoDialogdoubletap(
        title: String,
        msg: String,
        acceptCb: (Boolean, Dialog) -> Unit
    ) {
        TODO("Not yet implemented")
    }

    override fun updatePercentProgress(percent: Int) {
        TODO("Not yet implemented")
    }

    override fun showPercentDialog(progressMsg: String) {
        TODO("Not yet implemented")
    }

    override fun getMsgDialog(
        title: String,
        msg: String,
        positiveTxt: String,
        negativeTxt: String,
        positiveAction: () -> Unit,
        negativeAction: () -> Unit,
        isCancellable: Boolean
    ) {
        TODO("Not yet implemented")
    }

    override fun setProgressTitle(title: String) {
        //progressTitleMsg.text = title
    }

    override fun showToast(msg: String) {
        TODO("Not yet implemented")
    }
}