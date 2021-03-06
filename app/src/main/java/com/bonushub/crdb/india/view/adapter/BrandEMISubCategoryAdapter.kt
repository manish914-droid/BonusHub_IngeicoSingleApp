package com.bonushub.crdb.india.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.ItemBrandEmiMasterBinding
import com.bonushub.crdb.india.model.local.BrandEMISubCategoryTable

class BrandEMISubCategoryAdapter (val onItemClickListener: (BrandEMISubCategoryTable) -> Unit) :
    ListAdapter<BrandEMISubCategoryTable, BrandEMISubCategoryAdapter.BrandEMISubCatViewHolder>(
        DiffUtilImpl()
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrandEMISubCategoryAdapter.BrandEMISubCatViewHolder {
        val binding: ItemBrandEmiMasterBinding =
            ItemBrandEmiMasterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BrandEMISubCatViewHolder(binding)

    }

    override fun onBindViewHolder(holder: BrandEMISubCategoryAdapter.BrandEMISubCatViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)

    }

    inner class BrandEMISubCatViewHolder(private val binding: ItemBrandEmiMasterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(brandData: BrandEMISubCategoryTable) {
            binding.tvBrandMasterName.text = brandData.categoryName
            binding.brandEmiMasterParent.setOnClickListener {

                binding.bankEmiCv.setBackgroundResource(R.drawable.edge_brand_selected)
                onItemClickListener(brandData)
            }
        }
    }

    class DiffUtilImpl :
        androidx.recyclerview.widget.DiffUtil.ItemCallback<BrandEMISubCategoryTable>() {
        override fun areItemsTheSame(
            oldItem: BrandEMISubCategoryTable,
            newItem: BrandEMISubCategoryTable
        ): Boolean {
            return oldItem.categoryID == newItem.categoryID

        }

        override fun areContentsTheSame(
            oldItem: BrandEMISubCategoryTable,
            newItem: BrandEMISubCategoryTable
        ): Boolean {
            return oldItem == newItem
        }

    }

}