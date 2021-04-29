package com.mergen.android.pro

import android.os.CountDownTimer
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.addTextChangedListener
import com.mergen.android.Fun
import com.mergen.android.Model
import com.mergen.android.Panel

class Writer(
    val that: Panel,
    val model: Model,
    val bResponse: TextView,
    val bResSV: ConstraintLayout,
    val bSay: EditText,
    val bSend: ConstraintLayout,
    val bSendIcon: ImageView,
    val bSending: ImageView
) {
    init {
        model.res.observe(that, { s ->
            resTyper = s
            typer?.cancel()
            typer = null
            bResponse.text = ""
            resHider?.cancel()
            resHider = null
            Fun.fade(bResSV)
            respond(0)
        })
        bSay.typeface = Fun.fRegular
        bSay.addTextChangedListener { Fun.vish(bSend, it.toString().isNotEmpty()) }
        bSend.setOnClickListener {
            if (bSay.text.toString() == "" || Talker.isSending) return@setOnClickListener
            Talker(that, bSay, model, bSendIcon, bSending)
        }
        bResponse.typeface = Fun.fBold
    }

    companion object {
        const val typeDur = 50L
        const val resHideAfter = 30L * 1000L
    }

    var resTyper = ""
    var typer: CountDownTimer? = null
    var resHider: CountDownTimer? = null
    fun respond(which: Int) {
        if (resTyper.length <= which) {
            resHider = object : CountDownTimer(resHideAfter, resHideAfter) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() {
                    Fun.fade(this@Writer.bResSV, false)
                    bResponse.text = ""
                }
            }.start()
            return
        }
        bResponse.text = bResponse.text.toString().plus(resTyper[which])
        typer = object : CountDownTimer(typeDur, typeDur) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                respond(which + 1)
                typer = null
            }
        }.apply { start() }
    }
}
