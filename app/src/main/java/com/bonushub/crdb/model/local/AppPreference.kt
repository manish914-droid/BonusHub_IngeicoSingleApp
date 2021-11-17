package com.bonushub.crdb.model.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.bonushub.crdb.HDFCApplication
import com.bonushub.crdb.utils.addPad
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

//region========EMV=======

const val HDFCIssuerID="50"

object AppPreference {

    private val TAG = AppPreference::class.java.simpleName

    const val AMEX_BANK_CODE = "07"
    const val ROC_V2 = "roc_tan_v2"
    const val WALLET_ISSUER_ID = "50"
    private val preferenceName = "HDFCPreference"
    private var sharedPreference: SharedPreferences? = null
    private const val PREFERENCE_NAME = "PaxApp"

    const val REVERSAL_DATA = "reversal_data"
    const val BANK_CODE_KEY = "bank_code_key"
    const val BANKCODE = "01" //For testing
    const val F48_STAMP = "f48timestamp"
    const val ACC_SEL_KEY = "acc_sel_key"
  /*  const val PC_NUMBER_KEY = "pc_number_key"
    const val PC_NUMBER_KEY_2 = "pc_number_key_2"*/
    const val LOGIN_KEY="login_key"
    const val F48IdentifierAndSuccesssTxn="f48id_txnDate"
    const val ENQUIRY_AMOUNT_FOR_EMI_CATALOGUE = "enquiry_amount_for_emi_catalogue"


    // kushal bank functions
    const val GENERIC_REVERSAL_KEY = "generic_reversal_key"

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


    fun clearReversal() {
       // logger(TAG, "========CLEAR Reversal=========", "e")
        sharedPreference?.edit()?.putString(REVERSAL_DATA, "")?.apply()
    }


    //region Below method is used to Save Batch File Data in App Preference:-
    fun saveBatchInPreference(batchList: MutableList<BatchFileDataTable?>?) {
        val tempBatchDataList = Gson().toJson(
            batchList,
            object : TypeToken<List<BatchFileDataTable>>() {}.type
        ) ?: ""
      //  saveString(PreferenceKeyConstant.LAST_BATCH.keyName, tempBatchDataList)
    }
    //endregion


}