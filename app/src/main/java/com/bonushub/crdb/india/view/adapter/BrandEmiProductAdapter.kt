package com.bonushub.crdb.india.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.ItemBrandEmiMasterBinding
import com.bonushub.crdb.india.model.remote.BrandEMIProductDataModal


class BrandEmiProductAdapter(val onItemClickListener: (BrandEMIProductDataModal) -> Unit) :
    ListAdapter<BrandEMIProductDataModal, BrandEmiProductAdapter.BrandEmiProductViewHolder>(
        DiffUtilImpl()
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrandEmiProductAdapter.BrandEmiProductViewHolder {
        val binding: ItemBrandEmiMasterBinding =
            ItemBrandEmiMasterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BrandEmiProductViewHolder(binding)

    }

    override fun onBindViewHolder(holder: BrandEmiProductAdapter.BrandEmiProductViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)

    }

    inner class BrandEmiProductViewHolder(private val binding: ItemBrandEmiMasterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(productData: BrandEMIProductDataModal) {
            binding.tvBrandMasterName.text = productData.productName
            binding.brandEmiMasterParent.setOnClickListener {

                binding.bankEmiCv.setBackgroundResource(R.drawable.edge_brand_selected)
                onItemClickListener(productData)
            }
        }
    }

    class DiffUtilImpl :
        androidx.recyclerview.widget.DiffUtil.ItemCallback<BrandEMIProductDataModal>() {
        override fun areItemsTheSame(
            oldItem: BrandEMIProductDataModal,
            newItem: BrandEMIProductDataModal
        ): Boolean {
            return oldItem.productID == newItem.productID

        }

        override fun areContentsTheSame(
            oldItem: BrandEMIProductDataModal,
            newItem: BrandEMIProductDataModal
        ): Boolean {
            return oldItem == newItem
        }

    }

}