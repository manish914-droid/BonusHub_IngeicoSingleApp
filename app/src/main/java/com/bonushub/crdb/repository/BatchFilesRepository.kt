package com.bonushub.crdb.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bonushub.crdb.di.DBModule
import com.bonushub.crdb.model.local.BatchFileDataTable
import kotlinx.coroutines.runBlocking

class BatchFilesRepository {

    companion object{

        @Synchronized
        fun getInstance():BatchFilesRepository{
            return BatchFilesRepository()
        }
    }


    fun getBatchTableData() : LiveData<MutableList<BatchFileDataTable?>>{
        val dataList = MutableLiveData<MutableList<BatchFileDataTable?>>()

        runBlocking {
            val table = DBModule.appDatabase?.appDao.getBatchTableData()

            if (table != null) {

                //  result.sortByDescending { invoice->(invoice.hostInvoice).toInt() }
                table.sortBy { it?.hostInvoice?.toInt() }
                table.reverse()
            }


            dataList.value = table
        }
        return dataList
    }


}