package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class EnergyLevel(val labelPersian: String, val emoji: String) {
    LOW("کم‌انرژی / قاشق پایین", "🔋"),
    MEDIUM("انرژی متوسط / متعادل", "⚡"),
    HIGH("هایپرفوکوس / انرژی بالا", "🚀")
}

enum class TaskPriority(val key: String, val labelPersian: String, val emoji: String) {
    HIGH("HIGH", "فوری / بالا", "🔺"),
    MEDIUM("MEDIUM", "متوسط", "🟡"),
    LOW("LOW", "پایین", "🟢")
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "کار",
    val energyLevel: String = EnergyLevel.MEDIUM.name,
    val estimatedMinutes: Int = 15,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val xpValue: Int = 20,
    val urgency: String = "MEDIUM",
    val aiAdvice: String? = null
)

@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["taskId"])]
)
data class SubTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val title: String,
    val isCompleted: Boolean = false,
    val estimatedMinutes: Int = 3,
    val orderIndex: Int = 0
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long? = null,
    val taskTitle: String = "تمرکز آزاد",
    val durationMinutes: Int = 25,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String, // "user" or "assistant"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class TaskWithSubTasks(
    val task: TaskEntity,
    val subtasks: List<SubTaskEntity>
)
