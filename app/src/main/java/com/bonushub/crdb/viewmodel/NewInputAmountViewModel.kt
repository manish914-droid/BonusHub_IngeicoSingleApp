package com.bonushub.crdb.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonushub.crdb.model.local.TerminalParameterTable
import com.bonushub.crdb.repository.RoomDBRepository
import com.bonushub.crdb.utils.ResponseHandler
import com.bonushub.crdb.utils.Result
import com.bonushub.pax.utils.EDashboardItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NewInputAmountViewModel @ViewModelInject constructor(private val roomDBRepository: RoomDBRepository) :
    ViewModel()  {
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