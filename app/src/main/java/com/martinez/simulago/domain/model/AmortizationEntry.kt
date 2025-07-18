package com.martinez.simulago.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
@Parcelize
data class AmortizationEntry(
    val monthNumber: Int,
    val monthlyPayment: Double,
    val principalPaid: Double,
    val interestPaid: Double,
    val remainingBalance: Double
): Parcelable
