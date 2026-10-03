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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SubTaskEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.ui.components.AddTaskDialog
import com.example.ui.components.DopamineHeader
import com.example.ui.components.ParalysisRescueDialog
import com.example.ui.components.TaskCard
import com.example.ui.components.VoiceTaskCaptureModal
import com.example.ui.viewmodel.MainViewModel

/**
 * Google Material 3 Expressive Tasks Screen
 * Features:
 * 1. Immediate Voice-to-Text Floating Action Button for zero-friction task capture.
 * 2. Executive Function Priority System (Urgent/High items float first with visual indicators).
 */
@Composable
fun TasksScreen(
    viewModel: MainViewModel,
    tasks: List<TaskEntity>,
    subtasks: List<SubTaskEntity>,
    level: Int,
    xp: Int,
    completedCount: Int,
    todayEnergy: String,
    bannerMessage: String?,
    breakdownTaskId: Long?,
    onNavigateToTimer: (Long?, String, Int) -> Unit,
    onNavigateToBrainDump: () -> Unit,
    onOpenAnalytics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showVoiceModal by remember { mutableStateOf(false) }
    var showParalysisDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("ALL") }

    // ADHD Executive Function Sorting: High Priority (Urgent) tasks float to top
    val priorityRank: (TaskEntity) -> Int = { task ->
        when (task.urgency) {
            TaskPriority.HIGH.key -> 0
            TaskPriority.MEDIUM.key -> 1
            TaskPriority.LOW.key -> 2
            else -> 1
        }
    }

    val pendingTasks = tasks.filter { !it.isCompleted }.sortedBy { priorityRank(it) }
    val focusHeroTask = pendingTasks.firstOrNull()
    val urgentTasksCount = tasks.count { it.urgency == TaskPriority.HIGH.key && !it.isCompleted }

    val displayedTasks = when (selectedTab) {
        "URGENT" -> tasks.filter { it.urgency == TaskPriority.HIGH.key && !it.isCompleted }
        "TODAY" -> pendingTasks
        "DONE" -> tasks.filter { it.isCompleted }
        else -> tasks.sortedWith(
            compareBy<TaskEntity> { it.isCompleted }
                .thenBy { priorityRank(it) }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 104.dp)
        ) {
            // Google M3 Large Top App Bar & Header
            item {
                DopamineHeader(
                    level = level,
                    xp = xp,
                    completedTasksCount = completedCount,
                    currentEnergy = todayEnergy,
                    onEnergySelected = { viewModel.setTodayEnergy(it) },
                    bannerMessage = bannerMessage,
                    onDismissBanner = { viewModel.dismissDopamineBanner() },
                    onOpenParalysisRescue = { showParalysisDialog = true },
                    onOpenAnalytics = onOpenAnalytics,
                    subtasks = subtasks
                )
            }

            // Google M3 "At A Glance" Hero Spotlight Card (Prioritizes Urgent/High item)
            if (focusHeroTask != null && selectedTab != "DONE") {
                val isHeroUrgent = focusHeroTask.urgency == TaskPriority.HIGH.key
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isHeroUrgent) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = if (isHeroUrgent) {
                            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        } else null
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isHeroUrgent) Icons.Default.PriorityHigh else Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (isHeroUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isHeroUrgent) "🚨 اولویت فوری: این کار توجه فوری نیاز دارد" else "پیشنهاد تمرکز الان (بدون اضطراب)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isHeroUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest
                                ) {
                                    Text(
                                        text = "${focusHeroTask.estimatedMinutes} دقیقه",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = focusHeroTask.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (focusHeroTask.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = focusHeroTask.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Start Sprint Button
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isHeroUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onNavigateToTimer(focusHeroTask.id, focusHeroTask.title, focusHeroTask.estimatedMinutes) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isHeroUrgent) "حل کار فوری با تایمر ⏱️" else "شروع اسپرینت تمرکز",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                // Mark Done Button
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { viewModel.toggleTaskComplete(focusHeroTask) }
                                ) {
                                    Text(
                                        text = "انجام شد ✓",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Google M3 Filter Chips (With Priority Filter)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        Pair("ALL", "همه (${tasks.size})"),
                        Pair("URGENT", "🔺 فوری (${urgentTasksCount})"),
                        Pair("TODAY", "در حال انجام (${pendingTasks.size})"),
                        Pair("DONE", "تکمیل شده (${completedCount})")
                    )

                    tabs.forEach { (key, label) ->
                        val isSelected = selectedTab == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTab = key },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (key == "URGENT") MaterialTheme.colorScheme.errorContainer
                                else MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = if (key == "URGENT") MaterialTheme.colorScheme.onErrorContainer
                                else MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedLeadingIconColor = if (key == "URGENT") MaterialTheme.colorScheme.onErrorContainer
                                else MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Tasks List
            if (displayedTasks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (selectedTab == "DONE") "هنوز کاری تکمیل نشده است"
                            else if (selectedTab == "URGENT") "تبریک! هیچ کار فوری و اضطراری ندارید 🎉"
                            else "هیچ کاری در این بخش نیست",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "با دکمه میکروفون صدای خودت رو ضبط کن یا با دکمه کار جدید، تسک بساز.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(displayedTasks, key = { it.id }) { task ->
                    val taskSubtasks = subtasks.filter { it.taskId == task.id }
                    TaskCard(
                        task = task,
                        subtasks = taskSubtasks,
                        isBreakingDown = breakdownTaskId == task.id,
                        onToggleTaskComplete = { viewModel.toggleTaskComplete(it) },
                        onToggleSubtaskComplete = { viewModel.toggleSubtaskComplete(it) },
                        onBreakdownWithAi = { viewModel.breakdownTask(it) },
                        onStartFocus = { onNavigateToTimer(it.id, it.title, it.estimatedMinutes) },
                        onDeleteTask = { viewModel.deleteTask(it) }
                    )
                }
            }
        }

        // Dual Floating Action Buttons: 1. Voice Capture FAB (Instant Thought Dump), 2. Extended Task FAB
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Voice Task Capture FAB (Zero-friction ADHD thought dump)
            FloatingActionButton(
                onClick = { showVoiceModal = true },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .size(54.dp)
                    .testTag("voice_capture_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "ثبت سریع صوتی تسک",
                    modifier = Modifier.size(24.dp)
                )
            }

            // Google M3 Extended Floating Action Button
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_task_fab"),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                },
                text = {
                    Text(
                        text = "کار جدید",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    }

    // Voice Task Capture Modal
    if (showVoiceModal) {
        VoiceTaskCaptureModal(
            onDismiss = { showVoiceModal = false },
            onSaveTask = { title, priority, energy, minutes ->
                viewModel.addTask(
                    title = title,
                    urgency = priority,
                    energyLevel = energy,
                    estimatedMinutes = minutes
                )
                viewModel.showDopamineBanner("کار صوتی در ۲ ثانیه ثبت شد! 🎙️✨ تمرکزت حفظ شد")
            }
        )
    }

    // Standard Add Task Dialog
    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, desc, cat, energy, mins, urgency, subtasksList ->
                viewModel.addTask(
                    title = title,
                    description = desc,
                    category = cat,
                    energyLevel = energy,
                    estimatedMinutes = mins,
                    urgency = urgency,
                    subtasks = subtasksList
                )
                showAddDialog = false
            }
        )
    }

    // Paralysis Rescue Dialog
    if (showParalysisDialog) {
        val paralysisState by viewModel.paralysisState.collectAsStateWithLifecycle()
        ParalysisRescueDialog(
            state = paralysisState,
            onDismiss = {
                showParalysisDialog = false
                viewModel.dismissParalysisDialog()
            },
            onRescueRequest = { userFeeling ->
                viewModel.rescueParalysis(userFeeling)
            },
            onStartTaskNow = { taskTitle, minutes ->
                showParalysisDialog = false
                viewModel.dismissParalysisDialog()
                onNavigateToTimer(null, taskTitle, minutes)
            }
        )
    }
}
