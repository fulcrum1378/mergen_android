package com.mergen.android.com

import com.mergen.android.Fun
import com.mergen.android.Fun.Companion.sp
import com.mergen.android.Panel
import com.mergen.android.Panel.Companion.handler
import java.lang.Exception
import java.net.Socket

class Connect(@Suppress("UNUSED_PARAMETER") that: Panel, val portAdd: Int = 0) {
    companion object {
        const val spHost = "host"
        const val spPort = "port"
        var host = "127.0.0.1"
        var port = 80
        var isAcknowledged = false

        fun acknowledged(h: String, p: Int) {
            host = h
            port = p
            sp.edit().apply {
                putString(spHost, h)
                putInt(spPort, p)
                apply()
            }
            isAcknowledged = true
        }
    }

    init {
        var hasHost = sp.contains(spHost)
        var hasPort = sp.contains(spPort)
        if (!hasHost || !hasPort) handler?.obtainMessage(Panel.Action.QUERY.ordinal)?.sendToTarget()
        else acknowledged(sp.getString(spHost, "127.0.0.1")!!, sp.getInt(spPort, 80))
    }

    fun send(data: ByteArray?) {
        if (data == null) error("false")
        else try {
            var socket = Socket(host, port + portAdd)
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
