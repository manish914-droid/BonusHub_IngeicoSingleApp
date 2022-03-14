package com.bonushub.crdb.india.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonushub.crdb.india.entity.CardOption
import com.bonushub.crdb.india.entity.EMVOption
import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.repository.SearchCardRepository

import kotlinx.coroutines.launch


class SearchViewModel @ViewModelInject constructor(
    private val repository: SearchCardRepository) : ViewModel() {

    var allcadType = MutableLiveData<CardProcessedDataModal>()
    var cardTpeData = MutableLiveData<CardProcessedDataModal>()

    override fun onCleared() {
        repository.stopEmv()
        super.onCleared()
    }

    fun fetchCardTypeData(cardProcessedDataModal: CardProcessedDataModal, cardOption: CardOption) {
        viewModelScope.launch {
            allcadType = repository.observeCardType(cardProcessedDataModal, cardOption) as MutableLiveData<CardProcessedDataModal>

        }
    }

    fun fetchCardPanData(){
        viewModelScope.launch {
            cardTpeData = repository.observemvEventHandler(EMVOption.create(), CardProcessedDataModal()) as MutableLiveData<CardProcessedDataModal>
        }
      }
    }













