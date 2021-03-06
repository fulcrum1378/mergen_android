package com.mergen.android.com

import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.util.Rational
import android.util.Size
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.mergen.android.Fun
import com.mergen.android.Fun.Companion.c
import com.mergen.android.Panel
import com.mergen.android.Panel.Action
import com.mergen.android.Panel.Companion.handler
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Recorder(val that: Panel, val bPreview: PreviewView) : ToRecord {
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var useCaseGroup: UseCaseGroup
    private lateinit var preview: Preview
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraSelector: CameraSelector
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    var canPreview = false
    var previewing = false
    var recording = false
    var ear: Hearing? = null
    var time: Long = 0L
    var pool: StreamPool = StreamPool(Connect(Controller.visPort))

    companion object {
        const val FRAME = 1L // 50L
        val size = Size(800, 400)
    }

    @SuppressLint("RestrictedApi")
    override fun on() {
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
                    ).build()
                cameraProvider.bindToLifecycle(that, cameraSelector, useCaseGroup)
                // "CameraSelector.DEFAULT_BACK_CAMERA" instead of "cameraSelector"
            } catch (e: Exception) {
                Toast.makeText(c, "CAMERA INIT ERROR: ${e.message}", Toast.LENGTH_SHORT).show()
                canPreview = false
            }
        }, ContextCompat.getMainExecutor(c))
    }

    override fun off() {
        if (!previewing || !canPreview) return
        previewing = false
        preview.setSurfaceProvider(null)
        bPreview.removeAllViews()
    }

    override fun begin() {
        if (!Controller.isAcknowledged) return
        time = 0L
        c.cacheDir.listFiles()?.forEach { it.delete() }
        recording = true
        ear = Hearing().apply { start() }
        handler?.obtainMessage(Action.TOGGLE.ordinal, true)?.sendToTarget()
        capture()
    }

    fun capture() {
        if (!recording) return
        val vis = File(c.cacheDir, "$time.jpg")
        imageCapture.takePicture(ImageCapture.OutputFileOptions.Builder(vis).build(),
            cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                // NOT UI THREAD
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    if (!recording) return
                    runBlocking {
                        FileInputStream(vis).use {
                            pool.add(StreamPool.Item(time, it.readBytes()))
                            it.close()
                        }
                    }
                    try {
                        vis.delete()
                    } catch (e: Exception) {
                    }
                }

                override fun onError(error: ImageCaptureException) {
                    if (!recording) return
                    handler?.obtainMessage(
                        Action.TOAST.ordinal, "ImageCaptureException: ${error.message}"
                    )?.sendToTarget()
                }
            })
        time++
        object : CountDownTimer(FRAME, FRAME) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                capture()
            }
        }.start()
    }

    override fun end() {
        if (recording) handler?.obtainMessage(Action.TOGGLE.ordinal, false)?.sendToTarget()
        recording = false
        pool.clear()
        ear?.pool?.clear()
        ear?.interrupt()
        ear = null
    }

    override fun destroy() {
        if (!canPreview) return
        cameraExecutor.shutdown()
        pool.destroy()
        ear?.pool?.destroy()
    }
}
