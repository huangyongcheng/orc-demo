package com.example.orcdemo2.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.example.orcdemo2.ml.model.DetectionResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.internal.SupportPreconditions
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class InvoiceParser (private val context: Context) {
    private lateinit var interpreter: Interpreter

    init {
        initializeModel()
    }

    //model_with_metadata.tflite"
    private fun initializeModel() {
        val model = loadMappedFile(context, "distilbert_ocr.tflite")
        interpreter = Interpreter(model!!)
    }

    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        val inputSize = 320  // Match model input shape
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)  // 4 bytes per float * 3 channels
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        resizedBitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            byteBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)  // R
            byteBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)   // G
            byteBuffer.putFloat((pixel and 0xFF) / 255.0f)           // B
        }
        return byteBuffer
    }


    fun detectItems(bitmap: Bitmap): List<DetectionResult> {
        val input = preprocessImage(bitmap)
        val outputLocations = Array(1) { FloatArray(10 * 4) }  // Adjust based on model output
        val outputClasses = Array(1) { FloatArray(10) }
        val outputScores = Array(1) { FloatArray(10) }

        interpreter.run(input, mapOf(
            0 to outputLocations,
            1 to outputClasses,
            2 to outputScores
        ))

        val labels = FileUtil.loadLabels(context, "labels.txt")
        return processOutput(outputLocations[0], outputClasses[0], outputScores[0], labels)
    }

    private fun processOutput(
        locations: FloatArray,
        classes: FloatArray,
        scores: FloatArray,
        labels: List<String>
    ): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()
        for (i in scores.indices) {
            if (scores[i] > 0.5f) {  // Confidence threshold
                results.add(
                    DetectionResult(
                        itemName = labels[classes[i].toInt()],
                        quantity = 1.0f,  // Replace with OCR extraction
                        unitPrice = 0.0f,  // Placeholder
                        boundingBox = RectF(
                            locations[i * 4 + 1],  // x1
                            locations[i * 4 + 0],  // y1
                            locations[i * 4 + 3],  // x2
                            locations[i * 4 + 2]   // y2
                        )
                    )
                )
            }
        }
        return results
    }

    fun extractTextFromBoundingBox(bitmap: Bitmap, box: RectF): String {
        val croppedBitmap = Bitmap.createBitmap(
            bitmap,
            (box.left * bitmap.width).toInt(),
            (box.top * bitmap.height).toInt(),
            ((box.right - box.left) * bitmap.width).toInt(),
            ((box.bottom - box.top) * bitmap.height).toInt()
        )

        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(croppedBitmap, 0)
        var extractedText = ""

        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                extractedText = visionText.text
            }
            .addOnFailureListener { e ->
                Log.e("OCR", "Error: ${e.message}")
            }

        return extractedText
    }


    ///


    @Throws(IOException::class)
    fun loadMappedFile(context: Context, filePath: String): MappedByteBuffer? {
        SupportPreconditions.checkNotNull(context, "Context should not be null.")
        SupportPreconditions.checkNotNull(filePath, "File path cannot be null.")
        if(context !=null) {
            val fileDescriptor = this.context.assets.openFd(filePath)
          //  val fileDescriptor = context?.assets?.open(filePath) ?: throw IllegalStateException("Context not available")

            val var9: MappedByteBuffer
            try {
                val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
                var9 = try {
                    val fileChannel = inputStream.channel
                    val startOffset = fileDescriptor.startOffset
                    val declaredLength = fileDescriptor.declaredLength
                    fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
                } catch (var12: Throwable) {
                    try {
                        inputStream.close()
                    } catch (var11: Throwable) {
                        var12.addSuppressed(var11)
                    }
                    throw var12
                }
                inputStream.close()
            } catch (var13: Throwable) {
                if (fileDescriptor != null) {
                    try {
                        fileDescriptor.close()
                    } catch (var10: Throwable) {
                        var13.addSuppressed(var10)
                    }
                }
                throw var13
            }
            if (fileDescriptor != null) {
                fileDescriptor.close()
            }
            return var9


        }
        return null
    }
}