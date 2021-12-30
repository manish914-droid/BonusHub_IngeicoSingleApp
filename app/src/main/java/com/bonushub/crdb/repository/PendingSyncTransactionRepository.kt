package com.bonushub.crdb.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.model.local.PendingSyncTransactionTable
import com.bonushub.crdb.utils.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PendingSyncTransactionRepository @Inject constructor(private val appDao: AppDao){

    suspend fun getAllPendingSyncTransactionTableData() : LiveData<MutableList<PendingSyncTransactionTable>> {

        val dataList = MutableLiveData<MutableList<PendingSyncTransactionTable>>()

        withContext(Dispatchers.IO) {

        val table = appDao.getAllPendingSyncTransactionData()
        dataList.postValue(table)

        }
        return dataList
    }

    suspend fun insertPendingSyncTransactionTable(pendingSyncTransactionTable :PendingSyncTransactionTable){

        withContext(Dispatchers.IO) {
            var insertRowNo = appDao.insertPendingSyncTransactionData(pendingSyncTransactionTable)
            logger("insertRowNo",""+insertRowNo,"e")
        }

    }

    suspend fun deletePendingSyncTransactionData(pendingSyncTransactionTable :PendingSyncTransactionTable){

        withContext(Dispatchers.IO) {
            var effectedRowNo = appDao.deletePendingSyncTransactionData(pendingSyncTransactionTable)
            logger("effectedRowNo",""+effectedRowNo,"e")
        }

    }

    suspend fun deletePendingSyncTransactionTable(){

        withContext(Dispatchers.IO) {
            appDao.deletePendingSyncTransactionTable()
        }

    }


}