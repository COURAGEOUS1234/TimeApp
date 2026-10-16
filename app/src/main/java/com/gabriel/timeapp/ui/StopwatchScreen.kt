package com.gabriel.timeapp.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.timeapp.Fmt
import com.gabriel.timeapp.Notifs
import com.gabriel.timeapp.StopwatchState
import com.gabriel.timeapp.Store

@Composable
fun StopwatchScreen(tick: Int) {
    val ctx = LocalContext.current
    var s by remember(tick) { mutableStateOf(Store.stopwatch(ctx)) }
    val now = rememberFrameClock(s.running)
    val elapsed = s.elapsed(now)

    fun update(n: StopwatchState) {
        s = n
        Store.saveStopwatch(ctx, n)
        if (n.running || n.acc > 0) Notifs.stopwatch(ctx, n) else Notifs.cancel(ctx, Notifs.ID_STOPWATCH)
    }
    fun t() = SystemClock.elapsedRealtime()

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(48.dp))
        Text(
            Fmt.stopwatch(elapsed), color = if (s.running) Red else Bone, fontFamily = Stencil, fontSize = 54.sp,
            style = TextStyle(shadow = if (s.running) Shadow(Red, blurRadius = 24f) else null),
        )
        Spacer(Modifier.height(32.dp))
        if (s.laps.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Text("Lap", color = Ash, fontSize = 13.sp, modifier = Modifier.weight(0.6f))
                Text("Lap time", color = Ash, fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                Text("Overall", color = Ash, fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
            HorizontalDivider(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = Panel2)
            val rows = s.laps.mapIndexed { i, total -> Triple(i + 1, total - (if (i > 0) s.laps[i - 1] else 0L), total) }.reversed()
            LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp)) {
                items(rows, key = { it.first }) { (n, lap, total) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text("%02d".format(n), color = Bone, modifier = Modifier.weight(0.6f))
                        Text(Fmt.stopwatch(lap), color = Bone, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Text(Fmt.stopwatch(total), color = Ash, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                }
            }
        } else Spacer(Modifier.weight(1f))

        ButtonRow {
            when {
                s.running -> {
                    ActionButton("Lap", false, Modifier.weight(1f)) { update(s.copy(laps = s.laps + s.elapsed(t()))) }
                    ActionButton("Stop", true, Modifier.weight(1f)) { update(s.copy(running = false, acc = s.elapsed(t()))) }
                }
                elapsed > 0 -> {
                    ActionButton("Reset", false, Modifier.weight(1f)) { update(StopwatchState()) }
                    ActionButton("Resume", true, Modifier.weight(1f)) { update(s.copy(running = true, base = t())) }
                }
                else -> ActionButton("Start", true, Modifier.weight(1f)) { update(s.copy(running = true, base = t())) }
            }
        }
    }
}
