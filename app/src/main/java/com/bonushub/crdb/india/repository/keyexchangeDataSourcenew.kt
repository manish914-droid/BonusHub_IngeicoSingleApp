package com.bonushub.crdb.india.repository



import com.bonushub.crdb.india.BuildConfig
import com.bonushub.crdb.india.HDFCApplication
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.db.AppDao
import com.bonushub.crdb.india.model.local.AppPreference
import com.bonushub.crdb.india.utils.*
import com.bonushub.pax.utils.*
import javax.inject.Inject


class keyexchangeDataSourcenew @Inject constructor(private val appDao: AppDao) : IKeyExchange {

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

    }

     suspend fun startExchange(tid: String) : RespMessageStatusData {
         val isoW = createKeyExchangeIso(tid)
            val keyExchangeStatus=HitServernew.hitServer(isoW)
            if(keyExchangeStatus.isSuccess){
                val isoReader=keyExchangeStatus.anyData as IsoDataReader
                        Utility().logger(KeyExchanger.TAG, isoReader.isoMap)
                        val f11 = isoReader.isoMap[11]
                        val f48 = isoReader.isoMap[48]
                        if (f48 != null) Utility.ConnectionTimeStamps.saveStamp(f48.parseRaw2String())
                        if (f11 != null) Utility().incrementRoc() // ROCProviderV2.incrementFromResponse(f11.rawData, AppPreference.HDFC_BANK_CODE) else ROCProviderV2.increment(AppPreference.HDFC_BANK_CODE)
                            if (tmk.isEmpty()) {
                                tmk = isoReader.isoMap[59]?.rawData ?: "" // tmk len should be 256 byte or 512 hex char
                                Utility().logger(KeyExchanger.TAG, "RAW TMK = $tmk")
                                if (tmk.length == 518) {  // if tmk len is 259 byte , it means last 3 bytes are tmk KCV
                                    tmkKcv = tmk.substring(512, 518).hexStr2ByteArr()
                                    tmk = tmk.substring(0, 512)
                                } else if (tmk.length == 524) { // if tmk len is 262 byte, it means last 6 bytes are tmk KCV and tmk wallet KCV
                                    tmkKcv = tmk.substring(512, 518).hexStr2ByteArr()
                                    tmk = tmk.substring(0, 512)
                                }
                              //  startExchange(tid,backToCalled)
                                return RespMessageStatusData()
                            }
                            else {
                                val ppkDpk = isoReader.isoMap[59]?.rawData ?: ""
                                Utility().logger(KeyExchanger.TAG, "RAW PPKDPK = $ppkDpk")
                                if (ppkDpk.length == 64 || ppkDpk.length == 76) {
                                    // if ppkDpk.length is 76 it mean last 6 bytes belongs to KCV of dpk and ppk
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
                                    /*insertSecurityKeys(ppk.hexStr2ByteArr(), dpk.hexStr2ByteArr(), ppkKcv, dpkKcv) {
                                        if (it) {
                                            launch { AppPreference.saveLogin(true) }
                                            if (keWithInit) {
                                                startInit()
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
                                    }*/
                           return RespMessageStatusData("Success Key Init", true)
                                } else {
                                    //Result.error("Key Injection fail","")
                                   return RespMessageStatusData("Key exchange error", false)
                                }
                            }
                    }
                else {
                return keyExchangeStatus
            }
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