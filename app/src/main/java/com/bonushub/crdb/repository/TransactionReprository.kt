package com.bonushub.crdb.repository

import android.util.Log
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.serverApi.RemoteService
import com.bonushub.pax.utils.IsoDataWriter
import javax.inject.Inject

class TransactionReprository @Inject constructor(private val  remoteService: RemoteService) {

    suspend fun getHostTransaction( transactionISOByteArray: IsoDataWriter) {
        when (val genericResp = remoteService.getHostTransaction(transactionISOByteArray)) {
            is GenericResponse.Success -> {
                Log.d("success:- ", "in success")
                val isoDataReader = genericResp.data
                // val issuerListData = isoDataReader?.isoMap?.get(57)?.parseRaw2String().toString()

            }
            is GenericResponse.Error -> {
                Log.d("error:- ", "in error")

            }
            is GenericResponse.Loading -> {

            }
        }
    }
}