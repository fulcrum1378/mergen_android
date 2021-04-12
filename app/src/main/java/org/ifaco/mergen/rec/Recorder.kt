package org.ifaco.mergen.rec

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
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
    lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    lateinit var cameraProvider: ProcessCameraProvider
    lateinit var useCaseGroup: UseCaseGroup
    lateinit var preview: Preview
    lateinit var imageCapture: ImageCapture
    private lateinit var cameraSelector: CameraSelector
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    var canPreview = false
    var previewing = false
    var recording = true
    var ear: Hearing? = null
    var time = 0
    var con: Connect? = null
    var anRecording: ObjectAnimator? = null

    companion object {
        const val camPerm = Manifest.permission.CAMERA
        const val audPerm = Manifest.permission.RECORD_AUDIO
        const val req = 786
        const val FRAME = 50L
        val size = Size(800, 400)
    }

    init {
        if (!Fun.permGranted(camPerm) || !Fun.permGranted(audPerm))
            ActivityCompat.requestPermissions(that, arrayOf(camPerm, audPerm), req)
        else {
            canPreview = true
            this.start()
        }
    }

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

                useCaseGroup = UseCaseGroup.Builder()
                    .addUseCase(preview)
                    .addUseCase(imageCapture)
                    .setViewPort(
                        ViewPort.Builder(
                            Rational(Fun.dm.heightPixels, Fun.dm.widthPixels), // HEIGHT * WIDTH
                            that.resources.configuration.orientation
                        ).build()
                    )
                    .build()
                cameraProvider.bindToLifecycle(that, cameraSelector, useCaseGroup)
                // "CameraSelector.DEFAULT_BACK_CAMERA" instead of "cameraSelector"
            } catch (e: Exception) {
                Toast.makeText(c, "CAMERA INIT ERROR: ${e.message}", Toast.LENGTH_SHORT).show()
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

    fun resume() {
        time = 0
        c.cacheDir.listFiles()?.forEach { it.delete() }
        ear = Hearing(that).also { it.start() }
        con = Connect(that)
        recording = true
        anRecording = Fun.whirl(bRecording, null)
        capture()
    }

    fun capture() {
        if (!recording) return
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

    fun pause() {
        recording = false
        con?.end()
        try {
            ear?.interrupt()
        } catch (ignored: Exception) {
        }
        ear = null
        anRecording = Fun.whirl(bRecording, anRecording)
    }

    fun destroy() {
        if (!canPreview) return
        cameraExecutor.shutdown()
    }
}
