package com.bonushub.crdb.india.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bonushub.crdb.india.di.DBModule
import com.bonushub.crdb.india.model.local.HDFCTpt
import com.bonushub.crdb.india.model.local.TerminalParameterTable
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getTptData
import kotlinx.coroutines.runBlocking

class NewInputAmountRepository {
    companion object{

        @Synchronized
        fun getInstance():NewInputAmountRepository{
            return NewInputAmountRepository()
        }
    }

    fun getTerminalParameterTable(): LiveData<TerminalParameterTable> {

        val tpt  = MutableLiveData<TerminalParameterTable>()

        runBlocking {
            val table = getTptData()//DBModule.appDatabase?.appDao.getTerminalParameterTableData()
            try {
                tpt.value = table
            }catch (ex:Exception)
            {
                tpt.value = null
                ex.printStackTrace()
            }

        }
        return tpt
    }

    fun getHdfcTerminalParameterTable(): LiveData<HDFCTpt> {

        var hdfcTpt  = MutableLiveData<HDFCTpt>()

        runBlocking {
            var table = DBModule.appDatabase?.appDao.getAllHDFCTPTTableData()
            try {
                 if (!table.isNullOrEmpty())
                     hdfcTpt.value=  table[0]
            }catch (ex:Exception)
            {
                hdfcTpt.value = null
                ex.printStackTrace()
            }

        }
        return hdfcTpt
    }


}