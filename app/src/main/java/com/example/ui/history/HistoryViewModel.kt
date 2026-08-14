package com.example.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.fake.AppContainer
import com.example.data.model.DriverHistoryEntry
import com.example.data.model.DriverHistorySummary
import com.example.data.repository.DriverHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(
    private val historyRepository: DriverHistoryRepository = AppContainer.historyRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow("7 dias")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    val historyEntries: StateFlow<List<DriverHistoryEntry>> = _selectedFilter
        .flatMapLatest { filter ->
            historyRepository.getHistory(filter)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historySummary: StateFlow<DriverHistorySummary> = _selectedFilter
        .flatMapLatest { filter ->
            historyRepository.getHistorySummary(filter)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DriverHistorySummary(0.0, 0, 0, 0.0)
        )

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }
}
