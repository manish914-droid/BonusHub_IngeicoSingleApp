
package com.bonushub.crdb.utils

import com.mindorks.example.coroutines.utils.Status


class RespMessageStatusData(var message: String="Unknown Error", var isSuccess: Boolean=false, var anyData:Any?=null)

class ResponseHandler(status: Status, message: String?, response: Boolean, response1: Boolean) {

}

class EmvHanndler(var message: String?)