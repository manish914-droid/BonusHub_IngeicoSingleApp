package com.bonushub.crdb.repository

import com.bonushub.pax.utils.IsoDataReader

sealed class Response(val data:IsoDataReader?=null,val errorMessage:String?=null){
    class Loading:Response()
    class Success(isoDataReader: IsoDataReader):Response(data=isoDataReader)
    class Error(errorMsg: String):Response(errorMessage = errorMsg)


}

sealed class GenericResponse<T>(val data:T?=null,val errorMessage:String?=null){
    class Loading<T>:GenericResponse<T>()
    class Success<T>(isoDataReader: T):GenericResponse<T>(data=isoDataReader)
    class Error<T>(errorMsg: String):GenericResponse<T>(errorMessage = errorMsg)
}

