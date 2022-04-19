package com.bonushub.crdb.india.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonushub.crdb.india.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.india.repository.GenericResponse
import com.bonushub.crdb.india.repository.ServerRepository
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp
import com.bonushub.crdb.india.utils.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BrandEmiMasterCategoryViewModel @ViewModelInject constructor(private val serverRepository: ServerRepository) : ViewModel() {

   val brandEMIMasterSubCategoryLivedata: LiveData<GenericResponse<List<BrandEMIMasterDataModal?>>>
        get() = serverRepository.brandLiveEMIMasterCategoryData

    init {
        viewModelScope.launch(Dispatchers.IO) {
            logger("Get DATA","----START----------","e")
            serverRepository.getBrandData()
            logger("Brand Tnc",serverRepository.appDB.appDao.getAllBrandTAndCData().toString(),"e")
            logger("Issuer Tnc",serverRepository.appDB.appDao.getAllIssuerTAndCData().toString(),"e")
            logger("Brand SubCat",serverRepository.appDB.appDao.getBrandEMISubCategoryData().toString(),"e")
            logger("Brand SubCat", Field48ResponseTimestamp.getBrandTAndCData().toString(),"e")

        }
    }
}