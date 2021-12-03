package com.bonushub.crdb.disputetransaction

import com.bonushub.pax.utils.IWriter

interface ISettlementPacketExchange {
    fun createSettlementISOPacket(): IWriter
}