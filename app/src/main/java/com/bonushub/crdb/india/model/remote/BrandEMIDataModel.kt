package com.bonushub.crdb.india.model.remote

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.Serializable


//region=============================Brand EMI Master Category Data Modal==========================

data class BrandEMIMasterDataModal(
    var brandID: String,
    var brandName: String,
    var mobileNumberBillNumberFlag: String
) : Serializable
//endregion



//BrandEMIMasterSubCategoryDataModal --> as per Verifone
@Parcelize
data class BrandEMIMasterSubCategoryDataModal(
    var brandID: String,
    var categoryID: String,
    var parentCategoryID: String,
    var categoryName: String
) : Parcelable


//region=============================Brand EMI Master Category Data Modal==========================

data class BrandEMIProductDataModal(
    var productID: String,
    var productName: String,
    var skuCode: String,
    var productMinAmount: String,
    var productMaxAmount: String,
    var amountRangeValidationFlag: String,
    var validationTypeName: String,
    var isRequired: String,
    var inputDataType: String,
    var minLength: String,
    var maxLength: String,
    var productCategoryName: String,
    var producatDesc:String,


) : Serializable{
    var imeiOrSerialNum:String=""
    var billNum:String=""
}
//endregion


class BrandEmiBillSerialMobileValidationModel : Serializable {
    var isMobileNumReq = false
    var isBillNumReq = false
    var isSerialNumReq = false
    var isImeiNumReq = false

    var isMobileNumMandatory = false
    var isBillNumMandatory = false
    var isSerialNumMandatory = false
    var isImeiNumMandatory = false

    var isIemeiOrSerialNumReq = false
}

//region==================Data Modal For BankEMI Issuer TAndC Data:-
@Parcelize
data class BankEMIIssuerTAndCDataModal(
    var emiSchemeID: String,
    var issuerID: String,
    var issuerName: String,
    var schemeTAndC: String,
    var updateIssuerTAndCTimeStamp: String
) : Parcelable
//endregion

//region==================Data Modal For BankEMI Data:-
// VX990 --> BankEMIDataModal
@Parcelize
data class BankEMITenureDataModal(
    var tenure: String,
    var tenureInterestRate: String,
    var effectiveRate: String,
    var instantDiscount: String="",
    var transactionAmount: String = "0",
    var discountAmount: String = "0",
    var discountFixedValue: String,
    var discountPercentage: String,
    var loanAmount: String = "0",
    var emiAmount: String,
    var totalEmiPay: String,
    var processingFee: String,
    var processingRate: String,
    var totalProcessingFee: String,
    var totalInterestPay: String = "0",
    val cashBackAmount: String = "0",
    var netPay: String,
    var tenureTAndC: String,
    var tenureWiseDBDTAndC: String,
    var discountCalculatedValue: String,
    var cashBackCalculatedValue: String,
    var tenureLabel:String="",
    var txnTID :String="",
    var isSelected:Boolean = false
) : Parcelable
//endregion

data class TenuresWithIssuerTncs(  var bankEMIIssuerTAndCList: BankEMIIssuerTAndCDataModal
        , var bankEMISchemesDataList: MutableList<BankEMITenureDataModal>
        , var tripleForInstaEmi:Triple<Boolean, String, Boolean> = Triple(false,"",false)
)


data class BrandEMIbyCodeDataModal(
    var emiCode: String,
    var bankID: String,
    var bankTID: String,
    var issuerID: String,
    var tenure: String,
    var brandID: String,
    var productID: String,
    var emiSchemeID: String,
    var transactionAmount: String,
    var discountAmount: String,
    var loanAmount: String,
    var interestAmount: String,
    var emiAmount: String,
    var cashBackAmount: String,
    var netPayAmount: String,
    var processingFee: String,
    var processingFeeRate: String,
    var totalProcessingFee: String,
    var brandName: String,
    var issuerName: String,
    var productName: String,
    var productCode: String,
    var productModal: String,
    var productCategoryName: String,
    var productSerialCode: String,
    var skuCode: String,
    var totalInterest: String,
    var schemeTAndC: String,
    var schemeTenureTAndC: String,
    var schemeDBDTAndC: String,
    var discountCalculatedValue: String,
    var cashBackCalculatedValue: String,
    var orignalTxnAmt:String,
    var mobileNo:String,
    var brandReservField:String,
    var productBaseCat:String,
    var issuerTimeStamp:String,
    var brandTimeStamp:String,
    var instaDiscount:String,
    var tenureLabel:String ,
    var txnTID :String

) : Serializable{
    var billNumberInvoiceNo:String=""
    var hostInvoice:String=""
}

class BrandEMIDataModal : Serializable {

    var brandID: String? = null
    var brandName: String? = null
    var brandReservedValues: String? = null
    var categoryID: String? = null
    var categoryName: String? = null
    var productID: String? = null
    var productName: String? = null
    var childSubCategoryID: String? = null
    var childSubCategoryName: String? = null
    var validationTypeName: String? = null
    var isRequired: String? = null
    var inputDataType: String? = null
    var minLength: String? = null
    var maxLength: String? = null
    var producatDesc: String? = null
    var productMinAmount: String? = null
    var productMaxAmount: String? = null
    var dataTimeStampChangedOrNot: Boolean = false

    var imeiORserailNum:String=""
    var productCategoryName: String = ""
    var mobileNumberBillNumberFlag: String = ""


}