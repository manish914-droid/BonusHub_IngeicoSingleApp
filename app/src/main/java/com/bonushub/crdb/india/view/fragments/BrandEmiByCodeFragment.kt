package com.bonushub.crdb.india.view.fragments

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bonushub.crdb.india.HDFCApplication
import com.bonushub.crdb.india.R

import com.bonushub.crdb.india.db.AppDatabase
import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.repository.ServerRepository
import com.bonushub.crdb.india.serverApi.RemoteService
import com.bonushub.crdb.india.utils.BhTransactionType
import com.bonushub.crdb.india.utils.EDashboardItem
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.showToast
import com.bonushub.crdb.india.utils.ProcessingCode
import com.bonushub.crdb.india.view.activity.TransactionActivity


class BrandEmiByCodeFragment : Fragment() {
    /** need to use Hilt for instance initializing here..*/
    private val remoteService: RemoteService = RemoteService()
    private val dbObj: AppDatabase = AppDatabase.getInstance(HDFCApplication.appContext)
    private val serverRepository: ServerRepository = ServerRepository(dbObj, remoteService)

    var binding: com.bonushub.crdb.india.databinding.FragmentBrandEmiByCodeBinding? = null

    private var cardProcessedDataModal: CardProcessedDataModal? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = com.bonushub.crdb.india.databinding.FragmentBrandEmiByCodeBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }
        binding?.subHeaderView?.subHeaderText?.text = "BRAND EMI BYCODE"
        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_brand_emi_code)
        cardProcessedDataModal = arguments?.getSerializable("cardProcessedData") as? CardProcessedDataModal?



        binding?.txtViewSubmit?.setOnClickListener {
            if (!TextUtils.isEmpty(binding?.edtTextEnterCode?.text?.toString())) {
                binding?.edtTextEnterCode?.text?.toString()?.let { it1 ->
                    startActivity(
                        Intent(
                            activity,
                            TransactionActivity::class.java
                        ).apply {
                           // val formattedTransAmount = "%.2f".format(amt.toDouble())
                         //   putExtra("saleAmt", formattedTransAmount)
                            val accessCode=binding?.edtTextEnterCode?.text?.toString()
                            putExtra("type", BhTransactionType.BRAND_EMI_BY_ACCESS_CODE.type)
                            putExtra("proc_code", ProcessingCode.SALE.code)
                            putExtra("brandAccessCode", accessCode)

                            putExtra("edashboardItem",  EDashboardItem.EMI_PRO)
                        }
                    )

                }
            }else
   showToast(getString(R.string.please_enter_access_code))


            // temp
           /* DialogUtilsNew1.showBrandEmiByCodeDetailsDialog(requireContext(),"Citi",
                "FX-A7s/1545KIT-EElH","APS-C Low","6 months","2565.00","2565.00","2565.00"){

            }*/
        }

    }
}