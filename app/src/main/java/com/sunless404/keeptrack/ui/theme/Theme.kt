package com.sunless404.keeptrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Paleta de la marca: pizarra oscuro (#232C35) y gris azulado claro (#ACBAC7). */
object Brand {
    val Slate = Color(0xFF232C35)
    val Mist = Color(0xFFACBAC7)
    val Accent = Color(0xFF7FB2E5)
    val Fire = Color(0xFFFF8A3D)
}

private val Dark = darkColorScheme(
    primary = Brand.Mist,
    onPrimary = Brand.Slate,
    primaryContainer = Color(0xFF34404C),
    onPrimaryContainer = Color(0xFFDCE4EC),
    secondary = Brand.Accent,
    onSecondary = Color(0xFF0E1E2E),
    secondaryContainer = Color(0xFF2B3A49),
    onSecondaryContainer = Color(0xFFD5E6F7),
    tertiary = Brand.Fire,
    tertiaryContainer = Color(0xFF4A3426),
    onTertiaryContainer = Color(0xFFFFDCC6),
    background = Color(0xFF12171C),
    onBackground = Color(0xFFE4E9EE),
    surface = Color(0xFF12171C),
    onSurface = Color(0xFFE4E9EE),
    surfaceVariant = Brand.Slate,
    onSurfaceVariant = Brand.Mist,
    surfaceContainerLowest = Color(0xFF0D1115),
    surfaceContainerLow = Color(0xFF171D23),
    surfaceContainer = Color(0xFF1B2229),
    surfaceContainerHigh = Brand.Slate,
    surfaceContainerHighest = Color(0xFF2B3540),
    outline = Color(0xFF5E6B78),
    outlineVariant = Color(0xFF34404C),
)

private val Light = lightColorScheme(
    primary = Brand.Slate,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE4EC),
    onPrimaryContainer = Brand.Slate,
    secondary = Color(0xFF2F6EA8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E6F6),
    onSecondaryContainer = Color(0xFF0E2A44),
    tertiary = Color(0xFFD9641A),
    tertiaryContainer = Color(0xFFFFE2CF),
    onTertiaryContainer = Color(0xFF4A2408),
    background = Color(0xFFF5F7F9),
    onBackground = Color(0xFF161B20),
    surface = Color(0xFFF5F7F9),
    onSurface = Color(0xFF161B20),
    surfaceVariant = Color(0xFFE3E8ED),
    onSurfaceVariant = Color(0xFF4F5B67),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F3F6),
    surfaceContainer = Color(0xFFEAEEF2),
    surfaceContainerHigh = Color(0xFFE3E8ED),
    surfaceContainerHighest = Color(0xFFDCE2E8),
    outline = Color(0xFF8A96A2),
    outlineVariant = Color(0xFFCBD3DB),
)

private val base = Typography()
private val typography = base.copy(
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp),
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun KeepTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = typography,
        shapes = shapes,
        content = content,
    )
}
