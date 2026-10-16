package com.gabriel.timeapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires at alarm time: posts a full-screen notification that opens AlarmActivity. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        AlarmScheduler.rearm(c)

        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Alarm", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null) // AlarmActivity plays the siren itself
            }
        )

        val open = Intent(c, AlarmActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        val pi = PendingIntent.getActivity(
            c, 2, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val n = Notification.Builder(c, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("警告 // ALARM")
            .setContentText("Tap to terminate")
            .setCategory(Notification.CATEGORY_ALARM)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
        nm.notify(NOTIF_ID, n)

        // If the app is in the foreground the full-screen intent may only show a banner;
        // launching directly covers that case (allowed while the app is visible).
        try { c.startActivity(open) } catch (_: Exception) { }
    }

    companion object {
        const val CHANNEL = "alarm_channel"
        const val NOTIF_ID = 42
    }
}
