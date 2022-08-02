package com.bonushub.crdb.india.view.adapter

import android.os.SystemClock
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.ItemReportsBinding
import com.bonushub.crdb.india.utils.ReportsItem
import com.bonushub.crdb.india.view.fragments.IReportsFragmentItemClick
import com.example.verifonevx990app.utils.setSafeOnClickListener

class ReportsAdapter(private val listItem: MutableList<ReportsItem>, var iReportsFragmentItemClick: IReportsFragmentItemClick?) : RecyclerView.Adapter<ReportsAdapter.ReportsViewHolder>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportsViewHolder {

        val itemBinding = ItemReportsBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)
        return ReportsViewHolder(
            itemBinding
        )
    }

    override fun getItemCount(): Int = listItem.size


    override fun onBindViewHolder(holder: ReportsViewHolder, position: Int) {

        val model = listItem[position]

        holder.viewBinding.textView.text = model._name
        holder.viewBinding.imgViewIcon.setImageResource(R.drawable.ic_receipt_new)
        holder.viewBinding.relLayParent.setBackgroundResource(R.drawable.edge_gray)

        holder.viewBinding.relLayParent.setOnClickListener {
            holder.viewBinding.relLayParent.setBackgroundResource(R.drawable.edge_brand_selected)
            iReportsFragmentItemClick?.ReportsOptionItemClick(model, position)
        }

    }



    inner class ReportsViewHolder(val viewBinding: ItemReportsBinding) : RecyclerView.ViewHolder(viewBinding.root)
}
