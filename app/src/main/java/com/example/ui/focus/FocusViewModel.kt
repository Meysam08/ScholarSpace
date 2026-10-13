package com.example.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CollectionEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.TaskEntity
import com.example.data.repository.FocusRepository
import com.example.data.repository.LibraryRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TaskRepository
import com.example.timer.PomodoroManager
import com.example.timer.PomodoroUiState
import com.example.timer.SessionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FocusScreenState(
    val timer: PomodoroUiState = PomodoroUiState(),
    val recentSessions: List<FocusSessionEntity> = emptyList(),
    val totalFocusSeconds: Long = 0L,
    val completedCount: Int = 0,
    val incompleteTasks: List<TaskEntity> = emptyList(),
    val collections: List<CollectionEntity> = emptyList(),
    val focusDurationMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15
)

class FocusViewModel(
    private val pomodoroManager: PomodoroManager,
    private val focusRepository: FocusRepository,
    private val settingsRepository: SettingsRepository,
    private val taskRepository: TaskRepository,
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    private val _focusMinutes = MutableStateFlow(25)
    private val _shortBreakMinutes = MutableStateFlow(5)
    private val _longBreakMinutes = MutableStateFlow(15)

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                if (settings != null) {
                    _focusMinutes.value = settings.focusDurationMinutes
                    _shortBreakMinutes.value = settings.shortBreakMinutes
                    _longBreakMinutes.value = settings.longBreakMinutes
                }
            }
        }
    }

    private val statsFlow = combine(
        focusRepository.getRecentCompletedSessions(15),
        focusRepository.getTotalFocusDurationSeconds(),
        focusRepository.getCompletedFocusSessionsCount()
    ) { recent, totalSec, compCount ->
        Triple(recent, totalSec ?: 0L, compCount)
    }

    private val associationsFlow = combine(
        taskRepository.getIncompleteTasks(),
        libraryRepository.getAllCollections()
    ) { tasks, cols ->
        Pair(tasks, cols)
    }

    val screenState: StateFlow<FocusScreenState> = combine(
        pomodoroManager.uiState,
        statsFlow,
        associationsFlow
    ) { timer, statsTriple, assocPair ->
        val (recent, totalSec, compCount) = statsTriple
        val (tasks, cols) = assocPair
        FocusScreenState(
            timer = timer,
            recentSessions = recent,
            totalFocusSeconds = totalSec,
            completedCount = compCount,
            incompleteTasks = tasks,
            collections = cols,
            focusDurationMinutes = _focusMinutes.value,
            shortBreakMinutes = _shortBreakMinutes.value,
            longBreakMinutes = _longBreakMinutes.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FocusScreenState()
    )

    fun startTimer() {
        val currentType = pomodoroManager.uiState.value.sessionType
        val minutes = when (currentType) {
            SessionType.FOCUS -> _focusMinutes.value
            SessionType.SHORT_BREAK -> _shortBreakMinutes.value
            SessionType.LONG_BREAK -> _longBreakMinutes.value
        }
        pomodoroManager.startTimer(type = currentType, durationMinutes = minutes)
    }

    fun pauseTimer() {
        pomodoroManager.pauseTimer()
    }

    fun resumeTimer() {
        pomodoroManager.resumeTimer()
    }

    fun resetTimer() {
        val currentType = pomodoroManager.uiState.value.sessionType
        val minutes = when (currentType) {
            SessionType.FOCUS -> _focusMinutes.value
            SessionType.SHORT_BREAK -> _shortBreakMinutes.value
            SessionType.LONG_BREAK -> _longBreakMinutes.value
        }
        pomodoroManager.resetTimer(defaultMinutes = minutes)
    }

    fun setSessionType(type: SessionType) {
        val minutes = when (type) {
            SessionType.FOCUS -> _focusMinutes.value
            SessionType.SHORT_BREAK -> _shortBreakMinutes.value
            SessionType.LONG_BREAK -> _longBreakMinutes.value
        }
        pomodoroManager.setSessionType(type, minutes)
    }

    fun associateTask(task: TaskEntity?) {
        pomodoroManager.associateTask(task?.id, task?.title)
    }

    fun associateCollection(collection: CollectionEntity?) {
        pomodoroManager.associateCollection(collection?.id, collection?.name)
    }

    class Factory(
        private val pomodoroManager: PomodoroManager,
        private val focusRepository: FocusRepository,
        private val settingsRepository: SettingsRepository,
        private val taskRepository: TaskRepository,
        private val libraryRepository: LibraryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FocusViewModel(
                pomodoroManager,
                focusRepository,
                settingsRepository,
                taskRepository,
                libraryRepository
            ) as T
        }
    }
}
