package com.bonushub.crdb.india.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonushub.crdb.india.model.remote.TenuresWithIssuerTncs
import com.bonushub.crdb.india.repository.GenericResponse
import com.bonushub.crdb.india.repository.ServerRepository
import com.bonushub.crdb.india.utils.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TenureSchemeViewModel (private val serverRepository: ServerRepository,val field56Pan:String,val field57Data:String) : ViewModel() {

    val emiTenureLiveData: LiveData<GenericResponse<TenuresWithIssuerTncs?>>
    get() = serverRepository.emiTenureLiveData

    init {
        viewModelScope.launch(Dispatchers.IO) {
            logger("Get DATA","----START----------","e")
            serverRepository.getEMITenureData(field56Pan,field57Data)
        }
    }
}