package com.bonushub.crdb.view.fragments


import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider

import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager

import com.bonushub.crdb.databinding.FragmentDashboardBinding
import com.bonushub.crdb.di.scope.BHDashboardItem
import com.bonushub.crdb.utils.DeviceHelper

import com.bonushub.crdb.utils.isExpanded
import com.bonushub.crdb.view.activity.IFragmentRequest
import com.bonushub.crdb.view.adapter.DashBoardAdapter
import com.bonushub.crdb.viewmodel.BankFunctionsViewModel
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



class DashboardFragment : androidx.fragment.app.Fragment() {
    companion object {
        var toRefresh = true
        val TAG = DashboardFragment::class.java.simpleName
    }
    private var defaultScope = CoroutineScope(Dispatchers.IO)
    lateinit var dashboardViewModel : DashboardViewModel
   private var iFragmentRequest: IFragmentRequest? = null
    private val itemList = mutableListOf<EDashboardItem>()
    private val list1 = arrayListOf<EDashboardItem>()
    private val list2 = arrayListOf<EDashboardItem>()
    private var battery: String? = null
    private var batteryINper: Int = 0
    private val dashBoardAdapter by lazy {
        DashBoardAdapter(iFragmentRequest, ::onItemLessMoreClick)
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
        dashboardViewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)
        observeDashboardViewModel()
    }
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IFragmentRequest) {
            iFragmentRequest = context
        }
    }

    private fun observeDashboardViewModel(){

        lifecycleScope.launch(Dispatchers.Main) {
            dashboardViewModel.eDashboardItem()?.observe(viewLifecycleOwner,{
                setupRecyclerview(it)
                setUpInitializtion()

            })

        }




    }

    private fun setUpInitializtion() {
        try {
            DeviceHelper.doTerminalInitialization(
                request = TerminalInitializationRequest(
                    1,
                    listOf("30160035")
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
    private fun setupRecyclerview(list:ArrayList<EDashboardItem>){
        lifecycleScope.launch(Dispatchers.Main) {
            dashboard_RV.layoutManager = GridLayoutManager(activity, 3)
            dashboard_RV.itemAnimator = DefaultItemAnimator()
            dashboard_RV.adapter = dashBoardAdapter
            if (isExpanded) dashBoardAdapter.onUpdatedItem(list) else dashBoardAdapter.onUpdatedItem(
                list
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