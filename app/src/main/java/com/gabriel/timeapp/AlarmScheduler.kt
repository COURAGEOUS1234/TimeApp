package com.gabriel.timeapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/** Stores the alarm time and schedules it with the system alarm clock. */
object AlarmScheduler {
    private const val PREFS = "alarm"
    private const val K_HOUR = "hour"
    private const val K_MIN = "min"
    private const val K_ON = "on"

    fun isSet(c: Context) = prefs(c).getBoolean(K_ON, false)
    fun hour(c: Context) = prefs(c).getInt(K_HOUR, 7)
    fun minute(c: Context) = prefs(c).getInt(K_MIN, 0)

    fun set(c: Context, hour: Int, minute: Int) {
        prefs(c).edit().putInt(K_HOUR, hour).putInt(K_MIN, minute).putBoolean(K_ON, true).apply()
        scheduleAt(c, nextOccurrence(hour, minute))
    }

    fun clear(c: Context) {
        prefs(c).edit().putBoolean(K_ON, false).apply()
        am(c).cancel(fireIntent(c))
    }

    /** Re-arm for tomorrow after it fires (daily alarm). */
    fun rearm(c: Context) { if (isSet(c)) scheduleAt(c, nextOccurrence(hour(c), minute(c))) }

    fun snooze(c: Context, minutes: Int = 5) =
        scheduleAt(c, System.currentTimeMillis() + minutes * 60_000L)

    private fun scheduleAt(c: Context, millis: Long) {
        val show = PendingIntent.getActivity(
            c, 1, Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am(c).setAlarmClock(AlarmManager.AlarmClockInfo(millis, show), fireIntent(c))
    }

    private fun nextOccurrence(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    private fun fireIntent(c: Context) = PendingIntent.getBroadcast(
        c, 0, Intent(c, AlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun am(c: Context) = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
