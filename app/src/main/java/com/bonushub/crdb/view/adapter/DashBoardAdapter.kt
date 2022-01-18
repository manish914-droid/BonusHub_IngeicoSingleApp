package com.bonushub.crdb.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

import com.bonushub.crdb.R
import com.bonushub.crdb.view.activity.IFragmentRequest

import com.bonushub.crdb.utils.EDashboardItem
import java.util.ArrayList

class DashBoardAdapter(private val fragReq: IFragmentRequest?, var lessMoreClick: (item: EDashboardItem) -> Unit
) : RecyclerView.Adapter<DashBoardAdapter.DashBoardViewHolder>() {

    var mList: ArrayList<EDashboardItem> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashBoardViewHolder {
        return DashBoardViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_dashboart, parent, false)
        )
    }

    override fun getItemCount(): Int = mList.size

    override fun onBindViewHolder(holder: DashBoardViewHolder, position: Int) {
        holder.logoIV.setImageResource(mList[position].res)
        // ContextCompat.getDrawable(holder.view.context, mList[position].res)
        holder.titleTV.text = mList[position].title
        holder.logoIV.setOnClickListener {
            if (mList[position] == EDashboardItem.LESS || mList[position] == EDashboardItem.MORE)
                lessMoreClick(mList[position])
            else
                fragReq?.onDashBoardItemClick(mList[position])

        }

    }

    fun onUpdatedItem(list: List<EDashboardItem>) {
        mList.clear()
        mList.addAll(list)

    }


    inner class DashBoardViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val logoIV: ImageView = view.findViewById(R.id.item_logo_iv)
        val titleTV: TextView = view.findViewById(R.id.item_title_tv)
        val itemParent: ConstraintLayout = view.findViewById(R.id.item_parent_rv)

    }
}
