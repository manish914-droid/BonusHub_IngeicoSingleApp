package com.bonushub.crdb.india.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.ItemBankFunctionsBinding
import com.bonushub.crdb.india.databinding.ItemReportsBinding
import com.bonushub.crdb.india.view.fragments.ICommunicationOptionFragmentItemClick
import com.bonushub.crdb.india.utils.CommunicationParamItem

class BankFunctionsCommParamAdapter(var iCommunicationOptionFragmentItemClick: ICommunicationOptionFragmentItemClick?, private val listItem: MutableList<CommunicationParamItem>) : RecyclerView.Adapter<BankFunctionsCommParamAdapter.BankFunctionsCommParamViewHolder>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BankFunctionsCommParamViewHolder {

        val itemBinding = ItemReportsBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)
        return BankFunctionsCommParamViewHolder(
            itemBinding
        )
    }

    override fun getItemCount(): Int = listItem.size


    override fun onBindViewHolder(holder: BankFunctionsCommParamViewHolder, position: Int) {

        val model = listItem[position]

        holder.viewBinding.textView.text = model._name
        holder.viewBinding.imgViewIcon.setImageResource(R.drawable.ic_bankfunction_new)
        holder.viewBinding.relLayParent.setOnClickListener {

            holder.viewBinding.relLayParent.setBackgroundResource(R.drawable.edge_brand_selected)
            iCommunicationOptionFragmentItemClick?.CommunicationOptionItemClick(model)
        }

    }



    inner class BankFunctionsCommParamViewHolder(val viewBinding: ItemReportsBinding) : RecyclerView.ViewHolder(viewBinding.root) {


    }
}
