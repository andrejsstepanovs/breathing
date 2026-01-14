package com.example.buteykoexercises.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.buteykoexercises.data.local.entity.ControlPauseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ControlPauseDao {
    @Insert
    suspend fun insert(cp: ControlPauseEntity)

    @Query("DELETE FROM standalone_cp WHERE id = (SELECT MAX(id) FROM standalone_cp)")
    suspend fun deleteLastRecord()

    // Modified: Get only last 3 for the UI snippet
    @Query("SELECT * FROM standalone_cp ORDER BY timestamp DESC LIMIT 3")
    fun getRecentThree(): Flow<List<ControlPauseEntity>>

    // New: Get the single best record
    @Query("SELECT * FROM standalone_cp ORDER BY durationSeconds DESC LIMIT 1")
    fun getBestRecord(): Flow<ControlPauseEntity?>

    // Kept for HistoryViewModel (returns all)
    @Query("SELECT * FROM standalone_cp ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ControlPauseEntity>>

    @Query("SELECT * FROM standalone_cp WHERE id = :id")
    suspend fun getById(id: Long): ControlPauseEntity?

    @Query("DELETE FROM standalone_cp WHERE id = :id")
    suspend fun deleteById(id: Long)
}