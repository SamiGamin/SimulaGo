package com.martinez.simulago.ui.calculator

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.lang.Double.max
import kotlin.math.pow

class CalculatorViewModel : ViewModel(){
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
            _uiState.update { it.copy(error = "La tasa de interés debe ser un número positivo.", showResults = false) }
            return
        }
        performCalculation(state, monthlyRatePercent, forceShowResults = true)
    }

    private fun recalculateInRealTime() {
        val state = _uiState.value
        val monthlyRatePercent = state.interestRate.toDoubleOrNull()
        if (monthlyRatePercent == null || monthlyRatePercent <= 0) {
            _uiState.update { it.copy(monthlyPayment = null, totalInterestPaid = null, totalLoanCost = null) }
            return
        }
        performCalculation(state, monthlyRatePercent)
    }

    private fun performCalculation(state: CalculatorUiState, monthlyRatePercent: Double, forceShowResults: Boolean = false) {
        val amountToFinance = max(0.0, (state.vehiclePrice - state.downPayment).toDouble())
        if (amountToFinance <= 0) {
            _uiState.update { it.copy(monthlyPayment = null, totalInterestPaid = null, totalLoanCost = null) }
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

        val totalLoanCost = monthlyPayment * numberOfMonths
        val totalInterestPaid = totalLoanCost - amountToFinance

        _uiState.update {
            it.copy(
                loanAmountToFinance = amountToFinance,
                monthlyPayment = monthlyPayment,
                totalInterestPaid = totalInterestPaid,
                totalLoanCost = totalLoanCost,
                showResults = if (forceShowResults) true else it.showResults,
                error = null
            )
        }
    }
}