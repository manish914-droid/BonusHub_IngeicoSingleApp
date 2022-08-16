package com.bonushub.crdb.india.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.viewmodel.CardlessModel

class EmiCardCardLessAdapter (val mlist2:List<CardlessModel>): RecyclerView.Adapter<EmiCardCardLessAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emi_cardless_adapter, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mlist2[position]

        holder.imageView.setImageResource(item.cardimg)
    }

    override fun getItemCount(): Int {
        return mlist2.size
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView)
    {
        val imageView: ImageView = itemView.findViewById(R.id.bnk_img)
    }
}