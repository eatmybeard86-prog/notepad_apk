package ua.diary.voice.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    // Розділи
    @Query("SELECT * FROM topics ORDER BY name COLLATE NOCASE")
    fun observeTopics(): Flow<List<Topic>>

    @Insert suspend fun insertTopic(topic: Topic): Long
    @Update suspend fun updateTopic(topic: Topic)
    @Delete suspend fun deleteTopic(topic: Topic)

    @Query("UPDATE entries SET topicId = NULL WHERE topicId = :topicId")
    suspend fun clearTopic(topicId: Long)

    // Записи
    @Transaction
    @Query("SELECT * FROM entries ORDER BY date DESC, createdAt DESC")
    fun observeEntries(): Flow<List<EntryWithNotes>>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun entry(id: Long): DiaryEntry?

    @Insert suspend fun insertEntry(entry: DiaryEntry): Long

    @Query("UPDATE entries SET text = :text, topicId = :topicId, updatedAt = :now WHERE id = :id")
    suspend fun updateEntry(id: Long, text: String, topicId: Long?, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    // Голосові
    @Query("SELECT * FROM voice_notes WHERE entryId = :entryId ORDER BY createdAt ASC")
    fun observeNotes(entryId: Long): Flow<List<VoiceNote>>

    @Query("SELECT * FROM voice_notes WHERE entryId = :entryId")
    suspend fun notesFor(entryId: Long): List<VoiceNote>

    @Insert suspend fun insertNote(note: VoiceNote): Long
    @Delete suspend fun deleteNote(note: VoiceNote)

    @Query("DELETE FROM voice_notes WHERE entryId = :entryId")
    suspend fun deleteNotesFor(entryId: Long)
}
