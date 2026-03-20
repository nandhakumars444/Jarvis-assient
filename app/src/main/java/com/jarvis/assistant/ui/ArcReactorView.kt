package com.jarvis.assistant.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Custom arc reactor that draws three rotating rings + a glowing core.
 * Drop this directly in XML as <com.jarvis.assistant.ui.ArcReactorView/>
 * and it replaces the CardView reactor in MainActivity.
 */
class ArcReactorView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : View(ctx, attrs) {

    enum class State { IDLE, LISTENING, THINKING }

    var state: State = State.IDLE
        set(v) { field = v; invalidate() }

    // ── Paints ────────────────────────────────────────────────────────────────
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize  = 22f
        color     = Color.parseColor("#00D4FF")
        typeface  = Typeface.DEFAULT_BOLD
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style      = Paint.Style.STROKE
        strokeWidth = 2f
        color      = Color.parseColor("#3300D4FF")
    }

    // Rotation angles for the three rings
    private var a1 = 0f   // outer, slow CW
    private var a2 = 0f   // mid, medium CCW
    private var a3 = 0f   // inner, fast CW

    private val arcBlue = Color.parseColor("#00D4FF")
    private val arcGold = Color.parseColor("#F0B429")

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r  = min(cx, cy) * 0.92f

        val accent = if (state == State.THINKING) arcGold else arcBlue

        // ── Outer dashed ring ─────────────────────────────────────────────────
        ringPaint.color = Color.argb(100, 0, 212, 255)
        ringPaint.strokeWidth = 2f
        ringPaint.pathEffect = DashPathEffect(floatArrayOf(10f, 8f), a1)
        canvas.drawCircle(cx, cy, r * 0.88f, ringPaint)
        ringPaint.pathEffect = null

        // ── Mid ring (counter-rotate) ─────────────────────────────────────────
        ringPaint.color = Color.argb(70, 0, 212, 255)
        ringPaint.strokeWidth = 1.5f
        ringPaint.pathEffect = DashPathEffect(floatArrayOf(6f, 12f), a2)
        canvas.drawCircle(cx, cy, r * 0.72f, ringPaint)
        ringPaint.pathEffect = null

        // ── Tick marks on outer ring ──────────────────────────────────────────
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30 + a1 * 0.3f).toDouble())
            val x1 = (cx + (r * 0.88f - 8) * cos(angle)).toFloat()
            val y1 = (cy + (r * 0.88f - 8) * sin(angle)).toFloat()
            val x2 = (cx + r * 0.88f * cos(angle)).toFloat()
            val y2 = (cy + r * 0.88f * sin(angle)).toFloat()
            canvas.drawLine(x1, y1, x2, y2, tickPaint)
        }

        // ── Core glow ─────────────────────────────────────────────────────────
        val coreR = r * 0.38f
        val shader = RadialGradient(
            cx, cy, coreR,
            intArrayOf(
                Color.argb(255, 0, 238, 255),
                Color.argb(200, 0, 130, 180),
                Color.argb(180, 0, 30,  60)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        corePaint.shader = shader
        canvas.drawCircle(cx, cy, coreR, corePaint)

        // Outer glow halo
        val haloR = when (state) {
            State.LISTENING -> coreR * 1.6f
            State.THINKING  -> coreR * 1.4f
            else            -> coreR * 1.2f
        }
        val haloAlpha = when (state) {
            State.LISTENING -> 80
            State.THINKING  -> 60
            else            -> 30
        }
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.shader = RadialGradient(
                cx, cy, haloR,
                intArrayOf(Color.argb(haloAlpha, 0, 212, 255), Color.TRANSPARENT),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, haloR, haloPaint)

        // ── State label ───────────────────────────────────────────────────────
        textPaint.textSize = r * 0.12f
        textPaint.color = accent
        val label = when (state) {
            State.IDLE      -> "TAP"
            State.LISTENING -> "···"
            State.THINKING  -> "THINKING"
        }
        canvas.drawText(label, cx, cy + textPaint.textSize * 0.38f, textPaint)

        // ── Advance rotation & redraw ─────────────────────────────────────────
        val speed = if (state == State.LISTENING) 2.2f else 1.0f
        a1 += 1.2f * speed
        a2 -= 1.8f * speed
        a3 += 3.0f * speed
        postInvalidateOnAnimation()
    }
}
