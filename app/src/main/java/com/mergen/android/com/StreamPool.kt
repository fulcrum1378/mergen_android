package com.mergen.android.com

class StreamPool(val con: Connect) : ArrayList<StreamPool.Item>() {
    companion object {
        const val period = 1000L
        var active = true
    }

    init {
        Thread { act() }.start()
    }// Looper.myLooper() != Looper.getMainLooper()

    fun act() {
        if (!active) return
        if (size > 0) {
            con.send(this@StreamPool[0].data)
            //if (size > 0) // IF STILL NOT EMPTY
            //    removeAt(0)
            act()
        } else {
            Thread.sleep(period)
            act()
        }
    }

    fun destroy() {
        clear()
        active = false
    }


    data class Item(val time: Long, val data: ByteArray)
}
