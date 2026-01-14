package com.example.buteykoexercises.ui.exercise

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.buteykoexercises.data.local.dao.ControlPauseDao
import com.example.buteykoexercises.data.local.dao.ExerciseDao
import com.example.buteykoexercises.data.local.entity.ExerciseLoopEntity
import com.example.buteykoexercises.data.local.entity.SessionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val dao: ExerciseDao,
    private val cpDao: ControlPauseDao,   // <--- NEW: Needed for history
    savedStateHandle: SavedStateHandle    // <--- NEW: Needed for arguments
) : ViewModel() {

    private val _state = MutableStateFlow(ExerciseUiState())
    val state: StateFlow<ExerciseUiState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var startTimeMillis: Long = 0

    init {
        // 1. Handle "Start Exercise" from Control Pause screen (Jump Start)
        val initialCpArg = savedStateHandle.get<Float>("initialCp")
        if (initialCpArg != null && initialCpArg > 0f) {
            startSessionWithPreCheck(initialCpArg)
        }

        // 2. Load the last known CP from history (for the "Use Last" button)
        viewModelScope.launch {
            val lastStandalone = cpDao.getAll().firstOrNull()?.firstOrNull()
            if (lastStandalone != null) {
                _state.update { it.copy(lastKnownCp = lastStandalone.durationSeconds) }
            }
        }
    }

    // --- LOGIC: Jump Start (Skip Pre-Check via Argument) ---
    private fun startSessionWithPreCheck(cpValue: Float) {
        // Update UI immediately
        _state.update {
            it.copy(
                step = ExerciseStep.Breathing,
                initialCp = cpValue,
                cpTimerSeconds = 0f,
                breathingTimerSeconds = 0,
                completedLoops = emptyList(),
                isTimerRunning = true,
                currentSessionId = null,
                lastKnownCp = cpValue
            )
        }

        startBreathingTimer()

        // Create Session in Background
        viewModelScope.launch {
            val sessionId = dao.insertSession(
                SessionEntity(timestamp = System.currentTimeMillis())
            )
            _state.update { it.copy(currentSessionId = sessionId) }
        }
    }

    // --- State 1: IDLE / START SESSION (Standard Flow) ---
    fun startSession() {
        viewModelScope.launch {
            // Refresh last known CP from DB just in case
            val lastStandalone = cpDao.getAll().firstOrNull()?.firstOrNull()

            val sessionId = dao.insertSession(
                SessionEntity(timestamp = System.currentTimeMillis())
            )
            _state.update {
                it.copy(
                    currentSessionId = sessionId,
                    step = ExerciseStep.PreCheckCp,
                    cpTimerSeconds = 0f,
                    isTimerRunning = false,
                    completedLoops = emptyList(),
                    // Set suggestion to last standalone record initially
                    lastKnownCp = lastStandalone?.durationSeconds ?: it.lastKnownCp
                )
            }
        }
    }

    fun abandonCurrentLoop() {
        // 1. Stop any running timers
        timerJob?.cancel()

        val currentSessionId = _state.value.currentSessionId
        val completed = _state.value.completedLoops

        if (completed.isEmpty()) {
            // Case A: User abandoning the FIRST loop (0 completed).
            // Delete the session so we don't save an empty record.
            viewModelScope.launch {
                if (currentSessionId != null) {
                    dao.deleteSessionById(currentSessionId)
                }
                // Reset to Idle
                _state.update { ExerciseUiState() }
            }
        } else {
            // Case B: User abandoning a subsequent loop (e.g., Loop 2).
            // Discard current progress and go back to Summary of the LAST completed loop.
            val lastLoop = completed.last()

            _state.update {
                it.copy(
                    step = ExerciseStep.Summary,
                    isTimerRunning = false,
                    cpTimerSeconds = 0f,
                    breathingTimerSeconds = lastLoop.breathingSeconds, // Show last loop's stats
                    initialCp = lastLoop.initialCp,
                    finalCp = lastLoop.finalCp,
                    // Revert lastKnownCp to the end of the previous loop
                    lastKnownCp = lastLoop.finalCp
                )
            }
        }
    }

    // --- NEW: User clicked "Skip & Use Last" ---
    fun useLastCpForPreCheck() {
        val lastVal = _state.value.lastKnownCp ?: return

        _state.update {
            it.copy(
                initialCp = lastVal,
                step = ExerciseStep.Breathing,
                breathingTimerSeconds = 0,
                isTimerRunning = true
            )
        }
        startBreathingTimer()
    }

    fun toggleCpTimer() {
        if (_state.value.isTimerRunning) {
            stopCpTimer()
        } else {
            startCpTimer()
        }
    }

    private fun startCpTimer() {
        startTimeMillis = System.currentTimeMillis()
        _state.update { it.copy(isTimerRunning = true, cpTimerSeconds = 0f) }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val diff = (System.currentTimeMillis() - startTimeMillis) / 1000f
                _state.update { it.copy(cpTimerSeconds = diff) }
                delay(50)
            }
        }
    }

    private fun stopCpTimer() {
        timerJob?.cancel()
        val duration = _state.value.cpTimerSeconds

        _state.update {
            it.copy(
                isTimerRunning = false,
                lastKnownCp = duration // <--- UPDATE: Update memory with this new measurement
            )
        }

        when (_state.value.step) {
            is ExerciseStep.PreCheckCp -> {
                _state.update {
                    it.copy(
                        initialCp = duration,
                        step = ExerciseStep.Breathing,
                        breathingTimerSeconds = 0,
                        isTimerRunning = true
                    )
                }
                startBreathingTimer()
            }
            is ExerciseStep.PostCheckCp -> {
                _state.update {
                    it.copy(
                        finalCp = duration,
                        step = ExerciseStep.Summary
                    )
                }
                saveLoopData()
            }
            else -> {}
        }
    }

    private fun startBreathingTimer() {
        startTimeMillis = System.currentTimeMillis() - (_state.value.breathingTimerSeconds * 1000)
        _state.update { it.copy(step = ExerciseStep.Breathing, isTimerRunning = true) }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val elapsed = (System.currentTimeMillis() - startTimeMillis) / 1000
                _state.update { it.copy(breathingTimerSeconds = elapsed) }
                delay(200)
            }
        }
    }

    fun pauseBreathing() {
        timerJob?.cancel()
        _state.update { it.copy(step = ExerciseStep.Paused, isTimerRunning = false) }
    }

    fun resumeBreathing() {
        startBreathingTimer()
    }

    fun finishBreathing() {
        timerJob?.cancel()
        _state.update {
            it.copy(
                step = ExerciseStep.RecoveryCountdown,
                recoveryTimerSeconds = 30,
                isTimerRunning = true
            )
        }
        startRecoveryCountdown()
    }

    private fun startRecoveryCountdown() {
        timerJob = viewModelScope.launch {
            while (_state.value.recoveryTimerSeconds > 0) {
                delay(1000)
                _state.update { it.copy(recoveryTimerSeconds = it.recoveryTimerSeconds - 1) }
            }
            skipRecovery()
        }
    }

    fun skipRecovery() {
        timerJob?.cancel()
        _state.update {
            it.copy(
                step = ExerciseStep.PostCheckCp,
                cpTimerSeconds = 0f,
                isTimerRunning = false
            )
        }
    }

    private fun saveLoopData() {
        val currentState = _state.value
        val sessionId = currentState.currentSessionId ?: return

        viewModelScope.launch {
            dao.insertLoop(
                ExerciseLoopEntity(
                    sessionId = sessionId,
                    timestamp = System.currentTimeMillis(),
                    initialCp = currentState.initialCp,
                    breathingDurationSeconds = currentState.breathingTimerSeconds,
                    finalCp = currentState.finalCp
                )
            )
        }

        val newLoop = CompletedLoop(
            initialCp = currentState.initialCp,
            breathingSeconds = currentState.breathingTimerSeconds,
            finalCp = currentState.finalCp
        )

        _state.update {
            it.copy(
                completedLoops = it.completedLoops + newLoop,
                lastKnownCp = currentState.finalCp // <--- UPDATE: The loop's final CP is now the latest
            )
        }
    }

    fun startNextLoop() {
        _state.update {
            it.copy(
                step = ExerciseStep.PreCheckCp,
                cpTimerSeconds = 0f,
                breathingTimerSeconds = 0,
                recoveryTimerSeconds = 30,
                initialCp = 0f,
                finalCp = 0f,
                isTimerRunning = false
                // We do NOT reset lastKnownCp here, so it persists for the next loop's PreCheck
            )
        }
    }

    fun finishSession(comment: String) {
        val sessionId = _state.value.currentSessionId ?: return
        viewModelScope.launch {
            if (comment.isNotBlank()) {
                dao.updateSessionNote(sessionId, comment)
            }
            _state.update { ExerciseUiState() }
        }
    }
}