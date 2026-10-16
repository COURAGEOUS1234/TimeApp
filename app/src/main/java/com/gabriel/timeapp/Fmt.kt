package com.gabriel.timeapp

import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

object Fmt {
    fun is24(c: Context): Boolean = when (Store.settings(c).timeFormat) {
        "24" -> true
        "12" -> false
        else -> DateFormat.is24HourFormat(c)
    }

    private fun f(p: String, tz: TimeZone) = SimpleDateFormat(p, Locale.US).apply { timeZone = tz }

    fun clock(ms: Long, is24: Boolean, tz: TimeZone = TimeZone.getDefault(), seconds: Boolean = false): String =
        f(if (is24) (if (seconds) "HH:mm:ss" else "HH:mm") else (if (seconds) "h:mm:ss" else "h:mm"), tz).format(Date(ms))

    fun ampm(ms: Long, tz: TimeZone = TimeZone.getDefault()): String = f("a", tz).format(Date(ms))

    fun date(ms: Long, tz: TimeZone = TimeZone.getDefault()): String =
        f("EEE, d MMM", tz).format(Date(ms)).uppercase(Locale.US)

    fun hm(hour: Int, minute: Int, is24: Boolean): String =
        if (is24) "%02d:%02d".format(hour, minute)
        else "%d:%02d".format(if (hour % 12 == 0) 12 else hour % 12, minute)

    fun ampmOf(hour: Int) = if (hour < 12) "AM" else "PM"

    fun stopwatch(ms: Long): String {
        val h = ms / 3_600_000; val m = (ms / 60_000) % 60; val s = (ms / 1000) % 60; val cs = (ms % 1000) / 10
        return if (h > 0) "%d:%02d:%02d.%02d".format(h, m, s, cs) else "%02d:%02d.%02d".format(m, s, cs)
    }

    /** Rounded up to the next second, like a countdown should be. */
    fun countdown(ms: Long): String {
        val t = (ms + 999) / 1000
        val h = t / 3600; val m = (t / 60) % 60; val s = t % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    fun hms(ms: Long): String {
        val t = ms / 1000
        return "%02d:%02d:%02d".format(t / 3600, (t / 60) % 60, t % 60)
    }

    private fun parts(deltaMs: Long): Triple<Long, Long, Long> {
        val mins = (deltaMs + 59_999) / 60_000
        return Triple(mins / 1440, (mins % 1440) / 60, mins % 60)
    }

    fun untilLong(deltaMs: Long): String {
        val (d, h, m) = parts(deltaMs)
        val p = mutableListOf<String>()
        if (d > 0) p += "$d day" + if (d > 1) "s" else ""
        if (h > 0) p += "$h hour" + if (h > 1) "s" else ""
        if (m > 0) p += "$m minute" + if (m > 1) "s" else ""
        return if (p.isEmpty()) "less than a minute" else p.joinToString(" and ")
    }

    fun untilShort(deltaMs: Long): String {
        val (d, h, m) = parts(deltaMs)
        return buildList {
            if (d > 0) add("${d}d"); if (h > 0) add("${h}h"); if (m > 0 || (d == 0L && h == 0L)) add("${m}m")
        }.joinToString(" ")
    }

    /** "T-07:12", or "T-1D 03:12" beyond a day. */
    fun tminus(deltaMs: Long): String {
        val (d, h, m) = parts(deltaMs)
        return if (d > 0) "T-%dD %02d:%02d".format(d, h, m) else "T-%02d:%02d".format(h, m)
    }

    fun days(a: Alarm, now: Long): String {
        if (a.days == 0) {
            val dayNow = (now + TimeZone.getDefault().getOffset(now)) / 86_400_000L
            val next = AlarmScheduler.nextTrigger(a, now)
            val dayNext = (next + TimeZone.getDefault().getOffset(next)) / 86_400_000L
            return if (dayNext == dayNow) "Today" else "Tomorrow"
        }
        if (a.days == 0x7F) return "Every day"
        if (a.days == 0x1F) return "Weekdays"
        if (a.days == 0x60) return "Weekends"
        val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        return names.filterIndexed { i, _ -> (a.days and (1 shl i)) != 0 }.joinToString(" ")
    }

    fun offset(offMs: Int): String {
        if (offMs == 0) return "Same time"
        val m = abs(offMs) / 60_000; val h = m / 60; val mm = m % 60
        val amount = if (mm == 0) "$h hr" else if (h == 0) "$mm min" else "$h hr $mm min"
        return amount + if (offMs > 0) " ahead" else " behind"
    }

    fun gmt(tz: TimeZone, now: Long): String {
        val o = tz.getOffset(now) / 60_000
        return "GMT%s%d:%02d".format(if (o < 0) "-" else "+", abs(o) / 60, abs(o) % 60)
    }
}
