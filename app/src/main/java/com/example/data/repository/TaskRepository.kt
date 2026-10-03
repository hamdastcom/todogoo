package com.example.data.repository

import com.example.data.local.AppDao
import com.example.data.model.ChatMessageEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.SubTaskEntity
import com.example.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val dao: AppDao,
    private val prefsRepo: UserPreferencesRepository
) {

    val allTasks: Flow<List<TaskEntity>> = dao.getAllTasks()
    val allSubtasks: Flow<List<SubTaskEntity>> = dao.getAllSubtasks()
    val completedCount: Flow<Int> = dao.getCompletedTasksCount()
    val focusSessions: Flow<List<FocusSessionEntity>> = dao.getAllFocusSessions()
    val totalFocusMinutes: Flow<Int?> = dao.getTotalFocusMinutes()
    val chatMessages: Flow<List<ChatMessageEntity>> = dao.getAllChatMessages()

    fun getSubtasksForTask(taskId: Long): Flow<List<SubTaskEntity>> = dao.getSubtasksForTask(taskId)

    suspend fun addTask(task: TaskEntity, subtaskTitles: List<String> = emptyList()): Long {
        val taskId = dao.insertTask(task)
        if (subtaskTitles.isNotEmpty()) {
            val subtasks = subtaskTitles.mapIndexed { index, title ->
                SubTaskEntity(
                    taskId = taskId,
                    title = title,
                    orderIndex = index,
                    estimatedMinutes = 3
                )
            }
            dao.insertSubtasks(subtasks)
        }
        return taskId
    }

    suspend fun toggleTaskComplete(task: TaskEntity): Boolean {
        val newStatus = !task.isCompleted
        val updated = task.copy(
            isCompleted = newStatus,
            completedAt = if (newStatus) System.currentTimeMillis() else null
        )
        dao.updateTask(updated)
        if (newStatus) {
            // Reward dopamine XP!
            prefsRepo.addXp(task.xpValue)
        }
        return newStatus
    }

    suspend fun updateTask(task: TaskEntity) {
        dao.updateTask(task)
    }

    suspend fun deleteTask(task: TaskEntity) {
        dao.deleteTask(task)
    }

    suspend fun toggleSubtaskComplete(subtask: SubTaskEntity) {
        val updated = subtask.copy(isCompleted = !subtask.isCompleted)
        dao.updateSubtask(updated)
        if (updated.isCompleted) {
            prefsRepo.addXp(5) // Mini dopamine boost for micro-step
        }
    }

    suspend fun addSubtasksToTask(taskId: Long, microSteps: List<Pair<String, Int>>) {
        val entities = microSteps.mapIndexed { index, (title, minutes) ->
            SubTaskEntity(
                taskId = taskId,
                title = title,
                estimatedMinutes = minutes,
                orderIndex = index
            )
        }
        dao.insertSubtasks(entities)
    }

    suspend fun recordFocusSession(taskId: Long?, title: String, minutes: Int) {
        dao.insertFocusSession(
            FocusSessionEntity(
                taskId = taskId,
                taskTitle = title,
                durationMinutes = minutes
            )
        )
        // Focus session reward
        prefsRepo.addXp(minutes * 2)
    }

    suspend fun addChatMessage(sender: String, text: String) {
        dao.insertChatMessage(
            ChatMessageEntity(
                sender = sender,
                message = text
            )
        )
    }

    suspend fun clearChat() {
        dao.clearChatMessages()
    }

    suspend fun seedInitialDataIfEmpty() {
        // Helpful ADHD starter templates if database is empty
        val sampleTasks = listOf(
            Pair(
                TaskEntity(
                    title = "یک لیوان آب بنوش و ۳ نفس عمیق بکش 💧",
                    description = "برای بازنشانی تمرکز مغز و رفع خستگی",
                    category = "سلامت",
                    energyLevel = "LOW",
                    estimatedMinutes = 2,
                    xpValue = 15,
                    aiAdvice = "فقط بلند شو و آب بخور، مغز کم‌آب زودتر حواس‌پرت می‌شه."
                ),
                listOf("بلند شدن از صندلی", "پر کردن لیوان", "نوشیدن آرام ۳ جرعه آب")
            ),
            Pair(
                TaskEntity(
                    title = "تخلیه ذهن در بخش هوش مصنوعی 🧠",
                    description = "هر فکری که تو سرته رو بریز روی کاغذ دیجیتال تا ذهنت آروم بشه",
                    category = "شخصی",
                    energyLevel = "MEDIUM",
                    estimatedMinutes = 5,
                    xpValue = 25,
                    aiAdvice = "هیچ ترتیبی لازم نیست، هر چی به ذهنت اومد تایپ کن!"
                ),
                listOf("زدن روی دکمه تخلیه ذهن", "نوشتن افکار پراکنده", "زدن دکمه تبدیل به تسک")
            )
        )

        for ((task, subs) in sampleTasks) {
            val id = dao.insertTask(task)
            val subEntities = subs.mapIndexed { idx, title ->
                SubTaskEntity(
                    taskId = id,
                    title = title,
                    orderIndex = idx,
                    estimatedMinutes = 1
                )
            }
            dao.insertSubtasks(subEntities)
        }
    }
}
