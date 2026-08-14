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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OrdersUiState(
    val selectedStoreFilterId: String? = null, // null means "All"
    val activeTabSegment: Int = 0, // 0 = Entregas, 1 = Histórico
    val orderToConfirmPin: DeliveryOrder? = null,
    val enteredPin: String = "",
    val pinErrorMessage: String? = null,
    val feedbackMessage: String? = null,
    val simulatedNavToastMessage: String? = null,
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
            DriverProfile("driver_01", "Carlos Eduardo", "carlos.motoboy@itasuper.com.br", "(37) 99842-7711")
        )

    val availability: StateFlow<DriverAvailability> = availabilityRepository.getAvailability()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DriverAvailability(true, "Recebendo pedidos · toque para pausar")
        )

    val linkedStores: StateFlow<List<StoreDriverLink>> = linkRepository.getLinks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableOrders: StateFlow<List<DeliveryOrder>> = ordersRepository.getAvailableOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeRouteOrders: StateFlow<List<DeliveryOrder>> = ordersRepository.getActiveRouteOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayCompletedCount: StateFlow<Int> = ordersRepository.getTodayCompletedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 12)

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
                _uiState.value = _uiState.value.copy(feedbackMessage = error.message)
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
                _uiState.value = _uiState.value.copy(feedbackMessage = "Pedido aceito e adicionado à rota.")
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(feedbackMessage = error.message)
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
                _uiState.value = _uiState.value.copy(feedbackMessage = "${orderIds.size} pedidos aceitos para a rota.")
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(feedbackMessage = error.message)
            }
        }
    }

    fun rejectOrder(orderId: String) {
        viewModelScope.launch {
            ordersRepository.rejectOrder(orderId)
            _uiState.value = _uiState.value.copy(feedbackMessage = "Pedido recusado localmente.")
        }
    }

    fun dispatchOrder(orderId: String) {
        viewModelScope.launch {
            ordersRepository.dispatchOrder(orderId)
            _uiState.value = _uiState.value.copy(feedbackMessage = "Status atualizado: Saiu para entrega.")
        }
    }

    fun dispatchAllReady() {
        viewModelScope.launch {
            ordersRepository.dispatchAllReadyOrders()
            _uiState.value = _uiState.value.copy(feedbackMessage = "Todos os pedidos saíram para entrega.")
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
                _uiState.value = _uiState.value.copy(pinErrorMessage = error.message)
            }
        }
    }

    fun simulateExternalNavigation(appName: String, address: String) {
        _uiState.value = _uiState.value.copy(
            simulatedNavToastMessage = "Navegação simulada: Abrindo destino no $appName ($address)"
        )
    }

    fun simulateContactAction(type: String, contact: String) {
        _uiState.value = _uiState.value.copy(
            simulatedNavToastMessage = "Ação simulada: Abrindo conversa no $type com $contact"
        )
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(
            feedbackMessage = null,
            simulatedNavToastMessage = null
        )
    }

    fun resetDemoData() {
        viewModelScope.launch {
            ordersRepository.resetDemoOrders()
            _uiState.value = _uiState.value.copy(feedbackMessage = "Dados de demonstração restaurados.")
        }
    }
}
