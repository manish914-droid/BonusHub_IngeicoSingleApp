package com.bonushub.crdb.india.repository

import androidx.lifecycle.LiveData
import com.bonushub.crdb.india.entity.CardOption
import com.bonushub.crdb.india.entity.EMVOption
import com.bonushub.crdb.india.model.CardProcessedDataModal


interface SearchCardRepository {

    fun stopEmv()

    fun observeCardType(cardProcessedDataModal: CardProcessedDataModal, cardOption: CardOption): LiveData<CardProcessedDataModal>

    fun observemvEventHandler(emvOption: EMVOption, cardProcessedDataModal: CardProcessedDataModal) : LiveData<CardProcessedDataModal>
}