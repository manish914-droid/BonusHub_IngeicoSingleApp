package com.bonushub.crdb.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.model.remote.TenuresWithIssuerTncs
import com.bonushub.crdb.repository.GenericResponse
import com.bonushub.crdb.repository.ServerRepository
import com.bonushub.crdb.utils.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TenureSchemeViewModel (private val serverRepository: ServerRepository) : ViewModel() {

    val emiTenureLiveData: LiveData<GenericResponse<TenuresWithIssuerTncs?>>
    get() = serverRepository.emiTenureLiveData

    init {
        viewModelScope.launch(Dispatchers.IO) {
            logger("Get DATA","----START----------","e")
            serverRepository.getEMITenureData("CB583507E9316C4BCFF0C2DEDE54AAB8E42DFE687C45E257")
        /*    logger("Brand Tnc",serverRepository.appDB.appDao.getAllBrandTAndCData().toString(),"e")
            logger("Issuer Tnc",serverRepository.appDB.appDao.getAllIssuerTAndCData().toString(),"e")
            logger("Brand SubCat",serverRepository.appDB.appDao.getBrandEMISubCategoryData().toString(),"e")
*/

        }
    }
}