package com.bonushub.crdb.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.databinding.ItemEmiIssuerTenureBinding
import com.bonushub.crdb.view.fragments.TenureBankModal

//region===============Below adapter is used to show the All Tenure for Issuer Bank lists available:-
class IssuerTenureListAdapter(
    var issuerTenureList: MutableList<TenureBankModal>,
    var cb: (Int) -> Unit
) :
    RecyclerView.Adapter<IssuerTenureListAdapter.IssuerTenureListViewHolder>() {

    var index = -1
    //var absoluteAdapterPosition=0
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IssuerTenureListViewHolder {
        val itemBinding = ItemEmiIssuerTenureBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return IssuerTenureListViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: IssuerTenureListViewHolder, position: Int) {
        val modal = issuerTenureList[position]
        val tenureData = "${modal.bankTenure} Months"
        holder.viewBinding.tenureRadioButton.text = tenureData

        //region===============Below Code will only execute in case of Single Radio Button Selection:-
        holder.viewBinding.tenureRadioButton.isChecked = index == position
        //endregion
        holder. viewBinding.tenureRadioButton.setOnClickListener {
            cb( position)
            index =position
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int = issuerTenureList.size

    inner class IssuerTenureListViewHolder(var viewBinding: ItemEmiIssuerTenureBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        init {
            /*viewBinding.tenureRadioButton.setOnClickListener {
                cb( absoluteAdapterPosition)
                index = absoluteAdapterPosition
                notifyDataSetChanged()
            }*/
        }
    }

    //region==================Uncheck All Tenure RadioButtons:-
    fun unCheckAllTenureRadioButton() {
        index = -1
        notifyDataSetChanged()
    }
    //endregion

    //region===================Refresh EMI Tenure List:-
    fun refreshTenureList(refreshTenureList: MutableList<TenureBankModal>) {
        this.issuerTenureList = refreshTenureList
        notifyDataSetChanged()
    }
    //endregion
}
//endregion
