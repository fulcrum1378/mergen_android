package org.ifaco.mergen

import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import org.ifaco.mergen.Fun.Companion.permResult
import org.ifaco.mergen.com.Hearer
import org.ifaco.mergen.com.Watcher
import org.ifaco.mergen.databinding.PanelBinding
import org.ifaco.mergen.pro.Talker
import org.ifaco.mergen.pro.Writer

// adb connect 192.168.1.4:

class Panel : AppCompatActivity() {
    lateinit var b: PanelBinding
    val model: Model by viewModels()
    lateinit var wri: Writer
    lateinit var tak: Talker

    companion object {
        var handler: Handler? = null
        var mp: MediaPlayer? = null
        lateinit var vis: Watcher
        lateinit var ear: Hearer
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
                    //Action.HEAR.ordinal -> ear.start()
                    Action.WATCH.ordinal -> vis.start()
                    Action.TOAST.ordinal -> try {
                        Toast.makeText(Fun.c, msg.obj as String, Toast.LENGTH_SHORT).show()
                    } catch (ignored: Exception) {
                        Toast.makeText(Fun.c, "INVALID MESSAGE", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Pronouncer
        wri = Writer(this, model, b.body, b.response, b.resSV, b.say, b.clear)
        tak = Talker(this, b.say, model)

        // Communication
        vis = Watcher(this, b.preview)
        ear = Hearer(this)
    }

    override fun onResume() {
        super.onResume()
        vis.start()
    }

    override fun onPause() {
        super.onPause()
        vis.stop()
    }

    override fun onDestroy() {
        vis.destroy()
        ear.destroy()
        try {
            mp?.release()
        } catch (ignored: Exception) {
        }
        mp = null
        handler = null
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val b = permResult(grantResults)
        when (requestCode) {
            Hearer.req -> if (b) handler?.obtainMessage(Action.HEAR.ordinal)?.sendToTarget()
            Watcher.req -> if (b) handler?.obtainMessage(Action.WATCH.ordinal)?.sendToTarget()
        }
    }

    enum class Action { WRITE, TALK, HEAR, WATCH, TOAST }
}
