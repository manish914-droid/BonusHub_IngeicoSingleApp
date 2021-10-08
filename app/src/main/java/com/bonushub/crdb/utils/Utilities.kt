/*
package com.bonushub.crdb.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.bonushub.crdb.BuildConfig
import com.bonushub.crdb.HDFCApplication
import com.bonushub.crdb.db.AppDatabase
import com.bonushub.crdb.di.scope.BHFieldParseIndex
import com.bonushub.crdb.model.*
import com.bonushub.crdb.model.local.AppPreference
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



var isExpanded = false
var isMerchantPrinted = false
var isDashboardOpen = false
val simpleTimeFormatter = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
val simpleDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)

val LYRA_IP_ADDRESS = "192.168.250.10"
var PORT2 = 4124
val NEW_IP_ADDRESS = "122.176.84.29"
var PORT = 8101//4124

class Utilities @Inject constructor(private val appDatabase: AppDatabase){

    fun doAThing1(): String{
        System.out.println("App data in Utils->"+appDatabase.appDao)
        return "Look I got:"
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
                    BufferedReader(InputStreamReader(HDFCApplication.appContext.openFileInput(filename)))

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
                                    ?.insertTerminalCommunicationDataInTable(terminalCommunicationTable)
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
                                    appDatabase?.appDao?.insertIssuerDataInTable(issuerParameterTable)
                                        ?: 0L
                            }
                            "3" -> appDatabase?.appDao?.deleteIssuerParameterTable(issuerParameterTable)
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
                                    ?.insertTerminalParameterDataInTable(terminalParameterTable) ?: 0L
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
                                insertStatus = appDatabase?.appDao?.insertHDFCCDTInTable(hdfcCdt) ?: 0L
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





   */
/* * savePcs takes take pc number and table id and as per table id
    * it save largest pc number 1 and 2 in the system.
    **//*




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
                    }
                }
            }
            print(Gson().toJson(tableName))
            tableDataCb(tableName)
        }
    }
//endregion


    fun getROC(): String? {
        return runBlocking(Dispatchers.IO) { appDatabase?.appDao?.getRoc() }
    }



    //region========================Get Invoice=================
    fun getInvoice(): String? {
        return runBlocking(Dispatchers.IO) { appDatabase?.appDao?.getInvoice() }
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



    //region============= ROC, ConnectionTime========
    fun getF48TimeStamp(): String {
        val currentTime = Calendar.getInstance().time
        val sdf = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault())
        return sdf.format(currentTime)
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

    fun getIpPort(): InetSocketAddress?{
        val tct = getTptData()
        return if (tct != null) {
            InetSocketAddress(InetAddress.getByName(""), tct.actionId.toInt())
        }else{
            InetSocketAddress(InetAddress.getByName(NEW_IP_ADDRESS), PORT)
        }
    }



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


    // Field 48 connection time stamp and other info
    object Field48ResponseTimestamp {
        var identifier = ""
        var oldSuccessTransDate = ""

        fun saveF48IdentifierAndTxnDate(f48: String): String {
            identifier = f48.split("~")[0]
           // oldSuccessTransDate = getF48TimeStamp()
            val value = "$identifier~$oldSuccessTransDate"
            AppPreference.saveString(AppPreference.F48IdentifierAndSuccesssTxn, value)
            Log.e("IDTXNDATE", value)
            return value
        }

    }



}
*/
