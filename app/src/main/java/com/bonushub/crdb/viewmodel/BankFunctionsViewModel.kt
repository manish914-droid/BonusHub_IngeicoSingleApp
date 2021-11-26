package com.bonushub.crdb.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.bonushub.crdb.model.local.TerminalParameterTable
import com.bonushub.crdb.repository.BankFunctionsRepository
import com.bonushub.crdb.view.fragments.TableEditHelper
import com.bonushub.crdb.view.fragments.TidsListModel

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

    private var isUpdateTid:LiveData<Boolean>? = null
    suspend fun updateTerminalTable(dataList: ArrayList<TableEditHelper?>, context:Context):LiveData<Boolean>?{
        isUpdateTid = BankFunctionsRepository.getInstance().updateTerminalParameterTable(dataList, context)
        return isUpdateTid
    }

    //TerminalCommunicationTable
    private var terminalCommunicationTable:LiveData<ArrayList<TableEditHelper?>>? = null

    suspend fun getTerminalCommunicationTableByRecordType(recordType:String):LiveData<ArrayList<TableEditHelper?>>? {

        terminalCommunicationTable = BankFunctionsRepository.getInstance().getTerminalCommunicationTableByRecordType(recordType)
        return terminalCommunicationTable
    }

    private var isUpdateTid2:LiveData<Boolean>? = null
    suspend fun updateTerminalCommunicationTable(dataList: ArrayList<TableEditHelper?>, recordType:String, context:Context):LiveData<Boolean>{
        isUpdateTid2 = BankFunctionsRepository.getInstance().updateTerminalCommunicationTable(dataList, recordType, context)
        return isUpdateTid2 as LiveData<Boolean>
    }

    private var allTidsWithStatus:LiveData<ArrayList<TidsListModel>>? = null

    suspend fun getAllTidsWithStatus():LiveData<ArrayList<TidsListModel>>? {

        allTidsWithStatus = BankFunctionsRepository.getInstance().getAllTidsWithStatus()
        return allTidsWithStatus
    }
}