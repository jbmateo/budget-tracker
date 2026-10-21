package app.budgettracker.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.budgettracker.domain.usecase.Dashboard
import app.budgettracker.domain.usecase.ObserveDashboard
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** [dashboard] is null until the first load completes. */
data class HomeUiState(val dashboard: Dashboard?)

class HomeViewModel(observeDashboard: ObserveDashboard) : ViewModel() {
    val state: StateFlow<HomeUiState> = observeDashboard()
        .map { HomeUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(null))
}
