package com.gabriel.timeapp

import org.json.JSONObject

/** days: bit 0 = Monday … bit 6 = Sunday. 0 = rings once. */
data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val days: Int = 0,
    val label: String = "",
    val enabled: Boolean = true,
    val vibrate: Boolean = true,
    val sound: String = SOUND_SIREN,
    val snoozeMin: Int = 5,
    val gradual: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("hour", hour).put("minute", minute).put("days", days)
        .put("label", label).put("enabled", enabled).put("vibrate", vibrate)
        .put("sound", sound).put("snooze", snoozeMin).put("gradual", gradual)

    companion object {
        const val SOUND_SIREN = "siren"
        const val SOUND_TONE = "ringtone"

        fun fromJson(o: JSONObject) = Alarm(
            id = o.getInt("id"),
            hour = o.getInt("hour"),
            minute = o.getInt("minute"),
            days = o.optInt("days", 0),
            label = o.optString("label", ""),
            enabled = o.optBoolean("enabled", true),
            vibrate = o.optBoolean("vibrate", true),
            sound = o.optString("sound", SOUND_SIREN),
            snoozeMin = o.optInt("snooze", 5),
            gradual = o.optBoolean("gradual", false),
        )
    }
}

data class Settings(
    val timeFormat: String = "system", // system | 12 | 24
    val glitch: Boolean = true,
    val keepScreenOn: Boolean = false,
    val defaultSnooze: Int = 5,
    val standbyDim: Boolean = true,
)

data class StopwatchState(
    val running: Boolean = false,
    val base: Long = 0L,          // SystemClock.elapsedRealtime() when last started
    val acc: Long = 0L,           // time accumulated before the last start
    val laps: List<Long> = emptyList(), // overall time at each lap
) {
    fun elapsed(now: Long) = acc + if (running) now - base else 0L
}

data class TimerState(
    val active: Boolean = false,
    val running: Boolean = false,
    val endAt: Long = 0L,      // wall clock, when running
    val remaining: Long = 0L,  // when paused
    val total: Long = 0L,
)

data class PomoConfig(
    val focus: Int = 25,
    val short: Int = 5,
    val long: Int = 15,
    val rounds: Int = 4,
    val autoBreak: Boolean = true,
    val autoFocus: Boolean = false,
    val sound: Boolean = true,
    val vibrate: Boolean = true,
)

data class PomoState(
    val phase: String = Pomodoro.FOCUS,
    val round: Int = 1,
    val running: Boolean = false,
    val endAt: Long = 0L,
    val remaining: Long = -1L, // -1 = full phase length
    val doneToday: Int = 0,
    val focusedToday: Long = 0L,
    val day: Long = 0L,
)
