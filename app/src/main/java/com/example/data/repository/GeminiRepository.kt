package com.example.data.repository

import com.example.data.model.ChatMessageEntity
import com.example.data.remote.ErrandPlaceItem
import com.example.data.remote.GeminiApiService
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiGenerateRequest
import com.example.data.remote.GeminiGenerationConfig
import com.example.data.remote.GeminiInlineData
import com.example.data.remote.GeminiPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class GeminiRepository(
    private val prefsRepo: UserPreferencesRepository
) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    private val moshi = com.squareup.moshi.Moshi.Builder()
        .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()

    private val apiService: GeminiApiService = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(GeminiApiService::class.java)

    private suspend fun getApiKey(): String {
        return prefsRepo.geminiApiKey.first()
    }

    suspend fun chatWithRole(
        role: String,
        history: List<ChatMessageEntity>,
        userMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("کلید API جمینای تنظیم نشده است. لطفاً در بخش تنظیمات کلید را وارد کنید."))
            }

            // Select Model based on complexity
            val model = when (role) {
                "BODY_DOUBLE" -> "gemini-3.1-flash-lite-preview"
                "EXECUTIVE_COACH" -> "gemini-3.5-flash"
                "PSYCHOLOGIST" -> "gemini-3.1-pro-preview"
                else -> "gemini-3.5-flash"
            }

            // System Instruction based on Role
            val systemPrompt = when (role) {
                "BODY_DOUBLE" -> """
                    تو یک «بادی دابل» (Body Double) و دوست و حامی بسیار پرانرژی و دوپامینی برای افراد دارای ADHD هستی.
                    پاسخ‌هایت باید کوتاه، فوق‌العاده صمیمی، پرحرارت و بدون هیچ قضاوت یا سرزنش باشد.
                    هدف تو این است که کاربر احساس تنهایی نکند و برای برداشتن هر میکرو-قدم تشویق شود.
                """.trimIndent()

                "EXECUTIVE_COACH" -> """
                    تو یک «مربی عملکرد اجرایی» (Executive Function Coach) برای افراد دارای ADHD هستی.
                    تخصص تو شکستن کارهای ترسناک، مهار فلج تصمیم‌گیری (Decision Paralysis) و اولویت‌بندی منطقی بر اساس سطح قاشق انرژی است.
                    گام‌به‌گام و بسیار شمرده صحبت کن.
                """.trimIndent()

                "PSYCHOLOGIST" -> """
                    تو یک «روانشناس متخصص ADHD و خودمهربانی» هستی.
                    تخصص تو کمک به کاربر در مواجهه با حساسیت به طرد شدن (RSD - Rejection Sensitive Dysphoria)، احساس گناه بابت تعلل، فرسودگی ذهنی و بازسازی اعتمادبه‌نفس است.
                    لحن تو عمیقاً همدلانه، آرامش‌بخش، علمی و فاقد هرگونه سرزنش است.
                """.trimIndent()

                else -> "تو یک دستیار هوشمند و همدل برای افراد ADHD هستی."
            }

            val contents = mutableListOf<GeminiContent>()

            // Add previous messages (last 8 turns)
            val recent = history.takeLast(8)
            for (msg in recent) {
                contents.add(
                    GeminiContent(
                        role = if (msg.sender == "user") "user" else "model",
                        parts = listOf(GeminiPart(text = msg.message))
                    )
                )
            }
            contents.add(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = userMessage))
                )
            )

            val request = GeminiGenerateRequest(
                contents = contents,
                systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
                generationConfig = GeminiGenerationConfig(temperature = 0.7f)
            )

            val response = apiService.generateContent(model, apiKey, request)
            if (response.isSuccessful) {
                val candidateText = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "من کنارت هستم، قدم بعدی چیه؟"
                Result.success(candidateText.trim())
            } else {
                val err = response.body()?.error?.message ?: response.errorBody()?.string() ?: "خطای سرور جمینای"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchNearbyErrands(
        query: String,
        locationContext: String = "تهران"
    ): Result<List<ErrandPlaceItem>> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("کلید API جمینای تنظیم نشده است."))
            }

            // Using gemini-3.5-flash with Google Maps tool
            val model = "gemini-3.5-flash"

            val prompt = """
                کاربر دارای بیش‌فعالی و نقص توجه (ADHD) قصد دارد این کار یا خرید بیرون از خانه را انجام دهد:
                "$query" در محدوده یا شهر: "$locationContext"
                
                برای افراد ADHD بیرون رفتن و انجام کارها به دلیل کوری زمانی و عدم قطعیت مکان بسیار اضطراب‌آور است (Errand Paralysis).
                اطلاعات ۳ تا ۴ مکان واقعی و مناسب را پیدا کن و دقیقاً در قالب این آرایه JSON بازگردان:
                [
                  {
                    "name": "نام دقیق مکان یا فروشگاه یا مرکز",
                    "category": "داروخانه / فروشگاه / کافه / خدمات",
                    "addressOrLocation": "نشانی یا خیابان و منطقه",
                    "openStatus": "وضعیت فعالیت (مثلاً باز است تا ساعت ۲۲)",
                    "estimatedTimeMinutes": 35,
                    "adhdAntiDistractionTip": "قبل خروج فقط کارت و کلید رو بردار، لیست ۲ تایی بنویس تا چیز اضافی نخری"
                  }
                ]
                فقط و فقط یک آرایه JSON معتبر ارسال کن.
            """.trimIndent()

            val request = GeminiGenerateRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = prompt))
                    )
                ),
                tools = listOf(
                    mapOf("google_search" to emptyMap<String, Any>())
                ),
                generationConfig = GeminiGenerationConfig(temperature = 0.4f)
            )

            val response = apiService.generateContent(model, apiKey, request)
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("خطا در جستجوی مکان‌ها: ${response.code()}"))
            }

            val rawText = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "[]"
            val jsonStr = extractJsonArray(rawText)
            val jsonArray = JSONArray(jsonStr)
            val result = mutableListOf<ErrandPlaceItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                result.add(
                    ErrandPlaceItem(
                        name = obj.optString("name", "مکان مورد نظر"),
                        category = obj.optString("category", "مکان"),
                        addressOrLocation = obj.optString("addressOrLocation", "محدوده انتخابی"),
                        openStatus = obj.optString("openStatus", "اطلاعات در دسترس نیست"),
                        estimatedTimeMinutes = obj.optInt("estimatedTimeMinutes", 30),
                        adhdAntiDistractionTip = obj.optString("adhdAntiDistractionTip", "فقط روی هدف اصلی تمرکز کن.")
                    )
                )
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun transcribeAudio(
        base64Audio: String,
        mimeType: String = "audio/mp3"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("کلید API برای رونویسی صوتی تنظیم نشده است."))
            }

            val model = "gemini-3.5-transcribe"

            val request = GeminiGenerateRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(
                            GeminiPart(text = "لطفاً این صوت را با دقت بالا به متن فارسی تبدیل کن بدون هیچ توضیح اضافی."),
                            GeminiPart(inlineData = GeminiInlineData(mimeType = mimeType, data = base64Audio))
                        )
                    )
                )
            )

            val response = apiService.generateContent(model, apiKey, request)
            if (response.isSuccessful) {
                val text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                Result.success(text.trim())
            } else {
                Result.failure(Exception("خطا در تبدیل صوت به متن: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractJsonArray(raw: String): String {
        val trimmed = raw.trim()
        val start = trimmed.indexOf('[')
        val end = trimmed.lastIndexOf(']')
        return if (start != -1 && end != -1 && end > start) {
            trimmed.substring(start, end + 1)
        } else {
            trimmed
        }
    }
}
