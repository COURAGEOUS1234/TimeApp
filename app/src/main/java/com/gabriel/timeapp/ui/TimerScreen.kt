@file:OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.gabriel.timeapp.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.timeapp.AlarmScheduler
import com.gabriel.timeapp.Fmt
import com.gabriel.timeapp.Notifs
import com.gabriel.timeapp.Store
import com.gabriel.timeapp.TimerState
import kotlinx.coroutines.delay
import kotlin.math.max

@Composable
fun TimerScreen(tick: Int) {
    val ctx = LocalContext.current
    var t by remember(tick) { mutableStateOf(Store.timer(ctx)) }
    var presets by remember(tick) { mutableStateOf(Store.timerPresets(ctx)) }
    val last = remember { Store.lastTimer(ctx) / 1000 }
    var h by remember { mutableIntStateOf((last / 3600).toInt()) }
    var m by remember { mutableIntStateOf(((last / 60) % 60).toInt()) }
    var sec by remember { mutableIntStateOf((last % 60).toInt()) }
    val now = rememberNow(250L)
    val remaining = if (t.running) max(0L, t.endAt - now) else t.remaining
    val is24 = remember(tick) { Fmt.is24(ctx) }

    LaunchedEffect(t.running && remaining == 0L) {
        if (t.running && remaining == 0L) { delay(1500); t = Store.timer(ctx) }
    }

    fun save(n: TimerState) { t = n; Store.saveTimer(ctx, n) }
    val totalSet = (h * 3600L + m * 60L + sec) * 1000L

    if (!t.active) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Wheel("Hours", h, 99) { h = it }
                Text(":", color = Ash, fontFamily = Stencil, fontSize = 44.sp)
                Wheel("Minutes", m, 59) { m = it }
                Text(":", color = Ash, fontFamily = Stencil, fontSize = 44.sp)
                Wheel("Seconds", sec, 59) { sec = it }
            }
            Spacer(Modifier.height(24.dp))
            SectionLabel("Presets", Modifier.fillMaxWidth().padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                presets.forEach { p ->
                    Text(
                        Fmt.hms(p), color = Bone, fontFamily = Stencil, fontSize = 18.sp,
                        modifier = Modifier.hudPanel(cut = 6.dp)
                            .combinedClickable(
                                onClick = {
                                    val s = p / 1000
                                    h = (s / 3600).toInt(); m = ((s / 60) % 60).toInt(); sec = (s % 60).toInt()
                                },
                                onLongClick = { presets = presets - p; Store.saveTimerPresets(ctx, presets) },
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
                if (totalSet > 0 && totalSet !in presets) {
                    Text(
                        "+ Save current", color = Amber, fontSize = 15.sp,
                        modifier = Modifier.hudPanel(cut = 6.dp)
                            .combinedClickable(onClick = {
                                presets = (presets + totalSet).sorted(); Store.saveTimerPresets(ctx, presets)
                            })
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                }
            }
            Text(
                "Tap a preset to load it · long-press to delete",
                Modifier.padding(16.dp), color = Ash, fontSize = 12.sp,
            )
            ButtonRow {
                ActionButton("Start", true, Modifier.weight(1f), enabled = totalSet > 0) {
                    val end = System.currentTimeMillis() + totalSet
                    val n = TimerState(active = true, running = true, endAt = end, remaining = totalSet, total = totalSet)
                    save(n)
                    Store.setLastTimer(ctx, totalSet)
                    AlarmScheduler.scheduleTimer(ctx, end)
                    Notifs.timer(ctx, n)
                }
            }
        }
    } else {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            Ring(remaining.toFloat() / max(1L, t.total), Amber, 280.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Fmt.countdown(remaining), color = Bone, fontFamily = Stencil, fontSize = 48.sp)
                    val endAt = if (t.running) t.endAt else System.currentTimeMillis() + t.remaining
                    Text(
                        if (t.running) "Ends at " + Fmt.clock(endAt, is24) + (if (is24) "" else " " + Fmt.ampm(endAt)) else "Paused",
                        color = Ash, fontSize = 14.sp,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            ButtonRow {
                ActionButton("Cancel", false, Modifier.weight(1f)) {
                    save(TimerState()); AlarmScheduler.cancelTimer(ctx); Notifs.cancel(ctx, Notifs.ID_TIMER)
                }
                if (t.running) ActionButton("Pause", true, Modifier.weight(1f)) {
                    val n = t.copy(running = false, remaining = max(0L, t.endAt - System.currentTimeMillis()))
                    save(n); AlarmScheduler.cancelTimer(ctx); Notifs.timer(ctx, n)
                } else ActionButton("Resume", true, Modifier.weight(1f)) {
                    val n = t.copy(running = true, endAt = System.currentTimeMillis() + t.remaining)
                    save(n); AlarmScheduler.scheduleTimer(ctx, n.endAt); Notifs.timer(ctx, n)
                }
            }
        }
    }
}

@Composable
private fun Wheel(label: String, value: Int, maxValue: Int, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 6.dp)) {
        IconButton(onClick = { onChange(if (value >= maxValue) 0 else value + 1) }) {
            Icon(Icons.Filled.KeyboardArrowUp, "Increase $label", tint = Ash)
        }
        Text("%02d".format(value), color = Bone, fontFamily = Stencil, fontSize = 52.sp)
        IconButton(onClick = { onChange(if (value <= 0) maxValue else value - 1) }) {
            Icon(Icons.Filled.KeyboardArrowDown, "Decrease $label", tint = Ash)
        }
        Text(label, color = Ash, fontSize = 12.sp)
    }
}
