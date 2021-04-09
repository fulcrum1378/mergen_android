package org.ifaco.mergen.rec

import android.Manifest
import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.util.Rational
import android.util.Size
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import org.ifaco.mergen.Fun
import org.ifaco.mergen.Fun.Companion.c
import org.ifaco.mergen.Panel
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@SuppressLint("UnsafeExperimentalUsageError", "RestrictedApi")
class Recorder(val that: Panel, val previewView: PreviewView) {
    companion object {
        const val camPerm = Manifest.permission.CAMERA
        const val audPerm = Manifest.permission.RECORD_AUDIO
        const val req = 786
        const val FRAME = 5000L
        val size = Size(1200, 800)
    }

    var canPreview = false

    init {
        if (!Fun.permGranted(camPerm) || !Fun.permGranted(audPerm))
            ActivityCompat.requestPermissions(that, arrayOf(camPerm, audPerm), req)
        else {
            canPreview = true
            this.start()
        }
    }

    lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    lateinit var cameraProvider: ProcessCameraProvider
    lateinit var useCaseGroup: UseCaseGroup
    lateinit var preview: Preview
    lateinit var videoCapture: VideoCapture
    lateinit var cameraSelector: CameraSelector
    var previewing = false
    fun start() {
        if (!canPreview) return
        if (previewing) return
        previewing = true
        cameraProviderFuture = ProcessCameraProvider.getInstance(c)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                preview = Preview.Builder()
                    .setTargetResolution(Size(Fun.dm.widthPixels, Fun.dm.heightPixels))
                    .build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                cameraProvider.unbindAll()
                cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()
                videoCapture = VideoCapture.Builder()
                    .setTargetRotation(that.resources.configuration.orientation)
                    .setMaxResolution(size)
                    .build()
                useCaseGroup = UseCaseGroup.Builder()
                    .addUseCase(preview)
                    .addUseCase(videoCapture)
                    .setViewPort(
                        ViewPort.Builder(
                            Rational(Fun.dm.heightPixels, Fun.dm.widthPixels), // HEIGHT * WIDTH
                            that.resources.configuration.orientation
                        ).build()
                    )
                    .build()
                cameraProvider.bindToLifecycle(that, cameraSelector, useCaseGroup)
                // "CameraSelector.DEFAULT_BACK_CAMERA" instead of "cameraSelector"
            } catch (exc: Exception) {
                Toast.makeText(c, exc.javaClass.name, Toast.LENGTH_SHORT).show()
                canPreview = false
            }
        }, ContextCompat.getMainExecutor(c))
    }

    fun stop() {
        if (!previewing || !canPreview) return
        previewing = false
        preview.setSurfaceProvider(null)
        previewView.removeAllViews()
    }

    var recording = false
    var time = 0
    private var streamer: CountDownTimer? = null
    private var con: Connect? = null
    fun recStart() {
        if (!previewing || recording) return
        con = Connect(that)
        removeCache()
        time = 0
        recording = true
        recResume()
        streamer = object : CountDownTimer(FRAME, FRAME) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                recPause()
                time += 1
                recResume()
            }
        }.start()
    }

    fun recStop() {
        if (!previewing || !recording) return
        recording = false
        streamer?.cancel()
        streamer = null
        con?.end()
    }

    var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    fun recResume() {
        if (!previewing || !recording) return
        val vid = File(c.cacheDir, "$time.mp4")
        videoCapture.startRecording(
            VideoCapture.OutputFileOptions.Builder(vid).build(),
            cameraExecutor, object : VideoCapture.OnVideoSavedCallback {
                override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                    FileInputStream(vid).use {
                        con?.sendable = it.readBytes()
                        it.close()
                    }
                }

                override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                    Panel.handler?.obtainMessage(Panel.Action.TOAST.ordinal, "ERROR: $message")
                        ?.sendToTarget()
                }
            })
    }

    fun recPause() {
        if (!previewing || !recording) return
        videoCapture.stopRecording()
    }

    fun removeCache() {
        c.cacheDir.listFiles()?.forEach { it.delete() }
    }

    fun destroy() {
        if (!canPreview) return
        cameraExecutor.shutdown()
    }
}
