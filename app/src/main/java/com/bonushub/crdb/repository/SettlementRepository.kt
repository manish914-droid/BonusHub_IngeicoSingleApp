package com.bonushub.crdb.repository

import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.model.local.BatchFileDataTable
import com.bonushub.pax.utils.TransactionType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettlementRepository @Inject constructor(private val appDao: AppDao) {

    //region===============Get Data For Terminal Parameter Data List:-
    fun getTerminalParameterDataList() = appDao.getAllTerminalParameterLiveData()
    //endregion

    // region===============Get Data For Terminal Communication Data List:-
    fun getCommunicationParameterDataList() =
        appDao.getAllTerminalCommunicationTableLiveData()
    //endregion

    // region===============Get Data For Batch:-
    fun getBatchDataList() = appDao.getAllBatchTableData()
    //endregion

    suspend fun insertBatchData() {
        val batchFileDataTable = BatchFileDataTable()
        batchFileDataTable.invoiceNumber = "000001"
        batchFileDataTable.totalAmount   = "20000"
        batchFileDataTable.transactionType = TransactionType.SALE.type
        batchFileDataTable.date            = ""

        appDao.insertBatchDataInTable(batchFileDataTable)
    }

}