package com.bonushub.crdb.india.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.bonushub.crdb.india.model.local.PendingSyncTransactionTable
import com.bonushub.crdb.india.repository.PendingSyncTransactionRepository

class PendingSyncTransactionViewModel@ViewModelInject constructor(private val pendingSyncTransactionRepository: PendingSyncTransactionRepository):ViewModel() {


    private var pendingSyncTransactionTableData : LiveData<MutableList<PendingSyncTransactionTable>>? = null
    suspend fun getPendingSyncTransactionTableData() : LiveData<MutableList<PendingSyncTransactionTable>>
    {
        pendingSyncTransactionTableData = pendingSyncTransactionRepository.getAllPendingSyncTransactionTableData()
        return pendingSyncTransactionTableData as LiveData<MutableList<PendingSyncTransactionTable>>

    }

    suspend fun insertPendingSyncTransactionData(pendingSyncTransactionTable :PendingSyncTransactionTable){

        pendingSyncTransactionRepository.insertPendingSyncTransactionTable(pendingSyncTransactionTable)
    }

    suspend fun deletePendingSyncTransactionData(pendingSyncTransactionTable :PendingSyncTransactionTable){

        pendingSyncTransactionRepository.deletePendingSyncTransactionData(pendingSyncTransactionTable)
    }

    suspend fun deletePendingSyncTransactionTable(){

        pendingSyncTransactionRepository.deletePendingSyncTransactionTable()
    }


}