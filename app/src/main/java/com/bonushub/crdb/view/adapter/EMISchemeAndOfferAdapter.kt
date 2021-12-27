package com.bonushub.crdb.view.adapter

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.databinding.ItemEmiSchemeOfferBinding
import com.bonushub.crdb.model.remote.BankEMITenureDataModal
import com.bonushub.crdb.utils.divideAmountBy100
import com.bonushub.crdb.utils.makeTextViewResizable
import com.bonushub.pax.utils.BhTransactionType
import com.ingenico.hdfcpayment.type.TransactionType


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
        val modelData = emiSchemeDataList?.get(holder.adapterPosition)
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

                // tvOffer.context.getString(R.string.cashBackOffer)
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
