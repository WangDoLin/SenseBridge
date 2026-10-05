package com.sensebridge.ui.ocr

import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.sensebridge.input.vision.OcrEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors
import java.util.concurrent.Future
import javax.inject.Inject

@HiltViewModel
class OcrReaderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ocrEngine: OcrEngine
) : ViewModel() {

    companion object {
        private const val TAG = "OcrReaderViewModel"
        private const val OCR_TARGET_WIDTH = 2560
        private const val OCR_TARGET_HEIGHT = 1920
    }

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null

    private val _isReading = MutableStateFlow(false)
    val isReading: StateFlow<Boolean> = _isReading.asStateFlow()

    private val _isCameraReady = MutableStateFlow(false)
    val isCameraReady: StateFlow<Boolean> = _isCameraReady.asStateFlow()

    private val _lastReadText = MutableStateFlow<String?>(null)
    val lastReadText: StateFlow<String?> = _lastReadText.asStateFlow()

    fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            previewView.post { bindUseCases(cameraProviderFuture, lifecycleOwner, previewView) }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindUseCases(
        providerFuture: Future<ProcessCameraProvider>,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        try {
            val provider = providerFuture.get().also { cameraProvider = it }
            provider.unbindAll()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val capture = buildImageCapture().also { imageCapture = it }

            val viewPort = previewView.viewPort
            if (viewPort == null) Log.w(TAG, "PreviewView viewport unavailable; OCR will use the full frame.")
            val useCaseGroup = UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(capture)
                .apply { viewPort?.let { setViewPort(it) } }
                .build()

            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, useCaseGroup)
            _isCameraReady.value = true
            Log.i(TAG, "OCR CameraX bound successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind CameraX for OCR: ${e.message}", e)
            _isCameraReady.value = false
        }
    }

    private fun buildImageCapture(): ImageCapture {
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(OCR_TARGET_WIDTH, OCR_TARGET_HEIGHT),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
            .build()
        return ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setResolutionSelector(resolutionSelector)
            .build()
    }

    fun captureAndRead() {
        val capture = imageCapture
        if (capture == null) {
            Log.w(TAG, "ImageCapture not initialized yet.")
            return
        }

        _isReading.value = true
        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    processFrameForOcr(imageProxy)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Image capture failed: ${exception.message}", exception)
                    _isReading.value = false
                }
            }
        )
    }

    fun processFrameForOcr(imageProxy: ImageProxy) {
        _isReading.value = true
        ocrEngine.processImageProxy(imageProxy) { recognized ->
            _isReading.value = false
            if (!recognized.isNullOrBlank()) {
                _lastReadText.value = recognized
            }
        }
    }

    fun clearResult() {
        _lastReadText.value = null
    }

    fun stopCamera() {
        try {
            cameraProvider?.unbindAll()
            _isCameraReady.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping OCR camera", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopCamera()
        cameraExecutor.shutdown()
    }
}
