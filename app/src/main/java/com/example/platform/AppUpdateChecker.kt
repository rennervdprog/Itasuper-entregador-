package com.example.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/** Versão mais nova publicada, servida junto do APK no site (Fase 4.2). */
@Serializable
data class DriverAppVersionInfo(
    val version: String = "",
    val versionCode: Int = 0,
    val apkUrl: String = ""
)

/**
 * Aviso de versão nova (Fase 4.2): ao abrir, o app compara a própria
 * versão com a servida em /apps/entregador-version.json — o mesmo lugar
 * de onde sai o APK do /download. Sem resposta (sem rede, arquivo
 * ausente), fica em silêncio: checagem é conveniência, nunca bloqueio.
 * Quem dispensa uma versão não é incomodado de novo por ELA (a próxima
 * volta a avisar).
 */
object AppUpdateChecker {
    private const val VERSION_URL = "https://www.itasuper.com.br/apps/entregador-version.json"
    private const val PREFS = "app_update"
    private const val KEY_DISMISSED = "dismissed_code"

    private val _available = MutableStateFlow<DriverAppVersionInfo?>(null)
    val available: StateFlow<DriverAppVersionInfo?> = _available.asStateFlow()

    @Volatile
    private var checked = false

    fun checkOnce(context: Context) {
        if (checked) return
        checked = true
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                val connection = URL(VERSION_URL).openConnection() as HttpURLConnection
                connection.connectTimeout = 8_000
                connection.readTimeout = 8_000
                connection.setRequestProperty("Cache-Control", "no-cache")
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                val info = Json { ignoreUnknownKeys = true }
                    .decodeFromString<DriverAppVersionInfo>(text)
                val dismissed = appContext
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getInt(KEY_DISMISSED, 0)
                if (info.versionCode > installedVersionCode(appContext) &&
                    info.versionCode != dismissed
                ) {
                    _available.value = info
                }
            }
        }
    }

    fun dismiss(context: Context) {
        val info = _available.value ?: return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_DISMISSED, info.versionCode)
            .apply()
        _available.value = null
    }

    fun openDownload(context: Context, info: DriverAppVersionInfo) {
        val url = info.apkUrl.ifBlank { "https://www.itasuper.com.br/download" }
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun installedVersionCode(context: Context): Int = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        PackageInfoCompat.getLongVersionCode(info).toInt()
    } catch (e: Exception) {
        0
    }
}
