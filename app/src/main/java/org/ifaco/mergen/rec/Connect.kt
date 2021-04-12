package org.ifaco.mergen.rec

import android.content.DialogInterface
import android.widget.EditText
import org.ifaco.mergen.Fun.Companion.c
import org.ifaco.mergen.Fun.Companion.dm
import org.ifaco.mergen.Fun.Companion.dp
import org.ifaco.mergen.Fun.Companion.sp
import org.ifaco.mergen.Panel
import org.ifaco.mergen.Panel.Companion.handler
import org.ifaco.mergen.R
import org.ifaco.mergen.otr.AlertDialogue.Companion.alertDialogue2
import org.ifaco.mergen.rec.Recorder.Companion.FRAME
import java.io.*
import java.lang.Exception
import java.net.Socket

class Connect(val that: Panel, val audioSocket: Boolean = false) : Thread() {
    private var host = "127.0.0.1"
    private var port = 80
    private var active = true

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

    fun acknowledged(h: String, p: Int) {
        host = h
        port = p
        sp.edit().apply {
            putString(spHost, h)
            putInt(spPort, p)
            apply()
        }
        start()
    }

    private var socket: Socket? = null
    private var output: OutputStream? = null
    var sendable: ByteArray? = null
    override fun run() {
        while (active) try {
            sendable?.let {
                socket = Socket(host, port + (if (audioSocket) 1 else 0))
                output = socket!!.getOutputStream()
                output!!.write(z(it.size.toString()).encodeToByteArray() + it)
                output!!.flush()
            }
            sleep(FRAME)
        } catch (e: Exception) {
            try {
                socket?.close()
                socket = null
                handler?.obtainMessage(Panel.Action.TOAST.ordinal, "Could not connect!")
                    ?.sendToTarget()
                // alertDialogue1 RETRY OR CANCEL
                // query() IS NOT ALLOWED HERE
                sleep(5000)
            } catch (ignored: Exception) {
            }
        }
    }

    fun z(s: String): String {
        var add = ""
        for (x in 0..(9 - s.length)) add += "0"
        return add + s
    }

    fun query() {
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

    fun end() {
        active = false
        socket?.close()
        socket = null
        sendable = null
        interrupt()
    }
}
