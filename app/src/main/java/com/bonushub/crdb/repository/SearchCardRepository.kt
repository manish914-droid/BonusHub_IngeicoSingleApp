package com.bonushub.crdb.repository

import androidx.lifecycle.LiveData
import com.bonushub.crdb.entity.CardOption
import com.bonushub.crdb.entity.EMVOption
import com.bonushub.crdb.model.CardProcessedDataModal


interface SearchCardRepository {

    fun stopEmv()

    fun observeCardType(cardProcessedDataModal: CardProcessedDataModal, cardOption: CardOption): LiveData<CardProcessedDataModal>

    fun observemvEventHandler(emvOption: EMVOption, cardProcessedDataModal: CardProcessedDataModal) : LiveData<CardProcessedDataModal>
}