package com.example.buteykoexercises.ui.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.buteykoexercises.data.local.dao.ControlPauseDao
import com.example.buteykoexercises.data.local.entity.ControlPauseEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ChartsViewModel @Inject constructor(
    dao: ControlPauseDao
) : ViewModel() {

    // Expose the raw list. The UI will handle scaling/drawing.
    val dataPoints: StateFlow<List<ControlPauseEntity>> = dao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
