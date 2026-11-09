package com.example.platform

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * "Saúde do rastreio": o checklist que decide se a loja vai enxergar o
 * motoboy no mapa enquanto ele estiver Online. Cada item pendente sabe
 * abrir a tela certa do sistema — inclusive os atalhos por fabricante
 * (autostart da Xiaomi, "nunca dormir" da Samsung), que nenhum código
 * consegue ligar sozinho.
 */
data class PresenceHealthState(
    val locationForeground: Boolean,
    val locationBackground: Boolean,
    val batteryUnrestricted: Boolean,
    val notificationsGranted: Boolean,
    val manufacturerHint: String?,
    val manufacturerIntent: Intent?,
    // Fase 4.1: a sobreposição virou recomendada. Enquanto não concedida,
    // o app roda com "alertas reduzidos" (notificação em vez da tela
    // cheia por cima dos outros apps) — e este cartão é o aviso disso.
    val overlayGranted: Boolean = false
) {
    /** Tudo certo p/ o mapa ao vivo funcionar o turno inteiro. */
    val allOk: Boolean
        get() = locationForeground && locationBackground && batteryUnrestricted
}

object PresenceHealthChecker {

    fun snapshot(context: Context): PresenceHealthState {
        val foreground = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        val background = Build.VERSION.SDK_INT < 29 ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        val battery = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            context.getSystemService(PowerManager::class.java)
                ?.isIgnoringBatteryOptimizations(context.packageName) == true
        val notifications = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        val overlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            Settings.canDrawOverlays(context)
        val (hint, intent) = manufacturerStep(context)
        return PresenceHealthState(
            locationForeground = foreground,
            locationBackground = background,
            batteryUnrestricted = battery,
            notificationsGranted = notifications,
            manufacturerHint = hint,
            manufacturerIntent = intent,
            overlayGranted = overlay
        )
    }

    /** Tela do sistema para permitir "exibir sobre outros apps". */
    fun overlayIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Tela de permissões do app (onde se escolhe "Permitir o tempo todo"). */
    fun appDetailsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Diálogo direto de isenção de bateria (1 toque). */
    fun batteryIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun manufacturerStep(context: Context): Pair<String?, Intent?> {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> {
                val intent = Intent().setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val resolved = runCatching {
                    context.packageManager.resolveActivity(intent, 0) != null
                }.getOrDefault(false)
                "Xiaomi: ligue o início automático do ItaSuper Entregador e deixe a bateria dele em \"Sem restrições\"" to
                    (if (resolved) intent else appDetailsIntent(context))
            }
            manufacturer.contains("samsung") -> {
                "Samsung: em Bateria, deixe o ItaSuper Entregador como \"Sem restrição\" e fora dos apps que dormem" to
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                "Huawei: em Inicialização de apps, deixe o ItaSuper Entregador em gerenciamento manual com os 3 itens ligados" to
                    appDetailsIntent(context)
            }
            manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("vivo") -> {
                "Neste aparelho, permita a inicialização automática do ItaSuper Entregador nas configurações de bateria" to
                    appDetailsIntent(context)
            }
            else -> null to null
        }
    }
}
