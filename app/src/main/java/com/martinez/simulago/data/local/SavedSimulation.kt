package com.martinez.simulago.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "simulations")
data class SavedSimulation(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val simulationName: String, // Un nombre para que el usuario la identifique
    val vehiclePrice: Float,
    val downPayment: Float,
    val loanTermInMonths: Int,
    val interestRate: String,
    val loanAmountToFinance: Double,
    val monthlyPayment: Double,
    val totalInterestPaid: Double,
    val totalLoanCost: Double,
    val createdAt: Date = Date() // Para poder ordenarlas por fecha
)