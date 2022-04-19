package com.bonushub.crdb.india.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.bonushub.crdb.india.model.remote.BrandEMIProductDataModal
import com.bonushub.crdb.india.repository.GenericResponse
import com.bonushub.crdb.india.repository.ServerRepository
import javax.inject.Inject


class BrandEmiProductViewModel @ViewModelInject constructor(private val serverRepository: ServerRepository) : ViewModel() {

    val brandEMIProductLivedata: LiveData<GenericResponse<List<BrandEMIProductDataModal?>>>
        get() = serverRepository.brandLiveEMIProductData

//    init {
//        viewModelScope.launch(Dispatchers.IO) {
//            logger("Get DATA","----START----------","e")
//            serverRepository.getBrandEmiProductData("0",brandId,subCatId,callFromViewModel = true)
//        }
//    }

    suspend fun getBrandData(dataCounter:String,brandId:String,subCatId : String,searchedProductName:String, isSearchData:Boolean)
    {
        serverRepository.getBrandEmiProductData(dataCounter,brandId,subCatId, searchedProductName, isSearchData,callFromViewModel = true)
    }

    suspend fun getBrandData(dataCounter:String,brandId:String,subCatId : String)
    {
        serverRepository.getBrandEmiProductData("0",brandId,subCatId,callFromViewModel = true)
    }
}

// old
/*
class BrandEmiProductViewModel(private val serverRepository: ServerRepository,private val brandId:String,private val subCatId : String) : ViewModel() {

    val brandEMIProductLivedata: LiveData<GenericResponse<List<BrandEMIProductDataModal?>>>
        get() = serverRepository.brandLiveEMIProductData

    init {
        viewModelScope.launch(Dispatchers.IO) {
            logger("Get DATA","----START----------","e")
            serverRepository.getBrandEmiProductData("0",brandId,subCatId,callFromViewModel = true)
        }
    }

    suspend fun getBrandData(dataCounter:String,brandId:String,subCatId : String,searchedProductName:String, isSearchData:Boolean)
    {
        serverRepository.getBrandEmiProductData(dataCounter,brandId,subCatId, searchedProductName, isSearchData,callFromViewModel = true)
    }

    suspend fun getBrandData(dataCounter:String,brandId:String,subCatId : String)
    {
        serverRepository.getBrandEmiProductData("0",brandId,subCatId,callFromViewModel = true)
    }
}*/
