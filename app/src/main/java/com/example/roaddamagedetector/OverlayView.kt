package com.example.roaddamagedetector

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var results: List<DetectionResult> = emptyList()

    private val boxPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    private val textBackgroundPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        isAntiAlias = true
    }

    fun setResults(detectionResults: List<DetectionResult>) {
        results = detectionResults
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        for (result in results) {
            val left = (result.xCenter - result.width / 2f) * viewWidth
            val top = (result.yCenter - result.height / 2f) * viewHeight
            val right = (result.xCenter + result.width / 2f) * viewWidth
            val bottom = (result.yCenter + result.height / 2f) * viewHeight

            val rect = RectF(left, top, right, bottom)
            canvas.drawRect(rect, boxPaint)

            val label = "${result.className} ${(result.score * 100).toInt()}%"
            val textWidth = textPaint.measureText(label)
            val textHeight = 40f

            val backgroundRect = RectF(
                left,
                (top - textHeight - 12f).coerceAtLeast(0f),
                left + textWidth + 16f,
                top.coerceAtLeast(textHeight + 12f)
            )
            canvas.drawRect(backgroundRect, textBackgroundPaint)

            canvas.drawText(
                label,
                left + 8f,
                (top - 8f).coerceAtLeast(textHeight),
                textPaint
            )
        }
    }
}
