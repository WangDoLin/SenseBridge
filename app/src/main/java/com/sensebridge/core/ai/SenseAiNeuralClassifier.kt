package com.sensebridge.core.ai

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

data class NeuralIntentPrediction(
    val intent: DialogueIntent,
    val confidence: Float,
    val rawTag: String
)

@Singleton
class SenseAiNeuralClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SenseAiNeuralClassifier"
        private const val MODEL_FILENAME = "sense_ai_neural_model.tflite"
        private const val VOCAB_FILENAME = "sense_ai_vocab.json"
        private const val LABELS_FILENAME = "sense_ai_labels.json"
        private const val MAX_SEQ_LEN = 20
        private const val MIN_CONFIDENCE_THRESHOLD = 0.40f
    }

    private var interpreter: Interpreter? = null
    private val vocab = mutableMapOf<String, Int>()
    private val labels = mutableListOf<String>()
    private var isInitialized = false

    init {
        initialize()
    }

    @Synchronized
    fun initialize(): Boolean {
        if (isInitialized) return true
        return try {
            val modelBuffer = loadModelFile(MODEL_FILENAME)
            interpreter = Interpreter(modelBuffer)
            loadVocab(VOCAB_FILENAME)
            loadLabels(LABELS_FILENAME)
            isInitialized = true
            Log.i(TAG, "SenseAI Neural Classifier initialized with ${labels.size} classes, ${vocab.size} vocab.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SenseAI Neural Classifier", e)
            isInitialized = false
            false
        }
    }

    fun classify(rawText: String): NeuralIntentPrediction {
        if (!isInitialized || interpreter == null) {
            return NeuralIntentPrediction(DialogueIntent.GENERAL, 0.0f, "GENERAL")
        }

        val sequence = textToSequence(rawText)
        val inputArray = arrayOf(sequence)
        val outputProbabilities = Array(1) { FloatArray(labels.size) }

        try {
            interpreter?.run(inputArray, outputProbabilities)
        } catch (e: Exception) {
            Log.e(TAG, "Error executing neural inference", e)
            return NeuralIntentPrediction(DialogueIntent.GENERAL, 0.0f, "GENERAL")
        }

        val probs = outputProbabilities[0]
        var maxIdx = 0
        var maxProb = probs[0]
        for (i in 1 until probs.size) {
            if (probs[i] > maxProb) {
                maxProb = probs[i]
                maxIdx = i
            }
        }

        val predictedTag = if (maxIdx < labels.size) labels[maxIdx] else "GENERAL"
        val mappedIntent = if (maxProb >= MIN_CONFIDENCE_THRESHOLD) {
            try {
                DialogueIntent.valueOf(predictedTag)
            } catch (e: Exception) {
                DialogueIntent.GENERAL
            }
        } else {
            DialogueIntent.GENERAL
        }

        return NeuralIntentPrediction(mappedIntent, maxProb, predictedTag)
    }

    fun isReady(): Boolean = isInitialized && interpreter != null

    fun close() {
        interpreter?.close()
        interpreter = null
        isInitialized = false
    }

    private fun loadModelFile(filename: String): ByteBuffer {
        val fileDescriptor = context.assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadVocab(filename: String) {
        val jsonString = context.assets.open(filename).bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(jsonString)
        vocab.clear()
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            vocab[key] = jsonObject.getInt(key)
        }
    }

    private fun loadLabels(filename: String) {
        val jsonString = context.assets.open(filename).bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(jsonString)
        labels.clear()
        for (i in 0 until jsonArray.length()) {
            labels.add(jsonArray.getString(i))
        }
    }

    private fun textToSequence(text: String): IntArray {
        val clean = text.lowercase().replace(Regex("""[^\w\s]"""), " ").trim()
        val tokens = clean.split(Regex("""\s+""")).filter { it.isNotBlank() }
        val sequence = IntArray(MAX_SEQ_LEN) { 0 }

        val unkId = vocab["<UNK>"] ?: 1
        val padLimit = minOf(tokens.size, MAX_SEQ_LEN)
        for (i in 0 until padLimit) {
            val token = tokens[i]
            sequence[i] = vocab[token] ?: unkId
        }
        return sequence
    }
}
