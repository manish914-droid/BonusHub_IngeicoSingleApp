package com.bonushub.crdb.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.model.local.BatchTableReversal
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


}