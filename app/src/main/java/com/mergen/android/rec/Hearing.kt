package com.mergen.android.rec

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.mergen.android.Panel
import com.mergen.android.R

class Hearing(that: Panel) : Thread() {
    private var recorder: AudioRecord? = null
    private var con: Connect? = Connect(that, true)
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
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
            val sent = con?.send(buffer)
            if (sent == null || !sent) {
                Panel.handler?.obtainMessage(
                    Panel.Action.ERROR.ordinal, R.string.recConnectErr, R.string.recSocketImgErr
                )?.sendToTarget()
                Recorder.handler.obtainMessage(Recorder.Action.PAUSE.ordinal).sendToTarget()
                interrupt()
            }
        }
    }

    override fun interrupt() {
        con = null
        status = false
        recorder?.release()
        super.interrupt()
    }
}
