package com.bonushub.crdb.india.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.bonushub.crdb.india.model.local.*
import kotlinx.coroutines.runBlocking

@Dao
interface AppDao{

    @Insert(onConflict = OnConflictStrategy.IGNORE)
     suspend fun insertIngenicoSettlement(ingenicoSettlementResponse: IngenicoSettlementResponse)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngenicoIntializationData(initialization: IngenicoInitialization)

    @Query("SELECT * FROM IngenicoInitialization")
     suspend fun getIngenicoInitialization(): MutableList<IngenicoInitialization?>?

    @Query("DELETE From IngenicoInitialization")
    suspend fun deleteIngenicoInitiaization()

    //  @Query("UPDATE TerminalParameterTable SET invoiceNumber = :updateInvoice WHERE tableId = :tableID")

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAll(initialization: IngenicoInitialization)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend  fun insertTerminalCommunicationData(student: TerminalCommunicationTable) : Long

    @Query("select * From TerminalCommunicationTable")
    fun  fetch() : MutableList<TerminalCommunicationTable>

    //region================================Batch Data Table Manipulation:-
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatchDataInTable(batchFileDataTable: BatchFileDataTable): Long?

    @Query("SELECT * FROM BatchFileDataTable")
    fun getAllBatchTableData(): LiveData<MutableList<BatchFileDataTable?>>?

    @Query("SELECT * FROM BatchFileDataTable")
    suspend fun getBatchTableData(): MutableList<BatchFileDataTable?>?

    @Query("DELETE From BatchFileDataTable")
    suspend fun deleteBatchFileTable()

  /*  @Query("SELECT * FROM BatchFileDataTable WHERE invoiceNumber = :invoice AND transactionType != :transType")
    fun getBatchDataFromInvoice(invoice: String?, transType: Int): BatchFileDataTable?
*/
    @Query("SELECT * FROM BatchFileDataTable WHERE invoiceNumber = :invoice AND transactionType = :transType")
    fun getSale2VoidUpdatedData(invoice: String?, transType: Int): BatchFileDataTable?

    @Query("SELECT * FROM BatchFileDataTable WHERE invoiceNumber = :invoice")
    suspend fun getBatchTableDataByInvoice(invoice: String?): MutableList<BatchFileDataTable?>?

    //endregion

    //region================================Terminal Communication Table Manipulation:-
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerminalCommunicationDataInTable(terminalCommunicationTable: TerminalCommunicationTable): Long?

    @Query("SELECT * FROM TerminalCommunicationTable")
    suspend fun getTerminalCommunicationTableData(): MutableList<TerminalCommunicationTable?>?

    @Query("SELECT * FROM TerminalCommunicationTable")
    fun getAllTerminalCommunicationTableLiveData(): LiveData<MutableList<TerminalCommunicationTable?>>

    @Delete
    suspend fun deleteTerminalCommunicationTable(terminalCommunicationTable: TerminalCommunicationTable)
    //endregion

    //region================================Issuer Data Table Manipulation:-
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIssuerDataInTable(issuerParameterTable: IssuerParameterTable): Long?

    @Query("SELECT * FROM IssuerParameterTable")
    suspend fun getAllIssuerTableData(): MutableList<IssuerParameterTable?>?

    @Delete
    suspend fun deleteIssuerParameterTable(issuerParameterTable: IssuerParameterTable)

    @Query("SELECT * FROM IssuerParameterTable WHERE issuerID = :issuerID")
    fun getIssuerTableDataByIssuerID(issuerID: String): IssuerParameterTable?
    //endregion

    //region================================Terminal Parameter Table Manipulation:-
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerminalParameterDataInTable(terminalParameterTable: TerminalParameterTable): Long?

    @Query("SELECT * FROM TerminalParameterTable")
    suspend fun getAllTerminalParameterTableData(): MutableList<TerminalParameterTable?>

    @Query("SELECT * FROM TerminalParameterTable")
    fun getAllTerminalParameterLiveData(): LiveData<MutableList<TerminalParameterTable?>>

    @Query("UPDATE TerminalParameterTable SET invoiceNumber = :updateInvoice WHERE tableId = :tableID")
    fun updateTPTInvoiceNumber(updateInvoice: String, tableID: String)

    @Delete
    suspend fun deleteTerminalParameterTable(terminalParameterTable: TerminalParameterTable)
    //endregion

    //region================================Card Data Table Manipulation:-
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCardDataInTable(cardDataTable: CardDataTable): Long?

    @Query("SELECT * FROM CardDataTable")
    suspend fun getAllCardTableData(): MutableList<CardDataTable>

    fun getCardDataByPanNumber(panNumber: String): CardDataTable? = runBlocking {
        val cdtl = getAllCardTableData()
        var result: CardDataTable? = null
        for (each in cdtl) {
            if (each.panLow.length >= 6 && each.panHi.length >= 6 && panNumber.length >= 6) {
                val panLow = each.panLow.substring(0, 6).toLong()
                val panHi = each.panHi.substring(0, 6).toLong()
                val cuPan = panNumber.substring(0, 6).toLong()
                if (cuPan in panLow..panHi) {
                    result = each
                    //break
                }
            }
        }
        result
    }

    @Delete
    suspend fun deleteCardDataTable(cardDataTable: CardDataTable)
    //endregion

    //region================================HDFC TPT Table Manipulation:-
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHDFCTPTDataInTable(hdfcTpt: HDFCTpt): Long?

    @Query("SELECT * FROM HDFCTpt")
    suspend fun getAllHDFCTPTTableData(): MutableList<HDFCTpt?>?

    @Delete
    suspend fun deleteHDFCTPT(hdfcTpt: HDFCTpt)
    //endregion

    //region================================HDFC CDT Table Manipulation:-
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHDFCCDTInTable(hdfcCdt: HDFCCdt): Long?

    @Query("SELECT * FROM HDFCCdt")
    suspend fun getAllHDFCCDTTableData(): MutableList<HDFCCdt?>?

    //region================================Wifi CDT Table Manipulation:-
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWifiCTTable(wifiCommunicationTable: WifiCommunicationTable): Long?

    @Query("SELECT * FROM WifiCommunicationTable")
    suspend fun getAllWifiCTTableData(): MutableList<WifiCommunicationTable?>?

    fun getCardDataByHDFCCDTPanNumber(panNumber: String): HDFCCdt? = runBlocking {
        val cdtl = getAllHDFCCDTTableData()
        var result: HDFCCdt? = null
        if (cdtl != null) {
            for (each in cdtl) {
                if (panNumber.length >= 6) {
                    val panLow = each?.panRangeLow?.toLong()
                    val panHi = each?.panRangeHigh?.toLong()
                    val cuPan = panNumber.substring(0, each?.panRangeHigh?.length ?: 0).toLong()
                    if (cuPan in panLow!!..panHi!!) {
                        result = each
                    }
                }
            }
        }
        result
    }

    @Delete
    suspend fun deleteHDFCCDT(hdfcCdt: HDFCCdt)
    //endregion

    @Delete
    suspend fun deleteWifiCT(wifiCommunicationTable: WifiCommunicationTable)
    //endregion

    //region=================================BrandEMIMasterCategory Table Manipulation:-
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrandEMIMasterCategoryData(brandEMIMasterCategoryTable: BrandEMIMasterCategoryTable): Long?

    @Query("SELECT * FROM BrandEMIMasterCategoryTable")
    suspend fun getAllBrandEMIMasterCategoryData(): MutableList<BrandEMIMasterCategoryTable?>?

    @Query("UPDATE BrandEMIMasterCategoryTable SET brandCategoryUpdatedTimeStamp = :updatedTimeStamp WHERE brandTimeStamp = :brandTimeStamp")
    fun updateCategoryTimeStamp(updatedTimeStamp: String, brandTimeStamp: String)

    @Query("DELETE FROM BrandEMIMasterCategoryTable")
    suspend fun deleteBrandEMIMasterCategoryData(): Int?
    //endregion

    // region=================================BrandEMIMasterSubCategory Table Manipulation:-
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrandEMIMasterSubCategoryData(brandEMIMasterSubCategoryTable: BrandEMIMasterSubCategoryTable): Long?

    @Query("SELECT * FROM BrandEMIMasterSubCategoryTable")
    suspend fun getAllBrandEMIMasterSubCategoryData(): MutableList<BrandEMIMasterSubCategoryTable?>?

    @Query("DELETE FROM BrandEMIMasterSubCategoryTable")
    suspend fun deleteBrandEMIMasterSubCategoryData()
    //endregion

    // region=================================IssuerTAndC Table Manipulation:-
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIssuerTAndCData(issuerTAndCTable: IssuerTAndCTable): Long?

    @Query("SELECT * FROM IssuerTAndCTable")
    suspend fun getAllIssuerTAndCData(): MutableList<IssuerTAndCTable?>?

    @Query("SELECT * FROM IssuerTAndCTable WHERE issuerId =:issuerId ")
    suspend fun getAllIssuerTAndCDataById(issuerId:String): IssuerTAndCTable?

    @Query("DELETE FROM IssuerTAndCTable")
    suspend fun deleteIssuerTAndCData(): Int?
    //endregion

    // region=================================BrandTAndC Table Manipulation:-
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrandTAndCData(brandTAndC: BrandTAndCTable): Long?

    @Query("SELECT * FROM BrandTAndCTable")
    suspend fun getAllBrandTAndCData(): MutableList<BrandTAndCTable?>?

    @Query("SELECT brandTAndC FROM BrandTAndCTable WHERE brandId =:brandId ")
    suspend fun getBrandTAndCDataById(brandId:String): String?

    @Query("DELETE FROM BrandTAndCTable")
    suspend fun deleteBrandTAndCData()
    //endregion


    //region==========================Read , Update , Reset ROC From Terminal Parameter Table:-
    @Query("SELECT stan FROM TerminalParameterTable")
    fun getRoc(): String?

    //region==========================Read , Update , Reset ROC From Terminal Parameter Table:-
    @Query("SELECT stan FROM TerminalParameterTable WHERE tidType = :tidType")
    fun getUpdateRoc(tidType: String): String?

    @Query("SELECT * FROM TerminalParameterTable LIMIT :limit OFFSET :offset")
    fun selectFromSchemeTable(limit: Int, offset: Int): TerminalParameterTable?

    @Query("SELECT * FROM TerminalParameterTable")
    fun selectAll(): List<TerminalParameterTable>?

    @Query("SELECT * FROM TerminalParameterTable")
    fun getTerminalDataByBankCode(bankCode: String): TerminalParameterTable? {
        var tpt: TerminalParameterTable? = null
        var ltpt: List<TerminalParameterTable> = listOf()
        val tp = selectAll()
        if (tp != null) ltpt = tp
        for (e in ltpt) {
            if (e.tidBankCode.toInt() == bankCode.toInt()) {
                tpt = e
                break
            }
        }
        return  tpt
    }


    @Query("UPDATE TerminalParameterTable SET stan = :roc WHERE tableId = :tableID AND tidType = :tidType")
    fun updateStan(roc: String, tableID: String, tidType: String)

    @Query("UPDATE TerminalParameterTable SET stan = :roc WHERE tableId = :tableID")
    fun updateRoc(roc: String, tableID: String)

    @Query("UPDATE TerminalParameterTable SET stan = :roc WHERE tableId = :tableID")
    fun clearRoc(roc: String, tableID: String)

    //endregion

    //region==========================Read , Update , Reset INVOICE From Terminal Parameter Table:-
    @Query("SELECT invoiceNumber FROM TerminalParameterTable")
    fun getInvoice(): String?

    @Query("SELECT invoiceNumber FROM TerminalParameterTable WHERE tidType = :tidType")
    fun getUpdatedInvoice(tidType: String): String?

    @Query("UPDATE TerminalParameterTable SET invoiceNumber = :invoice WHERE tableId = :tableID AND tidType = :tidType")
    fun updatedInvoice(invoice: String, tableID: String,tidType: String)

    @Query("UPDATE TerminalParameterTable SET invoiceNumber = :invoice WHERE tableId = :tableID")
    fun updateInvoice(invoice: String, tableID: String)

    @Query("UPDATE TerminalParameterTable SET invoiceNumber = :invoice WHERE tableId = :tableID")
    fun clearInvoice(invoice: String, tableID: String)
    //endregion

    //region==========================Read , Update , Reset Batch Number From Terminal Parameter Table:-
    @Query("SELECT batchNumber FROM TerminalParameterTable")
    fun getBatchNumber(): String?

    @Query("SELECT batchNumber FROM TerminalParameterTable WHERE tidType = :tidType")
    fun getUpdatedBatchNumber(tidType: String): String?

    @Query("UPDATE TerminalParameterTable SET batchNumber = :batch WHERE tableId = :tableID")
    fun updateBatchNumber(batch: String, tableID: String)

    @Query("UPDATE TerminalParameterTable SET batchNumber = :batch WHERE tableId = :tableID AND tidType = :tidType")
    fun updatedBatchNumber(batch: String, tableID: String,tidType: String)

    @Query("UPDATE TerminalParameterTable SET batchNumber = :batch WHERE tableId = :tableID")
    fun clearBatchNumber(batch: String, tableID: String)
    //endregion

    //region=============================Update Transaction Type after Void:-
    @Query("UPDATE BatchFileDataTable SET transactionType = :transType WHERE invoiceNumber = :invoice")
    fun updateTransactionType(invoice: String, transType: Int)
    //endregion

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateBatchDataTableRecord(batch: BatchFileDataTable): Int

    // region =========== Saving Brand TimeStamps method========
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrandEMIMasterTimeStamps(timestamps: BrandEMIMasterTimeStamps): Long?
    // endregion

    @Query("DELETE FROM BrandEMIMasterTimeStamps")
    suspend fun deleteBrandEMIMasterTimeStamps(): Int?

    // region =========== Saving Brand TimeStamps method========
    @Query("SELECT * FROM BrandEMIMasterTimeStamps")
    suspend fun getBrandEMIDateTimeStamps():List<BrandEMIMasterTimeStamps>?
    // endregion
    // region =========== Saving Brand Subcat data method========
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrandEMISubCategoryData(brandSubCat: BrandEMISubCategoryTable): Long?
    // endregion
    // region =========== Saving Brand TimeStamps method========
    @Query("SELECT * FROM BrandEMISubCategoryTable")
    suspend fun getBrandEMISubCategoryData():List<BrandEMISubCategoryTable>?
    // endregion
    suspend fun getBrandTimeStampFromDB(): BrandEMIMasterTimeStamps?{
   val list= getBrandEMIDateTimeStamps()
    return if(list.isNullOrEmpty()){
        null
    }else{
        list[0]
    }




}

    @Query("SELECT * FROM IssuerParameterTable WHERE issuerId = :issuerId ")
    fun selectFromIssuerParameterTable(issuerId: String): IssuerParameterTable?



    //region==============================BrandEMI Helper Methods:-
    /*fun insertBrandEMIMasterCategoryDataInDB(model: BrandEMIMasterCategoryTable): Long? {
        return runBlocking(Dispatchers.IO) {
            appDatabase?.dao()?.insertBrandEMIMasterCategoryData(model)
        }
    }*/

    /* fun insertBrandEMIMasterSubCategoryDataInDB(model: BrandEMIMasterSubCategoryTable): Long? {
         return runBlocking(Dispatchers.IO) {
             appDatabase?.dao()?.insertBrandEMIMasterSubCategoryData(model)
         }
     }
 */
    /* fun updateCategoryTimeStampInBrandEMICategoryTable(updateTimeStamp: String, brandTimeStamp: String) {
         return runBlocking(Dispatchers.IO) {
             appDatabase?.dao()?.updateCategoryTimeStamp(updateTimeStamp, brandTimeStamp)
         }
     }*/
    //endregion

    // region bank functions
    @Query("SELECT * FROM TerminalParameterTable WHERE tidType = :tidType")
    suspend fun getTerminalParameterTableDataByTidType(tidType: String): TerminalParameterTable?

    @Query("SELECT * FROM TerminalParameterTable WHERE LinkTidType = :LinkTidType")
    suspend fun getTerminalParameterTableDataByLinkTidType(LinkTidType: String): TerminalParameterTable?


    // region bank functions
    @Query("SELECT * FROM TerminalParameterTable WHERE terminalId = :tid")
    suspend fun getTerminalParameterTableDataByTid(tid: String): TerminalParameterTable?

    @Query("SELECT * FROM TerminalParameterTable LIMIT 1")
    suspend fun getSingleRowTerminalParameterTableData(): TerminalParameterTable?

    @Query("SELECT * FROM TerminalParameterTable")
    suspend fun getTerminalParameterTableData(): MutableList<TerminalParameterTable?>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTerminalParameterTable(terminalParameterTable: TerminalParameterTable)


    @Query("SELECT * FROM TerminalCommunicationTable WHERE recordType = :redordType")
    suspend fun getTerminalCommunicationTableByRecordType(redordType:String): TerminalCommunicationTable

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTerminalCommunicationTable(terminalCommunicationTable: TerminalCommunicationTable): Int
    // end region

    // brand emi region

    /*@Query("SELECT * FROM BrandEMIDataTable WHERE hostInvoice = :hostInvoice AND hostTid = :hostTid")
    fun getBrandEMIDataTable(hostInvoice: String?, hostTid: String): BrandEMIDataTable?*/
    // end region

    // region ========== BatchTable dao ======
    @Query("SELECT * FROM BatchTable")
    fun getBatchData(): LiveData<MutableList<BatchTable?>>?

    @Query("SELECT * FROM BatchTable")
    suspend fun getAllBatchData(): MutableList<BatchTable>

    @Query("SELECT * FROM BatchTable")
    suspend fun getSinleBatchData(): BatchTable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatchData(brandSubCat: BatchTable): Long?

    @Query("DELETE From BatchTable")
    suspend fun deleteBatchTable()

    @Query("SELECT * FROM BatchTable WHERE invoice = :invoice")// AND transactionType != :transType")
    fun getBatchDataFromInvoice(invoice: String?): BatchTable?

    @Query("SELECT * FROM BatchTable WHERE invoice = :invoice")
    suspend fun getBatchTableDataListByInvoice(invoice: String?): MutableList<BatchTable?>?

    // endregion ================


    // region ================ DigiPosDataTable dao ======== kushal
   /* @Query("SELECT * FROM DigiPosDataTable")
    fun getDigiPosDataTable(): LiveData<MutableList<DigiPosDataTable>>?

    @Query("SELECT * FROM DigiPosDataTable WHERE txnStatus = :txnStatus")
    fun getDigiPosDataTableByTxnStatus(txnStatus: String): MutableList<DigiPosDataTable>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDigiPosData(digiPosDataTable: DigiPosDataTable): Long?

    @Query("DELETE From DigiPosDataTable WHERE partnerTxnId = :partnerTxnId")
    suspend fun deleteDigiPosData(partnerTxnId:String)*/
    // endregion ====================

    // region ========== Batch Reversal Table dao ======
    @Query("SELECT * FROM BatchTableReversal")
    suspend fun getAllBatchReversalData(): MutableList<BatchTableReversal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatchReversalData(batchTableReversal: BatchTableReversal): Long?

    @Query("DELETE From BatchTableReversal")
    suspend fun deleteBatchReversalTable()

    // endregion ================

    // region =========== PendingSyncTransaction Table dao ==========
    @Query("SELECT * FROM PendingSyncTransactionTable")
    suspend fun getAllPendingSyncTransactionData(): MutableList<PendingSyncTransactionTable>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingSyncTransactionData(pendingSyncTransactionTable: PendingSyncTransactionTable): Long?

    @Query("DELETE From PendingSyncTransactionTable")
    suspend fun deletePendingSyncTransactionTable()

    @Delete
    suspend fun deletePendingSyncTransactionData(pendingSyncTransactionTable: PendingSyncTransactionTable)
    // end region


    // region ========== DigiPos Table dao ======
    @Query("SELECT * FROM DigiPosDataTable")
    suspend fun getAllDigiposData(): MutableList<DigiPosDataTable>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDigiposData(digiPosData:DigiPosDataTable): Long?

    @Delete
    suspend fun deleteDigiposData(digiPosData: DigiPosDataTable)

    @Query("SELECT * FROM DigiPosDataTable WHERE txnStatus = :txnStatus")
    fun getDigiPosDataTableByTxnStatus(txnStatus: String): MutableList<DigiPosDataTable>

    @Query("DELETE From DigiPosDataTable")
    suspend fun deleteDigiPosDataTable()


    // region ========== PreAuthTransactionTable Table dao ======
    @Query("SELECT * FROM PreAuthTransactionTable")
    suspend fun getAllPreAuthTransactionTableData(): MutableList<PreAuthTransactionTable>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePreAuthTransactionTableData(preAuthTransactionTable: PreAuthTransactionTable): Long?

    @Delete
    suspend fun deletePreAuthTransactionTableData(preAuthTransactionTable: PreAuthTransactionTable)

    @Query("SELECT * FROM PreAuthTransactionTable WHERE invoice = :invoice")// AND transactionType != :transType")
    fun getPreAuthTransactionTableDataFromInvoice(invoice: String?): PreAuthTransactionTable?

    @Query("DELETE From PreAuthTransactionTable")
    suspend fun deletePreAuthTransactionTableDataTable()

    @Query("DELETE From  PreAuthTransactionTable WHERE invoice = :invoice")
    suspend fun deletePreAuthTransactionTableDataFromInvoice(invoice: String?)
}