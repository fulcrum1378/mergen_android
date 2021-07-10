package com.mergen.android.com

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class StreamPool(con: Connect): ArrayList<StreamPool.Item>() {
    var active = false
    init {
        active = true
        runBlocking {
            while (active) {
                if (size == 0) delay(1000L)
                else {
                    launch {
                        con.send(this@StreamPool[0].data)
                    }
                    removeAt(0)
                }
            }
        }
    }

    fun destroy() {
        active = false
    }

    data class Item(val time: Long, val data: ByteArray) {
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
