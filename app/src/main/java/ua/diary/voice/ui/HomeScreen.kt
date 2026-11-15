package ua.diary.voice.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.diary.voice.data.EntryWithNotes
import ua.diary.voice.data.Topic
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: DiaryViewModel, lockEnabled: Boolean, onToggleLock: (Boolean) -> Unit) {
    val entries by vm.entries.collectAsStateWithLifecycle()
    val topics by vm.topics.collectAsStateWithLifecycle()
    val topicFilter by vm.topicFilter.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }

    val tab by vm.homeTab.collectAsStateWithLifecycle()
    var showSearch by rememberSaveable { mutableStateOf(query.isNotEmpty()) }
    var menu by remember { mutableStateOf(false) }
    var selectedDay by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var month by remember { mutableStateOf(YearMonth.from(LocalDate.ofEpochDay(selectedDay))) }

    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }

    val topicById = remember(topics) { topics.associateBy { it.id } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Щоденник") },
                actions = {
                    if (tab != 2) IconButton(onClick = {
                        showSearch = !showSearch
                        if (!showSearch) vm.setQuery("")
                    }) { Icon(Icons.Default.Search, "Пошук") }
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Меню") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Копіювати всі показані записи") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                            onClick = {
                                menu = false
                                if (entries.isEmpty()) vm.showMessage("Немає записів для копіювання")
                                else {
                                    clipboard.setText(AnnotatedString(vm.exportAll()))
                                    vm.showMessage("Скопійовано записів: ${entries.size}")
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Вхід відбитком") },
                            leadingIcon = { Icon(Icons.Default.Fingerprint, null) },
                            trailingIcon = { Switch(checked = lockEnabled, onCheckedChange = null) },
                            onClick = { onToggleLock(!lockEnabled) },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (tab != 2) ExtendedFloatingActionButton(
                onClick = { vm.newEntry(if (tab == 1) selectedDay else LocalDate.now().toEpochDay()) },
                icon = { Icon(Icons.Default.Edit, null) },
                text = { Text("Новий запис") },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { vm.setHomeTab(0) }, text = { Text("Список") })
                Tab(selected = tab == 1, onClick = { vm.setHomeTab(1) }, text = { Text("Календар") })
                Tab(selected = tab == 2, onClick = { vm.setHomeTab(2) }, text = { Text("Обов'язки") })
            }
            if (tab == 2) {
                DutiesTab(vm)
            } else {
            Spacer(Modifier.height(8.dp))
            TopicFilterRow(
                topics = topics,
                selected = topicFilter,
                onSelect = vm::setTopicFilter,
                onAdd = vm::addTopic,
                onRename = vm::renameTopic,
                onDelete = vm::deleteTopic,
            )
            if (showSearch) {
                OutlinedTextField(
                    value = query,
                    onValueChange = vm::setQuery,
                    placeholder = { Text("Пошук у записах") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            if (tab == 0) {
                if (entries.isEmpty()) {
                    EmptyHint(
                        if (query.isNotBlank()) "Нічого не знайдено"
                        else "Ще немає записів.\nНатисніть «Новий запис», щоб почати."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        entryCards(entries, topicById, showDate = true, vm = vm)
                    }
                }
            } else {
                val marks = remember(entries, topicById) {
                    entries.groupBy { it.entry.date }.mapValues { (_, list) ->
                        list.map { e -> topicById[e.entry.topicId]?.color ?: 0xFF888888.toInt() }.distinct()
                    }
                }
                val dayEntries = entries.filter { it.entry.date == selectedDay }
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        MonthCalendar(
                            month = month,
                            selected = LocalDate.ofEpochDay(selectedDay),
                            marks = marks,
                            onMonthChange = { month = it },
                            onSelect = { selectedDay = it.toEpochDay(); month = YearMonth.from(it) },
                        )
                    }
                    item {
                        Text(
                            "${formatWeekday(selectedDay)}, ${formatDate(selectedDay)}",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    if (dayEntries.isEmpty()) {
                        item {
                            Text(
                                "У цей день записів немає",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    } else {
                        entryCards(dayEntries, topicById, showDate = false, vm = vm, horizontal = 16)
                    }
                }
            }
            }
        }
    }
}

private fun LazyListScope.entryCards(
    list: List<EntryWithNotes>,
    topicById: Map<Long, Topic>,
    showDate: Boolean,
    vm: DiaryViewModel,
    horizontal: Int = 0,
) {
    items(list, key = { it.entry.id }) { item ->
        EntryCard(
            item = item,
            topic = item.entry.topicId?.let(topicById::get),
            showDate = showDate,
            modifier = Modifier.padding(horizontal = horizontal.dp),
            onClick = { vm.openEntry(item.entry) },
        )
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EntryCard(
    item: EntryWithNotes,
    topic: Topic?,
    showDate: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Card(modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    if (showDate) {
                        Text(formatWeekday(item.entry.date), style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
                        Text(formatDate(item.entry.date), style = MaterialTheme.typography.titleMedium)
                    }
                    if (topic != null) {
                        if (showDate) Spacer(Modifier.height(6.dp))
                        TopicLabel(topic)
                    }
                }
                if (item.notes.isNotEmpty()) {
                    Icon(Icons.Default.Mic, null, Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text("${item.notes.size}", style = MaterialTheme.typography.labelLarge)
                }
            }
            if (item.entry.text.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(item.entry.text, maxLines = 3, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium)
            } else if (!showDate && topic == null) {
                Text("Лише голосові нотатки", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
