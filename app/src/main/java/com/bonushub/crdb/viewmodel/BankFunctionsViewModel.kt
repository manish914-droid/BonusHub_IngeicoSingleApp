package com.bonushub.crdb.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.bonushub.crdb.model.local.TerminalParameterTable
import com.bonushub.crdb.repository.BankFunctionsRepository
import com.bonushub.crdb.view.fragments.TableEditHelper

class BankFunctionsViewModel:ViewModel() {

    private var isAdminPassword:LiveData<Boolean>? = null

    fun isAdminPassword(password:String):LiveData<Boolean>? {

        isAdminPassword = BankFunctionsRepository.getInstance().isAdminPassword(password)
        return isAdminPassword
    }

    private var isSuperAdminPassword:LiveData<Boolean>? = null

    fun isSuperAdminPassword(password:String):LiveData<Boolean>? {

        isSuperAdminPassword = BankFunctionsRepository.getInstance().isSuperAdminPassword(password)
        return isSuperAdminPassword
    }


    private var terminalParamField:LiveData<ArrayList<TableEditHelper?>>? = null

    suspend fun getTerminalParamField():LiveData<ArrayList<TableEditHelper?>>? {

        terminalParamField = BankFunctionsRepository.getInstance().getTerminalParameterTableData()
        return terminalParamField
    }

    private var terminalParameterTable:LiveData<TerminalParameterTable>? = null

    suspend fun getTerminalParameterTable():LiveData<TerminalParameterTable>? {

        terminalParameterTable = BankFunctionsRepository.getInstance().getTerminalParameterTable()
        return terminalParameterTable
    }

    suspend fun updateTerminalTable(dataList: ArrayList<TableEditHelper?>){
        BankFunctionsRepository.getInstance().updateTerminalParameterTable(dataList)
    }
}