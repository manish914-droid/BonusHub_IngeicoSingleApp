package com.bonushub.crdb.india.view.fragments.tets_emi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentTestEmiBinding
import com.bonushub.crdb.india.databinding.ItemReportsBinding
import com.bonushub.crdb.india.utils.EDashboardItem
import com.bonushub.crdb.india.utils.TestEmiItem
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.fragments.NewInputAmountFragment
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
        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_bankfunction_new)

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            try {
                parentFragmentManager.popBackStackImmediate()
            }catch (ex:Exception)
            {
                ex.printStackTrace()
            }
        }


        iTestEmiItemClick = this
        testEmiItem = mutableListOf()

        testEmiItem.addAll(TestEmiItem.values())
        val sortedList = testEmiItem.sortedWith(compareBy { it.id.toInt() })
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

        val itemBinding = ItemReportsBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)
        return TestEmiViewHolder(itemBinding)
    }

    override fun getItemCount(): Int = testEmiItem.size

    override fun onBindViewHolder(holder: TestEmiViewHolder, position: Int) {

        val model = testEmiItem[position]
        holder.viewBinding.imgViewIcon.setImageResource(R.drawable.ic_bankfunction_new)
        holder.viewBinding.textView.text = model._name


        holder.viewBinding.relLayParent.setOnClickListener {
            holder.viewBinding.relLayParent.setBackgroundResource(R.drawable.edge_brand_selected)
            iTestEmiItemClick?.testEmiItemClick(model)

        }

    }

    inner class TestEmiViewHolder(val viewBinding: ItemReportsBinding) : RecyclerView.ViewHolder(viewBinding.root)
}