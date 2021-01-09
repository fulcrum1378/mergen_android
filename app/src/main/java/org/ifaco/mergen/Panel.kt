package org.ifaco.mergen

import android.content.Intent
import android.os.*
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import org.ifaco.mergen.Client.Companion.bSending
import org.ifaco.mergen.Fun.Companion.c
import org.ifaco.mergen.Fun.Companion.cf
import org.ifaco.mergen.Fun.Companion.drown
import org.ifaco.mergen.Fun.Companion.fBold
import org.ifaco.mergen.Fun.Companion.fRegular
import org.ifaco.mergen.Fun.Companion.permResult
import org.ifaco.mergen.Fun.Companion.vis
import org.ifaco.mergen.Fun.Companion.vish
import org.ifaco.mergen.audio.Recognizer
import org.ifaco.mergen.audio.Recorder
import org.ifaco.mergen.audio.Speaker
import org.ifaco.mergen.camera.Capture
import org.ifaco.mergen.databinding.PanelBinding
import org.ifaco.mergen.more.DoubleClickListener
import java.util.*

class Panel : AppCompatActivity() {
    lateinit var b: PanelBinding
    val model: Model by viewModels()
    val typeDur = 87L

    companion object {
        lateinit var rec: Recognizer
        lateinit var cap: Capture
        var handler: Handler? = null
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
                    Action.CANT_HEAR.ordinal -> vis(b.hear, false)
                    Action.HEAR.ordinal -> rec.hear(b.hearIcon, b.hearing, b.waiting)
                    Action.HEARD.ordinal -> Client(
                        this@Panel, 3, b.say, b.waiting, b.sendingIcon, b.hearIcon, model
                    ).apply {
                        if (!result) {
                            drown(hearIcon, true)
                            if (rec.continuous) rec.continueIt(waitingView)
                        }
                    }
                    Action.CANT_SEE.ordinal -> {
                        vis(b.see, false)
                        vis(b.preview, false)
                    }
                    Action.WRITE.ordinal -> msg.obj?.let { b.say.setText("$it") }
                    Action.CLEAN.ordinal -> clear()
                    Action.EXIT.ordinal -> {
                        moveTaskToBack(true)
                        Process.killProcess(Process.myPid())
                        kotlin.system.exitProcess(1)
                    }
                }
            }
        }

        // Initializations
        //Nav.locationPermission()
        Speaker.init()
        rec = Recognizer(this)
        cap = Capture(this, b.preview)

        // Listening
        b.sSayHint = "....."// can be changed later but won't survive a configuration change
        model.res.observe(this, { s ->
            resTyper = s
            typer?.cancel()
            typer = null
            b.response.text = ""
            respond(0)
        })
        b.say.typeface = fRegular
        b.body.setOnClickListener(object : DoubleClickListener() {
            override fun onDoubleClick() {
                if (bSending || rec.listening) return
                if (rec.continuer != null) rec.doNotContinue(b.waiting)
                Client(this@Panel, 3, b.say, b.waiting, b.sendingIcon, b.hearIcon, model).apply {
                    if (!result) {
                        drown(b.hearIcon, true)
                        if (rec.continuous) rec.continueIt(b.waiting)
                    } else drown(b.hearIcon, false)
                }
            }
        })

        // Hearing
        b.hearIcon.drawable.apply { colorFilter = cf() }
        b.hear.setOnClickListener {
            if (bSending || rec.listening) return@setOnClickListener
            if (rec.continuer != null) {
                rec.doNotContinue(b.waiting); return@setOnClickListener; }
            rec.continuous = false
            rec.start()
        }
        b.hear.setOnLongClickListener {
            if (bSending || rec.listening) return@setOnLongClickListener false
            if (rec.continuer != null) {
                rec.doNotContinue(b.waiting); return@setOnLongClickListener false; }
            rec.continuous = true
            rec.start()
            true
        }

        // Seeing
        b.see.setOnClickListener {
            if (!cap.previewing) cap.start()
            else cap.pause()
            vish(b.preview, cap.previewing)
        }

        // Sending
        b.sendingIcon.drawable.apply { colorFilter = cf() }
        b.response.setOnClickListener {
            if (model.res.value == "") return@setOnClickListener
            Toast.makeText(c, model.mean.value, Toast.LENGTH_LONG).show()
        }
        b.clear.setOnClickListener { clear() }
        b.response.typeface = fBold
    }

    override fun onDestroy() {
        cap.destroy()
        rec.destroy()
        Speaker.destroy()
        handler = null
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        when (requestCode) {
            Nav.reqLocPer -> {
                if (permResult(grantResults)) Nav.locationSettings(this)
                else handler?.obtainMessage(Action.EXIT.ordinal)?.sendToTarget()
            }
            Recorder.reqRecPer -> if (permResult(grantResults))
                handler?.obtainMessage(Action.HEAR.ordinal)?.sendToTarget()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        when (requestCode) {
            Nav.reqLocSet -> if (resultCode == RESULT_OK) Nav.locate(this)
        }
    }


    var resTyper = ""
    var typer: CountDownTimer? = null
    fun respond(which: Int) {
        if (resTyper.length <= which) return
        b.response.text = b.response.text.toString().plus(resTyper[which])
        typer = object : CountDownTimer(typeDur, typeDur) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                respond(which + 1)
                typer = null
            }
        }.apply { start() }
    }

    fun clear() {
        b.say.setText("")
        model.res.value = ""
    }


    enum class Action { CANT_HEAR, HEAR, HEARD, CANT_SEE, WRITE, CLEAN, EXIT }
}
