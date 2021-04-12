package org.ifaco.mergen.rec

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import org.ifaco.mergen.Panel

class Hearing(that: Panel): Thread() {
    private var recorder: AudioRecord? = null
    private var con: Connect = Connect(that, true)
    private val sampleRate = 16000 // 44100 for music
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    var minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private var status = true
    private var buffer: ByteArray? = null

    override fun run() {
        buffer = ByteArray(minBufSize)
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate, channelConfig, audioFormat, minBufSize * 10
        )
        recorder!!.startRecording()
        while (status) {
            minBufSize = recorder!!.read(buffer!!, 0, buffer!!.size)
            con.sendable = buffer
        }
    }

    override fun interrupt() {
        status = false
        recorder?.release()
        super.interrupt()
    }
}