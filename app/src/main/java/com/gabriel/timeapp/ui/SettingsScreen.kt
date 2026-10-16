@file:OptIn(ExperimentalLayoutApi::class)

package com.gabriel.timeapp.ui

import android.content.Intent
import android.provider.Settings as AndroidSettings
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gabriel.timeapp.Store

@Composable
fun SettingsScreen(onStandby: () -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var s by remember { mutableStateOf(Store.settings(ctx)) }
    fun set(n: com.gabriel.timeapp.Settings) { s = n; Store.saveSettings(ctx, n) }
    fun open(i: Intent) = try { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Column(Modifier.fillMaxSize()) {
                HazardStripe(Modifier.fillMaxWidth().height(8.dp))
                Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Close", tint = Bone) }
                    SectionLabel("Settings")
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    SectionLabel("Display")
                    Spacer(Modifier.height(8.dp))
                    Text("Time format", color = Bone, fontSize = 16.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Choice("Phone setting", s.timeFormat == "system") { set(s.copy(timeFormat = "system")) }
                        Choice("12-hour", s.timeFormat == "12") { set(s.copy(timeFormat = "12")) }
                        Choice("24-hour", s.timeFormat == "24") { set(s.copy(timeFormat = "24")) }
                    }
                    ToggleRow("Glitch effect", s.glitch, "Jitter and colour split on the big clocks") { set(s.copy(glitch = it)) }
                    ToggleRow("Keep screen on", s.keepScreenOn, "While Terminal Hour is open") { set(s.copy(keepScreenOn = it)) }

                    Spacer(Modifier.height(20.dp))
                    SectionLabel("Alarms")
                    Spacer(Modifier.height(8.dp))
                    Text("Default snooze for new alarms", color = Bone, fontSize = 16.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3, 5, 10, 15, 30).forEach { m -> Choice("$m min", s.defaultSnooze == m) { set(s.copy(defaultSnooze = m)) } }
                    }

                    Spacer(Modifier.height(20.dp))
                    SectionLabel("Always-on clock")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Android doesn't let apps draw on Samsung's Always On Display, so Terminal Hour offers two alternatives. " +
                            "Both move the clock every minute to prevent burn-in.",
                        color = Ash, fontSize = 13.sp,
                    )
                    ToggleRow("Dim screen", s.standbyDim, "Lowest brightness in standby and screen saver") { set(s.copy(standbyDim = it)) }
                    TextButton(onClick = onStandby) { Text("Start standby mode", color = Red, fontSize = 16.sp) }
                    Text("Keeps the screen on with a dim clock. Double-tap to exit. Best used while charging.", color = Ash, fontSize = 12.sp)
                    TextButton(onClick = { open(Intent(AndroidSettings.ACTION_DREAM_SETTINGS)) }) {
                        Text("Set up screen saver while charging", color = Red, fontSize = 16.sp)
                    }
                    Text("Choose Terminal Hour as the screen saver. It then appears automatically while the phone charges.", color = Ash, fontSize = 12.sp)

                    Spacer(Modifier.height(20.dp))
                    SectionLabel("System")
                    TextButton(onClick = {
                        open(Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, ctx.packageName))
                    }) { Text("Notification settings", color = Red, fontSize = 16.sp) }

                    Spacer(Modifier.height(20.dp))
                    SectionLabel("About")
                    Text(
                        "Terminal Hour 2.0\nFonts: Black Ops One, Share Tech Mono (SIL Open Font License)",
                        color = Ash, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp),
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
