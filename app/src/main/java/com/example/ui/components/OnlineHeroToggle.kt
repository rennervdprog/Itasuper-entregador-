package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBike
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ItaDarkOffline
import com.example.ui.theme.ItaDarkOfflineCard
import com.example.ui.theme.ItaGreen
import com.example.ui.theme.ItaGreenDark
import com.example.ui.theme.ItaOrange

@Composable
fun OnlineHeroToggle(
    isOnline: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isOnline) ItaGreenDark else ItaDarkOffline,
        animationSpec = tween(300),
        label = "hero_bg_color"
    )

    // Transição curta de entrada/saída; evita uma animação infinita em segundo plano.
    val pulseAlpha by animateFloatAsState(
        targetValue = if (isOnline) 1.0f else 0.4f,
        animationSpec = tween(300),
        label = "pulse_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle(!isOnline) }
            .testTag("hero_online_toggle_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Status indicator circle with pulsing glow when online
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(44.dp)
                ) {
                    if (isOnline) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .alpha(pulseAlpha * 0.4f)
                                .background(Color.White, CircleShape)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                color = if (isOnline) Color.White else ItaDarkOfflineCard,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isOnline) Icons.Default.ElectricBike else Icons.Default.PowerSettingsNew,
                            contentDescription = if (isOnline) "Online" else "Offline",
                            tint = if (isOnline) ItaGreenDark else Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isOnline) "VOCÊ ESTÁ ONLINE" else "VOCÊ ESTÁ OFFLINE",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (isOnline) Color.White else Color(0xFF94A3B8),
                                    shape = CircleShape
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isOnline) "Recebendo pedidos das suas lojas" else "Toque para começar a receber pedidos",
                        color = if (isOnline) Color(0xFFE8F5E9) else Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Switch(
                checked = isOnline,
                onCheckedChange = { onToggle(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF047857),
                    uncheckedThumbColor = Color(0xFF94A3B8),
                    uncheckedTrackColor = ItaDarkOfflineCard
                ),
                modifier = Modifier.testTag("switch_online_status")
            )
        }
    }
}
