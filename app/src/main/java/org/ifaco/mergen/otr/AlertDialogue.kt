package org.ifaco.mergen.otr

import android.content.DialogInterface
import android.graphics.Typeface
import android.text.util.Linkify
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.ifaco.mergen.Fun
import org.ifaco.mergen.Fun.Companion.color
import org.ifaco.mergen.R

class AlertDialogue {
    companion object {
        fun alertDialogue1(
            that: AppCompatActivity, title: Int, message: String,
            onOK: DialogInterface.OnClickListener? = null,
            onCancel: DialogInterface.OnCancelListener? = null,
            font: Typeface = Fun.fRegular
        ) {
            AlertDialog.Builder(that, R.style.alertDialogue1).apply {
                setTitle(title)
                setMessage(message)
                setIcon(R.mipmap.launcher_round)
                setPositiveButton(R.string.ok, onOK)
                setOnCancelListener(onCancel)
            }.create().apply {
                show()
                fixADButton(that, getButton(AlertDialog.BUTTON_POSITIVE), font)
                fixADTitle(that, window, font)
                fixADMsg(that, window, font)
            }
        }

        fun alertDialogue2(
            that: AppCompatActivity, title: Int, message: Int, view: View,
            onOK: DialogInterface.OnClickListener? = null,
            font: Typeface = Fun.fRegular
        ) {
            AlertDialog.Builder(that, R.style.alertDialogue1).apply {
                setTitle(title)
                setMessage(message)
                setIcon(R.mipmap.launcher_round)
                setPositiveButton(R.string.ok, onOK)
                setView(view)
            }.create().apply {
                show()
                fixADButton(that, getButton(AlertDialog.BUTTON_POSITIVE), font)
                fixADTitle(that, window, font)
                fixADMsg(that, window, font)
            }
        }

        fun fixADButton(
            that: AppCompatActivity, button: Button, font: Typeface, sMargin: Boolean = false
        ) {
            button.apply {
                setTextColor(ContextCompat.getColor(that, R.color.CPO))
                setBackgroundColor(ContextCompat.getColor(that, R.color.CP))
                typeface = font
                textSize = that.resources.getDimension(R.dimen.alert1Button) / Fun.dm.density
                if (sMargin) (layoutParams as ViewGroup.MarginLayoutParams).apply {
                    marginStart = textSize.toInt()
                }
            }
        }

        fun fixADTitle(that: AppCompatActivity, window: Window?, font: Typeface): TextView? {
            var tvTitle = window?.findViewById<TextView>(R.id.alertTitle)
            tvTitle?.setTypeface(font, Typeface.BOLD)
            tvTitle?.textSize = that.resources.getDimension(R.dimen.alert1Title) / Fun.dm.density
            tvTitle?.setTextColor(color(R.color.CP))
            return tvTitle
        }

        fun fixADMsg(
            that: AppCompatActivity, window: Window?, font: Typeface, linkify: Boolean = false
        ): TextView? {
            var tvMsg = window?.findViewById<TextView>(android.R.id.message)
            tvMsg?.typeface = font
            tvMsg?.setLineSpacing(
                that.resources.getDimension(R.dimen.alert1MsgLine) / Fun.dm.density, 0f
            )
            tvMsg?.textSize = that.resources.getDimension(R.dimen.alert1Msg) / Fun.dm.density
            tvMsg?.setTextColor(color(R.color.ADTC))
            tvMsg?.setPadding(Fun.dp(28), Fun.dp(15), Fun.dp(28), Fun.dp(15))
            if (tvMsg != null && linkify) Linkify.addLinks(tvMsg, Linkify.ALL)
            return tvMsg
        }
    }
}