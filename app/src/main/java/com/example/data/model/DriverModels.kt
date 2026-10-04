package com.example.data.model

/**
 * Representa o perfil cadastrado do motoboy de loja do ItaSuper.
 */
data class DriverProfile(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    // Estes campos só devem ser preenchidos quando vierem de uma fonte real.
    val vehicleType: String = "",
    val vehiclePlate: String = "",
    val rating: Double = 0.0,
    val completedDeliveriesCount: Int = 0,
    val memberSince: String = ""
)

/**
 * Preferência voluntária e independente para a base de motoboys por cidade.
 * Não representa vínculo com loja, disponibilidade ou condição de contratação.
 */
data class DriverDirectoryPreference(
    val city: String = "",
    val isListed: Boolean = false,
    val hasContactConsent: Boolean = false
)

/**
 * Status de vínculo entre o motoboy e a loja parceira ItaSuper.
 */
enum class DriverLinkStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}

/**
 * Representa uma loja ItaSuper à qual o motoboy pode ser vinculado.
 */
data class LinkedStore(
    val id: String,
    val name: String,
    val tradeName: String,
    val address: String,
    val neighborhood: String,
    val city: String,
    val phone: String,
    val cnpj: String,
    val activeOrdersCount: Int = 0
)

/**
 * Vínculo de loja que o motoboy possui ou convite pendente.
 */
data class StoreDriverLink(
    val id: String,
    val store: LinkedStore,
    val status: DriverLinkStatus,
    val invitedAt: String,
    val acceptedAt: String? = null,
    val tags: List<String> = emptyList()
)

/**
 * Disponibilidade do entregador para receber pedidos das lojas vinculadas.
 */
data class DriverAvailability(
    val isOnline: Boolean,
    val statusText: String,
    val lastChangedAt: Long = System.currentTimeMillis()
)

/**
 * Status do ciclo de vida da entrega.
 */
enum class OrderDeliveryStatus(val label: String) {
    PRONTO_PARA_ENTREGA("Pronto p/ sair"),
    SAIU_ENTREGA("Em entrega"),
    EM_TRANSITO("Em rota"),
    FINALIZADO("Concluído")
}

/**
 * Informações de contato e telefone do cliente para entrega.
 */
data class DriverContact(
    val name: String,
    val phone: String,
    val whatsappAvailable: Boolean = true
)

/**
 * Resumo do pagamento do pedido pelo cliente.
 */
data class PaymentSummary(
    val method: String,
    val amount: Double,
    val changeFor: Double? = null,
    val isPaidOnline: Boolean = true
) {
    val displayChangeText: String?
        get() = if (changeFor != null && changeFor > amount) {
            "Troco para R$ ${String.format("%.2f", changeFor)} (R$ ${String.format("%.2f", changeFor - amount)})"
        } else null

    /** Texto operacional para o entregador, sem alterar o método ou o valor do pedido. */
    val driverInstruction: String
        get() {
            val normalized = method.trim().lowercase()
            return when {
                normalized.contains("pix") -> "Pix direto — já pago"
                normalized.contains("cart") || normalized.contains("crédito") || normalized.contains("credito") || normalized.contains("débito") || normalized.contains("debito") -> "Cartão — levar maquininha"
                normalized.contains("dinheiro") || normalized.contains("cash") -> "Dinheiro — cobrar na entrega"
                else -> method.replace('_', ' ').ifBlank { "Pagamento não informado" }
            }
        }
}

/**
 * Item constante no pedido do cliente.
 */
data class DeliveryItem(
    val id: String,
    val name: String,
    val quantity: Int,
    val unitPrice: Double
)

/**
 * Pedido de entrega atribuído ou disponível para o motoboy.
 */
data class DeliveryOrder(
    val id: String,
    val shortCode: String,
    val store: LinkedStore,
    val status: OrderDeliveryStatus,
    val customerName: String,
    val customerPhone: String,
    val addressStreet: String,
    val addressNumber: String,
    val addressNeighborhood: String,
    val addressCity: String,
    val addressState: String = "",
    val addressCep: String = "",
    val destinationLatitude: Double? = null,
    val destinationLongitude: Double? = null,
    /** `cep` indica centro aproximado; não deve ser tratado como imóvel exato. */
    val destinationPrecision: String? = null,
    val addressComplement: String? = null,
    val deliveryFee: Double,
    val driverEarnings: Double,
    val estimatedDistanceKm: Double,
    val estimatedTimeMinutes: Int,
    val items: List<DeliveryItem>,
    val payment: PaymentSummary,
    val customerPin: String = "4821",
    val isPinPreFilledAllowed: Boolean = false,
    val stopOrder: Int = 1,
    val notes: String? = null,
    val acceptedAt: String? = null,
    val createdAt: String = "Hoje, 14:15",
    val departedAt: String? = null,
    val deliveredAt: String? = null
) {
    val fullAddress: String
        get() = listOfNotNull(
            listOf(addressStreet, addressNumber).filter { it.isNotBlank() }.joinToString(", ").ifBlank { null },
            addressComplement?.takeIf { it.isNotBlank() },
            listOf(addressNeighborhood, addressCity, addressState)
                .filter { it.isNotBlank() }
                .joinToString(" - ")
                .ifBlank { null },
            addressCep.takeIf { it.isNotBlank() }?.let { "CEP $it" }
        ).joinToString(", ")

    val shortAddress: String
        get() = listOf(addressStreet, addressNumber).filter { it.isNotBlank() }.joinToString(", ")
            .ifBlank { addressStreet.ifBlank { "Endereço não informado" } }

    /** Resumo legível dos produtos para a visualização rápida do entregador. */
    val driverItemsSummary: String
        get() = items.joinToString(" + ") { item ->
            if (item.quantity == 1) item.name else "${item.quantity}x ${item.name}"
        }.ifBlank { "Itens não informados" }
}

/**
 * Preferência de aplicativo de navegação.
 */
enum class NavigationPreference(val displayName: String) {
    GOOGLE_MAPS("Google Maps"),
    WAZE("Waze")
}

/**
 * Registro de confirmação offline para sincronização posterior.
 */
data class OfflineDeliveryConfirmation(
    val orderId: String,
    val shortCode: String,
    val pin: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

/**
 * Entrada no histórico de entregas.
 */
data class DriverHistoryEntry(
    val id: String,
    val orderShortCode: String,
    val storeName: String,
    val dateFormatted: String,
    val timeFormatted: String,
    val neighborhood: String,
    val addressSummary: String,
    val distanceKm: Double,
    val timeMinutes: Int,
    val deliveryFee: Double,
    val driverEarnings: Double,
    val statusText: String = "Recebido", // "Recebido", "Confirmar", "Pendente"
    val isStraightLineEstimate: Boolean = true
)

/**
 * Resumo agregado das métricas do histórico.
 */
data class DriverHistorySummary(
    val totalDistanceKm: Double,
    val totalTimeMinutes: Int,
    val totalRides: Int,
    val totalEarnings: Double,
    val periodFilter: String = "7 dias" // "7 dias", "30 dias", "Tudo"
)

/**
 * Chamado de suporte local para simulação.
 */
data class SupportTicket(
    val id: String,
    val subject: String,
    val description: String,
    val category: String,
    val status: String = "Em análise",
    val createdAt: String = "Hoje, 15:30"
)
