package com.bonushub.crdb.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.R
import com.bonushub.crdb.view.fragments.IBankFunctionItemClick
import com.bonushub.pax.utils.BankFunctionsItem

class BankFunctionsAdapter(private var iBankFunctionItemClick: IBankFunctionItemClick?, private val bankFunctionsItem: MutableList<BankFunctionsItem>) : RecyclerView.Adapter<BankFunctionsAdapter.BankFunctionsViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BankFunctionsViewHolder {
        return BankFunctionsViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_bank_functions, parent, false)
        )
    }

    override fun getItemCount(): Int = bankFunctionsItem.size

    override fun onBindViewHolder(holder: BankFunctionsViewHolder, position: Int) {

        holder.textView.text = bankFunctionsItem[position]._name

        /*when(position){
            0 ->{
                holder.textView.text = "ADMIN VAS"
            }

            1 ->{
                holder.textView.text = "ADMIN PAYMENT"
            }
        }*/


        holder.relLayParent.setOnClickListener {

          /*  if(bankFuntionsItem[position]._name.equals(BankFuntionsItem.ADMIN_VAS._name)){
                fragment.openSuperAdminDialog()

            }*/
            iBankFunctionItemClick?.bankFunctionItemClick(bankFunctionsItem[position])

        }

    }



    inner class BankFunctionsViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.textView)
        val relLayParent: RelativeLayout = view.findViewById(R.id.relLayParent)

    }
}
