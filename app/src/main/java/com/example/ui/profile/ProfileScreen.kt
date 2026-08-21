package com.example.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.example.data.model.DriverLinkStatus
import com.example.data.model.StoreDriverLink
import com.example.data.model.IncomingOrderAlert
import com.example.data.model.NavigationPreference
import com.example.platform.AddressGeocodingCache
import com.example.platform.BrazilianMunicipality
import com.example.platform.BrazilianMunicipalityCatalog
import com.example.platform.DriverBiometricAccess
import com.example.platform.DriverBiometricAuthenticator
import com.example.platform.IncomingOrderOverlay
import com.example.ui.onboarding.DriverLinkViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    linkViewModel: DriverLinkViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.profile.collectAsState()
    val linkedStores by viewModel.linkedStores.collectAsState()
    val navPreference by viewModel.navPreference.collectAsState()
    val directoryPreference by viewModel.directoryPreference.collectAsState()
    val isSavingDirectoryPreference by viewModel.isSavingDirectoryPreference.collectAsState()
    val directoryPreferenceMessage by viewModel.directoryPreferenceMessage.collectAsState()
    val isDirectoryPreferenceError by viewModel.isDirectoryPreferenceError.collectAsState()
    val linkUiState by linkViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val biometricActivity = context as? FragmentActivity
    var biometricEnabled by remember(profile.id) {
        mutableStateOf(profile.id.isNotBlank() && DriverBiometricAccess.isEnabledFor(profile.id))
    }

    fun enableBiometricsFromProfile() {
        val host = biometricActivity
        if (host == null || profile.id.isBlank()) {
            Toast.makeText(context, "Não foi possível iniciar a biometria neste aparelho.", Toast.LENGTH_LONG).show()
            return
        }
        DriverBiometricAuthenticator.authenticate(
            activity = host,
            onSuccess = {
                DriverBiometricAccess.enableFor(profile.id)
                biometricEnabled = true
                Toast.makeText(context, "Biometria ativada para este aparelho.", Toast.LENGTH_SHORT).show()
            },
            onError = { message ->
                Toast.makeText(
                    context,
                    if (message.isBlank()) "A biometria não foi ativada." else message,
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

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
                            text = "Entregador oficial ItaSuper",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ItaGreenDark
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = ItaDivider)
                    Spacer(modifier = Modifier.height(14.dp))

                    if (profile.memberSince.isNotBlank()) {
                        Text(
                            text = "Cadastro em ${profile.memberSince}",
                            fontSize = 12.sp,
                            color = ItaTextSecondary
                        )
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
                        text = "Dados cadastrais",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ItaTextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ProfileInfoRow(
                        icon = Icons.Default.Email,
                        label = "E-mail",
                        value = profile.email.ifBlank { "Não informado" }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ProfileInfoRow(
                        icon = Icons.Default.Phone,
                        label = "Telefone",
                        value = profile.phone.ifBlank { "Não informado" }
                    )
                    if (profile.vehicleType.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        ProfileInfoRow(
                            icon = Icons.Default.TwoWheeler,
                            label = "Veículo",
                            value = listOf(profile.vehicleType, profile.vehiclePlate)
                                .filter { it.isNotBlank() }
                                .joinToString(" · ")
                        )
                    }
                }
            }

            val pendingInvites = linkViewModel.links.collectAsState().value
                .filter { it.status == DriverLinkStatus.PENDING }
            if (pendingInvites.isNotEmpty()) {
                PendingStoreInvitesCard(
                    invites = pendingInvites,
                    isChecking = linkUiState.isCheckingInvites,
                    processingInviteActions = linkUiState.processingInviteActions,
                    onRefresh = linkViewModel::checkNewInvites,
                    onAccept = { linkViewModel.acceptInvite(it) {} },
                    onReject = linkViewModel::rejectInvite
                )
            }

            DriverDirectoryPreferenceCard(
                preference = directoryPreference,
                isSaving = isSavingDirectoryPreference,
                message = directoryPreferenceMessage,
                isError = isDirectoryPreferenceError,
                onSave = viewModel::saveDirectoryPreference
            )

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
                            text = "Lojas vinculadas (${acceptedStores.size})",
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
                        text = "Navegação padrão",
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

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ItaSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ItaBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = ItaOrange,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Entrada com biometria",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ItaTextPrimary
                            )
                            Text(
                                text = if (biometricEnabled) "Ativada neste aparelho" else "Use a impressão digital para entrar mais rápido",
                                fontSize = 12.sp,
                                color = ItaTextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (biometricEnabled) {
                                DriverBiometricAccess.disableFor(profile.id)
                                biometricEnabled = false
                                Toast.makeText(context, "Biometria desativada neste aparelho.", Toast.LENGTH_SHORT).show()
                            } else {
                                enableBiometricsFromProfile()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_profile_biometric"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (biometricEnabled) Color(0xFFFEE2E2) else ItaOrange,
                            contentColor = if (biometricEnabled) ItaStatusDanger else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (biometricEnabled) "Desativar biometria" else "Ativar biometria",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ItaSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ItaBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Alerta visual de nova entrega",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ItaTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (IncomingOrderOverlay.isAllowed(context)) {
                            "Sobreposição autorizada. Use o teste para confirmar a chamada visual."
                        } else {
                            "Sobreposição ainda não autorizada neste aparelho."
                        },
                        fontSize = 13.sp,
                        color = ItaTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val shown = IncomingOrderOverlay.show(
                                context = context,
                                orderId = "overlay-test",
                                storeName = "Loja de teste ItaSuper",
                                neighborhood = "Painel visual",
                                shortCode = "#11F08E4A",
                                alert = IncomingOrderAlert(
                                    orderId = "overlay-test",
                                    shortCode = "#11F08E4A",
                                    storeName = "Loja de teste ItaSuper",
                                    pickupAddress = "Avenida Brasil, 524 · Centro · Araruama",
                                    destinationAddress = "Rua Professor Nunes Martins · Ponte dos Leites",
                                    neighborhood = "Ponte dos Leites",
                                    itemCount = 3,
                                    paymentMethod = "Pix",
                                    totalLabel = "R$ 42,90"
                                )
                            )
                            if (!shown) {
                                Toast.makeText(context, IncomingOrderOverlay.lastDiagnostic(), Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Testar painel visual", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Abrir permissão do Android",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                            }
                            .padding(vertical = 8.dp),
                        color = ItaOrange,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
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
                    text = "Sair da conta",
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
private fun PendingStoreInvitesCard(
    invites: List<StoreDriverLink>,
    isChecking: Boolean,
    processingInviteActions: Map<String, Boolean>,
    onRefresh: () -> Unit,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_pending_store_invites"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE8D6)),
        border = androidx.compose.foundation.BorderStroke(1.dp, ItaOrange)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Convites de lojas (${invites.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ItaTextPrimary
                    )
                    Text(
                        text = "Lojistas podem convidar você para uma parceria.",
                        fontSize = 12.sp,
                        color = Color(0xFF7C2D12)
                    )
                }
                Icon(
                    imageVector = Icons.Default.Store,
                    contentDescription = null,
                    tint = ItaOrange,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            invites.forEachIndexed { index, invite ->
                val processingAction = processingInviteActions[invite.id]
                val isProcessing = processingAction != null
                val isAccepting = processingAction == true
                if (index > 0) Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFD6B3), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFF97316), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = invite.store.tradeName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ItaTextPrimary
                    )
                    Text(
                        text = listOf(invite.store.address, invite.store.neighborhood, invite.store.city)
                            .filter { it.isNotBlank() }
                            .joinToString(" · ")
                            .ifBlank { "Endereço da loja não informado" },
                        fontSize = 12.sp,
                        color = Color(0xFF7C2D12)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onReject(invite.id) },
                            enabled = !isProcessing,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ItaStatusDanger),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isProcessing && !isAccepting) {
                                CircularProgressIndicator(
                                    color = ItaStatusDanger,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Recusar", color = ItaStatusDanger, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Button(
                            onClick = { onAccept(invite.id) },
                            enabled = !isProcessing,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ItaGreenDark),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isProcessing && isAccepting) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Aceitar", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onRefresh,
                enabled = !isChecking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ItaOrange),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        color = ItaOrange,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Atualizar convites", color = ItaOrange, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DriverDirectoryPreferenceCard(
    preference: com.example.data.model.DriverDirectoryPreference,
    isSaving: Boolean,
    message: String?,
    isError: Boolean?,
    onSave: (city: String, isListed: Boolean, hasContactConsent: Boolean, isCityConfirmed: Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var municipalities by remember { mutableStateOf<List<BrazilianMunicipality>>(emptyList()) }
    var citySuggestions by remember { mutableStateOf<List<BrazilianMunicipality>>(emptyList()) }
    var isSearchingCities by remember { mutableStateOf(false) }
    var city by remember(preference.city) { mutableStateOf(preference.city) }
    var selectedMunicipality by remember(preference.city) { mutableStateOf<BrazilianMunicipality?>(null) }
    var isCatalogLoading by remember { mutableStateOf(true) }
    var catalogLoadError by remember { mutableStateOf<String?>(null) }
    var isLocatingCity by remember { mutableStateOf(false) }
    var locationMessage by remember { mutableStateOf<String?>(null) }
    var isListed by remember(preference.city, preference.isListed) { mutableStateOf(preference.isListed) }
    var hasContactConsent by remember(preference.city, preference.isListed, preference.hasContactConsent) {
        mutableStateOf(preference.hasContactConsent)
    }

    LaunchedEffect(Unit) {
        runCatching { BrazilianMunicipalityCatalog.load(context) }
            .onSuccess { municipalities = it }
            .onFailure {
                catalogLoadError = "Não foi possível carregar a lista de cidades. Feche e abra o Perfil novamente."
            }
        isCatalogLoading = false
    }
    LaunchedEffect(preference.city, municipalities) {
        if (selectedMunicipality == null && preference.city.isNotBlank() && municipalities.isNotEmpty()) {
            selectedMunicipality = withContext(Dispatchers.Default) {
                BrazilianMunicipalityCatalog.findByName(municipalities, preference.city)
            }
        }
    }

    LaunchedEffect(city, municipalities, selectedMunicipality) {
        val query = city
        val shouldSearch = query.trim().length >= 2 &&
            (selectedMunicipality == null || selectedMunicipality?.name != query)
        if (!shouldSearch) {
            citySuggestions = emptyList()
            isSearchingCities = false
            return@LaunchedEffect
        }

        isSearchingCities = true
        delay(120)
        citySuggestions = withContext(Dispatchers.Default) {
            BrazilianMunicipalityCatalog.search(municipalities, query)
        }
        isSearchingCities = false
    }

    fun requestCityFromLocation() {
        isLocatingCity = true
        locationMessage = null
        scope.launch {
            val suggestion = AddressGeocodingCache.currentDriverMunicipality(context)
            val matchedMunicipality = suggestion?.let {
                BrazilianMunicipalityCatalog.findByName(municipalities, it.city)
            }
            if (matchedMunicipality != null) {
                city = matchedMunicipality.name
                selectedMunicipality = matchedMunicipality
                locationMessage = "Cidade sugerida pela sua localização. Confira antes de salvar."
            } else {
                locationMessage = "Não foi possível identificar uma cidade válida pela localização. Pesquise e selecione na lista."
            }
            isLocatingCity = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            requestCityFromLocation()
        } else {
            isLocatingCity = false
            locationMessage = "A localização é opcional. Pesquise sua cidade na lista para continuar."
        }
    }

    fun useMyLocation() {
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasLocationPermission) {
            requestCityFromLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_driver_directory"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE8D6)),
        border = androidx.compose.foundation.BorderStroke(1.dp, ItaOrange)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(ItaOrange, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(23.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Base de motoboys da cidade",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = ItaTextPrimary
                    )
                    Text(
                        text = "Participe somente se quiser receber contato de lojas.",
                        fontSize = 12.sp,
                        color = Color(0xFF7C2D12)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF59E0B))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "1. Defina sua cidade de atuação",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ItaTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Pesquise e escolha um município válido antes de salvar.",
                fontSize = 12.sp,
                color = Color(0xFF7C2D12)
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = city,
                onValueChange = { value ->
                    city = value
                    // Digitar não confirma a cidade: a confirmação ocorre apenas ao tocar em uma opção.
                    selectedMunicipality = null
                    locationMessage = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_directory_city"),
                enabled = !isSaving && !isLocatingCity,
                singleLine = true,
                label = { Text("Cidade de atuação") },
                placeholder = { Text("Pesquise o município") },
                supportingText = {
                    Text(
                        text = when {
                            isCatalogLoading -> "Carregando municípios brasileiros..."
                            selectedMunicipality != null -> "Cidade confirmada: ${selectedMunicipality?.displayName}"
                            else -> "Digite ao menos 2 letras e toque em uma cidade da lista."
                        },
                        color = ItaTextPrimary
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ItaOrange,
                    focusedLabelColor = ItaOrange,
                    cursorColor = ItaOrange,
                    focusedTextColor = ItaTextPrimary,
                    unfocusedTextColor = ItaTextPrimary,
                    focusedContainerColor = Color(0xFFFFE0CC),
                    unfocusedContainerColor = Color(0xFFFFE0CC),
                    unfocusedBorderColor = ItaOrange
                )
            )

            val showSuggestions = city.trim().length >= 2 &&
                (selectedMunicipality == null || selectedMunicipality?.name != city)
            if (showSuggestions && citySuggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFEDD5), RoundedCornerShape(10.dp))
                        .border(1.dp, ItaOrange, RoundedCornerShape(10.dp))
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Toque para confirmar sua cidade",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9A3412),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                    citySuggestions.forEach { municipality ->
                        Text(
                            text = municipality.displayName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isSaving && !isLocatingCity) {
                                    city = municipality.name
                                    selectedMunicipality = municipality
                                    locationMessage = null
                                }
                                .padding(horizontal = 12.dp, vertical = 11.dp)
                                .testTag("option_directory_city_${municipality.state}_${municipality.name}"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ItaTextPrimary
                        )
                    }
                }
            } else if (showSuggestions && isSearchingCities) {
                Text(
                    text = "Buscando cidades...",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF9A3412),
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else if (showSuggestions && !isCatalogLoading && citySuggestions.isEmpty()) {
                Text(
                    text = "Nenhuma cidade encontrada. Confira a escrita ou tente outra busca.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF9A3412),
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else if (catalogLoadError != null) {
                Text(
                    text = catalogLoadError.orEmpty(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = ItaStatusDanger,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = ::useMyLocation,
                enabled = !isSaving && !isLocatingCity,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("btn_use_location_for_directory_city"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD94801),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isLocatingCity) "Identificando cidade..." else "Usar minha localização",
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "A localização é usada apenas para sugerir sua cidade. Você confirma antes de salvar.",
                fontSize = 12.sp,
                color = Color(0xFF7C2D12),
                modifier = Modifier.padding(top = 6.dp)
            )
            if (locationMessage != null) {
                Text(
                    text = locationMessage.orEmpty(),
                    fontSize = 12.sp,
                    color = Color(0xFF7C2D12),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "2. Escolha sua visibilidade",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ItaTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFD6B3), RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFFF97316), RoundedCornerShape(14.dp))
                    .clickable(enabled = !isSaving) {
                        isListed = !isListed
                        if (!isListed) hasContactConsent = false
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Aparecer para lojistas",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ItaTextPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isListed) "Seu perfil pode ser encontrado por lojas da mesma cidade." else "Desligado: seu perfil não fica visível na base.",
                        fontSize = 12.sp,
                        color = Color(0xFF7C2D12)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Switch(
                    checked = isListed,
                    onCheckedChange = { checked ->
                        isListed = checked
                        if (!checked) hasContactConsent = false
                    },
                    enabled = !isSaving,
                    modifier = Modifier.testTag("switch_directory_listing"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ItaOrange
                    )
                )
            }

            if (isListed) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "3. Confirme a autorização de contato",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ItaTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFD6B3), RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFFF97316), RoundedCornerShape(14.dp))
                        .clickable(enabled = !isSaving) { hasContactConsent = !hasContactConsent }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Checkbox(
                        checked = hasContactConsent,
                        onCheckedChange = { hasContactConsent = it },
                        enabled = !isSaving,
                        modifier = Modifier.testTag("checkbox_directory_consent")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Autorizo a ItaSuper a mostrar meu nome, cidade, veículo e WhatsApp para lojistas da mesma cidade, exclusivamente para que possam entrar em contato direto sobre possível contratação.",
                        fontSize = 12.sp,
                        color = ItaTextPrimary,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Importante",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9A3412)
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "A ItaSuper não contrata, não paga e não intermedeia acordos. Contratação, pagamento, escala e seguro são definidos diretamente entre lojista e motoboy.",
                fontSize = 12.sp,
                color = Color(0xFF7C2D12),
                modifier = Modifier
                    .background(Color(0xFFFFCFA9), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFF97316), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            )

            if (message != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isError == true) ItaStatusDanger else ItaGreenDark,
                    modifier = Modifier.testTag("text_directory_preference_feedback")
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = { onSave(city, isListed, hasContactConsent, selectedMunicipality != null) },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_save_directory_preference"),
                colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = when {
                        isSaving -> "Salvando..."
                        isListed -> "Salvar e aparecer na base"
                        else -> "Salvar preferência"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
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
