package com.bonushub.crdb.serverApi

import android.util.Log
import com.bonushub.crdb.model.local.TerminalCommunicationTable
import com.bonushub.crdb.utils.*
import com.bonushub.pax.utils.*
import java.io.DataInputStream
import java.net.Socket
import java.nio.channels.ServerSocketChannel

object SocketHelper {
    val TAG: String = SocketHelper::class.java.simpleName
    private var tct: TerminalCommunicationTable? = null

    @Synchronized
    suspend fun getResponseFromServer(
        data: IWriter,
        needReversalSaved: Boolean = false
    ): RespMessageStatusData {
        try {
            if (Utility().checkInternetConnection()) {
                with(Utility.ConnectionTimeStamps) {
                    reset()
                    dialStart = Field48ResponseTimestamp.getF48TimeStamp()
                }
                val socketConnectionStatusRespMessageStatusData: RespMessageStatusData = getSocket()
                if (socketConnectionStatusRespMessageStatusData.isSuccess) {
                    val socket = socketConnectionStatusRespMessageStatusData.anyData as Socket
                    if (needReversalSaved) {
                        // Todo Reversal Saved Here
                        Log.i("Reversal", "Reversal Saved Here ")
                    }
                    Utility().logger(
                        SocketHelper.TAG,
                        "address = ${socket.inetAddress}, port = ${socket.port}",
                        "e"
                    )
                    Utility.ConnectionTimeStamps.dialConnected =
                        Field48ResponseTimestamp.getF48TimeStamp()

                    val byteData = data.generateIsoByteRequest()
                    Utility().logger(SocketHelper.TAG, "Data Send = ${byteData.byteArr2HexStr()}")
                  logISOReader("REQUEST",(data as IsoDataWriter).isoMap)
                    Utility.ConnectionTimeStamps.startTransaction =
                        Field48ResponseTimestamp.getF48TimeStamp()
                    val sos = socket.getOutputStream()
                    sos?.write(byteData)
                    sos.flush()
                    val dis = DataInputStream(socket.getInputStream())
                    val len = dis.readShort().toInt()
                    val response = ByteArray(len)
                    dis.readFully(response)
                    Utility.ConnectionTimeStamps.recieveTransaction =
                        Field48ResponseTimestamp.getF48TimeStamp()

                    val responseStr = response.byteArr2HexStr()
                    val reader = readIso(responseStr, false)
                    Field48ResponseTimestamp.saveF48IdentifierAndTxnDate(
                        reader.isoMap[48]?.parseRaw2String() ?: ""
                    )

                    Utility().logger(SocketHelper.TAG, "len=$len, data = $responseStr")
                    socket.close()
                    val isoReader = readIso(responseStr)
                    Utility().logger(KeyExchanger.TAG, isoReader.isoMap)
                    val respMsg = isoReader.isoMap[58]?.parseRaw2String() ?: ""
                   /* return if (isoReader.isoMap[39]?.rawData?.hexStr2ByteArr()?.byteArr2Str() == "00")
                        RespMessageStatusData(respMsg, true, isoReader)
                    else RespMessageStatusData(respMsg, false)*/
                    return RespMessageStatusData(respMsg, true, isoReader)

                } else {
                    return RespMessageStatusData(
                        socketConnectionStatusRespMessageStatusData.message,
                        socketConnectionStatusRespMessageStatusData.isSuccess
                    )
                }
            } else {
                return RespMessageStatusData("No internet", false)
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
            Utility().logger("EXCEPTION", "NO RESPONSE... ${ex.message.toString()}", "e")
            return RespMessageStatusData(message = ex.toString(), false)
        }
    }


    private suspend fun getSocket(): RespMessageStatusData {
        Log.d("Getting Socket:- ", "Socket Started Here.....")
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
                return RespMessageStatusData(isSuccess = true, anyData = socket)
            } else {
                return RespMessageStatusData("No Comm Data Found", isSuccess = false)
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
            Utility().logger("EXCEPTION", "SOCKET NOT CONNECTED", "e")
            return RespMessageStatusData(ex.message.toString(), isSuccess = false)
        } finally {
            Log.d("Finally Call:- ", "Final Block Runs Here.....")
        }
    }


}