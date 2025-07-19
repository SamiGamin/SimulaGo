package com.martinez.simulago.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "simulations")
data class SavedSimulation(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val simulationName: String,
    val vehiclePrice: Float,
    val downPayment: Float,
    val loanTermInMonths: Int,
    val interestRate: String,
    val loanAmountToFinance: Double,
    val monthlyPayment: Double,
    val totalInterestPaid: Double,
    val totalLoanCost: Double,
    val createdAt: Date = Date(),
    val isActiveCredit: Boolean = false,
//    val startDate: Date? = null,
//    val additionalCharges: Double = 0.0
)