package ua.diary.voice.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/** Розділ (тема), який користувач створює сам: «Робота», «Сон», «Подорожі»… */
@Entity(tableName = "topics")
data class Topic(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int,
)

/** Запис щоденника. [date] — LocalDate.toEpochDay(). В один день може бути кілька записів у різних розділах. */
@Entity(tableName = "entries", indices = [Index("date"), Index("topicId")])
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val topicId: Long? = null,
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Голосова нотатка, прив'язана до запису. */
@Entity(tableName = "voice_notes", indices = [Index("entryId")])
data class VoiceNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val filePath: String,
    val durationMs: Long,
    val createdAt: Long = System.currentTimeMillis(),
)

data class EntryWithNotes(
    @Embedded val entry: DiaryEntry,
    @Relation(parentColumn = "id", entityColumn = "entryId")
    val notes: List<VoiceNote>,
)

/** Підрозділ у розділі «Обов'язки»: взвод, група тощо. */
@Entity(tableName = "duty_units")
data class DutyUnit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
)

/**
 * Посада у підрозділі. За посадою можна закріпити посадову інструкцію (PDF/DOC/DOCX),
 * файл лежить у filesDir/docs/[docFileName].
 */
@Entity(tableName = "duty_positions", indices = [Index("unitId")])
data class DutyPosition(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unitId: Long,
    val name: String,
    val sortOrder: Int = 0,
    val docTitle: String? = null,
    val docFileName: String? = null,
    val docMime: String? = null,
)

data class UnitWithPositions(
    @Embedded val unit: DutyUnit,
    @Relation(parentColumn = "id", entityColumn = "unitId")
    val positions: List<DutyPosition>,
)
