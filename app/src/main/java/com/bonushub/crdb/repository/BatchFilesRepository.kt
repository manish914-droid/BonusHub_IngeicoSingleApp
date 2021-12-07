package com.bonushub.crdb.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.model.local.BatchFileDataTable
import com.bonushub.crdb.model.local.BatchTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BatchFilesRepository @Inject constructor(private val appDao: AppDao){

    suspend fun getBatchTableData() : LiveData<MutableList<BatchFileDataTable?>> {
        val dataList = MutableLiveData<MutableList<BatchFileDataTable?>>()

        withContext(Dispatchers.IO) {

        val table = appDao.getBatchTableData()
        if (table != null) {

            table.sortBy { it?.hostInvoice?.toInt() }
            table.reverse()
        }
        dataList.postValue(table)

        }
        return dataList
    }

    suspend fun getBatchTableDataByInVoice(invoice: String?) : LiveData<BatchTable?>{
        val dataList = MutableLiveData<BatchTable?>()

        withContext(Dispatchers.IO) {
           // val table = appDao.getBatchTableDataByInvoice(invoice)
            val table = appDao.getBatchDataFromInvoice(invoice)
            dataList.postValue(table)
        }
        return dataList
    }

    /*suspend fun getBrandEMIDataTable(hostInvoice: String?, hostTid: String) : LiveData<BrandEMIDataTable?>{
        val dataList = MutableLiveData<BrandEMIDataTable?>()

            val table = DBModule.appDatabase?.appDao.getBrandEMIDataTable(hostInvoice, hostTid)

            dataList.postValue(table)

        return dataList
    }*/


}