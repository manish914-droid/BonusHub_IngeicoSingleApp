package com.bonushub.crdb.entity;

import android.os.Bundle;

import com.usdk.apiservice.aidl.emv.EMVData;

/**
 * EMV startEMV的入参
 */
public class EMVOption {

    private Bundle option = new Bundle();

    private EMVOption(){}

    public static EMVOption create() {
        return new EMVOption();
    }

    /**
     * 应用选择方式
     * <br> 0 - PSE selection first and then AID selection;
     * <br> 1 - Only PSE selection;
     * <br> 2 - Only AID selection;
     * <br> 3 - Only PPSE selection;
     * <br> 4 - PPSE First, AID selection Second (Discover ZIP Mode)
     */
    public EMVOption flagPSE(byte flagPSE) {
        option.putByte(EMVData.FLAG_PSE, flagPSE);
        return this;
    }

    /** 闪卡恢复流程, 0:正常交易(默认); 1:单笔闪卡恢复流程; 2:全局闪卡恢复流程 */
    public EMVOption flagRecovery(byte flagRecovery) {
        option.putByte(EMVData.FLAG_RECOVERY, flagRecovery);
        return this;
    }

    /** 是否执行查询IC卡日志交易 */
    public EMVOption setFlagICCLog(boolean flagICCLog) {
        option.putBoolean(EMVData.FLAG_ICC_LOG, flagICCLog);
        return this;
    }

    /** 非接交易 应用选择回调 是否执行标志, 0:表示内核不用调用应用选择回调; 1:表示内核要调用应用选择回调 */
    public EMVOption flagCtlAsCb(byte flagCtlAsCb) {
        option.putByte(EMVData.FLAG_CTL_AS_CB, flagCtlAsCb);
        return this;
    }

    /** 是否需要执行发卡行脚本, 0:否(默认); 1: 是。 当联机处理后发卡行有返回 71 或 72 脚本数据时，需要设置为1 */
    public EMVOption flagExecuteIssuerScript(byte flagExecuteIssuerScript) {
        option.putByte(EMVData.FLAG_EXECUTE_ISSUER_SCRIPT, flagExecuteIssuerScript);
        return this;
    }

    public Bundle toBundle() {
        return option;
    }
}
