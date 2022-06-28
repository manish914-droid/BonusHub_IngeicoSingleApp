package com.bonushub.crdb.india.serverApi

import android.util.Log
import com.bonushub.crdb.india.di.DBModule
import com.bonushub.crdb.india.model.local.AppPreference
import com.bonushub.crdb.india.model.local.TerminalParameterTable
import com.bonushub.crdb.india.repository.GenericResponse
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getTptData
import com.bonushub.crdb.india.vxutils.getEncryptedPanorTrackData
import com.bonushub.pax.utils.KeyExchanger
import com.google.gson.Gson
import javax.inject.Inject

const val bankEMIRequestCode = "4"

class RemoteService @Inject constructor(){
//:LiveData<GenericResponse<IsoDataReader>>

// region  ------ Service for getting brand data from server ------
    suspend fun field57GenericService(field57RequestData:String):GenericResponse<IsoDataReader?>{
       // val field57RequestData ="${EMIRequestType.BRAND_DATA.requestType}^$counter"
        val isoDataWriter = IsoPacketCreator.createIsoPacketWithF57(field57RequestData)
    Log.e("isopacket", Gson().toJson(isoDataWriter))
        val response = SocketHelper.getResponseFromServer(isoDataWriter)
       // val liveData= MutableLiveData<GenericResponse<IsoDataReader>>()
        return if(response.isSuccess){
                val isoDataReader=response.anyData as IsoDataReader
          if(isoDataReader.isoMap[39]?.rawData?.hexStr2ByteArr()?.byteArr2Str() == "00")
              GenericResponse.Success(response.anyData as IsoDataReader)
            else
            GenericResponse.Error(response.message)
        }else{
            GenericResponse.Error(response.message)
        }
    }
// endregion

    suspend fun getEMITenureService(pan:String,field57RequestData: String):GenericResponse<IsoDataReader?>{
        // val field57RequestData ="${EMIRequestType.BRAND_DATA.requestType}^$counter"
        val isoDataWriter = IsoPacketCreator.createGetTenureIso(pan,field57RequestData)
        val response = SocketHelper.getResponseFromServer(isoDataWriter)
        // val liveData= MutableLiveData<GenericResponse<IsoDataReader>>()
        return if(response.isSuccess){
            val isoDataReader=response.anyData as IsoDataReader
            if(isoDataReader.isoMap[39]?.rawData?.hexStr2ByteArr()?.byteArr2Str() == "00") {
                GenericResponse.Success(response.anyData as IsoDataReader)
            }
            else
                GenericResponse.Error(response.message)
        }else
            GenericResponse.Error(response.message)
    }

    suspend fun getHostTransaction(transactionISOByteArray: IsoDataWriter):GenericResponse<IsoDataReader?>{
        // val field57RequestData ="${EMIRequestType.BRAND_DATA.requestType}^$counter"
     //   Log.e("isopacket", Gson().toJson(transactionISOByteArray))
        val response = SocketHelper.getResponseFromServer(transactionISOByteArray)
        // val liveData= MutableLiveData<GenericResponse<IsoDataReader>>()
        return if(response.isSuccess){
            val isoDataReader=response.anyData as IsoDataReader
            if(isoDataReader.isoMap[39]?.rawData?.hexStr2ByteArr()?.byteArr2Str() == "00")
                GenericResponse.Success(response.anyData as IsoDataReader)
            else
                GenericResponse.Error(response.message)
        }else{
            GenericResponse.Error(response.message)
        }
    }

}


object IsoPacketCreator{

    suspend fun createIsoPacketWithF57(field57RequestData: String)
            : IsoDataWriter =
        IsoDataWriter().apply {
            val terminalData: TerminalParameterTable? = (getTptData())
            if (terminalData != null) {
                mti = Mti.EIGHT_HUNDRED_MTI.mti

                //Processing Code Field 3
                addField(3, ProcessingCode.BRAND_EMI.code)

                //STAN(ROC) Field 11
                val stan =
                    paddingInvoiceRoc(DBModule.appDatabase.appDao.getRoc())?.let { addField(11, it) }
                //   addField(11, ROCProviderV2.getRoc(AppPreference.getBankCode()).toString())
                addField(11, "000012")

                //NII Field 24
                addField(24, Nii.BRAND_EMI_MASTER.nii)

                //TID Field 41
                //terminalData.terminalId?.get(0)?.let { addFieldByHex(41,  it.toString()) }
               // addFieldByHex(41, getBaseTID(DBModule.appDatabase.appDao))
                addFieldByHex(41, terminalData.terminalId)
                Log.d("terminalId:- ", terminalData.terminalId.toString())

                //adding field 57
                addFieldByHex(57, field57RequestData)

                //adding Field 61
                addFieldByHex(61, KeyExchanger.getF61())

                //adding field 63
                val deviceSerial =    addPad(
                    DeviceHelper.getDeviceSerialNo() ?: "",
                    " ",
                    15,
                    false
                )
                val bankCode = AppPreference.getBankCode()
                val f63 = "$deviceSerial$bankCode"
                addFieldByHex(63, f63)
            }
        }

    //region=========================BankEMI ISO Request Packet===============================
     suspend fun createGetTenureIso(pan:String,field57RequestData: String): IsoDataWriter = IsoDataWriter().apply {
        val terminalData: TerminalParameterTable? = (getTptData())
        if (terminalData != null) {
            mti = Mti.EIGHT_HUNDRED_MTI.mti

            //Processing Code Field 3
            addField(3, ProcessingCode.BANK_EMI.code)

            //STAN(ROC) Field 11
            val stan =
                paddingInvoiceRoc(DBModule.appDatabase.appDao.getRoc())?.let { addField(11, it) }
            //   addField(11, ROCProviderV2.getRoc(AppPreference.getBankCode()).toString())
            addField(11, "000013")

            //NII Field 24
            addField(24, Nii.BRAND_EMI_MASTER.nii)

            //TID Field 41
            //addFieldByHex(41, getBaseTID(DBModule.appDatabase.appDao))
            addFieldByHex(41, terminalData.terminalId)

            //This is for bankemi/insta emi/brand emi
            //New field 56 added by Manish Kumar for getting tenure
            //adding Field 56

            addField56(56, getEncryptedPanorTrackData(pan,false))
       //    addField56(56, getEncryptedPan(cardBinValue))

            //adding Field 56
            //addFieldByHex(56, getEncryptedPan(cardBinValue))

            //adding Field 57
          //  addFieldByHex(57, field57Request ?: "")
            Log.e("field57RequestData",""+field57RequestData)
            addFieldByHex(57, field57RequestData)

            //adding Field 61
            addFieldByHex(61,  KeyExchanger.getF61())
        //    addFieldByHex(61,  "33583939302A2A426F6E7573487562202030322E30312E30312E323130313035303030313535373133303030303030303030")
            /*val issuerParameterTable = IssuerParameterTable.selectFromIssuerParameterTable(AppPreference.WALLET_ISSUER_ID)
            issuerParameterTable?.let { getField61(it, terminalData.tidBankCode, Mti.BANK_EMI.mti) }
                ?.let { addFieldByHex(61, it) }*/

            //adding Field 63
            val deviceSerial = addPad(
                DeviceHelper.getDeviceSerialNo() ?: "",
                " ",
                15,
                false
            )
            val bankCode = AppPreference.getBankCode()
            val f63 = "$deviceSerial$bankCode"
            addFieldByHex(63, f63)
            //    addFieldByHex(63, "5631453032343231373720202020203031")  addFieldByHex(63, "5631453032343231373720202020203031")
        }
    }
    //endregion

}





