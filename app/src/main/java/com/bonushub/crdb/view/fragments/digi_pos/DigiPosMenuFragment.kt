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
import com.bonushub.crdb.databinding.FragmentDigiPosMenuBinding
import com.bonushub.crdb.databinding.FragmentTestEmiBinding
import com.bonushub.crdb.databinding.ItemBankFunctionsBinding
import com.bonushub.crdb.databinding.ItemDigiPosBinding
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.fragments.tets_emi.ITestEmiItemClick
import com.bonushub.crdb.view.fragments.tets_emi.TestEmiAdapter
import com.bonushub.pax.utils.DigiPosItem
import com.bonushub.pax.utils.EDashboardItem
import com.bonushub.pax.utils.TestEmiItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class DigiPosMenuFragment : Fragment(), IDigiPosMenuItemClick {

    var binding:FragmentDigiPosMenuBinding? = null
    private lateinit var transactionType: EDashboardItem

    private val digiPosItem: MutableList<DigiPosItem> by lazy { mutableListOf<DigiPosItem>() }
    private var iDigiPosMenuItemClick: IDigiPosMenuItemClick? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentDigiPosMenuBinding.inflate(inflater,container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionType = arguments?.getSerializable("type") as EDashboardItem

        binding?.subHeaderView?.subHeaderText?.text = transactionType.title
        binding?.subHeaderView?.headerImage?.setImageResource(transactionType.res)

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            try {
                parentFragmentManager.popBackStackImmediate()
            }catch (ex:Exception)
            {
                ex.printStackTrace()
            }
        }

        iDigiPosMenuItemClick = this
        digiPosItem.clear()
        digiPosItem.addAll(DigiPosItem.values())

        setupRecyclerview()
    }

    private fun setupRecyclerview(){
        lifecycleScope.launch(Dispatchers.Main) {
            binding?.let {
                it.recyclerView.layoutManager = GridLayoutManager(activity, 1)
                it.recyclerView.adapter = DigiPosMenuAdapter(iDigiPosMenuItemClick, digiPosItem)
            }

        }
    }

    override fun digiPosMenuItemClick(digiPosItem: DigiPosItem) {

        when(digiPosItem)
        {
            DigiPosItem.UPI ->{
                (activity as NavigationActivity).transactFragment(UpiSmsDynamicPayQrInputDetailFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("type", DigiPosItem.UPI)
                    }
                })
            }

            DigiPosItem.DYNAMIC_QR ->{
                (activity as NavigationActivity).transactFragment(UpiSmsDynamicPayQrInputDetailFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("type", DigiPosItem.DYNAMIC_QR)
                    }
                })
            }

            DigiPosItem.STATIC_QR ->{

                (activity as NavigationActivity).transactFragment(QrFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("type", DigiPosItem.STATIC_QR)
                    }
                })

            }

            DigiPosItem.SMS_PAY ->{
                (activity as NavigationActivity).transactFragment(UpiSmsDynamicPayQrInputDetailFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("type", DigiPosItem.SMS_PAY)
                    }
                })
            }

            DigiPosItem.PENDING_TXN ->{
                (activity as NavigationActivity).transactFragment(PendingTxnFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("type", DigiPosItem.PENDING_TXN)
                    }
                })
            }

            DigiPosItem.TXN_LIST ->{
                (activity as NavigationActivity).transactFragment(TxnListFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("type", DigiPosItem.TXN_LIST)
                    }
                })
            }
        }

    }
}

interface IDigiPosMenuItemClick{

    fun digiPosMenuItemClick(digiPosItem: DigiPosItem)
}

// addapter
class DigiPosMenuAdapter(private var iDigiPosMenuItemClick: IDigiPosMenuItemClick?, private val digiPosItem: MutableList<DigiPosItem>) : RecyclerView.Adapter<DigiPosMenuAdapter.DigiPosMenuViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DigiPosMenuViewHolder {

        val itemBinding = ItemDigiPosBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)
        return DigiPosMenuViewHolder(itemBinding)
    }

    override fun getItemCount(): Int = digiPosItem.size

    override fun onBindViewHolder(holder: DigiPosMenuViewHolder, position: Int) {

        val model = digiPosItem[position]
        holder.viewBinding.textView.text = model.title

        holder.viewBinding.imgViewIcon.setImageResource(model.res)


        holder.viewBinding.relLayParent.setOnClickListener {

            iDigiPosMenuItemClick?.digiPosMenuItemClick(model)

        }

    }

    inner class DigiPosMenuViewHolder(val viewBinding: ItemDigiPosBinding) : RecyclerView.ViewHolder(viewBinding.root)
}