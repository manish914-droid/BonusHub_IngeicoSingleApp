package com.bonushub.crdb.india.viewmodel.viewModelFactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bonushub.crdb.india.repository.ServerRepository
import com.bonushub.crdb.india.viewmodel.EmiissuerListViewModel

class IssuerListEmiViewModelFactory
    (private val serverRepository: ServerRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return EmiissuerListViewModel(serverRepository) as T
    }
}