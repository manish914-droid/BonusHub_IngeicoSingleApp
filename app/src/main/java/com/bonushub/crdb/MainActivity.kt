package com.bonushub.crdb

import android.os.Bundle
import android.os.RemoteException
import androidx.appcompat.app.AppCompatActivity
import com.bonushub.crdb.utils.DeviceHelper
import com.bonushub.crdb.utils.replaceFragmentWithNoHistory
import com.bonushub.crdb.view.MainInfoListFragment
import com.usdk.apiservice.aidl.pinpad.DeviceName
import com.usdk.apiservice.aidl.pinpad.KAPId
import com.usdk.apiservice.aidl.pinpad.UPinpad

import com.bonushub.crdb.HDFCApplication.Companion.appContext

import com.usdk.apiservice.limited.pinpad.PinpadLimited
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(),DeviceHelper.ServiceReadyListener {

    private var deviceserialno: String? = null
    private var devicemodelno: String? = null
    private var pinpad: UPinpad? = null
    private var pinpadLimited: PinpadLimited? = null

    //@Inject
   // lateinit var appDatabase: AppDatabase

  //  @Inject
 //   lateinit var utilitys: Utility

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        DeviceHelper.setServiceListener(this)

       // println("App database is"+appDatabase.appDao)
      //  println("App database will is"+utilitys.doAThing1())
        replaceFragmentWithNoHistory(MainInfoListFragment(), R.id.container_fragment)
    }

    protected fun initDeviceInstance() {
        deviceserialno = DeviceHelper.getSerialno()
        devicemodelno  = DeviceHelper.getDeviceModel()
        pinpad = createPinpad(KAPId(0, 0), 0, DeviceName.IPP)

    }


    fun createPinpad(kapId: KAPId?, keySystem: Int, deviceName: String?): UPinpad? {
        return try {

     //   var  pinpadLimited = PinpadLimited(appContext, kapId, keySystem, deviceName)
            DeviceHelper.getPinpad(kapId, keySystem, deviceName)
        } catch (e: RemoteException) {
            e.printStackTrace()
            null
        }
    }

    override fun onReady(version: String?) {
        register(true)
        initDeviceInstance()
    }

    private fun unregister() {
        try {
            DeviceHelper.unregister()
            // registerEnabled(true)
        } catch (e: IllegalStateException) {
            // toast("unregister fail: " + e.message)
        }
    }


    private fun register(useEpayModule: Boolean) {
        try {
            DeviceHelper.register(useEpayModule)
            //  registerEnabled(false)
        } catch (e: IllegalStateException) {
            //  toast("register fail: " + e.message)
        }
    }
}


