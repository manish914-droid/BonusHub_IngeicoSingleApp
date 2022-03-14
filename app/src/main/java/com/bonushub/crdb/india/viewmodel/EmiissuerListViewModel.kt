package com.bonushub.crdb.india.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.bonushub.crdb.india.repository.GenericResponse
import com.bonushub.crdb.india.repository.ServerRepository
import com.bonushub.crdb.india.view.fragments.IssuerBankModal
import com.bonushub.crdb.india.view.fragments.TenureBankModal

class EmiissuerListViewModel(private val serverRepository: ServerRepository) : ViewModel() {

    val emiIssuerListLivedata: LiveData<GenericResponse<List<IssuerBankModal?>>>
        get() = serverRepository.allIssuerBankListLiveData  as LiveData<GenericResponse<List<IssuerBankModal?>>>
    val emiIssuerTenureListLiveData:LiveData<GenericResponse<List<TenureBankModal?>>>
        get() = serverRepository.allIssuerTenureLisLiveData as LiveData<GenericResponse<List<TenureBankModal?>>>


     suspend fun getIssuerListData(field57RequestData:String){
        serverRepository.getIssuerList(field57RequestData)
    }
}