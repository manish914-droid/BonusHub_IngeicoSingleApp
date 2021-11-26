package com.bonushub.crdb.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bonushub.crdb.di.DBModule
import com.bonushub.crdb.model.local.BatchFileDataTable
import com.bonushub.crdb.model.local.BrandEMIDataTable
import kotlinx.coroutines.runBlocking

class BatchFilesRepository {

    companion object{

        @Synchronized
        fun getInstance():BatchFilesRepository{
            return BatchFilesRepository()
        }
    }


    suspend fun getBatchTableData() : LiveData<MutableList<BatchFileDataTable?>>{
        val dataList = MutableLiveData<MutableList<BatchFileDataTable?>>()

            val table = DBModule.appDatabase?.appDao.getBatchTableData()

            if (table != null) {

                //  result.sortByDescending { invoice->(invoice.hostInvoice).toInt() }
                table.sortBy { it?.hostInvoice?.toInt() }
                table.reverse()
            }


            dataList.postValue(table)

        return dataList
    }

    suspend fun getBatchTableDataByInVoice(invoice: String?) : LiveData<MutableList<BatchFileDataTable?>>{
        val dataList = MutableLiveData<MutableList<BatchFileDataTable?>>()

            val table = DBModule.appDatabase?.appDao.getBatchTableDataByInvoice(invoice)

//            if (table != null) {
//
//                //  result.sortByDescending { invoice->(invoice.hostInvoice).toInt() }
//                table.sortBy { it?.hostInvoice?.toInt() }
//                table.reverse()
//            }


            dataList.postValue(table)

        return dataList
    }

    /*suspend fun getBrandEMIDataTable(hostInvoice: String?, hostTid: String) : LiveData<BrandEMIDataTable?>{
        val dataList = MutableLiveData<BrandEMIDataTable?>()

            val table = DBModule.appDatabase?.appDao.getBrandEMIDataTable(hostInvoice, hostTid)

            dataList.postValue(table)

        return dataList
    }*/


}