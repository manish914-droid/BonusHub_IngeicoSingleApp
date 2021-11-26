package com.bonushub.crdb.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.bonushub.crdb.model.local.BatchFileDataTable
import com.bonushub.crdb.model.local.BrandEMIDataTable
import com.bonushub.crdb.repository.BatchFilesRepository

class BatchFileViewModel:ViewModel() {


    private var batchTableData : LiveData<MutableList<BatchFileDataTable?>>? = null
    suspend fun getBatchTableData() : LiveData<MutableList<BatchFileDataTable?>>
    {
        batchTableData = BatchFilesRepository.getInstance().getBatchTableData()
        return batchTableData as LiveData<MutableList<BatchFileDataTable?>>

    }

    private var batchTableDataByInvoice : LiveData<MutableList<BatchFileDataTable?>>? = null
    suspend fun getBatchTableDataByInvoice(invoice: String?) : LiveData<MutableList<BatchFileDataTable?>>
    {
        batchTableDataByInvoice = BatchFilesRepository.getInstance().getBatchTableDataByInVoice(invoice)
        return batchTableDataByInvoice as LiveData<MutableList<BatchFileDataTable?>>

    }

    /*private var brandEMIDataTable : LiveData<BrandEMIDataTable?>? = null
    suspend fun getBrandEMIDataTable(hostInvoice: String?, hostTid: String) : LiveData<BrandEMIDataTable?>
    {
        brandEMIDataTable = BatchFilesRepository.getInstance().getBrandEMIDataTable(hostInvoice, hostTid)
        return brandEMIDataTable as LiveData<BrandEMIDataTable?>

    }*/

}