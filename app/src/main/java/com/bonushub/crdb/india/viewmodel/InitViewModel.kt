package com.bonushub.crdb.india.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.bonushub.crdb.india.repository.RoomDBRepository
import com.bonushub.crdb.india.utils.ResponseHandler
import com.bonushub.crdb.india.utils.Result
import com.bonushub.crdb.india.utils.Status
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class InitViewModel @ViewModelInject constructor(private val roomDBRepository: RoomDBRepository) :
    ViewModel() {

    val initData = MutableLiveData<Result<ResponseHandler>>()
    fun insertInfo1(tid: String) {
        viewModelScope.launch{
            initData.postValue(Result.loading(null))
            if(tid.isEmpty()){
                Result.error(ResponseHandler(Status.ERROR,"Something Went Wrong",false,false),"Something Went Wrong")

            }else{
                val userId: Flow<Result<ResponseHandler>> = roomDBRepository.fetchInitData(tid)
                roomDBRepository.fetchInitData(tid).collect {

                    if(it.status.equals(Status.SUCCESS)){
                        initData.postValue(Result.success(it.data))
                    }
                    else if(it.status.equals(Status.ERROR)){
                        println("Result is"+it.message)
                        initData.postValue(Result.error(it.data,it.error))
                    }


                }

            }
        }
     }
    }