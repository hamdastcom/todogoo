package com.example.data.remote

import com.squareup.moshi.Json

data class ChatCompletionRequest(
    @field:Json(name = "model") val model: String = "gpt-4o-mini",
    @field:Json(name = "messages") val messages: List<ChatMessageDto>,
    @field:Json(name = "temperature") val temperature: Double = 0.7,
    @field:Json(name = "max_tokens") val maxTokens: Int? = null
)

data class ChatMessageDto(
    @field:Json(name = "role") val role: String,
    @field:Json(name = "content") val content: String
)

data class ChatCompletionResponse(
    @field:Json(name = "id") val id: String? = null,
    @field:Json(name = "choices") val choices: List<ChoiceDto> = emptyList(),
    @field:Json(name = "error") val error: MetisErrorDto? = null
)

data class ChoiceDto(
    @field:Json(name = "index") val index: Int? = 0,
    @field:Json(name = "message") val message: ChatMessageDto,
    @field:Json(name = "finish_reason") val finishReason: String? = null
)

data class MetisErrorDto(
    @field:Json(name = "message") val message: String?,
    @field:Json(name = "type") val type: String? = null,
    @field:Json(name = "code") val code: String? = null
)

// Data classes for Parsed AI Responses
data class ParsedBrainDumpTask(
    val title: String,
    val description: String = "",
    val category: String = "کار",
    val energyLevel: String = "MEDIUM",
    val estimatedMinutes: Int = 15,
    val urgency: String = "MEDIUM",
    val aiAdvice: String = "",
    val subtasks: List<String> = emptyList()
)

data class ParsedMicroStep(
    val title: String,
    val estimatedMinutes: Int = 3
)

data class ParalysisRescueResult(
    val selectedTaskTitle: String,
    val warmMessage: String,
    val firstMicroStep: String,
    val estimatedMinutes: Int = 5
)
