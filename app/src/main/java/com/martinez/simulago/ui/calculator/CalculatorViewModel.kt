package com.martinez.simulago.ui.calculator

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.martinez.simulago.data.local.SavedSimulation
import com.martinez.simulago.data.local.SimulationDao
import com.martinez.simulago.domain.model.AmortizationEntry
import com.martinez.simulago.domain.model.CalculatorUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.lang.Double.max
import javax.inject.Inject
import kotlin.math.pow

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val simulationDao: SimulationDao
) : ViewModel() {
    // _uiState es PRIVADO y Mutable. Solo el ViewModel puede cambiarlo.
    private val _uiState = MutableStateFlow(CalculatorUiState())

    // uiState es PÚBLICO e Inmutable. El Fragment solo puede LEERLO.
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    // --- Funciones para que el Fragment notifique los cambios ---

    fun onVehiclePriceChange(newPrice: Float) {
        _uiState.update { it.copy(vehiclePrice = newPrice, showResults = false) }
        recalculateInRealTime()
    }

    fun onDownPaymentChange(newDownPayment: Float) {
        _uiState.update { it.copy(downPayment = newDownPayment, showResults = false) }
    }

    fun onTermChange(newTerm: Float) {
        _uiState.update { it.copy(loanTermInMonths = newTerm.toInt(), showResults = false) }
    }

    fun onRateChange(newRate: String) {
        _uiState.update { it.copy(interestRate = newRate, showResults = false) }
    }

    // --- La función principal que se llama al pulsar el botón ---
    fun onSimulateClicked() {
        val state = _uiState.value
        val monthlyRatePercent = state.interestRate.toDoubleOrNull()
        if (monthlyRatePercent == null || monthlyRatePercent <= 0) {
            _uiState.update {
                it.copy(
                    error = "La tasa de interés debe ser un número positivo.",
                    showResults = false
                )
            }
            return
        }
        performCalculation(state, monthlyRatePercent, forceShowResults = true)
    }

    private fun recalculateInRealTime() {
        val state = _uiState.value
        val monthlyRatePercent = state.interestRate.toDoubleOrNull()
        if (monthlyRatePercent == null || monthlyRatePercent <= 0) {
            _uiState.update {
                it.copy(
                    monthlyPayment = null,
                    totalInterestPaid = null,
                    totalLoanCost = null
                )
            }
            return
        }
        performCalculation(state, monthlyRatePercent)
    }

    private fun performCalculation(
        state: CalculatorUiState,
        monthlyRatePercent: Double,
        forceShowResults: Boolean = false
    ) {
        val amountToFinance = max(0.0, (state.vehiclePrice - state.downPayment).toDouble())
        if (amountToFinance <= 0) {
            _uiState.update {
                it.copy(
                    monthlyPayment = null,
                    totalInterestPaid = null,
                    totalLoanCost = null
                )
            }
            return
        }

        // **LÓGICA CORRECTA BASADA EN TASA MENSUAL**
        val monthlyRateDecimal = monthlyRatePercent / 100
        val numberOfMonths = state.loanTermInMonths.toDouble()

        val monthlyPayment = if (monthlyRateDecimal > 0) {
            val powerTerm = (1.0 + monthlyRateDecimal).pow(numberOfMonths)
            amountToFinance * (monthlyRateDecimal * powerTerm) / (powerTerm - 1.0)
        } else {
            amountToFinance / numberOfMonths
        }

        val table = generateAmortizationTable(
            amountToFinance,
            monthlyRatePercent / 100,
            state.loanTermInMonths,
            monthlyPayment
        )

        val totalLoanCost = monthlyPayment * numberOfMonths
        val totalInterestPaid = totalLoanCost - amountToFinance

        _uiState.update {
            it.copy(
                loanAmountToFinance = amountToFinance,
                monthlyPayment = monthlyPayment,
                totalInterestPaid = totalInterestPaid,
                totalLoanCost = totalLoanCost,
                showResults = if (forceShowResults) true else it.showResults,
                amortizationTable = table,
                error = null
            )
        }
    }

    private fun generateAmortizationTable(
        principal: Double,
        monthlyRateDecimal: Double,
        termInMonths: Int,
        monthlyPayment: Double
    ): List<AmortizationEntry> {
        val table = mutableListOf<AmortizationEntry>()
        var currentBalance = principal

        for (month in 1..termInMonths) {
            val interestForMonth = currentBalance * monthlyRateDecimal
            val principalForMonth = monthlyPayment - interestForMonth
            val finalBalance = currentBalance - principalForMonth

            table.add(
                AmortizationEntry(
                    monthNumber = month,
                    monthlyPayment = monthlyPayment,
                    principalPaid = principalForMonth,
                    interestPaid = interestForMonth,
                    remainingBalance = if (finalBalance < 0) 0.0 else finalBalance // Evitar saldos negativos en la última cuota
                )
            )
            currentBalance = finalBalance
        }
        return table
    }

    fun onSaveSimulationClicked(simulationName: String) {
        val currentState = _uiState.value

        // Solo guardar si hay un resultado válido
        if (currentState.monthlyPayment == null || currentState.monthlyPayment <= 0) {
            _uiState.update { it.copy(error = "No hay una simulación válida para guardar.") }
            return
        }

        val simulationToSave = SavedSimulation(
            simulationName = simulationName,
            vehiclePrice = currentState.vehiclePrice,
            downPayment = currentState.downPayment,
            loanTermInMonths = currentState.loanTermInMonths,
            interestRate = currentState.interestRate,
            loanAmountToFinance = currentState.loanAmountToFinance!!,
            monthlyPayment = currentState.monthlyPayment,
            totalInterestPaid = currentState.totalInterestPaid!!,
            totalLoanCost = currentState.totalLoanCost!!
        )

        viewModelScope.launch {
            try {
                simulationDao.insertSimulation(simulationToSave)

                Log.d("DB_SAVE_SUCCESS", "Simulación guardada con éxito: $simulationToSave")
                _uiState.update { it.copy(showSaveSuccessMessage = true) }
            } catch (e: Exception) {
                // --- ¡AQUÍ ESTÁ TU LOG DE ERROR! ---
                Log.e("DB_SAVE_ERROR", "Error al guardar la simulación: ${e.message}", e)

                // Opcional: actualiza el estado para mostrar un error de guardado
                _uiState.update { it.copy(error = "No se pudo guardar la simulación.") }
            }
        }
        fun onSaveSuccessMessageShown() {
            _uiState.update { it.copy(showSaveSuccessMessage = false) }
        }
    }
}