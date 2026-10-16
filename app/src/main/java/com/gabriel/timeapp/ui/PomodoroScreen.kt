package com.gabriel.timeapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gabriel.timeapp.PomoConfig
import com.gabriel.timeapp.Pomodoro
import com.gabriel.timeapp.Store
import com.gabriel.timeapp.Fmt
import kotlinx.coroutines.delay
import kotlin.math.max

@Composable
fun PomodoroScreen(tick: Int) {
    val ctx = LocalContext.current
    var cfg by remember(tick) { mutableStateOf(Store.pomoConfig(ctx)) }
    var st by remember(tick) { mutableStateOf(Pomodoro.state(ctx)) }
    var customize by remember { mutableStateOf(false) }
    val now = rememberNow(250L)
    val total = Pomodoro.phaseMs(cfg, st.phase)
    val rem = Pomodoro.remaining(st, cfg, now)
    val isFocus = st.phase == Pomodoro.FOCUS
    val color = if (isFocus) Accent else Second

    LaunchedEffect(st.running && rem == 0L) {
        if (st.running && rem == 0L) { delay(1500); st = Pomodoro.state(ctx) }
    }
    fun refresh() { st = Pomodoro.state(ctx) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(20.dp))
        Text(Pomodoro.label(st.phase).uppercase(), color = color, fontFamily = Digits, fontSize = 26.sp, letterSpacing = 2.sp)
        Text("Round ${st.round} of ${cfg.rounds}", color = Ash, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        Ring(rem.toFloat() / max(1L, total), color, 260.dp) {
            Text(Fmt.countdown(rem), color = Bone, fontFamily = Digits, fontSize = 52.sp)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 1..cfg.rounds) {
                val done = i < st.round || (i == st.round && !isFocus)
                Box(
                    Modifier.size(12.dp).clip(CircleShape)
                        .background(if (done) Accent else if (i == st.round) Bone else Panel2)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        val mins = st.focusedToday / 60_000
        Text(
            "Today: ${st.doneToday} session${if (st.doneToday == 1) "" else "s"} · " +
                (if (mins >= 60) "${mins / 60}h ${mins % 60}m" else "${mins}m") + " focused",
            color = Ash, fontSize = 13.sp,
        )

        ButtonRow {
            ActionButton("Reset", false, Modifier.weight(1f)) { Pomodoro.reset(ctx); refresh() }
            val started = st.running || (st.remaining in 0 until total)
            ActionButton(
                when { st.running -> "Pause"; started -> "Resume"; else -> "Start" }, true, Modifier.weight(1.3f),
            ) { if (st.running) Pomodoro.pause(ctx) else Pomodoro.start(ctx); refresh() }
            ActionButton("Skip", false, Modifier.weight(1f)) { Pomodoro.skip(ctx); refresh() }
        }

        TextButton(onClick = { customize = true }) {
            Text("Customize  ·  ${cfg.focus} / ${cfg.short} / ${cfg.long} min", color = Accent, fontSize = 15.sp)
        }
        Spacer(Modifier.height(24.dp))
    }

    if (customize) PomodoroSettings(cfg, onDismiss = { customize = false }) { n ->
        cfg = n; Store.savePomoConfig(ctx, n); customize = false; refresh()
    }
}

@Composable
private fun PomodoroSettings(initial: PomoConfig, onDismiss: () -> Unit, onSave: (PomoConfig) -> Unit) {
    var c by remember { mutableStateOf(initial) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Column(Modifier.fillMaxSize()) {
                HazardStripe(Modifier.fillMaxWidth().height(8.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
                    SectionLabel("Customize pomodoro")
                    Spacer(Modifier.height(16.dp))
                    SectionLabel("Durations")
                    StepperRow("Focus", c.focus, "min", 1..120) { c = c.copy(focus = it) }
                    StepperRow("Short break", c.short, "min", 1..60) { c = c.copy(short = it) }
                    StepperRow("Long break", c.long, "min", 1..90) { c = c.copy(long = it) }
                    StepperRow("Rounds before long break", c.rounds, "", 1..12) { c = c.copy(rounds = it) }
                    Spacer(Modifier.height(16.dp))
                    SectionLabel("Flow")
                    ToggleRow("Auto-start breaks", c.autoBreak) { c = c.copy(autoBreak = it) }
                    ToggleRow("Auto-start focus sessions", c.autoFocus) { c = c.copy(autoFocus = it) }
                    Spacer(Modifier.height(16.dp))
                    SectionLabel("Alerts")
                    ToggleRow("Sound", c.sound, "Notification sound when a phase ends") { c = c.copy(sound = it) }
                    ToggleRow("Vibrate", c.vibrate) { c = c.copy(vibrate = it) }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Changes apply from the next phase. A running phase keeps its current length.",
                        color = Ash, fontSize = 12.sp,
                    )
                }
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { c = PomoConfig() }) { Text("Defaults", color = Ash) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Bone) }
                    ActionButton("Save", true) { onSave(c) }
                }
            }
        }
    }
}
