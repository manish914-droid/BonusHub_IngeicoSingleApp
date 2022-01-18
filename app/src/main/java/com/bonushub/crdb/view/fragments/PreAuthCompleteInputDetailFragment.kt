package com.bonushub.crdb.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentPreAuthCompleteDetailBinding
import com.bonushub.crdb.model.CardProcessedDataModal
import com.bonushub.crdb.utils.Field48ResponseTimestamp.showToast
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.base.IDialog
import com.bonushub.crdb.utils.EDashboardItem
import java.io.Serializable
import java.util.*

class PreAuthCompleteInputDetailFragment : Fragment() {
   // private val title: String by lazy { arguments?.getString(MainActivity.INPUT_SUB_HEADING) ?: "" }
    private val cardProcessedData: CardProcessedDataModal by lazy { CardProcessedDataModal() }
    private val authData: AuthCompletionData by lazy { AuthCompletionData() }
    private var binding: FragmentPreAuthCompleteDetailBinding? = null
    private val action by lazy { arguments?.getSerializable("type") ?: "" }
    private var iDiag: IDialog? = null
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
        iDiag = (activity as NavigationActivity)
        binding?.subHeaderView?.subHeaderText?.text = "PRE-AUTH COMPLETE"
        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_preauth)
       
        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }
        binding?.authCompleteBtn?.setOnClickListener {

            iDiag?.alertBoxWithAction("","Do you want to PreAuth Complete this transaction?",true,"YES",{
                authData.authTid = binding?.tidEt?.text.toString()//""
                authData.authAmt = binding?.amountEt?.text.toString()
                authData.authInvoice = binding?.invoiceEt?.text.toString()

                if (authData.authTid.isNullOrBlank() || authData.authTid!!.length < 8) {
                    showToast("Invalid TID")
                    return@alertBoxWithAction
                } else if (authData.authInvoice.isNullOrBlank()) {
                    showToast("Invalid Invoice")
                    return@alertBoxWithAction
                }  else if (authData.authAmt.isNullOrBlank() || authData.authAmt!!.toDouble() < 1) {
                    showToast("Invalid Amount")
                    return@alertBoxWithAction
                } else {
                    (activity as NavigationActivity).onFragmentRequest(
                        EDashboardItem.PREAUTH_COMPLETE,
                        Pair(authData, "")
                    )
                }
            },{

            })

        }
    }




}

class AuthCompletionData :Serializable{
    var authTid: String? = ""
    var authAmt: String? = ""
    var authInvoice: String? = ""
}