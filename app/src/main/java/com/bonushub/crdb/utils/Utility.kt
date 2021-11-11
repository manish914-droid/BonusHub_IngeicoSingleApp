
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
import android.widget.Toast
import com.bonushub.crdb.BuildConfig
import com.bonushub.crdb.HDFCApplication
import com.bonushub.crdb.MainActivity
import com.bonushub.crdb.db.AppDatabase
import com.bonushub.crdb.di.DBModule.appDatabase
import com.bonushub.crdb.di.scope.BHFieldParseIndex
import com.bonushub.crdb.model.local.*
import com.bonushub.pax.utils.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


/**
 * =========Written By Ajay Thakur (18th Nov 2020)==========
 **/

val LYRA_IP_ADDRESS = "192.168.250.10"
var PORT2 = 4124
val NEW_IP_ADDRESS = /*"203.112.151.169"*/"192.168.250.10"
var PORT = /*8109*/4124

 //val appDatabase by lazy { AppDatabase.getDatabase(HDFCApplication.appContext) }

var isExpanded = false
var isMerchantPrinted = false
var isDashboardOpen = false
val simpleTimeFormatter = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
val simpleDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)


class Utility @Inject constructor(appDatabase: AppDatabase)  {

    init {
        println("Utility has got a name as $appDatabase")
    }


    constructor() : this(appDatabase){

    }


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
                                insertStatus = appDatabase?.appDao
                                    ?.insertTerminalParameterDataInTable(terminalParameterTable)
                                    ?: 0L
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
                val ppc =
                    AppPreference.getString(PreferenceKeyConstant.PC_NUMBER_TWO.keyName).toInt()
                if (pcNum.toInt() > ppc) {
                    AppPreference.saveString(PreferenceKeyConstant.PC_NUMBER_TWO.keyName, pcNum)
                }
            }
            if (tn in pc1Tables) {
                val ppc =
                    AppPreference.getString(PreferenceKeyConstant.PC_NUMBER_ONE.keyName).toInt()
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
                    }
                }
            }
            print(Gson().toJson(tableName))
            tableDataCb(tableName)
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

    //region======================Reset Invoice Number==============
    fun resetInvoiceNumber() = appDatabase?.appDao
        ?.clearInvoice(addPad(1, "0", 6, true), TableType.TERMINAL_PARAMETER_TABLE.code)
//endregion

    //region=======================Increment Batch Number================
    fun incrementBatchNumber() {
        var increaseBatch = 0
        val batch = appDatabase?.appDao?.getBatchNumber()
        if (!TextUtils.isEmpty(batch) && batch?.toInt() != 0) {
            increaseBatch = batch?.toInt()?.plus(1) ?: 0
            appDatabase?.appDao?.updateBatchNumber(
                addPad(increaseBatch, "0", 6, true),
                TableType.TERMINAL_PARAMETER_TABLE.code
            )
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
            tctData = appDatabase?.appDao?.getTerminalCommunicationTableData()?.get(0)
        }
        return tctData
    }
//endregion

    //region======================Get TPT Data:-
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

    fun getCDTData(): TerminalCommunicationTable? {
        var cdtData: TerminalCommunicationTable? = null
        runBlocking(Dispatchers.IO) {
            cdtData = appDatabase?.appDao?.getTerminalCommunicationTableData()?.get(0)
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

    fun getIpPort(): InetSocketAddress? {
        val tct = getTptData()
        return if (tct != null) {
            InetSocketAddress(InetAddress.getByName(NEW_IP_ADDRESS), PORT)
        } else {
            InetSocketAddress(InetAddress.getByName(NEW_IP_ADDRESS), PORT)
        }
    }/*    fun checkInternetConnection(): Boolean {
        val cm =
            VerifoneApp.appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting
    }*/


//region=====================================Get IP Port:-
/*fun getIpPort(): InetSocketAddress {

    val cdt: TerminalCommunicationTable? = getCDTData()
    return if (cdt != null) {
        InetSocketAddress(InetAddress.getByName(cdt.hostPrimaryIp), cdt.hostPrimaryPortNo.toInt())
    } else {
        InetSocketAddress(InetAddress.getByName(LYRA_IP_ADDRESS), PORT2)
    }
}*/
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
            TransactionType.SALE.type -> TransactionType.SALE.txnTitle
            TransactionType.SALE_WITH_CASH.type -> TransactionType.SALE_WITH_CASH.txnTitle
            TransactionType.CASH_AT_POS.type -> TransactionType.CASH_AT_POS.txnTitle
            TransactionType.PRE_AUTH.type -> TransactionType.PRE_AUTH.txnTitle
            TransactionType.PRE_AUTH_COMPLETE.type -> TransactionType.PRE_AUTH_COMPLETE.txnTitle
            TransactionType.PENDING_PREAUTH.type -> TransactionType.PENDING_PREAUTH.txnTitle
            TransactionType.VOID.type -> TransactionType.VOID.txnTitle
            TransactionType.REFUND.type -> TransactionType.REFUND.txnTitle
            TransactionType.VOID_REFUND.type -> TransactionType.VOID_REFUND.txnTitle
            TransactionType.EMI.type -> TransactionType.EMI.txnTitle
            TransactionType.EMI_SALE.type -> TransactionType.EMI_SALE.txnTitle
            TransactionType.TIP_SALE.type -> TransactionType.TIP_SALE.txnTitle
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
    fun checkHDFCTPTFieldsBitOnOff(transactionType: TransactionType): Boolean {
        val hdfcTPTData = runBlocking(Dispatchers.IO) { getHDFCTptData() }
        Log.d("HDFC TPT:- ", hdfcTPTData.toString())
        var data: String? = null
        if (hdfcTPTData != null) {
            when (transactionType) {
                TransactionType.VOID -> {
                    data = convertValue2BCD(hdfcTPTData.localTerminalOption)
                    return data[1] == '1' // checking second position of data for on/off case
                }
                TransactionType.REFUND -> {
                    data = convertValue2BCD(hdfcTPTData.localTerminalOption)
                    return data[2] == '1' // checking third position of data for on/off case
                }
                TransactionType.TIP_ADJUSTMENT -> {
                    data = convertValue2BCD(hdfcTPTData.localTerminalOption)
                    return data[3] == '1' // checking fourth position of data for on/off case
                }
                TransactionType.TIP_SALE -> {
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
        transactionType: TransactionType
    ): Boolean {
        val hdfcCDTData = appDatabase?.appDao?.getCardDataByHDFCCDTPanNumber(panNumber)
        Log.d("HDFC TPT:- ", hdfcCDTData.toString())
        var data: String? = null
        return if (hdfcCDTData != null) {
            when (transactionType) {
                TransactionType.TIP_ADJUSTMENT -> {
                    data = convertValue2BCD(hdfcCDTData.option1)
                    data[7] == '1' // checking eight position of data for on/off case
                }
                TransactionType.PRE_AUTH -> {
                    data = convertValue2BCD(hdfcCDTData.option2)
                    data[7] == '1' // checking eight position of data for on/off case
                }
                TransactionType.REFUND -> {
                    data = convertValue2BCD(hdfcCDTData.option2)
                    data[6] == '1' // checking seventh position of data for on/off case
                }
                TransactionType.SALE_WITH_CASH -> {
                    data = convertValue2BCD(hdfcCDTData.option3)
                    data[1] == '1' // checking second position of data for on/off case
                }
                TransactionType.CASH_AT_POS -> {
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

}
