package com.example.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileRow(
    @SerialName("user_id") val userId: String,
    @SerialName("full_name") val fullName: String? = null,
    val phone: String? = null,
    val role: String? = null,
    val vehicle: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class StoreDriverLinkRow(
    val id: String,
    @SerialName("store_id") val storeId: String,
    val status: String,
    @SerialName("created_at") val createdAt: String? = null,
    val stores: StoreNameJoinRow? = null
)

@Serializable
data class StoreNameJoinRow(
    val name: String? = null
)

@Serializable
data class StoreRow(
    val id: String,
    val name: String? = null,
    @SerialName("address_street") val addressStreet: String? = null,
    @SerialName("address_neighborhood") val addressNeighborhood: String? = null,
    @SerialName("address_city") val addressCity: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("driver_pin_autofill") val driverPinAutofill: Boolean? = null
)

@Serializable
data class DriverStatusUpsert(
    @SerialName("user_id") val userId: String,
    @SerialName("is_online") val isOnline: Boolean,
    val name: String,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class OrderRow(
    val id: String,
    @SerialName("client_id") val clientId: String? = null,
    @SerialName("store_id") val storeId: String,
    val status: String,
    val subtotal: Double? = null,
    @SerialName("delivery_fee") val deliveryFee: Double? = null,
    @SerialName("total_price") val totalPrice: Double? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    val neighborhood: String? = null,
    @SerialName("address_details") val addressDetails: String? = null,
    @SerialName("delivery_cep") val deliveryCep: String? = null,
    @SerialName("delivery_city") val deliveryCity: String? = null,
    @SerialName("delivery_state") val deliveryState: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("driver_id") val driverId: String? = null,
    @SerialName("delivery_pin") val deliveryPin: String? = null,
    @SerialName("confirmed_at") val confirmedAt: String? = null,
    @SerialName("needs_change") val needsChange: Boolean? = null,
    @SerialName("change_for") val changeFor: Double? = null,
    @SerialName("client_lat") val clientLat: Double? = null,
    @SerialName("client_lng") val clientLng: Double? = null,
    @SerialName("assigned_driver_id") val assignedDriverId: String? = null,
    @SerialName("order_number") val orderNumber: Long? = null
)

@Serializable
data class OrderItemRow(
    val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("product_id") val productId: String,
    val quantity: Int,
    @SerialName("unit_price") val unitPrice: Double
)

@Serializable
data class ProductRow(
    val id: String,
    val name: String? = null
)

@Serializable
data class ProfileContactRow(
    @SerialName("user_id") val userId: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("whatsapp_number") val whatsappNumber: String? = null,
    val phone: String? = null
)

@Serializable
data class AcceptOrderParams(
    @SerialName("_order_id") val orderId: String
)

@Serializable
data class FinishDeliveryParams(
    @SerialName("_order_id") val orderId: String,
    @SerialName("_pin") val pin: String
)

@Serializable
data class DriverStatusRow(
    @SerialName("user_id") val userId: String,
    @SerialName("is_online") val isOnline: Boolean = false
)

@Serializable
data class StoreDriverEarningRow(
    val id: String,
    @SerialName("store_id") val storeId: String,
    @SerialName("driver_user_id") val driverUserId: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("fee_total") val feeTotal: Double = 0.0,
    @SerialName("driver_amount") val driverAmount: Double = 0.0,
    val status: String = "pendente",
    @SerialName("paid_at") val paidAt: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class SupportTicketRow(
    val id: String,
    @SerialName("ticket_number") val ticketNumber: Int? = null,
    @SerialName("user_id") val userId: String,
    val category: String,
    val priority: String = "normal",
    val status: String = "aberto",
    val subject: String,
    @SerialName("first_message") val firstMessage: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class SupportTicketInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("user_role") val userRole: String = "motoboy",
    val category: String,
    val subject: String,
    @SerialName("first_message") val firstMessage: String,
    val priority: String = "normal"
)
