package com.bonushub.crdb.view.fragments.pre_auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentPreAuthPendingBinding
import com.bonushub.crdb.databinding.ItemPreAuthPendingBinding
import com.bonushub.crdb.view.activity.NavigationActivity


class PreAuthPendingFragment : Fragment() {

    var binding:FragmentPreAuthPendingBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPreAuthPendingBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.subHeaderView?.subHeaderText?.text = "PRE-AUTH"
        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_preauth)
        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }

        binding?.rvPerAuthPending?.apply{
            layoutManager = GridLayoutManager(activity, 1)
            adapter = PreAuthPendingAdapter()
        }

        binding?.btnPrint?.setOnClickListener {

            (activity as NavigationActivity).transactFragment(PreAuthPendingDetailsFragment(),true)
        }

    }
}

class PreAuthPendingAdapter() : RecyclerView.Adapter<PreAuthPendingAdapter.PreAuthPendingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreAuthPendingViewHolder {

        val itemBinding = ItemPreAuthPendingBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)
        return PreAuthPendingViewHolder(
            itemBinding
        )
    }

   // override fun getItemCount(): Int = listItem.size
    override fun getItemCount(): Int = 4


    override fun onBindViewHolder(holder: PreAuthPendingViewHolder, position: Int) {

       // val model = listItem[position]

        // temp
        holder.viewBinding.txtViewBatch.text = "000075"
        holder.viewBinding.txtViewRoc.text = "000154"
        holder.viewBinding.txtViewPan.text = "5413********0078"
        holder.viewBinding.txtViewAmt.text = "10.00"
        holder.viewBinding.txtViewDate.text = "22/01/2021"
        holder.viewBinding.txtViewTime.text = "16:23:33"

    }



    inner class PreAuthPendingViewHolder(val viewBinding: ItemPreAuthPendingBinding) : RecyclerView.ViewHolder(viewBinding.root) {

    }
}