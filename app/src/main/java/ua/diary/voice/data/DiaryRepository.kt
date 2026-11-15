package ua.diary.voice.data

import androidx.room.withTransaction
import java.io.File

class DiaryRepository(private val db: DiaryDatabase) {
    private val dao = db.dao()

    val topics = dao.observeTopics()
    val entries = dao.observeEntries()
    fun notes(entryId: Long) = dao.observeNotes(entryId)

    suspend fun addTopic(name: String, color: Int) = dao.insertTopic(Topic(name = name.trim(), color = color))
    suspend fun renameTopic(topic: Topic, name: String) = dao.updateTopic(topic.copy(name = name.trim()))
    suspend fun deleteTopic(topic: Topic) = db.withTransaction {
        dao.clearTopic(topic.id)
        dao.deleteTopic(topic)
    }

    suspend fun createEntry(date: Long, topicId: Long?, text: String): Long =
        dao.insertEntry(DiaryEntry(date = date, topicId = topicId, text = text))

    suspend fun updateEntry(id: Long, text: String, topicId: Long?) = dao.updateEntry(id, text, topicId)

    suspend fun addVoiceNote(entryId: Long, file: File, durationMs: Long) {
        dao.insertNote(VoiceNote(entryId = entryId, filePath = file.absolutePath, durationMs = durationMs))
    }

    suspend fun deleteVoiceNote(note: VoiceNote) {
        File(note.filePath).delete()
        dao.deleteNote(note)
    }

    suspend fun deleteEntry(id: Long) {
        dao.notesFor(id).forEach { File(it.filePath).delete() }
        db.withTransaction {
            dao.deleteNotesFor(id)
            dao.deleteEntry(id)
        }
    }
}
