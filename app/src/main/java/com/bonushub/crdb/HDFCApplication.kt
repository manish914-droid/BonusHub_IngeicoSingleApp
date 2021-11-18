package com.bonushub.crdb

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import com.bonushub.crdb.model.local.AppPreference.initializeEncryptedSharedPreferences

import com.bonushub.crdb.utils.DeviceHelper
import com.usdk.apiservice.aidl.constants.RFDeviceName
import com.usdk.apiservice.aidl.pinpad.DeviceName
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HDFCApplication : Application() {

    companion object {
        @JvmStatic
        lateinit var appContext: Context
        private val TAG = HDFCApplication::class.java.simpleName

        var networkStrength = ""
            private set

        var internetConnection = false
            private set

        var batteryStrength = ""
            private set

        var imeiNo = ""
            private set

        var simNo = ""
            private set

        var operatorName = ""
            private set
    }

    private val mBatteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra("level", 0) ?: 0
            batteryStrength = if (level != 0) level.toString() else ""
        }
    }


    private val mTelephonyManager: TelephonyManager by lazy { getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }


    override fun onCreate() {
        super.onCreate()
        appContext = this
        initDefaultConfig()
        DeviceHelper.bindService()
       DeviceHelper.connect()
        initializeEncryptedSharedPreferences(appContext)
        setNetworkStrength()

      //  registerReceiver(mConnectionReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        registerReceiver(mBatteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun initDefaultConfig() {
        if (Build.MODEL.startsWith("AECR")) {
            //DemoConfig.PINPAD_DEVICE_NAME = DeviceName.COM_EPP
           // DemoConfig.RF_DEVICE_NAME = RFDeviceName.EXTERNAL
        } else {
          //  DemoConfig.PINPAD_DEVICE_NAME = DeviceName.IPP
          //  DemoConfig.RF_DEVICE_NAME = RFDeviceName.INNER
        }
    }

    private fun setNetworkStrength() {
        val nl = object : PhoneStateListener() {
            @RequiresApi(Build.VERSION_CODES.M)
            @SuppressLint("HardwareIds")
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                super.onSignalStrengthsChanged(signalStrength)
                if (signalStrength != null) {
                    var ss = signalStrength.gsmSignalStrength
                    ss = (2 * ss) - 113
                    networkStrength = getSignal(ss)
                    //  Log.e("Netwrk", signalStrength.gsmSignalStrength.toString())

                }

                operatorName = mTelephonyManager.networkOperatorName
                simNo = mTelephonyManager.simSerialNumber ?: ""
                imeiNo = mTelephonyManager.deviceId

             /*   if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    operatorName = mTelephonyManager.networkOperatorName
                    simNo = mTelephonyManager.simSerialNumber ?: ""
                    imeiNo = mTelephonyManager.deviceId
                }*/
            }
        }
        mTelephonyManager.listen(nl, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
    }

    override fun onTerminate() {
        //  LogUtil.d("-------------------onTerminate-------------------")
        super.onTerminate()
        DeviceHelper.unbindService()
        unregisterReceiver(mBatteryReceiver)
    }


}

internal fun getSignal(value: Int): String {
    return when {
        value >= -50 -> "100"
        value >= -60 && value < -50 -> "80"
        value >= -70 && value < -60 -> "60"
        value >= -80 && value < -70 -> "40"
        value >= -90 && value < -80 -> "20"
        else -> ""
    }
}