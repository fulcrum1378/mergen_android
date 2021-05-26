package com.mergen.android.com

import com.mergen.android.Fun
import com.mergen.android.Panel
import com.mergen.android.Panel.Companion.handler
import java.lang.Exception
import java.net.Socket

class Connect(val portAdd: Int = 0) {
    fun send(data: ByteArray?) {
        if (data == null) error("false")
        else try {
            var socket = Socket(Controller.host, Controller.port + portAdd)
            var output = socket.getOutputStream()
            output.write(Fun.z(data.size.toString()).encodeToByteArray() + data)
            output.flush()
            output.close()
            socket.close()
            System.gc()
        } catch (e: Exception) {
            error(e.javaClass.name)
        }
    }

    fun error(sent: String) {
        handler?.obtainMessage(Panel.Action.SOCKET_ERROR.ordinal, Error(sent, portAdd))
            ?.sendToTarget()
    }

    data class Error(val e: String, val portAdd: Int)
}
