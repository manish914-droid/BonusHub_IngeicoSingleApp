package com.bonushub.crdb.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.model.remote.BrandEMIProductDataModal
import com.bonushub.crdb.repository.GenericResponse
import com.bonushub.crdb.repository.ServerRepository
import com.bonushub.crdb.utils.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BrandEmiProductViewModel(private val serverRepository: ServerRepository,private val brandId:String,private val subCatId : String) : ViewModel() {

    val brandEMIProductLivedata: LiveData<GenericResponse<List<BrandEMIProductDataModal?>>>
        get() = serverRepository.brandLiveEMIProductData

    init {
        viewModelScope.launch(Dispatchers.IO) {
            logger("Get DATA","----START----------","e")
            serverRepository.getBrandEmiProductData("0",brandId,subCatId)
        }
    }

    suspend fun getBrandData(dataCounter:String,brandId:String,subCatId : String,searchedProductName:String, isSearchData:Boolean)
    {
        serverRepository.getBrandEmiProductData(dataCounter,brandId,subCatId, searchedProductName, isSearchData)
    }

    suspend fun getBrandData(dataCounter:String,brandId:String,subCatId : String)
    {
        serverRepository.getBrandEmiProductData("0",brandId,subCatId)
    }
}