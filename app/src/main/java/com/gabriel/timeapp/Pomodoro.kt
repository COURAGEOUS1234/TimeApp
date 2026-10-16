package com.gabriel.timeapp

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import kotlin.math.max

object Pomodoro {
    const val FOCUS = "focus"
    const val SHORT = "short"
    const val LONG = "long"

    fun phaseMs(cfg: PomoConfig, phase: String) =
        when (phase) { SHORT -> cfg.short; LONG -> cfg.long; else -> cfg.focus } * 60_000L

    fun label(phase: String) =
        when (phase) { SHORT -> "Short break"; LONG -> "Long break"; else -> "Focus" }

    fun remaining(s: PomoState, cfg: PomoConfig, now: Long): Long = when {
        s.running -> max(0L, s.endAt - now)
        s.remaining >= 0 -> s.remaining
        else -> phaseMs(cfg, s.phase)
    }

    private fun today(): Long = System.currentTimeMillis().let {
        (it + java.util.TimeZone.getDefault().getOffset(it)) / 86_400_000L
    }

    fun state(c: Context): PomoState {
        val s = Store.pomoState(c)
        return if (s.day != today()) s.copy(doneToday = 0, focusedToday = 0L, day = today()) else s
    }

    fun start(c: Context) {
        val cfg = Store.pomoConfig(c)
        val s = state(c)
        val now = System.currentTimeMillis()
        val ns = s.copy(running = true, endAt = now + remaining(s, cfg, now), remaining = -1L)
        Store.savePomoState(c, ns)
        AlarmScheduler.schedulePomodoro(c, ns.endAt)
        Notifs.pomodoro(c, ns, cfg)
    }

    fun pause(c: Context) {
        val cfg = Store.pomoConfig(c)
        val s = state(c)
        val ns = s.copy(running = false, remaining = remaining(s, cfg, System.currentTimeMillis()))
        Store.savePomoState(c, ns)
        AlarmScheduler.cancelPomodoro(c)
        Notifs.pomodoro(c, ns, cfg)
    }

    fun reset(c: Context) {
        val s = state(c)
        Store.savePomoState(c, PomoState(doneToday = s.doneToday, focusedToday = s.focusedToday, day = s.day))
        AlarmScheduler.cancelPomodoro(c)
        Notifs.cancel(c, Notifs.ID_POMO)
    }

    fun skip(c: Context) { advance(c, completed = false) }

    fun onPhaseEnd(c: Context) {
        val finished = state(c).phase
        val ns = advance(c, completed = true)
        val cfg = Store.pomoConfig(c)
        Notifs.pomodoroDone(c, finished, ns, cfg)
        if (cfg.vibrate) {
            c.getSystemService(Vibrator::class.java)
                ?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 400), -1))
        }
    }

    private fun advance(c: Context, completed: Boolean): PomoState {
        val cfg = Store.pomoConfig(c)
        var s = state(c)
        val next: String
        var round = s.round
        if (s.phase == FOCUS) {
            if (completed) s = s.copy(doneToday = s.doneToday + 1, focusedToday = s.focusedToday + phaseMs(cfg, FOCUS))
            next = if (round >= cfg.rounds) LONG else SHORT
        } else {
            next = FOCUS
            round = if (s.phase == LONG) 1 else round + 1
        }
        val auto = if (next == FOCUS) cfg.autoFocus else cfg.autoBreak
        val now = System.currentTimeMillis()
        val ns = s.copy(
            phase = next, round = round, running = auto,
            endAt = if (auto) now + phaseMs(cfg, next) else 0L, remaining = -1L,
        )
        Store.savePomoState(c, ns)
        if (auto) AlarmScheduler.schedulePomodoro(c, ns.endAt) else AlarmScheduler.cancelPomodoro(c)
        Notifs.pomodoro(c, ns, cfg)
        return ns
    }
}
