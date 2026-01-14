package com.example.buteykoexercises.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.buteykoexercises.data.local.dao.ControlPauseDao
import com.example.buteykoexercises.data.local.dao.ExerciseDao
import com.example.buteykoexercises.data.local.entity.ControlPauseEntity
import com.example.buteykoexercises.data.local.entity.ExerciseLoopEntity
import com.example.buteykoexercises.data.local.entity.SessionEntity

@Database(
    entities = [ControlPauseEntity::class, SessionEntity::class, ExerciseLoopEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun controlPauseDao(): ControlPauseDao
    abstract fun exerciseDao(): ExerciseDao
}
