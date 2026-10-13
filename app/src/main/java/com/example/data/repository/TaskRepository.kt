package com.example.data.repository

import com.example.data.dao.TaskDao
import com.example.data.model.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

class TaskRepository(private val taskDao: TaskDao) {

    fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()
    fun getIncompleteTasks(): Flow<List<TaskEntity>> = taskDao.getIncompleteTasks()
    fun getCompletedTasks(): Flow<List<TaskEntity>> = taskDao.getCompletedTasks()
    fun getMyDayTasks(): Flow<List<TaskEntity>> = taskDao.getMyDayTasks(getStartOfTodayMillis(), getEndOfTodayMillis())
    fun getPlannedTasks(): Flow<List<TaskEntity>> = taskDao.getPlannedTasks()
    fun getTasksForCollection(collectionId: String): Flow<List<TaskEntity>> = taskDao.getTasksForCollection(collectionId)
    fun getIncompleteCount(): Flow<Int> = taskDao.getIncompleteCount()
    fun getMyDayPendingCount(): Flow<Int> = taskDao.getMyDayPendingCount(getStartOfTodayMillis(), getEndOfTodayMillis())

    suspend fun createTask(
        title: String,
        notes: String? = null,
        dueDate: Long? = null,
        isMyDay: Boolean = false,
        collectionId: String? = null,
        materialId: String? = null
    ): TaskEntity = withContext(Dispatchers.IO) {
        val todayMidnight = getStartOfTodayMillis()
        val task = TaskEntity(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            notes = notes?.trim()?.ifBlank { null },
            dueDate = dueDate,
            isCompleted = false,
            completedAt = null,
            isMyDay = isMyDay,
            myDayDate = if (isMyDay) todayMidnight else null,
            collectionId = collectionId,
            materialId = materialId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        taskDao.insert(task)
        task
    }

    suspend fun updateTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        taskDao.update(task.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleCompleted(task: TaskEntity) = withContext(Dispatchers.IO) {
        val newCompleted = !task.isCompleted
        val completedAt = if (newCompleted) System.currentTimeMillis() else null
        taskDao.update(
            task.copy(
                isCompleted = newCompleted,
                completedAt = completedAt,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun toggleMyDay(task: TaskEntity) = withContext(Dispatchers.IO) {
        val newMyDay = !task.isMyDay
        val myDayDate = if (newMyDay) getStartOfTodayMillis() else null
        taskDao.update(
            task.copy(
                isMyDay = newMyDay,
                myDayDate = myDayDate,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteTask(id: String) = withContext(Dispatchers.IO) {
        taskDao.deleteById(id)
    }

    private fun getStartOfTodayMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfTodayMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}
