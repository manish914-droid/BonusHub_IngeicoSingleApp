package com.bonushub.crdb.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.repository.GenericResponse
import com.bonushub.crdb.repository.ServerRepository
import com.bonushub.crdb.utils.logger
import com.bonushub.crdb.view.fragments.IssuerBankModal
import com.bonushub.crdb.view.fragments.TenureBankModal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EmiissuerListViewModel(private val serverRepository: ServerRepository) : ViewModel() {

    val emiIssuerListLivedata: LiveData<GenericResponse<List<IssuerBankModal?>>>
        get() = serverRepository.allIssuerBankListLiveData  as LiveData<GenericResponse<List<IssuerBankModal?>>>
    val emiIssuerTenureListLiveData:LiveData<GenericResponse<List<TenureBankModal?>>>
        get() = serverRepository.allIssuerTenureLisLiveData as LiveData<GenericResponse<List<TenureBankModal?>>>


     suspend fun getIssuerListData(field57RequestData:String){
        serverRepository.getIssuerList(field57RequestData)
    }
}