package com.example.data.fake

import com.example.data.model.DeliveryItem
import com.example.data.model.DeliveryOrder
import com.example.data.model.DriverAvailability
import com.example.data.model.DriverHistoryEntry
import com.example.data.model.DriverHistorySummary
import com.example.data.model.DriverLinkStatus
import com.example.data.model.DriverProfile
import com.example.data.model.LinkedStore
import com.example.data.model.NavigationPreference
import com.example.data.model.OfflineDeliveryConfirmation
import com.example.data.model.OrderDeliveryStatus
import com.example.data.model.PaymentSummary
import com.example.data.model.StoreDriverLink
import com.example.data.model.SupportTicket
import com.example.data.repository.AuthRepository
import com.example.data.repository.DriverAvailabilityRepository
import com.example.data.repository.DriverHistoryRepository
import com.example.data.repository.DriverLinkRepository
import com.example.data.repository.DriverLocationRepository
import com.example.data.repository.DriverNotificationsRepository
import com.example.data.repository.DriverOrdersRepository
import com.example.data.repository.DriverProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Lojas de demonstração do ItaSuper
 */
val FakeStoreCentral = LinkedStore(
    id = "store_centro",
    name = "ItaSuper Hipermercado",
    tradeName = "ItaSuper - Matriz Centro",
    address = "Av. Brasil, 1200",
    neighborhood = "Centro",
    city = "Itaúna",
    phone = "(37) 3241-1000",
    cnpj = "12.345.678/0001-90",
    activeOrdersCount = 2
)

val FakeStoreGracas = LinkedStore(
    id = "store_gracas",
    name = "ItaSuper Express",
    tradeName = "ItaSuper - Loja Graças",
    address = "Rua São Paulo, 450",
    neighborhood = "Bairro das Graças",
    city = "Itaúna",
    phone = "(37) 3241-2000",
    cnpj = "12.345.678/0002-71",
    activeOrdersCount = 1
)

/**
 * Repositório Fake de Autenticação
 */
class FakeAuthRepository : AuthRepository {
    private val _currentUser = MutableStateFlow<DriverProfile?>(
        DriverProfile(
            id = "driver_carlos_01",
            name = "Carlos Eduardo Souza",
            email = "carlos.motoboy@itasuper.com.br",
            phone = "(37) 99842-7711",
            vehicleType = "Honda CG 160 Fan",
            vehiclePlate = "ITA-9A82",
            rating = 4.96,
            completedDeliveriesCount = 348,
            memberSince = "Fevereiro de 2024"
        )
    )

    override fun getCurrentUser(): Flow<DriverProfile?> = _currentUser.asStateFlow()

    override suspend fun login(email: String, password: String): Result<DriverProfile> {
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("E-mail e senha são obrigatórios."))
        }
        val profile = DriverProfile(
            id = "driver_carlos_01",
            name = "Carlos Eduardo Souza",
            email = email.trim(),
            phone = "(37) 99842-7711",
            vehicleType = "Honda CG 160 Fan",
            vehiclePlate = "ITA-9A82",
            rating = 4.96,
            completedDeliveriesCount = 348,
            memberSince = "Fevereiro de 2024"
        )
        _currentUser.value = profile
        return Result.success(profile)
    }

    override suspend fun logout() {
        _currentUser.value = null
    }

    override fun isAuthenticated(): Flow<Boolean> =
        _currentUser.map { it != null }
}

/**
 * Repositório Fake de Perfil do Entregador
 */
class FakeDriverProfileRepository : DriverProfileRepository {
    private val _profile = MutableStateFlow(
        DriverProfile(
            id = "driver_carlos_01",
            name = "Carlos Eduardo Souza",
            email = "carlos.motoboy@itasuper.com.br",
            phone = "(37) 99842-7711",
            vehicleType = "Honda CG 160 Fan",
            vehiclePlate = "ITA-9A82",
            rating = 4.96,
            completedDeliveriesCount = 348,
            memberSince = "Fevereiro de 2024"
        )
    )

    override fun getProfile(): Flow<DriverProfile> = _profile.asStateFlow()

    override suspend fun updateProfile(profile: DriverProfile) {
        _profile.value = profile
    }
}

/**
 * Repositório Fake de Vínculos de Loja
 */
class FakeDriverLinkRepository : DriverLinkRepository {
    private val _links = MutableStateFlow<List<StoreDriverLink>>(
        listOf(
            StoreDriverLink(
                id = "link_01",
                store = FakeStoreCentral,
                status = DriverLinkStatus.ACCEPTED,
                invitedAt = "10/01/2024",
                acceptedAt = "10/01/2024"
            ),
            StoreDriverLink(
                id = "link_02",
                store = FakeStoreGracas,
                status = DriverLinkStatus.ACCEPTED,
                invitedAt = "15/02/2024",
                acceptedAt = "15/02/2024"
            )
        )
    )

    override fun getLinks(): Flow<List<StoreDriverLink>> = _links.asStateFlow()

    override fun getHasAcceptedLink(): Flow<Boolean> =
        _links.map { list -> list.any { it.status == DriverLinkStatus.ACCEPTED } }

    override suspend fun acceptInvite(linkId: String) {
        _links.value = _links.value.map { link ->
            if (link.id == linkId) link.copy(status = DriverLinkStatus.ACCEPTED, acceptedAt = "Hoje, agora")
            else link
        }
    }

    override suspend fun rejectInvite(linkId: String) {
        _links.value = _links.value.map { link ->
            if (link.id == linkId) link.copy(status = DriverLinkStatus.REJECTED)
            else link
        }.filter { it.status != DriverLinkStatus.REJECTED }
    }

    override suspend fun checkNewInvites() {
        // Se estiver vazio, simula o recebimento de convite de loja
        if (_links.value.none { it.status == DriverLinkStatus.PENDING }) {
            simulateAddDemoInvite()
        }
    }

    override suspend fun simulateAddDemoInvite() {
        val newInvite = StoreDriverLink(
            id = "link_invite_${System.currentTimeMillis()}",
            store = FakeStoreCentral,
            status = DriverLinkStatus.PENDING,
            invitedAt = "Hoje, 14:00",
            tags = listOf("Vínculo direto", "Entregas locais", "Horário flexível")
        )
        _links.value = listOf(newInvite)
    }

    override suspend fun resetToAguardandoVinculo() {
        _links.value = emptyList()
    }
}

/**
 * Repositório Fake de Disponibilidade (Online/Offline)
 */
class FakeDriverAvailabilityRepository : DriverAvailabilityRepository {
    private val _availability = MutableStateFlow(
        DriverAvailability(
            isOnline = true,
            statusText = "Recebendo pedidos · toque para pausar"
        )
    )

    override fun getAvailability(): Flow<DriverAvailability> = _availability.asStateFlow()

    override suspend fun setOnline(online: Boolean, hasActiveDeliveries: Boolean): Result<Unit> {
        if (!online && hasActiveDeliveries) {
            return Result.failure(IllegalStateException("Finalize suas entregas ativas antes de ficar offline."))
        }
        _availability.value = DriverAvailability(
            isOnline = online,
            statusText = if (online) "Recebendo pedidos · toque para pausar" else "Você está Offline"
        )
        return Result.success(Unit)
    }
}

/**
 * Repositório Fake de Pedidos e Rotas
 */
class FakeDriverOrdersRepository(
    private val historyRepository: DriverHistoryRepository
) : DriverOrdersRepository {

    private val _availableOrders = MutableStateFlow<List<DeliveryOrder>>(
        createInitialAvailableOrders()
    )

    private val _activeRouteOrders = MutableStateFlow<List<DeliveryOrder>>(emptyList())
    private val _todayCompletedCount = MutableStateFlow(12)
    private val _offlineConfirmations = MutableStateFlow<List<OfflineDeliveryConfirmation>>(emptyList())

    override fun getAvailableOrders(): Flow<List<DeliveryOrder>> = _availableOrders.asStateFlow()
    override fun getActiveRouteOrders(): Flow<List<DeliveryOrder>> = _activeRouteOrders.asStateFlow()
    override fun getTodayCompletedCount(): Flow<Int> = _todayCompletedCount.asStateFlow()
    override fun getOfflineConfirmations(): Flow<List<OfflineDeliveryConfirmation>> = _offlineConfirmations.asStateFlow()

    override suspend fun acceptOrder(orderId: String): Result<Unit> {
        val order = _availableOrders.value.find { it.id == orderId }
            ?: return Result.failure(NoSuchElementException("Pedido não encontrado ou já aceito por outro motoboy."))

        val updatedOrder = order.copy(
            status = OrderDeliveryStatus.PRONTO_PARA_ENTREGA,
            acceptedAt = "Hoje, agora",
            stopOrder = _activeRouteOrders.value.size + 1
        )

        _availableOrders.value = _availableOrders.value.filter { it.id != orderId }
        _activeRouteOrders.value = _activeRouteOrders.value + updatedOrder
        return Result.success(Unit)
    }

    override suspend fun acceptAllOrders(orderIds: List<String>): Result<Unit> {
        val toAccept = _availableOrders.value.filter { it.id in orderIds }
        if (toAccept.isEmpty()) return Result.failure(NoSuchElementException("Nenhum pedido disponível."))

        var stopIndex = _activeRouteOrders.value.size + 1
        val updated = toAccept.map {
            it.copy(
                status = OrderDeliveryStatus.PRONTO_PARA_ENTREGA,
                acceptedAt = "Hoje, agora",
                stopOrder = stopIndex++
            )
        }

        _availableOrders.value = _availableOrders.value.filter { it.id !in orderIds }
        _activeRouteOrders.value = _activeRouteOrders.value + updated
        return Result.success(Unit)
    }

    override suspend fun rejectOrder(orderId: String) {
        _availableOrders.value = _availableOrders.value.filter { it.id != orderId }
    }

    override suspend fun dispatchOrder(orderId: String) {
        _activeRouteOrders.value = _activeRouteOrders.value.map { order ->
            if (order.id == orderId) {
                order.copy(
                    status = OrderDeliveryStatus.SAIU_ENTREGA,
                    departedAt = "Hoje, agora"
                )
            } else order
        }
    }

    override suspend fun dispatchAllReadyOrders() {
        _activeRouteOrders.value = _activeRouteOrders.value.map { order ->
            if (order.status == OrderDeliveryStatus.PRONTO_PARA_ENTREGA) {
                order.copy(
                    status = OrderDeliveryStatus.SAIU_ENTREGA,
                    departedAt = "Hoje, agora"
                )
            } else order
        }
    }

    override suspend fun completeDelivery(orderId: String, pin: String): Result<Unit> {
        val order = _activeRouteOrders.value.find { it.id == orderId }
            ?: return Result.failure(NoSuchElementException("Pedido não encontrado na rota ativa."))

        // Validação do PIN
        if (pin.trim() != order.customerPin && pin.trim() != "1234" && pin.trim() != "0000") {
            return Result.failure(IllegalArgumentException("PIN incorreto. Solicite o código de 4 dígitos ao cliente."))
        }

        val completedOrder = order.copy(
            status = OrderDeliveryStatus.FINALIZADO,
            deliveredAt = "Hoje, agora"
        )

        // Remove da rota ativa
        _activeRouteOrders.value = _activeRouteOrders.value.filter { it.id != orderId }
        _todayCompletedCount.value += 1

        // Adiciona ao histórico local
        historyRepository.addCompletedOrderToHistory(completedOrder)

        return Result.success(Unit)
    }

    override suspend fun clearOfflineConfirmations() {
        _offlineConfirmations.value = emptyList()
    }

    override suspend fun resetDemoOrders() {
        _availableOrders.value = createInitialAvailableOrders()
        _activeRouteOrders.value = emptyList()
    }

    private fun createInitialAvailableOrders(): List<DeliveryOrder> {
        return listOf(
            DeliveryOrder(
                id = "order_4920",
                shortCode = "#4920",
                store = FakeStoreCentral,
                status = OrderDeliveryStatus.PRONTO_PARA_ENTREGA,
                customerName = "Mariana Silveira",
                customerPhone = "(37) 99123-4567",
                addressStreet = "Rua dos Ipês",
                addressNumber = "240",
                addressNeighborhood = "Jardim América",
                addressCity = "Itaúna",
                addressComplement = "Casa azul, portão branco",
                deliveryFee = 8.50,
                driverEarnings = 7.00,
                estimatedDistanceKm = 3.2,
                estimatedTimeMinutes = 14,
                items = listOf(
                    DeliveryItem("item_1", "Leite Integral Piracanjuba 1L", 6, 4.89),
                    DeliveryItem("item_2", "Café ItaSuper Tradicional 500g", 2, 16.90),
                    DeliveryItem("item_3", "Pão Francês Fresco", 1, 8.50),
                    DeliveryItem("item_4", "Queijo Minas Padrão 500g", 1, 24.90)
                ),
                payment = PaymentSummary(
                    method = "PIX",
                    amount = 84.90,
                    changeFor = null,
                    isPaidOnline = true
                ),
                customerPin = "4821",
                isPinPreFilledAllowed = true,
                stopOrder = 1,
                notes = "Tocar campainha ou buzinar."
            ),
            DeliveryOrder(
                id = "order_4923",
                shortCode = "#4923",
                store = FakeStoreCentral,
                status = OrderDeliveryStatus.PRONTO_PARA_ENTREGA,
                customerName = "Rodrigo Alves",
                customerPhone = "(37) 98844-3322",
                addressStreet = "Av. Getúlio Vargas",
                addressNumber = "780",
                addressNeighborhood = "Vila Rica",
                addressCity = "Itaúna",
                addressComplement = "Apto 302 - Bloco B",
                deliveryFee = 10.00,
                driverEarnings = 8.50,
                estimatedDistanceKm = 4.8,
                estimatedTimeMinutes = 19,
                items = listOf(
                    DeliveryItem("item_5", "Refrigerante Guaraná Antarctica 2L", 2, 8.99),
                    DeliveryItem("item_6", "Carne Bovina Alcatra 1kg", 1, 38.90),
                    DeliveryItem("item_7", "Carvão Vegetal 3kg", 1, 14.50)
                ),
                payment = PaymentSummary(
                    method = "Dinheiro",
                    amount = 71.38,
                    changeFor = 100.00,
                    isPaidOnline = false
                ),
                customerPin = "5544",
                isPinPreFilledAllowed = false,
                stopOrder = 2,
                notes = "Deixar na portaria com o porteiro Silva."
            ),
            DeliveryOrder(
                id = "order_4927",
                shortCode = "#4927",
                store = FakeStoreGracas,
                status = OrderDeliveryStatus.PRONTO_PARA_ENTREGA,
                customerName = "Fernanda Lima",
                customerPhone = "(37) 99911-0022",
                addressStreet = "Rua Tiradentes",
                addressNumber = "88",
                addressNeighborhood = "Centro",
                addressCity = "Itaúna",
                addressComplement = "Casa de fundos",
                deliveryFee = 7.00,
                driverEarnings = 6.00,
                estimatedDistanceKm = 1.9,
                estimatedTimeMinutes = 9,
                items = listOf(
                    DeliveryItem("item_8", "Detergente Ypê Neutro 500ml", 3, 2.49),
                    DeliveryItem("item_9", "Amaciante Downy 1L", 1, 19.90),
                    DeliveryItem("item_10", "Sabão em Pó Omo 1.6kg", 1, 22.90)
                ),
                payment = PaymentSummary(
                    method = "Cartão na Entrega",
                    amount = 50.27,
                    changeFor = null,
                    isPaidOnline = false
                ),
                customerPin = "7890",
                isPinPreFilledAllowed = false,
                stopOrder = 3,
                notes = "Levar máquina de cartão Stone."
            )
        )
    }
}

/**
 * Repositório Fake de Histórico de Corridas
 */
class FakeDriverHistoryRepository : DriverHistoryRepository {
    private val _history = MutableStateFlow(createInitialHistory())

    override fun getHistory(filter: String): Flow<List<DriverHistoryEntry>> =
        _history.map { list ->
            when (filter) {
                "7 dias" -> list.take(6)
                "30 dias" -> list.take(12)
                else -> list
            }
        }

    override fun getHistorySummary(filter: String): Flow<DriverHistorySummary> =
        _history.map { list ->
            val filtered = when (filter) {
                "7 dias" -> list.take(6)
                "30 dias" -> list.take(12)
                else -> list
            }
            DriverHistorySummary(
                totalDistanceKm = filtered.sumOf { it.distanceKm },
                totalTimeMinutes = filtered.sumOf { it.timeMinutes },
                totalRides = filtered.size,
                totalEarnings = filtered.sumOf { it.driverEarnings },
                periodFilter = filter
            )
        }

    override suspend fun addCompletedOrderToHistory(order: DeliveryOrder) {
        val entry = DriverHistoryEntry(
            id = "hist_${System.currentTimeMillis()}",
            orderShortCode = order.shortCode,
            storeName = order.store.tradeName,
            dateFormatted = "Hoje",
            timeFormatted = "Agora",
            neighborhood = order.addressNeighborhood,
            addressSummary = order.shortAddress,
            distanceKm = order.estimatedDistanceKm,
            timeMinutes = order.estimatedTimeMinutes,
            deliveryFee = order.deliveryFee,
            driverEarnings = order.driverEarnings,
            statusText = "Recebido",
            isStraightLineEstimate = true
        )
        _history.value = listOf(entry) + _history.value
    }

    private fun createInitialHistory(): List<DriverHistoryEntry> {
        return listOf(
            DriverHistoryEntry(
                id = "h_1",
                orderShortCode = "#4918",
                storeName = "ItaSuper - Matriz Centro",
                dateFormatted = "Hoje",
                timeFormatted = "13:40",
                neighborhood = "Santana",
                addressSummary = "Rua Gonçalves da Silva, 310",
                distanceKm = 2.7,
                timeMinutes = 11,
                deliveryFee = 8.00,
                driverEarnings = 6.80,
                statusText = "Recebido"
            ),
            DriverHistoryEntry(
                id = "h_2",
                orderShortCode = "#4915",
                storeName = "ItaSuper - Matriz Centro",
                dateFormatted = "Hoje",
                timeFormatted = "12:55",
                neighborhood = "Garcias",
                addressSummary = "Rua Antônio Corradi, 520",
                distanceKm = 4.1,
                timeMinutes = 16,
                deliveryFee = 9.50,
                driverEarnings = 8.00,
                statusText = "Recebido"
            ),
            DriverHistoryEntry(
                id = "h_3",
                orderShortCode = "#4909",
                storeName = "ItaSuper - Loja Graças",
                dateFormatted = "Hoje",
                timeFormatted = "11:30",
                neighborhood = "Parque Jardim",
                addressSummary = "Av. Jove Soares, 1020",
                distanceKm = 3.5,
                timeMinutes = 14,
                deliveryFee = 8.50,
                driverEarnings = 7.20,
                statusText = "Recebido"
            ),
            DriverHistoryEntry(
                id = "h_4",
                orderShortCode = "#4898",
                storeName = "ItaSuper - Matriz Centro",
                dateFormatted = "Ontem",
                timeFormatted = "19:10",
                neighborhood = "Piedade",
                addressSummary = "Rua Silva Jardim, 90",
                distanceKm = 2.1,
                timeMinutes = 9,
                deliveryFee = 7.50,
                driverEarnings = 6.50,
                statusText = "Recebido"
            ),
            DriverHistoryEntry(
                id = "h_5",
                orderShortCode = "#4890",
                storeName = "ItaSuper - Matriz Centro",
                dateFormatted = "Ontem",
                timeFormatted = "17:45",
                neighborhood = "Morada Nova",
                addressSummary = "Rua Vicente Nogueira, 400",
                distanceKm = 5.3,
                timeMinutes = 21,
                deliveryFee = 11.00,
                driverEarnings = 9.50,
                statusText = "Recebido"
            ),
            DriverHistoryEntry(
                id = "h_6",
                orderShortCode = "#4882",
                storeName = "ItaSuper - Loja Graças",
                dateFormatted = "12/08",
                timeFormatted = "15:20",
                neighborhood = "Lourdes",
                addressSummary = "Rua Zezé Lima, 145",
                distanceKm = 3.0,
                timeMinutes = 12,
                deliveryFee = 8.00,
                driverEarnings = 7.00,
                statusText = "Recebido"
            ),
            DriverHistoryEntry(
                id = "h_7",
                orderShortCode = "#4875",
                storeName = "ItaSuper - Matriz Centro",
                dateFormatted = "11/08",
                timeFormatted = "14:10",
                neighborhood = "Vila Mozart",
                addressSummary = "Rua Mozart Silva, 22",
                distanceKm = 3.8,
                timeMinutes = 15,
                deliveryFee = 9.00,
                driverEarnings = 7.80,
                statusText = "Recebido"
            ),
            DriverHistoryEntry(
                id = "h_8",
                orderShortCode = "#4860",
                storeName = "ItaSuper - Matriz Centro",
                dateFormatted = "10/08",
                timeFormatted = "18:30",
                neighborhood = "Aeroporto",
                addressSummary = "Av. Gabriel da Silva, 800",
                distanceKm = 6.2,
                timeMinutes = 24,
                deliveryFee = 13.00,
                driverEarnings = 11.00,
                statusText = "Recebido"
            )
        )
    }
}

/**
 * Repositório Fake de Navegação e Rota Otimizada
 */
class FakeDriverLocationRepository : DriverLocationRepository {
    private val _optimizedRouteEnabled = MutableStateFlow(true)
    private val _navigationPreference = MutableStateFlow(NavigationPreference.GOOGLE_MAPS)

    override fun getOptimizedRouteEnabled(): Flow<Boolean> = _optimizedRouteEnabled.asStateFlow()

    override suspend fun setOptimizedRouteEnabled(enabled: Boolean) {
        _optimizedRouteEnabled.value = enabled
    }

    override fun getNavigationPreference(): Flow<NavigationPreference> = _navigationPreference.asStateFlow()

    override suspend fun setNavigationPreference(pref: NavigationPreference) {
        _navigationPreference.value = pref
    }
}

/**
 * Repositório Fake de Notificações e Suporte
 */
class FakeDriverNotificationsRepository : DriverNotificationsRepository {
    private val _connectivityBannerVisible = MutableStateFlow(true)
    private val _tickets = MutableStateFlow(
        listOf(
            SupportTicket(
                id = "ticket_101",
                subject = "Dúvida sobre taxa de entrega da Loja Graças",
                category = "Valores e Taxas",
                description = "Gostaria de verificar a taxa base aplicada no pedido #4909.",
                status = "Respondido",
                createdAt = "Ontem, 16:20"
            )
        )
    )

    override fun getConnectivityBannerVisible(): Flow<Boolean> = _connectivityBannerVisible.asStateFlow()

    override suspend fun dismissConnectivityBanner() {
        _connectivityBannerVisible.value = false
    }

    override fun getSupportTickets(): Flow<List<SupportTicket>> = _tickets.asStateFlow()

    override suspend fun createSupportTicket(
        subject: String,
        category: String,
        description: String
    ): Result<SupportTicket> {
        val newTicket = SupportTicket(
            id = "ticket_${System.currentTimeMillis()}",
            subject = subject.ifBlank { "Chamado geral do entregador" },
            category = category,
            description = description,
            status = "Em análise",
            createdAt = "Hoje, agora"
        )
        _tickets.value = listOf(newTicket) + _tickets.value
        return Result.success(newTicket)
    }
}

/**
 * Service Locator simples para injeção limpa de repositórios
 */
object AppContainer {
    val authRepository: AuthRepository by lazy { FakeAuthRepository() }
    val profileRepository: DriverProfileRepository by lazy { FakeDriverProfileRepository() }
    val linkRepository: DriverLinkRepository by lazy { FakeDriverLinkRepository() }
    val availabilityRepository: DriverAvailabilityRepository by lazy { FakeDriverAvailabilityRepository() }
    val historyRepository: DriverHistoryRepository by lazy { FakeDriverHistoryRepository() }
    val ordersRepository: DriverOrdersRepository by lazy { FakeDriverOrdersRepository(historyRepository) }
    val locationRepository: DriverLocationRepository by lazy { FakeDriverLocationRepository() }
    val notificationsRepository: DriverNotificationsRepository by lazy { FakeDriverNotificationsRepository() }
}
