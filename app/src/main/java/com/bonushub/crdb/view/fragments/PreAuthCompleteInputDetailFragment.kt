package com.bonushub.crdb.view.fragments

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentPreAuthCompleteDetailBinding
import com.bonushub.crdb.model.CardProcessedDataModal
import com.bonushub.crdb.utils.Field48ResponseTimestamp.showToast
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.base.BaseActivity
import com.bonushub.pax.utils.EDashboardItem
import com.bonushub.pax.utils.ProcessingCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

class PreAuthCompleteInputDetailFragment : Fragment() {
   // private val title: String by lazy { arguments?.getString(MainActivity.INPUT_SUB_HEADING) ?: "" }
    private val cardProcessedData: CardProcessedDataModal by lazy { CardProcessedDataModal() }
    private val authData: AuthCompletionData by lazy { AuthCompletionData() }
    private var binding: FragmentPreAuthCompleteDetailBinding? = null
    private val action by lazy { arguments?.getSerializable("type") ?: "" }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPreAuthCompleteDetailBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onDetach() {
        super.onDetach()
       // hideSoftKeyboard(requireActivity())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.subHeaderView?.subHeaderText?.text = "PRE-AUTH COMPLETE"
        binding?.subHeaderView?.headerImage?.setImageResource((action as EDashboardItem).res)
       
        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }
        binding?.authCompleteBtn?.setOnClickListener {
            authData.authTid = binding?.tidEt?.text.toString()//""
            authData.authAmt = binding?.amountEt?.text.toString()
            authData.authInvoice = binding?.invoiceEt?.text.toString()

            if (authData.authTid.isNullOrBlank() || authData.authTid!!.length < 8) {
                showToast("Invalid TID")
                return@setOnClickListener
            } else if (authData.authInvoice.isNullOrBlank()) {
                showToast("Invalid Invoice")
                return@setOnClickListener
            }  else if (authData.authAmt.isNullOrBlank() || authData.authAmt!!.toDouble() < 1) {
                showToast("Invalid Amount")
                return@setOnClickListener
            } else {
                (activity as NavigationActivity).onFragmentRequest(
                    EDashboardItem.PREAUTH_COMPLETE,
                    Pair(authData, "")
                )
            }
        }
    }




}

class AuthCompletionData :Serializable{
    var authTid: String? = ""
    var authAmt: String? = ""
    var authInvoice: String? = ""
}