package com.example

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.ui.platform.ComposeView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import com.example.data.fake.AppContainer
import com.example.data.remote.ItaSuperSupabase
import com.example.ui.navigation.AppNavHost
import com.example.ui.onboarding.DriverPermissionStatus
import com.example.ui.onboarding.DriverPermissionsScreen
import com.example.ui.system.BackendConfigurationScreen
import com.example.ui.theme.ItaBackground
import com.example.ui.theme.ItaSuperTheme

class MainActivity : FragmentActivity() {
    private val permissionRefreshHandler = Handler(Looper.getMainLooper())
    private var permissionStatus by mutableStateOf(
        DriverPermissionStatus(
            notificationsGranted = false,
            locationGranted = false,
            overlayGranted = false,
            backgroundLocationGranted = false,
            batteryUnrestricted = false
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fase 4.2: checagem de versão nova (silenciosa se falhar).
        com.example.platform.AppUpdateChecker.checkOnce(applicationContext)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        handleAcceptedOrderIntent(intent)
        handleViewOrderIntent(intent)
        refreshPermissionStatus()
        setContentView(ComposeView(this).apply {
            setContent {
            ItaSuperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ItaBackground
                ) {
                    when {
                        !ItaSuperSupabase.isConfigured -> {
                            BackendConfigurationScreen(
                                message = ItaSuperSupabase.configurationMessage,
                                onClose = ::finish
                            )
                        }
                        else -> {
                            // Permissões obrigatórias são cobradas DEPOIS do
                            // login (auditoria M7): o NavHost fica sempre
                            // montado (a SplashScreen roteia Login × Home
                            // sem destruir a navegação) e a tela de
                            // permissões entra como camada bloqueante por
                            // cima somente com sessão ativa e permissão
                            // faltando. Sem sessão, ninguém é travado antes
                            // de entrar/cadastrar.
                            val currentUser by AppContainer.authRepository.getCurrentUser()
                                .collectAsState(initial = null)
                            Box(modifier = Modifier.fillMaxSize()) {
                                AppNavHost()
                                if (currentUser != null && !permissionStatus.allGranted) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { }
                                    ) {
                                        DriverPermissionsScreen(
                                            status = permissionStatus,
                                            onRequestNotifications = ::requestNotificationPermission,
                                            onRequestLocation = ::requestLocationPermission,
                                            onRequestOverlay = ::openOverlaySettings,
                                            onContinue = { refreshPermissionStatus() },
                                            onRequestBackgroundLocation = ::requestBackgroundLocationPermission,
                                            onRequestBattery = ::requestBatteryUnrestricted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAcceptedOrderIntent(intent)
        handleViewOrderIntent(intent)
    }

    // Aceite vindo da sobreposição/tela de oferta: sinaliza o pedido para
    // a tela de pedidos forçar a atualização até a entrega aparecer na
    // rota ativa (trazer o app para a frente, sozinho, não atualizava).
    private fun handleAcceptedOrderIntent(intent: Intent?) {
        intent?.getStringExtra(EXTRA_ACCEPTED_ORDER_ID)
            ?.takeIf { it.isNotBlank() }
            ?.let { AppContainer.signalAcceptedOrder(it) }
    }

    // "Ver entrega" da sobreposição: sinaliza o pedido para a tela de
    // pedidos atualizar até a oferta aparecer — o aceite é dentro do app.
    private fun handleViewOrderIntent(intent: Intent?) {
        intent?.getStringExtra(EXTRA_VIEW_ORDER_ID)
            ?.takeIf { it.isNotBlank() }
            ?.let { AppContainer.signalViewOrder(it) }
    }

    override fun onResume() {
        super.onResume()
        AppContainer.availabilityRepository.onAppForeground()
        refreshPermissionStatus()
        // Alguns aparelhos One UI atualizam a permissão de sobreposição um pouco
        // depois do retorno das configurações do sistema.
        permissionRefreshHandler.postDelayed(::refreshPermissionStatus, 350)
    }

    override fun onPause() {
        AppContainer.availabilityRepository.onAppBackground()
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) refreshPermissionStatus()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refreshPermissionStatus()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST)
        } else {
            refreshPermissionStatus()
        }
    }

    private fun requestLocationPermission() {
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            LOCATION_PERMISSION_REQUEST
        )
    }

    // A tela de teste de overlay com dados de exemplo ("Loja de teste
    // ItaSuper", "R$ 42,90") foi removida de produção (auditoria M7): o
    // overlay real continua exercitado pelas chamadas de verdade.

    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } else {
            refreshPermissionStatus()
        }
    }

    // Etapa 2 da localização: o Android 11+ não concede "o tempo todo" por
    // diálogo — o motoboy escolhe na tela de permissões do app. Em Android
    // 10, a permissão pode ser pedida direto por requestPermissions.
    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= 30) {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                )
            )
        } else if (Build.VERSION.SDK_INT >= 29) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                BACKGROUND_LOCATION_PERMISSION_REQUEST
            )
        } else {
            refreshPermissionStatus()
        }
    }

    // Isenção de otimização de bateria: diálogo direto do sistema (1 toque).
    private fun requestBatteryUnrestricted() {
        val powerManager = getSystemService(PowerManager::class.java)
        if (powerManager?.isIgnoringBatteryOptimizations(packageName) == true) {
            refreshPermissionStatus()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
            )
        } else {
            refreshPermissionStatus()
        }
    }

    private fun refreshPermissionStatus() {
        permissionStatus = DriverPermissionStatus(
            notificationsGranted = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
            locationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED,
            overlayGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this),
            backgroundLocationGranted = Build.VERSION.SDK_INT < 29 ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED,
            batteryUnrestricted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(packageName) == true
        )
    }

    companion object {
        const val EXTRA_ACCEPTED_ORDER_ID = "extra_accepted_order_id"
        const val EXTRA_VIEW_ORDER_ID = "extra_view_order_id"
        const val NOTIFICATION_PERMISSION_REQUEST = 1104
        const val LOCATION_PERMISSION_REQUEST = 1105
        const val BACKGROUND_LOCATION_PERMISSION_REQUEST = 1106
    }
}
