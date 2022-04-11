package com.bonushub.crdb.india.serverApi
import android.content.Context.MODE_PRIVATE
import android.util.Log
import com.bonushub.crdb.india.HDFCApplication
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.model.local.DigiPosDataTable
import com.bonushub.crdb.india.model.local.TerminalCommunicationTable
import com.bonushub.crdb.india.repository.keyexchangeDataSource
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.checkInternetConnection
import com.bonushub.pax.utils.*
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getF48TimeStamp
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.insertOrUpdateDigiposData

import java.io.DataInputStream
import java.io.FileOutputStream
import java.net.ConnectException
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.nio.channels.ServerSocketChannel

val LYRA_IP_ADDRESS = "192.168.250.10"
var PORT2 = 4124
val NEW_IP_ADDRESS = "122.176.84.29"
var PORT = 8101//4124

object HitServer  {

    val TAG = HitServer::class.java.simpleName
    private var tct: TerminalCommunicationTable?= null
    private var callback: ServerMessageCallback? = null
    private var callbackInit: ServerMessageCallbackInit? = null
    private var callbackSale: ServerMessageCallbackSale? = null
    var reversalToBeSaved: IsoDataWriter?=null

    @Synchronized
    suspend fun hitServer(data: ByteArray, callback: ServerMessageCallback, progressMsg: ProgressCallback,isAppUpdate: Boolean = false){
        this@HitServer.callback = callback
        try {
            if (Utility().checkInternetConnection()) {
                with(Utility.ConnectionTimeStamps) {
                    reset()
                    dialStart = getF48TimeStamp()
                }
                openSocket({ socket ->
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
                },isAppUpdate = isAppUpdate)

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
    suspend fun hitServersale(data: ByteArray, callbackSale: ServerMessageCallbackSale, progressMsg: ProgressCallback) {
        this@HitServer.callbackSale = callbackSale
        try {
            if (checkInternetConnection()) {
                with(Utility.ConnectionTimeStamps) {
                    reset()
                    dialStart = getF48TimeStamp()
                }
                // Log.d("OpenSocket:- ", "Socket Start")

                var responseStr: String? = null
                openSocketSale { socket ->
                    try {
                        // irh?.saveReversal()
                        logger(TAG, "address = ${socket.inetAddress}, port = ${socket.port}", "e")
                        Utility.ConnectionTimeStamps.dialConnected = getF48TimeStamp()
                        progressMsg(HDFCApplication.appContext.getString(R.string.sending_reciving_data))
                        //println("Data send" + data.byteArr2HexStr())
                        logger(TAG, "Data Send = ${data.byteArr2HexStr()}")
                        Utility.ConnectionTimeStamps.startTransaction = getF48TimeStamp()
                        val sos = socket.getOutputStream()
                        sos?.write(data)
                        sos.flush()
                        progressMsg(HDFCApplication.appContext.getString(R.string.sending_reciving_data))
                        val dis = DataInputStream(socket.getInputStream())

                        //   throw SocketTimeoutException("Reversal generated") // please check

                        val len = dis.readShort().toInt()
                        val response = ByteArray(len)
                        dis.readFully(response)
                        Utility.ConnectionTimeStamps.recieveTransaction = getF48TimeStamp()
                        responseStr = response.byteArr2HexStr()
                        //   ConnectionTimeStamps.recieveTransaction = getF48TimeStamp()
                        if (responseStr != null) {
                            val reader = readIso(responseStr!!, false)
                            Field48ResponseTimestamp.saveF48IdentifierAndTxnDate(reader.isoMap[48]?.parseRaw2String() ?: "")
                        }
                        //println("Data Recieve"+response.byteArr2HexStr())
                        logger(TAG, "len=$len, data = $responseStr")
                        socket.close()
                        //   irh?.clearReversal()

                        println("Outside the Readtimeout error5")
                       // throw SocketTimeoutException("test reversal")
                        callbackSale(responseStr ?: "", true, "")
                        //  this@HitServer.callback = null

                    } catch (ex: SocketTimeoutException) {
                        println("Read Time out error1" + ex.message)
                        socket.close()
                        ex.printStackTrace()
                        callbackSale(ex.message ?: "Socket Timeout Error", true, ConnectionError.ReadTimeout.errorCode.toString())
                        //   this@HitServer.callback = null
                        return@openSocketSale
                    } catch (ex: ConnectException) {
                        ex.printStackTrace()
                        socket.close()
                        println("Read Time out error2" + ex.message)
                        callbackSale(ex.message ?: "Connection Error", true, ConnectionError.ReadTimeout.errorCode.toString())
                        //  this@HitServer.callback = null
                        return@openSocketSale
                    } catch (ex: SocketException) {
                        ex.printStackTrace()
                        println("Read Time out error3" + ex.message)
                        socket.close()
                        callbackSale(ex.message ?: "Connection Error", true, ConnectionError.ReadTimeout.errorCode.toString())
                        this@HitServer.callback = null
                        return@openSocketSale
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        println("Read Time out error4" + ex.message)
                        socket.close()
                        callbackSale(ex.message ?: "Connection Error", true, ConnectionError.ReadTimeout.errorCode.toString())
                        //  this@HitServer.callback = null
                        return@openSocketSale

                    }

                }

            } else {
                callbackSale(
                    HDFCApplication.appContext.getString(R.string.no_internet_error), false, ConnectionError.NetworkError.errorCode.toString())
                // this@HitServer.callback = null
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
            callbackSale(HDFCApplication.appContext.getString(R.string.connection_error), false, "")
            //  this@HitServer.callback = null
        }
    }

    @Synchronized
    suspend fun hitInitServer(callback: ServerMessageCallbackInit,progressMsg: ProgressCallback,keInit: keyexchangeDataSource, tid: String) {
        this@HitServer.callbackInit = callback
        val FILE_NAME = "init_packet_request_logs.txt"
        val fos : FileOutputStream = HDFCApplication.appContext.openFileOutput(FILE_NAME, MODE_PRIVATE)
        try {
//VerifoneApp.internetConnection
            if (true) {
                with(Utility.ConnectionTimeStamps) {
                    reset()
                    dialStart = getF48TimeStamp()
                }
                openSocket({ socket ->

                    Utility().logger(TAG, "address = ${socket.inetAddress}, port = ${socket.port}", "e")

                    var initSuccess = false
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
                                initSuccess = true
                              /*  Utility().readInitServer(initList) { result, message ->
                                    callback(message, result, ArrayList())
                                }*/
                               break
                            }
                        } else {
                            callback(reader.isoMap[58]?.parseRaw2String() ?: "", false, ArrayList())
                            break
                        }

                    }
                    socket.close()
                    fos.close()
                    Utility().resetRoc()
                    // ROCProviderV2.resetRoc(AppPreference.getBankCode())
                    callback("Init Succesful", initSuccess, initList)
                    //this@HitServer.callback = null
                },isAppUpdate = false)

            } else {
                callback("Offline, No Internet available", false, ArrayList())
                this@HitServer.callback = null
            }

        } catch (ex: Exception) {
            callback(ex.message ?: "Connection Error", false, ArrayList())
            this@HitServer.callback = null
        }
    }

    private suspend fun openSocketSale(cb: OnSocketComplete) {
        Log.d("Socket Start:- ", "Sale Socket Started Here.....")
        try {

            tct = Utility().getTctData()// always get tct it may get refresh meanwhile// always get tct it may get refresh meanwhile
            if (tct != null) {

                val sAddress = Utility().getIpPort(false, isPrimaryIpPort = hitCounter)
                logger("Connection Details:- ", sAddress.toString(), "e")
                logger("HIT COUNTER", "$hitCounter","e")
                Log.d("HIT COUNTER", "$hitCounter")
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
                socket.connect(sAddress, connTimeOut)//creating connection
                socket.soTimeout = resTimeOut
                cb(socket)

            } else callbackSale?.invoke("No Comm Data Found", false, "")

        } catch (ex: SocketTimeoutException) {
            println("SocketTimeoutException -> " + ex.message)
            if (hitCounter == 1) {
                hitCounter = 2
                openSocketSale {
                    hitCounter = 1
                    cb(it)
                }
            } else {
                hitCounter = 1
                callbackSale?.invoke(
                    ex.message ?: "Connection Error",
                    false,
                    ConnectionError.ConnectionTimeout.errorCode.toString()
                )
            }

        } catch (ex: ConnectException) {
            println("ConnectException ->" + ex.message)
            /*   callbackSale?.invoke(
                   ex.message ?: "Connection Error",
                   false,
                   ConnectionError.ConnectionRefusedorOtherError.errorCode.toString()
               )*/
            if (hitCounter == 1) {
                hitCounter = 2
                openSocketSale {
                    hitCounter = 1
                    cb(it)
                }
            } else {
                hitCounter = 1
                callbackSale?.invoke(
                    ex.message ?: "Connection Error",
                    false,
                    ConnectionError.ConnectionTimeout.errorCode.toString()
                )
            }
        } catch (ex: Exception) {
            println("Parent Exception-> " + ex.message)
            /*   callbackSale?.invoke(
                   ex.message ?: "Connection Error",
                   false,
                   ConnectionError.ConnectionRefusedorOtherError.errorCode.toString()
               )*/
            if (hitCounter == 1) {
                hitCounter = 2
                openSocketSale {
                    hitCounter = 1
                    cb(it)
                }
            } else {
                hitCounter = 1
                callbackSale?.invoke(
                    ex.message ?: "Connection Error",
                    false,
                    ConnectionError.ConnectionTimeout.errorCode.toString()
                )
            }
        } finally {
            Log.d("Finally Call:- ", "HIT SERVER SALE --> Final Block Runs Here.....")
        }
    }

    suspend fun openSocket(cb: OnSocketComplete,isAppUpdate: Boolean = false) {
        Log.d("Socket Start:- " , "Socket Started Here.....")

        try {
            tct = Utility().getTctData()// always get tct it may get refresh meanwhile
            if (tct != null) {

                val sAddress = Utility().getIpPort(isAppUpdate, isPrimaryIpPort = hitCounter)
                logger("Connection Details:- ", sAddress.toString(), "e")
                logger("HIT COUNTER", "$hitCounter","e")

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
                hitCounter = 1

            } else callback?.invoke("No Comm Data Found", false)

        }    catch (ex: SocketTimeoutException) {
            println("SocketTimeoutException -> " + ex.message)
            //    callback?.invoke(VerifoneApp.appContext.getString(R.string.socket_timeout), false)
            println("SocketTimeoutException -> " + ex.message)
            if (hitCounter == 1) {
                hitCounter = 2
                openSocket({ hitCounter = 1
                    cb(it)

                }, isAppUpdate)
            } else {
                hitCounter = 1
                callback?.invoke(
                    ex.message ?: "Connection Error",
                    false
                )
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
            println("SOCKET CONNECT Parent EXCEPTION")
            if (hitCounter == 1) {
                hitCounter = 2
                openSocket({ hitCounter = 1
                    cb(it)

                }, isAppUpdate)
            } else {
                hitCounter = 1
                callback?.invoke(
                    ex.message ?: "Connection Error",
                    false
                )
            }

        } finally {
            Log.d("Finally Call:- ", "Final Block Runs Here.....")
        }
    }



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


    @Synchronized
    suspend fun hitDigiPosServer(
        isoWriterData: IsoDataWriter, isSaveTransactionAsPending: Boolean,
        callback: ServerMessageCallback
    ) {
        this@HitServer.callback = callback

        try {
            if (Field48ResponseTimestamp.checkInternetConnection()) {
                with(Utility.ConnectionTimeStamps) {
                    reset()
                    dialStart = getF48TimeStamp()
                }
                //   Log.d("OpenSocket:- ", "Socket Start")
                //    logger("Connection Details:- ", VFService.getIpPort().toString(), "d")
                // var responseStr : String? = null
                openSocket({ socket ->

                    if (isSaveTransactionAsPending) {
                        val datatosave = isoWriterData.isoMap[57]?.parseRaw2String().toString()
                        logger(TAG, "SAVED TO DIGIPOS -->$datatosave", "e")
                        val datalist = datatosave.split("^")
                        // EnumDigiPosProcess.UPIDigiPOS.code + "^" + formattedAmt + "^" + binding?.descriptionEt?.text?.toString() + "^" + binding?.mobilenoEt?.text?.toString() + "^" + binding?.vpaEt?.text?.toString() + "^" + uniqueID
                        // EnumDigiPosProcess.SMS_PAYDigiPOS.code + "^" + formattedAmt + "^" + binding?.descriptionEt?.text?.toString() + "^" + binding?.mobilenoEt?.text?.toString() + "^" + uniqueID

                        val digiposData = DigiPosDataTable()
                        digiposData.requestType = datalist[0].toInt()
                        digiposData.amount = datalist[1]
                        digiposData.description = datalist[2]
                        digiposData.customerMobileNumber = datalist[3]
                        digiposData.displayFormatedDate = getCurrentDateInDisplayFormatDigipos()

                        when {
                            datalist[0].toInt() == EnumDigiPosProcess.UPIDigiPOS.code.toInt() -> {
                                digiposData.vpa = datalist[4]
                                digiposData.partnerTxnId = datalist[5]
                                digiposData.paymentMode = "UPI Pay"
                            }
                            datalist[0].toInt() == EnumDigiPosProcess.DYNAMIC_QR.code.toInt() -> {
                                digiposData.partnerTxnId = datalist[4]
                                digiposData.paymentMode = "Bhqr Pay"
                            }
                            else -> {
                                digiposData.partnerTxnId = datalist[4]
                                digiposData.paymentMode = "SMS Pay"
                            }
                        }
                        //

                      insertOrUpdateDigiposData(digiposData)
                    }

                    logger(TAG, "address = ${socket.inetAddress}, port = ${socket.port}", "e")
                    Utility.ConnectionTimeStamps.dialConnected = getF48TimeStamp()
                    // progressMsg("Please wait sending data to Bonushub server")
                    //println("Data send" + data.byteArr2HexStr())
                    val data = isoWriterData.generateIsoByteRequest()
                    logger(TAG, "Data Send = ${data.byteArr2HexStr()}")
                    Utility.ConnectionTimeStamps.startTransaction = getF48TimeStamp()
                    val sos = socket.getOutputStream()
                    sos?.write(data)
                    sos.flush()

                    //  progressMsg("Please wait receiving data from Bonushub server")
                    val dis = DataInputStream(socket.getInputStream())
                    val len = dis.readShort().toInt()
                    val response = ByteArray(len)
                    dis.readFully(response)
                    Utility.ConnectionTimeStamps.recieveTransaction = getF48TimeStamp()

                    //   ConnectionTimeStamps.recieveTransaction = getF48TimeStamp()

                    val responseStr = response.byteArr2HexStr()
                    val reader = readIso(responseStr, false)
                    Field48ResponseTimestamp.saveF48IdentifierAndTxnDate(
                        reader.isoMap[48]?.parseRaw2String() ?: ""
                    )

                    //println("Data Recieve" + response.byteArr2HexStr())
                    logger(TAG, "len=$len, data received = $responseStr")

                    socket.close()
                    callback(responseStr, true)
                    this@HitServer.callback = null
                },isAppUpdate = false)

            } else {
                callback(HDFCApplication.appContext.getString(R.string.no_internet_error), false)
                this@HitServer.callback = null
            }

        } catch (ex: SocketTimeoutException) {
            //  println("CHECK EXCEPTION SOCKET")
            callback(HDFCApplication.appContext.getString(R.string.connection_error), false)
            this@HitServer.callback = null
        } catch (ex: Exception) {
            callback(HDFCApplication.appContext.getString(R.string.connection_error), false)
            this@HitServer.callback = null
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

typealias ServerMessageCallbackInit = (String, Boolean,ArrayList<ByteArray>) -> Unit

typealias ProgressCallback = (String) -> Unit

typealias OnSocketComplete = suspend (socket: Socket) -> Unit