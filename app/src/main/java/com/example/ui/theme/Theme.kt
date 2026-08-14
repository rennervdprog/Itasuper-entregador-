package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ItaLightColorScheme = lightColorScheme(
    primary = ItaOrange,
    onPrimary = Color.White,
    primaryContainer = ItaOrangeLight,
    onPrimaryContainer = ItaOrangeDark,
    secondary = ItaGreen,
    onSecondary = Color.White,
    secondaryContainer = ItaGreenLight,
    onSecondaryContainer = ItaGreenDark,
    tertiary = ItaOrangeDark,
    onTertiary = Color.White,
    background = ItaBackground,
    onBackground = ItaTextPrimary,
    surface = ItaSurface,
    onSurface = ItaTextPrimary,
    surfaceVariant = Color(0xFFF4F4F5),
    onSurfaceVariant = ItaTextSecondary,
    outline = ItaDivider,
    outlineVariant = ItaBorder,
    error = ItaStatusDanger,
    onError = Color.White,
    errorContainer = ItaStatusDangerBg,
    onErrorContainer = ItaStatusDanger
)

private val ItaDarkColorScheme = darkColorScheme(
    primary = ItaOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF431A00),
    onPrimaryContainer = ItaOrangeLight,
    secondary = ItaGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = ItaGreenLight,
    tertiary = ItaOrangeHover,
    onTertiary = Color.White,
    background = Color(0xFF121212),
    onBackground = Color(0xFFF1F1F1),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFF1F1F1),
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF333333),
    outlineVariant = Color(0xFF444444),
    error = Color(0xFFF87171),
    onError = Color.Black
)

@Composable
fun ItaSuperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ItaDarkColorScheme else ItaLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
