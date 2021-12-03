package com.bonushub.crdb.disputetransaction

import com.bonushub.crdb.HDFCApplication
import com.bonushub.crdb.R
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.di.DBModule
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.model.local.BatchFileDataTable
import com.bonushub.crdb.model.local.TerminalParameterTable
import com.bonushub.crdb.utils.*
import com.bonushub.crdb.utils.Field48ResponseTimestamp.getTptData
import com.bonushub.pax.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import javax.inject.Inject


class CreateSettlementPacket @Inject constructor(private var appDao: AppDao) : ISettlementPacketExchange {

    override fun createSettlementISOPacket(): IWriter = IsoDataWriter().apply {
        val batchListData = runBlocking(Dispatchers.IO) { appDao?.getAllBatchData() }
        val tpt = runBlocking(Dispatchers.IO) { getTptData() }
        val tid = runBlocking(Dispatchers.IO) { getBaseTID(appDao) }
        if (batchListData?.isNotEmpty() == true) {
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
                addFieldByHex(41, tid)

                //adding mid
                addFieldByHex(42, tpt.merchantId)

                //adding field 48
                addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

                //Batch Number
                addFieldByHex(60, addPad(tpt.batchNumber, "0", 6, true))

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
                var saleCount = 0
                var saleAmount = 0L

                var refundCount = 0
                var refundAmount = "0"

                var saletid: String? = null
                var salebatchNumber: String? = null

                //SEQUENCE-------> sale, emi sale ,sale with cash, cash only,auth comp,and tip transaction type will be included.
                //Manipulating Data based on condition for Field 63:-
                if (batchListData?.size > 0) {
                    for (i in 0 until batchListData.size) {
                        when (batchListData[i]?.transactionType) {
                            TransactionType.SALE.type -> {
                                saletid = batchListData[i]?.receiptData?.tid
                                salebatchNumber = batchListData[i]?.receiptData?.batchNumber
                                saleCount = saleCount.plus(1)
                                saleAmount = saleAmount.plus(batchListData[i]?.receiptData?.txnAmount?.toLong() ?: 0L)
                            }
                            TransactionType.SALE_WITH_CASH.type -> {
                                saleCount = saleCount.plus(1)
                                saleAmount = saleAmount.plus(batchListData[i]?.receiptData?.txnAmount?.toLong() ?: 0L)
                            }

                          /*  TransactionType.SALE_WITH_CASH.type -> {
                                saleCount = saleCount.plus(1)
                                saleAmount = saleAmount.plus(batchListData[i]?.transactionalAmount?.toLong() ?: 0L)
                            }
                            TransactionType.CASH_AT_POS.type -> {
                                saleCount = saleCount.plus(1)
                                saleAmount = saleAmount.plus(batchListData[i]?.transactionalAmount?.toLong() ?: 0L)
                            }
                            TransactionType.PRE_AUTH_COMPLETE.type -> {
                                saleCount = saleCount.plus(1)
                                saleAmount = saleAmount.plus(batchListData[i]?.transactionalAmount?.toLong() ?: 0L)
                            }
                            TransactionType.TIP_SALE.type -> {
                                saleCount = saleCount.plus(1)
                                saleAmount = saleAmount.plus(batchListData[i]?.totalAmount?.toLong() ?: 0L)
                            }
                            TransactionType.TEST_EMI.type -> {
                                saleCount = saleCount.plus(1)
                                saleAmount = saleAmount.plus(100.toLong())
                            }
                            TransactionType.REFUND.type -> {
                                refundCount = refundCount.plus(1)
                                refundAmount =
                                    refundAmount.plus(batchListData[i]?.transactionalAmount?.toLong() ?: 0L)
                            }*/
                        }
                    }

                    val sCount = addPad(saleCount, "0", 3, true)
                    val sAmount = addPad(saleAmount.toString(), "0", 12, true)

                    val rCount = addPad(refundCount, "0", 3, true)
                    val rAmount = addPad(refundAmount, "0", 12, true)

                    //   sale, emi sale ,sale with cash, cash only,auth comp,and tip transaction


                    addFieldByHex(
                        63,
                        addPad(saletid ?: "", "0", 8, true)+
                        addPad(salebatchNumber ?: "", "0", 6, true)+
                        addPad(sCount + sAmount + rCount + rAmount, "0", 90,
                            toLeft = false
                        )
                    )
                } else {
                    addFieldByHex(63, addPad(0, "0", 90, toLeft = false))
                }
            }
            logger("SETTLEMENT REQ PACKET -->", this.isoMap, "e")
        }


    }
}