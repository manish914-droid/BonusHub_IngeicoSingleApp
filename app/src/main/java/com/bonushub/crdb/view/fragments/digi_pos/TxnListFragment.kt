package com.bonushub.crdb.view.fragments.digi_pos

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentPendingTxnBinding
import com.bonushub.crdb.databinding.FragmentTxnListBinding
import com.bonushub.crdb.databinding.ItemPendingTxnBinding
import com.bonushub.crdb.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.utils.logger
import com.bonushub.pax.utils.DigiPosItem
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class TxnListFragment : Fragment(), ITxnListItemClick {

    private var sheetBehavior: BottomSheetBehavior<ConstraintLayout>? = null

    var binding:FragmentTxnListBinding? = null
    lateinit var digiPosItemType:DigiPosItem

    lateinit var iTxnListItemClick:ITxnListItemClick

    private var selectedFilterTransactionType: String = ""
    private var selectedFilterTxnID: String = ""

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

        sheetBehavior = binding?.bottomSheet?.let { BottomSheetBehavior.from(it.bottomLayout) }

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
            toggleBottomSheet()
        }

        // region bottom sheet
        binding?.bottomSheet?.closeIconBottom?.setOnClickListener {
            closeBottomSheet()
        }

        //region===================Filter Transaction Type's RadioButton OnClick events:-
        binding?.bottomSheet?.upiCollectBottomRB?.setOnClickListener {

            selectedFilterTransactionType =
                binding?.bottomSheet?.upiCollectBottomRB?.text?.toString() ?: ""
            //filterTransactionType = EnumDigiPosProcess.UPIDigiPOS.code
            binding?.bottomSheet?.upiCollectBottomRB?.setTextColor(Color.parseColor("#001F79"))
            binding?.bottomSheet?.upiCollectBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#001F79"))

            binding?.bottomSheet?.dynamicQRBottomRB?.isChecked = false
            binding?.bottomSheet?.dynamicQRBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.dynamicQRBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))

            binding?.bottomSheet?.smsPayBottomRB?.isChecked = false
            binding?.bottomSheet?.smsPayBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.smsPayBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))

            binding?.bottomSheet?.staticQRBottomRB?.isChecked = false
            binding?.bottomSheet?.staticQRBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.staticQRBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))

            Log.d("SelectedRB:- ", selectedFilterTransactionType)
        }

        binding?.bottomSheet?.dynamicQRBottomRB?.setOnClickListener {
            selectedFilterTransactionType =
                binding?.bottomSheet?.dynamicQRBottomRB?.text?.toString() ?: ""
            //filterTransactionType = EnumDigiPosProcess.DYNAMIC_QR.code
            binding?.bottomSheet?.dynamicQRBottomRB?.setTextColor(Color.parseColor("#001F79"))
            binding?.bottomSheet?.dynamicQRBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#001F79"))

            binding?.bottomSheet?.upiCollectBottomRB?.isChecked = false
            binding?.bottomSheet?.upiCollectBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.upiCollectBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))

            binding?.bottomSheet?.smsPayBottomRB?.isChecked = false
            binding?.bottomSheet?.smsPayBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.smsPayBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))

            binding?.bottomSheet?.staticQRBottomRB?.isChecked = false
            binding?.bottomSheet?.staticQRBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.staticQRBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))
            Log.d("SelectedRB:- ", selectedFilterTransactionType)
        }

        binding?.bottomSheet?.smsPayBottomRB?.setOnClickListener {
            selectedFilterTransactionType =
                binding?.bottomSheet?.smsPayBottomRB?.text?.toString() ?: ""
            //filterTransactionType = EnumDigiPosProcess.SMS_PAYDigiPOS.code
            binding?.bottomSheet?.smsPayBottomRB?.setTextColor(Color.parseColor("#001F79"))
            binding?.bottomSheet?.smsPayBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#001F79"))

            binding?.bottomSheet?.dynamicQRBottomRB?.isChecked = false
            binding?.bottomSheet?.dynamicQRBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.dynamicQRBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))

            binding?.bottomSheet?.upiCollectBottomRB?.isChecked = false
            binding?.bottomSheet?.upiCollectBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.upiCollectBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))

            binding?.bottomSheet?.staticQRBottomRB?.isChecked = false
            binding?.bottomSheet?.staticQRBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.staticQRBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))
            Log.d("SelectedRB:- ", selectedFilterTransactionType)
        }

        binding?.bottomSheet?.staticQRBottomRB?.setOnClickListener {
            selectedFilterTransactionType =
                binding?.bottomSheet?.staticQRBottomRB?.text?.toString() ?: ""
           // filterTransactionType = EnumDigiPosProcess.STATIC_QR.code
            binding?.bottomSheet?.staticQRBottomRB?.setTextColor(Color.parseColor("#001F79"))
            binding?.bottomSheet?.staticQRBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#001F79"))

            binding?.bottomSheet?.dynamicQRBottomRB?.isChecked = false
            binding?.bottomSheet?.dynamicQRBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.dynamicQRBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))

            binding?.bottomSheet?.smsPayBottomRB?.isChecked = false
            binding?.bottomSheet?.smsPayBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.smsPayBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))

            binding?.bottomSheet?.upiCollectBottomRB?.isChecked = false
            binding?.bottomSheet?.upiCollectBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.upiCollectBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))
            Log.d("SelectedRB:- ", selectedFilterTransactionType)
        }
        //endregion

        //region===================PTXN ID and MTXN ID RadioButtons OnClick Listener event:-
        binding?.bottomSheet?.ptxnIDBottomRB?.setOnClickListener {
            selectedFilterTxnID = binding?.bottomSheet?.ptxnIDBottomRB?.text?.toString() ?: ""
            binding?.bottomSheet?.ptxnIDBottomRB?.setTextColor(Color.parseColor("#001F79"))
            binding?.bottomSheet?.ptxnIDBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#001F79"))

            binding?.bottomSheet?.mtxnIDBottomRB?.isChecked = false
            binding?.bottomSheet?.mtxnIDBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.mtxnIDBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))
        }

        binding?.bottomSheet?.mtxnIDBottomRB?.setOnClickListener {
            selectedFilterTxnID = binding?.bottomSheet?.mtxnIDBottomRB?.text?.toString() ?: ""
            binding?.bottomSheet?.mtxnIDBottomRB?.setTextColor(Color.parseColor("#001F79"))
            binding?.bottomSheet?.mtxnIDBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#001F79"))

            binding?.bottomSheet?.ptxnIDBottomRB?.isChecked = false
            binding?.bottomSheet?.ptxnIDBottomRB?.setTextColor(Color.parseColor("#707070"))
            binding?.bottomSheet?.ptxnIDBottomRB?.buttonTintList =
                ColorStateList.valueOf(Color.parseColor("#707070"))
        }
        //endregion

        // end region
    }

    //Method to be called when Bottom Sheet Toggle:-
    private fun toggleBottomSheet() {
        if (sheetBehavior?.state != BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            DialogUtilsNew1.hideKeyboardIfOpen(requireActivity())
            sheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    //Method to be called on Bottom Sheet Close:-
    private fun closeBottomSheet() {
        DialogUtilsNew1.hideKeyboardIfOpen(requireActivity())
        if (sheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {

            sheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
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