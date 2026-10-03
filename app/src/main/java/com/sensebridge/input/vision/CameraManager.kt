package com.sensebridge.input.vision

import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages CameraX lifecycle binding, preview display, and analyzer dispatching.
 */
@Singleton
class CameraManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val objectDetectorEngine: ObjectDetectorEngine
) {
    companion object {
        private const val TAG = "CameraManager"
        private val TARGET_ANALYSIS_RESOLUTION = Size(640, 480)
    }

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    /**
     * Binds CameraX Preview and Object Analysis use-cases to the provided LifecycleOwner.
     */
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView? = null
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val provider = cameraProvider ?: return@addListener
                provider.unbindAll()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Analysis UseCase tuned for battery and performance
                @Suppress("DEPRECATION")
                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(TARGET_ANALYSIS_RESOLUTION)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor, objectDetectorEngine)

                // Optional preview if UI is active
                if (previewView != null) {
                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                    provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                } else {
                    provider.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalysis)
                }

                Log.i(TAG, "CameraX successfully bound to lifecycle.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind CameraX use cases: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopCamera() {
        try {
            cameraProvider?.unbindAll()
            Log.i(TAG, "CameraX stopped.")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping camera", e)
        }
    }
}
