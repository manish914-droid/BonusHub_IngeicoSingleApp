package com.bonushub.crdb.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bonushub.crdb.di.DBModule
import com.bonushub.crdb.di.scope.BHFieldParseIndex
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.model.local.TerminalCommunicationTable
import com.bonushub.crdb.model.local.TerminalParameterTable
import com.bonushub.crdb.view.fragments.TableEditHelper
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

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
                // Log.e("sap",""+tpt?.adminPassword)
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
               // Log.e("sap",""+tpt?.superAdminPassword)
            data.value =  tpt?.superAdminPassword.equals(password,true)
            }catch (ex:Exception){
                 data.value = false
            }
        }

        return data
    }

    suspend fun getTerminalParameterTableData() : LiveData<ArrayList<TableEditHelper?>>{
        val dataList = MutableLiveData<ArrayList<TableEditHelper?>>()

        //val data = MutableLiveData<MutableList<TerminalParameterTable?>>()

        var table = DBModule.appDatabase?.appDao.getTerminalParameterTableData()

       // val table: Any? = getTable()
        //val table: Any? = TerminalParameterTable
        if (table != null) {
            val props = table::class.java.declaredFields
            for (prop in props) {
                //val ann = prop.getAnnotation(BHFieldName::class.java)
                val ann2=prop.getAnnotation(BHFieldParseIndex::class.java)
               // if (ann != null && ann2 !=null && ann.isToShow) {
                if (ann2 !=null) {
                    prop.isAccessible = true
                    val fieldName = prop.name
                    val value = prop.get(table)
                    if (value is String) {
                        //dataList.add(TableEditHelper(ann.name, value,ann2.index))
                        dataList.value?.add(TableEditHelper(fieldName, value,ann2.index))
                    }
                    dataList.value?.sortBy { it?.index }
                    dataList.value?.forEach {  println(it?.titleName) }
                }
            }

            /*dataList.add(
                TableEditHelper(
                    "F Batch",
                    if (AppPreference.getBoolean(PrefConstant.SERVER_HIT_STATUS.keyName.toString()))
                        "1"
                    else
                        "0"
                )
            )*/
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
                dataList.value?.filter { dl -> requiredField.any { rf -> rf == dl?.titleName } } as ArrayList<TableEditHelper>
            dataList.value?.clear()
            dataList.value?.addAll(requiredList)


        }


        return dataList
    }

}