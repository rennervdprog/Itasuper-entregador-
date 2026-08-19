package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.example.R

/**
 * Marca visual exclusiva do ItaSuper Entregador.
 * A arte possui fundo transparente e pode receber a cor adequada ao contexto.
 */
@Composable
fun DriverHelmetMark(
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    contentDescription: String? = null
) {
    Image(
        painter = painterResource(id = R.drawable.ic_launcher_helmet_foreground),
        contentDescription = contentDescription,
        modifier = modifier,
        colorFilter = tint.takeIf { it != Color.Unspecified }?.let(ColorFilter::tint)
    )
}
