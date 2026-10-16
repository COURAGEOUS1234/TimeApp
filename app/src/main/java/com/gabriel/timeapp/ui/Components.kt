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

/** Faint tactical grid with very light scanlines, drawn over the content. */
fun Modifier.hudGrid(): Modifier = drawWithContent {
    drawContent()
    val step = 24.dp.toPx()
    var x = 0f
    while (x < size.width) { drawLine(GridLine, Offset(x, 0f), Offset(x, size.height), 1f); x += step }
    var y = 0f
    while (y < size.height) { drawLine(GridLine, Offset(0f, y), Offset(size.width, y), 1f); y += step }
    var s = 0f
    while (s < size.height) { drawLine(Color(0x14000000), Offset(0f, s), Offset(size.width, s), 2f); s += 4f }
}

/** Chamfered panel with a thin outline and corner brackets on the two square corners. */
fun Modifier.hudPanel(accent: Color = Amber, cut: Dp = 10.dp): Modifier {
    val shape = CutCornerShape(topStart = cut, bottomEnd = cut)
    return this
        .clip(shape)
        .background(Panel)
        .border(1.dp, accent.copy(alpha = 0.22f), shape)
        .drawWithContent {
            drawContent()
            val l = 12.dp.toPx(); val w = 2.dp.toPx(); val c = accent.copy(alpha = 0.85f)
            // top-right
            drawLine(c, Offset(size.width - l, w / 2), Offset(size.width, w / 2), w)
            drawLine(c, Offset(size.width - w / 2, 0f), Offset(size.width - w / 2, l), w)
            // bottom-left
            drawLine(c, Offset(0f, size.height - w / 2), Offset(l, size.height - w / 2), w)
            drawLine(c, Offset(w / 2, size.height - l), Offset(w / 2, size.height), w)
        }
}

/** Amber/black hazard band (military equipment markings). */
@Composable
fun HazardStripe(modifier: Modifier = Modifier) = Canvas(modifier) {
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
        drawLine(Amber.copy(alpha = 0.35f), Offset(0f, base), Offset(size.width, base), 1f)
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
        val caret = Path().apply {
            moveTo(cx, base - 2.dp.toPx()); lineTo(cx - 6.dp.toPx(), size.height); lineTo(cx + 6.dp.toPx(), size.height); close()
        }
        drawPath(caret, Amber)
        drawLine(Amber, Offset(cx, 0f), Offset(cx, base), 2f)
    }
}

/** Hash-shaped (#) targeting reticle that frames its content and slowly breathes. */
@Composable
fun Reticle(modifier: Modifier = Modifier, color: Color = Amber, content: @Composable () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "reticle").animateFloat(
        0.35f, 0.75f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse), label = "a",
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val c = color.copy(alpha = pulse)
            val x1 = size.width * 0.06f; val x2 = size.width * 0.94f
            val y1 = size.height * 0.12f; val y2 = size.height * 0.88f
            val sw = 1.5.dp.toPx()
            val gapX = size.width * 0.28f; val gapY = size.height * 0.30f
            // verticals, broken in the middle
            for (x in listOf(x1, x2)) {
                drawLine(c, Offset(x, 0f), Offset(x, gapY), sw)
                drawLine(c, Offset(x, size.height - gapY), Offset(x, size.height), sw)
            }
            // horizontals, broken in the middle
            for (y in listOf(y1, y2)) {
                drawLine(c, Offset(0f, y), Offset(gapX, y), sw)
                drawLine(c, Offset(size.width - gapX, y), Offset(size.width, y), sw)
            }
            // range ticks
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
fun GlitchText(text: String, fontSize: TextUnit, enabled: Boolean, modifier: Modifier = Modifier, color: Color = Amber) {
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
            color = Cyan, fontSize = fontSize, fontFamily = Stencil, maxLines = 1,
        )
        Text(
            text, Modifier.graphicsLayer { translationX = dx; alpha = flicker },
            color = color, fontSize = fontSize, fontFamily = Stencil, maxLines = 1,
            style = TextStyle(shadow = Shadow(color.copy(alpha = 0.7f), blurRadius = 22f)),
        )
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = Amber) =
    Text("▸ " + text.uppercase(), modifier, color = color, fontFamily = Mono, fontSize = 12.sp, letterSpacing = 2.sp)

private val ButtonShape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)

@Composable
fun ActionButton(text: String, primary: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) =
    Button(
        onClick = onClick, modifier = modifier.height(54.dp), enabled = enabled,
        shape = ButtonShape,
        border = if (primary) null else BorderStroke(1.dp, if (enabled) Amber.copy(alpha = 0.7f) else Ash),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) Amber else Color.Transparent,
            contentColor = if (primary) Bg else Amber,
            disabledContainerColor = Panel2, disabledContentColor = Ash,
        ),
    ) { Text(text.uppercase(), fontFamily = Hud, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) }

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
    label = { Text(text.uppercase(), fontFamily = Hud, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp) },
    shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
    colors = FilterChipDefaults.filterChipColors(
        containerColor = Panel, labelColor = Bone,
        selectedContainerColor = Amber, selectedLabelColor = Bg,
    ),
)

@Composable
fun appSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Bg, checkedTrackColor = Amber,
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
        TextButton(onClick = { if (value > range.first) onChange(value - 1) }) { Text("−", color = Amber, fontSize = 22.sp) }
        Text("$value $unit".trim(), color = Bone, fontFamily = Stencil, fontSize = 18.sp)
        TextButton(onClick = { if (value < range.last) onChange(value + 1) }) { Text("+", color = Amber, fontSize = 22.sp) }
    }

/** Segmented HUD gauge: 60 segments, [progress] 0..1 of them lit. */
@Composable
fun Ring(progress: Float, color: Color, size: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 12.dp.toPx()
            val inset = sw / 2 + 6.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val segs = 60
            val lit = (progress.coerceIn(0f, 1f) * segs).let { kotlin.math.ceil(it).toInt() }
            for (i in 0 until segs) {
                val start = -90f + i * 6f + 0.8f
                drawArc(
                    if (i < lit) color else Panel2, start, 4.4f, false,
                    Offset(inset, inset), arcSize, style = Stroke(width = sw),
                )
            }
            // outer hairline + top marker
            drawCircle(color.copy(alpha = 0.25f), radius = this.size.minDimension / 2 - 1.dp.toPx(), style = Stroke(1.dp.toPx()))
            val cx = this.size.width / 2
            drawLine(color, Offset(cx, 0f), Offset(cx, 8.dp.toPx()), 2.dp.toPx())
        }
        content()
    }
