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
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        try {
            val prefs = context.getSharedPreferences("driver_tracking", Context.MODE_PRIVATE)
            val orderId = prefs.getString("active_order_id", null)

            if (orderId != null) {
                Log.i(TAG, "Reiniciando tracking para pedido $orderId")
                val serviceIntent = Intent(context, DriverTrackingService::class.java).apply {
                    putExtra("order_id", orderId)
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            }

            // Presença: se o motoboy estava Online (intenção salva), religa o
            // serviço de presença. Sem a permissão "o tempo todo", o Android
            // pode recusar o início em segundo plano — nesse caso, o serviço
            // volta sozinho na próxima abertura do app.
            if (DriverPresenceService.shouldBeRunning(context)) {
                runCatching {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, DriverPresenceService::class.java)
                    )
                    Log.i(TAG, "Presença online religada após boot/atualização")
                }.onFailure {
                    Log.w(TAG, "Não foi possível religar a presença agora", it)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Falha ao reiniciar serviços no boot", t)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
