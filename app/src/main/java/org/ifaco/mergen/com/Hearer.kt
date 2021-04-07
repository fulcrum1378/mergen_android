package org.ifaco.mergen.com

import android.Manifest
import androidx.core.app.ActivityCompat
import org.ifaco.mergen.Fun
import org.ifaco.mergen.Panel

class Hearer(val that: Panel) {
    companion object {
        lateinit var cli: Client

        const val req = 786
        const val perm = Manifest.permission.RECORD_AUDIO
        const val PORT = 3773
    }

    init {
        if (!Fun.permGranted(perm))
            ActivityCompat.requestPermissions(that, arrayOf(perm), req)
        else Panel.handler?.obtainMessage(Panel.Action.HEAR.ordinal)?.sendToTarget()
    }

    fun start() {
        cli = Client(that, PORT, object : Client.Repeat {
            override fun execute() {
            }
        })
    }

    fun destroy() {
        try {
            cli.interrupt()
        } catch (ignored: Exception) {
        }
    }
}
