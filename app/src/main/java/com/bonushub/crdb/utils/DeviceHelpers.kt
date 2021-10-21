package com.bonushub.crdb.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import com.bonushub.crdb.HDFCApplication.Companion.appContext
import com.usdk.apiservice.aidl.DeviceServiceData
import com.usdk.apiservice.aidl.UDeviceService
import com.usdk.apiservice.aidl.device.DeviceInfo
import com.usdk.apiservice.aidl.device.UDeviceManager
import com.usdk.apiservice.aidl.emv.UEMV
import com.usdk.apiservice.aidl.pinpad.KAPId
import com.usdk.apiservice.aidl.pinpad.UPinpad
import com.usdk.apiservice.limited.DeviceServiceLimited

object DeviceHelper : ServiceConnection  {

    private const val TAG = "DeviceHelper"

    // 最大重绑定次数
    private const val MAX_RETRY_COUNT = 3

    // 重绑定间隔时间
    private const val RETRY_INTERVALS: Long = 3000

    private var retry = 0

    private var serviceListener: ServiceReadyListener? = null

    @Volatile
    private var isBinded = false

    @JvmStatic
    var vfDeviceService: UDeviceService? = null

    @JvmStatic
    var vfDeviceManager: UDeviceManager? = null

    @JvmStatic
    var vfUEMV: UEMV? = null

    @JvmStatic
    lateinit var deviceInfo: DeviceInfo

    fun setServiceListener(listener: ServiceReadyListener) {
        serviceListener = listener
        if (isBinded) {
            notifyReady()
        }
    }

    private fun notifyReady() {
        if (serviceListener != null) {
            try {
                serviceListener?.onReady(vfDeviceService?.getVersion())
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
    }


    fun bindService() {

        if (isBinded) {
            return
        }
        val intent = Intent().apply {
            action = "com.usdk.apiservice"
            `package`= "com.usdk.apiservice"
        }
        val bindSucc = appContext.bindService(intent, this, Context.BIND_AUTO_CREATE)

        // 绑定失败, 则重新绑定
        if (!bindSucc && retry++ < MAX_RETRY_COUNT) {
            Log.e(TAG, "=> bind fail, rebind ($retry)")
            Handler().postDelayed({ bindService() }, RETRY_INTERVALS)
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        retry = 0
        isBinded = true
        vfDeviceService = UDeviceService.Stub.asInterface(service)
        vfUEMV         = UEMV.Stub.asInterface(service)
        vfDeviceManager         = UDeviceManager.Stub.asInterface(service)

        DeviceServiceLimited.bind(appContext, vfDeviceService, object :
            DeviceServiceLimited.ServiceBindListener {
            override fun onSuccess() {
                Log.d(TAG, "=> DeviceServiceLimited | bindSuccess")
                try {
                    // Log.d(TAG, "=> Emv is"+ vfDeviceService?.emv)
                    Log.d(TAG, "=> Version is"+ vfDeviceService?.version)
                }
                catch (ex: RemoteException){
                    ex.printStackTrace()
                }

            }

            override fun onFail() {
                Log.e(TAG, "=> bind DeviceServiceLimited fail")
            }
        })

        notifyReady()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Log.e(TAG, "=> onServiceDisconnected")
        vfDeviceService = null
        isBinded = false
        DeviceServiceLimited.unbind(appContext)
        bindService()
    }

    fun unbindService() {
        if (isBinded) {
            Log.e(TAG, "=> unbindService")
            appContext.unbindService(this)
            DeviceServiceLimited.unbind(appContext)
            isBinded = false
        }
    }

    @JvmStatic
    @Throws(IllegalStateException::class)
    fun getEMV(): UEMV? {
        val iBinder = object : IBinderCreator() {
            override fun create(): IBinder {
               return vfDeviceService!!.emv
            }

        }.start()
        return UEMV.Stub.asInterface(iBinder)
    }

    @Throws(IllegalStateException::class)
    fun getDeviceManager(): UDeviceManager? {
        val iBinder = object : IBinderCreator() {
            @Throws(RemoteException::class)
            override fun create(): IBinder {
                return vfDeviceService!!.deviceManager
            }
        }.start()
        return UDeviceManager.Stub.asInterface(iBinder)
    }

    fun getDeviceSerialNo(): String?{
        Log.d(TAG, "=> Device Serial no (${getDeviceManager()!!.deviceInfo?.serialNo})")
       // System.out.println("Serial no is"+getDeviceManager()!!.deviceInfo?.serialNo)
        return getDeviceManager()!!.deviceInfo?.serialNo
    }

    fun getDeviceModel(): String?{
        Log.d(TAG, "=> Device Model no (${getDeviceManager()!!.deviceInfo?.model})")
     //   System.out.println("Serial no is"+getDeviceManager()!!.deviceInfo?.mode)
        return getDeviceManager()!!.deviceInfo?.model
    }

    @JvmStatic
    @Throws(IllegalStateException::class)
    fun getPinpad(kapId: KAPId?, keySystem: Int, deviceName: String?): UPinpad? {
        val iBinder = object : IBinderCreator() {
            @Throws(RemoteException::class)
            override fun create(): IBinder {
                Log.d(TAG, "=> PinPad is(${vfDeviceService!!.getPinpad(kapId, keySystem, deviceName)})")
                return vfDeviceService!!.getPinpad(kapId, keySystem, deviceName)
            }
        }.start()
        return UPinpad.Stub.asInterface(iBinder)
    }

    @Throws(IllegalStateException::class)
    fun register(useEpayModule: Boolean) {
        try {
            val param = Bundle()
            param.putBoolean(DeviceServiceData.USE_EPAY_MODULE, useEpayModule)
            vfDeviceService?.register(param, Binder())
        } catch (e: RemoteException) {
            e.printStackTrace()
            throw IllegalStateException(e.message)
        } catch (e: SecurityException) {
            e.printStackTrace()
            throw IllegalStateException(e.message)
        }
    }

    @Throws(IllegalStateException::class)
    fun unregister() {
        try {
            vfDeviceService?.unregister(null)
        } catch (e: RemoteException) {
            e.printStackTrace()
            throw IllegalStateException(e.message)
        }
    }




    internal abstract class IBinderCreator {
        @Throws(IllegalStateException::class)
        fun start(): IBinder {
            if (vfDeviceService == null) {
                bindService()
                throw IllegalStateException("Servic unbound,please retry latter!")
            }
            return try {
                create()
            } catch (e: DeadObjectException) {
                vfDeviceService = null
                throw IllegalStateException("Service process has stopped,please retry latter!")
            } catch (e: RemoteException) {
                throw IllegalStateException(e.message, e)
            } catch (e: SecurityException) {
                throw IllegalStateException(e.message, e)
            }
        }

        @Throws(RemoteException::class)
        abstract fun create(): IBinder
    }

    interface ServiceReadyListener {
        fun onReady(version: String?)
    }

}