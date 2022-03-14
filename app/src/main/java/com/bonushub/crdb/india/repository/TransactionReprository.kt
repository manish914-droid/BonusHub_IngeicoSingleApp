package com.bonushub.crdb.india.repository

import android.util.Log
import com.bonushub.crdb.india.serverApi.RemoteService
import com.bonushub.crdb.india.utils.IsoDataReader
import com.bonushub.crdb.india.utils.IsoDataWriter
import javax.inject.Inject

class TransactionReprository @Inject constructor(private val  remoteService: RemoteService) {

    suspend fun getHostTransaction( transactionISOByteArray: IsoDataWriter):GenericResponse<IsoDataReader?> {
        when (val genericResp = remoteService.getHostTransaction(transactionISOByteArray)) {
            is GenericResponse.Success -> {
                Log.d("success:- ", "in success")
                val isoDataReader = genericResp.data
                // val issuerListData = isoDataReader?.isoMap?.get(57)?.parseRaw2String().toString()

                return genericResp
            }
            is GenericResponse.Error -> {
                Log.d("error:- ", "in error")
                return genericResp
            }
            is GenericResponse.Loading -> {
                return genericResp
            }
        }
    }
}