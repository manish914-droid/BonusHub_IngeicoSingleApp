package com.bonushub.crdb.india.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.ItemReportsBinding
import com.bonushub.crdb.india.utils.BankFunctionsAdminVasItem
import com.bonushub.crdb.india.view.fragments.IBankFunctionsAdminVasItemClick

class BankFunctionsAdminVasAdapter(var iBankFunctionsAdminVasItemClick: IBankFunctionsAdminVasItemClick?, private val listItem: MutableList<BankFunctionsAdminVasItem>) : RecyclerView.Adapter<BankFunctionsAdminVasAdapter.BankFunctionsAdminVasViewHolder>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BankFunctionsAdminVasViewHolder {

      //  val itemBinding = ItemBankFunctionsBinding.inflate(LayoutInflater.from(parent.context),
        val itemBinding = ItemReportsBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)
        return BankFunctionsAdminVasViewHolder(
            itemBinding
        )
    }

    override fun getItemCount(): Int = listItem.size


    override fun onBindViewHolder(holder: BankFunctionsAdminVasViewHolder, position: Int) {

        val model = listItem[position]

        holder.viewBinding.relLayParent.setBackgroundResource(R.drawable.edge_gray)
        holder.viewBinding.imgViewIcon.setImageResource(R.drawable.ic_bankfunction_new)
        holder.viewBinding.textView.text = model._name

        holder.viewBinding.relLayParent.setOnClickListener {

            holder.viewBinding.relLayParent.setBackgroundResource(R.drawable.edge_brand_selected)
            iBankFunctionsAdminVasItemClick?.bankFunctionsAdminVasItemClick(model,position)
        }

    }



    inner class BankFunctionsAdminVasViewHolder(val viewBinding: ItemReportsBinding) : RecyclerView.ViewHolder(viewBinding.root) {
//        val textView: TextView = view.findViewById(R.id.textView)
//        val relLayParent: RelativeLayout = view.findViewById(R.id.relLayParent)

    }
}
