package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ItaGreen
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaSlate200
import com.example.ui.theme.ItaSlate300
import com.example.ui.theme.ItaSurface
import com.example.ui.theme.ItaTextPrimary
import com.example.ui.theme.ItaTextSecondary

enum class RouteStopState {
  DONE,
  CURRENT,
  NEXT,
}

data class RouteStop(
  val title: String,
  val subtitle: String? = null,
  val state: RouteStopState,
)

/**
 * A linha da rota: marca visual do Modo Pilotagem. Linha vertical ligando a
 * loja às paradas, com a parada atual pulsando em laranja. Usada na oferta,
 * na rota ativa e na espera de vínculo. Não é decoração: é a informação.
 */
@Composable
fun RouteLine(
  stops: List<RouteStop>,
  modifier: Modifier = Modifier,
) {
  val pulse by rememberInfiniteTransition(label = "routePulse").animateFloat(
    initialValue = 1f,
    targetValue = 0.45f,
    animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
    label = "routePulseAlpha",
  )
  Column(modifier = modifier) {
    stops.forEachIndexed { index, stop ->
      Row {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(top = 4.dp),
        ) {
          val dotModifier = when (stop.state) {
            RouteStopState.DONE ->
              Modifier.size(12.dp).clip(CircleShape).background(ItaGreen)
            RouteStopState.CURRENT ->
              Modifier.size(16.dp).alpha(pulse).clip(CircleShape).background(ItaOrange)
            RouteStopState.NEXT ->
              Modifier.size(12.dp).clip(CircleShape).background(ItaSurface)
                .border(2.dp, ItaSlate300, CircleShape)
          }
          Box(dotModifier)
          if (index < stops.lastIndex) {
            Box(
              Modifier.width(3.dp).height(28.dp).background(ItaSlate200),
            )
          }
        }
        Column(
          modifier = Modifier.padding(start = 12.dp, bottom = if (index < stops.lastIndex) 8.dp else 0.dp),
        ) {
          Text(
            text = stop.title,
            style = MaterialTheme.typography.titleSmall,
            color = ItaTextPrimary,
          )
          stop.subtitle?.let {
            Text(
              text = it,
              style = MaterialTheme.typography.bodySmall,
              color = ItaTextSecondary,
            )
          }
        }
      }
    }
  }
}
