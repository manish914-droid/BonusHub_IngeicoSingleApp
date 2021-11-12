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

class BankFunctionsInitPaymentAppAdapter() : RecyclerView.Adapter<BankFunctionsInitPaymentAppAdapter.BankFunctionsViewHolder>() {

    var mList: ArrayList<EDashboardItem> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BankFunctionsViewHolder {
        return BankFunctionsViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_bank_functions_init_payment, parent, false)
        )
    }

    //override fun getItemCount(): Int = mList.size
    override fun getItemCount(): Int = 6

    override fun onBindViewHolder(holder: BankFunctionsViewHolder, position: Int) {


    }



    inner class BankFunctionsViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
       // val textView: TextView = view.findViewById(R.id.textView)

    }
}
