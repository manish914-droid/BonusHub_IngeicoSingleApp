package com.bonushub.crdb

import android.os.Bundle
import android.os.Handler
import android.os.RemoteException
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bonushub.crdb.db.AppDatabase
import com.bonushub.crdb.utils.BytesUtil
import com.bonushub.crdb.utils.DemoConfig
import com.bonushub.crdb.utils.DemoConfig.KEYID_MAIN
import com.bonushub.crdb.utils.DeviceHelper
import com.bonushub.pax.utils.KeyExchanger
import com.bonushub.pax.utils.Utility
import com.usdk.apiservice.aidl.pinpad.DeviceName
import com.usdk.apiservice.aidl.pinpad.KAPId
import com.usdk.apiservice.aidl.pinpad.KeyType
import com.usdk.apiservice.aidl.pinpad.UPinpad
import com.usdk.apiservice.limited.pinpad.PinpadLimited
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(),DeviceHelper.ServiceReadyListener {

    private var deviceserialno: String? = null
    private var devicemodelno: String? = null
    private var pinpad: UPinpad? = null
    private var pinpadLimited: PinpadLimited? = null

    @Inject
    lateinit var appDatabase: AppDatabase

  //  @Inject
 //   lateinit var utilitys: Utility

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        DeviceHelper.setServiceListener(this)

        println("App database is" + appDatabase.appDao)
      //  println("App database will is"+utilitys.doAThing1())
      //  replaceFragmentWithNoHistory(MainInfoListFragment(), R.id.container_fragment)
    }

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


      /*      // isKeyExist();
            val key = "6AC292FAA1315B4D858AB3A3D7D5933A"

            val keyId: Int = KEYID_MAIN
            var isSucc = false
            try {
                isSucc = pinpadLimited!!.loadPlainTextKey(KeyType.MAIN_KEY, keyId, BytesUtil.hexString2Bytes(key))
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
            if (isSucc) {
                println("TMK is$isSucc")
             //   outputText(String.format("loadPlainTextKey(MAIN_KEY, keyId = %s) success", 0))
            } else {
                println("TMK is$isSucc")
                // outputPinpadError("loadPlainTextKey fail");
                // return;
            }*/

        }, 100)


    }


    fun createPinpad(kapId: KAPId?, keySystem: Int, deviceName: String?): UPinpad? {
        return try {
            DeviceHelper.getPinpad(kapId, keySystem, deviceName)
        } catch (e: RemoteException) {
            e.printStackTrace()
            null
        }
    }

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
        Handler().postDelayed(Runnable {
            KeyExchanger(this, "41501379", ::onInitResponse).apply {
                keWithInit = true
            }.startExchange()
        },500)

    }

    private fun onInitResponse(res: String, success: Boolean, progress: Boolean, isReversalFail: Boolean = false) {

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


