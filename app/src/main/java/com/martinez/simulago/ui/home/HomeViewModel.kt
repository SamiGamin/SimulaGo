package com.martinez.simulago.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.martinez.simulago.BuildConfig
import com.martinez.simulago.data.local.SavedSimulation
import com.martinez.simulago.data.local.SimulationDao
import com.martinez.simulago.data.remote.UpdateApiService
import com.martinez.simulago.domain.model.AmortizationEntry
import com.martinez.simulago.domain.model.HomeUiState
import com.martinez.simulago.domain.usecase.GenerateAmortizationTableUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val simulationDao: SimulationDao,
    private val generateAmortizationTableUseCase: GenerateAmortizationTableUseCase,
    private val updateApiService: UpdateApiService
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // SAMINGAMIN: Tag para logs
    private val TAG = "HomeViewModel_UpdateCheck"

    init {
        viewModelScope.launch {
            combine(
                simulationDao.getAllSimulations(),
                simulationDao.getActiveCredit()
            ) { allSimulations, activeCredit ->
                Pair(allSimulations, activeCredit)
            }.collect { (allSimulations, activeCredit) ->
                _uiState.update { currentState ->
                    currentState.copy(
                        allSimulations = allSimulations,
                        activeCredit = activeCredit,
                        showOnboardingCard = activeCredit == null && allSimulations.isNotEmpty()
                    )
                }
            }
        }
        check_for_updates()
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

    fun check_for_updates() {
        Log.d(TAG, "Iniciando la verificación de actualizaciones...")
        viewModelScope.launch {
            try {
                Log.d(TAG, "Llamando a updateApiService.checkForUpdates()")
                val remoteUpdateInfo = updateApiService.checkForUpdates()
                // Log para la información completa (ya lo tenías, es útil)
                Log.i(TAG, "Información de actualización recibida del servidor: $remoteUpdateInfo")

                // Log específico para la URL de actualización para fácil verificación
                Log.i(TAG, "URL de actualización específica recibida: ${remoteUpdateInfo.updateUrl}")

                val currentVersionCode = BuildConfig.VERSION_CODE
                Log.i(TAG, "VersionCode actual de la aplicación: $currentVersionCode")

                if (remoteUpdateInfo.latestVersionCode > currentVersionCode) {
                    Log.i(TAG, "¡Actualización encontrada! Versión remota: ${remoteUpdateInfo.latestVersionCode}, Versión actual: $currentVersionCode")
                    Log.d(TAG, "Actualizando uiState con la información de la nueva versión.")
                    _uiState.update { it.copy(updateAvailableInfo = remoteUpdateInfo) }
                } else {
                    Log.i(TAG, "No hay actualizaciones disponibles. Versión remota: ${remoteUpdateInfo.latestVersionCode}, Versión actual: $currentVersionCode")
                    // Si no hay actualización o la URL es incorrecta incluso aquí,
                    // podría ser útil limpiar `updateAvailableInfo` para evitar mostrar el diálogo.
                    // Pero primero, confirmemos la URL.
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error durante la verificación de actualizaciones", e)
            }
        }
    }


    fun onUpdateDialogShown() {
        Log.d(TAG, "El diálogo de actualización ha sido mostrado, reseteando updateAvailableInfo.")
        _uiState.update { it.copy(updateAvailableInfo = null) }
    }
}