package com.bonushub.crdb.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bonushub.crdb.di.scope.BHDashboardItem
import com.bonushub.crdb.di.scope.BHFieldParseIndex
import com.bonushub.pax.utils.EDashboardItem
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
    var transactionalAmount: String = "",
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
    @PrimaryKey
    var tableId: String = "",

    @field:BHFieldParseIndex(3)
    var isActive: String = "",

    @field:BHFieldParseIndex(4)
    var recordId: String = "",

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
    var apn: String = "",

    @field:BHFieldParseIndex(15)
    var apnUserName: String = "",

    @field:BHFieldParseIndex(16)
    var apnPassword: String = "",

    @field:BHFieldParseIndex(17)
    var hostPrimaryIp: String = "",

    @field:BHFieldParseIndex(18)
    var hostPrimaryPortNo: String = "",

    @field:BHFieldParseIndex(19)
    var hostSecIp: String = "",

    @field:BHFieldParseIndex(20)
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
    var connectTimeOut: String = "",

    @field:BHFieldParseIndex(32)
    var responseTimeOut: String = "",

    @field:BHFieldParseIndex(33)
    var reserveValue: String = "",

    @field:BHFieldParseIndex(34)
    var apn2: String = "",

    @field:BHFieldParseIndex(35)
    var gprsUser2: String = "",

    @field:BHFieldParseIndex(36)
    var gprsPassword2: String = "",

    @field:BHFieldParseIndex(37)
    var hostPrimaryIp2: String = "",

    @field:BHFieldParseIndex(38)
    var hostPrimaryPort2: String = "",

    @field:BHFieldParseIndex(39)
    var hostSecondaryIp2: String = "",

    @field:BHFieldParseIndex(40)
    var hostSecondaryPort2: String = "",

    @field:BHFieldParseIndex(41)
    var bankCode: String = "",

    @field:BHFieldParseIndex(42)
    var tid: String = ""

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
    @PrimaryKey
    var tableId: String = "",

    @field:BHFieldParseIndex(3)
    var isActive: String = "",

    @field:BHFieldParseIndex(4)
    var terminalId: String = "",

    @field:BHFieldParseIndex(5)
    var merchantId: String = "",

    @field:BHFieldParseIndex(6)
    var batchNumber: String = "",

    @field:BHFieldParseIndex(7)
    var invoiceNumber: String = "",

    @field:BHFieldParseIndex(8)
    var receiptHeaderOne: String = "",

    @field:BHFieldParseIndex(9)
    var receiptHeaderTwo: String = "",

    @field:BHFieldParseIndex(10)
    var receiptHeaderThree: String = "",

    @field:BHFieldParseIndex(11)
    var printReceipt: String = "",

    @field:BHFieldParseIndex(12)
    var adminPassword: String = "",

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
    var currencySymbol: String = "",

    @field:BHFieldParseIndex(18)
     @field:BHDashboardItem(EDashboardItem.SALE_TIP)
    var tipProcessing: String = "",

    @field:BHFieldParseIndex(19)
    var tipPercent: String = "",

    @field:BHFieldParseIndex(20)
    var maxTipPercent: String = "",

    @field:BHFieldParseIndex(21)
    var maxTipLimit: String = "",

    @field:BHFieldParseIndex(22)
    var surcharge: String = "",

    @field:BHFieldParseIndex(23)
    var surchargeType: String = "",

    @field:BHFieldParseIndex(24)
    var surChargeValue: String = "",

    @field:BHFieldParseIndex(25)
    var maxSurchargeValue: String = "",

    @field:BHFieldParseIndex(26)
    var forceSettle: String = "",

    @field:BHFieldParseIndex(27)
    var forceSettleTime: String = "",

    @field:BHFieldParseIndex(28)
    @field:BHDashboardItem(EDashboardItem.SALE_WITH_CASH)
    var saleWithCash: String = "",

    @field:BHFieldParseIndex(29)
    @field:BHDashboardItem(EDashboardItem.CASH_ADVANCE)
    var cashAdvance: String = "",

    @field:BHFieldParseIndex(30)
    var cashAdvanceMaxAmountLimit: String = "",

    //allowed or not masking 0 -> default masking, 1-> masking based on maskformate
    @field:BHFieldParseIndex(32)
    var panMask: String = "",

    @field:BHFieldParseIndex(33)
    var panMaskFormate: String = "",

    //on which coppy allowed masking 0->none,1->customer coppy, 2->merchant coppy,3->both
    @field:BHFieldParseIndex(34)
    var panMaskConfig: String = "",


    //  @field:BHDashboardItem(EDashboardItem.SALE)
    @field:BHFieldParseIndex(35)
    var sale: String = "",

    @field:BHDashboardItem(EDashboardItem.VOID_SALE)
    @field:BHFieldParseIndex(36)
    var voidSale: String = "",

    @field:BHDashboardItem(EDashboardItem.REFUND)
    @field:BHFieldParseIndex(37)
    var refund: String = "",

     @field:BHDashboardItem(EDashboardItem.VOID_REFUND)
    @field:BHFieldParseIndex(38)
    var voidRefund: String = "",

      @field:BHDashboardItem(
          EDashboardItem.PREAUTH,
          EDashboardItem.PREAUTH_COMPLETE
      )
    @field:BHFieldParseIndex(39)
    var preAuth: String = "",

    @field:BHFieldParseIndex(31)
    var maxAmtEntryDigits: String = "",

     @field:BHDashboardItem(
         EDashboardItem.BANK_EMI,
         EDashboardItem.EMI_ENQUIRY
     )
    @field:BHFieldParseIndex(40)
    var bankEmi: String = "",

     @field:BHDashboardItem(EDashboardItem.BRAND_EMI)
    @field:BHFieldParseIndex(41)
    var brandEmi: String = "",

    @field:BHFieldParseIndex(42)
    var emiPro: String = "",

    @field:BHFieldParseIndex(43)
    var walletTranslation: String = "",

    @field:BHFieldParseIndex(44)
    var qrTransaction: String = "",

    @field:BHFieldParseIndex(45)
    var fManEntry: String = "",

       @field:BHDashboardItem(
           EDashboardItem.OFFLINE_SALE
       )
    @field:BHFieldParseIndex(46)
    var fManOfflineSale: String = "",

    @field:BHFieldParseIndex(47)
    var reservedValues: String = "",

    var stan: String = "",

     @field:BHDashboardItem(EDashboardItem.VOID_PREAUTH)
    @field:BHFieldParseIndex(48)
    var fVoidPreauth: String = "",

       @field:BHDashboardItem(EDashboardItem.VOID_OFFLINE_SALE)
    @field:BHFieldParseIndex(49)
    var fVoidOfflineSale: String = "",

    @field:BHDashboardItem(EDashboardItem.PENDING_PREAUTH)
    @field:BHFieldParseIndex(50)
    var fPendingPreauthTrans: String = "",

    @field:BHFieldParseIndex(51)
    var maxCtlsTransAmt: String = "",

    @field:BHFieldParseIndex(52)
    var minCtlsTransAmt: String = "",

    @field:BHFieldParseIndex(53)
    var minOfflineSalePanLen: String = "",

    @field:BHFieldParseIndex(54)
    var maxOfflineSalePanLen: String = "",

    @field:BHFieldParseIndex(55)
    var tlsFlag: String = "",

    @field:BHFieldParseIndex(56)
    var printingImpact: String = "",

    @field:BHFieldParseIndex(57)
    var posHealthStatics: String = "",

    @field:BHFieldParseIndex(58)
    var fPushEndPointDetail: String = "",

    @field:BHFieldParseIndex(59)
    var fPushTimeStamp: String = "",

    //region=========New Fields for HDFC===========
    @field:BHFieldParseIndex(60)
    var tidType: String = "",  // if type is 1 main else child tid

    @field:BHFieldParseIndex(61)
    var tidIndex: String = "",   // sorting order of child tid

    @field:BHFieldParseIndex(62)
    var tidBankCode: String = "",  // relation with bank

    @field:BHFieldParseIndex(63)
    var tidName: String = "",  // name of bank

    var clearFBatch: String = "0" //This field is for Server Hit Status
)

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

    @field:BHFieldParseIndex(15)
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