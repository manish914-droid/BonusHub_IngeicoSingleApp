package com.bonushub.crdb.india.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import com.bonushub.crdb.india.HDFCApplication.Companion.appContext
import com.bonushub.crdb.india.vxutils.getErrorDetail
import com.ingenico.hdfcpayment.IPaymentService
import com.ingenico.hdfcpayment.listener.OnOperationListener
import com.ingenico.hdfcpayment.listener.OnPaymentListener
import com.ingenico.hdfcpayment.request.*
import com.usdk.apiservice.aidl.DeviceServiceData
import com.usdk.apiservice.aidl.UDeviceService
import com.usdk.apiservice.aidl.algorithm.UAlgorithm
import com.usdk.apiservice.aidl.beeper.UBeeper
import com.usdk.apiservice.aidl.device.DeviceInfo
import com.usdk.apiservice.aidl.device.UDeviceManager
import com.usdk.apiservice.aidl.emv.UEMV
import com.usdk.apiservice.aidl.pinpad.KAPId
import com.usdk.apiservice.aidl.pinpad.UPinpad
import com.usdk.apiservice.aidl.printer.UPrinter
import com.usdk.apiservice.aidl.scanner.OnScanListener
import com.usdk.apiservice.aidl.scanner.ScannerData
import com.usdk.apiservice.aidl.scanner.UScanner
import com.usdk.apiservice.aidl.tms.UTMS
import com.usdk.apiservice.aidl.vectorprinter.UVectorPrinter
import com.usdk.apiservice.limited.DeviceServiceLimited
import java.text.SimpleDateFormat
import java.util.*

object DeviceHelper   {


    @JvmStatic
    private  val PACKAGE_ID = "com.ingenico.ingp.standalone"
    @JvmStatic
    private  val ACTION = "com.ingenico.hdfcpayment.PaymentService.BIND"
    @JvmStatic
    private  val PACKAGE_ID_USDK = "com.usdk.apiservice"
    @JvmStatic
    private  val ACTIONUSDKSERVICE = "com.usdk.apiservice"
    private  val TAG = DeviceHelper::class.java.simpleName
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

    @JvmStatic
    var iRemoteService: IPaymentService? = null

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
        val bindUSDKSucc =  appContext?.bindService(
            Intent(ACTIONUSDKSERVICE).apply { setPackage(PACKAGE_ID_USDK) },
            remoteUsdkServiceConnection,
            Context.BIND_AUTO_CREATE
        )

        /* val bindSucc =  appContext?.bindService(
             Intent(ACTION).apply { setPackage(PACKAGE_ID) },
             remoteServiceConnection,
             Context.BIND_AUTO_CREATE
         )*/

        // 绑定失败, 则重新绑定
        if (!bindUSDKSucc  && retry++ < MAX_RETRY_COUNT) {
            Log.e(TAG, "=> bind fail, rebind ($retry)")
            Handler().postDelayed({ bindService() }, RETRY_INTERVALS)
        }
    }

    private val remoteUsdkServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            retry = 0
            isBinded = true
            vfDeviceService = UDeviceService.Stub.asInterface(service)
            /* vfUEMV         = UEMV.Stub.asInterface(service)
             vfDeviceManager         = UDeviceManager.Stub.asInterface(service)*/
            //here I stub VFPayment Service
            DeviceServiceLimited.bind(appContext, vfDeviceService, object : DeviceServiceLimited.ServiceBindListener {
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

    }

    /**
     * handle remote service connection
     * */
    private val remoteServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service has connected")
            iRemoteService = IPaymentService.Stub.asInterface(service)
            notifyReady()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service has unexpectedly disconnected")
        }


    }

    fun connect() {
        if (iRemoteService == null) {
            //  activity = context as? BaseActivity
            appContext?.bindService(
                Intent(ACTION).apply { setPackage(PACKAGE_ID) },
                remoteServiceConnection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    /**
     * check if remote service is connected or not
     * */
    fun isServiceConnected() = iRemoteService != null

    fun unbindService() {
        if (isBinded) {
            Log.e(TAG, "=> unbindService")
            appContext.unbindService(remoteUsdkServiceConnection)
            DeviceServiceLimited.unbind(appContext)
            // appContext.unbindService(remoteServiceConnection)
            //  iRemoteService = null
            isBinded = false
        }
    }

    /**
     * Execute CommConnection Transaction
     * */
    fun setCommunicationSettings(request: CommRequest, listener: OnOperationListener) {
        iRemoteService?.setCommunicationSettings(request, listener)
    }

    /**
     * Execute Terminal Initialization transaction
     * */
    fun doTerminalInitialization(request: TerminalInitializationRequest?, listener: OnOperationListener?) {
        iRemoteService?.doTerminalInitialization(request, listener)
    }

    /**
     * Execute Sale transaction
     * */
    fun doSaleTransaction(request: SaleRequest, listener: OnPaymentListener?) {
        iRemoteService?.doSaleTransaction(request, listener)
    }
    /**
     * Execute Cash Advance transaction
     * */
    fun doCashAdvanceTxn(cashOnly: CashOnlyRequest, listener: OnPaymentListener?){
        iRemoteService?.doCashOnlyTransaction(cashOnly,listener)
    }
    /**
     * Execute Sale with Cash transaction
     * */
    fun doSaleWithCashTxn(saleCashBackRequest: SaleCashBackRequest, listener: OnPaymentListener?){
        iRemoteService?.doSaleCashBackTransaction(saleCashBackRequest,listener)
    }
    /**
     * Execute PreAuth transaction
     * */
    fun doPreAuthTxn(preAuthRequest: PreAuthRequest, listener: OnPaymentListener?){
        iRemoteService?.doPreAuthTransaction(preAuthRequest,listener)
    }
    /**
     * Execute Refund transaction
     * */
    fun doRefundTxn(refundRequest: RefundRequest, listener: OnPaymentListener?){
        iRemoteService?.doRefundTransaction(refundRequest,listener)
    }
    /**
     * Execute PreAuth complete transaction
     * */
    fun doPreAuthCompleteTxn(preAuthCompleteRequest: PreAuthCompleteRequest, listener: OnPaymentListener?){
        iRemoteService?.doPreAuthCompleteTransaction(preAuthCompleteRequest,listener)
    }
    /**
     * Execute PreAuth View transaction
     * */
    fun doPreAuthViewTxn(listener: OnOperationListener){
        iRemoteService?.showPreAuthView(listener)
    }
    /**
     * Execute EMI transaction
     * */
    fun doEMITxn(emiSaleRequest: EMISaleRequest, listener: OnPaymentListener?){
        iRemoteService?.doEMITransaction(emiSaleRequest,listener)
    }

    /**
     * Execute Settlement transaction
     * */
    fun doSettlement(request: SettlementRequest, listener: OnOperationListener?){
        iRemoteService?.doSettlement(request, listener)
    }


    /**
     * Execute void transaction
     * */
    fun doVoidTransaction(request: VoidRequest, listener:OnPaymentListener ){
        iRemoteService?.doVoidTransaction(request,listener)
    }

    /**
     * Show admin function screen
     * */
    fun showAdminFunction(listener: OnOperationListener) {
        iRemoteService?.showAdminFunction(listener)
    }
    /**
     * Show admin function screen
     * */
    fun getTransactionByUId(uid : String,listener: OnOperationListener) {
        iRemoteService?.getTransactionByEcrUId(uid,listener)
    }
    fun doSettlementtxn(request: SettlementRequest, listener:OnOperationListener ){
        iRemoteService?.doSettlement(request,listener)

    }

    @JvmStatic
    //@RequiresApi(Build.VERSION_CODES.O)
    fun getCurentDateTime() : String{
        val currentTime = SimpleDateFormat("yyMMdd HHmmss", Locale.getDefault()).format(Date())
        println("Current Date and Time is: $currentTime")
        return currentTime
    }

    @JvmStatic
    @Throws(IllegalStateException::class)
    fun getTMS(): UTMS? {
        val iBinder = object : IBinderCreator() {
            override fun create(): IBinder {
                return vfDeviceService!!.tms
            }

        }.start()
        return UTMS.Stub.asInterface(iBinder)
    }


    @JvmStatic
    @Throws(IllegalStateException::class)
    fun getEMV(): UEMV? {
        val iBinder = object : IBinderCreator() {
            override fun create(): IBinder {
                Log.e("EMV INSTANCE", vfDeviceService!!.emv.toString())
                return vfDeviceService!!.emv
            }

        }.start()
        return UEMV.Stub.asInterface(iBinder)
    }

     @JvmStatic
    @Throws(IllegalStateException::class)
    fun getBeeper(): UEMV? {
        val iBinder = object : IBinderCreator() {
            override fun create(): IBinder {
                Log.e("EMV INSTANCE", vfDeviceService!!.emv.toString())
                return vfDeviceService!!.emv
            }

        }.start()
        return UEMV.Stub.asInterface(iBinder)
    }



    @JvmStatic
    @Throws(IllegalStateException::class)
    fun getAlgorithm(): UAlgorithm? {
        val iBinder = object : IBinderCreator() {
            @Throws(RemoteException::class)
            override fun create(): IBinder {
                return vfDeviceService!!.algorithm
            }
        }.start()
        return UAlgorithm.Stub.asInterface(iBinder)
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

    fun getSimNumber(): String?{
        Log.d(TAG, "=> Device Sim no (${getDeviceManager()?.deviceInfo?.iccid})")
        // System.out.println("Serial no is"+getDeviceManager()!!.deviceInfo?.serialNo)
        return getDeviceManager()?.deviceInfo?.iccid
    }

    fun getDeviceModel(): String?{
        Log.d(TAG, "=> Device Model no (${getDeviceManager()!!.deviceInfo?.model})")
        //   System.out.println("Serial no is"+getDeviceManager()!!.deviceInfo?.mode)
        return getDeviceManager()!!.deviceInfo?.model
    }

    fun getImeiNumber(): String?{
        Log.d(TAG, "=> Device Imei no (${getDeviceManager()?.deviceInfo?.imei})")
        // System.out.println("Serial no is"+getDeviceManager()!!.deviceInfo?.serialNo)
        return getDeviceManager()?.deviceInfo?.imei
    }



    @Throws(java.lang.IllegalStateException::class)
    fun getPrinter(): UPrinter? {
        val iBinder = object : IBinderCreator() {
            @Throws(RemoteException::class)
            override fun create(): IBinder {
                return vfDeviceService?.printer!!
            }
        }.start()
        return UPrinter.Stub.asInterface(iBinder)
    }

    @Throws(java.lang.IllegalStateException::class)
    fun getVectorPrinter(): UVectorPrinter? {
        val iBinder = object : IBinderCreator() {
            @Throws(RemoteException::class)
            override fun create(): IBinder {
                return vfDeviceService?.vectorPrinter!!
            }
        }.start()
        return UVectorPrinter.Stub.asInterface(iBinder)
    }

    @JvmStatic
    @Throws(IllegalStateException::class)
    fun getUScanner(): UScanner? {
        val iBinder = object : IBinderCreator() {
            override fun create(): IBinder {
                return vfDeviceService!!.getScanner(1)
            }

        }.start()
        return UScanner.Stub.asInterface(iBinder)
    }

    fun openScanner(cb:(status:Boolean,message:String, barcode:String) -> Unit){

        try {
            val bundle = Bundle()
            bundle.putInt(ScannerData.TIMEOUT,50)
            logger("start scan","open","e")
            getUScanner()!!.startScan(bundle,object : OnScanListener.Stub(){
                override fun onSuccess(p0: String?) {
                    logger("start scan"," -> on Success","e")
                    cb(true, "success",p0?:"")
                }

                override fun onError(p0: Int) {
                    logger("start scan"," -> on Error","e")
                    getErrorDetail(p0)
                    cb(false, getErrorDetail(p0),"")

                }

                override fun onTimeout() {
                    logger("start scan"," -> on Timeout","e")
                    cb(false,"time out","")

                }

                override fun onCancel() {
                    logger("start scan"," -> on cancel","e")
                    cb(false,"cancel","")

                }
            })
        }catch (ex:RemoteException)
        {
            ex.printStackTrace()
            cb(false,"Something went wrong!!", "")

        }catch (ex:DeadObjectException)
        {
            ex.printStackTrace()
            cb(false,"Something went wrong!!", "")

        }catch (ex:Exception)
        {
            ex.printStackTrace()
            cb(false,"Something went wrong!!", "")

        }



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