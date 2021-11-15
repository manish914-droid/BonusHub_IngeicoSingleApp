package com.bonushub.crdb.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.ItemBankFunctionsBinding
import com.bonushub.crdb.view.fragments.IBankFunctionsAdminVasItemClick
import com.bonushub.pax.utils.BankFunctionsAdminVasItem

class BankFunctionsAdminVasAdapter(var iBankFunctionsAdminVasItemClick: IBankFunctionsAdminVasItemClick?, private val listItem: MutableList<BankFunctionsAdminVasItem>) : RecyclerView.Adapter<BankFunctionsAdminVasAdapter.BankFunctionsAdminVasViewHolder>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BankFunctionsAdminVasViewHolder {

        val itemBinding = ItemBankFunctionsBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)
        return BankFunctionsAdminVasViewHolder(
            itemBinding
        )
    }

    override fun getItemCount(): Int = listItem.size


    override fun onBindViewHolder(holder: BankFunctionsAdminVasViewHolder, position: Int) {

        val model = listItem[position]

        holder.viewBinding.textView.text = model._name

        holder.viewBinding.relLayParent.setOnClickListener {

            iBankFunctionsAdminVasItemClick?.bankFunctionsAdminVasItemClick(model)
        }

    }



    inner class BankFunctionsAdminVasViewHolder(val viewBinding: ItemBankFunctionsBinding) : RecyclerView.ViewHolder(viewBinding.root) {
//        val textView: TextView = view.findViewById(R.id.textView)
//        val relLayParent: RelativeLayout = view.findViewById(R.id.relLayParent)

    }
}
