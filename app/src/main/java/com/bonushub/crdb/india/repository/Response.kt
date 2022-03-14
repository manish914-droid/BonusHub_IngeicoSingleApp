package com.bonushub.crdb.india.repository

import com.bonushub.crdb.india.utils.IsoDataReader

sealed class Response(val data: IsoDataReader?=null, val errorMessage:String?=null){
    class Loading:Response()
    class Success(isoDataReader: IsoDataReader):Response(data=isoDataReader)
    class Error(errorMsg: String):Response(errorMessage = errorMsg)
}

sealed class GenericResponse<T>(val data:T?=null,val errorMessage:String?=null){
    class Loading<T>:GenericResponse<T>()
    class Success<T>(data: T):GenericResponse<T>(data=data)
    class Error<T>(errorMsg: String):GenericResponse<T>(errorMessage = errorMsg)
}

