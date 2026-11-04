package com.example.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.model.DeliveryOrder
import com.example.data.model.NavigationPreference
import com.example.data.model.OrderDeliveryStatus

object DriverExternalActions {
    /**
     * Abre a navegação usando primeiro as coordenadas imutáveis do snapshot do
     * pedido. O endereço textual é mantido apenas como contingência para pedidos
     * legados que ainda não possuem latitude/longitude; nenhum dado é gravado.
     */
    fun openNavigation(
        context: Context,
        preference: NavigationPreference,
        destination: String,
        latitude: Double? = null,
        longitude: Double? = null,
        destinationPrecision: String? = null
    ): Result<Unit> = runCatching {
        val hasCoordinates = latitude != null && longitude != null &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0
        val hasPreciseCoordinates = hasCoordinates &&
            !destinationPrecision.equals("cep", ignoreCase = true)
        require(hasPreciseCoordinates || destination.isNotBlank()) {
            "O endereço de destino não foi informado."
        }

        val coordinateQuery = if (hasPreciseCoordinates) "$latitude,$longitude" else null
        val encodedDestination = Uri.encode(coordinateQuery ?: destination)
        val primaryIntent = when (preference) {
            NavigationPreference.GOOGLE_MAPS -> Intent(
                Intent.ACTION_VIEW,
                Uri.parse("google.navigation:q=$encodedDestination")
            ).setPackage("com.google.android.apps.maps")

            NavigationPreference.WAZE -> Intent(
                Intent.ACTION_VIEW,
                // ll= só com precisão de endereço real (auditoria B11):
                // com precisão "cep", a coordenada é o centróide do CEP e
                // o Waze navegava p/ o miolo do bairro — igual ao Google,
                // cai p/ a consulta por texto do endereço.
                if (hasPreciseCoordinates) {
                    Uri.parse("waze://?ll=$latitude,$longitude&navigate=yes")
                } else {
                    Uri.parse("waze://?q=$encodedDestination&navigate=yes")
                }
            ).setPackage("com.waze")
        }

        val fallbackIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("geo:0,0?q=$encodedDestination")
        )
        context.startActivity(
            if (primaryIntent.resolveActivity(context.packageManager) != null) {
                primaryIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } else {
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    /**
     * Abre o Google Maps com a rota inteira: a origem é a posição atual do
     * aparelho (o Maps usa sozinho quando a origem é omitida), as paradas
     * intermediárias viram `waypoints` na ordem recebida e a última é o
     * destino. Parada com coordenada precisa entra como coordenada; as
     * demais (precisão de CEP ou sem coordenada) entram pelo endereço
     * textual — mesma regra da navegação individual (auditoria B11).
     *
     * Limite do Maps por link: destino + até 9 intermediárias. Rota maior
     * que isso abre com as 10 primeiras paradas da sequência; o restante
     * continua no app. O Waze não aceita rota multiparadas por link, por
     * isso esta ação é sempre no Google Maps.
     */
    fun openFullRoute(
        context: Context,
        orders: List<DeliveryOrder>
    ): Result<Unit> = runCatching {
        val stops = orders.filter { it.status != OrderDeliveryStatus.FINALIZADO }
        require(stops.size >= 2) { "A rota precisa de pelo menos duas paradas." }

        fun stopParam(order: DeliveryOrder): String {
            val lat = order.destinationLatitude
            val lng = order.destinationLongitude
            val hasPreciseCoordinates = lat != null && lng != null &&
                lat in -90.0..90.0 && lng in -180.0..180.0 &&
                !order.destinationPrecision.equals("cep", ignoreCase = true)
            return if (hasPreciseCoordinates) "$lat,$lng" else order.fullAddress
        }

        val limited = stops.take(10)
        val url = buildString {
            append("https://www.google.com/maps/dir/?api=1")
            append("&travelmode=driving")
            append("&destination=")
            append(Uri.encode(stopParam(limited.last())))
            val waypoints = limited.dropLast(1)
            if (waypoints.isNotEmpty()) {
                append("&waypoints=")
                append(waypoints.joinToString("%7C") { Uri.encode(stopParam(it)) })
            }
        }

        val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .setPackage("com.google.android.apps.maps")
        val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(
            if (mapsIntent.resolveActivity(context.packageManager) != null) {
                mapsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } else {
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun openDialer(context: Context, phone: String): Result<Unit> = runCatching {
        val normalized = phone.filter { it.isDigit() || it == '+' }
        require(normalized.isNotBlank()) { "Telefone do cliente não informado." }
        context.startActivity(
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$normalized"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openWhatsApp(context: Context, phone: String): Result<Unit> = runCatching {
        val digits = phone.filter(Char::isDigit)
        val normalized = when {
            // O perfil já armazena muitos números no padrão E.164 sem o "+".
            // Nunca prefixar outro 55 nesses casos.
            digits.startsWith("55") && digits.length in 12..13 -> digits
            // Contatos brasileiros antigos podem ter sido salvos sem DDI.
            digits.length in 10..11 -> "55$digits"
            digits.startsWith("0") && digits.drop(1).length in 10..11 -> "55${digits.drop(1)}"
            else -> error("WhatsApp do cliente está em formato inválido.")
        }
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://wa.me/$normalized")
        ).setPackage("com.whatsapp")
        val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$normalized"))
        context.startActivity(
            if (intent.resolveActivity(context.packageManager) != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } else {
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
