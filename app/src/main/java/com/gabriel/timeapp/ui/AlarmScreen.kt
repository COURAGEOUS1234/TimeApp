@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.gabriel.timeapp.ui

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gabriel.timeapp.Alarm
import com.gabriel.timeapp.AlarmScheduler
import com.gabriel.timeapp.Fmt
import com.gabriel.timeapp.Store

@Composable
fun AlarmScreen(tick: Int, addPending: Boolean, consumeAdd: () -> Unit) {
    val ctx = LocalContext.current
    val is24 = remember(tick) { Fmt.is24(ctx) }
    var alarms by remember(tick) { mutableStateOf(Store.alarms(ctx)) }
    var editing by remember { mutableStateOf<Alarm?>(null) }
    var deleting by remember { mutableStateOf<Alarm?>(null) }
    val now = rememberNow(15_000L)

    LaunchedEffect(addPending) {
        if (addPending) {
            editing = Alarm(id = 0, hour = 7, minute = 0, snoozeMin = Store.settings(ctx).defaultSnooze)
            consumeAdd()
        }
    }

    fun persist(a: Alarm, announce: Boolean) {
        Store.upsertAlarm(ctx, a)
        AlarmScheduler.schedule(ctx, a)
        alarms = Store.alarms(ctx)
        if (announce && a.enabled) {
            val ms = AlarmScheduler.nextTrigger(a) - System.currentTimeMillis()
            Toast.makeText(ctx, "Alarm set for ${Fmt.untilLong(ms)} from now", Toast.LENGTH_SHORT).show()
        }
    }

    val next = alarms.filter { it.enabled }.minOfOrNull { AlarmScheduler.nextTrigger(it, now) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
        item {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 44.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (next == null) {
                    SectionLabel("Next alarm", color = Ash)
                    Spacer(Modifier.height(8.dp))
                    Text("ALL ALARMS OFF", color = Ash, fontFamily = Stencil, fontSize = 26.sp)
                } else {
                    SectionLabel("Next alarm")
                    Spacer(Modifier.height(4.dp))
                    Reticle {
                        GlitchText(Fmt.tminus(next - now), 40.sp, Store.settings(ctx).glitch)
                    }
                    Text("RINGS IN " + Fmt.untilShort(next - now).uppercase(), color = Ash, fontFamily = Mono, fontSize = 12.sp, letterSpacing = 2.sp)
                }
            }
        }
        if (alarms.isEmpty()) item {
            Text(
                "No alarms yet. Tap + to add one.",
                Modifier.fillMaxWidth().padding(24.dp), color = Ash, textAlign = TextAlign.Center,
            )
        }
        items(alarms, key = { it.id }) { a ->
            AlarmCard(
                a, is24, now,
                onClick = { editing = a },
                onLongClick = { deleting = a },
                onToggle = { on -> persist(a.copy(enabled = on), announce = on) },
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    editing?.let { e ->
        AlarmEditor(
            initial = e, is24 = is24,
            onDismiss = { editing = null },
            onSave = { a ->
                val saved = if (a.id == 0) a.copy(id = Store.newAlarmId(ctx)) else a
                persist(saved.copy(enabled = true), announce = true)
                editing = null
            },
            onDelete = if (e.id == 0) null else ({ deleting = e; editing = null }),
        )
    }

    deleting?.let { d ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete alarm?") },
            text = { Text("${Fmt.hm(d.hour, d.minute, is24)}${if (is24) "" else " " + Fmt.ampmOf(d.hour)}  ${d.label}") },
            confirmButton = {
                TextButton(onClick = {
                    AlarmScheduler.cancel(ctx, d.id); Store.deleteAlarm(ctx, d.id)
                    alarms = Store.alarms(ctx); deleting = null
                }) { Text("Delete", color = Amber) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel", color = Bone) } },
            containerColor = Panel,
        )
    }
}

@Composable
private fun AlarmCard(a: Alarm, is24: Boolean, now: Long, onClick: () -> Unit, onLongClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val on = a.enabled
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).hudPanel()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (a.label.isNotBlank()) Text(a.label, color = Ash, fontSize = 13.sp, maxLines = 1)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(Fmt.hm(a.hour, a.minute, is24), color = if (on) Bone else Ash, fontFamily = Stencil, fontSize = 40.sp)
                if (!is24) Text(
                    " " + Fmt.ampmOf(a.hour), color = if (on) Bone else Ash, fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 7.dp),
                )
            }
        }
        Text(
            Fmt.days(a, now), color = if (on) Amber else Ash, fontSize = 13.sp,
            modifier = Modifier.padding(end = 12.dp),
        )
        Switch(checked = on, onCheckedChange = onToggle, colors = appSwitchColors())
    }
}

@Composable
private fun AlarmEditor(initial: Alarm, is24: Boolean, onDismiss: () -> Unit, onSave: (Alarm) -> Unit, onDelete: (() -> Unit)?) {
    val tp = rememberTimePickerState(initial.hour, initial.minute, is24)
    var days by remember { mutableIntStateOf(initial.days) }
    var label by remember { mutableStateOf(initial.label) }
    var vibrate by remember { mutableStateOf(initial.vibrate) }
    var gradual by remember { mutableStateOf(initial.gradual) }
    var sound by remember { mutableStateOf(initial.sound) }
    var snooze by remember { mutableIntStateOf(initial.snoozeMin) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Column(Modifier.fillMaxSize().hudGrid()) {
                HazardStripe(Modifier.fillMaxWidth().height(8.dp))
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SectionLabel(if (initial.id == 0) "Add alarm" else "Edit alarm")
                    Spacer(Modifier.height(16.dp))
                    TimePicker(
                        state = tp,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = Panel,
                            clockDialSelectedContentColor = Bg,
                            clockDialUnselectedContentColor = Bone,
                            selectorColor = Amber,
                            periodSelectorBorderColor = Amber,
                            periodSelectorSelectedContainerColor = Amber,
                            periodSelectorSelectedContentColor = Bg,
                            periodSelectorUnselectedContentColor = Ash,
                            timeSelectorSelectedContainerColor = Amber,
                            timeSelectorSelectedContentColor = Bg,
                            timeSelectorUnselectedContainerColor = Panel,
                            timeSelectorUnselectedContentColor = Bone,
                        ),
                    )

                    SectionLabel("Repeat", Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { i, d ->
                            val sel = (days and (1 shl i)) != 0
                            Box(
                                Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CutCornerShape(8.dp)).background(if (sel) Amber else Panel)
                                    .clickable { days = days xor (1 shl i) },
                                contentAlignment = Alignment.Center,
                            ) { Text(d, color = if (sel) Bg else Bone, fontSize = 15.sp) }
                        }
                    }
                    Text(
                        if (days == 0) "Rings once" else "Repeats: " + Fmt.days(initial.copy(days = days), System.currentTimeMillis()),
                        Modifier.fillMaxWidth().padding(top = 6.dp), color = Ash, fontSize = 12.sp,
                    )

                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = label, onValueChange = { label = it.take(40) },
                        label = { Text("Alarm name") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Amber, unfocusedBorderColor = Panel2,
                            focusedLabelColor = Amber, unfocusedLabelColor = Ash, cursorColor = Amber,
                            focusedTextColor = Bone, unfocusedTextColor = Bone,
                        ),
                    )

                    Spacer(Modifier.height(16.dp))
                    SectionLabel("Sound", Modifier.fillMaxWidth())
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Choice("Siren", sound == Alarm.SOUND_SIREN) { sound = Alarm.SOUND_SIREN }
                        Choice("Device alarm tone", sound == Alarm.SOUND_TONE) { sound = Alarm.SOUND_TONE }
                    }
                    ToggleRow("Gradually increase volume", gradual, "Fades in over 30 seconds") { gradual = it }
                    ToggleRow("Vibrate", vibrate) { vibrate = it }

                    Spacer(Modifier.height(8.dp))
                    SectionLabel("Snooze", Modifier.fillMaxWidth())
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3, 5, 10, 15, 30).forEach { m -> Choice("$m min", snooze == m) { snooze = m } }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete", color = Amber, fontSize = 16.sp) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Bone, fontSize = 16.sp) }
                    Spacer(Modifier.size(8.dp))
                    ActionButton("Save", primary = true) {
                        onSave(
                            initial.copy(
                                hour = tp.hour, minute = tp.minute, days = days, label = label.trim(),
                                vibrate = vibrate, gradual = gradual, sound = sound, snoozeMin = snooze,
                            )
                        )
                    }
                }
            }
        }
    }
}
