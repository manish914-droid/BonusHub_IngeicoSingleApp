package com.bonushub.crdb.india.model.remote

import com.bonushub.crdb.india.model.local.BatchTable
import com.bonushub.crdb.india.utils.EDashboardItem

class RestartHandlingModel(var transactionUuid: String , var transactionType: EDashboardItem) {

    constructor(transactionUuid: String , transactionType: EDashboardItem, batchData : BatchTable):this(transactionUuid,transactionType){
        this.batchData = batchData
    }

    var batchData:BatchTable? = null

}