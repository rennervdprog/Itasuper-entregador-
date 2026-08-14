package com.example.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.fake.AppContainer
import com.example.data.model.DriverProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.DriverLinkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "carlos.motoboy@itasuper.com.br",
    val password: String = "123456",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isHelpDialogOpen: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository = AppContainer.authRepository,
    private val linkRepository: DriverLinkRepository = AppContainer.linkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<DriverProfile?> = authRepository.getCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val hasAcceptedLink: StateFlow<Boolean> = linkRepository.getHasAcceptedLink()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email, errorMessage = null)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password, errorMessage = null)
    }

    fun setHelpDialogOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isHelpDialogOpen = open)
    }

    fun login(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.email.isBlank() || !state.email.contains("@")) {
            _uiState.value = state.copy(errorMessage = "Informe um e-mail válido.")
            return
        }
        if (state.password.length < 4) {
            _uiState.value = state.copy(errorMessage = "A senha deve conter no mínimo 4 caracteres.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.login(state.email, state.password)
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.onSuccess {
                onSuccess()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "Erro ao entrar.")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
