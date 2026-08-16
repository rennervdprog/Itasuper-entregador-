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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

@Entity(tableName = "offline_delivery_confirmations")
data class OfflineDeliveryConfirmationEntity(
    @androidx.room.PrimaryKey val orderId: String,
    val shortCode: String,
    val pin: String,
    val attemptedAt: String,
    val deviceId: String,
    val retries: Int = 0
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

    @Query("UPDATE offline_delivery_confirmations SET retries = :retries WHERE orderId = :orderId")
    suspend fun updateRetries(orderId: String, retries: Int)

    @Query("DELETE FROM offline_delivery_confirmations")
    suspend fun clear()
}

@Database(entities = [OfflineDeliveryConfirmationEntity::class], version = 1, exportSchema = false)
abstract class OfflineDeliveryDatabase : RoomDatabase() {
    abstract fun confirmations(): OfflineDeliveryConfirmationDao

    companion object {
        @Volatile private var instance: OfflineDeliveryDatabase? = null

        fun get(context: Context): OfflineDeliveryDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                OfflineDeliveryDatabase::class.java,
                "itasuper_driver_offline.db"
            ).build().also { instance = it }
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
                isSynced = false
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

    suspend fun clear() = dao.clear()

    companion object {
        private const val UNIQUE_WORK = "itasuper_offline_delivery_sync"

        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<OfflineDeliverySyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
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
        entries.forEach { item ->
            val result = runCatching {
                ItaSuperSupabase.client.postgrest.rpc(
                    "driver_finish_delivery_offline",
                    buildJsonObject {
                        put("_order_id", item.orderId)
                        put("_pin", item.pin)
                        put("_attempted_at", item.attemptedAt)
                        put("_device_id", item.deviceId)
                    }
                ).data
            }

            if (result.isSuccess) {
                // A RPC é idempotente; sucesso também cobre uma entrega já concluída.
                dao.delete(item.orderId)
            } else if (item.retries >= 20) {
                dao.delete(item.orderId)
            } else {
                dao.updateRetries(item.orderId, item.retries + 1)
            }
        }
        return Result.success()
    }
}
