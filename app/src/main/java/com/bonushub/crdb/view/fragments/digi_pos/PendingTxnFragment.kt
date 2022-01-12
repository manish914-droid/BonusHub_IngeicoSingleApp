package com.bonushub.crdb.view.fragments.digi_pos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentPendingTxnBinding
import com.bonushub.crdb.databinding.FragmentUpiSmsDynamicPayQrInputDetailBinding
import com.bonushub.crdb.databinding.ItemDigiPosBinding
import com.bonushub.crdb.databinding.ItemPendingTxnBinding
import com.bonushub.crdb.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.utils.logger
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.pax.utils.DigiPosItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class PendingTxnFragment : Fragment(), IPendingListItemClick {

    lateinit var digiPosItemType:DigiPosItem

    lateinit var iPendingListItemClick:IPendingListItemClick
    var binding:FragmentPendingTxnBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPendingTxnBinding.inflate(inflater,container, false)
        return binding?.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        iPendingListItemClick = this
        digiPosItemType = arguments?.getSerializable("type") as DigiPosItem

        binding?.linLayPendingTnx?.visibility = View.VISIBLE
        binding?.linLaySearch?.visibility = View.GONE

        binding?.subHeaderView?.subHeaderText?.text = digiPosItemType.title
        binding?.subHeaderView?.headerImage?.setImageResource(digiPosItemType.res)

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            try {
                parentFragmentManager.popBackStackImmediate()
            }catch (ex:Exception)
            {
                ex.printStackTrace()
            }
        }

        binding?.txtViewSearch?.setOnClickListener {
            (activity as NavigationActivity).transactFragment(SearchTxnFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("type", DigiPosItem.PENDING_TXN)
                }
            })

        }


        binding?.txtViewSearch?.setOnClickListener {

        }

        setupRecyclerview()

    }

    private fun setupRecyclerview(){
        lifecycleScope.launch(Dispatchers.Main) {
            binding?.let {
                it.recyclerView.layoutManager = GridLayoutManager(activity, 1)
                it.recyclerView.adapter = PendingTxnListAdapter(iPendingListItemClick)
            }

        }
    }

    override fun iPendingListItemClick() {
        logger("pending list","click")

        DialogUtilsNew1.showPendingTxnDetailsDialog(requireContext(),"300.00","SMS Pay","000156","15462","******7415","Pending") {

            (activity as NavigationActivity).transactFragment(PendingDetailsFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("type", DigiPosItem.PENDING_TXN)
                }
            })

        }
    }
}

interface IPendingListItemClick{

    fun iPendingListItemClick()
}


class PendingTxnListAdapter(private var iPendingListItemClick: IPendingListItemClick?) : RecyclerView.Adapter<PendingTxnListAdapter.PendingTxnListViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingTxnListViewHolder {

        val itemBinding = ItemPendingTxnBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)
        return PendingTxnListViewHolder(itemBinding)
    }

    //override fun getItemCount(): Int = digiPosItem.size
    override fun getItemCount(): Int = 10

    override fun onBindViewHolder(holder: PendingTxnListViewHolder, position: Int) {

        //val model = digiPosItem[position]
        holder.viewBinding.txtViewTxnType.text = "SMS Pay"
        holder.viewBinding.txtViewDateTime.text = "25 April, 05:39 PM"
        holder.viewBinding.txtViewAmount.text = "300.00"
        holder.viewBinding.txtViewPhoneNumber.text = "******3211"

        holder.viewBinding.imgViewTxnStatus.setImageResource(R.drawable.ic_init_payment_success)

        if(position == 2)
        {
            holder.viewBinding.imgViewTxnStatus.setImageResource(R.drawable.ic_init_payment_null)
        }

        holder.viewBinding.btnGetStatus.setOnClickListener {

            iPendingListItemClick?.iPendingListItemClick()

        }

    }

    inner class PendingTxnListViewHolder(val viewBinding: ItemPendingTxnBinding) : RecyclerView.ViewHolder(viewBinding.root)
}