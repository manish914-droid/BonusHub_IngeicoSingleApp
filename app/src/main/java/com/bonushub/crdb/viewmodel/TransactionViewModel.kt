package com.bonushub.crdb.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import com.bonushub.crdb.repository.GenericResponse
import com.bonushub.crdb.repository.TransactionReprository
import com.bonushub.crdb.utils.IsoDataReader
import com.bonushub.crdb.utils.IsoDataWriter

class TransactionViewModel @ViewModelInject constructor(private val transactionReprository: TransactionReprository):
    ViewModel()  {

   suspend fun serverCall(transactionISOByteArray: IsoDataWriter): GenericResponse<IsoDataReader?> {
       // viewModelScope.launch(Dispatchers.IO) {
           return transactionReprository.getHostTransaction(transactionISOByteArray)
      // }
   }

}