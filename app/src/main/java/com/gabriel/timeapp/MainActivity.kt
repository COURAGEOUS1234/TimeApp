package com.gabriel.timeapp

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import kotlin.random.Random

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var mainClock: View
    private lateinit var ghostClock: View
    private lateinit var alarmTime: TextView
    private lateinit var alarmState: TextView

    // Every few seconds the main clock "glitches": jitter + flicker with a cyan ghost split.
    private val glitch = object : Runnable {
        override fun run() {
            val bursts = Random.nextInt(3, 7)
            for (i in 0 until bursts) {
                handler.postDelayed({
                    val dx = Random.nextInt(-14, 15).toFloat()
                    mainClock.translationX = dx
                    mainClock.alpha = Random.nextDouble(0.55, 1.0).toFloat()
                    ghostClock.translationX = -dx * 1.6f
                    ghostClock.alpha = 0.7f
                }, i * 60L)
            }
            handler.postDelayed({
                mainClock.translationX = 0f; mainClock.alpha = 1f
                ghostClock.translationX = 0f; ghostClock.alpha = 0f
            }, bursts * 60L)
            handler.postDelayed(this, Random.nextLong(3000, 8000))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mainClock = findViewById(R.id.mainClock)
        ghostClock = findViewById(R.id.ghostClock)
        alarmTime = findViewById(R.id.alarmTime)
        alarmState = findViewById(R.id.alarmState)

        val bar = findViewById<View>(R.id.alarmBar)
        bar.setOnClickListener { pickAlarm() }
        bar.setOnLongClickListener {
            AlarmScheduler.clear(this); refreshAlarm()
            Toast.makeText(this, "ALARM DISARMED", Toast.LENGTH_SHORT).show(); true
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    }

    private fun pickAlarm() {
        TimePickerDialog(
            this, android.R.style.Theme_Material_Dialog_Alert,
            { _, h, m ->
                AlarmScheduler.set(this, h, m); refreshAlarm()
                Toast.makeText(this, "ALARM ARMED", Toast.LENGTH_SHORT).show()
            },
            AlarmScheduler.hour(this), AlarmScheduler.minute(this),
            DateFormat.is24HourFormat(this)
        ).show()
    }

    private fun refreshAlarm() {
        if (AlarmScheduler.isSet(this)) {
            alarmTime.text = String.format("%02d:%02d", AlarmScheduler.hour(this), AlarmScheduler.minute(this))
            alarmState.text = "▮ ARMED"
            alarmState.setTextColor(0xFFFF2A2A.toInt())
        } else {
            alarmTime.text = "--:--"
            alarmState.text = "▯ DISARMED"
            alarmState.setTextColor(0xFF6E6660.toInt())
        }
    }

    override fun onResume() { super.onResume(); refreshAlarm(); handler.postDelayed(glitch, 2000) }
    override fun onPause() { super.onPause(); handler.removeCallbacksAndMessages(null) }
}
