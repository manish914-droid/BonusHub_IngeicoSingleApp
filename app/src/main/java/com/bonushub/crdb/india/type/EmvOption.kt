package com.bonushub.crdb.india.type

import android.os.Bundle
import com.usdk.apiservice.aidl.emv.EMVData

class EmvOption {
    private val option = Bundle()

    companion object {
        fun create(): EmvOption {
            return EmvOption()
        }
    }

    /**
     *
     * <br></br> 0 - PSE selection first and then AID selection;
     * <br></br> 1 - Only PSE selection;
     * <br></br> 2 - Only AID selection;
     * <br></br> 3 - Only PPSE selection;
     * <br></br> 4 - PPSE First, AID selection Second (Discover ZIP Mode)
     */
    fun flagPSE(flagPSE: Byte): EmvOption? {
        option.putByte(EMVData.FLAG_PSE, flagPSE)
        return this
    }

    fun flagRecovery(flagRecovery: Byte): EmvOption? {
        option.putByte(EMVData.FLAG_RECOVERY, flagRecovery)
        return this
    }

    fun setFlagICCLog(flagICCLog: Boolean): EmvOption? {
        option.putBoolean(EMVData.FLAG_ICC_LOG, flagICCLog)
        return this
    }

    fun flagCtlAsCb(flagCtlAsCb: Byte): EmvOption? {
        option.putByte(EMVData.FLAG_CTL_AS_CB, flagCtlAsCb)
        return this
    }

    fun flagExecuteIssuerScript(flagExecuteIssuerScript: Byte): EmvOption? {
        option.putByte(EMVData.FLAG_EXECUTE_ISSUER_SCRIPT, flagExecuteIssuerScript)
        return this
    }

    fun toBundle(): Bundle? {
        return option
    }
}