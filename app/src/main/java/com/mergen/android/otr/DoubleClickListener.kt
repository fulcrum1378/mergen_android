@file:Suppress("unused")

package com.mergen.android.otr

import android.os.SystemClock
import android.view.View

abstract class DoubleClickListener(val span: Long = 200) : View.OnClickListener {
    private var times: Long = 0

    override fun onClick(v: View) {
        if ((SystemClock.elapsedRealtime() - times) < span) onDoubleClick()
        times = SystemClock.elapsedRealtime()
    }

    abstract fun onDoubleClick()
}
