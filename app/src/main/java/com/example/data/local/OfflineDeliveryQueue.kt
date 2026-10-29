package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.remote.ItaSuperSupabase
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.concurrent.TimeUnit

@Entity(tableName = "offline_delivery_confirmations")
data class OfflineDeliveryConfirmationEntity(
    @androidx.room.PrimaryKey val orderId: String,
    val shortCode: String,
    val pin: String,
    val attemptedAt: String,
    val deviceId: String,
    val retries: Int = 0,
    /** Último erro da sincronização (código devolvido pelo servidor, ex.:
     * "invalid_pin", ou "network_error"); nulo enquanto nunca falhou.
     * Fica persistido p/ o erro ser visível ao entregador — nunca descartar
     * a tentativa em silêncio (auditoria C2). */
    val lastError: String? = null
)

@Dao
interface OfflineDeliveryConfirmationDao {
    @Query("SELECT * FROM offline_delivery_confirmations ORDER BY attemptedAt ASC")
    fun observeAll(): Flow<List<OfflineDeliveryConfirmationEntity>>

    @Query("SELECT * FROM offline_delivery_confirmations ORDER BY attemptedAt ASC")
    suspend fun getAll(): List<OfflineDeliveryConfirmationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OfflineDeliveryConfirmationEntity)

    @Query("DELETE FROM offline_delivery_confirmations WHERE orderId = :orderId")
    suspend fun delete(orderId: String)

    @Query("UPDATE offline_delivery_confirmations SET retries = :retries, lastError = :lastError WHERE orderId = :orderId")
    suspend fun updateRetriesAndError(orderId: String, retries: Int, lastError: String?)

    @Query("DELETE FROM offline_delivery_confirmations")
    suspend fun clear()
}

@Database(entities = [OfflineDeliveryConfirmationEntity::class], version = 2, exportSchema = false)
abstract class OfflineDeliveryDatabase : RoomDatabase() {
    abstract fun confirmations(): OfflineDeliveryConfirmationDao

    companion object {
        /** v2: coluna lastError p/ registrar a recusa do servidor (C2). */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE offline_delivery_confirmations ADD COLUMN lastError TEXT")
            }
        }

        @Volatile private var instance: OfflineDeliveryDatabase? = null

        fun get(context: Context): OfflineDeliveryDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                OfflineDeliveryDatabase::class.java,
                "itasuper_driver_offline.db"
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}

class OfflineDeliveryQueue(private val context: Context) {
    private val dao = OfflineDeliveryDatabase.get(context).confirmations()

    fun observe(): Flow<List<com.example.data.model.OfflineDeliveryConfirmation>> = dao.observeAll().map { entries ->
        entries.map {
            com.example.data.model.OfflineDeliveryConfirmation(
                orderId = it.orderId,
                shortCode = it.shortCode,
                pin = it.pin,
                timestamp = runCatching { Instant.parse(it.attemptedAt).toEpochMilli() }.getOrDefault(0L),
                isSynced = false,
                lastError = it.lastError
            )
        }
    }

    suspend fun enqueue(orderId: String, shortCode: String, pin: String, deviceId: String) {
        dao.upsert(
            OfflineDeliveryConfirmationEntity(
                orderId = orderId,
                shortCode = shortCode,
                pin = pin,
                attemptedAt = Instant.now().toString(),
                deviceId = deviceId
            )
        )
        schedule(context)
    }

    suspend fun clear() {
        dao.clear()
        // Sem pendências, o worker agendado também é cancelado: ele não
        // deve acordar depois (ex.: após logout) sob outra sessão.
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
    }

    companion object {
        private const val UNIQUE_WORK = "itasuper_offline_delivery_sync"

        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<OfflineDeliverySyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                // Result.retry() do worker reagenda com espera crescente.
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}

class OfflineDeliverySyncWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val dao = OfflineDeliveryDatabase.get(applicationContext).confirmations()
        val entries = dao.getAll()
        var hasFailureToRetry = false
        entries.forEach { item ->
            val responseBody = runCatching {
                ItaSuperSupabase.client.postgrest.rpc(
                    "driver_finish_delivery_offline",
                    buildJsonObject {
                        put("_order_id", item.orderId)
                        put("_pin", item.pin)
                        put("_attempted_at", item.attemptedAt)
                        put("_device_id", item.deviceId)
                    }
                ).data
            }.getOrNull()

            if (responseBody == null) {
                // Falha de transporte (sem rede/servidor inacessível): a
                // tentativa continua na fila, com o erro registrado, e o
                // WorkManager tenta de novo com backoff.
                dao.updateRetriesAndError(item.orderId, item.retries + 1, ERROR_NETWORK)
                if (item.retries + 1 < MAX_RETRIES) hasFailureToRetry = true
                return@forEach
            }

            // A RPC NÃO levanta exceção quando recusa a confirmação: ela
            // devolve {"success": false, "error": "..."} com retorno normal.
            // Só apague da fila com success = true explícito — antes, a
            // recusa era tratada como entregue e sumia em silêncio (C2).
            val payload = runCatching { Json.parseToJsonElement(responseBody).jsonObject }.getOrNull()
            val succeeded = (payload?.get("success") as? JsonPrimitive)?.booleanOrNull == true
            if (succeeded) {
                // A RPC é idempotente; sucesso também cobre uma entrega já concluída.
                // Apagar a linha apaga junto o PIN persistido: o código do
                // cliente não permanece no aparelho depois que a entrega sai
                // da fila (auditoria A3). O mesmo vale p/ o clear() da fila.
                dao.delete(item.orderId)
            } else {
                val serverError = (payload?.get("error") as? JsonPrimitive)?.contentOrNull
                    ?: ERROR_UNKNOWN_RESPONSE
                dao.updateRetriesAndError(item.orderId, item.retries + 1, serverError)
                if (item.retries + 1 < MAX_RETRIES) hasFailureToRetry = true
            }
        }
        // Ao atingir o teto de tentativas, a linha PERMANECE na fila com o
        // erro registrado (visível no app) em vez de ser apagada em silêncio.
        return if (hasFailureToRetry) Result.retry() else Result.success()
    }

    private companion object {
        const val MAX_RETRIES = 20
        const val ERROR_NETWORK = "network_error"
        const val ERROR_UNKNOWN_RESPONSE = "unknown_error"
    }
}
