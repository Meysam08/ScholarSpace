package com.example.timer

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.ScholarApplication
import com.example.data.repository.FocusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PomodoroState {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}

enum class SessionType(val displayName: String) {
    FOCUS("Focus Session"),
    SHORT_BREAK("Short Break"),
    LONG_BREAK("Long Break")
}

data class PomodoroUiState(
    val state: PomodoroState = PomodoroState.IDLE,
    val sessionType: SessionType = SessionType.FOCUS,
    val remainingSeconds: Int = 25 * 60,
    val totalSeconds: Int = 25 * 60,
    val completedSessionsInCycle: Int = 0,
    val associatedTaskId: String? = null,
    val associatedTaskTitle: String? = null,
    val associatedCollectionId: String? = null,
    val associatedCollectionName: String? = null
)

class PomodoroManager(
    private val context: Context,
    private val focusRepository: FocusRepository,
    private val coroutineScope: CoroutineScope
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("scholar_pomodoro_state", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    init {
        restoreState()
    }

    private fun restoreState() {
        val stateName = prefs.getString(KEY_STATE, PomodoroState.IDLE.name) ?: PomodoroState.IDLE.name
        val sessionTypeName = prefs.getString(KEY_TYPE, SessionType.FOCUS.name) ?: SessionType.FOCUS.name
        val sessionType = try { SessionType.valueOf(sessionTypeName) } catch (e: Exception) { SessionType.FOCUS }
        val totalSec = prefs.getInt(KEY_TOTAL_SECONDS, 25 * 60)
        val cycleCount = prefs.getInt(KEY_CYCLE_COUNT, 0)
        val taskId = prefs.getString(KEY_TASK_ID, null)
        val taskTitle = prefs.getString(KEY_TASK_TITLE, null)
        val colId = prefs.getString(KEY_COL_ID, null)
        val colName = prefs.getString(KEY_COL_NAME, null)

        val targetEndEpoch = prefs.getLong(KEY_TARGET_END_EPOCH, 0L)
        val pausedRemaining = prefs.getInt(KEY_PAUSED_REMAINING, totalSec)

        val state = try { PomodoroState.valueOf(stateName) } catch (e: Exception) { PomodoroState.IDLE }

        if (state == PomodoroState.RUNNING) {
            val now = System.currentTimeMillis()
            val remaining = ((targetEndEpoch - now) / 1000).toInt()
            if (remaining <= 0) {
                // Timer finished while app was closed or backgrounded
                onSessionFinished(sessionType, totalSec, taskId, colId)
                _uiState.value = PomodoroUiState(
                    state = PomodoroState.COMPLETED,
                    sessionType = sessionType,
                    remainingSeconds = 0,
                    totalSeconds = totalSec,
                    completedSessionsInCycle = if (sessionType == SessionType.FOCUS) cycleCount + 1 else cycleCount,
                    associatedTaskId = taskId,
                    associatedTaskTitle = taskTitle,
                    associatedCollectionId = colId,
                    associatedCollectionName = colName
                )
                saveState()
            } else {
                _uiState.value = PomodoroUiState(
                    state = PomodoroState.RUNNING,
                    sessionType = sessionType,
                    remainingSeconds = remaining,
                    totalSeconds = totalSec,
                    completedSessionsInCycle = cycleCount,
                    associatedTaskId = taskId,
                    associatedTaskTitle = taskTitle,
                    associatedCollectionId = colId,
                    associatedCollectionName = colName
                )
                startTicker()
            }
        } else {
            _uiState.value = PomodoroUiState(
                state = state,
                sessionType = sessionType,
                remainingSeconds = pausedRemaining,
                totalSeconds = totalSec,
                completedSessionsInCycle = cycleCount,
                associatedTaskId = taskId,
                associatedTaskTitle = taskTitle,
                associatedCollectionId = colId,
                associatedCollectionName = colName
            )
        }
    }

    fun startTimer(
        type: SessionType = _uiState.value.sessionType,
        durationMinutes: Int = _uiState.value.totalSeconds / 60,
        taskId: String? = _uiState.value.associatedTaskId,
        taskTitle: String? = _uiState.value.associatedTaskTitle,
        colId: String? = _uiState.value.associatedCollectionId,
        colName: String? = _uiState.value.associatedCollectionName
    ) {
        val totalSec = durationMinutes * 60
        val targetEndEpoch = System.currentTimeMillis() + (totalSec * 1000L)

        _uiState.value = _uiState.value.copy(
            state = PomodoroState.RUNNING,
            sessionType = type,
            remainingSeconds = totalSec,
            totalSeconds = totalSec,
            associatedTaskId = taskId,
            associatedTaskTitle = taskTitle,
            associatedCollectionId = colId,
            associatedCollectionName = colName
        )

        prefs.edit()
            .putString(KEY_STATE, PomodoroState.RUNNING.name)
            .putString(KEY_TYPE, type.name)
            .putInt(KEY_TOTAL_SECONDS, totalSec)
            .putLong(KEY_TARGET_END_EPOCH, targetEndEpoch)
            .putLong(KEY_START_EPOCH, System.currentTimeMillis())
            .putString(KEY_TASK_ID, taskId)
            .putString(KEY_TASK_TITLE, taskTitle)
            .putString(KEY_COL_ID, colId)
            .putString(KEY_COL_NAME, colName)
            .apply()

        startTicker()
    }

    fun pauseTimer() {
        if (_uiState.value.state != PomodoroState.RUNNING) return
        tickerJob?.cancel()

        val remaining = _uiState.value.remainingSeconds
        _uiState.value = _uiState.value.copy(state = PomodoroState.PAUSED)

        prefs.edit()
            .putString(KEY_STATE, PomodoroState.PAUSED.name)
            .putInt(KEY_PAUSED_REMAINING, remaining)
            .apply()
    }

    fun resumeTimer() {
        if (_uiState.value.state != PomodoroState.PAUSED) return
        val remaining = _uiState.value.remainingSeconds
        val targetEndEpoch = System.currentTimeMillis() + (remaining * 1000L)

        _uiState.value = _uiState.value.copy(state = PomodoroState.RUNNING)

        prefs.edit()
            .putString(KEY_STATE, PomodoroState.RUNNING.name)
            .putLong(KEY_TARGET_END_EPOCH, targetEndEpoch)
            .apply()

        startTicker()
    }

    fun resetTimer(defaultMinutes: Int = 25) {
        tickerJob?.cancel()
        val totalSec = defaultMinutes * 60
        _uiState.value = _uiState.value.copy(
            state = PomodoroState.IDLE,
            remainingSeconds = totalSec,
            totalSeconds = totalSec
        )

        prefs.edit()
            .putString(KEY_STATE, PomodoroState.IDLE.name)
            .putInt(KEY_PAUSED_REMAINING, totalSec)
            .putInt(KEY_TOTAL_SECONDS, totalSec)
            .apply()
    }

    fun setSessionType(type: SessionType, minutes: Int) {
        tickerJob?.cancel()
        val totalSec = minutes * 60
        _uiState.value = _uiState.value.copy(
            state = PomodoroState.IDLE,
            sessionType = type,
            remainingSeconds = totalSec,
            totalSeconds = totalSec
        )
        saveState()
    }

    fun associateTask(taskId: String?, taskTitle: String?) {
        _uiState.value = _uiState.value.copy(
            associatedTaskId = taskId,
            associatedTaskTitle = taskTitle
        )
        prefs.edit()
            .putString(KEY_TASK_ID, taskId)
            .putString(KEY_TASK_TITLE, taskTitle)
            .apply()
    }

    fun associateCollection(colId: String?, colName: String?) {
        _uiState.value = _uiState.value.copy(
            associatedCollectionId = colId,
            associatedCollectionName = colName
        )
        prefs.edit()
            .putString(KEY_COL_ID, colId)
            .putString(KEY_COL_NAME, colName)
            .apply()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = coroutineScope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(500)
                val targetEnd = prefs.getLong(KEY_TARGET_END_EPOCH, 0L)
                val now = System.currentTimeMillis()
                val remaining = ((targetEnd - now) / 1000).toInt()

                if (remaining <= 0) {
                    val current = _uiState.value
                    onSessionFinished(
                        current.sessionType,
                        current.totalSeconds,
                        current.associatedTaskId,
                        current.associatedCollectionId
                    )

                    val newCycle = if (current.sessionType == SessionType.FOCUS) {
                        current.completedSessionsInCycle + 1
                    } else {
                        current.completedSessionsInCycle
                    }

                    _uiState.value = current.copy(
                        state = PomodoroState.COMPLETED,
                        remainingSeconds = 0,
                        completedSessionsInCycle = newCycle
                    )

                    prefs.edit()
                        .putString(KEY_STATE, PomodoroState.COMPLETED.name)
                        .putInt(KEY_CYCLE_COUNT, newCycle)
                        .apply()
                    break
                } else {
                    _uiState.value = _uiState.value.copy(remainingSeconds = remaining)
                }
            }
        }
    }

    private fun onSessionFinished(
        type: SessionType,
        totalSeconds: Int,
        taskId: String?,
        collectionId: String?
    ) {
        // Record session to local database
        val startedAt = prefs.getLong(KEY_START_EPOCH, System.currentTimeMillis() - (totalSeconds * 1000L))
        coroutineScope.launch(Dispatchers.IO) {
            focusRepository.recordSession(
                sessionType = type.name,
                plannedDurationSeconds = totalSeconds,
                actualDurationSeconds = totalSeconds,
                startedAt = startedAt,
                isCompleted = true,
                associatedTaskId = taskId,
                associatedCollectionId = collectionId
            )
        }

        // Trigger vibration
        triggerVibration()

        // Post Notification
        postCompletionNotification(type)
    }

    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300), -1)
                )
            }
        } catch (ignored: Exception) {}
    }

    private fun postCompletionNotification(type: SessionType) {
        val title = when (type) {
            SessionType.FOCUS -> "Focus Session Complete!"
            SessionType.SHORT_BREAK -> "Short Break Ended"
            SessionType.LONG_BREAK -> "Long Break Ended"
        }
        val message = when (type) {
            SessionType.FOCUS -> "Well done! Take a well-deserved break or start another round."
            else -> "Ready to get back to deep work? Start your next focus session."
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ScholarApplication.FOCUS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(1001, notification)
    }

    private fun saveState() {
        val current = _uiState.value
        prefs.edit()
            .putString(KEY_STATE, current.state.name)
            .putString(KEY_TYPE, current.sessionType.name)
            .putInt(KEY_TOTAL_SECONDS, current.totalSeconds)
            .putInt(KEY_PAUSED_REMAINING, current.remainingSeconds)
            .putInt(KEY_CYCLE_COUNT, current.completedSessionsInCycle)
            .putString(KEY_TASK_ID, current.associatedTaskId)
            .putString(KEY_TASK_TITLE, current.associatedTaskTitle)
            .putString(KEY_COL_ID, current.associatedCollectionId)
            .putString(KEY_COL_NAME, current.associatedCollectionName)
            .apply()
    }

    companion object {
        private const val KEY_STATE = "key_timer_state"
        private const val KEY_TYPE = "key_timer_type"
        private const val KEY_TOTAL_SECONDS = "key_total_seconds"
        private const val KEY_PAUSED_REMAINING = "key_paused_remaining"
        private const val KEY_TARGET_END_EPOCH = "key_target_end_epoch"
        private const val KEY_START_EPOCH = "key_start_epoch"
        private const val KEY_CYCLE_COUNT = "key_cycle_count"
        private const val KEY_TASK_ID = "key_task_id"
        private const val KEY_TASK_TITLE = "key_task_title"
        private const val KEY_COL_ID = "key_col_id"
        private const val KEY_COL_NAME = "key_col_name"
    }
}
