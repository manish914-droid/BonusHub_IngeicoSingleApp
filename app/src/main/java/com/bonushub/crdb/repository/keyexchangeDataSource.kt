package com.bonushub.crdb.repository



import android.os.RemoteException
import android.text.TextUtils
import android.widget.Toast
import com.bonushub.crdb.BuildConfig
import com.bonushub.crdb.HDFCApplication
import com.bonushub.crdb.R
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.di.DBModule
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.serverApi.HitServer
import com.bonushub.crdb.utils.*
import com.bonushub.crdb.vxutils.Utility.*

import com.bonushub.pax.utils.*
import com.mindorks.example.coroutines.utils.Status
import com.usdk.apiservice.aidl.BaseError
import com.usdk.apiservice.aidl.data.IntValue
import com.usdk.apiservice.aidl.pinpad.*
import com.usdk.apiservice.limited.pinpad.PinpadLimited
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

interface IKeyExchangeInit{
    suspend fun createInitIso(nextCounter: String, isFirstCall: Boolean,tid: String): IWriter
}

interface IKeyExchange : IKeyExchangeInit{
    suspend fun createKeyExchangeIso(tid: String): IWriter
}

typealias ApiCallback = (String, Boolean, Boolean,Boolean) -> Unit

class keyexchangeDataSource @Inject constructor(private val appDao: AppDao) : IKeyExchange {

    var keWithInit = true
    var isHdfc = false
    var afterSettlement = false

    private var tmk = ""
    private var tmkKcv: ByteArray = byteArrayOf()
    private var blankTMKByteArray: ByteArray = byteArrayOf()
    private lateinit var rsa: Map<String, Any>

    companion object {
        private val TAG = KeyExchanger::class.java.simpleName

        fun getF61(): String {
            val appName =
                addPad(HDFCApplication.appContext.getString(R.string.app_name), " ", 10, false)


            val deviceModel = /*DeviceHelper.getDeviceModel()*/"  X990"

          //  val deviceModel = addPad(DeviceHelper.getDeviceModel() ?: "", "*", 6, false)

            val buildDate: String = addPad("210105", "0", 15, false)/*SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date(BuildConfig.TIMESTAMP))*/
            //   val version1 = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)
            val version = "${BuildConfig.VERSION_NAME}.$buildDate"
            val connectionType = ConnectionType.GPRS.code
            val pccNo =
                addPad(AppPreference.getString(PreferenceKeyConstant.PC_NUMBER_ONE.keyName), "0", 9)
            val pcNo2 =
                addPad(AppPreference.getString(PreferenceKeyConstant.PC_NUMBER_TWO.keyName), "0", 9)

            return "$connectionType$deviceModel$appName$version$pccNo${""}"
        }

    }



    override suspend fun createKeyExchangeIso(tid: String): IWriter = IsoDataWriter().apply {
        mti = Mti.MTI_LOGON.mti
        // adding processing code and field 59 for public and private key
        //Condition to Check if ISO Packet Create for Logon After Settlement or for other cases:-
        if (!afterSettlement) {
            addField(
                3, if (tmk.isEmpty()) {
                    //resume after
                    rsa = RSAProvider.generateKeyPair()
                    val publicKey = RSAProvider.getPublicKeyBytes(rsa)
                    val f59 = insertBitsInPublicKey(
                        publicKey.substring(44).hexStr2ByteArr().byteArr2Str()
                    )
                    addFieldByHex(59, f59)
                    ProcessingCode.KEY_EXCHANGE.code
                } else ProcessingCode.KEY_EXCHANGE_RESPONSE.code
            )
        } else {
            addField(3, ProcessingCode.KEY_EXCHANGE_AFTER_SETTLEMENT.code)
        }

        //adding stan (padding of stan is internally handled by iso)
        addField(11, paddingInvoiceRoc(appDao.getRoc()) ?: "000000")

        //adding nii
        addField(24, Utility().getNII())

        //adding tid
        addFieldByHex(41, tid)

        //adding field 48
        addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

        //region=========adding field 61=============
        var f61 = getF61()

        //append 1 in case of hdfc bank
        if (isHdfc) {
            f61 += "1"
        }

        addFieldByHex(61, f61)
        //endregion

        //region=====adding field 63============
        val bankCode: String = AppPreference.getBankCode()
        val deviceSerial = addPad(
            DeviceHelper.getDeviceSerialNo() ?: "",
            " ",
            15,
            false
        ) // right padding of 15 byte
        val f63 = "$deviceSerial$bankCode"
        addFieldByHex(63, f63)
        //endregion
    }

    override suspend fun createInitIso(nextCounter: String, isFirstCall: Boolean,tid: String): IWriter = IsoDataWriter().apply  {
        mti = Mti.MTI_LOGON.mti
        // adding processing code and field 59 for public and private key
        addField(
            3, if (isFirstCall) {
                addFieldByHex(60, "${addPad(0, "0", 8)}BP${addPad(0, "0", 4)}")
                ProcessingCode.INIT.code
            } else {
                addFieldByHex(60, nextCounter)
                ProcessingCode.INIT_MORE.code
            }
        )
        //adding stan (padding of stan is internally handled by iso)
        paddingInvoiceRoc(DBModule.appDatabase?.appDao?.getRoc())?.let { addField(11, it) }
        //adding nii
        addField(24, Nii.DEFAULT.nii)

        //adding tid
        addFieldByHex(41, tid)

        //adding field 48
        addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

        //region=========adding field 61=============
        val appName =
            addPad(HDFCApplication.appContext.getString(R.string.app_name), " ", 10, false)

        val deviceModel = /*DeviceHelper.getDeviceModel()*/"  X990"

      // val deviceModel = addPad(DeviceHelper.getDeviceModel() ?: "", "*", 6, true)

        val buildDate: String = addPad("210105", "0", 15, false)/*SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date(BuildConfig.TIMESTAMP))*/
        // val buildDate: String = SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date(BuildConfig.TIMESTAMP))
        val version = "${BuildConfig.VERSION_NAME}.$buildDate"
        val connectionType = ConnectionType.GPRS.code
        val pccNo =
            addPad(AppPreference.getString(PreferenceKeyConstant.PC_NUMBER_ONE.keyName), "0", 9)
        val pcNo2 =
            addPad(AppPreference.getString(PreferenceKeyConstant.PC_NUMBER_TWO.keyName), "0", 9)
        val f61 = "$connectionType$deviceModel$appName$version$pccNo$pcNo2"
        addFieldByHex(61, f61)
        //endregion

        //region=====adding field 63============
        val bankCode: String = AppPreference.getBankCode()
//            val customerId = "00"
//            val walletIssuerId = AppPreference.WALLET_ISSUER_ID
        val deviceSerial = addPad(
            DeviceHelper.getDeviceSerialNo() ?: "",
            " ",
            15,
            false
        ) // right padding of 15 byte
        val f63 = "$deviceSerial$bankCode"
        addFieldByHex(63, f63)
        //endregion

    }

    suspend fun startExchange1(tid: String) : Result<ResponseHandler> {
        return try {
            var strinitSuccess: Boolean? = null

            val isoW = createKeyExchangeIso(tid)
            val bData = isoW.generateIsoByteRequest()
            val (strResult,strSucess,_) = socketConnection(bData)

            if (null !=strResult && strSucess == true) {
                val iso = readIso(strResult)
                Utility().logger(KeyExchanger.TAG, iso.isoMap)
                val resp = iso.isoMap[39]
                val f11 = iso.isoMap[11]

                val f48 = iso.isoMap[48]

                if (f48 != null) Utility.ConnectionTimeStamps.saveStamp(f48.parseRaw2String())

                if (f11 != null) Utility().incrementRoc()

                if (resp != null && resp.rawData.hexStr2ByteArr().byteArr2Str() == "00") {
                    if (tmk.isEmpty()) {
                        tmk = iso.isoMap[59]?.rawData ?: "" // tmk len should be 256 byte or 512 hex char
                        Utility().logger(KeyExchanger.TAG, "RAW TMK = $tmk")
                        if (tmk.length == 518) {  // if tmk len is 259 byte , it means last 3 bytes are tmk KCV
                            tmkKcv = tmk.substring(512, 518).hexStr2ByteArr()
                            tmk = tmk.substring(0, 512)
                        } else if (tmk.length == 524) { // if tmk len is 262 byte, it means last 6 bytes are tmk KCV and tmk wallet KCV
                            tmkKcv = tmk.substring(512, 518).hexStr2ByteArr()
                            tmk = tmk.substring(0, 512)
                        }
                        startExchange1(tid)
                    }
                    else {
                        val ppkDpk = iso.isoMap[59]?.rawData ?: ""
                        Utility().logger(KeyExchanger.TAG, "RAW PPKDPK = $ppkDpk")
                        if (ppkDpk.length == 64 || ppkDpk.length == 76) { // if ppkDpk.length is 76 it mean last 6 bytes belongs to KCV of dpk and ppk

                            var ppkKcv = byteArrayOf()
                            var dpkKcv = byteArrayOf()
                            val dpk = ppkDpk.substring(0, 32)
                            val ppk = ppkDpk.substring(32, 64)

                            if (ppkDpk.length == 76)
                                ppkDpk.substring(32)
                            else {
                                ppkKcv = ppkDpk.substring(64, 70).hexStr2ByteArr()
                                dpkKcv = ppkDpk.substring(70).hexStr2ByteArr()
                            }

                            //    ROCProviderV2.resetRoc(AppPreference.HDFC_BANK_CODE)
                            //    ROCProviderV2.resetRoc(AppPreference.AMEX_BANK_CODE)

                            var insertkeys = insertSecurityKeys(ppk.hexStr2ByteArr(), dpk.hexStr2ByteArr(), ppkKcv, dpkKcv)
                            if (insertkeys) {
                                AppPreference.saveLogin(true)
                                if (keWithInit) {
                                    val (strResult,strSucess,initList) = startInit(tid)
                                    if(strSucess == true){
                                        return Result.success(ResponseHandler(Status.SUCCESS,"Init Successful",false,initList))
                                    }
                                    else{
                                        return Result.error(ResponseHandler(Status.ERROR,strResult ?: "",false,false),strResult ?: "")
                                    }
                                } else {
                                    return  Result.success(ResponseHandler(Status.SUCCESS,"Key Exchange Successful",false,false))
                                }
                            } else {
                                AppPreference.saveLogin(false)
                                return Result.error(ResponseHandler(Status.ERROR,"Error in key insertion",false,false),"Error in key insertion")
                            }

                        }
                        else
                            return Result.error(ResponseHandler(Status.ERROR,"Key exchange error",false,false),"Key exchange error")

                    }

                }
                else{
                    val msg = iso.isoMap[58]?.parseRaw2String() ?: ""
                    return Result.error(ResponseHandler(Status.ERROR,msg,false,false),msg)
                }
            }
            else{
                return Result.error(ResponseHandler(Status.ERROR,strResult ?: "Something Went Wrong",false,false),strResult ?: "Something Went Wrong")

            }
        } catch (ex: Throwable) {
            return Result.error(ResponseHandler(Status.ERROR,ex.message ?: "Something Went Wrong",false,false),ex.message ?: "Something Went Wrong")
        }
    }

    private fun insertSecurityKeys(ppk: ByteArray, dpk: ByteArray, ppkKcv: ByteArray, dpkKcv: ByteArray): Boolean {

        var pinPad = createPinpad(KAPId(0, 0), 0, DeviceName.IPP)
        var pinpadLimited: PinpadLimited? = null
        try {
            val isSucc = pinPad!!.open()
            if (isSucc) {
                println("PINPAD "+"Open success")
            } else {
                println("PINPAD "+"Open fail")
            }
        } catch (e: RemoteException) {
            //  handleException(e)
        }
        try {
            pinpadLimited = PinpadLimited(HDFCApplication.appContext, KAPId(DemoConfig.REGION_ID, DemoConfig.KAP_NUM), 0, DemoConfig.PINPAD_DEVICE_NAME)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }

        val kapMode = IntValue()
        val isSucc1: Boolean = pinPad!!.getKapMode(kapMode)
        if (isSucc1) {
            println("PINPAD "+"getKapMode success[0 - LPTK_MODE; 1 - WORK_MODE]: " + kapMode.data)
        } else {
            println("getKapMode fail")

        }

        var isSucc = true /*pinpadLimited!!.format()*/
        if (isSucc) {
            println("PINPAD "+"format success")
        } else {
            println("PINPAD "+"format fail")

        }


        AppPreference.saveString("dpk", "d417d20909ab523550236d91ec1fc4fa")
        println("dpk value is"+"d417d2090923dgfjhddcvdsajanaXBA1")

        // var pinpadLimited = PinpadLimited(HDFCApplication.appContext, KAPId(0, 0), 0, DeviceName.IPP)

        var result = true
        try {
            val dTmkArr = RSAProvider.decriptTMK(tmk.hexStr2ByteArr(), rsa)
            // val decriptedTmk = dTmkArr[0].hexStr2ByteArr()

            val decriptedTmk = BytesUtil.hexString2Bytes(dTmkArr[0])

            val x = "TMK=${decriptedTmk.byteArr2HexStr()}\nPPK=${ppk.byteArr2HexStr()} KCV=${ppkKcv.byteArr2HexStr()}\nDPK=${dpk.byteArr2HexStr()} KCV=${dpkKcv.byteArr2HexStr()}"
            Utility().logger(KeyExchanger.TAG, x)
            val key = "111111111111111111111111111111111111111111111111"
            //6e54d3ecd57040a102324962d5150494
            //+BytesUtil.hexString2Bytes("                ")
            result = pinpadLimited!!.loadPlainTextKey(KeyType.MAIN_KEY, DemoConfig.KEYID_MAIN, decriptedTmk)
            //  val isExist: Boolean = pinpad.isKeyExist(keyId)
            System.out.println("TMK is success "+result)
            //result = NeptuneService.Device.writeTmk(decriptedTmk, tmkKcv)
            // NeptuneService.beepNormal()

            //  outputBlueText(">>> switchToWorkMode")
            isSucc = pinpadLimited.switchToWorkMode()
            if (isSucc) {
                println("PINPAD "+"switchToWorkMode success")
            } else {
                println("PINPAD  "+"switchToWorkMode fail")
            }


            if (result) {

                //   PPK - f5da035abebd921e64f3005c1b3fb655
                result = pinPad?.loadEncKey(KeyType.PIN_KEY, DemoConfig.KEYID_MAIN, DemoConfig.KEYID_PIN,ppk,ppkKcv) ?: false
                System.out.println("PPK is success "+result)

                //     result = NeptuneService.Device.writeTpk(ppk, ppkKcv)
                //   NeptuneService.beepNormal()
            }
            if (result) {
                //   DPK- d417d20909ab523550236d91ec1fc4fa
                //  result = pinPad?.loadEncKey(KeyType.TDK_KEY, DemoConfig.KEYID_MAIN, DemoConfig.KEYID_TRACK, /*BytesUtil.hexString2Bytes("BDE3888C42CE9DECBDE3888C42CE9DECBDE3888C42CE9DEC")*/BytesUtil.hexString2Bytes("d417d20909ab523550236d91ec1fc4fa"), /*BytesUtil.hexString2Bytes("4CBE91BE")*/null) ?: false
                System.out.println("TDK is success "+result)
                //  result = NeptuneService.Device.writeTdk(dpk, dpkKcv)
                // NeptuneService.beepKey(EBeepMode.FREQUENCE_LEVEL_6,1000)

                result = pinPad?.loadEncKey(KeyType.DEK_KEY, DemoConfig.KEYID_MAIN, DemoConfig.KEYID_DES,dpk,dpkKcv) ?: false
                System.out.println("TDK is success1 "+result)

                val isExist: Boolean = pinPad.isKeyExist(DemoConfig.KEYID_MAIN)
                System.out.println("KYIDMAIN is success "+isExist)
                val isExist1: Boolean = pinPad.isKeyExist(10)
                System.out.println("KEYIDPINKEY is success "+isExist1)
                val isExist2: Boolean = pinPad.isKeyExist(12)
                System.out.println("KEYIDDATAKEY is success "+isExist2)



                try {
                    var desMode = DESMode(DESMode.DM_ENC, DESMode.DM_OM_TECB)
                    val data = "02|36101010020281       "
                    val strtohex = data.str2ByteArr().byteArr2HexStr()
                    var  encResult = pinPad.calculateDes(DemoConfig.KEYID_DES, desMode, null, BytesUtil.hexString2Bytes(strtohex))
                    if (encResult == null) {
                        outputPinpadError("calculateDes fail",pinPad)
                        // return
                    }
                    println("TECB encrypt result = " + byte2HexStr(encResult))

                    desMode = DESMode(DESMode.DM_DEC, DESMode.DM_OM_TECB)
                    val decResult: ByteArray = pinPad.calculateDes(DemoConfig.KEYID_DES, desMode, null, encResult)
                    if (decResult == null) {
                        outputPinpadError("calculateDes fail",pinPad)
                        // return
                    }
                    println("TECB decrypt result = " + byte2HexStr(decResult))
                }
                catch (ex: RemoteException){
                    ex.printStackTrace()
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
            Utility().logger("Key Exc callback(result)hange", e.message ?: "")
            result = false
        } finally {
            try {
            } catch (ex: Exception) {
            }
            //  callback(result)
        }

        return result
    }

    //region============================createPinpad
    fun createPinpad(kapId: KAPId?, keySystem: Int, deviceName: String?): UPinpad? {
        return try {
            DeviceHelper.getPinpad(kapId, keySystem, deviceName)
        } catch (e: RemoteException) {
            e.printStackTrace()
            null
        }
    }

    suspend fun startInit(tid: String): Triple<String?, Boolean?, ArrayList<ByteArray>> {
        var datasaved = false
        var strResult: String? = null
        var strSucess: Boolean? = null
        var initListdata = ArrayList<ByteArray>()

        HitServer.apply { reversalToBeSaved = null }.hitInitServer({ result, success,initList ->
            System.out.println("Init Suceesuffly msg2 " + result)
            if (success) {
                strResult = result
                strSucess = success
                initListdata  = initList

            } else {
                strResult = result
                strSucess = success
            }
        }, {
            strResult = it
            strSucess = true
        },this@keyexchangeDataSource,tid)


        System.out.println("Init Suceesuffly msg1 "+strResult)


        return Triple(strResult,strSucess,initListdata)
    }



    private suspend fun socketConnection(bData: ByteArray) : Triple<String?, Boolean?, Boolean> {
        var strResult: String? = null
        var strSucess: Boolean? = null
        HitServer.apply {
            reversalToBeSaved = null
        }.hitServer(bData, { result, success ->
            if (success && !TextUtils.isEmpty(result)) {
                strResult = result
                strSucess = success
            }
            else{
                strResult = result
                strSucess = success
            }
        }, {
            strResult = it
            strSucess  = false
        })

        return Triple(strResult,strSucess,false)
    }

    private fun insertBitsInPublicKey(privatePublicDatum: String): String {
        val stringBuilder = StringBuilder(privatePublicDatum.length + 7)
        var i = 0
        while (i < privatePublicDatum.length) {
            when (i) {
                0 -> {
                    stringBuilder.append('K')//1 char
                    stringBuilder.append(privatePublicDatum[i])
                }
                9 -> {
                    stringBuilder.append('y')//11 char
                    stringBuilder.append(privatePublicDatum[i])
                }
                18 -> {
                    stringBuilder.append('@')//21 char
                    stringBuilder.append(privatePublicDatum[i])
                }
                42 -> {
                    stringBuilder.append('s')//46 th char
                    stringBuilder.append(privatePublicDatum[i])
                }
                70 -> {
                    stringBuilder.append('h')//75 th char
                    stringBuilder.append(privatePublicDatum[i])
                }
                93 -> {
                    stringBuilder.append('D')//99 th char
                    stringBuilder.append(privatePublicDatum[i])
                }
                137 -> {
                    stringBuilder.append('B')//144 th char
                    stringBuilder.append(privatePublicDatum[i])
                }
                else -> stringBuilder.append(privatePublicDatum[i])
            }
            i++
        }
        return stringBuilder.toString()
    }

    fun outputPinpadError(message: String,pinPad: UPinpad) {
        try {
            println(message + " : " + getErrorDetail(pinPad.getLastError()))
        } catch (e: RemoteException) {
            println("RemoteException | getLastError | " + e.message)
        }
    }

    fun getErrorMessage(error: Int): String? {
        val message: String
        when (error) {
            PinpadError.ERROR_ABOLISH -> message = "ERROR_ABOLISH"
            PinpadError.ERROR_ACCESSING_KAP_DENY -> message = "ERROR_ACCESSING_KAP_DENY"
            PinpadError.ERROR_BAD_KEY_USAGE -> message = "ERROR_BAD_KEY_USAGE"
            PinpadError.ERROR_BAD_MODE_OF_KEY_USE -> message = "ERROR_BAD_MODE_OF_KEY_USE"
            PinpadError.ERROR_BAD_STATUS -> message = "ERROR_BAD_STATUS"
            PinpadError.ERROR_BUSY -> message = "ERROR_BUSY"
            PinpadError.ERROR_CANCELLED_BY_USER -> message = "ERROR_CANCELLED_BY_USER"
            PinpadError.ERROR_COMM_ERR -> message = "ERROR_COMM_ERR"
            PinpadError.ERROR_DUKPT_COUNTER_OVERFLOW -> message = "ERROR_DUKPT_COUNTER_OVERFLOW"
            PinpadError.ERROR_DUKPT_NOT_INITED -> message = "ERROR_DUKPT_NOT_INITED"
            PinpadError.ERROR_ENC_KEY_FMT_TOO_SIMPLE -> message = "ERROR_ENC_KEY_FMT_TOO_SIMPLE"
            PinpadError.ERROR_ENCRYPT_MAG_TRACK_TOO_FREQUENTLY -> message =
                "ERROR_ENCRYPT_MAG_TRACK_TOO_FREQUENTLY"
            PinpadError.ERROR_OTHERERR -> message = "ERROR_OTHERERR"
            PinpadError.ERROR_FAIL_TO_AUTH -> message = "ERROR_FAIL_TO_AUTH"
            PinpadError.ERROR_INCOMPATIBLE_KEY_SYSTEM -> message = "ERROR_INCOMPATIBLE_KEY_SYSTEM"
            PinpadError.ERROR_INVALID_ARGUMENT -> message = "ERROR_INVALID_ARGUMENT"
            PinpadError.ERROR_INVALID_KEY_HANDLE -> message = "ERROR_INVALID_KEY_HANDLE"
            PinpadError.ERROR_KAP_ALREADY_EXIST -> message = "ERROR_KAP_ALREADY_EXIST"
            PinpadError.ERROR_ARGUMENT_CONFLICT -> message = "ERROR_ARGUMENT_CONFLICT"
            PinpadError.ERROR_KEYBUNDLE_ERR -> message = "ERROR_KEYBUNDLE_ERR"
            PinpadError.ERROR_NO_ENOUGH_SPACE -> message = "ERROR_NO_ENOUGH_SPACE"
            PinpadError.ERROR_NO_PIN_ENTERED -> message = "ERROR_NO_PIN_ENTERED"
            PinpadError.ERROR_NO_SUCH_KAP -> message = "ERROR_NO_SUCH_KAP"
            PinpadError.ERROR_NO_SUCH_KEY -> message = "ERROR_NO_SUCH_KEY"
            PinpadError.ERROR_NO_SUCH_PINPAD -> message = "ERROR_NO_SUCH_PINPAD"
            PinpadError.ERROR_PERMISSION_DENY -> message = "ERROR_PERMISSION_DENY"
            PinpadError.ERROR_PIN_ENTRY_TOO_FREQUENTLY -> message = "ERROR_PIN_ENTRY_TOO_FREQUENTLY"
            PinpadError.ERROR_REFER_TO_KEY_OUTSIDE_KAP -> message = "ERROR_REFER_TO_KEY_OUTSIDE_KAP"
            PinpadError.ERROR_REOPEN_PINPAD -> message = "ERROR_REOPEN_PINPAD"
            PinpadError.ERROR_SAME_KEY_VALUE_DETECTED -> message = "ERROR_SAME_KEY_VALUE_DETECTED"
            PinpadError.ERROR_SERVICE_DIED -> message = "ERROR_SERVICE_DIED"
            PinpadError.ERROR_TIMEOUT -> message = "ERROR_TIMEOUT"
            PinpadError.ERROR_UNSUPPORTED_FUNC -> message = "ERROR_UNSUPPORTED_FUNC"
            PinpadError.ERROR_WRONG_KAP_MODE -> message = "ERROR_WRONG_KAP_MODE"
            PinpadError.ERROR_KCV -> message = "ERROR_KCV"
            PinpadError.ERROR_INPUT_TIMEOUT -> message = "ERROR_INPUT_TIMEOUT"
            PinpadError.ERROR_INPUT_COMM_ERR -> message = "ERROR_INPUT_COMM_ERR"
            PinpadError.ERROR_INPUT_UNKNOWN -> message = "ERROR_INPUT_UNKNOWN"
            PinpadError.ERROR_NOT_CERT -> message = "ERROR_NOT_CERT"
            else -> message = getErrorMessage1(error)
        }
        return message
    }

    fun getErrorMessage1(error: Int): String {
        val message: String
        message = when (error) {
            BaseError.SERVICE_CRASH -> "SERVICE_CRASH"
            BaseError.REQUEST_EXCEPTION -> "REQUEST_EXCEPTION"
            BaseError.ERROR_CANNOT_EXECUTABLE -> "ERROR_CANNOT_EXECUTABLE"
            BaseError.ERROR_INTERRUPTED -> "ERROR_INTERRUPTED"
            BaseError.ERROR_HANDLE_INVALID -> "ERROR_HANDLE_INVALID"
            else -> "Unknown error"
        }
        return message
    }
}