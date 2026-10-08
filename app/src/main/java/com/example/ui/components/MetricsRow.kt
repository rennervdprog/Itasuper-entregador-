package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ItaBorder
import com.example.ui.theme.ItaGreenDark
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaSlate500
import com.example.ui.theme.ItaSlate900
import com.example.ui.theme.ItaSurface

@Composable
fun MetricsRow(
    activeCount: Int,
    availableCount: Int,
    completedCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricItem(
            title = "Na Rota",
            value = activeCount.toString(),
            valueColor = if (activeCount > 0) ItaOrange else ItaSlate900,
            modifier = Modifier.weight(1f),
            testTag = "metric_active_route"
        )
        MetricItem(
            title = "Disponíveis",
            value = availableCount.toString(),
            valueColor = if (availableCount > 0) ItaSlate900 else ItaSlate500,
            modifier = Modifier.weight(1f),
            testTag = "metric_available_orders"
        )
        MetricItem(
            title = "Concluídas",
            value = completedCount.toString(),
            valueColor = if (completedCount > 0) ItaSlate900 else ItaSlate500,
            subtitle = "no total",
            modifier = Modifier.weight(1f),
            testTag = "metric_completed_orders"
        )
    }
}

@Composable
private fun MetricItem(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    testTag: String = ""
) {
    Column(
        modifier = modifier
            .background(ItaSurface, RoundedCornerShape(12.dp))
            .border(1.dp, ItaBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp, horizontal = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = ItaSlate500
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = ItaSlate500
            )
        }
    }
}

