package com.bonushub.crdb.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.bonushub.crdb.model.local.*
import kotlinx.coroutines.runBlocking

@Dao
interface AppDao{

    @Insert(onConflict = OnConflictStrategy.IGNORE)
     suspend fun insertIngenicoSettlement(ingenicoSettlementResponse: IngenicoSettlementResponse)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIngenicoIntializationData(initialization: IngenicoInitialization)

    @Query("SELECT * FROM IngenicoInitialization")
     suspend fun getIngenicoInitialization(): MutableList<IngenicoInitialization?>?

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

    @Query("DELETE FROM IssuerTAndCTable")
    suspend fun deleteIssuerTAndCData(): Int?
    //endregion

    // region=================================BrandTAndC Table Manipulation:-
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrandTAndCData(brandTAndC: BrandTAndCTable): Long?

    @Query("SELECT * FROM BrandTAndCTable")
    suspend fun getAllBrandTAndCData(): MutableList<BrandTAndCTable?>?

    @Query("DELETE FROM BrandTAndCTable")
    suspend fun deleteBrandTAndCData()
    //endregion


    //region==========================Read , Update , Reset ROC From Terminal Parameter Table:-
    @Query("SELECT stan FROM TerminalParameterTable")
    fun getRoc(): String?

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




    @Query("UPDATE TerminalParameterTable SET stan = :roc WHERE tableId = :tableID")
    fun updateRoc(roc: String, tableID: String)

    @Query("UPDATE TerminalParameterTable SET stan = :roc WHERE tableId = :tableID")
    fun clearRoc(roc: String, tableID: String)

    //endregion

    //region==========================Read , Update , Reset INVOICE From Terminal Parameter Table:-
    @Query("SELECT invoiceNumber FROM TerminalParameterTable")
    fun getInvoice(): String?

    @Query("UPDATE TerminalParameterTable SET invoiceNumber = :invoice WHERE tableId = :tableID")
    fun updateInvoice(invoice: String, tableID: String)

    @Query("UPDATE TerminalParameterTable SET invoiceNumber = :invoice WHERE tableId = :tableID")
    fun clearInvoice(invoice: String, tableID: String)
    //endregion

    //region==========================Read , Update , Reset Batch Number From Terminal Parameter Table:-
    @Query("SELECT batchNumber FROM TerminalParameterTable")
    fun getBatchNumber(): String?

    @Query("UPDATE TerminalParameterTable SET batchNumber = :batch WHERE tableId = :tableID")
    fun updateBatchNumber(batch: String, tableID: String)

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
    @Query("SELECT * FROM TerminalParameterTable LIMIT 1")
    suspend fun getSingleRowTerminalParameterTableData(): TerminalParameterTable?

    @Query("SELECT * FROM TerminalParameterTable")
    suspend fun getTerminalParameterTableData(): MutableList<TerminalParameterTable?>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTerminalParameterTable(terminalParameterTable: TerminalParameterTable)


    @Query("SELECT * FROM TerminalCommunicationTable WHERE recordType = :redordType")
    suspend fun getTerminalCommunicationTableByRecordType(redordType:String): MutableList<TerminalCommunicationTable?>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTerminalCommunicationTable(terminalCommunicationTable: TerminalCommunicationTable)
    // end region

    // brand emi region

    /*@Query("SELECT * FROM BrandEMIDataTable WHERE hostInvoice = :hostInvoice AND hostTid = :hostTid")
    fun getBrandEMIDataTable(hostInvoice: String?, hostTid: String): BrandEMIDataTable?*/
    // end region

    // region ========== BatchTable dao ======
    @Query("SELECT * FROM BatchTable")
    fun getBatchData(): LiveData<MutableList<BatchTable?>>?

    @Query("SELECT * FROM BatchTable")
    suspend fun getAllBatchData(): MutableList<BatchTable?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatchData(brandSubCat: BatchTable): Long?

    @Query("DELETE From BatchTable")
    suspend fun deleteBatchTable()

    @Query("SELECT * FROM BatchTable WHERE invoice = :invoice")// AND transactionType != :transType")
    fun getBatchDataFromInvoice(invoice: String?): BatchTable?

    // endregion ================

}