package com.bonushub.pax.utils

import androidx.fragment.app.Fragment
import com.bonushub.crdb.R
import java.io.Serializable


//region=================Preference Keys==============
enum class PreferenceKeyConstant(var keyName: String) {
    PC_NUMBER_ONE("pc_number_one"),
    PC_NUMBER_TWO("pc_number_two"),
    INSERT_PPK_DPK("insert_ppk_dpk"),
    TMK_DOWNLOAD("tmk_download"),
    CRDB_ISSUER_ID_KEY("crdb_issuer_id_key"),
    ACC_SEL_KEY("accl_sel_key"),
    LAST_BATCH("last_batch"),
    LAST_SUCCESS_RECEIPT_KEY("last_success_receipt_key"),
    SETTLEMENT_FAILED("settlement failed"),
    IsAutoSettleDone("isAutoSettleDone"),
    LAST_SAVED_AUTO_SETTLE_DATE("lastSavedAutoSettleDate"),
    SERVER_HIT_STATUS("server_hit_status"),
}
//endregion

//region==================Account Type=================
enum class EAccountType(val code: String) {
    DEFAULT("00"), SAVING("10"), CREDIT("30"), CHEQUE("20"), UNIVERSAL(
        "40"
    )
}
//endregion

//region==================Transaction Type================
enum class BhTransactionType(
    val type: Int,
    val processingCode: ProcessingCode = ProcessingCode.NONE,
    val txnTitle: String = "Not Defined"
) {
    NONE(0),
    KEY_EXCHANGE(1, ProcessingCode.KEY_EXCHANGE),
    INIT(2, ProcessingCode.INIT),
    LOGON(3, ProcessingCode.KEY_EXCHANGE),
    SALE(4, ProcessingCode.SALE, "SALE"),
    VOID(5, ProcessingCode.VOID, "VOID"),
    SETTLEMENT(6, ProcessingCode.SETTLEMENT),
    APP_UPDATE(7),
    BALANCE_ENQUIRY(8),
    REFUND(9, ProcessingCode.REFUND, "REFUND"),
    VOID_REFUND(10, ProcessingCode.VOID_REFUND, "VOID OF REFUND"),
    SALE_WITH_CASH(11, ProcessingCode.SALE_WITH_CASH, "SALE-CASH"),
    CASH(12),
    BATCH_UPLOAD(13),
    PRE_AUTH(14, ProcessingCode.PRE_AUTH, "PRE-AUTH"),
    SALE_WITH_TIP(15),
    ADJUSTMENT(16),
    REVERSAL(17, ProcessingCode.REFUND),
    EMI_SALE(18, ProcessingCode.SALE, "EMI SALE"),
    EMI(19),
    SALE_COMPLETION(20),
    TIP_ADJUSTMENT(21,txnTitle = "TIP ADJUST"),
    OFF_SALE(22),
    CASH_AT_POS(23, ProcessingCode.CASH_AT_POS, "CASH ONLY"),
    BATCH_SETTLEMENT(24),
    PRE_AUTH_COMPLETE(25, ProcessingCode.PRE_SALE_COMPLETE, "AUTH-COMP"),
    VOID_PREAUTH(26, ProcessingCode.VOID_PREAUTH, "VOID PRE-AUTH"),
    TIP_SALE(27, ProcessingCode.TIP_SALE, "TIP ADJUST"),
    PENDING_PREAUTH(28, ProcessingCode.PENDING_PREAUTH, "PRE AUTH TXN"),
    OFFLINE_SALE(29, ProcessingCode.OFFLINE_SALE, "OFFLINE SALE"),
    VOID_OFFLINE_SALE(30, ProcessingCode.VOID_OFFLINE_SALE, "VOID OFFLINE SALE"),
    BRAND_EMI_MASTER_DATA(31, ProcessingCode.BRAND_EMI, "BRAND EMI"),
    EMI_ENQUIRY(32, ProcessingCode.BANK_EMI, "EMI ENQUIRY"),
    BRAND_EMI(33, ProcessingCode.BRAND_EMI, "EMI SALE"),

    // requirement in reports print so added
    TEST_EMI(34, ProcessingCode.BRAND_EMI, "TEST EMI TXN"),
    BRAND_EMI_BY_ACCESS_CODE(35, ProcessingCode.BRAND_EMI, "EMI SALE"),
    VOID_EMI(37, ProcessingCode.VOID, "VOID EMI"),

}
//endregion

//region=========================Processing Code=================
enum class ProcessingCode(val code: String) {
    NONE("-1"),
    KEY_EXCHANGE("960300"),
    KEY_EXCHANGE_RESPONSE("960301"),
    KEY_EXCHANGE_AFTER_SETTLEMENT("960400"),
    INIT("960200"),
    INIT_MORE("960201"),
    APP_UPDATE("960100"),
    APP_UPDATE_CONTINUE("960101"),
    APP_UPDATE_CONFIRMATION("960111"),
    SALE("920001"),

    VOID("940001"),
    REFUND("941001"),
    PRE_SALE_COMPLETE("925001"),
    SETTLEMENT("970001"),
    FORCE_SETTLEMENT("970002"),
    ZERO_SETTLEMENT("970003"),

    EMI_ENQUIRY("920098"),
    VOID_REFUND("942001"),
    PENDING_PREAUTH("931000"),
    VOID_PREAUTH("940000"),

    CHARGE_SLIP_START("960210"),
    CHARGE_SLIP_CONTINUE("960211"),
    CHARGE_SLIP_HEADER_FOOTER("960209"),
    GCC("920099"),

    OFFLINE_SALE("920011"),
    VOID_OFFLINE_SALE("940011"),
    TIP_SALE("924001"),
    CASH_AT_POS("923001"),
    SALE_WITH_CASH("922001"),
    PRE_AUTH("920000"),
    CROSS_SELL("982001"),
    BRAND_EMI("982002"),
    BANK_EMI("982002"),

}
//endregion

//region========================NII=====================
enum class Nii(val nii: String) {
    DEFAULT("0091"),
    SOURCE("0001"),
    SMS_PAY("0411"),
    HDFC_DEFAULT("0002"),
    BRAND_EMI_MASTER("0028"),

}
//endregion

//region=======================ConnectionType==============
enum class ConnectionType(val code: String) {
    PSTN("1"), ETHERNET("2"), GPRS("3")
}
//endregion

//region=======================Table Names Code============
enum class TableType(val code: String) {
    TERMINAL_COMMUNICATION_TABLE("101"),
    ISSUER_PARAMETER_TABLE("102"),
    TERMINAL_PARAMETER_TABLE("106"),
    CARD_DATA_TABLE("107"),
    HDFC_TPT("201"),
    HDFC_CDT("202")
}
//endregion

//region=============================Drawer Submenu Group=============
enum class EDrawerSubmenuGroup {
    FUNCTIONS, REPORT, NONE
}

// endregion


enum class UiAction(val title: String = "Not Declared", val res: Int = R.drawable.ic_bank_emi) {
    INIT, KEY_EXCHANGE, INIT_WITH_KEY_EXCHANGE, START_SALE(
        "Sale",
        R.drawable.ic_bbg
    ),
    SETTLEMENT, APP_UPDATE, PRE_AUTH(
        title = "Pre-Auth",R.drawable.ic_preauth
    ),
    REFUND("Refund", R.drawable.ic_refund),
    BANK_EMI(
        "Bank EMI",
        R.drawable.ic_bank_emi
    ),
    OFFLINE_SALE(title = "Offline Sale"), CASH_AT_POS(
        "Cash Advance",
        R.drawable.ic_cash_at_pos
    ),
    SALE_WITH_CASH("Sale With Cash", R.drawable.ic_salewithcash),
    PRE_AUTH_COMPLETE(title = "Pre Auth Complete"), EMI_ENQUIRY(
        "EMI Catalogue",
        R.drawable.ic_emicatalogue
    ),
    BRAND_EMI("Brand EMI", R.drawable.ic_brandemi),
    TEST_EMI("Test EMI TXN", R.drawable.ic_brand_emi_code),
    FLEXI_PAY("Flexi Pay", R.drawable.ic_cash_at_pos),
  //  DEFAUTL("Not Declared", R.drawable.ic_sad),
    BRAND_EMI_CATALOGUE("Brand EMI Catalogue", R.drawable.ic_sale),
    BANK_EMI_CATALOGUE("Bank EMI Catalogue", R.drawable.ic_sale),
   // BANK_EMI_BY_ACCESS_CODE("Brand Emi By Code", R.drawable.ic_brand_emi),
    //DYNAMIC_QR("Dynamic QR", R.drawable.ic_qr_code)
}


//Below Enum Class is used to detect different card Types:-
enum class DetectCardType(val cardType: Int, val cardTypeName: String = "") {
    NONE(-1,"None"),
    REMOVE(-2,"Remove"),
    CARD_ERROR_TYPE(0),
    MAG_CARD_TYPE(1, "Mag"),
    EMV_CARD_TYPE(2, "Chip"),
    CONTACT_LESS_CARD_TYPE(3, "CTLS"),
    CONTACT_LESS_CARD_WITH_MAG_TYPE(4, "CTLSMAG"),
    CONTACT_LESS_CARD_WITH_EMV_TYPE(6, "CTLSEMV"),
    MANUAL_ENTRY_TYPE(5, "MAN")
}

//Below Enum Class is used to check which Pos Entry Type:-
enum class PosEntryModeType(val posEntry: Int) {
    //insert with pin
    EMV_POS_ENTRY_PIN(553),
    EMV_POS_ENTRY_NO_PIN(552),

    //off line pin
    EMV_POS_ENTRY_OFFLINE_PIN(554),

    //Used POS ENTRY Code for Offline Sale:-
    OFFLINE_SALE_POS_ENTRY_CODE(513),

    //used for fall back
    EMV_POS_ENTRY_FALL_MAGPIN(623),
    EMV_POS_ENTRY_FALL_MAGNOPIN(620),

    /*Below EMV Fallback case is of no USE:-
    EMV_POS_ENTRY_FALL_4DBCPIN(663),
    EMV_POS_ENTRY_FALL_4DBCNOPIN(660),*/

    ///swipe with cvv and pin
    POS_ENTRY_SWIPED_4DBC(563),

    //swipe with out cvv  with out pin
    POS_ENTRY_SWIPED_NO4DBC(523),

    //swipe pin with out cvv
    POS_ENTRY_SWIPED_NO4DBC_PIN(524),

    //Manual with cvv
    POS_ENTRY_MANUAL_4DBC(573),

    //Manual without cvv
    POS_ENTRY_MANUAL_NO4DBC(513),

    //contact less swipe data with out pin
    CTLS_MSD_POS_ENTRY_CODE(921),

    //contact less with  swipe data and pin
    CTLS_MSD_POS_WITH_PIN(923),

    // contact less  insert data with out pin
    CTLS_EMV_POS_ENTRY_CODE(911),

    // contact less insert data with pin
    CTLS_EMV_POS_WITH_PIN(913)
}

enum class CvmResultEnum {
    CVM_NO_CVM,  // no cvm
    CVM_OFFLINE_PIN,  //plaintext pin or enciphered pin, just for contact
    CVM_ONLINE_PIN,  //onlin pin
    CVM_SIG,  //signature
    CVM_ONLINE_PIN_SIG,  // online pin plus signature
    CVM_CONSUMER_DEVICE
    //see phone
}
enum class EDevAc {
    CARD_SCAN_RESULT, USE_CHIP, ERR_CDT, ERR_TPT, ERR_IPT, ERR_BELOW_FLOOR_LIMIT, ERR_SALE_NOT_ALLOWED,
    ERR_PASSWORD, ERR_SELECTION, ERR_OFFLINE_DENIED, ERR_EMV_DENIED, DONT_REMOVE_CARD,
    SHW_PROGRESS, HDE_PROGRESS, UPDATE_PROGRESS_MSG, ERR_TRANSACTION_CANCELLED,
    ERR_ENCRYPTION_TRACK2, SHOW_PAN, ERR_TIME_OUT, ERR_SEE_PHONE, REMOVE_CTLS_CARD, ERR_CLTS, TRANSACTION_APPROVED, TRANSACTION_DECLINED,
    ERR_PRINTING, MERCHANT_COPY_PRINITED, CUSTOMER_COPY_PRINTED,REVERSAL_SENDING_ERROR
}
enum class ETransResult {
    EMV_SUCCUSS,
    EMV_ONLINE_APPROVED,     //Online approval
    EMV_ONLINE_DENIED,    //Online rejection
    EMV_OFFLINE_APPROVED,    //Offline approval
    EMV_OFFLINE_DENIED,   //Offline rejection
    EMV_ONLINE_CARD_DENIED,  //Host approval, card rejection
    EMV_ABORT_TERMINATED, //abnormal
    EMV_ARQC,  //Apply online
    EMV_SIMPLEFLOWEND, //用户终止 User termination
}

enum class EPanMode {
    X9_8_WITH_PAN,
    X9_8_NO_PAN
}

enum class EDeviceType(val atmName: String = "") {
    NONE("None"), MS("Magnetic Stripe Mode"), IC("Chip Mode"), RF("Contactless Mode"),
    REMOVE
}

//region=====================================Nav Controller Fragments Labels:-
enum class NavControllerFragmentLabel(val destinationLabel: String) {
    DASHBOARD_FRAGMENT_LABEL("fragment_dash_board")
}
//endregion

//region=====================================App Update Enum Constants:-
enum class AppUpdate(var updateCode: String) {
    APP_UPDATE_AVAILABLE("0103"),
    MANDATORY_APP_UPDATE("0220"),
    OPTIONAL_APP_UPDATE("0210")
}
//endregion

enum class EPinRequire { NO_PIN, SHOULD_PIN, MUST_PIN }

enum class EFallbackCode(var fallBackCode: Int) {
    Swipe_fallback(111),
    EMV_fallback(8),
    NO_fallback(0),
    EMV_fallbackNew(12),
    CTLS_fallback(333)
}

//region============================Enum Class For Account Type:-
enum class AccountTypeSelection(var accountType: String) {
    DEFAULT("default"),
    SAVING("saving"),
    CHECKING("checking"),
    CREDIT("credit"),
    UNIVERSAL("universal")
}
//endregion

//region==============================Enum Class For Account Type Value in Sale Packet Field 58:-
enum class AccountTypePacketValue(var accountTypeValue: String) {
    DEFAULT("00"),
    SAVING("10"),
    CHECKING("20"),
    CREDIT("30"),
    UNIVERSAL("40")
}
//endregion

//region===============================Enum Class For Check Settlement Coming From Which Activity:-
enum class SettlementComingFrom(var screenType: String) {
    DASHBOARD("dashboard"),
    SETTLEMENT("settlement")
}
//endregion

//region==============================Enum Class for Splitter Types:-
enum class SplitterTypes(var splitter: String) {
    VERTICAL_LINE("|"),
    OPEN_CURLY_BRACE("{"),
    CLOSED_CURLY_BRACE("}"),
    COMMA(","),
    DOT("."),
    CARET("^"),
    STAR("*"),
    POUND("#")
}
//endregion

//region===============================Enum Class for BrandEMI Data RequestType:-
enum class BrandEMIRequestType(var requestType: String) {
    BRAND_DATA("1"),
    ISSUER_T_AND_C("5"),
    BRAND_T_AND_C("6"),
    BRAND_SUB_CATEGORY("2"),
    BRAND_EMI_Product("3"),
}
//endregion

//Below enum class is used to detect Cross Sell List Options:-
enum class CrossSellOptions(val heading: String, val code: Int) {
    CREDIT_LIMIT_INCREASE("Credit Limit Increase", 13),
    JUMBO_LOAN("Jumbo Loan", 14),
    INSTA_LOAN("Insta Loan", 15),
    HDFC_CREDIT_CARD("HDFC Credit Card", 16),
    REPORTS("Report", 17)
}

//Below enum class is used to identify cross sell packet request type:-
enum class CrossSellRequestType(val requestTypeCode: Int,val requestName:String="NOT DEFINED") {
    INSTA_LOAN_VERIFY_CARD_DETAILS_REQUEST_TYPE(1,"Insta Loan"),
    INSTA_LOAN_OTP_VERIFY_REQUEST_TYPE(2),
    JUMBO_LOAN_VERIFY_CARD_DETAILS_REQUEST_TYPE(3,"Jumbo Loan"),
    JUMBO_LOAN_OTP_VERIFY_REQUEST_TYPE(4),
    CREDIT_LIMIT_INCREASE_VERIFY_CARD_DETAILS_REQUEST_TYPE(5,"Credit Limit Increase"),
    CREDIT_LIMIT_INCREASE_OTP_VERIFY_REQUEST_TYPE(6),
    CARD_UPGRADE_VERIFY_CARD_DETAILS_REQUEST_TYPE(7),
    CARD_UPGRADE_OTP_VERIFY_REQUEST_TYPE(8),
    HDFC_CREDIT_CARD_VERIFY_CARD_DETAILS_REQUEST_TYPE(9,"HDFC Credit Card"),
    HDFC_CREDIT_CARD_OTP_VERIFY_REQUEST_TYPE(10),
    DOWNLOAD_AND_PRINT_MONTHLY_REPORT_ON_POS(11),
    SENT_REPORT_ON_MAIL_OR_SMS(12),
}


enum class EDashboardItem(val title: String, val res: Int, val rank: Int = 15, var childList:MutableList<EDashboardItem>?=null,) {
    NONE("No Option Found", R.drawable.ic_sale),
    SALE("Sale", R.drawable.ic_sale, 1),
    DIGI_POS("Digi POS", R.drawable.ic_digi_pos, 2),
    BANK_EMI("Bank EMI", R.drawable.ic_bank_emi, 3),
    BRAND_EMI("Brand EMI", R.drawable.ic_brandemi,4),
    EMI_PRO("Brand EMI By Code", R.drawable.ic_brand_emi_code, 5),
    EMI_ENQUIRY("EMI Catalogue", R.drawable.ic_emicatalogue, 6),
    PREAUTH("Pre-Auth", R.drawable.ic_preauth, 7),

   PREAUTH_COMPLETE("Pre-Auth Complete", R.drawable.ic_preauth, 5),
    PREAUTH_VIEW("Pre-Auth View", R.drawable.ic_preauth, 65),
    PENDING_PREAUTH("Pending Preauth", R.drawable.ic_preauth, 6),
    OFFLINE_SALE("Offline Sale", R.drawable.ic_sale, 7),
    VOID_OFFLINE_SALE("Void Offline Sale", R.drawable.ic_void, 8),
    SALE_TIP("Tip Adjust", R.drawable.ic_tipadjust, 9),
    VOID_PREAUTH("Void Preauth", R.drawable.ic_void, 10),
    REFUND("Refund", R.drawable.ic_refund, 11),
    VOID_REFUND("Void Refund", R.drawable.ic_void, 12),
    VOID_SALE("Void", R.drawable.ic_void, 13),
    CROSS_SELL("BNPL", R.drawable.ic_crosssell, 14),

    SALE_WITH_CASH("Sale With Cash", R.drawable.ic_salewithcash),
    CASH_ADVANCE("Cash Advance", R.drawable.ic_cash_at_pos),

  ///  PENDING_OFFLINE_SALE("View Offline Sale", R.drawable.ic_pending_preauth),
    PRE_AUTH_CATAGORY("Pre-Auth", R.drawable.ic_preauth, 9),
    MORE("View More", R.drawable.ic_digi_pos, 999),
    BONUS_PROMO("Bonus Promo", R.drawable.ic_merchant_promo, 15),

    EMI_CATALOGUE("EMI Catalogue", R.drawable.ic_emicatalogue, 17),
    BRAND_EMI_CATALOGUE("Brand EMI Catalogue", R.drawable.ic_brandemi, 18),
    BANK_EMI_CATALOGUE("Bank EMI Catalogue", R.drawable.ic_sale, 19),
    MERCHANT_REFERRAL("MRP", R.drawable.ic_merchant_referal_program, 20),
    LESS("View Less", R.drawable.ic_digi_pos, 888),

    TEST_EMI("Test Emi TXN", R.drawable.ic_digi_pos, 777),
    // just for handling the test emi not used in dashboard items
 /*
    FLEXI_PAY("Flexi Pay", R.drawable.ic_cash_advance, 666),
    LESS("View Less", R.drawable.ic_arrow_up, 888),

    UPI("UPI COLLECT", R.drawable.upi_icon, 901),
    SMS_PAY("SMS PAY", R.drawable.sms_icon, 902),
    TXN_LIST("TXN LIST", R.drawable.sms_icon, 903),
    PENDING_TXN("Pending Txn", R.drawable.pending_txn, 903),
    STATIC_QR("Static QR", R.drawable.ic_qr_code, 904),
    BHARAT_QR("Bharat QR", R.drawable.ic_qr_code, 905),*/


}
sealed class VxEvent {
    data class ChangeTitle(val titleName: String) : VxEvent()
    data class ReplaceFragment(val fragment: Fragment) : VxEvent()
    object AutoSettle : VxEvent()
    data class Emi(val amt: Double, val type: EDashboardItem) : VxEvent()
    object ForceSettle : VxEvent()
    object Home : VxEvent()
    object InitTerminal : VxEvent()
    object AppUpdate : VxEvent()
    object DownloadTMKForHDFC : VxEvent()
}

// written by kushal
// region for bank functions
enum class BankFunctionsItem(val _name: String){
    ADMIN_VAS("BH ADMIN"),
    ADMIN_PAYMENT("ADMIN PAYMENT")
}
// end region

// region for bank functions admin vas
enum class BankFunctionsAdminVasItem(val _name: String){
    INIT("INIT"),
    TEST_EMI("TEST EMI"),
    TERMINAL_PARAM("TERMINAL PARAM"),
    COMM_PARAM("COMM PARAM"),
    //ENV_PARAM("ENV PARAM"),
    INIT_PAYMENT_APP("INIT PAYMENT APP"),
}
// end region

// region for bank functions communication param
enum class CommunicationParamItem(val _name: String,val value:Int):Serializable{
    TXN_PARAM("TXN PARAM",1),
    APP_UPDATE_PARAM("APP UPDATE PARAM",2),

}
// end region

// region for bank functions admin vas
enum class BankFunctions(val _name: String){
    INIT("INIT"),
    TEST_EMI("TEST EMI"),
    TERMINAL_PARAM("TERMINAL PARAM"),
    COMM_PARAM("COMM PARAM"),
    ENV_PARAM("ENV PARAM"),
    INIT_PAYMENT_APP("INIT PAYMENT APP"),
}
// end region

// region for bank functions terminal param
enum class BankFunctionsTerminalItem(val _name: String){
    BATCH_NUMBER("BATCH NUMBER"),
    INVOICE_NUMBER("INVOICE NUMBER"),
    MERCHANT_ID("MERCHANT ID"),
    STN("STN"),
    TERMINAL_ID("TERMINAL ID"),
    CLEAR_FBATCH("CLEAR FBATCH"),
}
// end region
//Below Enum Class is used for Preference Save [String , Int and Boolean] Keys Constant:-
enum class PrefConstant(val keyName: Any) {
    AID_RID_INSERT_STATUS("aid_rid_status"),
    SALE_INVOICE_INCREMENT("sale_invoice_increment"),
    SETTLEMENT_ROC_INCREMENT("settlement_roc_increment"),
    OFFLINE_ROC_INCREMENT("offline_roc_increment"),
    SETTLEMENT_BATCH_INCREMENT("settlement_batch_number"),
    SETTLEMENT_PROCESSING_CODE("settlement_processing_code"),
    SETTLE_BATCH_SUCCESS("settle_batch_success"),
    INIT_AFTER_SETTLE_BATCH_SUCCESS("init_after_settle_batch_success"),
    FTP_IP_ADDRESS("ftp_ip_address"),
    FTP_IP_PORT("ftp_ip_port"),
    FTP_USER_NAME("ftp_user_name"),
    FTP_PASSWORD("ftp_password"),
    FTP_FILE_NAME("ftp_file_name"),
    FTP_FILE_SIZE("ftp_file_size"),
    BLOCK_MENU_OPTIONS("block_menu_options"),
    INSERT_PPK_DPK("insert_ppk_dpk"),
    INIT_AFTER_SETTLEMENT("init_after_settlement"),
    VOID_ROC_INCREMENT("void_roc_increment"),
    SERVER_HIT_STATUS("server_hit_status"),
    APP_UPDATE_CONFIRMATION_TO_HOST("app_update_confirmation_to_host"),
    TMK_DOWNLOAD("tmk_download"),
    PROMO_STRING("promo_string")
}

//Below Enum Class is used for SubHeading intent extra when we transact Fragment:-
enum class SubHeaderTitle(val title: String) {
    VOID_SUBHEADER_VALUE("Void Sale"),
    SALE_SUBHEADER_VALUE("Sale"),
    SALE_WITH_CASH_SUBHEADER_VALUE("Sale with Cash"),
    PREAUTH_SUBHEADER_VALUE("PreAuth"),
    OFFLINE_SALE_SUBHEADER_VALUE("Offline Sale"),
    VOID_OFFLINE_SALE_SUBHEADER_VALUE("Void Offline Sale"),
    SETTLEMENT_SUBHEADER_VALUE("Settlement"),
    CASH_ADVANCE("Cash Advance"),
    PRE_AUTH_COMPLETE("Auth Complete"),
    VOID_PRE_AUTH("Void Preauth"),
    TIP_SALE("Tip Adjust"),
    VOID_REFUND_SUBHEADER_VALUE("Void Refund"),
    REFUND_SUBHEADER_VALUE("Refund"),
    EMI_CATALOG("EMI Enquiry"),
    BANK_EMI("Bank EMI"),
    CROSS_SELL_SUBHEADER_VALUE("Cross Sell"),
    Brand_EMI_Master_Category("Brand EMI Master Category"),
    Brand_EMI_SUB_Category("Brand EMI Sub Category"),
    Brand_EMI("Brand EMI Sale"),
    Brand_EMI_BY_ACCESS_CODE("Brand EMI By Access Code"),
    TEST_EMI("Test EMI TXN"),
    Flexi_PAY("Flexi Pay"),
    Merchent_refferal("Merchant Referral Program"),
}
enum class EIntentRequest(val code: Int) {
    TRANSACTION(100),
    EMI_ENQUIRY(101),
    GALLERY(2000),
    PRINTINGRECEIPT(102),
    BankEMISchemeOffer(106),
    FlexiPaySchemeOffer(107),
}

// region reports option
enum class ReportsItem(val _name: String){
    LAST_RECEIPT("Last Receipt"),
    LAST_CANCEL_RECEIPT("Last Cancel Receipt"),
    ANY_RECEIPT("Any Receipt"),
    DETAIL_REPORT("Detail Report"),
    SUMMERY_REPORT("Summary Report"),
    LAST_SUMMERY_REPORT("Last Summary Report"),
    PRINT_REVERSAL_REPORT("Print Reversal Report")
}

enum class EPrintCopyType(val pName: String) {
    MERCHANT("**MERCHANT COPY**"), CUSTOMER("**CUSTOMER COPY**"), DUPLICATE("**DUPLICATE COPY**");
}
// end region

// region
enum class CardEntryMode(val _name: String, val _value: String){
    EMV_WITH_PIN("INSERT","0553- EMV with pin"), //Done
    EMV_NO_PIN("INSERT","0552- EMV No pin"),
    EMV_OFFLINE_PIN("INSERT","0554- EMV offline pin"),
    EMV_FALLBACK_SWIPE_WITH_PIN("FALLBACK","0623- EMV fallback to swipe with pin"), // Done
    EMV_FALLBACK_SWIPE_NO_PIN("FALLBACK","0620- Emv fallback to swipe no pin - 620"),
    SWIPE_WITH_PIN("SWIPE","0524- Swipe with pin"),// Done partial
    SWIPE_NO_PIN("SWIPE","0523- Swipe No pin"),
    CTLS_SWIPE_NO_PIN("CLESS_SWIPE","0921- CTLS swipe no pin"),
    CTLS_SWIPE_WITH_PIN("CLESS_SWIPE","0923- CTLS swipe with pin"),
    CTLS_EMV_NO_PIN("CLESS_EMV","0911- CTLS emv no pin"), // Done
    CTLS_EMV_WITH_PIN("CLESS_EMV","0913- ctls emv with pin"), //Done
    MANUAL("MANUAL","0913- ctls emv with pin")
}
// end region

// region
enum class TestEmiItem(val id:String, val _name: String){
    BASE_TID("1", "Base TID"),
    OFFUS_TID("2", "OFFUS TID"),
    _3_M_TID("3", "3 M TID"),
    _6_M_TID("6", "6 M TID"),
    _9_M_TID("9", "9 M TID"),
    _12_M_TID("12", "12 M TID")
}
// end region
