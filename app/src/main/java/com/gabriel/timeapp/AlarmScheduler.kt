package com.gabriel.timeapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AlarmScheduler {
    const val ACTION_ALARM = "com.gabriel.timeapp.ALARM"
    const val ACTION_SNOOZE = "com.gabriel.timeapp.SNOOZE"
    const val ACTION_TIMER = "com.gabriel.timeapp.TIMER"
    const val ACTION_POMODORO = "com.gabriel.timeapp.POMODORO"
    private const val SNOOZE_BASE = 100_000
    private const val TIMER_CODE = 900_001
    private const val POMO_CODE = 900_002

    fun am(c: Context): AlarmManager = c.getSystemService(AlarmManager::class.java)

    private fun pi(c: Context, action: String, code: Int, id: Int = -1): PendingIntent =
        PendingIntent.getBroadcast(
            c, code,
            Intent(c, AlarmReceiver::class.java).setAction(action).putExtra("id", id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** Calendar day for bit index (0 = Monday … 6 = Sunday). */
    private fun bitOf(calDay: Int) = (calDay + 5) % 7

    fun nextTrigger(a: Alarm, now: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, a.hour); set(Calendar.MINUTE, a.minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (a.days == 0) {
            if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis
        }
        for (d in 0..7) {
            if (cal.timeInMillis > now && (a.days and (1 shl bitOf(cal.get(Calendar.DAY_OF_WEEK)))) != 0)
                return cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    fun schedule(c: Context, a: Alarm) {
        if (!a.enabled) { cancel(c, a.id); return }
        alarmClock(c, nextTrigger(a), pi(c, ACTION_ALARM, a.id, a.id))
        Widgets.updateAll(c)
    }

    fun cancel(c: Context, id: Int) {
        am(c).cancel(pi(c, ACTION_ALARM, id, id))
        am(c).cancel(pi(c, ACTION_SNOOZE, SNOOZE_BASE + id, id))
        Widgets.updateAll(c)
    }

    fun snooze(c: Context, id: Int, minutes: Int) =
        alarmClock(c, System.currentTimeMillis() + minutes * 60_000L, pi(c, ACTION_SNOOZE, SNOOZE_BASE + id, id))

    fun scheduleAll(c: Context) = Store.alarms(c).forEach { schedule(c, it) }

    fun nextAlarm(c: Context): Pair<Long, Alarm>? =
        Store.alarms(c).filter { it.enabled }.map { nextTrigger(it) to it }.minByOrNull { it.first }

    fun scheduleTimer(c: Context, at: Long) = exact(c, at, pi(c, ACTION_TIMER, TIMER_CODE))
    fun cancelTimer(c: Context) = am(c).cancel(pi(c, ACTION_TIMER, TIMER_CODE))
    fun schedulePomodoro(c: Context, at: Long) = exact(c, at, pi(c, ACTION_POMODORO, POMO_CODE))
    fun cancelPomodoro(c: Context) = am(c).cancel(pi(c, ACTION_POMODORO, POMO_CODE))

    private fun alarmClock(c: Context, at: Long, op: PendingIntent) {
        val show = PendingIntent.getActivity(
            c, 7, Intent(c, MainActivity::class.java).putExtra(MainActivity.EXTRA_TAB, 0),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            am(c).setAlarmClock(AlarmManager.AlarmClockInfo(at, show), op)
        } catch (e: SecurityException) {
            exact(c, at, op)
        }
    }

    private fun exact(c: Context, at: Long, op: PendingIntent) {
        try {
            am(c).setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, op)
        } catch (e: SecurityException) {
            am(c).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, op)
        }
    }
}
