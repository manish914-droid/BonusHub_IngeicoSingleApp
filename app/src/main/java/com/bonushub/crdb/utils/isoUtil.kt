package com.bonushub.crdb.utils


const val SUCCESS="00"

enum class Mti(val mti: String) {
    MTI_INIT("0800"),
    MTI_LOGON("0800"),
    PRE_AUTH_MTI("0100"),
    PRE_AUTH_COMPLETE_MTI("0220"),  //also used in tipsale
    SETTLEMENT_MTI("0500"),
    DEFAULT_MTI("0200"),
    REVERSAL("0400"),
    APP_UPDATE_MTI("0800"),
    CROSS_SELL_MTI("0800"),
    EIGHT_HUNDRED_MTI("0800"),
    BRAND_EMI_MASTER_MTI("0800")
}
enum class ESaleType(val posEntryValue: Int) {
    //insert with pin
    EMV_POS_ENTRY_PIN(553),
    EMV_POS_ENTRY_NO_PIN(552),
    //off line pin
    EMV_POS_ENTRY_OFFLINE_PIN(554),
    //used for fall back
    EMV_POS_ENTRY_FALL_MAGPIN(623),
    EMV_POS_ENTRY_FALL_MAGNOPIN(620),
    EMV_POS_ENTRY_FALL_4DBCPIN(663),
    EMV_POS_ENTRY_FALL_4DBCNOPIN(660),

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