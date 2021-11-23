package com.bonushub.crdb.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.databinding.ItemReportsBinding
import com.bonushub.crdb.view.fragments.IReportsFragmentItemClick
import com.bonushub.pax.utils.ReportsItem

class ReportsAdapter( private val listItem: MutableList<ReportsItem>, var iReportsFragmentItemClick: IReportsFragmentItemClick?) : RecyclerView.Adapter<ReportsAdapter.ReportsViewHolder>() {



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

        holder.viewBinding.relLayParent.setOnClickListener {

            iReportsFragmentItemClick?.ReportsOptionItemClick(model)
        }

    }



    inner class ReportsViewHolder(val viewBinding: ItemReportsBinding) : RecyclerView.ViewHolder(viewBinding.root) {


    }
}
