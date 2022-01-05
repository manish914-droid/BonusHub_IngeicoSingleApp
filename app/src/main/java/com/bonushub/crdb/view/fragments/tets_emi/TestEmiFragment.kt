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
import com.bonushub.crdb.utils.logger
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.fragments.NewInputAmountFragment
import com.bonushub.pax.utils.EDashboardItem
import com.bonushub.pax.utils.TestEmiItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class TestEmiFragment : Fragment(), ITestEmiItemClick {

    var binding:FragmentTestEmiBinding? = null

    private val testEmiItem: MutableList<TestEmiItem> by lazy { mutableListOf<TestEmiItem>() }
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
        testEmiItem.addAll(TestEmiItem.values())

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

        (activity as NavigationActivity).transactFragment(NewInputAmountFragment().apply {
            arguments = Bundle().apply {
                putSerializable("type", EDashboardItem.TEST_EMI)
                putString(NavigationActivity.INPUT_SUB_HEADING, "")
                putString("TestEmiOption", TestEmiItem.BASE_TID.id)
            }
        }, true)

        when(testEmiItem){

            TestEmiItem.BASE_TID ->{
                logger("BASE_TID","click")
            }

            TestEmiItem.OFFUS_TID ->{
                logger("OFFUS_TID","click")

            }

            TestEmiItem._3_M_TID ->{
                logger("_3_M_TID","click")

            }

            TestEmiItem._6_M_TID ->{
                logger("_6_M_TID","click")

            }

            TestEmiItem._9_M_TID ->{
                logger("_9_M_TID","click")

            }

            TestEmiItem._12_M_TID ->{
                logger("_12_M_TID","click")

            }
        }

    }
}

interface ITestEmiItemClick{

    fun testEmiItemClick(testEmiItem:TestEmiItem)
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