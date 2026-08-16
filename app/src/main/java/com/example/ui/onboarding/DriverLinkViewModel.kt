package com.example.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val isCheckingInvites: Boolean = false,
    val feedbackMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class DriverLinkViewModel(
    private val linkRepository: DriverLinkRepository = AppContainer.linkRepository,
    private val profileRepository: DriverProfileRepository = AppContainer.profileRepository,
    private val authRepository: AuthRepository = AppContainer.authRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val inviteRefreshVersion = MutableStateFlow(0)

    val profile: StateFlow<DriverProfile> = profileRepository.getProfile()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DriverProfile(id = "", name = "Entregador", email = "", phone = "")
        )

    val links: StateFlow<List<StoreDriverLink>> = inviteRefreshVersion
        .flatMapLatest { linkRepository.getLinks() }
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
            runCatching { linkRepository.checkNewInvites() }
                .onSuccess {
                    inviteRefreshVersion.update { it + 1 }
                    _uiState.value = _uiState.value.copy(
                        isCheckingInvites = false,
                        feedbackMessage = "Convites atualizados."
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isCheckingInvites = false,
                        feedbackMessage = error.message ?: "Não foi possível atualizar os convites."
                    )
                }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}
