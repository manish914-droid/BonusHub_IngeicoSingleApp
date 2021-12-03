package com.bonushub.crdb.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.disputetransaction.CreateSettlementPacket
import com.bonushub.crdb.model.local.IngenicoSettlementResponse
import com.bonushub.crdb.repository.SettlementRepository
import com.bonushub.crdb.utils.Result
import com.ingenico.hdfcpayment.listener.OnOperationListener
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


    fun settlementResponse() {
        viewModelScope.launch {
            val settlementRequest = SettlementRequest(1, listOf("30160039"))
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


