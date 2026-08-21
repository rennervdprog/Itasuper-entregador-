package com.example.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.fake.AppContainer
import com.example.data.model.SupportTicket
import com.example.data.repository.DriverNotificationsRepository
import com.example.ui.common.DriverUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SupportUiState(
    val isNewTicketDialogOpen: Boolean = false,
    val subject: String = "",
    val category: String = "Dúvida sobre taxa de entrega",
    val description: String = "",
    val feedbackMessage: String? = null,
    val isSubmitting: Boolean = false
)

class SupportViewModel(
    private val notificationsRepository: DriverNotificationsRepository = AppContainer.notificationsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupportUiState())
    val uiState: StateFlow<SupportUiState> = _uiState.asStateFlow()

    val tickets: StateFlow<List<SupportTicket>> = notificationsRepository.getSupportTickets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun openNewTicketDialog() {
        _uiState.value = _uiState.value.copy(
            isNewTicketDialogOpen = true,
            subject = "",
            category = "Dúvida sobre taxa de entrega",
            description = "",
            feedbackMessage = null
        )
    }

    fun closeNewTicketDialog() {
        _uiState.value = _uiState.value.copy(isNewTicketDialogOpen = false)
    }

    fun onSubjectChange(subject: String) {
        _uiState.value = _uiState.value.copy(subject = subject)
    }

    fun onCategoryChange(category: String) {
        _uiState.value = _uiState.value.copy(category = category)
    }

    fun onDescriptionChange(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun submitTicket() {
        val state = _uiState.value
        if (state.description.isBlank()) {
            _uiState.value = state.copy(feedbackMessage = "Por favor, descreva sua solicitação.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, feedbackMessage = null)
            val result = notificationsRepository.createSupportTicket(
                subject = state.subject.ifBlank { state.category },
                category = state.category,
                description = state.description
            )
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    isNewTicketDialogOpen = false,
                    feedbackMessage = "Chamado de suporte enviado com sucesso!"
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    feedbackMessage = DriverUserMessage.support(error)
                )
            }
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }
}
