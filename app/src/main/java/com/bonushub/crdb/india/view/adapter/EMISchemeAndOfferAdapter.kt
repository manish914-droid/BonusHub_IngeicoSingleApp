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

import com.bonushub.crdb.india.vxutils.divideAmountBy100
import com.bonushub.crdb.india.utils.makeTextViewResizable
import com.bonushub.crdb.india.vxutils.BhTransactionType



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
