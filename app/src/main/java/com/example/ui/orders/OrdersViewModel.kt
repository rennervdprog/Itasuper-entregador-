package com.example.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.fake.AppContainer
import com.example.data.model.DeliveryOrder
import com.example.data.model.DriverAvailability
import com.example.data.model.DriverProfile
import com.example.data.model.NavigationPreference
import com.example.data.model.OfflineDeliveryConfirmation
import com.example.data.model.StoreDriverLink
import com.example.data.repository.DriverAvailabilityRepository
import com.example.data.repository.DriverLinkRepository
import com.example.data.repository.DriverLocationRepository
import com.example.data.repository.DriverNotificationsRepository
import com.example.data.repository.DriverOrdersRepository
import com.example.data.repository.DriverProfileRepository
import com.example.ui.common.DriverUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OrdersUiState(
    val selectedStoreFilterId: String? = null, // null means "All"
    val activeTabSegment: Int = 0, // 0 = Entregas, 1 = Histórico
    val orderToConfirmPin: DeliveryOrder? = null,
    val enteredPin: String = "",
    val pinErrorMessage: String? = null,
    val feedbackMessage: String? = null,
    val isCompletingDelivery: Boolean = false
)

class OrdersViewModel(
    private val ordersRepository: DriverOrdersRepository = AppContainer.ordersRepository,
    private val availabilityRepository: DriverAvailabilityRepository = AppContainer.availabilityRepository,
    private val linkRepository: DriverLinkRepository = AppContainer.linkRepository,
    private val locationRepository: DriverLocationRepository = AppContainer.locationRepository,
    private val profileRepository: DriverProfileRepository = AppContainer.profileRepository,
    private val notificationsRepository: DriverNotificationsRepository = AppContainer.notificationsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()


    val profile: StateFlow<DriverProfile> = profileRepository.getProfile()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DriverProfile(id = "", name = "Entregador", email = "", phone = "")
        )

    val availability: StateFlow<DriverAvailability> = availabilityRepository.getAvailability()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DriverAvailability(false, "Você está Offline")
        )

    val linkedStores: StateFlow<List<StoreDriverLink>> = linkRepository.getLinks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableOrders: StateFlow<List<DeliveryOrder>> = ordersRepository.getAvailableOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeRouteOrders: StateFlow<List<DeliveryOrder>> = ordersRepository.getActiveRouteOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayCompletedCount: StateFlow<Int> = ordersRepository.getTodayCompletedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isOptimizedRoute: StateFlow<Boolean> = locationRepository.getOptimizedRouteEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val navPreference: StateFlow<NavigationPreference> = locationRepository.getNavigationPreference()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NavigationPreference.GOOGLE_MAPS)

    val isConnectivityBannerVisible: StateFlow<Boolean> = notificationsRepository.getConnectivityBannerVisible()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val offlineConfirmations: StateFlow<List<OfflineDeliveryConfirmation>> = ordersRepository.getOfflineConfirmations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleAvailability(newOnlineState: Boolean) {
        viewModelScope.launch {
            val hasActive = activeRouteOrders.value.isNotEmpty()
            val result = availabilityRepository.setOnline(newOnlineState, hasActive)
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    feedbackMessage = DriverUserMessage.availability(error)
                )
            }
        }
    }

    fun setStoreFilter(storeId: String?) {
        _uiState.value = _uiState.value.copy(selectedStoreFilterId = storeId)
    }

    fun setTabSegment(segment: Int) {
        _uiState.value = _uiState.value.copy(activeTabSegment = segment)
    }

    fun toggleOptimizedRoute(enabled: Boolean) {
        viewModelScope.launch {
            locationRepository.setOptimizedRouteEnabled(enabled)
        }
    }

    fun setNavPreference(pref: NavigationPreference) {
        viewModelScope.launch {
            locationRepository.setNavigationPreference(pref)
        }
    }

    fun acceptOrder(orderId: String) {
        viewModelScope.launch {
            val result = ordersRepository.acceptOrder(orderId)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(feedbackMessage = "Pedido aceito. Inicie a rota quando estiver pronto para sair.")
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    feedbackMessage = DriverUserMessage.orderAcceptance(error)
                )
            }
        }
    }

    fun acceptAllAvailable() {
        val storeFilter = _uiState.value.selectedStoreFilterId
        val orders = availableOrders.value.filter {
            storeFilter == null || it.store.id == storeFilter
        }
        val orderIds = orders.map { it.id }

        viewModelScope.launch {
            val result = ordersRepository.acceptAllOrders(orderIds)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(feedbackMessage = "${orderIds.size} pedidos aceitos. Inicie a rota quando estiver pronto para sair.")
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    feedbackMessage = DriverUserMessage.orderBatchAcceptance(error)
                )
            }
        }
    }

    fun dispatchOrder(orderId: String) {
        viewModelScope.launch {
            runCatching { ordersRepository.dispatchOrder(orderId) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        feedbackMessage = "Rota iniciada: pedidos em saída para entrega."
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        feedbackMessage = DriverUserMessage.routeStart(error)
                    )
                }
        }
    }

    fun dispatchAllReady() {
        viewModelScope.launch {
            runCatching { ordersRepository.dispatchAllReadyOrders() }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        feedbackMessage = "Todos os pedidos da rota saíram para entrega."
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        feedbackMessage = DriverUserMessage.routeStart(error)
                    )
                }
        }
    }

    fun openPinConfirmDialog(order: DeliveryOrder) {
        _uiState.value = _uiState.value.copy(
            orderToConfirmPin = order,
            enteredPin = "",
            pinErrorMessage = null
        )
    }

    fun closePinConfirmDialog() {
        _uiState.value = _uiState.value.copy(
            orderToConfirmPin = null,
            enteredPin = "",
            pinErrorMessage = null
        )
    }

    fun onPinChange(pin: String) {
        _uiState.value = _uiState.value.copy(enteredPin = pin, pinErrorMessage = null)
    }

    fun confirmDeliveryWithPin() {
        val order = _uiState.value.orderToConfirmPin ?: return
        val pin = _uiState.value.enteredPin

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCompletingDelivery = true)
            val result = ordersRepository.completeDelivery(order.id, pin)
            _uiState.value = _uiState.value.copy(isCompletingDelivery = false)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    orderToConfirmPin = null,
                    enteredPin = "",
                    pinErrorMessage = null,
                    feedbackMessage = "Entrega ${order.shortCode} concluída com sucesso!"
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(pinErrorMessage = friendlyPinError(error))
            }
        }
    }

    /** Nunca exibir a mensagem bruta do Supabase: ela pode conter URL, headers e detalhes internos. */
    private fun friendlyPinError(error: Throwable): String {
        val raw = error.message.orEmpty()
        val remainingAttempts = Regex("(\\d+)\\s+tentativa", RegexOption.IGNORE_CASE)
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)

        val isInvalidPin = raw.contains("P0001", ignoreCase = true) ||
            raw.contains("código inválido", ignoreCase = true) ||
            raw.contains("pin inválido", ignoreCase = true)
        val isBlocked = raw.contains("bloqueado", ignoreCase = true) ||
            raw.contains("limite de tentativas", ignoreCase = true)

        return when {
            isBlocked -> "A validação do PIN foi bloqueada temporariamente após muitas tentativas. Confirme o código com o cliente e aguarde antes de tentar novamente."
            isInvalidPin && remainingAttempts != null -> "PIN inválido. Ainda restam $remainingAttempts tentativa(s). Confirme os 4 dígitos com o cliente."
            isInvalidPin -> "PIN inválido. Confirme os 4 dígitos com o cliente e tente novamente."
            else -> "Não foi possível validar o PIN agora. Confira os 4 dígitos com o cliente e tente novamente."
        }
    }

    fun reportExternalActionFailure(message: String?) {
        _uiState.value = _uiState.value.copy(
            feedbackMessage = DriverUserMessage.externalApp(null)
        )
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }

    /**
     * Recupera pedidos e Realtime depois de uma pausa longa. As tentativas são
     * silenciosas porque a conexão do Supabase pode levar alguns instantes para
     * voltar após o Android colocar o aplicativo em segundo plano.
     */
    suspend fun refreshAfterAppResume() {
        listOf(0L, 1_500L, 4_000L).forEach { waitMillis ->
            if (waitMillis > 0) delay(waitMillis)
            try {
                ordersRepository.refreshAfterAppResume()
                return
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                // Falhas transitórias ao retornar do segundo plano são tentadas novamente.
            }
        }
    }

    fun refreshOrders(silent: Boolean = false) {
        viewModelScope.launch {
            runCatching { ordersRepository.refreshOrders() }
                .onSuccess {
                    if (!silent) {
                        _uiState.value = _uiState.value.copy(feedbackMessage = "Pedidos atualizados.")
                    }
                }
                .onFailure { error ->
                    if (!silent) {
                        _uiState.value = _uiState.value.copy(
                            feedbackMessage = DriverUserMessage.orderRefresh(error)
                        )
                    }
                }
        }
    }
}
