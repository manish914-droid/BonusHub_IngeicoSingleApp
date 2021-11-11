package com.bonushub.crdb.view.fragments

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.databinding.FragmentDashboardBinding
import com.bonushub.crdb.di.scope.BHDashboardItem
import com.bonushub.crdb.utils.DeviceHelper
import com.bonushub.crdb.utils.isExpanded
import com.bonushub.crdb.view.activity.TransactionActivity
import com.bonushub.crdb.view.adapter.DashBoardAdapter
import com.bonushub.crdb.viewmodel.DashboardViewModel
import com.bonushub.pax.utils.EDashboardItem

import com.bonushub.pax.utils.UiAction

import com.google.gson.Gson
import com.ingenico.hdfcpayment.listener.OnOperationListener
import com.ingenico.hdfcpayment.request.TerminalInitializationRequest
import com.ingenico.hdfcpayment.response.OperationResult
import com.ingenico.hdfcpayment.response.TerminalInitializationResponse
import com.ingenico.hdfcpayment.type.RequestStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.coroutines.*

@AndroidEntryPoint
class DashboardFragment : Fragment(),IFragmentRequest {
    companion object {
        var toRefresh = true
        val TAG = DashboardFragment::class.java.simpleName
    }
    private var defaultScope = CoroutineScope(Dispatchers.IO)
    private val dashboardViewModel : DashboardViewModel by viewModels()
   // private var iFragmentRequest: IFragmentRequest? = null
    private val itemList = mutableListOf<EDashboardItem>()
    private val list1 = arrayListOf<EDashboardItem>()
    private val list2 = arrayListOf<EDashboardItem>()
    private val dashBoardAdapter by lazy {
        DashBoardAdapter(this, ::onItemLessMoreClick)
    }
    private  var data: MutableList<BannerConfigModal>?=null
    private var animShow: Animation? = null
    private var animHide: Animation? = null
    private var binding: FragmentDashboardBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding?.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("Dashboard:- ", "onViewCreated")
        dashboardViewModel.fetchtptData()
        observeDashboardViewModel()
    }

    private fun observeDashboardViewModel(){
        dashboardViewModel.mutableLiveData.observe(viewLifecycleOwner, Observer {
            defaultScope.launch {
                Log.d("tpt===>:- ", Gson().toJson(it))
                val tpt = it
                if (toRefresh || itemList.isEmpty()) {
                    itemList.clear()
                    list1.clear()
                    list2.clear()

                    if (tpt != null) {
                        val tableClass =
                            tpt::class.java //Class Name (class com.bonushub.pax.utilss.TerminalParameterTable)
                        for (e in tableClass.declaredFields) {
                            val ann = e.getAnnotation(BHDashboardItem::class.java)
                            //If table's field  having the particular annotation as @BHDasboardItem then it returns the value ,If not then return null
                            if (ann != null) {
                                e.isAccessible = true
                                val t = e.get(tpt) as String
                                if (t == "1") {
                                    itemList.add(ann.item)
                                    if (ann.childItem != EDashboardItem.NONE) {
                                        itemList.add(ann.childItem)
                                    }
                                }
                            }
                        }

                    } else {
                        itemList.add(EDashboardItem.NONE)
                    }
                    Log.d("itemList===>:- ", Gson().toJson(itemList))
                    itemList.add(EDashboardItem.MERCHANT_REFERRAL)
                    itemList.add(EDashboardItem.CROSS_SELL)
                    Log.d("itemList===>:- ", Gson().toJson(itemList))
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
                        if (totalPreAuthItem.size > 0) {
                            val preAuth = EDashboardItem.PRE_AUTH_CATAGORY
                            preAuth.childList = totalPreAuthItem
                            itemList.add(preAuth)
                        }
                    }

                    itemList.sortWith(compareBy { it.rank })
                    // Below code is used for dashboard items divided into view less and view more functionality
                    for (lst in itemList.indices) {
                        if (data?.isNotEmpty() == true) {
                            if (lst <= 5) {
                                list1.add(itemList[lst])
                            } else {
                                list1[5] = EDashboardItem.MORE
                                list2.addAll(itemList)
                                list2.add(EDashboardItem.LESS)
                                break
                            }
                        } else {
                            list1.addAll(itemList)
                            break

                        }

                        /*    if (lst <= 5) {
                            list1.add(itemList[lst])
                        } else {
                            list1[5] = EDashboardItem.MORE
                            list2.addAll(itemList)
                            list2.add(EDashboardItem.LESS)
                            break
                        }*/
                    }

                    withContext(Dispatchers.Main) {
                        val result = async { setupRecyclerview() }.await()
                        val result1 = async {
                            delay(2000)
                              setUpInitializtion()
                        }.await()
                    }

                    // Setting up recyclerview of DashBoard Items

                } else {
                    binding?.dashboardRV?.apply {
                        layoutManager = GridLayoutManager(activity, 3)
                        itemAnimator = DefaultItemAnimator()
                        adapter = dashBoardAdapter
                        if (isExpanded) dashBoardAdapter.onUpdatedItem(list2) else dashBoardAdapter.onUpdatedItem(
                            list1
                        )
                        scheduleLayoutAnimation()
                    }
                }
            }
      

        })


    }

    private fun setUpInitializtion() {
        try {
            DeviceHelper.doTerminalInitialization(
                request = TerminalInitializationRequest(
                    1,
                    listOf("41501379")
                ),
                listener = object : OnOperationListener.Stub() {
                    override fun onCompleted(p0: OperationResult?) {
                        Log.d(TAG, "OnTerminalInitializationListener.onCompleted")
                        val response = p0?.value as? TerminalInitializationResponse
                        val initResult =
                            """
                                   Response_Code = ${response?.responseCode}
                                   API_Response_Status = ${response?.status}
                                   Response_Code = ${response?.responseCode}
                                   TIDStatusList = [${response?.tidStatusList?.joinToString()}]
                                   TIDs = [${response?.tidList?.joinToString()}]
                                   INITDATAList = [${response?.initDataList?.firstOrNull().toString()}]
                                """.trimIndent()

                        when (response?.status) {
                            RequestStatus.SUCCESS -> println(initResult)
                            RequestStatus.ABORTED,
                            RequestStatus.FAILED -> println(initResult)
                            else -> println(initResult)
                        }
                    }
                }
            )
        }
        catch (ex: Exception){
            ex.printStackTrace()
        }
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

    override fun onFragmentRequest(
        action: UiAction,
        data: Any,
        extraPair: Triple<String, String, Boolean>?
    ) {

        }


    override fun onDashBoardItemClick(action: EDashboardItem) {
        val intent = Intent (getActivity(), TransactionActivity::class.java)
        getActivity()?.startActivity(intent)
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

//region===================BannerConfigModal:-
data class BannerConfigModal(
    var bannerImageBitmap: Bitmap,
    var bannerID: String,
    var bannerDisplayOrderID: String,
    var bannerShowOrHideID: String,
    var clickableOrNot: String,
    var bannerClickActionID: String,
    var bannerClickMessageData: String
)
//endregion