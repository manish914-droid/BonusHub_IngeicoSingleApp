package com.bonushub.crdb.india.appupdate

import com.bonushub.crdb.india.utils.IWriter

interface IAppUpdateConfirmationPacketExchange {
    fun createAppUpdateConfirmationPacket(): IWriter
}