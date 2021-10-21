package com.bonushub.crdb.serverApi

//region===============================Enum Class for BrandEMI Data RequestType:-
enum class EMIRequestType(var requestType: String) {
    BRAND_DATA("1"),
    ISSUER_T_AND_C("5"),
    BRAND_T_AND_C("6"),
    BRAND_SUB_CATEGORY("2"),
    BRAND_EMI_Product("3"),
    BRAND_EMI_Product_WithCategory("11"),//It will used to get Productcategory Name on Searching of product,to show correct Product catergory Name in print
    BRAND_EMI_BY_ACCESS_CODE("7"),
    EMI_CATALOGUE_ACCESS_CODE("10")
}
//endregion