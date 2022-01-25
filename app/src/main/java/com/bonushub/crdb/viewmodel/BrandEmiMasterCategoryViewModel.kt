package com.bonushub.crdb.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.repository.GenericResponse
import com.bonushub.crdb.repository.ServerRepository
import com.bonushub.crdb.utils.Field48ResponseTimestamp
import com.bonushub.crdb.utils.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BrandEmiMasterCategoryViewModel(private val serverRepository: ServerRepository) : ViewModel() {

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