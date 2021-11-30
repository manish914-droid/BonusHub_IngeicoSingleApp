package com.bonushub.crdb.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.bonushub.crdb.repository.RoomDBRepository
import com.bonushub.crdb.repository.SettlementRepository
import com.bonushub.crdb.utils.ResponseHandler
import com.bonushub.crdb.utils.Result
import com.mindorks.example.coroutines.utils.Status
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SettlementViewModel @ViewModelInject constructor(private val settlementRepository: SettlementRepository) :
    ViewModel() {

    // region============Batch Data
    fun getBatchData() = settlementRepository.getBatchDataList()
    //endregion

    fun insertdata(){
        viewModelScope.launch {
            settlementRepository.insertBatchData()
        }

        }

    }

