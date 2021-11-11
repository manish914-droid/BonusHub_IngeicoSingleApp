package com.bonushub.crdb.serverApi
import android.content.Context.MODE_PRIVATE
import android.util.Log
import com.bonushub.crdb.HDFCApplication
import com.bonushub.crdb.model.local.TerminalCommunicationTable
import com.bonushub.crdb.repository.keyexchangeDataSource
import com.bonushub.crdb.utils.*
import com.bonushub.pax.utils.*
import com.bonushub.crdb.utils.Field48ResponseTimestamp.getF48TimeStamp
import java.io.DataInputStream
import java.io.FileOutputStream
import java.net.Socket
import java.nio.channels.ServerSocketChannel

val LYRA_IP_ADDRESS = "192.168.250.10"
var PORT2 = 4124
val NEW_IP_ADDRESS = "122.176.84.29"
var PORT = 8101//4124

object HitServer  {

    val TAG = HitServer::class.java.simpleName
    private var tct: TerminalCommunicationTable?= null
    private var callback: ServerMessageCallback? = null
    private var callbackSale: ServerMessageCallbackSale? = null
    var reversalToBeSaved:IsoDataWriter?=null

    @Synchronized
    suspend fun hitServer(data: ByteArray, callback: ServerMessageCallback, progressMsg: ProgressCallback){
        this@HitServer.callback = callback
        try {
            if (Utility().checkInternetConnection()) {
                with(Utility.ConnectionTimeStamps) {
                    reset()
                    dialStart = getF48TimeStamp()
                }
                openSocket { socket ->
                    Utility().logger(TAG, "address = ${socket.inetAddress}, port = ${socket.port}", "e")
                    Utility.ConnectionTimeStamps.dialConnected = getF48TimeStamp()
                    progressMsg("Please wait sending data to Bonushub server")
                    Utility().logger(TAG, "Data Send = ${data.byteArr2HexStr()}")
                    Utility.ConnectionTimeStamps.startTransaction = getF48TimeStamp()
                    val sos = socket.getOutputStream()
                    sos?.write(data)
                    sos.flush()
                    if (reversalToBeSaved != null) {
                       // AppPreference.saveReversal(reversalToBeSaved!!)
                    }
                    progressMsg("Please wait receiving data from Bonushub server")
                    val dis = DataInputStream(socket.getInputStream())
                    val len = dis.readShort().toInt()
                    val response = ByteArray(len)
                    dis.readFully(response)
                    Utility.ConnectionTimeStamps.recieveTransaction = getF48TimeStamp()

                    val responseStr = response.byteArr2HexStr()
                    val reader = readIso(responseStr,false)
                    Field48ResponseTimestamp.saveF48IdentifierAndTxnDate(reader.isoMap[48]?.parseRaw2String()?:"")

                    Utility().logger(TAG, "len=$len, data = $responseStr")
                    socket.close()
                    //
                    callback(responseStr, true)
                    this@HitServer.callback = null
                }

            } else {
                callback("No internet", false)
                this@HitServer.callback = null
            }

        } catch (ex: Exception) {
            callback("NO RESPONSE...", false)
            Utility().logger("EXCEPTION","NO RESPONSE...","e")
            this@HitServer.callback = null
        }
    }

    @Synchronized
    suspend fun hitInitServer(callback: ServerMessageCallback, progressMsg: ProgressCallback, keInit: keyexchangeDataSource, tid: String) {
        this@HitServer.callback = callback
        val FILE_NAME = "init_packet_request_logs.txt"
        val fos : FileOutputStream = HDFCApplication.appContext.openFileOutput(FILE_NAME, MODE_PRIVATE)
        try {
//VerifoneApp.internetConnection
            if (true) {
                with(Utility.ConnectionTimeStamps) {
                    reset()
                    dialStart = getF48TimeStamp()
                }
                openSocket { socket ->

                    Utility().logger(TAG, "address = ${socket.inetAddress}, port = ${socket.port}", "e")

                    var nextCounter = ""

                    var isFirstCall = true
                    val initList = ArrayList<ByteArray>()
                    while (true) {
                        val data = keInit.createInitIso(nextCounter, isFirstCall,tid).generateIsoByteRequest()
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
                    this@HitServer.callback = null
                }

            } else {
                callback("Offline, No Internet available", false)
                this@HitServer.callback = null
            }

        } catch (ex: Exception) {
            callback(ex.message ?: "Connection Error", false)
            this@HitServer.callback = null
        }
    }


    suspend fun openSocket(cb: OnSocketComplete) {
        Log.d("Socket Start:- " , "Socket Started Here.....")

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
                cb(socket)

            } else callback?.invoke("No Comm Data Found", false)

        } catch (ex: Exception) {
            ex.printStackTrace()
            //  callback?.invoke(ex.message ?: "Connection Error", false)
            callback?.invoke("Socket Time out", false)
            Utility().logger("EXCEPTION","SOCKET NOT CONNECTED","e")
        } finally {
            Log.d("Finally Call:- ", "Final Block Runs Here.....")
        }
    }

    /*  suspend fun openSocket(): Socket? {
          try {
              tct = TerminalCommunicationTable.selectFromSchemeTable()  // always get tct it may get refresh meanwhile
              if (tct != null) {

                  val sAddress = VFService.getIpPort()

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
                  socket.connect(sAddress, connTimeOut)
                  socket.soTimeout = resTimeOut
                  return socket

              } else return null

          } catch (ex: Exception) {
              ex.printStackTrace()
              return null
          }
      }*/

    suspend fun openSocket(): Socket? {
        try {
            tct = Utility().getTctData()
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
                socket.connect(sAddress, connTimeOut)
                socket.soTimeout = resTimeOut
                return socket

            } else return null

        } catch (ex: Exception) {
            ex.printStackTrace()
            return null
        }
    }


}

/**
 * This class is used to communicate with server for multiple byte request and response
 * call open only once,
 * call close only once and in the end
 * use send function to send the data
 * */
class ServerCommunicator {

    companion object {
        private val TAG = ServerCommunicator::class.java.simpleName
    }

    private var socket: Socket? = null

    suspend fun open(): Boolean {
        var isconn = false
        //  if (VerifoneApp.internetConnection) {
        val soc = HitServer.openSocket()
        if (soc != null) {
            socket = soc
            isconn = true
            //    }
        }
        return isconn
    }

    suspend fun sendData(data: ByteArray): String {
        if (socket != null) {
            try {
                val soc = socket as Socket
                Utility().logger(TAG, "address = ${soc.inetAddress}, port = ${soc.port}", "e")
                Utility.ConnectionTimeStamps.dialConnected = getF48TimeStamp()

                Utility().logger(TAG, "Data Send = ${data.byteArr2HexStr()}")
                Utility.ConnectionTimeStamps.startTransaction = getF48TimeStamp()
                val sos = soc.getOutputStream()
                sos?.write(data)
                sos.flush()


                val dis = DataInputStream(soc.getInputStream())
                val len = dis.readShort().toInt()
                val response = ByteArray(len)
                dis.readFully(response)
                Utility.ConnectionTimeStamps.recieveTransaction = getF48TimeStamp()
                val responseStr = response.byteArr2HexStr()
                Utility().logger(TAG, "len=$len, data = $responseStr")
                return responseStr
            }catch (ex:Exception){return ""}
        } else {
            return ""
        }
    }

    fun close() {
        try {
            socket?.close()
        } catch (ex: Exception) {
        }
    }

}


/**
 * HitServer class do not modifies anything in packet
 * it simply takes data string and return response string
 * in case of no communication , is comm will be false and response string will contain message
 *
 * */
typealias ServerMessageCallbackSale = (String, Boolean,String) -> Unit

typealias ServerMessageCallback = (String, Boolean) -> Unit

typealias ProgressCallback = (String) -> Unit

typealias OnSocketComplete = suspend (socket: Socket) -> Unit