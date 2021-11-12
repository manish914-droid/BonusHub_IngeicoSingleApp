package com.bonushub.crdb.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.R
import com.bonushub.crdb.view.fragments.IBankFunctionsAdminVasItemClick
import com.bonushub.pax.utils.BankFunctionsAdminVasItem

class BankFunctionsAdminVasAdapter(var iBankFunctionsAdminVasItemClick: IBankFunctionsAdminVasItemClick?, private val listItem: MutableList<BankFunctionsAdminVasItem>) : RecyclerView.Adapter<BankFunctionsAdminVasAdapter.BankFunctionsViewHolder>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BankFunctionsViewHolder {
        return BankFunctionsViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_bank_functions, parent, false)
        )
    }

    override fun getItemCount(): Int = listItem.size


    override fun onBindViewHolder(holder: BankFunctionsViewHolder, position: Int) {

        holder.textView.text = listItem[position]._name


        holder.relLayParent.setOnClickListener {

            iBankFunctionsAdminVasItemClick?.bankFunctionsAdminVasItemClick(listItem[position])
        }

    }



    inner class BankFunctionsViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.textView)
        val relLayParent: RelativeLayout = view.findViewById(R.id.relLayParent)

    }
}
