package com.bonushub.crdb.india.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.india.databinding.ItemBankFunctionsTerminalParamBinding
import com.bonushub.crdb.india.view.fragments.IBankFunctionsTableEditItemClick
import com.bonushub.crdb.india.view.fragments.TableEditHelper

class BankFunctionsTableEditAdapter(private var dataList:ArrayList<TableEditHelper?>, private var iBankFunctionsTableEditItemClick: IBankFunctionsTableEditItemClick?) : RecyclerView.Adapter<BankFunctionsTableEditAdapter.BankFunctionsTableEditViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BankFunctionsTableEditViewHolder {
        val itemBinding = ItemBankFunctionsTerminalParamBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)
        return BankFunctionsTableEditViewHolder(itemBinding)
    }

    override fun getItemCount(): Int = dataList.size


    override fun onBindViewHolder(holder: BankFunctionsTableEditViewHolder, position: Int) {

        val model = dataList[position]

        holder.viewBinding.textViewTitle.text = model?.titleName

        holder.viewBinding.textViewValue.text = model?.titleValue

        holder.viewBinding.imgViewEdit.setOnClickListener {
            iBankFunctionsTableEditItemClick?.bankFunctionsTableEditItemClick(position)
        }



    }



    inner class BankFunctionsTableEditViewHolder(val viewBinding: ItemBankFunctionsTerminalParamBinding) : RecyclerView.ViewHolder(viewBinding.root) {
//        val textViewTitle: TextView = view.findViewById(R.id.textViewTitle)
//        val textViewValue: TextView = view.findViewById(R.id.textViewValue)
//        val relLayParent: RelativeLayout = view.findViewById(R.id.relLayParent)
//        val imgViewEdit: ImageView = view.findViewById(R.id.imgViewEdit)

    }
}
