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
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.disputetransaction.CreateSettlementPacket
import com.bonushub.crdb.utils.checkBaseTid
import com.bonushub.crdb.utils.doInitializtion



import com.bonushub.crdb.utils.isExpanded
import com.bonushub.crdb.view.activity.IFragmentRequest
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.adapter.DashBoardAdapter
import com.bonushub.crdb.viewmodel.DashboardViewModel
import com.bonushub.crdb.viewmodel.SettlementViewModel
import com.bonushub.pax.utils.EDashboardItem
import com.mindorks.example.coroutines.utils.Status


import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.coroutines.*
import javax.inject.Inject


@AndroidEntryPoint
class DashboardFragment : androidx.fragment.app.Fragment() {
    companion object {
        var toRefresh = true
        val TAG = DashboardFragment::class.java.simpleName
    }
    @Inject
    lateinit var appDao: AppDao
    private val settlementViewModel : SettlementViewModel by viewModels()
    private var ioSope = CoroutineScope(Dispatchers.IO)
    private var defaultSope = CoroutineScope(Dispatchers.Default)
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
            dashboardViewModel.eDashboardItem().observe(viewLifecycleOwner,{
                ioSope.launch(Dispatchers.Main) {
                    val result = async {  setupRecyclerview(it) }.await()
                    val result1 = async {
                        var listofTids = checkBaseTid(appDao)
                        println("List of tids are"+listofTids)
                        delay(1000)
                       // sett()
                    doInitializtion(appDao,listofTids)
                    }.await()

                }
            })

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