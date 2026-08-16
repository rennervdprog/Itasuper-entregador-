package com.example.data.model

/**
 * Dados operacionais permitidos antes do aceite. Contato do cliente e qualquer
 * ganho privado do motoboy permanecem ocultos até a rota ser atribuída.
 */
data class IncomingOrderAlert(
    val orderId: String,
    val shortCode: String,
    val storeName: String,
    val pickupAddress: String = "Retirada na loja parceira",
    val destinationAddress: String = "Destino disponível",
    val neighborhood: String = "",
    val itemCount: Int = 0,
    val paymentMethod: String = "",
    val totalLabel: String = ""
)
