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

class MainViewModel @ViewModelInject constructor(private val roomDBRepository: RoomDBRepository) :
    ViewModel(),LifecycleObserver {

    private  val insertedId =  MutableLiveData<Long>()
    private val  error = MutableLiveData<String>()

    private  val initResponseMessage =  MutableLiveData<String>()
    private  val initResponse =  MutableLiveData<Boolean>()
    private  val initprogress =  MutableLiveData<Boolean>()

    private val _movieList = MutableLiveData<Result<ResponseHandler>>()
    val mutableLiveData = _movieList

    var userFinalList: LiveData<MutableList<TerminalCommunicationTable>> = MutableLiveData<MutableList<TerminalCommunicationTable>>()

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
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
        viewModelScope.launch{
            if(tid.isNullOrEmpty()){
                error.postValue( "Input Fields cannot be Empty")

            }else{
                val userId: Flow<Result<ResponseHandler>> = roomDBRepository.fetchInitData(tid)
                roomDBRepository.fetchInitData(tid).collect {
                    mutableLiveData.value = it
                }
                //  insertedId.postValue(userId)
            }
        }
    }

    private fun onInitResponse(res: String, success: Boolean, progress: Boolean,nothing : Boolean) {
            initResponseMessage.postValue(res)
            initResponse.postValue(success)
            initprogress.postValue(progress)


    }

    fun fetchError(): LiveData<String> = error

    fun fetchInsertedId():LiveData<Long> = insertedId

    fun fetchInitResponse(): LiveData<Boolean> = initResponse

    fun fetchInitMessage(): LiveData<String> = initResponseMessage

    fun fetchInitProgress(): LiveData<Boolean> = initprogress


}