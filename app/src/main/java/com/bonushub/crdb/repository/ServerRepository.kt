package com.bonushub.crdb.repository


import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bonushub.crdb.db.AppDatabase
import com.bonushub.crdb.model.local.BrandEMIMasterTimeStamps
import com.bonushub.crdb.model.local.BrandEMISubCategoryTable
import com.bonushub.crdb.model.local.BrandTAndCTable
import com.bonushub.crdb.model.local.IssuerTAndCTable
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.model.remote.BrandEMIMasterSubCategoryDataModal
import com.bonushub.crdb.model.remote.BrandEMIProductDataModal
import com.bonushub.crdb.serverApi.EMIRequestType
import com.bonushub.crdb.serverApi.RemoteService
import com.bonushub.crdb.utils.Utility
import com.bonushub.pax.utils.SplitterTypes
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ServerRepository( val appDB: AppDatabase, private val remoteService: RemoteService) {
    private val brandEMIMasterCategoryMLData = MutableLiveData<GenericResponse<List<BrandEMIMasterDataModal?>>>()
    val brandLiveEMIMasterCategoryData: LiveData<GenericResponse<List<BrandEMIMasterDataModal?>>>
        get() = brandEMIMasterCategoryMLData


    private val brandEMIProductMLData = MutableLiveData<GenericResponse<List<BrandEMIProductDataModal?>>>()
    val brandLiveEMIProductData: LiveData<GenericResponse<List<BrandEMIProductDataModal?>>>
        get() = brandEMIProductMLData


    private var moreDataFlag = "0"
    private var totalRecord= "0"

    // region ====== TIME STAMPS =========
    private var brandTimeStamp: String? = null
    private var brandCategoryUpdatedTimeStamp: String? = null
    private var issuerTAndCTimeStamp: String? = null
    private var brandTAndCTimeStamp: String? = null
    // endregion =======

    private var brandEmiMasterDataList = mutableListOf<BrandEMIMasterDataModal>()

    //region====================Issuer Terms And Conditions Variables:-
    private var issuerTAndCMoreDataFlag: String? = null
    private var issuerTAndCPerPageRecord: Int = 0
    private var issuerTAndCTotalRecord: Int = 0
    private var issuerTAndCRecordData: String? = null
    private var issuerTermsAndConditionsDataList = mutableListOf<String>()
    //endregion


    //region====================Brand Terms And Conditions Variables:-
    private var brandTAndCMoreDataFlag: String? = null
    private var brandTAndCPerPageRecord: Int = 0
    private var brandTAndCTotalRecord: Int = 0
    private var brandTAndCRecordData: String? = null
    private var brandTermsAndConditionsDataList = mutableListOf<String>()
    //endregion

    //region  =======Sub categories
    private var perPageRecord: String? = "0"

    private var brandEmiMasterSubCategoryDataList = mutableListOf<BrandEMIMasterSubCategoryDataModal>()

    // endregion ========

    private val brandEmiProductDataList by lazy { mutableListOf<BrandEMIProductDataModal>() }
    private val brandEmiSearchedProductDataList by lazy { mutableListOf<BrandEMIProductDataModal>() }


    suspend fun getBrandData(dataCounter:String="0") {
       // val genericResp = remoteService.getBrandDataService(dataCounter)
     //   brandMLData.postValue(genericResp)
        val field57= "${EMIRequestType.BRAND_DATA.requestType}^$dataCounter"
        when(val genericResp = remoteService.field57GenericService(field57)){
            is GenericResponse.Success->{
                val isoDataReader=genericResp.data
                val brandEMIMasterData = isoDataReader?.isoMap?.get(57)?.parseRaw2String().toString()
               stubbingBrandEMIMasterDataToList(brandEMIMasterData)
            }
            is GenericResponse.Error->{
                brandEMIMasterCategoryMLData .postValue(GenericResponse.Error(genericResp.errorMessage.toString()))
            }
            is GenericResponse.Loading->{

            }
        }
    }
    private suspend fun getBrandTnc(dataCounter: String="0"){
        val field57= "${EMIRequestType.BRAND_T_AND_C.requestType}^$dataCounter"
        when(val genericResp = remoteService.field57GenericService(field57)){
            is GenericResponse.Success->{
                val isoDataReader=genericResp.data
                val tncString = isoDataReader?.isoMap?.get(57)?.parseRaw2String().toString()
                stubbingBrandTAndCData(tncString)
            }
            is GenericResponse.Error->{
                brandEMIMasterCategoryMLData.postValue(GenericResponse.Error(genericResp.errorMessage.toString()))
            }
            is GenericResponse.Loading->{

            }
        }
    }
     private suspend fun getIssuerTnc(dataCounter:String="0"){
        val field57= "${EMIRequestType.ISSUER_T_AND_C.requestType}^$dataCounter"
        when(val genericResp = remoteService.field57GenericService(field57)){
            is GenericResponse.Success->{
                val isoDataReader=genericResp.data
                val tncString = isoDataReader?.isoMap?.get(57)?.parseRaw2String().toString()
                stubbingIssuerTAndCData(tncString)
            }
            is GenericResponse.Error->{
                brandEMIMasterCategoryMLData.postValue(GenericResponse.Error(genericResp.errorMessage.toString()))
            }
            is GenericResponse.Loading->{

            }
        }
    }
    private suspend fun getBrandSubCategoryData(dataCounter: String){

        val field57=  "${EMIRequestType.BRAND_SUB_CATEGORY.requestType}^$dataCounter^${brandEmiMasterDataList[0].brandID }"

        when(val genericResp = remoteService.field57GenericService(field57)){
            is GenericResponse.Success->{
                val isoDataReader=genericResp.data
                val brandSubCatDataString = isoDataReader?.isoMap?.get(57)?.parseRaw2String().toString()
                stubbingBrandEMIMasterSubCategoryDataToList(brandSubCatDataString)
            }
            is GenericResponse.Error->{
                brandEMIMasterCategoryMLData.postValue(GenericResponse.Error(genericResp.errorMessage.toString()))
            }
            is GenericResponse.Loading->{

            }
        }


    }
     suspend fun getBrandEmiProductData(dataCounter: String="0",brandID:String?,categoryID:String?){
        val field57=  "${EMIRequestType.BRAND_EMI_Product.requestType}^$dataCounter^${brandID}^${categoryID}"

        when(val genericResp = remoteService.field57GenericService(field57)){
            is GenericResponse.Success->{
                val isoDataReader=genericResp.data
                val brandEmiProductDataString = isoDataReader?.isoMap?.get(57)?.parseRaw2String().toString()
                stubbingBrandEMIProductDataToList(brandEmiProductDataString,brandID,categoryID)

            }
            is GenericResponse.Error->{
                brandEMIProductMLData.postValue(GenericResponse.Error(genericResp.errorMessage.toString()))
            }
            is GenericResponse.Loading->{

            }
        }


    }

    suspend fun getEMITenureData(pan:String="0"){

        when(val genericResp = remoteService.getEMITenureService(pan)){
            is GenericResponse.Success->{
                val isoDataReader=genericResp.data
                val tenureTnc = isoDataReader?.isoMap?.get(57)?.parseRaw2String().toString()
                Log.e("Tenure",tenureTnc)
             //   stubbingBrandTAndCData(tncString)
            }
            is GenericResponse.Error->{
                brandEMIMasterCategoryMLData.postValue(GenericResponse.Error(genericResp.errorMessage.toString()))
            }
            is GenericResponse.Loading->{

            }
        }
    }

    //region=================================Stubbing BrandEMI Master Data to List:-
    private suspend fun stubbingBrandEMIMasterDataToList(brandEMIMasterData: String) {
                val dataList = Utility().parseDataListWithSplitter("|", brandEMIMasterData)
                if (dataList.isNotEmpty()) {
                    moreDataFlag = dataList[0]
                    totalRecord = dataList[1]
                    brandTimeStamp = dataList[2]
                    brandCategoryUpdatedTimeStamp = dataList[3]
                    issuerTAndCTimeStamp = dataList[4]
                    brandTAndCTimeStamp = dataList[5]
                    //Store DataList in Temporary List and remove first 5 index values to get sublist from 5th index till dataList size
                    // and iterate further on record data only:-
                    val tempDataList: MutableList<String> = dataList.subList(6, dataList.size)
                    for (i in tempDataList.indices) {
                        if (!TextUtils.isEmpty(tempDataList[i])) {
                            /* Below parseDataWithSplitter gives following data:-
                                 0 index -> Brand ID
                                 1 index -> Brand Name
                                 2 index -> Mobile Number Capture Flag / Bill Invoice Capture Flag
                               */
                            if (!TextUtils.isEmpty(tempDataList[i])) {
                                val brandData = Utility().parseDataListWithSplitter(SplitterTypes.CARET.splitter, tempDataList[i])
                                brandEmiMasterDataList.add(BrandEMIMasterDataModal(brandData[0], brandData[1], brandData[2]))
                            }
                        }
                    }
                    //Refresh Field57 request value for Pagination if More Record Flag is True:-
                    if (moreDataFlag == "1") {
                        getBrandData(totalRecord!!)
                    } else {

                        getBrandTnc()
                     //   brandEMIMasterCategoryMLData.postValue(GenericResponse.Success(brandEmiMasterDataList))
                    }
                } else {
                    brandEMIMasterCategoryMLData.postValue(GenericResponse.Error("Data list is empty"))
                }
    }
    //endregion
    //region===========================Below method is used to Stubbing issuer terms and conditions data:-
    private suspend fun stubbingIssuerTAndCData(issuerTAndC: String) {
            if (!TextUtils.isEmpty(issuerTAndC)) {
                val dataList = Utility().parseDataListWithSplitter("|", issuerTAndC)
                if (dataList.isNotEmpty()) {
                    issuerTAndCMoreDataFlag = dataList[0]
                    issuerTAndCPerPageRecord = dataList[1].toInt()
                    issuerTAndCTotalRecord += issuerTAndCPerPageRecord
                    issuerTAndCRecordData = dataList[2]

                    //Store DataList in Temporary List and remove first 2 index values to get sublist from 3th index till dataList size
                    // and iterate further on record data only:-
                    var tempDataList = mutableListOf<String>()
                    tempDataList = dataList.subList(2, dataList.size - 1)
                    for (i in tempDataList.indices) {
                        Log.d("IssuerTAndC:- ", tempDataList[i])
                        issuerTermsAndConditionsDataList.add(tempDataList[i])
                    }
                    if (issuerTAndCMoreDataFlag == "1") {
                      /*  issuerField57Data =
                            "${EMIRequestType.ISSUER_T_AND_C.requestType}^$issuerTAndCTotalRecord"
                        fetchIssuerTAndC()*/
                        getIssuerTnc(issuerTAndCTotalRecord.toString())

                    } else {
                        if (issuerTermsAndConditionsDataList.isNotEmpty()) {
                            //region================Insert IssuerTAndC and Brand TAndC in DB:-
                            //Issuer TAndC Inserting:-
                            for (i in 0 until issuerTermsAndConditionsDataList.size) {
                                val issuerModel = IssuerTAndCTable()
                                if (!TextUtils.isEmpty(issuerTermsAndConditionsDataList[i])) {
                                    val splitData = Utility().parseDataListWithSplitter(
                                        SplitterTypes.CARET.splitter,
                                        issuerTermsAndConditionsDataList[i]

                                    )
                                    if (splitData.size > 2) {
                                        issuerModel.issuerId = splitData[0]
                                        issuerModel.headerTAndC = splitData[1]
                                        issuerModel.footerTAndC = splitData[2]
                                    } else {
                                        issuerModel.issuerId = splitData[0]
                                        issuerModel.headerTAndC = splitData[1]
                                    }
                                    //save issuer tnc here..........
                                    appDB.appDao.insertIssuerTAndCData(issuerModel)
                                }
                            }

                            getBrandSubCategoryData("0")

                        }

                    }
                }
            }
    }
    //endregion
    //region===========================Below method is used to Stubbing brand terms and conditions data:-
    private suspend fun stubbingBrandTAndCData(brandTAndC: String) {
            if (!TextUtils.isEmpty(brandTAndC)) {
                val dataList = Utility().parseDataListWithSplitter("|", brandTAndC)
                if (dataList.isNotEmpty()) {
                    brandTAndCMoreDataFlag = dataList[0]
                    brandTAndCPerPageRecord = dataList[1].toInt()
                    brandTAndCTotalRecord += brandTAndCPerPageRecord
                    brandTAndCRecordData = dataList[2]
                    //Store DataList in Temporary List and remove first 2 index values to get sublist from 3th index till dataList size
                    // and iterate further on record data only:-
                    var tempDataList = mutableListOf<String>()
                    tempDataList = dataList.subList(2, dataList.size - 1)
                    for (i in tempDataList.indices) {
                        Log.d("IssuerTAndC:- ", tempDataList[i])
                        brandTermsAndConditionsDataList.add(tempDataList[i])
                    }
                    if (brandTAndCMoreDataFlag == "1") {
                       // brandField57Data = "$brandRequestType^$brandTAndCTotalRecord"
                        getBrandTnc(brandTAndCTotalRecord.toString())
                    } else {
                        if (brandTermsAndConditionsDataList.isNotEmpty()) {
                            for (i in 0 until brandTermsAndConditionsDataList.size) {
                                val brandModel = BrandTAndCTable()
                                if (!TextUtils.isEmpty(brandTermsAndConditionsDataList[i])) {
                                    val splitData = Utility().parseDataListWithSplitter(
                                        SplitterTypes.CARET.splitter,
                                        brandTermsAndConditionsDataList[i]
                                    )
                                    brandModel.brandId = splitData[0]
                                    brandModel.brandTAndC = splitData[1]
                                    // saving data to db
                                    appDB.appDao.insertBrandTAndCData(brandModel)

                                }
                            }
                            getIssuerTnc()

                        }
                    }
                }
            }

    }
    //endregion

    //region=================================Stubbing BrandEMI Master SubCategory Data and Display in List:-
    private suspend fun stubbingBrandEMIMasterSubCategoryDataToList(
        brandEMIMasterSubCategoryData: String
    ) {
        var counter="0"
            if (!TextUtils.isEmpty(brandEMIMasterSubCategoryData)) {
                val dataList = Utility().parseDataListWithSplitter("|", brandEMIMasterSubCategoryData)
                if (dataList.isNotEmpty()) {
                    moreDataFlag = dataList[0]
                    perPageRecord = dataList[1]
                    counter = (totalRecord?.toInt()?.plus(perPageRecord?.toInt() ?: 0)).toString()
                    //Store DataList in Temporary List and remove first 2 index values to get sublist from 2nd index till dataList size
                    // and iterate further on record data only:-
                    var tempDataList = mutableListOf<String>()
                    tempDataList = dataList.subList(2, dataList.size)
                    for (i in tempDataList.indices) {
                        //Below we are splitting Data from tempDataList to extract brandID , categoryID , parentCategoryID , categoryName:-
                        if (!TextUtils.isEmpty(tempDataList[i])) {
                            val splitData =  Utility().parseDataListWithSplitter(
                                SplitterTypes.CARET.splitter,
                                tempDataList[i]
                            )
                            brandEmiMasterSubCategoryDataList.add(
                                BrandEMIMasterSubCategoryDataModal(
                                    splitData[0], splitData[1],
                                    splitData[2], splitData[3]
                                )
                            )
                        }
                    }

                    //Notify RecyclerView DataList on UI with Category Data that has ParentCategoryID == 0 && BrandID = selected brandID :-
                    val totalDataList = brandEmiMasterSubCategoryDataList
                    Log.d("TotalDataList:- ", Gson().toJson(totalDataList))

                    //Refresh Field57 request value for Pagination if More Record Flag is True:-
                    if (moreDataFlag == "1") {
                      /*  field57RequestData =
                            "${EMIRequestType.BRAND_SUB_CATEGORY.requestType}^$totalRecord^${brandEmiMasterDataList[0].brandID}"
                        fetchBrandEMIMasterSubCategoryDataFromHost()*/
                        getBrandSubCategoryData(counter)
                        Log.d("FullDataList:- ", brandEmiMasterSubCategoryDataList.toString())
                    } else {
                        withContext(Dispatchers.Main) {
                        //    if (brandEmiMasterSubCategoryDataList.isEmpty()) {
                         //       navigateToProductPage(isSubCategoryItem = false, -1)
                                // todo navigate product page
                        //    } else {
                                withContext(Dispatchers.IO) {
                                    saveAllSubCategoryDataInDB(brandEmiMasterSubCategoryDataList)
                                }
                                Log.e(
                                    "Sub Category Data:- ",
                                    Gson().toJson(brandEmiMasterSubCategoryDataList)
                                )
                                brandEMIMasterCategoryMLData.postValue(GenericResponse.Success(brandEmiMasterDataList))



                                /*  brandEMIAllDataList = brandEmiMasterSubCategoryDataList

                                  //region=====================Line added to resolve category only issue===================== By Manish
                                  brandEmiMasterSubCategoryDataList =
                                      brandEmiMasterSubCategoryDataList.filter {
                                          it.brandID == brandEMIDataModal?.brandID && it.parentCategoryID == "0"
                                      } as MutableList<BrandEMIMasterSubCategoryDataModal>
                                  brandEmiMasterSubCategoryDataListUpdate = brandEmiMasterSubCategoryDataList
                                  //region=====================Line added to resolve category only issue end=====================

                                  brandEMIMasterSubCategoryAdapter.refreshAdapterList(
                                      brandEmiMasterSubCategoryDataList
                                  )*/
                        //    }
                           // iDialog?.hideProgress()
                        }
                    }
                }
            } else {
               /* withContext(Dispatchers.Main) {
                    iDialog?.hideProgress()
                    *//*iDialog?.alertBoxWithAction(null, null,
                        getString(R.string.error), hostMsg,
                        false, getString(R.string.positive_button_ok),
                        {}, {})*//*
                }*/
                // todo in case of empty subcatlist
            }

    }
    //endregion

    //region=================================Stubbing BrandEMI Product Data and Display in List:-
    private suspend fun stubbingBrandEMIProductDataToList(brandEMIProductData: String, brandID: String?,catagoryID:String?) {
            if (!TextUtils.isEmpty(brandEMIProductData)) {
                val dataList = Utility().parseDataListWithSplitter("|", brandEMIProductData)
                if (dataList.isNotEmpty()) {
                    moreDataFlag = dataList[0]
                    perPageRecord = dataList[1]
                    totalRecord = (totalRecord?.toInt()?.plus(perPageRecord?.toInt() ?: 0)).toString()
                    //Store DataList in Temporary List and remove first 2 index values to get sublist from 2nd index till dataList size
                    // and iterate further on record data only:-
                    var tempDataList = mutableListOf<String>()
                    tempDataList = dataList.subList(2, dataList.size)
                    for (i in tempDataList.indices) {
                        //Below we are splitting Data from tempDataList to extract brandID , categoryID , parentCategoryID , categoryName:-
                        if (!TextUtils.isEmpty(tempDataList[i])) {
                            val splitData =Utility().parseDataListWithSplitter(
                                SplitterTypes.CARET.splitter,
                                tempDataList[i]
                            )
                            brandEmiProductDataList.add(
                                BrandEMIProductDataModal(
                                    splitData[0], splitData[1],
                                    splitData[2], splitData[3],
                                    splitData[4], splitData[5],
                                    splitData[6], splitData[7],
                                    splitData[8], splitData[9],
                                    splitData[10],"","subCat")
                            )
                        }
                    }

                    if (brandEmiProductDataList.isNotEmpty()) {
                      //  binding?.emptyViewPlaceholder?.visibility = View.INVISIBLE
                      //  brandEMIProductAdapter.refreshAdapterList(brandEmiProductDataList)
                        println("Product List is Empty")

                    }

                    //Refresh Field57 request value for Pagination if More Record Flag is True:-
                    if (moreDataFlag == "1") {
                     getBrandEmiProductData(totalRecord,brandID ,catagoryID)
                        Log.d("FullDataList:- ", brandEmiProductDataList.toString())
                    } else {
                        brandEMIProductMLData.postValue(GenericResponse.Success(brandEmiProductDataList))

                        Log.d("Full Product Data:- ", Gson().toJson(brandEmiProductDataList))
                    }
                }
            } else {
              //  Empty product data in f57
            }

    }
    //endregion


    //region==============Save Brand Master Data TimeStamps:-
    private suspend fun saveBrandMasterTimeStampsData() {
        val model = BrandEMIMasterTimeStamps()
        model.brandTimeStamp = brandTimeStamp ?: ""
        model.brandCategoryUpdatedTimeStamp = brandCategoryUpdatedTimeStamp ?: ""
        model.issuerTAndCTimeStamp = issuerTAndCTimeStamp ?: ""
        model.brandTAndCTimeStamp = brandTAndCTimeStamp ?: ""
        appDB.appDao.insertBrandEMIMasterTimeStamps(model)
    }
    //endregion
    //region=======================Check Whether we got Updated Data from Host or to use Previous BrandEMIMaster Store Data:-
    private suspend fun matchHostAndDBTimeStamp(): Boolean {
        val dbTimeStamps = appDB.appDao.getBrandTimeStampFromDB()
        var isDataMatch=false
        if (!dbTimeStamps?.brandTAndCTimeStamp.isNullOrBlank() &&
            !dbTimeStamps?.issuerTAndCTimeStamp.isNullOrBlank() &&
            !dbTimeStamps?.brandCategoryUpdatedTimeStamp.isNullOrBlank()) {
            isDataMatch = issuerTAndCTimeStamp == dbTimeStamps?.issuerTAndCTimeStamp &&
                    brandTAndCTimeStamp == dbTimeStamps?.brandTAndCTimeStamp &&
                    brandCategoryUpdatedTimeStamp==dbTimeStamps?.brandCategoryUpdatedTimeStamp
        }
        return isDataMatch
    }
    //endregion

    //region======================save all sub-category data in DB:-
    private suspend fun saveAllSubCategoryDataInDB(subCategoryDataList: MutableList<BrandEMIMasterSubCategoryDataModal>) {
        val modal = BrandEMISubCategoryTable()
       // BrandEMISubCategoryTable.clear()
        for (value in subCategoryDataList) {
            modal.brandID = value.brandID
            modal.categoryID = value.categoryID
            modal.parentCategoryID = value.parentCategoryID
            modal.categoryName = value.categoryName
            appDB.appDao.insertBrandEMISubCategoryData(modal)
          //  BrandEMISubCategoryTable.performOperation(modal)
        }
    }
    //endregion
}