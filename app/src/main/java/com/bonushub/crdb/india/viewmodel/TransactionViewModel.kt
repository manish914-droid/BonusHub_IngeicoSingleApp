package com.bonushub.crdb.india.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import com.bonushub.crdb.india.repository.GenericResponse
import com.bonushub.crdb.india.repository.TransactionReprository
import com.bonushub.crdb.india.utils.IsoDataReader
import com.bonushub.crdb.india.utils.IsoDataWriter

class TransactionViewModel @ViewModelInject constructor(private val transactionReprository: TransactionReprository):
    ViewModel()  {

   suspend fun serverCall(transactionISOByteArray: IsoDataWriter): GenericResponse<IsoDataReader?> {
       // viewModelScope.launch(Dispatchers.IO) {
           return transactionReprository.getHostTransaction(transactionISOByteArray)
      // }
   }

}