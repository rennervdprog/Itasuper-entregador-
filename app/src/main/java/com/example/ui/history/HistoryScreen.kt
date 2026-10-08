package com.example.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
    val totalFees = entries.sumOf { it.deliveryFee }

    val filters = listOf("7 dias", "30 dias", "Tudo")

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                    fontSize = 13.sp,
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

            // 4 Metrics Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HistoryMetricCard(
                            title = "Corridas",
                            value = "${summary.totalRides} entregas",
                            icon = Icons.Default.DirectionsBike,
                            iconColor = ItaOrange,
                            modifier = Modifier.weight(1f),
                            testTag = "metric_history_rides"
                        )
                        if (totalFees > 0.0) {
                            HistoryMetricCard(
                                title = "Taxas do período",
                                value = formatBrl(totalFees),
                                subtitle = "a acertar com a loja",
                                icon = Icons.Default.AttachMoney,
                                iconColor = ItaGreenDark,
                                modifier = Modifier.weight(1f),
                                testTag = "metric_history_fees"
                            )
                        }
                    }

                    if (summary.totalDistanceKm > 0.0) {
                        HistoryMetricCard(
                            title = "Distância Total",
                            value = "${String.format("%.1f", summary.totalDistanceKm)} km",
                            icon = Icons.Default.Timeline,
                            iconColor = Color(0xFF3B82F6),
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "metric_history_distance"
                        )
                    }
                }
            }

            // Discrete Straight Line Disclaimer
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "A distância é estimada em linha reta com base nas coordenadas disponíveis.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // History Header
            item {
                Text(
                    text = "Histórico de entregas",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ItaTextPrimary
                )
            }

            // History entries
            if (entries.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ItaSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ItaBorder)
                    ) {
                        Text(
                            text = "Nenhuma entrega concluída neste período. Altere o filtro para consultar outro intervalo.",
                            modifier = Modifier.padding(18.dp),
                            color = ItaTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(entries, key = { it.id }) { entry ->
                    HistoryEntryCard(entry = entry)
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ItaSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ItaBorder)
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
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = ItaTextSecondary
                )
                Text(
                    text = value,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ItaTextPrimary
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ItaSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ItaBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Store Name + Date/Time + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .background(ItaOrangeLight, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = entry.storeName.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = ItaOrange
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${entry.orderShortCode} · ${formatRelativeDateTime(entry.dateFormatted, entry.timeFormatted)}",
                        fontSize = 13.sp,
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
                                else -> Color(0xFFE2E8F0)
                            },
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = entry.statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (entry.statusText) {
                            "Recebido", "Concluída" -> ItaGreenDark
                            "Confirmar" -> ItaStatusPending
                            else -> Color(0xFF475569)
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
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = ItaTextPrimary
            )
            Text(
                text = entry.addressSummary,
                fontSize = 12.sp,
                color = ItaTextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            val distanceLabel =
                if (entry.distanceKm > 0.0) "${String.format("%.1f", entry.distanceKm)} km" else null
            val feeLabel =
                if (entry.deliveryFee > 0.0) "Taxa ${formatBrl(entry.deliveryFee)}" else null
            val metaLabel = listOfNotNull(distanceLabel, feeLabel).joinToString(" · ")
            if (metaLabel.isNotEmpty()) {
                Text(
                    text = metaLabel,
                    fontSize = 11.sp,
                    color = ItaTextTertiary
                )
            }
        }
    }
}

private fun formatBrl(value: Double): String =
    "R$ " + String.format("%.2f", value).replace('.', ',')

/**
 * Exibe a data de forma relativa e amigavel: "Hoje, 16:14", "Ontem, 16:14"
 * ou "06/10, 16:14" (com ano reduzido quando for de outro ano).
 * Espera dateFormatted no formato ISO "yyyy-MM-dd".
 */
private fun formatRelativeDateTime(dateFormatted: String, timeFormatted: String): String {
    return try {
        val parts = dateFormatted.split("-")
        if (parts.size != 3) return "$dateFormatted, $timeFormatted"
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
        val label = when {
            diffDays <= 0 -> "Hoje"
            diffDays == 1 -> "Ontem"
            date.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) ->
                "${parts[2]}/${parts[1]}"
            else -> "${parts[2]}/${parts[1]}/${parts[0].takeLast(2)}"
        }
        "$label, $timeFormatted"
    } catch (_: Exception) {
        "$dateFormatted, $timeFormatted"
    }
}
