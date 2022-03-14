package com.bonushub.crdb.india.utils

import android.util.Log
import com.bonushub.crdb.india.BuildConfig
import java.lang.Exception
import java.util.HashMap


class LogUtils private constructor() {


    companion object {
        private val IS_DEBUG = BuildConfig.DEBUG
        fun e(tag: String?, msg: Any) {
            if (IS_DEBUG) {
                Log.e(tag, "" + msg)
            }
        }

        fun w(tag: String?, msg: Any) {
            if (IS_DEBUG) {
                Log.w(tag, "" + msg)
            }
        }

        fun i(tag: String?, msg: Any) {
            if (IS_DEBUG) {
                Log.i(tag, "" + msg)
            }
        }

        fun d(tag: String?, msg: Any) {
            if (IS_DEBUG) {
                Log.d(tag, "" + msg)
            }
        }

        fun v(tag: String?, msg: Any) {
            if (IS_DEBUG) {
                Log.v(tag, "" + msg)
            }
        }

        fun e(tag: String?, msg: String?, th: Throwable?) {
            Log.e(tag, msg, th)
        }

        fun e(tag: String?, exception: Exception?) {
            Log.e(tag, "", exception)
        }

        fun w(tag: String?, msg: String?, th: Throwable?) {
            if (IS_DEBUG) {
                Log.w(tag, msg, th)
            }
        }

        fun i(tag: String?, msg: String?, th: Throwable?) {
            if (IS_DEBUG) {
                Log.i(tag, msg, th)
            }
        }

        fun d(tag: String?, msg: String?, th: Throwable?) {
            if (IS_DEBUG) {
                Log.d(tag, msg, th)
            }
        }

        fun v(tag: String?, msg: String?, th: Throwable?) {
            if (IS_DEBUG) {
                Log.v(tag, msg, th)
            }
        }
    }
}

// For logging
fun logISOReader(tag: String, msg: HashMap<Byte, IsoField>, type: String = "d") {
    if (BuildConfig.DEBUG) {
        for ((k, v) in msg) {
            logger(v.fieldName + "---->>", "$k = ${v.rawData}", type)
        }
    }
}

fun logger(tag: String, msg: String, type: String = "d") {
    if (BuildConfig.DEBUG) {
        when (type) {
            "d", "D" -> Log.d(tag, msg)
            "i", "I" -> Log.i(tag, msg)
            "e", "E" -> Log.e(tag, msg)
            "v", "V" -> Log.v(tag, msg)
            else -> Log.i(tag, msg)
        }
    }
}

// For logging
fun logger(tag: String, msg: HashMap<Byte, IsoField>, type: String = "d") {
    if (BuildConfig.DEBUG) {
        for ((k, v) in msg) {
            logger(v.fieldName + "---->>", "$k = ${v.rawData}", type)
        }
    }
}

