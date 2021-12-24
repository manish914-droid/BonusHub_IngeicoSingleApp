package com.bonushub.crdb.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.bonushub.crdb.model.local.BatchTableReversal
import com.bonushub.crdb.repository.BatchReversalRepository

class BatchReversalViewModel@ViewModelInject constructor(private val batchReversalRepository: BatchReversalRepository):ViewModel() {


    private var batchTableReversalData : LiveData<MutableList<BatchTableReversal>>? = null
    suspend fun getBatchTableReversalData() : LiveData<MutableList<BatchTableReversal>>
    {
        batchTableReversalData = batchReversalRepository.getBatchTableReversalData()
        return batchTableReversalData as LiveData<MutableList<BatchTableReversal>>

    }

    suspend fun deleteBatchReversalTable(){

        batchReversalRepository.deleteBatchReversalTable()
    }


}