@file:OptIn(ExperimentalFoundationApi::class)

package com.gabriel.timeapp.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gabriel.timeapp.Fmt
import com.gabriel.timeapp.Store
import java.util.Locale
import java.util.TimeZone

object Zones {
    private val regions = listOf(
        "Africa/", "America/", "Antarctica/", "Asia/", "Atlantic/", "Australia/", "Europe/", "Indian/", "Pacific/",
    )
    val all: List<String> by lazy {
        TimeZone.getAvailableIDs()
            .filter { z -> z == "UTC" || regions.any { z.startsWith(it) } }
            .distinct()
            .sortedBy { city(it) }
    }

    fun city(z: String) = if (z == "UTC") "UTC" else z.substringAfterLast('/').replace('_', ' ')

    fun region(z: String): String {
        if (z == "UTC") return "Coordinated Universal Time"
        val parts = z.split('/')
        return parts.dropLast(1).joinToString(" · ") { it.replace('_', ' ') }
    }

    /** Days between the city's calendar date and local: -1, 0, +1. */
    fun dayDelta(tz: TimeZone, now: Long): Long {
        val local = TimeZone.getDefault()
        return Math.floorDiv(now + tz.getOffset(now), 86_400_000L) - Math.floorDiv(now + local.getOffset(now), 86_400_000L)
    }
}

@Composable
fun WorldClockScreen(tick: Int, glitch: Boolean, addPending: Boolean, consumeAdd: () -> Unit) {
    val ctx = LocalContext.current
    val is24 = remember(tick) { Fmt.is24(ctx) }
    var cities by remember(tick) { mutableStateOf(Store.cities(ctx)) }
    var adding by remember { mutableStateOf(false) }
    val now = rememberNow(1000L)

    LaunchedEffect(addPending) { if (addPending) { adding = true; consumeAdd() } }
    fun save(l: List<String>) { cities = l; Store.saveCities(ctx, l) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
        item {
            val tz = TimeZone.getDefault()
            Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                SectionLabel("Local time")
                Spacer(Modifier.height(8.dp))
                GlitchText(Fmt.clock(now, is24, seconds = true), if (is24) 54.sp else 58.sp, glitch)
                Text(
                    Fmt.date(now) + if (is24) "" else "  ·  " + Fmt.ampm(now),
                    color = Bone, fontSize = 16.sp, letterSpacing = 2.sp,
                )
                Text(
                    tz.getDisplayName(tz.inDaylightTime(java.util.Date(now)), TimeZone.LONG, Locale.US),
                    color = Ash, fontSize = 12.sp,
                )
            }
        }
        if (cities.isEmpty()) item {
            Text("Tap + to add a city", Modifier.fillMaxWidth().padding(24.dp), color = Ash, textAlign = TextAlign.Center)
        }
        itemsIndexed(cities, key = { _, z -> z }) { i, z ->
            CityRow(
                z, now, is24,
                canUp = i > 0, canDown = i < cities.lastIndex,
                onUp = { save(cities.toMutableList().apply { add(i - 1, removeAt(i)) }) },
                onDown = { save(cities.toMutableList().apply { add(i + 1, removeAt(i)) }) },
                onRemove = { save(cities - z) },
            )
        }
        item {
            Text(
                "Long-press a city to reorder or remove it",
                Modifier.fillMaxWidth().padding(16.dp), color = Ash, fontSize = 12.sp, textAlign = TextAlign.Center,
            )
        }
    }

    if (adding) CityPicker(cities, onDismiss = { adding = false }) { z -> save(cities + z); adding = false }
}

@Composable
private fun CityRow(
    z: String, now: Long, is24: Boolean, canUp: Boolean, canDown: Boolean,
    onUp: () -> Unit, onDown: () -> Unit, onRemove: () -> Unit,
) {
    val tz = TimeZone.getTimeZone(z)
    val off = tz.getOffset(now) - TimeZone.getDefault().getOffset(now)
    val day = when (Zones.dayDelta(tz, now)) { 1L -> "Tomorrow"; -1L -> "Yesterday"; else -> "Today" }
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(20.dp)).background(Panel)
                .combinedClickable(onClick = {}, onLongClick = { menu = true })
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(Zones.city(z), color = Bone, fontSize = 18.sp, maxLines = 1)
                Text("$day · ${Fmt.offset(off)}", color = Ash, fontSize = 13.sp)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(Fmt.clock(now, is24, tz), color = Bone, fontFamily = Stencil, fontSize = 32.sp)
                if (!is24) Text(" " + Fmt.ampm(now, tz), color = Ash, fontSize = 14.sp, modifier = Modifier.padding(bottom = 5.dp))
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            if (canUp) DropdownMenuItem(text = { Text("Move up") }, onClick = { menu = false; onUp() })
            if (canDown) DropdownMenuItem(text = { Text("Move down") }, onClick = { menu = false; onDown() })
            DropdownMenuItem(text = { Text("Remove", color = Red) }, onClick = { menu = false; onRemove() })
        }
    }
}

@Composable
private fun CityPicker(existing: List<String>, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    var q by remember { mutableStateOf("") }
    val now = remember { System.currentTimeMillis() }
    val results = remember(q) {
        val s = q.trim().lowercase()
        Zones.all.filter { it !in existing && (s.isEmpty() || Zones.city(it).lowercase().contains(s) || it.lowercase().contains(s)) }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Close", tint = Bone) }
                    SectionLabel("Add city")
                }
                OutlinedTextField(
                    value = q, onValueChange = { q = it }, singleLine = true,
                    placeholder = { Text("Search city, e.g. Harare") },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = Ash) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Red, unfocusedBorderColor = Panel2, cursorColor = Red,
                        focusedTextColor = Bone, unfocusedTextColor = Bone,
                    ),
                )
                LazyColumn(Modifier.fillMaxSize()) {
                    items(results, key = { it }) { z ->
                        val tz = TimeZone.getTimeZone(z)
                        Row(
                            Modifier.fillMaxWidth().clickable { onPick(z) }.padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(Zones.city(z), color = Bone, fontSize = 17.sp)
                                Text(Zones.region(z), color = Ash, fontSize = 12.sp)
                            }
                            Text(Fmt.gmt(tz, now), color = Red, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
