package com.bonushub.crdb.india.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bonushub.crdb.india.db.AppDao
import com.bonushub.crdb.india.model.local.BatchTableReversal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BatchReversalRepository @Inject constructor(private val appDao: AppDao){

    suspend fun getBatchTableReversalData() : LiveData<MutableList<BatchTableReversal>> {
        val dataList = MutableLiveData<MutableList<BatchTableReversal>>()

        withContext(Dispatchers.IO) {

        val table = appDao.getAllBatchReversalData()
        if (table != null) {

            table.reverse()
        }
        dataList.postValue(table)

        }
        return dataList
    }

    suspend fun deleteBatchReversalTable(){

        withContext(Dispatchers.IO) {
            appDao.deleteBatchReversalTable()
        }

    }


}