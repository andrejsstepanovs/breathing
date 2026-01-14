package com.example.buteykoexercises.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.buteykoexercises.data.local.dao.ExerciseDao
import com.example.buteykoexercises.data.local.entity.ExerciseLoopEntity
import com.example.buteykoexercises.data.local.entity.SessionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val dao: ExerciseDao
) : ViewModel() {

    private val _state = MutableStateFlow(ExerciseUiState())
    val state: StateFlow<ExerciseUiState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var startTimeMillis: Long = 0

    // --- State 1: IDLE / START SESSION ---
    fun startSession() {
        viewModelScope.launch {
            val sessionId = dao.insertSession(
                SessionEntity(timestamp = System.currentTimeMillis())
            )
            _state.update {
                it.copy(
                    currentSessionId = sessionId,
                    step = ExerciseStep.PreCheckCp,
                    cpTimerSeconds = 0f,
                    isTimerRunning = false,
                    completedLoops = emptyList() // Reset history
                )
            }
        }
    }

    // ... (toggleCpTimer, startCpTimer, stopCpTimer remain largely the same) ...

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
        _state.update { it.copy(isTimerRunning = false) }

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
                recoveryTimerSeconds = 30, // Standard 30s
                isTimerRunning = true
            )
        }
        startRecoveryCountdown()
    }

    // --- State 4: RECOVERY COUNTDOWN & SKIP ---

    private fun startRecoveryCountdown() {
        timerJob = viewModelScope.launch {
            while (_state.value.recoveryTimerSeconds > 0) {
                delay(1000)
                _state.update { it.copy(recoveryTimerSeconds = it.recoveryTimerSeconds - 1) }
            }
            // Countdown finished naturally
            skipRecovery()
        }
    }

    // NEW: Allow skipping the countdown
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

    // --- SAVE LOGIC ---
    private fun saveLoopData() {
        val currentState = _state.value
        val sessionId = currentState.currentSessionId ?: return

        // 1. Save to DB
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

        // 2. Add to local history list for display
        val newLoop = CompletedLoop(
            initialCp = currentState.initialCp,
            breathingSeconds = currentState.breathingTimerSeconds,
            finalCp = currentState.finalCp
        )

        _state.update {
            it.copy(completedLoops = it.completedLoops + newLoop)
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