package com.sensebridge.core.ai

import android.app.ActivityManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SlmModelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val DEFAULT_MODEL_NAME = "sense_slm.bin"
        const val ALT_MODEL_NAME = "sense_slm.task"
        const val MIN_RECOMMENDED_RAM_MB = 1500L
    }

    private var customModelPath: String? = null

    fun getModelFile(): File? {
        customModelPath?.let { path ->
            val custom = File(path)
            if (custom.exists() && custom.length() > 0) return custom
        }

        val internalDir = File(context.filesDir, "models")
        val internalBin = File(internalDir, DEFAULT_MODEL_NAME)
        if (internalBin.exists() && internalBin.length() > 0) return internalBin

        val internalTask = File(internalDir, ALT_MODEL_NAME)
        if (internalTask.exists() && internalTask.length() > 0) return internalTask

        extractAssetModelIfPresent(DEFAULT_MODEL_NAME)?.let { return it }
        extractAssetModelIfPresent(ALT_MODEL_NAME)?.let { return it }

        val adbTmpBin = File("/data/local/tmp", DEFAULT_MODEL_NAME)
        if (adbTmpBin.exists() && adbTmpBin.canRead() && adbTmpBin.length() > 0) return adbTmpBin

        val adbTmpTask = File("/data/local/tmp", ALT_MODEL_NAME)
        if (adbTmpTask.exists() && adbTmpTask.canRead() && adbTmpTask.length() > 0) return adbTmpTask

        return null
    }

    private fun extractAssetModelIfPresent(assetName: String): File? {
        val targetDir = File(context.filesDir, "models")
        if (!targetDir.exists()) targetDir.mkdirs()
        val targetFile = File(targetDir, assetName)
        if (targetFile.exists() && targetFile.length() > 0) return targetFile

        return try {
            context.assets.open(assetName).use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (targetFile.exists() && targetFile.length() > 0) targetFile else null
        } catch (e: Exception) {
            null
        }
    }

    fun isModelAvailable(): Boolean = getModelFile() != null

    fun setCustomModelPath(path: String?) {
        customModelPath = path
    }

    fun getAvailableMemoryMb(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return 0L
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem / (1024 * 1024)
    }

    fun isDeviceMemorySufficient(): Boolean =
        getAvailableMemoryMb() >= MIN_RECOMMENDED_RAM_MB
}
