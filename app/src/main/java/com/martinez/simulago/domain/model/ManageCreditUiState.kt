package com.martinez.simulago.domain.model

import com.martinez.simulago.data.local.PaymentEntry
import com.martinez.simulago.data.local.SavedSimulation

data class ManageCreditUiState(
    val activeCredit: SavedSimulation? = null,
    val paymentHistory: List<PaymentEntry> = emptyList(),
    val currentBalance: Double = 0.0,
    val isLoading: Boolean = true,
    val error: String? = null // Nuevo campo para mensajes de error
)
