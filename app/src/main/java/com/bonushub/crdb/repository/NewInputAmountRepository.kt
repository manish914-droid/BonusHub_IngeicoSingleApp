package com.bonushub.crdb.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bonushub.crdb.di.DBModule
import com.bonushub.crdb.model.local.HDFCTpt
import com.bonushub.crdb.model.local.TerminalParameterTable
import kotlinx.coroutines.runBlocking

class NewInputAmountRepository {
    companion object{

        @Synchronized
        fun getInstance():NewInputAmountRepository{
            return NewInputAmountRepository()
        }
    }

    fun getTerminalParameterTable(): LiveData<TerminalParameterTable> {

        var tpt  = MutableLiveData<TerminalParameterTable>()

        runBlocking {
            var table = DBModule.appDatabase?.appDao.getTerminalParameterTableData()
            try {
                tpt.value = table.get(0)
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