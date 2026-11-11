package com.example.platform

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.IncomingOrderAlert
import com.example.ui.orders.IncomingOrderActivity
import kotlin.math.absoluteValue

object DriverNotificationHelper {
    const val CHANNEL_ORDERS = "driver_orders"
    const val CHANNEL_ORDER_ALERTS = "driver_order_alerts_v3"
    // Canais antigos do alerta. Canal de notificação é imutável depois de
    // criado, então cada troca de som entra num canal novo e os antigos
    // são apagados: v1 = alarme do aparelho (Fase 2), v2 = primeiro som
    // próprio do ItaSuper (2.5.6).
    private const val CHANNEL_ORDER_ALERTS_LEGACY = "driver_order_alerts_v2"
    private const val CHANNEL_ORDER_ALERTS_LEGACY_V1 = "driver_order_alerts"
    const val CHANNEL_TRACKING = "driver_tracking"
    private const val ORDER_NOTIFICATION_ID = 2001
    const val TRACKING_NOTIFICATION_ID = 2002

    // Padrão do alerta de pedido novo (Fase 2): vibra-pausa-vibra-pausa-
    // vibra longo. O MESMO padrão vale pro canal de notificação e pra
    // sobreposição em tela cheia, pra experiência ser uma só.
    private val ALERT_VIBRATION_PATTERN = longArrayOf(0, 400, 200, 400, 200, 700)

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ORDERS,
                "Pedidos do entregador",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Alertas de novos pedidos e atualizações de entrega" }
        )
        // Canal DEDICADO do alerta de pedido novo (Fase 2): vibração
        // própria e o som próprio do ItaSuper (2.5.6; trocado pela v4
        // "estilo 99" na 2.5.7 — os canais antigos são apagados logo abaixo).
        runCatching { manager.deleteNotificationChannel(CHANNEL_ORDER_ALERTS_LEGACY) }
        runCatching { manager.deleteNotificationChannel(CHANNEL_ORDER_ALERTS_LEGACY_V1) }
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ORDER_ALERTS,
                "Alerta de novo pedido",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Som e vibração do aviso de nova entrega disponível"
                setSound(
                    newOrderSoundUri(context),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                enableVibration(true)
                vibrationPattern = ALERT_VIBRATION_PATTERN
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TRACKING,
                "Rastreamento ativo",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Indica que a localização da rota está sendo compartilhada" }
        )
    }

    fun showNewOrder(context: Context, orderCode: String) {
        if (!canNotify(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ORDERS)
            .setSmallIcon(R.drawable.ic_stat_driver_helmet)
            .setContentTitle("Novo pedido disponível")
            .setContentText("O pedido $orderCode está pronto para entrega.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(ORDER_NOTIFICATION_ID, notification)
    }

    // Guarda do som de pedido novo (correção da duplicação): UM único
    // aviso sonoro por pedido numa janela de 60 s, não importa o caminho
    // (push FCM, Realtime ou sobreposição). Quem reivindica primeiro toca;
    // os demais caminhos do MESMO pedido são suprimidos. Sem isso, o FCM
    // tocava o som do canal + o ringtone da sobreposição juntos, e o
    // Realtime postava a mesma notificação de novo segundos depois.
    private const val NEW_ORDER_ALERT_WINDOW_MS = 60_000L
    private val newOrderAlertLock = Any()
    private var lastNewOrderAlertOrderId: String? = null
    private var lastNewOrderAlertAtMs: Long = 0L

    private fun claimNewOrderAlert(orderId: String): Boolean {
        if (orderId.isBlank()) return true
        val now = System.currentTimeMillis()
        synchronized(newOrderAlertLock) {
            val sameOrder = lastNewOrderAlertOrderId == orderId
            if (sameOrder && now - lastNewOrderAlertAtMs < NEW_ORDER_ALERT_WINDOW_MS) {
                return false
            }
            lastNewOrderAlertOrderId = orderId
            lastNewOrderAlertAtMs = now
            return true
        }
    }

    fun showIncomingOrder(
        context: Context,
        orderId: String,
        storeName: String,
        neighborhood: String,
        shortCode: String,
        alert: IncomingOrderAlert? = null
    ) {
        if (!canNotify(context) || orderId.isBlank()) return
        // Caminho duplicado do mesmo pedido (FCM x Realtime): a primeira
        // postagem já soou pelo canal e continua na bandeja com o mesmo
        // id de notificação — postar de novo só repetiria o som.
        if (!claimNewOrderAlert(orderId)) return

        val launchIntent = IncomingOrderActivity.intent(
            context = context,
            orderId = orderId,
            storeName = storeName,
            neighborhood = neighborhood,
            shortCode = shortCode,
            pickupAddress = alert?.pickupAddress.orEmpty(),
            destinationAddress = alert?.destinationAddress.orEmpty(),
            itemCount = alert?.itemCount ?: 0,
            paymentMethod = alert?.paymentMethod.orEmpty(),
            totalLabel = alert?.totalLabel.orEmpty()
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val contentIntent = PendingIntent.getActivity(
            context,
            orderId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ORDER_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_driver_helmet)
            .setContentTitle("Nova entrega disponível")
            .setContentText("$storeName • ${alert?.destinationAddress?.ifBlank { neighborhood } ?: neighborhood}")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    listOfNotNull(
                        "$shortCode • $storeName",
                        "Destino: ${alert?.destinationAddress?.ifBlank { neighborhood } ?: neighborhood}",
                        alert?.itemCount?.takeIf { it > 0 }?.let { "$it ${if (it == 1) "item" else "itens"}" },
                        alert?.paymentMethod?.takeIf { it.isNotBlank() },
                        alert?.totalLabel?.takeIf { it.isNotBlank() }
                    ).joinToString("\n")
                )
            )
            .setContentIntent(contentIntent)
            // Fase 2: com a tela apagada, a notificação acorda o aparelho
            // já na tela da oferta (o aceite continua dentro do app).
            .setFullScreenIntent(contentIntent, true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)

        context.getSystemService(NotificationManager::class.java)
            .notify(ORDER_NOTIFICATION_ID + (orderId.hashCode().absoluteValue % 10_000), builder.build())
    }

    fun showArrivalAtDestination(context: Context, orderId: String, shortCode: String, destination: String) {
        if (!canNotify(context)) return
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ?: return
        val contentIntent = PendingIntent.getActivity(
            context,
            ("arrival-" + orderId).hashCode(),
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ORDERS)
            .setSmallIcon(R.drawable.ic_stat_driver_helmet)
            .setContentTitle("Você chegou ao destino")
            .setContentText("$shortCode • Toque para voltar ao ItaSuper e confirmar a entrega.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "$shortCode\nDestino: $destination\nVolte ao ItaSuper para confirmar a entrega com o PIN do cliente."
            ))
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(ORDER_NOTIFICATION_ID + 20_000 + (orderId.hashCode().absoluteValue % 10_000), notification)
    }

    fun showRemotePush(context: Context, title: String, body: String) {
        if (!canNotify(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ORDERS)
            .setSmallIcon(R.drawable.ic_stat_driver_helmet)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(ORDER_NOTIFICATION_ID + 1, notification)
    }

    /** URI do som próprio do ItaSuper (res/raw/itasuper_new_order.ogg). */
    private fun newOrderSoundUri(context: Context): Uri =
        Uri.parse("android.resource://${context.packageName}/${R.raw.itasuper_new_order}")

    private var activeRingtone: Ringtone? = null

    /**
     * Chama a atenção na hora (Fase 2): vibração no padrão do alerta +
     * som próprio do ItaSuper (2.5.6) por alguns segundos. Usado pela
     * sobreposição em tela cheia, que não passa por canal de
     * notificação — sem isso, o aviso era só visual.
     */
    fun alertAttention(context: Context, orderId: String? = null) {
        // Sobreposição do MESMO pedido cuja notificação do canal já soou
        // (o chamador do push posta a notificação antes de abrir a
        // sobreposição): não tocar o ringtone nem vibrar de novo — o som
        // do canal é o aviso único. Se ninguém reivindicou o pedido
        // (ex.: notificações sem permissão), a sobreposição reivindica e
        // toca como reserva.
        if (!orderId.isNullOrBlank() && !claimNewOrderAlert(orderId)) return
        runCatching {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(ALERT_VIBRATION_PATTERN, -1)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(ALERT_VIBRATION_PATTERN, -1)
            }
        }
        runCatching {
            activeRingtone?.stop()
            val ringtone = RingtoneManager.getRingtone(
                context.applicationContext,
                newOrderSoundUri(context)
            )
            activeRingtone = ringtone
            ringtone.play()
            Handler(Looper.getMainLooper()).postDelayed({
                runCatching { ringtone.stop() }
                if (activeRingtone === ringtone) activeRingtone = null
            }, 4_500)
        }
    }

    /**
     * Aviso de evento de um pedido específico (Fase 2): cancelamento,
     * confirmação do cliente etc. Id próprio por pedido, pra um aviso
     * não apagar o outro na bandeja.
     */
    fun showOrderEvent(context: Context, orderId: String, title: String, body: String) {
        if (!canNotify(context) || orderId.isBlank()) return
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val contentIntent = launch?.let {
            PendingIntent.getActivity(
                context,
                ("event-" + orderId).hashCode(),
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_ORDERS)
            .setSmallIcon(R.drawable.ic_stat_driver_helmet)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        if (contentIntent != null) builder.setContentIntent(contentIntent)
        context.getSystemService(NotificationManager::class.java)
            .notify(ORDER_NOTIFICATION_ID + 30_000 + (orderId.hashCode().absoluteValue % 10_000), builder.build())
    }

    /** Apaga da bandeja o aviso de um pedido que não está mais disponível. */
    fun cancelIncomingOrderNotification(context: Context, orderId: String) {
        if (orderId.isBlank()) return
        context.getSystemService(NotificationManager::class.java)
            .cancel(ORDER_NOTIFICATION_ID + (orderId.hashCode().absoluteValue % 10_000))
    }

    const val PRESENCE_NOTIFICATION_ID = 2003

    fun presenceNotification(context: Context): Notification {
        val openApp = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val contentIntent = openApp?.let {
            PendingIntent.getActivity(
                context,
                PRESENCE_NOTIFICATION_ID,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val offlineIntent = PendingIntent.getService(
            context,
            PRESENCE_NOTIFICATION_ID + 1,
            Intent(context, DriverPresenceService::class.java).setAction(DriverPresenceService.ACTION_GO_OFFLINE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_TRACKING)
            .setSmallIcon(R.drawable.ic_stat_driver_helmet)
            .setContentTitle("Você está ONLINE")
            .setContentText("Sua localização está sendo compartilhada com suas lojas enquanto você estiver online.")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .apply { if (contentIntent != null) setContentIntent(contentIntent) }
            .addAction(0, "Ficar offline", offlineIntent)
            .build()
    }

    fun trackingNotification(context: Context): Notification =
        NotificationCompat.Builder(context, CHANNEL_TRACKING)
            .setSmallIcon(R.drawable.ic_stat_driver_helmet)
            .setContentTitle("Rastreamento de entrega ativo")
            .setContentText("Sua localização está sendo enviada enquanto houver rota ativa.")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

}
