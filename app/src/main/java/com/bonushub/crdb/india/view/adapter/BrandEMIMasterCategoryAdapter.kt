package com.bonushub.crdb.india.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.ItemBrandEmiMasterBinding
import com.bonushub.crdb.india.model.remote.BrandEMIMasterDataModal

class BrandEMIMasterCategoryAdapter(val onItemClickListener: (BrandEMIMasterDataModal) -> Unit) :
    ListAdapter<BrandEMIMasterDataModal, BrandEMIMasterCategoryAdapter.BrandEMIMasterViewHolder>(
        DiffUtilImpl()
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrandEMIMasterViewHolder {
        val binding: ItemBrandEmiMasterBinding =
            ItemBrandEmiMasterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BrandEMIMasterViewHolder(binding)

    }

    override fun onBindViewHolder(holder: BrandEMIMasterViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)

    }

    inner class BrandEMIMasterViewHolder(private val binding: ItemBrandEmiMasterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(brandData: BrandEMIMasterDataModal) {
            binding.tvBrandMasterName.text = brandData.brandName
            binding.brandEmiMasterParent.setOnClickListener {

                binding.bankEmiCv.setBackgroundResource(R.drawable.edge_brand_selected)

                onItemClickListener(brandData)
            }
        }
    }

    class DiffUtilImpl :
        androidx.recyclerview.widget.DiffUtil.ItemCallback<BrandEMIMasterDataModal>() {
        override fun areItemsTheSame(
            oldItem: BrandEMIMasterDataModal,
            newItem: BrandEMIMasterDataModal
        ): Boolean {
            return oldItem.brandID == newItem.brandID

        }

        override fun areContentsTheSame(
            oldItem: BrandEMIMasterDataModal,
            newItem: BrandEMIMasterDataModal
        ): Boolean {
            return oldItem == newItem
        }

    }

}