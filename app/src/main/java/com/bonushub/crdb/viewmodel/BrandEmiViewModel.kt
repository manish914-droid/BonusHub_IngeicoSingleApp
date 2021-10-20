package com.bonushub.crdb.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.model.remote.BrandEMIMasterSubCategoryDataModal
import com.bonushub.crdb.repository.GenericResponse
import com.bonushub.crdb.repository.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BrandEmiViewModel(private val serverRepository: ServerRepository) : ViewModel() {

   val brandEMIMasterSubCategoryLivedata: LiveData<GenericResponse<List<BrandEMIMasterDataModal?>>>
        get() = serverRepository.brandLiveEMIMasterSubCategoryData

    init {
        viewModelScope.launch(Dispatchers.IO) {
            serverRepository.getBrandData()
        }
    }
}