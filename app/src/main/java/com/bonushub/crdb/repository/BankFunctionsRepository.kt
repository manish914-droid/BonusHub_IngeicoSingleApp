package com.bonushub.crdb.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bonushub.crdb.HDFCApplication.Companion.appContext
import com.bonushub.crdb.R
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.di.DBModule
import com.bonushub.crdb.di.scope.BHFieldName
import com.bonushub.crdb.di.scope.BHFieldParseIndex
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.model.local.TerminalCommunicationTable
import com.bonushub.crdb.model.local.TerminalParameterTable
import com.bonushub.crdb.utils.ToastUtils
import com.bonushub.crdb.utils.checkBaseTid
import com.bonushub.crdb.utils.logger
import com.bonushub.crdb.utils.updateBaseTid
import com.bonushub.crdb.view.fragments.TableEditHelper
import com.bonushub.crdb.view.fragments.TidsListModel
import com.bonushub.pax.utils.PrefConstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BankFunctionsRepository @Inject constructor(private val appDao: AppDao) {

    suspend fun isAdminPassword(password:String):LiveData<Boolean>{
        val data = MutableLiveData<Boolean>()

        // write logic whether password is correct or not
        withContext(Dispatchers.IO){
            val tpt = appDao.getSingleRowTerminalParameterTableData()
            try {
                // logger("sap",""+tpt?.adminPassword)
                var adminPassword = tpt?.adminPassword?.substring(0,4)
                data.postValue(adminPassword.equals(password,true))
            }catch (ex:Exception){
                data.postValue(false)
            }
        }

        return data
    }

    suspend fun isSuperAdminPassword(password:String):LiveData<Boolean>{
        val data = MutableLiveData<Boolean>()

        // write logic whether super password is correct or not
        withContext(Dispatchers.IO) {
            val tpt = appDao.getSingleRowTerminalParameterTableData()
            try {
               // logger("sap",""+tpt?.superAdminPassword)

            data.postValue(tpt?.superAdminPassword.equals(password,true))
            }catch (ex:Exception){
                 data.postValue(false)
            }
        }

        return data
    }


    suspend fun getTerminalParameterTableData() : LiveData<ArrayList<TableEditHelper?>> {
        val dataList = MutableLiveData<ArrayList<TableEditHelper?>>()
        val dataListLocal = ArrayList<TableEditHelper?>()

        //val data = MutableLiveData<MutableList<TerminalParameterTable?>>()

        withContext(Dispatchers.IO) {

        var table = appDao.getTerminalParameterTableData()

            val props = TerminalParameterTable::class.java.declaredFields
            for (prop in props) {
                val ann = prop.getAnnotation(BHFieldName::class.java)
                val ann2 = prop.getAnnotation(BHFieldParseIndex::class.java)

                if (ann != null && ann2 != null && ann.isToShow) {
                    // if (ann2 !=null) {
                    logger("ann.name", "" + ann.name)
                    prop.isAccessible = true
                    //val fieldName = prop.name
                    try {
                        val value = prop.get(table.get(0))
                        if (value is String) {
                            dataListLocal?.add(TableEditHelper(ann.name, value, ann2.index))
                            //dataList.value?.add(TableEditHelper(fieldName, value,ann2.index))
                        } else if (ann.name.equals("TID")) {
                            val tids = checkBaseTid(DBModule.appDatabase?.appDao)
                            dataListLocal?.add(TableEditHelper(ann.name, tids.get(0), ann2.index))

                            logger("TID case", value.toString(), "e")
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }


                }
            }

            dataListLocal.sortBy { it?.index }
            dataListLocal.forEach { println(it?.titleName) }


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
            val table = appDao.getTerminalParameterTableData()
            try {
                tpt.postValue(table.get(0))
            } catch (ex: Exception) {
                tpt.postValue(null)
                ex.printStackTrace()
            }
        }

        return tpt
    }


    suspend fun updateTerminalParameterTable(dataList: ArrayList<TableEditHelper?>, context:Context):LiveData<Boolean> {
        val dataReturn = MutableLiveData<Boolean>()
        dataReturn.postValue(false)

        val data = dataList.filter { it?.isUpdated ?: false }
        //val table: Any? = getTable()

        withContext(Dispatchers.IO) {
        val table: Any? = appDao.getTerminalParameterTableData().get(0)

        if (table != null) {
            if (data.isNotEmpty()) {
                data.forEach { ed ->
                    ed?.isUpdated = false
                    val props = table::class.java.declaredFields
                    for (prop in props) {
                        val ann = prop.getAnnotation(BHFieldName::class.java)
                        if (ann != null && ann.name.equals(ed?.titleName)) {
                            prop.isAccessible = true
                            val value = prop.get(table)
                            logger("ann.name", "" + ann.name)
                            if (value is String) {
                                prop.set(table, ed?.titleValue)

                            } else if (ann.name.equals("TID")) {

                                // logger("TID case update",value.toString(),"e")
                                // logger("TID case update",ed?.titleValue?:"")

                                val tids = updateBaseTid(
                                    appDao,
                                    ed?.titleValue ?: ""
                                )

                                // logger("TID case update",tids.toString(),"e")

                                prop.set(table, tids)


                            }
                        }
                    }
                }


                /*Condition to check whether terminal id is
                changed by user if so then we need to Navigate user to
                MainActivity and auto perform fresh init with new terminal id:-
                 */

                //Below conditional code will only execute in case of Change TID:- // BB
                if (data[0]?.titleName.equals("TID", ignoreCase = true) && data[0]?.titleValue != "") {

                        if (data[0]?.titleValue?.length == 8) {
                            //TerminalParameterTable.updateTerminalID(data[0]?.titleValue)
                            appDao.updateTerminalParameterTable(table as TerminalParameterTable)

                            dataReturn.postValue(true)

                        } else {
                            withContext(Dispatchers.Main) {
                                ToastUtils.showToast(
                                    appContext,
                                    appContext.getString(R.string.enter_terminal_id_must_be_valid_8digit)
                                )
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

                //logger("update",""+table.toString())
                appDao.updateTerminalParameterTable(table as TerminalParameterTable)
            } else logger("TAG", "No data to update is found")
        }

        }
        return dataReturn
    }


    // comm param
    suspend fun getTerminalCommunicationTableByRecordType(redordType:String) : LiveData<ArrayList<TableEditHelper?>>{


        val dataList = MutableLiveData<ArrayList<TableEditHelper?>>()
        val dataListLocal = ArrayList<TableEditHelper?>()

        withContext(Dispatchers.IO) {
            //val data = MutableLiveData<MutableList<TerminalParameterTable?>>()

            var table = appDao.getTerminalCommunicationTableByRecordType(redordType)

            if (table != null) {
                val props = TerminalCommunicationTable::class.java.declaredFields
                for (prop in props) {
                    val ann = prop.getAnnotation(BHFieldName::class.java)
                    val ann2 = prop.getAnnotation(BHFieldParseIndex::class.java)

                    if (ann != null && ann2 != null && ann.isToShow) {
                        // if (ann2 !=null) {
                        logger("ann.name", "" + ann.name)
                        prop.isAccessible = true
                        //val fieldName = prop.name
                        try {
                            val value = prop.get(table.get(0))
                            if (value is String) {
                                dataListLocal?.add(TableEditHelper(ann.name, value, ann2.index))
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }

                        dataListLocal.sortBy { it?.index }
                        dataListLocal.forEach { println(it?.titleName) }
                    }
                }

            }

            logger("dataList", "" + dataListLocal?.size)
            dataList.postValue(dataListLocal)
        }

        return dataList
    }

    suspend fun updateTerminalCommunicationTable(dataList: ArrayList<TableEditHelper?>, recordType:String, context: Context):LiveData<Boolean>
    {
        val dataReturn = MutableLiveData<Boolean>()
        dataReturn.value = false

        withContext(Dispatchers.IO) {
            val data = dataList.filter { it?.isUpdated ?: false }
            //val table: Any? = getTable()

            val table: Any? = appDao.getTerminalCommunicationTableByRecordType(recordType)
                    .get(0)

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


                    /*Condition to check whether terminal id is
                changed by user if so then we need to Navigate user to
                MainActivity and auto perform fresh init with new terminal id:-
                 */

                    //Below conditional code will only execute in case of Change TID:- // BB
                    if (data[0]?.titleName.equals("TID", ignoreCase = true) && data[0]?.titleValue != "") {

                            if (data[0]?.titleValue?.length == 8) {
                                //TerminalParameterTable.updateTerminalID(data[0]?.titleValue)
                                DBModule.appDatabase?.appDao.updateTerminalCommunicationTable(table as TerminalCommunicationTable)

                                dataReturn.value = true

                            } else {
                                withContext(Dispatchers.Main) {
                                    ToastUtils.showToast(
                                        appContext,
                                        appContext.getString(R.string.enter_terminal_id_must_be_valid_8digit)
                                    )
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
                    //logger("update",""+table.toString())
                    appDao.updateTerminalCommunicationTable(table as TerminalCommunicationTable)
                } else logger("TAG", "No data to update is found")
            }

        }
        return dataReturn
    }

    suspend fun getAllTidsWithStatus():LiveData<ArrayList<TidsListModel>>{
        val dataList = MutableLiveData<ArrayList<TidsListModel>>()
        val tidsWithStatusList = ArrayList<TidsListModel>()

        withContext(Dispatchers.IO) {
            val tpt = appDao.getTerminalParameterTableData()

            val IngenicoInitializationTable = appDao.getIngenicoInitialization()

            val rseultsize = IngenicoInitializationTable?.size

            var tidType = tpt.get(0)?.tidType
            var linkTidType = tpt.get(0)?.LinkTidType
            var tids = tpt.get(0)?.terminalId
            var status = ArrayList<String>()

            if (rseultsize != null) {
                logger("IngenicoInitializationTable", "" + rseultsize)
                var statusList = IngenicoInitializationTable.get(0)?.tidStatusList
                var tidsStatusList = IngenicoInitializationTable.get(0)?.tidList

                tids?.forEachIndexed { index, value ->
                    tidsStatusList?.forEachIndexed { index2, value2 ->

                        if (value.equals(value2, true)) {
                            status.add(statusList?.get(index2) ?: "")
                        }

                    }
                }
            } else {
                logger("IngenicoInitializationTable", "" + rseultsize)
            }

            if (tidType?.size == linkTidType?.size) {
                for (i in 0 until tidType?.size!!) {

                    if (tidType[i].equals("1")) {
                        tidsWithStatusList.add(
                            TidsListModel(
                                tids?.get(i) ?: "",
                                "Base Tid",
                                status.get(i)
                            )
                        )
                    } else {
                        when (linkTidType?.get(0)) {

                            "0" -> {
                                tidsWithStatusList.add(
                                    TidsListModel(
                                        tids?.get(i) ?: "",
                                        "for Amex",
                                        status.get(i)
                                    )
                                )
                            }

                            "1" -> {
                                tidsWithStatusList.add(
                                    TidsListModel(
                                        tids?.get(i) ?: "",
                                        "DC type",
                                        status.get(i)
                                    )
                                )
                            }

                            "2" -> {
                                tidsWithStatusList.add(
                                    TidsListModel(
                                        tids?.get(i) ?: "",
                                        "offus Tid",
                                        status.get(i)
                                    )
                                )
                            }

                            else -> {
                                tidsWithStatusList.add(
                                    TidsListModel(
                                        tids?.get(i) ?: "",
                                        "${linkTidType?.get(i)} months onus",
                                        status.get(i)
                                    )
                                )
                            }

                        }
                    }
                }
            }

            dataList.postValue(tidsWithStatusList)
        }

        return dataList
    }
}