package com.example.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DriverHistoryEntry
import com.example.ui.theme.ItaBackground
import com.example.ui.theme.ItaBorder
import com.example.ui.theme.ItaDivider
import com.example.ui.theme.ItaGreenDark
import com.example.ui.theme.ItaGreenLight
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaOrangeLight
import com.example.ui.theme.ItaSlate100
import com.example.ui.theme.ItaSlate200
import com.example.ui.theme.ItaSlate500
import com.example.ui.theme.ItaSlate600
import com.example.ui.theme.ItaStatusInTransit
import com.example.ui.theme.ItaStatusPending
import com.example.ui.theme.ItaStatusPendingBg
import com.example.ui.theme.ItaSurface
import com.example.ui.theme.ItaTextPrimary
import com.example.ui.theme.ItaTextSecondary
import com.example.ui.theme.ItaTextTertiary

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val entries by viewModel.historyEntries.collectAsState()
    val summary by viewModel.historySummary.collectAsState()

    val filters = listOf("7 dias", "30 dias", "Tudo")
    // Agrupado por dia, na ordem em que as entregas chegam (mais recentes
    // primeiro). O groupBy preserva a ordem de primeira aparição da chave.
    val entriesByDay = entries.groupBy { it.dateFormatted }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(ItaBackground),
        color = ItaBackground
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Segmented Filters Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("row_history_filters"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEach { filter ->
                        val isSelected = filter == selectedFilter
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setFilter(filter) },
                            label = {
                                Text(
                                    text = filter,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ItaOrangeLight,
                                selectedLabelColor = ItaOrange,
                                containerColor = ItaSurface,
                                labelColor = ItaTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) ItaOrange else ItaBorder
                            )
                        )
                    }
                }
            }

            // Metrics: corridas + distância, sem fantasma de ganhos.
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HistoryMetricCard(
                            title = "Corridas",
                            value = "${summary.totalRides} entregas",
                            icon = Icons.Filled.DirectionsBike,
                            iconColor = ItaOrange,
                            modifier = Modifier.weight(1f),
                            testTag = "metric_history_rides"
                        )
                    }

                    if (summary.totalDistanceKm > 0.0) {
                        HistoryMetricCard(
                            title = "Distância Total",
                            value = "${String.format("%.1f", summary.totalDistanceKm)} km",
                            icon = Icons.Filled.Timeline,
                            iconColor = ItaStatusInTransit,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "metric_history_distance"
                        )
                    }
                }
            }

            // Aviso honesto: o pagamento é da loja, fora do app.
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ItaOrangeLight, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Store,
                        contentDescription = null,
                        tint = ItaOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Você é pago pela loja, fora do app. Aqui ficam suas corridas e distâncias — nada de “ganhos” que não existem.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = ItaTextSecondary
                    )
                }
            }

            // Discrete Straight Line Disclaimer
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ItaSlate100, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = ItaSlate500,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "A distância é estimada em linha reta com base nas coordenadas disponíveis.",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = ItaSlate500
                    )
                }
            }

            // History Header
            item {
                Text(
                    text = "Histórico de entregas",
                    style = MaterialTheme.typography.titleSmall,
                    color = ItaTextPrimary
                )
            }

            // O HistoryViewModel expõe só filtro, lista e resumo — sem estado
            // de loading/erro. Sem sinal real, inventar spinner/erro seria
            // desonesto: a lista vazia é o estado vazio (também é o que se
            // vê no instante inicial, antes do primeiro carregamento).
            if (entries.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ItaSurface),
                        border = BorderStroke(1.dp, ItaBorder)
                    ) {
                        Text(
                            text = "Nenhuma entrega concluída neste período. Altere o filtro para consultar outro intervalo.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ItaTextSecondary
                        )
                    }
                }
            } else {
                entriesByDay.forEach { (date, dayEntries) ->
                    item {
                        Text(
                            text = formatDayLabel(date),
                            style = MaterialTheme.typography.titleSmall,
                            color = ItaTextPrimary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(dayEntries, key = { it.id }) { entry ->
                        HistoryEntryCard(entry = entry)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun HistoryMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    testTag: String = ""
) {
    Card(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ItaSurface),
        border = BorderStroke(1.dp, ItaBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = ItaTextSecondary
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    color = ItaTextPrimary
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = ItaTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryCard(
    entry: DriverHistoryEntry
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_history_entry_${entry.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ItaSurface),
        border = BorderStroke(1.dp, ItaBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Store Name + Time + Status (o dia está no cabeçalho do grupo)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .background(ItaOrangeLight, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = entry.storeName.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                            color = ItaOrange
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${entry.orderShortCode} · ${entry.timeFormatted}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = ItaTextPrimary
                    )
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .background(
                            when (entry.statusText) {
                                "Recebido", "Concluída" -> ItaGreenLight
                                "Confirmar" -> ItaStatusPendingBg
                                else -> ItaSlate200
                            },
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = entry.statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (entry.statusText) {
                            "Recebido", "Concluída" -> ItaGreenDark
                            "Confirmar" -> ItaStatusPending
                            else -> ItaSlate600
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = ItaDivider)
            Spacer(modifier = Modifier.height(8.dp))

            // Destination
            Text(
                text = entry.neighborhood,
                style = MaterialTheme.typography.titleSmall,
                color = ItaTextPrimary
            )
            Text(
                text = entry.addressSummary,
                style = MaterialTheme.typography.bodySmall,
                color = ItaTextSecondary
            )

            if (entry.distanceKm > 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${String.format("%.1f", entry.distanceKm)} km",
                    style = MaterialTheme.typography.labelSmall,
                    color = ItaTextTertiary
                )
            }
        }
    }
}

/**
 * Rótulo do dia, no mesmo padrão relativo de antes: "Hoje", "Ontem" ou
 * "06/10" (com ano reduzido quando for de outro ano).
 * Espera dateFormatted no formato ISO "yyyy-MM-dd".
 */
private fun formatDayLabel(dateFormatted: String): String {
    return try {
        val parts = dateFormatted.split("-")
        if (parts.size != 3) return dateFormatted
        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val date = java.util.Calendar.getInstance().apply {
            set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val diffDays = ((today.timeInMillis - date.timeInMillis) / 86_400_000L).toInt()
        when {
            diffDays <= 0 -> "Hoje"
            diffDays == 1 -> "Ontem"
            date.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) ->
                "${parts[2]}/${parts[1]}"
            else -> "${parts[2]}/${parts[1]}/${parts[0].takeLast(2)}"
        }
    } catch (_: Throwable) {
        dateFormatted
    }
}
