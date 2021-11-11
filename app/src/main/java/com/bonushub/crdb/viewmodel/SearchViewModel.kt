package com.bonushub.crdb.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonushub.crdb.entity.CardOption
import com.bonushub.crdb.entity.EMVOption
import com.bonushub.crdb.model.CardProcessedDataModal
import com.bonushub.crdb.repository.SearchCardRepository

import kotlinx.coroutines.launch


class SearchViewModel @ViewModelInject constructor(
    private val repository: SearchCardRepository) : ViewModel() {

    var allcadType = MutableLiveData<CardProcessedDataModal>()
    var cardTpeData = MutableLiveData<CardProcessedDataModal>()

    override fun onCleared() {
        repository.stopEmv()
        super.onCleared()
    }

    fun fetchCardTypeData(){
        viewModelScope.launch {
            allcadType = repository.observeCardType(CardProcessedDataModal(), CardOption.create()) as MutableLiveData<CardProcessedDataModal>

        }
    }

    fun fetchCardPanData(){
        viewModelScope.launch {
            cardTpeData = repository.observemvEventHandler(EMVOption.create(), CardProcessedDataModal()) as MutableLiveData<CardProcessedDataModal>
        }
      }
    }













