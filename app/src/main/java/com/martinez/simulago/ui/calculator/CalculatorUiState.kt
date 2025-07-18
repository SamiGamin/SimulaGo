package com.martinez.simulago.ui.calculator

data class CalculatorUiState(

    // --- Inputs del Usuario ---
    val vehiclePrice: Float = 80_000_000f,      // Nuevo: Precio total del bien (ej. coche)
    val downPayment: Float = 30_000_000f,        // Nuevo: Cuota inicial
    val loanTermInMonths: Int = 72,
    val interestRate: String = "1.8",

    // --- Resultados del Cálculo ---
    val loanAmountToFinance: Double? = null, // Monto a financiar (Precio - Cuota Inicial)
    val monthlyPayment: Double? = null,      // Cuota mensual
    val totalInterestPaid: Double? = null,   // Nuevo: Total de intereses
    val totalLoanCost: Double? = null,       // Nuevo: Costo total (Financiado + Intereses)

    // --- Control de la UI ---
    val showResults: Boolean = false,        // Para mostrar/ocultar el panel de resultados
    val error: String? = null
)
