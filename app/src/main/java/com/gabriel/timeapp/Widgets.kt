package com.gabriel.timeapp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.gabriel.timeapp.ui.Zones
import java.util.TimeZone

class ClockWidget : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) = Widgets.updateAll(c)
}

class WorldWidget : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) = Widgets.updateAll(c)
}

class FocusWidget : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) = Widgets.updateAll(c)

    override fun onReceive(c: Context, intent: Intent) {
        when (intent.action) {
            Widgets.ACTION_TOGGLE -> if (Pomodoro.state(c).running) Pomodoro.pause(c) else Pomodoro.start(c)
            Widgets.ACTION_SKIP -> Pomodoro.skip(c)
            else -> super.onReceive(c, intent)
        }
    }
}

/** Builds the home-screen widgets. Times are TextClock/Chronometer, so the launcher ticks them; we only push state changes. */
object Widgets {
    const val ACTION_TOGGLE = "com.gabriel.timeapp.W_TOGGLE"
    const val ACTION_SKIP = "com.gabriel.timeapp.W_SKIP"

    fun updateAll(c: Context) {
        try {
            val m = AppWidgetManager.getInstance(c) ?: return
            ids(c, m, ClockWidget::class.java).takeIf { it.isNotEmpty() }?.let { m.updateAppWidget(it, clock(c)) }
            ids(c, m, WorldWidget::class.java).takeIf { it.isNotEmpty() }?.let { m.updateAppWidget(it, world(c)) }
            ids(c, m, FocusWidget::class.java).takeIf { it.isNotEmpty() }?.let { m.updateAppWidget(it, focus(c)) }
        } catch (_: Exception) { /* widgets are best-effort */ }
    }

    private fun ids(c: Context, m: AppWidgetManager, cls: Class<*>) = m.getAppWidgetIds(ComponentName(c, cls))

    private fun open(c: Context, tab: Int) = PendingIntent.getActivity(
        c, 200 + tab,
        Intent(c, MainActivity::class.java).putExtra(MainActivity.EXTRA_TAB, tab)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun broadcast(c: Context, action: String, code: Int) = PendingIntent.getBroadcast(
        c, code, Intent(c, FocusWidget::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** Respect the app's 12/24-hour setting rather than only the phone's. */
    private fun format(c: Context, rv: RemoteViews, id: Int, p24: String, p12: String) {
        when (Store.settings(c).timeFormat) {
            "24" -> { rv.setCharSequence(id, "setFormat12Hour", p24); rv.setCharSequence(id, "setFormat24Hour", p24) }
            "12" -> { rv.setCharSequence(id, "setFormat12Hour", p12); rv.setCharSequence(id, "setFormat24Hour", p12) }
            else -> { rv.setCharSequence(id, "setFormat12Hour", p12); rv.setCharSequence(id, "setFormat24Hour", p24) }
        }
    }

    private fun pre(c: Context) = if (Look.tower(c)) "— " else "▸ "

    private fun clock(c: Context): RemoteViews {
        val tower = Look.tower(c)
        val rv = RemoteViews(c.packageName, if (tower) R.layout.widget_clock_tower else R.layout.widget_clock_tactical)
        val now = System.currentTimeMillis()
        format(c, rv, R.id.w_time, "HH:mm", "h:mm")
        format(c, rv, R.id.w_ampm, "", "a")
        rv.setTextViewText(R.id.w_label, pre(c) + "LOCAL · " + Fmt.gmt(TimeZone.getDefault(), now))
        val next = AlarmScheduler.nextAlarm(c)
        rv.setTextViewText(
            R.id.w_alarm,
            if (next == null) "NO ALARMS SET" else {
                val is24 = Fmt.is24(c)
                val t = Fmt.clock(next.first, is24) + if (is24) "" else " " + Fmt.ampm(next.first)
                "ALARM " + Fmt.date(next.first).substringBefore(",") + " " + t
            }
        )
        rv.setOnClickPendingIntent(R.id.w_root, open(c, MainActivity.TAB_ALARM))
        return rv
    }

    private val rowIds = intArrayOf(R.id.w_row0, R.id.w_row1, R.id.w_row2, R.id.w_row3)
    private val cityIds = intArrayOf(R.id.w_city0, R.id.w_city1, R.id.w_city2, R.id.w_city3)
    private val tzIds = intArrayOf(R.id.w_tz0, R.id.w_tz1, R.id.w_tz2, R.id.w_tz3)

    private fun world(c: Context): RemoteViews {
        val rv = RemoteViews(c.packageName, if (Look.tower(c)) R.layout.widget_world_tower else R.layout.widget_world_tactical)
        val zones = listOf(TimeZone.getDefault().id) + Store.cities(c).take(3)
        for (i in 0 until 4) {
            if (i < zones.size) {
                rv.setViewVisibility(rowIds[i], View.VISIBLE)
                rv.setTextViewText(cityIds[i], if (i == 0) "LOCAL" else Zones.city(zones[i]).uppercase())
                rv.setString(tzIds[i], "setTimeZone", zones[i])
                format(c, rv, tzIds[i], "HH:mm", "h:mm a")
            } else rv.setViewVisibility(rowIds[i], View.GONE)
        }
        rv.setOnClickPendingIntent(R.id.w_root, open(c, MainActivity.TAB_WORLD))
        return rv
    }

    private fun focus(c: Context): RemoteViews {
        val rv = RemoteViews(c.packageName, if (Look.tower(c)) R.layout.widget_focus_tower else R.layout.widget_focus_tactical)
        val cfg = Store.pomoConfig(c)
        val s = Pomodoro.state(c)
        val now = System.currentTimeMillis()
        val rem = Pomodoro.remaining(s, cfg, now)
        val total = Pomodoro.phaseMs(cfg, s.phase)
        rv.setTextViewText(R.id.w_phase, pre(c) + Pomodoro.label(s.phase).uppercase())
        rv.setTextColor(R.id.w_phase, if (s.phase == Pomodoro.FOCUS) Look.accent(c) else Look.second(c))
        rv.setChronometer(R.id.w_chrono, SystemClock.elapsedRealtime() + rem, null, s.running)
        rv.setChronometerCountDown(R.id.w_chrono, true)
        rv.setTextViewText(R.id.w_round, "ROUND ${s.round} / ${cfg.rounds} · ${s.doneToday} TODAY")
        val started = s.running || (s.remaining in 0 until total)
        rv.setTextViewText(R.id.w_toggle, when { s.running -> "PAUSE"; started -> "RESUME"; else -> "START" })
        rv.setOnClickPendingIntent(R.id.w_toggle, broadcast(c, ACTION_TOGGLE, 301))
        rv.setOnClickPendingIntent(R.id.w_skip, broadcast(c, ACTION_SKIP, 302))
        rv.setOnClickPendingIntent(R.id.w_root, open(c, MainActivity.TAB_POMODORO))
        return rv
    }
}
