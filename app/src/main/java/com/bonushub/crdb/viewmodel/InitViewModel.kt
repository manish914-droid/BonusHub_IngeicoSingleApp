package com.bonushub.crdb.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.bonushub.crdb.model.TerminalCommunicationTable
import com.bonushub.crdb.repository.RoomDBRepository
import com.bonushub.crdb.utils.ResponseHandler
import com.bonushub.crdb.utils.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class InitViewModel @ViewModelInject constructor(private val roomDBRepository: RoomDBRepository) :
    ViewModel() {

    val initData = MutableLiveData<Result<Result<ResponseHandler>>>()
    fun insertInfo1(tid: String) {
        viewModelScope.launch{
            initData.postValue(Result.loading(null))
            if(tid.isNullOrEmpty()){
                initData.postValue(Result.error("Something Went Wrong", null))
            }else{
                val userId: Flow<Result<ResponseHandler>> = roomDBRepository.fetchInitData(tid)
                roomDBRepository.fetchInitData(tid).collect {

                    initData.postValue(Result.success(it))

                }

            }
        }
    }

    }