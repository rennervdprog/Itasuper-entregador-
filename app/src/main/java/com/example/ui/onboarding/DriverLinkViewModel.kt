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
    /** `true` para aceite e `false` para recusa; cada convite é tratado isoladamente. */
    val processingInviteActions: Map<String, Boolean> = emptyMap(),
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
        if (!startInviteAction(linkId, isAccepting = true)) return
        viewModelScope.launch {
            try {
                linkRepository.acceptInvite(linkId)
                inviteRefreshVersion.update { it + 1 }
                _uiState.update {
                    it.copy(feedbackMessage = "Convite aceito. Esta loja já pode enviar pedidos para você.")
                }
                onAccepted()
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(feedbackMessage = safeInviteError(error, "Não foi possível aceitar o convite."))
                }
            } finally {
                finishInviteAction(linkId)
            }
        }
    }

    fun rejectInvite(linkId: String) {
        if (!startInviteAction(linkId, isAccepting = false)) return
        viewModelScope.launch {
            try {
                linkRepository.rejectInvite(linkId)
                inviteRefreshVersion.update { it + 1 }
                _uiState.update { it.copy(feedbackMessage = "Convite recusado.") }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(feedbackMessage = safeInviteError(error, "Não foi possível recusar o convite."))
                }
            } finally {
                finishInviteAction(linkId)
            }
        }
    }

    private fun startInviteAction(linkId: String, isAccepting: Boolean): Boolean {
        var started = false
        _uiState.update { state ->
            if (state.processingInviteActions.containsKey(linkId)) {
                state
            } else {
                started = true
                state.copy(
                    processingInviteActions = state.processingInviteActions + (linkId to isAccepting),
                    feedbackMessage = null
                )
            }
        }
        return started
    }

    private fun finishInviteAction(linkId: String) {
        _uiState.update { state ->
            state.copy(processingInviteActions = state.processingInviteActions - linkId)
        }
    }

    private fun safeInviteError(error: Throwable, fallback: String): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("Sessão expirada", ignoreCase = true) -> "Sua sessão expirou. Entre novamente para responder ao convite."
            else -> fallback
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
                        feedbackMessage = safeInviteError(error, "Não foi possível atualizar os convites.")
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
