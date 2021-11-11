package com.bonushub.crdb.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.R
import com.bonushub.crdb.view.fragments.BankFunctionsAdminVasFragment
import com.bonushub.crdb.view.fragments.BankFunctionsFragment
import com.bonushub.pax.utils.EDashboardItem
import java.util.*

class BankFunctionsAdminVasAdapter(var fragment: BankFunctionsAdminVasFragment) : RecyclerView.Adapter<BankFunctionsAdminVasAdapter.BankFunctionsViewHolder>() {

    var mList: ArrayList<EDashboardItem> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BankFunctionsViewHolder {
        return BankFunctionsViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_bank_functions, parent, false)
        )
    }

    //override fun getItemCount(): Int = mList.size
    override fun getItemCount(): Int = 6

    override fun onBindViewHolder(holder: BankFunctionsViewHolder, position: Int) {

        when(position){
            0 ->{
                holder.textView.text = "INIT"
            }

            1 ->{
                holder.textView.text = "TEST EMI"
            }

            2 ->{
                holder.textView.text = "TERMINAL PARAM"
            }

            3 ->{
                holder.textView.text = "COMM PARAM"
            }

            4 ->{
                holder.textView.text = "ENV PARAM"
            }

            5 ->{
                holder.textView.text = "INIT PAYMENT APP"
            }
        }


        holder.relLayParent.setOnClickListener {

            fragment.itemClick(position)
        }

    }



    inner class BankFunctionsViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.textView)
        val relLayParent: RelativeLayout = view.findViewById(R.id.relLayParent)

    }
}