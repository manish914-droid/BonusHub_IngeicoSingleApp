package com.bonushub.crdb.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.ItemBankFunctionsTerminalParamBinding
import com.bonushub.crdb.view.fragments.IBankFunctionsTerminalItemClick
import com.bonushub.crdb.view.fragments.TableEditHelper
import com.bonushub.pax.utils.BankFunctionsTerminalItem

class BankFunctionsTerminalParamAdapter(private var dataList:ArrayList<TableEditHelper?>, private var iBankFunctionsTerminalItemClick: IBankFunctionsTerminalItemClick?) : RecyclerView.Adapter<BankFunctionsTerminalParamAdapter.BankFunctionsTerminalParamViewHolder>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BankFunctionsTerminalParamViewHolder {
        val itemBinding = ItemBankFunctionsTerminalParamBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)
        return BankFunctionsTerminalParamViewHolder(itemBinding)
    }

    override fun getItemCount(): Int = dataList.size


    override fun onBindViewHolder(holder: BankFunctionsTerminalParamViewHolder, position: Int) {

        val model = dataList[position]

        holder.viewBinding.textViewTitle.text = model?.titleName

        holder.viewBinding.textViewValue.text = model?.titleValue

        // temparary
        /*when(position){
            0 ->{
                //holder.textViewTitle.text = "BATCH NUMBER"
                holder.viewBinding.textViewValue.text = "000184"
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
        }*/

        holder.viewBinding.imgViewEdit.setOnClickListener {
            iBankFunctionsTerminalItemClick?.bankFunctionsTerminalItemClick(position)
        }



    }



    inner class BankFunctionsTerminalParamViewHolder(val viewBinding: ItemBankFunctionsTerminalParamBinding) : RecyclerView.ViewHolder(viewBinding.root) {
//        val textViewTitle: TextView = view.findViewById(R.id.textViewTitle)
//        val textViewValue: TextView = view.findViewById(R.id.textViewValue)
//        val relLayParent: RelativeLayout = view.findViewById(R.id.relLayParent)
//        val imgViewEdit: ImageView = view.findViewById(R.id.imgViewEdit)

    }
}
