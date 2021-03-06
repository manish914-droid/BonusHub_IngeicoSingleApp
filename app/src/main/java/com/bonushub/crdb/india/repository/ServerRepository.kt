package com.bonushub.crdb.india.repository

import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import com.bonushub.crdb.india.db.AppDatabase
import com.bonushub.crdb.india.model.local.BrandEMISubCategoryTable
import com.bonushub.crdb.india.model.local.BrandEMITimeStamps
import com.bonushub.crdb.india.model.local.BrandTAndCTable
import com.bonushub.crdb.india.model.local.IssuerTAndCTable
import com.bonushub.crdb.india.model.remote.*
import com.bonushub.crdb.india.serverApi.EMIRequestType
import com.bonushub.crdb.india.serverApi.RemoteService
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.parseDataListWithSplitter
import com.bonushub.crdb.india.utils.SplitterTypes
import com.bonushub.crdb.india.utils.Utility
import com.bonushub.crdb.india.utils.logger
import com.bonushub.crdb.india.view.fragments.IssuerBankModal
import com.bonushub.crdb.india.view.fragments.TenureBankModal

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ServerRepository @Inject constructor(val appDB: AppDatabase, private val remoteService: RemoteService) {

    // Live objs for
    private val brandEMIMasterCategoryMLData =
        MutableLiveData<GenericResponse<List<BrandEMIMasterDataModal?>>>()
    val brandLiveEMIMasterCategoryData: LiveData<GenericResponse<List<BrandEMIMasterDataModal?>>>
        get() = brandEMIMasterCategoryMLData


    private val brandEMIProductMLData =
        MutableLiveData<GenericResponse<List<BrandEMIProductDataModal?>>>()
    val brandLiveEMIProductData: LiveData<GenericResponse<List<BrandEMIProductDataModal?>>>
        get() = brandEMIProductMLData

    private val emiTenureMLData = MutableLiveData<GenericResponse<TenuresWithIssuerTncs?>>()
    val emiTenureLiveData: LiveData<GenericResponse<TenuresWithIssuerTncs?>>
        get() = emiTenureMLData

    private val brandEMIAccessCodeMLData =
        MutableLiveData<GenericResponse<BrandEMIbyCodeDataModal?>>()
    val brandEMIAccessCodeLiveData: LiveData<GenericResponse<BrandEMIbyCodeDataModal?>>
        get() = brandEMIAccessCodeMLData


    private var moreDataFlag = "0"
    private var totalRecord = "0"

    // Getting brands
    private var brandTotalRecord = "0"
    private var brandCategoryTotalRecord = "0"


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

    private var brandEmiMasterSubCategoryDataList =
        mutableListOf<BrandEMIMasterSubCategoryDataModal>()

    // endregion ========

    private val brandEmiProductDataList by lazy { mutableListOf<BrandEMIProductDataModal>() }
    private val brandEmiSearchedProductDataList by lazy { mutableListOf<BrandEMIProductDataModal>() }

    private val serialNumBlockUnblockML = MutableLiveData<Boolean>()
    val serialNumBlockUnblockL: LiveData<Boolean>
        get() = serialNumBlockUnblockML


    // === EMI Tenure
    lateinit var bankEMIIssuerTAndCList: BankEMIIssuerTAndCDataModal
    private var bankEMISchemesDataList: MutableList<BankEMITenureDataModal> = mutableListOf()

    //region====================EMIIssuerList Variables:-
    private var allIssuerBankList: MutableList<IssuerBankModal> = mutableListOf()
    private var allIssuerTenureList: MutableList<TenureBankModal> = mutableListOf()
    private val allIssuerTenureListMLData =
        MutableLiveData<GenericResponse<List<TenureBankModal?>>>()
    val allIssuerTenureLisLiveData: LiveData<GenericResponse<List<TenureBankModal?>>>
        get() = allIssuerTenureListMLData
    private val issuerListMLData = MutableLiveData<GenericResponse<List<IssuerBankModal?>>>()
    val allIssuerBankListLiveData: LiveData<GenericResponse<List<IssuerBankModal?>>>
        get() = issuerListMLData


    //endregion

    suspend fun getBrandData(dataCounter: String = "0") {
        // val genericResp = remoteService.getBrandDataService(dataCounter)
        //   brandMLData.postValue(genericResp)
        val field57 = "${EMIRequestType.BRAND_DATA.requestType}^$dataCounter"
        when (val genericResp = remoteService.field57GenericService(field57)) {
            is GenericResponse.Success -> {
                val isoDataReader = genericResp.data
                val brandEMIMasterData =
                    isoDataReader?.isoMap?.get(57)?.parseRaw2String().toString()
                stubbingBrandEMIMasterDataToList(brandEMIMasterData)
            }
            is GenericResponse.Error -> {
                brandEMIMasterCategoryMLData.postValue(GenericResponse.Error(genericResp.errorMessage.toString()))
            }
            is GenericResponse.Loading -> {

            }
        }
    }

    private suspend fun getBrandTnc(dataCounter: String = "0") {
        val field57 = "${EMIRequestType.BRAND_T_AND_C.requestType}^$dataCounter"
        when (val genericResp = remoteService.field57GenericService(field57)) {
            is GenericResponse.Success -> {
                val isoDataReader = genericResp.data
                val tncString = isoDataReader?.isoMap?.get(57)?.parseRaw2String().toString()
                stubbingBrandTAndCData(tncString)
            }
            is GenericResponse.Error -> {
                brandEMIMasterCategoryMLData.postValue(GenericResponse.Error(genericResp.errorMessage.toString()))
            }
            is GenericResponse.Loading -> {

            }
        }
    }

    private suspend fun getIssuerTnc(dataCounter: String = "0", fromBankEmi: Boolean) {
        val field57 = "${EMIRequestType.ISSUER_T_AND_C.requestType}^$dataCounter"
        when (val genericResp = remoteService.field57GenericService(field57)) {
            is GenericResponse.Success -> {
                val isoDataReader = genericResp.data
                val tncString = isoDataReader?.isoMap?.get(57)?.parseRaw2String().toString()
                stubbingIssuerTAndCData(tncString, fromBankEmi)
            }
            is GenericResponse.Error -> {
                brandEMIMasterCategoryMLData.postValue(GenericResponse.Error(genericResp.errorMessage.toString()))
            }
            is GenericResponse.Loading -> {

            }
        }
    }

    private suspend fun getBrandSubCategoryData(dataCounter: String) {

        val field57 =
            "${EMIRequestType.BRAND_SUB_CATEGORY.requestType}^$dataCounter^${brandEmiMasterDataList[0].brandID}"

        when (val genericResp = remoteService.field57GenericService(field57)) {
            is GenericResponse.Success -> {
                val isoDataReader = genericResp.data
                val brandSubCatDataString =
                    isoDataReader?.isoMap?.get(57)?.parseRaw2String().toString()
                stubbingBrandEMIMasterSubCategoryDataToList(brandSubCatDataString)
            }
            is GenericResponse.Error -> {
                brandEMIMasterCategoryMLData.postValue(GenericResponse.Error(genericResp.errorMessage.toString()))
            }
            is GenericResponse.Loading -> {

            }
        }


    }

    suspend fun getBrandEmiProductData(
        dataCounter: String = "0",
        brandID: String?,
        categoryID: String?,
        searchedProductName: String = "",
        isSearchData: Boolean = false,
        callFromViewModel: Boolean = false
    ) {
        var field57 = ""
        field57 = if (isSearchData) {
            //             field57 = "${EMIRequestType.BRAND_EMI_Product.requestType}^$dataCounter^${brandID}^${categoryID}"
            //             field57RequestData = "${EMIRequestType.BRAND_EMI_Product.requestType}^0^${brandEMIDataModal?.brandID}^${brandEMIDataModal?.categoryID}"
            //             field57RequestData = "${EMIRequestType.BRAND_EMI_Product_WithCategory.requestType}^$totalRecord^${brandEMIDataModal?.brandID}^^$searchedProductName"
            "${EMIRequestType.BRAND_EMI_Product_WithCategory.requestType}^$dataCounter^${brandID}^^$searchedProductName"

        } else {
            "${EMIRequestType.BRAND_EMI_Product.requestType}^$dataCounter^${brandID}^${categoryID}"
        }

        if (callFromViewModel) {
            brandEmiProductDataList.clear()
        }

        when (val genericResp = remoteService.field57GenericService(field57)) {
            is GenericResponse.Success -> {
                val isoDataReader = genericResp.data
                val brandEmiProductDataString =
                    isoDataReader?.isoMap?.get(57)?.parseRaw2String().toString()
                stubbingBrandEMIProductDataToList(
                    brandEmiProductDataString,
                    brandID,
                    categoryID,
                    searchedProductName,
                    isSearchData
                )

            }
            is GenericResponse.Error -> {
                brandEMIProductMLData.postValue(GenericResponse.Error(genericResp.errorMessage.toString()))
            }
            is GenericResponse.Loading -> {

            }
        }


    }

    suspend fun getEMITenureData(field56Pan: String = "0", field57: String, counter: String = "0") {
        /* val field57=  "$bankEMIRequestCode^0^${brandEmiData?.brandID}^${brandEmiData?.productID}^${brandEmiData?.imeiORserailNum}" +
                 "^${*//*cardBinValue.substring(0, 8)*//*""}^$transactionAmount"
        */
        // val field57="4^0^25^2454^^^5800000"
        when (val genericResp = remoteService.getEMITenureService(field56Pan, field57)) {
            is GenericResponse.Success -> {
                val isoDataReader = genericResp.data
                val tenureTnc = isoDataReader?.isoMap?.get(57)?.parseRaw2String().toString()
                Log.e("Tenure", tenureTnc)
                stubbingEMITenureDataToList(tenureTnc, field56Pan, field57)
            }
            is GenericResponse.Error -> {
                emiTenureMLData.postValue(GenericResponse.Error(genericResp.errorMessage.toString()))
            }
            is GenericResponse.Loading -> {

            }
        }
    }

    suspend fun getIssuerList(field57RequestData: String) {
        totalRecord = "0"


        Log.d("field57:- ", field57RequestData)
        when (val genericResp = remoteService.field57GenericService(field57RequestData)) {
            is GenericResponse.Success -> {
                val isoDataReader = genericResp.data
                val issuerListData = isoDataReader?.isoMap?.get(57)?.parseRaw2String().toString()
                parseAndStubbingBankEMICatalogueDataToList(issuerListData)
            }
            is GenericResponse.Error -> {
                Log.d("error:- ", "in error")
                allIssuerTenureListMLData.postValue(GenericResponse.Error(genericResp.errorMessage.toString()))
                issuerListMLData.postValue(GenericResponse.Error(genericResp.errorMessage.toString()))
            }
            is GenericResponse.Loading -> {

            }
        }
    }

    suspend fun blockUnblockSerialNum(field57RequestData: String): Pair<Boolean, String> {
        return when (val genericResp = remoteService.field57GenericService(field57RequestData)) {
            is GenericResponse.Success -> {
                val isoDataReader = genericResp.data
                val respMsg = isoDataReader?.isoMap?.get(58)?.parseRaw2String().toString()
                //serialNumBlockUnblockML.postValue(true)
                Pair(true, respMsg)
            }
            is GenericResponse.Error -> {
                // serialNumBlockUnblockML.postValue(false)
                Pair(false, genericResp.errorMessage.toString())
            }
            is GenericResponse.Loading -> {
                Pair(false, "Loading State")
            }
        }


    }


    suspend fun getBrandEmiByCodeData(
        field56Pan: String = "0",
        field57: String,
        counter: String = "0"
    ) {

        when (val genericResp = remoteService.getEMITenureService(field56Pan, field57)) {
            is GenericResponse.Success -> {
                val isoDataReader = genericResp.data
                val brandByCodeDataStr =
                    isoDataReader?.isoMap?.get(57)?.parseRaw2String().toString()
                Log.e("Brand By Code :- ", brandByCodeDataStr)
                stubbingBrandEMIAccessCodeDataToList(brandByCodeDataStr)
            }
            is GenericResponse.Error -> {
                brandEMIAccessCodeMLData.postValue(GenericResponse.Error(genericResp.errorMessage.toString()))
            }
            is GenericResponse.Loading -> {

            }
        }
    }

    //region=================================Stubbing BrandEMI Master Data to List:-
    private suspend fun stubbingBrandEMIMasterDataToList(brandEMIMasterData: String) {
        val dataList = Utility().parseDataListWithSplitter("|", brandEMIMasterData)
        if (dataList.isNotEmpty()) {
            moreDataFlag = dataList[0]
            perPageRecord = dataList[1]
            brandTimeStamp = dataList[2]
            brandCategoryUpdatedTimeStamp = dataList[3]
            issuerTAndCTimeStamp = dataList[4]
            brandTAndCTimeStamp = dataList[5]
            brandTotalRecord =
                (brandTotalRecord.toInt().plus(perPageRecord?.toInt() ?: 0)).toString()

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
                        val brandData = Utility().parseDataListWithSplitter(
                            SplitterTypes.CARET.splitter,
                            tempDataList[i]
                        )
                        brandEmiMasterDataList.add(
                            BrandEMIMasterDataModal(
                                brandData[0],
                                brandData[1],
                                brandData[2]
                            )
                        )
                    }
                }
            }
            //Refresh Field57 request value for Pagination if More Record Flag is True:-
            if (moreDataFlag == "1") {
                getBrandData(brandTotalRecord)
            } else {
                if (matchBrandTncTimeStamp(brandTAndCTimeStamp ?: "")) {
                   Log.i("matchBrandTncTimeStamp","Brand Tnc time stamp matched")

                } else {
                    // saveBrandMasterTimeStampsData()
                    getBrandTnc()
                }
                if (matchIssuerTncTimeStamp(issuerTAndCTimeStamp ?: "")) {
                    Log.i("matchIssuerTncTimeStamp","Issuer Tnc time stamp matched")

                } else {
                    getIssuerTnc(fromBankEmi = false)
                }

                if (matchCategoryTimeStamp(brandCategoryUpdatedTimeStamp ?: "")) {
                    Log.i("matchCategoryTimeStamp","Category time stamp matched")

                } else {
                    //  if(!fromBankEmi)
                    getBrandSubCategoryData("0")
                }

                withContext(Dispatchers.IO) {
                    brandEMIMasterCategoryMLData.postValue(
                        GenericResponse.Success(
                            brandEmiMasterDataList
                        )
                    )
                }

            }
        }
        else {
            brandEMIMasterCategoryMLData.postValue(GenericResponse.Error("Brand Data List is Empty"))
        }
    }

    //endregion
    //region===========================Below method is used to Stubbing issuer terms and conditions data:-
    private suspend fun stubbingIssuerTAndCData(issuerTAndC: String, fromBankEmi: Boolean) {
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
                    getIssuerTnc(issuerTAndCTotalRecord.toString(), fromBankEmi)

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
                        appDB.appDao.insertIssuerTncTimeStamp(issuerTAndCTimeStamp?:"")

                       /* if (!matchCategoryTimeStamp(brandCategoryUpdatedTimeStamp ?: "")){
                            if(!fromBankEmi)
                            getBrandSubCategoryData("0")
                        }*/



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
                       /* if (!matchIssuerTncTimeStamp(issuerTAndCTimeStamp ?: "")){
                            getIssuerTnc(fromBankEmi = false)
                        }*/
                        appDB.appDao.insertBrandTncTimeStamp(brandTAndCTimeStamp?:"")


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
        // var counter="0"
        if (!TextUtils.isEmpty(brandEMIMasterSubCategoryData)) {
            val dataList = Utility().parseDataListWithSplitter("|", brandEMIMasterSubCategoryData)
            if (dataList.isNotEmpty()) {
                moreDataFlag = dataList[0]
                perPageRecord = dataList[1]
                brandCategoryTotalRecord =
                    (brandCategoryTotalRecord.toInt().plus(perPageRecord?.toInt() ?: 0)).toString()
                //Store DataList in Temporary List and remove first 2 index values to get sublist from 2nd index till dataList size
                // and iterate further on record data only:-
                var tempDataList = mutableListOf<String>()
                tempDataList = dataList.subList(2, dataList.size)
                for (i in tempDataList.indices) {
                    //Below we are splitting Data from tempDataList to extract brandID , categoryID , parentCategoryID , categoryName:-
                    if (!TextUtils.isEmpty(tempDataList[i])) {
                        val splitData = Utility().parseDataListWithSplitter(
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

                    getBrandSubCategoryData(brandCategoryTotalRecord)
                    Log.d("FullDataList:- ", brandEmiMasterSubCategoryDataList.toString())
                } else {
                    withContext(Dispatchers.Main) {
                        withContext(Dispatchers.IO) {
                            saveAllSubCategoryDataInDB(brandEmiMasterSubCategoryDataList)

                        }
                        Log.e(
                            "Sub Category Data:- ",
                            Gson().toJson(brandEmiMasterSubCategoryDataList)
                        )
                      /*  brandEMIMasterCategoryMLData.postValue(
                            GenericResponse.Success(
                                brandEmiMasterDataList
                            )
                        )*/

                    }
                }
            }
        } else {
           /* withContext(Dispatchers.IO) {
                saveAllSubCategoryDataInDB(brandEmiMasterSubCategoryDataList)
            }
            Log.e(
                "Sub Category Data:- ",
                Gson().toJson(brandEmiMasterSubCategoryDataList)
            )
            brandEMIMasterCategoryMLData.postValue(GenericResponse.Success(brandEmiMasterDataList))
*/
        }

    }
    //endregion


    //region=================Parse and Stubbing BankEMI Data To List:-
    private fun parseAndStubbingBankEMICatalogueDataToList(bankEMICatalogueHostResponseData: String) {

        if (!TextUtils.isEmpty(bankEMICatalogueHostResponseData)) {
            val parsingDataWithVerticalLineSeparator =
                Utility().parseDataListWithSplitter("|", bankEMICatalogueHostResponseData)
            if (parsingDataWithVerticalLineSeparator.isNotEmpty()) {
                moreDataFlag = parsingDataWithVerticalLineSeparator[0]
                perPageRecord = parsingDataWithVerticalLineSeparator[1]
                totalRecord = (totalRecord.toInt().plus(perPageRecord?.toInt() ?: 0)).toString()
                //Store DataList in Temporary List and remove first 2 index values to get sublist from 2nd index till dataList size
                // and iterate further on record data only:-
                var tempDataList = mutableListOf<String>()
                tempDataList = parsingDataWithVerticalLineSeparator.subList(
                    2,
                    parsingDataWithVerticalLineSeparator.size
                )

                //region=========================Stub Data in AllIssuerList:-
                for (i in tempDataList.indices) {
                    if (!TextUtils.isEmpty(tempDataList[i])) {
                        val splitData = Utility().parseDataListWithSplitter(
                            SplitterTypes.CARET.splitter,
                            tempDataList[i]
                        )
                        allIssuerBankList.add(
                            IssuerBankModal(
                                splitData[0], splitData[1],
                                splitData[2], splitData[3],
                                splitData[4], splitData[5],
                                splitData[6], splitData[7],
                                splitData[8], splitData[9],
                                splitData[10], splitData[11],
                                splitData[12], splitData[13],
                                splitData[14], splitData[15],
                                splitData[16], splitData[17],
                                splitData[18], splitData[19],
                                splitData[20], splitData[21],
                                splitData[22], splitData[23]
                            )
                        )
                    }
                }
                //endregion
                //region========================Stub Data in AllTenureList:-
                if (allIssuerBankList.isNotEmpty()) {
                    val dataLength = allIssuerBankList.size
                    for (i in 0 until dataLength)
                        allIssuerTenureList.add(
                            TenureBankModal(
                                allIssuerBankList[i].issuerBankTenure,
                                isTenureSelected = false
                            )
                        )
                    issuerListMLData.postValue(
                        GenericResponse.Success(
                            allIssuerBankList
                        )
                    )
                    allIssuerTenureListMLData.postValue(
                        GenericResponse.Success(
                            allIssuerTenureList
                        )
                    )


                }
                //endregion
                Log.d("AllIssuerList:- ", allIssuerBankList.toString())
                Log.d("AllTenureList:- ", allIssuerTenureList.toString())


            }
        }
    }

    //region=================================Stubbing BrandEMI Product Data and Display in List:-
    private suspend fun stubbingBrandEMIProductDataToList(
        brandEMIProductData: String,
        brandID: String?,
        catagoryID: String?,
        searchedProductName: String = "",
        isSearchData: Boolean = false
    ) {
        if (!TextUtils.isEmpty(brandEMIProductData)) {
            val dataList = Utility().parseDataListWithSplitter("|", brandEMIProductData)
            if (dataList.isNotEmpty()) {
                logger("moreDataFlag", "serverSide " + dataList[0], "e")
                moreDataFlag = dataList[0]
                perPageRecord = dataList[1]
                totalRecord = (totalRecord.toInt().plus(perPageRecord?.toInt() ?: 0)).toString()
                //Store DataList in Temporary List and remove first 2 index values to get sublist from 2nd index till dataList size
                // and iterate further on record data only:-
                var tempDataList = mutableListOf<String>()
                tempDataList = dataList.subList(2, dataList.size)
                for (i in tempDataList.indices) {
                    //Below we are splitting Data from tempDataList to extract brandID , categoryID , parentCategoryID , categoryName:-
                    if (!TextUtils.isEmpty(tempDataList[i])) {
                        val splitData = Utility().parseDataListWithSplitter(
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
                                splitData[10], "", "subCat"
                            )
                        )
                    }
                }

                if (brandEmiProductDataList.isNotEmpty()) {
                    //  binding?.emptyViewPlaceholder?.visibility = View.INVISIBLE
                    //  brandEMIProductAdapter.refreshAdapterList(brandEmiProductDataList)
                    println("Product List is Empty")

                }

                //Refresh Field57 request value for Pagination if More Record Flag is True:-
                logger("moreDataFlag", "" + moreDataFlag, "e")
                if (moreDataFlag == "1") {
                    logger("brandEmiProductDataList", "" + brandEmiProductDataList.size)
                    Log.d("FullDataList:- ", brandEmiProductDataList.toString())
                    if (isSearchData) {
                        getBrandEmiProductData(
                            totalRecord,
                            brandID,
                            catagoryID,
                            searchedProductName,
                            isSearchData
                        )
                    } else {
                        getBrandEmiProductData(totalRecord, brandID, catagoryID)
                    }

                } else {
                    logger("brandEmiProductDataList", "" + brandEmiProductDataList.size)
                    brandEMIProductMLData.postValue(GenericResponse.Success(brandEmiProductDataList))
                    Log.d("Full Product Data:- ", Gson().toJson(brandEmiProductDataList))
                }
            }
        } else {
            //  Empty product data in f57
            Log.d("List:- ", "List is empty")
            //brandEMIMasterCategoryMLData .postValue(GenericResponse.Error("List is empty"))
            //brandEMIMasterCategoryMLData .postValue(GenericResponse.Success(mutableListOf<BrandEMIProductDataModal>()))
            brandEMIProductMLData.postValue(GenericResponse.Success(brandEmiProductDataList))
        }

    }
    //endregion

    //region=================Parse and Stubbing BankEMI Data To List:-
    private suspend fun stubbingEMITenureDataToList(
        bankEMITenureResponseData: String, field56Pan: String = "0", field57: String
    ) {
        if (!TextUtils.isEmpty(bankEMITenureResponseData)) {
            val parsingDataWithCurlyBrace =
                Utility().parseDataListWithSplitter("}", bankEMITenureResponseData)
            val parsingDataWithVerticalLineSeparator =
                Utility().parseDataListWithSplitter("|", parsingDataWithCurlyBrace[0])
            if (parsingDataWithVerticalLineSeparator.isNotEmpty()) {
                moreDataFlag = parsingDataWithVerticalLineSeparator[0]
                perPageRecord = parsingDataWithVerticalLineSeparator[1]
                totalRecord = (totalRecord.toInt().plus(perPageRecord?.toInt() ?: 0)).toString()
                //Store DataList in Temporary List and remove first 2 index values to get sublist from 2nd index till dataList size
                // and iterate further on record data only:-
                var tempDataList = mutableListOf<String>()
                tempDataList = parsingDataWithVerticalLineSeparator.subList(
                    2,
                    parsingDataWithVerticalLineSeparator.size
                )

                for (i in tempDataList.indices) {
                    if (!TextUtils.isEmpty(tempDataList[i])) {
                        val splitData = Utility().parseDataListWithSplitter(
                            SplitterTypes.CARET.splitter,
                            tempDataList[i]
                        )
                        bankEMISchemesDataList.add(
                            BankEMITenureDataModal(
                                splitData[0], splitData[1],
                                splitData[2], splitData[3],
                                splitData[4], splitData[5],
                                splitData[6], splitData[7],
                                splitData[8], splitData[9],
                                splitData[10], splitData[11],
                                splitData[12], splitData[13],
                                splitData[14], splitData[15],
                                splitData[16], splitData[17],
                                splitData[18], splitData[19],
                                splitData[20], splitData[21], splitData[22]
                            )
                        )
                    }
                }

                //Parsing and Stubbing BankEMI TAndC Data:-
                val issuerRelatedData = Utility().parseDataListWithSplitter(
                    SplitterTypes.VERTICAL_LINE.splitter,
                    parsingDataWithCurlyBrace[1]
                )
                if (!TextUtils.isEmpty(issuerRelatedData[0])) {
                    val issuerTAndCData = Utility().parseDataListWithSplitter(
                        SplitterTypes.CARET.splitter,
                        issuerRelatedData[0]
                    )
                    issuerTAndCTimeStamp=issuerRelatedData[1]
                    Log.d("IssuerSchemeID", issuerTAndCData[0])
                    Log.d("IssuerID", issuerTAndCData[1])
                    Log.d("IssuerName", issuerTAndCData[2])
                    if (issuerTAndCData.size > 3) {
                        bankEMIIssuerTAndCList =
                            BankEMIIssuerTAndCDataModal(
                                issuerTAndCData[0],
                                issuerTAndCData[1],
                                issuerTAndCData[2],
                                issuerTAndCData[3],
                                issuerRelatedData[1]
                            )

                    } else {
                        bankEMIIssuerTAndCList
                        BankEMIIssuerTAndCDataModal(
                            issuerTAndCData[0],
                            issuerTAndCData[1],
                            issuerTAndCData[2],
                            "",
                            issuerRelatedData[1]
                        )

                    }
                }
                //Refresh Field57 request value for Pagination if More Record Flag is True:-
                if (moreDataFlag == "1") {
                    getEMITenureData(field56Pan, field57, counter = totalRecord)
                } else {
                    Log.d("Total BankEMI Data:- ", bankEMISchemesDataList.toString())
                    Log.d("Total BankEMI TAndC:- ", parsingDataWithCurlyBrace[1])
                    val tenuresWithIssuerTncs = TenuresWithIssuerTncs(bankEMIIssuerTAndCList, bankEMISchemesDataList)
                    if (matchIssuerTncTimeStamp(issuerTAndCTimeStamp ?: "")) {
                        Log.i("matchIssuerTncTimeStamp","Issuer Tnc time stamp matched")
                    } else {
                        getIssuerTnc(fromBankEmi = true)
                    }
                    emiTenureMLData.postValue(GenericResponse.Success(tenuresWithIssuerTncs))
                }
            }
        } else {
            emiTenureMLData.postValue(GenericResponse.Error("Empty Tenure data received from server"))
        }

        //endregion
    }

    //region==============Save Brand Master Data TimeStamps:-
    private suspend fun saveBrandMasterTimeStampsData() {
        val model = BrandEMITimeStamps()
        model.brandTimeStamp = brandTimeStamp ?: ""
        model.brandCategoryUpdatedTimeStamp = brandCategoryUpdatedTimeStamp ?: ""
        model.issuerTAndCTimeStamp = issuerTAndCTimeStamp ?: ""
        model.brandTAndCTimeStamp = brandTAndCTimeStamp ?: ""
        appDB.appDao.deleteBrandEMITimeStamps()
        appDB.appDao.insertBrandEMITimeStamps(model)
    }



    //endregion
    //region=======================Check Whether we got Updated Data from Host or to use Previous BrandEMIMaster Store Data:-
    private suspend fun matchHostAndDBTimeStamp(): Boolean {
        var isDataMatch = false
        withContext(Dispatchers.IO) {
            val dbTimeStamps = appDB.appDao.getBrandTimeStampFromDB()
            /* val brandTnc = appDB.appDao.getAllBrandTAndCData()
             val issuerTnC = appDB.appDao.getAllIssuerTAndCData()
             val brandCatSubCat = appDB.appDao.getBrandEMISubCategoryData()
             val getBrandTAndC = Field48ResponseTimestamp.getBrandTAndCData()
             val terminalHasData =
                 !brandTnc.isNullOrEmpty() && !issuerTnC.isNullOrEmpty() && !brandCatSubCat.isNullOrEmpty()
 */
            if (!dbTimeStamps?.brandTAndCTimeStamp.isNullOrBlank() &&
                !dbTimeStamps?.issuerTAndCTimeStamp.isNullOrBlank() &&
                !dbTimeStamps?.brandCategoryUpdatedTimeStamp.isNullOrBlank()
            ) {

                isDataMatch =
                    issuerTAndCTimeStamp == dbTimeStamps?.issuerTAndCTimeStamp &&
                            brandTAndCTimeStamp == dbTimeStamps?.brandTAndCTimeStamp &&
                            brandCategoryUpdatedTimeStamp == dbTimeStamps?.brandCategoryUpdatedTimeStamp

            }
        }
        return isDataMatch

    }
    //endregion

    private suspend fun matchBrandTncTimeStamp(currentBrandTncTimeStamp: String): Boolean {
        var isDataMatch: Boolean
        withContext(Dispatchers.IO) {
            val masterTimeStamps = appDB.appDao.getBrandTimeStampFromDB()
            val savedBrandTAndCTimeStamp = masterTimeStamps?.brandTAndCTimeStamp

            isDataMatch = currentBrandTncTimeStamp == savedBrandTAndCTimeStamp


        }
        return isDataMatch

    }

    private suspend fun matchIssuerTncTimeStamp(currentIssuerTncTimeStamp: String): Boolean {

        var isDataMatch: Boolean
        withContext(Dispatchers.IO) {
            val masterTimeStamps = appDB.appDao.getBrandTimeStampFromDB()
            val savedIssuerTAndCTimeStamp = masterTimeStamps?.issuerTAndCTimeStamp

            isDataMatch = currentIssuerTncTimeStamp == savedIssuerTAndCTimeStamp


        }
        return isDataMatch
    }

    private suspend fun matchCategoryTimeStamp(currentCategoryTncTimeStamp: String): Boolean {
        var isDataMatch: Boolean
        withContext(Dispatchers.IO) {
            val masterTimeStamps = appDB.appDao.getBrandTimeStampFromDB()
            val savedCategoryTAndCTimeStamp = masterTimeStamps?.brandCategoryUpdatedTimeStamp

            isDataMatch = currentCategoryTncTimeStamp == savedCategoryTAndCTimeStamp


        }
        return isDataMatch
    }

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
            println("Stubbing brand --->  "+modal.brandID)
        }
       appDB.appDao.insertCategoryTimeStamp(brandCategoryUpdatedTimeStamp?:"")


    }
    //endregion

    //region=================================Stubbing BrandEMI Access Code Data and Display in List:-
    private suspend fun stubbingBrandEMIAccessCodeDataToList(brandEMIAccessData: String) {
        var brandEmiByCodeData: BrandEMIbyCodeDataModal? = null
        if (!TextUtils.isEmpty(brandEMIAccessData)) {
            val dataList = parseDataListWithSplitter("|", brandEMIAccessData)
            if (dataList.isNotEmpty()) {
                // and iterate further on record data only:-
                var tempDataList = mutableListOf<String>()
                tempDataList = dataList.subList(2, dataList.size)
                if (!TextUtils.isEmpty(tempDataList[0])) {
                    val splitData = parseDataListWithSplitter(
                        SplitterTypes.CARET.splitter, tempDataList[0]
                    )

                    brandEmiByCodeData = BrandEMIbyCodeDataModal(
                        splitData[0],
                        splitData[1],
                        splitData[2],
                        splitData[3],
                        splitData[4],
                        splitData[5],
                        splitData[6],
                        splitData[7],
                        splitData[8],
                        splitData[9],
                        splitData[10],
                        splitData[11],
                        splitData[12],
                        splitData[13],
                        splitData[14],
                        splitData[15],
                        splitData[16],
                        splitData[17],
                        splitData[18],
                        splitData[19],
                        splitData[20],
                        splitData[21],
                        splitData[22],
                        splitData[23],
                        splitData[24],
                        splitData[25],
                        splitData[26],
                        splitData[27],
                        splitData[28],
                        splitData[29],
                        splitData[30],
                        splitData[31],
                        splitData[32],
                        splitData[33],
                        splitData[34],
                        splitData[35],
                        splitData[36],
                        splitData[37],
                        splitData[38],
                        splitData[39],
                        splitData[40]
                    )
                }

                if (brandEmiByCodeData != null)
                    brandEMIAccessCodeMLData.postValue(GenericResponse.Success(brandEmiByCodeData))
            }
        }

    }
    //endregion

}