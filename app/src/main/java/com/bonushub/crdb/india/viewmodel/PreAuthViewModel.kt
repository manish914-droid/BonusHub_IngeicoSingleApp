package com.bonushub.crdb.india.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonushub.crdb.india.repository.PreAuthRepository
import com.bonushub.crdb.india.view.fragments.AuthCompletionData
import com.bonushub.crdb.india.view.fragments.pre_auth.CompletePreAuthData
import com.bonushub.crdb.india.view.fragments.pre_auth.PendingPreAuthDataResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PreAuthViewModel @ViewModelInject constructor(private val preAuthRepository: PreAuthRepository)  : ViewModel() {

    /*var eDashboardItemLiveData:LiveData<ArrayList<EDashboardItem>>? = null

    suspend fun eDashboardItem() : LiveData<ArrayList<EDashboardItem>> {
        eDashboardItemLiveData = DashboardRepository.getInstance().getEDashboardItem()
        return eDashboardItemLiveData as LiveData<ArrayList<EDashboardItem>>
    }*/

    //var pendingPreAuthData:LiveData<PendingPreAuthDataaaa>? = null
    var pendingPreAuthData:MutableLiveData<PendingPreAuthDataResponse> = MutableLiveData()

    suspend fun getPendingPreAuthData() : LiveData<PendingPreAuthDataResponse> {
        pendingPreAuthData = preAuthRepository.getPendingPreAuthTxn()
      //  pendingPreAuthData = PreAuthRepository.getInstance().getPendingPreAuthTxn()
        return pendingPreAuthData as LiveData<PendingPreAuthDataResponse>
    }

    val progressDialog = MutableLiveData<LiveData<Boolean>>()


    var completePreAuthData:MutableLiveData<CompletePreAuthData> = MutableLiveData()
    suspend fun getCompletePreAuthData(authData: AuthCompletionData):LiveData<CompletePreAuthData>{
        viewModelScope.launch(Dispatchers.IO) {
            completePreAuthData = preAuthRepository.confirmCompletePreAuth(authData)
        }

        return completePreAuthData
    }

}