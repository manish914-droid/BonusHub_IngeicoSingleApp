package com.bonushub.crdb.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonushub.crdb.model.local.HDFCTpt
import com.bonushub.crdb.model.local.TerminalParameterTable
import com.bonushub.crdb.repository.NewInputAmountRepository
import com.bonushub.crdb.repository.NewInputAmountRepository.Companion.getInstance


import com.bonushub.crdb.repository.RoomDBRepository
import com.bonushub.crdb.utils.ResponseHandler
import com.bonushub.crdb.utils.Result
import com.bonushub.pax.utils.EDashboardItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NewInputAmountViewModel  : ViewModel()  {
    private var terminalParameterTable:LiveData<TerminalParameterTable>? = null
    private var hdfcTerminalParameterTable:LiveData<HDFCTpt>? = null
    fun fetchtptData() : LiveData<TerminalParameterTable>? {
        terminalParameterTable = NewInputAmountRepository.getInstance().getTerminalParameterTable()
        return terminalParameterTable
    }
    fun fetchHdfcTptData():LiveData<HDFCTpt>?{
        hdfcTerminalParameterTable = NewInputAmountRepository.getInstance().getHdfcTerminalParameterTable()
        return hdfcTerminalParameterTable
    }
}