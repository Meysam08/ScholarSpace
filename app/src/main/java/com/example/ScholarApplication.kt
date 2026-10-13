package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.database.AppDatabase
import com.example.data.file.FileManager
import com.example.data.repository.FocusRepository
import com.example.data.repository.LibraryRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TaskRepository

class ScholarApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val fileManager by lazy { FileManager(this) }

    val libraryRepository by lazy {
        LibraryRepository(database.collectionDao(), database.materialDao(), fileManager)
    }

    val taskRepository by lazy {
        TaskRepository(database.taskDao())
    }

    val focusRepository by lazy {
        FocusRepository(database.focusDao())
    }

    val settingsRepository by lazy {
        SettingsRepository(database.settingsDao())
    }

    val appScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default
    )

    val pomodoroManager by lazy {
        com.example.timer.PomodoroManager(this, focusRepository, appScope)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusChannel = NotificationChannel(
                FOCUS_CHANNEL_ID,
                "Pomodoro Focus Timer",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when a study session or break ends"
                enableVibration(true)
            }

            val tasksChannel = NotificationChannel(
                TASKS_CHANNEL_ID,
                "Study Task Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily study task reminders and due date notifications"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(focusChannel)
            notificationManager?.createNotificationChannel(tasksChannel)
        }
    }

    companion object {
        const val FOCUS_CHANNEL_ID = "scholar_focus_timer_channel"
        const val TASKS_CHANNEL_ID = "scholar_tasks_channel"
    }
}
