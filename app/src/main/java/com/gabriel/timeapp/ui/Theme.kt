package com.gabriel.timeapp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.gabriel.timeapp.R

// Tactical HUD palette
val Amber = Color(0xFFFFB23E)     // primary HUD accent
val Alert = Color(0xFFFF4D3A)     // critical only
val Cyan = Color(0xFF7FD8E6)      // secondary info
val Bg = Color(0xFF0A0D0B)        // olive gunmetal black
val Panel = Color(0xFF111611)
val Panel2 = Color(0xFF1C241C)
val GridLine = Color(0x1A9FB89F)
val Bone = Color(0xFFE6E8DF)
val Ash = Color(0xFF8C948A)

val Stencil = FontFamily(Font(R.font.black_ops_one))
val Mono = FontFamily(Font(R.font.share_tech_mono))
val Hud = FontFamily(
    Font(R.font.rajdhani_semibold, FontWeight.SemiBold),
    Font(R.font.rajdhani_bold, FontWeight.Bold),
)

private fun TextStyle.hud() = copy(fontFamily = Hud, fontWeight = FontWeight.SemiBold)

private val base = Typography()
private val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Stencil),
    displayMedium = base.displayMedium.copy(fontFamily = Stencil),
    displaySmall = base.displaySmall.copy(fontFamily = Stencil),
    headlineLarge = base.headlineLarge.hud(),
    headlineMedium = base.headlineMedium.hud(),
    headlineSmall = base.headlineSmall.hud(),
    titleLarge = base.titleLarge.hud(),
    titleMedium = base.titleMedium.hud(),
    titleSmall = base.titleSmall.hud(),
    bodyLarge = base.bodyLarge.hud(),
    bodyMedium = base.bodyMedium.hud(),
    bodySmall = base.bodySmall.hud(),
    labelLarge = base.labelLarge.hud(),
    labelMedium = base.labelMedium.hud(),
    labelSmall = base.labelSmall.hud(),
)

private val Scheme = darkColorScheme(
    primary = Amber, onPrimary = Bg,
    secondary = Cyan, onSecondary = Bg,
    error = Alert,
    background = Bg, onBackground = Bone,
    surface = Panel, onSurface = Bone,
    surfaceVariant = Panel2, onSurfaceVariant = Ash,
    outline = Color(0x66FFB23E),
)

@Composable
fun TerminalTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = Scheme, typography = AppTypography, content = content)
