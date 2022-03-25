package com.bonushub.crdb.india.utils;

import com.usdk.apiservice.aidl.constants.RFDeviceName;
import com.usdk.apiservice.aidl.pinpad.DeviceName;


public class DemoConfig {
    public static final String TAG = "[DemoUSDK]";

    public static String PINPAD_DEVICE_NAME = DeviceName.IPP;
    public static String RF_DEVICE_NAME = RFDeviceName.INNER;
    public static boolean USDK_LOG_OPEN = true;
    public static int REGION_ID = 0;
    public static int KAP_NUM = 0;

    public static final int KEYID_MAIN = 15;
    public static final int KEYID_PIN = 16;
 //   public static final int KEYID_PIN = DemoConfig.KEYID_PIN;
    public static final int KEYID_DES = 17;
    public static final int KEYID_TRACK = 18;




}
