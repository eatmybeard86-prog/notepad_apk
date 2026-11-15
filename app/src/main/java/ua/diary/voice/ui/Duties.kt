package ua.diary.voice.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.diary.voice.data.DutyPosition
import ua.diary.voice.data.DutyRepository
import ua.diary.voice.data.UnitWithPositions

private const val UNIT_PLACEHOLDER = "Назва, напр. «Взвод зв'язку»"

/** Вкладка «Обов'язки» на головному екрані: список підрозділів. */
@Composable
fun DutiesTab(vm: DiaryViewModel) {
    val units by vm.units.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(units, key = { it.unit.id }) { u -> UnitCard(u, vm) }
        item {
            OutlinedButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Додати підрозділ")
            }
        }
    }

    if (adding) {
        NameDialog(
            title = "Новий підрозділ",
            initial = "",
            placeholder = UNIT_PLACEHOLDER,
            onDismiss = { adding = false },
            onConfirm = { vm.addUnit(it); adding = false },
        )
    }
}

@Composable
private fun UnitCard(u: UnitWithPositions, vm: DiaryViewModel) {
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val withDocs = u.positions.count { it.docFileName != null }

    Card(Modifier.fillMaxWidth().clickable { vm.openUnit(u.unit.id) }) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Groups, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(u.unit.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (u.positions.isEmpty()) "Посад ще немає"
                    else "Посад: ${u.positions.size} · з інструкцією: $withDocs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Дії") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Перейменувати") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = { menu = false; renaming = true })
                    DropdownMenuItem(text = { Text("Видалити") },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = { menu = false; deleting = true })
                }
            }
        }
    }

    if (renaming) {
        NameDialog(
            title = "Перейменувати підрозділ",
            initial = u.unit.name,
            placeholder = UNIT_PLACEHOLDER,
            onDismiss = { renaming = false },
            onConfirm = { vm.renameUnit(u.unit, it); renaming = false },
        )
    }
    if (deleting) {
        ConfirmDelete(
            title = "Видалити «${u.unit.name}»?",
            text = "Усі посади цього підрозділу і закріплені за ними інструкції буде видалено з додатка.",
            onDismiss = { deleting = false },
            onConfirm = { vm.deleteUnit(u.unit.id); deleting = false },
        )
    }
}

/** Екран підрозділу: посади та їхні інструкції. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitScreen(vm: DiaryViewModel, unitId: Long) {
    val ctx = LocalContext.current
    val units by vm.units.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val u = units.firstOrNull { it.unit.id == unitId }
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var addingPosition by remember { mutableStateOf(false) }

    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        vm.attachPicked(uri)
    }
    fun pickFor(p: DutyPosition) {
        vm.attachTargetId = p.id
        ExternalLaunch.mark()
        try {
            picker.launch(DutyRepository.DOC_MIME_TYPES)
        } catch (e: android.content.ActivityNotFoundException) {
            ExternalLaunch.consume()
            vm.attachTargetId = null
            vm.showMessage("На телефоні немає програми для вибору файлів")
        }
    }
    fun open(p: DutyPosition) {
        val f = vm.duty.fileOf(p) ?: return
        if (!openDocument(ctx, f, p.docMime ?: DutyRepository.mimeFromName(f.name))) {
            vm.showMessage("Немає програми для відкриття. Встановіть переглядач PDF або Word / Google Docs.")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { vm.openUnit(null) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
                },
                title = { Text(u?.unit?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                actions = {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Меню") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Перейменувати підрозділ") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { menu = false; renaming = true })
                        DropdownMenuItem(text = { Text("Видалити підрозділ") },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = { menu = false; deleting = true })
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { addingPosition = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Додати посаду") },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val positions = u?.positions.orEmpty().sortedWith(compareBy({ it.sortOrder }, { it.id }))
        if (positions.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Посад ще немає.\nНатисніть «Додати посаду», а потім закріпіть за нею інструкцію.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(positions, key = { it.id }) { p ->
                    PositionCard(
                        p = p,
                        vm = vm,
                        onOpen = { open(p) },
                        onAttach = { pickFor(p) },
                        onShare = {
                            vm.duty.fileOf(p)?.let { f ->
                                shareDocument(ctx, f, p.docMime ?: DutyRepository.mimeFromName(f.name), p.docTitle ?: p.name)
                            }
                        },
                    )
                }
            }
        }
    }

    if (addingPosition) {
        NameDialog(
            title = "Нова посада",
            initial = "",
            placeholder = "Напр. «Командир відділення»",
            onDismiss = { addingPosition = false },
            onConfirm = { vm.addPosition(unitId, it); addingPosition = false },
        )
    }
    if (renaming && u != null) {
        NameDialog(
            title = "Перейменувати підрозділ",
            initial = u.unit.name,
            placeholder = UNIT_PLACEHOLDER,
            onDismiss = { renaming = false },
            onConfirm = { vm.renameUnit(u.unit, it); renaming = false },
        )
    }
    if (deleting && u != null) {
        ConfirmDelete(
            title = "Видалити «${u.unit.name}»?",
            text = "Усі посади цього підрозділу і закріплені за ними інструкції буде видалено з додатка.",
            onDismiss = { deleting = false },
            onConfirm = { deleting = false; vm.deleteUnit(u.unit.id) },
        )
    }
}

@Composable
private fun PositionCard(
    p: DutyPosition,
    vm: DiaryViewModel,
    onOpen: () -> Unit,
    onAttach: () -> Unit,
    onShare: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val hasDoc = p.docFileName != null

    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Badge, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(p.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Дії з посадою") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Перейменувати посаду") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = { menu = false; renaming = true })
                    if (hasDoc) {
                        DropdownMenuItem(text = { Text("Замінити інструкцію") },
                            leadingIcon = { Icon(Icons.Default.SwapHoriz, null) },
                            onClick = { menu = false; onAttach() })
                        DropdownMenuItem(text = { Text("Надіслати інструкцію") },
                            leadingIcon = { Icon(Icons.Default.Share, null) },
                            onClick = { menu = false; onShare() })
                        DropdownMenuItem(text = { Text("Відкріпити інструкцію") },
                            leadingIcon = { Icon(Icons.Default.LinkOff, null) },
                            onClick = { menu = false; vm.detachDocument(p) })
                    }
                    DropdownMenuItem(text = { Text("Видалити посаду") },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = { menu = false; deleting = true })
                }
            }
        }
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        if (hasDoc) {
            val isPdf = p.docMime == "application/pdf" || p.docFileName?.endsWith(".pdf") == true
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Description, null,
                    tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Посадова інструкція", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(p.docTitle ?: "Документ", maxLines = 2, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Відкрити")
            }
        } else {
            TextButton(onClick = onAttach, modifier = Modifier.padding(8.dp)) {
                Icon(Icons.Default.AttachFile, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Закріпити інструкцію (PDF, DOC, DOCX)")
            }
        }
        Spacer(Modifier.height(4.dp))
    }

    if (renaming) {
        NameDialog(
            title = "Перейменувати посаду",
            initial = p.name,
            placeholder = "Назва посади",
            onDismiss = { renaming = false },
            onConfirm = { vm.renamePosition(p, it); renaming = false },
        )
    }
    if (deleting) {
        ConfirmDelete(
            title = "Видалити посаду «${p.name}»?",
            text = if (hasDoc) "Закріплену інструкцію теж буде видалено з додатка." else "Посаду буде видалено.",
            onDismiss = { deleting = false },
            onConfirm = { deleting = false; vm.deletePosition(p) },
        )
    }
}

@Composable
private fun ConfirmDelete(title: String, text: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Видалити") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
    )
}
