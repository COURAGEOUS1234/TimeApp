package com.gabriel.timeapp

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.json.JSONArray

/** All persistence lives here (SharedPreferences). */
object Store {
    private fun p(c: Context) = c.getSharedPreferences("terminal_hour", Context.MODE_PRIVATE)

    // ---- alarms
    fun alarms(c: Context): List<Alarm> {
        val s = p(c).getString("alarms", null) ?: return emptyList()
        return try {
            val a = JSONArray(s)
            (0 until a.length()).map { Alarm.fromJson(a.getJSONObject(it)) }
        } catch (e: Exception) { emptyList() }
    }

    private fun saveAlarms(c: Context, list: List<Alarm>) {
        val a = JSONArray()
        list.sortedBy { it.hour * 60 + it.minute }.forEach { a.put(it.toJson()) }
        p(c).edit().putString("alarms", a.toString()).apply()
    }

    fun alarm(c: Context, id: Int) = alarms(c).firstOrNull { it.id == id }

    fun upsertAlarm(c: Context, alarm: Alarm) {
        val l = alarms(c).toMutableList()
        val i = l.indexOfFirst { it.id == alarm.id }
        if (i >= 0) l[i] = alarm else l.add(alarm)
        saveAlarms(c, l)
    }

    fun deleteAlarm(c: Context, id: Int) = saveAlarms(c, alarms(c).filter { it.id != id })

    fun newAlarmId(c: Context): Int {
        val n = p(c).getInt("next_id", 1)
        p(c).edit().putInt("next_id", n + 1).apply()
        return n
    }

    // ---- world clock
    fun cities(c: Context): List<String> =
        p(c).getString("cities", null)?.split(",")?.filter { it.isNotBlank() }
            ?: listOf("Asia/Tokyo", "Europe/London")

    fun saveCities(c: Context, l: List<String>) {
        p(c).edit().putString("cities", l.joinToString(",")).apply()
        Widgets.updateAll(c)
    }

    // ---- settings
    fun settings(c: Context) = p(c).run {
        Settings(
            timeFormat = getString("s_fmt", "system") ?: "system",
            glitch = getBoolean("s_glitch", true),
            keepScreenOn = getBoolean("s_keep", false),
            defaultSnooze = getInt("s_snooze", 5),
            standbyDim = getBoolean("s_dim", true),
            theme = getString("s_theme", "tactical") ?: "tactical",
        )
    }

    fun saveSettings(c: Context, s: Settings) = p(c).edit()
        .putString("s_fmt", s.timeFormat).putBoolean("s_glitch", s.glitch)
        .putBoolean("s_keep", s.keepScreenOn).putInt("s_snooze", s.defaultSnooze)
        .putBoolean("s_dim", s.standbyDim).putString("s_theme", s.theme).apply()
        .also { Widgets.updateAll(c) }

    fun lastTab(c: Context) = p(c).getInt("tab", 1)
    fun setLastTab(c: Context, t: Int) = p(c).edit().putInt("tab", t).apply()

    // ---- stopwatch
    fun stopwatch(c: Context) = p(c).run {
        StopwatchState(
            running = getBoolean("sw_run", false),
            base = getLong("sw_base", 0L),
            acc = getLong("sw_acc", 0L),
            laps = getString("sw_laps", "")!!.split(",").mapNotNull { it.toLongOrNull() },
        )
    }

    fun saveStopwatch(c: Context, s: StopwatchState) = p(c).edit()
        .putBoolean("sw_run", s.running).putLong("sw_base", s.base).putLong("sw_acc", s.acc)
        .putString("sw_laps", s.laps.joinToString(",")).apply()

    // ---- timer
    fun timer(c: Context) = p(c).run {
        TimerState(
            active = getBoolean("t_active", false),
            running = getBoolean("t_run", false),
            endAt = getLong("t_end", 0L),
            remaining = getLong("t_rem", 0L),
            total = getLong("t_total", 0L),
        )
    }

    fun saveTimer(c: Context, t: TimerState) = p(c).edit()
        .putBoolean("t_active", t.active).putBoolean("t_run", t.running).putLong("t_end", t.endAt)
        .putLong("t_rem", t.remaining).putLong("t_total", t.total).apply()

    fun timerPresets(c: Context): List<Long> =
        (p(c).getString("t_presets", null) ?: "600000,900000,1800000")
            .split(",").mapNotNull { it.toLongOrNull() }

    fun saveTimerPresets(c: Context, l: List<Long>) =
        p(c).edit().putString("t_presets", l.joinToString(",")).apply()

    fun lastTimer(c: Context) = p(c).getLong("t_last", 300_000L)
    fun setLastTimer(c: Context, ms: Long) = p(c).edit().putLong("t_last", ms).apply()

    // ---- pomodoro
    fun pomoConfig(c: Context) = p(c).run {
        PomoConfig(
            focus = getInt("p_focus", 25), short = getInt("p_short", 5),
            long = getInt("p_long", 15), rounds = getInt("p_rounds", 4),
            autoBreak = getBoolean("p_ab", true), autoFocus = getBoolean("p_af", false),
            sound = getBoolean("p_sound", true), vibrate = getBoolean("p_vib", true),
        )
    }

    fun savePomoConfig(c: Context, x: PomoConfig) = p(c).edit()
        .putInt("p_focus", x.focus).putInt("p_short", x.short).putInt("p_long", x.long)
        .putInt("p_rounds", x.rounds).putBoolean("p_ab", x.autoBreak).putBoolean("p_af", x.autoFocus)
        .putBoolean("p_sound", x.sound).putBoolean("p_vib", x.vibrate).apply()

    fun pomoState(c: Context) = p(c).run {
        PomoState(
            phase = getString("ps_phase", Pomodoro.FOCUS) ?: Pomodoro.FOCUS,
            round = getInt("ps_round", 1), running = getBoolean("ps_run", false),
            endAt = getLong("ps_end", 0L), remaining = getLong("ps_rem", -1L),
            doneToday = getInt("ps_done", 0), focusedToday = getLong("ps_focused", 0L),
            day = getLong("ps_day", 0L),
        )
    }

    fun savePomoState(c: Context, s: PomoState) = p(c).edit()
        .putString("ps_phase", s.phase).putInt("ps_round", s.round).putBoolean("ps_run", s.running)
        .putLong("ps_end", s.endAt).putLong("ps_rem", s.remaining).putInt("ps_done", s.doneToday)
        .putLong("ps_focused", s.focusedToday).putLong("ps_day", s.day).apply()

    /** Carry over the single alarm from version 1 of the app. */
    fun migrate(c: Context) {
        val sp = p(c)
        if (sp.getBoolean("migrated_v1", false)) return
        val old = c.getSharedPreferences("alarm", Context.MODE_PRIVATE)
        if (old.getBoolean("on", false)) {
            old.edit().putBoolean("on", false).apply()
            val legacy = PendingIntent.getBroadcast(
                c, 0, Intent(c, AlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (legacy != null) AlarmScheduler.am(c).cancel(legacy)
            val a = Alarm(newAlarmId(c), old.getInt("hour", 7), old.getInt("min", 0), days = 0x7F)
            upsertAlarm(c, a)
            AlarmScheduler.schedule(c, a)
        }
        sp.edit().putBoolean("migrated_v1", true).apply()
    }
}
