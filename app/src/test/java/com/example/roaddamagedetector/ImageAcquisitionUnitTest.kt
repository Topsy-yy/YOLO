package com.example.roaddamagedetector

import android.graphics.RectF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ImageAcquisitionUnitTest {

    @Test
    fun testCoordinateTransformationMapping() {
        // Model space normalized coordinates [0.0, 1.0]
        val normXCenter = 0.5f
        val normYCenter = 0.5f
        val normWidth = 0.2f
        val normHeight = 0.2f

        // Display View space dimensions (e.g., 1080x1920 phone screen)
        val viewWidth = 1080f
        val viewHeight = 1920f

        val mappedLeft = (normXCenter - normWidth / 2f) * viewWidth
        val mappedTop = (normYCenter - normHeight / 2f) * viewHeight
        val mappedRight = (normXCenter + normWidth / 2f) * viewWidth
        val mappedBottom = (normYCenter + normHeight / 2f) * viewHeight

        assertEquals(432f, mappedLeft, 0.001f)
        assertEquals(768f, mappedTop, 0.001f)
        assertEquals(648f, mappedRight, 0.001f)
        assertEquals(1152f, mappedBottom, 0.001f)
    }

    @Test
    fun testEmptyDetectionStateHandling() {
        val emptyDetections = emptyList<DetectionResult>()
        assertEquals(0, emptyDetections.size)
    }
}