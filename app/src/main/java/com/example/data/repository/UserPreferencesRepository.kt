package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "adhd_prefs")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        private val KEY_METIS_TOKEN = stringPreferencesKey("metis_token")
        private val KEY_METIS_BASE_URL = stringPreferencesKey("metis_base_url")
        private val KEY_GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        private val KEY_AI_PROVIDER = stringPreferencesKey("ai_provider")
        private val KEY_CHAT_ROLE = stringPreferencesKey("chat_role")
        private val KEY_AI_MODEL = stringPreferencesKey("ai_model")
        private val KEY_USER_LEVEL = intPreferencesKey("user_level")
        private val KEY_USER_XP = intPreferencesKey("user_xp")
        private val KEY_TODAY_ENERGY = stringPreferencesKey("today_energy")

        const val DEFAULT_TOKEN = "tpsg-6PCI4FkbraNkxJ9hjDjYGiuWHIjJ3Ad"
        const val DEFAULT_BASE_URL = "https://api.metisai.ir/v1/"
        const val DEFAULT_MODEL = "gpt-4o-mini"
    }

    val geminiApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        val userSaved = prefs[KEY_GEMINI_API_KEY]
        if (!userSaved.isNullOrBlank()) {
            userSaved
        } else {
            try {
                val key = BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String
                if (key != null && key != "MY_GEMINI_API_KEY") key else ""
            } catch (_: Exception) {
                ""
            }
        }
    }

    val aiProvider: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_AI_PROVIDER] ?: "METIS" // "METIS" or "GEMINI"
    }

    val chatRole: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CHAT_ROLE] ?: "BODY_DOUBLE"
    }

    val metisToken: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_METIS_TOKEN] ?: try {
            val buildConfigToken = BuildConfig::class.java.getField("METIS_API_KEY").get(null) as? String
            if (!buildConfigToken.isNullOrBlank()) buildConfigToken else DEFAULT_TOKEN
        } catch (_: Exception) {
            DEFAULT_TOKEN
        }
    }

    val metisBaseUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_METIS_BASE_URL] ?: try {
            val buildConfigUrl = BuildConfig::class.java.getField("METIS_BASE_URL").get(null) as? String
            if (!buildConfigUrl.isNullOrBlank()) buildConfigUrl else DEFAULT_BASE_URL
        } catch (_: Exception) {
            DEFAULT_BASE_URL
        }
    }

    val aiModel: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_AI_MODEL] ?: DEFAULT_MODEL
    }

    val userLevel: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_LEVEL] ?: 1
    }

    val userXp: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_XP] ?: 40
    }

    val todayEnergy: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_TODAY_ENERGY] ?: "MEDIUM"
    }

    suspend fun saveMetisToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_METIS_TOKEN] = token.trim()
        }
    }

    suspend fun saveMetisBaseUrl(url: String) {
        var cleanUrl = url.trim()
        if (!cleanUrl.endsWith("/")) cleanUrl += "/"
        context.dataStore.edit { prefs ->
            prefs[KEY_METIS_BASE_URL] = cleanUrl
        }
    }

    suspend fun saveAiModel(model: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AI_MODEL] = model.trim()
        }
    }

    suspend fun saveGeminiApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GEMINI_API_KEY] = key.trim()
        }
    }

    suspend fun saveAiProvider(provider: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AI_PROVIDER] = provider
        }
    }

    suspend fun saveChatRole(role: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CHAT_ROLE] = role
        }
    }

    suspend fun addXp(amount: Int): Pair<Int, Boolean> {
        var newLevel = 1
        var leveledUp = false
        context.dataStore.edit { prefs ->
            val currentXp = prefs[KEY_USER_XP] ?: 40
            val currentLevel = prefs[KEY_USER_LEVEL] ?: 1
            val updatedXp = currentXp + amount
            val calculatedLevel = (updatedXp / 100) + 1
            if (calculatedLevel > currentLevel) {
                leveledUp = true
            }
            newLevel = calculatedLevel
            prefs[KEY_USER_XP] = updatedXp
            prefs[KEY_USER_LEVEL] = calculatedLevel
        }
        return Pair(newLevel, leveledUp)
    }

    suspend fun setTodayEnergy(energy: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TODAY_ENERGY] = energy
        }
    }
}
