package com.mergen.android.com

import android.os.CountDownTimer

class StreamPool(val con: Connect) : ArrayList<StreamPool.Item>() {
    companion object {
        const val period = 1000L
        var active = true
        var gotError = 0
    }

    init {
        act()
    }

    fun act() {
        if (active) when {
            gotError > 0 -> {
                clear()
                gotError--
                act()
            }
            size > 0 -> {
                con.send(this@StreamPool[0].data)
                if (size > 0) // IF STILL NOT EMPTY
                    removeAt(0)
                act()
            }
            else -> object : CountDownTimer(period, period) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() {
                    act()
                }
            }.start()
        }
    }

    fun destroy() {
        active = false
        clear()
    }


    class Item(val time: Long, val data: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Item
            if (time != other.time) return false
            if (!data.contentEquals(other.data)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = time.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }
}
