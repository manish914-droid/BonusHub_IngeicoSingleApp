package com.bonushub.crdb.serverApi

import android.util.Log
import com.bonushub.crdb.di.DBModule
import com.bonushub.crdb.model.local.TerminalParameterTable
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.repository.GenericResponse
import com.bonushub.crdb.utils.*
import com.bonushub.pax.utils.*

class RemoteService {
//:LiveData<GenericResponse<IsoDataReader>>

// region  ------ Service for getting brand data from server ------
    suspend fun field57GenericService(field57RequestData:String):GenericResponse<IsoDataReader?>{
       // val field57RequestData ="${EMIRequestType.BRAND_DATA.requestType}^$counter"
        val isoDataWriter = IsoPacketCreator.createIsoPacketWithF57(field57RequestData)
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
            val terminalData: TerminalParameterTable? = (Utility().getTptData())
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
                addFieldByHex(41,  terminalData.terminalId.toString())
                Log.d("terminalId:- ", terminalData.terminalId.toString())

                //adding field 57
                addFieldByHex(57, field57RequestData)

                //adding Field 61
                addFieldByHex(61, KeyExchanger.getF61())

                //adding field 63
                val deviceSerial = addPad(AppPreference.getString("serialNumber"), " ", 15, false)
                val bankCode = AppPreference.getBankCode()
                val f63 = "$deviceSerial$bankCode"
                addFieldByHex(63, f63)
            }
        }

    //region=========================BankEMI ISO Request Packet===============================
     suspend fun createGetTenureIso(pan:String,field57RequestData: String): IsoDataWriter = IsoDataWriter().apply {
        val terminalData: TerminalParameterTable? = (Utility().getTptData())
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
            addFieldByHex(41, /*terminalData.terminalId*/"41501370")

            //This is for bankemi/insta emi/brand emi
            //New field 56 added by Manish Kumar for getting tenure
            //adding Field 56

            addField56(56, pan)
       //    addField56(56, getEncryptedPan(cardBinValue))

            //adding Field 56
            //addFieldByHex(56, getEncryptedPan(cardBinValue))

            //adding Field 57
          //  addFieldByHex(57, field57Request ?: "")
            addFieldByHex(57, "4^0^7^2696^h^^589000" ?: "")

            //adding Field 61
            addFieldByHex(61,  KeyExchanger.getF61())
        //    addFieldByHex(61,  "33583939302A2A426F6E7573487562202030322E30312E30312E323130313035303030313535373133303030303030303030")
            /*val issuerParameterTable = IssuerParameterTable.selectFromIssuerParameterTable(AppPreference.WALLET_ISSUER_ID)
            issuerParameterTable?.let { getField61(it, terminalData.tidBankCode, Mti.BANK_EMI.mti) }
                ?.let { addFieldByHex(61, it) }*/

            //adding Field 63
            val deviceSerial = addPad(AppPreference.getString("serialNumber"), " ", 15, false)
            val bankCode = AppPreference.getBankCode()
            val f63 = "$deviceSerial$bankCode"
            addFieldByHex(63, f63)
        //    addFieldByHex(63, "5631453032343231373720202020203031")
        }
    }
    //endregion


}





