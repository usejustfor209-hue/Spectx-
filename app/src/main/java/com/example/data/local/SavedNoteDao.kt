package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SavedNote
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedNoteDao {
    @Query("SELECT * FROM saved_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<SavedNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: SavedNote): Long

    @Update
    suspend fun updateNote(note: SavedNote)

    @Delete
    suspend fun deleteNote(note: SavedNote)
}
