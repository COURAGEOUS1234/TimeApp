package com.gabriel.timeapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** The system clears alarms on reboot; put ours back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        AlarmScheduler.scheduleAll(c)
        val t = Store.timer(c)
        if (t.active && t.running) AlarmScheduler.scheduleTimer(c, t.endAt)
        val p = Store.pomoState(c)
        if (p.running) AlarmScheduler.schedulePomodoro(c, p.endAt)
        // Stopwatch uses elapsedRealtime, which resets on reboot.
        if (Store.stopwatch(c).running) Store.saveStopwatch(c, StopwatchState())
        Widgets.updateAll(c)
    }
}
