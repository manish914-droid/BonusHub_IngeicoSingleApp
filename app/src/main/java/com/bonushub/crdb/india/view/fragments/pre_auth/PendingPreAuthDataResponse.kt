package com.bonushub.crdb.india.view.fragments.pre_auth

import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.utils.ApiStatus
import java.util.ArrayList

class PendingPreAuthDataResponse {
    var apiStatus: ApiStatus = ApiStatus.Nothing
    var msg: String? = null
    var pendingList = ArrayList<PendingPreauthData>()
    var cardProcessedDataModal = CardProcessedDataModal()
}