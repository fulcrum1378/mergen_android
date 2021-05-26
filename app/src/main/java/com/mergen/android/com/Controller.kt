package com.mergen.android.com

import android.Manifest
import android.os.CountDownTimer
import android.widget.ImageView
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import com.mergen.android.Fun
import com.mergen.android.Fun.Companion.c
import com.mergen.android.Panel
import com.mergen.android.R
import com.mergen.android.otr.AlertDialogue

class Controller(val that: Panel, bPreview: PreviewView, bRecording: ImageView) : ToRecord {
    private val rec = Recorder(that, bPreview, bRecording)

    companion object {
        const val camPerm = Manifest.permission.CAMERA
        const val audPerm = Manifest.permission.RECORD_AUDIO
        const val req = 786
        const val socketErrorTO = 1000L
        val socketErrors = ArrayList<Connect.Error>()
        var socketErrorTimer: CountDownTimer? = null
    }

    init {
        if (!Fun.permGranted(camPerm) || !Fun.permGranted(audPerm))
            ActivityCompat.requestPermissions(that, arrayOf(camPerm, audPerm), req)
        else {
            rec.canPreview = true
            rec.start()
        }
    }

    fun recToggle() {
        if (socketErrorTimer != null) return
        if (!rec.recording) rec.resume() else rec.pause()
    }

    fun socketError(e: Connect.Error) {
        rec.pause()
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
        var imgProblem = false
        var audProblem = false
        var unknown: String? = null
        var sentNull = false
        for (e in socketErrors) {
            //TODO: handle controller problems
            if (e.portAdd == 1) imgProblem = true
            else audProblem = true
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
                c.getString(R.string.recSocketUnknownErr, unknown)
            )
            sentNull -> AlertDialogue.alertDialogue1(
                that, R.string.recConnectErr,
                c.getString(R.string.recSocketSentNull, if (imgProblem) "picture" else "audio")
            )
            conProblem -> {
                if (imgProblem) AlertDialogue.alertDialogue1(
                    that, R.string.recConnectErr,
                    c.getString(R.string.recSocketImgErr, Connect.host + ":" + Connect.port)
                )
                if (audProblem) AlertDialogue.alertDialogue1(
                    that, R.string.recConnectErr,
                    c.getString(R.string.recSocketAudErr, Connect.host + ":" + (Connect.port + 1))
                )
            }
        }
    }

    override fun start() {
        rec.start()
    }

    override fun resume() {
        rec.resume()
    }

    override fun pause() {
        rec.pause()
    }

    override fun stop() {
        rec.stop()
    }

    override fun destroy() {
        rec.destroy()
    }
}
