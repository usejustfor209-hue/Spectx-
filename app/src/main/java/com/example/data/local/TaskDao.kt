package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ScheduledTask
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM scheduled_tasks ORDER BY scheduledTimeMillis ASC")
    fun getAllTasks(): Flow<List<ScheduledTask>>

    @Query("SELECT * FROM scheduled_tasks WHERE isCompleted = 0 ORDER BY scheduledTimeMillis ASC")
    fun getActiveTasks(): Flow<List<ScheduledTask>>

    @Query("SELECT * FROM scheduled_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): ScheduledTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ScheduledTask): Long

    @Update
    suspend fun updateTask(task: ScheduledTask)

    @Delete
    suspend fun deleteTask(task: ScheduledTask)

    @Query("UPDATE scheduled_tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun setTaskCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM scheduled_tasks WHERE isCompleted = 1")
    suspend fun deleteCompletedTasks()
}
