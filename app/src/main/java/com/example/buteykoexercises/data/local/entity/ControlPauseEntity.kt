package com.example.buteykoexercises.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "standalone_cp")
data class ControlPauseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val durationSeconds: Float
)
