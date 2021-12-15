package com.bonushub.crdb.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonushub.crdb.repository.TransactionReprository
import com.bonushub.pax.utils.IsoDataWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TransactionViewModel @ViewModelInject constructor(private val transactionReprository: TransactionReprository):
    ViewModel()  {

    fun serverCall(transactionISOByteArray: IsoDataWriter){
        viewModelScope.launch(Dispatchers.IO) {
           transactionReprository.getHostTransaction(transactionISOByteArray)
       }
   }

}