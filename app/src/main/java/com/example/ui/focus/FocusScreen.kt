package com.example.ui.focus

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CollectionEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.TaskEntity
import com.example.timer.PomodoroState
import com.example.timer.SessionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    viewModel: FocusViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.screenState.collectAsStateWithLifecycle()
    val timer = state.timer

    var showAssociateDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Focus Timer",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Session Type Selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    SessionType.values().forEach { type ->
                        val isSelected = timer.sessionType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setSessionType(type) },
                            label = {
                                val label = when (type) {
                                    SessionType.FOCUS -> "Focus (${state.focusDurationMinutes}m)"
                                    SessionType.SHORT_BREAK -> "Short Break (${state.shortBreakMinutes}m)"
                                    SessionType.LONG_BREAK -> "Long Break (${state.longBreakMinutes}m)"
                                }
                                Text(label)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            // Associated Topic / Task Chip
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clickable { showAssociateDialog = true }
                        .testTag("focus_associate_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (timer.associatedTaskId != null) Icons.Default.CheckCircle else Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val text = when {
                            timer.associatedTaskTitle != null -> "Task: ${timer.associatedTaskTitle}"
                            timer.associatedCollectionName != null -> "Subject: ${timer.associatedCollectionName}"
                            else -> "Link Study Task or Subject"
                        }
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Circular Countdown Timer
            item {
                CircularTimerDisplay(
                    remainingSeconds = timer.remainingSeconds,
                    totalSeconds = timer.totalSeconds,
                    state = timer.state,
                    sessionType = timer.sessionType
                )
            }

            // 4-Dot Cycle Progress Indicator
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val count = timer.completedSessionsInCycle % 4
                    for (i in 0 until 4) {
                        val isFilled = i < count
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${count}/4 Pomodoros",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action Buttons (Start, Pause, Resume, Reset)
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset Button
                    OutlinedButton(
                        onClick = { viewModel.resetTimer() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("timer_reset_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset")
                    }

                    // Main Start/Pause/Resume Button
                    when (timer.state) {
                        PomodoroState.IDLE -> {
                            Button(
                                onClick = { viewModel.startTimer() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .height(52.dp)
                                    .width(160.dp)
                                    .testTag("timer_start_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        PomodoroState.COMPLETED -> {
                            val nextType = if (timer.sessionType == SessionType.FOCUS) {
                                if (timer.completedSessionsInCycle % 4 == 0 && timer.completedSessionsInCycle > 0) SessionType.LONG_BREAK else SessionType.SHORT_BREAK
                            } else {
                                SessionType.FOCUS
                            }
                            Button(
                                onClick = {
                                    viewModel.setSessionType(nextType)
                                    viewModel.startTimer()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .height(52.dp)
                                    .width(180.dp)
                                    .testTag("timer_start_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Next")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (nextType == SessionType.FOCUS) "Start Focus" else "Start Break",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        PomodoroState.RUNNING -> {
                            Button(
                                onClick = { viewModel.pauseTimer() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier
                                    .height(52.dp)
                                    .width(160.dp)
                                    .testTag("timer_pause_button")
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pause", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        PomodoroState.PAUSED -> {
                            Button(
                                onClick = { viewModel.resumeTimer() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .height(52.dp)
                                    .width(160.dp)
                                    .testTag("timer_resume_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Resume", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Study History Card
            item {
                Spacer(modifier = Modifier.height(8.dp))
                FocusStatsCard(
                    totalSeconds = state.totalFocusSeconds,
                    completedCount = state.completedCount,
                    recentSessions = state.recentSessions
                )
            }

            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }

    if (showAssociateDialog) {
        AssociateFocusDialog(
            tasks = state.incompleteTasks,
            collections = state.collections,
            currentTaskId = timer.associatedTaskId,
            currentColId = timer.associatedCollectionId,
            onDismiss = { showAssociateDialog = false },
            onSelectTask = {
                viewModel.associateTask(it)
                showAssociateDialog = false
            },
            onSelectCollection = {
                viewModel.associateCollection(it)
                showAssociateDialog = false
            },
            onClear = {
                viewModel.associateTask(null)
                viewModel.associateCollection(null)
                showAssociateDialog = false
            }
        )
    }
}

@Composable
fun CircularTimerDisplay(
    remainingSeconds: Int,
    totalSeconds: Int,
    state: PomodoroState,
    sessionType: SessionType
) {
    val progress = if (totalSeconds > 0) {
        remainingSeconds.toFloat() / totalSeconds.toFloat()
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 400),
        label = "timer_progress"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val accentColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = Modifier
            .size(240.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()

            // Background Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress Arc
            drawArc(
                color = if (sessionType == SessionType.FOCUS) primaryColor else accentColor,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            val timeString = String.format(Locale.US, "%02d:%02d", minutes, seconds)

            Text(
                text = timeString,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            val statusText = when (state) {
                PomodoroState.IDLE -> "READY"
                PomodoroState.RUNNING -> if (sessionType == SessionType.FOCUS) "DEEP STUDY" else "REST BREAK"
                PomodoroState.PAUSED -> "PAUSED"
                PomodoroState.COMPLETED -> "SESSION DONE"
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (state == PomodoroState.RUNNING) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = if (state == PomodoroState.RUNNING) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun FocusStatsCard(
    totalSeconds: Long,
    completedCount: Int,
    recentSessions: List<FocusSessionEntity>
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Study Focus History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val hours = totalSeconds / 3600f
                    Text(
                        text = String.format(Locale.US, "%.1f hrs", hours),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Total Time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = completedCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "Sessions Done",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (recentSessions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "RECENT LOGS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                recentSessions.take(5).forEach { session ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = session.sessionType.replace("_", " "),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${session.actualDurationSeconds / 60} min • ${dateFormat.format(Date(session.startedAt))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AssociateFocusDialog(
    tasks: List<TaskEntity>,
    collections: List<CollectionEntity>,
    currentTaskId: String?,
    currentColId: String?,
    onDismiss: () -> Unit,
    onSelectTask: (TaskEntity) -> Unit,
    onSelectCollection: (CollectionEntity) -> Unit,
    onClear: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link Focus Session") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                if (currentTaskId != null || currentColId != null) {
                    item {
                        TextButton(onClick = onClear) {
                            Text("Clear Link", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                if (tasks.isNotEmpty()) {
                    item {
                        Text(
                            "TASKS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(tasks) { task ->
                        Card(
                            onClick = { onSelectTask(task) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (task.id == currentTaskId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(task.title, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                if (collections.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "COLLECTIONS / SUBJECTS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(collections) { col ->
                        Card(
                            onClick = { onSelectCollection(col) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (col.id == currentColId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(col.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
