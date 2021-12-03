package com.bonushub.crdb.utils

import com.mindorks.example.coroutines.utils.Status

/**
 * Generic class for holding success response, error response and loading status
 */
data class Result<out T>(val status: Status, val data: T?, val error: String?, val message: String?) {

   /* enum class Status {
        SUCCESS,
        ERROR,
        LOADING
    }*/

    companion object {
        fun <T> success(data: T?): Result<T> {
            return Result(Status.SUCCESS, data, null, null)
        }

        fun <T> error(data: T?, error: String?): Result<T> {
            return Result(Status.ERROR, data, error, null)
        }

        fun <T> loading(data: T? = null): Result<T> {
            return Result(Status.LOADING, data, null, null)
        }
    }

    override fun toString(): String {
        return "Result(status=$status, data=$data, error=$error, message=$message)"
    }
}