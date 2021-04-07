package org.ifaco.mergen.com

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
import java.io.*
import java.lang.Exception
import java.net.Socket

class Client(val that: Panel, val port: Int, val repeatable: Repeat) : Thread() {
    private var host = "127.0.0.1"
    var output: OutputStream? = null

    companion object {
        const val HOST = "host"
        const val FRAME = 500L
        val TOAST = Panel.Action.TOAST.ordinal
    }

    init {
        var hasHost = sp.contains(HOST)
        if (!hasHost) query()
        else acknowledged(sp.getString(HOST, "127.0.0.1")!!)
    }

    fun acknowledged(h: String) {
        host = h
        sp.edit().apply {
            putString(HOST, h)
            apply()
        }
        start()
    }

    override fun run() {
        var socket: Socket? = null
        while (true) try {
            socket = Socket(host, port)
            output = socket.getOutputStream() // socket.getInputStream()
            while (true) {
                repeatable.execute()
                sleep(FRAME)
            }
        } catch (e: IOException) {
            socket?.close()
            socket = null
            handler?.obtainMessage(TOAST, "Could not connect!")?.sendToTarget()
            // alertDialogue1 RETRY OR CANCEL
            // query() IS NOT ALLOWED HERE
            sleep(5000)
        }
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
                    acknowledged(et.text.toString())
                } catch (ignored: Exception) {
                    handler?.obtainMessage(TOAST, "Invalid address, please try again!")
                        ?.sendToTarget()
                }
            }
        )
    }

    interface Repeat {
        fun execute()
    }
}
