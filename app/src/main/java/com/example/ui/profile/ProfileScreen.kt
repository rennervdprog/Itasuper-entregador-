package com.example.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.data.model.DriverLinkStatus
import com.example.data.model.NavigationPreference
import com.example.ui.theme.ItaBackground
import com.example.ui.theme.ItaBorder
import com.example.ui.theme.ItaDivider
import com.example.ui.theme.ItaGreen
import com.example.ui.theme.ItaGreenDark
import com.example.ui.theme.ItaGreenLight
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaOrangeLight
import com.example.ui.theme.ItaStatusDanger
import com.example.ui.theme.ItaSurface
import com.example.ui.theme.ItaTextPrimary
import com.example.ui.theme.ItaTextSecondary
import com.example.ui.theme.ItaTextTertiary

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.profile.collectAsState()
    val linkedStores by viewModel.linkedStores.collectAsState()
    val navPreference by viewModel.navPreference.collectAsState()

    val acceptedStores = linkedStores.filter { it.status == DriverLinkStatus.ACCEPTED }
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(ItaBackground),
        color = ItaBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profile Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_profile_header"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ItaSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ItaBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(ItaOrange, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.name.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = profile.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ItaTextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(ItaGreenLight, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = ItaGreenDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Entregador Oficial ItaSuper",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ItaGreenDark
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = ItaDivider)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Stats row: Rating, Deliveries, Member Since
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = profile.rating.toString(),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ItaTextPrimary
                                )
                            }
                            Text("Avaliação", fontSize = 11.sp, color = ItaTextSecondary)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = profile.completedDeliveriesCount.toString(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ItaTextPrimary
                            )
                            Text("Corridas", fontSize = 11.sp, color = ItaTextSecondary)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = profile.memberSince,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ItaTextPrimary
                            )
                            Text("No ItaSuper", fontSize = 11.sp, color = ItaTextSecondary)
                        }
                    }
                }
            }

            // Driver Data Details
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ItaSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ItaBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Dados Cadastrais",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ItaTextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ProfileInfoRow(icon = Icons.Default.Email, label = "E-mail", value = profile.email)
                    Spacer(modifier = Modifier.height(10.dp))
                    ProfileInfoRow(icon = Icons.Default.Phone, label = "Telefone", value = profile.phone)
                    Spacer(modifier = Modifier.height(10.dp))
                    ProfileInfoRow(icon = Icons.Default.TwoWheeler, label = "Veículo", value = "${profile.vehicleType} · ${profile.vehiclePlate}")
                }
            }

            // Linked Stores Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ItaSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ItaBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lojas Vinculadas (${acceptedStores.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ItaTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    acceptedStores.forEach { link ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Store,
                                    contentDescription = null,
                                    tint = ItaOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = link.store.tradeName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ItaTextPrimary
                                    )
                                    Text(
                                        text = "${link.store.neighborhood} · Desde ${link.acceptedAt ?: link.invitedAt}",
                                        fontSize = 11.sp,
                                        color = ItaTextSecondary
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Vinculado",
                                tint = ItaGreenDark,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // GPS App Preference
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ItaSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ItaBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "App de Navegação Padrão",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ItaTextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NavPreferenceOption(
                            name = "Google Maps",
                            isSelected = navPreference == NavigationPreference.GOOGLE_MAPS,
                            onClick = { viewModel.setNavPreference(NavigationPreference.GOOGLE_MAPS) },
                            modifier = Modifier.weight(1f)
                        )
                        NavPreferenceOption(
                            name = "Waze",
                            isSelected = navPreference == NavigationPreference.WAZE,
                            onClick = { viewModel.setNavPreference(NavigationPreference.WAZE) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Logout Action
            Button(
                onClick = { viewModel.logout(onLogout) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFEE2E2),
                    contentColor = ItaStatusDanger
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_logout_profile")
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    tint = ItaStatusDanger,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sair da Conta",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ItaStatusDanger
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ItaTextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = ItaTextTertiary)
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ItaTextPrimary)
        }
    }
}

@Composable
private fun NavPreferenceOption(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = if (isSelected) ItaOrangeLight else Color(0xFFF8FAFC),
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) ItaOrange else ItaBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) ItaOrange else ItaTextSecondary
        )
    }
}
