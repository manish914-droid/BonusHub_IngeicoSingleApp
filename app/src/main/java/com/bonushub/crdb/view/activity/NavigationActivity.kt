package com.bonushub.crdb

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.RemoteException
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bonushub.crdb.databinding.ActivityNavigationBinding
import com.bonushub.crdb.databinding.MainDrawerBinding
import com.bonushub.crdb.utils.DemoConfig
import com.bonushub.crdb.utils.DeviceHelper
import com.bonushub.pax.utils.Utility
import com.google.android.material.navigation.NavigationView
import com.usdk.apiservice.aidl.pinpad.DeviceName
import com.usdk.apiservice.aidl.pinpad.KAPId
import com.usdk.apiservice.aidl.pinpad.UPinpad
import com.usdk.apiservice.limited.pinpad.PinpadLimited
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NavigationActivity : AppCompatActivity(), DeviceHelper.ServiceReadyListener,NavigationView.OnNavigationItemSelectedListener,
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
    private var pinpad: UPinpad? = null
    private var pinpadLimited: PinpadLimited? = null
    private val dialog by lazy {   Dialog(this) }
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

    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        TODO("Not yet implemented")
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
    open fun transactFragment(fragment: Fragment, isBackStackAdded: Boolean = false): Boolean {
        val trans = supportFragmentManager.beginTransaction().apply {
            replace(R.id.nav_host_fragment, fragment, fragment::class.java.simpleName)
            addToBackStack(fragment::class.java.simpleName)
        }
        if (isBackStackAdded) trans.addToBackStack(null)
        return trans.commitAllowingStateLoss() >= 0
    }
    //endregion

}
//region=============================Interface to implement Dashboard Show More to Show Less Options:-
interface ShowLessOnBackPress {
    fun showLessDashOptions()
}
//endregion

