package com.bonushub.crdb.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.databinding.ItemBankFunctionsTerminalParamBinding
import com.bonushub.crdb.view.fragments.IBankFunctionsTerminalItemClick
import com.bonushub.crdb.view.fragments.TableEditHelper

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
