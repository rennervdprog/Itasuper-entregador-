package com.example

import android.app.ActivityManager
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

        if (isTrackingServiceRunning()) {
            Log.i(TAG, "Tracking já está rodando para $orderId")
            return
        }

        Log.i(TAG, "Reiniciando tracking para pedido ativo $orderId")
        val intent = Intent(this, DriverTrackingService::class.java).apply {
            putExtra("order_id", orderId)
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun isTrackingServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        return manager.getRunningServices(Integer.MAX_VALUE).any {
            it.service.className == DriverTrackingService::class.java.name
        }
    }

    companion object {
        private const val TAG = "ItaSuperApplication"
        lateinit var appContext: android.content.Context
            private set
    }
}
