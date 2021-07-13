package com.mergen.android.com

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

class Hearing : Thread() {
    private var recorder: AudioRecord? = null
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private var buffer: ByteArray? = null
    private var minBufSize =
        AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)// 1000000
    private var time: Long = 0L
    var active = true
    var pool = StreamPool(Connect(Controller.earPort))

    companion object {
        const val sampleRate = 44100
    }

    override fun run() {
        buffer = ByteArray(minBufSize)
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat,
            minBufSize * 10
        )
        recorder!!.startRecording()
        while (active) {
            minBufSize = recorder!!.read(buffer!!, 0, buffer!!.size)
            pool.add(StreamPool.Item(time, buffer!!))
            time++
        }
    }

    override fun interrupt() {
        active = false
        recorder?.stop()
        recorder?.release()
        recorder = null
        super.interrupt()
    }
}
