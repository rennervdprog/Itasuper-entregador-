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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isHelpDialogOpen: Boolean = false
)

data class MotoboyRegistrationUiState(
    val fullName: String = "",
    val document: String = "",
    val vehicle: String = "",
    val whatsapp: String = "",
    val email: String = "",
    val password: String = "",
    val passwordConfirmation: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository = AppContainer.authRepository,
    private val linkRepository: DriverLinkRepository = AppContainer.linkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _registrationUiState = MutableStateFlow(MotoboyRegistrationUiState())
    val registrationUiState: StateFlow<MotoboyRegistrationUiState> = _registrationUiState.asStateFlow()

    val currentUser: StateFlow<DriverProfile?> = authRepository.getCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val hasAcceptedLink: StateFlow<Boolean> = linkRepository.getHasAcceptedLink()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email, errorMessage = null)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password, errorMessage = null)
    }

    fun setHelpDialogOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isHelpDialogOpen = open)
    }

    fun onRegistrationChange(
        fullName: String? = null,
        document: String? = null,
        vehicle: String? = null,
        whatsapp: String? = null,
        email: String? = null,
        password: String? = null,
        passwordConfirmation: String? = null
    ) {
        val current = _registrationUiState.value
        _registrationUiState.value = current.copy(
            fullName = fullName ?: current.fullName,
            document = document ?: current.document,
            vehicle = vehicle ?: current.vehicle,
            whatsapp = whatsapp ?: current.whatsapp,
            email = email ?: current.email,
            password = password ?: current.password,
            passwordConfirmation = passwordConfirmation ?: current.passwordConfirmation,
            errorMessage = null
        )
    }

    fun registerMotoboy(onSuccess: (profile: DriverProfile, hasAcceptedLink: Boolean) -> Unit) {
        val state = _registrationUiState.value
        when {
            state.fullName.trim().length < 3 -> registrationError("Informe seu nome completo.")
            state.document.filter(Char::isDigit).length !in 11..14 -> registrationError("Informe um CPF válido.")
            state.vehicle.trim().length < 3 -> registrationError("Informe o modelo do veículo.")
            state.whatsapp.filter(Char::isDigit).length !in 10..13 -> registrationError("Informe um WhatsApp válido com DDD.")
            !state.email.trim().contains("@") -> registrationError("Informe um e-mail válido.")
            state.password.length < 6 -> registrationError("A senha deve conter pelo menos 6 caracteres.")
            state.password != state.passwordConfirmation -> registrationError("As senhas não coincidem.")
            else -> viewModelScope.launch {
                _registrationUiState.value = state.copy(isLoading = true, errorMessage = null)
                val result = authRepository.registerMotoboy(
                    fullName = state.fullName,
                    document = state.document,
                    vehicle = state.vehicle,
                    whatsapp = state.whatsapp,
                    email = state.email,
                    password = state.password
                )
                if (result.isSuccess) {
                    val hasLink = runCatching {
                        linkRepository.getLinks().first().any { it.status.name == "ACCEPTED" }
                    }.getOrDefault(false)
                    _registrationUiState.value = MotoboyRegistrationUiState()
                    onSuccess(result.getOrThrow(), hasLink)
                } else {
                    _registrationUiState.value = _registrationUiState.value.copy(
                        isLoading = false,
                        errorMessage = registrationErrorMessage(result.exceptionOrNull())
                    )
                }
            }
        }
    }

    private fun registrationError(message: String) {
        _registrationUiState.value = _registrationUiState.value.copy(errorMessage = message)
    }

    fun loginWithBiometrics(onSuccess: (profile: DriverProfile, hasAcceptedLink: Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.restoreSession()
            val profile = result.getOrNull()
            if (profile != null) {
                val hasLink = runCatching {
                    linkRepository.getLinks().first().any { it.status.name == "ACCEPTED" }
                }.getOrDefault(false)
                _uiState.value = _uiState.value.copy(isLoading = false)
                onSuccess(profile, hasLink)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Sua sessão não está mais disponível. Entre com e-mail e senha."
                )
            }
        }
    }

    fun login(onSuccess: (profile: DriverProfile, hasAcceptedLink: Boolean) -> Unit) {
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
            if (result.isSuccess) {
                // Espelha o Capacitor: somente decide a rota depois de terminar a
                // primeira consulta real de store_drivers.
                val hasLink = runCatching {
                    linkRepository.getLinks().first().any { it.status.name == "ACCEPTED" }
                }.getOrDefault(false)
                _uiState.value = _uiState.value.copy(isLoading = false)
                onSuccess(result.getOrThrow(), hasLink)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = loginErrorMessage(result.exceptionOrNull())
                )
            }
        }
    }

    private fun registrationErrorMessage(error: Throwable?): String {
        val detail = error?.message.orEmpty().lowercase()
        return when {
            detail.contains("already registered") || detail.contains("already been registered") || detail.contains("user already registered") ->
                "Este e-mail já possui uma conta. Entre com sua senha."
            detail.contains("email not confirmed") || detail.contains("email_not_confirmed") ->
                "Confirme seu e-mail e entre novamente para concluir o cadastro."
            detail.contains("network") || detail.contains("timeout") || detail.contains("connection") ->
                "Não foi possível concluir o cadastro agora. Verifique sua internet."
            error?.message?.isNotBlank() == true -> error.message.orEmpty()
            else -> "Não foi possível concluir o cadastro agora. Tente novamente."
        }
    }

    private fun loginErrorMessage(error: Throwable?): String {
        val detail = error?.message.orEmpty().lowercase()
        return when {
            detail.contains("invalid_credentials") || detail.contains("invalid login credentials") ->
                "E-mail ou senha incorretos. Confira os dados e tente novamente."
            detail.contains("email not confirmed") || detail.contains("email_not_confirmed") ->
                "Confirme seu e-mail antes de entrar no aplicativo."
            detail.contains("network") || detail.contains("timeout") || detail.contains("connection") ->
                "Não foi possível conectar agora. Verifique sua internet e tente novamente."
            else -> "Não foi possível entrar agora. Tente novamente em instantes."
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
