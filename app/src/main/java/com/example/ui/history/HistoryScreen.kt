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
                            title = "Ganhos",
                            value = "R$ ${String.format("%.2f", summary.totalEarnings)}",
                            icon = Icons.Default.AttachMoney,
                            iconColor = ItaGreenDark,
                            modifier = Modifier.weight(1f),
                            testTag = "metric_history_earnings"
                        )
                        HistoryMetricCard(
                            title = "Corridas",
                            value = "${summary.totalRides} entregas",
                            icon = Icons.Default.DirectionsBike,
                            iconColor = ItaOrange,
                            modifier = Modifier.weight(1f),
                            testTag = "metric_history_rides"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HistoryMetricCard(
                            title = "Distância Total",
                            value = "${String.format("%.1f", summary.totalDistanceKm)} km",
                            icon = Icons.Default.Timeline,
                            iconColor = Color(0xFF3B82F6),
                            modifier = Modifier.weight(1f),
                            testTag = "metric_history_distance"
                        )
                        HistoryMetricCard(
                            title = "Tempo Total",
                            value = "${summary.totalTimeMinutes} min",
                            icon = Icons.Default.Schedule,
                            iconColor = Color(0xFF8B5CF6),
                            modifier = Modifier.weight(1f),
                            testTag = "metric_history_time"
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
                        text = "Distância demonstrativa estimada em linha reta.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // History Header
            item {
                Text(
                    text = "Registros de Corridas",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ItaTextPrimary
                )
            }

            // History entries
            items(entries, key = { it.id }) { entry ->
                HistoryEntryCard(entry = entry)
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
                    Text(
                        text = entry.storeName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ItaTextSecondary
                    )
                    Text(
                        text = "${entry.orderShortCode} · ${entry.dateFormatted}, ${entry.timeFormatted}",
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
                                "Recebido" -> ItaGreenLight
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
                            "Recebido" -> ItaGreenDark
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

            // Stats & Earnings Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${entry.distanceKm} km · ${entry.timeMinutes} min · Taxa: R$ ${String.format("%.2f", entry.deliveryFee)}",
                    fontSize = 11.sp,
                    color = ItaTextTertiary
                )

                Text(
                    text = "+ R$ ${String.format("%.2f", entry.driverEarnings)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ItaGreenDark
                )
            }
        }
    }
}
