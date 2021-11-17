package com.bonushub.crdb.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.bonushub.crdb.model.local.BatchFileDataTable
import com.bonushub.crdb.repository.BatchFilesRepository

class BatchFileViewModel:ViewModel() {


    private var batchTableData : LiveData<MutableList<BatchFileDataTable?>>? = null
    fun getBatchTableData() : LiveData<MutableList<BatchFileDataTable?>>
    {
        batchTableData = BatchFilesRepository.getInstance().getBatchTableData()
        return batchTableData as LiveData<MutableList<BatchFileDataTable?>>

    }

}