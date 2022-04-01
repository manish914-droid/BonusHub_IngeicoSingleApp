package com.bonushub.crdb.india.view.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bonushub.crdb.india.BuildConfig
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.appupdate.AppUpdateDownloadManager
import com.bonushub.crdb.india.appupdate.OnDownloadCompleteListener

import com.bonushub.crdb.india.databinding.ActivityNavigationBinding
import com.bonushub.crdb.india.databinding.MainDrawerBinding
import com.bonushub.crdb.india.db.AppDao
import com.bonushub.crdb.india.db.AppDatabase
import com.bonushub.crdb.india.di.DBModule
import com.bonushub.crdb.india.disputetransaction.CreateSettlementPacket
import com.bonushub.crdb.india.model.local.AppPreference
import com.bonushub.crdb.india.model.local.BatchTable
import com.bonushub.crdb.india.model.local.BrandEMISubCategoryTable
import com.bonushub.crdb.india.model.local.TerminalParameterTable
import com.bonushub.crdb.india.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.india.model.remote.BrandEMIProductDataModal
import com.bonushub.crdb.india.serverApi.HitServer
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.checkInternetConnection
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getHDFCTptData
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getTptData
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.performOperation
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.selectAllDigiPosData
import com.bonushub.crdb.india.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.india.utils.dialog.OnClickDialogOkCancel
import com.bonushub.crdb.india.utils.printerUtils.PrintUtil
import com.bonushub.crdb.india.view.base.BaseActivityNew
import com.bonushub.crdb.india.view.fragments.*
import com.bonushub.crdb.india.view.fragments.digi_pos.DigiPosMenuFragment
import com.bonushub.crdb.india.view.fragments.pre_auth.PreAuthFragment
import com.bonushub.crdb.india.view.fragments.pre_auth.PreAuthPendingFragment
import com.bonushub.crdb.india.viewmodel.BankFunctionsViewModel
import com.bonushub.crdb.india.viewmodel.InitViewModel
import com.bonushub.crdb.india.viewmodel.SettlementViewModel
import com.bonushub.pax.utils.*
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import com.ingenico.hdfcpayment.listener.OnOperationListener
import com.ingenico.hdfcpayment.response.OperationResult
import com.mindorks.example.coroutines.utils.Status
import com.usdk.apiservice.aidl.pinpad.DeviceName
import com.usdk.apiservice.aidl.pinpad.KAPId
import com.usdk.apiservice.aidl.pinpad.UPinpad
import com.usdk.apiservice.aidl.tms.OnResultListener
import com.usdk.apiservice.aidl.tms.TMSData
import com.usdk.apiservice.aidl.tms.UTMS
import com.usdk.apiservice.limited.pinpad.PinpadLimited
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_navigation.*
import kotlinx.android.synthetic.main.navigation_footer_layout.view.*
import kotlinx.coroutines.*
import java.io.File
import java.lang.Runnable
import javax.inject.Inject
import android.view.Gravity

import android.os.Build
import java.lang.IllegalArgumentException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.HashMap


@AndroidEntryPoint
class NavigationActivity : BaseActivityNew(), DeviceHelper.ServiceReadyListener,NavigationView.OnNavigationItemSelectedListener,
    ActivityCompat.OnRequestPermissionsResultCallback , IFragmentRequest {
    private  var  j : Long = 0
    private var tms: UTMS? = null
    @Inject
    lateinit var appDao: AppDao
    private var navigationBinding: ActivityNavigationBinding?=null
    private var navHostFragment: NavHostFragment? = null
    private var isToExit = false
    private var tempSettlementByteArray: ByteArray? = null
    // private var isoPacketByteArray: ByteArray? = null
    private var headerView: View? = null
    private var mainDrawerBinding: MainDrawerBinding? = null
    private var showLessOnBackPress: ShowLessOnBackPress? = null
    private var deviceserialno: String? = null
    private var devicemodelno: String? = null
    private var tid: String? = null
    private var mid: String? = null
    private var pinpad: UPinpad? = null
    private var pinpadLimited: PinpadLimited? = null
    private val settlementViewModel : SettlementViewModel by viewModels()
    var currentFocus = false

    // To keep track of activity's foreground/background status
    var isPaused = false

    var collapseNotificationHandler: Handler? = null

    private val dialog by lazy {   Dialog(this) }
    companion object {
        val TAG = NavigationActivity::class.java.simpleName
        const val INPUT_SUB_HEADING = "input_amount"

    }
    private var ioSope = CoroutineScope(Dispatchers.IO)

    @Inject
    lateinit var appDatabase: AppDatabase

    private val bankFunctionsViewModel : BankFunctionsViewModel by viewModels()
    private var dialogAdminPassword:Dialog? = null

    private val initViewModel : InitViewModel by viewModels()
    // for init`
    private var isFresApp = "true"
    private var isFresAppStatus = false
    //Below Key is only we get in case of Auto Settlement == 1 after Sale:-
    private val appUpdateFromSale by lazy { intent.getBooleanExtra("appUpdateFromSale", false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigationBinding = ActivityNavigationBinding.inflate(layoutInflater)
        setContentView(navigationBinding?.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment?
        DeviceHelper.setServiceListener(this)
        setupNavigationDrawerLayout()
         lockStatusBar()
/*         isFresAppStatus = WifiPrefManager(this).isWifiStatus
         if (!isFresAppStatus) {
             isFresApp = WifiPrefManager(this).appStatus
         }
         if (isFresApp == "true" && isFresAppStatus) {
             wifiHandaling()
         }
         onWindowFocusChanged(false)*/
        //region============================Below Logic is to Hide Back Arrow from Toolbar
        navHostFragment?.navController?.addOnDestinationChangedListener { _, _, _ ->
            navigationBinding?.toobar?.dashboardToolbar?.navigationIcon = null
        }
        //endregion
        navigationBinding?.toobar?.mainToolbarStart?.setOnClickListener { toggleDrawer() }

        //region====================Adding Right Icon to Navigation Drawer Bank Function Option:-
        navigationBinding?.navView?.menu?.getItem(0)
            ?.setActionView(R.layout.bank_function_right_icon)
        //endregion

        //region Getting side drawer header for setting tid mid and merchant name to it
        navigationBinding?.navView?.setNavigationItemSelectedListener(this)
        headerView = navigationBinding?.navView?.getHeaderView(0)
        mainDrawerBinding = headerView?.let { MainDrawerBinding.bind(it) }
        // endregion
        decideDashBoard()

        observeMainViewModel()


        // refresh drawer tid and mid
        val mDrawerToggle: ActionBarDrawerToggle = object : ActionBarDrawerToggle(
            this, navigationBinding?.mainDl,
            R.drawable.ic_menu, R.string.merchant_name
        ) {
            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                // Do whatever you want here
                logger("drawer","close")

            }

            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                // Do whatever you want here
                logger("drawer","open")
                refreshDrawer()
            }
        }
        navigationBinding?.mainDl?.addDrawerListener(mDrawerToggle)


        bankFunctionsViewModel.adminPassword.observe(this@NavigationActivity) {

            if (it.value == true) {
                logger("isAdminPassword", ("" + it.value ?: false) as String)
                dialogAdminPassword?.dismiss()
                closeDrawer()
                transactFragment(BankFunctionsFragment())
            } else {
                Toast.makeText(
                    this@NavigationActivity,
                    R.string.invalid_password,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        //Settle Batch When Auto Settle == 1 After Sale:- kushal -> auto settlement doing in dashboard
        //      if (appUpdateFromSale) {
        //         autoSettleBatchData()
        //    }
    }




    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        isDashboardOpen = false
        //==============kushal ======= implemented drawer menu
        when(item.itemId)
        {
            R.id.bankFunction -> {

                DialogUtilsNew1.showDialog(this,getString(R.string.admin_password),getString(R.string.hint_enter_admin_password),onClickDialogOkCancel, false)
            }

            R.id.reportFunction -> {

                closeDrawer()
                transactFragment(ReportsFragment())

            }

            R.id.settlement -> {
                closeDrawer()
                if(AppPreference.getBoolean(PrefConstant.BLOCK_MENU_OPTIONS.keyName.toString())) {
                    alertBoxWithAction(
                        getString(R.string.batch_settle),
                        getString(R.string.please_settle_batch),
                        false, getString(R.string.positive_button_ok),
                        {
                            //autoSettleBatchData()
                        },
                        {})
                }
                else {
                    transactFragment(SettlementFragment())
                }

            }
        }
        return true
    }
    //region==========================SetUp Drawer Layout================
    private fun setupNavigationDrawerLayout() {
        navigationBinding?.navView?.setupWithNavController(navHostFragment?.navController!!)
        setupWithNavController(
            navigationBinding?.toobar?.dashboardToolbar!!,
            navHostFragment?.navController!!
        )
    }
    //endregion
    //region====================Toggle Drawer=========================
    private fun toggleDrawer() {
        if (navigationBinding?.mainDl!!.isDrawerOpen(GravityCompat.START)) {
            navigationBinding?.mainDl?.closeDrawer(GravityCompat.START, true)
        } else {
            navigationBinding?.mainDl?.openDrawer(GravityCompat.START, true)
        }
    }
    //endregion

    //region  Checked that the terminal is initialized or not.
    private fun decideDashBoard() {
        if (AppPreference.getLogin()) {
            //refreshDrawer()
            navHostFragment?.navController?.popBackStack()
            Log.e("NAV", "DECIDE HOME")
            navHostFragment?.navController?.navigate(R.id.dashBoardFragment)

        } else {
            AppPreference.saveString(PreferenceKeyConstant.Wifi_Communication.keyName, "0")
            GlobalScope.launch(Dispatchers.IO) {
                Utility().readLocalInitFile { status, msg ->
                    Log.d("Init File Read Status ", status.toString())
                    Log.d("Message ", msg)
                    //    refreshDrawer()
                }
            }
            navHostFragment?.navController?.popBackStack()
            navHostFragment?.navController?.navigate(R.id.initFragment)
        }
    }

    //endregion==========

    // region
    fun decideDashBoardOnBackPress() {
        if (AppPreference.getLogin()) {
            Log.e("NAV", "DECIDE HOME")
            transactFragment(DashboardFragment())

        } else {
            /*      GlobalScope.launch(Dispatchers.IO) {
                      Utility().readLocalInitFile { status, msg ->
                          Log.d("Init File Read Status ", status.toString())
                          Log.d("Message ", msg)
                          //    refreshDrawer()
                      }
                  }*/

            transactFragment(InitFragment())

        }
    }
    // end region
    //region============================ready
    override fun onReady(version: String?) {
        register(true)
        initDeviceInstance()

    }
    //endregion

    //region============================register
    private fun register(useEpayModule: Boolean) {
        try {
            DeviceHelper.register(useEpayModule)
            //  registerEnabled(false)
        } catch (e: IllegalStateException) {
            //  toast("register fail: " + e.message)
        }
    }
    //endregion




    //region============================initDeviceInstance
    protected fun initDeviceInstance() {
        Handler().postDelayed(Runnable {
            deviceserialno = DeviceHelper.getDeviceSerialNo()
            devicemodelno = DeviceHelper.getDeviceModel()
            pinpad = createPinpad(KAPId(0, 0), 0, DeviceName.IPP)
            try {
                val isSucc = pinpad!!.open()
                if (isSucc) {
                    //  outputText("open success")
                } else {
                    //   outputPinpadError("open fail");
                }
            } catch (e: RemoteException) {
                //  handleException(e)
            }
            try {
                pinpadLimited = PinpadLimited(applicationContext, KAPId(DemoConfig.REGION_ID, DemoConfig.KAP_NUM), 0, DemoConfig.PINPAD_DEVICE_NAME)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }



        }, 100)


    }

    //endregion

    //region============================createPinpad
    fun createPinpad(kapId: KAPId?, keySystem: Int, deviceName: String?): UPinpad? {
        return try {
            DeviceHelper.getPinpad(kapId, keySystem, deviceName)
        } catch (e: RemoteException) {
            e.printStackTrace()
            null
        }
    }
    //endregion




    //region============================fragment transaction
    fun transactSubCatFragment(isBackStackAdded: Boolean = false, brandDataMaster: BrandEMIMasterDataModal?, brandSubCatList: ArrayList<BrandEMISubCategoryTable>?, filteredSubCat: ArrayList<BrandEMISubCategoryTable>?,eDashBoardItem: EDashboardItem): Boolean {
        val trans = supportFragmentManager.beginTransaction().apply {
            val fragment= BrandEmiSubCategoryFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("brandDataMaster", brandDataMaster)
                    putSerializable("brandSubCatList", brandSubCatList)
                    putSerializable("filteredSubCat", filteredSubCat)
                    putSerializable("type", eDashBoardItem)
                    //  putBoolean("navigateFromMaster",true)
                    // putParcelableArrayList("brandSubCatList",ArrayList<Parcelable>( brandSubCatList))
                }
            }
            replace(R.id.nav_host_fragment, fragment, fragment::class.java.simpleName)
            addToBackStack(fragment::class.java.simpleName)
        }
        if (isBackStackAdded) trans.addToBackStack(null)
        return trans.commitAllowingStateLoss() >= 0
    }
    //endregion


    //region==========Setting for sidebar details==========
    private fun refreshDrawer() {


        headerView?.let { footer?.version_name?.text = "App Version :${BuildConfig.VERSION_NAME}"}
        headerView?.let { footer?.version_id?.text = "Revision Id :${BuildConfig.REVISION_ID}"}

        lifecycleScope.launch{
            // val tpt = DBModule.appDatabase.appDao?.getAllTerminalParameterTableData()?.get(0) // old
            var tpt:TerminalParameterTable? = null
            if(AppPreference.getLogin()) {
                tpt = DBModule.appDatabase.appDao?.getTerminalParameterTableDataByTidType("1")
            }else{
                tpt = DBModule.appDatabase.appDao?.getTerminalParameterTableDataByTidType("-1")
            }
            // old
            /* var listofTids = ArrayList<String>()
             tpt?.tidType?.forEachIndexed { index, tidType ->
                 if(tidType.equals("1")){
                     tpt?.terminalId?.get(index)
                     //Have to change again done
                   //  listofTids.add(0,tpt?.terminalId?.get(index) ?: "")
                 }
                 else{
                     //Have to change again done
                    // listofTids.add(tpt?.terminalId?.get(index) ?: "")
                 }
             }*/

            //tid = getString(R.string.terminal_id) + "   : " + listofTids[0]  // old
            tid = getString(R.string.terminal_id) + "   : " + tpt?.terminalId
            mid = getString(R.string.merchant_id) + "  : " + tpt?.merchantId

            withContext(Dispatchers.Main){
                mainDrawerBinding?.mdTidTv?.text = tid
                mainDrawerBinding?.mdMidTv?.text = mid
            }

            //region=================Show help Desk Number in Navigation Footer after Init:-

            val hdfcTpt = getHDFCTptData()
            if (hdfcTpt != null) {
                val helplineNumber = "HelpLine: ${hdfcTpt.helpDeskNumber.replace("F", "")}"
                //binding?.mainDrawerView?.helpDeskTV?.text = helplineNumber
                //binding?.mainDrawerView?.helpDeskTV?.visibility = View.VISIBLE
                headerView?.let { footer?.help_desk_number?.text = helplineNumber}
                headerView?.let { footer?.help_desk_number?.visibility = View.VISIBLE}
            }

            //endregion
        }

    }

    //region============================On Back Pressed======================
    override fun onBackPressed() {

        //logger("current Fragment2",""+navHostFragment?.navController?.currentDestination?.label,"e")
        /*if (navHostFragment?.navController?.currentDestination?.label ==
            NavControllerFragmentLabel.DASHBOARD_FRAGMENT_LABEL.destinationLabel || navHostFragment?.navController?.currentDestination?.label == "fragment_init"
        )*/
        /*logger("current Fragment3",""+supportFragmentManager.fragments.size)
        logger("current Fragment4",""+supportFragmentManager.fragments.toString())
        logger("current Fragment5",""+supportFragmentManager.fragments.get(0)::class.java.simpleName)*/
        if (supportFragmentManager.fragments.get(0)::class.java.simpleName.equals("NavHostFragment",true) || supportFragmentManager.fragments.get(0)::class.java.simpleName.equals("DashboardFragment",true) || supportFragmentManager.fragments.get(0)::class.java.simpleName.equals("InitFragment",true)) {
            Log.d("Dashboard:- ", "Inflated")
            if (isExpanded)
                showLessOnBackPress?.showLessDashOptions()
            else {
                if (navigationBinding?.mainDl?.isDrawerOpen(GravityCompat.START)!!)
                    navigationBinding?.mainDl?.closeDrawer(GravityCompat.START)
                else
                 exitApp()
            }
        }else if(supportFragmentManager.fragments.get(0)::class.java.simpleName.equals("BankFunctionsFragment",true)
            ||supportFragmentManager.fragments.get(0)::class.java.simpleName.equals("ReportsFragment",true)
            ||supportFragmentManager.fragments.get(0)::class.java.simpleName.equals("SettlementFragment",true) ){

            decideDashBoardOnBackPress()

        }else{
            supportFragmentManager.popBackStackImmediate()
        }
    }


    //endregion

    //region======================================================method to exitApp:-
    private fun exitApp() {
        if (isToExit) {
            super.finishAffinity()
        } else {
            isToExit = true
            Handler(Looper.getMainLooper()).postDelayed({
                isToExit = false
                Toast.makeText(this, "Double click back button to exit.", Toast.LENGTH_SHORT).show()

            }, 1000)
        }
    }
    //endregion

    // region =========kushal=== Close Drawer ==========
    private fun closeDrawer() {
        if (navigationBinding?.mainDl!!.isDrawerOpen(GravityCompat.START)) {
            navigationBinding?.mainDl?.closeDrawer(GravityCompat.START, true)
        }
    }
    //endregion

    // written by kushal region == dialog click
    var onClickDialogOkCancel:OnClickDialogOkCancel = object : OnClickDialogOkCancel{

        override fun onClickOk(dialog: Dialog, password:String) {

            dialogAdminPassword = dialog
            bankFunctionsViewModel.isAdminPassword(password)

            // convert in above
            /* bankFunctionsViewModel.isAdminPassword(password)?.observe(this@NavigationActivity,{

                    if(it)
                    {
                        dialog.dismiss()
                        closeDrawer()
                        transactFragment(BankFunctionsFragment())
                    }else{
                        Toast.makeText(this@NavigationActivity,R.string.invalid_password,Toast.LENGTH_LONG).show()
                    }
                })*/
        }

        override fun onClickCancel() {

        }

    }

    override fun onFragmentRequest(action: EDashboardItem, data: Any, extraPair: Triple<String, String, Boolean>?) {
        when (action) {
            EDashboardItem.SALE -> {
                if (checkInternetConnection()) {
                    //  val amt = data as String
                    val amt = (data as Pair<*, *>).first.toString()
                    val saleWithTipAmt = data.second.toString()
                    startActivityForResult(
                        Intent(this, TransactionActivity::class.java).apply {
                            val formattedTransAmount = "%.2f".format(amt.toDouble())
                            putExtra("saleAmt", formattedTransAmount)
                            putExtra("type", BhTransactionType.SALE.type)
                            putExtra("proc_code", ProcessingCode.SALE.code)
                            putExtra("mobileNumber", extraPair?.first)
                            putExtra("billNumber", extraPair?.second)
                            putExtra("saleWithTipAmt", saleWithTipAmt)
                            putExtra("edashboardItem",  EDashboardItem.SALE)
                        }, EIntentRequest.TRANSACTION.code
                    )
                } else {
                    ToastUtils.showToast(this,getString(R.string.no_internet_available_please_check_your_internet))
                }
            }

            EDashboardItem.BANK_EMI -> {
                if (checkInternetConnection()) {
                    //  val amt = data as String
                    val amt = (data as Pair<*, *>).first.toString()
                    val saleWithTipAmt = data.second.toString()
                    startActivity(
                        Intent(
                            this,
                            TransactionActivity::class.java
                        ).apply {
                            val formattedTransAmount = "%.2f".format(amt.toDouble())
                            putExtra("saleAmt", formattedTransAmount)
                            putExtra("type", BhTransactionType.SALE.type)
                            putExtra("proc_code", ProcessingCode.SALE.code)
                            putExtra("mobileNumber", extraPair?.first)
                            putExtra("billNumber", extraPair?.second)
                            putExtra("saleWithTipAmt", saleWithTipAmt)
                            putExtra("edashboardItem",  EDashboardItem.SALE)
                        }
                    )
                } else {
                    ToastUtils.showToast(this,getString(R.string.no_internet_available_please_check_your_internet))
                }
            }

            // Brand EMI calling is set on
            EDashboardItem.BRAND_EMI -> {


            }

            EDashboardItem.CASH_ADVANCE -> {
                if (checkInternetConnection()) {
                    //  val amt = data as String
                    val amt = (data as Pair<*, *>).first.toString()

                    startActivityForResult(
                        Intent(
                            this,
                            TransactionActivity::class.java
                        ).apply {
                            val formattedTransAmount = "%.2f".format(amt.toDouble())
                            putExtra("saleAmt", formattedTransAmount)
                            putExtra("type", BhTransactionType.CASH_AT_POS.type)
                            putExtra("proc_code", ProcessingCode.CASH_AT_POS.code)
                            /*      putExtra("mobileNumber", extraPair?.first)
                                  putExtra("billNumber", extraPair?.second)
                                  putExtra("saleWithTipAmt", saleWithTipAmt)*/
                            putExtra("edashboardItem",  EDashboardItem.CASH_ADVANCE)
                        }, EIntentRequest.TRANSACTION.code
                    )
                } else {
                    ToastUtils.showToast(this,getString(R.string.no_internet_available_please_check_your_internet))
                }
            }

            EDashboardItem.SALE_WITH_CASH -> {
                if (checkInternetConnection()) {
                    //  val amt = data as String
                    val amt = (data as Pair<*, *>).first.toString()
                    val cashBackAmount = data.second.toString()
                    startActivityForResult(
                        Intent(
                            this,
                            TransactionActivity::class.java
                        ).apply {
                            val formattedTransAmount = "%.2f".format(amt.toDouble())
                            putExtra("saleAmt", formattedTransAmount)
                            putExtra("cashBackAmt", cashBackAmount)
                            putExtra("type", BhTransactionType.SALE_WITH_CASH.type)
                            putExtra("proc_code", ProcessingCode.SALE_WITH_CASH.code)
                            putExtra("edashboardItem",  EDashboardItem.SALE_WITH_CASH)
                        }, EIntentRequest.TRANSACTION.code
                    )
                } else {
                    ToastUtils.showToast(this,getString(R.string.no_internet_available_please_check_your_internet))
                }
            }

            EDashboardItem.PREAUTH -> {
                if (checkInternetConnection()) {
                    //  val amt = data as String
                    val amt = (data as Pair<*, *>).first.toString()
                    startActivityForResult(
                        Intent(
                            this,
                            TransactionActivity::class.java
                        ).apply {
                            val formattedTransAmount = "%.2f".format(amt.toDouble())
                            putExtra("saleAmt", formattedTransAmount)
                            putExtra("type", BhTransactionType.PRE_AUTH.type)
                            putExtra("proc_code", ProcessingCode.PRE_AUTH.code)
                            /*      putExtra("mobileNumber", extraPair?.first)
                                  putExtra("billNumber", extraPair?.second)
                                  putExtra("saleWithTipAmt", saleWithTipAmt)*/
                            putExtra("edashboardItem",  EDashboardItem.PREAUTH)
                        }, EIntentRequest.TRANSACTION.code
                    )
                } else {
                    ToastUtils.showToast(this,getString(R.string.no_internet_available_please_check_your_internet))
                }
            }

            EDashboardItem.REFUND -> {
                if (checkInternetConnection()) {
                    //  val amt = data as String
                    val amt = (data as Pair<*, *>).first.toString()
                    startActivityForResult(
                        Intent(
                            this,
                            TransactionActivity::class.java
                        ).apply {
                            val formattedTransAmount = "%.2f".format(amt.toDouble())
                            putExtra("saleAmt", formattedTransAmount)
                            putExtra("type", BhTransactionType.REFUND.type)
                            putExtra("proc_code", ProcessingCode.REFUND.code)
                            /*      putExtra("mobileNumber", extraPair?.first)
                                  putExtra("billNumber", extraPair?.second)
                                  putExtra("saleWithTipAmt", saleWithTipAmt)*/
                            putExtra("edashboardItem",  EDashboardItem.REFUND)
                        }, EIntentRequest.TRANSACTION.code
                    )
                } else {
                    ToastUtils.showToast(this,getString(R.string.no_internet_available_please_check_your_internet))
                }
            }

            EDashboardItem.PREAUTH_COMPLETE->{
                if (checkInternetConnection()) {
                    //  val amt = data as String
                    val authCompletionData = (data as Pair<*, *>).first as AuthCompletionData
                    val amt= authCompletionData.authAmt
                    startActivityForResult(
                        Intent(
                            this,
                            TransactionActivity::class.java
                        ).apply {
                            val formattedTransAmount = "%.2f".format(amt?.toDouble())
                            putExtra("saleAmt", formattedTransAmount)
                            putExtra("authCompletionData",authCompletionData)
                            putExtra("type", BhTransactionType.PRE_AUTH_COMPLETE.type)
                            putExtra("proc_code", ProcessingCode.PRE_SALE_COMPLETE.code)
                            putExtra("edashboardItem",  EDashboardItem.PREAUTH_COMPLETE)
                        }, EIntentRequest.TRANSACTION.code
                    )
                } else {
                    ToastUtils.showToast(this,getString(R.string.no_internet_available_please_check_your_internet))
                }


            }

            EDashboardItem.EMI_ENQUIRY -> {
            }
            EDashboardItem.BRAND_EMI_CATALOGUE, EDashboardItem.BANK_EMI_CATALOGUE -> {
                val amt = (data as Pair<*, *>).first.toString()
                val brandId=(data as Pair<*, *>).second.toString()
                val emiCatalogueImageList =
                    runBlocking(Dispatchers.IO) {
                        /// readEMICatalogueAndBannerImages()
                    }
                transactFragment(EMIIssuerList().apply {
                    arguments = Bundle().apply {
                        putSerializable("type", action)
                        putString("proc_code", ProcessingCode.PRE_AUTH.code)
                        putString("mobileNumber", extraPair?.first)
                        putString("enquiryAmt", amt)
                        putString("brandId", brandId)
                        //  putSerializable("imagesData", emiCatalogueImageList as HashMap<*, *>)


                    }
                })
            }

            else -> {

            }
        }
    }

    fun startTransactionActivityForEmi(eDashBoardItem: EDashboardItem, amt:String, mobileNum:String="", billNum:String="", imeiOrSerialNum:String="", brandEmiSubCatData: BrandEMISubCategoryTable?=null, brandEmiCat: BrandEMISubCategoryTable?=null,
                                       brandEmiProductData: BrandEMIProductDataModal?=null,
                                       brandDataMaster: BrandEMIMasterDataModal?=null, testEmiTxnType: String=""){
        val intent = Intent (this, TransactionActivity::class.java)
        intent.putExtra("mobileNumber", mobileNum)
        intent.putExtra("billNumber", billNum)
        intent.putExtra("saleAmt", amt)
        intent.putExtra("imeiOrSerialNum", imeiOrSerialNum)
        intent.putExtra("brandEmiSubCatData", brandEmiSubCatData)
        intent.putExtra("brandEmiCat", brandEmiCat)
        intent.putExtra("brandEmiProductData", brandEmiProductData)
        intent.putExtra("brandDataMaster", brandDataMaster)
        intent.putExtra("edashboardItem", eDashBoardItem)
        var txnType= BhTransactionType.NONE.type
        if(eDashBoardItem== EDashboardItem.BRAND_EMI){
            txnType=   BhTransactionType.BRAND_EMI.type
        }
        if(eDashBoardItem== EDashboardItem.BANK_EMI){
            txnType=   BhTransactionType.EMI_SALE.type
        }
        // test emi
        if(eDashBoardItem== EDashboardItem.TEST_EMI){
            txnType=   BhTransactionType.TEST_EMI.type
        }
        intent.putExtra("TestEmiOption", testEmiTxnType)

        intent.putExtra("type", txnType)
        startActivity(intent)

    }
    fun startTenure(){
        val intent = Intent (this, TenureSchemeActivity::class.java)
        startActivity(intent)
    }

    override  fun onDashBoardItemClick(action: EDashboardItem) {

        isDashboardOpen = false

        when (action) {
            EDashboardItem.SALE, EDashboardItem.BANK_EMI, EDashboardItem.SALE_WITH_CASH, EDashboardItem.CASH_ADVANCE, EDashboardItem.PREAUTH, EDashboardItem.REFUND -> {
                if (checkInternetConnection()) {
                    /*CoroutineScope(Dispatchers.Default).launch {
                        startActivityForResult(Intent(this@NavigationActivity, TransactionActivity::class.java).apply {
                            //  putExtra("amt", amt)
                            //  putExtra("type", transType)
                        },1000)

                        //inflateInputFragment(PreAuthCompleteInputDetailFragment(), SubHeaderTitle.SALE_SUBHEADER_VALUE.title, EDashboardItem.PREAUTH_COMPLETE)
                    }*/
                    CoroutineScope(Dispatchers.Main).launch {
                        inflateInputFragment(NewInputAmountFragment(), SubHeaderTitle.SALE_SUBHEADER_VALUE.title,action)
                    }

                } else {
                    ToastUtils.showToast(this,R.string.no_internet_available_please_check_your_internet)
                }


            }
            EDashboardItem.PREAUTH_COMPLETE->{
                if (checkInternetConnection()) {
                    transactFragment(PreAuthCompleteInputDetailFragment(), true)
                }else{
                    ToastUtils.showToast(this,R.string.no_internet_available_please_check_your_internet)
                }

              /*  lifecycleScope.launch(Dispatchers.IO) {
                    appDao.deletePendingSyncTransactionTable()
                }*/


            }
            EDashboardItem.EMI_ENQUIRY -> {
                if (Field48ResponseTimestamp.checkInternetConnection()) {
                    CoroutineScope(Dispatchers.IO).launch{
                        val listofTids = withContext(Dispatchers.IO) { checkBaseTid(appDao) }
                        println("TID LIST --->  $listofTids")
                        val resultTwo = withContext(Dispatchers.IO) {  doInitializtion(appDao,listofTids,this@NavigationActivity) }
                        println("RESULT TWO --->  $resultTwo")
                    }
                    transactFragment(EMICatalogue().apply {
                        arguments = Bundle().apply {
                            putSerializable("type", EDashboardItem.EMI_CATALOGUE)
                            //  putString(INPUT_SUB_HEADING, "")
                        }
                    })

                } else {
                    ToastUtils.showToast(this,getString(R.string.no_internet_available_please_check_your_internet))
                }
            }
            EDashboardItem.BRAND_EMI->{
                if (Field48ResponseTimestamp.checkInternetConnection()) {
                    CoroutineScope(Dispatchers.IO).launch{
                        val listofTids = withContext(Dispatchers.IO) { checkBaseTid(appDao) }
                        println("TID LIST --->  $listofTids")
                        val resultTwo = withContext(Dispatchers.IO) {  doInitializtion(appDao,listofTids,this@NavigationActivity) }
                        println("RESULT TWO --->  $resultTwo")
                    }
                    transactFragment(BrandEmiMasterCategoryFragment().apply {
                        arguments = Bundle().apply {
                            putSerializable("type", action)
                            putString(
                                INPUT_SUB_HEADING,
                                SubHeaderTitle.Brand_EMI_Master_Category.title
                            )
                        }
                    }, true)

                }else{
                    ToastUtils.showToast(this,getString(R.string.no_internet_available_please_check_your_internet))
                }
            }
            EDashboardItem.VOID_SALE->{
                CoroutineScope(Dispatchers.IO).launch{
                    val listofTids = withContext(Dispatchers.IO) { checkBaseTid(appDao) }
                    println("TID LIST --->  $listofTids")
                    val resultTwo = withContext(Dispatchers.IO) {  doInitializtion(appDao,listofTids,this@NavigationActivity) }
                    println("RESULT TWO --->  $resultTwo")
                }
                transactFragment(VoidMainFragment())
                // todo uncomment below
                /*  lifecycleScope.launch(Dispatchers.IO) {
                      //    appDao.insertBatchData(batchData)
                      val dd=    DBModule.appDatabase.appDao.getBatchData()
                      println(dd.toString())
                  }
                  transactFragment(VoidMainFragment())*/
                val cardDataTable = DBModule.appDatabase.appDao.getCardDataByPanNumber("53")

                //  val cardDataTable = CardDataTable.selectFromCardDataTable(cardProcessedData.getTrack2Data()!!)
                val cdtIndex = cardDataTable?.cardTableIndex ?: ""
                /*     val accSellection =
                         addPad(
                             AppPreference.getString(AppPreference.ACC_SEL_KEY),
                             "0",
                             2
                         )*/

            }
            EDashboardItem.PRE_AUTH_CATAGORY -> {

                if (!action.childList.isNullOrEmpty()) {
                    // dashBoardCatagoryDialog(action.childList!!)
                    if (checkInternetConnection()) {
                        CoroutineScope(Dispatchers.IO).launch{
                            val listofTids = withContext(Dispatchers.IO) { checkBaseTid(appDao) }
                            println("TID LIST --->  $listofTids")
                            val resultTwo = withContext(Dispatchers.IO) {  doInitializtion(appDao,listofTids,this@NavigationActivity) }
                            println("RESULT TWO --->  $resultTwo")
                        }
                        (transactFragment(
                            PreAuthFragment()
                                .apply {
                                    arguments = Bundle().apply {
                                        putSerializable(
                                            "preAuthOptionList",
                                            (action.childList) as java.util.ArrayList
                                        )
                                        putSerializable("type", EDashboardItem.PRE_AUTH_CATAGORY)
                                    }
                                }))
                    } else {
                        ToastUtils.showToast(this,getString(R.string.no_internet_available_please_check_your_internet))
                    }
                } else {
                    showToast("PreAuth Not Found")
                    return
                }


            }

            EDashboardItem.PREAUTH_VIEW ->{
                CoroutineScope(Dispatchers.IO).launch{
                    val listofTids = withContext(Dispatchers.IO) { checkBaseTid(appDao) }
                    println("TID LIST --->  $listofTids")
                    val resultTwo = withContext(Dispatchers.IO) {  doInitializtion(appDao,listofTids,this@NavigationActivity) }
                    println("RESULT TWO --->  $resultTwo")
                }
                DeviceHelper.doPreAuthViewTxn(object: OnOperationListener.Stub(){
                    override fun onCompleted(p0: OperationResult?) {
                        p0?.value?.apply {
                            println("Status = $status")
                            println("Response code = $responseCode")
                        }
                    }
                })
            }
            EDashboardItem.VOID_PREAUTH->{
              /*  lifecycleScope.launch(Dispatchers.IO) {
                    appDao.deletePendingSyncTransactionTable()
                }*/
            }
            EDashboardItem.PENDING_PREAUTH ->{

                transactFragment(PreAuthPendingFragment(),true)
            }
            EDashboardItem.MERCHANT_REFERRAL->{
                /*   transactFragment(BrandEmiMasterCategoryFragment().apply {
                       arguments = Bundle().apply {
                           putSerializable("type", action)
                           putString(
                               INPUT_SUB_HEADING,
                               SubHeaderTitle.Brand_EMI_Master_Category.title
                           )
                       }
                   })*/

            }
            EDashboardItem.DIGI_POS -> {
                /* if (!AppPreference.getBoolean(PrefConstant.BLOCK_MENU_OPTIONS.keyName.toString()) &&
             !AppPreference.getBoolean(PrefConstant.INSERT_PPK_DPK.keyName.toString()) &&
             !AppPreference.getBoolean(PrefConstant.INIT_AFTER_SETTLEMENT.keyName.toString())
         ) {*/
                if (checkInternetConnection()) {
                    transactFragment(DigiPosMenuFragment().apply {
                        //   DigiPosDataTable.clear()
                        CoroutineScope(Dispatchers.IO).launch{
                            val listofTids = withContext(Dispatchers.IO) { checkBaseTid(appDao) }
                            println("TID LIST --->  $listofTids")
                            val resultTwo = withContext(Dispatchers.IO) {  doInitializtion(appDao,listofTids,this@NavigationActivity) }
                            println("RESULT TWO --->  $resultTwo")
                        }
                        val dp = selectAllDigiPosData()
                        val dpObj = Gson().toJson(dp)
                        logger("UPDATEDIGI", dpObj, "e")

                        arguments = Bundle().apply {
                            putSerializable("type", EDashboardItem.DIGI_POS)
                            // putString(INPUT_SUB_HEADING, "")
                        }
                    }, false)

                } else {
                    showToast(getString(R.string.no_internet_available_please_check_your_internet))
                }
                /*} else {
            checkAndPerformOperation()
        }*/

            }

            EDashboardItem.EMI_PRO->{
                if (checkInternetConnection()) {
                    transactFragment(BrandEmiByCodeFragment(), true)
                }else{
                    ToastUtils.showToast(this,R.string.no_internet_available_please_check_your_internet)
                }
            }
            else->{


            }

        }
    }
    // endregion
//Below Method is to Handle the Input Fragment Inflate with the Sub Heading it belongs to:-
    fun inflateInputFragment(fragment: Fragment, subHeading: String, action: EDashboardItem, testEmiOption: String = "0") {

        System.out.println("Insert Block option "+AppPreference.getBoolean(PrefConstant.BLOCK_MENU_OPTIONS.keyName.toString()))
        System.out.println("Insert PPk dpk "+AppPreference.getBoolean(PrefConstant.INSERT_PPK_DPK.keyName.toString()))
        System.out.println("Init after settlement "+AppPreference.getBoolean(PrefConstant.INIT_AFTER_SETTLEMENT.keyName.toString()))

        if (!AppPreference.getBoolean(PrefConstant.BLOCK_MENU_OPTIONS.keyName.toString()) &&
            !AppPreference.getBoolean(PrefConstant.BLOCK_MENU_OPTIONS_INGENICO.keyName.toString()) &&
            !AppPreference.getBoolean(PrefConstant.INSERT_PPK_DPK.keyName.toString()) &&
            !AppPreference.getBoolean(PrefConstant.INIT_AFTER_SETTLEMENT.keyName.toString())) {
            transactFragment(fragment.apply {
                arguments = Bundle().apply {
                    putSerializable("type", action)
                    putString(INPUT_SUB_HEADING, subHeading)
                    putString("TestEmiOption", testEmiOption)
                }
            }, false)
        } else {
            if (checkInternetConnection())
                checkAndPerformOperation()
            else  ToastUtils.showToast(this,getString(R.string.no_internet_available_please_check_your_internet))
        }
    }

    //Below method is used to check which action to perform on click of any module in app whether Force Settlement  , Init or Logon:-
    private fun checkAndPerformOperation() {
        if (checkInternetConnection()) {
            if (AppPreference.getBoolean(PrefConstant.BLOCK_MENU_OPTIONS.keyName.toString())) {
                alertBoxWithAction(
                    getString(R.string.batch_settle),
                    getString(R.string.please_settle_batch),
                    false, getString(R.string.positive_button_ok),
                    {
                        //autoSettleBatchData()
                    },
                    {})

            }
            else if (AppPreference.getBoolean(PrefConstant.INSERT_PPK_DPK.keyName.toString())) {
                CoroutineScope(Dispatchers.Main).launch{
                    val tid = getBaseTID(appDao)
                    showProgress()
                    initViewModel.insertInfo1(tid)

                }

            }
            else if (AppPreference.getBoolean(PrefConstant.INIT_AFTER_SETTLEMENT.keyName.toString())) {
                CoroutineScope(Dispatchers.Main).launch{
                    val tid = getBaseTID(appDao)
                    showProgress()
                    initViewModel.insertInfo1(tid)
                    //  observeMainViewModel()
                }
            }
            else {
                // VFService.showToast(getString(R.string.something_went_wrong))
            }

        } else {
            // VFService.showToast(getString(R.string.no_internet_available_please_check_your_internet))
        }
    }




    private fun observeMainViewModel(){

        initViewModel.initData.observe(this@NavigationActivity, Observer { result ->

            when (result.status) {
                Status.SUCCESS -> {

                    var isStaticQrAvailable=false

                    CoroutineScope(Dispatchers.IO).launch{
                        Utility().readInitServer(result?.data?.data as java.util.ArrayList<ByteArray>) { result, message ->

                            lifecycleScope.launch(Dispatchers.IO){

                                //--region
                                KeyExchanger.getDigiPosStatus(
                                    EnumDigiPosProcess.InitializeDigiPOS.code,
                                    EnumDigiPosProcessingCode.DIGIPOSPROCODE.code, false
                                ) { isSuccess, responseMsg, responsef57, fullResponse ->
                                    try {
                                        if (isSuccess) {
                                            //1^Success^Success^S101^Active^Active^Active^Active^0^1
                                            val responsF57List = responsef57.split("^")
                                            Log.e("F56->>", responsef57)
                                            //  if (responsF57List[4] == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode) {
                                            val tpt1 = getTptData()
                                            tpt1?.digiPosResponseType = responsF57List[0].toString()
                                            tpt1?.digiPosStatus = responsF57List[1].toString()
                                            tpt1?.digiPosStatusMessage =
                                                responsF57List[2].toString()
                                            tpt1?.digiPosStatusCode = responsF57List[3].toString()
                                            tpt1?.digiPosTerminalStatus  = responsF57List[4].toString()
                                            tpt1?.digiPosBQRStatus = responsF57List[5].toString()
                                            tpt1?.digiPosUPIStatus =  responsF57List[6].toString()
                                            tpt1?.digiPosSMSpayStatus = responsF57List[7].toString()
                                            tpt1?.digiPosStaticQrDownloadRequired =
                                                responsF57List[8].toString()
                                            tpt1?.digiPosCardCallBackRequired =
                                                responsF57List[9].toString()

                                            if ((tpt1?.digiPosTerminalStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode) && (tpt1?.digiPosUPIStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode
                                                        || tpt1.digiPosBQRStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode
                                                        || tpt1.digiPosSMSpayStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode) ){
                                                tpt1.isDigiposActive = "1"
                                            }
                                            else{
                                                tpt1?.isDigiposActive = "0"
                                            }

                                            if (tpt1 != null) {
                                                performOperation(tpt1) {
                                                    logger(
                                                        LOG_TAG.DIGIPOS.tag,
                                                        "Terminal parameter Table updated successfully $tpt1 "
                                                    )
                                                    //val ttp = TerminalParameterTable.selectFromSchemeTable()
                                                    val ttp = getTptData()
                                                    val tptObj = Gson().toJson(ttp)
                                                    logger(
                                                        LOG_TAG.DIGIPOS.tag,
                                                        "After success      $tptObj "
                                                    )
                                                }
                                                if (tpt1.digiPosBQRStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode) {
                                                    var imgbm: Bitmap? = null
                                                    runBlocking(Dispatchers.IO) {
                                                        val tpt= getTptData()
                                                        imgbm = loadStaticQrFromInternalStorage() // it return null when file not exist
                                                        if(imgbm==null || tpt?.digiPosStaticQrDownloadRequired =="1") {
                                                            isStaticQrAvailable=true
                                                        }
                                                    }

                                                }

                                            }
                                            //  }

                                            /* else {
                                                    logger("DIGI_POS", "DIGI_POS_UNAVAILABLE")
                                                }*/
                                        }else{
                                            //VFService.showToast(responseMsg)
                                        }

                                    } catch (ex: java.lang.Exception) {
                                        ex.printStackTrace()
                                        logger(
                                            LOG_TAG.DIGIPOS.tag,
                                            "Somethig wrong... in response data field 57"
                                        )
                                    }
                                }

                                if(isStaticQrAvailable){
                                    // getting static qr from server if required
                                    //withContext(Dispatchers.IO){
                                    getStaticQrFromServerAndSaveToFile(this@NavigationActivity){
                                        // FAIL AND SUCCESS HANDELED IN FUNCTION getStaticQrFromServerAndSaveToFile itself
                                    }
                                    //}

                                }
                                // end region
                                hideProgress()
                                //Field48ResponseTimestamp.showToast("Navigation")
                                var checkinitstatus = checkInitializationStatus(appDao)
                                if(checkinitstatus) {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        alertBoxMsgWithIconOnly(
                                            R.drawable.ic_tick,
                                            this@NavigationActivity.getString(R.string.successfull_init)
                                        )
                                    }
                                }
                                else{
                                    transactFragment(DashboardFragment())
                                }
                            }


                        }

                    }
                }
                Status.ERROR -> {
                    hideProgress()
                    CoroutineScope(Dispatchers.Main).launch {
                        getInfoDialog("Error", result.error ?: "") {}
                    }
                    //  ToastUtils.showToast(this@NavigationActivity,"Error called  ${result.error}")
                }
                Status.LOADING -> {
                    showProgress(getString(R.string.sending_receiving_host))

                }
            }

        })


    }
    //endregion


    //Settle Batch and Do the Init:-
    suspend fun settleBatch(settlementByteArray: ByteArray?, settlementFrom: String? = null, settlementCB: ((Boolean) -> Unit)? = null) {
        runOnUiThread {
            showProgress()
        }
        if (settlementByteArray != null) {
            HitServer.hitServer(settlementByteArray, { result, success ->
                if (success && !TextUtils.isEmpty(result)) {
                    hideProgress()
                    tempSettlementByteArray = settlementByteArray
                    /* Note:- If responseCode is "00" then delete Batch File Data Table happens and Navigate to MainActivity
                              else responseCode is "95" then Batch Upload will Happens and then delete Batch File Data Table happens
                              and Navigate to MainActivity */
                    val responseIsoData: IsoDataReader = readIso(result, false)
                    logger("Transaction RESPONSE ", "---", "e")
                    logger("Transaction RESPONSE --->>", responseIsoData.isoMap, "e")
                    Log.e("Success 39-->  ", responseIsoData.isoMap[39]?.parseRaw2String()
                        .toString() + "---->" + responseIsoData.isoMap[58]?.parseRaw2String().toString()
                    )
                    val responseCode = responseIsoData.isoMap[39]?.parseRaw2String().toString()
                    val hostFailureValidationMsg =
                        responseIsoData.isoMap[58]?.parseRaw2String().toString()
                    /* Note:- If responseCode is "00" then delete Batch File Data Table happens and Navigate to MainActivity
                             else responseCode is "95" then Batch Upload will Happens and then delete Batch File Data Table happens
                             and Navigate to MainActivity */

                    if (responseCode == "00") {
                        //  settlementServerHitCount = 0
                        AppPreference.saveBoolean(PrefConstant.SERVER_HIT_STATUS.keyName.toString(), false)
                        AppPreference.saveBoolean(PrefConstant.BLOCK_MENU_OPTIONS.keyName.toString(), false)
                        val terminalParameterTable = getTptData()
                        val isAppUpdateAvailableData = responseIsoData.isoMap[63]?.parseRaw2String()
                        Log.d("Success Data:- ", result)
                        Log.d("isAppUpdate:- ", isAppUpdateAvailableData.toString())
                        //Below we are placing values in preference for the use to know whether batch is settled or not:-
                        AppPreference.saveString(PrefConstant.SETTLEMENT_PROCESSING_CODE.keyName.toString(), ProcessingCode.SETTLEMENT.code)
                        AppPreference.saveBoolean(PrefConstant.SETTLE_BATCH_SUCCESS.keyName.toString(), false)
                        Log.d("Success Data:- ", result)
                        //Below we are placing values in preference for the use to know whether batch is settled or not:-
                        AppPreference.saveString(
                            PrefConstant.SETTLEMENT_PROCESSING_CODE.keyName.toString(),
                            ProcessingCode.SETTLEMENT.code
                        )
                        AppPreference.saveBoolean(
                            PrefConstant.SETTLE_BATCH_SUCCESS.keyName.toString(),
                            false
                        )
                        val batchList = runBlocking(Dispatchers.IO) {
                            appDao.getAllBatchData()
                        }

                        //To increase Roc and Batch number
                        Utility().incrementUpdateRoc()
                        Utility().incrementBatchNumber()
                        //Batch and Roc Increment for Settlement:-
                        val settlement_roc = AppPreference.getIntData(PrefConstant.SETTLEMENT_ROC_INCREMENT.keyName.toString()) + 1
                        AppPreference.setIntData(PrefConstant.SETTLEMENT_ROC_INCREMENT.keyName.toString(), settlement_roc)
                        //region Setting AutoSettle Status and Last Settlement DateTime:-

                        //region Setting AutoSettle Status and Last Settlement DateTime:-
                        when (settlementFrom) {
                            SettlementComingFrom.DASHBOARD.screenType -> {

                                AppPreference.saveBoolean(PreferenceKeyConstant.IsAutoSettleDone.keyName, true)
                                AppPreference.saveString(
                                    PreferenceKeyConstant.LAST_SAVED_AUTO_SETTLE_DATE.keyName,
                                    getSystemTimeIn24Hour().terminalDate()
                                )

                            }
                            else -> {
                                //AppPreference.saveBoolean(AppPreference.IsAutoSettleDone, false)
                            }
                        }

                        //Here printing will be there
                        PrintUtil(this).printSettlementReportupdate(this, batchList, true) {
                            if (it) {

                                //region Saving Batch Data For Last Summary Report and Update Required Values in DB:-
                                runBlocking(Dispatchers.IO) {
                                    AppPreference.saveBatchInPreference(batchList as MutableList<BatchTable?>)
                                    //Delete All BatchFile Data from Table after Settlement:-
                                    //appDao.deleteBatchTable() // old
                                    appDao.deleteTempBatchFileDataTable()

                                    // clear reversal table and preference kushal
                                    appDao.deleteBatchReversalTable()
                                    AppPreference.clearReversal()
                                    AppPreference.saveLastCancelReceiptDetails("")

                                    appDao.deleteDigiPosDataTable()
                                }
                                //endregion

                                //Added by Lucky Singh.
                                //Delete Last Success Receipt From App Preference.
                                AppPreference.saveString(AppPreference.LAST_SUCCESS_RECEIPT_KEY, "")

                                GlobalScope.launch(Dispatchers.Main) {
                                    alertBoxMsgWithIconOnly(R.drawable.ic_tick,getString(R.string.settlement_success))

                                    delay(2000)
                                    if (!TextUtils.isEmpty(isAppUpdateAvailableData) && isAppUpdateAvailableData != "00" && isAppUpdateAvailableData != "01") {
                                        val dataList = isAppUpdateAvailableData?.split("|") as MutableList<String>
                                        if (dataList.size > 1) {
                                            onBackPressed()
                                            writeAppRevisionIDInFile(this@NavigationActivity)
                                            when (dataList[0]) {
                                                AppUpdate.MANDATORY_APP_UPDATE.updateCode -> {
                                                    if (terminalParameterTable?.reservedValues?.length == 20 && terminalParameterTable.reservedValues.endsWith("1"))
                                                    //  startFTPAppUpdate(dataList[2], dataList[3].toInt(), dataList[4], dataList[5], dataList[7], dataList[8])
                                                    else if (terminalParameterTable?.reservedValues?.length == 20 && terminalParameterTable.reservedValues.endsWith("3"))
                                                    //  startHTTPSAppUpdate1(dataList[2],dataList[3].toInt(), dataList[7], dataList[8]) //------------>HTTPS App Update not in use currently
                                                        startHTTPSAppUpdate(dataList[2],dataList[3].toInt(), dataList[7], dataList[8]) //------------>HTTPS App Update not in use currently
                                                }
                                                AppUpdate.OPTIONAL_APP_UPDATE.updateCode -> {
                                                    alertBoxWithAction(getString(R.string.app_update), getString(R.string.app_update_available_do_you_want_to_update), true, getString(R.string.yes), {
                                                        if (terminalParameterTable?.reservedValues?.length == 20 && terminalParameterTable.reservedValues.endsWith("1"))
                                                        //   startFTPAppUpdate(dataList[2], dataList[3].toInt(), dataList[4], dataList[5], dataList[7], dataList[8])
                                                        else if (terminalParameterTable?.reservedValues?.length == 20 && terminalParameterTable.reservedValues.endsWith("3"))
                                                            startHTTPSAppUpdate(dataList[2],dataList[3].toInt(), dataList[7], dataList[8]) //------------>HTTPS App Update not in use currently
                                                    },
                                                        {})
                                                }
                                                else -> {
                                                    onBackPressed()
                                                }
                                            }
                                        } else {
                                            runOnUiThread {
                                                ToastUtils.showToast(this@NavigationActivity,getString(R.string.something_went_wrong_in_app_update))
                                            }

                                        }
                                    } else {
                                        onBackPressed()
                                        when (isAppUpdateAvailableData) {
                                            "00" -> {
                                                if (terminalParameterTable != null) {
                                                    val tid = getBaseTID(appDao)
                                                    showProgress()
                                                    initViewModel.insertInfo1(tid)
                                                    //  observeMainViewModel()
                                                }
                                            }

                                            "01" -> {
                                                if (terminalParameterTable != null) {
                                                    val tid = getBaseTID(appDao)
                                                    showProgress()
                                                    initViewModel.insertInfo1(tid)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                GlobalScope.launch(Dispatchers.Main) {
                                    hideProgress()
                                    //region Saving Batch Data For Last Summary Report and Update Required Values in DB:-
                                    runBlocking(Dispatchers.IO) {
                                        AppPreference.saveBatchInPreference(batchList as MutableList<BatchTable?>)
                                        //Delete All BatchFile Data from Table after Settlement:-
                                        //appDao.deleteBatchTable() // old
                                        appDao.deleteTempBatchFileDataTable()
                                        appDao.deleteDigiPosDataTable()
                                    }
                                    //endregion

                                    //Added by Lucky Singh.
                                    //Delete Last Success Receipt From App Preference.
                                    AppPreference.saveString(
                                        AppPreference.LAST_SUCCESS_RECEIPT_KEY,
                                        ""
                                    )


                                    GlobalScope.launch(Dispatchers.Main) {
                                        alertBoxMsgWithIconOnly(R.drawable.ic_tick,getString(R.string.settlement_success))

                                        delay(2000)
                                        if (!TextUtils.isEmpty(isAppUpdateAvailableData) && isAppUpdateAvailableData != "00" && isAppUpdateAvailableData != "01") {
                                            val dataList = isAppUpdateAvailableData?.split("|") as MutableList<String>
                                            if (dataList.size > 1) {
                                                onBackPressed()
                                                writeAppRevisionIDInFile(this@NavigationActivity)
                                                when (dataList[0]) {
                                                    AppUpdate.MANDATORY_APP_UPDATE.updateCode -> {
                                                        if (terminalParameterTable?.reservedValues?.length == 20 && terminalParameterTable.reservedValues.endsWith("1"))
                                                        // startFTPAppUpdate(dataList[2], dataList[3].toInt(), dataList[4], dataList[5], dataList[7], dataList[8])
                                                        else if (terminalParameterTable?.reservedValues?.length == 20 && terminalParameterTable.reservedValues.endsWith("3"))
                                                            startHTTPSAppUpdate(dataList[2],dataList[3].toInt(), dataList[7], dataList[8]) //------------>HTTPS App Update not in use currently
                                                    }
                                                    AppUpdate.OPTIONAL_APP_UPDATE.updateCode -> {
                                                        alertBoxWithAction(getString(R.string.app_update), getString(R.string.app_update_available_do_you_want_to_update), true, getString(R.string.yes), {
                                                            if (terminalParameterTable?.reservedValues?.length == 20 && terminalParameterTable.reservedValues.endsWith("1"))
                                                            //  startFTPAppUpdate(dataList[2], dataList[3].toInt(), dataList[4], dataList[5], dataList[7], dataList[8])
                                                            else if (terminalParameterTable?.reservedValues?.length == 20 && terminalParameterTable.reservedValues.endsWith("3"))
                                                                startHTTPSAppUpdate(dataList[2],dataList[3].toInt(), dataList[7], dataList[8])  //------------>HTTPS App Update not in use currently
                                                        },
                                                            {})
                                                    }
                                                    else -> {
                                                        onBackPressed()
                                                    }
                                                }
                                            } else {
                                                // VFService.showToast(getString(R.string.something_went_wrong_in_app_update))
                                                onBackPressed()
                                            }

                                        } else {
                                            onBackPressed()
                                            when (isAppUpdateAvailableData) {
                                                "00" -> {
                                                    if (terminalParameterTable != null) {
                                                        val tid = getBaseTID(appDao)
                                                        showProgress()
                                                        initViewModel.insertInfo1(tid)
                                                        // observeMainViewModel()
                                                    }
                                                }

                                                "01" -> {
                                                    if (terminalParameterTable != null) {
                                                        val tid = getBaseTID(appDao)
                                                        showProgress()
                                                        initViewModel.insertInfo1(tid)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                runOnUiThread {
                                    ToastUtils.showToast(this@NavigationActivity,"Printing error")
                                }

                            }
                        }
                    } else {
                        runOnUiThread {
                            ToastUtils.showToast(this@NavigationActivity,hostFailureValidationMsg)
                            AppPreference.saveBoolean(
                                PrefConstant.BLOCK_MENU_OPTIONS.keyName.toString(), true)
                        }
                        when (settlementFrom) {
                            SettlementComingFrom.DASHBOARD.screenType -> {
                                AppPreference.saveBoolean(PreferenceKeyConstant.IsAutoSettleDone.keyName, true)
                                AppPreference.saveString(
                                    PreferenceKeyConstant.LAST_SAVED_AUTO_SETTLE_DATE.keyName,
                                    getSystemTimeIn24Hour().terminalDate()
                                )
                            }
                            else -> {
                                //AppPreference.saveBoolean(AppPreference.IsAutoSettleDone, false)
                            }
                        }

                        settlementCB?.invoke(false)
                    }

                } else {
                    hideProgress()
                    runOnUiThread {
                        AppPreference.saveBoolean(PrefConstant.BLOCK_MENU_OPTIONS.keyName.toString(), true)
                    }
                    runOnUiThread {
                        Toast.makeText(this@NavigationActivity,"Settlement Failure",Toast.LENGTH_SHORT).show()

                    }
                    Log.d("Failure Data:- ", result)
                    AppPreference.saveString(PrefConstant.SETTLEMENT_PROCESSING_CODE.keyName.toString(), ProcessingCode.FORCE_SETTLEMENT.code)

                    //Added by Manish Kumar
                    val settlement_roc = AppPreference.getIntData(PrefConstant.SETTLEMENT_ROC_INCREMENT.keyName.toString()) + 1
                    AppPreference.setIntData(PrefConstant.SETTLEMENT_ROC_INCREMENT.keyName.toString(), settlement_roc)

                    AppPreference.saveString(PrefConstant.SETTLEMENT_PROCESSING_CODE.keyName.toString(), ProcessingCode.FORCE_SETTLEMENT.code)

                    AppPreference.saveBoolean(PrefConstant.SETTLE_BATCH_SUCCESS.keyName.toString(), true)

                    when (settlementFrom) {
                        SettlementComingFrom.DASHBOARD.screenType -> {

                            AppPreference.saveBoolean(PreferenceKeyConstant.IsAutoSettleDone.keyName, true)
                            AppPreference.saveString(
                                PreferenceKeyConstant.LAST_SAVED_AUTO_SETTLE_DATE.keyName,
                                getSystemTimeIn24Hour().terminalDate()
                            )

                        }
                        else -> {
                            //AppPreference.saveBoolean(AppPreference.IsAutoSettleDone, false)
                        }
                    }

                    settlementCB?.invoke(false)
                }
            }, {
                //backToCalled(it, false, true)
            })
        }
        else
            hideProgress()
    }

    //Below method is used to update App through HTTP/HTTPs:-

    private fun startHTTPSAppUpdate(appHostDownloadURL: String? = null, ftpIPPort: Int? = null, downloadAppFileName: String, downloadFileSize: String) {
        showPercentDialog(getString(R.string.please_wait_downloading_application_update))
        if (appHostDownloadURL != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val appHostDownloadURL = appHostDownloadURL?.replace("/app", ":"+ftpIPPort)
            //   AppUpdateDownloadManager(this@MainActivity,"https://bonushub.co.in/",
            //https://testapp.bonushub.co.in:8055/app/pos.zip
            AppUpdateDownloadManager(this@NavigationActivity,appHostDownloadURL+"app"+"/"+"APOSA8"+"/"+downloadAppFileName,
                object : OnDownloadCompleteListener {
                    override fun onError(msg: String) {
                        GlobalScope.launch(Dispatchers.Main) {
                            hideProgress()
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            getInfoDialog(
                                msg,
                                "App Update Failed"
                            ) {}
                        }
                    }

                    @SuppressLint("LongLogTag")
                    override fun onDownloadComplete(path: String, appName: String, fileUri: File?) {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        if (!TextUtils.isEmpty(path)) {
                            hideProgress()
                            Log.d("DownloadAppFilePath:- ", path)
                            Log.d("DownloadAppFilePath file uri :- ", fileUri?.toString() ?: "")

                            val downloadedFile = File(fileUri?.path ?: "")

                            if (!TextUtils.isEmpty(fileUri.toString())) {
                                autoInstallApk(fileUri.toString()) { status, packageName, code ->
                                    GlobalScope.launch(Dispatchers.Main) {
                                        // VFService.showToast(getString(R.string.app_updated_successfully))
                                    }
                                }
                            }

                        } else {
                            hideProgress()
                            // VFService.showToast(getString(R.string.something_went_wrong))
                        }
                    }
                }).execute()


            /*  AppUpdateDownloadManager("https://testapp.bonushub.co.in:8055/app/pos.zip",
                       object : OnDownloadCompleteListener {
                           override fun onError(msg: String) {
                               GlobalScope.launch(Dispatchers.Main) {
                                   hideProgress()
                                   getInfoDialog(
                                       getString(R.string.connection_error),
                                       "No update available"
                                   ) {}
                               }
                           }

                           override fun onDownloadComplete(path: String, appName: String) {
                               if (!TextUtils.isEmpty(path)) {
                                   hideProgress()
                                   Log.d("DownloadAppFilePath:- ", path)
                                   autoInstallApk(path) { status, packageName, code ->
                                       GlobalScope.launch(Dispatchers.Main) {
                                           VFService.showToast(getString(R.string.app_updated_successfully))
                                       }
                                   }
                               } else {
                                   hideProgress()
                                   VFService.showToast(getString(R.string.something_went_wrong))
                               }
                           }
                       }).execute()*/

        } else {
            //  VFService.showToast("Download URL Not Found!!!")
        }
    }

    //region=========================Auto Install Apk Execution Code:-
    fun autoInstallApk(filePath: String?, apkInstallCB: (Boolean, String, Int) -> Unit) {
        val pInfo = this@NavigationActivity?.packageManager?.getPackageInfo(this@NavigationActivity.packageName, 0)
        tms = DeviceHelper.getTMS()
        showProgress(getString(R.string.please_wait_aaplication_is_configuring_updates))
        if (tms != null && !TextUtils.isEmpty(filePath)) {
            try {
                val param = Bundle()
                param.putString(TMSData.FILE_PATH, filePath)
                tms?.install(param,listener)
            }
            catch (ex: Exception){
                ex.printStackTrace()
                hideProgress()
            }
        }
        else{
            hideProgress()
            runOnUiThread {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(this@NavigationActivity,"Something went wrong!!!",Toast.LENGTH_SHORT).show()

                }
            }
        }

    }
    //endregion
    private val listener: OnResultListener = object : OnResultListener.Stub() {
        @Throws(RemoteException::class)
        override fun onSuccess() {
            hideProgress()
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(this@NavigationActivity,"Application updated successfully",Toast.LENGTH_SHORT).show()
            }
            println("Application installed succeesfully")
            println("=> onSuccess")
        }

        @Throws(RemoteException::class)
        override fun onError(errorList: List<Bundle>) {
            hideProgress()
            println("=> onError")
            for (item in errorList) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(this@NavigationActivity,item.getString(TMSData.ERROR_MESSAGE),Toast.LENGTH_SHORT).show()
                }
                println(item.getString(TMSData.ERROR_MESSAGE))
            }
        }
    }


    //Auto Settle Batch:- kushal
    /*private fun autoSettleBatchData() {
        // val settlementBatchData = BatchFileDataTable.selectBatchData()
        val settlementBatchData = runBlocking(Dispatchers.IO) {
            appDao.getAllBatchData()
        }

        val dataListReversal = runBlocking(Dispatchers.IO) {
            appDao.getAllBatchReversalData()
        }

        GlobalScope.launch(Dispatchers.IO) {
            hideProgress()
            if(AppPreference.getBoolean(PrefConstant.BLOCK_MENU_OPTIONS_INGENICO.keyName.toString())){
                PrintUtil(this@NavigationActivity).printDetailReportupdate(settlementBatchData, this@NavigationActivity) {
                        detailPrintStatus -> }

                GlobalScope.launch(Dispatchers.Main) {

                    var reversalTid  = checkReversal(dataListReversal)
                    var listofTxnTid =  checkSettlementTid(settlementBatchData)

                    val result: ArrayList<String> = ArrayList()
                    result.addAll(listofTxnTid)

                    for (e in reversalTid) {
                        if (!result.contains(e)) result.add(e)
                    }
                    System.out.println("Total transaction tid is"+result.forEach {
                        println("Tid are "+it)
                    })
                    settlementViewModel.settlementResponse(result)
                }

                GlobalScope.launch(Dispatchers.Main) {

                    settlementViewModel.ingenciosettlement.observe(this@NavigationActivity) { result ->
                        when (result.status) {
                            Status.SUCCESS -> {
                                CoroutineScope(Dispatchers.IO).launch {
                                    AppPreference.saveBoolean(
                                        PrefConstant.BLOCK_MENU_OPTIONS.keyName.toString(),
                                        false
                                    )
                                    AppPreference.saveBoolean(
                                        PrefConstant.BLOCK_MENU_OPTIONS_INGENICO.keyName.toString(),
                                        false
                                    )

                                    val data = CreateSettlementPacket(appDao).createSettlementISOPacket()
                                    var settlementByteArray = data.generateIsoByteRequest()
                                    try {
                                        settleBatch(settlementByteArray) {}
                                    } catch (ex: Exception) {
                                        hideProgress()
                                        ex.printStackTrace()
                                    }
                                }
                                //  Toast.makeText(activity,"Sucess called  ${result.message}", Toast.LENGTH_LONG).show()
                            }
                            Status.ERROR -> {
                                AppPreference.saveBoolean(
                                    PrefConstant.BLOCK_MENU_OPTIONS.keyName.toString(),
                                    true
                                )
                                AppPreference.saveBoolean(
                                    PrefConstant.BLOCK_MENU_OPTIONS_INGENICO.keyName.toString(),
                                    true
                                )
                                // Toast.makeText(activity,"Error called  ${result.error}", Toast.LENGTH_LONG).show()
                            }
                            Status.LOADING -> {
                                // Toast.makeText(activity,"Loading called  ${result.message}", Toast.LENGTH_LONG).show()


                            }
                        }
                    }

                }

            }
            else {
                PrintUtil(this@NavigationActivity).printDetailReportupdate(settlementBatchData, this@NavigationActivity) { detailPrintStatus ->
                    if (detailPrintStatus) {

                        // this code below converted
                        val settlementPacket = CreateSettlementPacket(appDao).createSettlementISOPacket()

                        val isoByteArray = settlementPacket.generateIsoByteRequest()
                        GlobalScope.launch(Dispatchers.IO) {
                            settleBatch(isoByteArray)
                        }
                    } else
                        alertBoxWithAction(getString(R.string.printing_error),
                            getString(R.string.failed_to_print_settlement_detail_report),
                            false, getString(R.string.positive_button_ok),
                            {}, {})
                }

            }


        }


    }*/
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        currentFocus = hasFocus
        if (!hasFocus) {

            // Method that handles loss of window focus
            collapseNow()
        }
    }
    private fun lockStatusBar(){
        val manager = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val localLayoutParams = WindowManager.LayoutParams()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        localLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR
        }
        else
        {
            localLayoutParams.type =  WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }
        localLayoutParams.gravity = Gravity.TOP
        localLayoutParams.flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or  // this is to enable the notification to recieve touch events
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or  // Draws over status bar
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        localLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        localLayoutParams.height = (50 * resources
            .displayMetrics.scaledDensity).toInt()
        localLayoutParams.format = PixelFormat.TRANSPARENT
        val view = customViewGroup(this)

            manager.addView(view, localLayoutParams)

    }
    open fun collapseNow() {

        // Initialize 'collapseNotificationHandler'
        if (collapseNotificationHandler == null) {
            collapseNotificationHandler = Handler()
        }

        // If window focus has been lost && activity is not in a paused state
        // Its a valid check because showing of notification panel
        // steals the focus from current activity's window, but does not
        // 'pause' the activity
        if (!currentFocus && !isPaused) {

            // Post a Runnable with some delay - currently set to 300 ms
            collapseNotificationHandler!!.postDelayed(object : Runnable {
                @SuppressLint("WrongConstant")
                override fun run() {

                    // Use reflection to trigger a method from 'StatusBarManager'
                    val statusBarService = getSystemService("statusbar")
                    var statusBarManager: Class<*>? = null
                    try {
                        statusBarManager = Class.forName("android.app.StatusBarManager")
                    } catch (e: ClassNotFoundException) {
                        e.printStackTrace()
                    }
                    var collapseStatusBar: Method? = null
                    try {

                        // Prior to API 17, the method to call is 'collapse()'
                        // API 17 onwards, the method to call is `collapsePanels()`
                        collapseStatusBar = if (Build.VERSION.SDK_INT > 16) {
                            statusBarManager!!.getMethod("collapsePanels")
                        } else {
                            statusBarManager!!.getMethod("collapse")
                        }
                    } catch (e: NoSuchMethodException) {
                        e.printStackTrace()
                    }
                    if (collapseStatusBar != null) {
                        collapseStatusBar.setAccessible(true)
                    }
                    try {
                        if (collapseStatusBar != null) {
                            collapseStatusBar.invoke(statusBarService)
                        }
                    } catch (e: IllegalArgumentException) {
                        e.printStackTrace()
                    } catch (e: IllegalAccessException) {
                        e.printStackTrace()
                    } catch (e: InvocationTargetException) {
                        e.printStackTrace()
                    }

                    // Check if the window focus has been returned
                    // If it hasn't been returned, post this Runnable again
                    // Currently, the delay is 100 ms. You can change this
                    // value to suit your needs.
                    if (!currentFocus && !isPaused) {
                        collapseNotificationHandler!!.postDelayed(this, 100L)
                    }
                }
            }, 300L)
        }
    }


    override fun onPause() {
        super.onPause()

        // Activity's been paused
        isPaused = true
    }

    override fun onResume() {
        super.onResume()

        // Activity's been resumed
        isPaused = false
    }

    private fun exitluncher(){
        if (!checkingPakage("com.verifone.adc.launcher")) {
            return;
        }
        if (System.currentTimeMillis() - j > 2000) {
            showToast("Press again to exit the program.")
            j = System.currentTimeMillis();
            return;
        }
        moveToAnotherApp()
    }
    //getting all the application
    // then checking our package is available or not
    private fun checkingPakage(str: String?): Boolean {
        val installedPackages: List<PackageInfo> = this.packageManager.getInstalledPackages(0)
        for (i in installedPackages.indices) {
            if (installedPackages[i].packageName.equals(str, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun moveToAnotherApp() {
        try {
            val hashMap = HashMap<Any, Any>()
            customIntent("com.verifone.adc.launcher", "com.verifone.adc.launcher.MainActivity")
        } catch (e2: java.lang.Exception) {
            e2.printStackTrace()
        }
    }
    //// this is for moving another application
    private fun customIntent(str: String?, str2: String?) {
        val intent = Intent()
        intent.setClassName(str!!, str2!!)
        startActivity(intent)
    }
    private fun wifiHandaling() {
        WifiPrefManager(this).saveFreshApps("false")
        val wifiManager = this.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val wifiEnabled = wifiManager.isWifiEnabled
        if (wifiEnabled) {
            wifiManager.isWifiEnabled = false
        }
    }

}
//region=============================Interface to implement Dashboard Show More to Show Less Options:-
interface ShowLessOnBackPress {
    fun showLessDashOptions()
}
//endregion


interface IFragmentRequest {
    fun onFragmentRequest(
        action: EDashboardItem,
        data: Any,
        extraPair: Triple<String, String, Boolean>? = Triple("", "", third = true)
    )

    fun onDashBoardItemClick(action: EDashboardItem)


}
