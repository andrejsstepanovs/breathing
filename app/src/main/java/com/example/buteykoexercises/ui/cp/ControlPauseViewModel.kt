package com.example.buteykoexercises.ui.cp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.buteykoexercises.data.local.dao.ControlPauseDao
import com.example.buteykoexercises.data.local.entity.ControlPauseEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ControlPauseState(
    val isRunning: Boolean = false,
    val elapsedSeconds: Float = 0.0f,
    // Data for the bottom list
    val recentRecords: List<ControlPauseEntity> = emptyList(),
    val bestRecord: ControlPauseEntity? = null,
    // UI flag: Only show delete if user just finished a session
    val canDeleteLast: Boolean = false
)

@HiltViewModel
class ControlPauseViewModel @Inject constructor(
    private val dao: ControlPauseDao
) : ViewModel() {

    private val _state = MutableStateFlow(ControlPauseState())
    val state: StateFlow<ControlPauseState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var startTimeMillis: Long = 0

    init {
        // Observe DB changes for "Last 3" and "Best"
        viewModelScope.launch {
            combine(
                dao.getRecentThree(),
                dao.getBestRecord()
            ) { recent, best ->
                Pair(recent, best)
            }.collect { (recent, best) ->
                _state.update {
                    it.copy(
                        recentRecords = recent,
                        bestRecord = best
                    )
                }
            }
        }
    }

    fun toggleTimer() {
        if (_state.value.isRunning) {
            stopTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        startTimeMillis = System.currentTimeMillis()
        timerJob?.cancel()

        // Reset delete flag when starting new run
        _state.update {
            it.copy(
                isRunning = true,
                elapsedSeconds = 0f,
                canDeleteLast = false
            )
        }

        timerJob = viewModelScope.launch {
            while (true) {
                val current = System.currentTimeMillis()
                val diff = (current - startTimeMillis) / 1000f
                _state.update { it.copy(elapsedSeconds = diff) }
                delay(50)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        val finalDuration = _state.value.elapsedSeconds

        // Timer stopped: Enable delete button
        _state.update {
            it.copy(
                isRunning = false,
                canDeleteLast = true
            )
        }
        saveRecord(finalDuration)
    }

    private fun saveRecord(duration: Float) {
        viewModelScope.launch {
            dao.insert(
                ControlPauseEntity(
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = duration
                )
            )
        }
    }

    fun reset() {
        timerJob?.cancel()
        _state.update {
            it.copy(
                isRunning = false,
                elapsedSeconds = 0f,
                canDeleteLast = false // Hide delete button on reset
            )
        }
    }

    fun deleteLastRecord() {
        viewModelScope.launch {
            dao.deleteLastRecord()
            // Immediately hide the button so they can't double-click/delete older records
            _state.update { it.copy(canDeleteLast = false, elapsedSeconds = 0f) }
        }
    }
}