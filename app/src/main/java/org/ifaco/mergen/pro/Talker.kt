package org.ifaco.mergen.pro

import android.animation.ObjectAnimator
import android.net.Uri
import android.text.TextUtils
import android.util.Base64
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.net.toUri
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.ifaco.mergen.Fun
import org.ifaco.mergen.Fun.Companion.c
import org.ifaco.mergen.Model
import org.ifaco.mergen.Panel
import org.ifaco.mergen.Panel.Companion.handler
import org.ifaco.mergen.R
import org.ifaco.mergen.otr.AlertDialogue
import java.io.File
import java.io.FileOutputStream
import java.util.regex.Pattern

class Talker(
    val that: Panel,
    val et: EditText,
    val model: Model,
    val bSend: ConstraintLayout,
    val bSendIcon: ImageView,
    val bSending: ImageView,
    var said: String = et.text.toString()
) {
    var result = true

    companion object {
        var isSending = false
        var anSending: ObjectAnimator? = null
    }

    init {
        result = true
        if (said == "" || isSending) result = false
        if (result) {
            sending(true)
            var got = "t=$said"
            Volley.newRequestQueue(c).add(
                StringRequest(Request.Method.GET, encode("http://82.102.10.134/?$got"), { res ->
                    sending(false)
                    if (res.startsWith("Traceback "))
                        AlertDialogue.alertDialogue1(that, R.string.proError, res)
                    else {
                        val temp = File(c.cacheDir, "response.wav")
                        FileOutputStream(temp).apply {
                            write(Base64.decode(res, Base64.DEFAULT))
                            close()
                        }
                        handler?.obtainMessage(Panel.Action.TALK.ordinal, temp.toUri())
                            ?.sendToTarget()
                        temp.deleteOnExit()
                    }
                    model.res.value = said
                }, {
                    sending(false)
                    Toast.makeText(c, "Connection failed: $it", Toast.LENGTH_SHORT).show()
                }).setShouldCache(false).setTag("talk").setRetryPolicy(
                    DefaultRetryPolicy(
                        10000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                    )
                )
            )
        }
    }

    private fun sending(bb: Boolean) {
        isSending = bb
        et.isEnabled = !bb
        anSending = Fun.whirl(bSending, if (bb) null else anSending)
        Fun.drown(bSendIcon, !bb)
    }

    private fun encode(uriString: String): String {
        if (TextUtils.isEmpty(uriString)) return uriString
        val allowedUrlCharacters = Pattern.compile(
            "([A-Za-z0-9_.~:/?\\#\\[\\]@!$&'()*+,;" + "=-]|%[0-9a-fA-F]{2})+"
        )
        val matcher = allowedUrlCharacters.matcher(uriString)
        var validUri: String? = null
        if (matcher.find()) validUri = matcher.group()
        if (TextUtils.isEmpty(validUri) || uriString.length == validUri!!.length)
            return uriString

        val uri = Uri.parse(uriString)
        val uriBuilder = Uri.Builder().scheme(uri.scheme).authority(uri.authority)
        for (path in uri.pathSegments) uriBuilder.appendPath(path)
        for (key in uri.queryParameterNames)
            uriBuilder.appendQueryParameter(key, uri.getQueryParameter(key))
        return uriBuilder.build().toString()
    }
}
