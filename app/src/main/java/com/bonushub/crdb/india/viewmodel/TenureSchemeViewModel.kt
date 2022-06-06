package com.bonushub.crdb.india.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.bonushub.crdb.india.model.remote.TenuresWithIssuerTncs
import com.bonushub.crdb.india.repository.GenericResponse
import com.bonushub.crdb.india.repository.ServerRepository
import com.bonushub.crdb.india.utils.logger

class TenureSchemeViewModel @ViewModelInject constructor(private val serverRepository: ServerRepository) : ViewModel()  {

    val emiTenureLiveData: LiveData<GenericResponse<TenuresWithIssuerTncs?>>
    get() = serverRepository.emiTenureLiveData

    suspend fun getEMITenureData(field56Pan:String="0",field57:String,counter:String="0"){
            logger("Get DATA","----START----------","e")
            serverRepository.getEMITenureData(field56Pan,field57)

    }
}