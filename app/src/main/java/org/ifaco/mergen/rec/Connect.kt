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
import java.io.*
import java.lang.Exception
import java.net.Socket

class Connect(val that: Panel) : Thread() {
    private var host = "127.0.0.1"
    private var port = 80
    var output: PrintWriter? = null

    companion object {
        const val FRAME = 3000L
        const val spHost = "host"
        const val spPort = "host"
    }

    init {
        var hasHost = sp.contains(spHost)
        if (!hasHost) query()
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

    override fun run() {
        var socket: Socket? = null
        while (true) try {
            socket = Socket(host, port)
            output = PrintWriter(socket.getOutputStream()) // socket.getInputStream()
            /*for (x in 1..2) {
                repeatable.execute()
                sleep(FRAME)
            }*/
        } catch (e: IOException) {
            socket?.close()
            socket = null
            handler?.obtainMessage(Panel.Action.TOAST.ordinal, "Could not connect!")?.sendToTarget()
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
                    val spl = et.text.toString().split(":")
                    acknowledged(spl[0], spl[1].toInt())
                } catch (ignored: Exception) {
                    handler?.obtainMessage(Panel.Action.TOAST.ordinal, "Invalid address, please try again!")
                        ?.sendToTarget()
                }
            }
        )
    }

    interface Repeat {
        fun execute()
    }
}
