package com.mergen.android

import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.mergen.android.Fun.Companion.permResult
import com.mergen.android.databinding.PanelBinding
import com.mergen.android.otr.AlertDialogue
import com.mergen.android.pro.Writer
import com.mergen.android.rec.Connect
import com.mergen.android.rec.Recorder

// adb connect 192.168.1.5:

class Panel : AppCompatActivity() {
    private lateinit var b: PanelBinding
    private val model: Model by viewModels()
    private lateinit var pro: Writer
    private lateinit var rec: Recorder
    private val socketErrors = ArrayList<Connect.Error>()
    private var socketErrorTimer: CountDownTimer? = null
    private val socketErrorTO = 1000L

    companion object {
        var handler: Handler? = null
        var mp: MediaPlayer? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = PanelBinding.inflate(layoutInflater)
        setContentView(b.root)
        Fun.init(this, b.root)

        // Handlers
        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    Action.WRITE.ordinal -> msg.obj?.let { b.say.setText("$it") }
                    Action.TALK.ordinal -> {
                        mp = MediaPlayer.create(this@Panel, msg.obj as Uri)
                        mp?.setOnPreparedListener { mp?.start() }
                        mp?.setOnCompletionListener { mp?.release(); mp = null }
                    }
                    Action.RECORD.ordinal -> rec.start()
                    Action.TOAST.ordinal -> try {
                        Toast.makeText(Fun.c, msg.obj as String, Toast.LENGTH_SHORT).show()
                    } catch (ignored: Exception) {
                        Toast.makeText(Fun.c, "INVALID MESSAGE", Toast.LENGTH_SHORT).show()
                    }
                    Action.QUERY.ordinal -> {
                        val et = EditText(Fun.c).apply {
                            setPadding(Fun.dp(18), Fun.dp(12), Fun.dp(18), Fun.dp(12))
                            textSize =
                                this@Panel.resources.getDimension(R.dimen.alert1Title) / Fun.dm.density
                            setText(R.string.recDefIP)
                        }
                        AlertDialogue.alertDialogue2(
                            this@Panel, R.string.recConnect, R.string.recConnectIP, et, { _, _ ->
                                try {
                                    val spl = et.text.toString().split(":")
                                    Connect.acknowledged(spl[0], spl[1].toInt())
                                } catch (ignored: java.lang.Exception) {
                                    handler?.obtainMessage(
                                        Action.TOAST.ordinal, "Invalid address, please try again!"
                                    )?.sendToTarget()
                                }
                            }
                        )
                    }
                    Action.SOCKET_ERROR.ordinal -> {
                        rec.pause()
                        rec.ear?.interrupt()
                        socketErrors.add(msg.obj as Connect.Error)
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
                }
            }
        }

        // INITIALIZATION
        pro = Writer(this, model, b.response, b.resSV, b.say, b.send, b.sendIcon, b.sending)
        rec = Recorder(this, b.preview, b.recording)
        b.record.setOnClickListener {
            if (socketErrorTimer != null) return@setOnClickListener
            if (!rec.recording) rec.resume() else rec.pause()
        }
        b.recording.colorFilter = Fun.cf(R.color.CPO)
    }

    override fun onResume() {
        super.onResume()
        rec.start()
    }

    override fun onPause() {
        super.onPause()
        rec.stop()
    }

    override fun onDestroy() {
        rec.pause()
        rec.destroy()
        try {
            mp?.release()
        } catch (ignored: Exception) {
        }
        mp = null
        handler = null
        System.gc()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val b = permResult(grantResults)
        when (requestCode) {
            Recorder.req -> if (b) handler?.obtainMessage(Action.RECORD.ordinal)?.sendToTarget()
        }
    }


    fun judgeSocketStatus() {
        var mustQuery = false
        var conProblem = false
        var imgProblem = false
        var audProblem = false
        var unknown: String? = null
        var sentNull = false
        for (e in socketErrors) {
            if (!e.isAudio) imgProblem = true
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
            mustQuery -> handler?.obtainMessage(Action.QUERY.ordinal)?.sendToTarget()
            unknown != null -> AlertDialogue.alertDialogue1(
                this@Panel, R.string.recConnectErr,
                getString(R.string.recSocketUnknownErr, unknown)
            )
            sentNull -> AlertDialogue.alertDialogue1(
                this@Panel, R.string.recConnectErr,
                getString(R.string.recSocketSentNull, if (imgProblem) "picture" else "audio")
            )
            conProblem -> {
                if (imgProblem) AlertDialogue.alertDialogue1(
                    this@Panel, R.string.recConnectErr,
                    getString(R.string.recSocketImgErr, Connect.host + ":" + Connect.port)
                )
                if (audProblem) AlertDialogue.alertDialogue1(
                    this@Panel, R.string.recConnectErr,
                    getString(R.string.recSocketAudErr, Connect.host + ":" + (Connect.port + 1))
                )
            }
        }
    }


    enum class Action { WRITE, TALK, RECORD, TOAST, QUERY, SOCKET_ERROR }
}
