/*
 * ===========================================================================================
 * = COPYRIGHT
 *          PAX Computer Technology(Shenzhen) CO., LTD PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or nondisclosure
 *   agreement with PAX Computer Technology(Shenzhen) CO., LTD and may not be copied or
 *   disclosed except in accordance with the terms in that agreement.
 *     Copyright (C) 2019-? PAX Computer Technology(Shenzhen) CO., LTD All rights reserved.
 * Description: // Detail description about the function of this module,
 *             // interfaces with the other modules, and dependencies.
 * Revision History:
 * Date                  Author	                 Action
 * 20190108  	         guanjw                  Create
 * ===========================================================================================
 */

package com.bonushub.crdb.utils;

import android.util.Log;

import com.bonushub.crdb.BuildConfig;


public class LogUtils {
    private static final boolean IS_DEBUG = BuildConfig.DEBUG;
    private LogUtils() {

    }

    public static void e(String tag, Object msg) {
        if (IS_DEBUG) {
            Log.e(tag, "" + msg);
        }
    }

    public static void w(String tag, Object msg) {
        if (IS_DEBUG) {
            Log.w(tag, "" + msg);
        }
    }

    public static void i(String tag, Object msg) {
        if (IS_DEBUG) {
            Log.i(tag, "" + msg);
        }
    }

    public static void d(String tag, Object msg) {
        if (IS_DEBUG) {
            Log.d(tag, "" + msg);
        }
    }

    public static void v(String tag, Object msg) {
        if (IS_DEBUG) {
            Log.v(tag, "" + msg);
        }
    }

    public static void e(String tag, String msg, Throwable th) {
        Log.e(tag, msg, th);
    }

    public static void e(String tag, Exception exception) {
        Log.e(tag, "", exception);
    }

    public static void w(String tag, String msg, Throwable th) {
        if (IS_DEBUG) {
            Log.w(tag, msg, th);
        }
    }

    public static void i(String tag, String msg, Throwable th) {
        if (IS_DEBUG) {
            Log.i(tag, msg, th);
        }
    }

    public static void d(String tag, String msg, Throwable th) {
        if (IS_DEBUG) {
            Log.d(tag, msg, th);
        }
    }

    public static void v(String tag, String msg, Throwable th) {
        if (IS_DEBUG) {
            Log.v(tag, msg, th);
        }
    }


}

