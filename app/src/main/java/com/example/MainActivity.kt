package com.example

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.data.model.IncomingOrderAlert
import com.example.platform.IncomingOrderOverlay
import com.example.ui.navigation.AppNavHost
import com.example.ui.onboarding.DriverPermissionStatus
import com.example.ui.onboarding.DriverPermissionsScreen
import com.example.ui.theme.ItaBackground
import com.example.ui.theme.ItaSuperTheme

class MainActivity : ComponentActivity() {
    private val permissionRefreshHandler = Handler(Looper.getMainLooper())
    private var permissionStatus by mutableStateOf(
        DriverPermissionStatus(
            notificationsGranted = false,
            locationGranted = false,
            overlayGranted = false
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        refreshPermissionStatus()
        setContent {
            ItaSuperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ItaBackground
                ) {
                    if (permissionStatus.allGranted) {
                        AppNavHost()
                    } else {
                        DriverPermissionsScreen(
                            status = permissionStatus,
                            onRequestNotifications = ::requestNotificationPermission,
                            onRequestLocation = ::requestLocationPermission,
                            onRequestOverlay = ::openOverlaySettings,
                            onTestOverlay = ::showOverlayTest,
                            onContinue = { refreshPermissionStatus() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStatus()
        // Alguns aparelhos One UI atualizam a permissão de sobreposição um pouco
        // depois do retorno das configurações do sistema.
        permissionRefreshHandler.postDelayed(::refreshPermissionStatus, 350)
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

    private fun showOverlayTest() {
        val shown = IncomingOrderOverlay.show(
            context = applicationContext,
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
            Toast.makeText(
                this,
                IncomingOrderOverlay.lastDiagnostic(),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
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
            overlayGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
        )
    }

    private companion object {
        const val NOTIFICATION_PERMISSION_REQUEST = 1104
        const val LOCATION_PERMISSION_REQUEST = 1105
    }
}
