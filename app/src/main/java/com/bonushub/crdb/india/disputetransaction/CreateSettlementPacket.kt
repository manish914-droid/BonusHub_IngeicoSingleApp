package com.bonushub.crdb.india.disputetransaction

import com.bonushub.crdb.india.HDFCApplication
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.db.AppDao
import com.bonushub.crdb.india.model.local.AppPreference
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getTptData
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getTptDataByTid
import com.bonushub.crdb.india.utils.Mti
import com.bonushub.crdb.india.utils.Nii
import com.bonushub.crdb.india.utils.ProcessingCode
import com.bonushub.crdb.india.utils.Utility
import com.bonushub.crdb.india.vxutils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import javax.inject.Inject


class CreateSettlementPacket @Inject constructor(private var appDao: AppDao) : ISettlementPacketExchange {
    val tempbatchListData = runBlocking(Dispatchers.IO) { appDao?.getAllTempBatchFileDataTableDataForSettlement() }
    val batchListData = tempbatchListData
    //val batchListData = runBlocking(Dispatchers.IO) { appDao?.getAllBatchData() } // old
    val baseTid = runBlocking(Dispatchers.IO) { getBaseTID(appDao) }
    val reversaldata = runBlocking(Dispatchers.IO) { appDao.getAllBatchReversalData() }
    override fun createSettlementISOPacket(): IWriter = IsoDataWriter().apply {
        val tpt = runBlocking(Dispatchers.IO) { getTptData() }
        val tptbatchnumber = runBlocking(Dispatchers.IO) { getTptDataByTid(baseTid) }
        var batchNumber:String?=null

        if (tpt != null) {
            mti = Mti.SETTLEMENT_MTI.mti

            //Processing Code:-
            val processingCode: String? = if (AppPreference.getBoolean(PrefConstant.BLOCK_MENU_OPTIONS.keyName.toString())) {
                ProcessingCode.FORCE_SETTLEMENT.code
            } else {
                ProcessingCode.SETTLEMENT.code
            }
            addField(3, processingCode ?: ProcessingCode.SETTLEMENT.code)

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
                    TransactionType.SALE.type -> {
                        batchNumber = batchListData[i]?.receiptData?.batchNumber
                    }

                }
            }*/

            // batch no. also go in zero settlement
            batchNumber = tptbatchnumber?.batchNumber
            //Batch Number
            batchNumber?.let { addPad(it, "0", 6, true) }?.let { addFieldByHex(60, it) }

            //adding field 61
            addFieldByHex(61, addPad(DeviceHelper.getDeviceSerialNo() ?: "", " ", 15, false) + AppPreference.getBankCode())

            val version = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)
            val pcNumber = addPad(AppPreference.getString(PreferenceKeyConstant.PC_NUMBER_ONE.keyName), "0", 9)
            //adding field 62
            addFieldByHex(62, getConnectionType() +
                    addPad(deviceModel(), " ", 6, false)
                    + addPad(HDFCApplication.appContext.getString(R.string.app_name), " ", 10, false)
                    + version + pcNumber + addPad("0", "0", 9)
            )
            //adding field 63
            //SEQUENCE-------> sale, emi sale ,sale with cash, cash only,auth comp,and tip transaction type will be included.
            //Manipulating Data based on condition for Field 63:-
            /*if (batchListData.size > 0) {


                val map = mutableMapOf<String, MutableMap<Int, SummeryModel>>()

                for (it in batchListData) {

                    val transAmt = try {
                        it.receiptData?.txnAmount?.toLong()
                    } catch (ex: Exception) {
                        0L
                    }
                    if (map.containsKey(it.receiptData?.tid!!)) {
                        val ma = map[it.receiptData?.tid!!] as MutableMap<Int, SummeryModel>
                        if (ma.containsKey(it.transactionType)) {
                            val m = ma[it.transactionType] as SummeryModel
                            m.count += 1
                            m.total += transAmt!!
                            m.ingenicobatchNumber  = it.receiptData?.batchNumber!!
                            m.bonushubbatchNumber  = it.bonushubbatchnumber
                        } else {
                            val sm = SummeryModel(transactionType2Name(it.transactionType), 1, transAmt!!,it.receiptData?.tid ?: "",it.receiptData?.batchNumber!!,it.bonushubbatchnumber)
                            ma[it.transactionType] = sm
                        }
                    } else {
                        val hm = HashMap<Int, SummeryModel>().apply {
                            this[it.transactionType] = SummeryModel(transactionType2Name(it.transactionType), 1, transAmt!!,it.receiptData?.tid ?: "",it.receiptData?.batchNumber!!,it.bonushubbatchnumber)
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
                var ingenicosalebatchNumber: String? = null
                var bonushubsalebatchNumber: String? = null

                var field63: String? = null

                for ((key, _map) in map) {
                    println("Key value in map "+key)
                    for ((k, m) in _map) {
                        println("key value in _map "+ k)
                        println("Value value in _map "+ m)
                        when (k) {
                            TransactionType.SALE.type -> {
                                saletid = key
                                ingenicosalebatchNumber = m.ingenicobatchNumber
                                bonushubsalebatchNumber = m.bonushubbatchNumber
                                saleCount += m.count
                                saleAmount += m.total
                            }

                            TransactionType.EMI_SALE.type -> {

                                saletid = key
                                ingenicosalebatchNumber = m.ingenicobatchNumber
                                bonushubsalebatchNumber = m.bonushubbatchNumber
                                saleCount += m.count
                                saleAmount += m.total

                            }

                            TransactionType.BRAND_EMI.type -> {
                                saletid = key
                                ingenicosalebatchNumber = m.ingenicobatchNumber
                                bonushubsalebatchNumber = m.bonushubbatchNumber
                                saleCount += m.count
                                saleAmount += m.total
                            }
                            TransactionType.BRAND_EMI_BY_ACCESS_CODE.type -> {
                                saletid = key
                                ingenicosalebatchNumber = m.ingenicobatchNumber
                                bonushubsalebatchNumber = m.bonushubbatchNumber
                                saleCount += m.count
                                saleAmount += m.total
                            }

                            TransactionType.SALE_WITH_CASH.type -> {
                                saletid = key
                                ingenicosalebatchNumber = m.ingenicobatchNumber
                                bonushubsalebatchNumber = m.bonushubbatchNumber
                                saleCount += m.count
                                saleAmount += m.total
                            }

                            TransactionType.CASH_AT_POS.type -> {
                                saletid = key
                                ingenicosalebatchNumber = m.ingenicobatchNumber
                                bonushubsalebatchNumber = m.bonushubbatchNumber
                                saleCount += m.count
                                saleAmount += m.total
                            }

                            TransactionType.PRE_AUTH_COMPLETE.type -> {
                                saletid = key
                                ingenicosalebatchNumber = m.ingenicobatchNumber
                                bonushubsalebatchNumber = m.bonushubbatchNumber
                                saleCount += m.count
                                saleAmount += m.total
                            }
                            TransactionType.TIP_SALE.type -> {
                                saletid = key
                                ingenicosalebatchNumber = m.ingenicobatchNumber
                                bonushubsalebatchNumber = m.bonushubbatchNumber
                                saleCount += m.count
                                saleAmount += m.total
                            }

                            TransactionType.REFUND.type -> {
                                refundCount += m.count
                                refundAmount += m.total
                            }
                        }

                 *//*       val sCount = addPad(saleCount, "0", 3, true)
                        val sAmount = addPad(saleAmount.toString(), "0", 12, true)
                        val rCount = addPad(refundCount, "0", 3, true)
                        val rAmount = addPad(refundAmount.toString(), "0", 12, true)*//*
                        //   sale,sale with cash, cash only,auth comp,and tip transaction




                    }
                    val sCount = addPad(saleCount, "0", 3, true)
                    val sAmount = addPad(saleAmount.toString(), "0", 12, true)
                    val rCount = addPad(refundCount, "0", 3, true)
                    val rAmount = addPad(refundAmount.toString(), "0", 12, true)

                    if(null !=field63) {
                        field63 += addPad(saletid ?: "", "0", 8, true) +
                                addPad(bonushubsalebatchNumber ?: "", "0", 6, true) +
                                addPad(ingenicosalebatchNumber ?: "", "0", 6, true) +
                                addPad(
                                    sCount + sAmount + rCount + rAmount, "0", 30,
                                    toLeft = false
                                )
                    }
                    else{
                        field63 = addPad(saletid ?: "", "0", 8, true) +
                                addPad(bonushubsalebatchNumber ?: "", "0", 6, true) +
                                addPad(ingenicosalebatchNumber ?: "", "0", 6, true) +
                                addPad(
                                    sCount + sAmount + rCount + rAmount, "0", 30,
                                    toLeft = false
                                )
                    }

                    saleCount = 0
                    saleAmount = 0
                    refundCount = 0
                    refundAmount = 0

                }

                if(reversaldata.size > 0){
                    reversaldata.forEach {
                      saletid =  it.receiptData?.tid
                       bonushubsalebatchNumber = it.bonushubBatchnumber
                        ingenicosalebatchNumber  = it.receiptData?.batchNumber
                    }

                    field63 += addPad(saletid ?: "", "0", 8, true) +
                            addPad(ingenicosalebatchNumber ?: "", "0", 6, true) +
                            addPad("0" + "0" + "0" + "0", "0", 30,
                                toLeft = false
                            )
                }



                addFieldByHex(63,field63!!)


            }
*/

            //adding field 63
            var saleCount = 0
            var saleAmount = 0L

            var refundCount = 0
            var refundAmount = "0"

            if(batchListData.size > 0){

                for (i in 0 until batchListData.size) {
                    when (batchListData[i]?.transactionType) {
                        BhTransactionType.SALE.type -> {
                            if(batchListData[i]?.tenure=="1"){
                                saleCount = saleCount.plus(1)
                                saleAmount = saleAmount.plus(batchListData[i]?.emiTransactionAmount?.toLong()?:0L)
                            }else{
                                saleCount = saleCount.plus(1)
                                saleAmount = saleAmount.plus(batchListData[i]?.transactionalAmmount?.toLong()?:0L)
                            }
                        }
                        BhTransactionType.EMI_SALE.type -> {
                            saleCount = saleCount.plus(1)
                            saleAmount = saleAmount.plus(batchListData[i]?.emiTransactionAmount?.toLong()?:0L)
                        }
                        BhTransactionType.BRAND_EMI.type -> {
                            saleCount = saleCount.plus(1)
                            saleAmount = saleAmount.plus(batchListData[i]?.emiTransactionAmount?.toLong()?:0L)
                        }
                        BhTransactionType.BRAND_EMI_BY_ACCESS_CODE.type -> {
                            saleCount = saleCount.plus(1)
                            saleAmount = saleAmount.plus(batchListData[i]?.transactionalAmmount?.toLong()?:0L)
                        }
                        BhTransactionType.SALE_WITH_CASH.type -> {
                            saleCount = saleCount.plus(1)
                            saleAmount = saleAmount.plus(batchListData[i]?.transactionalAmmount?.toLong()?:0L)
                        }
                        BhTransactionType.CASH_AT_POS.type -> {
                            saleCount = saleCount.plus(1)
                            saleAmount = saleAmount.plus(batchListData[i]?.transactionalAmmount?.toLong()?:0L)
                        }
                        BhTransactionType.PRE_AUTH_COMPLETE.type -> {
                            saleCount = saleCount.plus(1)
                            saleAmount = saleAmount.plus(batchListData[i]?.transactionalAmmount?.toLong()?:0L)
                        }
                        BhTransactionType.TIP_SALE.type -> {
                            saleCount = saleCount.plus(1)
                            saleAmount = saleAmount.plus(batchListData[i]?.totalAmmount?.toLong()?:0L)
                        }
                        BhTransactionType.TEST_EMI.type -> {
                            saleCount = saleCount.plus(1)
                            saleAmount = saleAmount.plus(100.toLong())
                        }
                        BhTransactionType.REFUND.type -> {
                            refundCount = refundCount.plus(1)
                            refundAmount =
                                refundAmount.plus(batchListData[i]?.transactionalAmmount?.toLong()?:0L)
                        }
                    }
                }

                val sCount = addPad(saleCount, "0", 3, true)
                val sAmount = addPad(saleAmount.toString(), "0", 12, true)

                val rCount = addPad(refundCount, "0", 3, true)
                val rAmount = addPad(refundAmount, "0", 12, true)

                //   sale, emi sale ,sale with cash, cash only,auth comp,and tip transaction


                addFieldByHex(
                    63,
                    addPad(
                        sCount + sAmount + rCount + rAmount,
                        "0",
                        90,
                        toLeft = false
                    )
                )

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
        var ingenicobatchNumber: String,
        var bonushubbatchNumber: String
    )
}