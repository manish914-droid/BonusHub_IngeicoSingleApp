package com.bonushub.crdb.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonushub.crdb.model.local.HDFCTpt
import com.bonushub.crdb.model.local.TerminalParameterTable
import com.bonushub.crdb.repository.DashboardRepository
import com.bonushub.crdb.repository.NewInputAmountRepository
import com.bonushub.crdb.repository.RoomDBRepository
import com.bonushub.crdb.utils.ResponseHandler
import com.bonushub.crdb.utils.Result
import com.bonushub.crdb.view.fragments.TableEditHelper
import com.bonushub.pax.utils.EDashboardItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DashboardViewModel  :
ViewModel() {

    var eDashboardItemLiveData:LiveData<ArrayList<EDashboardItem>>? = null
    suspend fun eDashboardItem() : LiveData<ArrayList<EDashboardItem>> {
        eDashboardItemLiveData = DashboardRepository.getInstance().getEDashboardItem()
        return eDashboardItemLiveData as LiveData<ArrayList<EDashboardItem>>
    }


}