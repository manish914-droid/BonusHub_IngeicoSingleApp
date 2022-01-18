package com.bonushub.crdb.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.databinding.ItemBankFunctionsBinding
import com.bonushub.crdb.view.fragments.ICommunicationOptionFragmentItemClick
import com.bonushub.crdb.utils.CommunicationParamItem

class BankFunctionsCommParamAdapter(var iCommunicationOptionFragmentItemClick: ICommunicationOptionFragmentItemClick?, private val listItem: MutableList<CommunicationParamItem>) : RecyclerView.Adapter<BankFunctionsCommParamAdapter.BankFunctionsCommParamViewHolder>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BankFunctionsCommParamViewHolder {

        val itemBinding = ItemBankFunctionsBinding.inflate(LayoutInflater.from(parent.context),
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

        holder.viewBinding.relLayParent.setOnClickListener {

            iCommunicationOptionFragmentItemClick?.CommunicationOptionItemClick(model)
        }

    }



    inner class BankFunctionsCommParamViewHolder(val viewBinding: ItemBankFunctionsBinding) : RecyclerView.ViewHolder(viewBinding.root) {


    }
}
