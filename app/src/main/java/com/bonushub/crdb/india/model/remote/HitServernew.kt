

import android.content.Context.MODE_PRIVATE
import android.util.Log
import com.bonushub.crdb.india.HDFCApplication
import com.bonushub.crdb.india.model.local.TerminalCommunicationTable
import com.bonushub.crdb.india.serverApi.ProgressCallback
import com.bonushub.crdb.india.serverApi.ServerMessageCallback
import com.bonushub.crdb.india.serverApi.ServerMessageCallbackSale
import com.bonushub.crdb.india.utils.*
import com.bonushub.pax.utils.*
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getF48TimeStamp
import java.io.DataInputStream
import java.io.FileOutputStream
import java.net.Socket
import java.nio.channels.ServerSocketChannel

/*val LYRA_IP_ADDRESS = "192.168.250.10"
var PORT2 = 4124
val NEW_IP_ADDRESS = "122.176.84.29"
var PORT = 8101//4124*/

object HitServernew  {

    private val TAG = HitServernew::class.java.simpleName
    private var tct: TerminalCommunicationTable?= null
    private var callback: ServerMessageCallback? = null
    private var callbackSale: ServerMessageCallbackSale? = null


    @Synchronized
    suspend fun hitServer(data: IWriter, needReversalSaved:Boolean=false):RespMessageStatusData{
        try {
            if (Utility().checkInternetConnection()) {
                with(Utility.ConnectionTimeStamps) {
                    reset()
                    dialStart = getF48TimeStamp()
                }
                val socketConnectionStatusRespMessageStatusData: RespMessageStatusData = getSocket()
                if (socketConnectionStatusRespMessageStatusData.isSuccess){
                    val socket=socketConnectionStatusRespMessageStatusData.anyData as Socket
                    if (needReversalSaved) {
                        // Todo Reversal Saved Here
                    Log.i("Reversal", "Reversal Saved Here ")
                    }
                Utility().logger(TAG, "address = ${socket.inetAddress}, port = ${socket.port}", "e")
                Utility.ConnectionTimeStamps.dialConnected = getF48TimeStamp()

                val byteData = data.generateIsoByteRequest()
                Utility().logger(TAG, "Data Send = ${byteData.byteArr2HexStr()}")
                Utility.ConnectionTimeStamps.startTransaction = getF48TimeStamp()
                val sos = socket.getOutputStream()
                sos?.write(byteData)
                sos.flush()
                val dis = DataInputStream(socket.getInputStream())
                val len = dis.readShort().toInt()
                val response = ByteArray(len)
                dis.readFully(response)
                Utility.ConnectionTimeStamps.recieveTransaction = getF48TimeStamp()

                val responseStr = response.byteArr2HexStr()
                val reader = readIso(responseStr, false)
                Field48ResponseTimestamp.saveF48IdentifierAndTxnDate(reader.isoMap[48]?.parseRaw2String() ?: "")

                Utility().logger(TAG, "len=$len, data = $responseStr")
                socket.close()
                    val isoReader = readIso(responseStr)
                    Utility().logger(KeyExchanger.TAG, isoReader.isoMap)
                    val respMsg = isoReader.isoMap[58]?.parseRaw2String() ?: ""
                    return if( isoReader.isoMap[39]?.rawData?.hexStr2ByteArr()?.byteArr2Str() == "00")
                        RespMessageStatusData(respMsg, true,isoReader)
                    else RespMessageStatusData(respMsg, false)
            }
                else{
                  return RespMessageStatusData(socketConnectionStatusRespMessageStatusData.message, socketConnectionStatusRespMessageStatusData.isSuccess)
                }
            } else {
                return RespMessageStatusData("No internet", false)
            }

        } catch (ex: Exception) {
            Utility().logger("EXCEPTION","NO RESPONSE...","e")
            return RespMessageStatusData(ex.message.toString(), false)
        }
    }

    @Synchronized
    suspend fun hitInitServer(callback: ServerMessageCallback, progressMsg: ProgressCallback, keInit: IKeyExchangeInit) {
        this@HitServernew.callback = callback
        val FILE_NAME = "init_packet_request_logs.txt"
        val fos : FileOutputStream = HDFCApplication.appContext.openFileOutput(FILE_NAME, MODE_PRIVATE)
        try {
//VerifoneApp.internetConnection
            if (true) {
                with(Utility.ConnectionTimeStamps) {
                    reset()
                    dialStart = getF48TimeStamp()
                }
               val socketStatus= getSocket()
              if(socketStatus.isSuccess)  {
                  val socket=socketStatus.anyData as Socket
                    Utility().logger(TAG, "address = ${socket.inetAddress}, port = ${socket.port}", "e")

                    var nextCounter = ""

                    var isFirstCall = true
                    val initList = ArrayList<ByteArray>()
                    while (true) {
                        val data =
                            keInit.createInitIso(nextCounter, isFirstCall).generateIsoByteRequest()
                        val formattedInitPackets = data.byteArr2HexStr()
                        Utility().logger(TAG, "init iso = $formattedInitPackets")
                        //println("Init iso packet send --- > $formattedInitPackets")
                        Utility.ConnectionTimeStamps.dialConnected = getF48TimeStamp()
                        Utility.ConnectionTimeStamps.startTransaction = getF48TimeStamp()
                        progressMsg("Please wait sending data to Bonushub server")
                        val sos = socket.getOutputStream()
                        sos?.write(data)
                        sos.flush()

                        progressMsg("Please wait receiving data from Bonushub server")
                        val dis = DataInputStream(socket.getInputStream())
                        val len = dis.readShort().toInt()
                        val response = ByteArray(len)
                        dis.readFully(response)
                        Utility.ConnectionTimeStamps.recieveTransaction = getF48TimeStamp()
                        val responseStr = response.byteArr2HexStr()
                        //println("Init iso packet Recieve --- > $formattedInitPackets")
                        Utility().logger(TAG, "len=$len, data = $responseStr")
                        /* writeInitPacketLog(
                             "$formattedInitPackets||",
                             "$responseStr||",
                             fos,
                             FILE_NAME
                         )*/

                        val reader = readIso(responseStr)


                        val roc = reader.isoMap[11]
                        if (roc != null)
                            Utility().incrementRoc()
                        /*ROCProviderV2.incrementFromResponse(
                            roc.rawData,
                            AppPreference.HDFC_BANK_CODE
                        ) else ROCProviderV2.increment(AppPreference.HDFC_BANK_CODE)*/

                        if (reader.isoMap[39]?.parseRaw2String() == "00") {

                            val f48 = reader.isoMap[48]
                            if (f48 != null) Utility.ConnectionTimeStamps.saveStamp(f48.parseRaw2String())

                            val f60 = reader.isoMap[60]

                            if (f60 != null) {
                                val f60Arr = f60.rawData.hexStr2ByteArr()

                                nextCounter = f60Arr.sliceArray(4..17).byteArr2Str()
                                isFirstCall = false

                                Utility().logger(TAG, "nextCounter = $nextCounter")

                                val f60Str = f60Arr.sliceArray(48..f60Arr.lastIndex)

                                initList.add(f60Str)

                                Utility().logger(TAG, f60Str.byteArr2Str())
                            }
                            val pCode = reader.isoMap[3]?.rawData ?: ""
                            Utility().logger(TAG, "Processing code $pCode")
                            if (pCode != ProcessingCode.INIT_MORE.code) {
                                Utility().readInitServer(initList) { result, message ->
                                    callback(message, result)
                                }
                                break
                            }
                        } else {
                            callback(reader.isoMap[58]?.parseRaw2String() ?: "", false)
                            break
                        }

                    }
                    socket.close()
                    fos.close()
                    Utility().resetRoc()
                    // ROCProviderV2.resetRoc(AppPreference.getBankCode())
                    this@HitServernew.callback = null
                }else{


                }

            } else {
                callback("Offline, No Internet available", false)
                this@HitServernew.callback = null
            }

        } catch (ex: Exception) {
            callback(ex.message ?: "Connection Error", false)
            this@HitServernew.callback = null
        }
    }


    suspend fun getSocket():RespMessageStatusData{
        Log.d("Getting Socket:- " , "Socket Started Here.....")
        try {
            tct = Utility().getTctData()// always get tct it may get refresh meanwhile
            if (tct != null) {
                val sAddress = Utility().getIpPort()
                ServerSocketChannel.open().apply {
                    configureBlocking(false)
                }
                val socket = Socket()
                val connTimeOut = try {
                    (tct as TerminalCommunicationTable).connectTimeOut.toInt() * 1000
                } catch (ex: Exception) {
                    30 * 1000
                }
                val resTimeOut = try {
                    (tct as TerminalCommunicationTable).responseTimeOut.toInt() * 1000
                } catch (ex: Exception) {
                    30 * 1000
                }
                socket.connect(sAddress, connTimeOut)//
                socket.soTimeout = resTimeOut
                return RespMessageStatusData(isSuccess = true,anyData = socket)
            } else {
                return RespMessageStatusData("No Comm Data Found",isSuccess = false)
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
            Utility().logger("EXCEPTION","SOCKET NOT CONNECTED","e")
            return RespMessageStatusData(ex.message.toString(),isSuccess = false)
        } finally {
            Log.d("Finally Call:- ", "Final Block Runs Here.....")
        }
    }



}

/**
 * This class is used to communicate with server for multiple byte request and response
 * call open only once,
 * call close only once and in the end
 * use send function to send the data
 * */
