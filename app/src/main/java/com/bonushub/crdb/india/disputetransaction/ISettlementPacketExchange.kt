package com.bonushub.crdb.india.disputetransaction

import com.bonushub.crdb.india.utils.IWriter

interface ISettlementPacketExchange {
    fun createSettlementISOPacket(): IWriter
}