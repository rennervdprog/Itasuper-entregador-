package com.example.platform

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Vigia da presença: a cada 15 min (mínimo do WorkManager) confere se o
 * motoboy está com a intenção Online ligada mas sem posição fresca — e tenta
 * religar o serviço de presença e/ou renovar o heartbeat.
 *
 * É rede de segurança, não garantia: em ROMs agressivas o próprio
 * WorkManager pode ser adiado. Sem a permissão "o tempo todo", religar o
 * FGS em segundo plano pode ser recusado pelo Android — nesse caso o
 * worker desiste em silêncio e o serviço volta na próxima abertura do app.
 */
object TrackingWatchdog {
    private const val WORK_NAME = "tracking_watchdog"

    fun schedule(context: Context) {
        runCatching {
            val request = PeriodicWorkRequestBuilder<TrackingWatchdogWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }.onFailure { Log.w("TrackingWatchdog", "Falha ao agendar vigia", it) }
    }

    fun cancel(context: Context) {
        runCatching { WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME) }
    }
}

class TrackingWatchdogWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (!DriverPresenceService.shouldBeRunning(context)) {
            return Result.success()
        }
        val lastPublish = DriverLocationPublisher.lastPublishedAtMs
        val fresh = lastPublish > 0L &&
            System.currentTimeMillis() - lastPublish < STALE_AFTER_MS
        if (!fresh) {
            runCatching {
                PresenceControl.start(context)
                Log.i(TAG, "Vigia religou a presença (posição velha ou serviço morto)")
            }.onFailure { Log.w(TAG, "Vigia não conseguiu religar a presença", it) }
        }
        // Presença no backend renovada mesmo se o processo principal morreu.
        DriverPresenceService.heartbeatFromBackground(context)
        return Result.success()
    }

    private companion object {
        const val TAG = "TrackingWatchdog"
        const val STALE_AFTER_MS = 5 * 60_000L
    }
}
