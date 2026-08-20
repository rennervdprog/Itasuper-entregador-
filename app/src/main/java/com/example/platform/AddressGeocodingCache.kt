package com.example.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** Coordenadas de destino resolvidas a partir do endereço textual do pedido. */
data class DestinationCoordinates(
    val latitude: Double,
    val longitude: Double
)

/** Município sugerido somente para preenchimento manual da preferência do diretório. */
data class CurrentMunicipalitySuggestion(
    val city: String,
    val state: String = ""
)

/**
 * Resolve e armazena em cache o endereço de entrega para pedidos antigos que
 * ainda não possuem client_lat/client_lng. O resultado permite apresentar uma
 * distância estimada entre a loja e o destino, sem gravar ou alterar dados do
 * pedido no Supabase.
 */
object AddressGeocodingCache {
    private const val PREFS_NAME = "itasuper_address_geocoding_cache"

    suspend fun resolve(
        context: Context,
        address: String,
        neighborhood: String,
        city: String
    ): DestinationCoordinates? = withContext(Dispatchers.IO) {
        val normalizedAddress = listOf(address, neighborhood, city, "Brasil")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(", ")

        if (normalizedAddress.isBlank()) return@withContext null

        val preferences = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cacheKey = "destination_${sha256(normalizedAddress.lowercase(Locale.ROOT))}"
        preferences.getString(cacheKey, null)
            ?.split(',')
            ?.takeIf { it.size == 2 }
            ?.let { cached ->
                val latitude = cached[0].toDoubleOrNull()
                val longitude = cached[1].toDoubleOrNull()
                if (latitude != null && longitude != null) {
                    return@withContext DestinationCoordinates(latitude, longitude)
                }
            }

        if (!Geocoder.isPresent()) return@withContext null

        val destination = runCatching {
            @Suppress("DEPRECATION")
            val result = Geocoder(context.applicationContext, Locale("pt", "BR"))
                .getFromLocationName(normalizedAddress, 1)
                ?.firstOrNull()
            result?.let { DestinationCoordinates(it.latitude, it.longitude) }
        }.getOrNull() ?: return@withContext null

        preferences.edit()
            .putString(cacheKey, "${destination.latitude},${destination.longitude}")
            .apply()
        destination
    }

    /** Retorna a posição atual do motoboy quando a permissão de localização está concedida. */
    suspend fun currentDriverLocation(context: Context): DestinationCoordinates? = withContext(Dispatchers.IO) {
        val hasFinePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarsePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFinePermission && !hasCoarsePermission) return@withContext null

        suspendCancellableCoroutine { continuation ->
            val cancellation = CancellationTokenSource()
            LocationServices.getFusedLocationProviderClient(context.applicationContext)
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token)
                .addOnSuccessListener { location ->
                    if (continuation.isActive) {
                        continuation.resume(location?.let {
                            DestinationCoordinates(it.latitude, it.longitude)
                        })
                    }
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }
            continuation.invokeOnCancellation { cancellation.cancel() }
        }
    }

    /**
     * Obtém a cidade correspondente à posição atual somente quando o motoboy solicita.
     * A posição não é persistida, compartilhada ou usada para alterar disponibilidade.
     */
    suspend fun currentDriverMunicipality(context: Context): CurrentMunicipalitySuggestion? = withContext(Dispatchers.IO) {
        val location = currentDriverLocation(context) ?: return@withContext null
        if (!Geocoder.isPresent()) return@withContext null

        runCatching {
            @Suppress("DEPRECATION")
            val address = Geocoder(context.applicationContext, Locale("pt", "BR"))
                .getFromLocation(location.latitude, location.longitude, 1)
                ?.firstOrNull()
                ?: return@runCatching null
            val city = address.locality
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: address.subAdminArea?.trim()?.takeIf { it.isNotBlank() }
                ?: return@runCatching null
            CurrentMunicipalitySuggestion(
                city = city,
                state = address.adminArea.orEmpty().trim()
            )
        }.getOrNull()
    }

    private fun sha256(value: String): String = MessageDigest
        .getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }
}
