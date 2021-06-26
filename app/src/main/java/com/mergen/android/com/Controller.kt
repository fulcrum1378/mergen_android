package com.mergen.android.com

import android.Manifest
import android.os.CountDownTimer
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import com.mergen.android.Fun
import com.mergen.android.Fun.Companion.c
import com.mergen.android.Panel
import com.mergen.android.R
import com.mergen.android.otr.AlertDialogue

class Controller(val that: Panel, bPreview: PreviewView) : ToRecord {
    private var con = Connect(conPort)
    val rec = Recorder(that, bPreview)

    companion object {
        const val camPerm = Manifest.permission.CAMERA
        const val audPerm = Manifest.permission.RECORD_AUDIO
        const val req = 786
        const val spHost = "host"
        const val spPort = "port"
        const val socketErrorTO = 1000L
        const val conPort = 0
        const val visPort = 1
        const val earPort = 2
        val socketErrors = ArrayList<Connect.Error>()
        var socketErrorTimer: CountDownTimer? = null
        var host = "127.0.0.1"
        var port = 80
        var isAcknowledged = false
    }

    init {
        if (!Fun.permGranted(camPerm) || !Fun.permGranted(audPerm))
            ActivityCompat.requestPermissions(that, arrayOf(camPerm, audPerm), req)
        else permitted()
    }

    fun permitted() {
        rec.canPreview = true
        on()

        var hasHost = Fun.sp.contains(spHost)
        var hasPort = Fun.sp.contains(spPort)
        if (!hasHost || !hasPort) Panel.handler?.obtainMessage(Panel.Action.QUERY.ordinal)
            ?.sendToTarget()
        else acknowledged(Fun.sp.getString(spHost, "127.0.0.1")!!, Fun.sp.getInt(spPort, 80))
    }

    fun acknowledged(h: String, p: Int) {
        host = h
        port = p
        Fun.sp.edit().apply {
            putString(spHost, h)
            putInt(spPort, p)
            apply()
        }
        isAcknowledged = true
    }

    fun socketError(e: Connect.Error) {
        end()
        rec.ear?.interrupt()
        socketErrors.add(e)
        socketErrorTimer?.cancel()
        socketErrorTimer = object : CountDownTimer(socketErrorTO, socketErrorTO) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                judgeSocketStatus()
                socketErrors.clear()
                socketErrorTimer = null
            }
        }.start()
    }

    fun judgeSocketStatus() {
        var mustQuery = false
        var conProblem = false
        var whichSock = "UNKNOWN"
        var whichAddr = whichSock
        var unknown: String? = null
        var sentNull = false
        for (e in socketErrors) {
            whichAddr = "$host:${port + e.portAdd}"
            whichSock = when (e.portAdd) {
                conPort -> "controller"
                visPort -> "picture"
                earPort -> "audio"
                else -> whichSock
            }
            when (e.e) {
                "java.net.NoRouteToHostException", "java.net.UnknownHostException" ->
                    mustQuery = true
                "java.net.ConnectException" -> conProblem = true
                "false" -> sentNull = true
                else -> unknown = e.e
            }
        }
        when {
            mustQuery -> Panel.handler?.obtainMessage(Panel.Action.QUERY.ordinal)?.sendToTarget()
            unknown != null -> AlertDialogue.alertDialogue1(
                that, R.string.recConnectErr,
                c.getString(R.string.recSocketUnknownErr, unknown, whichSock)
            )
            sentNull -> AlertDialogue.alertDialogue1(
                that, R.string.recConnectErr,
                c.getString(R.string.recSocketSentNull, whichSock)
            )
            conProblem -> AlertDialogue.alertDialogue1(
                that, R.string.recConnectErr,
                c.getString(R.string.recSocketErr, whichSock, whichAddr)
            )
        }
    }


    var toggling = false
    fun toggle() {
        if (socketErrorTimer != null || toggling) return
        toggling = true
        if (!rec.recording) begin() else end()
    }

    override fun on() {
        rec.on()
    }

    override fun begin() {
        Thread {
            if (con.send(Notify.START.s, foreword = false, receive = true) == "true")
                Panel.handler?.obtainMessage(Panel.Action.FORCE_REC.ordinal)?.sendToTarget()
        }.start()
        toggling = false
    }

    override fun end() {
        rec.end()
        Thread { con.send(Notify.STOP.s, foreword = false, receive = true) }.start()
        toggling = false
    }

    override fun off() {
        rec.off()
        Thread { con.send(Notify.KILL.s, foreword = false, receive = true) }.start()
        // Move this to destroy() and implement Coroutines...
    }

    override fun destroy() {
        rec.destroy()
    }


    enum class Notify(val s: ByteArray) {
        START("start".encodeToByteArray()),
        STOP("stop".encodeToByteArray()),
        KILL("kill".encodeToByteArray())
    }
}
