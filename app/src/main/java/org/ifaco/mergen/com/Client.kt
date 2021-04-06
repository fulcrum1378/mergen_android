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

class Client(val that: Panel) : Thread() {
    private var output: PrintWriter? = null
    private var input: BufferedReader? = null
    var host = "0.0.0.0"
    var port = 80

    companion object {
        const val HOST = "host"
        const val PORT = "port"
        const val FRAME =  500L // will be multiplied by 2
        val TOAST = Panel.Action.TOAST.ordinal
    }

    init {
        var hasHost = sp.contains(HOST)
        var hasPort = sp.contains(PORT)
        if (!hasHost || !hasPort) query()
        else acknowledged(sp.getString(HOST, "0.0.0.0")!!, sp.getInt(PORT, 80))
    }

    fun acknowledged(h: String, p: Int) {
        host = h
        port = p
        sp.edit().apply {
            putString(HOST, h)
            putInt(PORT, p)
            apply()
        }
        start()
    }

    override fun run() {
        var socket: Socket? = null
        while (true) try {
            socket = Socket(host, port)
            output = PrintWriter(socket.getOutputStream())
            input = BufferedReader(InputStreamReader(socket.getInputStream()))
            for (x in 1..2) {
                output!!.write("hey")
                output!!.flush() // input!!.readLine()?
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
                    et.text.toString().split(":").apply {
                        acknowledged(this[0], this[1].toInt())
                    }
                } catch (ignored: Exception) {
                    handler?.obtainMessage(TOAST, "Invalid address, please try again!")
                        ?.sendToTarget()
                }
            }
        )
    }
}
