package com.bonushub.crdb.india.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonushub.crdb.india.repository.PreAuthRepository
import com.bonushub.crdb.india.view.fragments.AuthCompletionData
import com.bonushub.crdb.india.view.fragments.pre_auth.CompletePreAuthData
import com.bonushub.crdb.india.view.fragments.pre_auth.PendingPreAuthDataResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PreAuthViewModel @ViewModelInject constructor(private val preAuthRepository: PreAuthRepository)  : ViewModel() {


    private val _pendingPreAuthData = MutableLiveData<PendingPreAuthDataResponse>()
    val pendingPreAuthData: LiveData<PendingPreAuthDataResponse>
        get() = preAuthRepository.data

    suspend fun getPendingPreAuthData(){
            preAuthRepository.getPendingPreAuthTxn()
    }


    private val _completePreAuthData = MutableLiveData<PendingPreAuthDataResponse>()
    val completePreAuthData: LiveData<PendingPreAuthDataResponse>
        get() = preAuthRepository.completePreAuthData

    suspend fun getCompletePreAuthData(authData: AuthCompletionData){

        preAuthRepository.confirmCompletePreAuth(authData)
    }

    private val _voidPreAuthData = MutableLiveData<PendingPreAuthDataResponse>()
    val voidPreAuthData: LiveData<PendingPreAuthDataResponse>
        get() = preAuthRepository.completePreAuthData

    suspend fun getVoidPreAuthData(authData: AuthCompletionData){

        preAuthRepository.voidAuthDataCreation(authData)
    }

}