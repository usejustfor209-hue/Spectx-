package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_tasks")
data class ScheduledTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val scheduledTimeMillis: Long,
    val category: String = "REMINDER", // "REMINDER", "AI_BRIEFING", "HABIT", "ALARM", "MEETING"
    val isCompleted: Boolean = false,
    val repeatInterval: String = "NONE", // "NONE", "DAILY", "WEEKLY", "HOURLY"
    val aiPromptOnTrigger: String? = null,
    val notificationId: Int = (System.currentTimeMillis() % 100000).toInt()
)
