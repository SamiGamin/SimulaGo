package com.martinez.simulago.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SimulationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSimulation(simulation: SavedSimulation)

    @Query("SELECT * FROM simulations ORDER BY createdAt DESC")
    fun getAllSimulations(): Flow<List<SavedSimulation>>

    @Query("DELETE FROM simulations WHERE id = :id")
    suspend fun deleteSimulationById(id: Int)

    @Query("UPDATE simulations SET isActiveCredit = 0")
    suspend fun deactivateAllCredits()

    @Query("UPDATE simulations SET isActiveCredit = 1 WHERE id = :id")
    suspend fun activateCreditById(id: Int)

    @Transaction
    suspend fun setActiveCredit(id: Int) {
        deactivateAllCredits()
        activateCreditById(id)
    }

    @Query("SELECT * FROM simulations WHERE isActiveCredit = 1 LIMIT 1")
    fun getActiveCredit(): Flow<SavedSimulation?>
}