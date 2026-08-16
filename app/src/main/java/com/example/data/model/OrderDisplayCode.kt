package com.example.data.model

/**
 * Código público de referência do pedido, alinhado ao formato exibido no site.
 * O número sequencial interno (por exemplo, #32) não é usado na interface do
 * motoboy; a referência visível deriva dos oito primeiros caracteres do UUID.
 */
object OrderDisplayCode {
    fun fromOrderId(orderId: String): String {
        val normalized = orderId.trim()
            .replace("-", "")
            .take(8)
            .uppercase()
        return if (normalized.isBlank()) "#PEDIDO" else "#$normalized"
    }
}
