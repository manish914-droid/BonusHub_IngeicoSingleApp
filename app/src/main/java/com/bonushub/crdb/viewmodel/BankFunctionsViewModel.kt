package com.bonushub.crdb.viewmodel

import android.content.Context
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonushub.crdb.model.local.TerminalParameterTable
import com.bonushub.crdb.repository.BankFunctionsRepository
import com.bonushub.crdb.repository.RoomDBRepository
import com.bonushub.crdb.view.fragments.TableEditHelper
import com.bonushub.crdb.view.fragments.TidsListModel
import kotlinx.coroutines.launch

class BankFunctionsViewModel @ViewModelInject constructor(private val bankFunctionsRepository: BankFunctionsRepository):ViewModel() {

    private var isAdminPassword:LiveData<Boolean>? = null
    val adminPassword = MutableLiveData<LiveData<Boolean>>()

    fun isAdminPassword(password:String) {
        viewModelScope.launch {
            try {
                isAdminPassword = bankFunctionsRepository.isAdminPassword(password)
                adminPassword.postValue(isAdminPassword)
            }
            catch (ex: Exception){
                ex.printStackTrace()
                val data = MutableLiveData<Boolean>()
                data.value = false
                adminPassword.postValue(data)
            }

        }
    }


    private var isSuperAdminPassword:LiveData<Boolean>? = null
    fun isSuperAdminPassword(password:String):LiveData<Boolean>? {

        viewModelScope.launch {
            try {
                isSuperAdminPassword = bankFunctionsRepository.isSuperAdminPassword(password)
            }catch (ex: Exception){
                ex.printStackTrace()
            }
        }

        return isSuperAdminPassword

    }


    private var terminalParamField:LiveData<ArrayList<TableEditHelper?>>? = null

    suspend fun getTerminalParamField():LiveData<ArrayList<TableEditHelper?>>? {

        terminalParamField = bankFunctionsRepository.getTerminalParameterTableData()

        return terminalParamField
    }

    private var terminalParameterTable:LiveData<TerminalParameterTable>? = null
    suspend fun getTerminalParameterTable():LiveData<TerminalParameterTable>? {

        terminalParameterTable = bankFunctionsRepository.getTerminalParameterTable()

        return terminalParameterTable
    }

    private var isUpdateTid:LiveData<Boolean>? = null
    suspend fun updateTerminalTable(dataList: ArrayList<TableEditHelper?>, context:Context):LiveData<Boolean>?{

        isUpdateTid = bankFunctionsRepository.updateTerminalParameterTable(dataList, context)
        return isUpdateTid
    }

    //TerminalCommunicationTable
    private var terminalCommunicationTable:LiveData<ArrayList<TableEditHelper?>>? = null
    suspend fun getTerminalCommunicationTableByRecordType(recordType:String):LiveData<ArrayList<TableEditHelper?>>? {

            terminalCommunicationTable =
                bankFunctionsRepository.getTerminalCommunicationTableByRecordType(recordType)
        return terminalCommunicationTable
    }

    private var isUpdateTid2:LiveData<Boolean>? = null
    suspend fun updateTerminalCommunicationTable(dataList: ArrayList<TableEditHelper?>, recordType:String, context:Context):LiveData<Boolean>{

            isUpdateTid2 = bankFunctionsRepository.updateTerminalCommunicationTable(
                dataList,
                recordType,
                context
            )

        return isUpdateTid2 as LiveData<Boolean>
    }

    private var allTidsWithStatus:LiveData<ArrayList<TidsListModel>>? = null
    suspend fun getAllTidsWithStatus():LiveData<ArrayList<TidsListModel>>? {

            allTidsWithStatus = bankFunctionsRepository.getAllTidsWithStatus()

        return allTidsWithStatus
    }
}