package com.bonushub.crdb.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bonushub.crdb.entity.CardOption
import com.bonushub.crdb.entity.EMVOption
import com.bonushub.crdb.model.CardProcessedDataModal
import com.bonushub.crdb.model.local.IngenicoSettlementResponse
import com.bonushub.crdb.utils.Result
import com.ingenico.hdfcpayment.listener.OnOperationListener
import com.ingenico.hdfcpayment.request.SettlementRequest
import kotlinx.coroutines.flow.Flow


interface SettleRepository {

    suspend fun settlement(settlement: SettlementRequest, operationListener: OnOperationListener?): Flow<MutableLiveData<Result<IngenicoSettlementResponse>>>


}