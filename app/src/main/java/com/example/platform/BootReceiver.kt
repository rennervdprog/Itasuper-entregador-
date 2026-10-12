package com.example.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Reinicia o tracking GPS após o reboot do dispositivo, se houver uma entrega ativa persistida.
 * Garante que a localização nunca seja perdida por causa de reinicialização do celular.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        try {
            val prefs = context.getSharedPreferences("driver_tracking", Context.MODE_PRIVATE)
            val orderId = prefs.getString("active_order_id", null)

            if (orderId != null) {
                Log.i(TAG, "Boot concluído: reiniciando tracking para pedido $orderId")
                val serviceIntent = Intent(context, DriverTrackingService::class.java).apply {
                    putExtra("order_id", orderId)
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                Log.i(TAG, "Boot concluído: nenhum pedido ativo, tracking não reiniciado")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Falha ao reiniciar tracking no boot", t)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
