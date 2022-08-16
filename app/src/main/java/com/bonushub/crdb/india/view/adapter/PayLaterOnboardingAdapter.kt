package com.bonushub.crdb.india.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.viewmodel.ModelOnboarding

class PayLaterOnboardingAdapter(val mlist:List<ModelOnboarding>): RecyclerView.Adapter<PayLaterOnboardingAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view=LayoutInflater.from(parent.context).inflate(R.layout.item_pay_later_onboarding,parent,false)

        return ViewHolder(view)

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val listposition=mlist[position]

        holder.imagevw.setImageResource(listposition.img)
        holder.textvw.text=listposition.itemname
    }

    override fun getItemCount(): Int {
        return mlist.size
    }
    class ViewHolder(ItemView:View):RecyclerView.ViewHolder(ItemView)
    {
        val imagevw:ImageView=itemView.findViewById(R.id.itm_img_onboarding)
        val textvw:TextView=itemView.findViewById(R.id.item_name)
    }
}