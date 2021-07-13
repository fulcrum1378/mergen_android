package com.mergen.android.com

import com.mergen.android.Fun
import com.mergen.android.Panel
import com.mergen.android.Panel.Companion.handler
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket

class Connect(val portAdd: Int = 0) {
    fun send(
        data: ByteArray?,
        foreword: Boolean = true,
        receive: Boolean = false
    ): String? {
        var ret: String? = null
        if (data == null) error("false")
        else try {
            var socket = Socket(Controller.host, Controller.port + portAdd)
            var output = socket.getOutputStream()
            var input = BufferedReader(InputStreamReader(socket.getInputStream()))
            if (foreword) output.write(Fun.z(data.size.toString()).encodeToByteArray() + data)
            else output.write(data)
            output.flush()
            if (receive) ret = input.readLine()
            input.close()
            output.close()
            socket.close()
            System.gc()
        } catch (e: Exception) {
            error(e.javaClass.name)
        }
        return ret
    }

    fun error(sent: String) {
        handler?.obtainMessage(Panel.Action.SOCKET_ERROR.ordinal, Error(sent, portAdd))
            ?.sendToTarget()
    }

    data class Error(val e: String, val portAdd: Int, val byController: Boolean = portAdd == 0)
}
