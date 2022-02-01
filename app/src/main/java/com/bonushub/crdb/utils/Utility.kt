
package com.bonushub.crdb.utils

import android.app.Activity
import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Build
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import com.bonushub.crdb.BuildConfig
import com.bonushub.crdb.HDFCApplication
import com.bonushub.crdb.MainActivity
import com.bonushub.crdb.R
import com.bonushub.crdb.db.AppDatabase
import com.bonushub.crdb.di.DBModule
import com.bonushub.crdb.di.DBModule.appDatabase
import com.bonushub.crdb.di.scope.BHFieldParseIndex
import com.bonushub.crdb.model.local.*
import com.bonushub.crdb.repository.GenericResponse
import com.bonushub.crdb.transactionprocess.CreateTransactionPacket
import com.bonushub.crdb.view.base.IDialog
import com.bonushub.crdb.viewmodel.TransactionViewModel
import com.bonushub.pax.utils.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.io.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList


/**
 * =========Written By Ajay Thakur (18th Nov 2020)==========
 **/

val LYRA_IP_ADDRESS = "192.168.250.10"
var PORT2 = 4124
//203.112.151.169, port = 8109

val NEW_IP_ADDRESS ="203.112.151.169"//"192.168.250.10"/*"192.168.250.10"*/ //"203.112.151.169"//
var PORT =8109//4124//// /*9101*//*4124*/8109

 //val appDatabase by lazy { AppDatabase.getDatabase(HDFCApplication.appContext) }

var isExpanded = false
var isMerchantPrinted = false
var isDashboardOpen = false
val simpleTimeFormatter = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
val simpleDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
private val dbObj: AppDatabase = AppDatabase.getInstance(HDFCApplication.appContext)

class Utility @Inject constructor(appDatabase: AppDatabase)  {

    init {
        println("Utility has got a name as $appDatabase")
    }


    constructor() : this(appDatabase){

    }
    var list   = ArrayList<String>()
    var listTidType   = ArrayList<String>()
    var listLinkTidType   = ArrayList<String>()

    //region======================Read Local Init File======================
    suspend fun readLocalInitFile(callback: suspend (Boolean, String) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            var reader: BufferedReader? = null
            try {
                reader =
                    BufferedReader(InputStreamReader(HDFCApplication.appContext.assets.open("init_file.txt")))
                var mLine = reader.readLine()
                while (mLine != null) {
                    logger("readInitFile", mLine)
                    if (mLine.isNotEmpty()) {
                        val splitter = mLine.split("|")
                        if (splitter.isNotEmpty()) {
                            saveToDB(splitter)
                        }
                    }
                    mLine = reader.readLine()
                }
                GlobalScope.launch(Dispatchers.Main) {
                    callback(true, "")
                }
            } catch (ex: Exception) {
                GlobalScope.launch(Dispatchers.Main) {
                    logger("readInitFile", ex.message ?: "", "e")
                    callback(false, ex.message ?: "")
                }
            } finally {
                reader?.close()
            }
        }
    }
//endregion

    //region======================Read Host Response and Write in Init File===========
    suspend fun readInitServer(data: ArrayList<ByteArray>, callback: (Boolean, String) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val filename = "init_file.txt"
                HDFCApplication.appContext.openFileOutput(filename, Context.MODE_PRIVATE).apply {
                    for (each in data) write(each)
                    flush()
                }.close()

                val fin =
                    BufferedReader(
                        InputStreamReader(
                            HDFCApplication.appContext.openFileInput(
                                filename
                            )
                        )
                    )

                var line: String? = fin.readLine()

                while (line != null) {
                    if (line.isNotEmpty()) {
                        logger("readInitServer", line)
                        val spilter = line.split("|")
                        if (spilter.isNotEmpty()) {
                            if (AppPreference.getInt("PcNo") <= Integer.parseInt(spilter[0])) {
                                AppPreference.saveInt("PcNo", Integer.parseInt(spilter[0]))
                            }
                            saveToDB(spilter)
                        }
                    }
                    line = fin.readLine()
                }
                fin.close()
                GlobalScope.launch(Dispatchers.Main) {
                    callback(true, "Successful init")
                }
            } catch (ex: Exception) {
                GlobalScope.launch(Dispatchers.Main) {
                    callback(false, ex.message ?: "")
                }
            }
        }
    }
//endregion


    //region===========================Save DB Table Data
    suspend fun saveToDB(splitter: List<String>) {
        val funTag = "saveToDB"
        logger(funTag, splitter[2])
        savePcs(splitter[0], splitter[2])
        when {
            splitter[2] == "101" -> {
                val terminalCommunicationTable = TerminalCommunicationTable()
                parseTableData(terminalCommunicationTable, splitter) {
                    GlobalScope.launch(Dispatchers.IO) {
                        var insertStatus = 0L
                        when (terminalCommunicationTable.actionId) {
                            "1", "2" -> {
                                insertStatus = appDatabase?.appDao
                                    ?.insertTerminalCommunicationDataInTable(
                                        terminalCommunicationTable
                                    )
                                    ?: 0L
                            }
                            "3" -> appDatabase?.appDao
                                ?.deleteTerminalCommunicationTable(terminalCommunicationTable)
                        }
                        Log.d("TCT Insert:- ", insertStatus.toString())
                    }
                }
            }
            splitter[2] == "102" -> {
                val issuerParameterTable = IssuerParameterTable()
                parseTableData(issuerParameterTable, splitter) {
                    GlobalScope.launch(Dispatchers.IO) {
                        var insertStatus = 0L
                        when (issuerParameterTable.actionId) {
                            "1", "2" -> {
                                insertStatus =
                                    appDatabase?.appDao?.insertIssuerDataInTable(
                                        issuerParameterTable
                                    )
                                        ?: 0L
                            }
                            "3" -> appDatabase?.appDao?.deleteIssuerParameterTable(
                                issuerParameterTable
                            )
                        }
                        Log.d("Issuer Insert:- ", insertStatus.toString())
                    }
                }
            }
            splitter[2] == "106" -> {
                val terminalParameterTable = TerminalParameterTable()
                parseTableData(terminalParameterTable, splitter) {
                    GlobalScope.launch(Dispatchers.IO) {
                        var insertStatus = 0L
                        when (terminalParameterTable.actionId) {
                            "1", "2" -> {
                                terminalParameterTable.stan = "000001"

                                try {
                                    //Check for Enabling BANK EMI Enquiry on terminal from reservedValues .
                                    if (terminalParameterTable.reservedValues[6] == '1' ) {
                                        terminalParameterTable.bankEnquiry = "1"
                                        //Check for Enabling Phone number at the time of EMI Enquiry on terminal by reservedValues check
                                        terminalParameterTable.bankEnquiryMobNumberEntry =
                                            terminalParameterTable.reservedValues[7].toString().toInt() == 1
                                    }
                                    //Check for Enabling BRAND EMI Enquiry on terminal from reservedValues .
                                    if (terminalParameterTable.reservedValues[10] == '1' ) {
                                        terminalParameterTable.bankEnquiry = "1"
                                        //Check for Enabling Phone number at the time of EMI Enquiry on terminal by reservedValues check
                                        terminalParameterTable.bankEnquiryMobNumberEntry =
                                            terminalParameterTable.reservedValues[7].toString().toInt() == 1
                                    }

                                } catch (ex: Exception) {
                                    //ex.printStackTrace()
                                    println("Exception in brand catalogue display on dashboard")
                                } finally {
                                    insertStatus = appDatabase?.appDao
                                        ?.insertTerminalParameterDataInTable(terminalParameterTable)
                                        ?: 0L
                                    }
                            }
                            "3" -> appDatabase?.appDao
                                ?.deleteTerminalParameterTable(terminalParameterTable)
                        }
                        Log.d("TPT Insert:- ", insertStatus.toString())
                    }
                }
            }
            splitter[2] == "107" -> {
                val cardDataTable = CardDataTable()
                parseTableData(cardDataTable, splitter) {
                    GlobalScope.launch(Dispatchers.IO) {
                        var insertStatus = 0L
                        when (cardDataTable.actionId) {
                            "1", "2" -> {
                                insertStatus =
                                    appDatabase?.appDao?.insertCardDataInTable(cardDataTable) ?: 0L
                            }
                            "3" -> appDatabase?.appDao?.deleteCardDataTable(cardDataTable)
                        }
                        Log.d("CDT Insert:- ", insertStatus.toString())
                    }
                }
            }
            splitter[2] == "201" -> {  // HDFC TPT
                val hdfcTpt = HDFCTpt()
                parseTableData(hdfcTpt, splitter) {
                    GlobalScope.launch(Dispatchers.IO) {
                        var insertStatus = 0L
                        when (hdfcTpt.actionId) {
                            "1", "2" -> {
                                insertStatus =
                                    appDatabase?.appDao?.insertHDFCTPTDataInTable(hdfcTpt) ?: 0L
                            }
                            "3" -> appDatabase?.appDao?.deleteHDFCTPT(hdfcTpt)
                        }
                        Log.d("HDFC TPT Insert:- ", insertStatus.toString())
                    }
                }
            }
            splitter[2] == "202" -> {   // HDFC CDT/IPT-->(Cantains both parameters of HDFC CDT and IPT table)
                val hdfcCdt = HDFCCdt()
                parseTableData(hdfcCdt, splitter) {
                    GlobalScope.launch(Dispatchers.IO) {
                        var insertStatus = 0L
                        when (hdfcCdt.actionId) {
                            "1", "2" -> {
                                insertStatus =
                                    appDatabase?.appDao?.insertHDFCCDTInTable(hdfcCdt) ?: 0L
                            }
                            "3" -> appDatabase?.appDao?.deleteHDFCCDT(hdfcCdt)
                        }
                        Log.d("HDFC CDT Insert:- ", insertStatus.toString())
                    }
                }
            }
            splitter[2] == "203" -> {
                val wifiCtTable = WifiCommunicationTable()
                parseTableData(wifiCtTable, splitter) {
                    GlobalScope.launch(Dispatchers.IO) {
                        var insertStatus = 0L
                        when (wifiCtTable.actionId) {
                            "1", "2" -> {
                                insertStatus =
                                    appDatabase?.appDao?.insertWifiCTTable(wifiCtTable) ?: 0L
                            }
                            "3" -> appDatabase?.appDao?.deleteWifiCT(wifiCtTable)
                        }
                        Log.d("Wifi CDT Insert:- ", insertStatus.toString())
                    }
                }
            }
        }
    }
//endregion

    //region============================================Save PCS
    private val pc2Tables = arrayOf(108, 110, 111, 112, 113, 114, 115, 116)

    //For Terminal Purpose
    private val pc1Tables = arrayOf(101, 102, 107, 106, 109)


    /**
     * savePcs takes take pc number and table id and as per table id
     * it save largest pc number 1 and 2 in the system.
     * */

    private fun savePcs(pcNum: String, table: String) {
        try {
            val tn = table.toInt()
            if (tn in pc2Tables) {
                val ppc = AppPreference.getString(PreferenceKeyConstant.PC_NUMBER_TWO.keyName).toInt()
                if (pcNum.toInt() > ppc) {
                    AppPreference.saveString(PreferenceKeyConstant.PC_NUMBER_TWO.keyName, pcNum)
                }
            }
            if (tn in pc1Tables) {
                val ppc = AppPreference.getString(PreferenceKeyConstant.PC_NUMBER_ONE.keyName).toInt()
                if (pcNum.toInt() > ppc) {
                    AppPreference.saveString(PreferenceKeyConstant.PC_NUMBER_ONE.keyName, pcNum)
                }
            }
        } catch (ex: Exception) {
            try {
                val tn = table.toInt()
                if (tn in pc2Tables) {
                    AppPreference.saveString(PreferenceKeyConstant.PC_NUMBER_TWO.keyName, pcNum)
                } else {
                    AppPreference.saveString(PreferenceKeyConstant.PC_NUMBER_ONE.keyName, pcNum)
                }
            } catch (ex: Exception) {
            }
        }
    }
//endregion

    //region===================================Below method is used to Parse Table Data:-
    fun parseTableData(tableName: Any, dataList: List<String>, tableDataCb: (Any) -> Unit) {
        if (dataList.isNotEmpty()) {
            val tableClass = tableName::class.java
            for (e in tableClass.declaredFields) {
                val ann = e.getAnnotation(BHFieldParseIndex::class.java)
                if (ann != null) {
                    val index = ann.index
                    if (dataList.size > index) {
                        e.isAccessible = true
                        e.set(tableName, dataList[index])

                    /*    if(dataList[2]== "106" && index == 4){
                            var string = String()
                            for(i in dataList[index]){
                                println("Index for TID "+i)
                                string = string+i
                            }
                            println("Index for TID1 "+string)
                            list.add(string)
                            println("Index for TID2 "+list)
                            e.set(tableName, list)
                        }
                        else if(dataList[2]== "106" && index == 60){
                            var string = String()
                            for(i in dataList[index]){
                                println("Index for TID Type "+i)
                                string = string+i
                            }
                            println("Index for TID Type1 "+string)
                            listTidType.add(string)
                            println("Index for TID Type2 "+listTidType)
                            e.set(tableName, listTidType)
                        }
                        else if(dataList[2]== "106" && index == 64){
                            var string = String()
                            for(i in dataList[index]){
                                println("Index for LinkTID Type "+i)
                                string = string+i
                            }
                            println("Index for TID LinkTIDType1 "+string)
                            listLinkTidType.add(string)
                            println("Index for TID LinkTIDType2 "+listLinkTidType)
                            e.set(tableName, listLinkTidType)
                        }
                        else {
                            e.set(tableName, dataList[index])

                        }*/
                    }
                }
            }
            print(Gson().toJson(tableName))
            tableDataCb(tableName)
        }
    }
//endregion

    //region========================Increment ROC===============
    fun incrementUpdateRoc() {
        var increasedRoc = 0
        val roc = runBlocking(Dispatchers.IO) {
            appDatabase?.appDao?.getUpdateRoc("1")
        }
        println("Incremented roc value "+roc)
        if (!TextUtils.isEmpty(roc) && roc?.toInt() != 0) {
            increasedRoc = roc?.toInt()?.plus(1) ?: 0
            if (increasedRoc > 999999) {
                increasedRoc = 1
                appDatabase?.appDao?.updateStan(
                    addPad(increasedRoc, "0", 6, true),
                    TableType.TERMINAL_PARAMETER_TABLE.code,"1"
                )
            } else {
                appDatabase?.appDao?.updateStan(
                    addPad(increasedRoc, "0", 6, true),
                    TableType.TERMINAL_PARAMETER_TABLE.code,"1"
                )
            }
        }
    }
    //endregion

    //region========================Increment ROC===============
    fun incrementRoc() {
        var increasedRoc = 0
        val roc = appDatabase?.appDao?.getRoc()
        if (!TextUtils.isEmpty(roc) && roc?.toInt() != 0) {
            increasedRoc = roc?.toInt()?.plus(1) ?: 0
            if (increasedRoc > 999999) {
                increasedRoc = 1
                appDatabase?.appDao?.updateRoc(
                    addPad(increasedRoc, "0", 6, true),
                    TableType.TERMINAL_PARAMETER_TABLE.code
                )
            } else {
                appDatabase?.appDao?.updateRoc(
                    addPad(increasedRoc, "0", 6, true),
                    TableType.TERMINAL_PARAMETER_TABLE.code
                )
            }
        }
    }
   //endregion

    fun getROC(): String? {
        return runBlocking(Dispatchers.IO) { appDatabase?.appDao?.getRoc() }
    }

    //region========================Reset ROC===============
    fun resetRoc() =
        appDatabase?.appDao?.clearRoc(
            addPad(1, "0", 6, true),
            TableType.TERMINAL_PARAMETER_TABLE.code
        )
//endregion

    //region========================Get Invoice=================
    fun getInvoice(): String? {
        return runBlocking(Dispatchers.IO) { appDatabase?.appDao?.getInvoice() }
    }
//endregion

    //region========================Increment ROC===============
    fun incrementInvoice() {
        var increaseInvoice = 0
        val invoice = appDatabase?.appDao?.getInvoice()
        if (!TextUtils.isEmpty(invoice) && invoice?.toInt() != 0) {
            increaseInvoice = invoice?.toInt()?.plus(1) ?: 0
            appDatabase?.appDao?.updateInvoice(
                addPad(increaseInvoice, "0", 6, true),
                TableType.TERMINAL_PARAMETER_TABLE.code
            )
        }
    }
//endregion

    //region========================Increment Invoice===============
    fun incrementUpdateInvoice() {
        var increaseInvoice = 0
        val invoice = runBlocking(Dispatchers.IO) {
            appDatabase?.appDao?.getUpdatedInvoice("1")
        }
        println("Incremented Invoice value "+invoice)
        if (!TextUtils.isEmpty(invoice) && invoice?.toInt() != 0) {
            increaseInvoice = invoice?.toInt()?.plus(1) ?: 0
            if (increaseInvoice > 999999) {
                increaseInvoice = 1
                appDatabase?.appDao?.updatedInvoice(
                    addPad(increaseInvoice, "0", 6, true),
                    TableType.TERMINAL_PARAMETER_TABLE.code,"1"
                )
            }
            else {
                appDatabase?.appDao?.updatedInvoice(
                    addPad(increaseInvoice, "0", 6, true),
                    TableType.TERMINAL_PARAMETER_TABLE.code, "1"
                )
            }
        }
    }
//endregion

    //region======================Reset Invoice Number==============
    fun resetInvoiceNumber() = appDatabase?.appDao
        ?.clearInvoice(addPad(1, "0", 6, true), TableType.TERMINAL_PARAMETER_TABLE.code)
//endregion

    //region=======================Increment Batch Number================
    fun incrementBatchNumber() {
        var increaseBatch = 0
        val batch = runBlocking(Dispatchers.IO) {
            appDatabase?.appDao?.getUpdatedBatchNumber("1")
        }
        println("Incremented Batch number "+batch)
        if (!TextUtils.isEmpty(batch) && batch?.toInt() != 0) {
            increaseBatch = batch?.toInt()?.plus(1) ?: 0
            if (increaseBatch > 999999) {
                increaseBatch = 1
                appDatabase?.appDao?.updatedBatchNumber(
                    addPad(increaseBatch, "0", 6, true),
                    TableType.TERMINAL_PARAMETER_TABLE.code,"1"
                )
            }
            else {
                appDatabase?.appDao?.updatedBatchNumber(
                    addPad(increaseBatch, "0", 6, true),
                    TableType.TERMINAL_PARAMETER_TABLE.code, "1"
                )
            }
        }
    }
//endregion

    //region======================Reset Batch Number==============
    fun resetBatchNumber() = appDatabase?.appDao
        ?.clearBatchNumber(addPad(1, "0", 6, true), TableType.TERMINAL_PARAMETER_TABLE.code)
//endregion

    //region=========================Get Pax Device Modal Name:-
    fun getDeviceModelName(): String? {
        val service = DeviceHelper.getDeviceModel()
        if (service != null) {
            return when {
                service.length > 6 -> service.takeLast(6)
                service.length < 6 -> addPad(service, " ", 6, false)
                else -> service
            }
        }
        return service
    }
//endregion

    //region==========================Get Pax Device Serial Number:-
    fun getDeviceSerialNumber(): String? {
        //  val service = NeptuneService.dal.sys.termInfo[ETermInfoKey.SN] ?: ""
        val service = DeviceHelper.getDeviceSerialNo()
        if (service != null) {
            return when {
                service.length > 15 -> service.substring(service.length - 15, service.length)
                service.length < 15 -> addPad(service, " ", 15)
                else -> service
            }
        }

        return service
    }
//endregion

    //region=======Logging============
    @JvmOverloads
    fun logger(tag: String, msg: String, type: String = "d") {
        if (BuildConfig.DEBUG) {
            when (type) {
                "d", "D" -> Log.d(tag, msg)
                "i", "I" -> Log.i(tag, msg)
                "e", "E" -> Log.e(tag, msg)
                "v", "V" -> Log.v(tag, msg)
                else -> Log.i(tag, msg)
            }
        }
    }

    // For logging
    fun logger(tag: String, msg: HashMap<Byte, IsoField>, type: String = "d") {
        if (BuildConfig.DEBUG) {
            for ((k, v) in msg) {
                logger(v.fieldName + "---->>", "$k = ${v.rawData}", type)
            }
        }
    }

//endregion


    // region ===ConnectionTimestamp==========
    object ConnectionTimeStamps {
        var identifier: String = ""
        var dialStart = ""
        var dialConnected = ""
        var startTransaction = ""
        var recieveTransaction = ""
        private var stamp = "~~~~"

        init {
            stamp = AppPreference.getString(AppPreference.F48_STAMP)
        }

        fun reset() {
            identifier = ""
            dialStart = ""
            dialConnected = ""
            startTransaction = ""
            recieveTransaction = ""
        }

        fun saveStamp() {
            stamp = getFormattedStamp()
            AppPreference.saveString(AppPreference.F48_STAMP, stamp)
            reset()
        }

        fun getFormattedStamp(): String =
            "$identifier~$startTransaction~$recieveTransaction~$dialStart~$dialConnected"

        fun saveStamp(f48: String) {
            identifier = f48.split("~")[0]
            saveStamp()
        }


        fun getStamp(): String = if (stamp.isNotEmpty()) stamp else "~~~~"

        fun getOtherInfo(): String {
            return "~${HDFCApplication.networkStrength}~${""}~${HDFCApplication.imeiNo}~${HDFCApplication.simNo}~${HDFCApplication.operatorName}"
        }
    }
//endregion



    //region====================INTERNET CONNECTION CHECK:-
    fun checkInternetConnection(): Boolean {
        val cm =
            HDFCApplication.appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting
    }
//endregion

    //region ====helping methods for DB=======
    fun getTctData(): TerminalCommunicationTable? {
        var tctData: TerminalCommunicationTable? = null
        runBlocking(Dispatchers.IO) {
            tctData = appDatabase.appDao.getTerminalCommunicationTableData()?.get(0)
        }
        return tctData
    }
//endregion

    //region======================Get TPT Data:-  // already present in line no 1250
    fun getTptData(): TerminalParameterTable? {
        var tptData: TerminalParameterTable? = null
        runBlocking(Dispatchers.IO) {
            tptData = appDatabase?.appDao?.getAllTerminalParameterTableData()?.get(0)
        }
        return tptData
    }
//endregion

    //region===========================Get HDFCTPT Data:-
    fun getHDFCTptData(): HDFCTpt? {
        val hdfcTpt = runBlocking(Dispatchers.IO) { appDatabase?.appDao?.getAllHDFCTPTTableData() }
        return if (hdfcTpt?.size ?: 0 > 0)
            runBlocking(Dispatchers.IO) { appDatabase?.appDao?.getAllHDFCTPTTableData()?.get(0) }
        else
            null
    }
//endregion

    fun getCDTData(recordType:String): TerminalCommunicationTable? {
        var cdtData: TerminalCommunicationTable? = null
        runBlocking(Dispatchers.IO) {
            cdtData = appDatabase.appDao.getTerminalCommunicationTableByRecordType(recordType)
        }
        return cdtData
    }


    // region===========================Get HDFCTPT Data:-
    fun getHDFCCDTData(): HDFCCdt? {
        val hdfcTpt = runBlocking(Dispatchers.IO) { appDatabase?.appDao?.getAllHDFCCDTTableData() }
        return if (hdfcTpt?.size ?: 0 > 0)
            runBlocking(Dispatchers.IO) { appDatabase?.appDao?.getAllHDFCCDTTableData()?.get(0) }
        else
            null
    }
//endregion

// region ============getIpPort===========

/*    fun getIpPort(): InetSocketAddress? {
        val tct = getTptData()
        return if (tct != null) {
            InetSocketAddress(InetAddress.getByName(NEW_IP_ADDRESS), PORT)
        } else {
            InetSocketAddress(InetAddress.getByName(NEW_IP_ADDRESS), PORT)
        }
    }*/

    /*    fun checkInternetConnection(): Boolean {
        val cm =
            VerifoneApp.appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting
    }*/


//region=====================================Get IP Port:-
    fun getIpPort(isAppUpdate:Boolean=false,isPrimaryIpPort:Int=1): InetSocketAddress? {
        val txnCpt = getCDTData("1")
        val appUpdateCpt=getCDTData("2")
        if(isAppUpdate){
            return when {
                appUpdateCpt!=null -> {
                    if(isPrimaryIpPort==1) {
                        InetSocketAddress(
                            InetAddress.getByName(appUpdateCpt.hostPrimaryIp),
                            appUpdateCpt.hostPrimaryPortNo.toInt()
                        )
                    }
                    else{
                        InetSocketAddress(
                            InetAddress.getByName(appUpdateCpt.hostSecIp),
                            appUpdateCpt.hostSecPortNo.toInt()
                        )
                    }
                }
                else -> {
                    if (txnCpt != null) {
                        if(isPrimaryIpPort==1) {
                            InetSocketAddress(
                                InetAddress.getByName(txnCpt.hostPrimaryIp),
                                txnCpt.hostPrimaryPortNo.toInt()
                            )
                        }else{
                            InetSocketAddress(
                                InetAddress.getByName(txnCpt.hostSecIp),
                                txnCpt.hostSecPortNo.toInt()
                            )
                        }
                    } else {
                        InetSocketAddress(InetAddress.getByName(NEW_IP_ADDRESS), PORT)
                    }
                }
            }
        }else{
            return if (txnCpt != null) {
                if(isPrimaryIpPort==1) {
                    InetSocketAddress(
                        InetAddress.getByName(txnCpt.hostPrimaryIp),
                        txnCpt.hostPrimaryPortNo.toInt()
                    )
                }else{
                    InetSocketAddress(
                        InetAddress.getByName(txnCpt.hostSecIp),
                        txnCpt.hostSecPortNo.toInt()
                    )
                }
            } else {
                InetSocketAddress(InetAddress.getByName(NEW_IP_ADDRESS), PORT)
            }

        }

    }

//endregion

    open class OnTextChange(private val cb: (String) -> Unit) : TextWatcher {

        override fun afterTextChanged(s: Editable?) {
            cb(s.toString())
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

    }


    //region==============================Method to Hide Soft System Keyboard of Android Device:-
    fun hideSoftKeyboard(activity: Activity) {
        try {
            val ims = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            var view = activity.currentFocus

            if (view == null) {
                view = View(activity)
            }
            ims.hideSoftInputFromWindow(view.windowToken, 0)
        } catch (ex: Exception) {
        }
    }
//endregion

    //region===============================Below method is used to show Invoice with Padding:-
    fun invoiceWithPadding(invoiceNo: String) =
        addPad(input = invoiceNo, padChar = "0", totalLen = 6, toLeft = true)
//endregion

    //region=================================Get Current Date and Time Data:-
    fun getCurrentDateTime(): Pair<String?, String?> {
        val date: Long = Calendar.getInstance().timeInMillis
        var currentDate: String? = null
        var currentTime: String? = null

        try {
            currentDate = simpleDateFormatter.format(date)
            currentTime = simpleTimeFormatter.format(date)
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
            return Pair(currentDate, currentTime)
        }
        return Pair(currentDate, currentTime)
    }
//endregion

    //region=====================================GET Transaction Type Name:-
    fun getTransactionTypeName(type: Int): String? {
        var name: String? = null
        name = when (type) {
            BhTransactionType.SALE.type -> BhTransactionType.SALE.txnTitle
            BhTransactionType.SALE_WITH_CASH.type -> BhTransactionType.SALE_WITH_CASH.txnTitle
            BhTransactionType.CASH_AT_POS.type -> BhTransactionType.CASH_AT_POS.txnTitle
            BhTransactionType.PRE_AUTH.type -> BhTransactionType.PRE_AUTH.txnTitle
            BhTransactionType.PRE_AUTH_COMPLETE.type -> BhTransactionType.PRE_AUTH_COMPLETE.txnTitle
            BhTransactionType.PENDING_PREAUTH.type -> BhTransactionType.PENDING_PREAUTH.txnTitle
            BhTransactionType.VOID.type -> BhTransactionType.VOID.txnTitle
            BhTransactionType.REFUND.type -> BhTransactionType.REFUND.txnTitle
            BhTransactionType.VOID_REFUND.type -> BhTransactionType.VOID_REFUND.txnTitle
            BhTransactionType.EMI.type -> BhTransactionType.EMI.txnTitle
            BhTransactionType.EMI_SALE.type -> BhTransactionType.EMI_SALE.txnTitle
            BhTransactionType.TIP_SALE.type -> BhTransactionType.TIP_SALE.txnTitle
            else -> "NONE"
        }
        return name
    }
//endregion


    //region==============================================Getting NII from Terminal Communication Table:-
    fun getNII(): String {
        return runBlocking {
            appDatabase?.appDao?.getTerminalCommunicationTableData()?.get(0)?.nii ?: "0000"
        }
    }
//endregion

    //region==========================Navigate Fragment from Any Fragment to Dashboard Fragment:-
    fun switchToDashboard(context: Context) {
        context.startActivity(Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        })
    }
//endregion

    // region ========== converting value in BCD format:-
    fun convertValue2BCD(optionValue: String): String {
        val optionsBinaryValue = optionValue.toInt(16).let { Integer.toBinaryString(it) }
        return addPad(optionsBinaryValue, "0", 8, toLeft = true)
    }
//endregion

    //region==================Below method is used to convert 12 length String to 6 bit Byte Array:-
    fun convertStr2Nibble2Str(data: String): String {
        var tempData = ""
        val splitData = data.chunked(2)
        for (i in splitData.indices) {
            if (splitData[i].toInt() != 0) {
                val convertData = str2NibbleArr(splitData[i])[0].toString()
                if (convertData.length == 1)
                    tempData = "${tempData}0${convertData}"
                else
                    tempData += convertData
                continue
            }
            tempData += splitData[i]
        }
        return tempData
    }
//endregion

    //region=========================Below method to check HDFC TPT Fields Check:-
    fun checkHDFCTPTFieldsBitOnOff(bhTransactionType: BhTransactionType): Boolean {
        val hdfcTPTData = runBlocking(Dispatchers.IO) { getHDFCTptData() }
        Log.d("HDFC TPT:- ", hdfcTPTData.toString())
        var data: String? = null
        if (hdfcTPTData != null) {
            when (bhTransactionType) {
                BhTransactionType.VOID -> {
                    data = convertValue2BCD(hdfcTPTData.localTerminalOption)
                    return data[1] == '1' // checking second position of data for on/off case
                }
                BhTransactionType.REFUND -> {
                    data = convertValue2BCD(hdfcTPTData.localTerminalOption)
                    return data[2] == '1' // checking third position of data for on/off case
                }
                BhTransactionType.TIP_ADJUSTMENT -> {
                    data = convertValue2BCD(hdfcTPTData.localTerminalOption)
                    return data[3] == '1' // checking fourth position of data for on/off case
                }
                BhTransactionType.TIP_SALE -> {
                    data = convertValue2BCD(hdfcTPTData.option1)
                    return data[2] == '1' // checking third position of data for on/off case
                }
                else -> {
                }
            }
        }

        return false
    }
//endregion

    //region=========================Below method to check HDFC CDT Fields Check:-
    fun checkForAccountTypeDialog(panNumber: String): Boolean {
        val hdfcCDTData = appDatabase?.appDao?.getCardDataByHDFCCDTPanNumber(panNumber)
        Log.d("HDFC TPT:- ", hdfcCDTData.toString())
        var data: String? = null
        return if (hdfcCDTData != null) {
            data = convertValue2BCD(hdfcCDTData.option1)
            data[0] == '1' // checking first position of data for on/off case
        } else
            true
    }
//endregion

    //region Check for Type of Transaction Allowed or not on Pan:-
//region=========================Below method to check HDFC CDT Fields Check:-
    fun checkForTransactionAllowedOrNot(
        panNumber: String,
        bhTransactionType: BhTransactionType
    ): Boolean {
        val hdfcCDTData = appDatabase?.appDao?.getCardDataByHDFCCDTPanNumber(panNumber)
        Log.d("HDFC TPT:- ", hdfcCDTData.toString())
        var data: String? = null
        return if (hdfcCDTData != null) {
            when (bhTransactionType) {
                BhTransactionType.TIP_ADJUSTMENT -> {
                    data = convertValue2BCD(hdfcCDTData.option1)
                    data[7] == '1' // checking eight position of data for on/off case
                }
                BhTransactionType.PRE_AUTH -> {
                    data = convertValue2BCD(hdfcCDTData.option2)
                    data[7] == '1' // checking eight position of data for on/off case
                }
                BhTransactionType.REFUND -> {
                    data = convertValue2BCD(hdfcCDTData.option2)
                    data[6] == '1' // checking seventh position of data for on/off case
                }
                BhTransactionType.SALE_WITH_CASH -> {
                    data = convertValue2BCD(hdfcCDTData.option3)
                    data[1] == '1' // checking second position of data for on/off case
                }
                BhTransactionType.CASH_AT_POS -> {
                    data = convertValue2BCD(hdfcCDTData.option3)
                    data[6] == '1' // checking seventh position of data for on/off case
                }
                else -> true
            }
        } else
            true
    }
//endregion

    //region===============Check for Sign Print on Charge Slip in Transaction or not:-
    fun checkForSignPrintOrNot(panNumber: String): Boolean {
        val hdfcCDTData = appDatabase?.appDao?.getCardDataByHDFCCDTPanNumber(panNumber)
        Log.d("HDFC TPT:- ", hdfcCDTData.toString())
        var data: String? = null
        return if (hdfcCDTData != null) {
            data = convertValue2BCD(hdfcCDTData.option3)
            data[2] == '1' // checking third position of data for on/off case
        } else
            false
    }
//endregion

    // region===============Check for Sign Print on Charge Slip in Transaction or not:-
    fun checkForNoRefundPrint(panNumber: String): Boolean {
        val hdfcCDTData = appDatabase?.appDao?.getCardDataByHDFCCDTPanNumber(panNumber)
        Log.d("HDFC TPT:- ", hdfcCDTData.toString())
        var data: String? = null
        return if (hdfcCDTData != null) {
            data = convertValue2BCD(hdfcCDTData.option3)
            data[3] == '1' // checking fourth position of data for on/off case
        } else
            false
    }
//endregion

    // region===============Check for Void of Transaction Allowed ot Not:-
    fun checkForVoidAllowedOrNot(panNumber: String): Boolean {
        val hdfcCDTData = appDatabase?.appDao?.getCardDataByHDFCCDTPanNumber(panNumber)
        Log.d("HDFC TPT:- ", hdfcCDTData.toString())
        var data: String? = null
        return if (hdfcCDTData != null) {
            data = convertValue2BCD(hdfcCDTData.option3)
            data[0] != '1' // checking first position of data for on/off case
        } else
            false
    }
//endregion

//region ============================Get System DateTime in millis:-
/*fun getSystemDateTimeInMillis(): String {
    return NeptuneService.dal.sys.date ?: ""
}*/
//endregion

// region ============================Get System Time in 24Hour Format:-
/*fun getSystemTimeIn24Hour(): String {
    return NeptuneService.dal.sys.date.substring(8, NeptuneService.dal.sys.date.length - 2)
}*/
//endregion


/* Below Point Need to Implement When Manual Sale is Implemented and Also we need to Check Luhn Check:-
* short fChkLuhn--------------optionTwo[0] & 0x01-------------default we have to check--NA;
* short fExpDtReqd-------------optionOne[0] & 0x08;              expiry date required in case of manual sale//
* short fCheckExpDt----------------optionTwo[0] & 0x10;--------validate expiry date
* short fManEntry-----------------optionOne[0] & 0x04;// need to check manual sale -
*
*  */


    fun dateFormater(date: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)

    fun timeFormater(date: Long): String =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(date)


    //region==================Below method is used to read App Revision ID from saved File in Terminal:-
    fun readAppRevisionIDFromFile(context: Context, cb: (String) -> Unit) {
        var revisionID: String? = null
        try {
            val file = File(context.externalCacheDir, "version.txt")
            val text: StringBuilder? = null
            val br = BufferedReader(FileReader(file))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                text?.append(line)
                text?.append('\n')
                revisionID = line.toString()
            }
            Log.d("DataList:- ", revisionID.toString())
            br.close().toString()
            cb(revisionID ?: "")
        } catch (ex: IOException) {
            ex.printStackTrace()
            cb(revisionID ?: "")
        }
    }
//endregion


    //region=======================DataParser According to Splitter Provided in Method and Return MutableList:-
    fun parseDataListWithSplitter(splitterType: String, data: String): MutableList<String> {
        var dataList = mutableListOf<String>()
        when (splitterType) {
            SplitterTypes.CLOSED_CURLY_BRACE.splitter -> {
                dataList = data.split(splitterType) as MutableList<String>
            }
            SplitterTypes.VERTICAL_LINE.splitter -> {
                dataList = data.split(splitterType) as MutableList<String>
            }
            SplitterTypes.CARET.splitter -> {
                dataList = data.split(splitterType) as MutableList<String>
            }
        }

        return dataList
    }
//endregion

    //region============================Method to return BrandEMIMasterCategory Data in Triple Return Type:-
    fun getBrandEMIMasterCategoryTimeStampsData(): Pair<String, String> {
        val data =
            runBlocking(Dispatchers.IO) { appDatabase?.appDao?.getAllBrandEMIMasterCategoryData() }
        return if (data?.size ?: 0 > 0) {
            Pair(data?.get(0)?.issuerTAndCTimeStamp ?: "", data?.get(0)?.brandTAndCTimeStamp ?: "")
        } else
            Pair("", "")
    }
//region

    // region============================Method to return BrandEMIMasterCategory Data categoryUpdatedTimeStamps:-
    fun getBrandEMIMasterSubCategoryUpdatedTimeStamps(): String {
        val data =
            runBlocking(Dispatchers.IO) { appDatabase?.appDao?.getAllBrandEMIMasterCategoryData() }
        return if (data?.size ?: 0 > 0) {
            return data?.get(0)?.brandCategoryUpdatedTimeStamp ?: ""
        } else
            ""
    }
//region


    fun getBtry(): Int {
        return if (Build.VERSION.SDK_INT >= 21) {
            val bm = HDFCApplication.appContext.getSystemService(BATTERY_SERVICE) as BatteryManager
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } else {
            logger("Btry StatusError", "Error in Btry percent")
            0
        }
    }

    suspend fun syncPendingTransaction(transactionViewModel:TransactionViewModel,cb:(Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {

            com.bonushub.crdb.utils.logger(
                "syncPendingTransaction",
                " ----------------------->  START",
                "e"
            )
            var txnSync = true
            val pendingTxn = appDatabase.appDao.getAllPendingSyncTransactionData()

            if (pendingTxn.size != 0) {

                for (item in pendingTxn) {
                    val transactionISO =
                        CreateTransactionPacket(
                            appDatabase.appDao,
                            item.cardProcessedDataModal,
                            item.batchTable
                        ).createTransactionPacket()

                    when (val genericResp = transactionViewModel.serverCall(transactionISO)) {
                        is GenericResponse.Success -> {
                            com.bonushub.crdb.utils.logger(
                                "success:- ",
                                "in success ${genericResp.errorMessage}",
                                "e"
                            )
                            // to remove transaction after sync
                            appDatabase.appDao.deletePendingSyncTransactionData(item)
                        }
                        is GenericResponse.Error -> {
                            txnSync = false
                            com.bonushub.crdb.utils.logger(
                                "error:- ",
                                "in error ${genericResp.errorMessage}",
                                "e"
                            )
                            com.bonushub.crdb.utils.logger("error:- ", "try in next time", "e")

                        }
                        is GenericResponse.Loading -> {
                            com.bonushub.crdb.utils.logger(
                                "Loading:- ",
                                "in Loading ${genericResp.errorMessage}",
                                "e"
                            )
                        }
                    }
                }
            }
            withContext(Dispatchers.Main){
                cb(txnSync)
            }
        }
    }

    /*fun creatCardProcessingModelData(receiptDetail: ReceiptDetail):CardProcessedDataModal {
        var globalCardProcessedModel = CardProcessedDataModal()

        globalCardProcessedModel.setTransType(receiptDetail.)

        globalCardProcessedModel.setProcessingCode("920001")
        receiptDetail.txnAmount?.let { globalCardProcessedModel.setTransactionAmount(it.toLong()) }
        receiptDetail.txnOtherAmount?.let { globalCardProcessedModel.setOtherAmount(it.toLong()) }
        globalCardProcessedModel.setMobileBillExtraData(Pair(mobileNumber, billNumber))
        receiptDetail.stan?.let { globalCardProcessedModel.setAuthRoc(it) }
        globalCardProcessedModel.setCardMode("0553- emv with pin")
        globalCardProcessedModel.setRrn(receiptDetail.rrn)
        receiptDetail.authCode?.let { globalCardProcessedModel.setAuthCode(it) }
        globalCardProcessedModel.setTid(receiptDetail.tid)
        globalCardProcessedModel.setMid(receiptDetail.mid)
        globalCardProcessedModel.setBatch(receiptDetail.batchNumber)
        globalCardProcessedModel.setInvoice(receiptDetail.invoice)
        val date = receiptDetail.dateTime
        val parts = date?.split(" ")
        globalCardProcessedModel.setDate(parts!![0])
        globalCardProcessedModel.setTime(parts[1])
        globalCardProcessedModel.setTimeStamp(receiptDetail.dateTime!!)
        globalCardProcessedModel.setPosEntryMode("0553")
        receiptDetail.maskedPan?.let { globalCardProcessedModel.setPanNumberData(it) }

        return globalCardProcessedModel
    }*/

}

// Field 48 connection time stamp and other info
object Field48ResponseTimestamp {
    var identifier = ""
    var oldSuccessTransDate = ""

    fun saveF48IdentifierAndTxnDate(f48: String): String {
        identifier = f48.split("~")[0]
        oldSuccessTransDate = getF48TimeStamp()
        val value = "$identifier~$oldSuccessTransDate"
        AppPreference.saveString(AppPreference.F48IdentifierAndSuccesssTxn, value)
        Log.e("IDTXNDATE", value)
        return value
    }

    fun getF48Data(): String {
        val idTxnDate = AppPreference.getString(AppPreference.F48IdentifierAndSuccesssTxn)
        val identifier = idTxnDate.split("~")[0]
        val startTran = getF48TimeStamp()
        var receiveTransTime = ""
        if (idTxnDate != "") {
            receiveTransTime = idTxnDate.split("~")[1]
        }
        val dialStart = getF48TimeStamp()
        val dialConnect = getF48TimeStamp()
        val timeStamp = "${identifier}~${startTran}~${receiveTransTime}~${dialStart}~${dialConnect}"
        Log.e("timeStamp", timeStamp)
        val otherInfo = Utility.ConnectionTimeStamps.getOtherInfo()
        return timeStamp + otherInfo
    }

    // region ============================Get System Time in 24Hour Format:-
    fun getSystemTimeIn24Hour(): String {
        val calendar = Calendar.getInstance()
        val dateFormatter = SimpleDateFormat("yyyMMddHH:mm:ss", Locale.getDefault())
        return dateFormatter.format(calendar.time).replaceTimeColon()
    }
//endregion

    //region============= ROC, ConnectionTime========
    fun getF48TimeStamp(): String {
        val currentTime = Calendar.getInstance().time
        val sdf = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault())
        return sdf.format(currentTime)
    }

    //region=================Format Date by replacing colon:-
    fun String.replaceTimeColon() = this.replace(":", "")
//endregion




    fun checkInternetConnection(): Boolean {
        val cm =
            HDFCApplication.appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting
    }
/*
    //region == this fun for getting battry from vfService
    fun getbatteryinfo(context: Context): String? {
        return try {
            VFService.vfDeviceService?.deviceInfo?.batteryLevel
        } catch (e: Exception) {
            getbatteryinfoAndroid(context)
        }
    }
    //endregion*/

    //region === this is for getting battery from android
    fun getbatteryinfoAndroid (context: Context):String{
        // Call battery manager service
        val bm = context.getSystemService(BATTERY_SERVICE) as BatteryManager
        // Get the battery percentage and store it in a INT variable
        val batLevel:Int = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return batLevel.toString()
    }
    //endregion

    //region  ==== this fun is return charger is connected or not
    fun getChargerStatus(context: Context):Boolean{
        val batteryStatus: Intent? =
            IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                context.registerReceiver(null, ifilter)
            }
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        var isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL
        val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val usbCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
        val acCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_AC
        when {
            usbCharge -> {

                isCharging=true
            }
            acCharge -> {
                isCharging=true
            }

            else -> {
                isCharging=false
            }
        }
        return isCharging
    }

    //endregion

    //Below method is used to show Toast on UI Thread:-
    fun showToast(message: String) {
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(HDFCApplication.appContext, message, Toast.LENGTH_SHORT).show()
        }

    }
    //getHDFCTptData
    suspend fun getHDFCTptData(): HDFCTpt? {
        val hdfcTptRecords =  dbObj.appDao.getAllHDFCTPTTableData()
        return if (!hdfcTptRecords.isNullOrEmpty())
            hdfcTptRecords[0]
        else null
    }
    //endregion
    // region ========== converting value in BCD format:-
    fun convertValue2BCD(optionValue: String): String {
        val optionsBinaryValue = optionValue.toInt(16).let { Integer.toBinaryString(it) }
        return addPad(optionsBinaryValue, "0", 8, toLeft = true)
    }
    //endregion

    //region======================Get TPT Data:-
    fun getTptData(): TerminalParameterTable? {
        var tptData: TerminalParameterTable? = null
        runBlocking(Dispatchers.IO) {
            tptData = appDatabase.appDao?.getTerminalParameterTableDataByTidType("1")
            val jsonResp=Gson().toJson(tptData)
            println(jsonResp)
        }
        return tptData
    }
//endregion

    //region======================Get TPT Data:-
    fun getBatchDataByInvoice(invoice:String): BatchTable? {
        var batchTable: BatchTable? = null
        runBlocking(Dispatchers.IO) {
            batchTable = appDatabase.appDao?.getBatchDataFromInvoice(invoice)
            val jsonResp=Gson().toJson(batchTable)
            println(jsonResp)
        }
        return batchTable
    }
//endregion

    //region======================Get TPT Data:-
    fun getTptDataByLinkTidType(linkTidType:String): TerminalParameterTable? {
        var tptData: TerminalParameterTable? = null
        runBlocking(Dispatchers.IO) {
            tptData = appDatabase.appDao?.getTerminalParameterTableDataByLinkTidType(linkTidType)
            val jsonResp=Gson().toJson(tptData)
            println(jsonResp)
        }
        return tptData
    }
//endregion

    //region======================Get TPT Data By terminal id:-
    fun getTptDataByTid(tid: String): TerminalParameterTable? {
        var tptData: TerminalParameterTable? = null
        runBlocking(Dispatchers.IO) {
            tptData = appDatabase.appDao?.getTerminalParameterTableDataByTid(tid)
            val jsonResp=Gson().toJson(tptData)
            println("Data in tpt ---->"+jsonResp)
        }
        return tptData
    }
//endregion


//
// region======================Get TPT Data:-
    fun getAllTptData(): ArrayList<TerminalParameterTable?> {
        var tptData = ArrayList<TerminalParameterTable?>()
        runBlocking(Dispatchers.IO) {
            tptData = DBModule.appDatabase.appDao?.getTerminalParameterTableData() as ArrayList<TerminalParameterTable?>
            val jsonResp=Gson().toJson(tptData)
            println(jsonResp)
        }
        return tptData
    }
//endregion

    //region== insert digipos data
    fun insertOrUpdateDigiposData(param: DigiPosDataTable){
        runBlocking(Dispatchers.IO) {
            val effectedRow = appDatabase.appDao.insertOrUpdateDigiposData(
                param
            )
            logger("effectedRow",""+effectedRow)
        }

    }

    fun deleteDigiposData(digiPosDataTable: DigiPosDataTable){
        runBlocking(Dispatchers.IO) {
            val effectedRow = appDatabase.appDao.deleteDigiposData(digiPosDataTable)
            logger("effectedRow",""+effectedRow)
        }

    }

    fun deleteDigiposData(partnerTxnId: String){
        runBlocking(Dispatchers.IO) {
            val digiPosDataTable = DigiPosDataTable(partnerTxnId = partnerTxnId)
            val effectedRow = appDatabase.appDao.deleteDigiposData(digiPosDataTable)
            logger("effectedRow",""+effectedRow)
        }

    }

    fun selectAllDigiPosData():ArrayList<DigiPosDataTable?>{

        var digiPosData = ArrayList<DigiPosDataTable?>()
        runBlocking(Dispatchers.IO) {
            digiPosData = DBModule.appDatabase.appDao.getAllDigiposData() as ArrayList<DigiPosDataTable?>
            val jsonResp=Gson().toJson(digiPosData)
            println(jsonResp)
        }
        return digiPosData
    }

    fun selectDigiPosDataAccordingToTxnStatus(status: String):ArrayList<DigiPosDataTable?>{

        var digiPosData: java.util.ArrayList<DigiPosDataTable?>
        runBlocking(Dispatchers.IO) {
            digiPosData = DBModule.appDatabase.appDao.getDigiPosDataTableByTxnStatus(status) as ArrayList<DigiPosDataTable?>
            val jsonResp=Gson().toJson(digiPosData)
            println(jsonResp)
        }
        return digiPosData
    }



    // region
    fun isTipEnable(): Boolean {
        var isTipEnable: Boolean = false
        runBlocking(Dispatchers.IO){
            val result = DBModule.appDatabase.appDao.getIngenicoInitialization()

            if(result != null && result.size > 0)
            {
                if(result.get(0)?.initdataList != null && result.get(0)?.initdataList!!.isNotEmpty()){
                    logger("isTipEnable",""+result[0]?.initdataList!![0].isTipEnable)
                    isTipEnable = result[0]?.initdataList!![0].isTipEnable ?:false
                }
            }
        }

        return isTipEnable
    }
    // end region

    // region
    fun performOperation(tpt:TerminalParameterTable, callback: () -> Unit){

        runBlocking(Dispatchers.IO){

            when(tpt.actionId) {
                "1", "2" -> {
                    appDatabase.appDao.insertTerminalParameterDataInTable(tpt) ?: 0L
                }
                "3" -> appDatabase.appDao.deleteTerminalParameterTable(tpt)

                else ->{

                }
            }

            callback()
        }

    }
    // end region

    // region
    fun getInitdataList(): InitDataListList? {
        var initdataListItem: InitDataListList? = null
        runBlocking(Dispatchers.IO){
            val result = DBModule.appDatabase.appDao.getIngenicoInitialization()

            if(result != null && result.size > 0)
            {
                if(result.get(0)?.initdataList != null && result.get(0)?.initdataList!!.isNotEmpty()){
                    logger("isTipEnable",""+result[0]?.initdataList!![0].isTipEnable)
                    initdataListItem = result[0]?.initdataList!![0]
                }
            }
        }

        return initdataListItem
    }
    // end region
//region======================Get brand tnc Data:-
fun getBrandTAndCDataByBrandId(brandId : String): String {
    var brandTANDC: String? = null
    runBlocking(Dispatchers.IO) {
         brandTANDC  =  appDatabase?.appDao?.getBrandTAndCDataById(brandId)
        val jsonResp=Gson().toJson(brandTANDC)
        println(jsonResp)
    }
    return brandTANDC?:""
}
//endregion


    //region======================Get issuer tnc Data:-
    fun getIssuerTAndCDataByIssuerId(issuerId: String ): IssuerTAndCTable? {
        var issuerTANDC:
                IssuerTAndCTable?= null
        runBlocking(Dispatchers.IO) {
            issuerTANDC  =  appDatabase?.appDao?.getAllIssuerTAndCDataById(issuerId)
            val jsonResp=Gson().toJson(issuerTANDC)
            println(jsonResp)
        }
        return issuerTANDC
    }
//endregion

    //region======================Get brand  Data:-
    fun getBrandTAndCData(): MutableList<BrandTAndCTable?>? {
        var brandTANDC: MutableList<BrandTAndCTable?>? = null
        runBlocking(Dispatchers.IO) {
            brandTANDC  =  appDatabase?.appDao?.getAllBrandTAndCData()
            val jsonResp=Gson().toJson(brandTANDC)
            println(jsonResp)
        }
        return brandTANDC
    }
//endregion


    //region======================Get TPT Data:-
    fun getIssuerData(issuerId:String): IssuerParameterTable? {
        var issuerData: IssuerParameterTable? = null
        runBlocking(Dispatchers.IO) {
            issuerData = DBModule.appDatabase.appDao.getIssuerTableDataByIssuerID(issuerId)
            val jsonResp=Gson().toJson(issuerData)
            println(jsonResp)
        }
        return issuerData
    }
//endregion
//region======================Get issuer Data:-
fun getAllIssuerData(): MutableList<IssuerParameterTable?>? {
    var issuerData: MutableList<IssuerParameterTable?>? = null
    runBlocking(Dispatchers.IO) {
          issuerData = DBModule.appDatabase.appDao.getAllIssuerTableData()
        val jsonResp=Gson().toJson(issuerData)
        println(jsonResp)
    }
    return issuerData
}
//endregion
//region======================Get issuer Data:-
fun getAllBrandEMIMasterDataTimeStamps(): List<BrandEMIMasterTimeStamps>? {
    var issuerData: List<BrandEMIMasterTimeStamps>? = null
    runBlocking(Dispatchers.IO) {
        issuerData = DBModule.appDatabase.appDao.getBrandEMIDateTimeStamps()
        val jsonResp=Gson().toJson(issuerData)
        println(jsonResp)
    }
    return issuerData
}
//endregion

    fun maxAmountLimitDialog(iDialog: IDialog?, maxTxnLimit:Double){
        GlobalScope.launch(Dispatchers.Main) {
            val msg=  "Max txn limit Rs ${("%.2f".format((maxTxnLimit)))}"

            iDialog?.getInfoDialog("Amount Limit", msg) {}
        }

    }
    //region=========================Below method to check HDFC TPT Fields Check:-

//endregion

    // region bank functions

    // end region

    fun transactionType2Name(code: Int): String {
        return when (code) {
            BhTransactionType.SALE.type -> "Sale"
            BhTransactionType.VOID.type -> "Void"
            BhTransactionType.VOID_REFUND.type -> "Void Refund"
            BhTransactionType.REFUND.type -> "Refund"
            BhTransactionType.PRE_AUTH.type -> "Pre-Auth"
            BhTransactionType.PRE_AUTH_COMPLETE.type -> "Auth Complete"
            BhTransactionType.VOID_PREAUTH.type -> "Void Pre-Auth"
            BhTransactionType.OFFLINE_SALE.type -> "Offline Sale"
            BhTransactionType.TIP_SALE.type -> "Tip Sale"
            BhTransactionType.SALE_WITH_CASH.type -> "Sale Cash"
            BhTransactionType.TIP_ADJUSTMENT.type -> "Tip Adjust"
            BhTransactionType.VOID_OFFLINE_SALE.type -> "Void Offline Sale"
            BhTransactionType.TEST_EMI.type -> "Test EMI Txn"
            BhTransactionType.BRAND_EMI.type, BhTransactionType.BRAND_EMI_BY_ACCESS_CODE.type , BhTransactionType.EMI_SALE.type -> "EMI Sale"
            BhTransactionType.CASH_AT_POS.type -> "Cash only"
            BhTransactionType.VOID_EMI.type -> "Void EMI"
            else -> "Unknown"
        }
    }

    fun panMasking(input: String, maskFormat: String): String {
        if (input.isNotEmpty()) {
            var mskF=""
            mskF = if(maskFormat.first()=='*'){
                "*****0000"
            }else{
                maskFormat
            }
            val maskCharArr = mskF.toCharArray()
            val inputArr = input.toCharArray()

            //  maskCharArr= charArrayOf('*','*','*','*','0','0','0','0',)
            // get all stars index
            val li = arrayListOf<Int>()
            for (e in maskCharArr.indices) {
                if (mskF[e] == '*') {
                    li.add(e)
                }
            }
            when {
                inputArr.size == maskCharArr.size -> for (e in li) {
                    inputArr[e] = '*'
                }
                inputArr.size > maskCharArr.size -> {
                    for (e in li.first()..(inputArr.lastIndex - li.last())) {
                        inputArr[e] = '*'
                    }
                }
                else -> for (e in 4..(inputArr.lastIndex - 4)) {
                    inputArr[e] = '*'
                }
            }
            val sb = StringBuilder()

            var index = 0
            /* while (index < inputArr.size) {
                 var endIndex = index + 3
                 if (endIndex > inputArr.lastIndex) {
                     endIndex = inputArr.lastIndex
                 }
                 val tempCh = inputArr.slice(index..endIndex)
                 sb.append(tempCh.toCharArray())
                 sb.append(" ")
                 index += 4
             }*/

            for(i in inputArr){
                sb.append(i)
            }

            //  return sb.toString().substring(0, sb.lastIndex)
            return sb.toString()
        } else return ""
    }

    //region=======================DataParser According to Splitter Provided in Method and Return MutableList:-
    fun parseDataListWithSplitter(splitterType: String, data: String): MutableList<String> {
        var dataList = mutableListOf<String>()
        when (splitterType) {
            SplitterTypes.CLOSED_CURLY_BRACE.splitter -> {
                dataList = data.split(splitterType) as MutableList<String>
            }
            SplitterTypes.VERTICAL_LINE.splitter -> {
                dataList = data.split(splitterType) as MutableList<String>
            }
            SplitterTypes.CARET.splitter -> {
                dataList = data.split(splitterType) as MutableList<String>
            }
        }

        return dataList
    }
//endregion



    fun setToolbarLogos(activity: Activity) {
        //Show Logo of Bank by checking Bank Code:-
        val tpt = getTptData()
        val bonushubLogo = activity.findViewById<ImageView>(R.id.main_toolbar_BhLogo)
        val bankLogoImageView = activity.findViewById<ImageView>(R.id.toolbar_Bank_logo)
        var bankLogo = 0

        when (AppPreference.getBankCode()) {
           // "07" -> bankLogo = R.drawable.amex_logo
            "01" -> bankLogo = R.drawable.ic_hdfcsvg
            else -> {
            }
        }

        //Show Both BonusHub and Bank Logo on base of condition check on tpt.reservedValues 10th Position:-
        if (tpt != null) {
            if (!TextUtils.isEmpty(tpt.reservedValues)) {
                if (tpt.reservedValues.length > 10) {
                    for (i in tpt.reservedValues.indices) {
                        if (i == 9) {
                            if (tpt.reservedValues[i].toString() == "1") {
                                bonushubLogo?.visibility = View.VISIBLE
                                bankLogoImageView?.setImageResource(bankLogo)
                                bankLogoImageView?.visibility = View.VISIBLE
                                break
                            } else {
                                bonushubLogo?.visibility = View.GONE
                                bankLogoImageView?.setImageResource(bankLogo)
                                bankLogoImageView?.visibility = View.VISIBLE
                            }
                        }
                    }
                } else {
                    bonushubLogo?.visibility = View.GONE
                    bankLogoImageView?.setImageResource(bankLogo)
                    bankLogoImageView?.visibility = View.VISIBLE
                }
            } else {
                bonushubLogo?.visibility = View.GONE
                bankLogoImageView?.setImageResource(bankLogo)
                bankLogoImageView?.visibility = View.VISIBLE
            }
        }

        if (!AppPreference.getLogin()) {
            bonushubLogo?.visibility = View.VISIBLE
            //   bankLogoImageView?.setImageResource(bankLogo)
            bankLogoImageView?.visibility = View.GONE

        }
    }


}
