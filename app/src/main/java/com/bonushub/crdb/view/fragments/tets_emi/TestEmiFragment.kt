package com.bonushub.crdb.view.fragments.tets_emi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentTestEmiBinding
import com.bonushub.crdb.databinding.ItemBankFunctionsBinding
import com.bonushub.crdb.utils.Field48ResponseTimestamp
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.fragments.NewInputAmountFragment
import com.bonushub.crdb.utils.EDashboardItem
import com.bonushub.crdb.utils.TestEmiItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class TestEmiFragment : Fragment(), ITestEmiItemClick {

    var binding:FragmentTestEmiBinding? = null

    private var testEmiItem: MutableList<TestEmiItem> =mutableListOf()
    private var iTestEmiItemClick: ITestEmiItemClick? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentTestEmiBinding.inflate(inflater,container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.test_emi_header)
        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_bank_emi)

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            try {
                parentFragmentManager.popBackStackImmediate()
            }catch (ex:Exception)
            {
                ex.printStackTrace()
            }
        }


        iTestEmiItemClick = this
        testEmiItem.clear()
        val tpt=Field48ResponseTimestamp.getTptData()
        val linkedTid:ArrayList<String> =tpt?.LinkTidType as ArrayList<String>
        for (tid in linkedTid){
            when(tid){
             "0"->{
                 // for Amex

             }
                "1"->{
                    // DC type
                    testEmiItem.add(TestEmiItem.BASE_TID)
                }
                "2"->{
                    // off us Tid
                    testEmiItem.add(TestEmiItem.OFFUS_TID)
                }
                "3"->{
                    // 3 months onus
                    testEmiItem.add(TestEmiItem._3_M_TID)
                }
                "6"->{
                    // 6 months onus
                    testEmiItem.add(TestEmiItem._6_M_TID)
                }
                "9"->{
                    // 9 months onus
                    testEmiItem.add(TestEmiItem._9_M_TID)
                }
                "12"->{
                    // 12 months onus
                    testEmiItem.add(TestEmiItem._12_M_TID)
                }

            }
        }
        val sortedList = testEmiItem.sortedWith(compareBy { it.id })
        testEmiItem=sortedList as MutableList<TestEmiItem>

        setupRecyclerview()

    }



    private fun setupRecyclerview(){
        lifecycleScope.launch(Dispatchers.Main) {
            binding?.let {
                it.recyclerView.layoutManager = GridLayoutManager(activity, 1)
                it.recyclerView.adapter = TestEmiAdapter(iTestEmiItemClick, testEmiItem)
            }

        }
    }

    override fun testEmiItemClick(testEmiItem: TestEmiItem) {

/*
var testEmiOption="0"

        when(testEmiItem){
            TestEmiItem.BASE_TID ->{
                testEmiOption=TestEmiItem.BASE_TID.id
                logger("BASE_TID","click")
            }

            TestEmiItem.OFFUS_TID ->{
                testEmiOption=TestEmiItem.OFFUS_TID.id
                logger("OFFUS_TID","click")
            }

            TestEmiItem._3_M_TID ->{
                testEmiOption=TestEmiItem._3_M_TID.id
                logger("_3_M_TID","click")
            }

            TestEmiItem._6_M_TID ->{
                testEmiOption=TestEmiItem._6_M_TID.id
                logger("_6_M_TID","click")
            }

            TestEmiItem._9_M_TID ->{
                testEmiOption=TestEmiItem._9_M_TID.id
                logger("_9_M_TID","click")
            }

            TestEmiItem._12_M_TID ->{
                testEmiOption=TestEmiItem._12_M_TID.id
                logger("_12_M_TID","click")
            }
        }*/

        (activity as NavigationActivity).transactFragment(NewInputAmountFragment().apply {
            arguments = Bundle().apply {
                putSerializable("type", EDashboardItem.TEST_EMI)
                putString(NavigationActivity.INPUT_SUB_HEADING, "")
                putString("TestEmiOption", testEmiItem.id)
            }
        }, true)


    }
}

interface ITestEmiItemClick{

    fun testEmiItemClick(testEmiItem: TestEmiItem)
}

// adapter
class TestEmiAdapter(private var iTestEmiItemClick: ITestEmiItemClick?, private val testEmiItem: MutableList<TestEmiItem>) : RecyclerView.Adapter<TestEmiAdapter.TestEmiViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestEmiViewHolder {

        val itemBinding = ItemBankFunctionsBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)
        return TestEmiViewHolder(itemBinding)
    }

    override fun getItemCount(): Int = testEmiItem.size

    override fun onBindViewHolder(holder: TestEmiViewHolder, position: Int) {

        val model = testEmiItem[position]
        holder.viewBinding.textView.text = model._name


        holder.viewBinding.relLayParent.setOnClickListener {

            iTestEmiItemClick?.testEmiItemClick(model)

        }

    }

    inner class TestEmiViewHolder(val viewBinding: ItemBankFunctionsBinding) : RecyclerView.ViewHolder(viewBinding.root)
}