package com.bonushub.crdb.repository

import android.os.DeadObjectException
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.model.local.BatchFileDataTable
import com.bonushub.crdb.model.local.IngenicoSettlementResponse
import com.bonushub.crdb.utils.DeviceHelper
import com.bonushub.crdb.utils.Result
import com.bonushub.pax.utils.TransactionType
import com.ingenico.hdfcpayment.listener.OnOperationListener
import com.ingenico.hdfcpayment.request.SettlementRequest
import com.ingenico.hdfcpayment.response.OperationResult
import com.ingenico.hdfcpayment.response.SettlementResponse
import com.ingenico.hdfcpayment.type.ResponseCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import javax.inject.Inject


class SettlementRepository @Inject constructor(private val appDao: AppDao){

    //region===============Get Data For Terminal Parameter Data List:-
    fun getTerminalParameterDataList() = appDao.getAllTerminalParameterLiveData()
    //endregion

    // region===============Get Data For Terminal Communication Data List:-
    fun getCommunicationParameterDataList() =
        appDao.getAllTerminalCommunicationTableLiveData()
    //endregion

    // region===============Get Data For Batch:-
    fun getBatchDataList() = appDao.getBatchData()
    //endregion

    suspend fun fetchSettlementResponse(): Flow<Result<IngenicoSettlementResponse>> {
        return flow {
            emit(Result.loading())
            val result = doSettlement()
            if (result.data?.status.equals("SUCCESS")) {
                result.data?.let { it ->
                   // appDao.insertIngenicoSettlement(it)
                }
                emit(result)
            }
        }
            .catch {
                emit(Result.error(IngenicoSettlementResponse(),it.message))
            }
            .flowOn(Dispatchers.IO)
    }




    private fun doSettlement() : Result<IngenicoSettlementResponse> {
        var ingencioresponse          = IngenicoSettlementResponse()
        return try {
            val settlementRequest = SettlementRequest(1, listOf("30160039"))
            DeviceHelper.doSettlement(
                request = settlementRequest,
                listener = object : OnOperationListener.Stub() {
                    override fun onCompleted(p0: OperationResult?) {
                        val response = p0?.value
                        val settlementResponse = p0?.value as? SettlementResponse

                      /*  if (response is SettlementResponse) {
                            response.apply {
                                println("Status = $status")
                                println("Response code = $responseCode")
                                println("Batch number = $batchNumber")
                                println("App version = $appVersion")
                                println("Release date = $releaseDate")
                            }
                        } else {
                            println("Error")
                        }*/

                        when (settlementResponse?.responseCode) {
                            ResponseCode.SUCCESS.value -> {

                                ingencioresponse.id           = 0
                                ingencioresponse.status       = settlementResponse.status.name
                                ingencioresponse.responseCode = settlementResponse.responseCode
                                ingencioresponse.batchNumber  = settlementResponse.batchNumber
                                ingencioresponse.appVersion   = settlementResponse.appVersion
                                ingencioresponse.releaseDate  = settlementResponse.releaseDate
                                ingencioresponse.tidList      = settlementResponse.tidList
                                ingencioresponse.tidStatusList      = settlementResponse.tidStatusList
                                ingencioresponse.tids      = settlementResponse.tids
                            }
                            ResponseCode.FAILED.value,
                            ResponseCode.ABORTED.value -> {
                                var responses          = IngenicoSettlementResponse()
                                ingencioresponse.id    = 0
                                responses.status       = settlementResponse.status.name
                                responses.responseCode = settlementResponse.responseCode
                                responses.batchNumber  = settlementResponse.batchNumber
                                responses.appVersion   = settlementResponse.appVersion
                                responses.releaseDate  = settlementResponse.releaseDate
                                responses.tidList      = settlementResponse.tidList
                                responses.tidStatusList      = settlementResponse.tidStatusList
                                responses.tids      = settlementResponse.tids
                            }
                            else -> {

                            }

                        }

                    }
                })

            if(true)
                return Result.success(ingencioresponse)
            else
                return return Result.success(ingencioresponse)
        }
        catch (ex: DeadObjectException){
            ex.printStackTrace()
            return Result.error(ingencioresponse,"")
        }
    }


    fun fetchSettlementResponseData(settlementRequest: SettlementRequest) = callbackFlow {
        try {

            val callback = object : OnOperationListener.Stub() {
                override fun onCompleted(p0: OperationResult?) {
                    val response = p0?.value
                    val settlementResponse = p0?.value as? SettlementResponse
                    var ingencioresponse          = IngenicoSettlementResponse()
                    when (settlementResponse?.responseCode) {
                        ResponseCode.SUCCESS.value -> {
                            ingencioresponse.id           = 0
                            ingencioresponse.status       = settlementResponse.status.name
                            ingencioresponse.responseCode = settlementResponse.responseCode
                            ingencioresponse.batchNumber  = settlementResponse.batchNumber
                            ingencioresponse.appVersion   = settlementResponse.appVersion
                            ingencioresponse.releaseDate  = settlementResponse.releaseDate
                            ingencioresponse.tidList      = settlementResponse.tidList
                            ingencioresponse.tidStatusList      = settlementResponse.tidStatusList
                            ingencioresponse.tids      = settlementResponse.tids

                            trySend(Result.success(ingencioresponse)).isSuccess
                        }
                        ResponseCode.FAILED.value,
                        ResponseCode.ABORTED.value -> {
                            var responses          = IngenicoSettlementResponse()
                            ingencioresponse.id    = 0
                            responses.status       = settlementResponse.status.name
                            responses.responseCode = settlementResponse.responseCode
                            responses.batchNumber  = settlementResponse.batchNumber
                            responses.appVersion   = settlementResponse.appVersion
                            responses.releaseDate  = settlementResponse.releaseDate
                            responses.tidList      = settlementResponse.tidList
                            responses.tidStatusList      = settlementResponse.tidStatusList
                            responses.tids      = settlementResponse.tids

                            trySend(Result.error(ingencioresponse,"Failed")).isSuccess
                        }
                        else -> {

                        }

                    }
                }

            }

            DeviceHelper.doSettlement(settlementRequest,callback)
        }
        catch (ex: Exception){
            ex.printStackTrace()
            trySend(Result.error(IngenicoSettlementResponse(),ex.message)).isSuccess
        }
        awaitClose {

        }
    }


}