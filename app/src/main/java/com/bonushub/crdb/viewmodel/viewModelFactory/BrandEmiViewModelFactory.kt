package com.bonushub.crdb.viewmodel.viewModelFactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bonushub.crdb.repository.ServerRepository
import com.bonushub.crdb.viewmodel.BrandEmiViewModel

class BrandEmiViewModelFactory
    (private val serverRepository: ServerRepository):ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return BrandEmiViewModel(serverRepository) as T
    }
}