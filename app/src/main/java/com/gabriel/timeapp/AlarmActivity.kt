package com.gabriel.timeapp

import android.app.Activity
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.WindowManager

/** Full-screen red alert: siren, vibration, flashing background. */
class AlarmActivity : Activity() {
    private val siren = Siren()
    private val handler = Handler(Looper.getMainLooper())
    private var flashOn = false
    private lateinit var root: View
    private lateinit var kanji: View

    private val flash = object : Runnable {
        override fun run() {
            flashOn = !flashOn
            root.setBackgroundColor(if (flashOn) 0xFF3A0000.toInt() else 0xFF070606.toInt())
            kanji.alpha = if (flashOn) 1f else 0.35f
            handler.postDelayed(this, 400)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true); setTurnScreenOn(true)
            (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
                .requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        setContentView(R.layout.activity_alarm)
        root = findViewById(R.id.alarmRoot)
        kanji = findViewById(R.id.warnKanji)

        findViewById<View>(R.id.dismiss).setOnClickListener { finishAlarm() }
        findViewById<View>(R.id.snooze).setOnClickListener {
            AlarmScheduler.snooze(this); finishAlarm()
        }

        siren.start()
        @Suppress("DEPRECATION")
        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 600, 400), 0)
        )
        handler.post(flash)
    }

    private fun finishAlarm() {
        siren.stop()
        @Suppress("DEPRECATION")
        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).cancel()
        handler.removeCallbacksAndMessages(null)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(AlarmReceiver.NOTIF_ID)
        finish()
    }

    override fun onDestroy() { siren.stop(); handler.removeCallbacksAndMessages(null); super.onDestroy() }

    // Back button must not silently kill the alarm
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { }
}
