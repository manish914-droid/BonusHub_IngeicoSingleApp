package com.bonushub.crdb.india.transactionprocess


import android.util.Log
import com.bonushub.crdb.india.HDFCApplication
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.db.AppDao
import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.model.local.AppPreference
import com.bonushub.crdb.india.model.local.BatchTable
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getIssuerData
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getTptData
import com.bonushub.crdb.india.vxutils.BhTransactionType
import com.bonushub.crdb.india.vxutils.deviceModel
import com.bonushub.crdb.india.vxutils.getAppVersionNameAndRevisionID
import com.bonushub.crdb.india.vxutils.getConnectionType
import com.bonushub.pax.utils.ITransactionPacketExchange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class CreateTransactionPacket @Inject constructor(private var appDao: AppDao,private var cardProcessedData: CardProcessedDataModal,private var batchdata:BatchTable?) : ITransactionPacketExchange {


    private var indicator: String? = null
    //  private var brandEMIData: brandEMIData? = null
    //  private var brandEMIByAccessCodeData: BrandEMIAccessDataModalTable? = null

    //Below method is used to create Transaction Packet in all cases:-

   // val receiptDetail: ReceiptDetail =batchTable.receiptData ?: ReceiptDetail(

    override fun createTransactionPacket(): IsoDataWriter = IsoDataWriter().apply {
       // val batchListData = runBlocking(Dispatchers.IO) { appDao.getSinleBatchData() }
         //To get Base Tid
     //   val baseTid = runBlocking(Dispatchers.IO) { getBaseTID(appDao) }
        //To get base Tid batch Number
        val tptbaseTiddata = runBlocking(Dispatchers.IO) { getTptData() }

        val terminalData = getTptData()
        if (terminalData != null) {
            logger("PINREQUIRED--->  ", cardProcessedData.getIsOnline().toString(), "e")
            mti = when (cardProcessedData.getTransType()) {
                       BhTransactionType.PRE_AUTH.type -> Mti.PRE_AUTH_MTI.mti
                BhTransactionType.PRE_AUTH_COMPLETE.type , BhTransactionType.VOID_PREAUTH.type->Mti.PRE_AUTH_COMPLETE_MTI.mti
                        else -> Mti.DEFAULT_MTI.mti
                    }

            //Processing Code Field 3
            addField(3, cardProcessedData.getProcessingCode().toString())

            //Transaction Amount Field
            //val formattedTransAmount = "%.2f".format(cardProcessedData.getTransactionAmount()?.toDouble()).replace(".", "")
            if (cardProcessedData.getTransType() == BhTransactionType.BRAND_EMI_BY_ACCESS_CODE.type) {
// todo for brand Emi by code
            }
          /*  else if(cardProcessedData.getTransType() == BhTransactionType.SALE_WITH_CASH.type){
             val txnAmt= batchdata?.receiptData?.txnAmount?.toLong()
                 ?.plus(batchdata?.receiptData?.txnOtherAmount?.toLong()!!)
                addField(
                    4,
                    addPad(txnAmt.toString(), "0", 12, true)
                )
            }*/
            else {
                addField(
                    4,
                    addPad(cardProcessedData.getTransactionAmount().toString(), "0", 12, true)
                )
            }

            //STAN(ROC) Field 11
           // cardProcessedData?.getAuthRoc()?.let { addField(11, it) }
            addField(11,batchdata?.bonushubStan ?: "")

            //Date and Time Field 12 & 13
            val date = cardProcessedData.getTimeStamp()
            if (date != null) {
                addIsoDateTime(this)
            }

            //println("Pos entry mode is --->" + cardProcessedData.getPosEntryMode().toString())
            //Pos Entry Mode Field 22
            //    if(null !=cardProcessedData.getPosEntryMode().toString() && cardProcessedData.getPosEntryMode().toString().isNotEmpty())

if(cardProcessedData.getTransType()!= BhTransactionType.PRE_AUTH_COMPLETE.type) {
    addField(22, cardProcessedData.getPosEntryMode().toString())
}

            //NII Field 24
            addField(24, Nii.DEFAULT.nii)
            //RRN  Field 37
        cardProcessedData.getRrn()?.let { addFieldByHex(37, it) }
            //RRN  Field 38
           val authcpdewithpading= cardProcessedData.getAuthCode()?.let { Padding(it) }
            authcpdewithpading?.let { addFieldByHex(38, it) }
            //TID Field 41
            cardProcessedData.getTid()?.let { tptbaseTiddata?.terminalId?.let { it1 ->
                addFieldByHex(41,
                    it1
                )
            } } //here will be base tid

            //MID Field 42
            terminalData.merchantId?.let { addFieldByHex(42, it) }

            //addFieldByHex(48, Field48ResponseTimestamp.getF48Data())
            //Connection Time Stamps Field 48
            addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

            when (cardProcessedData.getTransType()) {
                BhTransactionType.CASH_AT_POS.type, BhTransactionType.SALE_WITH_CASH.type ->
                    addFieldByHex(
                        54,
                        addPad(cardProcessedData.getOtherAmount().toString(), "0", 12, true)
                    )
                BhTransactionType.SALE.type ->{
                    if(cardProcessedData.getOtherAmount()!=0L)  {
                        addFieldByHex(
                            54,
                            addPad(cardProcessedData.getTipAmount().toString(), "0", 12, true)
                        )
                    }
                }
                else -> {
                }
            }

            //Below Field56 is used for Void txns.
            if(cardProcessedData.getTransType()==BhTransactionType.VOID.type){
           /* old (means main transaction???s BH Base tid)stan 6 digits and then old data time yymmddhhmmss*/
                try {
                    val dateTime = batchdata?.oldDateTimeInVoid
                    val fromFormat: DateFormat =
                        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                    val toFormat: DateFormat = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault())
                    val reqDate: Date? = fromFormat.parse(dateTime ?: "")

                    val stan = batchdata?.oldStanForVoid
                    val reqDateString = toFormat.format(reqDate)

                    val field56="${stan}${reqDateString}"
                    addFieldByHex(56,field56)

                }catch (ex:Exception){
                    logger("Date Error","Exception in adding field 56","e")
                }
            }

            if(cardProcessedData.getTransType()==BhTransactionType.PRE_AUTH_COMPLETE.type){
              //  TidBatchStanYYMMHHMMSS like 41501121000166000581220127193501
                val preAuthData= batchdata?.receiptData?.invoice?.let {
                    Field48ResponseTimestamp.getPreAuthByInvoice(
                        it
                    )
                }
                val dataList =
                    preAuthData?.field56String?.let { Utility().parseDataListWithSplitter("|", it) }
                //preAuth 0|10|50|000000000VZ0001|30160031|000066|000236|000236||
               /// TidBatchStanYYMMHHMMSS
                try {
                    val dateTime = batchdata?.oldDateTimeInVoid
                    val fromFormat: DateFormat =
                        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                    val toFormat: DateFormat = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault())
                    val reqDate: Date? = fromFormat.parse(dateTime ?: "")

                  //  val stan = batchdata?.oldStanForVoid
                    val reqDateString = toFormat.format(reqDate)

                 //   val field56="${stan}${reqDateString}"

                    val data1=dataList?.get(4)+dataList?.get(5)+dataList?.get(6)+reqDateString
                  // val data=batchdata?.receiptData?.tid+batchdata?.bonushubbatchnumber+batchdata?.oldStanForVoid+reqDateString
                     addFieldByHex(56,data1)


                }catch (ex:Exception){
                    logger("Date Error","Exception in adding field 56","e")
                }



            }


            //Below Field57 is Common for Cases Like CTLS + CTLSMAG + EMV + MAG:-
            when (cardProcessedData.getTransType()) {
                BhTransactionType.SALE.type ,BhTransactionType.CASH_AT_POS.type,
                BhTransactionType.SALE_WITH_CASH.type,BhTransactionType.REFUND.type,BhTransactionType.PRE_AUTH.type,
                BhTransactionType.PRE_AUTH_COMPLETE.type -> {
                    batchdata?.field57EncryptedData?.let { addField(57, it) }
                }
                BhTransactionType.BRAND_EMI.type , BhTransactionType.EMI_SALE.type , BhTransactionType.TEST_EMI.type->{
                    batchdata?.field57EncryptedData?.let { addField(57, it) }
                }

                BhTransactionType.VOID.type->{
                    batchdata?.field57EncryptedData?.let { addField(57, it) }
                }

            }

            //region===============Check If Transaction Type is EMI_SALE , Brand_EMI or Other then Field would be appended with Bank EMI Scheme Offer Values:-

            when (cardProcessedData.getTransType()) {

                BhTransactionType.EMI_SALE.type -> {
                    batchdata?.field58EmiData?.let { addFieldByHex(58, it) }
                }

                BhTransactionType.BRAND_EMI.type -> {
                    batchdata?.field58EmiData?.let { addFieldByHex(58, it) }
     }

                BhTransactionType.BRAND_EMI_BY_ACCESS_CODE.type -> {
                    //cardProcessedData.getMobileBillExtraData()?.second replace with billno
               /*     indicator = "$cardIndFirst|$firstTwoDigitFoCard|$cdtIndex|$accSellection," +
                            "${cardProcessedData.getPanNumberData()?.substring(0, 8)}," +
                            "${brandEMIByAccessCodeDataModel?.issuerID},${brandEMIByAccessCodeDataModel?.emiSchemeID},${brandEMIByAccessCodeDataModel?.brandID}," +
                            "${brandEMIByAccessCodeDataModel?.productID},${brandEMIByAccessCodeDataModel?.orignalTxnAmt}," +
                            "${brandEMIByAccessCodeDataModel?.discountAmount},${brandEMIByAccessCodeDataModel?.loanAmount},${brandEMIByAccessCodeDataModel?.tenure}," +
                            "${brandEMIByAccessCodeDataModel?.interestAmount},${brandEMIByAccessCodeDataModel?.emiAmount},${brandEMIByAccessCodeDataModel?.cashBackAmount}," +
                            "${brandEMIByAccessCodeDataModel?.netPayAmount},${cardProcessedData.getMobileBillExtraData()?.second ?: ""}," +
                            "${brandEMIByAccessCodeDataModel?.productSerialCode ?: ""},,${cardProcessedData.getMobileBillExtraData()?.first ?: ""},,0,${brandEMIByAccessCodeDataModel?.processingFee},${brandEMIByAccessCodeDataModel?.processingFeeRate}," +
                            "${brandEMIByAccessCodeDataModel?.totalProcessingFee},${brandEMIByAccessCodeDataModel?.emiCode},${brandEMIByAccessCodeDataModel?.instaDiscount}"
            */

                }

                BhTransactionType.TEST_EMI.type->{

                    batchdata?.field58EmiData?.let { addFieldByHex(58, it) }
                }

                BhTransactionType.VOID.type->{
                    if(batchdata?.field58EmiData?.isNotBlank() == true){
                        batchdata?.field58EmiData?.let { addFieldByHex(58, it) }
                    }
                }

                else -> {
                     }
            }

            //  Filed 60-
            //  6 digit batch number BH(OF Base  tid)|mob|coupon code |ing tid|ING batch|Ing stan |Ing invoice |Aid|TC|app name|
            val bonushubbatch = addPad(tptbaseTiddata?.batchNumber ?: "", "0", 6, true)
            val emptyString: String= ""
            var ingenicotid          = addPad(batchdata?.receiptData?.tid ?: "", "0", 8, true)
            val ingenibatchnumber    = addPad(batchdata?.receiptData?.batchNumber ?: "", "0", 6, true)
            val ingenicostan         = addPad(batchdata?.receiptData?.stan ?: "", "0", 6, true)
            val ingenicoInvoice        = addPad(batchdata?.receiptData?.invoice ?: "", "0", 6, true)
            val ingenicoaid           = batchdata?.receiptData?.aid ?: ""
            val ingenicotc           = batchdata?.receiptData?.tc ?: ""
            val ingenicoappName     = batchdata?.receiptData?.appName ?: ""

            if(batchdata?.transactionType==BhTransactionType.VOID.type){
               // ingenicoaid=
            }

            val fielddata60 = "$bonushubbatch|$emptyString|$emptyString|$ingenicotid|"+
                    "$ingenibatchnumber|$ingenicostan|$ingenicoInvoice|"+
                    "$ingenicoaid|$ingenicotc|$ingenicoappName|"


            println("Field 60 value is -> $fielddata60")

            addFieldByHex(60,fielddata60)


            Log.d("SALE Indicator:- ", indicator.toString())
            additionalData["indicatorF58"] = indicator ?: ""
            //Adding Field 58
            //addFieldByHex(58, indicator ?: "")

            //Adding Field 60 value on basis of Condition Whether it consist Mobile Number Data , Bill Number Data or not:-
            val gcc = "0"
            var field60: String? = null
             var batchNumber: String? = null
       /*     when {
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
            }*/

            //adding field 61
            //adding field 61
            val issuerParameterTable = getIssuerData(AppPreference.WALLET_ISSUER_ID)
            val version = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)
            val pcNumbers = addPad(AppPreference.getString(PreferenceKeyConstant.PC_NUMBER_ONE.keyName), "0", 9)+addPad(AppPreference.getString(
                PreferenceKeyConstant.PC_NUMBER_TWO.keyName), "0", 9)
            val data = getConnectionType() + addPad(
                deviceModel(), "*",
                6,
                false
            ) + addPad(HDFCApplication.appContext.getString(R.string.app_name), " ", 10, false) +
                    version + pcNumbers
            /* val customerID = HexStringConverter.addPreFixer(
                 issuerParameterTable?.customerIdentifierFiledType,
                 2
             )*/
            val customerID = issuerParameterTable?.customerIdentifierFiledType?.let { addPad(it, "0", 2) } ?: 0

            //  val walletIssuerID = issuerParameterTable?.issuerId?.let { addPad(it, "0", 2) } ?: 0

            val walletIssuerID:String = if (cardProcessedData.getTransType() == BhTransactionType.EMI_SALE.type || cardProcessedData.getTransType() == BhTransactionType.BRAND_EMI.type) {
               // bankEmiTandCData?.issuerID?.let { addPad(it, "0", 2) } ?: 0
                val emiIssuerDataModel=batchdata?.emiIssuerDataModel
                emiIssuerDataModel?.issuerID?.let { addPad(it, "0", 2) } ?: "0"
            }
            else if( cardProcessedData.getTransType() == BhTransactionType.BRAND_EMI_BY_ACCESS_CODE.type){
              ///  brandEMIByAccessCodeDataModel?.issuerID?.let { addPad(it, "0", 2) } ?: "0"
                "0"
            }
            else {
                issuerParameterTable?.issuerId?.let { addPad(it, "0", 2) } ?: "0"
            }

          var serialnumm=""
            serialnumm = if(BhTransactionType.BRAND_EMI.type==cardProcessedData.getTransType()|| BhTransactionType.EMI_SALE.type==cardProcessedData.getTransType() ) {
                val tenureData=batchdata?.emiTenureDataModel
                if( cardProcessedData.getTid()==tenureData?.txnTID){
                    DeviceHelper.getDeviceSerialNo().toString()
                }else{
                    tenureData?.txnTID.toString()
                }
            }else{
                DeviceHelper.getDeviceSerialNo().toString()
            }
            addFieldByHex(61, addPad(serialnumm ?: "", " ", 15, false) + AppPreference.getBankCode() + customerID + walletIssuerID + data)
            //adding field 62
         //   cardProcessedData.getInvoice()?.let { addFieldByHex(62, it) }

           //Below Field62 is used for Void txns.
                if(cardProcessedData.getTransType()==BhTransactionType.VOID.type){
                 //   Field 62-old invoice [of BH base tid assigned to that main txn] (means main transaction???s BH Base TID)
                    addFieldByHex(62, batchdata?.bonushubInvoice ?: "")

                }else {
                    addFieldByHex(62, batchdata?.bonushubInvoice ?: "")
                }
        }
    }

    private fun addIsoDateTime(iWriter: IsoDataWriter) {
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
        val connectionType = getConnectionType()
        val pccNo = addPad(AppPreference.getString(PreferenceKeyConstant.PC_NUMBER_ONE.keyName), "0", 9)
        val pcNo2 = addPad(AppPreference.getString(PreferenceKeyConstant.PC_NUMBER_TWO.keyName), "0", 9)
        return "$serialNo$connectionType$deviceModel$appName$version$pccNo$pcNo2"
    }
}