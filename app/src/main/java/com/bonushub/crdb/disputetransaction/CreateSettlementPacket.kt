package com.bonushub.crdb.disputetransaction

import com.bonushub.crdb.HDFCApplication
import com.bonushub.crdb.R
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.utils.*
import com.bonushub.crdb.utils.Field48ResponseTimestamp.getTptData
import com.bonushub.crdb.utils.Field48ResponseTimestamp.transactionType2Name
import com.bonushub.pax.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.HashMap
import javax.inject.Inject


class CreateSettlementPacket @Inject constructor(private var appDao: AppDao) : ISettlementPacketExchange {
    val batchListData = runBlocking(Dispatchers.IO) { appDao?.getAllBatchData() }
    val baseTid = runBlocking(Dispatchers.IO) { getBaseTID(appDao) }
    val reversaldata = runBlocking(Dispatchers.IO) { appDao.getAllBatchReversalData() }
    override fun createSettlementISOPacket(): IWriter = IsoDataWriter().apply {
        val tpt = runBlocking(Dispatchers.IO) { getTptData() }
        var batchNumber:String?=null

        if (tpt != null) {
            mti = Mti.SETTLEMENT_MTI.mti

            //Processing Code:-
            addField(3, ProcessingCode.SETTLEMENT.code)

            //ROC will not go in case of AMEX on all PORT but for HDFC it was mandatory:-
            // Sending ROC in case of HDFC ........
            addField(11, Utility().getROC().toString())

            //adding nii
            addField(24, Nii.DEFAULT.nii)


            //adding tid
            addFieldByHex(41, baseTid)

            //adding mid

            addFieldByHex(42, tpt.merchantId)

            //adding field 48
            addFieldByHex(48, Field48ResponseTimestamp.getF48Data())
            /*for (i in 0 until batchListData.size) {
                when (batchListData[i]?.transactionType) {
                    BhTransactionType.SALE.type -> {
                        batchNumber = batchListData[i]?.receiptData?.batchNumber
                    }

                }
            }*/

            if(batchListData.size > 0){
                batchNumber = batchListData[0].receiptData?.batchNumber
            }
            //Batch Number
            batchNumber?.let { addPad(it, "0", 6, true) }?.let { addFieldByHex(60, it) }

            //adding field 61
            addFieldByHex(61, addPad(DeviceHelper.getDeviceSerialNo() ?: "", " ", 15, false) + AppPreference.getBankCode())

            val version = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)
            val pcNumber = addPad(AppPreference.getString(PreferenceKeyConstant.PC_NUMBER_ONE.keyName), "0", 9)
            //adding field 62
            addFieldByHex(62, ConnectionType.GPRS.code +
                    addPad(deviceModel(), " ", 6, false)
                    + addPad(HDFCApplication.appContext.getString(R.string.app_name), " ", 10, false)
                    + version + pcNumber + addPad("0", "0", 9)
            )
            //adding field 63


            //SEQUENCE-------> sale, emi sale ,sale with cash, cash only,auth comp,and tip transaction type will be included.
            //Manipulating Data based on condition for Field 63:-
            if (batchListData?.size > 0) {


                val map = mutableMapOf<String, MutableMap<Int, SummeryModel>>()

                for (it in batchListData) {

                    val transAmt = try {
                        it.receiptData?.txnAmount?.toLong()
                    } catch (ex: Exception) {
                        0L
                    }
                    if (map.containsKey(it.receiptData?.tid!!)) {
                        val ma = map[it.receiptData?.tid!!] as MutableMap<Int, SummeryModel>
                        if (ma!!.containsKey(it.transactionType)) {
                            val m = ma[it.transactionType] as SummeryModel
                            m.count += 1
                            m.total += transAmt!!
                            m.batchNumber  = it.receiptData?.batchNumber!!
                        } else {
                            val sm = SummeryModel(transactionType2Name(it.transactionType), 1, transAmt!!,it.receiptData?.tid ?: "",it.receiptData?.batchNumber!!)
                            ma[it.transactionType] = sm
                        }
                    } else {
                        val hm = HashMap<Int, SummeryModel>().apply {
                            this[it.transactionType] = SummeryModel(transactionType2Name(it.transactionType), 1, transAmt!!,it.receiptData?.tid ?: "",it.receiptData?.batchNumber!!)
                        }
                        map[it.receiptData?.tid!!] = hm
                    }
                }

                //adding field 63
                var saleCount = 0
                var saleAmount = 0L

                var refundCount = 0
                var refundAmount = 0L

                var saletid: String? = null
                var salebatchNumber: String? = null

                var field63: String? = null

                for ((key, _map) in map) {

                    println("Key value in map "+key)

                    for ((k, m) in _map) {
                        println("key value in _map "+ k)
                        println("Value value in _map "+ m)
                        when (k) {
                            BhTransactionType.SALE.type -> {
                                saletid = key
                                salebatchNumber = m.batchNumber
                                saleCount += m.count
                                saleAmount += m.total
                            }

                            BhTransactionType.EMI_SALE.type -> {

                                saletid = key
                                salebatchNumber = m.batchNumber
                                saleCount += m.count
                                saleAmount += m.total

                            }

                            BhTransactionType.BRAND_EMI.type -> {
                                saletid = key
                                salebatchNumber = m.batchNumber
                                saleCount += m.count
                                saleAmount += m.total
                            }
                            BhTransactionType.BRAND_EMI_BY_ACCESS_CODE.type -> {
                                saletid = key
                                salebatchNumber = m.batchNumber
                                saleCount += m.count
                                saleAmount += m.total
                            }

                            BhTransactionType.SALE_WITH_CASH.type -> {

                                saletid = key
                                salebatchNumber = m.batchNumber
                                saleCount += m.count
                                saleAmount += m.total
                            }

                            BhTransactionType.CASH_AT_POS.type -> {
                                saletid = key
                                salebatchNumber = m.batchNumber
                                saleCount += m.count
                                saleAmount += m.total
                            }

                            BhTransactionType.PRE_AUTH_COMPLETE.type -> {
                                saletid = key
                                salebatchNumber = m.batchNumber
                                saleCount += m.count
                                saleAmount += m.total
                            }
                            BhTransactionType.TIP_SALE.type -> {
                                saletid = key
                                salebatchNumber = m.batchNumber
                                saleCount += m.count
                                saleAmount += m.total
                            }

                            BhTransactionType.REFUND.type -> {
                                refundCount += m.count
                                refundAmount += m.total
                            }
                        }



                        val sCount = addPad(saleCount, "0", 3, true)
                        val sAmount = addPad(saleAmount.toString(), "0", 12, true)

                        val rCount = addPad(refundCount, "0", 3, true)
                        val rAmount = addPad(refundAmount.toString(), "0", 12, true)

                        //   sale,sale with cash, cash only,auth comp,and tip transaction

                        if(null !=field63) {

                            field63 += addPad(saletid ?: "", "0", 8, true) +
                                    addPad(salebatchNumber ?: "", "0", 6, true) +
                                    addPad(
                                        sCount + sAmount + rCount + rAmount, "0", 30,
                                        toLeft = false
                                    )
                        }
                        else{
                            field63 = addPad(saletid ?: "", "0", 8, true) +
                                    addPad(salebatchNumber ?: "", "0", 6, true) +
                                    addPad(
                                        sCount + sAmount + rCount + rAmount, "0", 30,
                                        toLeft = false
                                    )
                        }



                        saleCount = 0;
                        saleAmount = 0;
                        refundCount = 0;
                        refundAmount = 0;

                    }
                }

                if(reversaldata.size > 0){
                    reversaldata.forEach {
                      saletid =  it.receiptData?.tid
                        salebatchNumber  = it.receiptData?.batchNumber
                    }

                    field63 += addPad(saletid ?: "", "0", 8, true) +
                            addPad(salebatchNumber ?: "", "0", 6, true) +
                            addPad("0" + "0" + "0" + "0", "0", 30,
                                toLeft = false
                            )
                }



                addFieldByHex(63,field63!!)


            }
            else if(reversaldata.size >0 ){

            }
            else {
                addFieldByHex(63, addPad(0, "0", 90, toLeft = false))
            }
        }
        logger("SETTLEMENT REQ PACKET -->", this.isoMap, "e")

    }

    internal data class SummeryModel(
        val type: String,
        var count: Int = 0,
        var total: Long = 0,
        var hostTid: String,
        var batchNumber: String
    )
}