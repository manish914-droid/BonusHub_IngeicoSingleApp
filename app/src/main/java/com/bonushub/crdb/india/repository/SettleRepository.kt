package com.bonushub.crdb.india.repository

import androidx.lifecycle.MutableLiveData
import com.bonushub.crdb.india.model.local.IngenicoSettlementResponse
import com.bonushub.crdb.india.utils.Result
import com.ingenico.hdfcpayment.listener.OnOperationListener
import com.ingenico.hdfcpayment.request.SettlementRequest
import kotlinx.coroutines.flow.Flow


interface SettleRepository {

    suspend fun settlement(settlement: SettlementRequest, operationListener: OnOperationListener?): Flow<MutableLiveData<Result<IngenicoSettlementResponse>>>


}