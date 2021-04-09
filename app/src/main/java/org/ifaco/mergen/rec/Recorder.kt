package org.ifaco.mergen.rec

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.util.Rational
import android.util.Size
import android.widget.ImageView
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
class Recorder(
    val that: Panel,
    val bPreview: PreviewView,
    val bRecording: ImageView
) {
    companion object {
        const val camPerm = Manifest.permission.CAMERA
        const val audPerm = Manifest.permission.RECORD_AUDIO
        const val req = 786
        const val FRAME = 50L
        val size = Size(800, 400)
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
    lateinit var imageCapture: ImageCapture
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
                    .build().also { it.setSurfaceProvider(bPreview.surfaceProvider) }
                cameraProvider.unbindAll()
                cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()
                imageCapture = ImageCapture.Builder()
                    .setTargetRotation(that.resources.configuration.orientation)
                    .setMaxResolution(size)
                    .build()
                videoCapture = VideoCapture.Builder()
                    .setTargetRotation(that.resources.configuration.orientation)
                    .setMaxResolution(size)
                    .build()
                useCaseGroup = UseCaseGroup.Builder()
                    .addUseCase(preview)
                    .addUseCase(imageCapture)
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
                strStart()
                val timeout = 60L * 1000L
                object : CountDownTimer(timeout, timeout) {
                    override fun onTick(millisUntilFinished: Long) {}
                    override fun onFinish() {
                        strStop()
                    }
                }.start()
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
        bPreview.removeAllViews()
    }

    var recording = false
    private var time = 0
    private var streamer: CountDownTimer? = null
    private var con: Connect? = null
    private var anRecording: ObjectAnimator? = null
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
        anRecording = Fun.whirl(bRecording, null)
    }

    fun recStop() {
        if (!previewing || !recording) return
        recording = false
        streamer?.cancel()
        streamer = null
        con?.end()
        anRecording = Fun.whirl(bRecording, anRecording)
    }

    var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    fun recResume() {
        if (!previewing || !recording) return
        val vid = File(c.cacheDir, "$time.mp4")
        videoCapture.startRecording(
            VideoCapture.OutputFileOptions.Builder(vid).build(),
            cameraExecutor, object : VideoCapture.OnVideoSavedCallback {
                // NOT UI THREAD
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

    var streaming = true
    fun strStart() {
        time = 0
        removeCache()
        con = Connect(that)
        streaming = true
        capture()
    }

    fun capture() {
        if (!streaming) return
        val vis = File(c.cacheDir, "$time.jpg")
        imageCapture.takePicture(ImageCapture.OutputFileOptions.Builder(vis).build(),
            cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                override fun onError(error: ImageCaptureException) {
                    Panel.handler?.obtainMessage(
                        Panel.Action.TOAST.ordinal, "ImageCaptureException: ${error.message}"
                    )?.sendToTarget()
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    FileInputStream(vis).use {
                        con?.sendable = it.readBytes()
                        it.close()
                    }
                    /*try {
                        vis.delete()
                    } catch (e: Exception) {
                    }*/
                    capture()
                }
            })
    }

    fun strStop() {
        streaming = false
        con?.end()
    }

    fun removeCache() {
        c.cacheDir.listFiles()?.forEach { it.delete() }
    }

    fun destroy() {
        if (!canPreview) return
        cameraExecutor.shutdown()
    }
}
