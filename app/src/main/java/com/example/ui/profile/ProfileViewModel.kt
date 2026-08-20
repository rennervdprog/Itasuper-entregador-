package com.example.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.fake.AppContainer
import com.example.data.model.DriverDirectoryPreference
import com.example.data.model.DriverProfile
import com.example.data.model.NavigationPreference
import com.example.data.model.StoreDriverLink
import com.example.data.repository.AuthRepository
import com.example.data.repository.DriverDirectoryRepository
import com.example.data.repository.DriverLinkRepository
import com.example.data.repository.DriverLocationRepository
import com.example.data.repository.DriverProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileRepository: DriverProfileRepository = AppContainer.profileRepository,
    private val directoryRepository: DriverDirectoryRepository = AppContainer.directoryRepository,
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

    private val _directoryPreference = MutableStateFlow(DriverDirectoryPreference())
    val directoryPreference: StateFlow<DriverDirectoryPreference> = _directoryPreference.asStateFlow()

    private val _isSavingDirectoryPreference = MutableStateFlow(false)
    val isSavingDirectoryPreference: StateFlow<Boolean> = _isSavingDirectoryPreference.asStateFlow()

    private val _directoryPreferenceMessage = MutableStateFlow<String?>(null)
    val directoryPreferenceMessage: StateFlow<String?> = _directoryPreferenceMessage.asStateFlow()

    private val _isDirectoryPreferenceError = MutableStateFlow<Boolean?>(null)
    val isDirectoryPreferenceError: StateFlow<Boolean?> = _isDirectoryPreferenceError.asStateFlow()

    init {
        viewModelScope.launch {
            directoryRepository.observeMyPreference().collect { preference ->
                _directoryPreference.value = preference
            }
        }
    }

    fun setNavPreference(preference: NavigationPreference) {
        viewModelScope.launch {
            locationRepository.setNavigationPreference(preference)
        }
    }

    fun saveDirectoryPreference(
        city: String,
        isListed: Boolean,
        hasContactConsent: Boolean,
        isCityConfirmed: Boolean
    ) {
        val normalizedCity = city.trim().replace(Regex("\\s+"), " ")
        when {
            normalizedCity.length < 2 -> {
                _isDirectoryPreferenceError.value = true
                _directoryPreferenceMessage.value = "Informe uma cidade de atuação com pelo menos 2 caracteres."
                return
            }
            !isCityConfirmed -> {
                _isDirectoryPreferenceError.value = true
                _directoryPreferenceMessage.value = "Selecione uma cidade válida na lista de municípios."
                return
            }
            isListed && !hasContactConsent -> {
                _isDirectoryPreferenceError.value = true
                _directoryPreferenceMessage.value = "Autorize o contato dos lojistas para aparecer na base."
                return
            }
        }

        viewModelScope.launch {
            _isSavingDirectoryPreference.value = true
            _directoryPreferenceMessage.value = null
            _isDirectoryPreferenceError.value = null
            directoryRepository.savePreference(normalizedCity, isListed)
                .onSuccess { preference ->
                    _directoryPreference.value = preference
                    _isDirectoryPreferenceError.value = false
                    _directoryPreferenceMessage.value = if (isListed) {
                        "Preferência salva. Você está visível para lojistas da sua cidade."
                    } else {
                        "Preferência salva. Você não aparece mais na base de motoboys."
                    }
                }
                .onFailure { error ->
                    _isDirectoryPreferenceError.value = true
                    _directoryPreferenceMessage.value = error.message
                        ?: "Não foi possível salvar sua preferência agora. Tente novamente."
                }
            _isSavingDirectoryPreference.value = false
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}
