package com.martinez.simulago.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.martinez.simulago.data.local.SavedSimulation
import com.martinez.simulago.data.local.SimulationDao
import com.martinez.simulago.domain.model.AmortizationEntry
import com.martinez.simulago.domain.model.HomeUiState
import com.martinez.simulago.domain.usecase.GenerateAmortizationTableUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val simulationDao: SimulationDao,
    private val generateAmortizationTableUseCase: GenerateAmortizationTableUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    init {
        // Combinamos los flows de la DB para actualizar la parte de los datos
        viewModelScope.launch {
            combine(
                simulationDao.getAllSimulations(),
                simulationDao.getActiveCredit()
            ) { allSimulations, activeCredit ->
                // Creamos un estado de datos parcial
                Pair(allSimulations, activeCredit)
            }.collect { (allSimulations, activeCredit) ->
                // Actualizamos el _uiState con los nuevos datos, pero preservando el modo selección
                _uiState.update { currentState ->
                    currentState.copy(
                        allSimulations = allSimulations,
                        activeCredit = activeCredit,
                        showOnboardingCard = activeCredit == null && allSimulations.isNotEmpty()
                    )
                }
            }
        }
    }
    fun enterSelectionMode() {
        _uiState.update { it.copy(isInSelectionMode = true) }
    }

    fun exitSelectionMode() {
        _uiState.update { it.copy(isInSelectionMode = false) }
    }
    fun onSetAsActiveCreditClicked(simulation: SavedSimulation) {
        viewModelScope.launch {
            simulationDao.setActiveCredit(simulation.id)
        }
    }

    fun deleteSimulation(simulation: SavedSimulation) {
        viewModelScope.launch {
            simulationDao.deleteSimulationById(simulation.id)
        }
    }

    fun getAmortizationTableForSimulation(simulation: SavedSimulation): List<AmortizationEntry> {

        val monthlyRateDecimal = simulation.interestRate.toDoubleOrNull()?.div(100) ?: 0.0

        return generateAmortizationTableUseCase(
            principal = simulation.loanAmountToFinance,
            monthlyInterestRateDecimal = monthlyRateDecimal,
            termInMonths = simulation.loanTermInMonths,
            monthlyPayment = simulation.monthlyPayment
        )
    }
}