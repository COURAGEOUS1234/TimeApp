package com.gabriel.timeapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock

object Notifs {
    const val CH_ALARM = "alarm_channel"
    const val CH_ONGOING = "ongoing"
    const val CH_POMO = "pomodoro"
    const val CH_POMO_QUIET = "pomodoro_quiet"

    const val ID_ALARM = 42
    const val ID_STOPWATCH = 43
    const val ID_TIMER = 44
    const val ID_POMO = 45
    const val ID_POMO_DONE = 46

    fun nm(c: Context): NotificationManager = c.getSystemService(NotificationManager::class.java)

    fun ensure(c: Context) {
        nm(c).createNotificationChannels(
            listOf(
                NotificationChannel(CH_ALARM, "Alarms and timers", NotificationManager.IMPORTANCE_HIGH)
                    .apply { setSound(null, null) },
                NotificationChannel(CH_ONGOING, "Running stopwatch, timer and pomodoro", NotificationManager.IMPORTANCE_LOW),
                NotificationChannel(CH_POMO, "Pomodoro alerts", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(CH_POMO_QUIET, "Pomodoro alerts (silent)", NotificationManager.IMPORTANCE_HIGH)
                    .apply { setSound(null, null); enableVibration(false) },
            )
        )
    }

    private fun openApp(c: Context, tab: Int): PendingIntent = PendingIntent.getActivity(
        c, 100 + tab,
        Intent(c, MainActivity::class.java).putExtra(MainActivity.EXTRA_TAB, tab)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun base(c: Context, channel: String, tab: Int): Notification.Builder {
        ensure(c)
        return Notification.Builder(c, channel)
            .setSmallIcon(R.drawable.ic_stat_clock)
            .setColor(0xFFFF2A2A.toInt())
            .setContentIntent(openApp(c, tab))
            .setOnlyAlertOnce(true)
    }

    fun stopwatch(c: Context, s: StopwatchState) {
        val elapsed = s.elapsed(SystemClock.elapsedRealtime())
        val b = base(c, CH_ONGOING, MainActivity.TAB_STOPWATCH).setContentTitle("Stopwatch").setOngoing(s.running)
        if (s.running) b.setUsesChronometer(true).setShowWhen(true)
            .setWhen(System.currentTimeMillis() - elapsed).setContentText("Running")
        else b.setShowWhen(false).setContentText("Paused · " + Fmt.stopwatch(elapsed))
        nm(c).notify(ID_STOPWATCH, b.build())
    }

    fun timer(c: Context, t: TimerState) {
        val b = base(c, CH_ONGOING, MainActivity.TAB_TIMER).setContentTitle("Timer").setOngoing(t.running)
        if (t.running) b.setUsesChronometer(true).setChronometerCountDown(true).setShowWhen(true)
            .setWhen(t.endAt).setContentText("Running")
        else b.setShowWhen(false).setContentText("Paused · " + Fmt.countdown(t.remaining))
        nm(c).notify(ID_TIMER, b.build())
    }

    fun pomodoro(c: Context, s: PomoState, cfg: PomoConfig) {
        val b = base(c, CH_ONGOING, MainActivity.TAB_POMODORO)
            .setContentTitle("Pomodoro · " + Pomodoro.label(s.phase))
            .setOngoing(s.running)
        if (s.running) b.setUsesChronometer(true).setChronometerCountDown(true).setShowWhen(true)
            .setWhen(s.endAt).setContentText("Round ${s.round} of ${cfg.rounds}")
        else b.setShowWhen(false).setContentText(
            "Paused · " + Fmt.countdown(Pomodoro.remaining(s, cfg, System.currentTimeMillis()))
        )
        nm(c).notify(ID_POMO, b.build())
    }

    fun pomodoroDone(c: Context, finished: String, next: PomoState, cfg: PomoConfig) {
        val nextMin = Pomodoro.phaseMs(cfg, next.phase) / 60_000
        val what = if (next.phase == Pomodoro.FOCUS) "Time to focus ($nextMin min)"
        else "Take a $nextMin min ${Pomodoro.label(next.phase).lowercase()}"
        val b = base(c, if (cfg.sound) CH_POMO else CH_POMO_QUIET, MainActivity.TAB_POMODORO)
            .setContentTitle(Pomodoro.label(finished) + " complete")
            .setContentText(if (next.running) what else "$what. Tap to start.")
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
        nm(c).notify(ID_POMO_DONE, b.build())
    }

    fun cancel(c: Context, id: Int) = nm(c).cancel(id)
}
