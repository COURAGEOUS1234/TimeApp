package com.gabriel.timeapp.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.timeapp.StandbyActivity
import com.gabriel.timeapp.Store

private val TABS = listOf("Alarm", "World clock", "Stopwatch", "Timer", "Pomodoro")

@Composable
fun AppRoot(pendingTab: MutableIntState, resumeTick: Int, onSettingsChanged: () -> Unit) {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(Store.lastTab(ctx)) }
    LaunchedEffect(pendingTab.intValue) {
        val t = pendingTab.intValue
        if (t >= 0) { tab = t; pendingTab.intValue = -1 }
    }
    var showSettings by remember { mutableStateOf(false) }
    var settingsTick by remember { mutableIntStateOf(0) }
    val settings = remember(resumeTick, settingsTick) { Store.settings(ctx) }
    var addPending by remember { mutableStateOf(false) }
    val tick = resumeTick + settingsTick * 1000

    Box(Modifier.fillMaxSize().background(Bg).scanlines()) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            HazardStripe(Modifier.fillMaxWidth().height(8.dp))
            TopBar(
                showAdd = tab == 0 || tab == 1,
                onAdd = { addPending = true },
                onStandby = { ctx.startActivity(Intent(ctx, StandbyActivity::class.java)) },
                onSettings = { showSettings = true },
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    0 -> AlarmScreen(tick, addPending) { addPending = false }
                    1 -> WorldClockScreen(tick, settings.glitch, addPending) { addPending = false }
                    2 -> StopwatchScreen(tick)
                    3 -> TimerScreen(tick)
                    else -> PomodoroScreen(tick)
                }
            }
            BottomTabs(tab) { tab = it; addPending = false; Store.setLastTab(ctx, it) }
        }
        if (showSettings) SettingsScreen(
            onStandby = { ctx.startActivity(Intent(ctx, StandbyActivity::class.java)) },
            onDismiss = { showSettings = false; settingsTick++; onSettingsChanged() },
        )
    }
}

@Composable
private fun TopBar(showAdd: Boolean, onAdd: () -> Unit, onStandby: () -> Unit, onSettings: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(52.dp).padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("TERMINAL HOUR", color = Red, fontFamily = Mono, fontSize = 14.sp, letterSpacing = 3.sp)
        Spacer(Modifier.weight(1f))
        if (showAdd) IconButton(onClick = onAdd) { Icon(Icons.Filled.Add, "Add", tint = Bone) }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More options", tint = Bone) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Standby mode") }, onClick = { menu = false; onStandby() })
                DropdownMenuItem(text = { Text("Settings") }, onClick = { menu = false; onSettings() })
            }
        }
    }
}

@Composable
private fun BottomTabs(selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Panel).padding(vertical = 4.dp)) {
        TABS.forEachIndexed { i, name ->
            val sel = i == selected
            Column(
                Modifier.weight(1f).clickable { onSelect(i) }.padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    name, color = if (sel) Bone else Ash, fontFamily = Mono, fontSize = 12.sp,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1, softWrap = false, overflow = TextOverflow.Clip,
                )
                Spacer(Modifier.height(4.dp))
                Box(Modifier.width(20.dp).height(2.dp).background(if (sel) Red else Color.Transparent))
            }
        }
    }
}
