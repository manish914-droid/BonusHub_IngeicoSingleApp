package com.bonushub.crdb.india.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.bonushub.crdb.india.repository.DashboardRepository
import com.bonushub.crdb.india.utils.EDashboardItem

class DashboardViewModel  : ViewModel() {

    var eDashboardItemLiveData:LiveData<ArrayList<EDashboardItem>>? = null

    suspend fun eDashboardItem() : LiveData<ArrayList<EDashboardItem>> {
        eDashboardItemLiveData = DashboardRepository.getInstance().getEDashboardItem()
        return eDashboardItemLiveData as LiveData<ArrayList<EDashboardItem>>
    }


}