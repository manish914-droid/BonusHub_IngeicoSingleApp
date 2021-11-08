package com.bonushub.crdb.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.databinding.ItemBrandEmiMasterBinding
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.model.remote.BrandEMIProductDataModal


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