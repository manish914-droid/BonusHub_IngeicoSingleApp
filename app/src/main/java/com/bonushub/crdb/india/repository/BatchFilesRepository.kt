package com.bonushub.crdb.india.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bonushub.crdb.india.db.AppDao
import com.bonushub.crdb.india.model.local.BatchFileDataTable
import com.bonushub.crdb.india.model.local.BatchTable
import com.bonushub.crdb.india.model.local.TempBatchFileDataTable
import com.bonushub.crdb.india.utils.logger
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

    suspend fun getBatchTableDataListByInvoice(invoice: String?) : LiveData<MutableList<BatchTable?>> {
        val dataList = MutableLiveData<MutableList<BatchTable?>>()

        withContext(Dispatchers.IO) {

            val table = appDao.getBatchTableDataListByInvoice(invoice)
//            if (table != null) {
//
//                table.sortBy { it?.hostInvoice?.toInt() }
//                table.reverse()
//            }
            dataList.postValue(table)

        }
        return dataList
    }

    suspend fun getTempBatchTableDataListByInvoice(invoice: String?) : LiveData<MutableList<TempBatchFileDataTable?>> {
        val dataList = MutableLiveData<MutableList<TempBatchFileDataTable?>>()

        withContext(Dispatchers.IO) {

            val table = appDao.getTempBatchTableDataListByInvoice(invoice)
//            if (table != null) {
//
//                table.sortBy { it?.hostInvoice?.toInt() }
//                table.reverse()
//            }
            dataList.postValue(table)

        }
        return dataList
    }

    suspend fun deleteTempBatchFileDataTableFromInvoice(invoice: String?, tid: String?) {

        withContext(Dispatchers.IO) {
            val effectedRow = appDao.deleteTempBatchFileDataTableFromInvoice(invoice, tid)
            logger("effectedRow",""+effectedRow,"e")
        }
    }

    suspend fun insertTempBatchFileDataTable(data: TempBatchFileDataTable) {

        withContext(Dispatchers.IO) {
            val effectedRow = appDao.insertOrUpdateTempBatchFileDataTableData(data)
            logger("effectedRow",""+effectedRow,"e")
        }
    }

    /*suspend fun getBrandEMIDataTable(hostInvoice: String?, hostTid: String) : LiveData<BrandEMIDataTable?>{
        val dataList = MutableLiveData<BrandEMIDataTable?>()

            val table = DBModule.appDatabase?.appDao.getBrandEMIDataTable(hostInvoice, hostTid)

            dataList.postValue(table)

        return dataList
    }*/


}