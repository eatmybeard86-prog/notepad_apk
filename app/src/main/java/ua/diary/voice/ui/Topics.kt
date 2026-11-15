package ua.diary.voice.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ua.diary.voice.data.Topic

@Composable
fun TopicDot(color: Int, modifier: Modifier = Modifier) {
    Box(modifier.size(10.dp).clip(CircleShape).background(Color(color)))
}

@Composable
fun TopicLabel(topic: Topic) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp))
            .background(Color(topic.color).copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TopicDot(topic.color)
        Text(topic.name, style = MaterialTheme.typography.labelMedium)
    }
}

/** Ряд фільтрів: «Усі», розділи (довге натискання — змінити), «+ Розділ». */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopicFilterRow(
    topics: List<Topic>,
    selected: Long?,
    onSelect: (Long?) -> Unit,
    onAdd: (String) -> Unit,
    onRename: (Topic, String) -> Unit,
    onDelete: (Topic) -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Topic?>(null) }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("Усі") })
        }
        items(topics, key = { it.id }) { t ->
            // Власний «чип», бо FilterChip не підтримує довге натискання
            val sel = selected == t.id
            val shape = RoundedCornerShape(8.dp)
            Surface(
                shape = shape,
                color = if (sel) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                border = if (sel) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.height(32.dp).clip(shape)
                    .combinedClickable(onClick = { onSelect(t.id) }, onLongClick = { editing = t }),
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TopicDot(t.color)
                    Text(t.name, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        item {
            AssistChip(
                onClick = { adding = true },
                leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(18.dp)) },
                label = { Text("Розділ") },
            )
        }
    }

    if (adding) {
        NameDialog(
            title = "Новий розділ",
            initial = "",
            onDismiss = { adding = false },
            onConfirm = { onAdd(it); adding = false },
        )
    }
    editing?.let { t ->
        NameDialog(
            title = "Розділ «${t.name}»",
            initial = t.name,
            onDismiss = { editing = null },
            onConfirm = { onRename(t, it); editing = null },
            onDelete = { onDelete(t); editing = null },
        )
    }
}

@Composable
fun NameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onDelete: (() -> Unit)? = null,
    placeholder: String = "Назва, напр. «Робота»",
    deleteTitle: String = "Видалити розділ?",
    deleteText: String = "Записи залишаться, але будуть без розділу.",
) {
    var name by remember { mutableStateOf(initial) }
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(deleteTitle) },
            text = { Text(deleteText) },
            confirmButton = { TextButton(onClick = onDelete) { Text("Видалити") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Скасувати") } },
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true,
                placeholder = { Text(placeholder) })
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Зберегти") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = { confirmDelete = true }) { Text("Видалити") }
                TextButton(onClick = onDismiss) { Text("Скасувати") }
            }
        },
    )
}
