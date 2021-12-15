package com.bonushub.crdb.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentPreAuthBinding
import com.bonushub.crdb.databinding.ItemDashboartBinding
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.adapter.BankFunctionsAdapter
import com.bonushub.pax.utils.EDashboardItem


class PreAuthFragment : Fragment() {

    val preAuthOptionList by lazy {
        arguments?.getSerializable("preAuthOptionList") as ArrayList<EDashboardItem>
    }
    private val action by lazy { arguments?.getSerializable("type") ?: "" }

    val mAdapter by lazy {
        PreAuthOptionAdapter(preAuthOptionList){
            onOptionClickListner(it)
        }
    }

    var binding:FragmentPreAuthBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPreAuthBinding.inflate(inflater, container, false)
        return binding?.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.subHeaderView?.subHeaderText?.text = "PRE-AUTH"
        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_preauth)

        binding?.rvPerAuthCategory?.apply{
            layoutManager = GridLayoutManager(activity, 3)
            itemAnimator = DefaultItemAnimator()
            adapter = mAdapter
            scheduleLayoutAnimation()
        }
        /*if (isExpanded) dashBoardAdapter.onUpdatedItem(list) else dashBoardAdapter.onUpdatedItem(
                list
            )*/



        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }
    }

    private fun onOptionClickListner(option: EDashboardItem) {
        (activity as NavigationActivity).onDashBoardItemClick(option)
    }
}

class PreAuthOptionAdapter(private val listItem: MutableList<EDashboardItem>,var cb: (EDashboardItem) -> Unit) : RecyclerView.Adapter<PreAuthOptionAdapter.PreAuthCategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreAuthCategoryViewHolder {

        val itemBinding = ItemDashboartBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)
        return PreAuthCategoryViewHolder(
            itemBinding
        )
    }

    override fun getItemCount(): Int = listItem.size


    override fun onBindViewHolder(holder: PreAuthCategoryViewHolder, position: Int) {

        val model = listItem[position]

        holder.viewBinding.itemLogoIv.setImageResource(model.res)
        holder.viewBinding.itemTitleTv.text = model.title

        holder.viewBinding.itemParentRv.setOnClickListener {
            cb(model)
        }

    }



    inner class PreAuthCategoryViewHolder(val viewBinding: ItemDashboartBinding) : RecyclerView.ViewHolder(viewBinding.root) {

    }
}