package com.example.data.remote

import com.squareup.moshi.Json

data class GeminiGenerateRequest(
    @field:Json(name = "contents") val contents: List<GeminiContent>,
    @field:Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null,
    @field:Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    @field:Json(name = "tools") val tools: List<Map<String, Any>>? = null
)

data class GeminiContent(
    @field:Json(name = "role") val role: String? = null,
    @field:Json(name = "parts") val parts: List<GeminiPart>
)

data class GeminiPart(
    @field:Json(name = "text") val text: String? = null,
    @field:Json(name = "inlineData") val inlineData: GeminiInlineData? = null
)

data class GeminiInlineData(
    @field:Json(name = "mimeType") val mimeType: String,
    @field:Json(name = "data") val data: String
)

data class GeminiGenerationConfig(
    @field:Json(name = "temperature") val temperature: Float? = 0.7f,
    @field:Json(name = "topP") val topP: Float? = 0.95f,
    @field:Json(name = "maxOutputTokens") val maxOutputTokens: Int? = null
)

data class GeminiGenerateResponse(
    @field:Json(name = "candidates") val candidates: List<GeminiCandidate>? = null,
    @field:Json(name = "error") val error: GeminiError? = null
)

data class GeminiCandidate(
    @field:Json(name = "content") val content: GeminiContent? = null,
    @field:Json(name = "finishReason") val finishReason: String? = null,
    @field:Json(name = "groundingMetadata") val groundingMetadata: Map<String, Any>? = null
)

data class GeminiError(
    @field:Json(name = "code") val code: Int? = null,
    @field:Json(name = "message") val message: String? = null,
    @field:Json(name = "status") val status: String? = null
)

// Data class for Maps Errand Result
data class ErrandPlaceItem(
    val name: String,
    val category: String,
    val addressOrLocation: String,
    val openStatus: String,
    val estimatedTimeMinutes: Int,
    val adhdAntiDistractionTip: String
)
