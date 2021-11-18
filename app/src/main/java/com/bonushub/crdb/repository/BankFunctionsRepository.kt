package com.bonushub.crdb.repository

import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bonushub.crdb.HDFCApplication.Companion.appContext
import com.bonushub.crdb.MainActivity
import com.bonushub.crdb.R
import com.bonushub.crdb.di.DBModule
import com.bonushub.crdb.di.scope.BHFieldName
import com.bonushub.crdb.di.scope.BHFieldParseIndex
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.model.local.TerminalCommunicationTable
import com.bonushub.crdb.model.local.TerminalParameterTable
import com.bonushub.crdb.utils.ToastUtils
import com.bonushub.crdb.utils.logger
import com.bonushub.crdb.view.fragments.TableEditHelper
import com.bonushub.pax.utils.PrefConstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class BankFunctionsRepository {

    companion object{

        @Synchronized
        fun getInstance():BankFunctionsRepository{
            return BankFunctionsRepository()
        }
    }

    fun isAdminPassword(password:String):LiveData<Boolean>{
        val data = MutableLiveData<Boolean>()

        // write logic whether password is correct or not
        runBlocking {
            var tpt = DBModule.appDatabase?.appDao?.getSingleRowTerminalParameterTableData()
            try {
                // logger("sap",""+tpt?.adminPassword)
                data.value =  tpt?.adminPassword.equals(password,true)
            }catch (ex:Exception){
                data.value = false
            }
        }

        return data
    }

    fun isSuperAdminPassword(password:String):LiveData<Boolean>{
        val data = MutableLiveData<Boolean>()

        // write logic whether super password is correct or not
        runBlocking {
            var tpt = DBModule.appDatabase?.appDao?.getSingleRowTerminalParameterTableData()
            try {
               // logger("sap",""+tpt?.superAdminPassword)

            data.value =  tpt?.superAdminPassword.equals(password,true)
            }catch (ex:Exception){
                 data.value = false
            }
        }

        return data
    }


    suspend fun getTerminalParameterTableData() : LiveData<ArrayList<TableEditHelper?>>{
        val dataList = MutableLiveData<ArrayList<TableEditHelper?>>()
        val dataListLocal = ArrayList<TableEditHelper?>()

        //val data = MutableLiveData<MutableList<TerminalParameterTable?>>()

        var table = DBModule.appDatabase?.appDao.getTerminalParameterTableData()

        if (table != null) {
            val props = TerminalParameterTable::class.java.declaredFields
            for (prop in props) {
                val ann = prop.getAnnotation(BHFieldName::class.java)
                val ann2=prop.getAnnotation(BHFieldParseIndex::class.java)

                if (ann != null && ann2 !=null && ann.isToShow) {
               // if (ann2 !=null) {
                    Log.e("ann.name",""+ann.name)
                    prop.isAccessible = true
                    //val fieldName = prop.name
                    try{
                        val value = prop.get(table.get(0))
                        if (value is String) {
                            dataListLocal?.add(TableEditHelper(ann.name, value,ann2.index))
                            //dataList.value?.add(TableEditHelper(fieldName, value,ann2.index))
                        }
                    }catch (ex:Exception){
                        ex.printStackTrace()
                    }

                    dataListLocal.sortBy { it?.index }
                    dataListLocal.forEach {  println(it?.titleName) }
                }
            }

            // region  remaining this
            dataListLocal.add(
                TableEditHelper(
                    "F Batch",
                    if (AppPreference.getBoolean(PrefConstant.SERVER_HIT_STATUS.keyName.toString()))
                        "1"
                    else
                        "0"
                )
            )//BB

            // end region
            //In Case Of AMEX only below arrayList items options are shown to user (In TPT table)
            val requiredField = arrayListOf(
                "TID",
                "MID",
                "STAN",
                "Batch Number",
                "Invoice Number",
                "F Batch"
            )
            val requiredList =
                dataListLocal.filter { dl -> requiredField.any { rf -> rf == dl?.titleName } } as ArrayList<TableEditHelper>
            dataListLocal.clear()
            dataListLocal.addAll(requiredList)


        }

        logger("dataList",""+dataListLocal?.size)

        dataList.postValue(dataListLocal)
        return dataList
    }

    suspend fun getTerminalParameterTable():LiveData<TerminalParameterTable>{

        val tpt  = MutableLiveData<TerminalParameterTable>()


        withContext(Dispatchers.IO) {
            val table = DBModule.appDatabase?.appDao.getTerminalParameterTableData()
            try {
                tpt.postValue(table.get(0))
            } catch (ex: Exception) {
                tpt.postValue(null)
                ex.printStackTrace()
            }
        }

        return tpt
    }

    suspend fun updateTerminalParameterTable(dataList: ArrayList<TableEditHelper?>)
    {
        val data = dataList.filter { it?.isUpdated?:false }
        //val table: Any? = getTable()

            val table: Any? = DBModule.appDatabase?.appDao.getTerminalParameterTableData().get(0)


        if (table != null) {
            if (data.isNotEmpty()) {
                data.forEach { ed ->
                    ed?.isUpdated = false
                    val props = table::class.java.declaredFields
                    for (prop in props) {
                        val ann = prop.getAnnotation(BHFieldName::class.java)
                        if (ann != null && ann.name == ed?.titleName) {
                            prop.isAccessible = true
                            val value = prop.get(table)
                            if (value is String) {
                                prop.set(table, ed.titleValue)

                            }
                        }
                    }
                }


// region remaining this region
                /*Condition to check whether terminal id is
                changed by user if so then we need to Navigate user to
                MainActivity and auto perform fresh init with new terminal id:-
                 */

                //Below conditional code will only execute in case of Change TID:- // BB
                if (data[0]?.titleName.equals("TID", ignoreCase = true)) {
                    if (data[0]?.titleValue != DBModule.appDatabase?.appDao.getTerminalParameterTableData().get(0)?.terminalId
                        && data[0]?.titleName.equals("TID", ignoreCase = true)
                    ) {
                        if (data[0]?.titleValue?.length == 8) {
                            //TerminalParameterTable.updateTerminalID(data[0]?.titleValue)
                            DBModule.appDatabase?.appDao.updateTerminalParameterTable(table as TerminalParameterTable)

                            withContext(Dispatchers.Main) {
                            appContext.startActivity(Intent(appContext, MainActivity::class.java).apply {
                                putExtra("changeTID", true)
                                flags =
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                        }
                        } else {
                            withContext(Dispatchers.Main){
                                ToastUtils.showToast(appContext, appContext.getString(R.string.enter_terminal_id_must_be_valid_8digit))
                            }
                        }
                    }
                }

                //Below conditional code will only execute in case of cLEAR FBATCH :- // BB
                if (data[0]?.titleName.equals("F BATCH", ignoreCase = true)) {
                    if (data[0]?.titleValue == "0" && data[0]?.titleName.equals(
                            "F BATCH",
                            ignoreCase = true
                        )
                    ) {
                        AppPreference.saveBoolean(
                            PrefConstant.SERVER_HIT_STATUS.keyName.toString(),
                            false
                        )
                    } else {
                        AppPreference.saveBoolean(
                            PrefConstant.SERVER_HIT_STATUS.keyName.toString(),
                            true
                        )
                    }
                }
// end region
                //logger("update",""+table.toString())
                DBModule.appDatabase?.appDao.updateTerminalParameterTable(table as TerminalParameterTable)
            } else logger("TAG", "No data to update is found")
        }
    }


    // comm param
    suspend fun getTerminalCommunicationTableByRecordType(redordType:String) : LiveData<ArrayList<TableEditHelper?>>{
        val dataList = MutableLiveData<ArrayList<TableEditHelper?>>()
        val dataListLocal = ArrayList<TableEditHelper?>()

        //val data = MutableLiveData<MutableList<TerminalParameterTable?>>()

        var table = DBModule.appDatabase?.appDao.getTerminalCommunicationTableByRecordType(redordType)

        if (table != null) {
            val props = TerminalCommunicationTable::class.java.declaredFields
            for (prop in props) {
                val ann = prop.getAnnotation(BHFieldName::class.java)
                val ann2=prop.getAnnotation(BHFieldParseIndex::class.java)

                if (ann != null && ann2 !=null && ann.isToShow) {
                    // if (ann2 !=null) {
                    logger("ann.name",""+ann.name)
                    prop.isAccessible = true
                    //val fieldName = prop.name
                    try{
                        val value = prop.get(table.get(0))
                        if (value is String) {
                            dataListLocal?.add(TableEditHelper(ann.name, value,ann2.index))
                            //dataList.value?.add(TableEditHelper(fieldName, value,ann2.index))
                        }
                    }catch (ex:Exception){
                        ex.printStackTrace()
                    }

                    dataListLocal.sortBy { it?.index }
                    dataListLocal.forEach {  println(it?.titleName) }
                }
            }

        }

        logger("dataList",""+dataListLocal?.size)

        dataList.postValue(dataListLocal)
        return dataList
    }

    suspend fun updateTerminalCommunicationTable(dataList: ArrayList<TableEditHelper?>, recordType:String)
    {
        val data = dataList.filter { it?.isUpdated?:false }
        //val table: Any? = getTable()

        val table: Any? = DBModule.appDatabase?.appDao.getTerminalCommunicationTableByRecordType(recordType).get(0)


        if (table != null) {
            if (data.isNotEmpty()) {
                data.forEach { ed ->
                    ed?.isUpdated = false
                    val props = table::class.java.declaredFields
                    for (prop in props) {
                        val ann = prop.getAnnotation(BHFieldName::class.java)
                        if (ann != null && ann.name == ed?.titleName) {
                            prop.isAccessible = true
                            val value = prop.get(table)
                            if (value is String) {
                                prop.set(table, ed.titleValue)

                            }
                        }
                    }
                }


// region remaining this region
                /*Condition to check whether terminal id is
                changed by user if so then we need to Navigate user to
                MainActivity and auto perform fresh init with new terminal id:-
                 */

                //Below conditional code will only execute in case of Change TID:- // BB
                if (data[0]?.titleName.equals("TID", ignoreCase = true)) {
                    if (data[0]?.titleValue != DBModule.appDatabase?.appDao.getTerminalParameterTableData().get(0)?.terminalId
                        && data[0]?.titleName.equals("TID", ignoreCase = true)
                    ) {
                        if (data[0]?.titleValue?.length == 8) {
                            //TerminalParameterTable.updateTerminalID(data[0]?.titleValue)
                            DBModule.appDatabase?.appDao.updateTerminalCommunicationTable(table as TerminalCommunicationTable)

                            withContext(Dispatchers.Main) {
                                appContext.startActivity(Intent(appContext, MainActivity::class.java).apply {
                                    putExtra("changeTID", true)
                                    flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                })
                            }
                        } else {
                            withContext(Dispatchers.Main){
                                ToastUtils.showToast(appContext, appContext.getString(R.string.enter_terminal_id_must_be_valid_8digit))
                            }
                        }
                    }
                }

                //Below conditional code will only execute in case of cLEAR FBATCH :- // BB
                if (data[0]?.titleName.equals("F BATCH", ignoreCase = true)) {
                    if (data[0]?.titleValue == "0" && data[0]?.titleName.equals(
                            "F BATCH",
                            ignoreCase = true
                        )
                    ) {
                        AppPreference.saveBoolean(
                            PrefConstant.SERVER_HIT_STATUS.keyName.toString(),
                            false
                        )
                    } else {
                        AppPreference.saveBoolean(
                            PrefConstant.SERVER_HIT_STATUS.keyName.toString(),
                            true
                        )
                    }
                }
// end region
                //logger("update",""+table.toString())
                DBModule.appDatabase?.appDao.updateTerminalCommunicationTable(table as TerminalCommunicationTable)
            } else logger("TAG", "No data to update is found")
        }
    }
}