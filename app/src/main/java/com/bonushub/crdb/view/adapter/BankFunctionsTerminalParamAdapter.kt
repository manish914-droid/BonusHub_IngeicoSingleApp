package com.bonushub.crdb.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.R
import com.bonushub.crdb.view.fragments.IBankFunctionsTerminalItemClick
import com.bonushub.pax.utils.BankFunctionsTerminalItem

class BankFunctionsTerminalParamAdapter(private var iBankFunctionsTerminalItemClick: IBankFunctionsTerminalItemClick?, private var listItem: MutableList<BankFunctionsTerminalItem>) : RecyclerView.Adapter<BankFunctionsTerminalParamAdapter.BankFunctionsViewHolder>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BankFunctionsViewHolder {
        return BankFunctionsViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_bank_functions_terminal_param, parent, false)
        )
    }

    override fun getItemCount(): Int = listItem.size


    override fun onBindViewHolder(holder: BankFunctionsViewHolder, position: Int) {

        holder.textViewTitle.text = listItem[position]._name


        // temparary
        when(position){
            0 ->{
                //holder.textViewTitle.text = "BATCH NUMBER"
                holder.textViewValue.text = "000184"
            }

            1 ->{
                //holder.textViewTitle.text = "INVOICE NUMBER "
                holder.textViewValue.text = "000403"
            }

            2 ->{
                //holder.textViewTitle.text = "MERCHANT ID"
                holder.textViewValue.text = "0000014512"
            }

            3 ->{
                //holder.textViewTitle.text = "STN"
                holder.textViewValue.text = "000021"
            }

            4 ->{
                //holder.textViewTitle.text = "TERMINAL ID"
                holder.textViewValue.text = "41501369"
            }

            5 ->{
               // holder.textViewTitle.text = "CLEAR FBATCH"
                holder.textViewValue.text = "0"
            }
        }

        holder.imgViewEdit.setOnClickListener {
            iBankFunctionsTerminalItemClick?.bankFunctionsTerminalItemClick(listItem[position])
        }



    }



    inner class BankFunctionsViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val textViewTitle: TextView = view.findViewById(R.id.textViewTitle)
        val textViewValue: TextView = view.findViewById(R.id.textViewValue)
        val relLayParent: RelativeLayout = view.findViewById(R.id.relLayParent)
        val imgViewEdit: ImageView = view.findViewById(R.id.imgViewEdit)

    }
}
