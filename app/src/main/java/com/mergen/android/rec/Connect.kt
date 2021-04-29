package com.mergen.android.rec

import com.mergen.android.Fun
import com.mergen.android.Fun.Companion.sp
import com.mergen.android.Panel
import com.mergen.android.Panel.Companion.handler
import java.net.ConnectException
import java.net.Socket

class Connect(@Suppress("UNUSED_PARAMETER") that: Panel, val audioSocket: Boolean = false) {
    companion object {
        const val spHost = "host"
        const val spPort = "port"
        private var host = "127.0.0.1"
        private var port = 80
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

    fun send(data: ByteArray?): Boolean {
        if (data == null) return false
        return try {
            var socket = Socket(host, port + (if (audioSocket) 1 else 0))
            var output = socket.getOutputStream()
            output.write(Fun.z(data.size.toString()).encodeToByteArray() + data)
            output.flush()
            output.close()
            socket.close()
            System.gc()
            true
        } catch (ignored: ConnectException) {
            false// CONNECTION REFUSED
        }
    }
}
