package com.example.data.repository

import com.example.data.model.ChatMessageEntity
import com.example.data.model.TaskEntity
import com.example.data.remote.ChatCompletionRequest
import com.example.data.remote.ChatMessageDto
import com.example.data.remote.MetisApiService
import com.example.data.remote.ParsedBrainDumpTask
import com.example.data.remote.ParsedMicroStep
import com.example.data.remote.ParalysisRescueResult
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

class MetisAiRepository(
    private val prefsRepo: UserPreferencesRepository
) {

    private suspend fun getApiService(): Pair<MetisApiService, String> {
        val baseUrl = prefsRepo.metisBaseUrl.first()
        val token = prefsRepo.metisToken.first()

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .build()

        val moshi = com.squareup.moshi.Moshi.Builder()
            .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return Pair(retrofit.create(MetisApiService::class.java), token)
    }

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val (api, token) = getApiService()
            val model = prefsRepo.aiModel.first()

            val request = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessageDto(
                        role = "system",
                        content = "You are a helpful assistant. Reply with: 'اتصال به متیس با موفقیت برقرار شد!'."
                    ),
                    ChatMessageDto(
                        role = "user",
                        content = "سلام تست اتصال"
                    )
                ),
                maxTokens = 50
            )

            val response = api.createChatCompletion("Bearer $token", request)
            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content ?: "اتصال برقرار شد!"
                Result.success(content.trim())
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                Result.failure(Exception("خطا از سرور متیس (${response.code()}): $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parseBrainDump(rawText: String): Result<List<ParsedBrainDumpTask>> = withContext(Dispatchers.IO) {
        try {
            val (api, token) = getApiService()
            val model = prefsRepo.aiModel.first()

            val systemPrompt = """
                تو یک دستیار متخصص ADHD (نقص توجه و بیش‌فعالی) هستی.
                کاربر افکار نامنظم، پریشان یا شلوغ ذهنی‌اش (Brain Dump) را می‌نویسد.
                وظیفه تو این است که این آشفتگی ذهنی را بدون قضاوت و با همدلی عمیق، به لیست وظایف شفاف، عملی و خرد تبدیل کنی.
                
                قوانین مهم برای افراد ADHD:
                1. عنوان تسک‌ها باید با فعل روشن و ترغیب‌کننده باشد.
                2. برای مقابله با کوری زمانی (Time Blindness)، زمان واقع‌بینانه در نظر بگیر.
                3. سطح انرژی (energyLevel) را یکی از این‌ها بگذار: "LOW", "MEDIUM", "HIGH"
                4. دسته‌بندی (category): یکی از این‌ها: "کار", "شخصی", "سلامت", "خلاقیت", "مطالعه"
                5. برای هر تسک، ۳ تا ۴ ریز‌قدم (subtasks) بسیار آسان و بدون اصطکاک پیشنهاد بده که شروع کردنش راحت باشد.
                6. یک توصیه کوتاه و آرامش‌بخش (aiAdvice) بنویس.
                
                خروجی فقط و فقط باید یک آرایه معتبر JSON بدون هیچ متن اضافی یا توضیحات اضافی باشد:
                [
                  {
                    "title": "تماس با دندانپزشک",
                    "description": "برای چکاپ و درد دندان",
                    "category": "سلامت",
                    "energyLevel": "LOW",
                    "estimatedMinutes": 10,
                    "urgency": "HIGH",
                    "aiAdvice": "فقط شماره رو بگیر، اگر مشغول بود پیام بذار، هیچ استرسی نداره!",
                    "subtasks": ["پیدا کردن شماره در پیامک‌ها", "گرفتن شماره و تعیین وقت", "ثبت زمان در تقویم"]
                  }
                ]
            """.trimIndent()

            val request = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessageDto(role = "system", content = systemPrompt),
                    ChatMessageDto(role = "user", content = rawText)
                ),
                temperature = 0.5
            )

            val response = api.createChatCompletion("Bearer $token", request)
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("خطا در ارتباط با متیس: ${response.code()}"))
            }

            val rawContent = response.body()?.choices?.firstOrNull()?.message?.content ?: "[]"
            val jsonString = extractJsonArrayString(rawContent)

            val resultList = mutableListOf<ParsedBrainDumpTask>()
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val subtasksList = mutableListOf<String>()
                if (obj.has("subtasks")) {
                    val subs = obj.getJSONArray("subtasks")
                    for (j in 0 until subs.length()) {
                        subtasksList.add(subs.getString(j))
                    }
                }
                resultList.add(
                    ParsedBrainDumpTask(
                        title = obj.optString("title", "کار جدید"),
                        description = obj.optString("description", ""),
                        category = obj.optString("category", "کار"),
                        energyLevel = obj.optString("energyLevel", "MEDIUM"),
                        estimatedMinutes = obj.optInt("estimatedMinutes", 15),
                        urgency = obj.optString("urgency", "MEDIUM"),
                        aiAdvice = obj.optString("aiAdvice", "یک نفس عمیق بکش و شروع کن."),
                        subtasks = subtasksList
                    )
                )
            }
            Result.success(resultList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun breakdownTask(taskTitle: String, taskDescription: String): Result<List<ParsedMicroStep>> = withContext(Dispatchers.IO) {
        try {
            val (api, token) = getApiService()
            val model = prefsRepo.aiModel.first()

            val systemPrompt = """
                تو متخصص مبارزه با فلج اجرایی (Executive Dysfunction) در افراد ADHD هستی.
                کاربر عنوان یک تسک را می‌دهد که شاید سنگین یا ترسناک به نظر برسد.
                وظیفه تو این است که آن را به ۳ تا ۶ «میکرو-قدم» (Micro-steps) فوق‌العاده کوچک و بدون اصطکاک بشکنی.
                قانون طلایی: قدم اول باید آن‌قدر مضحک و ساده باشد که مغز نتواند در برابر انجام دادنش مقاومت کند (مثلاً ۳۰ ثانیه: فقط فایل را باز کن، یا صندلی را مرتب کن).
                
                خروجی فقط و فقط یک آرایه JSON معتبر:
                [
                  {"title": "لپ‌تاپ را باز کن و فایل پروژه را باز کن", "estimatedMinutes": 1},
                  {"title": "فقط دو خط اول یا یک تیتر بنویس", "estimatedMinutes": 3},
                  {"title": "یک پاراگراف کوتاه تایپ کن", "estimatedMinutes": 5}
                ]
            """.trimIndent()

            val userPrompt = "تسک: $taskTitle\nتوضیحات: $taskDescription"

            val request = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessageDto(role = "system", content = systemPrompt),
                    ChatMessageDto(role = "user", content = userPrompt)
                ),
                temperature = 0.6
            )

            val response = api.createChatCompletion("Bearer $token", request)
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("خطا در پاسخ هوش مصنوعی: ${response.code()}"))
            }

            val rawContent = response.body()?.choices?.firstOrNull()?.message?.content ?: "[]"
            val jsonString = extractJsonArrayString(rawContent)
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<ParsedMicroStep>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ParsedMicroStep(
                        title = obj.optString("title", "قدم ${i + 1}"),
                        estimatedMinutes = obj.optInt("estimatedMinutes", 3)
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rescueParalysis(
        tasks: List<TaskEntity>,
        userFeeling: String
    ): Result<ParalysisRescueResult> = withContext(Dispatchers.IO) {
        try {
            val (api, token) = getApiService()
            val model = prefsRepo.aiModel.first()

            val tasksSummary = tasks.filter { !it.isCompleted }.take(10).joinToString("\n") {
                "- ${it.title} (دسته: ${it.category}، انرژی: ${it.energyLevel}، تخمین: ${it.estimatedMinutes} دقیقه)"
            }

            val systemPrompt = """
                تو دکمه اضطراری رفع فلج ذهنی (Paralysis Breaker) برای افراد ADHD هستی.
                کاربر دچار درماندگی و سردرگمی (Overwhelm) شده و نمی‌داند از کجا شروع کند.
                حال یا احساس فعلی کاربر: "$userFeeling"
                لیست کارهای باز او:
                $tasksSummary
                
                وظیفه تو:
                1. دقیقا ONE (یک کار) را برایش انتخاب کن که کمترین مقاومت ذهنی را ایجاد کند و زودترین دوپامین را به او بدهد.
                2. یک پیام گرم، بدون هیچ سرزنش و پر از محبت و آرامش (warmMessage) برایش بنویس.
                3. یک حرکت ۲ دقیقه‌ای بسیار ساده برای شروع (firstMicroStep) بگو.
                
                خروجی فقط یک JSON Object معتبر باشد:
                {
                  "selectedTaskTitle": "نام دقیق یا خلاصه کار انتخاب شده",
                  "warmMessage": "متن آرامش‌بخش و انگیزه دهنده",
                  "firstMicroStep": "قدم فوق‌العاده ساده شروع",
                  "estimatedMinutes": 3
                }
            """.trimIndent()

            val request = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessageDto(role = "system", content = systemPrompt),
                    ChatMessageDto(role = "user", content = "من واقعاً گیجم و گیر کردم. کمکم کن یک کار انتخاب کنم.")
                ),
                temperature = 0.7
            )

            val response = api.createChatCompletion("Bearer $token", request)
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("خطا در متیس: ${response.code()}"))
            }

            val rawContent = response.body()?.choices?.firstOrNull()?.message?.content ?: "{}"
            val jsonString = extractJsonObjectString(rawContent)
            val obj = JSONObject(jsonString)

            Result.success(
                ParalysisRescueResult(
                    selectedTaskTitle = obj.optString("selectedTaskTitle", tasks.firstOrNull()?.title ?: "یک کار ساده"),
                    warmMessage = obj.optString("warmMessage", "آرام باش، مغزت فقط به یک شروع خیلی کوچیک نیاز داره. بیا فقط همین یک کار رو انجام بدیم."),
                    firstMicroStep = obj.optString("firstMicroStep", "فقط صندلی‌ات رو راحت کن و ۳۰ ثانیه چشات رو ببند."),
                    estimatedMinutes = obj.optInt("estimatedMinutes", 3)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun chatWithBodyDouble(
        history: List<ChatMessageEntity>,
        userMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val (api, token) = getApiService()
            val model = prefsRepo.aiModel.first()

            val systemMessage = ChatMessageDto(
                role = "system",
                content = """
                    تو «بادی دابل» (Body Double) و دوست و مربی اختصاصی افراد دارای ADHD هستی.
                    پشتوانه علمی: حضور یک همراه (Body Doubling) به مغز ADHD کمک می‌کند ترشح دوپامین و تمرکز را حفظ کند و از تعلل دوری کند.
                    
                    ویژگی‌های شخصیتی تو:
                    - صمیمی، پرانرژی، بی نهایت مهربان و فاقد هرگونه سرزنش یا ایجاد حس گناه.
                    - به زبان فارسی دوستانه و محاوره‌ای یا روان صحبت کن.
                    - پاسخ‌هایت بیش از حد طولانی نباشند (مغز ADHD متن‌های بیش از حد طولانی را رد می‌کند).
                    - اگر کاربر کار کوچکی را تمام کرد، طوفانی از تشویق و دوپامین به او بده!
                    - اگر گیج شد، کمکش کن کار را بشکند یا یک تایمر ۱۵ دقیقه‌ای با هم بگذارید.
                """.trimIndent()
            )

            val messages = mutableListOf<ChatMessageDto>()
            messages.add(systemMessage)

            // Add last 8 messages for context
            val recentHistory = history.takeLast(8)
            for (msg in recentHistory) {
                messages.add(
                    ChatMessageDto(
                        role = if (msg.sender == "user") "user" else "assistant",
                        content = msg.message
                    )
                )
            }
            messages.add(ChatMessageDto(role = "user", content = userMessage))

            val request = ChatCompletionRequest(
                model = model,
                messages = messages,
                temperature = 0.7
            )

            val response = api.createChatCompletion("Bearer $token", request)
            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content ?: "من کنارت هستم، بگو قدم بعدی چیه؟"
                Result.success(content.trim())
            } else {
                Result.failure(Exception("خطا در ارتباط با متیس (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractJsonArrayString(raw: String): String {
        val trimmed = raw.trim()
        val startIndex = trimmed.indexOf('[')
        val lastIndex = trimmed.lastIndexOf(']')
        return if (startIndex != -1 && lastIndex != -1 && lastIndex > startIndex) {
            trimmed.substring(startIndex, lastIndex + 1)
        } else {
            trimmed
        }
    }

    private fun extractJsonObjectString(raw: String): String {
        val trimmed = raw.trim()
        val startIndex = trimmed.indexOf('{')
        val lastIndex = trimmed.lastIndexOf('}')
        return if (startIndex != -1 && lastIndex != -1 && lastIndex > startIndex) {
            trimmed.substring(startIndex, lastIndex + 1)
        } else {
            trimmed
        }
    }
}
