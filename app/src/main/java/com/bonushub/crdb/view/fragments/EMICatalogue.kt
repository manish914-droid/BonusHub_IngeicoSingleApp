package com.bonushub.crdb.view.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bonushub.crdb.HDFCApplication


import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentEmiCatalogueBinding
import com.bonushub.crdb.db.AppDatabase
import com.bonushub.crdb.model.local.TerminalParameterTable
import com.bonushub.crdb.view.activity.IFragmentRequest
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.base.IDialog
import com.bonushub.pax.utils.EDashboardItem


class EMICatalogue : Fragment() {
    private var binding: FragmentEmiCatalogueBinding? = null
    private var iDialog: IDialog? = null
    private val dbObj : AppDatabase = AppDatabase.getInstance(HDFCApplication.appContext)
    private val action by lazy { arguments?.getSerializable("type") ?: "" }
   private var tptData: TerminalParameterTable? = null
    private var iFrReq: IFragmentRequest? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IFragmentRequest) iFrReq = context
        if (context is IDialog) iDialog = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentEmiCatalogueBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.emi_catalogue)
        (action as? EDashboardItem)?.res?.let {
            binding?.subHeaderView?.headerImage?.setImageResource(it)
        }
        binding?.subHeaderView?.backImageButton?.setOnClickListener { parentFragmentManager.popBackStackImmediate() }
        binding?.subHeaderView?.headerHome?.visibility= View.VISIBLE
        binding?.subHeaderView?.headerHome?.setOnClickListener { parentFragmentManager.popBackStackImmediate() }
        Log.d("EMI Catalogue Action:- ", (action as EDashboardItem).toString())

//11111011000000000000
        //region================Brand and Bank EMI Catalogue Button Hide/Show Conditions:-
        /* if (tptData?.reservedValues?.substring(9, 10) == "1" && tptData?.reservedValues?.substring(
                 5,
                 6
             ) == "1"
         ) {
             binding?.brandEmiCv?.visibility = View.VISIBLE
             binding?.bankEmiCv?.visibility = View.VISIBLE
         } else if (tptData?.reservedValues?.substring(
                 9,
                 10
             ) == "1" && tptData?.reservedValues?.substring(5, 6) == "0"
         ) {
             binding?.brandEmiCv?.visibility = View.VISIBLE
             binding?.bankEmiCv?.visibility = View.GONE
         } else if (tptData?.reservedValues?.substring(
                 9,
                 10
             ) == "0" && tptData?.reservedValues?.substring(5, 6) == "1"
         ) {
             binding?.brandEmiCv?.visibility = View.GONE
             binding?.bankEmiCv?.visibility = View.VISIBLE
         }*/
  /*      tptData?.let {
            enabledEmiOptions(it) { isBankEmiOn, isBrandEmiOn ->
                if (isBankEmiOn) {
                    binding?.bankEmiCv?.visibility = View.VISIBLE
                }
                if (isBrandEmiOn) {
                    binding?.brandEmiCv?.visibility = View.VISIBLE
                }

            }
        }*/


        //endregion

        //region================Navigate to NewInputAmount Fragment on Click Event of BankEMI Button:-
        binding?.bankEmiCv?.setOnClickListener {
            (activity as NavigationActivity).transactFragment(NewInputAmountFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("type", EDashboardItem.BANK_EMI_CATALOGUE)
                   // putString(NavigationActivity.INPUT_SUB_HEADING, "")
                }
            })
        }
        //endregion

        //region================Navigate BrandEMI Page by onClick event of BrandEMI Button:-
/*        binding?.br?.setOnClickListener {
            (activity as NavigationActivity).transactFragment(BrandEMIMasterCategoryFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("type", EDashboardItem.BRAND_EMI_CATALOGUE)
                    //putString(MainActivity.INPUT_SUB_HEADING, "")
                }
            })
        }*/
        //endregion
    }

    override fun onDetach() {
        super.onDetach()
        iDialog = null
    }
}