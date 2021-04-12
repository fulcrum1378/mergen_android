package org.ifaco.mergen

import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import org.ifaco.mergen.Fun.Companion.permResult
import org.ifaco.mergen.databinding.PanelBinding
import org.ifaco.mergen.pro.Writer
import org.ifaco.mergen.rec.Recorder

// adb connect 192.168.1.5:

class Panel : AppCompatActivity() {
    private lateinit var b: PanelBinding
    private val model: Model by viewModels()
    private lateinit var pro: Writer
    private lateinit var rec: Recorder

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
                }
            }
        }

        // INITIALIZATION
        pro = Writer(this, model, b.response, b.resSV, b.say, b.send, b.sendIcon, b.sending)
        rec = Recorder(this, b.preview, b.recording)
        b.record.setOnClickListener { if (!rec.recording) rec.resume() else rec.pause() }
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

    enum class Action { WRITE, TALK, RECORD, TOAST }
}
