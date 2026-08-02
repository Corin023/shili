package com.example.timetracker.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.timetracker.data.Category
import com.example.timetracker.data.TimeRecord
import com.example.timetracker.data.TimeTrackerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

sealed class TimerState {
    data object Idle : TimerState()
    data class Running(
        val recordId: Long,
        val categoryName: String,
        val startTime: Instant
    ) : TimerState()
}

data class TimerUiState(
    val timerState: TimerState = TimerState.Idle,
    val elapsedSeconds: Long = 0,
    val categories: List<Category> = emptyList(),
    val selectedCategory: Category? = null,
    val newCategoryName: String = ""
)

class TimerViewModel(private val repository: TimeTrackerRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    init {
        viewModelScope.launch {
            repository.seedDefaultCategories()
        }

        combine(
            repository.rootCategories,
            repository.allCategories
        ) { roots, all ->
            roots to all
        }.onEach { (roots, all) ->
            _uiState.update { state ->
                val selected = state.selectedCategory ?: roots.firstOrNull()
                state.copy(categories = all, selectedCategory = selected)
            }
        }.launchIn(viewModelScope)
    }

    fun selectCategory(category: Category) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun onNewCategoryNameChange(name: String) {
        _uiState.update { it.copy(newCategoryName = name) }
    }

    fun addCategory(parentId: Long? = null) {
        val name = _uiState.value.newCategoryName.trim()
        if (name.isBlank()) return

        viewModelScope.launch {
            val id = repository.insertCategory(name, parentId)
            val newCategory = repository.getCategoryById(id)
            newCategory?.let {
                _uiState.update { state ->
                    state.copy(
                        selectedCategory = it,
                        newCategoryName = ""
                    )
                }
            }
        }
    }

    fun startTimer() {
        val category = _uiState.value.selectedCategory ?: return
        viewModelScope.launch {
            val id = repository.startRecord(category.id, category.name)
            _uiState.update { state ->
                state.copy(
                    timerState = TimerState.Running(
                        recordId = id,
                        categoryName = category.name,
                        startTime = Instant.now()
                    ),
                    elapsedSeconds = 0
                )
            }
            startTicking()
        }
    }

    fun stopTimer() {
        val state = _uiState.value.timerState
        if (state is TimerState.Running) {
            timerJob?.cancel()
            viewModelScope.launch {
                repository.stopRecord(state.recordId)
                _uiState.update { it.copy(timerState = TimerState.Idle, elapsedSeconds = 0) }
            }
        }
    }

    private fun startTicking() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { state ->
                    val running = state.timerState as? TimerState.Running
                    val elapsed = running?.let {
                        java.time.temporal.ChronoUnit.SECONDS.between(it.startTime, Instant.now())
                    } ?: 0L
                    state.copy(elapsedSeconds = elapsed)
                }
            }
        }
    }

    fun formatElapsed(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    class Factory(private val repository: TimeTrackerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TimerViewModel::class.java)) {
                return TimerViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
