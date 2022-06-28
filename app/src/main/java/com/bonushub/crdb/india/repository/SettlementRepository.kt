package com.bonushub.crdb.india.repository

import androidx.lifecycle.MutableLiveData
import com.bonushub.crdb.india.db.AppDao
import com.bonushub.crdb.india.model.local.IngenicoSettlementResponse
import com.bonushub.crdb.india.utils.DeviceHelper
import com.bonushub.crdb.india.utils.Result
import com.ingenico.hdfcpayment.listener.OnOperationListener
import com.ingenico.hdfcpayment.request.SettlementRequest
import com.ingenico.hdfcpayment.response.OperationResult
import com.ingenico.hdfcpayment.response.SettlementResponse
import com.ingenico.hdfcpayment.type.ResponseCode
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject


class SettlementRepository @Inject constructor(private val appDao: AppDao){

    private val _insertCardStatus = MutableLiveData<Result<IngenicoSettlementResponse>>()

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

    // region===============Get Data For getTempBatchDataList:-
    fun getTempBatchDataList() = appDao.getAllTempBatchFileDataTableData()
    //endregion

/*    suspend fun fetchSettlementResponse(){
         flow {
           // emit(Result.loading())
            val result = doSettlement()
             _insertCardStatus
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
    }*/




 /*   private fun doSettlement()  {
        var ingencioresponse          = IngenicoSettlementResponse()

        return try {
            val settlementRequest = SettlementRequest(1, listOf("30160039"))
            DeviceHelper.doSettlement(
                request = settlementRequest,
                listener = object : OnOperationListener.Stub() {
                    override fun onCompleted(p0: OperationResult?) {
                        val response = p0?.value
                        val settlementResponse = p0?.value as? SettlementResponse

                        _insertCardStatus.postValue(Result.success(ingencioresponse))

                      *//*  if (response is SettlementResponse) {
                            response.apply {
                                println("Status = $status")
                                println("Response code = $responseCode")
                                println("Batch number = $batchNumber")
                                println("App version = $appVersion")
                                println("Release date = $releaseDate")
                            }
                        } else {
                            println("Error")
                        }*//*

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
            return Result.success(ingencioresponse)
        }
        catch (ex: DeadObjectException){
            ex.printStackTrace()
            return Result.error(ingencioresponse,"")
        }
    }*/

     var listner : OnOperationListener? = null
    fun fetchSettlementResponseData(
        settlementRequest: SettlementRequest) = callbackFlow {
        try {

            listner = object : OnOperationListener.Stub() {
                override fun onCompleted(p0: OperationResult?) {
                    val response = p0?.value
                    if (response is SettlementResponse) {
                        response.apply {
                            """
                               Response_Code = $responseCode
                               API_Response_Status = $status
                               Response_Code = $responseCode
                               Batch_number = $batchNumber
                               App_version = $appVersion
                               Release_date = $releaseDate
                               TIDStatusList = [${tidStatusList.joinToString()}]
                               TIDs = [${tidList.joinToString()}]
                            """.trimIndent().apply { println(this) }
                        }
                    }


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

                            trySend(Result.error(ingencioresponse,"Failed")).isFailure
                        }
                        else -> {

                        }

                    }
                }

            }

            DeviceHelper.doSettlement(settlementRequest,listner)
        }
        catch (ex: Exception){
            ex.printStackTrace()
            trySend(Result.error(IngenicoSettlementResponse(),ex.message)).isFailure
        }
        awaitClose {
            listner = null
        }
    }


}