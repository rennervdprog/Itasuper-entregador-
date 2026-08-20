package com.example.data.repository

import com.example.data.model.DeliveryOrder
import com.example.data.model.DriverAvailability
import com.example.data.model.DriverHistoryEntry
import com.example.data.model.DriverDirectoryPreference
import com.example.data.model.DriverHistorySummary
import com.example.data.model.DriverProfile
import com.example.data.model.NavigationPreference
import com.example.data.model.OfflineDeliveryConfirmation
import com.example.data.model.StoreDriverLink
import com.example.data.model.SupportTicket
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getCurrentUser(): Flow<DriverProfile?>
    suspend fun login(email: String, password: String): Result<DriverProfile>
    suspend fun registerMotoboy(
        fullName: String,
        document: String,
        vehicle: String,
        whatsapp: String,
        email: String,
        password: String
    ): Result<DriverProfile>
    suspend fun logout()
    suspend fun restoreSession(): Result<DriverProfile?>
    fun isAuthenticated(): Flow<Boolean>
}

interface DriverProfileRepository {
    fun getProfile(): Flow<DriverProfile>
    suspend fun updateProfile(profile: DriverProfile)
}

/** Preferência voluntária de divulgação do próprio motoboy na base por cidade. */
interface DriverDirectoryRepository {
    fun observeMyPreference(): Flow<DriverDirectoryPreference>
    suspend fun savePreference(city: String, isListed: Boolean): Result<DriverDirectoryPreference>
}

interface DriverLinkRepository {
    fun getLinks(): Flow<List<StoreDriverLink>>
    fun getHasAcceptedLink(): Flow<Boolean>
    suspend fun acceptInvite(linkId: String)
    suspend fun rejectInvite(linkId: String)
    suspend fun checkNewInvites()
}

interface DriverAvailabilityRepository {
    fun getAvailability(): Flow<DriverAvailability>
    suspend fun setOnline(online: Boolean, hasActiveDeliveries: Boolean): Result<Unit>
    suspend fun restoreOnlinePresence(): Result<Unit>
    suspend fun setRouteActive(active: Boolean): Result<Unit>
    fun onAppForeground()
    fun onAppBackground()
    suspend fun prepareForLogout(): Result<Unit>
}

interface DriverOrdersRepository {
    fun getAvailableOrders(): Flow<List<DeliveryOrder>>
    fun getActiveRouteOrders(): Flow<List<DeliveryOrder>>
    fun getTodayCompletedCount(): Flow<Int>
    suspend fun acceptOrder(orderId: String): Result<Unit>
    suspend fun acceptAllOrders(orderIds: List<String>): Result<Unit>
    suspend fun dispatchOrder(orderId: String)
    suspend fun dispatchAllReadyOrders()
    suspend fun completeDelivery(orderId: String, pin: String): Result<Unit>
    fun getOfflineConfirmations(): Flow<List<OfflineDeliveryConfirmation>>
    suspend fun clearOfflineConfirmations()
    suspend fun refreshOrders()
}

interface DriverHistoryRepository {
    fun getHistory(filter: String): Flow<List<DriverHistoryEntry>>
    fun getHistorySummary(filter: String): Flow<DriverHistorySummary>
    suspend fun addCompletedOrderToHistory(order: DeliveryOrder)
}

interface DriverLocationRepository {
    fun getOptimizedRouteEnabled(): Flow<Boolean>
    suspend fun setOptimizedRouteEnabled(enabled: Boolean)
    fun getNavigationPreference(): Flow<NavigationPreference>
    suspend fun setNavigationPreference(pref: NavigationPreference)
}

interface DriverNotificationsRepository {
    fun getConnectivityBannerVisible(): Flow<Boolean>
    suspend fun dismissConnectivityBanner()
    fun getSupportTickets(): Flow<List<SupportTicket>>
    suspend fun createSupportTicket(subject: String, category: String, description: String): Result<SupportTicket>
}
