package com.martinez.simulago.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SimulationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSimulation(simulation: SavedSimulation)

    @Query("SELECT * FROM simulations ORDER BY createdAt DESC")
    fun getAllSimulations(): Flow<List<SavedSimulation>>

    @Query("DELETE FROM simulations WHERE id = :id")
    suspend fun deleteSimulationById(id: Int)
}