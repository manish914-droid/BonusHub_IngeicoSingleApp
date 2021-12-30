package com.bonushub.crdb.model.local

import androidx.room.TypeConverter
import com.bonushub.crdb.model.CardProcessedDataModal
import com.bonushub.crdb.model.remote.BankEMIIssuerTAndCDataModal
import com.bonushub.crdb.model.remote.BankEMITenureDataModal
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.model.remote.BrandEMIProductDataModal
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ingenico.hdfcpayment.model.ReceiptDetail
import com.ingenico.hdfcpayment.type.CvmAction

class Converters {

    @TypeConverter
    fun fromTIDSList(tidList: List<String?>?): String? {
        val type = object : TypeToken<List<String?>?>() {}.type
        return Gson().toJson(tidList, type)
    }

    @TypeConverter
    fun toTIDSList(tidString: String?): List<String>? {
        val type = object : TypeToken<List<String?>?>() {}.type
        return Gson().fromJson<List<String>>(tidString, type)
    }

    @TypeConverter
    fun fromInitDataList(initDataListList: List<InitDataListList?>?): String? {
        val type = object : TypeToken<List<InitDataListList?>?>() {}.type
        return Gson().toJson(initDataListList, type)
    }

    @TypeConverter
    fun toInitDataSList(initDataListList: String?): List<InitDataListList>? {
        val type = object : TypeToken<List<InitDataListList?>?>() {}.type
        return Gson().fromJson<List<InitDataListList>>(initDataListList, type)
    }

    @TypeConverter
    fun fromReceiptDetail(receiptDetail: ReceiptDetail?): String? {
        val type = object : TypeToken<ReceiptDetail?>() {}.type
        return Gson().toJson(receiptDetail, type)
    }

    @TypeConverter
    fun toReceiptDetail(receiptDetail: String?): ReceiptDetail? {
        val type = object : TypeToken<ReceiptDetail>() {}.type
        return Gson().fromJson<ReceiptDetail>(receiptDetail, type)
    }
    @TypeConverter
    fun fromEmiBrandData(emiBrandData: BrandEMIMasterDataModal?): String? {
        val type = object : TypeToken<BrandEMIMasterDataModal?>() {}.type
        return Gson().toJson(emiBrandData, type)
    }
    @TypeConverter
    fun toEmiBrandData(emiBrandData: String?): BrandEMIMasterDataModal? {
        val type = object : TypeToken<BrandEMIMasterDataModal>() {}.type
        return Gson().fromJson<BrandEMIMasterDataModal>(emiBrandData, type)
    }

    @TypeConverter
    fun fromEmiCategoryData(emiCategoryData: BrandEMISubCategoryTable?): String? {
        val type = object : TypeToken<BrandEMISubCategoryTable?>() {}.type
        return Gson().toJson(emiCategoryData, type)
    }
    @TypeConverter
    fun toEmiCategoryData(emiCategoryData: String?): BrandEMISubCategoryTable? {
        val type = object : TypeToken<BrandEMISubCategoryTable>() {}.type
        return Gson().fromJson<BrandEMISubCategoryTable>(emiCategoryData, type)
    }

 @TypeConverter
    fun fromEmiProductData(emiProductData: BrandEMIProductDataModal?): String? {
        val type = object : TypeToken<BrandEMIProductDataModal?>() {}.type
        return Gson().toJson(emiProductData, type)
    }

    @TypeConverter
    fun toEmiProductData(emiProductData: String?): BrandEMIProductDataModal? {
        val type = object : TypeToken<BrandEMIProductDataModal>() {}.type
        return Gson().fromJson<BrandEMIProductDataModal>(emiProductData, type)
    }

    @TypeConverter
    fun fromEmiTenureDataModal(emiTenureDataModal: BankEMITenureDataModal?): String? {
      //  val type = object : TypeToken<BankEMITenureDataModal?>() {}.type
        return Gson().toJson(emiTenureDataModal, BankEMITenureDataModal::class.java)
    }

    @TypeConverter
    fun toEmiTenureDataModal(emiTenureDataModal: String?): BankEMITenureDataModal? {
      //  val type = object : TypeToken<BankEMITenureDataModal>() {}.type
        return Gson().fromJson(emiTenureDataModal, BankEMITenureDataModal::class.java)
    }

    @TypeConverter
    fun fromEmiIssuerTAndCData(emiIssuerTAndCData: BankEMIIssuerTAndCDataModal?): String? {
      //  val type = object : TypeToken<BankEMITenureDataModal?>() {}.type
        return Gson().toJson(emiIssuerTAndCData, BankEMIIssuerTAndCDataModal::class.java)
    }

    @TypeConverter
    fun toEmiIssuerTAndCData(emiIssuerTAndCData: String?): BankEMIIssuerTAndCDataModal? {
      //  val type = object : TypeToken<BankEMITenureDataModal>() {}.type
        return Gson().fromJson(emiIssuerTAndCData, BankEMIIssuerTAndCDataModal::class.java)
    }

    @TypeConverter
    fun toCvmAction(value: Int) = enumValues<CvmAction>()[value]

    @TypeConverter
    fun fromCvmAction(value: CvmAction) = value.ordinal



    @TypeConverter
    fun fromBatchTableData(batchTable: BatchTable?): String? {
        return Gson().toJson(batchTable, BatchTable::class.java)
    }

    @TypeConverter
    fun toBatchTableData(batchTable: String?): BatchTable? {
        return Gson().fromJson(batchTable, BatchTable::class.java)
    }

    @TypeConverter
    fun fromCardProcessedDataModal(cardProcessedDataModal: CardProcessedDataModal?): String? {
        return Gson().toJson(cardProcessedDataModal, CardProcessedDataModal::class.java)
    }

    @TypeConverter
    fun toCardProcessedDataModal(cardProcessedDataModal: String?): CardProcessedDataModal? {
        return Gson().fromJson(cardProcessedDataModal, CardProcessedDataModal::class.java)
    }

}