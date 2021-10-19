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

    private  val insertedId =  MutableLiveData<Long>()
    private val  error = MutableLiveData<String>()
    private var _errorMessage = MutableLiveData<String>()
    private val mutableLiveDataList = MutableLiveData<Result<ResponseHandler>>()
    val _initData = MutableLiveData<Result<Result<ResponseHandler>>>()
    val mutableLiveData = mutableLiveDataList
    var userFinalList: LiveData<MutableList<TerminalCommunicationTable>> = MutableLiveData<MutableList<TerminalCommunicationTable>>()

    fun insertInfo1(tid: String) {
        viewModelScope.launch{
            _initData.postValue(Result.loading(null))
            if(tid.isNullOrEmpty()){
                error.postValue( "Input Fields cannot be Empty")
                _initData.postValue(Result.error("Something Went Wrong", null))
            }else{
                val userId: Flow<Result<ResponseHandler>> = roomDBRepository.fetchInitData(tid)
                roomDBRepository.fetchInitData(tid).collect {
                    mutableLiveData.value = it

                    _initData.postValue(Result.success(it))

                }

            }
        }
    }

    }