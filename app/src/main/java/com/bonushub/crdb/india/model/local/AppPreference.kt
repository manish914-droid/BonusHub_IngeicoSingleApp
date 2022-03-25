package com.bonushub.crdb.india.model.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.bonushub.crdb.india.HDFCApplication
import com.bonushub.crdb.india.model.remote.RestartHandlingModel
import com.bonushub.crdb.india.utils.addPad
import com.bonushub.crdb.india.utils.logger
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ingenico.hdfcpayment.model.ReceiptDetail

//region========EMV=======

const val HDFCIssuerID="50"

object AppPreference {

    private val TAG = AppPreference::class.java.simpleName
    const val PC_NUMBER_KEY = "pc_number_key"
    const val PC_NUMBER_KEY_2 = "pc_number_key_2"
    const val AMEX_BANK_CODE = "07"
    const val ROC_V2 = "roc_tan_v2"
    const val WALLET_ISSUER_ID = "50"
    private val preferenceName = "HDFCPreference"
    private var sharedPreference: SharedPreferences? = null
    private const val PREFERENCE_NAME = "PaxApp"

    const val REVERSAL_DATA = "reversal_data"
    const val BANK_CODE_KEY = "bank_code_key"

    const val BANKCODE = "2" //For testing
    const val F48_STAMP = "f48timestamp"
    const val ACC_SEL_KEY = "acc_sel_key"
 /*   const val PC_NUMBER_KEY = "pc_number_key"
    const val PC_NUMBER_KEY_2 = "pc_number_key_2"*/
    const val LOGIN_KEY="login_key"
    const val F48IdentifierAndSuccesssTxn="f48id_txnDate"
    const val ENQUIRY_AMOUNT_FOR_EMI_CATALOGUE = "enquiry_amount_for_emi_catalogue"


    // kushal bank functions
    const val GENERIC_REVERSAL_KEY = "generic_reversal_key"

    const val LAST_SUCCESS_RECEIPT_KEY = "Last_Success_Receipt"
    const val LAST_BATCH = "last_batch"
    const val RESTART_HANDLING = "restart_handling"
    const val LAST_CANCEL_RECEIPT_KEY = "Last_Cancel_Receipt"

    @JvmStatic
    fun initializeEncryptedSharedPreferences(context: Context) {
        val startTs = System.currentTimeMillis()

        // Step 1: Create or retrieve the Master Key for encryption/decryption
        val masterKeyAlias = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

        // Step 2: Initialize/open an instance of EncryptedSharedPreferences
        sharedPreference = EncryptedSharedPreferences.create(
            context, preferenceName, masterKeyAlias,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val endTs = System.currentTimeMillis()
        Log.d("Time Difference:- ", ((endTs - startTs).toString()))
    }

    //region==================================Below methods is used to save/retrieve string values in shared preference:-
    fun saveString(keyName: String, value: String) =
        sharedPreference?.edit()?.putString(keyName, value)?.apply()

    fun getString(keyName: String) = sharedPreference?.getString(keyName, null) ?: ""
    //endregion

    //region==================================Below methods is used to save/retrieve int values in shared preference:-
    fun saveInt(keyName: String, value: Int) =
        sharedPreference?.edit()?.putInt(keyName, value)?.apply()

    fun getInt(keyName: String) = sharedPreference?.getInt(keyName, 0) ?: 0
    //endregion

    //region for saving that the user is inited the terminal or not
    fun saveLogin(isSave:Boolean) {
        saveBoolean(LOGIN_KEY,isSave)
    }
// TO get the INIT status
    fun getLogin():Boolean{
      return  getBoolean(LOGIN_KEY)
    }
    //region==================================Below methods is used to save/retrieve boolean values in shared preference:-
    fun saveBoolean(keyName: String, value: Boolean) =
        sharedPreference?.edit()?.putBoolean(keyName, value)?.apply()

    fun getBoolean(keyName: String) = sharedPreference?.getBoolean(keyName, false) ?: false
    //endregion


    @JvmStatic
    fun getBankCode(): String {
        val tBc = getString(BANK_CODE_KEY)
        return addPad(if (tBc.isNotEmpty()) tBc else BANKCODE, "0", 2)
    }

    fun setBankCode(bankCode: String) = saveString(BANK_CODE_KEY, bankCode)

    @JvmStatic
    fun getLongData(key: String): Long {
        val v = HDFCApplication.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return v?.getLong(key, 0L) ?: 0L
    }

    @JvmStatic
    fun setLongData(key: String, value: Long) {
        val p = HDFCApplication.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val edit = p?.edit()
        edit?.putLong(key, value)
        edit?.apply()
    }

    @JvmStatic
    fun getIntData(key: String): Int {
        val v = HDFCApplication.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return v?.getInt(key, 0) ?: 0
    }

    @JvmStatic
    fun setIntData(key: String, value: Int) {
        val p = HDFCApplication.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val edit = p?.edit()
        edit?.putInt(key, value)
        edit?.apply()
    }

   /* fun clearReversal() {
       // logger(TAG, "========CLEAR Reversal=========", "e")
        sharedPreference?.edit()?.putString(REVERSAL_DATA, "")?.apply()
    }*/


    //region Below method is used to Save Batch File Data in App Preference:-
    fun saveBatchInPreference(batchList: MutableList<BatchTable?>) {
        logger(TAG, "========saveLastSuccessReceipt=========", "e")
        logger(TAG, Gson().toJson(batchList), "e")
        val tempBatchDataList = Gson().toJson(
            batchList,
            object : TypeToken<List<BatchTable>>() {}.type
        ) ?: ""
       val v= HDFCApplication.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        v?.edit()?.putString(LAST_BATCH,tempBatchDataList)?.apply()
       // saveString(LAST_BATCH, tempBatchDataList)
    }
    //endregion




    // region
    fun getLastBatch(): List<BatchTable>? {
        logger(TAG, "========getLastSuccessReceipt=========", "e")
        val v = HDFCApplication.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

        return if (v != null) {
            try {
                val str = v.getString(LAST_BATCH, "")
                if (!str.isNullOrEmpty()) {
                    val batList = Gson().fromJson<List<BatchTable>>(
                        str,
                        object : TypeToken<List<BatchTable>>() {}.type
                    )
                    logger(TAG, Gson().toJson(batList), "e")
                    return batList
                } else null
            } catch (ex: Exception) {
                throw Exception("Last Success Receipt Error!!!")
            }
        } else
            null
    }
    // end region


    // region
    fun saveLastReceiptDetails(receiptDetail:String?){
        val v = HDFCApplication.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        v?.edit()?.putString(LAST_SUCCESS_RECEIPT_KEY, receiptDetail?:"")?.apply()
    }

    fun saveLastReceiptDetails(receiptDetail:BatchTable?){
        val v = HDFCApplication.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val jsonResp=Gson().toJson(receiptDetail)
        v?.edit()?.putString(LAST_SUCCESS_RECEIPT_KEY, jsonResp?:"")?.apply()
    }
    // end region

    //region kushal
    @JvmStatic
    fun getLastSuccessReceipt(): BatchTable? {
        logger(TAG, "========getLastSuccessReceipt=========", "e")
        val v = HDFCApplication.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

        return if (v != null) {
            try {
                val str = v.getString(LAST_SUCCESS_RECEIPT_KEY, "")
                if (!str.isNullOrEmpty()) {
                    Gson().fromJson<BatchTable>(
                        str,
                        object : TypeToken<BatchTable>() {}.type
                    )
                } else null
            } catch (ex: Exception) {
                throw Exception("Last Success Receipt Error!!!")
            }
        } else
            null
    }

    // end region

    // region
    fun saveLastCancelReceiptDetails(receiptDetail:String?){
        val v = HDFCApplication.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        v?.edit()?.putString(LAST_CANCEL_RECEIPT_KEY, receiptDetail?:"")?.apply()
    }

    fun saveLastCancelReceiptDetails(receiptDetail:ReceiptDetail?){
        val v = HDFCApplication.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val jsonResp=Gson().toJson(receiptDetail)
        v?.edit()?.putString(LAST_CANCEL_RECEIPT_KEY, jsonResp?:"")?.apply()

        saveReversal()
    }
    // end region

    //region kushal
    @JvmStatic
    fun getLastCancelReceipt(): ReceiptDetail? {
        logger(TAG, "========getLastSuccessReceipt=========", "e")
        val v = HDFCApplication.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

        return if (v != null) {
            try {
                val str = v.getString(LAST_CANCEL_RECEIPT_KEY, "")
                if (!str.isNullOrEmpty()) {
                    Gson().fromJson<ReceiptDetail>(
                        str,
                        object : TypeToken<ReceiptDetail>() {}.type
                    )
                } else null
            } catch (ex: Exception) {
                throw Exception("Last Cancel Receipt Error!!!")
            }
        } else
            null
    }

    // end region


    // region need in report
    @JvmStatic
    fun getReversal(): String? {
        logger(TAG, "========getReversal=========", "e")
        val v = HDFCApplication.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return if (v != null) {
            try {
                val str = v.getString(GENERIC_REVERSAL_KEY, "")
                if (!str.isNullOrEmpty()) {
                   // Gson().fromJson<IsoDataWriter>(str, object : TypeToken<IsoDataWriter>() {}.type)
                    return str
                } else null
            } catch (ex: Exception) {
                throw Exception("Reversal error!!!")
            }
        } else
            null
    }

    @JvmStatic
    fun saveReversal(){
        val v = HDFCApplication.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        v?.edit()?.putString(GENERIC_REVERSAL_KEY, "true")?.apply()
    }

    @JvmStatic
    fun clearReversal(){
        val v = HDFCApplication.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        v?.edit()?.putString(GENERIC_REVERSAL_KEY, "")?.apply()
    }

    // end region

    //region Below method is used to Save Restart Handling File Data in App Preference:-
    fun saveRestartDataPreference(restartData:String?) {
        val v = HDFCApplication.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        v?.edit()?.putString(RESTART_HANDLING, restartData?:"")?.apply()
    }

    //endregion
    @JvmStatic
    fun clearRestartDataPreference(){
        val v = HDFCApplication.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        v?.edit()?.putString(RESTART_HANDLING, "")?.apply()
    }

    // end region


    // region
    @JvmStatic
    fun getRestartDataPreference(): RestartHandlingModel? {
        logger(TAG, "========get=========", "e")
        val v = HDFCApplication.appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

        return if (v != null) {
            try {
                val str = v.getString(RESTART_HANDLING, "")
                if (!str.isNullOrEmpty()) {
                    Gson().fromJson<RestartHandlingModel>(
                        str,
                        object : TypeToken<RestartHandlingModel>() {}.type
                    )
                } else null
            } catch (ex: Exception) {
                throw Exception("Last Success Receipt Error!!!")
            }
        } else
            null
    }

    // end reegion


    // region


}