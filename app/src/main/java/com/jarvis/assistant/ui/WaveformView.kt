package com.jarvis.assistant.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

class WaveformView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : View(ctx, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00d4ff")
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private var phase = 0f
    private var active = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!active) return

        val w = width.toFloat()
        val h = height.toFloat()
        val midY = h / 2f
        val amp  = h / 2.5f

        val path = android.graphics.Path()
        path.moveTo(0f, midY)

        var x = 0f
        while (x <= w) {
            val y = midY + amp * sin((x / w * 4 * Math.PI + phase).toFloat()).toFloat()
            if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
            x += 4f
        }
        canvas.drawPath(path, paint)

        // Secondary wave (offset, dimmer)
        paint.alpha = 80
        val path2 = android.graphics.Path()
        x = 0f
        while (x <= w) {
            val y = midY + (amp * 0.5f) * sin((x / w * 6 * Math.PI + phase * 1.3f).toFloat()).toFloat()
            if (x == 0f) path2.moveTo(x, y) else path2.lineTo(x, y)
            x += 4f
        }
        canvas.drawPath(path2, paint)
        paint.alpha = 255

        phase += 0.18f
        postInvalidateOnAnimation()
    }

    fun startAnimation() {
        active = true
        invalidate()
    }

    fun stopAnimation() {
        active = false
        invalidate()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) startAnimation() else stopAnimation()
    }
}
