package com.bonushub.crdb.repository



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
import com.bonushub.pax.utils.*
import com.mindorks.example.coroutines.utils.Status
import com.usdk.apiservice.aidl.pinpad.DeviceName
import com.usdk.apiservice.aidl.pinpad.KAPId
import com.usdk.apiservice.aidl.pinpad.KeyType
import com.usdk.apiservice.limited.pinpad.PinpadLimited
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

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
        var f61 = KeyExchanger.getF61()

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

        val buildDate: String = SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date(BuildConfig.TIMESTAMP))
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
                                    val (strResult,strSucess,_) = startInit(tid)
                                    if(strSucess == true){
                                        return Result.success(ResponseHandler(Status.SUCCESS,"Init Successful",false,false))
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

        AppPreference.saveString("dpk",dpk.toString())

        var pinpadLimited = PinpadLimited(HDFCApplication.appContext, KAPId(0, 0), 0, DeviceName.IPP)

        var result = true
        try {
            val dTmkArr = RSAProvider.decriptTMK(tmk.hexStr2ByteArr(), rsa)
            // val decriptedTmk = dTmkArr[0].hexStr2ByteArr()

            val decriptedTmk = BytesUtil.hexString2Bytes(dTmkArr[0])

            val x = "TMK=${decriptedTmk.byteArr2HexStr()}\nPPK=${ppk.byteArr2HexStr()} KCV=${ppkKcv.byteArr2HexStr()}\nDPK=${dpk.byteArr2HexStr()} KCV=${dpkKcv.byteArr2HexStr()}"
            Utility().logger(KeyExchanger.TAG, x)
            result = pinpadLimited.loadPlainTextKey(KeyType.MAIN_KEY, DemoConfig.KEYID_MAIN, decriptedTmk)
            System.out.println("TMK is success "+result)
            //result = NeptuneService.Device.writeTmk(decriptedTmk, tmkKcv)
            // NeptuneService.beepNormal()
            if (result) {
                result = DeviceHelper.getPinpad(KAPId(0, 0), 0, DeviceName.IPP)?.loadEncKey(KeyType.PIN_KEY, DemoConfig.KEYID_MAIN, DemoConfig.KEYID_PIN, ppk, ppkKcv) ?: false
                System.out.println("PPK is success "+result)

                //     result = NeptuneService.Device.writeTpk(ppk, ppkKcv)
                //   NeptuneService.beepNormal()
            }
            if (result) {
                result = DeviceHelper.getPinpad(KAPId(0, 0), 0, DeviceName.IPP)
                        ?.loadEncKey(KeyType.TDK_KEY, DemoConfig.KEYID_MAIN, DemoConfig.KEYID_TRACK, dpk, ppkKcv) ?: false
                System.out.println("TDK is success "+result)
                //  result = NeptuneService.Device.writeTdk(dpk, dpkKcv)
                // NeptuneService.beepKey(EBeepMode.FREQUENCE_LEVEL_6,1000)
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

    suspend fun startInit(tid: String): Triple<String?, Boolean?, Boolean> {
        var strResult: String? = null
        var strSucess: Boolean? = null

            HitServer.apply {
                reversalToBeSaved = null
            }.hitInitServer({ result, success ->
                System.out.println("Init Suceesuffly msg2 " + result)
                if (success) {
                    strResult = result
                    strSucess = success
                    Toast.makeText(
                        HDFCApplication.appContext, "Init Suceesuffly msg " + strSucess, Toast.LENGTH_LONG
                    ).show()
                    // System.out.println("Init Suceesuffly msg "+strSucess)
                } else {
                    strResult = result
                    strSucess = success
                }
            }, {
                 strResult = it
                strSucess  = true

            }, this@keyexchangeDataSource,tid)


          System.out.println("Init Suceesuffly msg1 "+strResult)


        return Triple(strResult,strSucess,false)
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

}