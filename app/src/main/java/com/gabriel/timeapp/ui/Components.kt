@file:OptIn(ExperimentalMaterial3Api::class)

package com.gabriel.timeapp.ui

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

/** CRT scanlines + faint red tint over everything. */
fun Modifier.scanlines(): Modifier = drawWithContent {
    drawContent()
    drawRect(Color(0x0AFF2A2A))
    var y = 0f
    while (y < size.height) {
        drawLine(Color(0x22000000), Offset(0f, y), Offset(size.width, y), strokeWidth = 2f)
        y += 5f
    }
}

@Composable
fun HazardStripe(modifier: Modifier = Modifier) = Canvas(modifier) {
    drawRect(Color(0xFF0A0A0A))
    val h = size.height
    val w = h * 0.9f
    var x = -h
    while (x < size.width + h) {
        val p = Path().apply { moveTo(x, h); lineTo(x + h, 0f); lineTo(x + h + w, 0f); lineTo(x + w, h); close() }
        drawPath(p, Color(0xFFD7261E))
        x += w * 2
    }
}

/** Wall-clock millis, refreshed every [periodMs] (aligned to the period). */
@Composable
fun rememberNow(periodMs: Long = 1000L): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(periodMs) {
        while (true) {
            now = System.currentTimeMillis()
            delay(periodMs - System.currentTimeMillis() % periodMs)
        }
    }
    return now
}

/** elapsedRealtime, refreshed every frame while [active]. */
@Composable
fun rememberFrameClock(active: Boolean): Long {
    var t by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(active) {
        t = SystemClock.elapsedRealtime()
        while (active) { withFrameMillis { }; t = SystemClock.elapsedRealtime() }
    }
    return t
}

/** Red neon text that jitters with a cyan ghost split every few seconds. */
@Composable
fun GlitchText(text: String, fontSize: TextUnit, enabled: Boolean, modifier: Modifier = Modifier) {
    var dx by remember { mutableFloatStateOf(0f) }
    var flicker by remember { mutableFloatStateOf(1f) }
    var ghost by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(enabled) {
        if (!enabled) { dx = 0f; flicker = 1f; ghost = 0f; return@LaunchedEffect }
        while (true) {
            delay(Random.nextLong(3000, 8000))
            repeat(Random.nextInt(3, 7)) {
                dx = Random.nextInt(-14, 15).toFloat()
                flicker = Random.nextDouble(0.55, 1.0).toFloat()
                ghost = 0.7f
                delay(60)
            }
            dx = 0f; flicker = 1f; ghost = 0f
        }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text, Modifier.graphicsLayer { translationX = -dx * 1.6f; alpha = ghost },
            color = Cyan, fontSize = fontSize, fontFamily = Stencil, maxLines = 1,
        )
        Text(
            text, Modifier.graphicsLayer { translationX = dx; alpha = flicker },
            color = Red, fontSize = fontSize, fontFamily = Stencil, maxLines = 1,
            style = TextStyle(shadow = Shadow(Red, blurRadius = 28f)),
        )
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) =
    Text(text.uppercase(), modifier, color = Red, fontFamily = Mono, fontSize = 12.sp, letterSpacing = 2.sp)

@Composable
fun ActionButton(text: String, primary: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) =
    Button(
        onClick = onClick, modifier = modifier.height(56.dp), enabled = enabled,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) Red else Panel2,
            contentColor = if (primary) Bg else Bone,
            disabledContainerColor = Panel2, disabledContentColor = Ash,
        ),
    ) { Text(text, fontFamily = Mono, fontSize = 17.sp, fontWeight = FontWeight.Bold) }

@Composable
fun ButtonRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) =
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )

@Composable
fun Choice(text: String, selected: Boolean, onClick: () -> Unit) = FilterChip(
    selected = selected, onClick = onClick,
    label = { Text(text, fontFamily = Mono) },
    colors = FilterChipDefaults.filterChipColors(
        containerColor = Panel, labelColor = Bone,
        selectedContainerColor = Red, selectedLabelColor = Bg,
    ),
)

@Composable
fun appSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Bg, checkedTrackColor = Red,
    uncheckedThumbColor = Ash, uncheckedTrackColor = Panel2, uncheckedBorderColor = Ash,
)

@Composable
fun ToggleRow(label: String, checked: Boolean, sub: String? = null, onChange: (Boolean) -> Unit) =
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(label, color = Bone, fontSize = 16.sp)
            if (sub != null) Text(sub, color = Ash, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange, colors = appSwitchColors())
    }

/** "Label   [−] 25 min [+]" */
@Composable
fun StepperRow(label: String, value: Int, unit: String, range: IntRange, onChange: (Int) -> Unit) =
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Bone, fontSize = 16.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = { if (value > range.first) onChange(value - 1) }) { Text("−", color = Red, fontSize = 22.sp) }
        Text("$value $unit", color = Bone, fontFamily = Stencil, fontSize = 18.sp)
        TextButton(onClick = { if (value < range.last) onChange(value + 1) }) { Text("+", color = Red, fontSize = 22.sp) }
    }

/** Countdown ring: [progress] 0..1 of time remaining. */
@Composable
fun Ring(progress: Float, color: Color, size: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val sw = 10.dp.toPx()
            val st = Stroke(width = sw, cap = StrokeCap.Round)
            val inset = sw / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - sw, this.size.height - sw)
            drawArc(Panel2, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(width = sw))
            drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, Offset(inset, inset), arcSize, style = st)
        }
        content()
    }
