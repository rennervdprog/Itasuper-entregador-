package com.example.data.remote

import com.example.data.model.DeliveryOrder
import com.example.data.model.DriverHistoryEntry
import com.example.data.model.DriverHistorySummary
import com.example.data.model.OrderDisplayCode
import com.example.data.repository.DriverHistoryRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.Instant
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Histórico operacional do motoboy. A fonte de verdade são os próprios pedidos
 * finalizados e atribuídos ao usuário autenticado; dados financeiros não são
 * requisito para registrar uma entrega concluída.
 */
class SupabaseDriverHistoryRepository : DriverHistoryRepository {
    private val supabase = ItaSuperSupabase.client

    override fun getHistory(filter: String): Flow<List<DriverHistoryEntry>> = flow {
        emit(runCatching { fetchHistory(filter) }.getOrDefault(emptyList()))
    }

    override fun getHistorySummary(filter: String): Flow<DriverHistorySummary> = flow {
        val history = runCatching { fetchHistory(filter) }.getOrDefault(emptyList())
        emit(
            DriverHistorySummary(
                totalDistanceKm = history.sumOf { it.distanceKm },
                totalTimeMinutes = history.sumOf { it.timeMinutes },
                totalRides = history.size,
                totalEarnings = 0.0,
                periodFilter = filter
            )
        )
    }

    override suspend fun addCompletedOrderToHistory(order: DeliveryOrder) = Unit

    private suspend fun fetchHistory(filter: String): List<DriverHistoryEntry> {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return emptyList()
        val since = when (filter) {
            "7 dias" -> Instant.now().minus(Duration.ofDays(7))
            "30 dias" -> Instant.now().minus(Duration.ofDays(30))
            else -> null
        }

        val completedOrders = supabase
            .from("orders")
            .select(columns = Columns.list(
                "id", "store_id", "status", "created_at", "confirmed_at", "neighborhood", "address_details",
                "delivery_cep", "delivery_city", "delivery_state", "client_lat", "client_lng", "delivery_fee"
            )) {
                filter {
                    eq("driver_id", userId)
                    eq("status", "finalizado")
                }
            }
            .decodeList<OrderRow>()
            .filter { order ->
                since == null || runCatching {
                    Instant.parse(order.confirmedAt ?: order.createdAt).isAfter(since)
                }.getOrDefault(false)
            }
            .sortedByDescending { it.confirmedAt ?: it.createdAt }

        if (completedOrders.isEmpty()) return emptyList()

        val storeById = supabase
            .from("stores_driver_view")
            .select(columns = Columns.list("id", "name", "latitude", "longitude")) {
                filter { isIn("id", completedOrders.map { it.storeId }.distinct()) }
            }
            .decodeList<StoreRow>()
            .associateBy { it.id }

        return completedOrders.map { order ->
            val store = storeById[order.storeId]
            val distance = haversineKm(store?.latitude, store?.longitude, order.clientLat, order.clientLng) ?: 0.0
            val timestampValue = order.confirmedAt ?: order.createdAt
            val timestamp = timestampValue?.let { runCatching { Instant.parse(it) }.getOrNull() }
            DriverHistoryEntry(
                id = order.id,
                orderShortCode = OrderDisplayCode.fromOrderId(order.id),
                storeName = store?.name ?: "Loja ItaSuper",
                dateFormatted = timestamp?.toString()?.take(10).orEmpty(),
                timeFormatted = timestamp?.toString()?.drop(11)?.take(5).orEmpty(),
                neighborhood = order.neighborhood.orEmpty().ifBlank { "Destino informado" },
                addressSummary = order.addressDetails.orEmpty(),
                distanceKm = distance,
                timeMinutes = 0,
                deliveryFee = order.deliveryFee ?: 0.0,
                driverEarnings = 0.0,
                statusText = "Concluída",
                isStraightLineEstimate = true
            )
        }
    }

    private fun haversineKm(lat1: Double?, lng1: Double?, lat2: Double?, lng2: Double?): Double? {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) return null
        val radius = 6371.0
        fun rad(value: Double) = value * Math.PI / 180.0
        val deltaLat = rad(lat2 - lat1)
        val deltaLng = rad(lng2 - lng1)
        val a = sin(deltaLat / 2).pow(2) + cos(rad(lat1)) * cos(rad(lat2)) * sin(deltaLng / 2).pow(2)
        return 2 * radius * asin(sqrt(a))
    }
}
