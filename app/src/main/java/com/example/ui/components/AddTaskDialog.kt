package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.HighEnergySpoon
import com.example.ui.theme.LowEnergySpoon
import com.example.ui.theme.MediumEnergySpoon

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        category: String,
        energyLevel: String,
        estimatedMinutes: Int,
        urgency: String,
        subtasks: List<String>
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("کار") }
    var energyLevel by remember { mutableStateOf("MEDIUM") }
    var urgency by remember { mutableStateOf("MEDIUM") }
    var estimatedMinutes by remember { mutableIntStateOf(15) }

    var newSubtaskInput by remember { mutableStateOf("") }
    val subtasksList = remember { mutableStateListOf<String>() }

    val categories = listOf("کار", "شخصی", "سلامت", "خلاقیت", "مطالعه")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "افزودن کار جدید",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان تسک (شفاف و اجرایی)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_task_title_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات یا یادداشت کوچک") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("add_task_desc_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Energy Level Selection
                Text(
                    text = "سطح انرژی مورد نیاز:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val energyLevels: List<Triple<String, String, Color>> = listOf(
                        Triple("LOW", "🔋 کم‌انرژی", LowEnergySpoon),
                        Triple("MEDIUM", "⚡ معمولی", MediumEnergySpoon),
                        Triple("HIGH", "🚀 هایپر", HighEnergySpoon)
                    )
                    energyLevels.forEach { (key, label, color) ->
                        val selected = energyLevel == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) color.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { energyLevel = key }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Priority Selection
                Text(
                    text = "اولویت تسک (مدیریت موارد فوری):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val priorityOptions = listOf(
                        Triple(com.example.data.model.TaskPriority.HIGH.key, "🔺 فوری / بالا", com.example.ui.theme.M3ErrorLight),
                        Triple(com.example.data.model.TaskPriority.MEDIUM.key, "🟡 متوسط", com.example.ui.theme.M3Warning),
                        Triple(com.example.data.model.TaskPriority.LOW.key, "🟢 پایین", com.example.ui.theme.M3Success)
                    )

                    priorityOptions.forEach { (key, label, color) ->
                        val selected = urgency == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = if (selected) 1.5.dp else 0.dp,
                                    color = if (selected) color else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { urgency = key }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Realistic Time estimate pills
                Text(
                    text = "تخمین زمان واقع‌بینانه (با احتساب مکث و آماده‌سازی):",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(5, 15, 25, 45).forEach { mins ->
                        val selected = estimatedMinutes == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { estimatedMinutes = mins }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$mins د",
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Micro-steps quick add
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newSubtaskInput,
                        onValueChange = { newSubtaskInput = it },
                        placeholder = { Text("ریز‌قدم (مثلاً: باز کردن سایت)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("add_subtask_input")
                    )
                    IconButton(
                        onClick = {
                            if (newSubtaskInput.isNotBlank()) {
                                subtasksList.add(newSubtaskInput.trim())
                                newSubtaskInput = ""
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "افزودن ریز‌قدم")
                    }
                }

                if (subtasksList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Column {
                        subtasksList.forEachIndexed { index, sub ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• $sub",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = { subtasksList.removeAt(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "حذف",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("انصراف")
                    }
                    androidx.compose.material3.Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(
                                    title,
                                    description,
                                    category,
                                    energyLevel,
                                    estimatedMinutes,
                                    urgency,
                                    subtasksList.toList()
                                )
                            }
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.AsanaCoral),
                        shape = RoundedCornerShape(8.dp),
                        enabled = title.isNotBlank(),
                        modifier = Modifier.testTag("save_new_task_button")
                    ) {
                        Text("ثبت کار", color = Color.White)
                    }
                }
            }
        }
    }
}
