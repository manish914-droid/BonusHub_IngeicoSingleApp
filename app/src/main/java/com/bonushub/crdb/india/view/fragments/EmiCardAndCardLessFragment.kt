package com.bonushub.crdb.india.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentEmiCardAndCardlessBinding
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.viewmodel.CardlessModel
import com.bonushub.crdb.india.view.adapter.EmiCardCardLessAdapter

class EmiCardAndCardLessFragment : Fragment() {

    lateinit var binding:FragmentEmiCardAndCardlessBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_pay_later, container, false)
        binding=FragmentEmiCardAndCardlessBinding.inflate(layoutInflater, container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setCardRecycler()
        (activity as NavigationActivity).manageTopToolBar(false)
        binding.subHeaderView.backImageButton.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }

        binding.cardlessManageBtn.setOnClickListener {
            /*(activity as MainActivity).transactFragment(PayLaterFragment().apply {
                arguments = Bundle().apply {
                    *//* putInt(MainActivity.CROSS_SELL_OPTIONS, type)
                     putInt(MainActivity.CROSS_SELL_REQUEST_TYPE, requestType)
                     putString(MainActivity.CROSS_SELL_PROCESS_TYPE_HEADING, heading)*//*
                }
            }, isBackStackAdded = true)*/

            (activity as NavigationActivity).transactFragment(PayLaterFragment().apply {

            }, isBackStackAdded = true)


        }
    }

    private fun setCardRecycler() {


        val listitem = ArrayList<CardlessModel>()



//
//         "hdfc bank cc" -> R.drawable.hdfc_issuer_icon
//                "hdfc bank dc" -> R.drawable.hdfc_dc_issuer_icon
//                "sbi card" -> R.drawable.sbi_issuer_icon
//                "citi" -> R.drawable.citi_issuer_icon
//                "icici" -> R.drawable.icici_issuer_icon
//                "yes" -> R.drawable.yes_issuer_icon
//                "kotak" -> R.drawable.kotak_issuer_icon
//                "rbl" -> R.drawable.rbl_issuer_icon
//                "scb" -> R.drawable.scb_issuer_icon
//                "axis" -> R.drawable.axis_issuer_icon
//                "indusind" -> R.drawable.indusind_issuer_icon
//                "amex" ->  R.drawable.amex_issuer_icon // to change when backend issue fixed
//
//
//
//        listitem.add(CardlessModel(R.mipmap.img_yes_bank))*/

        listitem.add(CardlessModel(R.drawable.hdfc_dc_issuer_icon))
        listitem.add(CardlessModel(R.drawable.sbi_issuer_icon))
        listitem.add(CardlessModel(R.drawable.citi_issuer_icon))
        listitem.add(CardlessModel(R.drawable.icici_issuer_icon))
        listitem.add(CardlessModel(R.drawable.yes_issuer_icon))
        listitem.add(CardlessModel(R.drawable.kotak_issuer_icon))
        listitem.add(CardlessModel(R.drawable.scb_issuer_icon))
        listitem.add(CardlessModel(R.drawable.axis_issuer_icon))
        listitem.add(CardlessModel(R.drawable.rbl_issuer_icon))
        listitem.add(CardlessModel(R.drawable.indusind_issuer_icon))
        listitem.add(CardlessModel(R.drawable.amex_issuer_icon))

        val adapter = EmiCardCardLessAdapter(listitem)

        binding.recclerviewCards.adapter = adapter
        binding.recclerviewCards.layoutManager = GridLayoutManager(activity, 4)

        val listitem2 = ArrayList<CardlessModel>()

        /*listitem2.add(CardlessModel(R.mipmap.img_hdfc_debit))*/
        listitem2.add(CardlessModel(R.drawable.hdfc_dc_issuer_icon))

        val adapter2 = EmiCardCardLessAdapter(listitem2)
        binding.recylerviewCardles.adapter = adapter2
        binding.recylerviewCardles.layoutManager = GridLayoutManager(activity, 4)




    }

}