package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubTaskEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.ui.theme.CalmMint
import com.example.ui.theme.DopamineAmber
import com.example.ui.theme.M3ErrorLight
import com.example.ui.theme.M3PrimaryLight
import com.example.ui.theme.M3Success
import com.example.ui.theme.M3Warning
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

enum class PomodoroMode(val title: String, val defaultMinutes: Int, val emoji: String) {
    FOCUS("تمرکز عمیق", 25, "🎯"),
    SHORT_BREAK("استراحت کوتاه", 5, "☕"),
    LONG_BREAK("استراحت طولانی", 15, "🧘")
}

/**
 * Pomodoro Focus Timer
 * Integrated deeply with the user's task list:
 * - Single-task isolation to combat ADHD distractibility and paralysis
 * - Active task binding with live in-timer subtasks checklist
 * - 1-Tap task completion with XP dopamine reward
 * - Classic Pomodoro work/break intervals with haptic finish vibration
 */
@Composable
fun FocusTimerScreen(
    viewModel: MainViewModel,
    tasks: List<TaskEntity> = emptyList(),
    subtasks: List<SubTaskEntity> = emptyList(),
    initialTaskId: Long? = null,
    initialTaskTitle: String = "تمرکز آزاد",
    initialMinutes: Int = 25,
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Active Task State
    var activeTaskId by remember { mutableStateOf(initialTaskId) }
    val activeTask = tasks.find { it.id == activeTaskId }
    var customTaskTitle by remember { mutableStateOf(activeTask?.title ?: initialTaskTitle) }
    var showTaskPicker by remember { mutableStateOf(false) }

    // Pomodoro Mode and Timer State
    var currentMode by remember { mutableStateOf(PomodoroMode.FOCUS) }
    var selectedDurationMinutes by remember { mutableIntStateOf(initialMinutes) }
    var secondsRemaining by remember { mutableIntStateOf(initialMinutes * 60) }
    var isRunning by remember { mutableStateOf(false) }
    var completedPomodorosCount by remember { mutableIntStateOf(0) }

    // Haptic vibration helper
    fun triggerVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(500)
                }
            }
        } catch (_: Exception) {}
    }

    // Update seconds when duration or mode changes
    LaunchedEffect(selectedDurationMinutes, currentMode) {
        if (!isRunning) {
            secondsRemaining = selectedDurationMinutes * 60
        }
    }

    // Timer countdown loop
    LaunchedEffect(isRunning, secondsRemaining) {
        if (isRunning && secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--
        } else if (isRunning && secondsRemaining == 0) {
            isRunning = false
            triggerVibration()

            if (currentMode == PomodoroMode.FOCUS) {
                completedPomodorosCount++
                val taskName = activeTask?.title ?: customTaskTitle
                viewModel.saveFocusSession(activeTaskId, taskName, selectedDurationMinutes)
                viewModel.showDopamineBanner("آفرین! یک چرخه پومودورو با موفقیت انجام شد 🍅 (+25 XP)")

                // Auto-suggest break
                if (completedPomodorosCount % 4 == 0) {
                    currentMode = PomodoroMode.LONG_BREAK
                    selectedDurationMinutes = PomodoroMode.LONG_BREAK.defaultMinutes
                } else {
                    currentMode = PomodoroMode.SHORT_BREAK
                    selectedDurationMinutes = PomodoroMode.SHORT_BREAK.defaultMinutes
                }
                secondsRemaining = selectedDurationMinutes * 60
            } else {
                // Break ended, back to focus
                viewModel.showDopamineBanner("استراحت تمام شد! آماده تمرکز بعدی باش 🚀")
                currentMode = PomodoroMode.FOCUS
                selectedDurationMinutes = PomodoroMode.FOCUS.defaultMinutes
                secondsRemaining = selectedDurationMinutes * 60
            }
        }
    }

    val totalSeconds = (selectedDurationMinutes * 60).toFloat()
    val progress = (secondsRemaining / totalSeconds).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "timerProgress")

    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    val activeSubtasks = subtasks.filter { it.taskId == activeTaskId }
    val uncompletedTasks = tasks.filter { !it.isCompleted }

    val arcColor by animateColorAsState(
        targetValue = when (currentMode) {
            PomodoroMode.FOCUS -> if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
            PomodoroMode.SHORT_BREAK -> M3Success
            PomodoroMode.LONG_BREAK -> DopamineAmber
        },
        label = "arcColor"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(14.dp))

            // Header Row: Title, Pomodoro Counter & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "تایمر پومودورو و تمرکز 🍅",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$completedPomodorosCount پومودورو تکمیل‌شده امروز",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("close_timer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "بستن",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pomodoro Mode Tabs (Focus, Short Break, Long Break)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PomodoroMode.values().forEach { mode ->
                    val isSelected = currentMode == mode
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isRunning) {
                                currentMode = mode
                                selectedDurationMinutes = mode.defaultMinutes
                                secondsRemaining = mode.defaultMinutes * 60
                            }
                            .testTag("mode_${mode.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${mode.emoji} ${mode.title}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Integrated Active Task Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("active_timer_task_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            // Priority Icon/Badge if bound to task
                            if (activeTask != null) {
                                val priorityText = when (activeTask.urgency) {
                                    TaskPriority.HIGH.key -> "🔺 فوری"
                                    TaskPriority.LOW.key -> "🟢 پایین"
                                    else -> "🟡 متوسط"
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (activeTask.urgency == TaskPriority.HIGH.key) MaterialTheme.colorScheme.errorContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Text(
                                        text = priorityText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeTask.urgency == TaskPriority.HIGH.key) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Text(
                                text = activeTask?.title ?: customTaskTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Switch Task Button
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { showTaskPicker = !showTaskPicker }
                                .testTag("switch_task_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (showTaskPicker) "بستن" else "انتخاب کار",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Icon(
                                    imageVector = if (showTaskPicker) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Task Picker Dropdown list
                    AnimatedVisibility(visible = showTaskPicker) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Text(
                                text = "کارهای در انتظار برای تمرکز:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            uncompletedTasks.forEach { taskItem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            activeTaskId = taskItem.id
                                            customTaskTitle = taskItem.title
                                            selectedDurationMinutes = taskItem.estimatedMinutes
                                            showTaskPicker = false
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = taskItem.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (activeTaskId == taskItem.id) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${taskItem.estimatedMinutes} د",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // In-Timer Subtasks Checklist
                    if (activeSubtasks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "ریزمتدها (گام‌های حین کار):",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            activeSubtasks.forEach { sub ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (sub.isCompleted) MaterialTheme.colorScheme.primary
                                                else Color.Transparent
                                            )
                                            .border(
                                                width = 1.5.dp,
                                                color = if (sub.isCompleted) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outline,
                                                shape = CircleShape
                                            )
                                            .clickable { viewModel.toggleSubtaskComplete(sub) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (sub.isCompleted) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = sub.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                        color = if (sub.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Quick Complete Task Button (Mark Done from Timer)
                    if (activeTask != null && !activeTask.isCompleted) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.toggleTaskComplete(activeTask)
                                    viewModel.showDopamineBanner("تسک با موفقیت کامل شد! 🎉 عالی کار کردی (+40 XP)")
                                }
                                .testTag("complete_active_task_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 7.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "این تسک تمام شد! علامت‌گذاری انجام ✓",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Duration Customizer (Micro, Sprint, Pomodoro, Deep)
            if (currentMode == PomodoroMode.FOCUS) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val focusDurations = listOf(
                        Pair(10, "۱۰د (میکرو)"),
                        Pair(15, "۱۵د (اسپرینت)"),
                        Pair(25, "۲۵د (استاندارد)"),
                        Pair(45, "۴۵د (عمیق)")
                    )

                    focusDurations.forEach { (mins, label) ->
                        val isSelected = selectedDurationMinutes == mins
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !isRunning) {
                                    selectedDurationMinutes = mins
                                    secondsRemaining = mins * 60
                                }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Circular Pomodoro Gauge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(240.dp)
            ) {
                Canvas(modifier = Modifier.size(220.dp)) {
                    // Outer subtle track
                    drawCircle(
                        color = Color(0xFFE2EBE8),
                        radius = size.minDimension / 2f,
                        style = Stroke(width = 14.dp.toPx())
                    )

                    // Animated Progress Arc
                    drawArc(
                        color = arcColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formattedTime,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isRunning) {
                            if (currentMode == PomodoroMode.FOCUS) "در حال تمرکز عمیق 🚀" else "در حال استراحت مغز ☕"
                        } else "آماده شروع",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mindful ADHD Breathing Prompt
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SelfImprovement,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (currentMode == PomodoroMode.FOCUS)
                            "🌱 تمرکز روی یک کار در این ۲۵ دقیقه. هر فکر مزاحمی اومد، فقط رهاش کن یا یادداشت کن."
                        else
                            "☕ از پشت سیستم یا گوشی بلند شو، آب بنوش و چند حرکت کششی انجام بده.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Play / Pause / Reset Action Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset Button
                IconButton(
                    onClick = {
                        isRunning = false
                        secondsRemaining = selectedDurationMinutes * 60
                    },
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .testTag("reset_timer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "بازنشانی",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Primary Start / Pause Button
                Button(
                    onClick = { isRunning = !isRunning },
                    modifier = Modifier
                        .height(54.dp)
                        .width(170.dp)
                        .testTag("toggle_timer_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRunning) "توقف تایمر" else "شروع پومودورو",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
