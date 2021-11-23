package com.bonushub.crdb.view.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.databinding.ItemReportsBinding
import com.bonushub.crdb.databinding.ItemSummaryReport1Binding
import com.bonushub.crdb.databinding.ItemSummaryReportBinding
import com.bonushub.crdb.view.fragments.IReportsFragmentItemClick
import com.bonushub.crdb.view.fragments.SummaryReportModel
import com.bonushub.crdb.view.fragments.SummaryReportSubModel
import com.bonushub.pax.utils.ReportsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReportsSummaryAdapter(var context:Context,var dataList:ArrayList<SummaryReportModel>) : RecyclerView.Adapter<ReportsSummaryAdapter.ReportsViewHolder>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportsViewHolder {

        val itemBinding = ItemSummaryReportBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)
        return ReportsViewHolder(
            itemBinding
        )
    }

    override fun getItemCount(): Int = dataList.size
    //override fun getItemCount(): Int = 2


    override fun onBindViewHolder(holder: ReportsViewHolder, position: Int) {

        val model = dataList[position]

        if(model.heading.isEmpty()){
            holder.viewBinding.textViewHeader.visibility = View.GONE
        }else {
            holder.viewBinding.textViewHeader.visibility = View.VISIBLE
            holder.viewBinding.textViewHeader.text = model.heading
        }
//
//        holder.viewBinding.relLayParent.setOnClickListener {
//
//            iReportsFragmentItemClick?.ReportsOptionItemClick(model)
//        }

        setupRecyclerview(holder.viewBinding.recyclerView,context, model.subList)
    }



    inner class ReportsViewHolder(val viewBinding: ItemSummaryReportBinding) : RecyclerView.ViewHolder(viewBinding.root) {


    }

    private fun setupRecyclerview(recyclerView:RecyclerView, context:Context, subdataList:ArrayList<SummaryReportSubModel>){
       // withContext(Dispatchers.Main) {
           // binding?.let {
                recyclerView.layoutManager = GridLayoutManager(context, 1)
                recyclerView.adapter = ReportsSummarySubAdapter(subdataList)
           // }

       // }
    }
}


// region sub item adapter
class ReportsSummarySubAdapter(var subdataList:ArrayList<SummaryReportSubModel>) : RecyclerView.Adapter<ReportsSummarySubAdapter.ReportsSubViewHolder>() {




    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportsSubViewHolder {

        val itemBinding = ItemSummaryReport1Binding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)
        return ReportsSubViewHolder(
            itemBinding
        )

    }

    override fun onBindViewHolder(holder: ReportsSubViewHolder, position: Int) {

        var model = subdataList[position]

        holder.viewBinding.txtViewHeading.text = model.heading
        holder.viewBinding.txtViewCount.text = model.count
        holder.viewBinding.txtViewTotal.text = model.total
    }

    override fun getItemCount(): Int = subdataList.size

    inner class ReportsSubViewHolder(val viewBinding: ItemSummaryReport1Binding) : RecyclerView.ViewHolder(viewBinding.root) {


    }
}

// end region

