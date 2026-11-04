package com.example

import com.example.data.model.DeliveryOrder
import com.example.data.model.LinkedStore
import com.example.data.model.OrderDeliveryStatus
import com.example.data.model.PaymentSummary
import com.example.data.remote.optimizeRouteStops
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes do otimizador de rota (vizinho mais próximo em dois blocos:
 * já saiu para entrega primeiro, ainda na loja depois).
 */
class RouteOptimizerTest {

    private val store = LinkedStore(
        id = "loja-1",
        name = "Loja Teste",
        tradeName = "Loja Teste",
        address = "Rua A, 1",
        neighborhood = "Centro",
        city = "Araruama",
        phone = "",
        cnpj = "",
        latitude = 0.0,
        longitude = 0.0
    )

    private fun order(
        id: String,
        status: OrderDeliveryStatus,
        lat: Double?,
        lng: Double?
    ) = DeliveryOrder(
        id = id,
        shortCode = id,
        store = store,
        status = status,
        customerName = "Cliente",
        customerPhone = "",
        addressStreet = "Rua B",
        addressNumber = "2",
        addressNeighborhood = "Centro",
        addressCity = "Araruama",
        destinationLatitude = lat,
        destinationLongitude = lng,
        deliveryFee = 0.0,
        estimatedDistanceKm = 0.0,
        estimatedTimeMinutes = 0,
        items = emptyList(),
        payment = PaymentSummary(method = "pix", amount = 0.0)
    )

    @Test
    fun ordenaPeloVizinhoMaisProximoAPartirDaOrigem() {
        val longe = order("longe", OrderDeliveryStatus.PRONTO_PARA_ENTREGA, 0.0, 0.03)
        val perto = order("perto", OrderDeliveryStatus.PRONTO_PARA_ENTREGA, 0.0, 0.01)
        val meio = order("meio", OrderDeliveryStatus.PRONTO_PARA_ENTREGA, 0.0, 0.02)

        val result = optimizeRouteStops(
            orders = listOf(longe, perto, meio),
            departedOriginLat = null, departedOriginLng = null,
            readyOriginLat = 0.0, readyOriginLng = 0.0
        )

        assertEquals(listOf("perto", "meio", "longe"), result.map { it.id })
        assertEquals(listOf(1, 2, 3), result.map { it.stopOrder })
        // Cada trecho de 0,01 grau no equador ≈ 1,11 km.
        result.forEach { stop ->
            val leg = stop.routeLegKm ?: error("trecho ausente")
            assertTrue(leg > 1.0 && leg < 1.25)
        }
    }

    @Test
    fun paradaSemCoordenadaVaiParaOFimDoBloco() {
        val semCoord = order("sem", OrderDeliveryStatus.PRONTO_PARA_ENTREGA, null, null)
        val comCoord = order("com", OrderDeliveryStatus.PRONTO_PARA_ENTREGA, 0.0, 0.01)

        val result = optimizeRouteStops(
            orders = listOf(semCoord, comCoord),
            departedOriginLat = null, departedOriginLng = null,
            readyOriginLat = 0.0, readyOriginLng = 0.0
        )

        assertEquals(listOf("com", "sem"), result.map { it.id })
        assertNull(result.last().routeLegKm)
    }

    @Test
    fun quemJaSaiuVemAntesDeQuemEstaNaLoja() {
        val naLojaPerto = order("loja", OrderDeliveryStatus.PRONTO_PARA_ENTREGA, 0.0, 0.01)
        val naRuaLonge = order("rua", OrderDeliveryStatus.SAIU_ENTREGA, 0.0, 0.05)

        val result = optimizeRouteStops(
            orders = listOf(naLojaPerto, naRuaLonge),
            departedOriginLat = 0.0, departedOriginLng = 0.0,
            readyOriginLat = 0.0, readyOriginLng = 0.0
        )

        assertEquals(listOf("rua", "loja"), result.map { it.id })
    }

    @Test
    fun semOrigemMantemOrdemDeChegada() {
        val a = order("a", OrderDeliveryStatus.PRONTO_PARA_ENTREGA, 0.0, 0.03)
        val b = order("b", OrderDeliveryStatus.PRONTO_PARA_ENTREGA, 0.0, 0.01)

        val result = optimizeRouteStops(
            orders = listOf(a, b),
            departedOriginLat = null, departedOriginLng = null,
            readyOriginLat = null, readyOriginLng = null
        )

        assertEquals(listOf("a", "b"), result.map { it.id })
        assertNull(result[0].routeLegKm)
    }
}
