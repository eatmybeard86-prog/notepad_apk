package ua.diary.voice.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.diary.voice.data.VoiceNote
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(vm: DiaryViewModel, state: EditorState) {
    val ctx = LocalContext.current
    val date = state.date
    val text = state.text
    val topics by vm.topics.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    var topicMenu by remember { mutableStateOf(false) }
    val notes by vm.notes.collectAsStateWithLifecycle()
    val recording by vm.isRecording.collectAsStateWithLifecycle()
    val elapsed by vm.recordElapsed.collectAsStateWithLifecycle()
    val playingId by vm.playingId.collectAsStateWithLifecycle()
    val error by vm.message.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let { snackbar.showSnackbar(it); vm.clearMessage() }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.startRecording()
    }

    fun onMic() {
        when {
            recording -> vm.stopRecording()
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED -> vm.startRecording()
            else -> permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = vm::closeEditor) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
                },
                title = {
                    Column {
                        Text(formatDate(date), style = MaterialTheme.typography.titleMedium)
                        Text(formatWeekday(date), style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        vm.exportCurrent()?.let { clipboard.setText(AnnotatedString(it)) }
                        vm.showMessage("Запис скопійовано")
                    }) { Icon(Icons.Default.ContentCopy, "Копіювати") }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Видалити запис") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = ::onMic,
                containerColor = if (recording) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.primaryContainer,
                icon = { Icon(if (recording) Icons.Default.Stop else Icons.Default.Mic, null) },
                text = { Text(if (recording) "Стоп  ${formatDuration(elapsed)}" else "Записати голос") },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                val topic = topics.firstOrNull { it.id == state.topicId }
                Box {
                    AssistChip(
                        onClick = { topicMenu = true },
                        leadingIcon = { if (topic != null) TopicDot(topic.color) },
                        label = { Text(topic?.name ?: "Без розділу") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    )
                    DropdownMenu(expanded = topicMenu, onDismissRequest = { topicMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Без розділу") },
                            onClick = { vm.setEntryTopic(null); topicMenu = false },
                        )
                        topics.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.name) },
                                leadingIcon = { TopicDot(t.color) },
                                onClick = { vm.setEntryTopic(t.id); topicMenu = false },
                            )
                        }
                        if (topics.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Розділи створюються на головному екрані") },
                                enabled = false,
                                onClick = {},
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = text,
                    onValueChange = vm::onTextChange,
                    placeholder = { Text("Як минув день?") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                )
            }
            if (notes.isNotEmpty()) {
                item {
                    Text("Голосові нотатки", style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp))
                }
            }
            items(notes, key = { it.id }) { note ->
                VoiceNoteRow(
                    note = note,
                    playing = playingId == note.id,
                    onPlay = { vm.togglePlay(note) },
                    onDelete = { vm.deleteNote(note) },
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Видалити цей запис?") },
            text = { Text("Текст і всі голосові нотатки буде видалено назавжди.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.deleteEntry() }) { Text("Видалити") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Скасувати") } },
        )
    }
}

@Composable
private fun VoiceNoteRow(note: VoiceNote, playing: Boolean, onPlay: () -> Unit, onDelete: () -> Unit) {
    val time = remember(note.createdAt) {
        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(note.createdAt))
    }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPlay) {
                Icon(if (playing) Icons.Default.Stop else Icons.Default.PlayArrow,
                    if (playing) "Зупинити" else "Відтворити")
            }
            Column(Modifier.weight(1f)) {
                Text("Нотатка о $time", style = MaterialTheme.typography.bodyLarge)
                Text(formatDuration(note.durationMs), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, "Видалити нотатку") }
        }
    }
}
