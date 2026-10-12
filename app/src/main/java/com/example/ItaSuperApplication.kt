package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.platform.DriverNotificationHelper
import com.example.platform.DriverTrackingService

class ItaSuperApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        runCatching { DriverNotificationHelper.createChannels(this) }
        // Se há entrega ativa persistida mas o serviço não está rodando, reinicia o tracking.
        // Cobre: update do app, serviço morto pelo sistema sem START_STICKY funcionar, etc.
        runCatching { restartTrackingIfNeeded() }
    }

    private fun restartTrackingIfNeeded() {
        val prefs = getSharedPreferences("driver_tracking", Context.MODE_PRIVATE)
        val orderId = prefs.getString("active_order_id", null) ?: return

        // Sempre tenta iniciar - se já estiver rodando, o onStartCommand só é chamado de novo (inofensivo)
        Log.i(TAG, "Garantindo tracking ativo para pedido $orderId")
        val intent = Intent(this, DriverTrackingService::class.java).apply {
            putExtra("order_id", orderId)
        }
        runCatching {
            ContextCompat.startForegroundService(this, intent)
        }.onFailure {
            Log.e(TAG, "Falha ao reiniciar tracking", it)
        }
    }

    companion object {
        private const val TAG = "ItaSuperApplication"
        lateinit var appContext: android.content.Context
            private set
    }
}
