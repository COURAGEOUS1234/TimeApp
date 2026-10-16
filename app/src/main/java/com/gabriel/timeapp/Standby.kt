package com.gabriel.timeapp

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.service.dreams.DreamService
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import kotlin.math.min
import kotlin.random.Random

/** Dim, burn-in-safe clock: moves to a new spot every minute. Used by standby mode and the screen saver. */
class StandbyClockView(c: Context, a: AttributeSet? = null) : View(c, a) {
    private val timeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Look.digits(c)
        color = Look.standbyTime(c); textAlign = Paint.Align.CENTER
    }
    private val subP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = c.resources.getFont(R.font.share_tech_mono)
        color = 0xFF6E6660.toInt(); textAlign = Paint.Align.CENTER; letterSpacing = 0.15f
    }
    private var ox = 0f
    private var oy = 0f

    private val tick = object : Runnable {
        override fun run() {
            ox = (Random.nextFloat() - 0.5f) * width * 0.3f
            oy = (Random.nextFloat() - 0.5f) * height * 0.4f
            invalidate()
            postDelayed(this, 60_000L - System.currentTimeMillis() % 60_000L + 50)
        }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); post(tick) }
    override fun onDetachedFromWindow() { removeCallbacks(tick); super.onDetachedFromWindow() }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)
        val now = System.currentTimeMillis()
        val is24 = Fmt.is24(context)
        timeP.textSize = min(width.toFloat(), height * 1.6f) * 0.24f
        subP.textSize = timeP.textSize * 0.2f
        val cx = width / 2f + ox
        val cy = height / 2f + oy
        val time = Fmt.clock(now, is24) + if (is24) "" else " " + Fmt.ampm(now)
        if (!is24) timeP.textSize *= 0.8f
        canvas.drawText(time, cx, cy, timeP)
        canvas.drawText(Fmt.date(now), cx, cy + subP.textSize * 2f, subP)
        AlarmScheduler.nextAlarm(context)?.let { (at, _) ->
            val day = Fmt.date(at).substringBefore(",")
            val t = Fmt.clock(at, is24) + if (is24) "" else " " + Fmt.ampm(at)
            canvas.drawText("ALARM $day $t", cx, cy + subP.textSize * 3.6f, subP)
        }
    }
}

class StandbyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= 27) setShowWhenLocked(true)
        if (Store.settings(this).standbyDim) {
            window.attributes = window.attributes.apply { screenBrightness = 0.02f }
        }
        val v = StandbyClockView(this)
        setContentView(v)
        if (Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
        val gd = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean { finish(); return true }
            override fun onDown(e: MotionEvent) = true
        })
        v.setOnTouchListener { _, e -> gd.onTouchEvent(e) }
        Toast.makeText(this, "Double-tap to exit standby", Toast.LENGTH_SHORT).show()
    }
}

/** Android screen saver ("Screen saver" in Display settings) — shows while charging/docked. */
class ClockDream : DreamService() {
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = !Store.settings(this).standbyDim
        setContentView(StandbyClockView(this))
    }
}
