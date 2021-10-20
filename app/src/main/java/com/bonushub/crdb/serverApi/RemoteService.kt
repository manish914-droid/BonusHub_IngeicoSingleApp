package com.bonushub.crdb.serverApi

import com.bonushub.crdb.di.DBModule
import com.bonushub.crdb.model.TerminalParameterTable
import com.bonushub.crdb.model.local.AppPreference
import com.bonushub.crdb.repository.GenericResponse
import com.bonushub.crdb.utils.*
import com.bonushub.pax.utils.*

class RemoteService {
//:LiveData<GenericResponse<IsoDataReader>>

// region  ------ Service for getting brand data from server ------
    suspend fun getBrandDataService(counter:String):GenericResponse<IsoDataReader?>{
        val field57RequestData ="${EMIRequestType.BRAND_DATA.requestType}^$counter"
        val isoDataWriter = IsoPacketCreator.createBrandDataIsoPacket(field57RequestData)
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

}


object IsoPacketCreator{
    fun createBrandDataIsoPacket(field57RequestData: String)
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
                addFieldByHex(41, terminalData.terminalId)

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


}





