package ua.diary.voice.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Розділ «Обов'язки»: підрозділи, посади та закріплені посадові інструкції. */
class DutyRepository(private val context: Context, private val db: DiaryDatabase) {
    private val dao = db.dao()
    private val docsDir get() = File(context.filesDir, "docs").apply { mkdirs() }

    val units = dao.observeUnits()

    fun fileOf(p: DutyPosition): File? = p.docFileName?.let { File(docsDir, it) }

    // ----- Підрозділи -----
    suspend fun addUnit(name: String) {
        dao.insertUnit(DutyUnit(name = name.trim(), sortOrder = dao.nextUnitOrder()))
    }

    suspend fun renameUnit(unit: DutyUnit, name: String) = dao.updateUnit(unit.copy(name = name.trim()))

    suspend fun deleteUnit(unitId: Long) {
        dao.positionsFor(unitId).forEach { fileOf(it)?.delete() }
        db.withTransaction {
            dao.deletePositionsFor(unitId)
            dao.deleteUnit(unitId)
        }
    }

    // ----- Посади -----
    suspend fun addPosition(unitId: Long, name: String): Long =
        dao.insertPosition(DutyPosition(unitId = unitId, name = name.trim(), sortOrder = dao.nextPositionOrder(unitId)))

    suspend fun renamePosition(p: DutyPosition, name: String) {
        val cur = dao.position(p.id) ?: return
        dao.updatePosition(cur.copy(name = name.trim()))
    }

    suspend fun deletePosition(p: DutyPosition) {
        val cur = dao.position(p.id) ?: return
        fileOf(cur)?.delete()
        dao.deletePosition(cur.id)
    }

    /** Закріплює за посадою вибраний файл (копіює в пам'ять додатка). Попередній файл видаляється. */
    suspend fun attachDocument(positionId: Long, uri: Uri) = withContext(Dispatchers.IO) {
        val cur = dao.position(positionId) ?: return@withContext
        val resolver = context.contentResolver
        val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
            ?: "Інструкція"
        val reported = resolver.getType(uri)
        val mime = if (reported == null || reported == "application/octet-stream") mimeFromName(displayName) else reported
        val fileName = "${UUID.randomUUID()}.${extensionOf(displayName, mime)}"
        val input0 = resolver.openInputStream(uri) ?: error("файл недоступний")
        input0.use { input ->
            File(docsDir, fileName).outputStream().use { input.copyTo(it) }
        }
        fileOf(cur)?.delete()
        dao.updatePosition(cur.copy(docTitle = displayName, docFileName = fileName, docMime = mime))
    }

    suspend fun detachDocument(p: DutyPosition) {
        val cur = dao.position(p.id) ?: return
        fileOf(cur)?.delete()
        dao.updatePosition(cur.copy(docTitle = null, docFileName = null, docMime = null))
    }

    /**
     * Інструкції, вбудовані в APK: assets/duties/index.txt, рядки
     * «Назва підрозділу|Назва посади|файл у assets/duties». Додаються один раз.
     */
    suspend fun seedBundledDocuments() = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SEEDED, false)) return@withContext
        val lines = runCatching {
            context.assets.open("duties/index.txt").bufferedReader().readLines()
        }.getOrDefault(emptyList())
        for (line in lines) {
            if (line.isBlank() || line.trimStart().startsWith("#")) continue
            val parts = line.split('|').map { it.trim() }
            if (parts.size != 3) continue
            val (unitName, positionName, asset) = parts
            val unitId = dao.unitByName(unitName)?.id ?: continue
            val mime = mimeFromName(asset)
            val fileName = "${UUID.randomUUID()}.${extensionOf(asset, mime)}"
            runCatching {
                context.assets.open("duties/$asset").use { input ->
                    File(docsDir, fileName).outputStream().use { input.copyTo(it) }
                }
                dao.insertPosition(
                    DutyPosition(
                        unitId = unitId, name = positionName, sortOrder = dao.nextPositionOrder(unitId),
                        docTitle = asset, docFileName = fileName, docMime = mime,
                    )
                )
            }
        }
        prefs.edit().putBoolean(KEY_SEEDED, true).apply()
    }

    companion object {
        private const val KEY_SEEDED = "bundled_docs_seeded_v1"
        val DOC_MIME_TYPES = arrayOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            // Деякі файлові менеджери і месенджери віддають DOC/DOCX саме так
            "application/octet-stream",
            "application/vnd.ms-word",
            "application/x-msword",
        )

        fun mimeFromName(name: String): String {
            val ext = name.substringAfterLast('.', "").lowercase()
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: when (ext) {
                "pdf" -> "application/pdf"
                "doc" -> "application/msword"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                else -> "application/octet-stream"
            }
        }

        private fun extensionOf(name: String, mime: String): String {
            val fromName = name.substringAfterLast('.', "").lowercase()
            if (fromName.isNotEmpty() && fromName.length <= 5) return fromName
            return MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "bin"
        }
    }
}
