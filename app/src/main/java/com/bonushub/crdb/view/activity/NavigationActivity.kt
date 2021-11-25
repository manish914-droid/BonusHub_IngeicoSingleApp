package com.bonushub.crdb.view.activity

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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
import com.bonushub.crdb.model.local.BrandEMISubCategoryTable
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.model.remote.BrandEMIProductDataModal
import com.bonushub.crdb.utils.*
import com.bonushub.crdb.utils.Field48ResponseTimestamp.checkInternetConnection
import com.bonushub.crdb.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.utils.dialog.OnClickDialogOkCancel
import com.bonushub.crdb.view.base.BaseActivityNew
import com.bonushub.crdb.view.fragments.*
import com.bonushub.crdb.viewmodel.BankFunctionsViewModel

import com.bonushub.pax.utils.*
import com.google.android.material.navigation.NavigationView
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
    private var headerView: View? = null
    private var mainDrawerBinding: MainDrawerBinding? = null
    private var showLessOnBackPress: ShowLessOnBackPress? = null
    private var isoPacketByteArray: ByteArray? = null
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigationBinding = ActivityNavigationBinding.inflate(layoutInflater)
        setContentView(navigationBinding?.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment?
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
    }


    lateinit var bankFunctionsViewModel: BankFunctionsViewModel

    override fun onNavigationItemSelected(item: MenuItem): Boolean {


        //==============kushal ======= implemented drawer menu
        when(item.itemId)
        {
            R.id.bankFunction -> {

                bankFunctionsViewModel = ViewModelProvider(this).get(BankFunctionsViewModel::class.java)

                DialogUtilsNew1.showDialog(this,getString(R.string.admin_password),getString(R.string.hint_enter_admin_password),onClickDialogOkCancel, false)
            }

            R.id.reportFunction -> {

                closeDrawer()
                transactFragment(ReportsFragment())

            }

            R.id.settlement -> {

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
            NavControllerFragmentLabel.DASHBOARD_FRAGMENT_LABEL.destinationLabel
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

            bankFunctionsViewModel.isAdminPassword(password)?.observe(this@NavigationActivity,{

                if(it)
                {
                    dialog.dismiss()
                    closeDrawer()
                    transactFragment(BankFunctionsFragment())
                }else{
                    Toast.makeText(this@NavigationActivity,R.string.invalid_password,Toast.LENGTH_LONG).show()
                }
            })
            //transactFragment(BankFunctionsFragment())
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
                            putExtra("type", TransactionType.SALE.type)
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

            }

            EDashboardItem.SALE_WITH_CASH -> {

            }

            EDashboardItem.PREAUTH -> {

            }

            EDashboardItem.REFUND -> {

            }

            EDashboardItem.EMI_ENQUIRY -> {
            }
            EDashboardItem.BRAND_EMI_CATALOGUE, EDashboardItem.BANK_EMI_CATALOGUE -> {
                val amt = (data as Pair<*, *>).first.toString()
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
        startActivity(intent)

    }
    fun startTenure(){
        val intent = Intent (this, TenureSchemeActivity::class.java)

        startActivity(intent)

    }

    override  fun onDashBoardItemClick(action: EDashboardItem) {
        when (action) {
            EDashboardItem.SALE, EDashboardItem.BANK_EMI, EDashboardItem.SALE_WITH_CASH, EDashboardItem.CASH_ADVANCE, EDashboardItem.PREAUTH -> {
                if (checkInternetConnection()) {
                    CoroutineScope(Dispatchers.IO).launch{
                        val listofTids = withContext(Dispatchers.IO) { checkBaseTid(appDao) }
                        val resultTwo = withContext(Dispatchers.IO) {  doInitializtion(appDao,listofTids) }
                    }

                    inflateInputFragment(NewInputAmountFragment(), SubHeaderTitle.SALE_SUBHEADER_VALUE.title, action)
                } else {
                    ToastUtils.showToast(this,R.string.no_internet_available_please_check_your_internet)
                }


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
                transactFragment(BrandEmiMasterCategoryFragment())

            }
            EDashboardItem.VOID_SALE->{
                transactFragment(BrandEmiMasterCategoryFragment())

            }
            else->{
                val intent = Intent (this, TransactionActivity::class.java)
                startActivity(intent)

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
        if (!AppPreference.getBoolean(PrefConstant.BLOCK_MENU_OPTIONS.keyName.toString()) &&
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
