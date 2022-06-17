

package com.bonushub.crdb.india.transactionprocess

import android.text.TextUtils
import android.util.Log
import com.bonushub.crdb.india.BuildConfig
import com.bonushub.crdb.india.HDFCApplication
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.db.AppDao
import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.model.local.AppPreference
import com.bonushub.crdb.india.model.local.BatchTable
import com.bonushub.crdb.india.model.remote.BankEMIIssuerTAndCDataModal
import com.bonushub.crdb.india.model.remote.BankEMITenureDataModal
import com.bonushub.crdb.india.model.remote.BrandEMIDataModal
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getIssuerData
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getMaskedPan
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getTptData
import com.bonushub.crdb.india.vxutils.TransactionType
import com.bonushub.pax.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.lang.Exception
import java.text.DateFormat


import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class CreateTransactionPacketNew @Inject constructor(private var appDao: AppDao,
                                                     private var bankEmiSchemeData: BankEMITenureDataModal?,
                                                     private var bankEmiTandCData: BankEMIIssuerTAndCDataModal?,
                                                     private var brandEMIData: BrandEMIDataModal?,
                                                     private var cardProcessedData: CardProcessedDataModal,
                                                     private var batchdata:BatchTable?) : ITransactionPacketExchangeNew {

    // bankEmiTandCData =  emiTAndCData
    // bankEmiSchemeData =  emiSelectedData

    //Below method is used to create Transaction Packet in all cases:-
    // this method call two times. 1st when create a object. 2nd when call method to get IsoDataWriter object.
    // and In init block we can hold the return object 'IsoDataWriter' value so it's extra call.
   /* init {
        createTransactionPacketNew()
    }*/

    override fun createTransactionPacketNew(): IsoDataWriter = IsoDataWriter().apply {
        //     val batchFileDataTable = BatchFileDataTable.selectBatchData()
        logger("kushal","createTransactionPacketNew","e")
        val terminalData = getTptData()
        if (terminalData != null) {
            logger("PINREQUIRED--->  ", cardProcessedData.getIsOnline().toString(), "e")
            mti =
                if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.GENERIC_REVERSAL_KEY))) {
                    Mti.REVERSAL.mti
                } else {
                    when (cardProcessedData.getTransType()) {
                       TransactionType.PRE_AUTH.type -> Mti.PRE_AUTH_MTI.mti
                        else -> Mti.DEFAULT_MTI.mti
                    }
                }

            //Processing Code Field 3
            addField(3, cardProcessedData.getProcessingCode().toString())
           // addField(3, "920001")

            //Transaction Amount Field
            addField(4, addPad(cardProcessedData.getTransactionAmount().toString(), "0", 12, true))


            //STAN(ROC) Field 11
            addField(11, Utility().getROC().toString())

            //Date and Time Field 12 & 13
            val dateTime = addIsoDateTime(this)

            //println("Pos entry mode is --->" + cardProcessedData.getPosEntryMode().toString())
            //Pos Entry Mode Field 22
            //    if(null !=cardProcessedData.getPosEntryMode().toString() && cardProcessedData.getPosEntryMode().toString().isNotEmpty())
            addField(22, cardProcessedData.getPosEntryMode().toString())
           // addField(22,"0553")

            //Pan Sequence Number Field 23
            addFieldByHex(
                23,
                addPad(cardProcessedData.getApplicationPanSequenceValue().toString(), "0", 3, true)
            )

            //NII Field 24
            addField(24, Nii.DEFAULT.nii)

            //TID Field 41
            addFieldByHex(41, terminalData.terminalId)

            //MID Field 42
            addFieldByHex(42, terminalData.merchantId)

            //Connection Time Stamps Field 48
            addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

            //Field 52 in case of Pin
            if (!(TextUtils.isEmpty(cardProcessedData.getGeneratePinBlock())))
                addField(52, cardProcessedData.getGeneratePinBlock().toString())

            //Field 55
            when (cardProcessedData.getReadCardType()) {
                DetectCardType.EMV_CARD_TYPE, DetectCardType.CONTACT_LESS_CARD_TYPE -> addField(
                    55, cardProcessedData.getFiled55().toString()
                )
                else -> {
                }
            }



            logger("57 rwa data --->  ", cardProcessedData.getEncryptedPan().toString(), "e")
            //Below Field57 is Common for Cases Like CTLS + CTLSMAG + EMV + MAG:-
            addField(57, cardProcessedData.getEncryptedPan().toString())

            //Indicator Data Field 58
            val cardIndFirst = "0"
            val firstTwoDigitFoCard = cardProcessedData.getPanNumberData()?.substring(0, 2) ?: "37"
            val cardDataTable = appDao.getCardDataByPanNumber(cardProcessedData?.getPanNumberData().toString())
           // val cardDataTable = CardDataTable.selectFromCardDataTable(cardProcessedData.getPanNumberData().toString())
            //  val cardDataTable = CardDataTable.selectFromCardDataTable(cardProcessedData.getTrack2Data()!!)
            val cdtIndex = cardDataTable?.cardTableIndex ?: "4"
            val accSellection =
                addPad(
                    AppPreference.getString(AppPreference.ACC_SEL_KEY),
                    "0",
                    2
                ) //cardDataTable.getA//"00"

            var indicator = ""
                //"$cardIndFirst|$firstTwoDigitFoCard|$cdtIndex|$accSellection"//used for visa// used for ruppay//"0|54|2|00"

            when (cardProcessedData.getTransType()) {
                TransactionType.EMI_SALE.type -> {
                    indicator = "$cardIndFirst|$firstTwoDigitFoCard|$cdtIndex|$accSellection," +
                            "${cardProcessedData.getPanNumberData()?.substring(0, 8)}," +
                            "${bankEmiTandCData?.issuerID}," +
                            "${bankEmiTandCData?.emiSchemeID},1,0,${cardProcessedData.getEmiTransactionAmount()}," +
                            "${bankEmiSchemeData?.discountAmount},${bankEmiSchemeData?.loanAmount},${bankEmiSchemeData?.tenure}," +
                            "${bankEmiSchemeData?.tenureInterestRate},${bankEmiSchemeData?.emiAmount},${bankEmiSchemeData?.cashBackAmount}," +
                            "${bankEmiSchemeData?.netPay},${cardProcessedData.getMobileBillExtraData()?.second ?: ""}," +
                            ",,${cardProcessedData.getMobileBillExtraData()?.first ?: ""},,${cardProcessedData.getEmiType()},${bankEmiSchemeData?.processingFee},${bankEmiSchemeData?.processingRate}," +
                            "${bankEmiSchemeData?.totalProcessingFee},,${bankEmiSchemeData?.instantDiscount}"

                }
/*0|46|1|00,460133,54,135,25,586,650000,0,635960,3,1300,216596,14040,635748,12,8,,8287305603,,0,0,0,0,,*/
                TransactionType.BRAND_EMI.type -> {
                    var imeiOrSerialNo:String?=null
                    if(brandEMIData?.imeiORserailNum !="" ){
                        imeiOrSerialNo=brandEMIData?.imeiORserailNum
                    }

                    indicator = "$cardIndFirst|$firstTwoDigitFoCard|$cdtIndex|$accSellection," +
                            "${cardProcessedData.getPanNumberData()?.substring(0, 8)}," +
                            "${bankEmiTandCData?.issuerID},${bankEmiTandCData?.emiSchemeID},${brandEMIData?.brandID}," +
                            "${brandEMIData?.productID},${cardProcessedData.getEmiTransactionAmount()}," +
                            "${bankEmiSchemeData?.discountAmount},${bankEmiSchemeData?.loanAmount},${bankEmiSchemeData?.tenure}," +
                            "${bankEmiSchemeData?.tenureInterestRate},${bankEmiSchemeData?.emiAmount},${bankEmiSchemeData?.cashBackAmount}," +
                            "${bankEmiSchemeData?.netPay},${cardProcessedData.getMobileBillExtraData()?.second ?: ""}," +
                            "${imeiOrSerialNo ?: ""},,${cardProcessedData.getMobileBillExtraData()?.first ?: ""},,0,${bankEmiSchemeData?.processingFee},${bankEmiSchemeData?.processingRate}," +
                            "${bankEmiSchemeData?.totalProcessingFee},,${bankEmiSchemeData?.instantDiscount}"

                }
/*                0|43|1|00,438628,54,142,11,2358,1000000,0,1000000,3,1300,340581,0,1041743,,abcdxyz,,,,0,0,200.0,20000,42942319,
                  0|60|5|00,60832632,52,144,11,2356,800000,18320,781680,3,1400,266663,0,815623,,12qw3e,,,,0,0,200.0,15634,52429840,*/

                else -> {
                    indicator = if( cardProcessedData.getTransType()==TransactionType.TEST_EMI.type ){
                        logger("TEST OPTION",cardProcessedData.testEmiOption,"e")
                        "$cardIndFirst|$firstTwoDigitFoCard|$cdtIndex|$accSellection|${cardProcessedData.testEmiOption}"
                    }else
                        "$cardIndFirst|$firstTwoDigitFoCard|$cdtIndex|$accSellection"
                }
            }

            addFieldByHex(58, indicator)

            Log.d("SALE Indicator:- ", indicator.toString())
            additionalData["indicatorF58"] = indicator ?: ""

            //Adding Field 60 value on basis of Condition Whether it consist Mobile Number Data , Bill Number Data or not:-
            val gcc = "0"
            var field60 : String? = null
            var batchNumber : String? = null
            when{
                !TextUtils.isEmpty(cardProcessedData.getMobileBillExtraData()?.first) -> {
                    batchNumber = addPad(terminalData.batchNumber , "0" , 6,true)
                    val mobileNumber = cardProcessedData.getMobileBillExtraData()?.first
                    field60 = "$batchNumber|$mobileNumber|$gcc"
                    addFieldByHex(60 , field60)
                }

                else -> {
                    batchNumber = addPad(terminalData.batchNumber , "0" , 6,true)
                    field60 = "$batchNumber||$gcc"
                    addFieldByHex(60 , field60)
                }
            }

            //adding field 61
            val buildDate: String = SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date(BuildConfig.TIMESTAMP))
            val issuerParameterTable = getIssuerData(AppPreference.WALLET_ISSUER_ID)
           // val issuerParameterTable = IssuerParameterTable.selectFromIssuerParameterTable(AppPreference.WALLET_ISSUER_ID)
            val version = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)
            val pcNumbers= addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY), "0", 9)+addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY_2), "0", 9)
            val data = getConnectionType() + addPad(
                deviceModel(),
                " ",
                6,
                false
            ) + addPad(HDFCApplication.appContext.getString(R.string.app_name), " ", 10, false) +
                    version  + pcNumbers
            val customerID = issuerParameterTable?.customerIdentifierFiledType?.let { addPad(it, "0", 2) } ?: 0
            //val customerID = HexStringConverter.addPreFixer(issuerParameterTable?.customerIdentifierFiledType, 2)
            //val walletIssuerID = issuerParameterTable?.issuerId?.let { addPad(it, "0", 2) } ?: 0

            val walletIssuerID = if (cardProcessedData.getTransType() == TransactionType.EMI_SALE.type || cardProcessedData.getTransType() == TransactionType.BRAND_EMI.type) {
                bankEmiTandCData?.issuerID?.let { addPad(it, "0", 2) } ?: 0
            }
            else {
                issuerParameterTable?.issuerId?.let { addPad(it, "0", 2) } ?: 0
            }

            addFieldByHex(61, addPad(DeviceHelper.getDeviceSerialNo() ?: "", " ", 15, false)  + AppPreference.getBankCode() + customerID + walletIssuerID + data)

            //adding field 62
            addFieldByHex(62, terminalData.invoiceNumber)

            //Here we are Saving Date , Time and TimeStamp in CardProcessedDataModal:-
            var year: String = "Year"
            try {
                /*val date: Long = Calendar.getInstance().timeInMillis
                val timeFormater = SimpleDateFormat("HHmmss", Locale.getDefault())
                cardProcessedData.setTime(timeFormater.format(date))
                val dateFormater = SimpleDateFormat("MMdd", Locale.getDefault())
                cardProcessedData.setDate(dateFormater.format(date))
                cardProcessedData.setTimeStamp(date.toString())*/

                cardProcessedData.setTime(dateTime.second)
                cardProcessedData.setDate(dateTime.first)
                cardProcessedData.setTimeStamp(dateTime.third.toString())
                year = SimpleDateFormat("yy", Locale.getDefault()).format(dateTime.third)

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            //  saving field 56 if reversal generated for this trans then in next trans we send this field in reversal
            val f56Roc = Utility().getROC()?.let { addPad(it, "0", 6) }
            val f56Date=this.isoMap[13]?.rawData
            val f56Time=this.isoMap[12]?.rawData

            val tid = terminalData.terminalId
            val batchNumbers = terminalData.batchNumber
            //tid and batch number are latest change in no response reversal
            additionalData["F56reversal"] =
                tid+batchNumbers+f56Roc + year + f56Date + f56Time

            //here we are saving txn date time for 2nd gen ac reversal
            AppPreference.saveString("OldCurrentDate",f56Date)
            AppPreference.saveString("OldCurrentTime",f56Time)

            additionalData["pan"]= getMaskedPan(
                getTptData(), cardProcessedData.getPanNumberData() ?: "")

            additionalData["cardType"]= cardProcessedData.getReadCardType()?.cardTypeName.toString()

        }
        else{

            Log.e("TPT","Tpt is null")
        }
    }
}