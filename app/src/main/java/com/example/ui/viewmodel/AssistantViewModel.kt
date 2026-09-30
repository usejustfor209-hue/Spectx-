package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiApiClient
import com.example.data.api.ScheduleTaskAction
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.SavedNote
import com.example.data.model.ScheduledTask
import com.example.data.scheduler.TaskScheduler
import com.example.voice.VoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val geminiClient = GeminiApiClient(application)
    val voiceManager = VoiceManager(application)

    val chatMessages: StateFlow<List<ChatMessage>> = db.chatMessageDao()
        .getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scheduledTasks: StateFlow<List<ScheduledTask>> = db.taskDao()
        .getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedNotes: StateFlow<List<SavedNote>> = db.savedNoteDao()
        .getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _autoSpeak = MutableStateFlow(true)
    val autoSpeak: StateFlow<Boolean> = _autoSpeak.asStateFlow()

    private val _isDuplexVoiceModeActive = MutableStateFlow(false)
    val isDuplexVoiceModeActive: StateFlow<Boolean> = _isDuplexVoiceModeActive.asStateFlow()

    private val _selectedImageBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedImageBitmap: StateFlow<Bitmap?> = _selectedImageBitmap.asStateFlow()

    init {
        // Wire voice manager callbacks
        voiceManager.onSpeechRecognized = { text ->
            if (_isDuplexVoiceModeActive.value) {
                sendUserMessage(text)
            }
        }
        voiceManager.onSpeechComplete = {
            if (_isDuplexVoiceModeActive.value) {
                // In duplex mode, re-listen once speech finished
                voiceManager.startListening()
            }
        }
        voiceManager.onSpeechError = { error ->
            _errorMessage.value = error
        }

        // Initialize with friendly welcome if chat is empty
        viewModelScope.launch {
            if (chatMessages.value.isEmpty()) {
                db.chatMessageDao().insertMessage(
                    ChatMessage(
                        role = "model",
                        content = "Namaste! I am **Omni AI**, your most powerful executive personal assistant powered by the newest Gemini 3.5 Flash engine.\n\nI can do all your tasks:\n• ⏰ **Automated Task & Schedule Management** (Alarms, reminders, timetable creation)\n• 🎙️ **Real-time Voice Interaction** (Speak and listen seamlessly)\n• 💻 **Code, Math & Complex Problem Solving**\n• 🌐 **Language Translation & Creative Brainstorming**\n• 📷 **Vision & Document Analysis**\n\nHow can I empower you today?"
                    )
                )
            }
        }
    }

    fun setAutoSpeak(enabled: Boolean) {
        _autoSpeak.value = enabled
    }

    fun setSelectedImage(bitmap: Bitmap?) {
        _selectedImageBitmap.value = bitmap
    }

    fun setDuplexVoiceMode(active: Boolean) {
        _isDuplexVoiceModeActive.value = active
        if (active) {
            voiceManager.startListening()
        } else {
            voiceManager.stopListening()
            voiceManager.stopSpeaking()
        }
    }

    fun sendUserMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() && _selectedImageBitmap.value == null) return

        val image = _selectedImageBitmap.value
        _selectedImageBitmap.value = null // consume image
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            // Check for immediate local natural schedule parsing
            val naturalSchedule = TaskScheduler.parseNaturalSchedule(trimmed)
            var actionTag: String? = null

            if (naturalSchedule != null) {
                val (taskTitle, timeMillis) = naturalSchedule
                val newTask = ScheduledTask(
                    title = taskTitle,
                    description = "Automated reminder created via Omni AI voice command",
                    scheduledTimeMillis = timeMillis,
                    category = "REMINDER"
                )
                val taskId = db.taskDao().insertTask(newTask)
                TaskScheduler.scheduleAlarm(getApplication(), newTask.copy(id = taskId))
                actionTag = "Scheduled task: $taskTitle"
            }

            // Save user message
            db.chatMessageDao().insertMessage(
                ChatMessage(
                    role = "user",
                    content = trimmed,
                    actionExecuted = actionTag
                )
            )

            // Prepare history
            val history = chatMessages.value.takeLast(6).map { it.role to it.content }

            // Call Gemini
            val result = geminiClient.generateResponse(
                prompt = trimmed,
                history = history,
                imageBitmap = image
            )

            _isLoading.value = false

            result.onSuccess { aiResponse ->
                var executedAction: String? = null

                // Handle AI automated task schedule if requested
                if (aiResponse.scheduleAction != null) {
                    executedAction = handleAiScheduleAction(aiResponse.scheduleAction)
                }

                // Handle AI saved note if requested
                if (aiResponse.noteAction != null) {
                    val noteId = db.savedNoteDao().insertNote(
                        SavedNote(
                            title = aiResponse.noteAction.title,
                            content = aiResponse.noteAction.content
                        )
                    )
                    executedAction = "Saved note: ${aiResponse.noteAction.title}"
                }

                // Save assistant message
                db.chatMessageDao().insertMessage(
                    ChatMessage(
                        role = "model",
                        content = aiResponse.text,
                        actionExecuted = executedAction
                    )
                )

                // Speak aloud if enabled
                if (_autoSpeak.value || _isDuplexVoiceModeActive.value) {
                    voiceManager.speak(aiResponse.text)
                }
            }.onFailure { error ->
                _errorMessage.value = error.message
                db.chatMessageDao().insertMessage(
                    ChatMessage(
                        role = "model",
                        content = "⚠️ ${error.message ?: "An error occurred while contacting Gemini API."}"
                    )
                )
            }
        }
    }

    private suspend fun handleAiScheduleAction(action: ScheduleTaskAction): String {
        val delayMillis = (action.minutesFromNow.coerceAtLeast(1)) * 60 * 1000L
        val triggerTime = System.currentTimeMillis() + delayMillis

        val scheduledTask = ScheduledTask(
            title = action.title,
            description = action.notes.ifBlank { "Automated schedule triggered by Omni AI" },
            scheduledTimeMillis = triggerTime,
            category = action.category.ifBlank { "REMINDER" },
            repeatInterval = action.repeat
        )

        val taskId = db.taskDao().insertTask(scheduledTask)
        val created = scheduledTask.copy(id = taskId)
        TaskScheduler.scheduleAlarm(getApplication(), created)

        return "⚡ Automated Task Scheduled: ${action.title} (in ${action.minutesFromNow}m)"
    }

    fun createManualTask(
        title: String,
        description: String,
        timeMillis: Long,
        category: String,
        repeat: String
    ) {
        viewModelScope.launch {
            val task = ScheduledTask(
                title = title,
                description = description,
                scheduledTimeMillis = timeMillis,
                category = category,
                repeatInterval = repeat
            )
            val taskId = db.taskDao().insertTask(task)
            TaskScheduler.scheduleAlarm(getApplication(), task.copy(id = taskId))
        }
    }

    fun toggleTaskComplete(task: ScheduledTask) {
        viewModelScope.launch {
            val newStatus = !task.isCompleted
            db.taskDao().setTaskCompleted(task.id, newStatus)
            if (newStatus) {
                TaskScheduler.cancelAlarm(getApplication(), task)
            } else {
                TaskScheduler.scheduleAlarm(getApplication(), task)
            }
        }
    }

    fun deleteTask(task: ScheduledTask) {
        viewModelScope.launch {
            TaskScheduler.cancelAlarm(getApplication(), task)
            db.taskDao().deleteTask(task)
        }
    }

    fun autoPlanDay(goalsPrompt: String) {
        val prompt = """
            Create an automated daily routine and schedule for the following goals:
            "$goalsPrompt"
            
            Break it into 3 to 5 realistic time blocks for today. For each block, provide a clear, action-oriented schedule recommendation.
            At the end, include an ACTION_SCHEDULE for the very first immediate upcoming task block.
        """.trimIndent()
        sendUserMessage(prompt)
    }

    fun speakText(text: String) {
        voiceManager.speak(text)
    }

    fun clearChat() {
        viewModelScope.launch {
            db.chatMessageDao().clearHistory()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
