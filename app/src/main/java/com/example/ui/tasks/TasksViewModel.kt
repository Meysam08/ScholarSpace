package com.example.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CollectionEntity
import com.example.data.model.TaskEntity
import com.example.data.repository.LibraryRepository
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TaskFilter(val title: String) {
    MY_DAY("My Day"),
    PLANNED("Planned"),
    ALL("All Tasks"),
    COMPLETED("Completed")
}

data class TasksUiState(
    val currentFilter: TaskFilter = TaskFilter.MY_DAY,
    val tasks: List<TaskEntity> = emptyList(),
    val collections: List<CollectionEntity> = emptyList(),
    val incompleteCount: Int = 0,
    val myDayPendingCount: Int = 0,
    val selectedTask: TaskEntity? = null,
    val isCreatingTask: Boolean = false,
    val userMessage: String? = null
)

class TasksViewModel(
    private val taskRepository: TaskRepository,
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    private val _currentFilter = MutableStateFlow(TaskFilter.MY_DAY)
    val currentFilter: StateFlow<TaskFilter> = _currentFilter.asStateFlow()

    private val _selectedTask = MutableStateFlow<TaskEntity?>(null)
    val selectedTask: StateFlow<TaskEntity?> = _selectedTask.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val filterStateFlow = combine(_currentFilter, _selectedTask, _userMessage) { filter, selected, msg ->
        Triple(filter, selected, msg)
    }

    private val countsStateFlow = combine(
        libraryRepository.getAllCollections(),
        taskRepository.getIncompleteCount(),
        taskRepository.getMyDayPendingCount()
    ) { allCols, incompCount, myDayCount ->
        Triple(allCols, incompCount, myDayCount)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TasksUiState> = combine(filterStateFlow, countsStateFlow) { fTriple, cTriple ->
        val (filter, selected, msg) = fTriple
        val (allCols, incompCount, myDayCount) = cTriple
        FilterContext(filter, selected, msg, allCols, incompCount, myDayCount)
    }.flatMapLatest { ctx ->
        val flow = when (ctx.filter) {
            TaskFilter.MY_DAY -> taskRepository.getMyDayTasks()
            TaskFilter.PLANNED -> taskRepository.getPlannedTasks()
            TaskFilter.ALL -> taskRepository.getAllTasks()
            TaskFilter.COMPLETED -> taskRepository.getCompletedTasks()
        }
        kotlinx.coroutines.flow.flow {
            flow.collect { tasksList ->
                emit(
                    TasksUiState(
                        currentFilter = ctx.filter,
                        tasks = tasksList,
                        collections = ctx.allCols,
                        incompleteCount = ctx.incompCount,
                        myDayPendingCount = ctx.myDayCount,
                        selectedTask = ctx.selected,
                        userMessage = ctx.msg
                    )
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TasksUiState()
    )

    private data class FilterContext(
        val filter: TaskFilter,
        val selected: TaskEntity?,
        val msg: String?,
        val allCols: List<CollectionEntity>,
        val incompCount: Int,
        val myDayCount: Int
    )

    fun setFilter(filter: TaskFilter) {
        _currentFilter.value = filter
    }

    fun selectTask(task: TaskEntity?) {
        _selectedTask.value = task
    }

    fun createTask(
        title: String,
        notes: String? = null,
        dueDate: Long? = null,
        isMyDay: Boolean = (_currentFilter.value == TaskFilter.MY_DAY),
        collectionId: String? = null
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            try {
                taskRepository.createTask(
                    title = title,
                    notes = notes,
                    dueDate = dueDate,
                    isMyDay = isMyDay,
                    collectionId = collectionId
                )
                _userMessage.value = "Task created"
            } catch (e: Exception) {
                _userMessage.value = "Failed to create task: ${e.message}"
            }
        }
    }

    fun toggleCompleted(task: TaskEntity) {
        viewModelScope.launch {
            try {
                taskRepository.toggleCompleted(task)
            } catch (e: Exception) {
                _userMessage.value = "Error updating task"
            }
        }
    }

    fun toggleMyDay(task: TaskEntity) {
        viewModelScope.launch {
            try {
                taskRepository.toggleMyDay(task)
            } catch (e: Exception) {
                _userMessage.value = "Error updating My Day"
            }
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            try {
                taskRepository.updateTask(task)
                _selectedTask.value = null
                _userMessage.value = "Task updated"
            } catch (e: Exception) {
                _userMessage.value = "Error saving task"
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                taskRepository.deleteTask(taskId)
                _selectedTask.value = null
                _userMessage.value = "Task deleted"
            } catch (e: Exception) {
                _userMessage.value = "Error deleting task"
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    class Factory(
        private val taskRepository: TaskRepository,
        private val libraryRepository: LibraryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TasksViewModel(taskRepository, libraryRepository) as T
        }
    }
}
