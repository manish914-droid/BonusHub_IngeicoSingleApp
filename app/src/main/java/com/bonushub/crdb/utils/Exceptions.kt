
package com.bonushub.crdb.utils


class RespMessageStatusData(var message: String="Unknown Error", var isSuccess: Boolean=false, var anyData:Any?=null)

class ResponseHandler(message: String?, response: Boolean, response1: Boolean, response2: Boolean) {

    operator fun invoke(result: String, success: Boolean, b: Boolean, b1: Boolean): ResponseHandler {

        return ResponseHandler(result,success,b,b1)
    }
}