package com.example.platform

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.DeliveryOrder
import com.example.data.remote.ItaSuperSupabase
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "pending_driver_locations")
data class PendingDriverLocation(
    @androidx.room.PrimaryKey val orderId: String,
    val driverUserId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double?,
    val speed: Double?,
    val heading: Double?
)

@Dao
interface PendingDriverLocationDao {
    @Query("SELECT * FROM pending_driver_locations")
    suspend fun getAll(): List<PendingDriverLocation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(location: PendingDriverLocation)

    @Query("DELETE FROM pending_driver_locations WHERE orderId = :orderId")
    suspend fun delete(orderId: String)
}

@Database(entities = [PendingDriverLocation::class], version = 1, exportSchema = false)
abstract class DriverTrackingDatabase : RoomDatabase() {
    abstract fun locations(): PendingDriverLocationDao

    companion object {
        @Volatile private var instance: DriverTrackingDatabase? = null
        fun get(context: Context): DriverTrackingDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                DriverTrackingDatabase::class.java,
                "itasuper_driver_tracking.db"
            ).build().also { instance = it }
        }
    }
}

@Serializable
data class DriverLocationInsert(
    @SerialName("driver_user_id") val driverUserId: String,
    @SerialName("order_id") val orderId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double? = null,
    val speed: Double? = null,
    val heading: Double? = null,
    @SerialName("updated_at") val updatedAt: String = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }.format(java.util.Date())
)

class DriverTrackingService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedClient: com.google.android.gms.location.FusedLocationProviderClient
    private var activeOrderId: String? = null
    private var destinationLatitude: Double? = null
    private var destinationLongitude: Double? = null
    private var destinationAddress: String = ""
    private var activeShortCode: String = ""
    private var arrivalAlertSent = false

    private val prefs by lazy {
        getSharedPreferences(PREFS_TRACKING, MODE_PRIVATE)
    }

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val orderId = activeOrderId ?: return
            result.lastLocation?.let { location ->
                serviceScope.launch {
                    persistLocation(orderId, location)
                    detectArrival(orderId, location)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Restaura o order ID persistido (sobrevive a reinicializações do serviço)
        val persistedOrderId = prefs.getString(KEY_ACTIVE_ORDER_ID, null)
        val requestedOrderId = intent?.getStringExtra(EXTRA_ORDER_ID) ?: activeOrderId ?: persistedOrderId
        if (requestedOrderId != activeOrderId) arrivalAlertSent = false
        activeOrderId = requestedOrderId
        // Persiste para sobreviver a reinicializações
        if (requestedOrderId != null) {
            prefs.edit().putString(KEY_ACTIVE_ORDER_ID, requestedOrderId).apply()
        } else {
            prefs.edit().remove(KEY_ACTIVE_ORDER_ID).apply()
        }
        if (intent?.hasExtra(EXTRA_DESTINATION_LATITUDE) == true) {
            destinationLatitude = intent.getDoubleExtra(EXTRA_DESTINATION_LATITUDE, Double.NaN).takeIf { it.isFinite() }
            destinationLongitude = intent.getDoubleExtra(EXTRA_DESTINATION_LONGITUDE, Double.NaN).takeIf { it.isFinite() }
            destinationAddress = intent.getStringExtra(EXTRA_DESTINATION_ADDRESS).orEmpty()
            activeShortCode = intent.getStringExtra(EXTRA_ORDER_SHORT_CODE).orEmpty()
            // Persiste o destino JUNTO do pedido (auditoria M10): sem isso,
            // um restart do serviço (START_STICKY/boot) restaurava o pedido
            // mas perdia o destino, e o detectArrival nunca disparava.
            // Double vai como String p/ não perder precisão no SharedPreferences.
            prefs.edit()
                .putString(KEY_DESTINATION_LATITUDE, destinationLatitude?.toString())
                .putString(KEY_DESTINATION_LONGITUDE, destinationLongitude?.toString())
                .putString(KEY_DESTINATION_ADDRESS, destinationAddress)
                .putString(KEY_ORDER_SHORT_CODE, activeShortCode)
                .apply()
        } else if (requestedOrderId != null && requestedOrderId == persistedOrderId) {
            // Restart sem intent de destino: restaura o destino persistido
            // do MESMO pedido (pedido diferente = destino novo virá no start).
            destinationLatitude = prefs.getString(KEY_DESTINATION_LATITUDE, null)?.toDoubleOrNull()
            destinationLongitude = prefs.getString(KEY_DESTINATION_LONGITUDE, null)?.toDoubleOrNull()
            destinationAddress = prefs.getString(KEY_DESTINATION_ADDRESS, null).orEmpty()
            activeShortCode = prefs.getString(KEY_ORDER_SHORT_CODE, null).orEmpty()
        }
        startForeground(
            DriverNotificationHelper.TRACKING_NOTIFICATION_ID,
            DriverNotificationHelper.trackingNotification(this)
        )
        val hasPermission = hasLocationPermission()
        if (activeOrderId != null && hasPermission) {
            requestLocationUpdates()
        }
        // Tenta sincronizar o buffer offline ao iniciar
        serviceScope.launch {
            syncPendingLocations()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        fusedClient.removeLocationUpdates(callback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(15_000L)
            .setMinUpdateIntervalMillis(7_500L)
            .setMinUpdateDistanceMeters(20f)
            .setPriority(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY)
            .build()
        fusedClient.requestLocationUpdates(request, callback, mainLooper)
    }

    private suspend fun persistLocation(orderId: String, location: Location, attempt: Int = 1) {
        val userId = ItaSuperSupabase.client.auth.currentUserOrNull()?.id
        if (userId == null) {
            Log.e(TAG, "Usuário não autenticado - impossível enviar localização")
            return
        }
        val payload = DriverLocationInsert(
            driverUserId = userId,
            orderId = orderId,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy.toDouble(),
            speed = location.speed.toDouble(),
            heading = location.bearing.toDouble()
        )
        try {
            ItaSuperSupabase.client.from("driver_locations").upsert(payload) {
                onConflict = "driver_user_id"
            }
            // Após sucesso, tenta sincronizar o buffer offline
            syncPendingLocations()
        } catch (t: Throwable) {
            Log.e(TAG, "Falha ao enviar localização", t)
            // Salva no buffer offline
            try {
                DriverTrackingDatabase.get(this).locations().upsert(
                    PendingDriverLocation(
                        orderId = orderId,
                        driverUserId = userId,
                        latitude = payload.latitude,
                        longitude = payload.longitude,
                        accuracy = payload.accuracy,
                        speed = payload.speed,
                        heading = payload.heading
                    )
                )
            } catch (dbError: Throwable) {
                Log.e(TAG, "Falha ao salvar no buffer offline", dbError)
            }
            // Retry com backoff simples: 30s, máximo 3 tentativas
            if (attempt < MAX_RETRY_ATTEMPTS) {
                kotlinx.coroutines.delay(RETRY_DELAY_MS)
                persistLocation(orderId, location, attempt + 1)
            } else {
                Log.e(TAG, "Máximo de tentativas atingido. Localização ficou no buffer offline.")
            }
        }
    }

    /**
     * Sincroniza o buffer offline: envia cada localização pendente e remove as que tiveram sucesso.
     */
    private suspend fun syncPendingLocations() {
        try {
            val dao = DriverTrackingDatabase.get(this).locations()
            val pending = dao.getAll()
            if (pending.isEmpty()) {
                return
            }
            for (p in pending) {
                try {
                    ItaSuperSupabase.client.from("driver_locations").upsert(
                        DriverLocationInsert(
                            driverUserId = p.driverUserId,
                            orderId = p.orderId,
                            latitude = p.latitude,
                            longitude = p.longitude,
                            accuracy = p.accuracy,
                            speed = p.speed,
                            heading = p.heading
                        )
                    ) {
                        onConflict = "driver_user_id"
                    }
                    dao.delete(p.orderId)
                } catch (t: Throwable) {
                    Log.e(TAG, "Falha ao sincronizar localização pendente", t)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Erro ao sincronizar buffer offline", t)
        }
    }

    private fun detectArrival(orderId: String, location: Location) {
        if (arrivalAlertSent) return
        val latitude = destinationLatitude ?: return
        val longitude = destinationLongitude ?: return
        val destination = Location("itasuper-destination").apply {
            this.latitude = latitude
            this.longitude = longitude
        }
        // O raio se adapta à precisão do GPS, com mínimo de 80 m para evitar falso negativo urbano.
        val arrivalRadiusMeters = maxOf(80f, (location.accuracy.takeIf { it > 0f } ?: 0f) * 1.25f)
        if (location.distanceTo(destination) > arrivalRadiusMeters) return
        arrivalAlertSent = true
        DriverNotificationHelper.showArrivalAtDestination(
            context = applicationContext,
            orderId = orderId,
            shortCode = activeShortCode,
            destination = destinationAddress
        )
        Handler(Looper.getMainLooper()).post {
            DeliveryArrivalOverlay.show(
                context = applicationContext,
                shortCode = activeShortCode,
                destination = destinationAddress
            )
        }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    companion object {
        private const val TAG = "DriverTracking"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 30_000L

        private const val EXTRA_ORDER_ID = "order_id"
        private const val EXTRA_DESTINATION_LATITUDE = "destination_latitude"
        private const val EXTRA_DESTINATION_LONGITUDE = "destination_longitude"
        private const val EXTRA_DESTINATION_ADDRESS = "destination_address"
        private const val EXTRA_ORDER_SHORT_CODE = "order_short_code"
        private const val PREFS_TRACKING = "driver_tracking"
        private const val KEY_ACTIVE_ORDER_ID = "active_order_id"
        private const val KEY_DESTINATION_LATITUDE = "destination_latitude"
        private const val KEY_DESTINATION_LONGITUDE = "destination_longitude"
        private const val KEY_DESTINATION_ADDRESS = "destination_address"
        private const val KEY_ORDER_SHORT_CODE = "order_short_code"

        fun start(context: Context, order: DeliveryOrder) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, DriverTrackingService::class.java)
                    .putExtra(EXTRA_ORDER_ID, order.id)
                    .putExtra(EXTRA_DESTINATION_LATITUDE, order.destinationLatitude ?: Double.NaN)
                    .putExtra(EXTRA_DESTINATION_LONGITUDE, order.destinationLongitude ?: Double.NaN)
                    .putExtra(EXTRA_DESTINATION_ADDRESS, order.fullAddress)
                    .putExtra(EXTRA_ORDER_SHORT_CODE, order.shortCode)
            )
        }

        fun stop(context: Context) {
            // Limpa o pedido ativo E o destino persistido ao finalizar
            context.getSharedPreferences(PREFS_TRACKING, MODE_PRIVATE)
                .edit()
                .remove(KEY_ACTIVE_ORDER_ID)
                .remove(KEY_DESTINATION_LATITUDE)
                .remove(KEY_DESTINATION_LONGITUDE)
                .remove(KEY_DESTINATION_ADDRESS)
                .remove(KEY_ORDER_SHORT_CODE)
                .apply()
            context.stopService(Intent(context, DriverTrackingService::class.java))
        }
    }
}
