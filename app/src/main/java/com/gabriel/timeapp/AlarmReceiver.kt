package com.gabriel.timeapp

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        val id = intent.getIntExtra("id", -1)
        when (intent.action) {
            AlarmScheduler.ACTION_ALARM -> {
                val a = Store.alarm(c, id) ?: return
                if (!a.enabled) return
                if (a.days == 0) Store.upsertAlarm(c, a.copy(enabled = false))
                else AlarmScheduler.schedule(c, a)
                Alerts.fire(c, AlarmActivity.KIND_ALARM, id)
            }
            AlarmScheduler.ACTION_SNOOZE ->
                if (Store.alarm(c, id) != null) Alerts.fire(c, AlarmActivity.KIND_ALARM, id)
            AlarmScheduler.ACTION_TIMER -> {
                Store.saveTimer(c, TimerState())
                Notifs.cancel(c, Notifs.ID_TIMER)
                Alerts.fire(c, AlarmActivity.KIND_TIMER, -1)
            }
            AlarmScheduler.ACTION_POMODORO -> Pomodoro.onPhaseEnd(c)
        }
    }
}

/** Full-screen alert used by alarms and the timer. */
object Alerts {
    fun fire(c: Context, kind: String, id: Int) {
        Notifs.ensure(c)
        val open = Intent(c, AlarmActivity::class.java)
            .putExtra(AlarmActivity.EXTRA_KIND, kind)
            .putExtra(AlarmActivity.EXTRA_ID, id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        val pi = PendingIntent.getActivity(
            c, 2, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val label = Store.alarm(c, id)?.label?.takeIf { it.isNotBlank() }
        val title = if (kind == AlarmActivity.KIND_TIMER) "Time's up" else (label ?: "Alarm")
        val n = Notification.Builder(c, Notifs.CH_ALARM)
            .setSmallIcon(R.drawable.ic_stat_clock)
            .setColor(0xFFFFB23E.toInt())
            .setContentTitle(title)
            .setContentText("Tap to open")
            .setCategory(Notification.CATEGORY_ALARM)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
        Notifs.nm(c).notify(Notifs.ID_ALARM, n)
        // If the app is in the foreground the full-screen intent only shows a banner.
        try { c.startActivity(open) } catch (_: Exception) { }
    }
}
