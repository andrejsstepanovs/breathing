package com.example.buteykoexercises.ui.exercise

sealed class ExerciseStep {
    data object Idle : ExerciseStep()
    data object PreCheckCp : ExerciseStep()
    data object Breathing : ExerciseStep()
    data object Paused : ExerciseStep()
    data object RecoveryCountdown : ExerciseStep()
    data object PostCheckCp : ExerciseStep()
    data object Summary : ExerciseStep()
}

// Data class to hold info about previous loops in this session
data class CompletedLoop(
    val initialCp: Float,
    val breathingSeconds: Long,
    val finalCp: Float
)

data class ExerciseUiState(
    val step: ExerciseStep = ExerciseStep.Idle,

    // Timer values
    val cpTimerSeconds: Float = 0f,
    val breathingTimerSeconds: Long = 0,
    val recoveryTimerSeconds: Int = 30,

    // Data collected
    val currentSessionId: Long? = null,
    val initialCp: Float = 0f,
    val finalCp: Float = 0f,

    // History within the current session
    val completedLoops: List<CompletedLoop> = emptyList(),

    // Suggestion for skipping
    val lastKnownCp: Float? = null,

    val isTimerRunning: Boolean = false
)