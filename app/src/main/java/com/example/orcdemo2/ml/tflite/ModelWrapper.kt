package com.example.orcdemo2.ml.tflite

import android.content.res.AssetManager
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ModelWrapper (
    assetManager: AssetManager,
    modelPath: String = "distilbert_ocr.tflite",
    private val maxSeqLen: Int = 128,
    private var numLabels: Int = 15  // e.g., O, B-ITEM, I-ITEM, etc.
) {
    private val interpreter: Interpreter



    private val labelMap = mapOf(
//        0 to "O",
//        1 to "B-ITEM",
//        2 to "I-ITEM",
//        3 to "B-PRICE",
//        4 to "I-PRICE",
//        5 to "B-QUANTITY",
//        6 to "I-QUANTITY"
        0 to "O",
        1 to "B-ITEM",
        2 to "I-ITEM",
        3 to "B-PRICE",
        4 to "I-PRICE",
        5 to "B-QUANTITY",
        6 to "I-QUANTITY",
        7 to "B-DESCRIPTION",
        8 to "I-DESCRIPTION",
        9 to "B-TOTAL",
        10 to "I-TOTAL",
        11 to "B-DATE",
        12 to "I-DATE",
        13 to "B-VENDOR",
        14 to "I-VENDOR"
    )

    init {
        interpreter = Interpreter(loadModelFile(assetManager, modelPath))

        val outputShape = interpreter.getOutputTensor(0).shape() // [1, 128, N]
        numLabels = outputShape[2]
    }

    private fun loadModelFile(assetManager: AssetManager, filename: String): MappedByteBuffer {
        val fileDescriptor = assetManager.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun predict(inputIds: IntArray, attentionMask: IntArray): List<String> {
//        val input0 = ByteBuffer.allocateDirect(maxSeqLen * 4).order(ByteOrder.nativeOrder())
//        val input1 = ByteBuffer.allocateDirect(maxSeqLen * 4).order(ByteOrder.nativeOrder())

        val input0 = Array(1) { IntArray(maxSeqLen) }
        val input1 = Array(1) { IntArray(maxSeqLen) }

        for (i in 0 until maxSeqLen) {
            input0[0][i] = if (i < inputIds.size) inputIds[i] else 0
            input1[0][i] = if (i < attentionMask.size) attentionMask[i] else 0
        }

//        for (i in 0 until maxSeqLen) {
//            input0.putInt(if (i < inputIds.size) inputIds[i] else 0)
//            input1.putInt(if (i < attentionMask.size) attentionMask[i] else 0)
//        }

//        input0.rewind()
//        input1.rewind()

        val outputBuffer = Array(1) { Array(maxSeqLen) { FloatArray(numLabels) } }

        interpreter.runForMultipleInputsOutputs(arrayOf(input0, input1), mapOf(0 to outputBuffer))

//        val outputBuffer = Array(1) { Array(maxSeqLen) { FloatArray(numLabels) } }
//
//        interpreter.runForMultipleInputsOutputs(arrayOf(input0, input1), mapOf(0 to outputBuffer))

        val predictions = mutableListOf<String>()
        for (i in 0 until maxSeqLen) {
            val logits = outputBuffer[0][i]
            val maxIdx = logits.indices.maxByOrNull { logits[it] } ?: 0
            predictions.add(labelMap[maxIdx] ?: "O")
        }

        return predictions
    }

    fun extractEntities(tokens: List<String>, labels: List<String>): Map<String, List<String>> {
        val entities = mutableMapOf<String, MutableList<String>>()

        var currentLabel = ""
        var currentEntity = ""

        for (i in tokens.indices) {
            val token = tokens[i]
            val label = labels[i]

            when {
                label.startsWith("B-") -> {
                    if (currentEntity.isNotEmpty() && currentLabel.isNotEmpty()) {
                        entities.getOrPut(currentLabel) { mutableListOf() }.add(currentEntity.trim())
                    }
                    currentLabel = label.removePrefix("B-")
                    currentEntity = token
                }

                label.startsWith("I-") && currentLabel == label.removePrefix("I-") -> {
                    currentEntity += " $token"
                }

                else -> {
                    if (currentEntity.isNotEmpty() && currentLabel.isNotEmpty()) {
                        entities.getOrPut(currentLabel) { mutableListOf() }.add(currentEntity.trim())
                        currentEntity = ""
                        currentLabel = ""
                    }
                }
            }
        }

        // Add last entity
        if (currentEntity.isNotEmpty() && currentLabel.isNotEmpty()) {
            entities.getOrPut(currentLabel) { mutableListOf() }.add(currentEntity.trim())
        }

        return entities
    }
}