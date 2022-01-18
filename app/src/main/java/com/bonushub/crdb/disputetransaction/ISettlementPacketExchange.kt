package com.bonushub.crdb.disputetransaction

import com.bonushub.crdb.utils.IWriter

interface ISettlementPacketExchange {
    fun createSettlementISOPacket(): IWriter
}