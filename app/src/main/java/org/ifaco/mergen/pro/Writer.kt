package org.ifaco.mergen.pro

import android.os.CountDownTimer
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.addTextChangedListener
import org.ifaco.mergen.Fun
import org.ifaco.mergen.Model
import org.ifaco.mergen.Panel
import org.ifaco.mergen.otr.DoubleClickListener
import java.io.BufferedReader
import java.io.InputStreamReader

class Writer(
    val that: Panel,
    val model: Model,
    bBody: ConstraintLayout,
    val bResponse: TextView,
    val bResSV: ScrollView,
    val bSay: EditText,
    val bClear: ImageView
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
        bSay.addTextChangedListener { Fun.vish(bClear, it.toString().isNotEmpty()) }
        bBody.setOnClickListener(object : DoubleClickListener() {
            override fun onDoubleClick() {
                //if (!Talker.bSending) Talker(that, bSay, model)
                val output = StringBuilder()
                try {
                    val process = Runtime.getRuntime().exec(bSay.text.toString())
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null)
                        output.append(line).append("\n")
                } catch (e: Exception) {
                    output.append("ERROR: $e")
                }
                println(output)
                model.res.value = output.toString()
            }
        })
        bClear.setOnClickListener {
            bSay.setText("")
            model.res.value = ""
        }
        //bResponse.typeface = Fun.fBold
    }

    companion object {
        const val typeDur = 2L //87L
        const val resHideAfter = 90L * 1000L
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
