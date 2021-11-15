package com.bonushub.crdb.view.adapter

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.databinding.ItemBrandEmiMasterBinding
import com.bonushub.crdb.databinding.ItemEmiSchemeOfferBinding
import com.bonushub.crdb.model.remote.BankEMITenureDataModal
import com.bonushub.crdb.model.remote.BrandEMIProductDataModal
import com.bonushub.crdb.utils.divideAmountBy100
import com.bonushub.crdb.vxutils.Utility

internal class EMISchemeAndOfferAdapter(private val transactionType: Int,private val emiSchemeDataList: MutableList<BankEMITenureDataModal>?, private var schemeSelectCB: (Int) -> Unit) : RecyclerView.Adapter<EMISchemeAndOfferAdapter.EMISchemeOfferHolder>()
{

    private var index = -1

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): EMISchemeOfferHolder {
        val inflater: ItemEmiSchemeOfferBinding =
            ItemEmiSchemeOfferBinding.inflate(LayoutInflater.from(p0.context), p0, false)
        return EMISchemeOfferHolder(inflater)
    }

    override fun getItemCount(): Int {
        return emiSchemeDataList?.size ?: 0
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: EMISchemeOfferHolder, position: Int) {
        val modelData = emiSchemeDataList?.get(position)
        if (modelData != null) {
            // (((modelData.transactionAmount).toDouble()).div(100)).toString()
            "%.2f".format((((modelData.transactionAmount).toDouble()).div(100)).toString().toDouble())

            holder.binding.tvTransactionAmount.text = "\u20B9 " +  "%.2f".format((((modelData.transactionAmount).toDouble()).div(100)).toString().toDouble())
            holder.binding.tvLoanAmount.text = "\u20B9 " +   "%.2f".format((((modelData.loanAmount).toDouble()).div(100)).toString().toDouble())
            holder.binding.tvEmiAmount.text = "\u20B9 " +   "%.2f".format((((modelData.emiAmount).toDouble()).div(100)).toString().toDouble())
            val tenureDuration = "${modelData.tenure} Months"
            val tenureHeadingDuration = "${modelData.tenure} Months Scheme"
            holder.binding.tvTenure.text = tenureDuration
            holder.binding.tenureHeadingTv.text = tenureHeadingDuration

            //If Discount Amount Available show this else if CashBack Amount show that:-
            if (!modelData.discountAmount.isNullOrEmpty() && modelData.discountAmount.toInt() != 0) {
                holder.binding.tvDiscountAmount.text = "\u20B9 " + "%.2f".format((((modelData.discountAmount).toDouble()).div(100)).toString().toDouble())
                holder.binding.discountLL.visibility = View.VISIBLE
                holder.binding.cashBackLL.visibility = View.GONE
            }
            if (!modelData.cashBackAmount.isNullOrEmpty() && modelData.cashBackAmount.toInt() != 0) {
                holder.binding.tvCashbackAmount.text = "\u20B9 " + divideAmountBy100(modelData.cashBackAmount.toInt()).toString()
                holder.binding.cashBackLL.visibility = View.VISIBLE
                holder.binding.discountLL.visibility = View.GONE
            }

            /*if(transactionType != TransactionType.TEST_EMI.type) {
                holder.binding.toatalemipayLL.visibility = View.VISIBLE//tenureInterestRate
                holder.binding.tvTotalInterestPay.text = "\u20B9 " +  "%.2f".format((((modelData.totalInterestPay).toDouble()).div(100)).toString().toDouble())
                val roi=  "%.2f".format((((modelData.tenureInterestRate).toDouble()).div(100)).toString().toDouble())
                //totalinterestpay
                holder.binding.tvRoi.text = "$roi %"
                holder.binding.tvTotalEmiPay.text = "\u20B9 " + "%.2f".format((((modelData.netPay).toDouble()).div(100)).toString().toDouble())
            }*/
          //  else {
                holder.binding.toatalemipayLL.visibility = View.GONE
                holder.binding.tvInterestRate.text =  ""+divideAmountBy100(modelData.tenureInterestRate.toInt()).toString() +" %"
                holder.binding.tvTotalInterestPay.text = "\u20B9 " + "%.2f".format((((modelData.totalInterestPay).toDouble()).div(100)).toString().toDouble())

                val roi=  "%.2f".format((((modelData.tenureInterestRate).toDouble()).div(100)).toString().toDouble())
                holder.binding.tvRoi.text = "$roi %"

          //  }
        }

        holder.binding.parentEmiViewLl.setOnClickListener {
            index = position
            notifyDataSetChanged()
        }

        //region==========================Checked Particular Row of RecyclerView Logic:-
        if (index == position) {
            holder.binding.cardView.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#13E113")))
            holder.binding.schemeCheckIv.visibility = View.VISIBLE
            schemeSelectCB(position)
        } else {
            holder.binding.cardView.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#FFFFFF")))
            holder.binding.schemeCheckIv.visibility = View.GONE
        }
        //endregion
    }


    inner class EMISchemeOfferHolder(val binding: ItemEmiSchemeOfferBinding) :
        RecyclerView.ViewHolder(binding.root)
}



/*
class EMISchemeAndOfferAdapter(val onItemClickListener: (BankEMITenureDataModal) -> Unit) :
    ListAdapter<BankEMITenureDataModal,  EMISchemeAndOfferAdapter.EMISchemeOfferHolder>(
        DiffUtilImpl()
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EMISchemeAndOfferAdapter.EMISchemeOfferHolder {
        val inflater: ItemEmiSchemeOfferBinding =
            ItemEmiSchemeOfferBinding.inflate(LayoutInflater.from(p0.context), p0, false)
        return EMISchemeAndOfferAdapter.EMISchemeOfferHolder(inflater)

    }

    override fun onBindViewHolder(holder: BrandEmiProductAdapter.BrandEmiProductViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)

    }

    inner class EMISchemeOfferHolder(private val binding: ItemEmiSchemeOfferBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(productData: BankEMITenureDataModal) {
            binding.tvBrandMasterName.text = productData.productName
            binding.brandEmiMasterParent.setOnClickListener {

                onItemClickListener(productData)
            }
        }
    }

    class DiffUtilImpl :
        androidx.recyclerview.widget.DiffUtil.ItemCallback<BankEMITenureDataModal>() {
        override fun areItemsTheSame(
            oldItem: BankEMITenureDataModal,
            newItem: BankEMITenureDataModal
        ): Boolean {
            return oldItem.productID == newItem.productID

        }

        override fun areContentsTheSame(
            oldItem: BankEMITenureDataModal,
            newItem: BankEMITenureDataModal
        ): Boolean {
            return oldItem == newItem
        }

    }

}*/
