package com.example.roaddamagedetector

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class ExampleUnitTest {

    data class SimpleBox(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        val width: Float get() = max(0f, right - left)
        val height: Float get() = max(0f, bottom - top)
    }

    @Test
    fun detectionResult_coordinates_calculation() {
        val result = DetectionResult(
            classId = 0,
            className = "D00: Longitudinal Crack",
            score = 0.85f,
            xCenter = 0.5f,
            yCenter = 0.5f,
            width = 0.2f,
            height = 0.4f
        )

        val left = result.xCenter - result.width / 2f
        val top = result.yCenter - result.height / 2f
        val right = result.xCenter + result.width / 2f
        val bottom = result.yCenter + result.height / 2f

        assertEquals(0.4f, left, 0.001f)
        assertEquals(0.3f, top, 0.001f)
        assertEquals(0.6f, right, 0.001f)
        assertEquals(0.7f, bottom, 0.001f)
    }

    @Test
    fun iou_calculation_nonOverlappingBoxes() {
        val boxA = SimpleBox(0f, 0f, 10f, 10f)
        val boxB = SimpleBox(20f, 20f, 30f, 30f)

        val iou = calculateIoU(boxA, boxB)
        assertEquals(0f, iou, 0.001f)
    }

    @Test
    fun iou_calculation_identicalBoxes() {
        val boxA = SimpleBox(0f, 0f, 10f, 10f)
        val boxB = SimpleBox(0f, 0f, 10f, 10f)

        val iou = calculateIoU(boxA, boxB)
        assertEquals(1.0f, iou, 0.001f)
    }

    @Test
    fun iou_calculation_partialOverlap() {
        val boxA = SimpleBox(0f, 0f, 10f, 10f)
        val boxB = SimpleBox(5f, 0f, 15f, 10f)

        val iou = calculateIoU(boxA, boxB)
        assertEquals(50f / 150f, iou, 0.001f)
    }

    private fun calculateIoU(boxA: SimpleBox, boxB: SimpleBox): Float {
        val intersectionLeft = max(boxA.left, boxB.left)
        val intersectionTop = max(boxA.top, boxB.top)
        val intersectionRight = min(boxA.right, boxB.right)
        val intersectionBottom = min(boxA.bottom, boxB.bottom)

        val intersectionWidth = max(0f, intersectionRight - intersectionLeft)
        val intersectionHeight = max(0f, intersectionBottom - intersectionTop)
        val intersectionArea = intersectionWidth * intersectionHeight

        val areaA = boxA.width * boxA.height
        val areaB = boxB.width * boxB.height
        val unionArea = areaA + areaB - intersectionArea

        return if (unionArea <= 0f) 0f else intersectionArea / unionArea
    }
}
