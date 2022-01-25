package com.bonushub.crdb.model.remote

import com.bonushub.crdb.model.local.BatchTable
import com.bonushub.crdb.model.local.BrandEMISubCategoryTable
import com.bonushub.crdb.utils.EDashboardItem

class RestartHandlingModel(var transactionUuid: String , var transactionType: EDashboardItem) {

    constructor(transactionUuid: String , transactionType: EDashboardItem, batchData : BatchTable):this(transactionUuid,transactionType){
        this.batchData = batchData
    }

    var batchData:BatchTable? = null

}