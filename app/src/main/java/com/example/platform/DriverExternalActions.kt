package com.example.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.model.NavigationPreference

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
        longitude: Double? = null
    ): Result<Unit> = runCatching {
        val hasCoordinates = latitude != null && longitude != null &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0
        require(hasCoordinates || destination.isNotBlank()) {
            "O endereço de destino não foi informado."
        }

        val coordinateQuery = if (hasCoordinates) "$latitude,$longitude" else null
        val encodedDestination = Uri.encode(coordinateQuery ?: destination)
        val primaryIntent = when (preference) {
            NavigationPreference.GOOGLE_MAPS -> Intent(
                Intent.ACTION_VIEW,
                Uri.parse("google.navigation:q=$encodedDestination")
            ).setPackage("com.google.android.apps.maps")

            NavigationPreference.WAZE -> Intent(
                Intent.ACTION_VIEW,
                if (hasCoordinates) {
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
