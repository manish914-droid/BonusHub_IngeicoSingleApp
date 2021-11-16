package com.bonushub.crdb.viewmodel.viewModelFactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bonushub.crdb.repository.ServerRepository
import com.bonushub.crdb.view.fragments.TenureSchemeActivity
import com.bonushub.crdb.viewmodel.BrandEmiMasterCategoryViewModel
import com.bonushub.crdb.viewmodel.BrandEmiProductViewModel

class BrandEmiViewModelFactory
    (private val serverRepository: ServerRepository,private val brandId:String="",private val subCatId : String=""):ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return if(brandId.isNotBlank()&&subCatId.isNotBlank()){
            BrandEmiProductViewModel(serverRepository, brandId, subCatId) as T
        }else
            BrandEmiMasterCategoryViewModel(serverRepository) as T
    }
}

class TenureSchemeActivityFactory
    (private val serverRepository: ServerRepository):ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {

          return  TenureSchemeActivity() as T
    }
}