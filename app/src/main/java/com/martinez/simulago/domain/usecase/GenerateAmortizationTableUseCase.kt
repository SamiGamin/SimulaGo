package com.martinez.simulago.domain.usecase

import com.martinez.simulago.domain.model.AmortizationEntry
import javax.inject.Inject
import kotlin.math.pow

class GenerateAmortizationTableUseCase @Inject constructor() {

    // Usamos el operador 'invoke' para que la clase se pueda llamar como si fuera una función
    operator fun invoke(
        principal: Double,
        monthlyInterestRateDecimal: Double,
        termInMonths: Int,
        monthlyPayment: Double
    ): List<AmortizationEntry> {
        val table = mutableListOf<AmortizationEntry>()
        var currentBalance = principal

        for (month in 1..termInMonths) {
            val interestForMonth = currentBalance * monthlyInterestRateDecimal

            // Lógica para ajustar el último pago y evitar saldos negativos
            val principalForMonth = if (month == termInMonths) {
                currentBalance // En el último mes, el capital es todo lo que queda
            } else {
                monthlyPayment - interestForMonth
            }

            val finalBalance = currentBalance - principalForMonth
            val finalMonthlyPayment = if (month == termInMonths) {
                currentBalance + interestForMonth // El último pago es el saldo + el último interés
            } else {
                monthlyPayment
            }


            table.add(
                AmortizationEntry(
                    monthNumber = month,
                    monthlyPayment = finalMonthlyPayment,
                    principalPaid = principalForMonth,
                    interestPaid = interestForMonth,
                    remainingBalance = if (finalBalance <= 0.01) 0.0 else finalBalance
                )
            )
            currentBalance = finalBalance
            if (currentBalance <= 0) break // Salir si el balance ya es cero
        }
        return table
    }
}