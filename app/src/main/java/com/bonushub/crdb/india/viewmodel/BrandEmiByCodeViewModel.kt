package com.bonushub.crdb.india.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonushub.crdb.india.model.remote.BrandEMIbyCodeDataModal
import com.bonushub.crdb.india.repository.GenericResponse
import com.bonushub.crdb.india.repository.ServerRepository
import com.bonushub.crdb.india.utils.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BrandEmiByCodeViewModel(private val serverRepository: ServerRepository, val field56Pan:String, val field57Data:String) : ViewModel() {
    val brandEmiLiveData: LiveData<GenericResponse<BrandEMIbyCodeDataModal?>>
        get() = serverRepository.brandEMIAccessCodeLiveData
    //getBrandEmiByCodeData

    fun getBrandEmiByCodeDatafromVM() {
        viewModelScope.launch(Dispatchers.IO) {
            logger("Get DATA","----START----------","e")
            serverRepository.getBrandEmiByCodeData(field56Pan,field57Data)
        }
    }
}