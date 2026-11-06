package com.example.platform

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.ItaSuperApplication
import com.example.data.remote.ItaSuperSupabase
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ponto único de controle do serviço de presença: o repositório de
 * disponibilidade (toggle Online/Offline e logout) chama daqui.
 */
object PresenceControl {
    fun start(context: Context) {
        runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, DriverPresenceService::class.java)
            )
        }.onFailure { Log.w("PresenceControl", "Falha ao iniciar presença", it) }
        TrackingWatchdog.schedule(context)
    }

    fun stop(context: Context) {
        runCatching { context.stopService(Intent(context, DriverPresenceService::class.java)) }
        TrackingWatchdog.cancel(context)
    }
}

/**
 * Presença online do motoboy: publica a posição em driver_locations enquanto
 * ele estiver ONLINE — não só durante entregas — para a aba
 * "Onde Estão Meus Motoboys" do painel da loja.
 *
 * Desenho (estudo 2026-11-05):
 * - Serviço SEPARADO do DriverTrackingService (entrega), que segue intocado.
 *   Em entrega, este serviço PAUSA: o de entrega é dono da cadência fina.
 * - Cadência adaptativa: em movimento 15s/25m em alta precisão; parado,
 *   precisão equilibrada publicando 1x a cada 2 min (economia de bateria).
 * - Nasce e morre com o toggle Online; logout também encerra (prepareForLogout).
 * - Sem localização "o tempo todo", o Android pode recusar o restart em
 *   segundo plano (SecurityException): capturado e registrado, sem crash —
 *   o app volta ao modo de hoje (posição só com o app aberto/entrega).
 */
class DriverPresenceService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedClient: com.google.android.gms.location.FusedLocationProviderClient

    @Volatile private var movingMode: Boolean? = null
    @Volatile private var lastPublishedLoc: Location? = null
    @Volatile private var lastPublishedAtMs: Long = 0L
    @Volatile private var stillSinceMs: Long = 0L

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.locations.maxByOrNull { it.time } ?: return
            val now = System.currentTimeMillis()
            // Em entrega, o DriverTrackingService publica — presença cede.
            val trackingPrefs = getSharedPreferences(PREFS_TRACKING, MODE_PRIVATE)
            if (trackingPrefs.getString(KEY_ACTIVE_ORDER_ID, null) != null) return

            val last = lastPublishedLoc
            val displacement = if (last != null) location.distanceTo(last) else Float.MAX_VALUE
            val speed = if (location.hasSpeed()) location.speed else 0f
            val isMoving = speed > MOVING_SPEED_MPS || displacement >= MOVING_DISPLACEMENT_M

            if (isMoving) {
                stillSinceMs = 0L
                if (movingMode != true) {
                    movingMode = true
                    requestUpdates(moving = true)
                }
                if (last == null || displacement >= MOVING_DISPLACEMENT_M || now - lastPublishedAtMs >= MOVING_MIN_INTERVAL_MS) {
                    publish(location)
                }
            } else {
                if (stillSinceMs == 0L) stillSinceMs = now
                if (now - stillSinceMs >= STILL_AFTER_MS && movingMode != false) {
                    movingMode = false
                    requestUpdates(moving = false)
                }
                if (last == null || now - lastPublishedAtMs >= STILL_PUBLISH_INTERVAL_MS) {
                    publish(location)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_GO_OFFLINE) {
            serviceScope.launch { goOffline() }
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            ServiceCompat.startForeground(
                this,
                DriverNotificationHelper.PRESENCE_NOTIFICATION_ID,
                DriverNotificationHelper.presenceNotification(this),
                if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
            )
        } catch (t: Throwable) {
            // Sem "Permitir o tempo todo", o Android 14+ recusa FGS de
            // localização iniciado em segundo plano. Não é crash: o serviço
            // volta na próxima abertura do app (intent online persistida).
            Log.w(TAG, "startForeground recusado (permissão de fundo?)", t)
            stopSelf()
            return START_NOT_STICKY
        }
        if (hasLocationPermission()) {
            movingMode = null
            stillSinceMs = 0L
            requestUpdates(moving = true)
        } else {
            Log.w(TAG, "Sem permissão de localização — presença não publica")
        }
        serviceScope.launch { DriverLocationPublisher.syncPending(this@DriverPresenceService) }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // App saiu dos Recentes: o serviço continua (stopWithTask=false) e o
        // watchdog garante a volta se o sistema matar o processo.
        TrackingWatchdog.schedule(this)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        runCatching { fusedClient.removeLocationUpdates(callback) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** Ação "Ficar offline" da notificação permanente. */
    private suspend fun goOffline() {
        runCatching {
            val client = ItaSuperSupabase.client
            val userId = client.auth.currentUserOrNull()?.id ?: return@runCatching
            getSharedPreferences(PREFS_PRESENCE, MODE_PRIVATE)
                .edit().putBoolean(intentKey(userId), false).apply()
            client.from("drivers").update({ set("is_online", false) }) {
                filter { eq("user_id", userId) }
            }
            runCatching { client.postgrest.rpc("driver_presence_heartbeat") }
        }.onFailure { Log.w(TAG, "Falha ao ficar offline pela notificação", it) }
        PresenceControl.stop(this)
    }

    private fun publish(location: Location) {
        lastPublishedLoc = location
        lastPublishedAtMs = System.currentTimeMillis()
        serviceScope.launch {
            DriverLocationPublisher.publish(this@DriverPresenceService, null, location)
        }
    }

    private fun requestUpdates(moving: Boolean) {
        if (!hasLocationPermission()) return
        val request = if (moving) {
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, MOVING_INTERVAL_MS)
                .setMinUpdateIntervalMillis(MOVING_MIN_INTERVAL_MS)
                .setMinUpdateDistanceMeters(0f)
                .build()
        } else {
            LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, STILL_REQUEST_INTERVAL_MS)
                .setMinUpdateIntervalMillis(STILL_REQUEST_INTERVAL_MS)
                .setMinUpdateDistanceMeters(0f)
                .setMaxUpdateDelayMillis(STILL_PUBLISH_INTERVAL_MS)
                .build()
        }
        runCatching {
            fusedClient.removeLocationUpdates(callback)
            fusedClient.requestLocationUpdates(request, callback, mainLooper)
        }.onFailure { Log.w(TAG, "Falha ao pedir updates de localização", it) }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    companion object {
        private const val TAG = "DriverPresence"
        const val ACTION_GO_OFFLINE = "com.example.action.GO_OFFLINE"

        private const val PREFS_TRACKING = "driver_tracking"
        private const val KEY_ACTIVE_ORDER_ID = "active_order_id"
        private const val PREFS_PRESENCE = "itasuper_driver_presence"

        private const val MOVING_INTERVAL_MS = 15_000L
        private const val MOVING_MIN_INTERVAL_MS = 15_000L
        private const val MOVING_DISPLACEMENT_M = 25f
        private const val MOVING_SPEED_MPS = 2.0f
        private const val STILL_AFTER_MS = 150_000L
        private const val STILL_REQUEST_INTERVAL_MS = 60_000L
        private const val STILL_PUBLISH_INTERVAL_MS = 120_000L

        private fun intentKey(userId: String) = "online_intent_$userId"

        /**
         * A intenção Online sobrevive ao processo? Lê QUALQUER chave
         * online_intent_* ligada (o BootReceiver não conhece o usuário).
         */
        fun anyOnlineIntent(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_PRESENCE, MODE_PRIVATE)
            return prefs.all.any { (key, value) -> key.startsWith("online_intent_") && value == true }
        }

        fun shouldBeRunning(context: Context): Boolean = anyOnlineIntent(context)

        /** Heartbeat de presença a partir de contexto sem UI (watchdog). */
        suspend fun heartbeatFromBackground(context: Context) {
            runCatching {
                ItaSuperSupabase.client.postgrest.rpc("driver_presence_heartbeat")
            }
            delay(200)
        }
    }
}
