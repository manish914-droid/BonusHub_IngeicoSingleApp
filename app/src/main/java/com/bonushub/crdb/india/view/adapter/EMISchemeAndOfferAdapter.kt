package com.bonushub.crdb.india.view.adapter

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.ItemEmiSchemeOfferBinding
import com.bonushub.crdb.india.model.remote.BankEMITenureDataModal
import com.bonushub.crdb.india.utils.divideAmountBy100
import com.bonushub.crdb.india.utils.makeTextViewResizable
import com.bonushub.crdb.india.utils.BhTransactionType


internal class EMISchemeAndOfferAdapter(private val transactionType: Int,private val emiSchemeDataList: MutableList<BankEMITenureDataModal>?, private var schemeSelectCB: (Int) -> Unit) : RecyclerView.Adapter<EMISchemeAndOfferAdapter.EMISchemeOfferHolder>()
{

    private var index = -1

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): EMISchemeOfferHolder {
        val inflater: ItemEmiSchemeOfferBinding =
            ItemEmiSchemeOfferBinding.inflate(LayoutInflater.from(p0.context), p0, false)
        return EMISchemeOfferHolder(inflater)
    }

    override fun getItemCount(): Int {
        return emiSchemeDataList?.size?.plus(1) ?: 0
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: EMISchemeOfferHolder, position: Int) {

        if (position == emiSchemeDataList?.size) {
            holder.binding.cardView.visibility = View.INVISIBLE
        } else {
            holder.binding.cardView.visibility = View.VISIBLE
        val modelData = emiSchemeDataList?.get(position)

        //region==========================Checked Particular Row of RecyclerView Logic:-
        if (modelData?.isSelected == true) {
            // parent_emi_view_ll
            holder.binding.parentEmiViewLl.setBackgroundResource(R.drawable.card_edge_blue)
            holder.binding.schemeCheckIv.visibility = View.VISIBLE
        } else {
            holder.binding.parentEmiViewLl.setBackgroundResource(R.drawable.card_edge_transparent_bg_sky_blu)
            holder.binding.schemeCheckIv.visibility = View.INVISIBLE
        }
        /* if (modelData?.isSelected == true) {
            //holder.binding.cardView.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#13E113"))) //
            //holder.binding.schemeCheckIv.visibility = View.VISIBLE//
            holder.binding.cardView.setBackgroundResource(R.drawable.edge_blue)
            holder.binding.tenureHeadingTv.setBackgroundResource(R.drawable.edge_white_bg_transparent)
           // holder.binding.tenureHeadingTv.setTextColor(ColorStateList.valueOf(Color.parseColor("#FFFFFF")))
            setViewColor(holder.binding.tenureHeadingTv,true)
            setViewColor(holder.binding.tvTransactionAmountHeader,true)
            setViewColor(holder.binding.tvTransactionAmount,true)
            setViewColor(holder.binding.tvTenureHeader,true)
            setViewColor(holder.binding.tvTenure,true)
            setViewColor(holder.binding.tvLoanAmountHeader,true)
            setViewColor(holder.binding.tvLoanAmount,true)

            setViewColor(holder.binding.tvEmiAmountHeader,true)
            setViewColor(holder.binding.tvEmiAmount,true)
            setViewColor(holder.binding.tvDiscountAmountHeader,true)
            setViewColor(holder.binding.tvDiscountAmount,true)
            setViewColor(holder.binding.tvCashbackAmountHeader,true)
            setViewColor(holder.binding.tvCashbackAmount,true)
            setViewColor(holder.binding.tvInterestRateHeader,true)
            setViewColor(holder.binding.tvInterestRate,true)
            setViewColor(holder.binding.tvTotalInterestPayHeader,true)
            setViewColor(holder.binding.tvTotalInterestPay,true)
            setViewColor(holder.binding.tvRoiHeader,true)
            setViewColor(holder.binding.tvRoi,true)
            setViewColor(holder.binding.tvTotalEmiPayHeader,true)
            setViewColor(holder.binding.tvTotalEmiPay,true)
            setViewColor(holder.binding.tvOfferHeader,true)
            setViewColor(holder.binding.tvOffer,true)
            // schemeSelectCB(position)
        } else {
            //holder.binding.cardView.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#FFFFFF")))
            //holder.binding.schemeCheckIv.visibility = View.GONE//
            holder.binding.cardView.setBackgroundResource(R.drawable.edge_blue_bg_blue_transparent)
            holder.binding.tenureHeadingTv.setBackgroundResource(R.drawable.edge_blue_bg_transparent)
           // holder.binding.tenureHeadingTv.setTextColor(ColorStateList.valueOf(Color.parseColor("#013480")))
            setViewColor(holder.binding.tenureHeadingTv,false)
            setViewColor(holder.binding.tenureHeadingTv,false)
            setViewColor(holder.binding.tvTransactionAmountHeader,false)
            setViewColor(holder.binding.tvTransactionAmount,false)
            setViewColor(holder.binding.tvTenureHeader,false)
            setViewColor(holder.binding.tvTenure,false)
            setViewColor(holder.binding.tvLoanAmountHeader,false)
            setViewColor(holder.binding.tvLoanAmount,false)

            setViewColor(holder.binding.tvEmiAmountHeader,false)
            setViewColor(holder.binding.tvEmiAmount,false)
            setViewColor(holder.binding.tvDiscountAmountHeader,false)
            setViewColor(holder.binding.tvDiscountAmount,false)
            setViewColor(holder.binding.tvCashbackAmountHeader,false)
            setViewColor(holder.binding.tvCashbackAmount,false)
            setViewColor(holder.binding.tvInterestRateHeader,false)
            setViewColor(holder.binding.tvInterestRate,false)
            setViewColor(holder.binding.tvTotalInterestPayHeader,false)
            setViewColor(holder.binding.tvTotalInterestPay,false)
            setViewColor(holder.binding.tvRoiHeader,false)
            setViewColor(holder.binding.tvRoi,false)
            setViewColor(holder.binding.tvTotalEmiPayHeader,false)
            setViewColor(holder.binding.tvTotalEmiPay,false)
            setViewColor(holder.binding.tvOfferHeader,false)
            setViewColor(holder.binding.tvOffer,false)
        }*/
        //endregion

        if (modelData != null) {
            if (modelData.tenure == "1") {
                holder.binding.tenureHeadingTv.text = modelData.tenureLabel
                "%.2f".format(
                    (((modelData.transactionAmount).toDouble()).div(100)).toString().toDouble()
                )
                holder.binding.tvTransactionAmount.text = "\u20B9 " + "%.2f".format(
                    (((modelData.transactionAmount).toDouble()).div(100)).toString().toDouble()
                )
                holder.binding.offerLL.visibility = View.VISIBLE
                holder.binding.tenureLl.visibility = View.GONE
                holder.binding.loanAmtLl.visibility = View.GONE
                holder.binding.emiAmtLl.visibility = View.GONE
                holder.binding.discountLL.visibility = View.GONE
                holder.binding.totalIntPayLl.visibility = View.GONE
                holder.binding.rateofInterestLL.visibility = View.GONE
                holder.binding.toatalemipayLL.visibility = View.GONE

                val tvOffer = holder.binding.tvOffer

                //tvOffer.context.getString(R.string.cashBackOffer)
                tvOffer.text = modelData.tenureTAndC
                makeTextViewResizable(tvOffer, 8, "See More", true)
            } else {
                // (((modelData.transactionAmount).toDouble()).div(100)).toString()
                holder.binding.offerLL.visibility = View.GONE
                "%.2f".format(
                    (((modelData.transactionAmount).toDouble()).div(100)).toString().toDouble()
                )
                holder.binding.tvTransactionAmount.text = "\u20B9 " + "%.2f".format(
                    (((modelData.transactionAmount).toDouble()).div(100)).toString().toDouble()
                )
                holder.binding.tvLoanAmount.text = "\u20B9 " + "%.2f".format(
                    (((modelData.loanAmount).toDouble()).div(100)).toString().toDouble()
                )
                holder.binding.tvEmiAmount.text = "\u20B9 " + "%.2f".format(
                    (((modelData.emiAmount).toDouble()).div(100)).toString().toDouble()
                )
                //    val tenureDuration = "${modelData.tenure} Months"
                val tenureDuration = modelData.tenureLabel
                //   val tenureHeadingDuration = "${modelData.tenure} Months Scheme"
                val tenureHeadingDuration = modelData.tenureLabel
                holder.binding.tvTenure.text = tenureDuration
                holder.binding.tenureHeadingTv.text = tenureHeadingDuration
                //If Discount Amount Available show this else if CashBack Amount show that:-
                if (!modelData.discountAmount.isEmpty() && modelData.discountAmount.toInt() != 0) {
                    holder.binding.tvDiscountAmount.text = "\u20B9 " + "%.2f".format(
                        (((modelData.discountAmount).toDouble()).div(100)).toString().toDouble()
                    )
                    holder.binding.discountLL.visibility = View.VISIBLE
                    holder.binding.cashBackLL.visibility = View.GONE
                }
                if (!modelData.cashBackAmount.isEmpty() && modelData.cashBackAmount.toInt() != 0) {
                    holder.binding.tvCashbackAmount.text =
                        "\u20B9 " + divideAmountBy100(modelData.cashBackAmount.toInt()).toString()
                    holder.binding.cashBackLL.visibility = View.VISIBLE
                    holder.binding.discountLL.visibility = View.GONE
                }
                if (transactionType == BhTransactionType.TEST_EMI.type) {
                    holder.binding.tvTenure.text = modelData.tenure + " Months"
                    holder.binding.tenureHeadingTv.text = modelData.tenure + " Months"
                    holder.binding.toatalemipayLL.visibility = View.GONE
                    holder.binding.tvInterestRate.text =
                        "" + divideAmountBy100(modelData.tenureInterestRate.toInt()).toString() + " %"
                    holder.binding.tvTotalInterestPay.text = "\u20B9 " + "%.2f".format(
                        (((modelData.totalInterestPay).toDouble()).div(100)).toString().toDouble()
                    )

                    val roi = "%.2f".format(
                        (((modelData.tenureInterestRate).toDouble()).div(100)).toString().toDouble()
                    )
                    holder.binding.tvRoi.text = "$roi %"

                } else {
                    holder.binding.toatalemipayLL.visibility = View.VISIBLE//tenureInterestRate
                    holder.binding.tvTotalInterestPay.text = "\u20B9 " + "%.2f".format(
                        (((modelData.totalInterestPay).toDouble()).div(100)).toString().toDouble()
                    )
                    val roi = "%.2f".format(
                        (((modelData.tenureInterestRate).toDouble()).div(100)).toString().toDouble()
                    )
                    //totalinterestpay
                    holder.binding.tvRoi.text = "$roi %"
                    holder.binding.tvTotalEmiPay.text = "\u20B9 " + "%.2f".format(
                        (((modelData.netPay).toDouble()).div(100)).toString().toDouble()
                    )
                }

            }
        }

    }
        holder.binding.parentEmiViewLl.setOnClickListener {
            if (position == emiSchemeDataList?.size) {
                // do nothing
            }else{
                //index = position
                schemeSelectCB(position)
                for(i in emiSchemeDataList!!.indices){
                    emiSchemeDataList[i].isSelected = i == position
                }
                notifyDataSetChanged()
            }

        }


    }


    inner class EMISchemeOfferHolder(val binding: ItemEmiSchemeOfferBinding) :
        RecyclerView.ViewHolder(binding.root)

    private fun setViewColor(txtView:TextView, isWhite:Boolean){
        if(isWhite)
        {
            txtView.setTextColor(ColorStateList.valueOf(Color.parseColor("#FFFFFF")))
        }else{
            txtView.setTextColor(ColorStateList.valueOf(Color.parseColor("#013480")))
        }
    }
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
