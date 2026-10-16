package com.gabriel.timeapp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.gabriel.timeapp.R

val Red = Color(0xFFFF2A2A)
val Bg = Color(0xFF070606)
val Panel = Color(0xFF140E0D)
val Panel2 = Color(0xFF221816)
val Bone = Color(0xFFF2E9DC)
val Ash = Color(0xFF8A817A)
val Cyan = Color(0xFF00E5FF)

val Stencil = FontFamily(Font(R.font.black_ops_one))
val Mono = FontFamily(Font(R.font.share_tech_mono))

private fun TextStyle.mono() = copy(fontFamily = Mono)

private val base = Typography()
private val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Stencil),
    displayMedium = base.displayMedium.copy(fontFamily = Stencil),
    displaySmall = base.displaySmall.copy(fontFamily = Stencil),
    headlineLarge = base.headlineLarge.mono(),
    headlineMedium = base.headlineMedium.mono(),
    headlineSmall = base.headlineSmall.mono(),
    titleLarge = base.titleLarge.mono(),
    titleMedium = base.titleMedium.mono(),
    titleSmall = base.titleSmall.mono(),
    bodyLarge = base.bodyLarge.mono(),
    bodyMedium = base.bodyMedium.mono(),
    bodySmall = base.bodySmall.mono(),
    labelLarge = base.labelLarge.mono(),
    labelMedium = base.labelMedium.mono(),
    labelSmall = base.labelSmall.mono(),
)

private val Scheme = darkColorScheme(
    primary = Red, onPrimary = Bg,
    secondary = Red, onSecondary = Bg,
    background = Bg, onBackground = Bone,
    surface = Panel, onSurface = Bone,
    surfaceVariant = Panel2, onSurfaceVariant = Ash,
    outline = Color(0x80FF2A2A),
)

@Composable
fun TerminalTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = Scheme, typography = AppTypography, content = content)
