@file:OptIn(ExperimentalMaterial3Api::class)

package com.gabriel.timeapp.ui

import android.os.SystemClock
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.TimeZone
import kotlin.math.floor
import kotlin.random.Random

/** Background texture: tactical grid (Tactical) or light-table rings and crosshair (Tower). */
fun Modifier.hudGrid(): Modifier = drawWithContent {
    drawContent()
    if (isTower) {
        val c = Offset(size.width / 2, size.height * 0.32f)
        val step = 64.dp.toPx()
        var r = step
        while (r < size.height * 1.2f) { drawCircle(GridLine, r, c, style = Stroke(1f)); r += step }
        val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 10f))
        drawLine(GridLine, Offset(0f, c.y), Offset(size.width, c.y), 1f, pathEffect = dash)
        drawLine(GridLine, Offset(c.x, 0f), Offset(c.x, size.height), 1f, pathEffect = dash)
        return@drawWithContent
    }
    val step = 24.dp.toPx()
    var x = 0f
    while (x < size.width) { drawLine(GridLine, Offset(x, 0f), Offset(x, size.height), 1f); x += step }
    var y = 0f
    while (y < size.height) { drawLine(GridLine, Offset(0f, y), Offset(size.width, y), 1f); y += step }
    var sl = 0f
    while (sl < size.height) { drawLine(Color(0x14000000), Offset(0f, sl), Offset(size.width, sl), 2f); sl += 4f }
}

/** Panel: chamfered with corner brackets (Tactical) or thin glass card with tick marks (Tower). */
fun Modifier.hudPanel(accent: Color? = null, cut: Dp = 10.dp): Modifier {
    val a = accent ?: Accent
    if (isTower) {
        val shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
        return this.clip(shape).background(Panel).border(1.dp, a.copy(alpha = 0.18f), shape).drawWithContent {
            drawContent()
            val t = 10.dp.toPx(); val w = 1.5.dp.toPx(); val c = a.copy(alpha = 0.9f)
            drawLine(c, Offset(14.dp.toPx(), w / 2), Offset(14.dp.toPx() + t * 2, w / 2), w)
            for (i in 0..2) drawCircle(c.copy(alpha = 0.6f), 1.5.dp.toPx(), Offset(size.width - (10 + i * 7).dp.toPx(), 8.dp.toPx()))
        }
    }
    val shape = CutCornerShape(topStart = cut, bottomEnd = cut)
    return this
        .clip(shape)
        .background(Panel)
        .border(1.dp, a.copy(alpha = 0.22f), shape)
        .drawWithContent {
            drawContent()
            val l = 12.dp.toPx(); val w = 2.dp.toPx(); val c = a.copy(alpha = 0.85f)
            drawLine(c, Offset(size.width - l, w / 2), Offset(size.width, w / 2), w)
            drawLine(c, Offset(size.width - w / 2, 0f), Offset(size.width - w / 2, l), w)
            drawLine(c, Offset(0f, size.height - w / 2), Offset(l, size.height - w / 2), w)
            drawLine(c, Offset(w / 2, size.height - l), Offset(w / 2, size.height), w)
        }
}

/** Header band: hazard markings (Tactical) or a hairline with node dots (Tower). */
@Composable
fun HazardStripe(modifier: Modifier = Modifier) = Canvas(modifier) {
    if (isTower) {
        val y = size.height / 2
        drawLine(Accent.copy(alpha = 0.5f), Offset(0f, y), Offset(size.width, y), 1f)
        for (i in 1..5) drawCircle(Accent, 2.dp.toPx(), Offset(size.width * i / 6f, y))
        return@Canvas
    }
    drawRect(Color(0xFF0B0B08))
    val h = size.height
    val w = h * 0.9f
    var x = -h
    while (x < size.width + h) {
        val p = Path().apply { moveTo(x, h); lineTo(x + h, 0f); lineTo(x + h + w, 0f); lineTo(x + w, h); close() }
        drawPath(p, Color(0xFFD9A030))
        x += w * 2
    }
}

/**
 * HUD time tape, like a fighter-jet heading tape: a scrolling scale of the
 * day with a caret on "now". Spans two hours across the width.
 */
@Composable
fun TimeTape(now: Long, is24: Boolean, modifier: Modifier = Modifier) {
    val tm = rememberTextMeasurer()
    val labelStyle = TextStyle(color = Ash, fontFamily = Mono, fontSize = 10.sp)
    Canvas(modifier) {
        val local = now + TimeZone.getDefault().getOffset(now)
        val nowMin = local / 60_000.0
        val span = 120.0
        val ppm = (size.width / span).toFloat()
        val cx = size.width / 2
        val base = size.height - 6.dp.toPx()
        drawLine(Accent.copy(alpha = 0.35f), Offset(0f, base), Offset(size.width, base), 1f)
        val first = floor(nowMin - span / 2).toLong()
        for (m in first..(first + span.toLong() + 1)) {
            if (m % 5L != 0L) continue
            val x = cx + ((m - nowMin) * ppm).toFloat()
            val major = m % 15L == 0L
            val th = if (major) 10.dp.toPx() else 5.dp.toPx()
            drawLine(if (major) Bone.copy(alpha = 0.8f) else Ash.copy(alpha = 0.6f), Offset(x, base - th), Offset(x, base), 1.5f)
            if (major) {
                val hh = ((m / 60) % 24).toInt(); val mm = (m % 60).toInt()
                val label = if (is24) "%02d%02d".format(hh, mm)
                else "%d:%02d".format(if (hh % 12 == 0) 12 else hh % 12, mm)
                val r = tm.measure(label, labelStyle)
                drawText(r, topLeft = Offset(x - r.size.width / 2f, base - th - r.size.height - 1.dp.toPx()))
            }
        }
        // edge fade
        val fade = size.width * 0.18f
        drawRect(Brush.horizontalGradient(listOf(Bg, Color.Transparent), 0f, fade), Offset.Zero, Size(fade, size.height))
        drawRect(
            Brush.horizontalGradient(listOf(Color.Transparent, Bg), size.width - fade, size.width),
            Offset(size.width - fade, 0f), Size(fade, size.height),
        )
        // caret
        if (isTower) {
            drawCircle(Accent, 4.dp.toPx(), Offset(cx, base), style = Stroke(1.5.dp.toPx()))
            drawLine(Accent.copy(alpha = 0.7f), Offset(cx, 0f), Offset(cx, base - 4.dp.toPx()), 1f)
        } else {
            val caret = Path().apply {
                moveTo(cx, base - 2.dp.toPx()); lineTo(cx - 6.dp.toPx(), size.height); lineTo(cx + 6.dp.toPx(), size.height); close()
            }
            drawPath(caret, Accent)
            drawLine(Accent, Offset(cx, 0f), Offset(cx, base), 2f)
        }
    }
}

/** Target frame: hash (#) reticle (Tactical) or circular sight with ticks (Tower). Breathes slowly. */
@Composable
fun Reticle(modifier: Modifier = Modifier, color: Color? = null, content: @Composable () -> Unit) {
    val col = color ?: Accent
    val pulse by rememberInfiniteTransition(label = "reticle").animateFloat(
        0.35f, 0.75f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse), label = "a",
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val c = col.copy(alpha = pulse)
            val sw = 1.5.dp.toPx()
            if (isTower) {
                val r = size.height * 0.62f
                val ctr = Offset(size.width / 2, size.height / 2)
                drawArc(c, 200f, 140f, false, Offset(ctr.x - r, ctr.y - r), Size(r * 2, r * 2), style = Stroke(sw))
                drawArc(c, 20f, 140f, false, Offset(ctr.x - r, ctr.y - r), Size(r * 2, r * 2), style = Stroke(sw))
                val t = 8.dp.toPx()
                drawLine(c, Offset(ctr.x - r - t, ctr.y), Offset(ctr.x - r + t, ctr.y), sw)
                drawLine(c, Offset(ctr.x + r - t, ctr.y), Offset(ctr.x + r + t, ctr.y), sw)
                return@Canvas
            }
            val x1 = size.width * 0.06f; val x2 = size.width * 0.94f
            val y1 = size.height * 0.12f; val y2 = size.height * 0.88f
            val gapX = size.width * 0.28f; val gapY = size.height * 0.30f
            for (x in listOf(x1, x2)) {
                drawLine(c, Offset(x, 0f), Offset(x, gapY), sw)
                drawLine(c, Offset(x, size.height - gapY), Offset(x, size.height), sw)
            }
            for (y in listOf(y1, y2)) {
                drawLine(c, Offset(0f, y), Offset(gapX, y), sw)
                drawLine(c, Offset(size.width - gapX, y), Offset(size.width, y), sw)
            }
            val t = 4.dp.toPx()
            for (i in 1..3) {
                val yy = y1 + (y2 - y1) * i / 4f
                drawLine(c, Offset(x1, yy), Offset(x1 + t, yy), sw)
                drawLine(c, Offset(x2, yy), Offset(x2 - t, yy), sw)
            }
        }
        Box(Modifier.padding(horizontal = 28.dp, vertical = 18.dp)) { content() }
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

/** HUD text with occasional signal interference: brief jitter and a cyan split. */
@Composable
fun GlitchText(text: String, fontSize: TextUnit, enabled: Boolean, modifier: Modifier = Modifier, color: Color? = null) {
    if (isTower) {
        Text(
            text, modifier, color = Bone, fontSize = fontSize * 1.1f, fontFamily = Digits, maxLines = 1,
            style = TextStyle(shadow = Shadow(Accent.copy(alpha = 0.6f), blurRadius = 24f)),
        )
        return
    }
    val tint = color ?: Accent
    var dx by remember { mutableFloatStateOf(0f) }
    var flicker by remember { mutableFloatStateOf(1f) }
    var ghost by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(enabled) {
        if (!enabled) { dx = 0f; flicker = 1f; ghost = 0f; return@LaunchedEffect }
        while (true) {
            delay(Random.nextLong(4000, 9000))
            repeat(Random.nextInt(2, 5)) {
                dx = Random.nextInt(-8, 9).toFloat()
                flicker = Random.nextDouble(0.6, 1.0).toFloat()
                ghost = 0.5f
                delay(50)
            }
            dx = 0f; flicker = 1f; ghost = 0f
        }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text, Modifier.graphicsLayer { translationX = -dx * 1.5f; alpha = ghost },
            color = Second, fontSize = fontSize, fontFamily = Digits, maxLines = 1,
        )
        Text(
            text, Modifier.graphicsLayer { translationX = dx; alpha = flicker },
            color = tint, fontSize = fontSize, fontFamily = Digits, maxLines = 1,
            style = TextStyle(shadow = Shadow(tint.copy(alpha = 0.7f), blurRadius = 22f)),
        )
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color? = null) =
    Text(
        (if (isTower) "— " else "▸ ") + text.uppercase(), modifier, color = color ?: Accent,
        fontFamily = if (isTower) UiFont else Mono, fontSize = 12.sp, letterSpacing = if (isTower) 3.sp else 2.sp,
    )

private val buttonShape: androidx.compose.ui.graphics.Shape
    get() = if (isTower) androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
    else CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)

@Composable
fun ActionButton(text: String, primary: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) =
    Button(
        onClick = onClick, modifier = modifier.height(54.dp), enabled = enabled,
        shape = buttonShape,
        border = if (primary && !isTower) null else BorderStroke(1.dp, if (enabled) Accent.copy(alpha = if (primary) 1f else 0.6f) else Ash),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary && !isTower) Accent else if (primary) Accent.copy(alpha = 0.18f) else Color.Transparent,
            contentColor = if (primary && !isTower) Bg else Accent,
            disabledContainerColor = Panel2, disabledContentColor = Ash,
        ),
    ) { Text(text.uppercase(), fontFamily = UiFont, fontSize = 17.sp, fontWeight = if (isTower) FontWeight.SemiBold else FontWeight.Bold, letterSpacing = 2.sp) }

@Composable
fun ButtonRow(content: @Composable RowScope.() -> Unit) =
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )

@Composable
fun Choice(text: String, selected: Boolean, onClick: () -> Unit) = FilterChip(
    selected = selected, onClick = onClick,
    label = { Text(text.uppercase(), fontFamily = UiFont, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp) },
    shape = if (isTower) androidx.compose.foundation.shape.RoundedCornerShape(16.dp) else CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
    colors = FilterChipDefaults.filterChipColors(
        containerColor = Panel, labelColor = Bone,
        selectedContainerColor = Accent, selectedLabelColor = Bg,
    ),
)

@Composable
fun appSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Bg, checkedTrackColor = Accent,
    uncheckedThumbColor = Ash, uncheckedTrackColor = Panel2, uncheckedBorderColor = Ash,
)

@Composable
fun ToggleRow(label: String, checked: Boolean, sub: String? = null, onChange: (Boolean) -> Unit) =
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Bone, fontSize = 17.sp)
            if (sub != null) Text(sub, color = Ash, fontSize = 13.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange, colors = appSwitchColors())
    }

/** "Label   [−] 25 min [+]" */
@Composable
fun StepperRow(label: String, value: Int, unit: String, range: IntRange, onChange: (Int) -> Unit) =
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Bone, fontSize = 17.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = { if (value > range.first) onChange(value - 1) }) { Text("−", color = Accent, fontSize = 22.sp) }
        Text("$value $unit".trim(), color = Bone, fontFamily = Digits, fontSize = 18.sp)
        TextButton(onClick = { if (value < range.last) onChange(value + 1) }) { Text("+", color = Accent, fontSize = 22.sp) }
    }

/** Gauge: 60 lit segments (Tactical) or a hairline arc with a dial of ticks (Tower). */
@Composable
fun Ring(progress: Float, color: Color, size: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val p = progress.coerceIn(0f, 1f)
            val cx = this.size.width / 2
            val cy = this.size.height / 2
            if (isTower) {
                val r = this.size.minDimension / 2
                for (i in 0 until 60) {
                    val a = Math.toRadians(i * 6.0 - 90)
                    val len = if (i % 5 == 0) 10.dp.toPx() else 4.dp.toPx()
                    val cos = kotlin.math.cos(a).toFloat(); val sin = kotlin.math.sin(a).toFloat()
                    drawLine(
                        if (i / 60f < p) color.copy(alpha = 0.9f) else Panel2,
                        Offset(cx + (r - len) * cos, cy + (r - len) * sin), Offset(cx + r * cos, cy + r * sin), 1.5f,
                    )
                }
                val inset = 22.dp.toPx()
                val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
                drawArc(Panel2, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(2.dp.toPx()))
                drawArc(
                    color, -90f, 360f * p, false, Offset(inset, inset), arcSize,
                    style = Stroke(3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
                )
                return@Canvas
            }
            val sw = 12.dp.toPx()
            val inset = sw / 2 + 6.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val segs = 60
            val lit = kotlin.math.ceil(p * segs).toInt()
            for (i in 0 until segs) {
                drawArc(
                    if (i < lit) color else Panel2, -90f + i * 6f + 0.8f, 4.4f, false,
                    Offset(inset, inset), arcSize, style = Stroke(width = sw),
                )
            }
            drawCircle(color.copy(alpha = 0.25f), radius = this.size.minDimension / 2 - 1.dp.toPx(), style = Stroke(1.dp.toPx()))
            drawLine(color, Offset(cx, 0f), Offset(cx, 8.dp.toPx()), 2.dp.toPx())
        }
        content()
    }
