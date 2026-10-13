package com.example.ui.tasks

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.WbSunny
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CollectionEntity
import com.example.data.model.TaskEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var taskTitleInput by remember { mutableStateOf("") }
    var selectedDueDate by remember { mutableStateOf<Long?>(null) }
    var selectedCollectionId by remember { mutableStateOf<String?>(null) }
    var addToMyDay by remember { mutableStateOf(uiState.currentFilter == TaskFilter.MY_DAY) }
    var showCollectionPickerDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.currentFilter) {
        if (uiState.currentFilter == TaskFilter.MY_DAY) {
            addToMyDay = true
        }
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val headerIcon = when (uiState.currentFilter) {
                                TaskFilter.MY_DAY -> Icons.Default.WbSunny
                                TaskFilter.PLANNED -> Icons.Default.CalendarToday
                                TaskFilter.ALL -> Icons.Default.FormatListBulleted
                                TaskFilter.COMPLETED -> Icons.Default.CheckCircle
                            }
                            val headerTint = when (uiState.currentFilter) {
                                TaskFilter.MY_DAY -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.primary
                            }
                            Icon(
                                imageVector = headerIcon,
                                contentDescription = null,
                                tint = headerTint,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.currentFilter.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (uiState.currentFilter == TaskFilter.MY_DAY) {
                            val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
                            Text(
                                text = dateFormat.format(Date()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Chips Bar (Microsoft To Do navigation)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TaskFilter.values()) { filter ->
                    val isSelected = uiState.currentFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filter.title) },
                        leadingIcon = {
                            when (filter) {
                                TaskFilter.MY_DAY -> Icon(Icons.Default.WbSunny, contentDescription = null, modifier = Modifier.size(16.dp))
                                TaskFilter.PLANNED -> Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                TaskFilter.ALL -> Icon(Icons.Default.FormatListBulleted, contentDescription = null, modifier = Modifier.size(16.dp))
                                TaskFilter.COMPLETED -> Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // Quick Add Task Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = taskTitleInput,
                            onValueChange = { taskTitleInput = it },
                            placeholder = { Text("Add a task to study...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("task_title_input")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (taskTitleInput.isNotBlank()) {
                                    viewModel.createTask(
                                        title = taskTitleInput,
                                        dueDate = selectedDueDate,
                                        isMyDay = addToMyDay,
                                        collectionId = selectedCollectionId
                                    )
                                    taskTitleInput = ""
                                    selectedDueDate = null
                                    selectedCollectionId = null
                                }
                            },
                            enabled = taskTitleInput.isNotBlank(),
                            modifier = Modifier.testTag("task_add_button")
                        ) {
                            Text("Add")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick modifiers row (Due date, My Day, Collection)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Due date picker
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (selectedDueDate != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val pickedCal = Calendar.getInstance()
                                        pickedCal.set(year, month, day, 23, 59, 59)
                                        selectedDueDate = pickedCal.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (selectedDueDate != null) formatDueDate(selectedDueDate) else "Due Date",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                if (selectedDueDate != null) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clickable { selectedDueDate = null }
                                    )
                                }
                            }
                        }

                        // My Day Toggle button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (addToMyDay) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { addToMyDay = !addToMyDay }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (addToMyDay) Icons.Default.WbSunny else Icons.Outlined.WbSunny,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (addToMyDay) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("My Day", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        // Collection association dropdown
                        Box {
                            val selectedColName = uiState.collections.find { it.id == selectedCollectionId }?.name
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (selectedCollectionId != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { showCollectionPickerDropdown = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(selectedColName ?: "Link Subject", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            DropdownMenu(
                                expanded = showCollectionPickerDropdown,
                                onDismissRequest = { showCollectionPickerDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("None") },
                                    onClick = {
                                        selectedCollectionId = null
                                        showCollectionPickerDropdown = false
                                    }
                                )
                                uiState.collections.forEach { col ->
                                    DropdownMenuItem(
                                        text = { Text(col.name) },
                                        onClick = {
                                            selectedCollectionId = col.id
                                            showCollectionPickerDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tasks List
            if (uiState.tasks.isEmpty()) {
                EmptyTasksView(uiState.currentFilter)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.tasks, key = { it.id }) { task ->
                        val collectionName = uiState.collections.find { it.id == task.collectionId }?.name
                        TaskItemCard(
                            task = task,
                            collectionName = collectionName,
                            onToggleComplete = { viewModel.toggleCompleted(task) },
                            onToggleMyDay = { viewModel.toggleMyDay(task) },
                            onClick = { viewModel.selectTask(task) }
                        )
                    }
                }
            }
        }
    }

    // Task Details & Edit Dialog
    uiState.selectedTask?.let { task ->
        TaskDetailsDialog(
            task = task,
            collections = uiState.collections,
            onDismiss = { viewModel.selectTask(null) },
            onSave = { updated -> viewModel.updateTask(updated) },
            onDelete = { viewModel.deleteTask(task.id) }
        )
    }
}

@Composable
fun TaskItemCard(
    task: TaskEntity,
    collectionName: String?,
    onToggleComplete: () -> Unit,
    onToggleMyDay: () -> Unit,
    onClick: () -> Unit
) {
    val isOverdue = task.dueDate != null && !task.isCompleted && task.dueDate < System.currentTimeMillis()

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}")
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Custom circular checkbox with accessible touch target
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable { onToggleComplete() }
                    .testTag("task_checkbox_${task.id}"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                        .background(if (task.isCompleted) MaterialTheme.colorScheme.primary else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Metadata row
                if (task.dueDate != null || collectionName != null || !task.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (task.dueDate != null) {
                            Text(
                                text = (if (isOverdue) "Overdue • " else "Due ") + formatDueDate(task.dueDate),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        if (collectionName != null) {
                            Text(
                                text = "• $collectionName",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Quick My Day toggle button
            IconButton(onClick = onToggleMyDay) {
                Icon(
                    imageVector = if (task.isMyDay) Icons.Default.WbSunny else Icons.Outlined.WbSunny,
                    contentDescription = "Toggle My Day",
                    tint = if (task.isMyDay) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun TaskDetailsDialog(
    task: TaskEntity,
    collections: List<CollectionEntity>,
    onDismiss: () -> Unit,
    onSave: (TaskEntity) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(task.title) }
    var notes by remember { mutableStateOf(task.notes ?: "") }
    var dueDate by remember { mutableStateOf(task.dueDate) }
    var isMyDay by remember { mutableStateOf(task.isMyDay) }
    var collectionId by remember { mutableStateOf(task.collectionId) }
    var showColDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Task", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Description") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // My Day toggle row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isMyDay = !isMyDay }
                        .padding(vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isMyDay) Icons.Default.WbSunny else Icons.Outlined.WbSunny,
                        contentDescription = null,
                        tint = if (isMyDay) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isMyDay) "Added to My Day" else "Add to My Day",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isMyDay) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isMyDay) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Due date selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val cal = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val picked = Calendar.getInstance()
                                    picked.set(y, m, d, 23, 59, 59)
                                    dueDate = picked.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (dueDate != null) "Due: ${formatDueDate(dueDate)}" else "Set due date",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (dueDate != null) {
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { dueDate = null }) { Text("Clear") }
                    }
                }

                // Collection selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showColDropdown = true }
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        val currentCol = collections.find { it.id == collectionId }?.name
                        Text(
                            text = if (currentCol != null) "Collection: $currentCol" else "No Collection Linked",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    DropdownMenu(
                        expanded = showColDropdown,
                        onDismissRequest = { showColDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = { collectionId = null; showColDropdown = false }
                        )
                        collections.forEach { col ->
                            DropdownMenuItem(
                                text = { Text(col.name) },
                                onClick = { collectionId = col.id; showColDropdown = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        task.copy(
                            title = title.trim(),
                            notes = notes.trim().ifBlank { null },
                            dueDate = dueDate,
                            isMyDay = isMyDay,
                            collectionId = collectionId
                        )
                    )
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun EmptyTasksView(filter: TaskFilter) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (filter) {
                        TaskFilter.MY_DAY -> Icons.Default.WbSunny
                        TaskFilter.PLANNED -> Icons.Default.CalendarToday
                        TaskFilter.ALL -> Icons.Default.CheckCircle
                        TaskFilter.COMPLETED -> Icons.Default.Check
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when (filter) {
                    TaskFilter.MY_DAY -> "Ready for a productive day?"
                    TaskFilter.PLANNED -> "No upcoming deadlines"
                    TaskFilter.ALL -> "No tasks created yet"
                    TaskFilter.COMPLETED -> "No completed tasks yet"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = when (filter) {
                    TaskFilter.MY_DAY -> "Tasks you add to My Day appear here to help you focus on today's study goals."
                    TaskFilter.PLANNED -> "Tasks with due dates will be organized here by timeline."
                    TaskFilter.ALL -> "Use the input above to capture reading assignments, problem sets, and review tasks."
                    TaskFilter.COMPLETED -> "Completed tasks will be archived here for reference."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun formatDueDate(timestamp: Long?): String {
    if (timestamp == null) return ""
    val now = Calendar.getInstance()
    val due = Calendar.getInstance().apply { timeInMillis = timestamp }

    return if (now.get(Calendar.YEAR) == due.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == due.get(Calendar.DAY_OF_YEAR)
    ) {
        "Today"
    } else if (now.get(Calendar.YEAR) == due.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) + 1 == due.get(Calendar.DAY_OF_YEAR)
    ) {
        "Tomorrow"
    } else {
        val format = SimpleDateFormat("MMM d", Locale.getDefault())
        format.format(Date(timestamp))
    }
}
