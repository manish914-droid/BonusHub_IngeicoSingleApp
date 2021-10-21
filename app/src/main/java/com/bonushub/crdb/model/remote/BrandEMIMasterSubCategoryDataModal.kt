package com.bonushub.crdb.model.remote

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


//region=============================Brand EMI Master Category Data Modal==========================
@Parcelize
data class BrandEMIMasterDataModal(
    var brandID: String,
    var brandName: String,
    var mobileNumberBillNumberFlag: String
) : Parcelable
//endregion



//BrandEMIMasterSubCategoryDataModal --> as per Verifone
@Parcelize
data class BrandEMIMasterSubCategoryDataModal(
    var brandID: String,
    var categoryID: String,
    var parentCategoryID: String,
    var categoryName: String
) : Parcelable


