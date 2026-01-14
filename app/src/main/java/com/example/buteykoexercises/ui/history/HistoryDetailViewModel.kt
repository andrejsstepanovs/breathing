package com.example.buteykoexercises.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.buteykoexercises.data.local.dao.ControlPauseDao
import com.example.buteykoexercises.data.local.dao.ExerciseDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DetailUiState {
    data object Loading : DetailUiState()
    data class CpDetail(val record: com.example.buteykoexercises.data.local.entity.ControlPauseEntity) : DetailUiState()
    data class SessionDetail(val session: ExerciseDao.SessionWithLoops) : DetailUiState()
    data object Error : DetailUiState()
}

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cpDao: ControlPauseDao,
    private val exerciseDao: ExerciseDao
) : ViewModel() {

    // Arguments from Navigation
    private val type = savedStateHandle.get<String>("type") // "CP" or "SESSION"
    private val id = savedStateHandle.get<Long>("id")

    private val _state = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val state = _state.asStateFlow()

    // One-time event to tell UI to go back
    private val _deletedEvent = Channel<Unit>()
    val deletedEvent = _deletedEvent.receiveAsFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            if (id == null || type == null) {
                _state.value = DetailUiState.Error
                return@launch
            }

            if (type == "CP") {
                val record = cpDao.getById(id)
                _state.value = if (record != null) DetailUiState.CpDetail(record) else DetailUiState.Error
            } else {
                val session = exerciseDao.getSessionById(id)
                _state.value = if (session != null) DetailUiState.SessionDetail(session) else DetailUiState.Error
            }
        }
    }

    fun deleteRecord() {
        viewModelScope.launch {
            if (id != null && type != null) {
                if (type == "CP") {
                    cpDao.deleteById(id)
                } else {
                    exerciseDao.deleteSessionById(id)
                }
                _deletedEvent.send(Unit)
            }
        }
    }
}
