package com.bonushub.crdb.view.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.NavigationActivity
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentDashboardBinding
import com.bonushub.crdb.di.scope.BHDashboardItem
import com.bonushub.crdb.utils.Result
import com.bonushub.crdb.view.adapter.DashBoardAdapter
import com.bonushub.crdb.viewmodel.DashboardViewModel
import com.bonushub.crdb.viewmodel.InitViewModel
import com.bonushub.pax.utils.EDashboardItem
import com.bonushub.pax.utils.UiAction
import com.bonushub.pax.utils.isExpanded
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class DashboardFragment : Fragment() {
    companion object {
        var toRefresh = true
    }
    private val dashboardViewModel : DashboardViewModel by viewModels()
    private var iFragmentRequest: IFragmentRequest? = null
    private val itemList = mutableListOf<EDashboardItem>()
    private val list1 = arrayListOf<EDashboardItem>()
    private val list2 = arrayListOf<EDashboardItem>()
    /* private val mAdapter by lazy {
         DashBoardAdapter(iFragmentRequest, ::onItemLessMoreClick)
     }*/
    private val dashBoardAdapter by lazy {
        DashBoardAdapter(iFragmentRequest, ::onItemLessMoreClick)
    }
    private var animShow: Animation? = null
    private var animHide: Animation? = null
    private var binding: FragmentDashboardBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)

        return binding?.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dashboardViewModel.fetchtptData()
        observeDashboardViewModel()
    }

    fun observeDashboardViewModel(){
        dashboardViewModel.mutableLiveData.observe(viewLifecycleOwner, Observer {
            Log.d("tpt===>:- ", Gson().toJson(it))
            if (it != null) {
                Log.d("tpt===>:- ", Gson().toJson(it))
                val tableClass =
                    it::class.java //Class Name (class com.bonushub.pax.utilss.TerminalParameterTable)
                for (e in tableClass.declaredFields) {
                    val ann = e.getAnnotation(BHDashboardItem::class.java)
                    //If table's field  having the particular annotation as @BHDasboardItem then it returns the value ,If not then return null
                    if (ann != null) {
                        e.isAccessible = true
                        val t = e.get(it) as String
                        if (t == "1") {
                            itemList.add(ann.item)
                            if (ann.childItem != EDashboardItem.NONE) {
                                itemList.add(ann.childItem)
                            }
                        }
                    }
                }
                Log.d("itemList===>:- ", Gson().toJson(itemList))
                //Adding Field HDFC Cross Sell in Dashboard List:-
                  it.reservedValues = "00000000000000010000"
                when {
                    it.reservedValues[13].toString().toInt() == 1 ||
                            it.reservedValues[14].toString().toInt() == 1 ||
                            it.reservedValues[15].toString().toInt() == 1 ||
                            it.reservedValues[16].toString().toInt() == 1 -> itemList.add(
                        EDashboardItem.CROSS_SELL
                    )
                }

            } else {
                itemList.add(EDashboardItem.NONE)
            }

            // This list is a list where all types of preath available which was enable by backend
            val totalPreAuthItem = mutableListOf<EDashboardItem>()
            totalPreAuthItem.addAll(itemList)

            //After converting we are getting the total preauth trans type available(by retainAll fun)
            //It returns true if any praauth item is available and return false if no preauth item found
            val isAnyPreAuthItemAvailable = totalPreAuthItem.retainAll { item ->
                item == EDashboardItem.PREAUTH || item == EDashboardItem.PREAUTH_COMPLETE
                        || item == EDashboardItem.VOID_PREAUTH || item == EDashboardItem.PENDING_PREAUTH
            }

            if (isAnyPreAuthItemAvailable) {
                itemList.removeAll { item ->
                    item == EDashboardItem.PREAUTH || item == EDashboardItem.PREAUTH_COMPLETE
                            || item == EDashboardItem.VOID_PREAUTH || item == EDashboardItem.PENDING_PREAUTH
                }
            /*    val preAuth = EDashboardItem.PREAUTHCATAGORY
                preAuth.childList = totalPreAuthItem
                itemList.add(preAuth)*/
            }

            itemList.sortWith(compareBy { it.rank })
            // Below code is used for dashboard items divided into view less and view more functionality
            for (lst in itemList.indices) {
                if (lst <= 5) {
                    list1.add(itemList[lst])
                } else {
                    list1[5] = EDashboardItem.MORE
                    list2.addAll(itemList)
                    list2.add(EDashboardItem.LESS)
                    break
                }
            }

            setupRecyclerview()
      

        })


    }
    // Setting up recyclerview of DashBoard Items
    private fun setupRecyclerview(){
        lifecycleScope.launch(Dispatchers.Main) {
            dashboard_RV.layoutManager = GridLayoutManager(activity, 3)
            dashboard_RV.itemAnimator = DefaultItemAnimator()
            dashboard_RV.adapter = dashBoardAdapter
            if (isExpanded) dashBoardAdapter.onUpdatedItem(list2) else dashBoardAdapter.onUpdatedItem(
                list1
            )
            dashboard_RV.scheduleLayoutAnimation()
        }
    }


    private fun onItemLessMoreClick(item: EDashboardItem) {
        when (item) {
            EDashboardItem.MORE -> {
                isExpanded = true
                binding?.pagerViewLL?.startAnimation(animHide)
                binding?.pagerViewLL?.visibility = View.GONE
                dashBoardAdapter.onUpdatedItem(list2)
                dashBoardAdapter.notifyDataSetChanged()
                binding?.dashboardRV?.scheduleLayoutAnimation()
            }
            EDashboardItem.LESS -> {
                isExpanded = false
                binding?.pagerViewLL?.visibility = View.VISIBLE
                binding?.pagerViewLL?.startAnimation(animShow)
                dashBoardAdapter.onUpdatedItem(list1)
                dashBoardAdapter.notifyDataSetChanged()
                binding?.dashboardRV?.scheduleLayoutAnimation()
            }
            else -> {
            }
        }
    }
}


interface IFragmentRequest {
    fun onFragmentRequest(
        action: UiAction,
        data: Any,
        extraPair: Triple<String, String, Boolean>? = Triple("", "", third = true)
    )

    fun onDashBoardItemClick(action: EDashboardItem)


}