package com.bonushub.crdb.india.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.bonushub.crdb.india.db.AppDao
import com.bonushub.crdb.india.disputetransaction.CreateSettlementPacket
import com.bonushub.crdb.india.model.local.IngenicoSettlementResponse
import com.bonushub.crdb.india.model.local.TempBatchFileDataTable
import com.bonushub.crdb.india.repository.SettlementRepository
import com.bonushub.crdb.india.utils.Result
import com.bonushub.crdb.india.utils.getBaseTID
import com.bonushub.crdb.india.utils.logger
import com.ingenico.hdfcpayment.request.SettlementRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettlementViewModel @ViewModelInject constructor(private val settlementRepository: SettlementRepository,
                                                       private val appDao: AppDao,
                                                       private val createSettlementPacket: CreateSettlementPacket) : ViewModel() {
    private var settlementByteArray: ByteArray? = null
    private var _ingenicosettlement = MutableLiveData<Result<IngenicoSettlementResponse>>()
    val ingenciosettlement = _ingenicosettlement

    // region============Batch Data
     fun getBatchData() = settlementRepository.getBatchDataList()
    //endregion

    // region ======== getTempBatchFileData
    fun getTempBatchFileData() = settlementRepository.getTempBatchDataList()
    // end region

    fun settlementResponse(distinctbytid: ArrayList<String>) {
        viewModelScope.launch {
            logger("baseTID",listOf(getBaseTID(appDao)).toString())
            val settlementRequest = SettlementRequest(distinctbytid.size,distinctbytid) //listOf("30160031")
            settlementRepository.fetchSettlementResponseData(settlementRequest).collect { result ->
                withContext(Dispatchers.IO){
                    result.data?.let { it ->
                        appDao.insertIngenicoSettlement(it)
                    }
                }

                _ingenicosettlement.value = result
            }
        }
       }


       fun createPacket(){
           val data = createSettlementPacket.createSettlementISOPacket()
           settlementByteArray = data.generateIsoByteRequest()
       }

    }


