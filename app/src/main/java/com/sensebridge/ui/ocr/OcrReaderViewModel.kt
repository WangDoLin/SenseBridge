package com.sensebridge.ui.ocr

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import com.sensebridge.input.vision.OcrEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class OcrReaderViewModel @Inject constructor(
    private val ocrEngine: OcrEngine
) : ViewModel() {

    private val _isReading = MutableStateFlow(false)
    val isReading: StateFlow<Boolean> = _isReading.asStateFlow()

    private val _lastReadText = MutableStateFlow<String?>(null)
    val lastReadText: StateFlow<String?> = _lastReadText.asStateFlow()

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
}
