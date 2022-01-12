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
import com.bonushub.crdb.databinding.FragmentTxnListBinding
import com.bonushub.crdb.databinding.ItemPendingTxnBinding
import com.bonushub.crdb.utils.logger
import com.bonushub.pax.utils.DigiPosItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class TxnListFragment : Fragment(), ITxnListItemClick {

    var binding:FragmentTxnListBinding? = null
    lateinit var digiPosItemType:DigiPosItem

    lateinit var iTxnListItemClick:ITxnListItemClick

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentTxnListBinding.inflate(inflater,container, false)
        return binding?.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        iTxnListItemClick = this
        digiPosItemType = arguments?.getSerializable("type") as DigiPosItem

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

        setupRecyclerview()

        binding?.txtViewFilters?.setOnClickListener {
            logger("filter","openBottomSheet","e")
        }
    }

    private fun setupRecyclerview(){
        lifecycleScope.launch(Dispatchers.Main) {
            binding?.let {
                it.recyclerView.layoutManager = GridLayoutManager(activity, 1)
                it.recyclerView.adapter = TxnListAdapter(iTxnListItemClick)
            }

        }
    }

    override fun iTxnListItemClick() {
        logger("item","click","e")
    }

}

interface ITxnListItemClick{

    fun iTxnListItemClick()
}

class TxnListAdapter(private var iTxnListItemClick: ITxnListItemClick?) : RecyclerView.Adapter<TxnListAdapter.TxnListViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TxnListViewHolder {

        val itemBinding = ItemPendingTxnBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)
        return TxnListViewHolder(itemBinding)
    }

    // override fun getItemCount(): Int = digiPosItem.size
    override fun getItemCount(): Int = 10

    override fun onBindViewHolder(holder: TxnListViewHolder, position: Int) {

        //val model = digiPosItem[position]
        holder.viewBinding.txtViewTxnType.text = "SMS Pay"
        holder.viewBinding.txtViewDateTime.text = "25 April, 05:39 PM"
        holder.viewBinding.txtViewAmount.text = "300.00"
        holder.viewBinding.txtViewPhoneNumber.text = "******3211"

        holder.viewBinding.imgViewTxnStatus.setImageResource(R.drawable.ic_init_payment_success)

        if(position == 2)
        {
            holder.viewBinding.imgViewTxnStatus.setImageResource(R.drawable.ic_init_payment_null)
            //holder.viewBinding.txtViewTxnType.setD
        }

        holder.viewBinding.btnGetStatus.setOnClickListener {

            iTxnListItemClick?.iTxnListItemClick()

        }

    }

    inner class TxnListViewHolder(val viewBinding: ItemPendingTxnBinding) : RecyclerView.ViewHolder(viewBinding.root)
}