package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.TaskCard
import com.example.ui.viewmodel.AssistantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSchedulerScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.scheduledTasks.collectAsState()
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAiPlannerDialog by remember { mutableStateOf(false) }

    val filterOptions = listOf("Active", "All", "Completed")

    val filteredTasks = when (selectedFilterIndex) {
        0 -> tasks.filter { !it.isCompleted }
        2 -> tasks.filter { it.isCompleted }
        else -> tasks
    }

    val activeCount = tasks.count { !it.isCompleted }
    val completedCount = tasks.count { it.isCompleted }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Automated Task Scheduler", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAiPlannerDialog = true },
                        modifier = Modifier.testTag("action_ai_planner")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Auto-Planner",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_task")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Stats summary row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Pending Alarms",
                    count = "$activeCount",
                    icon = Icons.Default.Schedule,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Completed",
                    count = "$completedCount",
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }

            // AI Auto-Planner Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable { showAiPlannerDialog = true }
                    .testTag("ai_planner_banner"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Auto-Schedule Routine",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Tell Omni AI your day's goals and let it schedule real alarms automatically.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedFilterIndex,
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                filterOptions.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedFilterIndex == index,
                        onClick = { selectedFilterIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedFilterIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedFilterIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            // Task list
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No scheduled tasks in this view",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap + to create a task, or ask Omni AI in chat: 'Remind me in 30 mins to take a break'",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggleComplete = { viewModel.toggleTaskComplete(task) },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }
                }
            }
        }
    }

    // Add Task Dialog
    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, desc, delayMinutes, category, repeat ->
                val time = System.currentTimeMillis() + delayMinutes * 60 * 1000L
                viewModel.createManualTask(title, desc, time, category, repeat)
                showAddTaskDialog = false
            }
        )
    }

    // AI Planner Dialog
    if (showAiPlannerDialog) {
        AiPlannerDialog(
            onDismiss = { showAiPlannerDialog = false },
            onConfirm = { goals ->
                viewModel.autoPlanDay(goals)
                showAiPlannerDialog = false
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    count: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = count, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
                Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var delayMinutes by remember { mutableStateOf("15") }
    var category by remember { mutableStateOf("REMINDER") }
    var repeat by remember { mutableStateOf("NONE") }

    val categories = listOf("REMINDER", "ALARM", "MEETING", "HABIT", "AI_BRIEFING")
    val repeats = listOf("NONE", "DAILY", "HOURLY", "WEEKLY")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule New Automated Task", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                OutlinedTextField(
                    value = delayMinutes,
                    onValueChange = { delayMinutes = it },
                    label = { Text("Trigger in (Minutes from now)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Category chips
                Text("Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.take(3).forEach { cat ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (category == cat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { category = cat }
                        ) {
                            Text(
                                text = cat,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = if (category == cat) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Repeat chips
                Text("Repeat", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeats.forEach { rep ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (repeat == rep) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { repeat = rep }
                        ) {
                            Text(
                                text = rep,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = if (repeat == rep) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val minutes = delayMinutes.toLongOrNull() ?: 15L
                        onConfirm(title, description, minutes, category, repeat)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Set Alarm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AiPlannerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var goalsText by remember { mutableStateOf("Finish presentation by 2 PM, 30 min jog at 5 PM, review project plan at 7 PM") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Auto-Planner", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Describe your objectives or schedule intentions. Omni AI will synthesize the schedule, set smart milestones, and configure alarms.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = goalsText,
                    onValueChange = { goalsText = it },
                    label = { Text("Your goals or activities") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (goalsText.isNotBlank()) {
                        onConfirm(goalsText)
                    }
                },
                enabled = goalsText.isNotBlank()
            ) {
                Text("Generate & Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
