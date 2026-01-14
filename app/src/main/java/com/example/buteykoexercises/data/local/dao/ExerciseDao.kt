package com.example.buteykoexercises.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.buteykoexercises.data.local.entity.ExerciseLoopEntity
import com.example.buteykoexercises.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Query("UPDATE exercise_sessions SET note = :note WHERE id = :sessionId")
    suspend fun updateSessionNote(sessionId: Long, note: String)

    @Insert
    suspend fun insertLoop(loop: ExerciseLoopEntity)

    data class SessionWithLoops(
        @androidx.room.Embedded val session: SessionEntity,
        @androidx.room.Relation(
            parentColumn = "id",
            entityColumn = "sessionId"
        )
        val loops: List<ExerciseLoopEntity>
    )

    @Transaction
    @Query("SELECT * FROM exercise_sessions ORDER BY timestamp DESC")
    fun getHistory(): Flow<List<SessionWithLoops>>

    @Transaction
    @Query("SELECT * FROM exercise_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): SessionWithLoops?

    @Query("DELETE FROM exercise_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)
}
