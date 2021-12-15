package com.bonushub.crdb.transactionprocess

import android.text.TextUtils
import android.util.Log
import com.bonushub.crdb.HDFCApplication
import com.bonushub.crdb.R
import com.bonushub.crdb.model.CardProcessedDataModal
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.model.local.CardDataTable
import com.bonushub.crdb.model.local.IssuerParameterTable
import com.bonushub.crdb.model.remote.BankEMIIssuerTAndCDataModal
import com.bonushub.crdb.repository.keyexchangeDataSource
import com.bonushub.crdb.utils.*
import com.bonushub.crdb.utils.Field48ResponseTimestamp.getIssuerData
import com.bonushub.crdb.utils.Field48ResponseTimestamp.getTptData
import com.bonushub.pax.utils.*
import com.ingenico.hdfcpayment.type.BhTransactionType

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class CreateTransactionPacket(
    private var cardProcessedData: CardProcessedDataModal
    ) :
    ITransactionPacketExchange {

    private var indicator: String? = null
    //  private var brandEMIData: brandEMIData? = null
    //  private var brandEMIByAccessCodeData: BrandEMIAccessDataModalTable? = null

    //Below method is used to create Transaction Packet in all cases:-
    init {
        createTransactionPacket()
    }

    override fun createTransactionPacket(): IsoDataWriter = IsoDataWriter().apply {
        //Condition To Check BhTransactionType == BrandEMIByAccessCode if it is then fetch its value from DB:-
        /*if (cardProcessedData.getTransType() == BhTransactionType.BRAND_EMI_BY_ACCESS_CODE.type) {
            brandEMIByAccessCodeData =
                runBlocking(Dispatchers.IO) { BrandEMIAccessDataModalTable.getBrandEMIByAccessCodeData() }
        }*/


        /* if (cardProcessedData.getTransType() == BhTransactionType.BRAND_EMI.type) {
           // todo same
          //  brandEMIData = runBlocking(Dispatchers.IO) { brandEMIData.getAllEMIData() }
        }*/
        val terminalData = getTptData()
        if (terminalData != null) {
            logger("PINREQUIRED--->  ", cardProcessedData.getIsOnline().toString(), "e")
            mti =
                if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
                    Mti.REVERSAL.mti
                } else {
                    when (cardProcessedData.getTransType()) {
                       BhTransactionType.PRE_AUTH.type -> Mti.PRE_AUTH_MTI.mti
                        else -> Mti.DEFAULT_MTI.mti
                    }
                }

            //Processing Code Field 3
            addField(3, cardProcessedData.getProcessingCode().toString())

            //Transaction Amount Field
            //val formattedTransAmount = "%.2f".format(cardProcessedData.getTransactionAmount()?.toDouble()).replace(".", "")
            if (cardProcessedData.getTransType() == BhTransactionType.BRAND_EMI_BY_ACCESS_CODE.type) {

            } else {
                addField(
                    4,
                    addPad(cardProcessedData.getTransactionAmount().toString(), "0", 12, true)
                )
            }

            //STAN(ROC) Field 11
            cardProcessedData?.getAuthRoc()?.let { addField(11, it) }

            //Date and Time Field 12 & 13
            val date = cardProcessedData.getTimeStamp()
            if (date != null) {
                addIsoDateTime(this)
            }

            //println("Pos entry mode is --->" + cardProcessedData.getPosEntryMode().toString())
            //Pos Entry Mode Field 22
            //    if(null !=cardProcessedData.getPosEntryMode().toString() && cardProcessedData.getPosEntryMode().toString().isNotEmpty())
            addField(22, cardProcessedData.getPosEntryMode().toString())

            //Pan Sequence Number Field 23
            /*         if (null != cardProcessedData.getApplicationPanSequenceValue())
                addFieldByHex(
                    23,
                    addPad(
                        cardProcessedData.getApplicationPanSequenceValue().toString(),
                        "0",
                        3,
                        true
                    )
                )
            else {
                addFieldByHex(23, addPad("00", "0", 3, true))
            }*/

            //NII Field 24
            addField(24, Nii.DEFAULT.nii)
            //RRN  Field 37
        cardProcessedData.getRrn()?.let { addFieldByHex(37, it) }
            //RRN  Field 38
           val authcpdewithpading= cardProcessedData.getAuthCode()?.let { Padding(it) }
            authcpdewithpading?.let { addFieldByHex(38, it) }
            //TID Field 41
            cardProcessedData.getTid()?.let { addFieldByHex(41, it) }

            //MID Field 42
            terminalData.merchantId?.let { addFieldByHex(42, it) }

            //addFieldByHex(48, Field48ResponseTimestamp.getF48Data())
            //Connection Time Stamps Field 48
            addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

            /*   //Field 52 in case of Pin
            if (!(TextUtils.isEmpty(cardProcessedData.getGeneratePinBlock())) && cardProcessedData.getPinByPass() == 0)
                addField(52, cardProcessedData.getGeneratePinBlock().toString())

            //Field 54 in case od sale with cash AND Cash at POS.
            when (cardProcessedData.getTransType()) {
                BhTransactionType.CASH_AT_POS.type, BhTransactionType.SALE_WITH_CASH.type ->
                    addFieldByHex(
                        54,
                        addPad(cardProcessedData.getOtherAmount().toString(), "0", 12, true)
                    )
                else -> {
                }
            }


            //Field 55
            when (cardProcessedData.getReadCardType()) {
                DetectCardType.EMV_CARD_TYPE, DetectCardType.CONTACT_LESS_CARD_TYPE -> addField(
                    55, cardProcessedData.getFiled55().toString()
                )
                else -> {
                }
            }
*/
            //Below Field57 is Common for Cases Like CTLS + CTLSMAG + EMV + MAG:-
            // addField(57, cardProcessedData.getTrack2Data().toString())

            //Indicator Data Field 58
            /*  val cardIndFirst = "0"
            val firstTwoDigitFoCard = cardProcessedData.getPanNumberData()?.substring(0, 2)
            val cardDataTable = CardDataTable.selectFromCardDataTable(
                cardProcessedData.getPanNumberData().toString()
            )
            //  val cardDataTable = CardDataTable.selectFromCardDataTable(cardProcessedData.getTrack2Data()!!)
            val cdtIndex = cardDataTable?.cardTableIndex ?: ""
            val accSellection =
                addPad(
                    AppPreference.getString(AppPreference.ACC_SEL_KEY),
                    "0",
                    2
                ) //cardDataTable.getA//"00"

            //region===============Check If Transaction Type is EMI_SALE , Brand_EMI or Other then Field would be appended with Bank EMI Scheme Offer Values:-
            when (cardProcessedData.getTransType()) {
                BhTransactionType.EMI_SALE.type -> {


                }
*//*0|46|1|00,460133,54,135,25,586,650000,0,635960,3,1300,216596,14040,635748,12,8,,8287305603,,0,0,0,0,,*//*
                BhTransactionType.BRAND_EMI.type -> {

                }
*//*                0|43|1|00,438628,54,142,11,2358,1000000,0,1000000,3,1300,340581,0,1041743,,abcdxyz,,,,0,0,200.0,20000,42942319,
                  0|60|5|00,60832632,52,144,11,2356,800000,18320,781680,3,1400,266663,0,815623,,12qw3e,,,,0,0,200.0,15634,52429840,*//*

                BhTransactionType.BRAND_EMI_BY_ACCESS_CODE.type -> {
                    //cardProcessedData.getMobileBillExtraData()?.second replace with billno

                }

                else -> {
                    indicator = if( cardProcessedData.getTransType()==BhTransactionType.TEST_EMI.type ){
                            logger("TEST OPTION",cardProcessedData.testEmiOption,"e")
                        "$cardIndFirst|$firstTwoDigitFoCard|$cdtIndex|$accSellection|${cardProcessedData.testEmiOption}"
                    }else
                        "$cardIndFirst|$firstTwoDigitFoCard|$cdtIndex|$accSellection"
                }
            }*/


            Log.d("SALE Indicator:- ", indicator.toString())
            additionalData["indicatorF58"] = indicator ?: ""
            //Adding Field 58
            //addFieldByHex(58, indicator ?: "")

            //Adding Field 60 value on basis of Condition Whether it consist Mobile Number Data , Bill Number Data or not:-
            val gcc = "0"
            var field60: String? = null
            var batchNumber: String? = null
            when {
                !TextUtils.isEmpty(cardProcessedData.getMobileBillExtraData()?.first) -> {
                    batchNumber = addPad(terminalData.batchNumber, "0", 6, true)
                    val mobileNumber = cardProcessedData.getMobileBillExtraData()?.first
                    field60 = "$batchNumber|$mobileNumber|$gcc"
                    addFieldByHex(60, field60)
                }

                else -> {
                    batchNumber = cardProcessedData.getBatch()?.let { addPad(it, "0", 6, true) }
                    field60 = "$batchNumber||$gcc"
                    if (batchNumber != null) {
                        addFieldByHex(60, batchNumber)
                    }
                }
            }

            //adding field 61
            //adding field 61
            val issuerParameterTable =
                getIssuerData(AppPreference.WALLET_ISSUER_ID)
            val version = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)
            val pcNumbers = addPad(AppPreference.getString(PreferenceKeyConstant.PC_NUMBER_ONE.keyName), "0", 9)+addPad(AppPreference.getString(PreferenceKeyConstant.PC_NUMBER_TWO.keyName), "0", 9)
            val data = ConnectionType.GPRS.code + addPad(
                deviceModel(), "*",
                6,
                false
            ) + addPad(HDFCApplication.appContext.getString(R.string.app_name), " ", 10, false) +
                    version + pcNumbers
            /* val customerID = HexStringConverter.addPreFixer(
                 issuerParameterTable?.customerIdentifierFiledType,
                 2
             )*/
            val customerID =
                issuerParameterTable?.customerIdentifierFiledType?.let { addPad(it, "0", 2) } ?: 0

            //  val walletIssuerID = issuerParameterTable?.issuerId?.let { addPad(it, "0", 2) } ?: 0

            val walletIssuerID = if (cardProcessedData.getTransType() == BhTransactionType.EMI_SALE.type || cardProcessedData.getTransType() == BhTransactionType.BRAND_EMI.type) {
               // bankEmiTandCData?.issuerID?.let { addPad(it, "0", 2) } ?: 0
            }
            else if( cardProcessedData.getTransType() == BhTransactionType.BRAND_EMI_BY_ACCESS_CODE.type){
              ///  brandEMIByAccessCodeDataModel?.issuerID?.let { addPad(it, "0", 2) } ?: 0
            }
            else {
                issuerParameterTable?.issuerId?.let { addPad(it, "0", 2) } ?: 0
            }


            // old way
            //   val walletIssuerID = issuerParameterTable?.issuerId?.let { addPad(it, "0", 2) } ?: 0

            addFieldByHex(
                61, addPad(
                    DeviceHelper.getDeviceSerialNo() ?: "", " ", 15, false
                ) + AppPreference.getBankCode() + customerID + walletIssuerID + data
            )




            //adding field 62
            cardProcessedData.getInvoice()?.let { addFieldByHex(62, it) }

            //Here we are Saving Date , Time and TimeStamp in CardProcessedDataModal:-
            var year: String = "Year"


        }
    }

    fun addIsoDateTime(iWriter: IsoDataWriter) {
        val dateTime: Long = Calendar.getInstance().timeInMillis
        val time: String = SimpleDateFormat("HHmmss", Locale.getDefault()).format(dateTime)
        val date: String = SimpleDateFormat("MMdd", Locale.getDefault()).format(dateTime)

        with(iWriter) {
            addField(12, time)
            addField(13, date)
        }
    }

    private fun Padding(invoiceNo: String) = addPad(input = invoiceNo, padChar = " ", totalLen = 12, toLeft = true)
    fun getF61(): String {
        val serialNo=addPad(AppPreference.getString("serialNumber"), " ", 15, false)
        val appName = addPad(HDFCApplication.appContext.getString(R.string.app_name), " ", 10, false)
        val deviceModel = addPad(deviceModel(), "*", 6, false)
        val version = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)
        val connectionType = ConnectionType.GPRS.code
        val pccNo = addPad(AppPreference.getString(PreferenceKeyConstant.PC_NUMBER_ONE.keyName), "0", 9)
        val pcNo2 = addPad(AppPreference.getString(PreferenceKeyConstant.PC_NUMBER_TWO.keyName), "0", 9)
        return "$serialNo$connectionType$deviceModel$appName$version$pccNo$pcNo2"
    }
}