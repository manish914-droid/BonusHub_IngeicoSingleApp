package com.bonushub.crdb.appupdate

import com.bonushub.crdb.HDFCApplication
import com.bonushub.crdb.R
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.utils.*
import com.bonushub.crdb.utils.ConnectionType
import com.bonushub.crdb.utils.Field48ResponseTimestamp.getTptData
import com.bonushub.crdb.utils.Nii
import com.bonushub.crdb.utils.ProcessingCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import javax.inject.Inject


class SendAppUpdateConfirmationPacket@Inject constructor(private var appDao: AppDao) : IAppUpdateConfirmationPacketExchange {

    init {
        createAppUpdateConfirmationPacket()
    }

    override fun createAppUpdateConfirmationPacket(): IsoDataWriter = IsoDataWriter().apply {
        val terminalData = runBlocking(Dispatchers.IO) { getTptData() }
        val baseTid = runBlocking(Dispatchers.IO) { getBaseTID(appDao) }
        if (terminalData != null) {
            mti = Mti.APP_UPDATE_MTI.mti

            //Processing Code Field 3
            addField(3, ProcessingCode.APP_UPDATE_CONFIRMATION.code)

            //STAN(ROC) Field 11
            addField(11, Utility().getROC().toString())

            //NII Field 24
            addField(24, Nii.DEFAULT.nii)

            //TID Field 41
            addFieldByHex(41, baseTid)

            //Connection Time Stamps Field 48
            addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

            //adding Field 61
            val version = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)
            val pcNumber = addPad(AppPreference.getString(PreferenceKeyConstant.PC_NUMBER_ONE.keyName), "0", 9)
            val data = ConnectionType.GPRS.code + addPad(deviceModel(), " ", 6, false) +
                    addPad(HDFCApplication.appContext.getString(R.string.app_name), " ", 10, false) +
                    version + addPad("0", "0", 9) + pcNumber

            //Adding Field 61:-
            addFieldByHex(61, data)

            //adding Field 63
            val deviceSerial = addPad(DeviceHelper.getDeviceSerialNo() ?: "", " ", 15, false)
            val bankCode = AppPreference.getBankCode()
            val f63 = "$deviceSerial$bankCode"
            addFieldByHex(63, f63)
            logger("Update confirmation REQ PACKET -->", this.isoMap, "e")
        }
    }
}