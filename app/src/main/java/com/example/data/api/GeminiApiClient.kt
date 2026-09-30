package com.example.data.api

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiApiClient(private val context: Context) {

    companion object {
        const val MODEL_FLASH = "gemini-3.5-flash"
        const val MODEL_PRO = "gemini-3.1-pro-preview"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
        private const val PREFS_NAME = "omni_ai_prefs"
        private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"
        private const val KEY_SELECTED_MODEL = "selected_model"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getApiKey(): String {
        val customKey = prefs.getString(KEY_CUSTOM_API_KEY, "")?.trim()
        if (!customKey.isNullOrEmpty()) {
            return customKey
        }
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }
        return if (buildKey.isNotEmpty() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
    }

    fun setCustomApiKey(key: String) {
        prefs.edit().putString(KEY_CUSTOM_API_KEY, key.trim()).apply()
    }

    fun getSelectedModel(): String {
        return prefs.getString(KEY_SELECTED_MODEL, MODEL_FLASH) ?: MODEL_FLASH
    }

    fun setSelectedModel(model: String) {
        prefs.edit().putString(KEY_SELECTED_MODEL, model).apply()
    }

    suspend fun generateResponse(
        prompt: String,
        history: List<Pair<String, String>> = emptyList(), // role to content
        imageBitmap: Bitmap? = null,
        modelOverride: String? = null
    ): Result<AiResponse> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                Exception("Gemini API key is not configured. Please enter your Gemini API key in Settings.")
            )
        }

        val model = modelOverride ?: getSelectedModel()
        val url = "$BASE_URL$model:generateContent?key=$apiKey"

        try {
            val rootJson = JSONObject()

            // System Instruction
            val systemObj = JSONObject().apply {
                val partsArray = JSONArray()
                partsArray.put(JSONObject().apply {
                    put("text", """
                        You are Omni AI, the most powerful and comprehensive executive AI assistant.
                        You excel at solving problems, scheduling automated tasks, writing high-quality code, brainstorming, answering complex questions, language translation, and organizing lives.
                        You seamlessly speak and comprehend English, Hindi, and Hinglish. Keep your responses crisp, direct, engaging, and highly intelligent.
                        
                        SPECIAL EXECUTIVE CAPABILITIES:
                        When the user asks you to schedule a task, set a reminder, or create an alarm, include this executable JSON block on a new line at the end of your response:
                        ACTION_SCHEDULE: {"title": "<Concise Task Title>", "minutesFromNow": <integer_minutes_or_0_if_unspecified>, "category": "<REMINDER/ALARM/MEETING/HABIT/AI_BRIEFING>", "repeat": "<NONE/DAILY/HOURLY/WEEKLY>", "notes": "<optional brief description>"}

                        When the user asks you to save a note, memo, or key insights, include this block:
                        ACTION_NOTE: {"title": "<Note Title>", "content": "<Note Content>"}

                        When the user asks you to write code or solve equations, format cleanly using standard Markdown.
                    """.trimIndent())
                })
                put("parts", partsArray)
            }
            rootJson.put("systemInstruction", systemObj)

            // Contents array
            val contentsArray = JSONArray()

            // Add recent history turns (limit to last 6 for prompt efficiency)
            val recentHistory = history.takeLast(6)
            for ((role, text) in recentHistory) {
                val msgObj = JSONObject()
                msgObj.put("role", if (role == "user") "user" else "model")
                val parts = JSONArray()
                parts.put(JSONObject().put("text", text))
                msgObj.put("parts", parts)
                contentsArray.put(msgObj)
            }

            // Current user turn
            val currentUserObj = JSONObject()
            currentUserObj.put("role", "user")
            val currentParts = JSONArray()

            if (imageBitmap != null) {
                val base64Image = bitmapToBase64(imageBitmap)
                val inlineDataObj = JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", base64Image)
                }
                currentParts.put(JSONObject().put("inlineData", inlineDataObj))
            }

            currentParts.put(JSONObject().put("text", prompt))
            currentUserObj.put("parts", currentParts)
            contentsArray.put(currentUserObj)

            rootJson.put("contents", contentsArray)

            // Generation config
            val genConfig = JSONObject().apply {
                put("temperature", 0.7)
                put("topP", 0.95)
            }
            rootJson.put("generationConfig", genConfig)

            val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMessage = parseErrorMessage(responseString, response.code)
                return@withContext Result.failure(Exception(errorMessage))
            }

            val parsedResponse = parseGeminiResponse(responseString)
            Result.success(parsedResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun parseErrorMessage(jsonStr: String, statusCode: Int): String {
        return try {
            val json = JSONObject(jsonStr)
            val error = json.optJSONObject("error")
            val message = error?.optString("message") ?: "HTTP $statusCode error"
            "Gemini API Error ($statusCode): $message"
        } catch (_: Exception) {
            "Network error (HTTP $statusCode). Please check your internet connection or API key."
        }
    }

    private fun parseGeminiResponse(jsonStr: String): AiResponse {
        val root = JSONObject(jsonStr)
        val candidates = root.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            return AiResponse(text = "No response generated by model.")
        }

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content")
        val parts = content?.optJSONArray("parts")

        val rawTextBuilder = StringBuilder()
        if (parts != null) {
            for (i in 0 until parts.length()) {
                val p = parts.getJSONObject(i)
                rawTextBuilder.append(p.optString("text", ""))
            }
        }

        val fullText = rawTextBuilder.toString()
        return extractActionAndClean(fullText)
    }

    private fun extractActionAndClean(text: String): AiResponse {
        var cleanText = text
        var scheduledTaskAction: ScheduleTaskAction? = null
        var saveNoteAction: SaveNoteAction? = null

        // Check for ACTION_SCHEDULE
        val scheduleRegex = Regex("ACTION_SCHEDULE:\\s*(\\{.*?\\})", RegexOption.DOT_MATCHES_ALL)
        val scheduleMatch = scheduleRegex.find(text)
        if (scheduleMatch != null) {
            try {
                val jsonStr = scheduleMatch.groupValues[1]
                val obj = JSONObject(jsonStr)
                scheduledTaskAction = ScheduleTaskAction(
                    title = obj.optString("title", "Automated Task"),
                    minutesFromNow = obj.optInt("minutesFromNow", 15),
                    category = obj.optString("category", "REMINDER"),
                    repeat = obj.optString("repeat", "NONE"),
                    notes = obj.optString("notes", "")
                )
                cleanText = cleanText.replace(scheduleMatch.value, "").trim()
            } catch (_: Exception) {}
        }

        // Check for ACTION_NOTE
        val noteRegex = Regex("ACTION_NOTE:\\s*(\\{.*?\\})", RegexOption.DOT_MATCHES_ALL)
        val noteMatch = noteRegex.find(text)
        if (noteMatch != null) {
            try {
                val jsonStr = noteMatch.groupValues[1]
                val obj = JSONObject(jsonStr)
                saveNoteAction = SaveNoteAction(
                    title = obj.optString("title", "Saved Note"),
                    content = obj.optString("content", "")
                )
                cleanText = cleanText.replace(noteMatch.value, "").trim()
            } catch (_: Exception) {}
        }

        return AiResponse(
            text = cleanText,
            scheduleAction = scheduledTaskAction,
            noteAction = saveNoteAction
        )
    }
}

data class AiResponse(
    val text: String,
    val scheduleAction: ScheduleTaskAction? = null,
    val noteAction: SaveNoteAction? = null
)

data class ScheduleTaskAction(
    val title: String,
    val minutesFromNow: Int,
    val category: String,
    val repeat: String,
    val notes: String
)

data class SaveNoteAction(
    val title: String,
    val content: String
)
