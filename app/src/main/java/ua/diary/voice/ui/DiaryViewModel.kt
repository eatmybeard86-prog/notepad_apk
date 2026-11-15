package ua.diary.voice.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ua.diary.voice.audio.VoicePlayer
import ua.diary.voice.audio.VoiceRecorder
import ua.diary.voice.data.DiaryDatabase
import ua.diary.voice.data.DiaryEntry
import ua.diary.voice.data.DiaryRepository
import ua.diary.voice.data.DutyRepository
import ua.diary.voice.data.DutyUnit
import ua.diary.voice.data.DutyPosition
import ua.diary.voice.data.UnitWithPositions
import ua.diary.voice.data.EntryWithNotes
import ua.diary.voice.data.Topic
import ua.diary.voice.data.VoiceNote

/** Стан відкритого редактора. entryId == null — запис ще не збережено в базі. */
/** Стан відтворення відкритої нотатки. */
data class PlaybackState(
    val noteId: Long,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
)

data class EditorState(
    val session: Long,
    val entryId: Long?,
    val date: Long,
    val topicId: Long?,
    val text: String,
)

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = DiaryRepository(DiaryDatabase.get(app))
    private val recorder = VoiceRecorder(app)
    private val player = VoicePlayer()
    val duty = DutyRepository(app, DiaryDatabase.get(app))

    init {
        viewModelScope.launch { duty.seedBundledDocuments() }
    }

    // ---------- Головний екран ----------
    private val _homeTab = MutableStateFlow(0)
    val homeTab = _homeTab.asStateFlow()
    fun setHomeTab(t: Int) { _homeTab.value = t }

    // ---------- Обов'язки ----------
    val units: StateFlow<List<UnitWithPositions>> =
        duty.units.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _openUnitId = MutableStateFlow<Long?>(null)
    val openUnitId = _openUnitId.asStateFlow()
    fun openUnit(id: Long?) { _openUnitId.value = id }

    fun addUnit(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { duty.addUnit(name) }
    }

    fun renameUnit(unit: DutyUnit, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { duty.renameUnit(unit, name) }
    }

    fun deleteUnit(unitId: Long) {
        if (_openUnitId.value == unitId) _openUnitId.value = null
        viewModelScope.launch { duty.deleteUnit(unitId) }
    }

    fun addPosition(unitId: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { duty.addPosition(unitId, name) }
    }

    fun renamePosition(p: DutyPosition, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { duty.renamePosition(p, name) }
    }

    fun deletePosition(p: DutyPosition) {
        viewModelScope.launch { duty.deletePosition(p) }
    }

    fun attachDocument(p: DutyPosition, uri: Uri) {
        viewModelScope.launch {
            runCatching { duty.attachDocument(p.id, uri) }
                .onSuccess { _message.value = "Інструкцію закріплено за посадою «${p.name}»" }
                .onFailure { _message.value = "Не вдалося прочитати файл" }
        }
    }

    fun detachDocument(p: DutyPosition) {
        viewModelScope.launch { duty.detachDocument(p) }
    }

    // ---------- Розділи ----------
    val topics: StateFlow<List<Topic>> =
        repo.topics.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _topicFilter = MutableStateFlow<Long?>(null)
    val topicFilter = _topicFilter.asStateFlow()
    fun setTopicFilter(id: Long?) { _topicFilter.value = id }

    fun addTopic(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repo.addTopic(name, TOPIC_COLORS[topics.value.size % TOPIC_COLORS.size])
            _topicFilter.value = id
        }
    }

    fun renameTopic(topic: Topic, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repo.renameTopic(topic, name) }
    }

    fun deleteTopic(topic: Topic) {
        if (_topicFilter.value == topic.id) _topicFilter.value = null
        viewModelScope.launch { repo.deleteTopic(topic) }
    }

    // ---------- Записи ----------
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()
    fun setQuery(q: String) { _query.value = q }

    /** Записи з урахуванням вибраного розділу та пошуку. */
    val entries: StateFlow<List<EntryWithNotes>> =
        combine(repo.entries, _topicFilter, _query) { all, topic, q ->
            all.filter { e ->
                (topic == null || e.entry.topicId == topic) &&
                    (q.isBlank() || e.entry.text.contains(q.trim(), ignoreCase = true))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---------- Редактор ----------
    private val _editor = MutableStateFlow<EditorState?>(null)
    val editor = _editor.asStateFlow()
    private var sessionCounter = 0L
    private val saveMutex = Mutex()
    private var saveJob: Job? = null
    /** Останній збережений стан кожної сесії (щоб не писати в базу без змін). */
    private var lastSaved: EditorState? = null

    val notes: StateFlow<List<VoiceNote>> = _editor
        .map { it?.entryId }
        .distinctUntilChanged()
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.notes(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun openEntry(entry: DiaryEntry) {
        closeEditor()
        val s = EditorState(++sessionCounter, entry.id, entry.date, entry.topicId, entry.text)
        lastSaved = s
        _editor.value = s
    }

    fun newEntry(date: Long) {
        closeEditor()
        val s = EditorState(++sessionCounter, null, date, _topicFilter.value, "")
        lastSaved = s
        _editor.value = s
    }

    fun onTextChange(t: String) {
        _editor.update { it?.copy(text = t) }
        scheduleSave()
    }

    fun setEntryTopic(topicId: Long?) {
        _editor.update { it?.copy(topicId = topicId) }
        scheduleSave(0)
    }

    private fun scheduleSave(delayMs: Long = 600) {
        saveJob?.cancel()
        val snapshot = _editor.value ?: return
        saveJob = viewModelScope.launch {
            delay(delayMs)
            persist(snapshot)
        }
    }

    /** Зберігає стан; створює запис у базі, якщо його ще немає. Повертає id запису або null. */
    private suspend fun persist(s: EditorState, force: Boolean = false): Long? = saveMutex.withLock {
        // entryId міг з'явитися після знімка — беремо найсвіжіший для цієї сесії
        val current = _editor.value?.takeIf { it.session == s.session }
        val knownId = current?.entryId ?: s.entryId ?: lastSaved?.takeIf { it.session == s.session }?.entryId
        val state = s.copy(entryId = knownId)
        if (knownId == null) {
            if (state.text.isBlank() && !force) return@withLock null
            val id = repo.createEntry(state.date, state.topicId, state.text)
            lastSaved = state.copy(entryId = id)
            _editor.update { if (it != null && it.session == s.session) it.copy(entryId = id) else it }
            return@withLock id
        }
        val prev = lastSaved
        if (prev != null && prev.session == s.session && prev.text == state.text && prev.topicId == state.topicId) {
            return@withLock knownId
        }
        repo.updateEntry(knownId, state.text, state.topicId)
        lastSaved = state
        knownId
    }

    fun closeEditor() {
        val s = _editor.value ?: return
        saveJob?.cancel()
        if (recorder.isRecording) stopRecording()
        stopPlayback()
        _editor.value = null
        viewModelScope.launch { persist(s) }
    }

    fun deleteEntry() {
        val s = _editor.value ?: return
        saveJob?.cancel()
        recorder.stopAndDiscard()
        _isRecording.value = false
        _recordPaused.value = false
        stopPlayback()
        _editor.value = null
        viewModelScope.launch {
            val id = saveMutex.withLock { s.entryId ?: lastSaved?.takeIf { it.session == s.session }?.entryId }
            if (id != null) repo.deleteEntry(id)
        }
    }

    fun deleteEntryById(id: Long) {
        viewModelScope.launch { repo.deleteEntry(id) }
    }

    // ---------- Запис голосу ----------
    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()
    private val _recordElapsed = MutableStateFlow(0L)
    val recordElapsed = _recordElapsed.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    fun showMessage(m: String) { _message.value = m }
    fun clearMessage() { _message.value = null }

    fun startRecording() {
        if (_editor.value == null || recorder.isRecording) return
        stopPlayback()
        try {
            recorder.start()
        } catch (e: Exception) {
            _message.value = "Не вдалося почати запис: ${e.message}"
            return
        }
        _isRecording.value = true
        viewModelScope.launch {
            while (isActive && recorder.isRecording) {
                _recordElapsed.value = recorder.elapsedMs()
                delay(200)
            }
            _recordElapsed.value = 0
        }
    }

    private val _recordPaused = MutableStateFlow(false)
    val recordPaused = _recordPaused.asStateFlow()

    fun toggleRecordPause() {
        if (!recorder.isRecording) return
        if (recorder.isPaused) recorder.resume() else recorder.pause()
        _recordPaused.value = recorder.isPaused
    }

    fun stopRecording() {
        _recordPaused.value = false
        val s = _editor.value
        val result = recorder.stop()
        _isRecording.value = false
        if (result == null) {
            _message.value = "Запис надто короткий"
            return
        }
        if (s == null) { result.first.delete(); return }
        viewModelScope.launch {
            val id = persist(s, force = true)
            if (id == null) result.first.delete()
            else repo.addVoiceNote(id, result.first, result.second)
        }
    }

    // ---------- Відтворення ----------
    private val _playback = MutableStateFlow<PlaybackState?>(null)
    val playback = _playback.asStateFlow()
    private var tickJob: Job? = null

    private fun refreshPlayback() {
        val cur = _playback.value ?: return
        _playback.value = cur.copy(
            isPlaying = player.isPlaying,
            positionMs = player.positionMs(),
            durationMs = player.durationMs().takeIf { it > 0 } ?: cur.durationMs,
        )
    }

    /** Кнопка «відтворити/пауза» на нотатці. */
    fun togglePlay(note: VoiceNote) {
        val cur = _playback.value
        if (cur != null && cur.noteId == note.id) {
            if (player.isPlaying) player.pause() else player.resume()
            refreshPlayback()
            return
        }
        val ok = player.play(note.filePath) {
            // Дійшли до кінця: залишаємо нотатку відкритою на початку, на паузі
            player.pause()
            player.seekTo(0)
            refreshPlayback()
        }
        if (!ok) {
            _playback.value = null
            _message.value = "Не вдалося відтворити запис"
            return
        }
        _playback.value = PlaybackState(note.id, true, 0, player.durationMs().takeIf { it > 0 } ?: note.durationMs)
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive && player.isActive) {
                refreshPlayback()
                delay(200)
            }
        }
    }

    fun seekTo(ms: Long) {
        player.seekTo(ms)
        refreshPlayback()
    }

    fun skip(deltaMs: Long) = seekTo(player.positionMs() + deltaMs)

    fun stopPlayback() {
        tickJob?.cancel()
        player.stop()
        _playback.value = null
    }

    fun deleteNote(note: VoiceNote) {
        if (_playback.value?.noteId == note.id) stopPlayback()
        viewModelScope.launch { repo.deleteVoiceNote(note) }
    }

    // ---------- Експорт у буфер ----------
    fun topicName(id: Long?): String? = id?.let { tid -> topics.value.firstOrNull { it.id == tid }?.name }

    fun exportText(entry: DiaryEntry, notes: List<VoiceNote>): String = buildString {
        append(formatDate(entry.date)).append(", ").append(formatWeekdayPlain(entry.date))
        topicName(entry.topicId)?.let { append(" · ").append(it) }
        append('\n')
        if (entry.text.isNotBlank()) append(entry.text.trim()).append('\n')
        if (notes.isNotEmpty()) {
            append("🎙 Голосових нотаток: ").append(notes.size)
            append(" (").append(notes.joinToString { formatDuration(it.durationMs) }).append(")\n")
        }
    }.trimEnd()

    fun exportCurrent(): String? {
        val s = _editor.value ?: return null
        val entry = DiaryEntry(id = s.entryId ?: 0, date = s.date, topicId = s.topicId, text = s.text)
        return exportText(entry, notes.value)
    }

    fun exportAll(): String =
        entries.value.sortedWith(compareBy({ it.entry.date }, { it.entry.createdAt }))
            .joinToString("\n\n———\n\n") { exportText(it.entry, it.notes) }

    override fun onCleared() {
        recorder.stopAndDiscard()
        player.stop()
    }

    companion object {
        val TOPIC_COLORS = listOf(
            0xFF5B4B8A.toInt(), 0xFF2E7D6B.toInt(), 0xFFB5523B.toInt(), 0xFF3366AA.toInt(),
            0xFFAA7A1F.toInt(), 0xFF8A3B6E.toInt(), 0xFF4F7A2A.toInt(), 0xFF6B6B6B.toInt(),
        )
    }
}
