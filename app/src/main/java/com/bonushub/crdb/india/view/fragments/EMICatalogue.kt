package com.bonushub.crdb.india.view.fragments


import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bonushub.crdb.india.HDFCApplication
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentEmiCatalogueBinding
import com.bonushub.crdb.india.db.AppDatabase
import com.bonushub.crdb.india.model.local.TerminalParameterTable
import com.bonushub.crdb.india.utils.EDashboardItem
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp
import com.bonushub.crdb.india.utils.refreshSubToolbarLogos
import com.bonushub.crdb.india.view.activity.IFragmentRequest
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.base.IDialog
import com.bonushub.crdb.india.viewmodel.EmiCatalogueViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EMICatalogue : Fragment() {
    private var binding: FragmentEmiCatalogueBinding? = null
    private var iDialog: IDialog? = null
    private val dbObj : AppDatabase = AppDatabase.getInstance(HDFCApplication.appContext)
    private val emiCatalogueViewModel : EmiCatalogueViewModel by viewModels()
    private val action by lazy { arguments?.getSerializable("type") ?: "" }
  // private var tptData: TerminalParameterTable? = null
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
        /*binding?.subHeaderView?.subHeaderText?.text = getString(R.string.emi_catalogue)
        (action as? EDashboardItem)?.res?.let {
            binding?.subHeaderView?.headerImage?.setImageResource(it)
        }*/
        refreshSubToolbarLogos(this,null,(action as? EDashboardItem)?.res?:0, getString(R.string.emi_catalogue))

        binding?.subHeaderView?.backImageButton?.setOnClickListener { parentFragmentManager.popBackStackImmediate() }
        binding?.subHeaderView?.headerHome?.visibility= View.VISIBLE
        binding?.subHeaderView?.headerHome?.setOnClickListener { parentFragmentManager.popBackStackImmediate() }
        Log.d("EMI Catalogue Action:- ", (action as EDashboardItem).toString())

        getTptData()

        //region================Navigate to NewInputAmount Fragment on Click Event of BankEMI Button:-
        binding?.bankEmiCv?.setOnClickListener {
            (activity as NavigationActivity).transactFragment(NewInputAmountFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("type", EDashboardItem.BANK_EMI_CATALOGUE)
                     putString(NavigationActivity.INPUT_SUB_HEADING, "")
                }
            })
        }
        //endregion

        //region================Navigate BrandEMI Page by onClick event of BrandEMI Button:-
        binding?.brandEmiCv?.setOnClickListener {
            (activity as NavigationActivity).transactFragment(BrandEmiMasterCategoryFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("type", EDashboardItem.BRAND_EMI_CATALOGUE)
                    putString(NavigationActivity.INPUT_SUB_HEADING, "")
                }
            })
        }
        //endregion
    }

     private fun getTptData(){
         lifecycleScope.launch(Dispatchers.Main) {
             emiCatalogueViewModel.getTerminalParameterTable()?.observe(viewLifecycleOwner) {
                 //11111011000000000000
                 //region================Brand and Bank EMI Catalogue Button Hide/Show Conditions:-
                 it?.let {
                     enabledEmiOptions(it) { isBankEmiOn, isBrandEmiOn ->
                         if (isBankEmiOn) {
                             binding?.bankEmiCv?.visibility = View.VISIBLE
                         }
                         if (isBrandEmiOn) {
                             binding?.brandEmiCv?.visibility = View.VISIBLE
                         }

                     }
                 }


                 //endregion

             }
         }
    }
    private fun enabledEmiOptions(tpt: TerminalParameterTable, cb: (Boolean, Boolean) -> Unit) {
        var brandEmiOn = false
        var bankEmiOn = false
      val tpt2= Field48ResponseTimestamp.getTptData()


        if (tpt2 != null) {
            when (
                tpt2.reservedValues[6]) {
                '1' -> {
                    // bank emi on
                    bankEmiOn = true
                }
            }
        }
        if (tpt2 != null) {
            when (tpt2.reservedValues[10]) {
                '1' -> {
                    // brand emi on
                    brandEmiOn = true
                }


            }
        }
        cb(bankEmiOn, brandEmiOn)

    }
    override fun onDetach() {
        super.onDetach()
        iDialog = null
    }
}