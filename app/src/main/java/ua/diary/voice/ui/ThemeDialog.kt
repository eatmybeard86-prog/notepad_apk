package ua.diary.voice.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Вибір кольорової теми та світлого/темного режиму. Зміни застосовуються одразу. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeDialog(
    paletteId: String,
    darkMode: DarkMode,
    onPalette: (String) -> Unit,
    onDarkMode: (DarkMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val dark = isDark(darkMode)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Тема оформлення") },
        text = {
            Column {
                Text("Колір", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PALETTES.forEach { p ->
                        Swatch(
                            label = p.name,
                            color = if (dark) p.dark else p.light,
                            selected = paletteId == p.id,
                            onClick = { onPalette(p.id) },
                        )
                    }
                    if (supportsDynamicColor) {
                        Swatch(
                            label = "Як шпалери",
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            selected = paletteId == PALETTE_SYSTEM,
                            onClick = { onPalette(PALETTE_SYSTEM) },
                            icon = true,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("Режим", style = MaterialTheme.typography.titleSmall)
                DarkMode.entries.forEach { m ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onDarkMode(m) }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = darkMode == m, onClick = { onDarkMode(m) })
                        Spacer(Modifier.width(4.dp))
                        Text(m.label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } },
    )
}

@Composable
private fun Swatch(label: String, color: Color, selected: Boolean, onClick: () -> Unit, icon: Boolean = false) {
    Column(
        Modifier.width(64.dp).clip(MaterialTheme.shapes.small).clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        var m = Modifier.size(40.dp).clip(CircleShape).background(color)
        if (selected) m = m.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
        Box(m, contentAlignment = Alignment.Center) {
            when {
                selected -> Icon(Icons.Default.Check, null,
                    tint = if (icon) MaterialTheme.colorScheme.onSurface else Color.White)
                icon -> Icon(Icons.Default.Wallpaper, null, tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2)
    }
}
