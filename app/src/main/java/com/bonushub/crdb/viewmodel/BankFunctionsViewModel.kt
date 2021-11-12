package com.bonushub.crdb.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.bonushub.crdb.repository.BankFunctionsRepository

class BankFunctionsViewModel:ViewModel() {

    private var isAdminPassword:LiveData<Boolean>? = null

    fun isAdminPassword(password:String):LiveData<Boolean>? {

        isAdminPassword = BankFunctionsRepository.getInstance().isAdminPassword(password)
        return isAdminPassword
    }

    private var isSuperAdminPassword:LiveData<Boolean>? = null

    fun isSuperAdminPassword(password:String):LiveData<Boolean>? {

        isSuperAdminPassword = BankFunctionsRepository.getInstance().isSuperAdminPassword(password)
        return isSuperAdminPassword
    }
}