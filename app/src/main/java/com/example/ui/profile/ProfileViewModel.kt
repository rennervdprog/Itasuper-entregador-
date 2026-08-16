package com.example.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.fake.AppContainer
import com.example.data.model.DriverProfile
import com.example.data.model.NavigationPreference
import com.example.data.model.StoreDriverLink
import com.example.data.repository.AuthRepository
import com.example.data.repository.DriverLinkRepository
import com.example.data.repository.DriverLocationRepository
import com.example.data.repository.DriverProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileRepository: DriverProfileRepository = AppContainer.profileRepository,
    private val linkRepository: DriverLinkRepository = AppContainer.linkRepository,
    private val locationRepository: DriverLocationRepository = AppContainer.locationRepository,
    private val authRepository: AuthRepository = AppContainer.authRepository
) : ViewModel() {

    val profile: StateFlow<DriverProfile> = profileRepository.getProfile()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DriverProfile(
                id = "",
                name = "Entregador",
                email = "",
                phone = ""
            )
        )

    val linkedStores: StateFlow<List<StoreDriverLink>> = linkRepository.getLinks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val navPreference: StateFlow<NavigationPreference> = locationRepository.getNavigationPreference()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NavigationPreference.GOOGLE_MAPS)

    fun setNavPreference(preference: NavigationPreference) {
        viewModelScope.launch {
            locationRepository.setNavigationPreference(preference)
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}
