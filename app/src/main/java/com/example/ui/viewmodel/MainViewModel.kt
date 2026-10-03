package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessageEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.SubTaskEntity
import com.example.data.model.TaskEntity
import com.example.data.remote.ErrandPlaceItem
import com.example.data.remote.ParalysisRescueResult
import com.example.data.remote.ParsedBrainDumpTask
import com.example.data.repository.GeminiRepository
import com.example.data.repository.MetisAiRepository
import com.example.data.repository.TaskRepository
import com.example.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AiOperationState<out T> {
    data object Idle : AiOperationState<Nothing>
    data object Loading : AiOperationState<Nothing>
    data class Success<T>(val data: T) : AiOperationState<T>
    data class Error(val message: String) : AiOperationState<Nothing>
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val prefsRepo = UserPreferencesRepository(application)
    private val taskRepo = TaskRepository(db.appDao(), prefsRepo)
    private val aiRepo = MetisAiRepository(prefsRepo)
    private val geminiRepo = GeminiRepository(prefsRepo)
    private val metisBreakdownService = com.example.data.service.MetisTaskBreakdownService(aiRepo, taskRepo)

    val tasks: StateFlow<List<TaskEntity>> = taskRepo.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subtasks: StateFlow<List<SubTaskEntity>> = taskRepo.allSubtasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedCount: StateFlow<Int> = taskRepo.completedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val focusSessions: StateFlow<List<FocusSessionEntity>> = taskRepo.focusSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalFocusMinutes: StateFlow<Int?> = taskRepo.totalFocusMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val userLevel: StateFlow<Int> = prefsRepo.userLevel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val userXp: StateFlow<Int> = prefsRepo.userXp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 40)

    val todayEnergy: StateFlow<String> = prefsRepo.todayEnergy
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "MEDIUM")

    val metisToken: StateFlow<String> = prefsRepo.metisToken
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesRepository.DEFAULT_TOKEN)

    val metisBaseUrl: StateFlow<String> = prefsRepo.metisBaseUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesRepository.DEFAULT_BASE_URL)

    val geminiApiKey: StateFlow<String> = prefsRepo.geminiApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val aiProvider: StateFlow<String> = prefsRepo.aiProvider
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "METIS")

    val chatRole: StateFlow<String> = prefsRepo.chatRole
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "BODY_DOUBLE")

    val aiModel: StateFlow<String> = prefsRepo.aiModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesRepository.DEFAULT_MODEL)

    val chatMessages: StateFlow<List<ChatMessageEntity>> = taskRepo.chatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Operation States
    private val _brainDumpState = MutableStateFlow<AiOperationState<List<ParsedBrainDumpTask>>>(AiOperationState.Idle)
    val brainDumpState: StateFlow<AiOperationState<List<ParsedBrainDumpTask>>> = _brainDumpState.asStateFlow()

    private val _breakdownTaskId = MutableStateFlow<Long?>(null)
    val breakdownTaskId: StateFlow<Long?> = _breakdownTaskId.asStateFlow()

    private val _paralysisState = MutableStateFlow<AiOperationState<ParalysisRescueResult>>(AiOperationState.Idle)
    val paralysisState: StateFlow<AiOperationState<ParalysisRescueResult>> = _paralysisState.asStateFlow()

    private val _connectionTestState = MutableStateFlow<AiOperationState<String>>(AiOperationState.Idle)
    val connectionTestState: StateFlow<AiOperationState<String>> = _connectionTestState.asStateFlow()

    private val _errandsState = MutableStateFlow<AiOperationState<List<ErrandPlaceItem>>>(AiOperationState.Idle)
    val errandsState: StateFlow<AiOperationState<List<ErrandPlaceItem>>> = _errandsState.asStateFlow()

    private val _transcriptionState = MutableStateFlow<AiOperationState<String>>(AiOperationState.Idle)
    val transcriptionState: StateFlow<AiOperationState<String>> = _transcriptionState.asStateFlow()

    private val _isChatSending = MutableStateFlow(false)
    val isChatSending: StateFlow<Boolean> = _isChatSending.asStateFlow()

    private val _dopamineRewardBanner = MutableStateFlow<String?>(null)
    val dopamineRewardBanner: StateFlow<String?> = _dopamineRewardBanner.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = tasks.first()
            if (existing.isEmpty()) {
                taskRepo.seedInitialDataIfEmpty()
            }
        }
    }

    fun showDopamineBanner(message: String) {
        _dopamineRewardBanner.value = message
    }

    fun dismissDopamineBanner() {
        _dopamineRewardBanner.value = null
    }

    fun setTodayEnergy(energy: String) {
        viewModelScope.launch {
            prefsRepo.setTodayEnergy(energy)
        }
    }

    fun setChatRole(role: String) {
        viewModelScope.launch {
            prefsRepo.saveChatRole(role)
        }
    }

    fun setAiProvider(provider: String) {
        viewModelScope.launch {
            prefsRepo.saveAiProvider(provider)
        }
    }

    fun addTask(
        title: String,
        description: String = "",
        category: String = "کار",
        energyLevel: String = "MEDIUM",
        estimatedMinutes: Int = 15,
        urgency: String = "MEDIUM",
        subtasks: List<String> = emptyList(),
        aiAdvice: String? = null
    ) {
        viewModelScope.launch {
            val task = TaskEntity(
                title = title.trim(),
                description = description.trim(),
                category = category,
                energyLevel = energyLevel,
                estimatedMinutes = estimatedMinutes,
                urgency = urgency,
                xpValue = (estimatedMinutes * 2).coerceIn(10, 80),
                aiAdvice = aiAdvice
            )
            val newTaskId = taskRepo.addTask(task, subtasks)
            showDopamineBanner("کار جدید ثبت شد! قدم‌های کوچک بردار 🌟")

            // Metis API Automatic Breakdown Service for complex tasks with no initial subtasks
            if (subtasks.isEmpty() && metisBreakdownService.isComplexTask(title, description, estimatedMinutes)) {
                _breakdownTaskId.value = newTaskId
                val breakdownResult = metisBreakdownService.autoBreakdownTask(newTaskId, title, description)
                _breakdownTaskId.value = null
                if (breakdownResult.isSuccess) {
                    val generated = breakdownResult.getOrNull() ?: emptyList()
                    if (generated.isNotEmpty()) {
                        showDopamineBanner("تسک «$title» با هوش مصنوعی متیس خودکار به ${generated.size} زیرکار عملی خرد شد! 🪄")
                    }
                }
            }
        }
    }

    fun toggleTaskComplete(task: TaskEntity) {
        viewModelScope.launch {
            val completed = taskRepo.toggleTaskComplete(task)
            if (completed) {
                showDopamineBanner("+${task.xpValue} XP دوپامین دریافت شد! عالی بودی قهرمان! 🎉")
            }
        }
    }

    fun toggleSubtaskComplete(subtask: SubTaskEntity) {
        viewModelScope.launch {
            taskRepo.toggleSubtaskComplete(subtask)
            if (!subtask.isCompleted) {
                showDopamineBanner("+5 XP برای میکرو-قدم! ⚡")
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepo.deleteTask(task)
        }
    }

    // AI Feature: Brain Dump
    fun runBrainDump(rawText: String) {
        if (rawText.isBlank()) return
        viewModelScope.launch {
            _brainDumpState.value = AiOperationState.Loading
            val result = aiRepo.parseBrainDump(rawText)
            result.onSuccess { parsedTasks ->
                _brainDumpState.value = AiOperationState.Success(parsedTasks)
            }.onFailure { err ->
                _brainDumpState.value = AiOperationState.Error(err.message ?: "خطا در تجزیه افکار توسط هوش مصنوعی")
            }
        }
    }

    fun importBrainDumpTasks(tasksToImport: List<ParsedBrainDumpTask>) {
        viewModelScope.launch {
            for (pTask in tasksToImport) {
                taskRepo.addTask(
                    TaskEntity(
                        title = pTask.title,
                        description = pTask.description,
                        category = pTask.category,
                        energyLevel = pTask.energyLevel,
                        estimatedMinutes = pTask.estimatedMinutes,
                        urgency = pTask.urgency,
                        xpValue = (pTask.estimatedMinutes * 2).coerceIn(15, 60),
                        aiAdvice = pTask.aiAdvice
                    ),
                    pTask.subtasks
                )
            }
            _brainDumpState.value = AiOperationState.Idle
            showDopamineBanner("${tasksToImport.size} تسک هوشمندانه به لیست اضافه شد! ذهنت سبک شد 🧠✨")
        }
    }

    fun resetBrainDumpState() {
        _brainDumpState.value = AiOperationState.Idle
    }

    // AI Feature: Micro-steps Breakdown (Executive Dysfunction Buster)
    fun breakdownTask(task: TaskEntity) {
        viewModelScope.launch {
            _breakdownTaskId.value = task.id
            val result = aiRepo.breakdownTask(task.title, task.description)
            result.onSuccess { steps ->
                taskRepo.addSubtasksToTask(task.id, steps.map { Pair(it.title, it.estimatedMinutes) })
                showDopamineBanner("تسک خرد شد! حالا شروع کردنش خیلی راحته 🪄")
            }.onFailure { err ->
                showDopamineBanner("خطا در خرد کردن تسک: ${err.message}")
            }
            _breakdownTaskId.value = null
        }
    }

    // AI Feature: Paralysis Breaker (I'm Overwhelmed!)
    fun rescueParalysis(userFeeling: String) {
        viewModelScope.launch {
            _paralysisState.value = AiOperationState.Loading
            val currentTasks = tasks.value
            val result = aiRepo.rescueParalysis(currentTasks, userFeeling)
            result.onSuccess { rescue ->
                _paralysisState.value = AiOperationState.Success(rescue)
            }.onFailure { err ->
                _paralysisState.value = AiOperationState.Error(err.message ?: "خطا در برقراری ارتباط")
            }
        }
    }

    fun dismissParalysisDialog() {
        _paralysisState.value = AiOperationState.Idle
    }

    // AI Feature: Multi-turn Chatbot (Body Double, Executive Coach, Psychologist)
    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _isChatSending.value = true
            taskRepo.addChatMessage("user", text)
            val history = chatMessages.value
            val provider = aiProvider.value
            val role = chatRole.value

            val result = if (provider == "GEMINI" && geminiApiKey.value.isNotBlank()) {
                geminiRepo.chatWithRole(role, history, text)
            } else {
                aiRepo.chatWithBodyDouble(history, text)
            }

            result.onSuccess { responseText ->
                taskRepo.addChatMessage("assistant", responseText)
            }.onFailure { err ->
                taskRepo.addChatMessage("assistant", "پاسخ هوش مصنوعی: ${err.message}")
            }
            _isChatSending.value = false
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            taskRepo.clearChat()
        }
    }

    // AI Feature: Google Maps Grounding & Outdoor Errands Assistant
    fun searchErrandsWithMaps(query: String, locationContext: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _errandsState.value = AiOperationState.Loading
            val result = if (geminiApiKey.value.isNotBlank()) {
                geminiRepo.searchNearbyErrands(query, locationContext)
            } else {
                // Smart fallback with location reasoning
                val metisPrompt = "مکان‌ها و کارهای بیرون برای: $query در منطقه $locationContext. لطفا ۳ مکان واقعی همراه با تخمین زمان و راهنمای ضد حواس پرتی adhdAntiDistractionTip در قالب JSON بده."
                aiRepo.parseBrainDump(metisPrompt).map { parsed ->
                    parsed.map {
                        ErrandPlaceItem(
                            name = it.title,
                            category = it.category,
                            addressOrLocation = locationContext,
                            openStatus = "فعال و باز",
                            estimatedTimeMinutes = it.estimatedMinutes,
                            adhdAntiDistractionTip = it.aiAdvice
                        )
                    }
                }
            }

            result.onSuccess { places ->
                _errandsState.value = AiOperationState.Success(places)
            }.onFailure { err ->
                _errandsState.value = AiOperationState.Error(err.message ?: "خطا در جستجوی مکان‌ها")
            }
        }
    }

    fun addErrandAsTask(place: ErrandPlaceItem) {
        addTask(
            title = "انجام کار بیرون: ${place.name}",
            description = "آدرس: ${place.addressOrLocation} | وضعیت: ${place.openStatus}",
            category = "شخصی",
            energyLevel = "MEDIUM",
            estimatedMinutes = place.estimatedTimeMinutes,
            aiAdvice = place.adhdAntiDistractionTip,
            subtasks = listOf("بررسی کلید و کیف پول قبل خروج", "مسیریابی تا ${place.name}", "انجام کار و بازگشت به خانه")
        )
        showDopamineBanner("کار بیرون از خانه به لیست تسک‌ها اضافه شد! 🚗✨")
    }

    fun resetErrandsState() {
        _errandsState.value = AiOperationState.Idle
    }

    // AI Feature: Audio Transcription (Voice Brain Dump)
    fun transcribeAudio(base64Audio: String, mimeType: String = "audio/mp3") {
        viewModelScope.launch {
            _transcriptionState.value = AiOperationState.Loading
            val res = geminiRepo.transcribeAudio(base64Audio, mimeType)
            res.onSuccess { text ->
                _transcriptionState.value = AiOperationState.Success(text)
            }.onFailure { err ->
                _transcriptionState.value = AiOperationState.Error(err.message ?: "خطا در تبدیل صدا به متن")
            }
        }
    }

    fun resetTranscriptionState() {
        _transcriptionState.value = AiOperationState.Idle
    }

    // Focus Session
    fun saveFocusSession(taskId: Long?, title: String, minutes: Int) {
        viewModelScope.launch {
            taskRepo.recordFocusSession(taskId, title, minutes)
            showDopamineBanner("آفرین! $minutes دقیقه تمرکز عمیق داشتی 🎯 (+${minutes * 2} XP)")
        }
    }

    // Settings & Tokens
    fun saveMetisToken(token: String) {
        viewModelScope.launch {
            prefsRepo.saveMetisToken(token)
            showDopamineBanner("توکن متیس ذخیره شد ✅")
        }
    }

    fun saveMetisBaseUrl(url: String) {
        viewModelScope.launch {
            prefsRepo.saveMetisBaseUrl(url)
            showDopamineBanner("آدرس سرور متیس ذخیره شد ✅")
        }
    }

    fun saveGeminiApiKey(key: String) {
        viewModelScope.launch {
            prefsRepo.saveGeminiApiKey(key)
            showDopamineBanner("کلید جمینای ذخیره شد ✅")
        }
    }

    fun saveAiModel(model: String) {
        viewModelScope.launch {
            prefsRepo.saveAiModel(model)
            showDopamineBanner("مدل هوش مصنوعی تغییر کرد ✅")
        }
    }

    fun testMetisConnection() {
        viewModelScope.launch {
            _connectionTestState.value = AiOperationState.Loading
            val res = aiRepo.testConnection()
            res.onSuccess { reply ->
                _connectionTestState.value = AiOperationState.Success(reply)
            }.onFailure { err ->
                _connectionTestState.value = AiOperationState.Error(err.message ?: "اتصال برقرار نشد")
            }
        }
    }

    fun resetConnectionTestState() {
        _connectionTestState.value = AiOperationState.Idle
    }
}
