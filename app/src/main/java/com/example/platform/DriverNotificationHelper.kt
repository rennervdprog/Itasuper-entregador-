package com.example.platform

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.IncomingOrderAlert
import com.example.ui.orders.IncomingOrderActivity
import kotlin.math.absoluteValue

object DriverNotificationHelper {
    const val CHANNEL_ORDERS = "driver_orders"
    const val CHANNEL_TRACKING = "driver_tracking"
    private const val ORDER_NOTIFICATION_ID = 2001
    const val TRACKING_NOTIFICATION_ID = 2002

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ORDERS,
                "Pedidos do entregador",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Alertas de novos pedidos e atualizações de entrega" }
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

    fun showIncomingOrder(
        context: Context,
        orderId: String,
        storeName: String,
        neighborhood: String,
        shortCode: String,
        alert: IncomingOrderAlert? = null
    ) {
        if (!canNotify(context) || orderId.isBlank()) return

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

        val builder = NotificationCompat.Builder(context, CHANNEL_ORDERS)
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
