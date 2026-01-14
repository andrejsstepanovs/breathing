package com.example.buteykoexercises.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.buteykoexercises.data.local.dao.ControlPauseDao
import com.example.buteykoexercises.data.local.dao.ExerciseDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// Polymorphic list item
sealed class HistoryItem {
    abstract val timestamp: Long
    abstract val id: Long // --- ADDED THIS ABSTRACT VAL ---

    data class StandaloneCp(
        override val timestamp: Long,
        override val id: Long,
        val duration: Float
    ) : HistoryItem()

    data class Session(
        override val timestamp: Long,
        override val id: Long, // --- ADDED THIS OVERRIDE ---
        val note: String?,
        val loops: List<LoopSummary>
    ) : HistoryItem()
}

data class LoopSummary(
    val initialCp: Float,
    val breatheTime: Long,
    val finalCp: Float
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    cpDao: ControlPauseDao,
    exerciseDao: ExerciseDao
) : ViewModel() {

    val historyItems: StateFlow<List<HistoryItem>> = combine(
        cpDao.getAll(),
        exerciseDao.getHistory()
    ) { cps, sessions ->

        val cpItems = cps.map {
            HistoryItem.StandaloneCp(it.timestamp, it.id, it.durationSeconds)
        }

        val sessionItems = sessions.map { complex ->
            HistoryItem.Session(
                timestamp = complex.session.timestamp,
                id = complex.session.id,
                note = complex.session.note,
                loops = complex.loops.map {
                    LoopSummary(it.initialCp, it.breathingDurationSeconds, it.finalCp)
                }
            )
        }

        // Merge and Sort DESC
        (cpItems + sessionItems).sortedByDescending { it.timestamp }

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}

object DateFormatter {
    private val pattern = SimpleDateFormat("EEE, MMM d • HH:mm", Locale.getDefault())
    fun format(timestamp: Long): String = pattern.format(Date(timestamp))
}