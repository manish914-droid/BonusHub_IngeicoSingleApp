package com.bonushub.crdb.viewmodel.viewModelFactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bonushub.crdb.repository.ServerRepository
import com.bonushub.crdb.viewmodel.BrandEmiByCodeViewModel
import com.bonushub.crdb.viewmodel.BrandEmiMasterCategoryViewModel
import com.bonushub.crdb.viewmodel.BrandEmiProductViewModel
import com.bonushub.crdb.viewmodel.TenureSchemeViewModel

class BrandEmiViewModelFactory
    (private val serverRepository: ServerRepository,private val brandId:String="",private val subCatId : String=""):ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return if(brandId.isNotBlank()&&subCatId.isNotBlank()){
            BrandEmiProductViewModel(serverRepository, brandId, subCatId) as T
        }else
            BrandEmiMasterCategoryViewModel(serverRepository) as T
    }
}

class TenureSchemeActivityVMFactory
    (private val serverRepository: ServerRepository, private val field56Pan:String, private val field57Data:String):ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {

          return  TenureSchemeViewModel(serverRepository,field56Pan,field57Data) as T
    }
}

class BrandEmiByCodeVMFactory
    (private val serverRepository: ServerRepository, private val field56Pan:String, private val field57Data:String):ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {

        return  BrandEmiByCodeViewModel(serverRepository,field56Pan,field57Data) as T
    }
}