package com.bonushub.crdb.model.local

import android.os.Parcel
import android.os.Parcelable
import androidx.room.*
import com.bonushub.crdb.di.scope.BHDashboardItem
import com.bonushub.crdb.di.scope.BHFieldName
import com.bonushub.crdb.di.scope.BHFieldParseIndex
import com.bonushub.crdb.model.CardProcessedDataModal
import com.bonushub.crdb.model.remote.BankEMIIssuerTAndCDataModal
import com.bonushub.crdb.model.remote.BankEMITenureDataModal
import com.bonushub.crdb.model.remote.BrandEMIMasterDataModal
import com.bonushub.crdb.model.remote.BrandEMIProductDataModal
import com.bonushub.crdb.utils.EDigiPosPaymentStatus
import com.bonushub.crdb.utils.EDashboardItem
import com.ingenico.hdfcpayment.model.ReceiptDetail
import java.io.Serializable


@Entity
data class BatchFileDataTable(
    var authCode: String = "",
    var isChecked: Boolean = false,
    var cashBackAmount: String = "",
    var panMaskFormat: String = "",
    var panMaskConfig: String = "",
    var panMask: String = "",
    var terminalSerialNumber: String = "",
    var responseCode: String = "",
    var tid: String = "",
    var mid: String = "",
    var batchNumber: String = "",
    var baseAmount: String = "",
    var roc: String = "",

    @PrimaryKey
    var invoiceNumber: String = "",
    var panNumber: String = "",
    var time: String = "",
    var date: String = "",
    var printDate: String = "",
    var currentYear: String = "",
    var currentTime: String = "",
    var expiryDate: String = "",
    var cardHolderName: String = "",
    var timeStamp: Long = 0,
    var generatedPinBlock: String = "",
    var field55Data: String = "",
    var track2Data: ByteArray? = null,
    var transactionType: Int = 0,
    var applicationPanSequenceNumber: String = "",
    var nii: String = "",
    var indicator: String = "",
    var bankCode: String = "",
    var customerId: String = "",
    var walletIssuerId: String = "",
    var connectionType: String = "",
    var modelName: String = "",
    var appName: String = "",
    var appVersion: String = "",
    var pcNumber: String = "",
    var posEntryValue: String = "",
    var transactionalAmount: String = "200",
    var mti: String = "",
    var serialNumber: String = "",
    var sourceNII: String = "",
    var destinationNII: String = "",
    var processingCode: String = "",
    var merchantName: String = "",
    var merchantAddress1: String = "",
    var merchantAddress2: String = "",
    var transactionDate: String = "",
    var transactionTime: String = "",
    var transactionName: String = "",
    var cardType: String = "",
    var expiry: String = "",
    var cardNumber: String = "",
    var referenceNumber: String = "",
    var aid: String = "",
    var tc: String = "",
    var tipAmount: String = "",
    var totalAmount: String = "",
    var isPinVerified: Boolean = false,
    var disclaimerMessage: String = "",
    var isMerchantCopy: Boolean = true,
    var message: String = "",
    var isTimeOut: Boolean = false,
    var operationType: String = "",
    var isVoid: Boolean = false,
    var f48IdentifierWithTS: String = "",
    var tvr: String = "",
    var tsi: String = "",
    var aqrRefNo: String = "",
    var hasPromo: Boolean = false,
    var gccMsg: String = "",
    var isOfflineSale: Boolean = false,
    var cdtIndex: String = "",
    var isRefundSale: Boolean = false,
    //  var accountType: String = EAccountType.DEFAULT.code,
    var merchantBillNo: String = "",
    var serialNo: String = "",
    var customerName: String = "",
    var phoneNo: String = "",
    var email: String = "",
    var emiBin: String = "",
    var issuerId: String = "",
    var emiSchemeId: String = "",
    var transactionAmt: String = "",
    var cashDiscountAmt: String = "",
    var loanAmt: String = "",
    var tenure: String = "",
    var roi: String = "",
    var monthlyEmi: String = "",
    var cashback: String = "",
    var netPay: String = "",
    var processingFee: String = "",
    var totalInterest: String = "",
    var brandId: String = "01",
    var productId: String = "0",
    var isServerHit: Boolean = false,
    var merchantMobileNumber: String = "",
    var merchantBillNumber: String = "",
    var cashBackPercent: String = "",
    var isCashBackInPercent: Boolean = false,

    var authROC: String = "",
    var authTID: String = "",
    var authBatchNO: String = "",
    var encryptPan: String = "",
    var amountInResponse: String = "",
    var isVoidPreAuth: Boolean = false,
    var isPreAuthComplete: Boolean = false,
    var otherAmount: String = "",

    //Host Response Fields:-
    var hostAutoSettleFlag: String? = null,
    var hostBankID: String? = null,
    var hostIssuerID: String? = null,
    var hostMID: String? = null,
    var hostTID: String? = null,
    var hostBatchNumber: String? = null,
    var hostRoc: String? = null,
    var hostInvoice: String? = null,
    var hostCardType: String? = null
) : Serializable

@Entity
data class TerminalCommunicationTable(
    @field:BHFieldParseIndex(0)
    var pcNo: String = "",

    @field:BHFieldParseIndex(1)
    var actionId: String = "",

    @field:BHFieldParseIndex(2)
    var tableId: String = "",

    @field:BHFieldParseIndex(3)
    var isActive: String = "",

    @field:BHFieldParseIndex(4)
    var recordId: String = "",

    @PrimaryKey
    @field:BHFieldParseIndex(5)
    var recordType: String = "",

    @field:BHFieldParseIndex(7)
    var epbxEnable: String = "",

    @field:BHFieldParseIndex(8)
    var nii: String = "",

    @field:BHFieldParseIndex(9)
    var authorizationPrimaryPhoneNo1: String = "",

    @field:BHFieldParseIndex(10)
    var authorizationSecondaryPhone1: String = "",

    @field:BHFieldParseIndex(11)
    var primarySettlementPhone1: String = "",

    @field:BHFieldParseIndex(12)
    var secondarySettlementPhone2: String = "",

    @field:BHFieldParseIndex(13)
    var dialTimeOut: String = "",

    @field:BHFieldParseIndex(14)
    @field:BHFieldName("APN")
    var apn: String = "",

    @field:BHFieldParseIndex(15)
    @field:BHFieldName("APN User Name")
    var apnUserName: String = "",

    @field:BHFieldParseIndex(16)
    @field:BHFieldName("APN Password")
    var apnPassword: String = "",

    @field:BHFieldParseIndex(17)
    @field:BHFieldName("Host Primary IP")
    var hostPrimaryIp: String = "",

    @field:BHFieldParseIndex(18)
    @field:BHFieldName("Host Primary Port")
    var hostPrimaryPortNo: String = "",

    @field:BHFieldParseIndex(19)
    @field:BHFieldName("Host Secondary IP")
    var hostSecIp: String = "",

    @field:BHFieldParseIndex(20)
    @field:BHFieldName("Host Secondary Port")
    var hostSecPortNo: String = "",

    @field:BHFieldParseIndex(21)
    var dnsPrimary: String = "",

    @field:BHFieldParseIndex(22)
    var primaryGateway: String = "",

    @field:BHFieldParseIndex(23)
    var primarySubnet: String = "",

    @field:BHFieldParseIndex(24)
    var hostEthPrimaryIp: String = "",

    @field:BHFieldParseIndex(25)
    var hostPrimaryEthPort: String = "",

    @field:BHFieldParseIndex(26)
    var dnsSecondary: String = "",

    @field:BHFieldParseIndex(27)
    var secondaryGateway: String = "",

    @field:BHFieldParseIndex(28)
    var secondarySubnet: String = "",

    @field:BHFieldParseIndex(29)
    var hostEthSecondaryIp: String = "",

    @field:BHFieldParseIndex(30)
    var hostSecondaryEthPort: String = "",

    @field:BHFieldParseIndex(31)
    @field:BHFieldName("Connection Timeout")
    var connectTimeOut: String = "",

    @field:BHFieldParseIndex(32)
    @field:BHFieldName("Response Timeout")
    var responseTimeOut: String = "",

    @field:BHFieldParseIndex(33)
    var reserveValue: String = "",

    @field:BHFieldParseIndex(34)
    //   @field:BHFieldName("APN 2")
    var apn2: String = "",

    @field:BHFieldParseIndex(35)
    //  @field:BHFieldName("GPRS User 2")
    var gprsUser2: String = "",

    @field:BHFieldParseIndex(36)
    //   @field:BHFieldName("GPRS Password 2")
    var gprsPassword2: String = "",

    @field:BHFieldParseIndex(37)
    //   @field:BHFieldName("Host Primary IP 2")
    var hostPrimaryIp2: String = "",

    @field:BHFieldParseIndex(38)
    //   @field:BHFieldName("Host Primary Port 2")
    var hostPrimaryPort2: String = "",

    @field:BHFieldParseIndex(39)
    //   @field:BHFieldName("Host Secondary IP 2")
    var hostSecondaryIp2: String = "",

    @field:BHFieldParseIndex(40)
    //   @field:BHFieldName("Host Secondary Port 2")
    var hostSecondaryPort2: String = "",

    @field:BHFieldParseIndex(41)
    //   @field:BHFieldName("Bank Code")
    var bankCode: String = "",

    @field:BHFieldParseIndex(42)
    // @field:BHFieldName("TID")
    var tid: String = "",


)

@Entity
data class IssuerParameterTable(
    @field:BHFieldParseIndex(0)
    var pcNo: String = "",

    @field:BHFieldParseIndex(1)
    var actionId: String = "",

    @field:BHFieldParseIndex(2)
    var tableId: String = "",

    @field:BHFieldParseIndex(3)
    var isActive: String = "",

    @PrimaryKey
    @field:BHFieldParseIndex(4)
    var issuerId: String = "",

    @field:BHFieldParseIndex(5)
    var issuerTypeId: String = "",

    @field:BHFieldParseIndex(6)
    var issuerName: String = "",

    @field:BHFieldParseIndex(7)
    var otpSize: String = "",

    @field:BHFieldParseIndex(8)
    var tokenSize: String = "",

    @field:BHFieldParseIndex(9)
    var saleAllowed: String = "",

    @field:BHFieldParseIndex(10)
    var voidSaleAllowed: String = "",

    @field:BHFieldParseIndex(11)
    var cashReloadAllowed: String = "",

    @field:BHFieldParseIndex(12)
    var voidCashReloadAllowed: String = "",

    @field:BHFieldParseIndex(13)
    var creditReloadAllowed: String = "",

    @field:BHFieldParseIndex(14)
    var voidCreditReloadAllowed: String = "",

    @field:BHFieldParseIndex(15)
    var balanceEnquiry: String = "",

    @field:BHFieldParseIndex(16)
    var walletIssuerDisclaimerLength: String = "",

    @field:BHFieldParseIndex(17)
    var walletIssuerDisclaimer: String = "",

    @field:BHFieldParseIndex(18)
    var walletIssuerMasterKey: String = "",

    @field:BHFieldParseIndex(19)
    var customerIdentifierFiledType: String = "",

    @field:BHFieldParseIndex(20)
    var customerIdentifierFieldSize: String = "",

    @field:BHFieldParseIndex(21)
    var customerIdentifierFieldName: String = "",

    @field:BHFieldParseIndex(22)
    var identifierMasking: String = "",

    @field:BHFieldParseIndex(23)
    var transactionAmountLimit: String = "",

    @field:BHFieldParseIndex(24)
    var pushBillAllowed: String = "",

    @field:BHFieldParseIndex(25)
    var reEnteredCustomerId: String = "",

    @field:BHFieldParseIndex(26)
    var reservedForFutureUsed: String = ""
)

@Entity
data class TerminalParameterTable(
    @field:BHFieldParseIndex(0)
    var pcNO: String = "",

    @field:BHFieldParseIndex(1)
    var actionId: String = "",

    @field:BHFieldParseIndex(2)
    var tableId: String = "",

    @field:BHFieldParseIndex(3)
    var isActive: String = "",

    @PrimaryKey
    @field:BHFieldParseIndex(4)
    @field: BHFieldName("TID")
    var terminalId: String = "",

    @field:BHFieldParseIndex(5)
    @field:BHFieldName("MID")
    var merchantId: String = "",

    @field:BHFieldParseIndex(6)
    @field:BHFieldName("Batch Number")
    var batchNumber: String = "",

    @field:BHFieldParseIndex(7)
    @field:BHFieldName("Invoice Number")
    var invoiceNumber: String = "",

    @field:BHFieldParseIndex(8)
    var receiptHeaderOne: String = "",

    @field:BHFieldParseIndex(9)
    var receiptHeaderTwo: String = "",

    @field:BHFieldParseIndex(10)
    var receiptHeaderThree: String = "",

    @field:BHFieldParseIndex(11)
    @field:BHFieldName("Print Receipt")
    var printReceipt: String = "",

    @field:BHFieldParseIndex(12)
    var adminPassword: String = "",

    @field:BHFieldName("Manager Password")
    var managerPassword: String = "",
    /*  get() {
          return if (adminPassword.length == 8) adminPassword.substring(0, 4) else adminPassword
      }*/

    @field:BHFieldParseIndex(13)
    var trainingMode: String = "",

    @field:BHFieldParseIndex(14)
    var canceledTransactionReceiptPrint: String = "",

    @field:BHFieldParseIndex(15)
    var superAdminPassword: String = "",

    @field:BHFieldParseIndex(16)
    var terminalDateTime: String = "",

    @field:BHFieldParseIndex(17)
    @field:BHFieldName("Currency Symbol")
    var currencySymbol: String = "",

    @field:BHFieldParseIndex(18)
    @field:BHFieldName("Tip Processing")
    @field:BHDashboardItem(EDashboardItem.SALE_TIP)
    var tipProcessing: String = "",

    @field:BHFieldParseIndex(19)
    @field:BHFieldName("Tip Percent")
    var tipPercent: String = "",

    @field:BHFieldParseIndex(20)
    @field:BHFieldName("Max Tip Percent")
    var maxTipPercent: String = "",

    @field:BHFieldParseIndex(21)
    @field:BHFieldName("Max Tip Limit")
    var maxTipLimit: String = "",

    @field:BHFieldParseIndex(22)
    @field:BHFieldName("Surcharge")
    var surcharge: String = "",

    @field:BHFieldParseIndex(23)
    @field:BHFieldName("Surcharge Type")
    var surchargeType: String = "",

    @field:BHFieldParseIndex(24)
    @field:BHFieldName("Surcharge Value")
    var surChargeValue: String = "",

    @field:BHFieldParseIndex(25)
    var maxSurchargeValue: String = "",

    @field:BHFieldParseIndex(26)
    @field:BHFieldName("Force Settle")
    var forceSettle: String = "",

    @field:BHFieldParseIndex(27)
    @field:BHFieldName("Force Settle Time")
    var forceSettleTime: String = "",

    @field:BHFieldParseIndex(28)
    @field:BHFieldName("Sale With Cash")
    @field:BHDashboardItem(EDashboardItem.SALE_WITH_CASH)
    var saleWithCash: String = "",

    @field:BHFieldParseIndex(29)
    @field:BHFieldName("Cash Advance")
    @field:BHDashboardItem(EDashboardItem.CASH_ADVANCE)
    var cashAdvance: String = "",

    @field:BHFieldParseIndex(30)
    @field:BHFieldName("Cash Advance Limit")
    var cashAdvanceMaxAmountLimit: String = "",

    //allowed or not masking 0 -> default masking, 1-> masking based on maskformate
    @field:BHFieldParseIndex(32)
    @field:BHFieldName("Pan Mask")
    var panMask: String = "",

    @field:BHFieldParseIndex(33)
    @field:BHFieldName("Pan Mash Format")
    var panMaskFormate: String = "",

    //on which coppy allowed masking 0->none,1->customer coppy, 2->merchant coppy,3->both
    @field:BHFieldParseIndex(34)
    var panMaskConfig: String = "",


    @field:BHDashboardItem(EDashboardItem.SALE)
    @field:BHFieldParseIndex(35)
    @field:BHFieldName("Sale")
    var sale: String = "",

    @field:BHDashboardItem(EDashboardItem.VOID_SALE)
    @field:BHFieldParseIndex(36)
    @field:BHFieldName("Void")
    var voidSale: String = "",

    @field:BHDashboardItem(EDashboardItem.REFUND)
    @field:BHFieldParseIndex(37)
    @field:BHFieldName("Refund")
    var refund: String = "",

    @field:BHDashboardItem(EDashboardItem.VOID_REFUND)
    @field:BHFieldParseIndex(38)
    @field:BHFieldName("Void Refund")
    var voidRefund: String = "",

    @field:BHDashboardItem(
        EDashboardItem.PREAUTH,
        EDashboardItem.PREAUTH_COMPLETE
    )
    @field:BHFieldParseIndex(39)
    @field:BHFieldName("Pre Auth")
    var preAuth: String = "",

    @field:BHFieldParseIndex(31)
    var maxAmtEntryDigits: String = "",

    @field:BHDashboardItem(
        EDashboardItem.BANK_EMI,
        EDashboardItem.EMI_ENQUIRY
    )
    @field:BHFieldParseIndex(40)
    @field:BHFieldName("Bank Emi")
    var bankEmi: String = "",

    @field:BHDashboardItem(EDashboardItem.BRAND_EMI)
    @field:BHFieldParseIndex(41)
    @field:BHFieldName("Brand Emi")
    var brandEmi: String = "",

    @field:BHFieldParseIndex(42)
    @field:BHFieldName("Brand Emi By Access Code")
    var emiPro: String = "",

    @field:BHFieldParseIndex(43)
    var walletTranslation: String = "",

    @field:BHFieldParseIndex(44)
    var qrTransaction: String = "",

    @field:BHFieldParseIndex(45)
    @field: BHFieldName("Manual Entry")
    var fManEntry: String = "",

    @field:BHDashboardItem(
        EDashboardItem.OFFLINE_SALE
    )
    @field:BHFieldParseIndex(46)
    @field:BHFieldName("Offline Sale")
    var fManOfflineSale: String = "",

    @field:BHFieldParseIndex(47)
    var reservedValues: String = "",

    @field:BHFieldName("roc")
    var stan: String = "",

    @field:BHDashboardItem(EDashboardItem.VOID_PREAUTH)
    @field:BHFieldParseIndex(48)
    @field:BHFieldName("Void Preauth")
    var fVoidPreauth: String = "",

    @field:BHDashboardItem(EDashboardItem.VOID_OFFLINE_SALE)
    @field:BHFieldParseIndex(49)
    @field:BHFieldName("Void Offline Sale")
    var fVoidOfflineSale: String = "",

    @field:BHDashboardItem(EDashboardItem.PENDING_PREAUTH)
    @field:BHFieldParseIndex(50)
    @field:BHFieldName("Pending Preauth")
    var fPendingPreauthTrans: String = "",

    @field:BHFieldParseIndex(51)
    var maxCtlsTransAmt: String = "",

    @field:BHFieldParseIndex(52)
    var minCtlsTransAmt: String = "",

    @field:BHFieldParseIndex(53)
    @field:BHFieldName("Offline Sale Min PAN")
    var minOfflineSalePanLen: String = "",

    @field:BHFieldParseIndex(54)
    @field:BHFieldName("Offline Sale Max PAN")
    var maxOfflineSalePanLen: String = "",

    @field:BHFieldParseIndex(55)
    var tlsFlag: String = "",

    @field:BHFieldParseIndex(56)
    @field:BHFieldName("Printing Impact")
    var printingImpact: String = "",

    @field:BHFieldParseIndex(57)
    var posHealthStatics: String = "",

    @field:BHFieldParseIndex(58)
    var fPushEndPointDetail: String = "",

    @field:BHFieldParseIndex(59)
    var fPushTimeStamp: String = "",

    //region=========New Fields for HDFC===========
    @field:BHFieldParseIndex(60)
    @field:BHFieldName("Tid Type")
    var tidType: String = "",  // if type is 1 main else child tid


    @field:BHFieldParseIndex(61)
    @field:BHFieldName("Tid Index")
    var tidIndex: String = "",   // sorting order of child tid

    @field:BHFieldParseIndex(62)
    @field:BHFieldName("Tid Bank Code")
    var tidBankCode: String = "",  // relation with bank

    @field:BHFieldParseIndex(63)
    @field:BHFieldName("Tid Name")
    var tidName: String = "",  // name of bank

    @field:BHFieldParseIndex(64)
    @field:BHFieldName("LinkTidType")
    var LinkTidType: String = "",  // LinkTidType : for Amex  - 0,DC type - 1 ,offus Tid - 2, 3 months onus - 3,6 months onus - 6, 9 months onus - 9,12 months onus- 12

    @field:BHFieldParseIndex(67)
    @field:BHFieldName("STAN")
    var roc: String = "",

    @field:BHFieldParseIndex(68)
    var ctlsCaption: String = "",

    @field:BHFieldParseIndex(69)
     var flexiPayMinAmountLimit: String = "",

    @field:BHFieldParseIndex(70)
    var flexiPayMaxAmountLimit: String = "",

    @field:BHDashboardItem(EDashboardItem.EMI_ENQUIRY)
    var bankEnquiry: String = "",

    var clearFBatch: String = "0",//This field is for Server Hit Status,

    var bankEnquiryMobNumberEntry: Boolean = false,
    @field:BHDashboardItem(EDashboardItem.DIGI_POS)
    var isDigiposActive: String = ""


){
    var digiPosResponseType: String = ""
    var digiPosStatus: String = ""
    var digiPosStatusMessage: String = ""
    var digiPosStatusCode: String = ""
    var digiPosTerminalStatus: String = ""
    var digiPosBQRStatus: String = ""
    var digiPosUPIStatus: String = ""
    var digiPosSMSpayStatus: String = ""
    var digiPosStaticQrDownloadRequired: String = ""
    var digiPosCardCallBackRequired: String = ""
}

@Entity
data class CardDataTable(
    @field:BHFieldParseIndex(0)
    var pcNo: String = "",

    @field:BHFieldParseIndex(9)
    var maxPanDigits: String = "",

    @field:BHFieldParseIndex(1)
    var actionId: String = "",

    @field:BHFieldParseIndex(2)
    var tableId: String = "",

    @field:BHFieldParseIndex(3)
    var isActive: String = "",

    @field:BHFieldParseIndex(4)
    @PrimaryKey
    var cardTableIndex: String = "",

    @field:BHFieldParseIndex(5)
    var cardType: String = "",

    @field:BHFieldParseIndex(6)
    var cardAbbrev: String = "",

    @field:BHFieldParseIndex(7)
    var cardLabel: String = "",

    @field:BHFieldParseIndex(8)
    var minPanDigits: String = "",

    @field:BHFieldParseIndex(10)
    var floorLimit: String = "0",

    @field:BHFieldParseIndex(11)
    var panLow: String = "",

    @field:BHFieldParseIndex(12)
    var panHi: String = "",

    @field:BHFieldParseIndex(13)
    var manualEntry: String = "",

    @field:BHFieldParseIndex(14)
    var singleLine: String = "",


    var tipAdjustAllowed: String = "",

    @field:BHFieldParseIndex(16)
    var preAuthAllowed: String = "",

    @field:BHFieldParseIndex(17)
    var saleWithCashAllowed: String = "",

    @field:BHFieldParseIndex(18)
    var cashOnlyAllowed: String = "",

    @field:BHFieldParseIndex(19)
    var cashAdvanceAllowed: String = "",

    @field:BHFieldParseIndex(20)
    var saleAllowed: String = "",

    @field:BHFieldParseIndex(21)
    var voidSaleAllowed: String = "",

    @field:BHFieldParseIndex(22)
    var refundAllowed: String = "",

    @field:BHFieldParseIndex(23)
    var voidRefundAllowed: String = "",

    @field:BHFieldParseIndex(24)
    var manOffSaleAllowed: String = "",

    @field:BHFieldParseIndex(25)
    var reservedValued: String = "",

    @field:BHFieldParseIndex(26)
    var bankCode: String = "",

    @field:BHFieldParseIndex(27)
    var tid: String = "",

    @field:BHFieldParseIndex(28)
    var bankIssuerId: String = ""
)

@Entity
data class HDFCTpt(
    @field:BHFieldParseIndex(0)
    var pcNo: String = "",

    @field:BHFieldParseIndex(1)
    var actionId: String = "",

    @field:BHFieldParseIndex(2)
    @PrimaryKey
    var tableId: String = "",

    @field:BHFieldParseIndex(3)
    var isActive: String = "",

    @field:BHFieldParseIndex(4)
    var recordId: String = "",

    @field:BHFieldParseIndex(5)
    var bankId: String = "",

    @field:BHFieldParseIndex(6)
    var bankTid: String = "",

    @field:BHFieldParseIndex(7)
    var dateTime: String = "",

    @field:BHFieldParseIndex(8)
    var adminPassword: String = "",

    @field:BHFieldParseIndex(9)  // bit oriented for
    var option1: String = "",

    @field:BHFieldParseIndex(10)
    var option2: String = "",

    @field:BHFieldParseIndex(11)
    var receiptL2: String = "",

    @field:BHFieldParseIndex(12)
    var receiptL3: String = "",

    @field:BHFieldParseIndex(13)
    var defaultMerchantName: String = "",

    @field:BHFieldParseIndex(14)
    var localTerminalOption: String = "",

    @field:BHFieldParseIndex(15)
    var helpDeskNumber: String = "",

    @field:BHFieldParseIndex(16)
    var transAmountDigit: String = "",

    @field:BHFieldParseIndex(17)
    var settleAmtDigit: String = "",

    @field:BHFieldParseIndex(18)
    var option3: String = "",

    @field:BHFieldParseIndex(19)
    var option4: String = ""
)

@Entity
data class HDFCCdt(
    @field:BHFieldParseIndex(0)
    var pcNo: String = "",

    @field:BHFieldParseIndex(1)
    var actionId: String = "",

    @field:BHFieldParseIndex(2)
    var tableId: String = "",

    @field:BHFieldParseIndex(3)
    var isActive: String = "",

    @field:BHFieldParseIndex(4)
    @PrimaryKey
    var recordId: String = "",

    @field:BHFieldParseIndex(5)
    var bankId: String = "",

    @field:BHFieldParseIndex(6)
    var bankTid: String = "",

    @field:BHFieldParseIndex(7)
    var cardRangeNumber: String = "",

    @field:BHFieldParseIndex(8)
    var panRangeLow: String = "",

    @field:BHFieldParseIndex(9)
    var panRangeHigh: String = "",

    @field:BHFieldParseIndex(10)
    var issuerNumber: String = "",

    @field:BHFieldParseIndex(11)
    var maxPanDigit: String = "",

    @field:BHFieldParseIndex(12)
    var minPanDigit: String = "",

    @field:BHFieldParseIndex(13)
    var floorLimit: String = "",

    @field:BHFieldParseIndex(14)
    var reauthMarginPercent: String = "",

    @field:BHFieldParseIndex(15)
    var defaultAccount: String = "",

    @field:BHFieldParseIndex(16)  // Bit oriented
    var option1: String = "",

    @field:BHFieldParseIndex(17)
    var option2: String = "",

    @field:BHFieldParseIndex(18)
    var option3: String = "",

    @field:BHFieldParseIndex(19)
    var cardName: String = "",

    @field:BHFieldParseIndex(20)
    var cardLabel: String = "",

    @field:BHFieldParseIndex(21)
    var option4: String = "",

    @field:BHFieldParseIndex(22)
    var issuerIndex: String = "",

    @field:BHFieldParseIndex(23)
    var issuerName: String = ""
)

//table for WifiCommunication Table 203
@Entity
data class WifiCommunicationTable(

    @field:BHFieldParseIndex(0)
    var parameterControlId: String = "",

    @field:BHFieldParseIndex(1)
    var actionId: String = "",

    @PrimaryKey
    @field:BHFieldParseIndex(2)
    var tableId: String = "",

    @field:BHFieldParseIndex(3)
    var isActive: String = "",

    @field:BHFieldParseIndex(4)
    var recordId: String = "",

    @field:BHFieldParseIndex(5)
    var gprPrimaryHostIP : String = "",

    @field:BHFieldParseIndex(6)
    var gprPrimaryHostPort : String = "",

    @field:BHFieldParseIndex(7)
    var gprAPN : String = "",

    @field:BHFieldParseIndex(8)
    var gprAPNUserName: String = "",

    @field:BHFieldParseIndex(9)
    var gprAPNPassword : String = "",

    @field:BHFieldParseIndex(10)
    var gprsTimeOut: String = "",

    @field:BHFieldParseIndex(11)
    var wifiHostIP : String = "",

    @field:BHFieldParseIndex(12)
    var wifiHostPort : String = "",

    @field:BHFieldParseIndex(13)
    var wifiTimeOut : String = "",

    @field:BHFieldParseIndex(14)
    var reserveValue : String = "",


    )

//region===========================================BrandEMIMasterCategory Table:-
@Entity
data class BrandEMIMasterCategoryTable(
    @PrimaryKey
    var brandTimeStamp: String = "",
    var brandCategoryUpdatedTimeStamp: String? = null,
    var issuerTAndCTimeStamp: String? = null,
    var brandTAndCTimeStamp: String? = null
)
//region

// region===========================================BrandEMIMasterSubCategory Table:-
@Entity
data class BrandEMIMasterSubCategoryTable(
    var brandID: String? = null,
    @PrimaryKey
    var categoryID: String = "",
    var parentCategoryID: String? = null,
    var categoryName: String? = null
)
//region

// region===========================================IssuerTAndC Table:-
@Entity
data class IssuerTAndCTable(
    @PrimaryKey
    var issuerId: String = "",
    var headerTAndC: String? = null,
    var footerTAndC: String? = null
)
//region

// region===========================================BrandTAndC Table:-
@Entity
data class BrandTAndCTable(
    @PrimaryKey
    var brandId: String = "",
    var brandTAndC: String? = null
)
//region

// region===============Brand EMI Master Category TimeStamps Table:-
@Entity
data class BrandEMIMasterTimeStamps(
    @PrimaryKey
    var brandTimeStamp: String = "",
    var brandCategoryUpdatedTimeStamp: String = "",
    var issuerTAndCTimeStamp: String = "",
    var brandTAndCTimeStamp: String = ""
)
//endregion

// region===============Brand EMI Sub-Category Data Table:-
@Entity
data class BrandEMISubCategoryTable(
    @PrimaryKey
    var categoryID: String = "",
    var brandID: String = "",
    var parentCategoryID: String = "",
    var categoryName: String = ""
):Serializable
//endregion

//region===============Brand EMI Data Table:-
@Entity
data class BrandEMIDataTable(    // @PrimaryKey
    var hostInvoice: String = "",
    var brandID: String = "",
    var brandName: String = "",
    var brandReservedValues: String = "",
    var categoryID: String = "",
    var categoryName: String = "",
    var productID: String = "",
    var productName: String = "",
    var childSubCategoryID: String = "",
    var childSubCategoryName: String = "",
    var validationTypeName: String = "",
    var isRequired: String = "",
    var inputDataType: String = "",
    var imeiNumber: String = "",
    var serialNumber: String = "",
    var emiType: String = "",
    var producatDesc: String = "",
    var hostTid: String = ""
)
//endregion

//region================Brand EMI By Access Code Table:-
@Entity
data class BrandEMIAccessDataModalTable(
    var hostInvoice: String = "",
    var hostTid: String = "",
    var emiCode: String = "",
    var bankID: String = "",
    var bankTID: String = "",
    var issuerID: String = "",
    var tenure: String = "",
    var brandID: String = "",
    var productID: String = "",
    var emiSchemeID: String = "",
    var transactionAmount: String = "",
    var discountAmount: String = "",
    var loanAmount: String = "",
    var interestAmount: String = "",
    var emiAmount: String = "",
    var cashBackAmount: String = "",
    var netPayAmount: String = "",
    var processingFee: String = "",
    var processingFeeRate: String = "",
    var totalProcessingFee: String = "",
    var brandName: String = "",
    var issuerName: String = "",
    var productName: String = "",
    var productCode: String = "",
    var productModal: String = "",
    var productCategoryName: String = "",
    var productSerialCode: String = "",
    var skuCode: String = "",
    var totalInterest: String = "",
    var schemeTAndC: String = "",
    var schemeTenureTAndC: String = "",
    var schemeDBDTAndC: String = "",
    var discountCalculatedValue: String = "",
    var cashBackCalculatedValue: String = "",
    var orignalTxnAmt: String = "",
    var mobileNo: String = "",
    var brandReservField: String = "",
    var productBaseCat: String = "",
    var issuerTimeStamp: String = "",
    var brandTimeStamp: String = ""
)
//endregion

// region ====== OnpaymentListner response from ingenico data table ======
@Entity
data class BatchTable(var receiptData:ReceiptDetail?=null){
    var invoice: String=""

    @PrimaryKey(autoGenerate = false)  // we make primary key for multiple invoice in void
    var bonushubInvoice: String = ""

    var bonushubStan: String = ""
    var bonushubbatchnumber: String = ""
    var transactionType: Int = 0
    var imeiOrSerialNum: String?=null
    var billNumber: String?=null
    var emiBrandData: BrandEMIMasterDataModal?=null
    var emiSubCategoryData: BrandEMISubCategoryTable?=null
    var emiCategoryData: BrandEMISubCategoryTable?=null
    var emiProductData: BrandEMIProductDataModal?=null
    var emiTenureDataModel: BankEMITenureDataModal?=null
    var emiIssuerDataModel: BankEMIIssuerTAndCDataModal?=null
    var mobileNumber: String?=null
}
// endregion

@Entity
data class IngenicoInitialization(
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null,

    var responseCode: String? = null,

    var apiresponseCode: String? = null,

    @TypeConverters(Converters::class)
    var tidList: List<String>? = null,

    @TypeConverters(Converters::class)
    var tidStatusList: List<String>? = null,

   @TypeConverters(Converters::class)
    var initdataList: List<InitDataListList>? = null
)


data class InitDataListList(
    var adminPassword: String? = null,
    var helpDeskNumber: String? = null,
    var merAddHeader1: String? = null,
    var merAddHeader2: String? = null,
    var merchantName: String? = null,
    var isRefundPasswordEnable: Boolean? = null,
    var isReportPasswordEnable: Boolean? = null,
    var isVoidPasswordEnable: Boolean? = null,
    var isTipEnable: Boolean? = null,
    ) // same

@Entity
data class IngenicoSettlementResponse(
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null,
    var status: String? = null,
    var responseCode: String? = null,
    var batchNumber: String? = null,
    var appVersion: String? = null,
    var releaseDate: String? = null,

    @TypeConverters(Converters::class)
    var tidList: List<String>? = null,

    @TypeConverters(Converters::class)
    var tidStatusList: List<String>? = null,

    @TypeConverters(Converters::class)
    var tids: List<String>? = null,

    )

// region ======
@Entity
data class BatchTableReversal(var receiptData:ReceiptDetail?=null){
    @PrimaryKey(autoGenerate = false)
    var roc: String=""
    var invoice: String=""
    var transactionType: Int = 0
    var imeiOrSerialNum: String?=null
    var billNumber: String?=null
    var emiBrandData: BrandEMIMasterDataModal?=null
    var emiCategoryData: BrandEMISubCategoryTable?=null
    var emiProductData: BrandEMIProductDataModal?=null
    var responseCode: String?=""

    var bonushubInvoice: String = ""  // add host invoice
}
// endregion

// region ======
@Entity
data class PendingSyncTransactionTable(
    @PrimaryKey(autoGenerate = false)
    var invoice: String="",
    var batchTable: BatchTable?=null,
    var responseCode: String?="",
    var cardProcessedDataModal:CardProcessedDataModal
    )
// end region

//region == digipos
@Entity
data class DigiPosDataTable(
    // Digi POS Data
    var requestType: Int = 0,
    var amount: String = "",
    var description : String= "",
    var vpa: String = "",
    var mTxnId : String= "",

    @PrimaryKey(autoGenerate = false)
   var partnerTxnId : String= "",
    var status : String= "",
    var statusMsg : String= "",
    var statusCode : String= "",
    var customerMobileNumber : String= "",
    var transactionTimeStamp : String= "",
    var txnStatus : String= EDigiPosPaymentStatus.Pending.desciption,
    var paymentMode : String= "",
    var pgwTxnId : String= "",
    var txnDate : String= "",
    var txnTime : String= "",
    var displayFormatedDate : String= ""
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString()
    ) {
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(p0: Parcel?, p1: Int) {
        p0?.writeInt(requestType)
        p0?.writeString(amount)
        p0?.writeString(description)
        p0?.writeString(vpa)
        p0?.writeString(mTxnId)
        p0?.writeString(partnerTxnId)
        p0?.writeString(status)
        p0?.writeString(statusMsg)
        p0?.writeString(statusCode)
        p0?.writeString(customerMobileNumber)
        p0?.writeString(transactionTimeStamp)
        p0?.writeString(txnStatus)
        p0?.writeString(paymentMode)
        p0?.writeString(pgwTxnId)
        p0?.writeString(txnDate)
        p0?.writeString(txnTime)
        p0?.writeString(displayFormatedDate)
    }

    companion object CREATOR : Parcelable.Creator<DigiPosDataTable> {
        override fun createFromParcel(parcel: Parcel): DigiPosDataTable {
            return DigiPosDataTable(parcel)
        }

        override fun newArray(size: Int): Array<DigiPosDataTable?> {
            return arrayOfNulls(size)
        }
    }
}

//end region