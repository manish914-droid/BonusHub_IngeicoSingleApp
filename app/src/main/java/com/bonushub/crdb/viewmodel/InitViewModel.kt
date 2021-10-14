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
    private val _isLoading = MutableLiveData<Boolean>()
    private val mutableLiveDataList = MutableLiveData<Result<ResponseHandler>>()
    val mutableLiveData = mutableLiveDataList
    var userFinalList: LiveData<MutableList<TerminalCommunicationTable>> = MutableLiveData<MutableList<TerminalCommunicationTable>>()

    fun isLoading(): LiveData<Boolean> = _isLoading

    fun fetchData(){
        viewModelScope.launch {

            userFinalList = roomDBRepository.fetchdata()
        }
    }

     fun insertInfo(student: TerminalCommunicationTable) {
         viewModelScope.launch {
            if(student.pcNo.isNullOrEmpty() ||
                    student.actionId.isNullOrEmpty() ||
                    student.tableId.isNullOrEmpty() ||
                    student.isActive.toString().isNullOrEmpty()){
                error.postValue( "Input Fields cannot be Empty")

            }else{
                val userId: Long = roomDBRepository.insertdata(student)
                insertedId.postValue(userId)
            }
         }
     }

    fun insertInfo1(tid: String) {
        _isLoading.postValue(true)
        viewModelScope.launch{
            if(tid.isNullOrEmpty()){
                error.postValue( "Input Fields cannot be Empty")
                _isLoading.postValue(false)
            }else{
                val userId: Flow<Result<ResponseHandler>> = roomDBRepository.fetchInitData(tid)
                roomDBRepository.fetchInitData(tid).collect {
                    mutableLiveData.value = it
                    _isLoading.postValue(false)
                }
                //  insertedId.postValue(userId)
            }
        }
    }


    fun fetchError(): LiveData<String> = error

    fun fetchInsertedId():LiveData<Long> = insertedId



}