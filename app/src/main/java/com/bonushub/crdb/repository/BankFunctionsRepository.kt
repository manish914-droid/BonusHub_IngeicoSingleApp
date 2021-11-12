package com.bonushub.crdb.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class BankFunctionsRepository {

    companion object{

        @Synchronized
        fun getInstance():BankFunctionsRepository{
            return BankFunctionsRepository()
        }
    }

    fun isAdminPassword(password:String):LiveData<Boolean>{
        val data = MutableLiveData<Boolean>()

        // write logic whether password is correct or not
        data.value = true

        return data
    }

    fun isSuperAdminPassword(password:String):LiveData<Boolean>{
        val data = MutableLiveData<Boolean>()

        // write logic whether super password is correct or not
        data.value = true

        return data
    }
}