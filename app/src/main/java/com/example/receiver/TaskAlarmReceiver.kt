package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.ScheduledTask
import com.example.data.scheduler.TaskScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "omni_ai_task_channel"
        const val CHANNEL_NAME = "Omni AI Automated Tasks"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_DESC = "extra_task_desc"
        const val EXTRA_TASK_CATEGORY = "extra_task_category"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, 0L)
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Task Reminder"
        val taskDesc = intent.getStringExtra(EXTRA_TASK_DESC) ?: "Omni AI reminder ready."
        val category = intent.getStringExtra(EXTRA_TASK_CATEGORY) ?: "REMINDER"

        showNotification(context, taskId, taskTitle, taskDesc, category)

        // Handle recurring task or mark completed
        if (taskId > 0) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val task = db.taskDao().getTaskById(taskId)
                    if (task != null) {
                        when (task.repeatInterval) {
                            "DAILY" -> {
                                val nextTime = task.scheduledTimeMillis + 24 * 60 * 60 * 1000L
                                val updated = task.copy(scheduledTimeMillis = nextTime)
                                db.taskDao().updateTask(updated)
                                TaskScheduler.scheduleAlarm(context, updated)
                            }
                            "HOURLY" -> {
                                val nextTime = task.scheduledTimeMillis + 60 * 60 * 1000L
                                val updated = task.copy(scheduledTimeMillis = nextTime)
                                db.taskDao().updateTask(updated)
                                TaskScheduler.scheduleAlarm(context, updated)
                            }
                            "WEEKLY" -> {
                                val nextTime = task.scheduledTimeMillis + 7 * 24 * 60 * 60 * 1000L
                                val updated = task.copy(scheduledTimeMillis = nextTime)
                                db.taskDao().updateTask(updated)
                                TaskScheduler.scheduleAlarm(context, updated)
                            }
                            else -> {
                                db.taskDao().setTaskCompleted(taskId, true)
                            }
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun showNotification(
        context: Context,
        taskId: Long,
        title: String,
        description: String,
        category: String
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                this.description = "Automated Task and Schedule Alerts from Omni AI"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NOTIFICATION_TASK_ID", taskId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val iconRes = android.R.drawable.ic_popup_reminder

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle("⚡ Omni AI: $title")
            .setContentText(description)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$description\n\n[Category: $category] • Automated by Omni AI")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(taskId.toInt().coerceAtLeast(1001), notification)
    }
}
