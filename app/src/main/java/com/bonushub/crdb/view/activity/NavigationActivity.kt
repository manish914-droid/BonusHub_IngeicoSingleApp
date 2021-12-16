package com.bonushub.crdb.view.activity

import android.app.Dialog
import android.content.Intent
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
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.ActivityNavigationBinding
import com.bonushub.crdb.databinding.MainDrawerBinding
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.db.AppDatabase
import com.bonushub.crdb.di.DBModule
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.model.local.BatchTable
import com.bonushub.crdb.model.local.BrandEMISubCategoryTable
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.model.remote.BrandEMIProductDataModal
import com.bonushub.crdb.repository.keyexchangeDataSource
import com.bonushub.crdb.serverApi.HitServer
import com.bonushub.crdb.utils.*
import com.bonushub.crdb.utils.Field48ResponseTimestamp.checkInternetConnection
import com.bonushub.crdb.utils.Field48ResponseTimestamp.getTptData
import com.bonushub.crdb.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.utils.dialog.OnClickDialogOkCancel
import com.bonushub.crdb.utils.printerUtils.PrintUtil
import com.bonushub.crdb.view.base.BaseActivityNew
import com.bonushub.crdb.view.fragments.*
import com.bonushub.crdb.view.fragments.pre_auth.PreAuthCompleteFragment
import com.bonushub.crdb.view.fragments.pre_auth.PreAuthFragment
import com.bonushub.crdb.view.fragments.pre_auth.PreAuthPendingFragment
import com.bonushub.crdb.view.fragments.pre_auth.PreAuthVoidFragment
import com.bonushub.crdb.viewmodel.BankFunctionsViewModel
import com.bonushub.crdb.viewmodel.InitViewModel
import com.bonushub.pax.utils.*
import com.google.android.material.navigation.NavigationView
import com.ingenico.hdfcpayment.listener.OnOperationListener
import com.ingenico.hdfcpayment.response.OperationResult
import com.mindorks.example.coroutines.utils.Status
import com.usdk.apiservice.aidl.pinpad.DeviceName
import com.usdk.apiservice.aidl.pinpad.KAPId
import com.usdk.apiservice.aidl.pinpad.UPinpad
import com.usdk.apiservice.limited.pinpad.PinpadLimited
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_navigation.*
import kotlinx.coroutines.*
import java.lang.Runnable
import javax.inject.Inject


@AndroidEntryPoint
class NavigationActivity : BaseActivityNew(), DeviceHelper.ServiceReadyListener,NavigationView.OnNavigationItemSelectedListener,
    ActivityCompat.OnRequestPermissionsResultCallback , IFragmentRequest {
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
    private val dialog by lazy {   Dialog(this) }
    companion object {
        val TAG = NavigationActivity::class.java.simpleName
        const val INPUT_SUB_HEADING = "input_amount"

    }
    @Inject
    lateinit var appDatabase: AppDatabase

    private val bankFunctionsViewModel : BankFunctionsViewModel by viewModels()
    private var dialogAdminPassword:Dialog? = null

    private val initViewModel : InitViewModel by viewModels()
    // for init`

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigationBinding = ActivityNavigationBinding.inflate(layoutInflater)
        setContentView(navigationBinding?.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment?
        DeviceHelper.setServiceListener(this)
        setupNavigationDrawerLayout()

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


        bankFunctionsViewModel.adminPassword.observe(this@NavigationActivity,{

            if(it.value?:false)
            {
                logger("isAdminPassword", (""+it.value?:false) as String)
                dialogAdminPassword?.dismiss()
                closeDrawer()
                transactFragment(BankFunctionsFragment())
            }else{
                Toast.makeText(this@NavigationActivity,R.string.invalid_password,Toast.LENGTH_LONG).show()
            }
        })
    }




    override fun onNavigationItemSelected(item: MenuItem): Boolean {


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
                transactFragment(SettlementFragment())
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
            GlobalScope.launch(Dispatchers.IO) {
                Utility().readLocalInitFile { status, msg ->
                    Log.d("Init File Read Status ", status.toString())
                    Log.d("Message ", msg)
                    //    refreshDrawer()
                }
            }

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
    fun transactSubCatFragment(isBackStackAdded: Boolean = false, brandDataMaster: BrandEMIMasterDataModal?, brandSubCatList: ArrayList<BrandEMISubCategoryTable>?, filteredSubCat: ArrayList<BrandEMISubCategoryTable>?): Boolean {
        val trans = supportFragmentManager.beginTransaction().apply {
            val fragment= BrandEmiSubCategoryFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("brandDataMaster", brandDataMaster)
                    putSerializable("brandSubCatList", brandSubCatList)
                    putSerializable("filteredSubCat", filteredSubCat)

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
        lifecycleScope.launch{
            val tpt = DBModule.appDatabase.appDao?.getAllTerminalParameterTableData()?.get(0)

            var listofTids = ArrayList<String>()
            tpt?.tidType?.forEachIndexed { index, tidType ->
                if(tidType.equals("1")){
                    tpt?.terminalId?.get(index)
                    listofTids.add(0,tpt?.terminalId?.get(index) ?: "")
                }
                else{
                    listofTids.add(tpt?.terminalId?.get(index) ?: "")
                }
            }

            tid = getString(R.string.terminal_id) + "   : " + listofTids[0]
            mid = getString(R.string.merchant_id) + "  : " + tpt?.merchantId

            withContext(Dispatchers.Main){
                mainDrawerBinding?.mdTidTv?.text = tid
                mainDrawerBinding?.mdMidTv?.text = mid
            }
        }


    }

    //region============================On Back Pressed======================
    override fun onBackPressed() {
        if (navHostFragment?.navController?.currentDestination?.label ==
            NavControllerFragmentLabel.DASHBOARD_FRAGMENT_LABEL.destinationLabel || navHostFragment?.navController?.currentDestination?.label == "fragment_init"
        ) {
            Log.d("Dashboard:- ", "Inflated")
            if (isExpanded)
                showLessOnBackPress?.showLessDashOptions()
            else {
                if (navigationBinding?.mainDl?.isDrawerOpen(GravityCompat.START)!!)
                    navigationBinding?.mainDl?.closeDrawer(GravityCompat.START)
                else
                    exitApp()
            }
        }
    }

    override fun onEvents(event: VxEvent) {
        TODO("Not yet implemented")
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

    override fun onFragmentRequest(
        action: EDashboardItem,
        data: Any,
        extraPair: Triple<String, String, Boolean>?
    ) {
        when (action) {
            EDashboardItem.SALE -> {
                if (checkInternetConnection()) {
                    //  val amt = data as String
                    val amt = (data as Pair<*, *>).first.toString()
                    val saleWithTipAmt = data.second.toString()
                    startActivityForResult(
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
                        }, EIntentRequest.TRANSACTION.code
                    )
                } else {
                    ToastUtils.showToast(this,getString(R.string.no_internet_available_please_check_your_internet))
                }
            }

            EDashboardItem.BANK_EMI -> {

            }

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

    fun startTransactionActivity(amt:String,mobileNum:String="",billNum:String="",imeiOrSerialNum:String="",brandEmiSubCatData: BrandEMISubCategoryTable,
                                 brandEmiProductData: BrandEMIProductDataModal,
                                 brandDataMaster: BrandEMIMasterDataModal){
        val intent = Intent (this, TransactionActivity::class.java)
        intent.putExtra("mobileNumber", mobileNum)
        intent.putExtra("billNumber", billNum)
        intent.putExtra("saleAmt", amt)
        intent.putExtra("imeiOrSerialNum", imeiOrSerialNum)
        intent.putExtra("brandEmiSubCatData", brandEmiSubCatData)
        intent.putExtra("brandEmiProductData", brandEmiProductData)
        intent.putExtra("brandDataMaster", brandDataMaster)
        intent.putExtra("edashboardItem", EDashboardItem.BRAND_EMI)
        intent.putExtra("type", BhTransactionType.BRAND_EMI.type)
        startActivity(intent)

    }
    fun startTenure(){
        val intent = Intent (this, TenureSchemeActivity::class.java)

        startActivity(intent)

    }

    override  fun onDashBoardItemClick(action: EDashboardItem) {
        when (action) {
            EDashboardItem.SALE, EDashboardItem.BANK_EMI, EDashboardItem.SALE_WITH_CASH, EDashboardItem.CASH_ADVANCE, EDashboardItem.PREAUTH, EDashboardItem.REFUND -> {
                if (checkInternetConnection()) {
                    CoroutineScope(Dispatchers.IO).launch{
                        val listofTids = withContext(Dispatchers.IO) { checkBaseTid(appDao) }
                        println("TID LIST --->  $listofTids")
                        val resultTwo = withContext(Dispatchers.IO) {  doInitializtion(appDao,listofTids) }
                        println("RESULT TWO --->  $resultTwo")
                    }
                //    inflateInputFragment(PreAuthCompleteInputDetailFragment(), SubHeaderTitle.SALE_SUBHEADER_VALUE.title, EDashboardItem.PREAUTH_COMPLETE)
                    inflateInputFragment(NewInputAmountFragment(), SubHeaderTitle.SALE_SUBHEADER_VALUE.title,action)
                } else {
                    ToastUtils.showToast(this,R.string.no_internet_available_please_check_your_internet)
                }


            }
            EDashboardItem.PREAUTH_COMPLETE->{

                transactFragment(PreAuthCompleteInputDetailFragment(),true)

               /* if (checkInternetConnection()) {
                CoroutineScope(Dispatchers.IO).launch{
                    val listofTids = withContext(Dispatchers.IO) { checkBaseTid(appDao) }
                    println("TID LIST --->  $listofTids")
                    val resultTwo = withContext(Dispatchers.IO) {  doInitializtion(appDao,listofTids) }
                    println("RESULT TWO --->  $resultTwo")
                }
                inflateInputFragment(PreAuthCompleteInputDetailFragment(), SubHeaderTitle.SALE_SUBHEADER_VALUE.title, EDashboardItem.PREAUTH_COMPLETE)
            } else {
                ToastUtils.showToast(this,R.string.no_internet_available_please_check_your_internet)
            }*/

            }
            EDashboardItem.EMI_ENQUIRY -> {
                if (Field48ResponseTimestamp.checkInternetConnection()) {
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
                transactFragment(BrandEmiMasterCategoryFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("type", action)
                        putString(
                            INPUT_SUB_HEADING,
                            SubHeaderTitle.Brand_EMI_Master_Category.title
                        )
                    }
                })


            }
            EDashboardItem.VOID_SALE->{
                /* lifecycleScope.launch(Dispatchers.IO) {
                   //    appDao.insertBatchData(batchData)
               val dd=    DBModule.appDatabase.appDao.getBatchDataFromInvoice("000018")
                   println(dd.toString())
               }*/
                lifecycleScope.launch(Dispatchers.IO) {
                    //    appDao.insertBatchData(batchData)
                    val dd=    DBModule.appDatabase.appDao.getBatchData()
                    println(dd.toString())
                }
                transactFragment(VoidMainFragment())
             //    inflateInputFragment(PreAuthCompleteInputDetailFragment(), SubHeaderTitle.SALE_SUBHEADER_VALUE.title, EDashboardItem.PREAUTH_COMPLETE)


            }

            EDashboardItem.PRE_AUTH_CATAGORY -> {
                if (!action.childList.isNullOrEmpty()) {
                    // dashBoardCatagoryDialog(action.childList!!)
                    if (checkInternetConnection()) {
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

                DeviceHelper.doPreAuthViewTxn(object: OnOperationListener.Stub(){
                    override fun onCompleted(p0: OperationResult?) {
                        p0?.value?.apply {
                            println("Status = $status")
                            println("Response code = $responseCode")
                        }
                    }
                })
            }

            EDashboardItem.PENDING_PREAUTH ->{

                transactFragment(PreAuthPendingFragment(),true)
            }

            EDashboardItem.MERCHANT_REFERRAL->{
                transactFragment(BrandEmiMasterCategoryFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("type", action)
                        putString(
                            INPUT_SUB_HEADING,
                            SubHeaderTitle.Brand_EMI_Master_Category.title
                        )
                    }
                })

            }
            else->{


            }

        }
    }
    // endregion
//Below Method is to Handle the Input Fragment Inflate with the Sub Heading it belongs to:-
    fun inflateInputFragment(
        fragment: Fragment,
        subHeading: String,
        action: EDashboardItem, testEmiOption: String = "0"
    ) {
        if (
            !AppPreference.getBoolean(PrefConstant.INSERT_PPK_DPK.keyName.toString()) &&
            !AppPreference.getBoolean(PrefConstant.INIT_AFTER_SETTLEMENT.keyName.toString())
        ) {
            transactFragment(fragment.apply {
                arguments = Bundle().apply {
                    putSerializable("type", action)
                    putString(INPUT_SUB_HEADING, subHeading)
                    putString("TestEmiOption", testEmiOption)
                }
            }, false)
        } else {
            if (checkInternetConnection())
            ///  checkAndPerformOperation()
            else  ToastUtils.showToast(this,getString(R.string.no_internet_available_please_check_your_internet))
        }
    }
    //region================================================Settlement Server Hit:-
    suspend fun settleBatch(settlementByteArray: ByteArray?,
                            settlementCallFrom: String = SettlementComingFrom.SETTLEMENT.screenType,
                            settlementCB: ((Boolean) -> Unit)? = null) {
        runOnUiThread {
            showProgress()
        }
        if (settlementByteArray != null) {
            HitServer.apply { reversalToBeSaved = null }
                .hitServer(settlementByteArray, { result, success ->
                    if (success && !TextUtils.isEmpty(result)) {
                        hideProgress()
                        tempSettlementByteArray = settlementByteArray
                        val responseIsoData: IsoDataReader = readIso(result, false)
                        logger("Transaction RESPONSE ", "---", "e")
                        logger("Transaction RESPONSE --->>", responseIsoData.isoMap, "e")
                        Log.e("Success 39-->  ", responseIsoData.isoMap[39]?.parseRaw2String().toString() + "---->" + responseIsoData.isoMap[58]?.parseRaw2String().toString())

                        val responseCode = responseIsoData.isoMap[39]?.parseRaw2String().toString()
                        val hostMsg = responseIsoData.isoMap[58]?.parseRaw2String().toString()

                        val terminalParameterTable = getTptData()
                        val isAppUpdateAvailableData = responseIsoData.isoMap[63]?.parseRaw2String()

                        Log.d("Success Data:- ", result)
                        Log.d("isAppUpdate:- ", isAppUpdateAvailableData.toString())
                        //Below we are placing values in preference for the use to know whether batch is settled or not:-

                        if (responseCode == "00" || responseCode == "95") {
                            //Change status of autoSettle File Based Variable after Settlement Success to avoid
                            //regular auto settle check on dashboard:-
                            runBlocking(Dispatchers.IO) {
                                appDao.deleteBatchTable()
                            }

                            GlobalScope.launch(Dispatchers.Main) {
                                txnSuccessToast(
                                    this@NavigationActivity,
                                    getString(R.string.settlement_success)
                                )
                                if (!TextUtils.isEmpty(isAppUpdateAvailableData) && isAppUpdateAvailableData != "00" && isAppUpdateAvailableData != "01") {
                                    val dataList = isAppUpdateAvailableData?.split("|") as MutableList<String>
                                } else {
                                    onBackPressed()
                                    when (isAppUpdateAvailableData) {
                                        "00" -> {
                                            if (terminalParameterTable != null) {
                                                val tid = getBaseTID(appDao)
                                                showProgress()
                                                initViewModel.insertInfo1(tid)
                                                observeMainViewModel()
                                            }
                                        }


                                    }
                                }
                            }

                            when (settlementCallFrom) {
                                SettlementComingFrom.SETTLEMENT.screenType -> AppPreference.saveBoolean(
                                    PreferenceKeyConstant.IsAutoSettleDone.keyName,
                                    false
                                )

                                SettlementComingFrom.DASHBOARD.screenType -> {
                                    AppPreference.saveBoolean(
                                        PreferenceKeyConstant.IsAutoSettleDone.keyName,
                                        true
                                    )

                                }

                            }

                        } else {
                            AppPreference.saveBoolean(
                                PreferenceKeyConstant.SETTLEMENT_FAILED.keyName,
                                true
                            )
                            hideProgress()

                        }
                    } else {
                        hideProgress()
                    }
                }, {
                    //backToCalled(it, false, true)
                })
        }
    }

    private fun observeMainViewModel(){

        initViewModel.initData.observe(this@NavigationActivity, Observer { result ->

            when (result.status) {
                Status.SUCCESS -> {
                    CoroutineScope(Dispatchers.IO).launch{
                        Utility().readInitServer(result?.data?.data as java.util.ArrayList<ByteArray>) { result, message ->
                            hideProgress()
                            CoroutineScope(Dispatchers.Main).launch {
                                alertBoxMsgWithIconOnly(R.drawable.ic_tick,
                                    this@NavigationActivity.getString(R.string.successfull_init))
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
    suspend fun settleBatch1(settlementByteArray: ByteArray?, settlementFrom: String? = null, settlementCB: ((Boolean) -> Unit)? = null) {
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
                                appDao?.getAllBatchData()
                            }

                        //Batch and Roc Increment for Settlement:-
                        val settlement_roc = AppPreference.getIntData(PrefConstant.SETTLEMENT_ROC_INCREMENT.keyName.toString()) + 1
                        AppPreference.setIntData(PrefConstant.SETTLEMENT_ROC_INCREMENT.keyName.toString(), settlement_roc)
                        //region Setting AutoSettle Status and Last Settlement DateTime:-

                            //Here printing will be there
                        PrintUtil(this).printSettlementReportupdate(this, batchList, true) {
                            if (it) {

                                //region Saving Batch Data For Last Summary Report and Update Required Values in DB:-
                                runBlocking(Dispatchers.IO) {
                                    AppPreference.saveBatchInPreference(batchList as MutableList<BatchTable>)
                                    //Delete All BatchFile Data from Table after Settlement:-
                                    appDao.deleteBatchTable()


                                    //  resetRoc()

                                    //Here we are updating invoice by 1
                                    appDao.updateTPTInvoiceNumber("1".padStart(6, '0'), TableType.TERMINAL_PARAMETER_TABLE.code)

                                    //Here we are incrementing sale batch number also for next sale:-
                                    val updatedBatchNumber = getTptData()?.batchNumber?.toInt()?.plus(1)
                                    appDao?.updateBatchNumber(updatedBatchNumber.toString(), TableType.TERMINAL_PARAMETER_TABLE.code)
                                }
                                //endregion

                                //Added by Lucky Singh.
                                //Delete Last Success Receipt From App Preference.
                                AppPreference.saveString(AppPreference.LAST_SUCCESS_RECEIPT_KEY, "")

                                GlobalScope.launch(Dispatchers.Main) {
                                    txnSuccessToast(
                                        this@NavigationActivity,
                                        getString(R.string.settlement_success)
                                    )
                                    delay(2000)
                                    if (!TextUtils.isEmpty(isAppUpdateAvailableData) && isAppUpdateAvailableData != "00" && isAppUpdateAvailableData != "01") {
                                        val dataList = isAppUpdateAvailableData?.split("|") as MutableList<String>

                                    } else {
                                        onBackPressed()
                                        when (isAppUpdateAvailableData) {
                                            "00" -> {
                                                if (terminalParameterTable != null) {
                                                    val tid = getBaseTID(appDao)
                                                    showProgress()
                                                    initViewModel.insertInfo1(tid)
                                                    observeMainViewModel()
                                                }
                                            }

                                            "01" -> {
                                                if (terminalParameterTable != null) {

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
                                        AppPreference.saveBatchInPreference(batchList as MutableList<BatchTable>)
                                        //Delete All BatchFile Data from Table after Settlement:-
                                        appDao.deleteBatchTable()


                                        //  resetRoc()

                                        //Here we are updating invoice by 1
                                        appDao.updateTPTInvoiceNumber(
                                            "1".padStart(6, '0'),
                                            TableType.TERMINAL_PARAMETER_TABLE.code
                                        )

                                        //Here we are incrementing sale batch number also for next sale:-
                                        val updatedBatchNumber =
                                            getTptData()?.batchNumber?.toInt()?.plus(1)
                                        appDao?.updateBatchNumber(
                                            updatedBatchNumber.toString(),
                                            TableType.TERMINAL_PARAMETER_TABLE.code
                                        )
                                    }
                                    //endregion

                                    //Added by Lucky Singh.
                                    //Delete Last Success Receipt From App Preference.
                                    AppPreference.saveString(
                                        AppPreference.LAST_SUCCESS_RECEIPT_KEY,
                                        ""
                                    )


                                    GlobalScope.launch(Dispatchers.Main) {
                                        txnSuccessToast(
                                            this@NavigationActivity,
                                            getString(R.string.settlement_success)
                                        )
                                        delay(2000)
                                        if (!TextUtils.isEmpty(isAppUpdateAvailableData) && isAppUpdateAvailableData != "00" && isAppUpdateAvailableData != "01") {
                                            val dataList =
                                                isAppUpdateAvailableData?.split("|") as MutableList<String>

                                        } else {
                                            onBackPressed()
                                            when (isAppUpdateAvailableData) {
                                                "00" -> {
                                                    if (terminalParameterTable != null) {

                                                    }
                                                }

                                                "01" -> {
                                                    if (terminalParameterTable != null) {

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
                    settlementCB?.invoke(false)
                }
            }, {
                //backToCalled(it, false, true)
            })
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
