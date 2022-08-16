package com.bonushub.crdb.india.view.fragments.pre_auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentPreAuthBinding
import com.bonushub.crdb.india.databinding.ItemReportsBinding
import com.bonushub.crdb.india.utils.EDashboardItem
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp
import com.bonushub.crdb.india.utils.ToastUtils
import com.bonushub.crdb.india.utils.refreshSubToolbarLogos
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.fragments.EmiCardAndCardLessFragment
import com.bonushub.crdb.india.view.fragments.PayLaterFragment


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

        (activity as NavigationActivity).manageTopToolBar(false)
        /*binding?.subHeaderView?.subHeaderText?.text = "PRE-AUTH"
        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_preauth_new)*/

        binding?.subHeaderView?.subHeaderText?.setOnClickListener {
            (activity as NavigationActivity).transactFragment(EmiCardAndCardLessFragment().apply {

            }, isBackStackAdded = true)
        }

        refreshSubToolbarLogos(this,null,R.drawable.ic_preauth_new, "PRE-AUTH")

        binding?.rvPerAuthCategory?.apply{
            layoutManager = GridLayoutManager(activity, 1)
            adapter = mAdapter
        }
        /*if (isExpanded) dashBoardAdapter.onUpdatedItem(list) else dashBoardAdapter.onUpdatedItem(
                list
            )*/



        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }
    }

    private fun onOptionClickListner(option: EDashboardItem) {
        if(option == EDashboardItem.PENDING_PREAUTH){
            if (Field48ResponseTimestamp.checkInternetConnection()) {

                (activity as NavigationActivity).alertBoxWithActionNew(getString(R.string.confirmation),
                    getString(R.string.pending_preauth_alert_msg),
                    R.drawable.ic_info_orange,
                    "Yes","No",true,false,{
                        (activity as NavigationActivity).onDashBoardItemClick(option)
                    },{
                        mAdapter.notifyDataSetChanged()
                    })

            } else {
                ToastUtils.showToast(activity,getString(R.string.no_internet_available_please_check_your_internet))
            }
        }else {
            (activity as NavigationActivity).onDashBoardItemClick(option)
        }
    }

}

class PreAuthOptionAdapter(private val listItem: MutableList<EDashboardItem>, var cb: (EDashboardItem) -> Unit) : RecyclerView.Adapter<PreAuthOptionAdapter.PreAuthCategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreAuthCategoryViewHolder {

        val itemBinding = ItemReportsBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)
        return PreAuthCategoryViewHolder(
            itemBinding
        )
    }

    override fun getItemCount(): Int = listItem.size


    override fun onBindViewHolder(holder: PreAuthCategoryViewHolder, position: Int) {

        val model = listItem[position]

        holder.viewBinding.textView.text = model.title
        holder.viewBinding.imgViewIcon.setImageResource(R.drawable.ic_preauth_submenu)
        holder.viewBinding.relLayParent.setBackgroundResource(R.drawable.edge_gray)

        holder.viewBinding.relLayParent.setOnClickListener {
            holder.viewBinding.relLayParent.setBackgroundResource(R.drawable.edge_brand_selected)
            cb(model)
        }

    }



    inner class PreAuthCategoryViewHolder(val viewBinding: ItemReportsBinding) : RecyclerView.ViewHolder(viewBinding.root) {

    }
}