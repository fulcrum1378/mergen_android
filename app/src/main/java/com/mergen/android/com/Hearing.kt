package com.mergen.android.com

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

class Hearing : Thread() {
    private var recorder: AudioRecord? = null
    private var con: Connect? = Connect(Controller.earPort)
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_8BIT
    private var buffer: ByteArray? = null
    private var minBufSize =
        AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)// 1000000
    private var status = true

    companion object {
        const val sampleRate = 44100
    }

    override fun run() {
        buffer = ByteArray(minBufSize)
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBufSize * 10
        )
        recorder!!.startRecording()
        while (status) {
            minBufSize = recorder!!.read(buffer!!, 0, buffer!!.size)
            con?.send(buffer)
        }
    }

    override fun interrupt() {
        con = null
        status = false
        recorder?.stop()
        recorder?.release()
        recorder = null
        super.interrupt()
    }
}
