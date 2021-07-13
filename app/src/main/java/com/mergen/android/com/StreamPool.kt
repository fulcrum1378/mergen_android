package com.mergen.android.com

import kotlinx.coroutines.runBlocking

class StreamPool(val con: Connect) : ArrayList<StreamPool.Item>() {
    val work = Thread { act() }
    val period = 1000L
    var active = true

    init {
        work.start() // Looper.myLooper() != Looper.getMainLooper()
    }

    fun act() {
        if (!active) return
        if (size > 0) {
            runBlocking { con.send(this@StreamPool[0].data) }
            removeAt(0)
        } else Thread.sleep(period)
        act()
    }

    fun destroy() {
        active = false
        clear()
        work.interrupt()
    }

    data class Item(val time: Long, val data: ByteArray)
}
