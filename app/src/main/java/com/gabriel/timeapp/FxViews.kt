package com.gabriel.timeapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/** CRT scanlines drawn over the whole screen. */
class ScanlineView(c: Context, a: AttributeSet?) : View(c, a) {
    private val p = Paint().apply { color = 0x22000000; strokeWidth = 2f }
    private val tint = Paint().apply { color = 0x0AFFB23E }
    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), tint)
        var y = 0f
        while (y < height) { canvas.drawLine(0f, y, width.toFloat(), y, p); y += 5f }
    }
}

/** Black/red diagonal hazard tape. */
class HazardStripeView(c: Context, a: AttributeSet?) : View(c, a) {
    private val red = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD9A030.toInt() }
    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(0xFF0A0A0A.toInt())
        val h = height.toFloat(); val w = h * 0.9f
        var x = -h
        while (x < width + h) {
            val path = Path().apply {
                moveTo(x, h); lineTo(x + h, 0f); lineTo(x + h + w, 0f); lineTo(x + w, h); close()
            }
            canvas.drawPath(path, red); x += w * 2
        }
    }
}
