package com.bonushub.crdb.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.databinding.ItemBrandEmiMasterBinding
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.google.gson.Gson

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