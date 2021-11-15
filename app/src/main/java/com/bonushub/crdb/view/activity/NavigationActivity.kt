package com.bonushub.crdb.view.activity

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.ActivityNavigationBinding
import com.bonushub.crdb.databinding.MainDrawerBinding
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.db.AppDatabase
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.model.local.BrandEMISubCategoryTable
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.utils.DemoConfig
import com.bonushub.crdb.utils.DeviceHelper

import com.bonushub.crdb.utils.Utility
import com.bonushub.crdb.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.utils.dialog.OnClickDialogOkCancel
import com.bonushub.crdb.utils.isExpanded
import com.bonushub.crdb.view.base.BaseActivityNew
import com.bonushub.crdb.view.fragments.BankFunctionsFragment
import com.bonushub.crdb.view.fragments.BrandEmiMasterCategoryFragment
import com.bonushub.crdb.view.fragments.BrandEmiSubCategoryFragment
import com.bonushub.crdb.viewmodel.BankFunctionsViewModel

import com.bonushub.pax.utils.NavControllerFragmentLabel
import com.bonushub.pax.utils.VxEvent

import com.google.android.material.navigation.NavigationView
import com.usdk.apiservice.aidl.pinpad.DeviceName
import com.usdk.apiservice.aidl.pinpad.KAPId
import com.usdk.apiservice.aidl.pinpad.UPinpad
import com.usdk.apiservice.limited.pinpad.PinpadLimited
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.lang.Runnable
import javax.inject.Inject


@AndroidEntryPoint
class NavigationActivity : BaseActivityNew(), DeviceHelper.ServiceReadyListener,NavigationView.OnNavigationItemSelectedListener,
    ActivityCompat.OnRequestPermissionsResultCallback {
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
         //   refreshDrawer()
            navHostFragment?.navController?.popBackStack()
            Log.e("NAV", "DECIDE HOME")
            navHostFragment?.navController?.navigate(R.id.dashBoardFragment)

        } else {
            GlobalScope.launch(Dispatchers.IO) {
                Utility().readLocalInitFile { status, msg ->
                    Log.d("Init File Read Status ", status.toString())
                    Log.d("Message ", msg)
                       // refreshDrawer()
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
        GlobalScope.launch(Dispatchers.IO) {
            Utility().readLocalInitFile { status, msg ->
                Log.d("Init File Read Status ", status.toString())
                Log.d("Message ", msg)
                if (status){

                }
            }
        }
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
         val appDao: AppDao?=null
      runBlocking(Dispatchers.IO) {
        val tpt = appDao?.getAllTerminalParameterTableData()?.get(0)
          val merchantName = tpt?.receiptHeaderOne
           tid = getString(R.string.terminal_id) + "   : " + tpt?.terminalId
           mid = getString(R.string.merchant_id) + "  : " + tpt?.merchantId

      }

    runBlocking(Dispatchers.Main)  {
        mainDrawerBinding?.mdTidTv?.text = tid
        mainDrawerBinding?.mdMidTv?.text = mid
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
                    Toast.makeText(this@NavigationActivity,"Password not match",Toast.LENGTH_LONG).show()
                }
            })
            //transactFragment(BankFunctionsFragment())
        }

        override fun onClickCancel() {

        }

    }
// endregion
}
//region=============================Interface to implement Dashboard Show More to Show Less Options:-
interface ShowLessOnBackPress {
    fun showLessDashOptions()
}
//endregion



