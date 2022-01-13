package com.bonushub.crdb.view.fragments


import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
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
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.utils.*
import com.bonushub.crdb.utils.Field48ResponseTimestamp.getTptData


import com.bonushub.crdb.view.activity.IFragmentRequest
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.adapter.DashBoardAdapter
import com.bonushub.crdb.viewmodel.DashboardViewModel
import com.bonushub.crdb.viewmodel.SettlementViewModel
import com.bonushub.pax.utils.EDashboardItem
import com.bonushub.pax.utils.PreferenceKeyConstant
import com.bonushub.pax.utils.SettlementComingFrom
import com.mindorks.example.coroutines.utils.Status


import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.coroutines.*
import java.lang.Runnable
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

    private var runnable: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())


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
        isDashboardOpen = true
        Utility().hideSoftKeyboard(requireActivity())

        dashboardViewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)
        observeDashboardViewModel()

        //region======================Change isAutoSettleDone Boolean Value to False if Date is greater then :- written by kushal
        //last saved Auto Settle Date:-
        if (!TextUtils.isEmpty(AppPreference.getString(PreferenceKeyConstant.LAST_SAVED_AUTO_SETTLE_DATE.keyName))) {
            if (AppPreference.getString(PreferenceKeyConstant.LAST_SAVED_AUTO_SETTLE_DATE.keyName).toInt() > 0) {
                if (getSystemTimeIn24Hour().terminalDate().toInt() >
                    AppPreference.getString(PreferenceKeyConstant.LAST_SAVED_AUTO_SETTLE_DATE.keyName).toInt()
                ) {
                    AppPreference.saveBoolean(PreferenceKeyConstant.IsAutoSettleDone.keyName, false)
                }
            }
        }
        //endregion

        logger("check",""+AppPreference.getBoolean(PreferenceKeyConstant.IsAutoSettleDone.keyName))
        //region=======================Check For AutoSettle at regular interval if App is on Dashboard:-
        if (isDashboardOpen && !AppPreference.getBoolean(PreferenceKeyConstant.IsAutoSettleDone.keyName))
            checkForAutoSettle()
        //endregion
    }

    override fun onPause() {
        super.onPause()
        //timer?.cancel()
        runnable?.let { handler.removeCallbacks(it) }
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


    //region============================Auto Settle Check on Dashboard at Regular Intervals:-
    private fun checkForAutoSettle() {
        runnable = object : Runnable {
            override fun run() {
                try {
                    logger("AutoSettle:- ", "Checking....")
                    autoSettleBatch()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    //also call the same runnable to call it at regular interval
                    handler.postDelayed(this, 20000)
                }
            }
        }
        handler.post(runnable as Runnable)
    }
    //endregion

    //region=======================Check for User IDLE on Dashboard and do auto settle if conditions match:-
    private fun autoSettleBatch() {
        //val tptData = runBlocking(Dispatchers.IO) { TerminalParameterTable.selectFromSchemeTable() }
        val tptData = runBlocking(Dispatchers.IO) { getTptData() }
        //val batchData = runBlocking(Dispatchers.IO) { BatchFileDataTable.selectBatchData() }
        val batchData = runBlocking(Dispatchers.IO) { appDao.getAllBatchData() }
        Log.d("HostForceSettle:- ", tptData?.forceSettle ?: "")
        Log.d("HostForceSettleTime:- ", tptData?.forceSettleTime ?: "")
        Log.d(
            "System Date Time:- ",
            "${getSystemTimeIn24Hour().terminalDate()} ${getSystemTimeIn24Hour().terminalTime()}"
        )

        // region temp for testing
//        tptData?.forceSettleTime = "183030"
//        tptData?.forceSettle = "1"
        // end region

        if (isDashboardOpen && !AppPreference.getBoolean(PreferenceKeyConstant.IsAutoSettleDone.keyName)) {

            Log.d("Dashboard Open:- ", "Yes")
            if (!TextUtils.isEmpty(tptData?.forceSettle)
                && !TextUtils.isEmpty(tptData?.forceSettleTime)
                && tptData?.forceSettle == "1"
            ) {
                if ((tptData.forceSettleTime.toLong() == getSystemTimeIn24Hour().terminalTime().toLong()
                            || getSystemTimeIn24Hour().terminalTime().toLong() > tptData.forceSettleTime.toLong()
                            ) /*&& batchData.size > 0*/
                ) {
                    logger("Auto Settle:- ", "Auto Settle Available")
                    val data = runBlocking(Dispatchers.IO) {
                        /*CreateSettlementPacket(
                            ProcessingCode.SETTLEMENT.code, batchData
                        ).createSettlementISOPacket()*/
                        // this code below converted

                        CreateSettlementPacket(appDao).createSettlementISOPacket()
                    }
                    lifecycleScope.launch(Dispatchers.IO) {
                        val settlementByteArray = data.generateIsoByteRequest()
                        (activity as NavigationActivity).settleBatch1(
                            settlementByteArray,
                            SettlementComingFrom.DASHBOARD.screenType
                        )
                    }
                } else
                    logger("Auto Settle:- ", "Auto Settle Mismatch Time")
            } else
                logger("Auto Settle:- ", "Auto Settle Not Available")
        } else {
            Log.d("Dashboard Close:- ", "Yes")
        }
    }
    //endregion
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