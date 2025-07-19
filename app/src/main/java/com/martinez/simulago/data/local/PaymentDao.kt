package com.martinez.simulago.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Insert
    suspend fun insertPayment(payment: PaymentEntry)

    @Query("SELECT * FROM payments WHERE simulationId = :simulationId ORDER BY paymentDate DESC")
    fun getPaymentsForSimulation(simulationId: Int): Flow<List<PaymentEntry>>
}