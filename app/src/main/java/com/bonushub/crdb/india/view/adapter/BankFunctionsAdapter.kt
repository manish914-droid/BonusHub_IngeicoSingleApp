package com.bonushub.crdb.india.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.india.databinding.ItemBankFunctionsBinding
import com.bonushub.crdb.india.view.fragments.IBankFunctionItemClick
import com.bonushub.crdb.india.utils.BankFunctionsItem

class BankFunctionsAdapter(private var iBankFunctionItemClick: IBankFunctionItemClick?, private val bankFunctionsItem: MutableList<BankFunctionsItem>) : RecyclerView.Adapter<BankFunctionsAdapter.BankFunctionsViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BankFunctionsViewHolder {

        val itemBinding = ItemBankFunctionsBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)
        return BankFunctionsViewHolder(itemBinding)
        /*return BankFunctionsViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_bank_functions, parent, false)
        )*/
    }

    override fun getItemCount(): Int = bankFunctionsItem.size

    override fun onBindViewHolder(holder: BankFunctionsViewHolder, position: Int) {

        val model = bankFunctionsItem[position]
        holder.viewBinding.textView.text = model._name


        holder.viewBinding.relLayParent.setOnClickListener {

            iBankFunctionItemClick?.bankFunctionItemClick(model)

        }

    }



    inner class BankFunctionsViewHolder(val viewBinding: ItemBankFunctionsBinding) : RecyclerView.ViewHolder(viewBinding.root) {
//        val textView: TextView = view.findViewById(R.id.textView)
//        val relLayParent: RelativeLayout = view.findViewById(R.id.relLayParent)

    }
}
