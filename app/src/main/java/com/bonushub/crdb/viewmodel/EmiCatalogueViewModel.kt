package com.bonushub.crdb.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.bonushub.crdb.model.local.TerminalParameterTable
import com.bonushub.crdb.repository.BankFunctionsRepository

class EmiCatalogueViewModel @ViewModelInject constructor(private val bankFunctionsRepository: BankFunctionsRepository):
    ViewModel() {
    private var terminalParameterTable:LiveData<TerminalParameterTable>? = null
     suspend fun getTerminalParameterTable(): LiveData<TerminalParameterTable>? {

        terminalParameterTable = bankFunctionsRepository.getTerminalParameterTable()

        return terminalParameterTable
    }

}