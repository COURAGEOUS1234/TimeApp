package com.gabriel.timeapp

import android.app.Activity
import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

/** Full-screen alert for alarms and the timer: sound, vibration, flashing background. */
class AlarmActivity : Activity() {
    companion object {
        const val EXTRA_KIND = "kind"
        const val EXTRA_ID = "id"
        const val KIND_ALARM = "alarm"
        const val KIND_TIMER = "timer"
        private const val AUTO_STOP_MS = 10 * 60_000L
    }

    private lateinit var sound: AlarmSound
    private val handler = Handler(Looper.getMainLooper())
    private var flashOn = false
    private var alarm: Alarm? = null
    private var done = false
    private lateinit var root: View
    private lateinit var title: View

    private val flash = object : Runnable {
        override fun run() {
            flashOn = !flashOn
            root.setBackgroundColor(if (flashOn) 0xFF2E1E00.toInt() else 0xFF0A0D0B.toInt())
            title.alpha = if (flashOn) 1f else 0.4f
            handler.postDelayed(this, 400)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true); setTurnScreenOn(true)
            getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        setContentView(R.layout.activity_alarm)
        root = findViewById(R.id.alarmRoot)
        title = findViewById(R.id.alertTitle)

        val kind = intent.getStringExtra(EXTRA_KIND) ?: KIND_ALARM
        alarm = if (kind == KIND_ALARM) Store.alarm(this, intent.getIntExtra(EXTRA_ID, -1)) else null

        (title as TextView).text = if (kind == KIND_TIMER) "TIME'S UP" else "ALARM"
        findViewById<TextView>(R.id.alertLabel).text = when {
            kind == KIND_TIMER -> "TIMER FINISHED"
            !alarm?.label.isNullOrBlank() -> alarm!!.label.uppercase()
            else -> "WAKE-UP SIGNAL"
        }

        val snooze = findViewById<Button>(R.id.snooze)
        val a = alarm
        if (a == null) snooze.visibility = View.GONE
        else {
            snooze.text = "SNOOZE ${a.snoozeMin} MIN"
            snooze.setOnClickListener { AlarmScheduler.snooze(this, a.id, a.snoozeMin); finishAlert() }
        }
        findViewById<View>(R.id.dismiss).setOnClickListener { finishAlert() }

        sound = AlarmSound(this)
        sound.start(a?.sound ?: Alarm.SOUND_SIREN, a?.gradual ?: false)
        if (a?.vibrate != false) {
            getSystemService(Vibrator::class.java)
                ?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 400), 0))
        }
        handler.post(flash)
        // Stop ringing after 10 minutes; an unattended alarm snoozes like Samsung's.
        handler.postDelayed({
            if (a != null) AlarmScheduler.snooze(this, a.id, a.snoozeMin)
            finishAlert()
        }, AUTO_STOP_MS)
    }

    private fun finishAlert() {
        if (done) return
        done = true
        sound.stop()
        getSystemService(Vibrator::class.java)?.cancel()
        handler.removeCallbacksAndMessages(null)
        Notifs.cancel(this, Notifs.ID_ALARM)
        finish()
    }

    override fun onDestroy() {
        if (!done) { sound.stop(); getSystemService(Vibrator::class.java)?.cancel() }
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { /* back must not silently kill the alarm */ }
}
