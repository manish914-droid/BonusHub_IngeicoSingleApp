package com.bonushub.crdb.repository


import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.model.remote.BrandEMIMasterSubCategoryDataModal
import com.bonushub.crdb.serverApi.EMIRequestType
import com.bonushub.crdb.serverApi.RemoteService
import com.bonushub.crdb.utils.Utility
import com.bonushub.pax.utils.SplitterTypes
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ServerRepository(private val remoteService: RemoteService) {

    private var moreDataFlag = "0"
    private var totalRecord: String? = null
    private var brandTimeStamp: String? = null
    private var brandCategoryUpdatedTimeStamp: String? = null
    private var issuerTAndCTimeStamp: String? = null
    private var brandTAndCTimeStamp: String? = null
    private var brandEmiMasterDataList = mutableListOf<BrandEMIMasterDataModal>()

    private val brandEMIMasterSubCategoryMLData = MutableLiveData<GenericResponse<List<BrandEMIMasterDataModal?>>>()
    val brandLiveEMIMasterSubCategoryData: LiveData<GenericResponse<List<BrandEMIMasterDataModal?>>>
        get() = brandEMIMasterSubCategoryMLData

    suspend fun getBrandData(dataCounter:String = "0") {
       // val genericResp = remoteService.getBrandDataService(dataCounter)
     //   brandMLData.postValue(genericResp)
        when(val genericResp = remoteService.getBrandDataService(dataCounter)){
            is GenericResponse.Success->{
                val isoDataReader=genericResp.data
                val brandEMIMasterData = isoDataReader?.isoMap?.get(57)?.parseRaw2String().toString()
               stubbingBrandEMIMasterDataToList(brandEMIMasterData)
            }
            is GenericResponse.Error->{
                brandEMIMasterSubCategoryMLData.postValue(GenericResponse.Error(genericResp.errorMessage.toString()))
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
                        brandEMIMasterSubCategoryMLData.postValue(GenericResponse.Success(brandEmiMasterDataList))
                    }
                } else {
                    brandEMIMasterSubCategoryMLData.postValue(GenericResponse.Error("Data list is empty"))
                }
    }
    //endregion
}