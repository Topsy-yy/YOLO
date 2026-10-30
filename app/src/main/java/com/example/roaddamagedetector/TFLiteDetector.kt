package com.example.roaddamagedetector

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.MappedByteBuffer
import kotlin.math.max
import kotlin.math.min
import java.io.Serializable


data class DetectionResult(
    val classId: Int,
    val className: String,
    val score: Float,
    val xCenter: Float,
    val yCenter: Float,
    val width: Float,
    val height: Float,
    @Transient val boundingBox: RectF = RectF()
) : java.io.Serializable

class TFLiteDetector(private val context: Context) {

    private var interpreter: Interpreter? = null
    private val modelInputSize = 640

    // RDD2022 Official 9 Class Index Mapping
    val classNames = mapOf(
        0 to "D00: Longitudinal Crack",
        1 to "D01: Longitudinal Lateral Crack",
        2 to "D10: Transverse Crack",
        3 to "D11: Transverse Lateral Crack",
        4 to "D20: Alligator Crack",
        5 to "D40: Pothole",
        6 to "D43: Crosswalk Blur",
        7 to "D44: White Line Deterioration",
        8 to "D50: Other Damage"
    )

    init {
        setupInterpreter()
    }

    @Synchronized
    private fun setupInterpreter() {
        try {
            val modelBuffer: MappedByteBuffer = FileUtil.loadMappedFile(context, "best.tflite")
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(modelBuffer, options)

            // --- ADD THIS DEBUG CODE ---
            val inputShape = interpreter?.getInputTensor(0)?.shape()?.joinToString(",")
            val outputShape = interpreter?.getOutputTensor(0)?.shape()?.joinToString(",")
            Log.d("TFLiteDetector", "MODEL INPUT SHAPE: [$inputShape]")
            Log.d("TFLiteDetector", "MODEL OUTPUT SHAPE: [$outputShape]")
            // ---------------------------

            Log.d("TFLiteDetector", "TFLite interpreter initialized successfully.")
        } catch (e: Exception) {
            Log.e("TFLiteDetector", "Failed to load TFLite model best.tflite", e)
            interpreter = null
        }
    }

    @Synchronized
    fun detect(bitmap: Bitmap, confidenceThreshold: Float = 0.25f, iouThreshold: Float = 0.45f): List<DetectionResult> {
        val activeInterpreter = interpreter ?: run {
            Log.w("TFLiteDetector", "Detector called before model initialization.")
            return emptyList()
        }

        try {
            // 1. Resize the bitmap to 640x640 manually
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, modelInputSize, modelInputSize, true)

            // 2. Extract pixels into a FloatArray
            val intValues = IntArray(modelInputSize * modelInputSize)
            resizedBitmap.getPixels(intValues, 0, modelInputSize, 0, 0, modelInputSize, modelInputSize)

            // 3. Create the input buffer for [1, 3, 640, 640] (CHW format)
            val inputBuffer = Array(1) { Array(3) { Array(modelInputSize) { FloatArray(modelInputSize) } } }

            // 4. Fill the buffer (Normalize pixels to 0.0 - 1.0)
            for (y in 0 until modelInputSize) {
                for (x in 0 until modelInputSize) {
                    val pixel = intValues[y * modelInputSize + x]
                    // Extract RGB values and normalize
                    inputBuffer[0][0][y][x] = ((pixel shr 16) and 0xFF) / 255.0f // Red
                    inputBuffer[0][1][y][x] = ((pixel shr 8) and 0xFF) / 255.0f  // Green
                    inputBuffer[0][2][y][x] = (pixel and 0xFF) / 255.0f         // Blue
                }
            }

            // 5. Allocate Output Buffer [1, 13, 8400]
            val outputBuffer = Array(1) { Array(13) { FloatArray(8400) } }

            // 6. Execute Inference
            activeInterpreter.run(inputBuffer, outputBuffer)

            // 7. Decode Output Tensor
            return decodeTensorOutput(outputBuffer[0], confidenceThreshold, iouThreshold)

        } catch (e: Exception) {
            Log.e("TFLiteDetector", "Error executing TFLite inference", e)
            return emptyList()
        }
    }
    fun decodeTensorOutput(
        predictions: Array<FloatArray>,
        confidenceThreshold: Float,
        iouThreshold: Float
    ): List<DetectionResult> {
        val candidateDetections = mutableListOf<DetectionResult>()

        for (i in 0 until 8400) {
            var maxScore = 0.0f
            var maxClassId = -1

            // Inspect the 9 damage class probabilities at indices 4..12
            for (c in 0 until 9) {
                val score = predictions[4 + c][i]
                if (score > maxScore) {
                    maxScore = score
                    maxClassId = c
                }
            }

            // Confidence Threshold Filtering
            if (maxScore >= confidenceThreshold && maxClassId != -1) {
                val xCenter = predictions[0][i]
                val yCenter = predictions[1][i]
                val w = predictions[2][i]
                val h = predictions[3][i]

                val left = max(0.0f, xCenter - w / 2.0f)
                val top = max(0.0f, yCenter - h / 2.0f)
                val right = min(1.0f, xCenter + w / 2.0f)
                val bottom = min(1.0f, yCenter + h / 2.0f)

                candidateDetections.add(
                    DetectionResult(
                        classId = maxClassId,
                        className = classNames[maxClassId] ?: "Unknown Defect",
                        score = maxScore,
                        xCenter = xCenter,
                        yCenter = yCenter,
                        width = w,
                        height = h,
                        boundingBox = RectF(left, top, right, bottom)
                    )
                )
            }
        }

        // 5. Apply Non-Maximum Suppression (NMS)
        return applyNMS(candidateDetections, iouThreshold)
    }

    fun applyNMS(detections: List<DetectionResult>, iouThreshold: Float): List<DetectionResult> {
        val sortedDetections = detections.sortedByDescending { it.score }.toMutableList()
        val selectedDetections = mutableListOf<DetectionResult>()

        while (sortedDetections.isNotEmpty()) {
            val first = sortedDetections.removeAt(0)
            selectedDetections.add(first)

            val iterator = sortedDetections.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (calculateIoU(first.boundingBox, next.boundingBox) >= iouThreshold) {
                    iterator.remove()
                }
            }
        }
        return selectedDetections
    }

    fun calculateIoU(boxA: RectF, boxB: RectF): Float {
        val intersectionLeft = max(boxA.left, boxB.left)
        val intersectionTop = max(boxA.top, boxB.top)
        val intersectionRight = min(boxA.right, boxB.right)
        val intersectionBottom = min(boxA.bottom, boxB.bottom)

        val intersectionWidth = max(0.0f, intersectionRight - intersectionLeft)
        val intersectionHeight = max(0.0f, intersectionBottom - intersectionTop)
        val intersectionArea = intersectionWidth * intersectionHeight

        val boxAArea = (boxA.right - boxA.left) * (boxA.bottom - boxA.top)
        val boxBArea = (boxB.right - boxB.left) * (boxB.bottom - boxB.top)

        val unionArea = boxAArea + boxBArea - intersectionArea
        return if (unionArea <= 0.0f) 0.0f else intersectionArea / unionArea
    }

    @Synchronized
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}