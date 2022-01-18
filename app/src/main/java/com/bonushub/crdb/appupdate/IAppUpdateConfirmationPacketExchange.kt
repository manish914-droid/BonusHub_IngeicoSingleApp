package com.bonushub.crdb.appupdate

import com.bonushub.crdb.utils.IWriter

interface IAppUpdateConfirmationPacketExchange {
    fun createAppUpdateConfirmationPacket(): IWriter
}