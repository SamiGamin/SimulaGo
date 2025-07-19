package com.martinez.simulago.domain.model

import com.martinez.simulago.data.local.SavedSimulation
import com.martinez.simulago.data.remote.UpdateInfo

data class HomeUiState(
    val allSimulations: List<SavedSimulation> = emptyList(),
    val activeCredit: SavedSimulation? = null,
    val showOnboardingCard: Boolean = true,
    val isInSelectionMode: Boolean = false,
    val updateAvailableInfo: UpdateInfo? = null
)
