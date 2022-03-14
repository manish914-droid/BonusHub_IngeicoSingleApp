package com.bonushub.crdb.india.appupdate

import com.bonushub.crdb.india.HDFCApplication
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.db.AppDao
import com.bonushub.crdb.india.model.local.AppPreference
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.ConnectionType
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getTptData
import com.bonushub.crdb.india.utils.Nii
import com.bonushub.crdb.india.utils.ProcessingCode
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
            val pcNumber1 = addPad(AppPreference.getString(PreferenceKeyConstant.PC_NUMBER_ONE.keyName), "0", 9)
            val pcNumber2 = addPad(AppPreference.getString(PreferenceKeyConstant.PC_NUMBER_TWO.keyName), "0", 9)
            val data = ConnectionType.GPRS.code + addPad(deviceModel(), " ", 6, false) +
                    addPad(HDFCApplication.appContext.getString(R.string.app_name), " ", 10, false) +
                    version +  pcNumber1 + pcNumber2

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