package com.gabriel.timeapp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.gabriel.timeapp.R

enum class HudStyle { TACTICAL, TOWER }

data class Palette(
    val style: HudStyle,
    val accent: Color,
    val second: Color,
    val alert: Color,
    val bg: Color,
    val panel: Color,
    val panel2: Color,
    val grid: Color,
    val text: Color,
    val muted: Color,
    val digits: FontFamily,
    val ui: FontFamily,
    val uiWeight: FontWeight,
)

private val BlackOps = FontFamily(Font(R.font.black_ops_one))
private val Rajdhani = FontFamily(
    Font(R.font.rajdhani_semibold, FontWeight.SemiBold),
    Font(R.font.rajdhani_bold, FontWeight.Bold),
)
private val JuraThin = FontFamily(Font(R.font.jura_light, FontWeight.Light))
private val Jura = FontFamily(
    Font(R.font.jura_light, FontWeight.Light),
    Font(R.font.jura_medium, FontWeight.Medium),
    Font(R.font.jura_semibold, FontWeight.SemiBold),
    Font(R.font.jura_semibold, FontWeight.Bold),
)

object Themes {
    /** Military field HUD: amber on olive gunmetal, stencil digits, chamfered panels. */
    val tactical = Palette(
        style = HudStyle.TACTICAL,
        accent = Color(0xFFFFB23E), second = Color(0xFF7FD8E6), alert = Color(0xFFFF4D3A),
        bg = Color(0xFF0A0D0B), panel = Color(0xFF111611), panel2 = Color(0xFF1C241C),
        grid = Color(0x1A9FB89F), text = Color(0xFFE6E8DF), muted = Color(0xFF8C948A),
        digits = BlackOps, ui = Rajdhani, uiWeight = FontWeight.SemiBold,
    )

    /** Sky-station HUD: pale ice-blue hairlines on deep glass, thin geometric type, circles. */
    val tower = Palette(
        style = HudStyle.TOWER,
        accent = Color(0xFF9FDFFF), second = Color(0xFFFF9F5A), alert = Color(0xFFFF5A3C),
        bg = Color(0xFF04070A), panel = Color(0xFF0A1016), panel2 = Color(0xFF17212B),
        grid = Color(0x149FDFFF), text = Color(0xFFF2F7FA), muted = Color(0xFF7D8B97),
        digits = JuraThin, ui = Jura, uiWeight = FontWeight.Medium,
    )

    fun of(id: String) = if (id == "tower") tower else tactical
}

object ThemeState {
    var palette by mutableStateOf(Themes.tactical)
}

val isTower: Boolean get() = ThemeState.palette.style == HudStyle.TOWER

// Read through the current palette, so every composable recolours on theme change.
val Accent: Color get() = ThemeState.palette.accent
val Second: Color get() = ThemeState.palette.second
val Alert: Color get() = ThemeState.palette.alert
val Bg: Color get() = ThemeState.palette.bg
val Panel: Color get() = ThemeState.palette.panel
val Panel2: Color get() = ThemeState.palette.panel2
val GridLine: Color get() = ThemeState.palette.grid
val Bone: Color get() = ThemeState.palette.text
val Ash: Color get() = ThemeState.palette.muted
val Digits: FontFamily get() = ThemeState.palette.digits
val UiFont: FontFamily get() = ThemeState.palette.ui
val Mono = FontFamily(Font(R.font.share_tech_mono))

private fun typography(p: Palette): Typography {
    val b = Typography()
    fun TextStyle.ui() = copy(fontFamily = p.ui, fontWeight = p.uiWeight)
    return Typography(
        displayLarge = b.displayLarge.copy(fontFamily = p.digits),
        displayMedium = b.displayMedium.copy(fontFamily = p.digits),
        displaySmall = b.displaySmall.copy(fontFamily = p.digits),
        headlineLarge = b.headlineLarge.ui(), headlineMedium = b.headlineMedium.ui(), headlineSmall = b.headlineSmall.ui(),
        titleLarge = b.titleLarge.ui(), titleMedium = b.titleMedium.ui(), titleSmall = b.titleSmall.ui(),
        bodyLarge = b.bodyLarge.ui(), bodyMedium = b.bodyMedium.ui(), bodySmall = b.bodySmall.ui(),
        labelLarge = b.labelLarge.ui(), labelMedium = b.labelMedium.ui(), labelSmall = b.labelSmall.ui(),
    )
}

@Composable
fun TerminalTheme(content: @Composable () -> Unit) {
    val p = ThemeState.palette
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = p.accent, onPrimary = p.bg,
            secondary = p.second, onSecondary = p.bg,
            error = p.alert,
            background = p.bg, onBackground = p.text,
            surface = p.panel, onSurface = p.text,
            surfaceVariant = p.panel2, onSurfaceVariant = p.muted,
            outline = p.accent.copy(alpha = 0.4f),
        ),
        typography = typography(p),
        content = content,
    )
}
