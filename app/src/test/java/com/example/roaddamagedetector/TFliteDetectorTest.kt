package com.example.roaddamagedetector

import android.graphics.RectF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TFLiteDetectorUnitTest {

    private val classNames = mapOf(
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

    @Test
    fun testClassMappingIntegrity() {
        assertEquals("D00: Longitudinal Crack", classNames[0])
        assertEquals("D40: Pothole", classNames[5])
        assertEquals("D50: Other Damage", classNames[8])
        assertEquals(9, classNames.size)
    }

    @Test
    fun testTensorDecoding_WithNoDetections() {
        // Mock output buffer [13][8400] with zero confidence scores (no damage image simulation)
        val mockPredictions = Array(13) { FloatArray(8400) { 0.01f } }

        val results = decodeMockTensor(mockPredictions, confidenceThreshold = 0.25f, iouThreshold = 0.45f)
        assertTrue("No detections should be returned for low-confidence tensor output", results.isEmpty())
    }

    @Test
    fun testTensorDecoding_WithValidDamageDetection() {
        val mockPredictions = Array(13) { FloatArray(8400) { 0.0f } }

        // Inject a simulated Pothole (Class Index 5 -> index 4 + 5 = 9 in output tensor) at anchor index 100
        mockPredictions[0][100] = 0.5f  // xCenter
        mockPredictions[1][100] = 0.5f  // yCenter
        mockPredictions[2][100] = 0.2f  // width
        mockPredictions[3][100] = 0.2f  // height
        mockPredictions[9][100] = 0.85f // Class 5 (D40: Pothole) confidence = 85%

        val results = decodeMockTensor(mockPredictions, confidenceThreshold = 0.25f, iouThreshold = 0.45f)

        assertEquals(1, results.size)
        val detection = results[0]
        assertEquals(5, detection.classId)
        assertEquals("D40: Pothole", detection.className)
        assertEquals(0.85f, detection.score, 0.001f)
        assertEquals(0.5f, detection.xCenter, 0.001f)
        assertEquals(0.5f, detection.yCenter, 0.001f)
    }

    @Test
    fun testNMS_DeduplicationOfOverlappingBoxes() {
        val boxA = DetectionResult(5, "D40: Pothole", 0.90f, 0.5f, 0.5f, 0.2f, 0.2f, RectF(0.4f, 0.4f, 0.6f, 0.6f))
        val boxB = DetectionResult(5, "D40: Pothole", 0.80f, 0.51f, 0.51f, 0.2f, 0.2f, RectF(0.41f, 0.41f, 0.61f, 0.61f))

        val nmsResults = applyMockNMS(listOf(boxA, boxB), iouThreshold = 0.45f)

        assertEquals(1, nmsResults.size)
        assertEquals(0.90f, nmsResults[0].score, 0.001f)
    }

    private fun decodeMockTensor(
        predictions: Array<FloatArray>,
        confidenceThreshold: Float,
        iouThreshold: Float
    ): List<DetectionResult> {
        val candidateDetections = mutableListOf<DetectionResult>()
        for (i in 0 until 8400) {
            var maxScore = 0.0f
            var maxClassId = -1
            for (c in 0 until 9) {
                val score = predictions[4 + c][i]
                if (score > maxScore) {
                    maxScore = score
                    maxClassId = c
                }
            }
            if (maxScore >= confidenceThreshold && maxClassId != -1) {
                val xCenter = predictions[0][i]
                val yCenter = predictions[1][i]
                val w = predictions[2][i]
                val h = predictions[3][i]
                val left = Math.max(0.0f, xCenter - w / 2.0f)
                val top = Math.max(0.0f, yCenter - h / 2.0f)
                val right = Math.min(1.0f, xCenter + w / 2.0f)
                val bottom = Math.min(1.0f, yCenter + h / 2.0f)

                candidateDetections.add(
                    DetectionResult(
                        classId = maxClassId,
                        className = classNames[maxClassId] ?: "Unknown",
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
        return applyMockNMS(candidateDetections, iouThreshold)
    }

    private fun applyMockNMS(detections: List<DetectionResult>, iouThreshold: Float): List<DetectionResult> {
        val sortedDetections = detections.sortedByDescending { it.score }.toMutableList()
        val selectedDetections = mutableListOf<DetectionResult>()
        while (sortedDetections.isNotEmpty()) {
            val first = sortedDetections.removeAt(0)
            selectedDetections.add(first)
            val iterator = sortedDetections.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                // Pass the whole DetectionResult instead of the broken RectF
                if (calculateMockIoU(first, next) >= iouThreshold) {
                    iterator.remove()
                }
            }
        }
        return selectedDetections
    }

    private fun calculateMockIoU(a: DetectionResult, b: DetectionResult): Float {
        // Reconstruct the boxes from the normalized Float values
        val aLeft = a.xCenter - a.width / 2f
        val aTop = a.yCenter - a.height / 2f
        val aRight = a.xCenter + a.width / 2f
        val aBottom = a.yCenter + a.height / 2f

        val bLeft = b.xCenter - b.width / 2f
        val bTop = b.yCenter - b.height / 2f
        val bRight = b.xCenter + b.width / 2f
        val bBottom = b.yCenter + b.height / 2f

        val iLeft = maxOf(aLeft, bLeft)
        val iTop = maxOf(aTop, bTop)
        val iRight = minOf(aRight, bRight)
        val iBottom = minOf(aBottom, bBottom)

        val iWidth = maxOf(0f, iRight - iLeft)
        val iHeight = maxOf(0f, iBottom - iTop)
        val intersectionArea = iWidth * iHeight

        val areaA = a.width * a.height
        val areaB = b.width * b.height
        val unionArea = areaA + areaB - intersectionArea

        return if (unionArea <= 0f) 0f else intersectionArea / unionArea
    }
}