package com.bonushub.crdb.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.model.TerminalCommunicationTable
import com.bonushub.crdb.model.TerminalParameterTable
import com.bonushub.crdb.repository.RoomDBRepository
import com.bonushub.crdb.utils.ResponseHandler
import com.bonushub.crdb.utils.Result
import com.bonushub.pax.utils.EDashboardItem
import com.bonushub.pax.utils.isExpanded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardViewModel @ViewModelInject constructor(private val roomDBRepository: RoomDBRepository) :
ViewModel() {
    private val itemList = MutableLiveData<EDashboardItem>()
    private val list1 = MutableLiveData<EDashboardItem>()
    private val mutableLiveDataList = MutableLiveData<Result<ResponseHandler>>()

    var tpt = MutableLiveData<TerminalParameterTable>()
    val mutableLiveData = tpt
    // region ======SetUp DashBoard items
    fun fetchtptData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = roomDBRepository.fetcDashboarddata()
                mutableLiveData.postValue(data)
            }catch (e: Exception){

            }

        }

    }
}