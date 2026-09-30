package com.example.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.ScheduledTask
import com.example.receiver.TaskAlarmReceiver
import java.util.Calendar
import java.util.regex.Pattern

object TaskScheduler {

    fun scheduleAlarm(context: Context, task: ScheduledTask) {
        if (task.scheduledTimeMillis <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra(TaskAlarmReceiver.EXTRA_TASK_ID, task.id)
            putExtra(TaskAlarmReceiver.EXTRA_TASK_TITLE, task.title)
            putExtra(TaskAlarmReceiver.EXTRA_TASK_DESC, task.description)
            putExtra(TaskAlarmReceiver.EXTRA_TASK_CATEGORY, task.category)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        task.scheduledTimeMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        task.scheduledTimeMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    task.scheduledTimeMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                task.scheduledTimeMillis,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(context: Context, task: ScheduledTask) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Parses natural language phrases for scheduling, e.g.:
     * "remind me in 10 minutes to take medicine"
     * "schedule meeting tomorrow at 3 pm"
     * "in 1 hour call John"
     */
    fun parseNaturalSchedule(input: String): Pair<String, Long>? {
        val lower = input.lowercase().trim()
        val now = System.currentTimeMillis()

        // Match "in X minutes/mins"
        val minPattern = Pattern.compile("in\\s+(\\d+)\\s*(?:minute|minutes|min|mins)", Pattern.CASE_INSENSITIVE)
        val minMatcher = minPattern.matcher(lower)
        if (minMatcher.find()) {
            val mins = minMatcher.group(1)?.toLongOrNull() ?: 10L
            val cleanTitle = input.replace(minMatcher.group(0) ?: "", "")
                .replace("remind me to", "", ignoreCase = true)
                .replace("remind me", "", ignoreCase = true)
                .replace("schedule", "", ignoreCase = true)
                .trim()
            val time = now + mins * 60 * 1000L
            return Pair(if (cleanTitle.isBlank()) "Automated Task" else cleanTitle, time)
        }

        // Match "in X hours/hrs"
        val hourPattern = Pattern.compile("in\\s+(\\d+)\\s*(?:hour|hours|hr|hrs)", Pattern.CASE_INSENSITIVE)
        val hourMatcher = hourPattern.matcher(lower)
        if (hourMatcher.find()) {
            val hrs = hourMatcher.group(1)?.toLongOrNull() ?: 1L
            val cleanTitle = input.replace(hourMatcher.group(0) ?: "", "")
                .replace("remind me to", "", ignoreCase = true)
                .replace("remind me", "", ignoreCase = true)
                .trim()
            val time = now + hrs * 3600 * 1000L
            return Pair(if (cleanTitle.isBlank()) "Automated Task" else cleanTitle, time)
        }

        // Match "tomorrow at X (am/pm)" or "tomorrow X (am/pm)"
        val tomorrowPattern = Pattern.compile("tomorrow(?:\\s+at)?\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", Pattern.CASE_INSENSITIVE)
        val tomMatcher = tomorrowPattern.matcher(lower)
        if (tomMatcher.find()) {
            var hour = tomMatcher.group(1)?.toIntOrNull() ?: 9
            val minute = tomMatcher.group(2)?.toIntOrNull() ?: 0
            val amPm = tomMatcher.group(3)?.lowercase()

            if (amPm == "pm" && hour < 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0

            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val cleanTitle = input.replace(tomMatcher.group(0) ?: "", "")
                .replace("remind me to", "", ignoreCase = true)
                .replace("schedule", "", ignoreCase = true)
                .trim()

            return Pair(if (cleanTitle.isBlank()) "Scheduled Task" else cleanTitle, cal.timeInMillis)
        }

        // Match "at X (am/pm)" or "at X:YY (am/pm)"
        val atPattern = Pattern.compile("at\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)", Pattern.CASE_INSENSITIVE)
        val atMatcher = atPattern.matcher(lower)
        if (atMatcher.find()) {
            var hour = atMatcher.group(1)?.toIntOrNull() ?: 12
            val minute = atMatcher.group(2)?.toIntOrNull() ?: 0
            val amPm = atMatcher.group(3)?.lowercase()

            if (amPm == "pm" && hour < 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis <= now) {
                cal.add(Calendar.DAY_OF_YEAR, 1) // Tomorrow if time already passed today
            }

            val cleanTitle = input.replace(atMatcher.group(0) ?: "", "")
                .replace("remind me to", "", ignoreCase = true)
                .replace("schedule", "", ignoreCase = true)
                .trim()

            return Pair(if (cleanTitle.isBlank()) "Scheduled Task" else cleanTitle, cal.timeInMillis)
        }

        return null
    }
}
