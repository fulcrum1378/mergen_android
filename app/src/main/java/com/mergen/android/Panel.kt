package com.mergen.android

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.mergen.android.Fun.Companion.permResult
import com.mergen.android.com.Connect
import com.mergen.android.com.Controller
import com.mergen.android.databinding.PanelBinding
import com.mergen.android.otr.AlertDialogue
import com.mergen.android.pro.Writer

// adb connect 192.168.1.20:
// python C:\Users\fulcr\Projects\mergen\main.py

class Panel : AppCompatActivity() {
    private lateinit var b: PanelBinding
    private lateinit var m: Model
    private lateinit var pro: Writer
    private lateinit var com: Controller
    private var anRecording: ObjectAnimator? = null

    companion object {
        var handler: Handler? = null
        var mp: MediaPlayer? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = PanelBinding.inflate(layoutInflater)
        m = ViewModelProvider(this, Model.Factory()).get("Model", Model::class.java)
        setContentView(b.root)
        Fun.init(this, b.root)

        // Handlers
        handler = object : Handler(Looper.getMainLooper()) {
            @SuppressLint("SetTextI18n")
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    Action.WRITE.ordinal -> msg.obj?.let { b.say.setText("$it") }
                    Action.TALK.ordinal -> {
                        mp = MediaPlayer.create(this@Panel, msg.obj as Uri)
                        mp?.setOnPreparedListener { mp?.start() }
                        mp?.setOnCompletionListener { mp?.release(); mp = null }
                    }
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
                            setText(Fun.sp.getString(Controller.spHost, "127.0.0.1") +
                                    ":${Fun.sp.getInt(Controller.spPort, 80)}")
                        }
                        AlertDialogue.alertDialogue2(
                            this@Panel, R.string.recConnect, R.string.recConnectIP, et, { _, _ ->
                                try {
                                    val spl = et.text.toString().split(":")
                                    com.acknowledged(spl[0], spl[1].toInt())
                                } catch (ignored: java.lang.Exception) {
                                    handler?.obtainMessage(
                                        Action.TOAST.ordinal, "Invalid address, please try again!"
                                    )?.sendToTarget()
                                }
                            }
                        )
                    }
                    Action.SOCKET_ERROR.ordinal -> com.socketError(msg.obj as Connect.Error)
                    Action.TOGGLE.ordinal -> anRecording =
                        if (msg.obj as Boolean) Fun.whirl(b.recording, null)
                        else Fun.whirl(b.recording, anRecording)
                    Action.FORCE_REC.ordinal -> com.rec.begin()
                }
            }
        }

        // INITIALIZATION
        pro = Writer(this, m, b.response, b.resSV, b.say, b.send, b.sendIcon, b.sending)
        com = Controller(this, b.preview)
        b.record.setOnClickListener { com.toggle() }
        b.recording.colorFilter = Fun.cf(R.color.CPO)
    }

    override fun onResume() {
        super.onResume()
        com.on()
    }

    override fun onPause() {
        super.onPause()
        com.off()
    }

    override fun onDestroy() {
        com.end()
        com.off()
        com.destroy()
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
            Controller.req -> if (b) com.permitted()
        }
    }

    enum class Action { WRITE, TALK, TOAST, QUERY, SOCKET_ERROR, TOGGLE, FORCE_REC }
}
