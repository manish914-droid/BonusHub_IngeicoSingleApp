package com.bonushub.pax.utils


import android.content.Context
import android.util.Log
import com.bonushub.crdb.di.DBModule.appDatabase
import com.bonushub.crdb.BuildConfig
import com.bonushub.crdb.HDFCApplication
import com.bonushub.crdb.R
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.di.DBModule
import com.bonushub.crdb.model.local.TerminalParameterTable
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.serverApi.HitServer
import com.bonushub.crdb.serverApi.ServerCommunicator
import com.bonushub.crdb.utils.*
import com.bonushub.crdb.utils.DemoConfig.*
import com.usdk.apiservice.aidl.pinpad.DeviceName
import com.usdk.apiservice.aidl.pinpad.KAPId
import com.usdk.apiservice.aidl.pinpad.KeyType
import com.usdk.apiservice.limited.pinpad.PinpadLimited
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

interface ITransactionPacketExchange {
    fun createTransactionPacket(): IWriter
}

interface IKeyExchangeInit {
    suspend fun createInitIso(nextCounter: String, isFirstCall: Boolean): IWriter
}

interface IKeyExchange : IKeyExchangeInit {
    suspend fun createKeyExchangeIso(): IWriter
}


/**
 * KCV matching part is remaining, KCV extraction is done [18-07-2019]
 * */




typealias ApiCallback = (String, Boolean, Boolean,Boolean) -> Unit

class KeyExchanger(private var context: Context, private val tid: String, private val callback: ApiCallback) : IKeyExchange {

    var keWithInit = true
    var isHdfc = false
    var afterSettlement = false
    @Inject
    lateinit var appDao: AppDao
    companion object {
        val TAG = KeyExchanger::class.java.simpleName

        fun getF61(): String {
            val appName =
                addPad(HDFCApplication.appContext.getString(R.string.app_name), " ", 10, false)

            val deviceModel = DeviceHelper.getDeviceModel()?.substring(0,6)//"  X990"

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

        // region for auto settlement
        fun getDigiPosStatus(
            field57RequestData: String,
            processingCode: String, isSaveTransAsPending: Boolean = false,
            cb: (Boolean, String, String, String) -> Unit
        ) {
            val baseTid = runBlocking(Dispatchers.IO) { getBaseTID(DBModule.appDatabase.appDao) }
            val idw = IsoDataWriter().apply {
                val terminalData = Field48ResponseTimestamp.getTptData()
                if (terminalData != null) {
                    mti = Mti.EIGHT_HUNDRED_MTI.mti

                    //Processing Code Field 3
                    addField(3, processingCode)

                    //STAN(ROC) Field 11
                    addField(11, "000236")

                    //NII Field 24
                    addField(24, Nii.BRAND_EMI_MASTER.nii)

                    //TID Field 41
                    addFieldByHex(41, baseTid)

                    //Connection Time Stamps Field 48
                    addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

                    //adding Field 57
                    addFieldByHex(57, field57RequestData)

                    //adding Field 61
                    val version = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)
                    val pcNumber = addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY), "0", 9)
                    val pcNumber2 =
                        addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY_2), "0", 9)
                    val f61 = ConnectionType.GPRS.code + addPad(
                        deviceModel(),
                        " ",
                        6,
                        false
                    ) + addPad(
                        HDFCApplication.appContext.getString(R.string.app_name),
                        " ",
                        10,
                        false
                    ) + version + pcNumber + pcNumber2
                    //adding Field 61
                    addFieldByHex(61, f61)

                    //adding Field 63
                    val deviceSerial = addPad(AppPreference.getString("serialNumber"), " ", 15, false)
                    val bankCode = AppPreference.getBankCode()
                    val f63 = "$deviceSerial$bankCode"
                    addFieldByHex(63, f63)
                }
            }

            logger("DIGIPOS REQ1>>", idw.isoMap, "e")

            // val idwByteArray = idw.generateIsoByteRequest()

            var responseField57 = ""
            var responseMsg = ""
            var isBool = false

            runBlocking {
                HitServer.hitDigiPosServer(idw, isSaveTransAsPending) { result, success ->
                    responseMsg = result
                    if (success) {
                        /*     ROCProviderV2.incrementFromResponse(
                                 ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                                 AppPreference.getBankCode()
                             )*/
                        val responseIsoData: IsoDataReader = readIso(result, false)
                        logger("Transaction RESPONSE ", "---", "e")
                        logger("Transaction RESPONSE --->>", responseIsoData.isoMap, "e")
                        Log.e(
                            "Success 39-->  ",
                            responseIsoData.isoMap[39]?.parseRaw2String().toString() + "---->" +
                                    responseIsoData.isoMap[58]?.parseRaw2String().toString()
                        )
                        val successResponseCode = responseIsoData.isoMap[39]?.parseRaw2String().toString()
                        if (responseIsoData.isoMap[57] != null) {
                            responseField57 = responseIsoData.isoMap[57]?.parseRaw2String().toString()
                        }
                        if (responseIsoData.isoMap[58] != null) {
                            responseMsg = responseIsoData.isoMap[58]?.parseRaw2String().toString()
                        }
                        isBool = successResponseCode == "00"
                        if(!isBool){
                            responseField57="Empty"
                        }
                        cb(isBool, responseMsg, responseField57, result)

                    } else {
                        /*   ROCProviderV2.incrementFromResponse(
                               ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                               AppPreference.getBankCode()
                           )*/
                        cb(isBool, responseMsg, responseField57, result)
                    }
                }
            }

        }
        // end region
    }

    private var tmk = ""
    private var tmkKcv: ByteArray = byteArrayOf()
    private var blankTMKByteArray: ByteArray = byteArrayOf()
    private lateinit var rsa: Map<String, Any>

    private fun backToCalled(msg: String, success: Boolean, isProgressType: Boolean) {
        Utility().logger(TAG, "msg = $msg, success = $success, isProgressType = $isProgressType")
        GlobalScope.launch(Dispatchers.Main) {
            callback(msg, success, isProgressType, false)
        }
    }
/*

    fun startExchange() {
        GlobalScope.launch {
            val isoW = createKeyExchangeIso()
            val bData = isoW.generateIsoByteRequest()
            HitServer.apply {
                reversalToBeSaved = null
            }.hitServer(isoW, { result, success ->
                if (success && !TextUtils.isEmpty(result)) {
                    launch {
                        val iso = readIso(result)
                        Utility().logger(TAG, iso.isoMap)
                        val resp = iso.isoMap[39]
                        val f11 = iso.isoMap[11]

                        val f48 = iso.isoMap[48]

                        if (f48 != null) Utility.ConnectionTimeStamps.saveStamp(f48.parseRaw2String())

                        if (f11 != null) Utility().incrementRoc() // ROCProviderV2.incrementFromResponse(f11.rawData, AppPreference.HDFC_BANK_CODE) else ROCProviderV2.increment(AppPreference.HDFC_BANK_CODE)

                        if (resp != null && resp.rawData.hexStr2ByteArr().byteArr2Str() == "00") {
                            if (tmk.isEmpty()) {
                                tmk = iso.isoMap[59]?.rawData
                                    ?: "" // tmk len should be 256 byte or 512 hex char
                                Utility().logger(TAG, "RAW TMK = $tmk")
                                if (tmk.length == 518) {  // if tmk len is 259 byte , it means last 3 bytes are tmk KCV
                                    tmkKcv = tmk.substring(512, 518).hexStr2ByteArr()
                                    tmk = tmk.substring(0, 512)
                                } else if (tmk.length == 524) { // if tmk len is 262 byte, it means last 6 bytes are tmk KCV and tmk wallet KCV
                                    tmkKcv = tmk.substring(512, 518).hexStr2ByteArr()
                                    tmk = tmk.substring(0, 512)
                                }
                                startExchange()
                            }
                            else {
                                val ppkDpk = iso.isoMap[59]?.rawData ?: ""
                                Utility().logger(TAG, "RAW PPKDPK = $ppkDpk")
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

                                    insertSecurityKeys(ppk.hexStr2ByteArr(), dpk.hexStr2ByteArr(), ppkKcv, dpkKcv) {
                                        if (it) {
                                            launch { AppPreference.saveLogin(true) }
                                            if (keWithInit) {
                                                //startInit()
                                            } else {
                                                backToCalled(
                                                    "Key Exchange Successful",
                                                    success,
                                                    false
                                                )
                                            }
                                        } else {
                                            launch { AppPreference.saveLogin(false) }
                                            backToCalled("Error in key insertion", false, false)
                                        }
                                    }
                                } else backToCalled("Key exchange error", false, false)
                            }
                        } else {
                            val msg = iso.isoMap[58]?.parseRaw2String() ?: ""
                            backToCalled(msg, false, false)
                        }
                    }
                } else backToCalled(result, false, false)

            }, { backToCalled(it, false, true) })

        }
    }
*/

    override suspend fun createKeyExchangeIso(): IWriter = IsoDataWriter().apply {
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
        addField(11, paddingInvoiceRoc(appDatabase?.appDao.getRoc()) ?: "000000")

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

  /*  fun startInit() {
        GlobalScope.launch {
            HitServer.hitInitServer({ result, success ->
                if (success) {
                    GlobalScope.launch {
                        // setAutoSettlement()  // Setting auto settlement.
                        downloadPromo()  // Setting
                    }

                    backToCalled(result, success, false)
                } else backToCalled(result, false, false)
            }, {
                backToCalled(it, false, true)
            }, this@KeyExchanger)
        }
    }*/

    override suspend fun createInitIso(nextCounter: String, isFirstCall: Boolean): IWriter =
        IsoDataWriter().apply {
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
            paddingInvoiceRoc(appDatabase?.appDao?.getRoc())?.let { addField(11, it) }
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

    private fun insertSecurityKeys(
        ppk: ByteArray, dpk: ByteArray,
        ppkKcv: ByteArray, dpkKcv: ByteArray, callback: (Boolean) -> Unit
    ) {

        var pinpadLimited = PinpadLimited(HDFCApplication.appContext, KAPId(0, 0), 0, DeviceName.IPP)

        var result = true
        try {
            val dTmkArr = RSAProvider.decriptTMK(tmk.hexStr2ByteArr(), rsa)
           // val decriptedTmk = dTmkArr[0].hexStr2ByteArr()

            val decriptedTmk = BytesUtil.hexString2Bytes(dTmkArr[0])

            val x = "TMK=${decriptedTmk.byteArr2HexStr()}\nPPK=${ppk.byteArr2HexStr()} KCV=${ppkKcv.byteArr2HexStr()}\nDPK=${dpk.byteArr2HexStr()} KCV=${dpkKcv.byteArr2HexStr()}"
            Utility().logger(TAG, x)
            result = pinpadLimited.loadPlainTextKey(KeyType.MAIN_KEY, KEYID_MAIN, decriptedTmk)
            System.out.println("TMK is success "+result)
            //result = NeptuneService.Device.writeTmk(decriptedTmk, tmkKcv)
            // NeptuneService.beepNormal()
            if (result) {
                result = DeviceHelper.getPinpad(KAPId(0, 0), 0, DeviceName.IPP)?.loadEncKey(KeyType.PIN_KEY, KEYID_MAIN, KEYID_PIN, ppk, ppkKcv) ?: false
                System.out.println("PPK is success "+result)

                //     result = NeptuneService.Device.writeTpk(ppk, ppkKcv)
                //   NeptuneService.beepNormal()
            }
            if (result) {
                result = DeviceHelper.getPinpad(KAPId(0, 0), 0, DeviceName.IPP)
                    ?.loadEncKey(KeyType.TDK_KEY, KEYID_MAIN, KEYID_TRACK, dpk, ppkKcv) ?: false
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
            callback(result)
        }
    }


    suspend fun downloadPromo() {
        val sc = ServerCommunicator()
        val tpt: TerminalParameterTable? = (Utility().getTptData())
        val fileArray = mutableListOf<Byte>()

        if (tpt != null && sc.open()) {

            //======First Get header and footer, then proceed for image downloading =========

            val isoW = IsoDataWriter().apply {

                mti = Mti.MTI_INIT.mti
                addField(3, ProcessingCode.CHARGE_SLIP_HEADER_FOOTER.code)
                paddingInvoiceRoc(appDatabase?.appDao?.getRoc())?.let { addField(11, it) }
                addField(24, Nii.DEFAULT.nii)

              //  addFieldByHex(41, tpt.terminalId)

                addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

                //region========Adding Field 61=========
                addFieldByHex(61, KeyExchanger.getF61())
                //endregion

                //region====Adding field 63==========
                val f63 = DeviceHelper.getDeviceSerialNo()
                val bankCode = AppPreference.getBankCode()
                addFieldByHex(63, "$f63$bankCode")
                //endregion


                addFieldByHex(60, "00000000000000")
            }

            //region======First Get header and footer, then proceed for image downloading =========

            val data = isoW.generateIsoByteRequest()

            val respH = sc.sendData(data)

            if (respH.isNotEmpty()) {
                val res = readIso(respH, false)

                //region==adding ROC==
                val roc = res.isoMap[11]?.rawData
                if (roc != null) Utility().incrementRoc()// ROCProvider.incrementFromResponse(roc) else ROCProvider.increment()
                //endregion
                if (res.isoMap[39]?.parseRaw2String() == "00") {
                    val f59 = res.isoMap[59]?.parseRaw2String() ?: ""
                    if (f59.isNotEmpty()) {
                        //   AppPreference.saveString(AppPreference.HEADER_FOOTER, f59)
                    }
                }

            }

            //endregion=============

            //=====Changing processing code for image downloading========
            isoW.addField(3, ProcessingCode.CHARGE_SLIP_START.code)

            var pCode = ""
            var packetRecd: Int = 0
            do {

                val data = isoW.generateIsoByteRequest()

                val resp = sc.sendData(data)

                if (resp.isNotEmpty()) {
                    val res = readIso(resp, false)
                    pCode = res.isoMap[3]?.rawData ?: ""
                    if (pCode.isNotEmpty()) isoW.addField(3, pCode)

                    //region==adding ROC==
                    val roc = res.isoMap[11]?.rawData
                    if (roc != null) Utility().incrementRoc()// ROCProvider.incrementFromResponse(roc) else ROCProvider.increment()
                    //endregion

                    val f60 = res.isoMap[60]?.rawData ?: ""

                    if (f60.isNotEmpty()) {

                        val entry = f60.substring(8..35).hexStr2ByteArr().byteArr2Str()

                        packetRecd = entry.substring(0, 8).toInt() - packetRecd

                        if (packetRecd > 0) {
                            isoW.addFieldByHex(60, entry)
                            val da1 = f60.substring(f60.length - (packetRecd * 2), f60.length)
                            fileArray.addAll(da1.hexStr2ByteArr().toList())
                        }
                    }
                } else break
            } while (pCode == ProcessingCode.CHARGE_SLIP_CONTINUE.code)
            sc.close()
        }
        if (fileArray.isNotEmpty()) {
            //   unzipZipedBytes(fileArray.toByteArray())
        }

    }

}



