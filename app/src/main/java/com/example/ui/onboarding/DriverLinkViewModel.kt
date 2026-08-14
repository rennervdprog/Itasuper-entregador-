package com.example.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.fake.AppContainer
import com.example.data.model.DriverLinkStatus
import com.example.data.model.DriverProfile
import com.example.data.model.StoreDriverLink
import com.example.data.repository.AuthRepository
import com.example.data.repository.DriverLinkRepository
import com.example.data.repository.DriverProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val isCheckingInvites: Boolean = false,
    val feedbackMessage: String? = null
)

class DriverLinkViewModel(
    private val linkRepository: DriverLinkRepository = AppContainer.linkRepository,
    private val profileRepository: DriverProfileRepository = AppContainer.profileRepository,
    private val authRepository: AuthRepository = AppContainer.authRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    val profile: StateFlow<DriverProfile> = profileRepository.getProfile()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DriverProfile(
                id = "driver_01",
                name = "Carlos Eduardo Souza",
                email = "carlos.motoboy@itasuper.com.br",
                phone = "(37) 99842-7711"
            )
        )

    val links: StateFlow<List<StoreDriverLink>> = linkRepository.getLinks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun acceptInvite(linkId: String, onAccepted: () -> Unit) {
        viewModelScope.launch {
            linkRepository.acceptInvite(linkId)
            _uiState.value = _uiState.value.copy(feedbackMessage = "Vínculo aceito com sucesso!")
            onAccepted()
        }
    }

    fun rejectInvite(linkId: String) {
        viewModelScope.launch {
            linkRepository.rejectInvite(linkId)
            _uiState.value = _uiState.value.copy(feedbackMessage = "Convite recusado.")
        }
    }

    fun checkNewInvites() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingInvites = true, feedbackMessage = null)
            kotlinx.coroutines.delay(600) // Simulação rápida de feedback visual
            linkRepository.checkNewInvites()
            _uiState.value = _uiState.value.copy(
                isCheckingInvites = false,
                feedbackMessage = "Convites atualizados."
            )
        }
    }

    fun simulateResetToAguardando() {
        viewModelScope.launch {
            linkRepository.resetToAguardandoVinculo()
        }
    }

    fun simulateAddInvite() {
        viewModelScope.launch {
            linkRepository.simulateAddDemoInvite()
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}
