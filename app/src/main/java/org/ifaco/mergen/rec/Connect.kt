package org.ifaco.mergen.rec

import android.content.DialogInterface
import android.system.ErrnoException
import android.widget.EditText
import org.ifaco.mergen.Fun
import org.ifaco.mergen.Fun.Companion.c
import org.ifaco.mergen.Fun.Companion.dm
import org.ifaco.mergen.Fun.Companion.dp
import org.ifaco.mergen.Fun.Companion.sp
import org.ifaco.mergen.Panel
import org.ifaco.mergen.Panel.Companion.handler
import org.ifaco.mergen.R
import org.ifaco.mergen.otr.AlertDialogue.Companion.alertDialogue2
import java.lang.Exception
import java.net.Socket

class Connect(val that: Panel, val audioSocket: Boolean = false) {
    private var host = "127.0.0.1"
    private var port = 80

    companion object {
        const val spHost = "host"
        const val spPort = "port"
    }

    init {
        var hasHost = sp.contains(spHost)
        var hasPort = sp.contains(spPort)
        if (!hasHost || !hasPort) query()
        else acknowledged(sp.getString(spHost, "127.0.0.1")!!, sp.getInt(spPort, 80))
    }

    fun send(data: ByteArray?) {
        if (data == null) return
        try {
            var socket = Socket(host, port + (if (audioSocket) 1 else 0))
            var output = socket.getOutputStream()
            output.write(Fun.z(data.size.toString()).encodeToByteArray() + data)
            output.flush()
            output.close()
            socket.close()
            System.gc()
        } catch (ignored: ErrnoException) {
            // CONNECTION REFUSED
        }
    }

    private fun acknowledged(h: String, p: Int) {
        host = h
        port = p
        sp.edit().apply {
            putString(spHost, h)
            putInt(spPort, p)
            apply()
        }
    }

    private fun query() {
        val et = EditText(c).apply {
            setPadding(dp(18), dp(12), dp(18), dp(12))
            textSize = that.resources.getDimension(R.dimen.alert1Title) / dm.density
            setText(R.string.cliDefIP)
        }
        alertDialogue2(
            that, R.string.cliConnect, R.string.cliConnectIP, et,
            DialogInterface.OnClickListener { _, _ ->
                try {
                    val spl = et.text.toString().split(":")
                    acknowledged(spl[0], spl[1].toInt())
                } catch (ignored: Exception) {
                    handler?.obtainMessage(
                        Panel.Action.TOAST.ordinal, "Invalid address, please try again!"
                    )?.sendToTarget()
                }
            }
        )
    }
}
