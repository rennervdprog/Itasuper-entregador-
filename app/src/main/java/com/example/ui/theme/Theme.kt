package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
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

/**
 * O app é sempre claro, por decisão de produto (16/11/2026: "tudo em modo claro").
 * O esquema escuro anterior vazava via colorScheme e quebrava telas de fundo fixo
 * claro (ex.: título da oferta invisível em dark mode). Não reintroduzir dark theme.
 */
@Composable
fun ItaSuperTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ItaLightColorScheme,
        typography = Typography,
        content = content
    )
}
