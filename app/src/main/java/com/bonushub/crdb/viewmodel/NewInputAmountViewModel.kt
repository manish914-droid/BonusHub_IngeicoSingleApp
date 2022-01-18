package com.bonushub.crdb.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.bonushub.crdb.model.local.HDFCTpt
import com.bonushub.crdb.model.local.TerminalParameterTable
import com.bonushub.crdb.repository.NewInputAmountRepository

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