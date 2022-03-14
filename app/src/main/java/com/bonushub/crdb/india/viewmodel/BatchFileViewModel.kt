package com.bonushub.crdb.india.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.bonushub.crdb.india.model.local.BatchFileDataTable
import com.bonushub.crdb.india.model.local.BatchTable
import com.bonushub.crdb.india.repository.BatchFilesRepository

class BatchFileViewModel@ViewModelInject constructor(private val batchFilesRepository: BatchFilesRepository):ViewModel() {


    private var batchTableData : LiveData<MutableList<BatchFileDataTable?>>? = null
    suspend fun getBatchTableData() : LiveData<MutableList<BatchFileDataTable?>>
    {
        batchTableData = batchFilesRepository.getBatchTableData()
        return batchTableData as LiveData<MutableList<BatchFileDataTable?>>

    }

    private var batchTableDataByInvoice : LiveData<BatchTable?>? = null
    suspend fun getBatchTableDataByInvoice(invoice: String?) : LiveData<BatchTable?>
    {
        batchTableDataByInvoice = batchFilesRepository.getBatchTableDataByInVoice(invoice)
        return batchTableDataByInvoice as LiveData<BatchTable?>

    }

    private var batchTableDataListByInvoice : LiveData<MutableList<BatchTable?>>? = null
    suspend fun getBatchTableDataListByInvoice(invoice: String?) : LiveData<MutableList<BatchTable?>>
    {
        batchTableDataListByInvoice = batchFilesRepository.getBatchTableDataListByInvoice(invoice)
        return batchTableDataListByInvoice as LiveData<MutableList<BatchTable?>>

    }

    /*private var brandEMIDataTable : LiveData<BrandEMIDataTable?>? = null
    suspend fun getBrandEMIDataTable(hostInvoice: String?, hostTid: String) : LiveData<BrandEMIDataTable?>
    {
        brandEMIDataTable = BatchFilesRepository.getInstance().getBrandEMIDataTable(hostInvoice, hostTid)
        return brandEMIDataTable as LiveData<BrandEMIDataTable?>

    }*/

}