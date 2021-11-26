package com.bonushub.crdb.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.ItemBankFunctionsInitPaymentBinding
import com.bonushub.crdb.view.fragments.BankFunctionsAdminVasFragment
import com.bonushub.crdb.view.fragments.BankFunctionsFragment
import com.bonushub.crdb.view.fragments.TidsListModel
import com.bonushub.pax.utils.EDashboardItem
import java.util.*

class BankFunctionsInitPaymentAppAdapter(var tidsList:ArrayList<TidsListModel>) : RecyclerView.Adapter<BankFunctionsInitPaymentAppAdapter.BankFunctionsInitPaymentViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BankFunctionsInitPaymentViewHolder {

        var itemBinding = ItemBankFunctionsInitPaymentBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)

        return BankFunctionsInitPaymentViewHolder(itemBinding)
    }

    override fun getItemCount(): Int = tidsList.size


    override fun onBindViewHolder(holder: BankFunctionsInitPaymentViewHolder, position: Int) {

        var model = tidsList[position]

        if(position+1 == itemCount) {
            holder.viewBinding.viewLine.visibility = View.GONE
        }else{
            holder.viewBinding.viewLine.visibility = View.VISIBLE

        }

        holder.viewBinding.textViewTid.text = model.tids
        holder.viewBinding.textViewDes.text = model.des
        holder.viewBinding.textViewStatus.text = model.status

        if(model.status.equals("Success", true))
        {
            holder.viewBinding.imgViewStatus.setImageResource(R.drawable.ic_init_payment_success)
            holder.viewBinding.textViewStatus.text = model.status


        }else{
            holder.viewBinding.imgViewStatus.setImageResource(R.drawable.ic_init_payment_fail)
            holder.viewBinding.textViewStatus.text = model.status

        }

    }



    inner class BankFunctionsInitPaymentViewHolder(val viewBinding: ItemBankFunctionsInitPaymentBinding) : RecyclerView.ViewHolder(viewBinding.root) {

    }
}
