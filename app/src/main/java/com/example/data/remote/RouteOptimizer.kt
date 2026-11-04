package com.example.data.remote

import com.example.data.model.DeliveryOrder
import com.example.data.model.OrderDeliveryStatus
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Distância em linha reta entre dois pontos, em km (haversine).
 * Retorna nulo quando qualquer coordenada falta.
 */
internal fun haversineKm(
    originLat: Double?,
    originLng: Double?,
    clientLat: Double?,
    clientLng: Double?
): Double? {
    if (originLat == null || originLng == null || clientLat == null || clientLng == null) {
        return null
    }
    val earthRadiusKm = 6371.0
    val latitudeDelta = Math.toRadians(clientLat - originLat)
    val longitudeDelta = Math.toRadians(clientLng - originLng)
    val a = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
        cos(Math.toRadians(originLat)) * cos(Math.toRadians(clientLat)) *
        sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
    return earthRadiusKm * 2 * atan2(sqrt(a), sqrt(1 - a))
}

/**
 * Ordena as paradas da rota ativa pela menor distância (vizinho mais
 * próximo), em dois blocos:
 *
 * 1. Pedidos que já saíram para entrega (SAIU_ENTREGA/EM_TRANSITO),
 *    sequenciados a partir da posição atual do motoboy.
 * 2. Pedidos ainda na loja (PRONTO_PARA_ENTREGA), sequenciados a
 *    partir da loja — eles só entram na rua depois da retirada.
 *
 * Paradas sem coordenadas ficam no fim do próprio bloco, na ordem de
 * chegada, com `routeLegKm` nulo. Sem origem conhecida, o bloco mantém
 * a ordem de chegada. Empates de distância seguem a ordem de entrada
 * (que já vem ordenada pela criação do pedido).
 *
 * A função é pura: devolve cópias com `stopOrder` (1..n) e
 * `routeLegKm` preenchidos, sem tocar na lista original.
 */
internal fun optimizeRouteStops(
    orders: List<DeliveryOrder>,
    departedOriginLat: Double?,
    departedOriginLng: Double?,
    readyOriginLat: Double?,
    readyOriginLng: Double?
): List<DeliveryOrder> {
    if (orders.isEmpty()) return orders
    val departed = orders.filter {
        it.status == OrderDeliveryStatus.SAIU_ENTREGA || it.status == OrderDeliveryStatus.EM_TRANSITO
    }
    val ready = orders.filter { it.status == OrderDeliveryStatus.PRONTO_PARA_ENTREGA }
    val sequenced = sequenceSegment(departed, departedOriginLat, departedOriginLng) +
        sequenceSegment(ready, readyOriginLat, readyOriginLng)
    return sequenced.mapIndexed { index, stop -> stop.copy(stopOrder = index + 1) }
}

private fun sequenceSegment(
    segment: List<DeliveryOrder>,
    originLat: Double?,
    originLng: Double?
): List<DeliveryOrder> {
    if (segment.size <= 1 || originLat == null || originLng == null) return segment
    val withCoords = segment.filter { it.destinationLatitude != null && it.destinationLongitude != null }
    val withoutCoords = segment.filter { it.destinationLatitude == null || it.destinationLongitude == null }
    if (withCoords.isEmpty()) return segment

    val remaining = withCoords.toMutableList()
    val ordered = mutableListOf<DeliveryOrder>()
    var currentLat = originLat
    var currentLng = originLng
    while (remaining.isNotEmpty()) {
        val next = remaining.minByOrNull { candidate ->
            haversineKm(currentLat, currentLng, candidate.destinationLatitude, candidate.destinationLongitude)
                ?: Double.MAX_VALUE
        } ?: break
        val legKm = haversineKm(currentLat, currentLng, next.destinationLatitude, next.destinationLongitude)
        ordered += next.copy(routeLegKm = legKm)
        currentLat = next.destinationLatitude
        currentLng = next.destinationLongitude
        remaining.remove(next)
    }
    return ordered + withoutCoords
}
