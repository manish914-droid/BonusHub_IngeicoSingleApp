package com.bonushub.crdb

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.RemoteException
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bonushub.crdb.db.AppDatabase
import com.bonushub.crdb.utils.DemoConfig
import com.bonushub.crdb.utils.DeviceHelper
import com.bonushub.crdb.utils.replaceFragmentWithNoHistory
import com.bonushub.crdb.view.fragments.MainInfoListFragment
import com.bonushub.crdb.utils.Utility
import com.usdk.apiservice.aidl.pinpad.DeviceName
import com.usdk.apiservice.aidl.pinpad.KAPId
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

     //   println("App database is" + appDatabase.appDao)
      //  println("App database will is"+utilitys.doAThing1())
        replaceFragmentWithNoHistory(MainInfoListFragment(), R.id.container_fragment)
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


        /*    // isKeyExist();
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
            }
*/
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
    }

    private fun onInitResponse(res: String, success: Boolean, progress: Boolean, isReversalFail: Boolean = false) {
        System.out.println("Init Sucessfull msg "+success)
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
interface IDialog {

    fun getMsgDialog(
        title: String,
        msg: String,
        positiveTxt: String,
        negativeTxt: String,
        positiveAction: () -> Unit,
        negativeAction: () -> Unit,
        isCancellable: Boolean = false
    )

    fun setProgressTitle(title: String)
    //fun onEvents(event: VxEvent)

    fun showToast(msg: String)
    fun showProgress(progressMsg: String = "Please Wait....")
    fun hideProgress()
    fun getInfoDialog(title: String, msg: String, acceptCb: () -> Unit)
    fun getInfoDialogdoubletap(title: String, msg: String, acceptCb: (Boolean, Dialog) -> Unit)

    fun updatePercentProgress(percent:Int)
    fun showPercentDialog(progressMsg: String = "Please Wait....")

}
