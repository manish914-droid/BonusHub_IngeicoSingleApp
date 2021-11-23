package com.bonushub.crdb.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentReportSummaryBinding
import com.bonushub.crdb.utils.logger
import com.bonushub.crdb.view.adapter.ReportsSummaryAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ReportSummaryFragment : Fragment() {


    lateinit var dataList:ArrayList<SummaryReportModel>
    lateinit var subdataList:ArrayList<SummaryReportSubModel>
    lateinit var subdataList2:ArrayList<SummaryReportSubModel>

    var binding:FragmentReportSummaryBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentReportSummaryBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.summary_report)

        dataList = ArrayList()
        // regioin temp
        subdataList = ArrayList()
        subdataList2 = ArrayList()

        var item1 = SummaryReportSubModel("Card Issuer:","Rupay Prepaid","")
        var item2 = SummaryReportSubModel("TXN Type:","Count","Total")
        var item3 = SummaryReportSubModel("Sale:","4","2565.00")
        var item4 = SummaryReportSubModel("EMI Sale:","1","2565.00")

        subdataList.add(item1)
        subdataList.add(item2)
        subdataList.add(item3)
        subdataList.add(item4)

        subdataList2.add(item3)
        subdataList2.add(item4)

        dataList.add(SummaryReportModel("",subdataList))
        dataList.add(SummaryReportModel("TOTAL TRANSACTION",subdataList2))
        // end region
        setupRecyclerview()

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }

        binding?.textViewEmailReport?.setOnClickListener {
            logger("EmailReport","click")
        }

        binding?.textViewPrint?.setOnClickListener {
            logger("Print","click")
        }
    }

    private fun setupRecyclerview(){
        lifecycleScope.launch(Dispatchers.Main) {
            binding?.let {
                it.recyclerView.layoutManager = GridLayoutManager(activity, 1)
                it.recyclerView.adapter = ReportsSummaryAdapter(requireContext(), dataList)
            }

        }
    }
}

data class SummaryReportModel(var heading:String, var subList:ArrayList<SummaryReportSubModel>)
data class SummaryReportSubModel(var heading:String, var count:String, var total:String)
