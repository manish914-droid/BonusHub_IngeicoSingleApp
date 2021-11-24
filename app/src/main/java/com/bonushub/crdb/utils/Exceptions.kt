
package com.bonushub.crdb.utils

import com.mindorks.example.coroutines.utils.Status


class RespMessageStatusData(var message: String="Unknown Error", var isSuccess: Boolean=false, var anyData:Any?=null)

class ResponseHandler(var status: Status, var message: String?, var response: Boolean, var data: Any? =null)

class EmvHanndler(var message: String?)